package nucleo;

// Factory (Clase 3) del armado inicial. Antes vivía dentro de Board
// (Board.standardSetup), mezclando "guardar el estado" con "saber cómo
// arranca una partida". Ahora el tamaño y el orden de la fila trasera son
// parámetros: un tablero de otro tamaño es una llamada distinta a custom(),
// no un cambio de código.
public final class BoardSetup {

    private static final PieceType[] STANDARD_BACK_ROW = {
        PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
        PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
    };

    private BoardSetup() { }

    public static Board standard(PieceFactory pieces) {
        return custom(8, 8, STANDARD_BACK_ROW, pieces);
    }

    // backRow tiene una entrada por columna. Blancas arman desde la fila 0
    // (fila trasera y peones en la 1); negras desde la última (peones en la
    // anteúltima).
    public static Board custom(int rows, int columns, PieceType[] backRow, PieceFactory pieces) {
        if (backRow.length != columns) {
            throw new IllegalArgumentException("La fila trasera debe tener " + columns + " piezas.");
        }
        if (rows < 4) {
            throw new IllegalArgumentException("Hacen falta al menos 4 filas.");
        }

        Board board = new Board(rows, columns);
        for (int col = 0; col < columns; col++) {
            board.placePiece(pieces.create(backRow[col], Color.WHITE), new Position(0, col));
            board.placePiece(pieces.create(PieceType.PAWN, Color.WHITE), new Position(1, col));

            board.placePiece(pieces.create(PieceType.PAWN, Color.BLACK), new Position(rows - 2, col));
            board.placePiece(pieces.create(backRow[col], Color.BLACK), new Position(rows - 1, col));
        }
        return board;
    }
}
