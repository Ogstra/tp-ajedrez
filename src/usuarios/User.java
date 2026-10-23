package usuarios;

import java.util.Objects;

// Value object inmutable: quién es un jugador. El id lo asigna el sistema al
// registrarse; junto con el nombre alcanza para volver a entrar (no hay contraseña).
public final class User {
    private final int id;
    private final String name;

    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return id == ((User) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (id " + id + ")";
    }
}
