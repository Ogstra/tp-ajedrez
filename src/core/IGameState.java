package core;

// State (Clase 3): cada estado sabe de quién es el turno, si la partida
// terminó y a qué estado pasa después de una jugada. Los estados terminales
// (mate, ahogado) devuelven this: de ahí no se sale jugando, solo deshaciendo.
public interface IGameState {
    Color currentColor();
    boolean isOver();
    IGameState afterMove();
}
