package adaptadores;

import usuarios.AccountService;
import usuarios.Score;
import usuarios.User;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;

// Adaptador de entrada (Clase 2) de las cuentas, con ventana: botones para iniciar sesión (se elige de una lista),
// registrarse o jugar sin usuario. Se entra solo con el nombre; el id lo da el sistema y no se muestra.
public final class LoginDialog {

    private LoginDialog() { }

    // Devuelve el usuario que entró, o null si se elige jugar sin usuario. Primero se elige qué
    // hacer con tres botones; si en el formulario se cancela, se vuelve a esta pantalla.
    // "hint" es una aclaración opcional que se agrega debajo de la pregunta (puede ir vacía).
    public static User show(Component parent, AccountService accounts, String role, String hint) {
        String[] options = {"Iniciar sesión", "Registrarse", "Jugar sin usuario"};
        String message = "Jugador de las " + role + ": ¿cómo querés entrar?" + (hint.isEmpty() ? "" : "\n" + hint);
        while (true) {
            int choice = JOptionPane.showOptionDialog(parent, message, "Jugador de las " + role,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                User user = askLogin(parent, accounts);
                if (user != null) return user;
            } else if (choice == 1) {
                User user = askRegister(parent, accounts);
                if (user != null) return user;
            } else {
                return null;
            }
        }
    }

    // Se elige el usuario de una lista: no hay que escribir nada.
    private static User askLogin(Component parent, AccountService accounts) {
        List<Score> ranking = accounts.ranking();
        if (ranking.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Todavía no hay usuarios registrados. Elegí Registrarse.",
                "Iniciar sesión", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        JComboBox<String> name = new JComboBox<>();
        for (Score score : ranking) name.addItem(score.getUser().getName());
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Usuario"));
        panel.add(name);
        if (JOptionPane.showConfirmDialog(parent, panel, "Iniciar sesión",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return null;
        }
        return enter(accounts, false, (String) name.getSelectedItem());
    }

    // Al registrarse solo se pide el nombre: el id lo asigna el sistema y no se muestra.
    private static User askRegister(Component parent, AccountService accounts) {
        JTextField name = new JTextField(16);
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Usuario"));
        panel.add(name);
        while (JOptionPane.showConfirmDialog(parent, panel, "Registrarse",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                return enter(accounts, true, name.getText());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(), "No se pudo registrar", JOptionPane.WARNING_MESSAGE);
            }
        }
        return null;
    }

    // La lógica del formulario, sin ventanas, para poder probarla. Lanza IllegalArgumentException
    // con el mensaje que se le muestra a la persona.
    public static User enter(AccountService accounts, boolean registering, String name) {
        if (registering) return accounts.register(name);
        User user = accounts.login(name);
        if (user == null) throw new IllegalArgumentException("No existe un usuario llamado " + name + ".");
        return user;
    }
}
