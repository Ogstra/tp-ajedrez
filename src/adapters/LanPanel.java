package adapters;

import core.Color;
import network.LanConnection;
import network.LanGame;
import network.LanLobby;

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
import java.util.ArrayList;
import java.util.List;

// Pantalla "Jugar por LAN", con dos vistas: la lista (crear una partida o unirse a una de las
// que hay en la red) y la espera de rival. Solo muestra y reacciona a los botones: los sockets y
// los hilos están en LanLobby, que entra por constructor. Lo que el lobby avisa desde sus hilos
// se pasa al hilo de la interfaz con invokeLater; cuando hay rival, se le da la conexión a la ventana.
public class LanPanel extends JPanel {

    // Lo que hace la ventana cuando hay rival: arma la partida con esta conexión.
    public interface Listener {
        void connected(LanConnection connection, Color localColor, String rivalName);
    }

    private static final String LIST = "list";
    private static final String WAITING = "waiting";

    private final LanLobby lobby;
    private final Listener listener;
    private final JPanel views = new JPanel(new CardLayout());
    private final JPanel gamesBox = new JPanel(new GridBagLayout());
    private final JLabel empty = MenuPanel.small("No hay partidas en la red.");
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final FlatButton createButton = new FlatButton("Crear partida", FlatButton.Style.PRIMARY);
    private final Timer refresh = new Timer(1000, e -> showGames()); // la lista se actualiza cada segundo
    private final LanLobby.Listener lobbyListener = new LanLobby.Listener() {
        @Override
        public void connected(LanConnection connection, Color localColor, String rivalName) {
            SwingUtilities.invokeLater(() -> {
                // Entre el aviso y este momento la persona pudo tocar Volver: ahí la conexión sobra.
                if (!active) {
                    connection.close();
                    return;
                }
                start(connection, localColor, rivalName);
            });
        }

        @Override
        public void failed(String text) {
            SwingUtilities.invokeLater(() -> {
                if (!active) return;
                open(ownName);
                message.setText(text);
            });
        }
    };
    private List<String> shownIds = new ArrayList<>();
    private String ownName;
    private boolean waiting = false;
    private boolean active = false; // la pantalla está abierta (entre open y close)

    public LanPanel(LanLobby lobby, Listener listener, Runnable onBack) {
        this.lobby = lobby;
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
            lobby.stopHosting();
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
        active = true;
        waiting = false;
        message.setForeground(Palette.ERROR);
        message.setText(" ");
        shownIds = new ArrayList<>();
        ((CardLayout) views.getLayout()).show(views, LIST);
        if (!lobby.startBrowsing()) message.setText("No se pudo buscar partidas en la red.");
        showGames();
        refresh.start();
        updateDefaultButton();
    }

    // Deja de buscar y de esperar rival (al volver al menú o al empezar la partida).
    public void close() {
        active = false;
        refresh.stop();
        lobby.close();
    }

    // Crear partida: el lobby abre un puerto libre, lo anuncia y espera a que alguien se una.
    private void create() {
        refresh.stop();
        if (!lobby.host(ownName, lobbyListener)) {
            open(ownName);
            message.setText("No se pudo crear la partida.");
            return;
        }
        waiting = true;
        ((CardLayout) views.getLayout()).show(views, WAITING);
        updateDefaultButton();
    }

    // Unirse: el lobby se conecta en otro hilo a la PC que anunció la partida. Mientras tanto se
    // avisa "Conectando..."; un segundo click no abre otra conexión (el lobby lo rechaza).
    private void join(LanGame game) {
        if (lobby.join(game, ownName, lobbyListener)) {
            message.setForeground(Palette.MUTED);
            message.setText("Conectando con la partida de " + game.getHostName() + "...");
        }
    }

    private void start(LanConnection connection, Color localColor, String rival) {
        close();
        listener.connected(connection, localColor, rival);
    }

    // Un botón por partida ("Partida de Ana"). Solo se rearma si cambió la lista.
    private void showGames() {
        List<LanGame> games = lobby.games();
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
