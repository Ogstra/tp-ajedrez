package ai;

import core.IChessGame;
import core.Move;

// Strategy (Clase 3) de dificultad: cada nivel decide la jugada de otra forma. El rival por LAN
// también es un oponente: en vez de pensar, espera la jugada por la red.
public interface IOpponent {
    // Elige una jugada para el bando que tiene el turno. Se llama solo si la partida no terminó.
    // Devuelve null si el oponente ya no puede jugar (el rival por LAN se fue o mandó algo que no
    // es una jugada legal). Las IA siempre devuelven una jugada legal.
    Move chooseMove(IChessGame game);
}
