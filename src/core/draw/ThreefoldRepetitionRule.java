package core.draw;

import core.IDrawRule;
import core.IGameContext;

// Tablas por repeticion: mismo tablero, turno, derechos de enroque y capturas
// al paso legales, tres veces. La identidad la calcula la partida con sus estrategias.
public class ThreefoldRepetitionRule implements IDrawRule {

    @Override
    public String reason(IGameContext context) {
        return context.occurrencesOfCurrentPosition() >= 3 ? "repetición de la posición tres veces" : null;
    }
}
