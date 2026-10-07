package nucleo.estrategias;

import nucleo.Board;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

public class PawnMovementStrategy implements IMovementStrategy {

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        List<Position> moves = new ArrayList<>();
        int direction = (color == Color.WHITE) ? 1 : -1;
        // Las blancas arrancan en la fila 1; las negras en la anteúltima,
        // sea cual sea el tamaño del tablero.
        int filaInicial = (color == Color.WHITE) ? 1 : board.getRows() - 2;

        // Un paso adelante, si está vacío.
        Position unPaso = new Position(from.getRow() + direction, from.getColumn());
        if (board.isInside(unPaso) && board.isEmpty(unPaso)) {
            moves.add(unPaso);

            // Dos pasos adelante, solo desde la fila inicial y con el camino libre.
            Position dosPasos = new Position(from.getRow() + 2 * direction, from.getColumn());
            if (from.getRow() == filaInicial && board.isInside(dosPasos) && board.isEmpty(dosPasos)) {
                moves.add(dosPasos);
            }
        }

        // Capturas en diagonal (no se implementa "al paso").
        for (Position diagonal : attackedSquares(board, from, color)) {
            if (board.isOccupiedByOpponent(diagonal, color)) {
                moves.add(diagonal);
            }
        }

        return moves;
    }

    // El peón ataca las dos diagonales de adelante, haya algo o no.
    @Override
    public List<Position> attackedSquares(Board board, Position from, Color color) {
        List<Position> attacked = new ArrayList<>();
        int direction = (color == Color.WHITE) ? 1 : -1;
        for (int deltaCol : new int[]{-1, 1}) {
            Position diagonal = new Position(from.getRow() + direction, from.getColumn() + deltaCol);
            if (board.isInside(diagonal)) {
                attacked.add(diagonal);
            }
        }
        return attacked;
    }
}
