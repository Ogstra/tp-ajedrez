package nucleo;

import java.util.List;

// Strategy (Clase 3) para las jugadas que no son "mover una pieza a una casilla
// que su movimiento ya permite": enroque, coronación, y una futura captura
// al paso. ChessGame las recibe por constructor y no sabe cuáles son.
// Agregar una regla nueva es una clase nueva que implementa esta interfaz
// más una línea en el composition root; ChessGame no se modifica (Open/Closed).
public interface ISpecialMove {

    // Casillas extra a las que la pieza de "from" podría ir por esta regla
    // (el rey puede ir a g1 por enroque aunque su movimiento normal no llegue).
    // ChessGame igual descarta las que dejarían al propio rey en jaque.
    List<Position> extraDestinations(IGameContext context, Position from);

    // Si esta regla maneja la jugada from -> to, devuelve su comando; si no
    // aplica, devuelve null y ChessGame prueba con la siguiente.
    IMoveCommand commandFor(IGameContext context, Position from, Position to);
}
