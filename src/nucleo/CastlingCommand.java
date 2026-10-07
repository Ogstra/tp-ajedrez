package nucleo;

import puertos.IGameObserver;

import java.util.Arrays;
import java.util.List;

// Command (Clase 3), variante de MoveCommand para el enroque: son DOS
// piezas moviéndose en el mismo turno (rey y torre). No hay captura posible
// en un enroque (las reglas exigen las casillas vacías), por eso no hace
// falta guardar ninguna pieza capturada.
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
    public void announce(IGameObserver observer) {
        boolean corto = kingTo.getColumn() > kingFrom.getColumn();
        observer.onCastling(color, corto);
    }
}
