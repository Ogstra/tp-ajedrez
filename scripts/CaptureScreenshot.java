import adapters.*;
import core.*;
import ports.*;
import accounts.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;

// Genera la captura del README y de la presentación: una partida a medias entre dos usuarios,
// con el caballo elegido y sus destinos marcados. Uso: java --class-path out scripts/CaptureScreenshot.java docs/captura.png
public class CaptureScreenshot {
    static SwingChessView view;
    public static void main(String[] a) throws Exception {
        Path data = Files.createTempDirectory("cap");
        SwingUtilities.invokeAndWait(() -> {
            AccountService acc = new AccountService(new FileAccountStore(data.resolve("u.csv")));
            acc.register("Ana"); acc.register("Beto");
            GameBuilder b = (v, w, bl, level, aiColor) -> {
                PieceFactory p = PieceFactory.standard();
                ScoreRecorder r = new ScoreRecorder(acc, w, bl);
                IChessGame g = new ChessGame(BoardSetup.standard(p), Arrays.<IMoveListener>asList(v, r), Arrays.<IGameStatusListener>asList(v, r),
                    StandardRules.specialMoves(p, v),
                    StandardRules.drawRules());
                r.attachGame(g);
                return new Match(g, null);
            };
            view = new SwingChessView(acc, b);
            view.setVisible(true);
            ((JComboBox<?>) named(view.getContentPane(), "white")).setSelectedIndex(2);
            ((JComboBox<?>) named(view.getContentPane(), "black")).setSelectedIndex(3);
            btn("Jugar").doClick();
            String[] moves = {"e2", "e4", "e7", "e5", "g1", "f3", "b8", "c6", "f1", "c4", "g8", "f6"};
            for (int i = 0; i < moves.length; i++) sq(moves[i]).doClick();
            sq("f3").doClick();   // queda elegido el caballo, con sus destinos marcados
        });
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Container cp = view.getContentPane();
                BufferedImage img = new BufferedImage(cp.getWidth(), cp.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                cp.paint(g);
                g.dispose();
                javax.imageio.ImageIO.write(img, "png", new java.io.File(a[0]));
                System.out.println(cp.getWidth() + "x" + cp.getHeight());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.exit(0);
    }
    static JButton sq(String t) {
        Position p = Position.fromAlgebraic(t);
        JPanel board = (JPanel) named(view.getContentPane(), "board");
        return (JButton) board.getComponent((7 - p.getRow()) * 8 + p.getColumn());
    }
    static Component named(Container c, String n) {
        for (Component x : c.getComponents()) {
            if (n.equals(x.getName())) return x;
            if (x instanceof Container) { Component f = named((Container) x, n); if (f != null) return f; }
        }
        return null;
    }
    static JButton btn(String t) { return find(view.getContentPane(), t); }
    static JButton find(Container c, String t) {
        for (Component x : c.getComponents()) {
            if (x instanceof JButton && t.equals(((JButton) x).getText())) return (JButton) x;
            if (x instanceof Container) { JButton f = find((Container) x, t); if (f != null) return f; }
        }
        return null;
    }
}
