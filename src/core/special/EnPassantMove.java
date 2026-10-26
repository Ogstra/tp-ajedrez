package core.special;

import core.Board;
import core.Color;
import core.EnPassantCommand;
import core.IGameContext;
import core.IMoveCommand;
import core.ISpecialMove;
import core.Move;
import core.Piece;
import core.PieceType;
import core.Position;

import java.util.ArrayList;
import java.util.List;

// Regla de captura al paso: si el peón rival acaba de avanzar dos casillas y
// quedó al lado de mi peón, puedo capturarlo como si hubiera avanzado una.
// Solo vale en la jugada inmediata. El mismo comando quita el peon capturado
// tanto en la partida como en la simulacion de legalidad.
public class EnPassantMove implements ISpecialMove {

    @Override
    public List<Move> positionState(IGameContext context) {
        List<Move> rights = new ArrayList<>();
        Move last = context.lastMove();
        if (last == null || Math.abs(last.getTo().getRow() - last.getFrom().getRow()) != 2) return rights;
        Piece moved = context.getBoard().pieceAt(last.getTo());
        if (moved == null || !moved.getType().equals(PieceType.PAWN)) return rights;
        List<Move> legal = null;
        for (Position from : context.getBoard().positionsOfColor(moved.getColor().opposite())) {
            Position target = targetSquare(context, from);
            if (target != null) {
                if (legal == null) legal = context.legalMoves();
                Move capture = new Move(from, target);
                if (legal.contains(capture)) rights.add(capture);
            }
        }
        rights.sort(java.util.Comparator.comparing(Move::toString));
        return rights;
    }

    @Override
    public List<Position> extraDestinations(IGameContext context, Position from) {
        List<Position> destinations = new ArrayList<>();
        Position target = targetSquare(context, from);
        if (target != null) destinations.add(target);
        return destinations;
    }

    @Override
    public IMoveCommand commandFor(IGameContext context, Position from, Position to) {
        Position target = targetSquare(context, from);
        if (target == null || !target.equals(to)) return null;
        Position capturedPosition = new Position(from.getRow(), to.getColumn());
        return new EnPassantCommand(context.getBoard(), from, to, capturedPosition);
    }

    // La casilla a la que el peón de "from" puede capturar al paso, o null.
    private Position targetSquare(IGameContext context, Position from) {
        Board board = context.getBoard();
        Piece pawn = board.pieceAt(from);
        if (pawn == null || !pawn.getType().equals(PieceType.PAWN)) return null;

        Move last = context.lastMove();
        if (last == null) return null;
        Piece moved = board.pieceAt(last.getTo());
        if (moved == null || !moved.getType().equals(PieceType.PAWN) || moved.getColor() == pawn.getColor()) {
            return null;
        }

        boolean doubleStep = Math.abs(last.getTo().getRow() - last.getFrom().getRow()) == 2;
        boolean besideMyPawn = last.getTo().getRow() == from.getRow()
            && Math.abs(last.getTo().getColumn() - from.getColumn()) == 1;
        if (!doubleStep || !besideMyPawn) return null;

        int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
        Position target = new Position(from.getRow() + direction, last.getTo().getColumn());
        return board.isInside(target) && board.isEmpty(target) ? target : null;
    }
}
