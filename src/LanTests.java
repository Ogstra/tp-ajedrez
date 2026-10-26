import core.Board;
import core.BoardSetup;
import core.ChessGame;
import core.Color;
import core.IChessGame;
import core.IDrawRule;
import core.ISpecialMove;
import core.Move;
import core.Piece;
import core.PieceFactory;
import core.PieceType;
import core.Position;
import core.special.CastlingMove;
import core.special.EnPassantMove;
import core.special.PromotionMove;
import ports.IGameStatusListener;
import ports.IMoveListener;
import network.LanAnnouncer;
import network.LanBrowser;
import network.LanConnection;
import network.LanGame;
import network.LanLobby;
import network.LanNames;
import network.LanOpponent;

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
        testDiscoveryPortIsNotAirPlay();
        testHostedGameUsesAFreePortAndConnects();
        testMalformedLineIsTreatedAsDisconnection();
        testIllegalMoveIsTreatedAsDisconnection();
        testLongLineIsRejected();
        testRivalNameIsCleaned();
        testNamesCleaning();
        testJoinCancelledOrRepeatedDoesNotStartAGame();

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
        LanAnnouncer announcer = new LanAnnouncer("Ana", 52817);

        // Act: se anuncia la partida y se espera hasta 3 segundos a que aparezca.
        announcer.start();
        LanGame found = waitForGame(browser, "Ana");
        announcer.stop();
        browser.stop();

        // Assert
        assertTrue("testAnnouncedGameShowsUpInTheList",
            listening && found != null && found.getPort() == 52817,
            "la partida de Ana debería aparecer en la lista con su puerto");
    }

    // Espera hasta 3 segundos a que aparezca en la lista la partida de ese nombre.
    private static LanGame waitForGame(LanBrowser browser, String hostName) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(100);
            for (LanGame game : browser.games()) {
                if (game.getHostName().equals(hostName)) return game;
            }
        }
        return null;
    }

    private static void testDiscoveryPortIsNotAirPlay() {
        // Assert: en macOS el receptor de AirPlay ocupa el 5000 y el 7000.
        int port = LanAnnouncer.DISCOVERY_PORT;
        assertTrue("testDiscoveryPortIsNotAirPlay", port != 5000 && port != 7000 && port > 1024,
            "el puerto de los avisos no puede ser uno de AirPlay ni uno reservado: " + port);
    }

    private static void testHostedGameUsesAFreePortAndConnects() throws Exception {
        // Arrange: Ana crea la partida con el lobby; Beto la busca en la lista.
        LanLobby hostLobby = new LanLobby();
        LanLobby guestLobby = new LanLobby();
        RecordingLobbyListener hostSide = new RecordingLobbyListener();
        RecordingLobbyListener guestSide = new RecordingLobbyListener();
        guestLobby.startBrowsing();

        // Act
        boolean hosting = hostLobby.host("Ana", hostSide);
        LanGame found = null;
        for (int i = 0; i < 30 && found == null; i++) {
            Thread.sleep(100);
            for (LanGame game : guestLobby.games()) {
                if (game.getHostName().equals("Ana")) found = game;
            }
        }
        if (found != null) guestLobby.join(found, "Beto", guestSide);
        boolean connected = hostSide.await() && guestSide.await();

        // Assert: el puerto lo eligió el sistema (no es el 5000 de AirPlay), y cada uno quedó
        // con su color y el nombre del otro.
        assertTrue("testHostedGameUsesAFreePortAndConnects",
            hosting && found != null && found.getPort() != 5000 && connected
                && hostSide.color == Color.WHITE && "Beto".equals(hostSide.rival)
                && guestSide.color == Color.BLACK && "Ana".equals(guestSide.rival),
            "la partida debería encontrarse y conectarse: " + found + " " + hostSide.rival + " / " + guestSide.rival);
        hostLobby.close();
        guestLobby.close();
        if (hostSide.connection != null) hostSide.connection.close();
        if (guestSide.connection != null) guestSide.connection.close();
    }

    // Registra lo que avisa el lobby (un mock simple) y deja esperar a que avise.
    private static class RecordingLobbyListener implements LanLobby.Listener {
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        volatile LanConnection connection;
        volatile Color color;
        volatile String rival;

        @Override
        public void connected(LanConnection connection, Color localColor, String rivalName) {
            this.connection = connection;
            this.color = localColor;
            this.rival = rivalName;
            done.countDown();
        }

        @Override
        public void failed(String message) {
            done.countDown();
        }

        boolean await() throws InterruptedException {
            return done.await(5, java.util.concurrent.TimeUnit.SECONDS) && connection != null;
        }
    }

    private static void testMalformedLineIsTreatedAsDisconnection() throws Exception {
        // Arrange
        LanConnection[] pair = connectedPair();
        PieceFactory pieces = PieceFactory.standard();
        LanOpponent hostLan = new LanOpponent(pair[0], Color.WHITE, "Beto", color -> PieceType.QUEEN);
        IChessGame host = newGame(BoardSetup.standard(pieces), hostLan, pieces);
        host.tryMove(at("e2"), at("e4"));
        pair[1].receive(); // la jugada de Ana llega a la otra punta

        // Act: del otro lado llega algo que no es una jugada.
        pair[1].send("<html><img src=x>");
        Move move = hostLan.chooseMove(host);

        // Assert
        assertTrue("testMalformedLineIsTreatedAsDisconnection", move == null && pair[1].receive() == null,
            "una línea que no es una jugada corta la conexión en vez de romper la partida");
        pair[1].close();
    }

    private static void testIllegalMoveIsTreatedAsDisconnection() throws Exception {
        // Arrange
        LanConnection[] pair = connectedPair();
        PieceFactory pieces = PieceFactory.standard();
        LanOpponent hostLan = new LanOpponent(pair[0], Color.WHITE, "Beto", color -> PieceType.QUEEN);
        IChessGame host = newGame(BoardSetup.standard(pieces), hostLan, pieces);
        host.tryMove(at("e2"), at("e4"));
        pair[1].receive();

        // Act: el rival "mueve" el rey negro de e8 a e1, que no es una jugada legal.
        pair[1].send("e8 e1");
        Move move = hostLan.chooseMove(host);

        // Assert: el núcleo decide qué es legal; la red solo propone.
        assertTrue("testIllegalMoveIsTreatedAsDisconnection",
            move == null && host.getBoard().pieceAt(at("e8")) != null,
            "una jugada ilegal que llega por la red no se aplica y corta la conexión");
        pair[1].close();
    }

    private static void testLongLineIsRejected() throws Exception {
        // Arrange
        LanConnection[] pair = connectedPair();
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 10_000; i++) huge.append('x');

        // Act
        pair[1].send(huge.toString());
        String received = pair[0].receive();

        // Assert
        assertTrue("testLongLineIsRejected", received == null,
            "una línea más larga de lo que usa el juego no se guarda en memoria");
        pair[0].close();
        pair[1].close();
    }

    private static void testRivalNameIsCleaned() throws Exception {
        // Arrange
        LanConnection[] pair = connectedPair();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Act: el rival manda un nombre con etiquetas HTML, saltos de línea y demasiado largo.
        Future<String> hostSees = executor.submit(() -> pair[0].greet("Ana"));
        pair[1].greet("<html><b>Beto</b>;con un nombre larguísimo");

        // Assert
        String name = hostSees.get();
        assertTrue("testRivalNameIsCleaned",
            name != null && !name.contains("<") && !name.contains(">") && !name.contains(";") && name.length() <= 20,
            "el nombre que llega por la red se limpia antes de mostrarlo: " + name);
        executor.shutdown();
        pair[0].close();
        pair[1].close();
    }

    private static void testNamesCleaning() {
        // Assert: casos de borde de la limpieza de nombres.
        assertTrue("testNamesCleaning",
            LanNames.clean(null).equals("rival") && LanNames.clean("  <>  ").equals("rival")
                && LanNames.clean("Ana").equals("Ana") && LanNames.clean("A\nna").equals("Ana"),
            "nulo o vacío queda como 'rival' y se sacan los caracteres de control");
    }

    private static void testJoinCancelledOrRepeatedDoesNotStartAGame() throws Exception {
        // Arrange: Ana crea la partida y Beto la ve en la lista.
        LanLobby hostLobby = new LanLobby();
        LanLobby guestLobby = new LanLobby();
        RecordingLobbyListener hostSide = new RecordingLobbyListener();
        RecordingLobbyListener guestSide = new RecordingLobbyListener();
        guestLobby.startBrowsing();
        hostLobby.host("Ana", hostSide);
        LanGame found = null;
        for (int i = 0; i < 30 && found == null; i++) {
            Thread.sleep(100);
            for (LanGame game : guestLobby.games()) {
                if (game.getHostName().equals("Ana")) found = game;
            }
        }

        // Act: Beto toca la partida dos veces (doble click) y enseguida Volver.
        boolean first = found != null && guestLobby.join(found, "Beto", guestSide);
        boolean second = found != null && guestLobby.join(found, "Beto", guestSide);
        guestLobby.close();
        boolean guestNotified = guestSide.done.await(2, java.util.concurrent.TimeUnit.SECONDS);

        // Assert: el segundo click no abre otra conexión y, cancelado, a Beto no le arranca ninguna partida.
        assertTrue("testJoinCancelledOrRepeatedDoesNotStartAGame", first && !second && !guestNotified,
            "unirse dos veces o cancelar no debería empezar una partida: " + first + " " + second + " " + guestNotified);
        hostLobby.close();
        if (hostSide.connection != null) hostSide.connection.close();
    }

}
