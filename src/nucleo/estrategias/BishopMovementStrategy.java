package nucleo.estrategias;

import nucleo.Board;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.Position;

import java.util.List;

public class BishopMovementStrategy implements IMovementStrategy {

    // Las 4 diagonales.
    private static final int[][] DIRECCIONES = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        return MovimientoDeslizante.enDirecciones(board, from, color, DIRECCIONES);
    }
}
