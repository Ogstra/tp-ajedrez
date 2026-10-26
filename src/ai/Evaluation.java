package ai;

import core.Board;
import core.Color;
import core.Piece;
import core.PieceType;
import core.Position;

// Cuánto vale una posición para un bando: el material, que es lo principal, y
// un poco de criterio posicional para que no juegue al azar cuando ninguna
// jugada gana ni pierde material. Son reglas simples y conocidas:
//   los peones avanzan por el centro,
//   los caballos y alfiles se desarrollan y van al centro,
//   mientras haya damas el rey se queda en su fila (y se enroca),
//   en el final, con menos piezas, el rey va hacia el centro.
// Todo es relativo al tamaño del tablero, igual que el resto del proyecto.
public final class Evaluation {

    private static final int MATERIAL_UNIT = 100; // un peón vale 100; el criterio posicional suma pocas unidades

    private Evaluation() { }

    // Positivo si va ganando "me", negativo si va ganando el rival.
    public static int score(Board board, Color me) {
        boolean queensOnBoard = hasQueen(board);
        int total = 0;
        for (Color color : new Color[]{Color.WHITE, Color.BLACK}) {
            for (Position position : board.positionsOfColor(color)) {
                Piece piece = board.pieceAt(position);
                int value = PieceValues.of(piece.getType()) * MATERIAL_UNIT
                    + positional(board, piece, position, queensOnBoard);
                total += color == me ? value : -value;
            }
        }
        return total;
    }

    private static int positional(Board board, Piece piece, Position position, boolean queensOnBoard) {
        PieceType type = piece.getType();
        int centrality = centrality(board, position);
        int stepsFromHome = Math.abs(position.getRow() - backRow(board, piece.getColor()));

        if (type.equals(PieceType.PAWN)) {
            return 4 * centrality + 6 * stepsFromHome;
        }
        if (type.equals(PieceType.KNIGHT) || type.equals(PieceType.BISHOP)) {
            int developed = stepsFromHome > 0 ? 15 : 0;
            return 8 * centrality + developed;
        }
        if (type.equals(PieceType.KING)) {
            if (!queensOnBoard) return 6 * centrality; // en el final, el rey participa
            int castled = stepsFromHome == 0 && centrality <= 2 ? 25 : 0; // en una esquina, y no en el medio
            return castled - 40 * stepsFromHome;
        }
        return 2 * centrality; // torre, dama y piezas desconocidas: apenas hacia el centro
    }

    // 0 en una esquina, más grande cuanto más cerca del centro del tablero.
    private static int centrality(Board board, Position position) {
        double dx = Math.abs(position.getColumn() - (board.getColumns() - 1) / 2.0);
        double dy = Math.abs(position.getRow() - (board.getRows() - 1) / 2.0);
        double max = (board.getColumns() - 1) / 2.0 + (board.getRows() - 1) / 2.0;
        return (int) Math.round(max - dx - dy);
    }

    private static int backRow(Board board, Color color) {
        return color == Color.WHITE ? 0 : board.getRows() - 1;
    }

    private static boolean hasQueen(Board board) {
        for (Color color : new Color[]{Color.WHITE, Color.BLACK}) {
            for (Position position : board.positionsOfColor(color)) {
                if (board.pieceAt(position).getType().equals(PieceType.QUEEN)) return true;
            }
        }
        return false;
    }
}
