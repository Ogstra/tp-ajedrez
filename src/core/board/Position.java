package core.board;

import java.util.Objects;

// Una casilla del tablero. Inmutable y con equals/hashCode por valor, para usarla
// como clave de un Map. Si existe o no en el tablero lo responde Board.isInside().
public final class Position {
    private final int row;    // 0 es la fila 1
    private final int column; // 0 es la columna a

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRow() { return row; }
    public int getColumn() { return column; }

    // "e2" es la columna 4 y la fila 1. Acepta filas de más de un dígito ("e10").
    // Devuelve null si el texto no tiene ese formato.
    public static Position fromAlgebraic(String text) {
        if (text == null || text.length() < 2) return null;
        char columnLetter = Character.toLowerCase(text.charAt(0));
        if (columnLetter < 'a' || columnLetter > 'z') return null;
        try {
            int row = Integer.parseInt(text.substring(1)) - 1;
            return new Position(row, columnLetter - 'a');
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String toAlgebraic() {
        return "" + (char) ('a' + column) + (row + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position position = (Position) o;
        return row == position.row && column == position.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
