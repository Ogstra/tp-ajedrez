package nucleo.estrategias;

import nucleo.Board;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

// No implementa enroque: fuera del alcance mínimo de este TP.
public class KingMovementStrategy implements IMovementStrategy {

    private static final int[][] DIRECCIONES = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        List<Position> moves = new ArrayList<>();
        for (int[] dir : DIRECCIONES) {
            Position destino = new Position(from.getRow() + dir[0], from.getColumn() + dir[1]);
            if (board.isInside(destino) && !board.isOccupiedByColor(destino, color)) {
                moves.add(destino);
            }
        }
        return moves;
    }
}
