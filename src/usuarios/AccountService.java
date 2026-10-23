package usuarios;

import puertos.IAccountStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Registro, login y ranking de jugadores. No sabe dónde se guardan los datos:
// los pide y los entrega por el puerto IAccountStore, que entra por constructor.
// El login es solo por nombre de usuario: no hay contraseña. El id lo asigna el sistema y es interno.
public class AccountService {

    public static final int MAX_NAME_LENGTH = 20;

    private final IAccountStore store;

    public AccountService(IAccountStore store) {
        this.store = store;
    }

    // Da de alta un usuario nuevo con el próximo id libre.
    // Lanza IllegalArgumentException (con un mensaje para mostrar) si el nombre no sirve.
    public User register(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        validate(name);

        List<Score> scores = store.loadAll();
        int nextId = 1;
        for (Score score : scores) {
            if (score.getUser().getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("Ya existe un usuario llamado " + score.getUser().getName() + ".");
            }
            nextId = Math.max(nextId, score.getUser().getId() + 1);
        }

        User user = new User(nextId, name);
        scores.add(new Score(user, 0, 0, 0));
        store.saveAll(scores);
        return user;
    }

    // Devuelve el usuario con ese nombre (sin distinguir mayúsculas), o null si no existe.
    public User login(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        for (Score score : store.loadAll()) {
            if (score.getUser().getName().equalsIgnoreCase(name)) return score.getUser();
        }
        return null;
    }

    // Todos los usuarios, del que más puntos tiene al que menos. Empata por victorias y después por nombre.
    public List<Score> ranking() {
        List<Score> scores = store.loadAll();
        Collections.sort(scores, new Comparator<Score>() {
            @Override
            public int compare(Score a, Score b) {
                if (a.getPoints() != b.getPoints()) return b.getPoints() - a.getPoints();
                if (a.getWins() != b.getWins()) return b.getWins() - a.getWins();
                return a.getUser().getName().compareToIgnoreCase(b.getUser().getName());
            }
        });
        return scores;
    }

    public void record(User user, Result result) {
        adjust(user, result, 1);
    }

    // Retira un resultado ya acreditado (cuando se deshace el mate o las tablas).
    public void revert(User user, Result result) {
        adjust(user, result, -1);
    }

    private void adjust(User user, Result result, int amount) {
        List<Score> scores = store.loadAll();
        List<Score> updated = new ArrayList<>();
        for (Score score : scores) {
            updated.add(score.getUser().equals(user) ? score.plus(result, amount) : score);
        }
        store.saveAll(updated);
    }

    // El nombre termina en un archivo de texto separado por ";", por eso no puede tener ";" ni saltos de línea.
    private void validate(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Escribí un nombre de usuario.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre puede tener hasta " + MAX_NAME_LENGTH + " caracteres.");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ';' || Character.isISOControl(c)) {
                throw new IllegalArgumentException("El nombre no puede tener ';' ni saltos de línea.");
            }
        }
    }
}
