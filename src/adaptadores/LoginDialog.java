package adaptadores;

import usuarios.AccountService;
import usuarios.User;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;

// Adaptador de entrada (Clase 2) de las cuentas, con ventana: botones para iniciar sesión (usuario e id),
// registrarse (solo el nombre, el id lo da el sistema) o jugar sin usuario. No hay contraseña.
public final class LoginDialog {

    private LoginDialog() { }

    // Devuelve el usuario que entró, o null si se elige jugar sin usuario. Primero se elige qué
    // hacer con tres botones; si en el formulario se cancela, se vuelve a esta pantalla.
    public static User show(Component parent, AccountService accounts, String role) {
        String[] options = {"Iniciar sesión", "Registrarse", "Jugar sin usuario"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(parent, "Jugador " + role, "Jugador " + role,
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

    private static User askLogin(Component parent, AccountService accounts) {
        JTextField name = new JTextField(16);
        JTextField id = new JTextField(16);
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Usuario"));
        panel.add(name);
        panel.add(new JLabel("Id"));
        panel.add(id);
        while (JOptionPane.showConfirmDialog(parent, panel, "Iniciar sesión",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                return enter(accounts, false, name.getText(), id.getText());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(), "No se pudo entrar", JOptionPane.WARNING_MESSAGE);
            }
        }
        return null;
    }

    // Al registrarse solo se pide el nombre: el id lo asigna el sistema y se le muestra a la persona.
    private static User askRegister(Component parent, AccountService accounts) {
        JTextField name = new JTextField(16);
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Usuario"));
        panel.add(name);
        while (JOptionPane.showConfirmDialog(parent, panel, "Registrarse",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                User user = enter(accounts, true, name.getText(), "");
                JOptionPane.showMessageDialog(parent, "Listo, " + user.getName() + ". Tu id es " + user.getId()
                    + ": anotalo, lo necesitás para volver a entrar.", "Registro", JOptionPane.INFORMATION_MESSAGE);
                return user;
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(), "No se pudo registrar", JOptionPane.WARNING_MESSAGE);
            }
        }
        return null;
    }

    // La lógica del formulario, sin ventanas, para poder probarla. Lanza IllegalArgumentException
    // con el mensaje que se le muestra a la persona.
    public static User enter(AccountService accounts, boolean registering, String name, String rawId) {
        if (registering) return accounts.register(name);
        int id;
        try {
            id = Integer.parseInt(rawId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El id es un número. Si todavía no tenés uno, elegí Registrarme.");
        }
        User user = accounts.login(name, id);
        if (user == null) throw new IllegalArgumentException("Usuario o id incorrectos.");
        return user;
    }
}
