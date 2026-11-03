import adapters.swing.GameBuilder;
import adapters.swing.SwingChessView;
import core.board.BoardSetup;
import core.board.PieceFactory;
import core.ChessGame;
import core.StandardRules;
import ports.IGameStatusListener;
import ports.IMoveListener;

import javax.swing.SwingUtilities;
import java.util.Collections;

// Composition root de la variante gráfica. Mismo esquema que Main.java: se crea el
// adaptador (SwingChessView en vez de ConsoleRenderer, y a la vez es quien elige la pieza
// de coronación) y se le da el GameBuilder que arma cada partida
// con sus dependencias por constructor. ChessGame, Board y todo core/ no tienen ni un
// import de Swing.
public class MainSwing {
    public static void main(String[] args) {
        // En macOS, el nombre que se ve en la barra de menú y en el Dock (si no, dice "MainSwing").
        // Tiene que ir antes de crear cualquier ventana; en Windows y Linux no hace nada.
        System.setProperty("apple.awt.application.name", "Ajedrez");
        SwingUtilities.invokeLater(() -> {
            // La ventana pide una partida nueva cada vez que empieza una: acá se arma con sus reglas.
            GameBuilder builder = view -> {
                PieceFactory pieces = PieceFactory.standard();
                return new ChessGame(BoardSetup.standard(pieces),
                    Collections.<IMoveListener>singletonList(view),
                    Collections.<IGameStatusListener>singletonList(view),
                    StandardRules.specialMoves(pieces, view),
                    StandardRules.drawRules());
            };

            new SwingChessView(builder).setVisible(true);
        });
    }
}
