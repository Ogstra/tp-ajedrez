package core.movement;

import core.Board;
import core.Color;
import core.IMovementStrategy;
import core.Position;

import java.util.ArrayList;
import java.util.List;

public class RookMovementStrategy implements IMovementStrategy {

    // Las 4 direcciones en las que desliza una torre: arriba, abajo, izquierda, derecha.
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        return SlidingMovement.inDirections(board, from, color, DIRECTIONS);
    }
}
