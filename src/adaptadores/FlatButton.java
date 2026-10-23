package adaptadores;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

// Botón con los colores de la paleta, dibujado a mano por la misma razón que ChoiceControl.
// Sigue siendo un JButton: funciona con Enter, con el teclado y con doClick().
public class FlatButton extends JButton {

    public enum Style {
        PRIMARY,    // verde con texto blanco: la acción principal (Jugar)
        SECONDARY,  // beige con borde: acciones comunes
        LINK        // solo texto subrayado: acciones menores (Ver ranking)
    }

    private final Style style;

    public FlatButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Font base = new JLabel().getFont();
        setFont(style == Style.PRIMARY ? base.deriveFont(Font.BOLD, 17f) : base.deriveFont(14f));
    }

    @Override
    public Dimension getPreferredSize() {
        int width = getFontMetrics(getFont()).stringWidth(getText()) + 32;
        int height = style == Style.PRIMARY ? 52 : style == Style.SECONDARY ? 34 : 26;
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean active = isEnabled() && (getModel().isRollover() || getModel().isPressed());

        java.awt.Color text;
        if (style == Style.PRIMARY) {
            g2.setColor(!isEnabled() ? Palette.CONTROL : active ? Palette.PRIMARY_HOVER : Palette.PRIMARY);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            if (hasFocus()) {
                g2.setColor(Palette.BADGE_TEXT);
                g2.drawRoundRect(3, 3, w - 7, h - 7, 10, 10);
            }
            text = java.awt.Color.WHITE;
        } else if (style == Style.SECONDARY) {
            g2.setColor(!isEnabled() ? Palette.BACKGROUND : active ? Palette.TRACK_HOVER : Palette.TRACK);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g2.setColor(hasFocus() ? Palette.PRIMARY : Palette.CONTROL);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            text = isEnabled() ? Palette.INK : Palette.CONTROL;
        } else {
            text = active ? Palette.INK : Palette.MUTED;
        }

        g2.setFont(getFont());
        g2.setColor(text);
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(getText())) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), x, y);
        if (style == Style.LINK) g2.drawLine(x, y + 2, x + fm.stringWidth(getText()), y + 2);
        g2.dispose();
    }
}
