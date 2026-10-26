package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Escucha los avisos de LanAnnouncer y arma la lista de partidas abiertas en la red. La IP de
// cada partida sale del paquete recibido: nadie la tiene que escribir. Si una partida deja de
// avisar (se empezó a jugar o se cerró), a los 3 segundos sale de la lista. El mismo aviso
// puede llegar por varias placas de red; se guarda una sola vez por su identificador.
public class LanBrowser {

    private static final long FORGET_AFTER_MS = 3000;

    private final Map<String, LanGame> games = new LinkedHashMap<>();
    private final Map<String, Long> lastSeen = new LinkedHashMap<>();
    private volatile boolean running = false;
    private DatagramSocket socket;

    // Empieza a escuchar en otro hilo. Devuelve false si el puerto de los avisos no está disponible.
    public boolean start() {
        try {
            socket = new DatagramSocket(null);
            // Así pueden buscar dos ventanas en la misma PC. En Windows alcanza con SO_REUSEADDR;
            // en macOS y Linux además hace falta SO_REUSEPORT (Windows no lo tiene).
            socket.setReuseAddress(true);
            if (socket.supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT)) {
                socket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            }
            socket.bind(new InetSocketAddress(LanAnnouncer.DISCOVERY_PORT));
            socket.setSoTimeout(500);     // cada medio segundo se revisa si hay que dejar de escuchar
        } catch (IOException e) {
            return false;
        }
        running = true;
        Thread thread = new Thread(this::listen, "busqueda-lan");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    // Las partidas vistas en los últimos segundos, en el orden en que aparecieron.
    public synchronized List<LanGame> games() {
        long now = System.currentTimeMillis();
        lastSeen.entrySet().removeIf(entry -> {
            boolean old = now - entry.getValue() > FORGET_AFTER_MS;
            if (old) games.remove(entry.getKey());
            return old;
        });
        return new ArrayList<>(games.values());
    }

    private void listen() {
        byte[] buffer = new byte[256];
        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet); // espera un aviso
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                break; // se cerró el socket con stop()
            }
            String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            LanGame game = parse(text, packet);
            if (game != null) seen(game);
        }
    }

    // "AJEDREZ;4821;Ana;52817" -> la partida 4821 de Ana, en la IP que mandó el paquete, puerto 52817.
    // Cualquier PC de la red puede mandar un paquete a este puerto, así que se descarta lo que no
    // tenga la forma esperada y el nombre se limpia antes de mostrarlo.
    private static LanGame parse(String text, DatagramPacket packet) {
        String[] parts = text.split(";");
        if (parts.length != 4 || !parts[0].equals(LanAnnouncer.PREFIX)) return null;
        try {
            int port = Integer.parseInt(parts[3]);
            if (port < 1 || port > 65535) return null;
            return new LanGame(parts[1], LanNames.clean(parts[2]), packet.getAddress(), port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private synchronized void seen(LanGame game) {
        games.putIfAbsent(game.getId(), game); // la primera dirección por la que llegó es la que se usa
        lastSeen.put(game.getId(), System.currentTimeMillis());
    }
}
