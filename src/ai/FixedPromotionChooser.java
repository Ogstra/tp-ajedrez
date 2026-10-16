package ai;

import nucleo.Color;
import nucleo.PieceType;
import puertos.IPromotionChooser;

// Puerto de salida (Clase 2) resuelto sin preguntar a nadie: siempre la misma pieza.
// Lo usa la partida de búsqueda de la IA.
public class FixedPromotionChooser implements IPromotionChooser {

    private final PieceType choice;

    public FixedPromotionChooser(PieceType choice) {
        this.choice = choice;
    }

    @Override
    public PieceType choose(Color color) {
        return choice;
    }
}
