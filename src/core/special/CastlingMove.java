package core.special;

import core.board.Board;
import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import core.command.CastlingCommand;
import core.command.IMoveCommand;
import core.IGameContext;

import java.util.ArrayList;
import java.util.List;

// Enroque: el rey va dos casillas hacia la torre de la esquina y la torre pasa a la
// casilla que el rey cruzó. Pide que ninguno de los dos se haya movido, que el camino
// esté vacío y que el rey no esté en jaque ni cruce o pise una casilla atacada.
public class CastlingMove implements ISpecialMove {

    @Override
    public List<Position> extraDestinations(IGameContext context, Position from) {
        List<Position> destinations = new ArrayList<>();
        Board board = context.getBoard();
        Piece king = board.pieceAt(from);

        if (king == null || !king.getType().equals(PieceType.KING)) return destinations;
        Color color = king.getColor();
        if (from.getRow() != backRow(board, color)) return destinations;
        if (context.hasBeenTouched(from)) return destinations;   // el rey ya se movió
        if (context.isAttacked(from, color)) return destinations; // no se enroca estando en jaque

        for (int side : new int[]{-1, 1}) {
            if (canCastle(context, from, color, side)) {
                destinations.add(new Position(from.getRow(), from.getColumn() + 2 * side));
            }
        }
        return destinations;
    }

    // side es -1 para el enroque largo (hacia la columna 0) y +1 para el corto.
    private boolean canCastle(IGameContext context, Position kingPos, Color color, int side) {
        Board board = context.getBoard();
        int row = kingPos.getRow();
        int rookColumn = side < 0 ? 0 : board.getColumns() - 1;
        Position rookPos = new Position(row, rookColumn);

        Piece rook = board.pieceAt(rookPos);
        if (rook == null || !rook.getType().equals(PieceType.ROOK) || rook.getColor() != color) return false;
        if (context.hasBeenTouched(rookPos)) return false; // la torre ya se movió, o la capturaron y llegó otra

        // El rey se mueve dos casillas y la torre queda pegada a él: hacen falta al menos 3 de distancia.
        int distance = Math.abs(rookColumn - kingPos.getColumn());
        if (distance < 3) return false;

        // Todas las casillas entre el rey y la torre tienen que estar vacías.
        for (int col = Math.min(kingPos.getColumn(), rookColumn) + 1; col < Math.max(kingPos.getColumn(), rookColumn); col++) {
            if (!board.isEmpty(new Position(row, col))) return false;
        }

        // El rey cruza una casilla y pisa la siguiente: ninguna puede estar atacada.
        for (int step = 1; step <= 2; step++) {
            if (context.isAttacked(new Position(row, kingPos.getColumn() + step * side), color)) return false;
        }
        return true;
    }

    @Override
    public IMoveCommand commandFor(IGameContext context, Position from, Position to) {
        Board board = context.getBoard();
        Piece piece = board.pieceAt(from);
        if (piece == null || !piece.getType().equals(PieceType.KING)) return null;
        if (from.getRow() != to.getRow() || Math.abs(to.getColumn() - from.getColumn()) != 2) return null;

        int side = to.getColumn() > from.getColumn() ? 1 : -1;
        Position rookFrom = new Position(from.getRow(), side < 0 ? 0 : board.getColumns() - 1);
        Position rookTo = new Position(from.getRow(), from.getColumn() + side);
        return new CastlingCommand(board, from, to, rookFrom, rookTo);
    }

    private int backRow(Board board, Color color) {
        return color == Color.WHITE ? 0 : board.getRows() - 1;
    }
}
