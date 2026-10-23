package adaptadores;

import usuarios.Score;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

// Pantalla del ranking dentro de la misma ventana. Se llena con show() cada vez que se abre.
public class RankingPanel extends JPanel {

    private static final String[] COLUMNS = {"#", "Usuario", "PJ", "G", "E", "P", "Pts"};

    private final JTable table = new JTable();

    public RankingPanel(Runnable onBack) {
        table.setEnabled(false);
        JLabel title = new JLabel("Ranking (PJ jugadas, G ganadas, E empatadas, P perdidas)", SwingConstants.CENTER);
        JButton back = new JButton("Volver");
        back.addActionListener(e -> onBack.run());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(back);

        setLayout(new BorderLayout(0, 8));
        add(title, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void show(List<Score> ranking) {
        table.setModel(model(ranking));
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
