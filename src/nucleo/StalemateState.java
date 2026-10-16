package nucleo;

// Ahogado: el bando que tiene el turno no está en jaque pero no tiene jugadas.
public class StalemateState implements IGameState {
    private final Color toMove;

    public StalemateState(Color toMove) {
        this.toMove = toMove;
    }

    @Override
    public Color currentColor() { return toMove; }

    @Override
    public boolean isOver() { return true; }

    @Override
    public IGameState afterMove() { return this; }
}
