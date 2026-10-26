package core.draw;

import core.IDrawRule;
import core.IGameContext;

// Tablas por la regla de los 50 movimientos: 100 medias jugadas seguidas sin
// ninguna captura ni movimiento de peón. El límite se puede cambiar para probar.
public class FiftyMoveRule implements IDrawRule {

    private final int halfMoves;

    public FiftyMoveRule() {
        this(100);
    }

    public FiftyMoveRule(int halfMoves) {
        this.halfMoves = halfMoves;
    }

    @Override
    public String reason(IGameContext context) {
        return context.halfMoveClock() >= halfMoves ? "regla de los 50 movimientos" : null;
    }
}
