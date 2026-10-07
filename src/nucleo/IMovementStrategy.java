package nucleo;

import java.util.List;

// Strategy (Clase 3): cada tipo de pieza sabe moverse distinto, pero todas
// se usan de la misma forma desde Piece. Composición, no herencia:
// Piece no es subclase de "PiezaTorre" ni nada por el estilo, TIENE una
// referencia a esta interfaz.
public interface IMovementStrategy {
    // Devuelve las casillas a las que la pieza podría moverse (pseudo-legal:
    // respeta cómo se mueve la pieza y el bloqueo/captura, pero todavía no
    // sabe si ese movimiento deja al propio rey en jaque: eso lo filtra
    // ChessGame, que es quien conoce el estado completo de la partida).
    List<Position> possibleMoves(Board board, Position from, Color color);

    // Casillas que la pieza "ataca" (a donde podría capturar). Para casi todas
    // las piezas es lo mismo que possibleMoves, por eso es un método default.
    // El peón es la excepción: avanza sin atacar y ataca en diagonal aunque
    // la casilla esté vacía (lo necesita el enroque, que no puede pasar por
    // una casilla atacada). PawnMovementStrategy lo redefine.
    default List<Position> attackedSquares(Board board, Position from, Color color) {
        return possibleMoves(board, from, color);
    }
}
