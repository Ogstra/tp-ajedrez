package accounts;

import core.Color;
import ports.IGameStatusListener;
import ports.IMoveListener;

import java.util.ArrayList;
import java.util.List;

// Observer (Clase 3): se entera de cómo termina la partida y acredita el resultado a los
// usuarios que juegan, sin que ChessGame sepa que existen los usuarios. Los usuarios quedan
// fijos al crearlo: solo tiene usuario el lado de la persona en una partida contra la IA (la
// IA nunca tiene), y en 1 contra 1 llegan los dos en null, así que no se acredita nada.
//
// Como la partida se puede deshacer y rehacer, también escucha eso: al deshacer el mate o las
// tablas retira lo que acreditó. Al rehacerlo, la partida vuelve a avisar el mate o las tablas
// y se acredita de nuevo. Así cada partida cuenta una sola vez.
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
}
