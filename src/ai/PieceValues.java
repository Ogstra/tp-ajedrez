package ai;

import core.board.PieceType;

// Valor de cada pieza, el clásico del ajedrez. Una pieza que no conoce vale 0.
public final class PieceValues {
    private PieceValues() { }

    public static int of(PieceType type) {
        if (type.equals(PieceType.PAWN)) return 1;
        if (type.equals(PieceType.KNIGHT)) return 3;
        if (type.equals(PieceType.BISHOP)) return 3;
        if (type.equals(PieceType.ROOK)) return 5;
        if (type.equals(PieceType.QUEEN)) return 9;
        return 0; // el rey no se cuenta porque nunca se captura
    }
}
