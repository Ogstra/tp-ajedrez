package core.movement;


import core.board.Board;
import core.board.Color;
import core.board.Position;
import java.util.List;

// La reina se mueve como torre y alfil: compone las dos estrategias y une los resultados.
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
