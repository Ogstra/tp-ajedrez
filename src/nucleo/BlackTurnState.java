package nucleo;

public class BlackTurnState implements IGameState {
    @Override
    public Color currentColor() { return Color.BLACK; }

    @Override
    public IGameState handleMove() { return new WhiteTurnState(); }
}
