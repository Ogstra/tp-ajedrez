package core;

import core.board.PieceFactory;
import core.draw.IDrawRule;
import core.special.ISpecialMove;
import core.draw.FiftyMoveRule;
import core.draw.InsufficientMaterialRule;
import core.draw.ThreefoldRepetitionRule;
import core.special.CastlingMove;
import core.special.PromotionMove;
import ports.IPromotionChooser;

import java.util.Arrays;
import java.util.List;

// Factory (Clase 3) de las reglas del ajedrez, armadas en un solo lugar. Las usan la consola
// y la ventana: una regla especial o de tablas nueva se agrega acá y la ven las dos. ChessGame
// no sabe cuáles son: las recibe por constructor. Los tests arman sus propias listas.
public final class StandardRules {

    private StandardRules() { }

    // Enroque y coronación. chooser decide a qué pieza se corona.
    public static List<ISpecialMove> specialMoves(PieceFactory pieces, IPromotionChooser chooser) {
        return Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(pieces, chooser));
    }

    // Tablas por material insuficiente, por la regla de los 50 movimientos y por repetición.
    public static List<IDrawRule> drawRules() {
        return Arrays.<IDrawRule>asList(
            new InsufficientMaterialRule(),
            new FiftyMoveRule(),
            new ThreefoldRepetitionRule());
    }
}
