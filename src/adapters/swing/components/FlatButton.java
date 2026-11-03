package adapters.swing.components;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

// Botón con los colores de la paleta, dibujado a mano porque con el aspecto del sistema Swing
// ignora el fondo de los botones. Sigue siendo un JButton: funciona con el teclado y con doClick().
public class FlatButton extends JButton {

    private static final int HEIGHT = 36;
    private static final int ARC = 10;

    public FlatButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(new JLabel().getFont().deriveFont(14f));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getFontMetrics(getFont()).stringWidth(getText()) + 32, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int w = getWidth();
        int h = getHeight();
        boolean active = isEnabled() && (getModel().isRollover() || getModel().isPressed());

        g2.setColor(!isEnabled() ? Palette.BACKGROUND : active ? Palette.TRACK_HOVER : Palette.TRACK);
        g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        // El borde se dibuja medio píxel adentro para que con la pantalla escalada (por ejemplo
        // al 125%) no se pierda ningún lado.
        g2.setColor(hasFocus() ? Palette.PRIMARY : Palette.CONTROL);
        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, ARC, ARC));

        g2.setFont(getFont());
        g2.setColor(isEnabled() ? Palette.INK : Palette.CONTROL);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
