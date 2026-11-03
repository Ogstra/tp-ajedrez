package core.draw;

import core.IGameContext;

// Tablas por repetición: la misma posición, con el mismo bando al turno, tres
// veces. No se tienen en cuenta los derechos de enroque (las reglas oficiales
// sí los consideran).
public class ThreefoldRepetitionRule implements IDrawRule {

    @Override
    public String reason(IGameContext context) {
        return context.occurrencesOfCurrentPosition() >= 3 ? "repetición de la posición tres veces" : null;
    }
}
