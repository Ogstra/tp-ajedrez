package adapters.swing;

import core.IChessGame;

// Arma una partida nueva con sus reglas. La ventana lo usa cada vez que empieza una partida y lo
// implementa MainSwing (el composition root), así la ventana no crea colaboradores con new
// (DI nivel 4).
public interface GameBuilder {
    IChessGame build(SwingChessView view);
}
