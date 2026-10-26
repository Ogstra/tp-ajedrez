package network;

import ai.IOpponent;
import core.Color;
import core.IChessGame;
import core.Move;
import core.Piece;
import core.PieceType;
import core.Position;
import ports.IMoveListener;
import ports.IPromotionChooser;

// El rival que juega desde otra PC. Para la partida es un oponente más, igual que los niveles de
// la IA (Strategy): en vez de pensar la jugada, la espera por la red. A la vez escucha la partida
// (Observer) para mandarle al rival las jugadas propias, y elige la pieza cuando corona el rival.
// Cada PC tiene su propia partida y aplica las jugadas que llegan con las mismas reglas, así que
// por la red solo viajan jugadas, nunca el tablero. El núcleo no sabe que existe la red.
//
// Mensajes: "e2 e4" para una jugada y "e7 e8 Q" si corona (Q reina, R torre, B alfil, N caballo).
public class LanOpponent implements IOpponent, IMoveListener, IPromotionChooser {

    private final LanConnection connection;
    private final Color localColor;
    private final String rivalName;
    private final IPromotionChooser localChooser; // pregunta a la persona a qué corona su peón
    private IChessGame game;
    private PieceType localPromotion = null;      // la pieza elegida para la jugada propia en curso
    private PieceType rivalPromotion = PieceType.QUEEN;

    public LanOpponent(LanConnection connection, Color localColor, String rivalName, IPromotionChooser localChooser) {
        this.connection = connection;
        this.localColor = localColor;
        this.rivalName = rivalName;
        this.localChooser = localChooser;
    }

    // Hace falta para reconstruir el enroque propio (ver onCastling).
    public void attachGame(IChessGame game) {
        this.game = game;
    }

    public Color getRivalColor() { return localColor.opposite(); }
    public String getRivalName() { return rivalName; }

    // Sale de la partida: el rival se entera como desconexión.
    public void close() {
        connection.close();
    }

    // IOpponent: espera la jugada del rival. Devuelve null si el rival se desconectó.
    @Override
    public Move chooseMove(IChessGame game) {
        String line = connection.receive();
        if (line == null) return null;
        String[] parts = line.trim().split(" ");
        if (parts.length < 2) return null;
        rivalPromotion = parts.length == 3 ? pieceFor(parts[2]) : PieceType.QUEEN;
        return new Move(Position.fromAlgebraic(parts[0]), Position.fromAlgebraic(parts[1]));
    }

    // IPromotionChooser: si corona el rival, la pieza que vino en su mensaje; si corona la
    // persona, se le pregunta y se recuerda para mandarla junto con la jugada.
    @Override
    public PieceType choose(Color color) {
        if (color != localColor) return rivalPromotion;
        localPromotion = localChooser.choose(color);
        return localPromotion;
    }

    // IMoveListener: cada jugada propia sale por la red. Las del rival también pasan por acá
    // (se aplican en esta partida), pero no se reenvían.
    @Override
    public void onMoveMade(Position from, Position to, Piece piece, Piece captured) {
        if (piece.getColor() != localColor) return;
        String line = from + " " + to;
        if (localPromotion != null) {
            line += " " + letterFor(localPromotion);
            localPromotion = null;
        }
        connection.send(line);
    }

    // El enroque se avisa sin casillas, pero el rey siempre se mueve dos columnas: con dónde
    // quedó el rey se sabe de dónde salió, y se manda como una jugada del rey.
    @Override
    public void onCastling(Color color, boolean kingside) {
        if (color != localColor || game == null) return;
        Position kingTo = game.getBoard().findKing(color);
        Position kingFrom = new Position(kingTo.getRow(), kingTo.getColumn() + (kingside ? -2 : 2));
        connection.send(kingFrom + " " + kingTo);
    }

    private static PieceType pieceFor(String letter) {
        switch (letter) {
            case "R": return PieceType.ROOK;
            case "B": return PieceType.BISHOP;
            case "N": return PieceType.KNIGHT;
            default:  return PieceType.QUEEN;
        }
    }

    private static String letterFor(PieceType type) {
        if (type.equals(PieceType.ROOK)) return "R";
        if (type.equals(PieceType.BISHOP)) return "B";
        if (type.equals(PieceType.KNIGHT)) return "N";
        return "Q";
    }

    // Lo demás que avisa la partida no hace falta mandarlo: el rival lo calcula en su PC.
    @Override public void onPromotion(Color color, Position position, PieceType newPiece) { }
    @Override public void onUndo() { }
    @Override public void onRedo() { }
}
