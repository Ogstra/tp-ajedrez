package nucleo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Mismo esquema que la Tarea 03: el tablero es un mapa "posición ocupada
// -> pieza", no una matriz fija. Una casilla vacía simplemente no tiene
// entrada en el mapa. El tamaño es un dato del tablero, no una constante
// repartida por el código: todo el que necesite saber si una casilla existe
// le pregunta a Board.isInside().
public class Board {
    private static final int DEFAULT_SIZE = 8;

    private final int rows;
    private final int columns;
    private final Map<Position, Piece> squares = new HashMap<>();

    public Board() {
        this(DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public Board(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    public int getRows() { return rows; }
    public int getColumns() { return columns; }

    public boolean isInside(Position pos) {
        return pos.getRow() >= 0 && pos.getRow() < rows
            && pos.getColumn() >= 0 && pos.getColumn() < columns;
    }

    public Piece pieceAt(Position pos) {
        return squares.get(pos);
    }

    public void placePiece(Piece piece, Position pos) {
        squares.put(pos, piece);
    }

    public void movePiece(Position from, Position to) {
        Piece piece = squares.remove(from);
        if (piece != null) {
            squares.put(to, piece);
        }
    }

    // Necesario para deshacer una coronación: hay que poder dejar una
    // casilla vacía sin poner ninguna pieza en su lugar.
    public void removePiece(Position pos) {
        squares.remove(pos);
    }

    public boolean isEmpty(Position pos) {
        return pieceAt(pos) == null;
    }

    public boolean isOccupiedByColor(Position pos, Color color) {
        Piece p = pieceAt(pos);
        return p != null && p.getColor() == color;
    }

    public boolean isOccupiedByOpponent(Position pos, Color color) {
        Piece p = pieceAt(pos);
        return p != null && p.getColor() != color;
    }

    public List<Position> positionsOfColor(Color color) {
        List<Position> result = new ArrayList<>();
        for (Map.Entry<Position, Piece> entry : squares.entrySet()) {
            if (entry.getValue().getColor() == color) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Position findKing(Color color) {
        for (Map.Entry<Position, Piece> entry : squares.entrySet()) {
            Piece p = entry.getValue();
            if (p.getType().equals(PieceType.KING) && p.getColor() == color) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Board cloneBoard() {
        Board copy = new Board(rows, columns);
        for (Map.Entry<Position, Piece> entry : squares.entrySet()) {
            copy.placePiece(entry.getValue(), entry.getKey());
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Board)) return false;
        Board board = (Board) o;
        return rows == board.rows && columns == board.columns
            && Objects.equals(squares, board.squares);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, columns, squares);
    }
}
