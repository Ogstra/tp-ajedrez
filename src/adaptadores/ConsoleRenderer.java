package adaptadores;

import nucleo.Board;
import nucleo.Color;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;
import puertos.IGameObserver;

import java.util.HashMap;
import java.util.Map;

// Adaptador de salida (Clase 2): implementa el puerto IGameObserver con
// tecnología concreta (consola). El núcleo no sabe que esto existe. Los
// símbolos unicode y los nombres en español son un detalle de presentación,
// por eso viven acá y no en PieceType. Una pieza que este adaptador no
// conoce (una nueva del núcleo) se dibuja con la inicial de su nombre.
public class ConsoleRenderer implements IGameObserver {

    private static final Map<PieceType, String> WHITE_SYMBOLS = new HashMap<>();
    private static final Map<PieceType, String> BLACK_SYMBOLS = new HashMap<>();
    private static final Map<PieceType, String> NAMES = new HashMap<>();

    static {
        define(PieceType.KING, "♔", "♚", "Rey");
        define(PieceType.QUEEN, "♕", "♛", "Reina");
        define(PieceType.ROOK, "♖", "♜", "Torre");
        define(PieceType.BISHOP, "♗", "♝", "Alfil");
        define(PieceType.KNIGHT, "♘", "♞", "Caballo");
        define(PieceType.PAWN, "♙", "♟", "Peón");
    }

    private static void define(PieceType type, String white, String black, String name) {
        WHITE_SYMBOLS.put(type, white);
        BLACK_SYMBOLS.put(type, black);
        NAMES.put(type, name);
    }

    public void render(Board board) {
        System.out.println();
        for (int row = board.getRows() - 1; row >= 0; row--) {
            System.out.print(String.format("%2d ", row + 1));
            for (int col = 0; col < board.getColumns(); col++) {
                System.out.print(symbol(board.pieceAt(new Position(row, col))) + " ");
            }
            System.out.println();
        }
        StringBuilder letters = new StringBuilder("   ");
        for (int col = 0; col < board.getColumns(); col++) {
            letters.append((char) ('a' + col)).append(' ');
        }
        System.out.println(letters);
    }

    private String symbol(Piece piece) {
        if (piece == null) return ".";
        boolean isWhite = piece.getColor() == Color.WHITE;
        String symbol = (isWhite ? WHITE_SYMBOLS : BLACK_SYMBOLS).get(piece.getType());
        if (symbol != null) return symbol;
        String initial = piece.getType().getName().substring(0, 1);
        return isWhite ? initial.toUpperCase() : initial.toLowerCase();
    }

    private String name(PieceType type) {
        String name = NAMES.get(type);
        return name != null ? name : type.getName();
    }

    private String name(Color color) {
        return color == Color.WHITE ? "Blancas" : "Negras";
    }

    @Override
    public void onMoveMade(Position from, Position to, Piece piece, Piece captured) {
        String captureText = captured != null ? " (captura " + name(captured.getType()) + ")" : "";
        System.out.println(name(piece.getColor()) + ": " + name(piece.getType())
            + " " + from + " -> " + to + captureText);
    }

    @Override
    public void onInvalidMove(String reason) {
        System.out.println("Movimiento inválido: " + reason);
    }

    @Override
    public void onCheck(Color colorInCheck) {
        System.out.println("¡Jaque a " + name(colorInCheck) + "!");
    }

    @Override
    public void onCheckmate(Color winnerColor) {
        System.out.println("¡Jaque mate! Ganan " + name(winnerColor) + ".");
    }

    @Override
    public void onStalemate() {
        System.out.println("Ahogado: tablas, el jugador en turno no tiene movimientos legales.");
    }

    @Override
    public void onUndo() {
        System.out.println("Se deshizo el último movimiento.");
    }

    @Override
    public void onCastling(Color color, boolean kingSide) {
        System.out.println(name(color) + ": enroque " + (kingSide ? "corto" : "largo") + ".");
    }

    @Override
    public void onPromotion(Color color, Position pos, PieceType newPiece) {
        System.out.println(name(color) + ": el peón corona en " + pos + " y se convierte en " + name(newPiece) + ".");
    }
}
