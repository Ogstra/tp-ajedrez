package core;

import core.draw.FiftyMoveRule;
import core.draw.InsufficientMaterialRule;
import core.draw.ThreefoldRepetitionRule;
import core.special.CastlingMove;
import core.special.EnPassantMove;
import core.special.PromotionMove;
import ports.IPromotionChooser;

import java.util.Arrays;
import java.util.List;

// Las reglas del ajedrez de siempre, armadas en un solo lugar. Las usan la consola, la ventana y
// la búsqueda de la IA: una regla especial o de tablas nueva se agrega acá y la ven todos (antes
// había que sumarla en tres lugares, y si faltaba en la IA, la IA no la conocía). ChessGame sigue
// sin saber cuáles son: las recibe por constructor. Los tests arman sus propias listas.
public final class StandardRules {

    private StandardRules() { }

    // Enroque, captura al paso y coronación. chooser decide a qué pieza se corona.
    public static List<ISpecialMove> specialMoves(PieceFactory pieces, IPromotionChooser chooser) {
        return Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new EnPassantMove(),
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
