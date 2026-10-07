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
        for (int fila = board.getRows() - 1; fila >= 0; fila--) {
            System.out.print(String.format("%2d ", fila + 1));
            for (int col = 0; col < board.getColumns(); col++) {
                System.out.print(symbol(board.pieceAt(new Position(fila, col))) + " ");
            }
            System.out.println();
        }
        StringBuilder letras = new StringBuilder("   ");
        for (int col = 0; col < board.getColumns(); col++) {
            letras.append((char) ('a' + col)).append(' ');
        }
        System.out.println(letras);
    }

    private String symbol(Piece piece) {
        if (piece == null) return ".";
        boolean blanca = piece.getColor() == Color.WHITE;
        String symbol = (blanca ? WHITE_SYMBOLS : BLACK_SYMBOLS).get(piece.getType());
        if (symbol != null) return symbol;
        String inicial = piece.getType().getName().substring(0, 1);
        return blanca ? inicial.toUpperCase() : inicial.toLowerCase();
    }

    private String nombre(PieceType type) {
        String nombre = NAMES.get(type);
        return nombre != null ? nombre : type.getName();
    }

    private String nombre(Color color) {
        return color == Color.WHITE ? "Blancas" : "Negras";
    }

    @Override
    public void onMoveMade(Position from, Position to, Piece piece, Piece captured) {
        String captura = captured != null ? " (captura " + nombre(captured.getType()) + ")" : "";
        System.out.println(nombre(piece.getColor()) + ": " + nombre(piece.getType())
            + " " + from + " -> " + to + captura);
    }

    @Override
    public void onInvalidMove(String reason) {
        System.out.println("Movimiento inválido: " + reason);
    }

    @Override
    public void onCheck(Color colorInCheck) {
        System.out.println("¡Jaque a " + nombre(colorInCheck) + "!");
    }

    @Override
    public void onCheckmate(Color winnerColor) {
        System.out.println("¡Jaque mate! Ganan " + nombre(winnerColor) + ".");
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
    public void onCastling(Color color, boolean cortoEnEnroque) {
        System.out.println(nombre(color) + ": enroque " + (cortoEnEnroque ? "corto" : "largo") + ".");
    }

    @Override
    public void onPromotion(Color color, Position pos, PieceType nuevaPieza) {
        System.out.println(nombre(color) + ": el peón corona en " + pos + " y se convierte en " + nombre(nuevaPieza) + ".");
    }
}
