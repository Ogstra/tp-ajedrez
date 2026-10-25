package adaptadores;

import usuarios.AccountService;
import usuarios.User;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.function.Consumer;

// Primera pantalla de la ventana, con dos vistas: el login (entrar con el nombre o como
// invitado) y el registro. No hay contraseña. Le pasa el usuario (null si es invitado) a
// SwingChessView.
public class LoginPanel extends JPanel {

    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private final AccountService accounts;
    private final Consumer<User> onLogin;
    private final JPanel views = new JPanel(new CardLayout());
    private final JTextField loginField = field("loginName");
    private final JTextField registerField = field("registerName");
    private final JLabel loginMessage = message();
    private final JLabel registerMessage = message();
    private final FlatButton loginButton = new FlatButton("Entrar", FlatButton.Style.PRIMARY);
    private final FlatButton createButton = new FlatButton("Crear usuario", FlatButton.Style.PRIMARY);
    private boolean registering = false;

    public LoginPanel(AccountService accounts, Consumer<User> onLogin) {
        this.accounts = accounts;
        this.onLogin = onLogin;

        loginButton.addActionListener(e -> login());
        FlatButton registerButton = new FlatButton("Registrarse", FlatButton.Style.LINK);
        registerButton.addActionListener(e -> show(REGISTER));
        FlatButton guestButton = new FlatButton("Entrar como invitado", FlatButton.Style.LINK);
        guestButton.addActionListener(e -> enter(null));

        createButton.addActionListener(e -> register());
        FlatButton backButton = new FlatButton("Volver", FlatButton.Style.LINK);
        backButton.addActionListener(e -> show(LOGIN));

        JPanel login = view("Login", loginField, loginMessage, loginButton);
        MenuPanel.put(login, registerButton, 8);
        MenuPanel.put(login, guestButton, 4);
        JPanel register = view("Registro", registerField, registerMessage, createButton);
        MenuPanel.put(register, backButton, 8);

        views.setOpaque(false);
        views.add(login, LOGIN);
        views.add(register, REGISTER);

        // Las vistas quedan centradas en la ventana, a lo ancho y a lo alto.
        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(12, 0, 12, 0);
        add(views, c);
    }

    // El botón que aprieta Enter: Entrar en el login y Crear usuario en el registro.
    public JButton getDefaultButton() { return registering ? createButton : loginButton; }

    // Al cerrar sesión se vuelve al login con los campos vacíos.
    public void clear() {
        loginField.setText("");
        registerField.setText("");
        show(LOGIN);
    }

    private void show(String view) {
        registering = view.equals(REGISTER);
        loginMessage.setText(" ");
        registerMessage.setText(" ");
        ((CardLayout) views.getLayout()).show(views, view);
        JRootPane root = getRootPane();
        if (root != null) root.setDefaultButton(getDefaultButton());
    }

    private void login() {
        User user = accounts.login(loginField.getText());
        if (user == null) {
            loginMessage.setText("No existe ese usuario.");
            return;
        }
        enter(user);
    }

    private void register() {
        try {
            enter(accounts.register(registerField.getText()));
        } catch (IllegalArgumentException e) {
            registerMessage.setText(e.getMessage());
        }
    }

    private void enter(User user) {
        clear();
        onLogin.accept(user);
    }

    // Una vista: título, casillas de adorno, el campo de usuario, el mensaje de error y el botón principal.
    private static JPanel view(String titleText, JTextField field, JLabel message, JButton main) {
        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setFont(Palette.title(36));
        title.setForeground(Palette.PRIMARY);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        strip.setOpaque(false);
        strip.add(new BoardStrip());

        JPanel view = MenuPanel.column();
        MenuPanel.put(view, title, 0);
        MenuPanel.put(view, strip, 8);
        MenuPanel.put(view, MenuPanel.label("Usuario"), 20);
        MenuPanel.put(view, field, 6);
        MenuPanel.put(view, message, 8);
        MenuPanel.put(view, main, 4);
        return view;
    }

    private static JTextField field(String name) {
        JTextField field = new HintField("Nombre de usuario");
        field.setName(name);
        field.setPreferredSize(new Dimension(200, FlatComboBox.HEIGHT));
        field.setBorder(new RoundedBorder());
        field.setFont(field.getFont().deriveFont(14f));
        field.setForeground(Palette.INK);
        return field;
    }

    private static JLabel message() {
        JLabel message = new JLabel(" ", SwingConstants.CENTER);
        message.setForeground(Palette.ERROR);
        return message;
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
