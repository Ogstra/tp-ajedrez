package adaptadores;

import ai.OpponentLevel;
import nucleo.Color;
import usuarios.User;

// Arma una partida nueva con todo lo que necesita: el juego con sus reglas, quién acredita los
// puntos y, si corresponde, la IA. La ventana lo usa cada vez que se aprieta Jugar y lo implementa
// MainSwing (el composition root), así la ventana no crea colaboradores con new (DI nivel 4).
public interface GameBuilder {
    // level NONE = dos personas; aiColor solo importa si juega la IA.
    Match build(SwingChessView view, User whiteUser, User blackUser, OpponentLevel level, Color aiColor);
}
