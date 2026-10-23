package adaptadores;

import nucleo.IChessGame;
import usuarios.ScoreRecorder;

// Arma una partida nueva con las reglas de siempre. La ventana lo usa cada vez que se aprieta Jugar,
// y lo implementa MainSwing (el composition root), así la ventana no conoce las reglas concretas.
public interface GameBuilder {
    IChessGame build(SwingChessView view, ScoreRecorder recorder);
}
