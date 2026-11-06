package adapters.swing;

import ai.OpponentLevel;

// Arma una partida nueva con sus reglas y, si corresponde, la IA. Lo implementa MainSwing,
// así la ventana no crea sus colaboradores con new.
public interface GameBuilder {
    // level NONE es una partida entre dos personas.
    Match build(SwingChessView view, OpponentLevel level);
}
