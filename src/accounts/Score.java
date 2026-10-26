package accounts;

// Value object inmutable: lo que lleva acumulado un jugador. Los puntos son
// los del ajedrez por torneos: 3 por victoria, 1 por empate, 0 por derrota.
public final class Score {
    public static final int POINTS_PER_WIN = 3;
    public static final int POINTS_PER_DRAW = 1;

    private final User user;
    private final int wins;
    private final int draws;
    private final int losses;

    public Score(User user, int wins, int draws, int losses) {
        this.user = user;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
    }

    public User getUser() { return user; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }
    public int getGames() { return wins + draws + losses; }
    public int getPoints() { return wins * POINTS_PER_WIN + draws * POINTS_PER_DRAW; }

    // Devuelve el puntaje nuevo; este objeto no cambia.
    public Score plus(Result result, int amount) {
        switch (result) {
            case WIN:  return new Score(user, Math.max(0, wins + amount), draws, losses);
            case DRAW: return new Score(user, wins, Math.max(0, draws + amount), losses);
            default:   return new Score(user, wins, draws, Math.max(0, losses + amount));
        }
    }

    @Override
    public String toString() {
        return user + " " + wins + "-" + draws + "-" + losses + " (" + getPoints() + " pts)";
    }
}
