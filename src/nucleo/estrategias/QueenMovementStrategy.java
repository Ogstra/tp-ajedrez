package nucleo.estrategias;

import nucleo.Board;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.Position;

import java.util.List;

// La reina se mueve como torre + alfil combinados. En vez de heredar de
// ninguna de las dos, COMPONE una de cada una y une los resultados:
// composición sobre herencia (Clase 3), aplicada dos veces en la misma clase.
public class QueenMovementStrategy implements IMovementStrategy {

    private final RookMovementStrategy comoTorre = new RookMovementStrategy();
    private final BishopMovementStrategy comoAlfil = new BishopMovementStrategy();

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        List<Position> moves = new java.util.ArrayList<>(comoTorre.possibleMoves(board, from, color));
        moves.addAll(comoAlfil.possibleMoves(board, from, color));
        return moves;
    }
}
