package nucleo;

public class WhiteTurnState implements IGameState {
    @Override
    public Color currentColor() { return Color.WHITE; }

    @Override
    public boolean isOver() { return false; }

    @Override
    public IGameState afterMove() { return new BlackTurnState(); }
}
