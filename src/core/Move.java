package core;

import java.util.Objects;

// Value object inmutable: una jugada es "de esta casilla a esta otra".
// Igualdad por contenido, igual que Position.
public final class Move {
    private final Position from;
    private final Position to;

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public Position getFrom() { return from; }
    public Position getTo() { return to; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move move = (Move) o;
        return from.equals(move.from) && to.equals(move.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + "->" + to;
    }
}
