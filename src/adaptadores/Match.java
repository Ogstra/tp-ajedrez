package adaptadores;

import ai.OpponentController;
import nucleo.IChessGame;

// Lo que devuelve GameBuilder: la partida y, si juega la IA, quien la controla (null si no).
public final class Match {
    private final IChessGame game;
    private final OpponentController opponent;

    public Match(IChessGame game, OpponentController opponent) {
        this.game = game;
        this.opponent = opponent;
    }

    public IChessGame getGame() { return game; }
    public OpponentController getOpponent() { return opponent; }
}
