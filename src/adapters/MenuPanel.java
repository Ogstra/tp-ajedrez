package adapters;

import ai.OpponentLevel;
import core.Color;
import accounts.AccountService;
import accounts.Score;
import accounts.User;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

// Pantalla de inicio de la ventana: modo (1 contra 1 o contra la IA), dificultad, color, usuarios
// y registro. Solo junta las elecciones; arrancar la partida lo hace SwingChessView.
public class MenuPanel extends JPanel {

    private static final int COLUMN_WIDTH = 360;
    private static final String NO_USER = "Sin usuario";
    // Orden de cada desplegable: Sin usuario, Nuevo usuario... y después los registrados.
    private static final int NEW_USER_INDEX = 1;
    private static final String NEW_USER = "Nuevo usuario...";
    private static final String[] LEVEL_HELP = {
        "Juega jugadas al azar.", "Prioriza las capturas.", "Piensa dos jugadas por delante."};

    private final AccountService accounts;
    private final Runnable onPlay;
    private final ChoiceControl modeControl = ChoiceControl.segments("1 contra 1", "Contra la IA");
    private final ChoiceControl levelControl = ChoiceControl.segments("Fácil", "Medio", "Difícil");
    private final ChoiceControl colorControl = ChoiceControl.cards(new String[]{"Blancas", "Negras"},
        new Image[]{SwingChessView.pieceImage("wK"), SwingChessView.pieceImage("bK")});
    private final JComboBox<String> whiteBox = new FlatComboBox();
    private final JComboBox<String> blackBox = new FlatComboBox();
    private final JPanel whiteSlot = new JPanel(new CardLayout());   // el desplegable o la etiqueta "IA"
    private final JPanel blackSlot = new JPanel(new CardLayout());
    private final JPanel aiSection = column();
    private final JLabel levelHelp = small(LEVEL_HELP[0]);
    private final JTextField nameField = new HintField("Nombre del usuario nuevo");
    private final JPanel newUserRow = new JPanel(new BorderLayout(8, 0));
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final FlatButton playButton = new FlatButton("Jugar", FlatButton.Style.PRIMARY);
    private List<User> users = new ArrayList<>();
    private boolean whiteAi = false;   // qué lado juega la IA en este momento
    private boolean blackAi = false;
    private boolean filling = false;   // true mientras se rearman los desplegables (no son elecciones de la persona)

    public MenuPanel(AccountService accounts, Runnable onPlay, Runnable onRanking) {
        this.accounts = accounts;
        this.onPlay = onPlay;
        modeControl.setName("mode");
        levelControl.setName("level");
        colorControl.setName("color");
        whiteBox.setName("white");
        blackBox.setName("black");
        nameField.setName("newUser");

        modeControl.addActionListener(e -> update());
        colorControl.addActionListener(e -> update());
        levelControl.addActionListener(e -> levelHelp.setText(LEVEL_HELP[levelControl.getSelectedIndex()]));
        whiteBox.addActionListener(e -> showNewUserRow());
        blackBox.addActionListener(e -> showNewUserRow());
        playButton.addActionListener(e -> play());

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

        // Usuario nuevo: el campo y su botón; la fila aparece solo al elegir "Nuevo usuario...".
        JButton registerButton = new FlatButton("Registrar", FlatButton.Style.SECONDARY);
        registerButton.addActionListener(e -> register());
        nameField.addActionListener(e -> register()); // Enter en el campo también registra
        nameField.setPreferredSize(new Dimension(200, FlatComboBox.HEIGHT));
        nameField.setBorder(new RoundedBorder());
        nameField.setFont(nameField.getFont().deriveFont(14f));
        nameField.setForeground(Palette.INK);
        newUserRow.setOpaque(false);
        newUserRow.add(nameField, BorderLayout.CENTER);
        newUserRow.add(registerButton, BorderLayout.EAST);

        message.setForeground(Palette.ERROR);
        FlatButton rankingButton = new FlatButton("Ver ranking", FlatButton.Style.LINK);
        rankingButton.addActionListener(e -> onRanking.run());

        JPanel content = column();
        put(content, title, 0);
        put(content, strip, 8);
        put(content, label("Modo"), 20);
        put(content, modeControl, 6);
        put(content, aiSection, 0);
        put(content, label("Jugadores"), 14);
        put(content, playersBox(), 6);
        put(content, newUserRow, 8);
        put(content, message, 8);
        put(content, playButton, 4);
        put(content, rankingButton, 8);

        // La columna queda centrada en la ventana, a lo ancho y a lo alto.
        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 0, 12, 0);
        add(content, c);
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

    // El lado que juega la IA no tiene usuario.
    public User whiteUser() { return whiteAi ? null : userAt(whiteBox); }

    public User blackUser() { return blackAi ? null : userAt(blackBox); }

    private void play() {
        if (isNew(whiteBox, whiteAi) || isNew(blackBox, blackAi)) {
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
        JComboBox<String> target = isNew(whiteBox, whiteAi) ? whiteBox : blackBox;
        try {
            User user = accounts.register(nameField.getText());
            nameField.setText("");
            refresh();
            target.setSelectedIndex(users.indexOf(user) + NEW_USER_INDEX + 1);
            message.setText(" ");
        } catch (IllegalArgumentException e) {
            message.setText(e.getMessage());
        }
    }

    // Contra la IA aparecen la dificultad y el color, y el lado de la IA muestra la etiqueta "IA".
    private void update() {
        aiSection.setVisible(versusAi());
        whiteAi = versusAi() && aiColor() == Color.WHITE;
        blackAi = versusAi() && aiColor() == Color.BLACK;
        ((CardLayout) whiteSlot.getLayout()).show(whiteSlot, whiteAi ? "ai" : "user");
        ((CardLayout) blackSlot.getLayout()).show(blackSlot, blackAi ? "ai" : "user");
        showNewUserRow();
    }

    private void showNewUserRow() {
        if (filling) return;
        newUserRow.setVisible(isNew(whiteBox, whiteAi) || isNew(blackBox, blackAi));
        revalidate();
        repaint();
    }

    private boolean isNew(JComboBox<String> box, boolean ai) {
        return !ai && box.getSelectedIndex() == NEW_USER_INDEX;
    }

    private User userAt(JComboBox<String> box) {
        int index = box.getSelectedIndex();
        return index <= NEW_USER_INDEX ? null : users.get(index - NEW_USER_INDEX - 1);
    }

    private void fill(JComboBox<String> box, User selected) {
        filling = true;
        box.removeAllItems();
        box.addItem(NO_USER);
        box.addItem(NEW_USER);
        for (User user : users) box.addItem(user.getName());
        box.setSelectedIndex(selected == null ? 0 : users.indexOf(selected) + NEW_USER_INDEX + 1);
        filling = false;
    }

    // Recuadro blanco con una fila por lado: la pieza, el nombre del lado y el usuario (o "IA").
    private JPanel playersBox() {
        JPanel box = new RoundedPanel();
        box.setLayout(new GridBagLayout());
        put(box, playerRow("wK", "Blancas", whiteBox, whiteSlot, "whiteAi", false), 0);
        put(box, playerRow("bK", "Negras", blackBox, blackSlot, "blackAi", true), 0);
        return box;
    }

    private static JPanel playerRow(String piece, String side, JComboBox<String> box, JPanel slot,
                                    String badgeName, boolean divider) {
        JLabel name = new JLabel(side);
        name.setForeground(Palette.INK);
        name.setFont(name.getFont().deriveFont(14f));
        name.setIconTextGap(10);
        name.setPreferredSize(new Dimension(108, 30));
        Image image = SwingChessView.pieceImage(piece);
        if (image != null) name.setIcon(new ImageIcon(SmoothImage.of(image, 28)));

        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        badge.setOpaque(false);
        badge.setName(badgeName);
        badge.add(new Badge("IA"));
        badge.add(small("Juega con " + side.toLowerCase()));

        slot.setOpaque(false);
        slot.add(box, "user");
        slot.add(badge, "ai");

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(divider ? 1 : 0, 0, 0, 0, Palette.LINE),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        row.add(name, BorderLayout.WEST);
        row.add(slot, BorderLayout.CENTER);
        return row;
    }

    // Una columna donde cada componente ocupa todo el ancho, uno debajo del otro.
    private static JPanel column() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        put(panel, Box.createHorizontalStrut(COLUMN_WIDTH), 0);
        return panel;
    }

    private static void put(JPanel panel, Component part, int top) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(top, 0, 0, 0);
        part.setMinimumSize(part.getPreferredSize()); // si no entra, se corta abajo en vez de aplastar los controles
        panel.add(part, c);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Palette.MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    private static JLabel small(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Palette.MUTED);
        label.setFont(label.getFont().deriveFont(12.5f));
        return label;
    }

    // Campo de texto que muestra una pista gris mientras está vacío.
    private static class HintField extends JTextField {
        private final String hint;

        HintField(String hint) {
            super(12);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Palette.CONTROL);
            Insets insets = getInsets();
            g2.drawString(hint, insets.left + 2, (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent());
            g2.dispose();
        }
    }

    // Etiqueta oscura con esquinas redondeadas ("IA").
    private static class Badge extends JLabel {
        Badge(String text) {
            super(text);
            setForeground(Palette.BADGE_TEXT);
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Palette.BADGE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
