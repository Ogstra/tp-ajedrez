package nucleo.tablas;

import nucleo.Board;
import nucleo.Color;
import nucleo.IDrawRule;
import nucleo.IGameContext;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

// Tablas por material insuficiente: ninguno de los dos bandos puede dar mate.
// Casos: rey contra rey, rey y un solo alfil o caballo contra rey, y solo
// alfiles todos en casillas del mismo color. Cualquier otra pieza (peón,
// torre, reina o una pieza nueva) cuenta como material suficiente.
public class InsufficientMaterialRule implements IDrawRule {

    @Override
    public String reason(IGameContext context) {
        Board board = context.getBoard();
        List<Position> positions = new ArrayList<>(board.positionsOfColor(Color.WHITE));
        positions.addAll(board.positionsOfColor(Color.BLACK));

        int minorPieces = 0;
        boolean onlyBishops = true;
        int bishopSquareColor = -1;
        boolean bishopsOnSameColor = true;

        for (Position position : positions) {
            Piece piece = board.pieceAt(position);
            if (piece.getType().equals(PieceType.KING)) continue;
            if (piece.getType().equals(PieceType.BISHOP)) {
                int squareColor = (position.getRow() + position.getColumn()) % 2;
                if (bishopSquareColor == -1) bishopSquareColor = squareColor;
                else if (bishopSquareColor != squareColor) bishopsOnSameColor = false;
                minorPieces++;
            } else if (piece.getType().equals(PieceType.KNIGHT)) {
                minorPieces++;
                onlyBishops = false;
            } else {
                return null; // peón, torre, reina o pieza desconocida: hay material para dar mate
            }
        }

        if (minorPieces == 0) return "rey contra rey";
        if (minorPieces == 1) return "un solo alfil o caballo contra rey";
        if (onlyBishops && bishopsOnSameColor) return "solo alfiles en casillas del mismo color";
        return null;
    }
}
