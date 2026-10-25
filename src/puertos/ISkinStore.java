package puertos;

// Puerto de salida (Clase 2): dónde se guarda qué skin eligió cada usuario. Igual que
// IAccountStore, las skins no saben si es un archivo o una lista en memoria (como en los tests).
public interface ISkinStore {
    // El id de la skin elegida por ese usuario, o null si nunca eligió.
    String load(int userId);
    void save(int userId, String skinId);
}
