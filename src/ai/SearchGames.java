package ai;

import core.IChessGame;
import core.PieceFactory;
import core.PieceType;
import core.StandardRules;

// Copia independiente sin listeners: conserva derechos, ultima jugada, reloj y repeticiones.
// La coronacion de la busqueda es automatica; las jugadas de la copia no afectan a la real.
public final class SearchGames {
    private SearchGames() { }

    public static IChessGame from(IChessGame game) {
        return game.copy(StandardRules.specialMoves(PieceFactory.standard(),
            new FixedPromotionChooser(PieceType.QUEEN)));
    }
}
