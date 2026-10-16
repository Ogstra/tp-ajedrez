package nucleo;

import java.util.List;

// Puerto de entrada (Clase 2): lo único que el mundo exterior (consola,
// ventana, IA) necesita saber de una partida. Los adaptadores dependen de
// esta interfaz, no de ChessGame (DIP, Clase 3).
public interface IChessGame {
    MoveOutcome tryMove(Position from, Position to);
    boolean undo();
    boolean redo();
    Color getTurn();
    boolean isGameOver();
    Board getBoard();

    // Todas las jugadas legales del bando que tiene el turno (vacía si la partida terminó).
    List<Move> legalMoves();
}
