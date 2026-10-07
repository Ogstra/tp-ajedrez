package nucleo.estrategias;

import nucleo.Board;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

public class RookMovementStrategy implements IMovementStrategy {

    // Las 4 direcciones en las que desliza una torre: arriba, abajo, izquierda, derecha.
    private static final int[][] DIRECCIONES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        return MovimientoDeslizante.enDirecciones(board, from, color, DIRECCIONES);
    }
}
