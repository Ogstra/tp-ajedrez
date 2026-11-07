package adapters.swing;

import core.board.Color;
import ai.OpponentLevel;
import accounts.User;

// Arma una partida nueva con sus reglas, el registro de puntos y, si corresponde, la IA.
// Lo implementa MainSwing, así la ventana no crea sus colaboradores con new.
public interface GameBuilder {
    // level NONE = dos personas; aiColor solo importa si juega la IA.
    Match build(SwingChessView view, User whiteUser, User blackUser, OpponentLevel level, Color aiColor);
}
