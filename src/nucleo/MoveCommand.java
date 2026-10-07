package nucleo;

import puertos.IGameObserver;

import java.util.Arrays;
import java.util.List;

// Mismo MoveCommand de la Tarea 03: guarda la pieza capturada recién al
// ejecutar (todavía no se sabe qué hay en destino hasta ese momento), y con
// eso alcanza para deshacer el movimiento exacto.
public class MoveCommand implements IMoveCommand {
    private final Board board;
    private final Position from;
    private final Position to;
    private Piece movedPiece;
    private Piece capturedPiece;

    public MoveCommand(Board board, Position from, Position to) {
        this.board = board;
        this.from = from;
        this.to = to;
    }

    public Position getFrom() { return from; }
    public Position getTo() { return to; }
    public Piece getCapturedPiece() { return capturedPiece; }

    @Override
    public void execute() {
        this.movedPiece = board.pieceAt(from);
        this.capturedPiece = board.pieceAt(to);
        board.movePiece(from, to);
    }

    @Override
    public void undo() {
        board.movePiece(to, from);
        if (capturedPiece != null) {
            board.placePiece(capturedPiece, to);
        }
    }

    @Override
    public List<Position> touchedSquares() {
        return Arrays.asList(from, to);
    }

    @Override
    public void announce(IGameObserver observer) {
        observer.onMoveMade(from, to, movedPiece, capturedPiece);
    }
}
