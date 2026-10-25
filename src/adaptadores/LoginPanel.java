package adaptadores;

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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

// Pantalla de usuario entre el menú y la partida contra la IA: iniciar sesión eligiendo de la
// lista, registrarse con el nombre o seguir sin usuario. Solo junta la elección; arrancar la
// partida lo hace SwingChessView. El 1 contra 1 no pasa por acá porque no suma al ranking.
public class LoginPanel extends JPanel {

    private static final String NO_USER = "Sin usuario";
    // Orden del desplegable: Sin usuario, Nuevo usuario... y después los registrados.
    private static final int NEW_USER_INDEX = 1;
    private static final String NEW_USER = "Nuevo usuario...";

    private final AccountService accounts;
    private final JComboBox<String> userBox = new FlatComboBox();
    private final JTextField nameField = new HintField("Nombre del usuario nuevo");
    private final JPanel newUserRow = new JPanel(new BorderLayout(8, 0));
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final FlatButton startButton = new FlatButton("Empezar", FlatButton.Style.PRIMARY);
    private List<User> users = new ArrayList<>();
    private boolean filling = false;   // true mientras se rearma el desplegable (no es una elección de la persona)

    public LoginPanel(AccountService accounts, Runnable onStart, Runnable onBack) {
        this.accounts = accounts;
        userBox.setName("user");
        nameField.setName("newUser");
        userBox.addActionListener(e -> showNewUserRow());
        startButton.addActionListener(e -> {
            if (isNew()) {
                message.setText("Registrá el usuario nuevo o elegí otra opción.");
                return;
            }
            message.setText(" ");
            onStart.run();
        });

        JLabel title = new JLabel("Contra la IA", SwingConstants.CENTER);
        title.setFont(Palette.title(32));
        title.setForeground(Palette.PRIMARY);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        strip.setOpaque(false);
        strip.add(new BoardStrip());

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
        FlatButton backButton = new FlatButton("Volver al menú", FlatButton.Style.LINK);
        backButton.addActionListener(e -> onBack.run());

        JPanel content = MenuPanel.column();
        MenuPanel.put(content, title, 0);
        MenuPanel.put(content, strip, 8);
        MenuPanel.put(content, MenuPanel.label("Usuario"), 20);
        MenuPanel.put(content, userBox, 6);
        MenuPanel.put(content, MenuPanel.small("Con un usuario, el resultado suma al ranking."), 5);
        MenuPanel.put(content, newUserRow, 8);
        MenuPanel.put(content, message, 8);
        MenuPanel.put(content, startButton, 4);
        MenuPanel.put(content, backButton, 8);

        // La columna queda centrada en la ventana, a lo ancho y a lo alto.
        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 0, 12, 0);
        add(content, c);
        refresh();
    }

    // Vuelve a leer los usuarios (cambian los registros) conservando lo elegido.
    public void refresh() {
        User selected = user();
        users = new ArrayList<>();
        for (Score score : accounts.ranking()) users.add(score.getUser());
        fill(selected);
        showNewUserRow();
    }

    // Para que la ventana lo use como botón por defecto (Enter) mientras se ve esta pantalla.
    public JButton getStartButton() { return startButton; }

    // El usuario elegido, o null con "Sin usuario" o "Nuevo usuario...".
    public User user() {
        int index = userBox.getSelectedIndex();
        return index <= NEW_USER_INDEX ? null : users.get(index - NEW_USER_INDEX - 1);
    }

    // El usuario nuevo queda elegido en el desplegable.
    private void register() {
        try {
            User user = accounts.register(nameField.getText());
            nameField.setText("");
            refresh();
            userBox.setSelectedIndex(users.indexOf(user) + NEW_USER_INDEX + 1);
            message.setText(" ");
        } catch (IllegalArgumentException e) {
            message.setText(e.getMessage());
        }
    }

    private void showNewUserRow() {
        if (filling) return;
        newUserRow.setVisible(isNew());
        revalidate();
        repaint();
    }

    private boolean isNew() {
        return userBox.getSelectedIndex() == NEW_USER_INDEX;
    }

    private void fill(User selected) {
        filling = true;
        userBox.removeAllItems();
        userBox.addItem(NO_USER);
        userBox.addItem(NEW_USER);
        for (User user : users) userBox.addItem(user.getName());
        userBox.setSelectedIndex(selected == null ? 0 : users.indexOf(selected) + NEW_USER_INDEX + 1);
        filling = false;
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
}
