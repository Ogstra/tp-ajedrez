import adaptadores.FileAccountStore;
import adaptadores.LoginDialog;
import adaptadores.SwingChessView;
import nucleo.Board;
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
import usuarios.AccountService;
import usuarios.ScoreRecorder;
import usuarios.User;
import puertos.IMoveListener;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Composition root de la variante gráfica. Mismo esquema que Main.java:
// se arma el tablero, se crea el adaptador (SwingChessView en vez de
// ConsoleRenderer, y a la vez es quien elige la pieza de coronación y quien
// maneja al oponente) y se inyecta en ChessGame por constructor. ChessGame,
// Board y todo nucleo/ no tienen ni un import de Swing.
public class MainSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Cuentas: los datos se guardan en data/usuarios.csv (se cambia con -Dajedrez.data=carpeta).
            Path dataDir = Path.of(System.getProperty("ajedrez.data", "data"));
            AccountService accounts = new AccountService(new FileAccountStore(dataDir.resolve("usuarios.csv")));
            // Los botones de los diálogos salen en español aunque el sistema esté en otro idioma.
            UIManager.put("OptionPane.okButtonText", "Aceptar");
            UIManager.put("OptionPane.cancelButtonText", "Cancelar");
            User white = LoginDialog.show(null, accounts, "blancas", "");
            User black = LoginDialog.show(null, accounts, "negras",
                "Si vas a jugar contra la IA, elegí Jugar sin usuario.");

            PieceFactory pieces = PieceFactory.standard();
            Board board = BoardSetup.standard(pieces);
            SwingChessView view = new SwingChessView(board.getRows(), board.getColumns());

            List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
                new CastlingMove(),
                new EnPassantMove(),
                new PromotionMove(pieces, view));
            List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
                new InsufficientMaterialRule(),
                new FiftyMoveRule(),
                new ThreefoldRepetitionRule());

            ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
            IChessGame game = new ChessGame(board,
                Arrays.<IMoveListener>asList(view, recorder),
                Arrays.<IGameStatusListener>asList(view, recorder),
                specialMoves,
                drawRules);
            recorder.attachGame(game);

            view.attachGame(game);
            view.attachAccounts(accounts, recorder, white, black);
            view.render(board);
            view.setVisible(true);
        });
    }
}
