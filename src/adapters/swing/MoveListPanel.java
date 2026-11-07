package adapters.swing;

import adapters.swing.components.Palette;
import adapters.swing.components.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;

// La lista de jugadas en un recuadro blanco, con el mismo estilo que el ranking: una fila por
// número de jugada, con la de las blancas y la de las negras. La jugada marcada (la última, o
// la que se está revisando) va resaltada en verde. Al revisar, un click en una jugada lleva a
// esa posición.
public class MoveListPanel extends RoundedPanel {

    private static final int ROW_HEIGHT = 26;
    private static final int NUMBER_WIDTH = 34;

    private final IntConsumer onMoveClicked; // recibe el índice de la jugada: 0 es la primera de las blancas
    private final RowsPanel rows = new RowsPanel();
    private final JScrollPane scroll = new JScrollPane(rows);
    private final CardLayout cards = new CardLayout();

    public MoveListPanel(IntConsumer onMoveClicked) {
        this.onMoveClicked = onMoveClicked;
        setName("moves");
        setLayout(cards);
        setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        JLabel empty = new JLabel("Todavía no hay jugadas.", SwingConstants.CENTER);
        empty.setForeground(Palette.MUTED);
        add(scroll, "list");
        add(empty, "empty");
    }

    // "marked" es el índice de la jugada resaltada, o -1 para ninguna.
    public void show(List<String> moves, int marked, boolean clickable) {
        rows.removeAll();
        JPanel markedRow = null;
        for (int i = 0; i < moves.size(); i += 2) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            row.setPreferredSize(new Dimension(0, ROW_HEIGHT));
            JLabel number = new JLabel((i / 2 + 1) + ".", SwingConstants.LEFT);
            number.setForeground(Palette.MUTED);
            number.setPreferredSize(new Dimension(NUMBER_WIDTH, 20));
            row.add(number, column(0, 0));
            row.add(cell(moves, i, marked, clickable), column(1, 1));
            row.add(cell(moves, i + 1, marked, clickable), column(2, 1));
            if (marked == i || marked == i + 1) markedRow = row;
            GridBagConstraints c = column(0, 1);
            c.gridy = i / 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            rows.add(row, c);
        }
        // Un relleno al final deja las filas arriba cuando son pocas.
        GridBagConstraints filler = column(0, 1);
        filler.gridy = moves.size();
        filler.weighty = 1;
        JPanel space = new JPanel();
        space.setOpaque(false);
        rows.add(space, filler);

        cards.show(this, moves.isEmpty() ? "empty" : "list");
        revalidate();
        repaint();
        // La jugada marcada siempre a la vista: la última mientras se juega, la revisada al revisar.
        final JPanel target = markedRow;
        if (target != null) {
            SwingUtilities.invokeLater(() -> rows.scrollRectToVisible(target.getBounds()));
        }
    }

    private JLabel cell(List<String> moves, int index, int marked, boolean clickable) {
        String text = index < moves.size() ? moves.get(index) : "";
        JLabel cell = new JLabel(text);
        cell.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        cell.setPreferredSize(new Dimension(1, 20)); // las dos columnas se reparten el ancho por igual
        cell.setForeground(Palette.INK);
        if (index == marked) {
            cell.setOpaque(true);
            cell.setBackground(Palette.TINT);
            cell.setForeground(Palette.PRIMARY);
            cell.setFont(cell.getFont().deriveFont(Font.BOLD));
        }
        if (clickable && !text.isEmpty()) {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onMoveClicked.accept(index); }
            });
        }
        return cell;
    }

    private static GridBagConstraints column(int x, double weight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.weightx = weight;
        c.fill = GridBagConstraints.BOTH;
        return c;
    }

    // Las filas ocupan todo el ancho del recuadro y se desplazan de a una fila.
    private static class RowsPanel extends JPanel implements Scrollable {
        RowsPanel() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return ROW_HEIGHT; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return ROW_HEIGHT * 5; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return getParent() != null && getParent().getHeight() > getPreferredSize().height; }
    }
}
