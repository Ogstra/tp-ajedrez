package nucleo;

// Lo que una regla especial (ISpecialMove) puede consultar de la partida,
// sin conocer a ChessGame (DIP: las reglas dependen de esta abstracción).
public interface IGameContext {
    Board getBoard();

    // true si, en alguna jugada que sigue en pie (no deshecha), una pieza
    // salió de esta casilla o llegó a ella. Una casilla sin tocar sigue
    // teniendo a la pieza con la que arrancó la partida.
    boolean hasBeenTouched(Position pos);

    // true si alguna pieza rival de "color" ataca la casilla.
    boolean isAttacked(Position pos, Color color);
}
