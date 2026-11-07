package accounts;

import java.util.List;

// Puerto de salida: dónde se guardan los usuarios y sus puntajes. Puede ser un archivo o una
// lista en memoria, como en los tests. Se lee y se guarda todo junto porque son pocos usuarios.
public interface IAccountStore {
    List<Score> loadAll();
    void saveAll(List<Score> scores);
}
