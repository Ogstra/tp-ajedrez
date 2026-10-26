import accounts.AccountService;
import accounts.ScoreRecorder;
import accounts.User;
import adapters.FileAccountStore;
import adapters.FileSkinStore;
import adapters.GameBuilder;
import adapters.IGameView;
import adapters.Match;
import adapters.SkinCatalogReader;
import adapters.SwingChessView;
import ai.AutoPromotionChooser;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import core.BoardSetup;
import core.ChessGame;
import core.Color;
import core.IChessGame;
import core.IDrawRule;
import core.ISpecialMove;
import core.PieceFactory;
import core.StandardRules;
import network.LanConnection;
import network.LanLobby;
import network.LanOpponent;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;
import skins.SkinService;

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
            // Skins: el catálogo viene con el juego (assets/skins/skins.csv) y la elegida por cada
            // usuario se guarda en data/skins.csv.
            SkinService skins = new SkinService(SkinCatalogReader.bundled(),
                accounts, new FileSkinStore(dataDir.resolve("skins.csv")));

            new SwingChessView(accounts, skins, new StandardGames(accounts, new Random()), new LanLobby())
                .setVisible(true);
        });
    }

    // La ventana pide una partida nueva cada vez que empieza una: acá se arma con sus reglas, el
    // que acredita los puntos y, si corresponde, la IA o el rival por LAN.
    private static final class StandardGames implements GameBuilder {
        private final AccountService accounts;
        private final Random random;

        StandardGames(AccountService accounts, Random random) {
            this.accounts = accounts;
            this.random = random;
        }

        @Override
        public Match local(IGameView view, User whiteUser, User blackUser, OpponentLevel level, Color aiColor) {
            ScoreRecorder recorder = new ScoreRecorder(accounts, whiteUser, blackUser);
            // Contra la IA, la coronación de la IA es siempre reina (Decorator, igual que en la consola).
            IPromotionChooser chooser = level == OpponentLevel.NONE ? view : new AutoPromotionChooser(aiColor, view);
            IChessGame game = newGame(view, recorder, chooser, null);
            OpponentController opponent = level == OpponentLevel.NONE
                ? null : new OpponentController(game, Opponents.create(level, random), aiColor);
            return new Match(game, opponent);
        }

        @Override
        public Match overLan(IGameView view, User localUser, Color localColor, LanConnection connection, String rivalName) {
            LanOpponent lan = new LanOpponent(connection, localColor, rivalName, view);
            ScoreRecorder recorder = new ScoreRecorder(accounts,
                localColor == Color.WHITE ? localUser : null, localColor == Color.BLACK ? localUser : null);
            // Las coronaciones del rival las decide el mensaje que llega por la red; las propias, la ventana.
            IChessGame game = newGame(view, recorder, lan, lan);
            return new Match(game, new OpponentController(game, lan, lan.getRivalColor()), lan);
        }

        // La partida estándar: tablero de 8x8, todas las reglas especiales y las de tablas.
        // extraListener es el rival por LAN, que manda las jugadas propias a la otra PC (o null).
        private IChessGame newGame(IGameView view, ScoreRecorder recorder, IPromotionChooser chooser,
                                   IMoveListener extraListener) {
            PieceFactory pieces = PieceFactory.standard();
            List<ISpecialMove> specialMoves = StandardRules.specialMoves(pieces, chooser);
            List<IDrawRule> drawRules = StandardRules.drawRules();
            List<IMoveListener> moveListeners = new ArrayList<>(Arrays.<IMoveListener>asList(view, recorder));
            if (extraListener != null) moveListeners.add(extraListener);
            IChessGame game = new ChessGame(BoardSetup.standard(pieces),
                moveListeners,
                Arrays.<IGameStatusListener>asList(view, recorder),
                specialMoves,
                drawRules);
            return game;
        }
    }
}
