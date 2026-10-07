package nucleo;

import java.util.Objects;

// Antes era un enum. Un enum es un conjunto cerrado: agregar una pieza nueva
// obligaba a editar este archivo (y el switch de PieceFactory), lo que rompe
// Open/Closed. Como clase de valor, cualquiera puede crear un tipo nuevo
// (new PieceType("CHANCELLOR")) sin tocar ninguna clase existente.
// Se identifica por nombre: dos PieceType con el mismo nombre son el mismo tipo.
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
