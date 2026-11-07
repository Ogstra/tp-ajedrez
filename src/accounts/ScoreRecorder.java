package accounts;

import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import ports.IGameStatusListener;
import ports.IMoveListener;

import java.util.ArrayList;
import java.util.List;

// Observer: se entera de cómo termina la partida y acredita el resultado a los usuarios, sin
// que ChessGame sepa que existen. Si un lado no tiene usuario (la IA o sin sesión), a ese lado
// no se le acredita nada. Al deshacer el final retira lo acreditado, así cada partida cuenta una vez.
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
    private List<Credit> recorded = new ArrayList<>();

    public ScoreRecorder(AccountService accounts, User whiteUser, User blackUser) {
        this.accounts = accounts;
        this.whiteUser = whiteUser;
        this.blackUser = blackUser;
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

    // Deshacer el final retira lo acreditado. Al rehacerlo, la partida vuelve a avisar el final
    // y se acredita de nuevo.
    @Override
    public void onUndo() {
        for (Credit credit : recorded) accounts.revert(credit.user, credit.result);
        recorded = new ArrayList<>();
    }

    private void credit(Result whiteResult, Result blackResult) {
        recorded = new ArrayList<>();
        if (whiteUser != null) recorded.add(new Credit(whiteUser, whiteResult));
        if (blackUser != null) recorded.add(new Credit(blackUser, blackResult));
        for (Credit credit : recorded) accounts.record(credit.user, credit.result);
    }

    // Esta clase solo se interesa por el final de la partida y por deshacer y rehacer.
    @Override public void onMoveMade(Position from, Position to, Piece piece, Piece captured) { }
    @Override public void onCastling(Color color, boolean kingside) { }
    @Override public void onPromotion(Color color, Position position, PieceType newPiece) { }
    @Override public void onRedo() { }
    @Override public void onInvalidMove(String reason) { }
    @Override public void onCheck(Color colorInCheck) { }
}
