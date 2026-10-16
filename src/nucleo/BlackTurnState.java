package nucleo;

public class BlackTurnState implements IGameState {
    @Override
    public Color currentColor() { return Color.BLACK; }

    @Override
    public boolean isOver() { return false; }

    @Override
    public IGameState afterMove() { return new WhiteTurnState(); }
}
