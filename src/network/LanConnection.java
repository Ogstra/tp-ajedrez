package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// El canal entre las dos PCs durante la partida (TCP). TCP garantiza que lo que se manda llega
// completo y en orden, así que alcanza con mandar líneas de texto: una jugada por línea.
// Quien crea la partida espera con waitForRival(); quien se une llama a connect().
//
// No hay puerto fijo: quien crea la partida abre el ServerSocket en el puerto 0 (el sistema elige
// uno libre) y lo anuncia por UDP. Así no se choca con otro programa, como el receptor de AirPlay
// de macOS, que ocupa el 5000.
public class LanConnection {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int GREETING_TIMEOUT_MS = 5000; // quien se conecta y no saluda, no traba la espera
    private static final int MAX_LINE = 128;             // una jugada o un saludo entran de sobra
    private static final String HELLO = "HOLA";
    private static final String BYE = "CHAU";

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    private LanConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    // Quien crea la partida: espera hasta que alguien se una. Se cancela cerrando el server.
    public static LanConnection waitForRival(ServerSocket server) throws IOException {
        return open(server.accept());
    }

    // Quien se une: se conecta a la PC que anunció la partida.
    public static LanConnection connect(InetAddress address, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), CONNECT_TIMEOUT_MS);
        } catch (IOException e) {
            socket.close(); // no queda un socket abierto por cada intento fallido
            throw e;
        }
        return open(socket);
    }

    // Arma la conexión sobre un socket ya conectado; si no se puede, lo cierra.
    private static LanConnection open(Socket socket) throws IOException {
        try {
            return new LanConnection(socket);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    // Saludo al conectarse: cada uno manda "HOLA;su nombre" y lee el del otro. Devuelve el
    // nombre del rival ya limpio (ver LanNames), o null si lo que llegó no es del juego o no
    // llegó a tiempo. El plazo es para el saludo entero, no por cada lectura: alguien que manda
    // un byte por vez no retiene la espera. Después del saludo, esperar jugadas no tiene límite.
    public String greet(String ownName) throws IOException {
        send(HELLO + ";" + LanNames.clean(ownName));
        String line = readLine(System.currentTimeMillis() + GREETING_TIMEOUT_MS);
        socket.setSoTimeout(0);
        if (line == null || !line.startsWith(HELLO + ";")) return null;
        return LanNames.clean(line.substring(HELLO.length() + 1));
    }

    public void send(String line) {
        out.println(line);
    }

    // Espera la próxima línea del rival. Devuelve null si el rival se fue (cerró el juego,
    // salió de la partida o se cortó la red) o si manda una línea más larga de lo que el juego
    // usa: lo que llega de la otra PC no es confiable y no se guarda sin límite en memoria.
    public String receive() {
        return readLine(0);
    }

    // Lee una línea; con deadline > 0, devuelve null si no termina de llegar antes de ese momento.
    private String readLine(long deadline) {
        try {
            StringBuilder line = new StringBuilder();
            while (true) {
                if (deadline > 0) {
                    long left = deadline - System.currentTimeMillis();
                    if (left <= 0) return null;
                    socket.setSoTimeout((int) left);
                }
                int c = in.read();
                if (c == '\n') break;
                if (c == -1) return null;
                if (c == '\r') continue;
                if (line.length() == MAX_LINE) return null;
                line.append((char) c);
            }
            return line.toString().equals(BYE) ? null : line.toString();
        } catch (IOException e) {
            return null;
        }
    }

    // Avisa que se sale y cierra el canal. El rival lo recibe como desconexión.
    public void close() {
        if (socket.isClosed()) return;
        send(BYE);
        try {
            socket.close();
        } catch (IOException e) {
            // ya estaba cerrado
        }
    }
}
