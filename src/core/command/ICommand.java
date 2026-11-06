package core.command;

// Command: la jugada se guarda como objeto para poder deshacerla.
public interface ICommand {
    void execute();
    void undo();
}
