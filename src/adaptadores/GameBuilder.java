package adaptadores;

import ai.OpponentLevel;
import nucleo.Color;
import red.LanOpponent;
import usuarios.User;

// Arma una partida nueva con todo lo que necesita: el juego con sus reglas, quién acredita los
// puntos y, si corresponde, la IA. La ventana lo usa cada vez que se aprieta Jugar y lo implementa
// MainSwing (el composition root), así la ventana no crea colaboradores con new (DI nivel 4).
public interface GameBuilder {
    // level NONE = sin IA; aiColor solo importa si juega la IA. lan es el rival de la otra PC
    // en una partida por LAN, y null en las demás.
    Match build(SwingChessView view, User whiteUser, User blackUser, OpponentLevel level, Color aiColor, LanOpponent lan);
}
