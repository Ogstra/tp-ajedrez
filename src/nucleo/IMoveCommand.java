package nucleo;

import puertos.IGameObserver;

import java.util.List;

// ICommand (execute/undo) más lo que ChessGame necesita saber de una jugada,
// sin preguntar de qué clase concreta es (antes había un instanceof):
//   touchedSquares: casillas de las que salió o a las que llegó una pieza.
//     Con eso se sabe si el rey o una torre ya se movieron (derechos de
//     enroque) recorriendo el historial, y deshacer los recupera solo.
//   announce: cada tipo de jugada avisa lo suyo al observer (polimorfismo
//     en vez de un if por tipo).
public interface IMoveCommand extends ICommand {
    List<Position> touchedSquares();
    void announce(IGameObserver observer);
}
