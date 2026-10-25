package adaptadores;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// Elegir una opción entre pocas. Se dibuja a mano porque con el aspecto del sistema Swing
// ignora el color de fondo de los botones. Dos estilos: segmentos dentro de una pista (modo y
// dificultad) o tarjetas separadas con ícono (color de las piezas). Se usa con el mouse o con
// las flechas izquierda y derecha.
public class ChoiceControl extends JComponent {

    private static final int PAD = 3;     // margen de la pista alrededor de los segmentos
    private static final int GAP = 10;    // separación entre tarjetas
    private static final int ICON = 34;

    private final String[] options;
    private final Image[] icons;          // null en el estilo de segmentos
    private final List<ActionListener> listeners = new ArrayList<>();
    private int selected = 0;
    private int hover = -1;

    public static ChoiceControl segments(String... options) {
        return new ChoiceControl(options, null, 42);
    }

    public static ChoiceControl cards(String[] options, Image[] icons) {
        return new ChoiceControl(options, icons, 56);
    }

    private ChoiceControl(String[] options, Image[] icons, int height) {
        this.options = options;
        this.icons = icons;
        setFont(new JLabel().getFont().deriveFont(14f));
        setPreferredSize(new Dimension(300, height));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                setSelectedIndex(indexAt(e.getX()));
            }
            @Override public void mouseMoved(MouseEvent e) { setHover(indexAt(e.getX())); }
            @Override public void mouseExited(MouseEvent e) { setHover(-1); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT && selected > 0) setSelectedIndex(selected - 1);
                if (e.getKeyCode() == KeyEvent.VK_RIGHT && selected < options.length - 1) setSelectedIndex(selected + 1);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    public int getSelectedIndex() { return selected; }

    public void setSelectedIndex(int index) {
        if (index == selected) return;
        selected = index;
        repaint();
        for (ActionListener listener : listeners) listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, options[index]));
    }

    public void addActionListener(ActionListener listener) { listeners.add(listener); }

    private void setHover(int index) {
        if (index == hover) return;
        hover = index;
        repaint();
    }

    private int indexAt(int x) {
        return Math.max(0, Math.min(options.length - 1, x * options.length / Math.max(1, getWidth())));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (icons == null) paintSegments(g2);
        else paintCards(g2);
        g2.dispose();
    }

    // Pista beige con borde; la opción elegida es un bloque verde con texto blanco.
    private void paintSegments(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Palette.TRACK);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
        g2.setColor(hasFocus() ? Palette.PRIMARY : Palette.CONTROL);
        RoundedBorder.outline(g2, 0, 0, w, h, 12);

        int segment = (w - 2 * PAD) / options.length;
        for (int i = 0; i < options.length; i++) {
            int x = PAD + i * segment;
            boolean on = i == selected;
            if (on || i == hover) {
                g2.setColor(on ? Palette.PRIMARY : Palette.TRACK_HOVER);
                g2.fillRoundRect(x, PAD, segment, h - 2 * PAD, 9, 9);
            }
            g2.setColor(on ? java.awt.Color.WHITE : Palette.INK);
            g2.setFont(on ? getFont().deriveFont(Font.BOLD) : getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(options[i], x + (segment - fm.stringWidth(options[i])) / 2,
                (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    // Tarjetas blancas; la elegida lleva borde verde de 2 px y fondo verde claro.
    private void paintCards(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        int card = (w - GAP * (options.length - 1)) / options.length;
        for (int i = 0; i < options.length; i++) {
            int x = i * (card + GAP);
            boolean on = i == selected;
            g2.setColor(on ? Palette.TINT : i == hover ? Palette.BACKGROUND : Palette.CARD);
            g2.fillRoundRect(x + 1, 1, card - 2, h - 2, 14, 14);
            g2.setStroke(new BasicStroke(on ? (hasFocus() ? 3f : 2f) : 1f));
            g2.setColor(on ? Palette.PRIMARY : Palette.CONTROL);
            g2.drawRoundRect(x + 1, 1, card - 2, h - 2, 14, 14);

            g2.setFont(on ? getFont().deriveFont(Font.BOLD) : getFont());
            FontMetrics fm = g2.getFontMetrics();
            int iconWidth = icons[i] != null ? ICON + 10 : 0;
            int start = x + (card - iconWidth - fm.stringWidth(options[i])) / 2;
            if (icons[i] != null) g2.drawImage(icons[i], start, (h - ICON) / 2, ICON, ICON, null);
            g2.setColor(Palette.INK);
            g2.drawString(options[i], start + iconWidth, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }
}
