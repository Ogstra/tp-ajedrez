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

    private static final String[] COLUMNS = {"#", "Usuario", "PJ", "G", "E", "P", "Pts"};

    private final JTable table = new JTable();

    public RankingPanel(Runnable onBack) {
        table.setEnabled(false);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);
        // Números centrados; el nombre queda a la izquierda.
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        JLabel title = new JLabel("Ranking", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JLabel legend = new JLabel("PJ jugadas, G ganadas, E empatadas, P perdidas, Pts puntos", SwingConstants.CENTER);
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.add(title, BorderLayout.NORTH);
        header.add(legend, BorderLayout.CENTER);

        JButton back = new JButton("Volver");
        back.addActionListener(e -> onBack.run());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(back);

        setBorder(BorderFactory.createEmptyBorder(24, 40, 16, 40));
        setLayout(new BorderLayout(0, 14));
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void show(List<Score> ranking) {
        table.setModel(model(ranking));
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
