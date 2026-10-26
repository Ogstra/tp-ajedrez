package ports;

import core.Color;
import core.Piece;
import core.PieceType;
import core.Position;

// Puerto de salida (Clase 2), parte 1: lo que pasa con las piezas.
// Interface Segregation (Clase 3): un adaptador que solo quiere saber de
// movimientos no está obligado a implementar el estado de la partida.
public interface IMoveListener {
    void onMoveMade(Position from, Position to, Piece piece, Piece captured);
    void onCastling(Color color, boolean kingside);
    void onPromotion(Color color, Position position, PieceType newPiece);
    void onUndo();
    void onRedo();
}
