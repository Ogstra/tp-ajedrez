package ports;

import core.board.Color;


// Puerto de salida para el estado de la partida: jaque, mate, ahogado, tablas y abandono.
public interface IGameStatusListener {
    void onInvalidMove(String reason);
    void onCheck(Color colorInCheck);
    void onCheckmate(Color winner);
    void onStalemate();
    void onDraw(String reason);
    void onResignation(Color winner);
}
