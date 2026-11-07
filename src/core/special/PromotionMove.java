package core.special;

import core.board.Board;
import core.board.Color;
import core.board.Piece;
import core.board.PieceFactory;
import core.board.PieceType;
import core.board.Position;
import core.command.IMoveCommand;
import core.command.PromotionCommand;
import core.IGameContext;
import ports.IPromotionChooser;

import java.util.Collections;
import java.util.List;

// Coronación: un peón que llega a la última fila se reemplaza por otra pieza. No agrega
// destinos, solo cambia el comando que se ejecuta. La pieza la elige IPromotionChooser.
public class PromotionMove implements ISpecialMove {

    private final PieceFactory pieces;
    private final IPromotionChooser chooser;

    public PromotionMove(PieceFactory pieces, IPromotionChooser chooser) {
        this.pieces = pieces;
        this.chooser = chooser;
    }

    @Override
    public List<Position> extraDestinations(IGameContext context, Position from) {
        return Collections.emptyList();
    }

    @Override
    public IMoveCommand commandFor(IGameContext context, Position from, Position to) {
        Board board = context.getBoard();
        Piece piece = board.pieceAt(from);
        if (piece == null || !piece.getType().equals(PieceType.PAWN)) return null;

        int lastRow = piece.getColor() == Color.WHITE ? board.getRows() - 1 : 0;
        if (to.getRow() != lastRow) return null;

        PieceType chosen = chooser.choose(piece.getColor());
        Piece promoted = pieces.create(chosen, piece.getColor());
        return new PromotionCommand(board, from, to, promoted);
    }
}
