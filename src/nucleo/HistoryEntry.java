package nucleo;

// Una jugada del historial junto con el estado de la partida antes y después.
// Así deshacer es volver a stateBefore, sin trucos de alternar turnos, y
// funciona también cuando la jugada terminó la partida.
final class HistoryEntry {
    final IMoveCommand command;
    final IGameState stateBefore;
    IGameState stateAfter;

    HistoryEntry(IMoveCommand command, IGameState stateBefore) {
        this.command = command;
        this.stateBefore = stateBefore;
    }
}
