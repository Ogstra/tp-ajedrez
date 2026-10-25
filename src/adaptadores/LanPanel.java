package adaptadores;

import nucleo.Color;
import red.LanAnnouncer;
import red.LanBrowser;
import red.LanConnection;
import red.LanGame;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

// Pantalla "Jugar por LAN", con dos vistas: la lista (crear una partida o unirse a una de las
// que hay en la red) y la espera de rival. Las tareas de red van en otros hilos para no
// congelar la ventana; cuando las dos PCs se conectan, le pasa la conexión a SwingChessView.
public class LanPanel extends JPanel {

    // Lo que hace la ventana cuando hay rival: arma la partida con esta conexión.
    public interface Listener {
        void connected(LanConnection connection, Color localColor, String rivalName);
    }

    private static final String LIST = "list";
    private static final String WAITING = "waiting";

    private final Listener listener;
    private final JPanel views = new JPanel(new CardLayout());
    private final JPanel gamesBox = new JPanel(new GridBagLayout());
    private final JLabel empty = MenuPanel.small("No hay partidas en la red.");
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final FlatButton createButton = new FlatButton("Crear partida", FlatButton.Style.PRIMARY);
    private final Timer refresh = new Timer(1000, e -> showGames()); // la lista se actualiza cada segundo
    private List<String> shownIds = new ArrayList<>();
    private String ownName;
    private LanBrowser browser;
    private LanAnnouncer announcer;
    private ServerSocket server;
    private boolean waiting = false;

    public LanPanel(Listener listener, Runnable onBack) {
        this.listener = listener;
        message.setForeground(Palette.ERROR);
        gamesBox.setOpaque(false);
        empty.setHorizontalAlignment(SwingConstants.CENTER);

        createButton.addActionListener(e -> create());
        FlatButton backButton = new FlatButton("Volver", FlatButton.Style.LINK);
        backButton.addActionListener(e -> {
            close();
            onBack.run();
        });
        FlatButton cancelButton = new FlatButton("Cancelar", FlatButton.Style.LINK);
        cancelButton.addActionListener(e -> {
            stopHosting();
            open(ownName);
        });

        JPanel list = header();
        MenuPanel.put(list, createButton, 20);
        MenuPanel.put(list, MenuPanel.label("Partidas en la red"), 20);
        MenuPanel.put(list, gamesBox, 6);
        MenuPanel.put(list, empty, 6);
        MenuPanel.put(list, message, 8);
        MenuPanel.put(list, backButton, 4);

        JLabel waitingText = MenuPanel.label("Esperando rival...");
        waitingText.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel wait = header();
        MenuPanel.put(wait, waitingText, 20);
        MenuPanel.put(wait, cancelButton, 12);

        views.setOpaque(false);
        views.add(list, LIST);
        views.add(wait, WAITING);

        // Las vistas quedan centradas en la ventana, a lo ancho y a lo alto.
        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 0, 12, 0);
        add(views, c);
    }

    // Enter aprieta Crear partida en la lista; mientras se espera rival no hace nada.
    public JButton getDefaultButton() { return waiting ? null : createButton; }

    // Muestra la lista y empieza a buscar partidas. ownName es el nombre que verá el rival.
    public void open(String ownName) {
        this.ownName = ownName;
        waiting = false;
        message.setText(" ");
        shownIds = new ArrayList<>();
        ((CardLayout) views.getLayout()).show(views, LIST);
        browser = new LanBrowser();
        if (!browser.start()) message.setText("No se pudo buscar partidas en la red.");
        showGames();
        refresh.start();
        updateDefaultButton();
    }

    // Deja de buscar y de esperar rival (al volver al menú o al empezar la partida).
    public void close() {
        refresh.stop();
        if (browser != null) browser.stop();
        stopHosting();
    }

    // Crear partida: se abre el puerto, se avisa a la red y se espera en otro hilo a que alguien se una.
    private void create() {
        try {
            server = new ServerSocket(LanConnection.PORT);
        } catch (IOException e) {
            message.setText("No se pudo crear la partida.");
            return;
        }
        refresh.stop();
        browser.stop();
        waiting = true;
        ((CardLayout) views.getLayout()).show(views, WAITING);
        updateDefaultButton();
        announcer = new LanAnnouncer(ownName, LanConnection.PORT);
        announcer.start();

        ServerSocket current = server;
        new Thread(() -> {
            try {
                LanConnection connection = LanConnection.waitForRival(current);
                String rival = connection.greet(ownName);
                SwingUtilities.invokeLater(() -> start(connection, Color.WHITE, rival));
            } catch (IOException e) {
                // se canceló: el server se cerró mientras esperaba
            }
        }, "espera-lan").start();
    }

    // Unirse: se conecta en otro hilo a la PC que anunció la partida. Quien se une juega con negras.
    private void join(LanGame game) {
        message.setText(" ");
        new Thread(() -> {
            try {
                LanConnection connection = LanConnection.connect(game.getAddress(), game.getPort());
                String rival = connection.greet(ownName);
                SwingUtilities.invokeLater(() -> start(connection, Color.BLACK, rival));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> message.setText("No se pudo conectar con esa partida."));
            }
        }, "union-lan").start();
    }

    private void start(LanConnection connection, Color localColor, String rival) {
        close();
        if (rival == null) { // del otro lado no respondió el juego
            connection.close();
            open(ownName);
            message.setText("No se pudo conectar con esa partida.");
            return;
        }
        listener.connected(connection, localColor, rival);
    }

    private void stopHosting() {
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

    // Un botón por partida ("Partida de Ana"). Solo se rearma si cambió la lista.
    private void showGames() {
        if (browser == null) return;
        List<LanGame> games = browser.games();
        List<String> ids = new ArrayList<>();
        for (LanGame game : games) ids.add(game.getId());
        if (ids.equals(shownIds)) return;
        shownIds = ids;

        gamesBox.removeAll();
        boolean first = true;
        for (LanGame game : games) {
            FlatButton button = new FlatButton("Partida de " + game.getHostName(), FlatButton.Style.SECONDARY);
            button.addActionListener(e -> join(game));
            MenuPanel.put(gamesBox, button, first ? 0 : 6);
            first = false;
        }
        empty.setVisible(games.isEmpty());
        revalidate();
        repaint();
    }

    private void updateDefaultButton() {
        if (getRootPane() != null) getRootPane().setDefaultButton(getDefaultButton());
    }

    // Título y casillas de adorno, como el resto de las pantallas.
    private static JPanel header() {
        JLabel title = new JLabel("Jugar por LAN", SwingConstants.CENTER);
        title.setFont(Palette.title(36));
        title.setForeground(Palette.PRIMARY);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        strip.setOpaque(false);
        strip.add(new BoardStrip());
        JPanel view = MenuPanel.column();
        MenuPanel.put(view, title, 0);
        MenuPanel.put(view, strip, 8);
        return view;
    }
}
