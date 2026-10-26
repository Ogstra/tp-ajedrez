package core.special;

import core.Board;
import core.Color;
import core.IGameContext;
import core.IMoveCommand;
import core.Move;
import core.ISpecialMove;
import core.Piece;
import core.PieceFactory;
import core.PieceType;
import core.Position;
import core.PromotionCommand;
import ports.IPromotionChooser;

import java.util.Collections;
import java.util.List;

// Regla de coronación: un peón que llega a la última fila se reemplaza por
// otra pieza. No agrega destinos nuevos (el peón ya llega por su movimiento
// normal); solo cambia qué comando se ejecuta. A qué pieza corona se lo
// pregunta al puerto IPromotionChooser, que cada adaptador resuelve a su manera.
public class PromotionMove implements ISpecialMove {

    private final PieceFactory pieces;
    private final IPromotionChooser chooser;

    public PromotionMove(PieceFactory pieces, IPromotionChooser chooser) {
        this.pieces = pieces;
        this.chooser = chooser;
    }

    @Override
    public List<Move> positionState(IGameContext context) {
        return Collections.emptyList(); // no deja un derecho pendiente entre jugadas
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
