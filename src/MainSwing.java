import adaptadores.SwingChessView;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.ISpecialMove;
import nucleo.PieceFactory;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.PromotionMove;
import puertos.IGameObserver;

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
                new PromotionMove(pieces, view));
            ChessGame game = new ChessGame(board, Collections.<IGameObserver>singletonList(view), specialMoves);

            view.attachGame(game);
            view.render(board);
            view.setVisible(true);
        });
    }
}
