import adapters.swing.GameBuilder;
import adapters.swing.Match;
import adapters.swing.SwingChessView;
import ai.AutoPromotionChooser;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import core.board.Color;
import core.board.BoardSetup;
import core.board.PieceFactory;
import core.ChessGame;
import core.IChessGame;
import core.StandardRules;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;

import javax.swing.SwingUtilities;
import java.util.Collections;
import java.util.Random;

// Composition root de la versión con ventana. Igual que Main, pero el adaptador es
// SwingChessView, que también elige la pieza de coronación. El GameBuilder arma cada
// partida con sus dependencias por constructor y, si se elige, la IA con negras.
public class MainSwing {
    public static void main(String[] args) {
        // En macOS, el nombre que se ve en la barra de menú y en el Dock (si no, dice "MainSwing").
        // Tiene que ir antes de crear cualquier ventana; en Windows y Linux no hace nada.
        System.setProperty("apple.awt.application.name", "Ajedrez");
        SwingUtilities.invokeLater(() -> {
            // La ventana pide una partida nueva cada vez que empieza una.
            Random random = new Random();
            GameBuilder builder = (view, level) -> {
                PieceFactory pieces = PieceFactory.standard();
                // Contra la IA, ella corona siempre a reina; a la persona le pregunta la ventana.
                IPromotionChooser chooser = level == OpponentLevel.NONE ? view : new AutoPromotionChooser(Color.BLACK, view);
                IChessGame game = new ChessGame(BoardSetup.standard(pieces),
                    Collections.<IMoveListener>singletonList(view),
                    Collections.<IGameStatusListener>singletonList(view),
                    StandardRules.specialMoves(pieces, chooser),
                    StandardRules.drawRules());
                OpponentController opponent = level == OpponentLevel.NONE
                    ? null
                    : new OpponentController(game, Opponents.create(level, random), Color.BLACK);
                return new Match(game, opponent);
            };

            new SwingChessView(builder).setVisible(true);
        });
    }
}
