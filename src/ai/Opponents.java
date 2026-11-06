package ai;

import java.util.Random;

// Factory de oponentes por nivel.
public final class Opponents {
    private Opponents() { }

    // Devuelve null para NONE, que es una partida entre dos personas.
    public static IOpponent create(OpponentLevel level, Random random) {
        switch (level) {
            case EASY:   return new RandomOpponent(random);
            case MEDIUM: return new GreedyOpponent(random);
            case HARD:   return new MinimaxOpponent(2, random);
            default:     return null;
        }
    }
}
