package adapters;

import javax.swing.border.AbstractBorder;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

// Borde redondeado de los campos y desplegables: gris, y verde cuando tienen el foco.
class RoundedBorder extends AbstractBorder {

    private static final int RADIUS = 10;

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c.hasFocus() ? Palette.PRIMARY : Palette.CONTROL);
        outline(g2, x, y, width, height, RADIUS);
        g2.dispose();
    }

    // Contorno de 1 px dentro del rectángulo dado. Se dibuja medio píxel adentro para que
    // con la pantalla escalada (por ejemplo al 125%) no se pierda ningún lado del borde.
    static void outline(Graphics2D g2, double x, double y, double width, double height, double arc) {
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1, height - 1, arc, arc));
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(1, 10, 1, 6);
    }
}
