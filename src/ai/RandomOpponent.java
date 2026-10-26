package ai;

import core.IChessGame;
import core.Move;

import java.util.List;
import java.util.Random;

// Nivel fácil: una jugada legal cualquiera.
public class RandomOpponent implements IOpponent {

    private final Random random;

    public RandomOpponent(Random random) {
        this.random = random;
    }

    @Override
    public Move chooseMove(IChessGame game) {
        List<Move> moves = game.legalMoves();
        return moves.get(random.nextInt(moves.size()));
    }
}
