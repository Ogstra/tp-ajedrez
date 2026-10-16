import adaptadores.SwingChessView;
import nucleo.*;
import nucleo.especiales.*;
import nucleo.tablas.*;
import puertos.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

// Prueba de integracion de la ventana real, sin reflection ni librerias de tests.
public class SwingIntegrationTests {
    private static SwingChessView view;
    private static IChessGame game;
    private static JPanel boardPanel;
    private static JComboBox<?> levelBox;
    private static Board before;
    private static Board after;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                PieceFactory pieces = PieceFactory.standard();
                Board board = BoardSetup.standard(pieces);
                before = board.cloneBoard();
                view = new SwingChessView(8, 8);
                game = new ChessGame(board, Collections.<IMoveListener>singletonList(view),
                    Collections.<IGameStatusListener>singletonList(view),
                    Arrays.<ISpecialMove>asList(new CastlingMove(), new EnPassantMove(), new PromotionMove(pieces, view)),
                    Arrays.<IDrawRule>asList(new InsufficientMaterialRule(), new FiftyMoveRule(), new ThreefoldRepetitionRule()));
                view.attachGame(game);
                view.render(board);
                inspect(view.getContentPane());
                view.setVisible(true);
                square("e2").doClick();
                square("e4").doClick();
                button(view.getContentPane(), "Deshacer").doClick();
                levelBox.setSelectedIndex(2);
                button(view.getContentPane(), "Rehacer").doClick();
                require(!levelBox.isEnabled(), "el selector debe bloquearse mientras piensa la IA");
            });
            awaitReply();
            SwingUtilities.invokeAndWait(() -> {
                require(levelBox.isEnabled(), "el selector debe habilitarse al terminar la busqueda");
                after = game.getBoard().cloneBoard();
                require(!before.equals(after), "deben quedar jugadas la blanca y la negra");
                button(view.getContentPane(), "Deshacer").doClick();
                require(before.equals(game.getBoard()), "deshacer contra IA debe restaurar el tablero inicial");
                button(view.getContentPane(), "Rehacer").doClick();
                require(after.equals(game.getBoard()), "rehacer debe restaurar ambas jugadas");
                levelBox.setSelectedIndex(3);
                square("d2").doClick();
                square("d4").doClick();
                require(!levelBox.isEnabled(), "el nivel dificil tambien debe bloquear el selector");
            });
            awaitReply();
            System.out.println("[OK]   Swing: niveles medio y dificil, respuesta, deshacer, rehacer y selector");
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (view != null) view.dispose(); });
        }
    }

    private static void awaitReply() throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        boolean[] ready = {false};
        while (!ready[0] && System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> ready[0] = game.getTurn() == nucleo.Color.WHITE && levelBox.isEnabled());
            if (!ready[0]) Thread.sleep(10);
        }
        require(ready[0], "la IA debe devolver el turno sin congelar la ventana");
    }

    private static void inspect(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox<?>) levelBox = (JComboBox<?>) component;
            if (component instanceof JPanel && ((JPanel) component).getLayout() instanceof GridLayout) boardPanel = (JPanel) component;
            if (component instanceof Container) inspect((Container) component);
        }
    }

    private static JButton square(String text) {
        Position position = Position.fromAlgebraic(text);
        return (JButton) boardPanel.getComponent((7 - position.getRow()) * 8 + position.getColumn());
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
