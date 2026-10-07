package nucleo;

import java.util.Objects;

// Value object inmutable. Misma idea que en la Tarea 03: equals/hashCode por
// valor, para poder usarlo como clave de un Map y compararlo por contenido.
// No sabe de qué tamaño es el tablero: eso lo responde Board.isInside().
public final class Position {
    private final int row;    // 0 es la fila 1
    private final int column; // 0 es la columna a

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRow() { return row; }
    public int getColumn() { return column; }

    // "e2" -> columna 'e'-'a'=4, fila 2-1=1. Acepta filas de más de un dígito
    // ("e10"). Devuelve null si el texto no tiene ese formato; si la casilla
    // existe en el tablero lo decide quien llama con Board.isInside().
    public static Position fromAlgebraic(String texto) {
        if (texto == null || texto.length() < 2) return null;
        char letraColumna = Character.toLowerCase(texto.charAt(0));
        if (letraColumna < 'a' || letraColumna > 'z') return null;
        try {
            int fila = Integer.parseInt(texto.substring(1)) - 1;
            return new Position(fila, letraColumna - 'a');
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
