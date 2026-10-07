package nucleo;

// State (Clase 3): cada estado sabe de quién es el turno y a qué estado
// transicionar. Agregar una fase nueva (por ejemplo un futuro estado de
// jaque) sería agregar una clase, sin tocar las que ya existen.
public interface IGameState {
    Color currentColor();
    IGameState handleMove();
}
