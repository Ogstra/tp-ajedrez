package core.state;

import core.board.Color;

// State: cada estado sabe de quién es el turno, si la partida terminó y a qué estado
// pasa después de una jugada. Los finales (mate, ahogado y tablas) devuelven this.
public interface IGameState {
    Color currentColor();
    boolean isOver();
    IGameState afterMove();
}
