package nucleo;

// Jaque mate: currentColor es el bando al que le tocaba mover y no puede.
public class CheckmateState implements IGameState {
    private final Color mated;

    public CheckmateState(Color mated) {
        this.mated = mated;
    }

    @Override
    public Color currentColor() { return mated; }

    @Override
    public boolean isOver() { return true; }

    @Override
    public IGameState afterMove() { return this; }
}
