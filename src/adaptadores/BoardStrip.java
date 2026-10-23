package adaptadores;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;

// Ocho casillas alternadas, como las del tablero. Solo de adorno, debajo de los títulos.
class BoardStrip extends JComponent {

    private static final int SQUARE = 18;

    BoardStrip() {
        setPreferredSize(new Dimension(8 * SQUARE, SQUARE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        for (int i = 0; i < 8; i++) {
            g.setColor(i % 2 == 0 ? Palette.BOARD_DARK : Palette.BOARD_LIGHT);
            g.fillRect(i * SQUARE, 0, SQUARE, SQUARE);
        }
    }
}
