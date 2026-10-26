package network;

import core.Color;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

// Todo lo de red antes de que empiece la partida: buscar partidas, crear una y esperar rival, o
// unirse a una de la lista. Antes esto lo hacía la pantalla (LanPanel) con sockets e hilos
// mezclados con los botones; ahora la pantalla solo muestra y le pide cosas al lobby (una sola
// razón de cambio por clase, SRP). Las esperas van en otros hilos y el resultado se avisa por
// Listener; quien lo recibe en una ventana tiene que pasarlo al hilo de la interfaz. Los métodos son
// synchronized porque los llaman tanto la ventana como esos hilos.
//
// Cada intento (crear o unirse) tiene un número. close() o un intento nuevo invalidan los
// anteriores: si una conexión vieja termina de llegar después de que se canceló, se cierra y no se
// avisa. Así no arranca una partida que la persona ya canceló, ni dos con un doble click.
public class LanLobby {

    // A quién se le avisa cuando hay rival o cuando no se pudo conectar.
    public interface Listener {
        void connected(LanConnection connection, Color localColor, String rivalName);
        void failed(String message);
    }

    private LanBrowser browser;
    private LanAnnouncer announcer;
    private ServerSocket server;
    private int attempt = 0;         // el intento vigente; los de número menor ya no avisan
    private boolean joining = false; // hay un "unirse" en curso

    // Empieza a escuchar los avisos de la red. Devuelve false si el puerto de los avisos está ocupado.
    public synchronized boolean startBrowsing() {
        stopBrowsing();
        browser = new LanBrowser();
        return browser.start();
    }

    public synchronized List<LanGame> games() {
        return browser == null ? new ArrayList<>() : browser.games();
    }

    public synchronized void stopBrowsing() {
        if (browser != null) browser.stop();
        browser = null;
    }

    // Crea la partida: abre un puerto libre (lo elige el sistema), lo anuncia y espera en otro hilo
    // a que alguien se una. Quien crea juega con blancas. Devuelve false si no se pudo abrir.
    // Si se conecta algo que no es el juego (por ejemplo, un escaneo de puertos), se sigue esperando.
    public synchronized boolean host(String ownName, Listener listener) {
        stopBrowsing();
        stopHosting();
        attempt++;
        try {
            server = new ServerSocket(0);
        } catch (IOException e) {
            return false;
        }
        announcer = new LanAnnouncer(ownName, server.getLocalPort());
        announcer.start();
        ServerSocket current = server;
        new Thread(() -> {
            while (true) {
                LanConnection connection;
                try {
                    connection = LanConnection.waitForRival(current);
                } catch (IOException e) {
                    synchronized (this) {
                        if (server != current) return; // se canceló: el server se cerró mientras esperaba
                        stopHosting();
                    }
                    listener.failed("No se pudo esperar rival.");
                    return;
                }
                String rival = greetOrNull(connection, ownName);
                synchronized (this) {
                    if (server != current) { // se canceló mientras se saludaban
                        connection.close();
                        return;
                    }
                    if (rival == null) {     // no era el juego: se descarta y se sigue esperando
                        connection.close();
                        continue;
                    }
                    stopHosting();
                }
                listener.connected(connection, Color.WHITE, rival);
                return;
            }
        }, "espera-lan").start();
        return true;
    }

    // Deja de anunciar y de esperar rival.
    public synchronized void stopHosting() {
        if (announcer != null) announcer.stop();
        announcer = null;
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                // ya estaba cerrado
            }
        }
        server = null;
    }

    // Se une a una partida de la lista, en otro hilo. Quien se une juega con negras. Devuelve false
    // (y no hace nada) si ya hay un intento de unirse en curso, por ejemplo con un doble click.
    public synchronized boolean join(LanGame game, String ownName, Listener listener) {
        if (joining) return false;
        joining = true;
        int mine = ++attempt;
        new Thread(() -> {
            LanConnection connection = null;
            String rival = null;
            try {
                connection = LanConnection.connect(game.getAddress(), game.getPort());
                rival = greetOrNull(connection, ownName);
            } catch (IOException e) {
                // no se pudo conectar: rival queda en null
            }
            synchronized (this) {
                if (mine != attempt) { // se canceló (Volver) o empezó otro intento mientras tanto
                    if (connection != null) connection.close();
                    return;
                }
                joining = false;
                if (rival != null) stopBrowsing();
            }
            if (rival == null) {
                if (connection != null) connection.close();
                listener.failed("No se pudo conectar con esa partida.");
            } else {
                listener.connected(connection, Color.BLACK, rival);
            }
        }, "union-lan").start();
        return true;
    }

    // Corta todo: al volver al menú o al cerrar la pantalla. Los intentos en curso ya no avisan.
    public synchronized void close() {
        attempt++;
        joining = false;
        stopBrowsing();
        stopHosting();
    }

    private static String greetOrNull(LanConnection connection, String ownName) {
        try {
            return connection.greet(ownName);
        } catch (IOException e) {
            return null;
        }
    }
}
