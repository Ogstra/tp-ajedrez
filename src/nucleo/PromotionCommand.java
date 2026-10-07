package nucleo;

import puertos.IGameObserver;

import java.util.Arrays;
import java.util.List;

// Command (Clase 3), variante de MoveCommand para cuando un peón llega a
// la última fila. En vez de mover la pieza tal cual, la reemplaza por la
// pieza ya elegida (se la pasa PromotionMove, que es quien le preguntó al
// jugador). undo() la devuelve a ser un peón.
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
    public void announce(IGameObserver observer) {
        observer.onMoveMade(from, to, originalPawn, capturedPiece);
        observer.onPromotion(promotedPiece.getColor(), to, promotedPiece.getType());
    }
}
