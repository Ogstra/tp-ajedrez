package red;

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
public class LanConnection {

    public static final int PORT = 5000;           // puerto de la partida
    private static final int CONNECT_TIMEOUT_MS = 3000;
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
        return new LanConnection(server.accept());
    }

    // Quien se une: se conecta a la PC que anunció la partida.
    public static LanConnection connect(InetAddress address, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(address, port), CONNECT_TIMEOUT_MS);
        return new LanConnection(socket);
    }

    // Saludo al conectarse: cada uno manda "HOLA;su nombre" y lee el del otro. Devuelve el
    // nombre del rival, o null si lo que llegó no es del juego.
    public String greet(String ownName) throws IOException {
        send(HELLO + ";" + ownName);
        String line = receive();
        if (line == null || !line.startsWith(HELLO + ";")) return null;
        return line.substring(HELLO.length() + 1);
    }

    public void send(String line) {
        out.println(line);
    }

    // Espera la próxima línea del rival. Devuelve null si el rival se fue (cerró el juego,
    // salió de la partida o se cortó la red).
    public String receive() {
        try {
            String line = in.readLine();
            return line == null || line.equals(BYE) ? null : line;
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
