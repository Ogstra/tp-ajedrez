import nucleo.especiales.EnPassantMove;
import nucleo.IDrawRule;
import nucleo.tablas.FiftyMoveRule;
import nucleo.tablas.InsufficientMaterialRule;
import nucleo.tablas.ThreefoldRepetitionRule;
import adaptadores.SwingChessView;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.IChessGame;
import nucleo.ISpecialMove;
import nucleo.PieceFactory;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.PromotionMove;
import puertos.IGameStatusListener;
import puertos.IMoveListener;

import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Composition root de la variante gráfica. Mismo esquema que Main.java:
// se arma el tablero, se crea el adaptador (ahora SwingChessView en vez de
// ConsoleRenderer, y a la vez es quien elige la pieza de coronación), se
// inyecta en ChessGame por constructor. ChessGame, Board y todo nucleo/ no
// tienen ni un import de Swing.
public class MainSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
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

        IChessGame game = new ChessGame(board,
                Collections.<IMoveListener>singletonList(view),
                Collections.<IGameStatusListener>singletonList(view),
                specialMoves, drawRules);

            view.attachGame(game);
            view.render(board);
            view.setVisible(true);
        });
    }
}
