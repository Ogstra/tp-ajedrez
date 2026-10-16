import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.Color;
import nucleo.IMovementStrategy;
import nucleo.ISpecialMove;
import nucleo.MoveCommand;
import nucleo.MoveOutcome;
import nucleo.Piece;
import nucleo.PieceFactory;
import nucleo.PieceType;
import nucleo.Position;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.PromotionMove;
import nucleo.estrategias.KnightMovementStrategy;
import nucleo.estrategias.RookMovementStrategy;
import puertos.IGameObserver;
import puertos.IPromotionChooser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Tests del núcleo, sin ninguna UI (Clase 4: el núcleo puro se testea sin
// infraestructura). Mismo formato Arrange-Act-Assert y mismo estilo de
// salida por consola que la Tarea 03 (no se usa JUnit: ningún build tool
// fue dado en la materia, esto corre con "java ChessCoreTests" directo).
public class ChessCoreTests {

    private static int passed = 0;
    private static int failed = 0;

    private static final PieceFactory PIECES = PieceFactory.standard();

    public static void main(String[] args) {
        testPawnAdvancesOneSquare();
        testPawnCannotCaptureStraightAhead();
        testKnightJumpsInL();
        testRookCannotJumpOverPieces();
        testKingCannotMoveToAttackedSquare();
        testBlackCannotMoveFirst();
        testCheckIsDetectedAndIsNotMate();
        testMoveCommandUndoRestoresBoard();
        testFoolsMate();
        testShortCastlingMovesKingAndRook();
        testLongCastlingMovesKingAndRook();
        testCannotCastleIfKingAlreadyMoved();
        testCannotCastleThroughSquareAttackedByPawn();
        testUndoRestoresCastlingRight();
        testPawnPromotesToQueen();
        testNewPieceWithoutTouchingExistingClasses();
        testBoardOfAnotherSize();
        testPromotionOnSmallBoard();
        testObserverReceivesOnlyCastling();
        testObserverReceivesMoveAndPromotion();

        testBishopSlidesAndStopsAtOwnPiece();
        testQueenMovesLikeRookAndBishop();
        testKingMovesOneSquareInAnyDirection();
        testPawnDoubleStepOnlyFromStartAndWithFreePath();
        testPawnCapturesOnlyDiagonally();

        testRookCapturesThroughTryMove();
        testInvalidMoveLeavesBoardUntouched();
        testPinnedPieceCannotMove();
        testStalemateEndsTheGame();
        testFinishedGameRejectsMoves();
        testUndoRestoresTheTurn();
        testInvalidMoveNotifiesObserver();

        testPromotionToKnight();
        testPromotionUndoRestoresThePawn();
        testCannotCastleWhileInCheck();
        testCannotCastleWithPieceBetween();

        System.out.println("\n" + passed + " pasados, " + failed + " fallados.");
        System.exit(failed > 0 ? 1 : 0);
    }

    // ---------- ayudas de los tests ----------

    private static void assertTrue(String testName, boolean condition, String detail) {
        if (condition) {
            passed++;
            System.out.println("[OK]   " + testName);
        } else {
            failed++;
            System.out.println("[FAIL] " + testName + " -> " + detail);
        }
    }

    private static Position at(String algebraic) {
        return Position.fromAlgebraic(algebraic);
    }

    private static Piece piece(PieceType type, Color color) {
        return PIECES.create(type, color);
    }

    // Stub (Clase 4): devuelve un dato fijo, sin lógica.
    private static class AlwaysQueenChooser implements IPromotionChooser {
        @Override
        public PieceType choose(Color color) {
            return PieceType.QUEEN;
        }
    }

    // Mock (Clase 4): registra qué le avisaron, para verificarlo en el Assert.
    private static class RecordingObserver implements IGameObserver {
        final List<String> events = new ArrayList<>();

        @Override public void onMoveMade(Position from, Position to, Piece piece, Piece captured) { events.add("move"); }
        @Override public void onInvalidMove(String reason) { events.add("invalid"); }
        @Override public void onCheck(Color colorInCheck) { events.add("check"); }
        @Override public void onCheckmate(Color winnerColor) { events.add("checkmate"); }
        @Override public void onStalemate() { events.add("stalemate"); }
        @Override public void onUndo() { events.add("undo"); }
        @Override public void onCastling(Color color, boolean kingSide) { events.add("castling"); }
        @Override public void onPromotion(Color color, Position pos, PieceType newPiece) { events.add("promotion"); }
    }

    // El armado que hace Main, sin consola: enroque + coronación (siempre a reina).
    private static ChessGame newGame(Board board, IGameObserver... observers) {
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(PIECES, new AlwaysQueenChooser()));
        return new ChessGame(board, Arrays.asList(observers), specialMoves);
    }

    // ---------- movimiento de cada pieza ----------

    private static void testPawnAdvancesOneSquare() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));

        // Act
        boolean puedeAvanzar = board.pieceAt(at("e2")).possibleMoves(board, at("e2")).contains(at("e3"));

        // Assert
        assertTrue("Peon_AvanzaUnPaso", puedeAvanzar, "e2->e3 debería ser un movimiento posible");
    }

    private static void testPawnCannotCaptureStraightAhead() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("e3"));

        // Act
        boolean intentaCapturarDeFrente = board.pieceAt(at("e2")).possibleMoves(board, at("e2")).contains(at("e3"));

        // Assert
        assertTrue("Peon_NoCapturaDeFrente", !intentaCapturarDeFrente,
            "un peón no debería poder capturar la pieza que tiene justo enfrente");
    }

    private static void testKnightJumpsInL() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KNIGHT, Color.WHITE), at("b1"));

        // Act
        List<Position> moves = board.pieceAt(at("b1")).possibleMoves(board, at("b1"));

        // Assert
        assertTrue("Caballo_SaltaEnL", moves.contains(at("c3")) && moves.contains(at("a3")),
            "desde b1 el caballo debería poder llegar a c3 y a a3");
    }

    private static void testRookCannotJumpOverPieces() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a3"));

        // Act
        List<Position> moves = board.pieceAt(at("a1")).possibleMoves(board, at("a1"));

        // Assert
        assertTrue("Torre_NoAtraviesaPiezas", !moves.contains(at("a4")) && moves.contains(at("a2")),
            "la torre debería frenar antes de a3 (pieza propia) y no poder saltarla");
    }

    // ---------- reglas de la partida ----------

    private static void testKingCannotMoveToAttackedSquare() {
        // Arrange: la torre negra en h2 controla toda la fila 2.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("h2"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome toAttackedSquare = game.tryMove(at("e1"), at("e2"));
        MoveOutcome toSafeSquare = game.tryMove(at("e1"), at("d1"));

        // Assert
        assertTrue("Rey_NoPuedeMoverseACasillaAtacada",
            toAttackedSquare == MoveOutcome.INVALID && toSafeSquare == MoveOutcome.OK,
            "e1->e2 queda en jaque por la torre (inválido), e1->d1 no (válido)");
    }

    private static void testBlackCannotMoveFirst() {
        // Arrange
        ChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        MoveOutcome blackAttempt = game.tryMove(at("e7"), at("e5"));

        // Assert
        assertTrue("Turnos_NegrasNoMuevenPrimero", blackAttempt == MoveOutcome.INVALID,
            "en el primer turno solo pueden mover las blancas");
    }

    private static void testCheckIsDetectedAndIsNotMate() {
        // Arrange: la torre blanca puede subir a a8 y dar jaque al rey negro por la fila 8.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("a1"), at("a8"));

        // Assert: el rey negro puede escapar (g7, h7), así que es jaque y no jaque mate.
        assertTrue("Jaque_SeDetectaYNoEsMate", outcome == MoveOutcome.CHECK && !game.isGameOver(),
            "Ta8 debería dar jaque sin ser mate");
    }

    private static void testMoveCommandUndoRestoresBoard() {
        // Arrange (mismo test que la Tarea 03, adaptado a este tablero de ajedrez completo)
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        Board boardBefore = board.cloneBoard();
        MoveCommand command = new MoveCommand(board, at("e2"), at("e4"));

        // Act
        command.execute();
        command.undo();

        // Assert
        assertTrue("MoveCommand_ExecuteUndo_RestauraTablero", boardBefore.equals(board),
            "el tablero después de undo() debería ser idéntico al de antes de execute()");
    }

    private static void testFoolsMate() {
        // Arrange: el jaque mate más rápido posible, en 2 jugadas por bando.
        ChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));
        MoveOutcome lastMove = game.tryMove(at("d8"), at("h4"));

        // Assert
        assertTrue("JaqueMate_MateDelLoco", lastMove == MoveOutcome.CHECKMATE && game.isGameOver(),
            "Dxh4 debería ser jaque mate y terminar la partida");
    }

    // ---------- enroque ----------

    private static Board castlingBoard() {
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        return board;
    }

    private static boolean hasPiece(Board board, String square, PieceType tipo) {
        Piece p = board.pieceAt(at(square));
        return p != null && p.getType().equals(tipo);
    }

    private static void testShortCastlingMovesKingAndRook() {
        // Arrange
        Board board = castlingBoard();
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_Corto_MueveReyYTorreJuntos",
            outcome == MoveOutcome.OK && hasPiece(board, "g1", PieceType.KING) && hasPiece(board, "f1", PieceType.ROOK),
            "tras enrocar corto, el rey debería estar en g1 y la torre en f1");
    }

    private static void testLongCastlingMovesKingAndRook() {
        // Arrange
        Board board = castlingBoard();
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("c1"));

        // Assert
        assertTrue("Enroque_Largo_MueveReyYTorreJuntos",
            outcome == MoveOutcome.OK && hasPiece(board, "c1", PieceType.KING) && hasPiece(board, "d1", PieceType.ROOK),
            "tras enrocar largo, el rey debería estar en c1 y la torre en d1");
    }

    private static void testCannotCastleIfKingAlreadyMoved() {
        // Arrange
        Board board = castlingBoard();
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("a7"));
        ChessGame game = newGame(board);

        // Act: el rey sale y vuelve a e1 (las negras mueven el peón en el medio), y recién ahí intenta enrocar.
        game.tryMove(at("e1"), at("e2"));
        game.tryMove(at("a7"), at("a6"));
        game.tryMove(at("e2"), at("e1"));
        game.tryMove(at("a6"), at("a5"));
        MoveOutcome castlingAttempt = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_NoPermitidoSiElReyYaSeMovio", castlingAttempt == MoveOutcome.INVALID,
            "el rey ya se movió antes (aunque volvió a e1), no debería poder enrocar");
    }

    private static void testCannotCastleThroughSquareAttackedByPawn() {
        // Arrange: el peón negro en h2 ataca g1 en diagonal, aunque g1 esté vacía.
        Board board = castlingBoard();
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("h2"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome castlingAttempt = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_NoPermitidoSiElReyPisaCasillaAtacadaPorPeon", castlingAttempt == MoveOutcome.INVALID,
            "el rey terminaría en g1, atacada por el peón de h2");
    }

    private static void testUndoRestoresCastlingRight() {
        // Arrange
        Board board = castlingBoard();
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("a7"));
        ChessGame game = newGame(board);

        // Act: el rey se mueve, las negras mueven, y se deshacen las dos jugadas.
        game.tryMove(at("e1"), at("e2"));
        game.tryMove(at("a7"), at("a6"));
        game.undo();
        game.undo();
        MoveOutcome outcome = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_DeshacerRecuperaElDerecho", outcome == MoveOutcome.OK,
            "si se deshizo el único movimiento del rey, el enroque tiene que volver a estar permitido");
    }

    // ---------- coronación ----------

    private static void testPawnPromotesToQueen() {
        // Arrange: el rey negro lejos de la fila 8 y de las diagonales de a8, para que no haya jaque.
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a7"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h4"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("a7"), at("a8"));

        // Assert
        Piece finalPiece = board.pieceAt(at("a8"));
        assertTrue("Peon_Corona_A_Reina", outcome == MoveOutcome.OK
            && finalPiece.getType().equals(PieceType.QUEEN) && finalPiece.getColor() == Color.WHITE,
            "en a8 debería haber una reina blanca, no un peón");
    }

    // ---------- extensibilidad (lo que pide la defensa individual) ----------

    // Una pieza que no existe en la materia: se mueve como torre y como caballo.
    // Se define ACÁ, en el test, sin tocar ninguna clase de nucleo/.
    private static class ChancellorMovementStrategy implements IMovementStrategy {
        private final RookMovementStrategy asRook = new RookMovementStrategy();
        private final KnightMovementStrategy asKnight = new KnightMovementStrategy();

        @Override
        public List<Position> possibleMoves(Board board, Position from, Color color) {
            List<Position> moves = new ArrayList<>(asRook.possibleMoves(board, from, color));
            moves.addAll(asKnight.possibleMoves(board, from, color));
            return moves;
        }
    }

    private static void testNewPieceWithoutTouchingExistingClasses() {
        // Arrange: se registra el tipo nuevo en una fábrica; no se modifica PieceType, PieceFactory ni nada.
        PieceType chancellor = new PieceType("CHANCELLOR");
        PieceFactory factory = PieceFactory.standard();
        factory.register(chancellor, new ChancellorMovementStrategy());
        Board board = new Board();
        board.placePiece(factory.create(chancellor, Color.WHITE), at("d4"));

        // Act
        List<Position> moves = board.pieceAt(at("d4")).possibleMoves(board, at("d4"));

        // Assert: d8 es de torre, e6 es de caballo, e5 (diagonal) no es ninguna de las dos.
        assertTrue("PiezaNueva_Chancellor_SeRegistraSinModificarClases",
            moves.contains(at("d8")) && moves.contains(at("e6")) && !moves.contains(at("e5")),
            "el chancellor debería moverse como torre + caballo");
    }

    private static void testBoardOfAnotherSize() {
        // Arrange: un tablero de 10x10 en vez de 8x8.
        Board board = new Board(10, 10);
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));

        // Act
        List<Position> moves = board.pieceAt(at("a1")).possibleMoves(board, at("a1"));

        // Assert
        assertTrue("Tablero_10x10_LaTorreRecorreTodoElTablero",
            moves.contains(at("a10")) && moves.contains(at("j1")) && !board.isInside(at("k1")),
            "la torre debería llegar a a10 y a j1, y k1 no existe en un tablero de 10 columnas");
    }

    private static void testPromotionOnSmallBoard() {
        // Arrange: tablero de 5x5; la última fila es la 5, no la 8.
        Board board = new Board(5, 5);
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a4"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("d3"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("a4"), at("a5"));

        // Assert
        Piece finalPiece = board.pieceAt(at("a5"));
        assertTrue("Tablero_5x5_CoronaEnLaUltimaFilaDeEseTablero",
            outcome == MoveOutcome.OK && finalPiece.getType().equals(PieceType.QUEEN),
            "el peón debería coronar al llegar a la fila 5, que es la última de este tablero");
    }

    // ---------- el observer recibe lo que corresponde (con un mock) ----------

    private static void testObserverReceivesOnlyCastling() {
        // Arrange
        RecordingObserver observer = new RecordingObserver();
        ChessGame game = newGame(castlingBoard(), observer);

        // Act
        game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Observer_Enroque_AvisaSoloEnroque", observer.events.equals(Arrays.asList("castling")),
            "esperaba [castling] y llegó " + observer.events);
    }

    private static void testObserverReceivesMoveAndPromotion() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a7"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h4"));
        RecordingObserver observer = new RecordingObserver();
        ChessGame game = newGame(board, observer);

        // Act
        game.tryMove(at("a7"), at("a8"));

        // Assert
        assertTrue("Observer_Coronacion_AvisaMovimientoYCoronacion",
            observer.events.equals(Arrays.asList("move", "promotion")),
            "esperaba [move, promotion] y llegó " + observer.events);
    }
    // ---------- movimiento de las piezas que faltaban ----------

    private static void testBishopSlidesAndStopsAtOwnPiece() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e3"));

        // Act
        List<Position> moves = board.pieceAt(at("c1")).possibleMoves(board, at("c1"));

        // Assert
        assertTrue("testBishopSlidesAndStopsAtOwnPiece",
            moves.contains(at("d2")) && moves.contains(at("b2")) && moves.contains(at("a3"))
                && !moves.contains(at("e3")) && !moves.contains(at("f4")),
            "el alfil desliza en diagonal y frena antes de su propio peón en e3");
    }

    private static void testQueenMovesLikeRookAndBishop() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.QUEEN, Color.WHITE), at("d4"));

        // Act
        List<Position> moves = board.pieceAt(at("d4")).possibleMoves(board, at("d4"));

        // Assert
        assertTrue("testQueenMovesLikeRookAndBishop",
            moves.contains(at("d8")) && moves.contains(at("a4")) && moves.contains(at("h8"))
                && moves.contains(at("a1")) && !moves.contains(at("b5")),
            "la reina llega en línea recta y en diagonal, pero no en L");
    }

    private static void testKingMovesOneSquareInAnyDirection() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e4"));

        // Act
        List<Position> moves = board.pieceAt(at("e4")).possibleMoves(board, at("e4"));

        // Assert
        assertTrue("testKingMovesOneSquareInAnyDirection",
            moves.size() == 8 && moves.contains(at("d3")) && moves.contains(at("f5")) && !moves.contains(at("e6")),
            "el rey tiene 8 destinos a una casilla y ninguno a dos");
    }

    private static void testPawnDoubleStepOnlyFromStartAndWithFreePath() {
        // Arrange: tres tableros, el peón blanco en e2 siempre.
        Board free = new Board();
        free.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        Board blockedOnE3 = new Board();
        blockedOnE3.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        blockedOnE3.placePiece(piece(PieceType.PAWN, Color.BLACK), at("e3"));
        Board blockedOnE4 = new Board();
        blockedOnE4.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        blockedOnE4.placePiece(piece(PieceType.PAWN, Color.BLACK), at("e4"));

        // Act
        List<Position> onFree = free.pieceAt(at("e2")).possibleMoves(free, at("e2"));
        List<Position> onE3 = blockedOnE3.pieceAt(at("e2")).possibleMoves(blockedOnE3, at("e2"));
        List<Position> onE4 = blockedOnE4.pieceAt(at("e2")).possibleMoves(blockedOnE4, at("e2"));

        // Assert
        assertTrue("testPawnDoubleStepOnlyFromStartAndWithFreePath",
            onFree.contains(at("e3")) && onFree.contains(at("e4"))
                && !onE3.contains(at("e3")) && !onE3.contains(at("e4"))
                && onE4.contains(at("e3")) && !onE4.contains(at("e4")),
            "el doble paso necesita las dos casillas libres");
    }

    private static void testPawnCapturesOnlyDiagonally() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e4"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("d5"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("f5"));
        Board empty = new Board();
        empty.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e4"));

        // Act
        List<Position> withEnemies = board.pieceAt(at("e4")).possibleMoves(board, at("e4"));
        List<Position> withoutEnemies = empty.pieceAt(at("e4")).possibleMoves(empty, at("e4"));

        // Assert
        assertTrue("testPawnCapturesOnlyDiagonally",
            withEnemies.contains(at("d5")) && withEnemies.contains(at("f5")) && withEnemies.contains(at("e5"))
                && !withoutEnemies.contains(at("d5")) && !withoutEnemies.contains(at("f5")),
            "el peón captura en diagonal solo si hay una pieza rival, y avanza recto");
    }
    // ---------- reglas de la partida ----------

    private static void testRookCapturesThroughTryMove() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("a5"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("a1"), at("a5"));

        // Assert
        assertTrue("testRookCapturesThroughTryMove",
            outcome == MoveOutcome.OK && hasPiece(board, "a5", PieceType.ROOK)
                && board.pieceAt(at("a5")).getColor() == Color.WHITE,
            "la torre blanca debería quedar en a5 después de capturar el peón");
    }

    private static void testInvalidMoveLeavesBoardUntouched() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        Board before = board.cloneBoard();
        ChessGame game = newGame(board);

        // Act: la torre no se mueve en diagonal.
        MoveOutcome outcome = game.tryMove(at("a1"), at("b2"));

        // Assert
        assertTrue("testInvalidMoveLeavesBoardUntouched",
            outcome == MoveOutcome.INVALID && before.equals(board),
            "un movimiento inválido no debe cambiar el tablero");
    }

    private static void testPinnedPieceCannotMove() {
        // Arrange: el alfil blanco en e2 está clavado a su rey por la torre negra de e8.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("e2"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e2"), at("d3"));

        // Assert
        assertTrue("testPinnedPieceCannotMove", outcome == MoveOutcome.INVALID,
            "mover el alfil dejaría al rey en jaque por la columna e");
    }

    private static void testStalemateEndsTheGame() {
        // Arrange: ahogado clásico. Si la reina blanca va a g3, el rey negro de h1 no tiene casillas y no está en jaque.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("f2"));
        board.placePiece(piece(PieceType.QUEEN, Color.WHITE), at("g5"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h1"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("g5"), at("g3"));

        // Assert
        assertTrue("testStalemateEndsTheGame", outcome == MoveOutcome.STALEMATE && game.isGameOver(),
            "Dg3 deja a las negras sin movimientos y sin jaque: ahogado");
    }

    private static void testFinishedGameRejectsMoves() {
        // Arrange: mate del loco.
        ChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));
        game.tryMove(at("d8"), at("h4"));

        // Act
        MoveOutcome outcome = game.tryMove(at("e2"), at("e3"));

        // Assert
        assertTrue("testFinishedGameRejectsMoves", outcome == MoveOutcome.INVALID,
            "con la partida terminada no se puede mover");
    }

    private static void testUndoRestoresTheTurn() {
        // Arrange
        ChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("e2"), at("e4"));

        // Act
        boolean undone = game.undo();
        MoveOutcome blackFirst = game.tryMove(at("e7"), at("e5"));

        // Assert
        assertTrue("testUndoRestoresTheTurn",
            undone && game.getTurn() == Color.WHITE && blackFirst == MoveOutcome.INVALID,
            "después de deshacer la jugada blanca, vuelve a mover blancas");
    }

    private static void testInvalidMoveNotifiesObserver() {
        // Arrange
        RecordingObserver observer = new RecordingObserver();
        ChessGame game = newGame(BoardSetup.standard(PIECES), observer);

        // Act
        game.tryMove(at("e7"), at("e5"));

        // Assert
        assertTrue("testInvalidMoveNotifiesObserver", observer.events.equals(Arrays.asList("invalid")),
            "esperaba [invalid] y llegó " + observer.events);
    }
    // ---------- coronación y enroque, casos que faltaban ----------

    // Stub que devuelve siempre la misma pieza, para probar coronaciones distintas de la reina.
    private static class FixedChooser implements IPromotionChooser {
        private final PieceType choice;

        FixedChooser(PieceType choice) {
            this.choice = choice;
        }

        @Override
        public PieceType choose(Color color) {
            return choice;
        }
    }

    private static ChessGame newGameChoosing(Board board, PieceType choice) {
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(PIECES, new FixedChooser(choice)));
        return new ChessGame(board, Arrays.<IGameObserver>asList(), specialMoves);
    }

    private static Board boardWithPawnOnSeventh() {
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a7"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h4"));
        return board;
    }

    private static void testPromotionToKnight() {
        // Arrange
        Board board = boardWithPawnOnSeventh();
        ChessGame game = newGameChoosing(board, PieceType.KNIGHT);

        // Act
        MoveOutcome outcome = game.tryMove(at("a7"), at("a8"));

        // Assert
        assertTrue("testPromotionToKnight",
            outcome == MoveOutcome.OK && hasPiece(board, "a8", PieceType.KNIGHT)
                && board.pieceAt(at("a8")).getColor() == Color.WHITE,
            "en a8 debería haber un caballo blanco");
    }

    private static void testPromotionUndoRestoresThePawn() {
        // Arrange
        Board board = boardWithPawnOnSeventh();
        Board before = board.cloneBoard();
        ChessGame game = newGameChoosing(board, PieceType.ROOK);
        game.tryMove(at("a7"), at("a8"));

        // Act
        game.undo();

        // Assert
        assertTrue("testPromotionUndoRestoresThePawn", before.equals(board),
            "al deshacer la coronación vuelve el peón a a7 y a8 queda vacía");
    }

    private static void testCannotCastleWhileInCheck() {
        // Arrange: la torre negra de e8 da jaque al rey blanco por la columna e.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("a8"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("testCannotCastleWhileInCheck", outcome == MoveOutcome.INVALID,
            "no se puede enrocar estando en jaque");
    }

    private static void testCannotCastleWithPieceBetween() {
        // Arrange: el caballo blanco en g1 estorba el enroque corto.
        Board board = castlingBoard();
        board.placePiece(piece(PieceType.KNIGHT, Color.WHITE), at("g1"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("testCannotCastleWithPieceBetween", outcome == MoveOutcome.INVALID,
            "con una pieza entre el rey y la torre no se puede enrocar");
    }
}
