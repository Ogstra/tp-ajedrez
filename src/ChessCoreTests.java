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

    private static int pasados = 0;
    private static int fallados = 0;

    private static final PieceFactory PIECES = PieceFactory.standard();

    public static void main(String[] args) {
        testPeonAvanzaUnPaso();
        testPeonNoPuedeCapturarDeFrente();
        testCaballoSaltaEnL();
        testTorreNoAtraviesaPiezas();
        testReyNoPuedeMoverseACasillaAtacada();
        testNegrasNoPuedenMoverPrimero();
        testJaqueSeDetectaYNoEsMate();
        testMoveCommandUndoRestauraElTablero();
        testJaqueMateDelLoco();
        testEnroqueCortoMueveReyYTorre();
        testEnroqueLargoMueveReyYTorre();
        testNoSePuedeEnrocarSiElReyYaSeMovio();
        testNoSePuedeEnrocarPorCasillaAtacadaPorPeon();
        testDeshacerRecuperaElDerechoDeEnroque();
        testPeonCoronaAReina();
        testPiezaNuevaSinTocarClasesExistentes();
        testTableroDeOtroTamano();
        testCoronacionEnTableroChico();
        testObserverRecibeSoloEnroque();
        testObserverRecibeMovimientoYCoronacion();

        System.out.println("\n" + pasados + " pasados, " + fallados + " fallados.");
    }

    // ---------- ayudas de los tests ----------

    private static void assertTrue(String nombreTest, boolean condicion, String detalle) {
        if (condicion) {
            pasados++;
            System.out.println("[OK]   " + nombreTest);
        } else {
            fallados++;
            System.out.println("[FAIL] " + nombreTest + " -> " + detalle);
        }
    }

    private static Position at(String algebraico) {
        return Position.fromAlgebraic(algebraico);
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
        @Override public void onCastling(Color color, boolean cortoEnEnroque) { events.add("castling"); }
        @Override public void onPromotion(Color color, Position pos, PieceType nuevaPieza) { events.add("promotion"); }
    }

    // El armado que hace Main, sin consola: enroque + coronación (siempre a reina).
    private static ChessGame newGame(Board board, IGameObserver... observers) {
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(PIECES, new AlwaysQueenChooser()));
        return new ChessGame(board, Arrays.asList(observers), specialMoves);
    }

    // ---------- movimiento de cada pieza ----------

    private static void testPeonAvanzaUnPaso() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));

        // Act
        boolean puedeAvanzar = board.pieceAt(at("e2")).possibleMoves(board, at("e2")).contains(at("e3"));

        // Assert
        assertTrue("Peon_AvanzaUnPaso", puedeAvanzar, "e2->e3 debería ser un movimiento posible");
    }

    private static void testPeonNoPuedeCapturarDeFrente() {
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

    private static void testCaballoSaltaEnL() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KNIGHT, Color.WHITE), at("b1"));

        // Act
        List<Position> moves = board.pieceAt(at("b1")).possibleMoves(board, at("b1"));

        // Assert
        assertTrue("Caballo_SaltaEnL", moves.contains(at("c3")) && moves.contains(at("a3")),
            "desde b1 el caballo debería poder llegar a c3 y a a3");
    }

    private static void testTorreNoAtraviesaPiezas() {
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

    private static void testReyNoPuedeMoverseACasillaAtacada() {
        // Arrange: la torre negra en h2 controla toda la fila 2.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("h2"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome haciaCasillaAtacada = game.tryMove(at("e1"), at("e2"));
        MoveOutcome haciaCasillaSegura = game.tryMove(at("e1"), at("d1"));

        // Assert
        assertTrue("Rey_NoPuedeMoverseACasillaAtacada",
            haciaCasillaAtacada == MoveOutcome.INVALID && haciaCasillaSegura == MoveOutcome.OK,
            "e1->e2 queda en jaque por la torre (inválido), e1->d1 no (válido)");
    }

    private static void testNegrasNoPuedenMoverPrimero() {
        // Arrange
        ChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        MoveOutcome intentoDeNegras = game.tryMove(at("e7"), at("e5"));

        // Assert
        assertTrue("Turnos_NegrasNoMuevenPrimero", intentoDeNegras == MoveOutcome.INVALID,
            "en el primer turno solo pueden mover las blancas");
    }

    private static void testJaqueSeDetectaYNoEsMate() {
        // Arrange: la torre blanca puede subir a a8 y dar jaque al rey negro por la fila 8.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome resultado = game.tryMove(at("a1"), at("a8"));

        // Assert: el rey negro puede escapar (g7, h7), así que es jaque y no jaque mate.
        assertTrue("Jaque_SeDetectaYNoEsMate", resultado == MoveOutcome.CHECK && !game.isGameOver(),
            "Ta8 debería dar jaque sin ser mate");
    }

    private static void testMoveCommandUndoRestauraElTablero() {
        // Arrange (mismo test que la Tarea 03, adaptado a este tablero de ajedrez completo)
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        Board tableroAntes = board.cloneBoard();
        MoveCommand comando = new MoveCommand(board, at("e2"), at("e4"));

        // Act
        comando.execute();
        comando.undo();

        // Assert
        assertTrue("MoveCommand_ExecuteUndo_RestauraTablero", tableroAntes.equals(board),
            "el tablero después de undo() debería ser idéntico al de antes de execute()");
    }

    private static void testJaqueMateDelLoco() {
        // Arrange: el jaque mate más rápido posible, en 2 jugadas por bando.
        ChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));
        MoveOutcome ultimaJugada = game.tryMove(at("d8"), at("h4"));

        // Assert
        assertTrue("JaqueMate_MateDelLoco", ultimaJugada == MoveOutcome.CHECKMATE && game.isGameOver(),
            "Dxh4 debería ser jaque mate y terminar la partida");
    }

    // ---------- enroque ----------

    private static Board tableroParaEnrocar() {
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        return board;
    }

    private static boolean hay(Board board, String casilla, PieceType tipo) {
        Piece p = board.pieceAt(at(casilla));
        return p != null && p.getType().equals(tipo);
    }

    private static void testEnroqueCortoMueveReyYTorre() {
        // Arrange
        Board board = tableroParaEnrocar();
        ChessGame game = newGame(board);

        // Act
        MoveOutcome resultado = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_Corto_MueveReyYTorreJuntos",
            resultado == MoveOutcome.OK && hay(board, "g1", PieceType.KING) && hay(board, "f1", PieceType.ROOK),
            "tras enrocar corto, el rey debería estar en g1 y la torre en f1");
    }

    private static void testEnroqueLargoMueveReyYTorre() {
        // Arrange
        Board board = tableroParaEnrocar();
        ChessGame game = newGame(board);

        // Act
        MoveOutcome resultado = game.tryMove(at("e1"), at("c1"));

        // Assert
        assertTrue("Enroque_Largo_MueveReyYTorreJuntos",
            resultado == MoveOutcome.OK && hay(board, "c1", PieceType.KING) && hay(board, "d1", PieceType.ROOK),
            "tras enrocar largo, el rey debería estar en c1 y la torre en d1");
    }

    private static void testNoSePuedeEnrocarSiElReyYaSeMovio() {
        // Arrange
        Board board = tableroParaEnrocar();
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("a7"));
        ChessGame game = newGame(board);

        // Act: el rey sale y vuelve a e1 (las negras mueven el peón en el medio), y recién ahí intenta enrocar.
        game.tryMove(at("e1"), at("e2"));
        game.tryMove(at("a7"), at("a6"));
        game.tryMove(at("e2"), at("e1"));
        game.tryMove(at("a6"), at("a5"));
        MoveOutcome intentoDeEnroque = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_NoPermitidoSiElReyYaSeMovio", intentoDeEnroque == MoveOutcome.INVALID,
            "el rey ya se movió antes (aunque volvió a e1), no debería poder enrocar");
    }

    private static void testNoSePuedeEnrocarPorCasillaAtacadaPorPeon() {
        // Arrange: el peón negro en h2 ataca g1 en diagonal, aunque g1 esté vacía.
        Board board = tableroParaEnrocar();
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("h2"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome intentoDeEnroque = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_NoPermitidoSiElReyPisaCasillaAtacadaPorPeon", intentoDeEnroque == MoveOutcome.INVALID,
            "el rey terminaría en g1, atacada por el peón de h2");
    }

    private static void testDeshacerRecuperaElDerechoDeEnroque() {
        // Arrange
        Board board = tableroParaEnrocar();
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("a7"));
        ChessGame game = newGame(board);

        // Act: el rey se mueve, las negras mueven, y se deshacen las dos jugadas.
        game.tryMove(at("e1"), at("e2"));
        game.tryMove(at("a7"), at("a6"));
        game.undo();
        game.undo();
        MoveOutcome resultado = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Enroque_DeshacerRecuperaElDerecho", resultado == MoveOutcome.OK,
            "si se deshizo el único movimiento del rey, el enroque tiene que volver a estar permitido");
    }

    // ---------- coronación ----------

    private static void testPeonCoronaAReina() {
        // Arrange: el rey negro lejos de la fila 8 y de las diagonales de a8, para que no haya jaque.
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a7"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h4"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome resultado = game.tryMove(at("a7"), at("a8"));

        // Assert
        Piece piezaFinal = board.pieceAt(at("a8"));
        assertTrue("Peon_Corona_A_Reina", resultado == MoveOutcome.OK
            && piezaFinal.getType().equals(PieceType.QUEEN) && piezaFinal.getColor() == Color.WHITE,
            "en a8 debería haber una reina blanca, no un peón");
    }

    // ---------- extensibilidad (lo que pide la defensa individual) ----------

    // Una pieza que no existe en la materia: se mueve como torre y como caballo.
    // Se define ACÁ, en el test, sin tocar ninguna clase de nucleo/.
    private static class ChancellorMovementStrategy implements IMovementStrategy {
        private final RookMovementStrategy comoTorre = new RookMovementStrategy();
        private final KnightMovementStrategy comoCaballo = new KnightMovementStrategy();

        @Override
        public List<Position> possibleMoves(Board board, Position from, Color color) {
            List<Position> moves = new ArrayList<>(comoTorre.possibleMoves(board, from, color));
            moves.addAll(comoCaballo.possibleMoves(board, from, color));
            return moves;
        }
    }

    private static void testPiezaNuevaSinTocarClasesExistentes() {
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

    private static void testTableroDeOtroTamano() {
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

    private static void testCoronacionEnTableroChico() {
        // Arrange: tablero de 5x5; la última fila es la 5, no la 8.
        Board board = new Board(5, 5);
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a4"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("d3"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome resultado = game.tryMove(at("a4"), at("a5"));

        // Assert
        Piece piezaFinal = board.pieceAt(at("a5"));
        assertTrue("Tablero_5x5_CoronaEnLaUltimaFilaDeEseTablero",
            resultado == MoveOutcome.OK && piezaFinal.getType().equals(PieceType.QUEEN),
            "el peón debería coronar al llegar a la fila 5, que es la última de este tablero");
    }

    // ---------- el observer recibe lo que corresponde (con un mock) ----------

    private static void testObserverRecibeSoloEnroque() {
        // Arrange
        RecordingObserver observer = new RecordingObserver();
        ChessGame game = newGame(tableroParaEnrocar(), observer);

        // Act
        game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("Observer_Enroque_AvisaSoloEnroque", observer.events.equals(Arrays.asList("castling")),
            "esperaba [castling] y llegó " + observer.events);
    }

    private static void testObserverRecibeMovimientoYCoronacion() {
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
}
