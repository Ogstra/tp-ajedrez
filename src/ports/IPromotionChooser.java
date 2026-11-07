package ports;

import core.board.Color;
import core.board.PieceType;


// Puerto de salida: el núcleo pregunta a qué pieza corona el peón. La consola pregunta
// por teclado, la ventana abre un diálogo y en los tests un stub devuelve siempre reina.
public interface IPromotionChooser {
    PieceType choose(Color color);
}
