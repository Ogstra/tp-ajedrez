package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Avisa a la red local que hay una partida esperando rival. Cada segundo manda por UDP un
// mensaje de texto "AJEDREZ;4821;Ana;52817" (identificador de la partida, nombre de quien la
// creó y puerto TCP donde espera) a todas las PCs de la red (broadcast). No hace falta conexión:
// si un aviso se pierde, llega el siguiente. Lo escucha LanBrowser.
public class LanAnnouncer {

    // Puerto UDP de los avisos. Es el único puerto fijo del juego, porque las dos PCs tienen que
    // coincidir en algo para encontrarse; el de la partida lo elige el sistema (ver LanLobby).
    // No se usa 5000 ni 7000: en macOS los ocupa el receptor de AirPlay. Si en alguna red está
    // ocupado, se cambia al arrancar con -Dajedrez.lan.port=otro (igual en las dos PCs).
    public static final int DISCOVERY_PORT = discoveryPort();
    public static final String PREFIX = "AJEDREZ";    // para ignorar paquetes que no son del juego
    private static final int INTERVAL_MS = 1000;

    // El puerto pedido con -Dajedrez.lan.port, si es válido (1025 a 65535); si no, el de siempre.
    private static int discoveryPort() {
        Integer requested = Integer.getInteger("ajedrez.lan.port");
        return requested != null && requested > 1024 && requested <= 65535 ? requested : 47123;
    }

    private final byte[] message;
    private volatile boolean running = false;

    public LanAnnouncer(String hostName, int gamePort) {
        // El identificador distingue dos partidas con el mismo nombre (por ejemplo, dos invitados).
        String id = String.valueOf(new Random().nextInt(1_000_000));
        this.message = (PREFIX + ";" + id + ";" + LanNames.clean(hostName) + ";" + gamePort).getBytes(StandardCharsets.UTF_8);
    }

    // Empieza a avisar en otro hilo, para no frenar la ventana.
    public void start() {
        running = true;
        Thread thread = new Thread(this::announce, "aviso-lan");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    private void announce() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            while (running) {
                for (InetAddress target : targets()) {
                    try {
                        socket.send(new DatagramPacket(message, message.length, target, DISCOVERY_PORT));
                    } catch (IOException e) {
                        // esa placa de red no deja mandar: se sigue con las demás
                    }
                }
                Thread.sleep(INTERVAL_MS);
            }
        } catch (IOException | InterruptedException e) {
            running = false;
        }
    }

    // A dónde se manda el aviso: la dirección de broadcast de cada placa de red conectada (por
    // ejemplo 192.168.1.255), porque una PC puede tener varias (wifi, cable, VPN, máquinas
    // virtuales). También a la propia PC, para poder probar con dos ventanas en la misma máquina.
    private static List<InetAddress> targets() {
        List<InetAddress> targets = new ArrayList<>();
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!network.isUp() || network.isLoopback()) continue;
                for (InterfaceAddress address : network.getInterfaceAddresses()) {
                    if (address.getBroadcast() != null) targets.add(address.getBroadcast());
                }
            }
            targets.add(InetAddress.getByName("255.255.255.255"));
            targets.add(InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            // sin placas de red solo queda la propia PC
            targets.add(InetAddress.getLoopbackAddress());
        }
        return targets;
    }
}
