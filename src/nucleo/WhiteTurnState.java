package nucleo;

public class WhiteTurnState implements IGameState {
    @Override
    public Color currentColor() { return Color.WHITE; }

    @Override
    public IGameState handleMove() { return new BlackTurnState(); }
}
