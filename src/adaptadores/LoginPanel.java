package adaptadores;

import usuarios.AccountService;
import usuarios.User;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.function.Consumer;

// Primera pantalla de la ventana: entrar con el nombre de usuario, registrarse o entrar como
// invitado. No hay contraseña. Le pasa el usuario (null si es invitado) a SwingChessView.
public class LoginPanel extends JPanel {

    private final AccountService accounts;
    private final Consumer<User> onLogin;
    private final JTextField nameField = new HintField("Nombre de usuario");
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final FlatButton loginButton = new FlatButton("Entrar", FlatButton.Style.PRIMARY);

    public LoginPanel(AccountService accounts, Consumer<User> onLogin) {
        this.accounts = accounts;
        this.onLogin = onLogin;
        nameField.setName("loginName");
        nameField.setPreferredSize(new Dimension(200, FlatComboBox.HEIGHT));
        nameField.setBorder(new RoundedBorder());
        nameField.setFont(nameField.getFont().deriveFont(14f));
        nameField.setForeground(Palette.INK);

        loginButton.addActionListener(e -> login());
        FlatButton registerButton = new FlatButton("Registrarse", FlatButton.Style.SECONDARY);
        registerButton.addActionListener(e -> register());
        FlatButton guestButton = new FlatButton("Entrar como invitado", FlatButton.Style.LINK);
        guestButton.addActionListener(e -> enter(null));

        JLabel title = new JLabel("Login", SwingConstants.CENTER);
        title.setFont(Palette.title(36));
        title.setForeground(Palette.PRIMARY);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        strip.setOpaque(false);
        strip.add(new BoardStrip());
        message.setForeground(Palette.ERROR);

        JPanel content = MenuPanel.column();
        MenuPanel.put(content, title, 0);
        MenuPanel.put(content, strip, 8);
        MenuPanel.put(content, MenuPanel.label("Usuario"), 20);
        MenuPanel.put(content, nameField, 6);
        MenuPanel.put(content, message, 8);
        MenuPanel.put(content, loginButton, 4);
        MenuPanel.put(content, registerButton, 8);
        MenuPanel.put(content, guestButton, 8);

        // La columna queda centrada en la ventana, a lo ancho y a lo alto.
        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 0, 12, 0);
        add(content, c);
    }

    // Para que la ventana lo use como botón por defecto (Enter) mientras se ve esta pantalla.
    public JButton getLoginButton() { return loginButton; }

    // Al cerrar sesión se vuelve acá con el campo vacío.
    public void clear() {
        nameField.setText("");
        message.setText(" ");
    }

    private void login() {
        User user = accounts.login(nameField.getText());
        if (user == null) {
            message.setText("No existe ese usuario. Si es nuevo, registrate.");
            return;
        }
        enter(user);
    }

    private void register() {
        try {
            enter(accounts.register(nameField.getText()));
        } catch (IllegalArgumentException e) {
            message.setText(e.getMessage());
        }
    }

    private void enter(User user) {
        clear();
        onLogin.accept(user);
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
