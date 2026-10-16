package ai;

import nucleo.Color;
import nucleo.IChessGame;
import nucleo.Move;

// Decide cuándo juega la IA: cuando la partida sigue y es el turno de su color.
// chooseMove y play están separados para que la ventana pueda pensar en otro
// hilo y aplicar la jugada después, en el hilo de la interfaz.
public class OpponentController {

    private final IChessGame game;
    private final IOpponent opponent;
    private final Color color;

    public OpponentController(IChessGame game, IOpponent opponent, Color color) {
        this.game = game;
        this.opponent = opponent;
        this.color = color;
    }

    public Color getColor() { return color; }

    public boolean isItsTurn() {
        return !game.isGameOver() && game.getTurn() == color;
    }

    public Move chooseMove() {
        return opponent.chooseMove(game);
    }

    public void play(Move move) {
        game.tryMove(move.getFrom(), move.getTo());
    }

    // Piensa y juega, todo junto. Devuelve false si no era su turno.
    public boolean playIfItsTurn() {
        if (!isItsTurn()) return false;
        play(chooseMove());
        return true;
    }
}
