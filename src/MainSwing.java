import adaptadores.FileAccountStore;
import adaptadores.GameBuilder;
import adaptadores.SwingChessView;
import nucleo.BoardSetup;
import nucleo.ChessGame;
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

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

// Composition root de la variante gráfica. Mismo esquema que Main.java: se crea el
// adaptador (SwingChessView en vez de ConsoleRenderer, y a la vez es quien elige la pieza
// de coronación y quien maneja al oponente) y se le da el GameBuilder que arma cada partida
// con sus dependencias por constructor. ChessGame, Board y todo nucleo/ no tienen ni un
// import de Swing.
public class MainSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Cuentas: los datos se guardan en data/usuarios.csv (se cambia con -Dajedrez.data=carpeta).
            Path dataDir = Path.of(System.getProperty("ajedrez.data", "data"));
            AccountService accounts = new AccountService(new FileAccountStore(dataDir.resolve("usuarios.csv")));

            // La ventana arma una partida nueva cada vez que se aprieta Jugar, con estas reglas.
            GameBuilder builder = (view, recorder) -> {
                PieceFactory pieces = PieceFactory.standard();
                List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
                    new CastlingMove(),
                    new EnPassantMove(),
                    new PromotionMove(pieces, view));
                List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
                    new InsufficientMaterialRule(),
                    new FiftyMoveRule(),
                    new ThreefoldRepetitionRule());
                return new ChessGame(BoardSetup.standard(pieces),
                    Arrays.<IMoveListener>asList(view, recorder),
                    Arrays.<IGameStatusListener>asList(view, recorder),
                    specialMoves,
                    drawRules);
            };

            new SwingChessView(accounts, builder).setVisible(true);
        });
    }
}
