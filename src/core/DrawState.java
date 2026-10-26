package core;

// Tablas: estado terminal, como el mate y el ahogado.
public class DrawState implements IGameState {
    private final Color toMove;
    private final String reason;

    public DrawState(Color toMove, String reason) {
        this.toMove = toMove;
        this.reason = reason;
    }

    public String getReason() { return reason; }

    @Override
    public Color currentColor() { return toMove; }

    @Override
    public boolean isOver() { return true; }

    @Override
    public IGameState afterMove() { return this; }
}
