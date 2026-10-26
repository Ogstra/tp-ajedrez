package adaptadores;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

// Recuadro blanco con borde gris y esquinas redondeadas (jugadores del menú, tabla del ranking).
class RoundedPanel extends JPanel {

    RoundedPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Palette.CARD);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(Palette.CONTROL);
        RoundedBorder.outline(g2, 0, 0, getWidth(), getHeight(), 14);
        g2.dispose();
    }
}
