package adaptadores;

import ai.OpponentLevel;
import nucleo.Color;
import usuarios.AccountService;
import usuarios.Score;
import usuarios.User;

import javax.swing.BorderFactory;
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

    private static final java.awt.Color ACCENT = new java.awt.Color(0x4E, 0x6B, 0x3A); // verde oscuro, como el tablero
    private static final java.awt.Color ERROR = new java.awt.Color(0xA3, 0x2A, 0x2A);
    private static final String NO_USER = "Sin usuario";
    // Orden de cada desplegable: Sin usuario, Nuevo usuario... y después los registrados.
    private static final int NEW_USER_INDEX = 1;
    private static final String NEW_USER = "Nuevo usuario...";

    private final AccountService accounts;
    private final Runnable onPlay;
    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{"1 contra 1", "Contra la IA"});
    private final JComboBox<String> levelBox = new JComboBox<>(new String[]{"Fácil", "Medio", "Difícil"});
    private final JComboBox<String> colorBox = new JComboBox<>(new String[]{"Blancas", "Negras"});
    private final JComboBox<String> whiteBox = new JComboBox<>();
    private final JComboBox<String> blackBox = new JComboBox<>();
    private final JTextField nameField = new JTextField(12);
    private final JLabel message = new JLabel(" ");
    private final JLabel newUserLabel = new JLabel("Usuario nuevo");
    private final JPanel newUserRow = new JPanel(new BorderLayout(6, 0));
    private List<User> users = new ArrayList<>();
    private User whiteChoice;      // lo elegido en cada lado, para no perderlo cuando un lado pasa a ser la IA
    private User blackChoice;
    private boolean filling = false; // true mientras se rearman los desplegables (no son elecciones de la persona)

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
        title.setFont(title.getFont().deriveFont(Font.BOLD, 38f));
        title.setForeground(ACCENT);
        JLabel subtitle = new JLabel("TP de Ingeniería de Software (UADE)");
        subtitle.setForeground(java.awt.Color.GRAY);

        JButton registerButton = new JButton("Registrar");
        registerButton.addActionListener(e -> register());
        JButton playButton = new JButton("Jugar");
        playButton.setFont(playButton.getFont().deriveFont(Font.BOLD, 18f));
        playButton.addActionListener(e -> play());
        JButton rankingButton = new JButton("Ver ranking");
        rankingButton.addActionListener(e -> onRanking.run());
        modeBox.addActionListener(e -> updateEnabled());
        colorBox.addActionListener(e -> updateEnabled());

        // Usuario nuevo: el campo y su botón van juntos; la fila aparece solo al elegir "Nuevo usuario...".
        newUserRow.add(nameField, BorderLayout.CENTER);
        newUserRow.add(registerButton, BorderLayout.EAST);
        whiteBox.addActionListener(e -> showNewUserRow());
        blackBox.addActionListener(e -> showNewUserRow());

        // Dos grupos con título: cómo se juega y quién juega.
        JPanel game = section("Partida");
        addRow(game, 0, "Modo", modeBox);
        addRow(game, 1, "Dificultad", levelBox);
        addRow(game, 2, "Juego con", colorBox);
        JPanel people = section("Jugadores");
        addRow(people, 0, "Blancas", whiteBox);
        addRow(people, 1, "Negras", blackBox);
        addRow(people, 2, newUserLabel, newUserRow);

        message.setHorizontalAlignment(SwingConstants.CENTER);
        message.setForeground(ERROR);
        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8));
        buttons.add(playButton);
        buttons.add(rankingButton);
        playButton.setPreferredSize(new Dimension(300, 44));

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(50, 0, 2, 0);
        add(title, c);
        c.insets = new Insets(0, 0, 2, 0);
        add(subtitle, c);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);
        c.insets = new Insets(22, 0, 0, 0);
        add(game, c);
        c.insets = new Insets(12, 0, 0, 0);
        add(people, c);
        c.insets = new Insets(10, 0, 4, 0);
        add(message, c);
        c.insets = new Insets(4, 20, 0, 20);
        add(buttons, c);
        // Un espacio que empuja todo hacia arriba, así el menú no se mueve cuando aparece el campo de usuario nuevo.
        c.weighty = 1;
        add(new JLabel(), c);
        refresh();
    }

    // Grupo con título y margen interno.
    private static JPanel section(String title) {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(title),
            BorderFactory.createEmptyBorder(4, 10, 8, 10)));
        return section;
    }

    // Vuelve a leer los usuarios (cambian los puntos y los registros) conservando lo elegido.
    public void refresh() {
        saveChoices();
        users = new ArrayList<>();
        for (Score score : accounts.ranking()) users.add(score.getUser());
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
        if (isNew(whiteBox) || isNew(blackBox)) {
            message.setText("Registrá el usuario nuevo o elegí otra opción.");
            return;
        }
        User white = whiteUser();
        if (white != null && white.equals(blackUser())) {
            message.setText("Elegí usuarios distintos para blancas y negras.");
            return;
        }
        message.setText(" ");
        onPlay.run();
    }

    // El usuario nuevo queda elegido en el lado que pidió "Nuevo usuario...".
    private void register() {
        JComboBox<String> target = isNew(whiteBox) ? whiteBox : blackBox;
        try {
            User user = accounts.register(nameField.getText());
            nameField.setText("");
            refresh();
            target.setSelectedIndex(users.indexOf(user) + NEW_USER_INDEX + 1);
            message.setText("Registrado: " + user.getName() + ".");
        } catch (IllegalArgumentException e) {
            message.setText(e.getMessage());
        }
    }

    // Contra la IA solo se elige el usuario del lado propio; el otro lado dice "IA".
    private void updateEnabled() {
        saveChoices();
        boolean ai = versusAi();
        levelBox.setEnabled(ai);
        colorBox.setEnabled(ai);
        fill(whiteBox, whiteChoice, !ai || humanColor() == Color.WHITE);
        fill(blackBox, blackChoice, !ai || humanColor() == Color.BLACK);
        showNewUserRow();
    }

    // Guarda lo que muestran los desplegables habilitados (los de la IA no tienen elección).
    private void saveChoices() {
        if (whiteBox.isEnabled()) whiteChoice = userAt(whiteBox);
        if (blackBox.isEnabled()) blackChoice = userAt(blackBox);
    }

    private void showNewUserRow() {
        if (filling) return;
        boolean visible = isNew(whiteBox) || isNew(blackBox);
        newUserLabel.setVisible(visible);
        newUserRow.setVisible(visible);
        revalidate();
        repaint();
    }

    private boolean isNew(JComboBox<String> box) {
        return box.isEnabled() && box.getSelectedIndex() == NEW_USER_INDEX;
    }

    private User userAt(JComboBox<String> box) {
        int index = box.getSelectedIndex();
        return index <= NEW_USER_INDEX ? null : users.get(index - NEW_USER_INDEX - 1);
    }

    private void fill(JComboBox<String> box, User selected, boolean enabled) {
        filling = true;
        box.removeAllItems();
        if (enabled) {
            box.addItem(NO_USER);
            box.addItem(NEW_USER);
            for (User user : users) box.addItem(user.getName());
            box.setSelectedIndex(selected == null ? 0 : users.indexOf(selected) + NEW_USER_INDEX + 1);
        } else {
            box.addItem("IA");
        }
        box.setEnabled(enabled);
        filling = false;
    }

    private static void addRow(JPanel form, int y, String label, Component control) {
        addRow(form, y, new JLabel(label), control);
    }

    private static void addRow(JPanel form, int y, JLabel label, Component control) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.EAST;
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(95, 28)); // mismo ancho en los dos grupos: los controles quedan alineados
        form.add(label, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        control.setPreferredSize(new Dimension(200, 28));
        form.add(control, c);
    }
}
