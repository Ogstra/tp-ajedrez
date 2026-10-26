package core.movement;

import core.Board;
import core.Color;
import core.IMovementStrategy;
import core.Position;

import java.util.List;

public class BishopMovementStrategy implements IMovementStrategy {

    // Las 4 diagonales.
    private static final int[][] DIRECTIONS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        return SlidingMovement.inDirections(board, from, color, DIRECTIONS);
    }
}
