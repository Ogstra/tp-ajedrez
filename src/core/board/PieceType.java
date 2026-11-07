package core.board;

import java.util.Objects;

// Clase y no enum: agregar una pieza a un enum obliga a editarlo (Open/Closed).
// Un tipo nuevo se crea con new PieceType("CHANCELLOR"). Dos tipos con el mismo
// nombre son el mismo tipo.
public final class PieceType {
    public static final PieceType PAWN = new PieceType("PAWN");
    public static final PieceType ROOK = new PieceType("ROOK");
    public static final PieceType KNIGHT = new PieceType("KNIGHT");
    public static final PieceType BISHOP = new PieceType("BISHOP");
    public static final PieceType QUEEN = new PieceType("QUEEN");
    public static final PieceType KING = new PieceType("KING");

    private final String name;

    public PieceType(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PieceType)) return false;
        return name.equals(((PieceType) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
