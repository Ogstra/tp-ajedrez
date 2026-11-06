package core.command;

import core.board.Position;
import ports.IMoveListener;

import java.util.List;

// Una jugada que se puede deshacer, con lo que ChessGame necesita saber de ella.
// touchedSquares sirve para saber si el rey o una torre ya se movieron, mirando el
// historial. announce deja que cada tipo de jugada avise lo suyo, sin ifs por tipo.
public interface IMoveCommand extends ICommand {
    List<Position> touchedSquares();
    void announce(IMoveListener observer);
    // true si la jugada mueve un peón o captura (reinicia el contador de los 50 movimientos).
    boolean resetsHalfMoveClock();
}
