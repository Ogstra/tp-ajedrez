package accounts;

import java.util.List;

// Puerto de salida (Clase 2): dónde se guardan los usuarios y sus puntajes. Las cuentas
// no saben si es un archivo, una base de datos o una lista en memoria (como en los tests).
// Es chico a propósito: se lee todo y se guarda todo, porque son pocos usuarios.
public interface IAccountStore {
    List<Score> loadAll();
    void saveAll(List<Score> scores);
}
