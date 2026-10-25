package adaptadores;

import usuarios.Score;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;

// Pantalla del ranking dentro de la misma ventana, con el mismo estilo que el menú: título,
// casillas de adorno y la tabla en un recuadro blanco, con el puntero en verde.
// Se llena con show() cada vez que se abre.
public class RankingPanel extends JPanel {

    private static final int COLUMN_WIDTH = 440;
    private static final int ROW_HEIGHT = 40;
    private static final int VISIBLE_ROWS = 10;   // con más usuarios aparece la barra de desplazamiento
    private static final String[] COLUMNS = {"#", "Usuario", "PJ", "G", "E", "P", "Pts"};
    private static final int[] WIDTHS = {30, 0, 40, 40, 40, 40, 48};  // 0 = ocupa lo que sobra

    private final RowsPanel rows = new RowsPanel();
    private final JScrollPane scroll = new JScrollPane(rows);
    private final RoundedPanel card = new RoundedPanel();
    private final JLabel empty = new JLabel("Todavía no hay usuarios.", SwingConstants.CENTER);

    public RankingPanel(Runnable onBack) {
        JLabel title = new JLabel("Ranking", SwingConstants.CENTER);
        title.setFont(Palette.title(32));
        title.setForeground(Palette.PRIMARY);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        strip.setOpaque(false);
        strip.add(new BoardStrip());
        empty.setForeground(Palette.MUTED);

        // La tabla: encabezado fijo y las filas debajo, dentro del recuadro blanco.
        rows.setName("rankingRows");
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));
        card.add(row(COLUMNS, true, false, false), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        FlatButton back = new FlatButton("Volver", FlatButton.Style.SECONDARY);
        back.addActionListener(e -> onBack.run());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.setOpaque(false);
        bottom.add(back);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        put(content, Box.createHorizontalStrut(COLUMN_WIDTH), 0);
        put(content, title, 0);
        put(content, strip, 8);
        put(content, card, 20);
        put(content, empty, 16);
        put(content, bottom, 20);

        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(12, 0, 12, 0);
        add(content, c);
    }

    public void show(List<Score> ranking) {
        rows.removeAll();
        int position = 1;
        for (Score score : ranking) {
            String[] values = {String.valueOf(position), score.getUser().getName(), String.valueOf(score.getGames()),
                String.valueOf(score.getWins()), String.valueOf(score.getDraws()), String.valueOf(score.getLosses()),
                String.valueOf(score.getPoints())};
            put(rows, row(values, false, position > 1, position == 1), 0);
            position++;
        }
        card.setVisible(!ranking.isEmpty());
        empty.setVisible(ranking.isEmpty());
        int visible = Math.max(1, Math.min(VISIBLE_ROWS, ranking.size()));
        scroll.setPreferredSize(new Dimension(COLUMN_WIDTH, visible * ROW_HEIGHT));
        revalidate();
        repaint();
    }

    // Una fila: el encabezado en gris y negrita; los datos con una línea arriba, el nombre a la
    // izquierda, los números centrados y los puntos en negrita. El primero va resaltado.
    private static JPanel row(String[] values, boolean header, boolean divider, boolean leader) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(leader);
        row.setBackground(Palette.TINT);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(divider ? 1 : 0, 0, header ? 1 : 0, 0, header ? Palette.CONTROL : Palette.LINE),
            BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        row.setPreferredSize(new Dimension(COLUMN_WIDTH, header ? 32 : ROW_HEIGHT));
        for (int i = 0; i < values.length; i++) {
            JLabel cell = new JLabel(values[i], i == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
            Font font = cell.getFont().deriveFont(header ? 12f : 14f);
            boolean bold = header || i == values.length - 1;
            cell.setFont(bold ? font.deriveFont(Font.BOLD) : font);
            cell.setForeground(header || i == 0 ? Palette.MUTED : Palette.INK);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = WIDTHS[i] == 0 ? 1 : 0;
            if (WIDTHS[i] > 0) cell.setPreferredSize(new Dimension(WIDTHS[i], 20));
            row.add(cell, c);
        }
        return row;
    }

    private static void put(JPanel panel, Component part, int top) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(top, 0, 0, 0);
        panel.add(part, c);
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
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
