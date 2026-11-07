package core.command;

import core.Move;
import core.board.Board;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import ports.IMoveListener;

import java.util.Arrays;
import java.util.List;

// Movimiento común. Al ejecutarse guarda la pieza capturada, si la hay, para poder deshacerlo.
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
    public void announce(IMoveListener listener) {
        listener.onMoveMade(from, to, movedPiece, capturedPiece);
    }
    @Override
    public Move getMove() {
        return new Move(from, to);
    }
    @Override
    public boolean resetsHalfMoveClock() {
        return movedPiece.getType().equals(PieceType.PAWN) || capturedPiece != null;
    }
}
