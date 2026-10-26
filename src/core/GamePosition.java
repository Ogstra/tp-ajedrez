package core;

import java.util.List;
import java.util.Objects;

// Valor inmutable: tablero, turno y derechos que cambian las jugadas posibles.
// Las estrategias aportan su estado; la foto del tablero nunca se expone ni se modifica.
final class GamePosition {
    private final Board board;
    private final Color turn;
    private final List<List<Move>> specialState;

    GamePosition(Board board, Color turn, List<List<Move>> specialState) {
        this.board = board.cloneBoard();
        this.turn = turn;
        this.specialState = specialState.stream().map(List::copyOf).toList();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GamePosition)) return false;
        GamePosition position = (GamePosition) other;
        return board.equals(position.board) && turn == position.turn && specialState.equals(position.specialState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, turn, specialState);
    }
}
