package adapters;

import ai.OpponentLevel;
import core.Color;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

// Menú de la ventana, después del login: modo (1 contra 1, contra la IA), dificultad y color.
// Solo junta las elecciones; arrancar la partida lo hace SwingChessView.
public class MenuPanel extends JPanel {

    private static final int COLUMN_WIDTH = 360;
    private static final String[] LEVEL_HELP = {
        "Juega jugadas al azar.", "Prioriza las capturas.", "Piensa dos jugadas por delante."};

    private final ChoiceControl modeControl = ChoiceControl.segments("1 contra 1", "Contra la IA");
    private final ChoiceControl levelControl = ChoiceControl.segments("Fácil", "Medio", "Difícil");
    private final ChoiceControl colorControl = ChoiceControl.cards(new String[]{"Blancas", "Negras"},
        new Image[]{SwingChessView.pieceImage("wK"), SwingChessView.pieceImage("bK")});
    private final JPanel aiSection = column();
    private final JLabel levelHelp = small(LEVEL_HELP[0]);
    private final FlatButton playButton = new FlatButton("Jugar", FlatButton.Style.PRIMARY);

    public MenuPanel(Runnable onPlay, Runnable onRanking, Runnable onSkins, Runnable onLogout) {
        modeControl.setName("mode");
        levelControl.setName("level");
        colorControl.setName("color");

        modeControl.addActionListener(e -> update());
        levelControl.addActionListener(e -> levelHelp.setText(LEVEL_HELP[levelControl.getSelectedIndex()]));
        playButton.addActionListener(e -> onPlay.run());

        // Encabezado: el título y una fila de casillas como las del tablero.
        JLabel title = new JLabel("Ajedrez", SwingConstants.CENTER);
        title.setFont(Palette.title(36));
        title.setForeground(Palette.PRIMARY);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        strip.setOpaque(false);
        strip.add(new BoardStrip());

        // Contra la IA: dificultad (con una línea que explica el nivel) y color propio.
        put(aiSection, label("Dificultad"), 14);
        put(aiSection, levelControl, 6);
        put(aiSection, levelHelp, 5);
        put(aiSection, label("Jugás con"), 14);
        put(aiSection, colorControl, 6);

        FlatButton rankingButton = new FlatButton("Ver ranking", FlatButton.Style.LINK);
        rankingButton.addActionListener(e -> onRanking.run());
        FlatButton skinsButton = new FlatButton("Skins", FlatButton.Style.LINK);
        skinsButton.addActionListener(e -> onSkins.run());
        FlatButton logoutButton = new FlatButton("Cerrar sesión", FlatButton.Style.LINK);
        logoutButton.addActionListener(e -> onLogout.run());

        JPanel content = column();
        put(content, title, 0);
        put(content, strip, 8);
        put(content, label("Modo"), 20);
        put(content, modeControl, 6);
        put(content, aiSection, 0);
        put(content, playButton, 20);
        put(content, rankingButton, 8);
        put(content, skinsButton, 4);
        put(content, logoutButton, 4);

        // La columna queda centrada en la ventana, a lo ancho y a lo alto.
        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 0, 12, 0);
        add(content, c);
        update();
    }

    // Para que la ventana lo use como botón por defecto (Enter) mientras se ve el menú.
    public JButton getPlayButton() { return playButton; }

    public boolean versusAi() { return modeControl.getSelectedIndex() == 1; }

    // Los niveles de OpponentLevel van después de NONE: Fácil es el 1.
    public OpponentLevel level() { return OpponentLevel.values()[levelControl.getSelectedIndex() + 1]; }

    public Color humanColor() {
        return versusAi() && colorControl.getSelectedIndex() == 1 ? Color.BLACK : Color.WHITE;
    }

    public Color aiColor() { return humanColor() == Color.WHITE ? Color.BLACK : Color.WHITE; }

    // Contra la IA aparecen la dificultad y el color.
    private void update() {
        aiSection.setVisible(versusAi());
        revalidate();
        repaint();
    }

    // Una columna donde cada componente ocupa todo el ancho, uno debajo del otro.
    // column(), put(), label() y small() son package-private porque también los usa LoginPanel.
    static JPanel column() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        put(panel, Box.createHorizontalStrut(COLUMN_WIDTH), 0);
        return panel;
    }

    static void put(JPanel panel, Component part, int top) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(top, 0, 0, 0);
        part.setMinimumSize(part.getPreferredSize()); // si no entra, se corta abajo en vez de aplastar los controles
        panel.add(part, c);
    }

    static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Palette.MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    static JLabel small(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Palette.MUTED);
        label.setFont(label.getFont().deriveFont(12.5f));
        return label;
    }
}
