package core.draw;

import core.IGameContext;

// Strategy para las condiciones de tablas. ChessGame las consulta después de cada jugada.
public interface IDrawRule {
    // El motivo de las tablas, o null si esta regla no las declara.
    String reason(IGameContext context);
}
