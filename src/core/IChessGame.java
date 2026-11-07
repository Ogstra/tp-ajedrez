package core;

import core.board.Board;
import core.board.Color;
import core.board.Position;
import java.util.List;

// Puerto de entrada: lo que la consola, la ventana y la IA necesitan de una partida.
// Los adaptadores dependen de esta interfaz y no de ChessGame.
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
