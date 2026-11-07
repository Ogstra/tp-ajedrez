package core.special;

import core.IGameContext;
import core.board.Position;
import core.command.IMoveCommand;
import java.util.List;

// Strategy para las jugadas especiales: enroque, coronación y captura al paso. Una regla
// nueva es una clase que implementa esta interfaz y una línea en StandardRules.
public interface ISpecialMove {

    // Destinos extra que da esta regla, como g1 para el rey por el enroque.
    List<Position> extraDestinations(IGameContext context, Position from);

    // El comando de la jugada si esta regla la maneja, o null para probar con la siguiente.
    IMoveCommand commandFor(IGameContext context, Position from, Position to);
}
