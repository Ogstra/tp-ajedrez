import adapters.swing.GameBuilder;
import adapters.swing.SwingChessView;
import ports.IGameStatusListener;
import ports.IMoveListener;
import core.*;
import core.board.*;
import core.command.*;
import core.draw.*;
import core.movement.*;
import core.special.*;
import core.state.*;
import ports.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

// Prueba de integración de la ventana real (partida, marcas, deshacer, rehacer y tamaño del
// tablero), sin reflection ni librerías de tests.
public class SwingIntegrationTests {
    private static SwingChessView view;
    private static IChessGame game;
    private static JPanel boardPanel;
    private static Board before;
    private static Board after;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                view = new SwingChessView(builder(8));
                view.setVisible(true);
                Container root = view.getContentPane();
                inspect(root);
                require(boardPanel.getComponentCount() == 64, "debe armarse el tablero de 64 casillas");
                require(label(root, "a") != null && label(root, "h") != null && label(root, "1") != null && label(root, "8") != null,
                    "el tablero muestra las coordenadas a-h y 1-8");

                // Selección y marcas de jugadas legales.
                java.awt.Color normalE3 = background("e3");
                java.awt.Color normalE5 = background("e5");
                java.awt.Color normalF3 = background("f3");
                java.awt.Color normalG3 = background("g3");
                square("e2").doClick();
                require(!background("e3").equals(normalE3), "al seleccionar el peon de e2 se debe marcar e3");
                require(background("e3").equals(background("e4")) && background("e5").equals(normalE5),
                    "al seleccionar el peon de e2 se marcan e3 y e4, y e5 queda igual");
                require(square("e3").isBorderPainted() && square("e4").isBorderPainted()
                        && !square("e5").isBorderPainted(),
                    "cada casilla marcada lleva su marco, para que las pegadas no se fundan");
                square("g1").doClick();
                require(background("e3").equals(normalE3) && !square("e3").isBorderPainted(),
                    "al cambiar de pieza se limpian los destinos anteriores y sus marcos");
                require(!background("f3").equals(normalF3) && background("g3").equals(normalG3),
                    "el caballo de g1 marca f3 y no marca g3");
                square("g1").doClick();
                require(background("f3").equals(normalF3), "clickear la misma pieza deselecciona y limpia");

                // Jugada, deshacer y rehacer.
                square("e2").doClick();
                square("e4").doClick();
                require(game.getTurn() == core.board.Color.BLACK, "despues de e2 e4 le toca a las negras");
                after = game.getBoard().cloneBoard();
                button(root, "Deshacer").doClick();
                require(before.equals(game.getBoard()), "deshacer restaura el tablero");
                button(root, "Rehacer").doClick();
                require(after.equals(game.getBoard()), "rehacer vuelve a hacer la jugada");

                // Empezar otra con la partida empezada pide confirmación: "Seguir jugando" la mantiene.
                IChessGame current = game;
                clickLater("Seguir jugando");
                button(root, "Nueva partida").doClick();
                require(game == current, "si se elige Seguir jugando debe quedar la misma partida");
                // Y "Empezar otra" arma una partida nueva desde el inicio.
                clickLater("Empezar otra");
                button(root, "Nueva partida").doClick();
                require(game != current && before.equals(game.getBoard()), "si se elige Empezar otra debe empezar de cero");
            });

            System.out.println("[OK]   Swing: partida, marcas, deshacer, rehacer y nueva partida con confirmacion");

            // Otro tamaño de tablero: la ventana se adapta sola.
            SwingUtilities.invokeAndWait(() -> {
                view.dispose();
                view = new SwingChessView(builder(10));
                view.setVisible(true);
                Container root = view.getContentPane();
                inspect(root);
                require(boardPanel.getComponentCount() == 100, "un tablero de 10x10 debe tener 100 casillas");
                JButton corner = (JButton) boardPanel.getComponent(0);
                require(corner.getWidth() == corner.getHeight() && corner.getWidth() > 0,
                    "las casillas deben ser cuadradas: " + corner.getWidth() + "x" + corner.getHeight());
                require(label(root, "j") != null && label(root, "10") != null, "las coordenadas llegan hasta j y 10");
                require(view.getContentPane().getHeight() <= Toolkit.getDefaultToolkit().getScreenSize().height,
                    "la ventana debe entrar en la pantalla");
            });
            System.out.println("[OK]   Swing: tablero de 10x10 con casillas cuadradas y coordenadas hasta j y 10");

            // Al agrandar la ventana, las casillas crecen (y siguen cuadradas).
            int[] sizes = new int[2];
            SwingUtilities.invokeAndWait(() -> {
                view.dispose();
                view = new SwingChessView(builder(8));
                view.setVisible(true);
                inspect(view.getContentPane());
                sizes[0] = boardPanel.getComponent(0).getWidth();
                view.setSize(view.getWidth() + 320, view.getHeight() + 320);
                view.validate();
            });
            Thread.sleep(600); // las piezas se rehacen cuando se deja de cambiar el tamaño
            SwingUtilities.invokeAndWait(() -> {
                JButton corner = (JButton) boardPanel.getComponent(0);
                sizes[1] = corner.getWidth();
                require(sizes[1] > sizes[0] && corner.getWidth() == corner.getHeight(),
                    "al agrandar la ventana las casillas deben crecer y seguir cuadradas: " + sizes[0] + " -> " + sizes[1]);
            });
            System.out.println("[OK]   Swing: el tablero crece con la ventana (" + sizes[0] + " -> " + sizes[1] + " px)");
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (view != null) view.dispose(); });
        }
    }

    // Dentro de un diálogo modal el hilo de la interfaz queda esperando: este temporizador
    // aprieta el botón con ese texto cuando aparece.
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

    // El mismo armado que MainSwing, con un tablero de 8x8 o de otro tamaño.
    private static GameBuilder builder(int size) {
        return v -> {
            PieceFactory pieces = PieceFactory.standard();
            Board board = size == 8 ? BoardSetup.standard(pieces) : BoardSetup.custom(size, size, backRow(size), pieces);
            before = board.cloneBoard();
            game = new ChessGame(board, Collections.<IMoveListener>singletonList(v),
                Collections.<IGameStatusListener>singletonList(v),
                StandardRules.specialMoves(pieces, v),
                StandardRules.drawRules());
            return game;
        };
    }

    // Fila trasera de cualquier ancho: la estándar y, si sobran columnas, torres.
    private static PieceType[] backRow(int size) {
        PieceType[] standard = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};
        PieceType[] row = new PieceType[size];
        for (int i = 0; i < size; i++) row[i] = i < standard.length ? standard[i] : PieceType.ROOK;
        return row;
    }

    private static void inspect(Container container) {
        for (Component component : container.getComponents()) {
            if ("board".equals(component.getName())) boardPanel = (JPanel) component;
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

    // El tablero se arma de nuevo en cada partida, así que se vuelve a buscar.
    private static JButton square(String text) {
        inspect(view.getContentPane());
        Position position = Position.fromAlgebraic(text);
        return (JButton) boardPanel.getComponent((7 - position.getRow()) * 8 + position.getColumn());
    }

    private static java.awt.Color background(String text) {
        return square(text).getBackground();
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
