package ai;

import java.util.Random;

// Factory (Clase 3) de oponentes por nivel.
public final class Opponents {

    private Opponents() { }

    // Devuelve null para NONE (dos jugadores humanos).
    public static IOpponent create(OpponentLevel level, Random random) {
        switch (level) {
            case EASY:   return new RandomOpponent(random);
            case MEDIUM: return new GreedyOpponent(random);
            case HARD:   return new MinimaxOpponent(2, random);
            default:     return null;
        }
    }
}
