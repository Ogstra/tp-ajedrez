package core;

// Lo que una regla (ISpecialMove, IDrawRule) puede consultar de la partida,
// sin conocer a ChessGame (DIP: las reglas dependen de esta abstracción).
public interface IGameContext {
    Board getBoard();

    // true si, en alguna jugada que sigue en pie (no deshecha), una pieza
    // salió de esta casilla o llegó a ella. Una casilla sin tocar sigue
    // teniendo a la pieza con la que arrancó la partida.
    boolean hasBeenTouched(Position pos);

    // true si alguna pieza rival de "color" ataca la casilla.
    boolean isAttacked(Position pos, Color color);

    // La última jugada que sigue en pie, o null si todavía no se jugó ninguna.
    Move lastMove();

    // Cuántas medias jugadas seguidas (de las últimas) no movieron un peón ni capturaron.
    int halfMoveClock();

    // Cuántas veces apareció la posición actual (con el mismo bando al turno), contando esta.
    int occurrencesOfCurrentPosition();
}
