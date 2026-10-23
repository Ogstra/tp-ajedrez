package adaptadores;

import usuarios.Score;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

// Pantalla del ranking dentro de la misma ventana. Se llena con show() cada vez que se abre.
public class RankingPanel extends JPanel {

    private static final int VISIBLE_ROWS = 14; // con más usuarios aparece la barra de desplazamiento
    private static final String[] COLUMNS = {"#", "Usuario", "PJ", "G", "E", "P", "Pts"};

    private final JTable table = new JTable();
    private JScrollPane scroll;
    private final JLabel empty = new JLabel("Todavía no hay usuarios registrados. Se crean desde el menú.", SwingConstants.CENTER);

    public RankingPanel(Runnable onBack) {
        table.setEnabled(false);
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setGridColor(Palette.LINE);
        table.setForeground(Palette.INK);
        table.getTableHeader().setReorderingAllowed(false);
        // Números centrados; el nombre queda a la izquierda.
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        JLabel title = new JLabel("Ranking", SwingConstants.CENTER);
        title.setFont(Palette.title(30));
        title.setForeground(Palette.PRIMARY);
        JLabel legend = new JLabel("PJ jugadas, G ganadas, E empatadas, P perdidas, Pts puntos", SwingConstants.CENTER);
        legend.setForeground(Palette.MUTED);
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(title, BorderLayout.NORTH);
        header.add(legend, BorderLayout.CENTER);

        JButton back = new FlatButton("Volver", FlatButton.Style.SECONDARY);
        back.addActionListener(e -> onBack.run());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 18));
        bottom.add(back);

        setBorder(BorderFactory.createEmptyBorder(28, 60, 16, 60));
        setLayout(new BorderLayout(0, 14));
        setBackground(Palette.BACKGROUND);
        header.setOpaque(false);
        bottom.setOpaque(false);
        add(header, BorderLayout.NORTH);
        // Debajo de las filas se ve el fondo de la ventana, no un recuadro blanco vacío.
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Palette.CONTROL));
        scroll.getViewport().setBackground(Palette.BACKGROUND);
        empty.setForeground(Palette.MUTED);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(empty, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        JPanel holder = new JPanel(new BorderLayout());   // la tabla mide lo que sus filas, no todo el alto
        holder.setOpaque(false);
        holder.add(body, BorderLayout.NORTH);
        holder.add(bottom, BorderLayout.CENTER);   // Volver queda justo debajo de la tabla
        this.scroll = scroll;
        add(holder, BorderLayout.CENTER);

    }

    public void show(List<Score> ranking) {
        table.setModel(model(ranking));
        empty.setVisible(ranking.isEmpty());
        scroll.setVisible(!ranking.isEmpty());
        int rows = Math.min(VISIBLE_ROWS, ranking.size());
        table.setPreferredScrollableViewportSize(new java.awt.Dimension(400, rows * table.getRowHeight()));
        revalidate();
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
    }

    // Una fila por usuario, en el orden en que viene el ranking.
    static DefaultTableModel model(List<Score> ranking) {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0);
        int position = 1;
        for (Score score : ranking) {
            model.addRow(new Object[]{position++, score.getUser().getName(), score.getGames(),
                score.getWins(), score.getDraws(), score.getLosses(), score.getPoints()});
        }
        return model;
    }
}
