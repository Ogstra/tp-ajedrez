package skins;

import ports.ISkinStore;
import accounts.AccountService;
import accounts.User;

import java.util.ArrayList;
import java.util.List;

// Qué skins hay, cuáles tiene desbloqueadas cada usuario y cuál eligió. Una skin se desbloquea
// cuando los puntos del ranking llegan a los que pide: los puntos no se gastan. Si los puntos
// bajan (se deshizo una victoria), una skin que ya no alcanza deja de usarse hasta volver a llegar.
// La skin elegida se guarda por el puerto ISkinStore; la de un invitado dura solo esa sesión.
public class SkinService {

    private final List<Skin> skins;
    private final AccountService accounts;
    private final ISkinStore store;
    private Skin guestSkin;

    // La primera skin del catálogo es la de siempre: no pide puntos y es la que se usa por defecto.
    public SkinService(List<Skin> skins, AccountService accounts, ISkinStore store) {
        if (skins.isEmpty() || skins.get(0).getRequiredPoints() != 0) {
            throw new IllegalArgumentException("La primera skin del catálogo tiene que pedir 0 puntos.");
        }
        this.skins = new ArrayList<>(skins);
        this.accounts = accounts;
        this.store = store;
        this.guestSkin = skins.get(0);
    }

    public List<Skin> all() {
        return new ArrayList<>(skins);
    }

    public Skin defaultSkin() {
        return skins.get(0);
    }

    // Los puntos del usuario en el ranking; un invitado tiene 0.
    public int pointsOf(User user) {
        return accounts.pointsOf(user);
    }

    public boolean isUnlocked(Skin skin, User user) {
        return pointsOf(user) >= skin.getRequiredPoints();
    }

    // La skin con la que se juega: la elegida, si todavía está desbloqueada; si no, la de siempre.
    public Skin selected(User user) {
        Skin chosen = user == null ? guestSkin : find(store.load(user.getId()));
        return chosen != null && isUnlocked(chosen, user) ? chosen : defaultSkin();
    }

    // Lanza IllegalArgumentException si la skin todavía está bloqueada para ese usuario.
    public void select(User user, Skin skin) {
        if (!isUnlocked(skin, user)) {
            throw new IllegalArgumentException("Esa skin se desbloquea con " + skin.getRequiredPoints() + " puntos.");
        }
        if (user == null) {
            guestSkin = skin;
        } else {
            store.save(user.getId(), skin.getId());
        }
    }

    private Skin find(String id) {
        for (Skin skin : skins) {
            if (skin.getId().equals(id)) return skin;
        }
        return null;
    }
}
