package adaptadores;

import javax.swing.border.AbstractBorder;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

// Borde redondeado de los campos y desplegables: gris, y verde cuando tienen el foco.
class RoundedBorder extends AbstractBorder {

    private static final int RADIUS = 10;

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c.hasFocus() ? Palette.PRIMARY : Palette.CONTROL);
        g2.drawRoundRect(x, y, width - 1, height - 1, RADIUS, RADIUS);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(1, 10, 1, 6);
    }
}
