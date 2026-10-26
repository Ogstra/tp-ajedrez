package ai;

import core.IChessGame;
import core.Move;

// Strategy (Clase 3) de dificultad: cada nivel decide la jugada de otra forma.
public interface IOpponent {
    // Elige una jugada legal para el bando que tiene el turno. Se llama solo
    // si la partida no terminó, así que siempre hay alguna.
    Move chooseMove(IChessGame game);
}
