package adapters;

import ai.OpponentController;
import core.IChessGame;

// La partida y su controlador de IA (null cuando juegan dos personas).
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
