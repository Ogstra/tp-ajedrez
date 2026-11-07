package core.movement;

import core.board.Board;
import core.board.Color;
import core.board.Position;
import java.util.List;

// Strategy: cada tipo de pieza se mueve distinto, pero Piece las usa a todas igual.
public interface IMovementStrategy {
    // Casillas a las que la pieza podría moverse, con bloqueos y capturas. No mira si
    // la jugada deja al propio rey en jaque; eso lo filtra ChessGame.
    List<Position> possibleMoves(Board board, Position from, Color color);

    // Casillas que la pieza ataca. Para casi todas es lo mismo que possibleMoves; el peón
    // lo redefine porque ataca en diagonal aunque la casilla esté vacía.
    default List<Position> attackedSquares(Board board, Position from, Color color) {
        return possibleMoves(board, from, color);
    }
}
