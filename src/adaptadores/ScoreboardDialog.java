package adaptadores;

import usuarios.Score;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

// Adaptador de salida (Clase 2): muestra el ranking en una tabla.
public final class ScoreboardDialog {

    private static final String[] COLUMNS = {"#", "Usuario", "PJ", "G", "E", "P", "Pts"};

    private ScoreboardDialog() { }

    public static void show(Component parent, List<Score> ranking) {
        if (ranking.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Todavía no hay usuarios registrados.", "Ranking", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JTable table = new JTable(model(ranking));
        table.setEnabled(false);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(520, Math.min(360, 60 + ranking.size() * 24)));
        JOptionPane.showMessageDialog(parent, scroll, "Ranking (PJ partidas, G ganadas, E empatadas, P perdidas)", JOptionPane.PLAIN_MESSAGE);
    }

    // Una fila por usuario, en el orden en que viene el ranking.
    static DefaultTableModel model(List<Score> ranking) {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0);
        int position = 1;
        for (Score score : ranking) {
            model.addRow(new Object[]{position++, score.getUser().getName(),
                score.getGames(), score.getWins(), score.getDraws(), score.getLosses(), score.getPoints()});
        }
        return model;
    }
}
