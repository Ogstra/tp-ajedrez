package ai;

import core.board.PieceFactory;
import core.board.PieceType;
import core.draw.IDrawRule;
import core.ChessGame;
import core.IChessGame;
import core.StandardRules;
import ports.IGameStatusListener;
import ports.IMoveListener;

import java.util.ArrayList;

// Arma la partida sobre la que piensa la IA: una copia del tablero, sin listeners para
// no avisar a la pantalla y con una coronación que no pregunta. La copia no tiene el
// historial, así que no sabe si el rey o las torres ya se movieron; por eso
// MinimaxOpponent solo elige jugadas que también son legales en la partida real.
public final class SearchGames {
    private SearchGames() { }

    public static IChessGame from(IChessGame game) {
        PieceFactory pieces = PieceFactory.standard();
        return new ChessGame(
            game.getBoard().cloneBoard(),
            new ArrayList<IMoveListener>(),
            new ArrayList<IGameStatusListener>(),
            StandardRules.specialMoves(pieces, new FixedPromotionChooser(PieceType.QUEEN)),
            new ArrayList<IDrawRule>(),
            game.getTurn());
    }
}
