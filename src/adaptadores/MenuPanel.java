package adaptadores;

import ai.OpponentLevel;
import nucleo.Color;
import usuarios.AccountService;
import usuarios.Score;
import usuarios.User;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

// Pantalla de inicio de la ventana: modo (1 contra 1 o contra la IA), dificultad, color, usuarios
// y registro. Solo junta las elecciones; arrancar la partida lo hace SwingChessView.
public class MenuPanel extends JPanel {

    private static final String NO_USER = "Sin usuario";

    private final AccountService accounts;
    private final Runnable onPlay;
    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{"1 contra 1", "Contra la IA"});
    private final JComboBox<String> levelBox = new JComboBox<>(new String[]{"Fácil", "Medio", "Difícil"});
    private final JComboBox<String> colorBox = new JComboBox<>(new String[]{"Blancas", "Negras"});
    private final JComboBox<String> whiteBox = new JComboBox<>();
    private final JComboBox<String> blackBox = new JComboBox<>();
    private final JTextField nameField = new JTextField(12);
    private final JLabel message = new JLabel(" ");
    private List<User> users = new ArrayList<>();

    public MenuPanel(AccountService accounts, Runnable onPlay, Runnable onRanking) {
        this.accounts = accounts;
        this.onPlay = onPlay;
        modeBox.setName("mode");
        levelBox.setName("level");
        nameField.setName("newUser");
        colorBox.setName("color");
        whiteBox.setName("white");
        blackBox.setName("black");

        JLabel title = new JLabel("Ajedrez");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));

        JButton registerButton = new JButton("Registrar");
        registerButton.addActionListener(e -> register());
        JButton playButton = new JButton("Jugar");
        playButton.setFont(playButton.getFont().deriveFont(Font.BOLD, 16f));
        playButton.addActionListener(e -> play());
        JButton rankingButton = new JButton("Ranking");
        rankingButton.addActionListener(e -> onRanking.run());
        modeBox.addActionListener(e -> updateEnabled());
        colorBox.addActionListener(e -> updateEnabled());

        // Usuario nuevo: el campo y su botón van juntos en una sola fila del formulario.
        JPanel newUser = new JPanel(new BorderLayout(6, 0));
        newUser.add(nameField, BorderLayout.CENTER);
        newUser.add(registerButton, BorderLayout.EAST);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.add(playButton);
        buttons.add(rankingButton);

        // Formulario centrado: etiquetas a la izquierda y controles alineados a la derecha.
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Modo", modeBox);
        addRow(form, 1, "Dificultad", levelBox);
        addRow(form, 2, "Juego con", colorBox);
        addRow(form, 3, "Blancas", whiteBox);
        addRow(form, 4, "Negras", blackBox);
        addRow(form, 5, "Usuario nuevo", newUser);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        addWide(form, 6, message, 14);
        addWide(form, 7, buttons, 6);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 24, 0);
        add(title, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        add(form, c);
        refresh();
    }

    // Vuelve a leer los usuarios (cambian los puntos y los registros) conservando lo elegido.
    public void refresh() {
        User white = userAt(whiteBox);
        User black = userAt(blackBox);
        users = new ArrayList<>();
        for (Score score : accounts.ranking()) users.add(score.getUser());
        fill(whiteBox, white);
        fill(blackBox, black);
        updateEnabled();
    }

    public boolean versusAi() { return modeBox.getSelectedIndex() == 1; }

    // Los niveles de OpponentLevel van después de NONE: Fácil es el 1.
    public OpponentLevel level() { return OpponentLevel.values()[levelBox.getSelectedIndex() + 1]; }

    public Color humanColor() {
        return versusAi() && colorBox.getSelectedIndex() == 1 ? Color.BLACK : Color.WHITE;
    }

    public Color aiColor() { return humanColor() == Color.WHITE ? Color.BLACK : Color.WHITE; }

    // El lado que juega la IA no tiene usuario.
    public User whiteUser() { return whiteBox.isEnabled() ? userAt(whiteBox) : null; }

    public User blackUser() { return blackBox.isEnabled() ? userAt(blackBox) : null; }

    private void play() {
        User white = whiteUser();
        if (white != null && white.equals(blackUser())) {
            message.setText("Elegí usuarios distintos para blancas y negras.");
            return;
        }
        message.setText(" ");
        onPlay.run();
    }

    private void register() {
        try {
            User user = accounts.register(nameField.getText());
            nameField.setText("");
            refresh();
            // El usuario nuevo queda elegido en el primer lado libre.
            int index = users.indexOf(user) + 1;
            if (whiteBox.isEnabled() && userAt(whiteBox) == null) whiteBox.setSelectedIndex(index);
            else if (blackBox.isEnabled() && userAt(blackBox) == null) blackBox.setSelectedIndex(index);
            message.setText("Registrado: " + user.getName() + ".");
        } catch (IllegalArgumentException e) {
            message.setText(e.getMessage());
        }
    }

    // Contra la IA solo se elige el usuario del lado propio.
    private void updateEnabled() {
        boolean ai = versusAi();
        levelBox.setEnabled(ai);
        colorBox.setEnabled(ai);
        whiteBox.setEnabled(!ai || humanColor() == Color.WHITE);
        blackBox.setEnabled(!ai || humanColor() == Color.BLACK);
    }

    private User userAt(JComboBox<String> box) {
        int index = box.getSelectedIndex();
        return index <= 0 || index > users.size() ? null : users.get(index - 1);
    }

    private void fill(JComboBox<String> box, User selected) {
        box.removeAllItems();
        box.addItem(NO_USER);
        for (User user : users) box.addItem(user.getName());
        box.setSelectedIndex(selected == null ? 0 : users.indexOf(selected) + 1);
    }

    private static void addRow(JPanel form, int y, String label, Component control) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.EAST;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        control.setPreferredSize(new Dimension(230, 28));
        form.add(control, c);
    }

    // Fila que ocupa las dos columnas (el mensaje y los botones).
    private static void addWide(JPanel form, int y, Component part, int top) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(top, 8, 0, 8);
        form.add(part, c);
    }
}
