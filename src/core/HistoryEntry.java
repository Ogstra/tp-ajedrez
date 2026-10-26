package core;

// Una jugada del historial junto con el estado de la partida antes y después,
// y la identidad de la posicion despues de la jugada (para contar repeticiones).
// Así deshacer es volver a stateBefore, sin trucos de alternar turnos.
final class HistoryEntry {
    final IMoveCommand command;
    final IGameState stateBefore;
    GamePosition positionAfter;
    IGameState stateAfter;
    MoveOutcome outcome;  // cómo quedó la partida: redo() lo vuelve a anunciar
    String drawReason;    // el motivo, si fueron tablas

    HistoryEntry(IMoveCommand command, IGameState stateBefore) {
        this.command = command;
        this.stateBefore = stateBefore;
    }
}
