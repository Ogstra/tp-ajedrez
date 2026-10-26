package core.movement;

import core.Board;
import core.Color;
import core.IMovementStrategy;
import core.Position;

import java.util.List;

// La reina se mueve como torre + alfil combinados. En vez de heredar de
// ninguna de las dos, COMPONE una de cada una y une los resultados:
// composición sobre herencia (Clase 3), aplicada dos veces en la misma clase.
public class QueenMovementStrategy implements IMovementStrategy {

    private final RookMovementStrategy asRook = new RookMovementStrategy();
    private final BishopMovementStrategy asBishop = new BishopMovementStrategy();

    @Override
    public List<Position> possibleMoves(Board board, Position from, Color color) {
        List<Position> moves = new java.util.ArrayList<>(asRook.possibleMoves(board, from, color));
        moves.addAll(asBishop.possibleMoves(board, from, color));
        return moves;
    }
}
