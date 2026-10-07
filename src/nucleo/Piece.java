package nucleo;

import java.util.Objects;

// Una sola clase Piece para las seis piezas. Composición sobre herencia
// (Clase 3): en vez de Pawn extends Piece, Rook extends Piece, etc.,
// Piece TIENE una IMovementStrategy que le da su comportamiento.
public final class Piece {
    private final PieceType type;
    private final Color color;
    private final IMovementStrategy movementStrategy;

    public Piece(PieceType type, Color color, IMovementStrategy movementStrategy) {
        this.type = type;
        this.color = color;
        this.movementStrategy = movementStrategy;
    }

    public PieceType getType() { return type; }
    public Color getColor() { return color; }

    public java.util.List<Position> possibleMoves(Board board, Position from) {
        return movementStrategy.possibleMoves(board, from, color);
    }

    public java.util.List<Position> attackedSquares(Board board, Position from) {
        return movementStrategy.attackedSquares(board, from, color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piece)) return false;
        Piece piece = (Piece) o;
        return type.equals(piece.type) && color == piece.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, color);
    }
}
