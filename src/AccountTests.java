import adaptadores.FileAccountStore;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.Color;
import nucleo.IChessGame;
import nucleo.IDrawRule;
import nucleo.ISpecialMove;
import nucleo.Piece;
import nucleo.PieceFactory;
import nucleo.PieceType;
import nucleo.Position;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.EnPassantMove;
import nucleo.especiales.PromotionMove;
import nucleo.tablas.FiftyMoveRule;
import nucleo.tablas.InsufficientMaterialRule;
import nucleo.tablas.ThreefoldRepetitionRule;
import puertos.IAccountStore;
import puertos.IGameStatusListener;
import puertos.IMoveListener;
import puertos.IPromotionChooser;
import usuarios.AccountService;
import usuarios.Result;
import usuarios.Score;
import usuarios.ScoreRecorder;
import usuarios.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Tests de usuarios y ranking. Mismo runner propio que ChessCoreTests: Arrange, Act,
// Assert y salida [OK] / [FAIL]. Los que se escribieron antes que el código van
// marcados con "// TDD:".
public class AccountTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        testRegisterAssignsConsecutiveIds();
        testRegisterRejectsDuplicateNameIgnoringCase();
        testRegisterRejectsInvalidNames();
        testLoginByNameIgnoringCase();
        testScorePoints();
        testRankingOrdersByPointsThenWinsThenName();
        testRecordAndRevertAdjustScores();
        testAnotherServiceReadsWhatWasSaved();
        testFileStoreRoundTripWithAccents();
        testFileStoreMissingFileIsEmpty();
        testFileStoreSkipsMalformedLines();
        testCheckmateCreditsWinnerAndLoser();
        testStalemateCreditsDrawToBoth();
        testDrawCreditsBoth();
        testNoBlackUserCreditsOnlyWhite();
        testNoUsersRecordsNothing();
        testUndoRevertsTheResult();
        testRedoReappliesTheResult();
        testRedoOfAnEarlierMoveDoesNotReapply();
        testBlackUserCanChangeDuringTheGame();

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

    // Fake (Clase 4): una implementación simple pero funcional, en memoria, del puerto de archivo.
    private static class InMemoryAccountStore implements IAccountStore {
        private List<Score> saved = new ArrayList<>();
        int saves = 0;

        @Override
        public List<Score> loadAll() {
            return new ArrayList<>(saved);
        }

        @Override
        public void saveAll(List<Score> scores) {
            saved = new ArrayList<>(scores);
            saves++;
        }
    }

    private static AccountService newService() {
        return new AccountService(new InMemoryAccountStore());
    }

    private static Score scoreOf(AccountService service, User user) {
        for (Score score : service.ranking()) {
            if (score.getUser().equals(user)) return score;
        }
        return null;
    }

    private static Position at(String text) {
        return Position.fromAlgebraic(text);
    }

    // El armado que hace Main, sin consola, con el recorder escuchando todo.
    private static ChessGame newGame(Board board, ScoreRecorder recorder) {
        PieceFactory pieces = PieceFactory.standard();
        IPromotionChooser alwaysQueen = new IPromotionChooser() {
            @Override
            public PieceType choose(Color color) {
                return PieceType.QUEEN;
            }
        };
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(), new EnPassantMove(), new PromotionMove(pieces, alwaysQueen));
        List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
            new InsufficientMaterialRule(), new FiftyMoveRule(), new ThreefoldRepetitionRule());
        ChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(recorder),
            Collections.<IGameStatusListener>singletonList(recorder),
            specialMoves, drawRules);
        recorder.attachGame(game);
        return game;
    }

    private static void foolsCheckmate(IChessGame game) {
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));
        game.tryMove(at("d8"), at("h4"));
    }

    private static Board kingsOnly() {
        Board board = new Board();
        PieceFactory pieces = PieceFactory.standard();
        board.placePiece(pieces.create(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(pieces.create(PieceType.KING, Color.BLACK), at("e8"));
        return board;
    }

    // ---------- registro y login ----------

    // TDD: el sistema asigna los ids, de a uno, sin que el usuario elija.
    private static void testRegisterAssignsConsecutiveIds() {
        // Arrange
        AccountService service = newService();

        // Act
        User ana = service.register("Ana");
        User beto = service.register("Beto");

        // Assert
        assertTrue("testRegisterAssignsConsecutiveIds",
            ana.getId() == 1 && beto.getId() == 2 && ana.getName().equals("Ana"),
            "ids " + ana.getId() + " y " + beto.getId());
    }

    // TDD
    private static void testRegisterRejectsDuplicateNameIgnoringCase() {
        // Arrange
        AccountService service = newService();
        service.register("Ana");

        // Act
        boolean rejected = false;
        try {
            service.register("ANA");
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        // Assert
        assertTrue("testRegisterRejectsDuplicateNameIgnoringCase", rejected && service.ranking().size() == 1,
            "no debería dejar registrar 'ANA' si ya existe 'Ana'");
    }

    // TDD: el nombre se guarda en un archivo de texto separado por ";", así que no puede romperlo.
    private static void testRegisterRejectsInvalidNames() {
        // Arrange
        AccountService service = newService();
        String[] invalid = {"", "   ", "a;b", "linea\nnueva", "nombre demasiado largo para ser aceptado"};

        // Act
        int rejected = 0;
        for (String name : invalid) {
            try {
                service.register(name);
            } catch (IllegalArgumentException e) {
                rejected++;
            }
        }

        // Assert
        assertTrue("testRegisterRejectsInvalidNames", rejected == invalid.length && service.ranking().isEmpty(),
            "se rechazaron " + rejected + " de " + invalid.length);
    }

    // TDD: se entra solo con el nombre de usuario; sin contraseña ni id.
    private static void testLoginByNameIgnoringCase() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        service.register("Beto");

        // Act
        User ok = service.login(" ana ");
        User unknown = service.login("Carla");

        // Assert
        assertTrue("testLoginByNameIgnoringCase",
            ok != null && ok.equals(ana) && unknown == null,
            "login correcto: " + ok + ", usuario inexistente: " + unknown);
    }

    // ---------- puntaje y ranking ----------

    private static void testScorePoints() {
        // Arrange
        Score score = new Score(new User(1, "Ana"), 2, 1, 3);

        // Act / Assert
        assertTrue("testScorePoints", score.getGames() == 6 && score.getPoints() == 7,
            "2 victorias, 1 empate y 3 derrotas son 6 partidas y 7 puntos (3 + 3 + 1)");
    }

    // TDD: el ranking ordena por puntos, después por victorias y después por nombre.
    private static void testRankingOrdersByPointsThenWinsThenName() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        User carla = service.register("Carla");
        User dani = service.register("Dani");
        service.record(ana, Result.WIN);                                    // 3 puntos, 1 victoria
        service.record(beto, Result.DRAW);
        service.record(beto, Result.DRAW);
        service.record(beto, Result.DRAW);                                  // 3 puntos, 0 victorias
        service.record(carla, Result.WIN);
        service.record(carla, Result.WIN);                                  // 6 puntos
        // dani queda con 0

        // Act
        List<Score> ranking = service.ranking();

        // Assert
        assertTrue("testRankingOrdersByPointsThenWinsThenName",
            ranking.get(0).getUser().equals(carla) && ranking.get(1).getUser().equals(ana)
                && ranking.get(2).getUser().equals(beto) && ranking.get(3).getUser().equals(dani),
            "orden obtenido: " + ranking);
    }

    // TDD
    private static void testRecordAndRevertAdjustScores() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");

        // Act
        service.record(ana, Result.WIN);
        service.record(ana, Result.LOSS);
        service.revert(ana, Result.WIN);

        // Assert
        Score score = scoreOf(service, ana);
        assertTrue("testRecordAndRevertAdjustScores",
            score.getWins() == 0 && score.getLosses() == 1 && score.getDraws() == 0,
            "quedó " + score.getWins() + "-" + score.getDraws() + "-" + score.getLosses());
    }

    // TDD: los datos viven en el almacenamiento, no en el servicio.
    private static void testAnotherServiceReadsWhatWasSaved() {
        // Arrange
        InMemoryAccountStore store = new InMemoryAccountStore();
        AccountService first = new AccountService(store);
        User ana = first.register("Ana");
        first.record(ana, Result.WIN);

        // Act
        AccountService second = new AccountService(store);

        // Assert
        Score score = scoreOf(second, ana);
        assertTrue("testAnotherServiceReadsWhatWasSaved",
            score != null && score.getWins() == 1 && second.login("Ana") != null,
            "el segundo servicio debería ver a Ana con una victoria");
    }

    // ---------- archivo ----------

    // TDD
    private static void testFileStoreRoundTripWithAccents() throws IOException {
        // Arrange
        Path dir = Files.createTempDirectory("ajedrez-test");
        Path file = dir.resolve("data").resolve("usuarios.csv");
        FileAccountStore store = new FileAccountStore(file);
        List<Score> toSave = Arrays.asList(new Score(new User(1, "Álvaro"), 2, 1, 0), new Score(new User(2, "Ana María"), 0, 0, 3));

        // Act
        store.saveAll(toSave);
        List<Score> loaded = new FileAccountStore(file).loadAll();

        // Assert
        boolean same = loaded.size() == 2
            && loaded.get(0).getUser().getName().equals("Álvaro") && loaded.get(0).getWins() == 2
            && loaded.get(1).getUser().getId() == 2 && loaded.get(1).getLosses() == 3;
        deleteQuietly(dir);
        assertTrue("testFileStoreRoundTripWithAccents", same, "lo leído no coincide con lo guardado: " + loaded);
    }

    // TDD: la primera vez no hay archivo todavía.
    private static void testFileStoreMissingFileIsEmpty() throws IOException {
        // Arrange
        Path dir = Files.createTempDirectory("ajedrez-test");
        FileAccountStore store = new FileAccountStore(dir.resolve("no-existe.csv"));

        // Act
        List<Score> loaded = store.loadAll();

        // Assert
        deleteQuietly(dir);
        assertTrue("testFileStoreMissingFileIsEmpty", loaded.isEmpty(), "se esperaba una lista vacía");
    }

    // TDD: una línea rota no tira abajo a todos los demás.
    private static void testFileStoreSkipsMalformedLines() throws IOException {
        // Arrange
        Path dir = Files.createTempDirectory("ajedrez-test");
        Path file = dir.resolve("usuarios.csv");
        Files.write(file, Arrays.asList("1;Ana;1;0;0", "esto no es una línea válida", "x;Beto;1;0;0", "2;Carla;0;2;1"), StandardCharsets.UTF_8);

        // Act
        List<Score> loaded = new FileAccountStore(file).loadAll();

        // Assert
        deleteQuietly(dir);
        assertTrue("testFileStoreSkipsMalformedLines",
            loaded.size() == 2 && loaded.get(0).getUser().getName().equals("Ana") && loaded.get(1).getDraws() == 2,
            "se esperaban solo Ana y Carla, y llegó " + loaded);
    }

    private static void deleteQuietly(Path dir) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    // ---------- registro del resultado de la partida ----------

    // TDD: en un mate gana uno y pierde el otro, y se guarda.
    private static void testCheckmateCreditsWinnerAndLoser() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        ChessGame game = newGame(BoardSetup.standard(PieceFactory.standard()), new ScoreRecorder(service, ana, beto));

        // Act: mate del loco, ganan las negras (Beto).
        foolsCheckmate(game);

        // Assert
        Score a = scoreOf(service, ana);
        Score b = scoreOf(service, beto);
        assertTrue("testCheckmateCreditsWinnerAndLoser",
            a.getLosses() == 1 && a.getWins() == 0 && b.getWins() == 1 && b.getPoints() == 3,
            "Ana " + a.getWins() + "-" + a.getDraws() + "-" + a.getLosses() + ", Beto " + b.getWins() + "-" + b.getDraws() + "-" + b.getLosses());
    }

    // TDD
    private static void testStalemateCreditsDrawToBoth() {
        // Arrange: ahogado clásico, la dama blanca a g3 deja a las negras sin jugadas y sin jaque.
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        Board board = new Board();
        PieceFactory pieces = PieceFactory.standard();
        board.placePiece(pieces.create(PieceType.KING, Color.WHITE), at("f2"));
        board.placePiece(pieces.create(PieceType.QUEEN, Color.WHITE), at("g5"));
        board.placePiece(pieces.create(PieceType.KING, Color.BLACK), at("h1"));
        ChessGame game = newGame(board, new ScoreRecorder(service, ana, beto));

        // Act
        game.tryMove(at("g5"), at("g3"));

        // Assert
        assertTrue("testStalemateCreditsDrawToBoth",
            scoreOf(service, ana).getDraws() == 1 && scoreOf(service, beto).getDraws() == 1,
            "el ahogado debería sumar un empate a los dos");
    }

    // TDD
    private static void testDrawCreditsBoth() {
        // Arrange: rey contra rey son tablas por material insuficiente.
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        ChessGame game = newGame(kingsOnly(), new ScoreRecorder(service, ana, beto));

        // Act
        game.tryMove(at("e1"), at("e2"));

        // Assert
        assertTrue("testDrawCreditsBoth",
            scoreOf(service, ana).getDraws() == 1 && scoreOf(service, beto).getDraws() == 1,
            "las tablas deberían sumar un empate a los dos");
    }

    // TDD: contra la IA (o sin usuario negro) solo se acredita al humano.
    private static void testNoBlackUserCreditsOnlyWhite() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        ChessGame game = newGame(BoardSetup.standard(PieceFactory.standard()), new ScoreRecorder(service, ana, null));

        // Act
        foolsCheckmate(game);

        // Assert
        assertTrue("testNoBlackUserCreditsOnlyWhite",
            scoreOf(service, ana).getLosses() == 1 && service.ranking().size() == 1,
            "solo Ana debería figurar, con una derrota");
    }

    // TDD
    private static void testNoUsersRecordsNothing() {
        // Arrange
        InMemoryAccountStore store = new InMemoryAccountStore();
        AccountService service = new AccountService(store);
        ChessGame game = newGame(BoardSetup.standard(PieceFactory.standard()), new ScoreRecorder(service, null, null));

        // Act
        foolsCheckmate(game);

        // Assert
        assertTrue("testNoUsersRecordsNothing", store.saves == 0, "sin usuarios no se debería guardar nada");
    }

    // TDD: se puede deshacer el mate, y entonces el resultado se retira.
    private static void testUndoRevertsTheResult() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        ChessGame game = newGame(BoardSetup.standard(PieceFactory.standard()), new ScoreRecorder(service, ana, beto));
        foolsCheckmate(game);

        // Act
        game.undo();

        // Assert
        assertTrue("testUndoRevertsTheResult",
            scoreOf(service, ana).getGames() == 0 && scoreOf(service, beto).getGames() == 0,
            "al deshacer el mate la partida sigue y nadie tiene partidas jugadas");
    }

    // TDD: y rehacerlo lo vuelve a acreditar, una sola vez.
    private static void testRedoReappliesTheResult() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        ChessGame game = newGame(BoardSetup.standard(PieceFactory.standard()), new ScoreRecorder(service, ana, beto));
        foolsCheckmate(game);
        game.undo();

        // Act
        game.redo();

        // Assert
        assertTrue("testRedoReappliesTheResult",
            scoreOf(service, ana).getLosses() == 1 && scoreOf(service, beto).getWins() == 1
                && scoreOf(service, beto).getGames() == 1,
            "después de rehacer el mate el resultado figura una sola vez");
    }

    // TDD: rehacer una jugada anterior al mate no acredita nada.
    private static void testRedoOfAnEarlierMoveDoesNotReapply() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        ChessGame game = newGame(BoardSetup.standard(PieceFactory.standard()), new ScoreRecorder(service, ana, beto));
        foolsCheckmate(game);
        game.undo();   // se deshace el mate
        game.undo();   // y la jugada anterior

        // Act
        game.redo();   // se rehace solo la jugada anterior: la partida no terminó

        // Assert
        assertTrue("testRedoOfAnEarlierMoveDoesNotReapply",
            scoreOf(service, ana).getGames() == 0 && scoreOf(service, beto).getGames() == 0 && !game.isGameOver(),
            "la partida sigue en juego, así que no hay resultado que acreditar");
    }

    // TDD: si en la ventana se pasa a jugar contra la IA, el usuario negro deja de acreditarse.
    private static void testBlackUserCanChangeDuringTheGame() {
        // Arrange
        AccountService service = newService();
        User ana = service.register("Ana");
        User beto = service.register("Beto");
        ScoreRecorder recorder = new ScoreRecorder(service, ana, beto);
        ChessGame game = newGame(BoardSetup.standard(PieceFactory.standard()), recorder);

        // Act
        recorder.setBlackUser(null);
        foolsCheckmate(game);

        // Assert
        assertTrue("testBlackUserCanChangeDuringTheGame",
            scoreOf(service, ana).getLosses() == 1 && scoreOf(service, beto).getGames() == 0,
            "Beto no debería sumar nada si ya no es el rival");
    }
}
