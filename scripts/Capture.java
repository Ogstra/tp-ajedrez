import adapters.SwingChessView;
import core.Board;
import core.BoardSetup;
import core.ChessGame;
import core.IChessGame;
import core.IDrawRule;
import core.ISpecialMove;
import core.PieceFactory;
import core.Position;
import core.special.CastlingMove;
import core.special.EnPassantMove;
import core.special.PromotionMove;
import ports.IGameStatusListener;
import ports.IMoveListener;

import javax.imageio.ImageIO;
import javax.swing.JRootPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Dibuja la ventana real fuera de pantalla, con una partida a medias, y la guarda como PNG.
// Uso (desde la raíz del repo): java --class-path out scripts/Capture.java docs/captura.png
public class Capture {
    public static void main(String[] args) throws Exception {
        PieceFactory pieces = PieceFactory.standard();
        Board board = BoardSetup.standard(pieces);
        SwingChessView view = new SwingChessView(board.getRows(), board.getColumns());
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(), new EnPassantMove(), new PromotionMove(pieces, view));
        IChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(view),
            Collections.<IGameStatusListener>singletonList(view),
            specialMoves,
            new ArrayList<IDrawRule>());
        view.attachGame(game);

        String[][] moves = {{"e2", "e4"}, {"e7", "e5"}, {"g1", "f3"}, {"b8", "c6"}, {"f1", "c4"}, {"g8", "f6"}};
        for (String[] move : moves) {
            game.tryMove(Position.fromAlgebraic(move[0]), Position.fromAlgebraic(move[1]));
        }
        view.render(board);

        view.addNotify();
        view.setSize(640, 750);
        view.validate();
        layout(view.getRootPane());
        JRootPane root = view.getRootPane();
        BufferedImage image = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        root.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", new File(args[0]));
        System.exit(0);
    }

    private static void layout(Component component) {
        if (component instanceof Container) {
            ((Container) component).doLayout();
            for (Component child : ((Container) component).getComponents()) layout(child);
        }
    }
}
