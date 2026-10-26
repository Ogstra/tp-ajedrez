package accounts;

import core.Color;
import core.IChessGame;
import core.Piece;
import core.PieceType;
import core.Position;
import ports.IGameStatusListener;
import ports.IMoveListener;

import java.util.ArrayList;
import java.util.List;

// Observer (Clase 3): se entera de cómo termina la partida y acredita el resultado a los
// usuarios que juegan, sin que ChessGame sepa que existen los usuarios. Los usuarios quedan
// fijos al crearlo: solo tiene usuario el lado de la persona en una partida contra la IA (la
// IA nunca tiene), y en 1 contra 1 llegan los dos en null, así que no se acredita nada.
//
// Como la partida se puede deshacer y rehacer, también escucha eso: al deshacer el mate o
// las tablas retira lo que acreditó, y al rehacerlo (si la partida terminó de nuevo) lo
// vuelve a acreditar. Así cada partida cuenta una sola vez.
public class ScoreRecorder implements IGameStatusListener, IMoveListener {

    private static class Credit {
        final User user;
        final Result result;

        Credit(User user, Result result) {
            this.user = user;
            this.result = result;
        }
    }

    private final AccountService accounts;
    private final User whiteUser;
    private final User blackUser;
    private IChessGame game;
    private List<Credit> recorded = new ArrayList<>();
    private List<Credit> reverted = new ArrayList<>();

    public ScoreRecorder(AccountService accounts, User whiteUser, User blackUser) {
        this.accounts = accounts;
        this.whiteUser = whiteUser;
        this.blackUser = blackUser;
    }

    // Hace falta para saber, al rehacer, si la partida volvió a terminar.
    public void attachGame(IChessGame game) {
        this.game = game;
    }

    @Override
    public void onCheckmate(Color winner) {
        credit(winner == Color.WHITE ? Result.WIN : Result.LOSS, winner == Color.BLACK ? Result.WIN : Result.LOSS);
    }

    @Override
    public void onStalemate() {
        credit(Result.DRAW, Result.DRAW);
    }

    @Override
    public void onDraw(String reason) {
        credit(Result.DRAW, Result.DRAW);
    }

    @Override
    public void onUndo() {
        if (recorded.isEmpty()) return;
        for (Credit credit : recorded) accounts.revert(credit.user, credit.result);
        reverted = recorded;
        recorded = new ArrayList<>();
    }

    @Override
    public void onRedo() {
        // Solo se vuelve a acreditar si rehacer dejó la partida terminada; rehacer una
        // jugada anterior al mate no es un final.
        if (reverted.isEmpty() || game == null || !game.isGameOver()) return;
        for (Credit credit : reverted) accounts.record(credit.user, credit.result);
        recorded = reverted;
        reverted = new ArrayList<>();
    }

    private void credit(Result whiteResult, Result blackResult) {
        recorded = new ArrayList<>();
        reverted = new ArrayList<>();
        if (whiteUser != null) recorded.add(new Credit(whiteUser, whiteResult));
        if (blackUser != null) recorded.add(new Credit(blackUser, blackResult));
        for (Credit credit : recorded) accounts.record(credit.user, credit.result);
    }

    // Esta clase solo se interesa por el final de la partida y por deshacer y rehacer.
    @Override public void onMoveMade(Position from, Position to, Piece piece, Piece captured) { }
    @Override public void onCastling(Color color, boolean kingside) { }
    @Override public void onPromotion(Color color, Position position, PieceType newPiece) { }
    @Override public void onInvalidMove(String reason) { }
    @Override public void onCheck(Color colorInCheck) { }
}
