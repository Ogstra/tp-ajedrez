package core.draw;

import core.IDrawRule;
import core.IGameContext;

// Tablas por repetición: la misma posición, con el mismo bando al turno, tres
// veces. Simplificación: se ignoran los derechos de enroque y de captura al
// paso (las reglas oficiales los tienen en cuenta).
public class ThreefoldRepetitionRule implements IDrawRule {

    @Override
    public String reason(IGameContext context) {
        return context.occurrencesOfCurrentPosition() >= 3 ? "repetición de la posición tres veces" : null;
    }
}
