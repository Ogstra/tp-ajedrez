import adaptadores.FileAccountStore;
import adaptadores.GameBuilder;
import adaptadores.SwingChessView;
import nucleo.*;
import nucleo.especiales.*;
import nucleo.tablas.*;
import puertos.*;
import usuarios.AccountService;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

// Prueba de integracion de la ventana real (menu, partida y ranking en una sola ventana),
// sin reflection ni librerias de tests.
public class SwingIntegrationTests {
    private static SwingChessView view;
    private static IChessGame game;
    private static JPanel boardPanel;
    private static JTable rankingTable;
    private static JButton menuButton;
    private static Board before;
    private static Board after;

    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("ajedrez-swing");
        try {
            SwingUtilities.invokeAndWait(() -> {
                AccountService accounts = new AccountService(new FileAccountStore(data.resolve("usuarios.csv")));
                GameBuilder builder = (v, recorder) -> {
                    PieceFactory pieces = PieceFactory.standard();
                    Board board = BoardSetup.standard(pieces);
                    before = board.cloneBoard();
                    game = new ChessGame(board, Arrays.<IMoveListener>asList(v, recorder),
                        Arrays.<IGameStatusListener>asList(v, recorder),
                        Arrays.<ISpecialMove>asList(new CastlingMove(), new EnPassantMove(), new PromotionMove(pieces, v)),
                        Arrays.<IDrawRule>asList(new InsufficientMaterialRule(), new FiftyMoveRule(), new ThreefoldRepetitionRule()));
                    return game;
                };
                view = new SwingChessView(accounts, builder);
                view.setVisible(true);
                Container root = view.getContentPane();

                // Menu: el campo de usuario nuevo solo aparece al elegir "Nuevo usuario..." en un lado.
                JComboBox<?> white = (JComboBox<?>) named(root, "white");
                JComboBox<?> black = (JComboBox<?>) named(root, "black");
                require(!named(root, "newUser").isShowing(), "el campo de usuario nuevo empieza oculto");
                white.setSelectedIndex(1);
                require("Nuevo usuario...".equals(white.getSelectedItem()), "Nuevo usuario va justo abajo de Sin usuario");
                require(named(root, "newUser").isShowing(), "al elegir Nuevo usuario debe aparecer el campo");
                ((JTextField) named(root, "newUser")).setText("Ana");
                button(root, "Registrar").doClick();
                require(white.getItemCount() == 3 && "Ana".equals(white.getSelectedItem()),
                    "al registrar a Ana debe quedar elegida para las blancas");
                require(!named(root, "newUser").isShowing(), "al registrar, el campo vuelve a ocultarse");
                require(accounts.ranking().size() == 1, "el registro debe guardarse");

                require(view.getRootPane().getDefaultButton() == button(root, "Jugar"),
                    "en el menu, Enter debe apretar Jugar");

                // 1 contra 1: la dificultad y el color quedan bloqueados.
                require(!named(root, "level").isEnabled() && !named(root, "color").isEnabled(),
                    "en 1 contra 1 no se elige dificultad ni color");
                button(root, "Jugar").doClick();
                inspect(root);
                require(boardPanel.getComponentCount() == 64, "debe armarse el tablero de 64 casillas");
                require(view.getRootPane().getDefaultButton() == null, "en la partida no hay boton por defecto");
                require(label(root, "a") != null && label(root, "h") != null && label(root, "1") != null && label(root, "8") != null,
                    "el tablero muestra las coordenadas a-h y 1-8");

                // Seleccion y marcas de jugadas legales.
                java.awt.Color normalE3 = background("e3", false);
                java.awt.Color normalE5 = background("e5", false);
                java.awt.Color normalF3 = background("f3", false);
                java.awt.Color normalG3 = background("g3", false);
                square("e2", false).doClick();
                require(!background("e3", false).equals(normalE3), "al seleccionar el peon de e2 se debe marcar e3");
                require(background("e3", false).equals(background("e4", false)) && background("e5", false).equals(normalE5),
                    "al seleccionar el peon de e2 se marcan e3 y e4, y e5 queda igual");
                require(square("e3", false).isBorderPainted() && square("e4", false).isBorderPainted()
                        && !square("e5", false).isBorderPainted(),
                    "cada casilla marcada lleva su marco, para que las pegadas no se fundan");
                square("g1", false).doClick();
                require(background("e3", false).equals(normalE3) && !square("e3", false).isBorderPainted(),
                    "al cambiar de pieza se limpian los destinos anteriores y sus marcos");
                require(!background("f3", false).equals(normalF3) && background("g3", false).equals(normalG3),
                    "el caballo de g1 marca f3 y no marca g3");
                square("g1", false).doClick();
                require(background("f3", false).equals(normalF3), "clickear la misma pieza deselecciona y limpia");

                // Jugada, deshacer y volver al menu.
                square("e2", false).doClick();
                square("e4", false).doClick();
                require(game.getTurn() == nucleo.Color.BLACK, "despues de e2 e4 le toca a las negras");
                button(root, "Deshacer").doClick();
                require(before.equals(game.getBoard()), "deshacer en 1 contra 1 restaura el tablero");
                button(root, "Menú").doClick();

                // Contra la IA (medio) jugando con blancas.
                ((JComboBox<?>) named(root, "mode")).setSelectedIndex(1);
                require(named(root, "level").isEnabled() && named(root, "color").isEnabled(),
                    "contra la IA se eligen dificultad y color");
                ((JComboBox<?>) named(root, "level")).setSelectedIndex(1);
                require("IA".equals(black.getSelectedItem()) && !black.isEnabled(),
                    "contra la IA con blancas, el lado de las negras dice IA");
                button(root, "Jugar").doClick();
                square("e2", false).doClick();
                square("e4", false).doClick();
                require(!menuButton().isEnabled(), "el boton Menu debe bloquearse mientras piensa la IA");
            });
            awaitReply(nucleo.Color.WHITE);
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                require(menuButton().isEnabled(), "el boton Menu debe habilitarse al terminar la busqueda");
                after = game.getBoard().cloneBoard();
                require(!before.equals(after), "deben quedar jugadas la blanca y la negra");
                button(root, "Deshacer").doClick();
                require(before.equals(game.getBoard()), "deshacer contra IA debe restaurar el tablero inicial");
                button(root, "Rehacer").doClick();
                require(after.equals(game.getBoard()), "rehacer debe restaurar ambas jugadas");

                // Salir con la partida empezada pide confirmacion: se elige "Seguir jugando" y no se sale.
                clickLater("Seguir jugando");
                menuButton().doClick();
                require(button(root, "Deshacer").isShowing(), "si se elige Seguir jugando debe quedar la partida");
                // Y "Salir al menu" vuelve al menu.
                clickLater("Salir al menú");
                menuButton().doClick();
                require(button(root, "Jugar").isShowing(), "si se elige Salir al menu debe verse el menu");

                // Dificil, tambien con blancas.
                ((JComboBox<?>) named(root, "level")).setSelectedIndex(2);
                button(root, "Jugar").doClick();
                square("d2", false).doClick();
                square("d4", false).doClick();
                require(!menuButton().isEnabled(), "el nivel dificil tambien debe bloquear el boton Menu");
            });
            awaitReply(nucleo.Color.WHITE);
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                // Contra la IA jugando con negras: el tablero se gira y la IA abre la partida.
                clickLater("Salir al menú"); // la partida difícil sigue en juego: sale confirmando
                button(root, "Menú").doClick();
                require(button(root, "Jugar").isShowing(), "debe verse el menu despues de confirmar la salida");
                ((JComboBox<?>) named(root, "color")).setSelectedIndex(1);
                button(root, "Jugar").doClick();
                require(((JButton) boardPanel.getComponent(0)).getIcon() != null
                        && boardPanel.getComponent(0) == square("h1", true),
                    "con negras el tablero se gira: la primera casilla de la grilla es h1");
            });
            awaitReply(nucleo.Color.BLACK);
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                require(!before.equals(game.getBoard()), "la IA con blancas debe abrir la partida");
                button(root, "Deshacer").doClick();
            });
            awaitReply(nucleo.Color.BLACK);

            // Ranking dentro de la misma ventana.
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                button(root, "Ranking").doClick();
                require(rankingTable.getRowCount() == 1, "el ranking debe mostrar al usuario registrado");
                button(root, "Volver").doClick();
            });
            System.out.println("[OK]   Swing: menu, registro, 1 contra 1, IA en tres niveles y dos colores, tablero girado, deshacer, rehacer y ranking");
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (view != null) view.dispose(); });
        }
    }

    // Espera a que la IA termine de pensar y sea el turno de la persona (color indicado).
    private static void awaitReply(nucleo.Color humanTurn) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        boolean[] ready = {false};
        while (!ready[0] && System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> ready[0] = game.getTurn() == humanTurn && menuButton().isEnabled());
            if (!ready[0]) Thread.sleep(10);
        }
        require(ready[0], "la IA debe devolver el turno sin congelar la ventana");
    }

    // Dentro de un dialogo modal el hilo de la interfaz queda esperando: este temporizador
    // aprieta el boton con ese texto cuando aparece.
    private static void clickLater(String text) {
        javax.swing.Timer timer = new javax.swing.Timer(200, e -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isShowing()) {
                    JButton found = button(((JDialog) window).getContentPane(), text);
                    if (found != null) found.doClick();
                }
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static JLabel label(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel && text.equals(((JLabel) component).getText())) return (JLabel) component;
            if (component instanceof Container) {
                JLabel found = label((Container) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JButton menuButton() {
        return button(view.getContentPane(), "Menú");
    }

    private static void inspect(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel && ((JPanel) component).getLayout() instanceof GridLayout
                    && ((JPanel) component).getComponentCount() == 64) boardPanel = (JPanel) component;
            if (component instanceof JTable) rankingTable = (JTable) component;
            if (component instanceof Container) inspect((Container) component);
        }
    }

    private static Component named(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container) {
                Component found = named((Container) component, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    // El tablero se arma de nuevo en cada partida, asi que se vuelve a buscar. Con el tablero
    // girado (negras abajo) el orden de la grilla es el inverso.
    private static JButton square(String text, boolean flipped) {
        inspect(view.getContentPane());
        Position position = Position.fromAlgebraic(text);
        int index = flipped
            ? position.getRow() * 8 + (7 - position.getColumn())
            : (7 - position.getRow()) * 8 + position.getColumn();
        return (JButton) boardPanel.getComponent(index);
    }

    private static java.awt.Color background(String text, boolean flipped) {
        return square(text, flipped).getBackground();
    }

    private static JButton button(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) return (JButton) component;
            if (component instanceof Container) {
                JButton found = button((Container) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
