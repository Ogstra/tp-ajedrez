package ai;

import core.IChessGame;
import core.Move;

// Strategy de dificultad: cada nivel elige la jugada de otra forma.
public interface IOpponent {
    // Una jugada legal para el bando que tiene el turno. Se llama solo si la partida sigue.
    Move chooseMove(IChessGame game);
}
