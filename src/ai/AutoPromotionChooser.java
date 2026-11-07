package ai;

import core.board.Color;
import core.board.PieceType;
import ports.IPromotionChooser;

// Decorator del chooser de la persona: cuando corona la IA elige reina sin preguntar, y
// cuando corona la persona le pregunta como siempre.
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
