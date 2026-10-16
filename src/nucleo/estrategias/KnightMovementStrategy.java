package nucleo.estrategias;

import nucleo.Board;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

public class KnightMovementStrategy implements IMovementStrategy {

    private static final int[][] JUMPS = {
        {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
        {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        List<Position> moves = new ArrayList<>();
        for (int[] jump : JUMPS) {
            Position destination = new Position(from.getRow() + jump[0], from.getColumn() + jump[1]);
            if (board.isInside(destination) && !board.isOccupiedByColor(destination, color)) {
                moves.add(destination);
            }
        }
        return moves;
    }
}
