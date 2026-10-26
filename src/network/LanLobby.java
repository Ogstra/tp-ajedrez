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
public class LanLobby {

    // A quién se le avisa cuando hay rival o cuando no se pudo conectar.
    public interface Listener {
        void connected(LanConnection connection, Color localColor, String rivalName);
        void failed(String message);
    }

    private LanBrowser browser;
    private LanAnnouncer announcer;
    private ServerSocket server;

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
    public synchronized boolean host(String ownName, Listener listener) {
        stopBrowsing();
        try {
            server = new ServerSocket(0);
        } catch (IOException e) {
            return false;
        }
        announcer = new LanAnnouncer(ownName, server.getLocalPort());
        announcer.start();
        ServerSocket current = server;
        new Thread(() -> {
            try {
                LanConnection connection = LanConnection.waitForRival(current);
                String rival = connection.greet(ownName);
                synchronized (this) {
                    if (server != current) { // se canceló mientras se saludaban
                        connection.close();
                        return;
                    }
                    stopHosting();
                }
                if (rival == null) {
                    connection.close();
                    listener.failed("Se conectó algo que no es el juego.");
                } else {
                    listener.connected(connection, Color.WHITE, rival);
                }
            } catch (IOException e) {
                // se canceló: el server se cerró mientras esperaba
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

    // Se une a una partida de la lista, en otro hilo. Quien se une juega con negras.
    public void join(LanGame game, String ownName, Listener listener) {
        new Thread(() -> {
            try {
                LanConnection connection = LanConnection.connect(game.getAddress(), game.getPort());
                String rival = connection.greet(ownName);
                if (rival == null) {
                    connection.close();
                    listener.failed("No se pudo conectar con esa partida.");
                } else {
                    stopBrowsing();
                    listener.connected(connection, Color.BLACK, rival);
                }
            } catch (IOException e) {
                listener.failed("No se pudo conectar con esa partida.");
            }
        }, "union-lan").start();
    }

    // Corta todo: al volver al menú o al cerrar la pantalla.
    public synchronized void close() {
        stopBrowsing();
        stopHosting();
    }
}
