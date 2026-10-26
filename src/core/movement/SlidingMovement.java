package core.movement;

import core.Board;
import core.Color;
import core.Position;

import java.util.ArrayList;
import java.util.List;

// Lógica compartida por Torre, Alfil y Reina: deslizar en un conjunto de
// direcciones hasta chocar con el borde, una pieza propia (no se puede
// entrar) o una pieza rival (se puede capturar, pero no seguir de largo).
// No es un patrón de la materia, es DRY (Clase 1) para no repetir este
// bucle tres veces.
final class SlidingMovement {

    private SlidingMovement() { }

    static List<Position> inDirections(Board board, Position from, Color color, int[][] directions) {
        List<Position> moves = new ArrayList<>();
        for (int[] dir : directions) {
            int row = from.getRow() + dir[0];
            int col = from.getColumn() + dir[1];
            Position pos = new Position(row, col);

            while (board.isInside(pos) && board.isEmpty(pos)) {
                moves.add(pos);
                row += dir[0];
                col += dir[1];
                pos = new Position(row, col);
            }
            if (board.isInside(pos) && board.isOccupiedByOpponent(pos, color)) {
                moves.add(pos);
            }
        }
        return moves;
    }
}
