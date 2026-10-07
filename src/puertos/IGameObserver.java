package puertos;

import nucleo.Color;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;

// Observer (Clase 3), y a la vez puerto de salida (Clase 2): el núcleo
// (ChessGame) notifica lo que pasó sin conocer quién escucha. El adaptador
// de consola implementa esto para imprimir mensajes; podría existir un
// adaptador web o gráfico implementando la misma interfaz sin tocar el núcleo.
public interface IGameObserver {
    void onMoveMade(Position from, Position to, Piece piece, Piece captured);
    void onInvalidMove(String reason);
    void onCheck(Color colorInCheck);
    void onCheckmate(Color winnerColor);
    void onStalemate();
    void onUndo();
    void onCastling(Color color, boolean cortoEnEnroque);
    void onPromotion(Color color, Position pos, PieceType nuevaPieza);
}
