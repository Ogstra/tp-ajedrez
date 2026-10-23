package adaptadores;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

// Desplegable con el mismo alto, borde y colores que el resto del menú. El del sistema (Aqua)
// tiene un alto fijo más bajo y no respeta los colores.
public class FlatComboBox extends JComboBox<String> {

    static final int HEIGHT = 36;

    public FlatComboBox() {
        setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new Arrow();
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                // el fondo redondeado lo pinta el componente
            }
        });
        setOpaque(false);
        setBackground(Palette.CARD);
        setForeground(Palette.INK);
        setFont(new JLabel().getFont().deriveFont(14f));
        setBorder(new RoundedBorder());
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.setBorder(BorderFactory.createEmptyBorder(6, index < 0 ? 2 : 10, 6, 10));
                if (index < 0) {
                    label.setOpaque(false);           // el valor elegido, dentro del desplegable cerrado
                    label.setForeground(Palette.INK);
                } else {
                    label.setBackground(selected ? Palette.TINT : Palette.CARD);
                    label.setForeground(Palette.INK);
                }
                return label;
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Palette.CARD);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }

    // Flecha hacia abajo, sin fondo.
    private static class Arrow extends JButton {
        Arrow() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(26, HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Palette.MUTED);
            g2.setStroke(new java.awt.BasicStroke(1.8f));
            int cx = getWidth() / 2 - 2;
            int cy = getHeight() / 2;
            g2.drawLine(cx - 5, cy - 2, cx, cy + 3);
            g2.drawLine(cx, cy + 3, cx + 5, cy - 2);
            g2.dispose();
        }
    }
}
