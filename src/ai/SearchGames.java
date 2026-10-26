package ai;

import core.ChessGame;
import core.IChessGame;
import core.IDrawRule;
import core.ISpecialMove;
import core.PieceFactory;
import core.PieceType;
import core.special.CastlingMove;
import core.special.EnPassantMove;
import core.special.PromotionMove;
import ports.IGameStatusListener;
import ports.IMoveListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Arma la partida sobre la que la IA "piensa": una copia del tablero, sin
// listeners (no avisa a la pantalla) y con una coronación que no pregunta.
// Limitación conocida: la copia no tiene el historial, así que no sabe qué
// piezas ya se movieron (derechos de enroque) ni la última jugada (captura
// al paso). Eso solo afecta cuánto "ve" la IA; las reglas de verdad las
// aplica siempre la partida real cuando la IA juega su jugada.
public final class SearchGames {

    private SearchGames() { }

    public static IChessGame from(IChessGame game) {
        PieceFactory pieces = PieceFactory.standard();
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new EnPassantMove(),
            new PromotionMove(pieces, new FixedPromotionChooser(PieceType.QUEEN)));
        return new ChessGame(
            game.getBoard().cloneBoard(),
            new ArrayList<IMoveListener>(),
            new ArrayList<IGameStatusListener>(),
            specialMoves,
            new ArrayList<IDrawRule>(),
            game.getTurn());
    }
}
