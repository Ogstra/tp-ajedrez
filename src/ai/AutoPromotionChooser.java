package ai;

import core.Color;
import core.PieceType;
import ports.IPromotionChooser;

// Decorador del chooser humano: cuando corona el bando de la IA elige reina
// sin preguntar; cuando corona el humano, le pregunta como siempre.
public class AutoPromotionChooser implements IPromotionChooser {

    private final Color opponentColor;
    private final IPromotionChooser human;

    public AutoPromotionChooser(Color opponentColor, IPromotionChooser human) {
        this.opponentColor = opponentColor;
        this.human = human;
    }

    @Override
    public PieceType choose(Color color) {
        if (color == opponentColor) return PieceType.QUEEN;
        return human.choose(color);
    }
}
