package nucleo;

// Command (Clase 3): la acción de mover se guarda como objeto, en vez de
// aplicarse directo, para poder deshacerla después.
public interface ICommand {
    void execute();
    void undo();
}
