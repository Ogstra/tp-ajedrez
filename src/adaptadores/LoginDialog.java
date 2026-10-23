package adaptadores;

import usuarios.AccountService;
import usuarios.User;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;

// Adaptador de entrada (Clase 2) de las cuentas, con ventana: pide usuario e id para entrar, o
// un nombre para registrarse (el id lo da el sistema). No hay contraseña.
public final class LoginDialog {

    private LoginDialog() { }

    // Devuelve el usuario que entró, o null si se elige jugar sin usuario.
    public static User show(Component parent, AccountService accounts, String role) {
        while (true) {
            JComboBox<String> mode = new JComboBox<>(new String[]{"Iniciar sesión", "Registrarme"});
            JTextField name = new JTextField(16);
            JTextField id = new JTextField(16);
            mode.addActionListener(e -> id.setEnabled(mode.getSelectedIndex() == 0));

            JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
            panel.add(new JLabel("Qué querés hacer"));
            panel.add(mode);
            panel.add(new JLabel("Usuario"));
            panel.add(name);
            panel.add(new JLabel("Id (solo para iniciar sesión)"));
            panel.add(id);

            int choice = JOptionPane.showOptionDialog(parent, panel, "Jugador " + role,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Entrar", "Jugar sin usuario"}, "Entrar");
            if (choice != 0) return null;

            boolean registering = mode.getSelectedIndex() == 1;
            try {
                User user = enter(accounts, registering, name.getText(), id.getText());
                if (registering) {
                    JOptionPane.showMessageDialog(parent, "Listo, " + user.getName() + ". Tu id es " + user.getId()
                        + ": anotalo, lo necesitás para volver a entrar.", "Registro", JOptionPane.INFORMATION_MESSAGE);
                }
                return user;
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(), "No se pudo entrar", JOptionPane.WARNING_MESSAGE);
            }
        }
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
