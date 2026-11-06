package core.movement;


import core.board.Board;
import core.board.Color;
import core.board.Position;
import java.util.ArrayList;
import java.util.List;

// Movimiento compartido por la torre, el alfil y la reina: avanza en cada dirección hasta
// el borde, una pieza propia (no entra) o una rival (la captura y se detiene).
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
