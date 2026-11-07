package ports;

import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;


// Puerto de salida para lo que pasa con las piezas. Está separado del estado de la
// partida (Interface Segregation).
public interface IMoveListener {
    void onMoveMade(Position from, Position to, Piece piece, Piece captured);
    void onCastling(Color color, boolean kingside);
    void onPromotion(Color color, Position position, PieceType newPiece);
    void onUndo();
    void onRedo();
}
