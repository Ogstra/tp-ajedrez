package nucleo;

import puertos.IMoveListener;

import java.util.Arrays;
import java.util.List;

// Command (Clase 3) de la captura al paso: el peón se mueve en diagonal a una
// casilla vacía y el peón capturado, que está al lado, sale del tablero.
public class EnPassantCommand implements IMoveCommand {
    private final Board board;
    private final Position from;
    private final Position to;
    private final Position capturedPosition;
    private Piece movedPawn;
    private Piece capturedPawn;

    public EnPassantCommand(Board board, Position from, Position to, Position capturedPosition) {
        this.board = board;
        this.from = from;
        this.to = to;
        this.capturedPosition = capturedPosition;
    }

    @Override
    public void execute() {
        this.movedPawn = board.pieceAt(from);
        this.capturedPawn = board.pieceAt(capturedPosition);
        board.removePiece(capturedPosition);
        board.movePiece(from, to);
    }

    @Override
    public void undo() {
        board.movePiece(to, from);
        board.placePiece(capturedPawn, capturedPosition);
    }

    @Override
    public List<Position> touchedSquares() {
        return Arrays.asList(from, to, capturedPosition);
    }

    @Override
    public Move getMove() {
        return new Move(from, to);
    }

    @Override
    public boolean resetsHalfMoveClock() {
        return true;
    }

    @Override
    public void announce(IMoveListener listener) {
        listener.onMoveMade(from, to, movedPawn, capturedPawn);
    }
}
