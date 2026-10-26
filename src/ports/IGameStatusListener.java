package ports;

import core.Color;

// Puerto de salida (Clase 2), parte 2: el estado de la partida. Igual que IMoveListener, cada
// adaptador sobrescribe solo los avisos que le interesan.
public interface IGameStatusListener {
    default void onInvalidMove(String reason) { }
    default void onCheck(Color colorInCheck) { }
    default void onCheckmate(Color winner) { }
    default void onStalemate() { }
    default void onDraw(String reason) { }
}
