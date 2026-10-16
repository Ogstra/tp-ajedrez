package ai;

import nucleo.IChessGame;
import nucleo.Move;
import nucleo.Piece;

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
