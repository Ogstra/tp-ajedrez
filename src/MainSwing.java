import adaptadores.FileAccountStore;
import adaptadores.GameBuilder;
import adaptadores.Match;
import adaptadores.SwingChessView;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.IChessGame;
import nucleo.IDrawRule;
import nucleo.ISpecialMove;
import nucleo.PieceFactory;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.EnPassantMove;
import nucleo.especiales.PromotionMove;
import nucleo.tablas.FiftyMoveRule;
import nucleo.tablas.InsufficientMaterialRule;
import nucleo.tablas.ThreefoldRepetitionRule;
import puertos.IGameStatusListener;
import puertos.IMoveListener;
import usuarios.AccountService;
import usuarios.ScoreRecorder;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Composition root de la variante gráfica. Mismo esquema que Main.java: se crea el
// adaptador (SwingChessView en vez de ConsoleRenderer, y a la vez es quien elige la pieza
// de coronación y quien maneja al oponente) y se le da el GameBuilder que arma cada partida
// con sus dependencias por constructor. ChessGame, Board y todo nucleo/ no tienen ni un
// import de Swing.
public class MainSwing {
    public static void main(String[] args) {
        // En macOS, el nombre que se ve en la barra de menú y en el Dock (si no, dice "MainSwing").
        // Tiene que ir antes de crear cualquier ventana; en Windows y Linux no hace nada.
        System.setProperty("apple.awt.application.name", "Ajedrez");
        SwingUtilities.invokeLater(() -> {
            // Cuentas: los datos se guardan en data/usuarios.csv (se cambia con -Dajedrez.data=carpeta).
            Path dataDir = Path.of(System.getProperty("ajedrez.data", "data"));
            AccountService accounts = new AccountService(new FileAccountStore(dataDir.resolve("usuarios.csv")));

            // La ventana pide una partida nueva cada vez que se aprieta Jugar: acá se arma con sus
            // reglas, el que acredita los puntos y, si corresponde, la IA.
            Random random = new Random();
            GameBuilder builder = (view, white, black, level, aiColor) -> {
                PieceFactory pieces = PieceFactory.standard();
                List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
                    new CastlingMove(),
                    new EnPassantMove(),
                    new PromotionMove(pieces, view));
                List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
                    new InsufficientMaterialRule(),
                    new FiftyMoveRule(),
                    new ThreefoldRepetitionRule());
                ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
                IChessGame game = new ChessGame(BoardSetup.standard(pieces),
                    Arrays.<IMoveListener>asList(view, recorder),
                    Arrays.<IGameStatusListener>asList(view, recorder),
                    specialMoves,
                    drawRules);
                recorder.attachGame(game);
                OpponentController opponent = level == OpponentLevel.NONE
                    ? null
                    : new OpponentController(game, Opponents.create(level, random), aiColor);
                return new Match(game, opponent);
            };

            new SwingChessView(accounts, builder).setVisible(true);
        });
    }
}
