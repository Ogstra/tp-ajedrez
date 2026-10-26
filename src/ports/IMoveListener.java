package ports;

import core.Color;
import core.Piece;
import core.PieceType;
import core.Position;

// Puerto de salida (Clase 2), parte 1: lo que pasa con las piezas.
// Interface Segregation (Clase 3): un adaptador que solo quiere saber de
// movimientos no está obligado a implementar el estado de la partida.
// Los métodos traen una implementación vacía: cada adaptador sobrescribe solo lo que le
// interesa (ScoreRecorder, por ejemplo, solo deshacer), sin una lista de métodos vacíos.
public interface IMoveListener {
    default void onMoveMade(Position from, Position to, Piece piece, Piece captured) { }

    // kingFrom y kingTo son las casillas del rey, para quien necesite repetir la jugada (la red).
    default void onCastling(Color color, boolean kingside, Position kingFrom, Position kingTo) { }

    default void onPromotion(Color color, Position position, PieceType newPiece) { }
    default void onUndo() { }
    default void onRedo() { }
}
