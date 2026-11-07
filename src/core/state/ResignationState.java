package core.state;

import core.board.Color;

// Abandono: el bando que tenía el turno se rindió. Es un estado final, como el mate.
public class ResignationState implements IGameState {
    private final Color resigned;

    public ResignationState(Color resigned) {
        this.resigned = resigned;
    }

    @Override
    public Color currentColor() { return resigned; }

    @Override
    public boolean isOver() { return true; }

    @Override
    public IGameState afterMove() { return this; }
}
