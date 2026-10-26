package ai;

import core.Color;
import core.IChessGame;
import core.Move;
import core.MoveOutcome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Nivel difícil: minimax que evalúa con el material y un poco de criterio
// posicional (ver Evaluation).
// "depth" cuenta las jugadas que mira, incluida la suya: con 2 mira su jugada
// y la respuesta del rival. Juega en una partida de búsqueda propia
// (SearchGames) y la deshace con undo() después de cada rama.
public class MinimaxOpponent implements IOpponent {

    private static final int MATE = 100000;

    private final int depth;
    private final Random random;

    public MinimaxOpponent(int depth, Random random) {
        this.depth = depth;
        this.random = random;
    }

    @Override
    public Move chooseMove(IChessGame game) {
        IChessGame search = SearchGames.from(game);
        Color me = game.getTurn();

        int bestValue = Integer.MIN_VALUE;
        List<Move> best = new ArrayList<>();
        // La copia no conserva derechos: la jugada elegida debe ser legal en la partida real.
        List<Move> realLegalMoves = game.legalMoves();
        for (Move move : search.legalMoves()) {
            if (!realLegalMoves.contains(move)) continue;
            int value = scoreMove(search, move, depth, me);
            if (value > bestValue) {
                bestValue = value;
                best.clear();
            }
            if (value == bestValue) {
                best.add(move);
            }
        }
        // Si solo hay una captura al paso que la copia no conoce, se elige entre las reales.
        if (best.isEmpty()) return realLegalMoves.get(random.nextInt(realLegalMoves.size()));
        return best.get(random.nextInt(best.size()));
    }

    // Juega la jugada, mira cuánto vale la posición resultante y la deshace.
    private int scoreMove(IChessGame game, Move move, int remainingDepth, Color me) {
        Color mover = game.getTurn();
        MoveOutcome outcome = game.tryMove(move.getFrom(), move.getTo());

        int value;
        if (outcome == MoveOutcome.CHECKMATE) {
            // Se prefiere el mate más rápido: queda más profundidad sin usar.
            value = mover == me ? MATE + remainingDepth : -(MATE + remainingDepth);
        } else if (outcome == MoveOutcome.STALEMATE || outcome == MoveOutcome.DRAW) {
            value = 0;
        } else if (remainingDepth <= 1) {
            value = Evaluation.score(game.getBoard(), me);
        } else {
            value = bestValue(game, remainingDepth - 1, me);
        }

        game.undo();
        return value;
    }

    // El que tiene el turno elige lo mejor para él: yo maximizo, el rival minimiza.
    private int bestValue(IChessGame game, int remainingDepth, Color me) {
        boolean maximizing = game.getTurn() == me;
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : game.legalMoves()) {
            int value = scoreMove(game, move, remainingDepth, me);
            best = maximizing ? Math.max(best, value) : Math.min(best, value);
        }
        return best;
    }
}
