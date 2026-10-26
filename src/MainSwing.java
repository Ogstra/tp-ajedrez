import adapters.FileAccountStore;
import adapters.FileSkinCatalog;
import adapters.FileSkinStore;
import adapters.GameBuilder;
import adapters.Match;
import adapters.SwingChessView;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import core.BoardSetup;
import core.ChessGame;
import core.IChessGame;
import core.IDrawRule;
import core.ISpecialMove;
import core.PieceFactory;
import core.special.CastlingMove;
import core.special.EnPassantMove;
import core.special.PromotionMove;
import core.draw.FiftyMoveRule;
import core.draw.InsufficientMaterialRule;
import core.draw.ThreefoldRepetitionRule;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;
import network.LanOpponent;
import skins.SkinService;
import accounts.AccountService;
import accounts.ScoreRecorder;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Composition root de la variante gráfica. Mismo esquema que Main.java: se crea el
// adaptador (SwingChessView en vez de ConsoleRenderer, y a la vez es quien elige la pieza
// de coronación y quien maneja al oponente) y se le da el GameBuilder que arma cada partida
// con sus dependencias por constructor. ChessGame, Board y todo core/ no tienen ni un
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
            // Skins: el catálogo está en src/assets/skins/skins.csv y la elegida por cada usuario en data/skins.csv.
            SkinService skins = new SkinService(FileSkinCatalog.load(Path.of("src", "assets", "skins", "skins.csv")),
                accounts, new FileSkinStore(dataDir.resolve("skins.csv")));

            // La ventana pide una partida nueva cada vez que se aprieta Jugar: acá se arma con sus
            // reglas, el que acredita los puntos y, si corresponde, la IA o el rival por LAN.
            Random random = new Random();
            GameBuilder builder = (view, white, black, level, aiColor, lan) -> {
                PieceFactory pieces = PieceFactory.standard();
                // Por LAN, las coronaciones del rival las decide el mensaje que llega por la red.
                IPromotionChooser chooser = lan != null ? lan : view;
                List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
                    new CastlingMove(),
                    new EnPassantMove(),
                    new PromotionMove(pieces, chooser));
                List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
                    new InsufficientMaterialRule(),
                    new FiftyMoveRule(),
                    new ThreefoldRepetitionRule());
                ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
                List<IMoveListener> moveListeners = new ArrayList<>(Arrays.<IMoveListener>asList(view, recorder));
                if (lan != null) moveListeners.add(lan); // manda las jugadas propias a la otra PC
                IChessGame game = new ChessGame(BoardSetup.standard(pieces),
                    moveListeners,
                    Arrays.<IGameStatusListener>asList(view, recorder),
                    specialMoves,
                    drawRules);
                recorder.attachGame(game);
                OpponentController opponent = null;
                if (lan != null) {
                    lan.attachGame(game);
                    opponent = new OpponentController(game, lan, lan.getRivalColor());
                } else if (level != OpponentLevel.NONE) {
                    opponent = new OpponentController(game, Opponents.create(level, random), aiColor);
                }
                return new Match(game, opponent);
            };

            new SwingChessView(accounts, skins, builder).setVisible(true);
        });
    }
}
