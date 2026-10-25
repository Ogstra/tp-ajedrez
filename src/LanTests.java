import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.Color;
import nucleo.IChessGame;
import nucleo.IDrawRule;
import nucleo.ISpecialMove;
import nucleo.Move;
import nucleo.Piece;
import nucleo.PieceFactory;
import nucleo.PieceType;
import nucleo.Position;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.EnPassantMove;
import nucleo.especiales.PromotionMove;
import puertos.IGameStatusListener;
import puertos.IMoveListener;
import red.LanAnnouncer;
import red.LanBrowser;
import red.LanConnection;
import red.LanGame;
import red.LanOpponent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Tests del juego por LAN. Mismo runner propio que los otros: Arrange, Act, Assert y salida
// [OK] / [FAIL]. Las dos "PCs" son dos partidas en este mismo programa, conectadas por la
// propia máquina (localhost), así que no hace falta una red de verdad.
public class LanTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testConnectAndGreet();
        testFoolsMateOverTheNetwork();
        testPromotionTravelsWithTheMove();
        testCastlingTravelsAsAKingMove();
        testLeavingIsSeenAsADisconnection();
        testAnnouncedGameShowsUpInTheList();

        System.out.println("\n" + passed + " pasados, " + failed + " fallados.");
        System.exit(failed > 0 ? 1 : 0);
    }

    // ---------- ayudas ----------

    private static void assertTrue(String name, boolean condition, String detail) {
        if (condition) {
            passed++;
            System.out.println("[OK]   " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + detail);
        }
    }

    // Las dos puntas de una conexión: [0] la de quien creó la partida, [1] la de quien se unió.
    private static LanConnection[] connectedPair() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0)) { // 0: cualquier puerto libre
            Future<LanConnection> host = executor.submit(() -> LanConnection.waitForRival(server));
            LanConnection guest = LanConnection.connect(InetAddress.getLoopbackAddress(), server.getLocalPort());
            return new LanConnection[]{host.get(), guest};
        } finally {
            executor.shutdown();
        }
    }

    // Una partida con su rival por red, que también decide las coronaciones.
    private static IChessGame newGame(Board board, LanOpponent lan, PieceFactory pieces) {
        ChessGame game = new ChessGame(board,
            Arrays.<IMoveListener>asList(lan),
            Collections.<IGameStatusListener>emptyList(),
            Arrays.<ISpecialMove>asList(new CastlingMove(), new EnPassantMove(), new PromotionMove(pieces, lan)),
            Collections.<IDrawRule>emptyList());
        lan.attachGame(game);
        return game;
    }

    // Aplica en una partida la jugada que llega por la red, como hace la ventana con el rival.
    private static void receive(IChessGame game, LanOpponent lan) {
        Move move = lan.chooseMove(game);
        game.tryMove(move.getFrom(), move.getTo());
    }

    private static Position at(String text) {
        return Position.fromAlgebraic(text);
    }

    // ---------- tests ----------

    private static void testConnectAndGreet() throws Exception {
        // Arrange
        LanConnection[] pair = connectedPair();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Act: los dos saludan a la vez, como pasa en la ventana.
        Future<String> hostSees = executor.submit(() -> pair[0].greet("Ana"));
        String guestSees = pair[1].greet("Beto");

        // Assert
        assertTrue("testConnectAndGreet", "Beto".equals(hostSees.get()) && "Ana".equals(guestSees),
            "cada uno debería recibir el nombre del otro: " + hostSees.get() + " / " + guestSees);
        executor.shutdown();
        pair[0].close();
        pair[1].close();
    }

    private static void testFoolsMateOverTheNetwork() throws Exception {
        // Arrange: blancas en la PC que creó la partida, negras en la que se unió.
        LanConnection[] pair = connectedPair();
        PieceFactory pieces = PieceFactory.standard();
        LanOpponent hostLan = new LanOpponent(pair[0], Color.WHITE, "Beto", color -> PieceType.QUEEN);
        LanOpponent guestLan = new LanOpponent(pair[1], Color.BLACK, "Ana", color -> PieceType.QUEEN);
        IChessGame host = newGame(BoardSetup.standard(pieces), hostLan, pieces);
        IChessGame guest = newGame(BoardSetup.standard(pieces), guestLan, pieces);

        // Act: cada uno juega en su partida y el otro recibe la jugada por la red.
        host.tryMove(at("f2"), at("f3"));
        receive(guest, guestLan);
        guest.tryMove(at("e7"), at("e5"));
        receive(host, hostLan);
        host.tryMove(at("g2"), at("g4"));
        receive(guest, guestLan);
        guest.tryMove(at("d8"), at("h4"));
        receive(host, hostLan);

        // Assert
        assertTrue("testFoolsMateOverTheNetwork",
            host.isGameOver() && guest.isGameOver() && host.getBoard().equals(guest.getBoard()),
            "las dos partidas deberían terminar en mate con el mismo tablero");
        pair[0].close();
        pair[1].close();
    }

    private static void testPromotionTravelsWithTheMove() throws Exception {
        // Arrange: un peón blanco a punto de coronar, y la persona de blancas elige caballo.
        LanConnection[] pair = connectedPair();
        PieceFactory pieces = PieceFactory.standard();
        LanOpponent hostLan = new LanOpponent(pair[0], Color.WHITE, "Beto", color -> PieceType.KNIGHT);
        LanOpponent guestLan = new LanOpponent(pair[1], Color.BLACK, "Ana", color -> PieceType.QUEEN);
        IChessGame host = newGame(promotionBoard(pieces), hostLan, pieces);
        IChessGame guest = newGame(promotionBoard(pieces), guestLan, pieces);

        // Act
        host.tryMove(at("e7"), at("e8"));
        receive(guest, guestLan);

        // Assert: en la otra PC también corona a caballo, sin preguntarle a nadie.
        Piece promoted = guest.getBoard().pieceAt(at("e8"));
        assertTrue("testPromotionTravelsWithTheMove",
            promoted != null && promoted.getType().equals(PieceType.KNIGHT) && host.getBoard().equals(guest.getBoard()),
            "el peón debería coronar a caballo en las dos partidas");
        pair[0].close();
        pair[1].close();
    }

    private static Board promotionBoard(PieceFactory pieces) {
        Board board = new Board();
        board.placePiece(pieces.create(PieceType.KING, Color.WHITE), at("a1"));
        board.placePiece(pieces.create(PieceType.KING, Color.BLACK), at("a8"));
        board.placePiece(pieces.create(PieceType.PAWN, Color.WHITE), at("e7"));
        return board;
    }

    private static void testCastlingTravelsAsAKingMove() throws Exception {
        // Arrange: rey y torre blancos listos para el enroque corto.
        LanConnection[] pair = connectedPair();
        PieceFactory pieces = PieceFactory.standard();
        LanOpponent hostLan = new LanOpponent(pair[0], Color.WHITE, "Beto", color -> PieceType.QUEEN);
        LanOpponent guestLan = new LanOpponent(pair[1], Color.BLACK, "Ana", color -> PieceType.QUEEN);
        IChessGame host = newGame(castlingBoard(pieces), hostLan, pieces);
        IChessGame guest = newGame(castlingBoard(pieces), guestLan, pieces);

        // Act
        host.tryMove(at("e1"), at("g1"));
        receive(guest, guestLan);

        // Assert: en la otra PC el rey quedó en g1 y la torre en f1.
        Piece rook = guest.getBoard().pieceAt(at("f1"));
        assertTrue("testCastlingTravelsAsAKingMove",
            rook != null && rook.getType().equals(PieceType.ROOK) && host.getBoard().equals(guest.getBoard()),
            "el enroque debería verse igual en las dos partidas");
        pair[0].close();
        pair[1].close();
    }

    private static Board castlingBoard(PieceFactory pieces) {
        Board board = new Board();
        board.placePiece(pieces.create(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(pieces.create(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(pieces.create(PieceType.KING, Color.BLACK), at("e8"));
        return board;
    }

    private static void testLeavingIsSeenAsADisconnection() throws Exception {
        // Arrange
        LanConnection[] pair = connectedPair();
        PieceFactory pieces = PieceFactory.standard();
        LanOpponent guestLan = new LanOpponent(pair[1], Color.BLACK, "Ana", color -> PieceType.QUEEN);
        IChessGame guest = newGame(BoardSetup.standard(pieces), guestLan, pieces);

        // Act: quien creó la partida se va antes de jugar.
        pair[0].close();
        Move move = guestLan.chooseMove(guest);

        // Assert
        assertTrue("testLeavingIsSeenAsADisconnection", move == null,
            "si el rival se va, no hay jugada: la ventana lo muestra como desconexión");
        pair[1].close();
    }

    private static void testAnnouncedGameShowsUpInTheList() throws InterruptedException {
        // Arrange
        LanBrowser browser = new LanBrowser();
        boolean listening = browser.start();
        LanAnnouncer announcer = new LanAnnouncer("Ana", LanConnection.PORT);

        // Act: se anuncia la partida y se espera hasta 3 segundos a que aparezca.
        announcer.start();
        LanGame found = null;
        for (int i = 0; i < 30 && found == null; i++) {
            Thread.sleep(100);
            for (LanGame game : browser.games()) {
                if (game.getHostName().equals("Ana")) found = game;
            }
        }
        announcer.stop();
        browser.stop();

        // Assert
        assertTrue("testAnnouncedGameShowsUpInTheList",
            listening && found != null && found.getPort() == LanConnection.PORT,
            "la partida de Ana debería aparecer en la lista con su puerto");
    }
}
