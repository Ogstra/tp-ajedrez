package core;

import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import core.command.IMoveCommand;
import ports.IMoveListener;

// Escribe una jugada en notación algebraica, con las letras en español: R rey, D dama,
// T torre, A alfil y C caballo; el peón no lleva letra. Por ejemplo "Cf3", "exd5", "O-O"
// o "e8=D+". Se entera de qué pasó escuchando el aviso del propio comando, así no
// pregunta de qué clase es la jugada.
final class MoveNotation implements IMoveListener {
    private String text = "";

    // "disambiguation" es la columna o la fila que se agrega cuando otra pieza igual
    // podía llegar a la misma casilla, como la "b" de "Cbd2".
    static String of(IMoveCommand command, String disambiguation, boolean check, boolean mate) {
        MoveNotation notation = new MoveNotation(disambiguation);
        command.announce(notation);
        return notation.text + (mate ? "#" : check ? "+" : "");
    }

    private final String disambiguation;

    private MoveNotation(String disambiguation) {
        this.disambiguation = disambiguation;
    }

    static String letter(PieceType type) {
        if (type.equals(PieceType.KING)) return "R";
        if (type.equals(PieceType.QUEEN)) return "D";
        if (type.equals(PieceType.ROOK)) return "T";
        if (type.equals(PieceType.BISHOP)) return "A";
        if (type.equals(PieceType.KNIGHT)) return "C";
        if (type.equals(PieceType.PAWN)) return "";
        return type.getName().substring(0, 1).toUpperCase(); // una pieza inventada usa su inicial
    }

    @Override
    public void onMoveMade(Position from, Position to, Piece piece, Piece captured) {
        if (piece.getType().equals(PieceType.PAWN)) {
            // El peón que captura se nombra por su columna de salida.
            text = (captured != null ? from.toAlgebraic().charAt(0) + "x" : "") + to;
        } else {
            text = letter(piece.getType()) + disambiguation + (captured != null ? "x" : "") + to;
        }
    }

    @Override
    public void onCastling(Color color, boolean kingside) {
        text = kingside ? "O-O" : "O-O-O";
    }

    @Override
    public void onPromotion(Color color, Position position, PieceType newPiece) {
        text += "=" + letter(newPiece);
    }

    @Override public void onUndo() { }
    @Override public void onRedo() { }
}
