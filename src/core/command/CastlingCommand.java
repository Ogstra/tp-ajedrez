package core.command;

import core.board.Board;
import core.board.Color;
import core.board.Position;
import ports.IMoveListener;

import java.util.Arrays;
import java.util.List;

// Comando del enroque: mueve el rey y la torre en la misma jugada. No hay captura,
// así que no guarda ninguna pieza.
public class CastlingCommand implements IMoveCommand {
    private final Board board;
    private final Position kingFrom;
    private final Position kingTo;
    private final Position rookFrom;
    private final Position rookTo;
    private Color color;

    public CastlingCommand(Board board, Position kingFrom, Position kingTo, Position rookFrom, Position rookTo) {
        this.board = board;
        this.kingFrom = kingFrom;
        this.kingTo = kingTo;
        this.rookFrom = rookFrom;
        this.rookTo = rookTo;
    }

    @Override
    public void execute() {
        this.color = board.pieceAt(kingFrom).getColor();
        board.movePiece(kingFrom, kingTo);
        board.movePiece(rookFrom, rookTo);
    }

    @Override
    public void undo() {
        board.movePiece(kingTo, kingFrom);
        board.movePiece(rookTo, rookFrom);
    }

    @Override
    public List<Position> touchedSquares() {
        return Arrays.asList(kingFrom, kingTo, rookFrom, rookTo);
    }

    @Override
    public void announce(IMoveListener listener) {
        boolean kingside = kingTo.getColumn() > kingFrom.getColumn();
        listener.onCastling(color, kingside);
    }
    @Override
    public boolean resetsHalfMoveClock() {
        return false;
    }
}
