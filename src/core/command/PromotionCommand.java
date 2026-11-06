package core.command;

import core.board.Board;
import core.board.Piece;
import core.board.Position;
import ports.IMoveListener;

import java.util.Arrays;
import java.util.List;

// Comando de la coronación: mueve el peón y lo reemplaza por la pieza elegida.
// undo() vuelve a dejar el peón.
public class PromotionCommand implements IMoveCommand {
    private final Board board;
    private final Position from;
    private final Position to;
    private final Piece promotedPiece;
    private Piece originalPawn;
    private Piece capturedPiece;

    public PromotionCommand(Board board, Position from, Position to, Piece promotedPiece) {
        this.board = board;
        this.from = from;
        this.to = to;
        this.promotedPiece = promotedPiece;
    }

    @Override
    public void execute() {
        this.originalPawn = board.pieceAt(from);
        this.capturedPiece = board.pieceAt(to);
        board.movePiece(from, to);
        board.placePiece(promotedPiece, to); // reemplaza al peón recién movido por la pieza elegida
    }

    @Override
    public void undo() {
        board.removePiece(to);
        board.placePiece(originalPawn, from);
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
        listener.onMoveMade(from, to, originalPawn, capturedPiece);
        listener.onPromotion(promotedPiece.getColor(), to, promotedPiece.getType());
    }
    @Override
    public boolean resetsHalfMoveClock() {
        return true;
    }
}
