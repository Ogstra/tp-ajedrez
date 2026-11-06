package adapters.swing;

import core.IChessGame;

// Arma una partida nueva con sus reglas. Lo implementa MainSwing, así la ventana no crea
// sus colaboradores con new.
public interface GameBuilder {
    IChessGame build(SwingChessView view);
}
