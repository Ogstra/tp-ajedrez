import ai.AutoPromotionChooser;
import ai.GreedyOpponent;
import ai.MinimaxOpponent;
import ai.OpponentController;
import ai.RandomOpponent;
import java.util.Random;
import nucleo.IGameContext;
import nucleo.especiales.EnPassantMove;
import nucleo.IDrawRule;
import nucleo.tablas.FiftyMoveRule;
import nucleo.tablas.InsufficientMaterialRule;
import nucleo.tablas.ThreefoldRepetitionRule;
import nucleo.IChessGame;
import java.util.Collections;
import nucleo.CastlingCommand;
import nucleo.Move;
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
import puertos.IGameStatusListener;
import puertos.IMoveListener;
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

        testMoveValueEquality();
        testCommandsExposeTheirMove();

        testUndoAfterCheckmateReopensTheGame();

        testLegalMovesAtStartIsTwenty();

        testRedoRestoresTheMove();
        testRedoWithNothingToRedoReturnsFalse();
        testNewMoveClearsRedo();
        testRedoOfPromotionDoesNotAskAgain();

        testKingAgainstKingIsDraw();
        testInsufficientMaterialCases();
        testFiftyMoveRuleDeclaresDraw();
        testThreefoldRepetitionDeclaresDraw();
        testUndoAfterDrawReopensTheGame();
        testStatusListenerReceivesDraw();
        testEnPassantIsAllowedRightAfterTheDoubleStep();
        testEnPassantExpiresAfterOneMove();
        testEnPassantUndoRestoresTheCapturedPawn();
        testNewDrawRuleWithoutTouchingExistingClasses();
        testRandomOpponentChoosesALegalMove();
        testGreedyOpponentTakesTheQueen();
        testMinimaxOpponentFindsMateInOne();
        testSearchDoesNotTouchTheRealGame();
        testOpponentControllerPlaysOnlyOnItsTurn();
        testAutoPromotionChooserDoesNotAskForTheOpponent();
        testHardLevelRespondsInReasonableTime();
        testMinimaxRespectsLostCastlingRights();
        testGreedyOpponentPrefersEnPassantToQuietMoves();
        System.out.println("\n" + passed + " pasados, " + failed + " fallados.");
        System.exit(failed > 0 ? 1 : 0);
    }

    // Stub que cuenta cuántas veces se le preguntó.
    private static class CountingChooser implements IPromotionChooser {
        int calls = 0;

        @Override
        public PieceType choose(Color color) {
            calls++;
            return PieceType.QUEEN;
        }
    }

    // TDD: rehacer vuelve a aplicar la jugada deshecha.
    private static void testRedoRestoresTheMove() {
        // Arrange
        Board board = BoardSetup.standard(PIECES);
        IChessGame game = newGame(board);
        game.tryMove(at("e2"), at("e4"));
        Board afterMove = board.cloneBoard();
        game.undo();

        // Act
        boolean redone = game.redo();

        // Assert
        assertTrue("testRedoRestoresTheMove",
            redone && afterMove.equals(board) && game.getTurn() == Color.BLACK,
            "después de rehacer, el peón vuelve a e4 y le toca a las negras");
    }

    // TDD: no se puede rehacer sin jugadas deshechas.
    private static void testRedoWithNothingToRedoReturnsFalse() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act / Assert
        assertTrue("testRedoWithNothingToRedoReturnsFalse", !game.redo(),
            "sin nada deshecho no hay qué rehacer");
    }

    // TDD: una jugada nueva borra lo que se podía rehacer.
    private static void testNewMoveClearsRedo() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("e2"), at("e4"));
        game.undo();
        game.tryMove(at("d2"), at("d4"));

        // Act
        boolean redone = game.redo();

        // Assert
        assertTrue("testNewMoveClearsRedo", !redone,
            "después de jugar algo distinto ya no se puede rehacer la jugada vieja");
    }

    // TDD: rehacer una coronación no vuelve a preguntar la pieza.
    private static void testRedoOfPromotionDoesNotAskAgain() {
        // Arrange
        Board board = boardWithPawnOnSeventh();
        CountingChooser chooser = new CountingChooser();
        IChessGame game = buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(), chooser);
        game.tryMove(at("a7"), at("a8"));
        game.undo();

        // Act
        game.redo();

        // Assert
        assertTrue("testRedoOfPromotionDoesNotAskAgain",
            chooser.calls == 1 && hasPiece(board, "a8", PieceType.QUEEN),
            "el comando ya guarda la pieza elegida: se pregunta una sola vez");
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


    // El armado que hace Main, sin consola: enroque + coronación (siempre a reina).


    // ---------- movimiento de cada pieza ----------

    private static void testPawnAdvancesOneSquare() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));

        // Act
        boolean canAdvance = board.pieceAt(at("e2")).possibleMoves(board, at("e2")).contains(at("e3"));

        // Assert
        assertTrue("Peon_AvanzaUnPaso", canAdvance, "e2->e3 debería ser un movimiento posible");
    }

    private static void testPawnCannotCaptureStraightAhead() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("e3"));

        // Act
        boolean attemptsForwardCapture = board.pieceAt(at("e2")).possibleMoves(board, at("e2")).contains(at("e3"));

        // Assert
        assertTrue("Peon_NoCapturaDeFrente", !attemptsForwardCapture,
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

    private static boolean hasPiece(Board board, String square, PieceType type) {
        Piece p = board.pieceAt(at(square));
        return p != null && p.getType().equals(type);
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



    private static Board boardWithPawnOnSeventh() {
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("h7"));

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
    // TDD: Move tiene que ser un value object (igualdad por contenido).
    private static void testMoveValueEquality() {
        // Arrange
        Move a = new Move(at("e2"), at("e4"));
        Move b = new Move(at("e2"), at("e4"));
        Move c = new Move(at("e2"), at("e3"));

        // Act / Assert
        assertTrue("testMoveValueEquality", a.equals(b) && a.hashCode() == b.hashCode() && !a.equals(c),
            "dos Move con las mismas casillas son iguales");
    }

    // TDD: cada comando sabe decir qué jugada representa.
    private static void testCommandsExposeTheirMove() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        MoveCommand simple = new MoveCommand(board, at("e2"), at("e4"));
        CastlingCommand castling = new CastlingCommand(board, at("e1"), at("g1"), at("h1"), at("f1"));

        // Act / Assert
        assertTrue("testCommandsExposeTheirMove",
            simple.getMove().equals(new Move(at("e2"), at("e4")))
                && castling.getMove().equals(new Move(at("e1"), at("g1"))),
            "el enroque se representa con el movimiento del rey");
    }
    // TDD: se puede deshacer la jugada que terminó la partida y seguir jugando.
    private static void testUndoAfterCheckmateReopensTheGame() {
        // Arrange: mate del loco.
        ChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));
        game.tryMove(at("d8"), at("h4"));

        // Act
        boolean undone = game.undo();
        MoveOutcome again = game.tryMove(at("d8"), at("h4"));

        // Assert
        assertTrue("testUndoAfterCheckmateReopensTheGame",
            undone && again == MoveOutcome.CHECKMATE && game.isGameOver(),
            "al deshacer el mate la partida sigue; al repetir la jugada vuelve a terminar");
    }
    // Mock (Clase 4): registra qué le avisaron, para verificarlo en el Assert.
    private static class RecordingObserver implements IMoveListener, IGameStatusListener {
        final List<String> events = new ArrayList<>();

        @Override public void onMoveMade(Position from, Position to, Piece piece, Piece captured) { events.add("move"); }
        @Override public void onCastling(Color color, boolean kingside) { events.add("castling"); }
        @Override public void onPromotion(Color color, Position position, PieceType newPiece) { events.add("promotion"); }
        @Override public void onUndo() { events.add("undo"); }
        @Override public void onDraw(String reason) { events.add("draw"); }
        @Override public void onRedo() { events.add("redo"); }
        @Override public void onInvalidMove(String reason) { events.add("invalid"); }
        @Override public void onCheck(Color colorInCheck) { events.add("check"); }
        @Override public void onCheckmate(Color winner) { events.add("checkmate"); }
        @Override public void onStalemate() { events.add("stalemate"); }
    }

    // El armado que hace Main, sin consola. Un solo lugar para armar partidas de prueba.
    private static ChessGame buildGame(Board board, List<IMoveListener> moveListeners,
                                       List<IGameStatusListener> statusListeners,
                                       IPromotionChooser chooser, List<IDrawRule> drawRules) {
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new EnPassantMove(),
            new PromotionMove(PIECES, chooser));
        return new ChessGame(board, moveListeners, statusListeners, specialMoves, drawRules);
    }

    private static List<IDrawRule> standardDrawRules() {
        return Arrays.<IDrawRule>asList(
            new InsufficientMaterialRule(), new FiftyMoveRule(), new ThreefoldRepetitionRule());
    }

    private static ChessGame buildGame(Board board, List<IMoveListener> moveListeners,
                                       List<IGameStatusListener> statusListeners, IPromotionChooser chooser) {
        return buildGame(board, moveListeners, statusListeners, chooser, standardDrawRules());
    }

    private static ChessGame newGameWithDrawRules(Board board, IDrawRule... rules) {
        return buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(),
            new AlwaysQueenChooser(), Arrays.asList(rules));
    }

    private static ChessGame newGame(Board board) {
        return buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(),
            new AlwaysQueenChooser());
    }

    private static ChessGame newGame(Board board, RecordingObserver observer) {
        return buildGame(board, Collections.<IMoveListener>singletonList(observer),
            Collections.<IGameStatusListener>singletonList(observer), new AlwaysQueenChooser());
    }

    private static ChessGame newGameChoosing(Board board, PieceType choice) {
        return buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(),
            new FixedChooser(choice));
    }
    // TDD: el puerto de entrada ofrece todas las jugadas legales del que tiene el turno.
    private static void testLegalMovesAtStartIsTwenty() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        List<Move> moves = game.legalMoves();

        // Assert
        assertTrue("testLegalMovesAtStartIsTwenty",
            moves.size() == 20 && moves.contains(new Move(at("e2"), at("e4")))
                && moves.contains(new Move(at("g1"), at("f3"))),
            "al empezar hay 20 jugadas legales (16 de peón y 4 de caballo)");
    }
    // TDD: rey contra rey son tablas.
    private static void testKingAgainstKingIsDraw() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        IChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("e2"));

        // Assert
        assertTrue("testKingAgainstKingIsDraw", outcome == MoveOutcome.DRAW && game.isGameOver(),
            "con solo dos reyes no hay forma de ganar");
    }

    // TDD
    private static void testInsufficientMaterialCases() {
        // Arrange: cuatro tableros. Caso por caso: [rey+alfil vs rey, rey+torre vs rey,
        // alfiles del mismo color, alfiles de distinto color].
        Board bishopVsKing = new Board();
        bishopVsKing.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        bishopVsKing.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        bishopVsKing.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));

        Board rookVsKing = new Board();
        rookVsKing.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        rookVsKing.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        rookVsKing.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));

        Board sameColorBishops = new Board();
        sameColorBishops.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        sameColorBishops.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        sameColorBishops.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        sameColorBishops.placePiece(piece(PieceType.BISHOP, Color.BLACK), at("f8"));

        Board oppositeColorBishops = new Board();
        oppositeColorBishops.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        oppositeColorBishops.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        oppositeColorBishops.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        oppositeColorBishops.placePiece(piece(PieceType.BISHOP, Color.BLACK), at("c8"));

        InsufficientMaterialRule rule = new InsufficientMaterialRule();

        // Act
        boolean a = rule.reason(newGameWithDrawRules(bishopVsKing)) != null;
        boolean b = rule.reason(newGameWithDrawRules(rookVsKing)) != null;
        boolean c = rule.reason(newGameWithDrawRules(sameColorBishops)) != null;
        boolean d = rule.reason(newGameWithDrawRules(oppositeColorBishops)) != null;

        // Assert
        assertTrue("testInsufficientMaterialCases", a && !b && c && !d,
            "alfil solo y alfiles del mismo color no alcanzan; torre sola y alfiles de colores distintos sí");
    }

    // TDD: 100 medias jugadas sin captura ni movimiento de peón (acá el límite es 4 para no jugar 100).
    private static void testFiftyMoveRuleDeclaresDraw() {
        // Arrange
        Board board = rooksOnTheHFile();
        IChessGame game = newGameWithDrawRules(board, new FiftyMoveRule(4));
        String[][] moves = {{"h1", "h2"}, {"h8", "h7"}, {"h2", "h1"}, {"h7", "h8"}};

        // Act
        MoveOutcome last = null;
        MoveOutcome beforeLast = null;
        for (int i = 0; i < moves.length; i++) {
            MoveOutcome outcome = game.tryMove(at(moves[i][0]), at(moves[i][1]));
            if (i == moves.length - 2) beforeLast = outcome;
            last = outcome;
        }

        // Assert
        assertTrue("testFiftyMoveRuleDeclaresDraw", beforeLast == MoveOutcome.OK && last == MoveOutcome.DRAW,
            "las tablas llegan recién en la cuarta media jugada sin captura ni peón");
    }

    private static Board rooksOnTheHFile() {
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("h8"));
        return board;
    }

    // TDD: la misma posición con el mismo turno tres veces.
    private static void testThreefoldRepetitionDeclaresDraw() {
        // Arrange: las dos torres van y vienen; la posición inicial reaparece cada 4 medias jugadas.
        Board board = rooksOnTheHFile();
        IChessGame game = newGameWithDrawRules(board, new ThreefoldRepetitionRule());
        String[][] cycle = {{"h1", "h2"}, {"h8", "h7"}, {"h2", "h1"}, {"h7", "h8"}};

        // Act
        boolean earlyDraw = false;
        MoveOutcome last = null;
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < cycle.length; i++) {
                last = game.tryMove(at(cycle[i][0]), at(cycle[i][1]));
                boolean isFinalMove = round == 1 && i == cycle.length - 1;
                if (last == MoveOutcome.DRAW && !isFinalMove) earlyDraw = true;
            }
        }

        // Assert
        assertTrue("testThreefoldRepetitionDeclaresDraw", !earlyDraw && last == MoveOutcome.DRAW,
            "la posición inicial aparece por tercera vez recién en la jugada 8");
    }

    // TDD: se puede deshacer la jugada que declaró las tablas.
    private static void testUndoAfterDrawReopensTheGame() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("e2"));

        // Act
        boolean undone = game.undo();

        // Assert
        assertTrue("testUndoAfterDrawReopensTheGame", undone && !game.isGameOver(),
            "al deshacer las tablas la partida sigue");
    }

    // TDD: los listeners de estado se enteran de las tablas.
    private static void testStatusListenerReceivesDraw() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        RecordingObserver observer = new RecordingObserver();
        IChessGame game = newGame(board, observer);

        // Act
        game.tryMove(at("e1"), at("e2"));

        // Assert
        assertTrue("testStatusListenerReceivesDraw", observer.events.equals(Arrays.asList("move", "draw")),
            "esperaba [move, draw] y llegó " + observer.events);
    }
    // El tablero de los tests de captura al paso: el peón blanco de e5 espera al peón negro de d7.
    private static Board enPassantBoard() {
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e5"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("d7"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        return board;
    }

    // TDD: justo después del doble paso del peón rival se puede capturar al paso.
    private static void testEnPassantIsAllowedRightAfterTheDoubleStep() {
        // Arrange
        Board board = enPassantBoard();
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));   // las blancas esperan
        game.tryMove(at("d7"), at("d5"));   // doble paso negro, al lado del peón blanco

        // Act
        MoveOutcome outcome = game.tryMove(at("e5"), at("d6"));

        // Assert
        assertTrue("testEnPassantIsAllowedRightAfterTheDoubleStep",
            outcome == MoveOutcome.OK && hasPiece(board, "d6", PieceType.PAWN)
                && board.pieceAt(at("d6")).getColor() == Color.WHITE && board.pieceAt(at("d5")) == null,
            "el peón blanco queda en d6 y el negro de d5 desaparece");
    }

    // TDD: la oportunidad dura una sola jugada.
    private static void testEnPassantExpiresAfterOneMove() {
        // Arrange
        Board board = enPassantBoard();
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));
        game.tryMove(at("d7"), at("d5"));
        game.tryMove(at("f1"), at("g1"));   // las blancas no capturan
        game.tryMove(at("e8"), at("f8"));   // las negras mueven el rey

        // Act
        MoveOutcome outcome = game.tryMove(at("e5"), at("d6"));

        // Assert
        assertTrue("testEnPassantExpiresAfterOneMove", outcome == MoveOutcome.INVALID,
            "ya no se puede capturar al paso una jugada después");
    }

    // TDD: deshacer la captura al paso devuelve el peón capturado.
    private static void testEnPassantUndoRestoresTheCapturedPawn() {
        // Arrange
        Board board = enPassantBoard();
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));
        game.tryMove(at("d7"), at("d5"));
        Board afterDoubleStep = board.cloneBoard();
        game.tryMove(at("e5"), at("d6"));

        // Act
        game.undo();

        // Assert
        assertTrue("testEnPassantUndoRestoresTheCapturedPawn", afterDoubleStep.equals(board),
            "al deshacer vuelven el peón blanco a e5 y el negro a d5");
    }
    // Una regla de tablas inventada en el test: declara tablas apenas hay una jugada hecha.
    private static class AlwaysDrawRule implements IDrawRule {
        @Override
        public String reason(IGameContext context) {
            return context.lastMove() != null ? "regla de prueba" : null;
        }
    }

    private static void testNewDrawRuleWithoutTouchingExistingClasses() {
        // Arrange
        IChessGame game = newGameWithDrawRules(BoardSetup.standard(PIECES), new AlwaysDrawRule());

        // Act
        MoveOutcome outcome = game.tryMove(at("e2"), at("e4"));

        // Assert
        assertTrue("testNewDrawRuleWithoutTouchingExistingClasses", outcome == MoveOutcome.DRAW,
            "una regla de tablas nueva se enchufa por constructor sin editar ChessGame");
    }
    // TDD: el nivel fácil siempre elige una jugada legal.
    private static void testRandomOpponentChoosesALegalMove() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        Move move = new RandomOpponent(new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testRandomOpponentChoosesALegalMove", game.legalMoves().contains(move),
            "la jugada elegida tiene que estar entre las legales");
    }

    // TDD: el nivel medio prefiere capturar la pieza de más valor.
    private static void testGreedyOpponentTakesTheQueen() {
        // Arrange: la torre blanca puede capturar la reina de a6 (valor 9) o el peón de c1 (valor 1).
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.QUEEN, Color.BLACK), at("a6"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("c1"));
        IChessGame game = newGame(board);

        // Act
        Move move = new GreedyOpponent(new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testGreedyOpponentTakesTheQueen", move.equals(new Move(at("a1"), at("a6"))),
            "entre la reina y el peón, elige la reina; eligió " + move);
    }

    // TDD: el nivel difícil encuentra un mate en una jugada.
    private static void testMinimaxOpponentFindsMateInOne() {
        // Arrange: después de 1.f3 e5 2.g4 las negras tienen Dh4#.
        IChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));

        // Act
        Move move = new MinimaxOpponent(2, new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testMinimaxOpponentFindsMateInOne", move.equals(new Move(at("d8"), at("h4"))),
            "la única jugada que da mate es Dh4; eligió " + move);
    }

    // TDD: pensar no toca la partida real ni avisa a los listeners.
    private static void testSearchDoesNotTouchTheRealGame() {
        // Arrange
        Board board = BoardSetup.standard(PIECES);
        RecordingObserver observer = new RecordingObserver();
        IChessGame game = newGame(board, observer);
        game.tryMove(at("e2"), at("e4"));
        Board before = board.cloneBoard();

        // Act
        new MinimaxOpponent(2, new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testSearchDoesNotTouchTheRealGame",
            before.equals(board) && observer.events.equals(Arrays.asList("move")),
            "la búsqueda trabaja sobre una copia: el tablero y los avisos quedan como estaban");
    }

    // TDD: el controlador solo juega cuando es el turno de la IA.
    private static void testOpponentControllerPlaysOnlyOnItsTurn() {
        // Arrange
        Board board = BoardSetup.standard(PIECES);
        IChessGame game = newGame(board);
        OpponentController controller = new OpponentController(game, new RandomOpponent(new Random(7)), Color.BLACK);

        // Act
        boolean playedOnWhitesTurn = controller.playIfItsTurn();
        game.tryMove(at("e2"), at("e4"));
        boolean playedOnItsTurn = controller.playIfItsTurn();

        // Assert
        assertTrue("testOpponentControllerPlaysOnlyOnItsTurn",
            !playedOnWhitesTurn && playedOnItsTurn && game.getTurn() == Color.WHITE,
            "no juega en el turno de las blancas; juega en el suyo y devuelve el turno");
    }

    // TDD: la IA corona sola, sin preguntar.
    private static void testAutoPromotionChooserDoesNotAskForTheOpponent() {
        // Arrange
        CountingChooser human = new CountingChooser();
        AutoPromotionChooser chooser = new AutoPromotionChooser(Color.BLACK, human);

        // Act
        PieceType forBlack = chooser.choose(Color.BLACK);
        chooser.choose(Color.WHITE);

        // Assert
        assertTrue("testAutoPromotionChooserDoesNotAskForTheOpponent",
            forBlack.equals(PieceType.QUEEN) && human.calls == 1,
            "para el bando de la IA corona a reina sin preguntar; para el humano pregunta");
    }

    // TDD: el nivel difícil contesta en un tiempo razonable desde la posición inicial.
    private static void testHardLevelRespondsInReasonableTime() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("e2"), at("e4"));
        long start = System.currentTimeMillis();

        // Act
        new MinimaxOpponent(2, new Random(7)).chooseMove(game);
        long elapsed = System.currentTimeMillis() - start;

        // Assert
        System.out.println("       (nivel difícil, profundidad 2: " + elapsed + " ms)");
        assertTrue("testHardLevelRespondsInReasonableTime", elapsed < 15000,
            "tardó " + elapsed + " ms; si pasa los 15 segundos hay que optimizar o bajar la profundidad");
    }
    // TDD: la copia de busqueda no debe habilitar un enroque perdido en la partida real.
    private static void testMinimaxRespectsLostCastlingRights() {
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("a8"));
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));
        game.tryMove(at("e8"), at("f8"));
        game.tryMove(at("f1"), at("e1"));
        game.tryMove(at("f8"), at("e8"));
        List<Move> legal = game.legalMoves();
        boolean allLegal = !legal.contains(new Move(at("e1"), at("g1")));
        for (int seed = 0; seed < 64; seed++) {
            Move chosen = new MinimaxOpponent(1, new Random(seed)).chooseMove(game);
            allLegal = allLegal && legal.contains(chosen);
        }
        assertTrue("testMinimaxRespectsLostCastlingRights", allLegal,
            "el oponente solo puede elegir jugadas legales en la partida real");
    }
    // TDD: la captura al paso vale un peon aunque la casilla de destino este vacia.
    private static void testGreedyOpponentPrefersEnPassantToQuietMoves() {
        Board board = enPassantBoard();
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));
        game.tryMove(at("d7"), at("d5"));
        Move capture = new Move(at("e5"), at("d6"));
        boolean alwaysCaptures = true;
        for (int seed = 0; seed < 16; seed++) {
            Move chosen = new GreedyOpponent(new Random(seed)).chooseMove(game);
            alwaysCaptures = alwaysCaptures && capture.equals(chosen);
        }
        assertTrue("testGreedyOpponentPrefersEnPassantToQuietMoves", alwaysCaptures,
            "la unica captura tiene mas valor que cualquier jugada tranquila");
    }
}
