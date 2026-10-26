package adapters;

import ai.OpponentController;
import core.IChessGame;
import network.LanOpponent;

// Lo que devuelve GameBuilder: la partida, quien controla al oponente (null en 1 contra 1) y,
// si es por LAN, el rival de la otra PC (null en las demás).
public final class Match {
    private final IChessGame game;
    private final OpponentController opponent;
    private final LanOpponent lan;

    public Match(IChessGame game, OpponentController opponent) {
        this(game, opponent, null);
    }

    public Match(IChessGame game, OpponentController opponent, LanOpponent lan) {
        this.game = game;
        this.opponent = opponent;
        this.lan = lan;
    }

    public IChessGame getGame() { return game; }
    public OpponentController getOpponent() { return opponent; }
    public LanOpponent getLan() { return lan; }
}
