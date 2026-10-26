package ai;

import core.IChessGame;
import core.Move;
import core.Piece;
import core.PieceType;
import core.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Nivel medio: la jugada que captura la pieza de más valor; si ninguna
// captura, o hay empate, una de las mejores al azar.
public class GreedyOpponent implements IOpponent {

    private final Random random;

    public GreedyOpponent(Random random) {
        this.random = random;
    }

    @Override
    public Move chooseMove(IChessGame game) {
        int bestValue = -1;
        List<Move> best = new ArrayList<>();
        for (Move move : game.legalMoves()) {
            Piece target = game.getBoard().pieceAt(move.getTo());
            Piece moving = game.getBoard().pieceAt(move.getFrom());
            // Un peon que captura en diagonal a casilla vacia captura al paso.
            if (target == null && moving.getType().equals(PieceType.PAWN)
                    && move.getFrom().getColumn() != move.getTo().getColumn()) {
                target = game.getBoard().pieceAt(new Position(move.getFrom().getRow(), move.getTo().getColumn()));
            }
            int value = target == null ? 0 : PieceValues.of(target.getType());
            if (value > bestValue) {
                bestValue = value;
                best.clear();
            }
            if (value == bestValue) {
                best.add(move);
            }
        }
        return best.get(random.nextInt(best.size()));
    }
}
