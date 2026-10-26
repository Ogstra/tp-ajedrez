import core.*;
import core.draw.FiftyMoveRule;
import ports.*;
import ai.SearchGames;
import java.util.*;

// Regresiones de reglas e historial, en memoria y sin infraestructura.
public class RulesRegressionTests {
    private static final PieceFactory PIECES = PieceFactory.standard();
    private static int passed;
    private static int failed;

    private static Position at(String square) { return Position.fromAlgebraic(square); }
    private static Move move(String from, String to) { return new Move(at(from), at(to)); }
    private static void put(Board board, PieceType type, Color color, String square) {
        board.placePiece(PIECES.create(type, color), at(square));
    }
    private static ChessGame game(Board board, Color turn) {
        return new ChessGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(),
            StandardRules.specialMoves(PIECES, color -> PieceType.QUEEN), StandardRules.drawRules(), turn);
    }
    private static void check(String name, boolean result) {
        if (result) { passed++; System.out.println("[OK]   " + name); }
        else { failed++; System.out.println("[FAIL] " + name); }
    }
    private static void cycle(IChessGame game, String... squares) {
        for (int i = 0; i < squares.length; i += 2) game.tryMove(at(squares[i]), at(squares[i + 1]));
    }
    private static void knightCycle(IChessGame game) {
        cycle(game, "b1", "a3", "b8", "a6", "a3", "b1", "a6", "b8");
    }
    private static void standardCycle(IChessGame game) {
        cycle(game, "g1", "f3", "g8", "f6", "f3", "g1", "f6", "g8");
    }
    private static Board exposedBoard() {
        Board board = new Board();
        put(board, PieceType.KING, Color.WHITE, "h5");
        put(board, PieceType.PAWN, Color.WHITE, "g5");
        put(board, PieceType.KING, Color.BLACK, "h8");
        put(board, PieceType.ROOK, Color.BLACK, "a5");
        put(board, PieceType.PAWN, Color.BLACK, "f7");
        return board;
    }
    private static Board repetitionBoard(boolean adjacent) {
        Board board = new Board();
        put(board, PieceType.KING, Color.WHITE, "h1");
        put(board, PieceType.KING, Color.BLACK, "h8");
        put(board, PieceType.KNIGHT, Color.WHITE, "b1");
        put(board, PieceType.KNIGHT, Color.BLACK, "b8");
        put(board, PieceType.PAWN, Color.WHITE, adjacent ? "e5" : "a2");
        put(board, PieceType.PAWN, Color.BLACK, "d7");
        return board;
    }
    private static ChessGame castlingGame() {
        Board board = new Board();
        put(board, PieceType.KING, Color.WHITE, "e1");
        put(board, PieceType.ROOK, Color.WHITE, "h1");
        put(board, PieceType.KING, Color.BLACK, "e8");
        return game(board, Color.WHITE);
    }
    private static void rookCycle(IChessGame game) {
        cycle(game, "h1", "h2", "e8", "f8", "h2", "h1", "f8", "e8");
    }
    public static void main(String[] args) {
        // Arrange / Act / Assert: una captura al paso no puede descubrir un jaque al propio rey.
        ChessGame exposed = game(exposedBoard(), Color.BLACK);
        exposed.tryMove(at("f7"), at("f5"));
        Board before = exposed.getBoard().cloneBoard();
        boolean listed = exposed.legalMoves().contains(move("g5", "f6"));
        check("enPassantCannotExposeKing", !listed
            && exposed.tryMove(at("g5"), at("f6")) == MoveOutcome.INVALID && before.equals(exposed.getBoard()));

        // Capturar al peon que da jaque debe ser legal y reversible.
        Board board = new Board();
        put(board, PieceType.KING, Color.WHITE, "e4");
        put(board, PieceType.PAWN, Color.WHITE, "e5");
        put(board, PieceType.KING, Color.BLACK, "h8");
        put(board, PieceType.PAWN, Color.BLACK, "d7");
        ChessGame checked = game(board, Color.BLACK);
        MoveOutcome checkMove = checked.tryMove(at("d7"), at("d5"));
        boolean allowed = checked.legalMoves().contains(move("e5", "d6"));
        MoveOutcome captured = checked.tryMove(at("e5"), at("d6"));
        Board after = board.cloneBoard();
        boolean undone = checked.undo();
        boolean restored = board.pieceAt(at("d5")) != null && board.pieceAt(at("e5")) != null;
        boolean redone = checked.redo();
        check("enPassantRemovesCheckingPawnAndSupportsUndoRedo", checkMove == MoveOutcome.CHECK && allowed
            && captured != MoveOutcome.INVALID && undone && restored && redone && after.equals(board));

        // Consultar jugadas legales no debe preguntar por una coronacion hipotetica.
        Board promotionBoard = new Board();
        put(promotionBoard, PieceType.KING, Color.WHITE, "a1");
        put(promotionBoard, PieceType.KING, Color.BLACK, "h8");
        put(promotionBoard, PieceType.PAWN, Color.WHITE, "e7");
        int[] calls = {0};
        ChessGame promotion = new ChessGame(promotionBoard, new ArrayList<IMoveListener>(),
            new ArrayList<IGameStatusListener>(), StandardRules.specialMoves(PIECES, color -> {
                calls[0]++; return PieceType.QUEEN;
            }), StandardRules.drawRules());
        check("legalMovesDoNotAskForPromotion", promotion.legalMoves().contains(move("e7", "e8")) && calls[0] == 0);

        ChessGame rook = castlingGame();
        rookCycle(rook);
        int first = rook.occurrencesOfCurrentPosition();
        rookCycle(rook);
        boolean ongoing = !rook.isGameOver() && rook.occurrencesOfCurrentPosition() == 2;
        rookCycle(rook);
        check("repetitionDistinguishesLostRookRights", first == 1 && ongoing && rook.isGameOver());

        ChessGame king = castlingGame();
        cycle(king, "e1", "f1", "e8", "f8", "f1", "e1", "f8", "e8");
        check("repetitionDistinguishesLostKingRights", king.occurrencesOfCurrentPosition() == 1);

        ChessGame available = game(repetitionBoard(true), Color.BLACK);
        available.tryMove(at("d7"), at("d5"));
        knightCycle(available);
        first = available.occurrencesOfCurrentPosition();
        knightCycle(available);
        ongoing = !available.isGameOver() && available.occurrencesOfCurrentPosition() == 2;
        knightCycle(available);
        check("repetitionDistinguishesLegalEnPassant", first == 1 && ongoing && available.isGameOver());

        ChessGame unavailable = game(repetitionBoard(false), Color.BLACK);
        unavailable.tryMove(at("d7"), at("d5"));
        knightCycle(unavailable); knightCycle(unavailable);
        check("repetitionIgnoresUnavailableEnPassant", unavailable.isGameOver());

        Board pinnedBoard = exposedBoard();
        put(pinnedBoard, PieceType.KNIGHT, Color.WHITE, "b1");
        put(pinnedBoard, PieceType.KNIGHT, Color.BLACK, "b8");
        ChessGame pinned = game(pinnedBoard, Color.BLACK);
        pinned.tryMove(at("f7"), at("f5"));
        knightCycle(pinned); knightCycle(pinned);
        check("repetitionIgnoresIllegalEnPassant", pinned.isGameOver());

        ChessGame original = game(repetitionBoard(true), Color.BLACK);
        original.tryMove(at("d7"), at("d5"));
        IChessGame search = SearchGames.from(original);
        boolean same = new HashSet<>(search.legalMoves()).equals(new HashSet<>(original.legalMoves()));
        boolean baseCannotUndo = !search.undo();
        captured = search.tryMove(at("e5"), at("d6"));
        undone = search.undo();
        check("searchPreservesEnPassantAndUndoBoundary", same && baseCannotUndo && captured != MoveOutcome.INVALID
            && undone && search.legalMoves().contains(move("e5", "d6")) && original.getBoard().pieceAt(at("d5")) != null);

        ChessGame lostRights = castlingGame();
        rookCycle(lostRights);
        search = SearchGames.from(lostRights);
        check("searchPreservesCastlingRights", !search.legalMoves().contains(move("e1", "g1"))
            && new HashSet<>(search.legalMoves()).equals(new HashSet<>(lostRights.legalMoves())));

        ChessGame clock = new ChessGame(BoardSetup.standard(PIECES), new ArrayList<IMoveListener>(),
            new ArrayList<IGameStatusListener>(), StandardRules.specialMoves(PIECES, color -> PieceType.QUEEN),
            Arrays.<IDrawRule>asList(new FiftyMoveRule(6)));
        standardCycle(clock);
        search = SearchGames.from(clock);
        search.tryMove(at("g1"), at("f3"));
        check("searchPreservesClockAndConfiguredDrawRules", search.tryMove(at("b8"), at("c6")) == MoveOutcome.DRAW
            && clock.halfMoveClock() == 4 && !clock.isGameOver());

        ChessGame repeated = game(BoardSetup.standard(PIECES), Color.WHITE);
        standardCycle(repeated);
        before = repeated.getBoard().cloneBoard();
        search = SearchGames.from(repeated);
        standardCycle(search);
        boolean draw = search.isGameOver();
        undone = search.undo();
        MoveOutcome again = search.tryMove(at("f6"), at("g8"));
        check("searchPreservesRepetitionAndIsolation", draw && undone && again == MoveOutcome.DRAW
            && !repeated.isGameOver() && before.equals(repeated.getBoard()) && repeated.occurrencesOfCurrentPosition() == 2);

        // Los derechos siguen contando aunque hoy haya piezas entre rey y torre.
        Board blockedBoard = BoardSetup.standard(PIECES);
        blockedBoard.removePiece(at("h2")); blockedBoard.removePiece(at("h7"));
        ChessGame blocked = game(blockedBoard, Color.WHITE);
        cycle(blocked, "h1", "h2", "h8", "h7", "h2", "h1", "h7", "h8");
        check("repetitionKeepsBlockedCastlingRights", blocked.occurrencesOfCurrentPosition() == 1);

        // Deshacer y rehacer en la real no cambian una copia ya creada.
        IChessGame detached = SearchGames.from(lostRights);
        for (int i = 0; i < 4; i++) lostRights.undo();
        boolean recovered = lostRights.legalMoves().contains(move("e1", "g1"));
        boolean isolatedRights = !detached.legalMoves().contains(move("e1", "g1"));
        for (int i = 0; i < 4; i++) lostRights.redo();
        check("castlingSnapshotsSurviveUndoRedoAndStayIndependent", recovered && isolatedRights
            && !lostRights.legalMoves().contains(move("e1", "g1")) && lostRights.occurrencesOfCurrentPosition() == 1);

        IChessGame firstCopy = SearchGames.from(original);
        firstCopy.tryMove(at("b1"), at("a3"));
        IChessGame secondCopy = SearchGames.from(firstCopy);
        check("copyOfCopyPreservesTurnAndLegalMoves", secondCopy.getTurn() == Color.BLACK && !secondCopy.undo()
            && new HashSet<>(secondCopy.legalMoves()).equals(new HashSet<>(firstCopy.legalMoves())));

        standardCycle(repeated);
        IChessGame finishedCopy = SearchGames.from(repeated);
        check("searchPreservesTerminalState", repeated.isGameOver() && finishedCopy.isGameOver()
            && finishedCopy.legalMoves().isEmpty());

        IChessGame resetClock = SearchGames.from(clock);
        resetClock.tryMove(at("a2"), at("a3"));
        check("searchPawnMoveResetsCopiedClock", ((IGameContext) resetClock).halfMoveClock() == 0
            && clock.halfMoveClock() == 4);

        System.out.println("\n" + passed + " pasados, " + failed + " fallados.");
        System.exit(failed == 0 ? 0 : 1);
    }
}
