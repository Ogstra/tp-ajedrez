package puertos;

import nucleo.Color;

// Puerto de salida (Clase 2), parte 2: el estado de la partida.
public interface IGameStatusListener {
    void onInvalidMove(String reason);
    void onCheck(Color colorInCheck);
    void onCheckmate(Color winner);
    void onStalemate();
}
