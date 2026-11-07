import adapters.storage.FileAccountStore;
import adapters.swing.GameBuilder;
import adapters.swing.Match;
import adapters.swing.SwingChessView;
import core.board.BoardSetup;
import core.board.PieceFactory;
import ai.AutoPromotionChooser;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import core.ChessGame;
import core.IChessGame;
import core.StandardRules;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;
import accounts.AccountService;
import accounts.ScoreRecorder;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

// Composition root de la versión con ventana. Igual que Main, pero el adaptador es
// SwingChessView, que también elige la pieza de coronación. El GameBuilder arma cada
// partida con sus dependencias por constructor.
public class MainSwing {
    public static void main(String[] args) {
        // En macOS, el nombre que se ve en la barra de menú y en el Dock (si no, dice "MainSwing").
        // Tiene que ir antes de crear cualquier ventana; en Windows y Linux no hace nada.
        System.setProperty("apple.awt.application.name", "Ajedrez");
        SwingUtilities.invokeLater(() -> {
            // Cuentas: los datos se guardan en data/usuarios.csv (se cambia con -Dajedrez.data=carpeta).
            Path dataDir = Path.of(System.getProperty("ajedrez.data", "data"));
            AccountService accounts = new AccountService(new FileAccountStore(dataDir.resolve("usuarios.csv")));

            // Cada vez que se aprieta Jugar se arma una partida con sus reglas, el que
            // acredita los puntos y, si corresponde, la IA.
            Random random = new Random();
            GameBuilder builder = (view, white, black, level, aiColor) -> {
                PieceFactory pieces = PieceFactory.standard();
                // Contra la IA, la pieza a la que corona ella la elige el Decorator (siempre reina),
                // igual que en la consola; la de la persona la pregunta la ventana.
                IPromotionChooser chooser = level == OpponentLevel.NONE ? view : new AutoPromotionChooser(aiColor, view);
                ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
                IChessGame game = new ChessGame(BoardSetup.standard(pieces),
                    Arrays.<IMoveListener>asList(view, recorder),
                    Arrays.<IGameStatusListener>asList(view, recorder),
                    StandardRules.specialMoves(pieces, chooser),
                    StandardRules.drawRules());
                OpponentController opponent = level == OpponentLevel.NONE
                    ? null
                    : new OpponentController(game, Opponents.create(level, random), aiColor);
                return new Match(game, opponent);
            };

            // La pista la da el nivel difícil, para el bando que tenga el turno.
            new SwingChessView(accounts, builder, Opponents.create(OpponentLevel.HARD, random)).setVisible(true);
        });
    }
}
