package nucleo.estrategias;

import nucleo.Board;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

public class KnightMovementStrategy implements IMovementStrategy {

    private static final int[][] SALTOS = {
        {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
        {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        List<Position> moves = new ArrayList<>();
        for (int[] salto : SALTOS) {
            Position destino = new Position(from.getRow() + salto[0], from.getColumn() + salto[1]);
            if (board.isInside(destino) && !board.isOccupiedByColor(destino, color)) {
                moves.add(destino);
            }
        }
        return moves;
    }
}
