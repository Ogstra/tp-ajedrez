package puertos;

import nucleo.Color;
import nucleo.PieceType;

// Puerto de salida (Clase 2): lo que el núcleo necesita del mundo exterior
// cuando un peón corona, sin saber cómo se resuelve. La consola pregunta por
// teclado, Swing abre un diálogo, y un test devuelve siempre reina (un stub).
public interface IPromotionChooser {
    PieceType choose(Color color);
}
