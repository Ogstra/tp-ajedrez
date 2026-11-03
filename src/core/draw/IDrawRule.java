package core.draw;

import core.IGameContext;

// Strategy (Clase 3) para las condiciones de tablas. ChessGame las recibe por
// constructor y las consulta después de cada jugada. Una regla nueva es una
// clase nueva más una línea en StandardRules.
public interface IDrawRule {
    // El motivo de las tablas, o null si esta regla no las declara.
    String reason(IGameContext context);
}
