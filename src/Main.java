import adaptadores.ConsoleAccounts;
import adaptadores.ConsolePromotionChooser;
import adaptadores.FileAccountStore;
import adaptadores.ConsoleRenderer;
import ai.AutoPromotionChooser;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.Color;
import nucleo.IChessGame;
import nucleo.IDrawRule;
import nucleo.ISpecialMove;
import nucleo.MoveOutcome;
import nucleo.PieceFactory;
import nucleo.Position;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.EnPassantMove;
import nucleo.especiales.PromotionMove;
import nucleo.tablas.FiftyMoveRule;
import nucleo.tablas.InsufficientMaterialRule;
import nucleo.tablas.ThreefoldRepetitionRule;
import puertos.IGameStatusListener;
import puertos.IMoveListener;
import puertos.IPromotionChooser;
import usuarios.AccountService;
import usuarios.ScoreRecorder;
import usuarios.User;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

// Adaptador de entrada (Clase 2) + composition root: acá, y solo acá, se
// arma todo. El núcleo (ChessGame) recibe sus dependencias por constructor
// (Clase 3: inyección de dependencias), ya armadas. Sumar una pieza o una
// regla nueva es agregar una línea en este armado.
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        OpponentLevel level = askLevel(scanner);

        // Cuentas: los datos se guardan en data/usuarios.csv (se cambia con -Dajedrez.data=carpeta).
        Path dataDir = Path.of(System.getProperty("ajedrez.data", "data"));
        AccountService accounts = new AccountService(new FileAccountStore(dataDir.resolve("usuarios.csv")));
        User white = ConsoleAccounts.askUser(scanner, accounts, "blancas");
        User black = level == OpponentLevel.NONE ? ConsoleAccounts.askUser(scanner, accounts, "negras") : null;

        PieceFactory pieces = PieceFactory.standard();
        Board board = BoardSetup.standard(pieces);
        ConsoleRenderer renderer = new ConsoleRenderer();

        IPromotionChooser human = new ConsolePromotionChooser(scanner);
        IPromotionChooser chooser = level == OpponentLevel.NONE
            ? human
            : new AutoPromotionChooser(Color.BLACK, human);

        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new EnPassantMove(),
            new PromotionMove(pieces, chooser));
        List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
            new InsufficientMaterialRule(),
            new FiftyMoveRule(),
            new ThreefoldRepetitionRule());

        ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
        IChessGame game = new ChessGame(board,
            Arrays.<IMoveListener>asList(renderer, recorder),
            Arrays.<IGameStatusListener>asList(renderer, recorder),
            specialMoves,
            drawRules);
        recorder.attachGame(game);

        // La IA juega con negras; el humano con blancas.
        OpponentController opponent = level == OpponentLevel.NONE
            ? null
            : new OpponentController(game, Opponents.create(level, new Random()), Color.BLACK);

        renderer.render(board);
        System.out.println("Ajedrez: TP Ingeniería de Software (UADE)");
        System.out.println("Blancas: " + (white == null ? "sin usuario" : white)
            + " | Negras: " + (level != OpponentLevel.NONE ? "la IA" : black == null ? "sin usuario" : black));
        System.out.println("Movimiento: 'e2 e4' (u 'e2e4'). 'undo' deshace, 'redo' rehace, 'ranking' muestra la tabla, 'salir' termina.");

        while (true) {
            System.out.print("\nTurno de " + (game.getTurn() == Color.WHITE ? "Blancas" : "Negras") + " > ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("salir")) {
                System.out.println("Partida abandonada.");
                break;
            }
            if (input.equalsIgnoreCase("ranking")) {
                ConsoleAccounts.printRanking(accounts);
                continue;
            }
            if (input.equalsIgnoreCase("undo")) {
                // Contra la IA se deshacen las dos: la respuesta de ella y la jugada propia.
                if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
                renderer.render(board);
                continue;
            }
            if (input.equalsIgnoreCase("redo")) {
                if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
                renderer.render(board);
                continue;
            }

            Position[] move = parseMove(input);
            if (move == null) {
                System.out.println("No entendí ese movimiento. Formato: 'e2 e4'.");
                continue;
            }

            if (game.tryMove(move[0], move[1]) != MoveOutcome.INVALID) {
                renderer.render(board);
                if (opponent != null && opponent.isItsTurn()) {
                    System.out.println("El oponente piensa...");
                    opponent.playIfItsTurn();
                    renderer.render(board);
                }
                // Al terminar la partida, el ranking ya incluye el resultado.
                if (game.isGameOver() && (white != null || black != null)) {
                    ConsoleAccounts.printRanking(accounts);
                }
            }
        }
        scanner.close();
    }

    private static OpponentLevel askLevel(Scanner scanner) {
        System.out.print("Oponente: 0 = dos jugadores, 1 = fácil, 2 = medio, 3 = difícil > ");
        String answer = scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
        switch (answer) {
            case "1": return OpponentLevel.EASY;
            case "2": return OpponentLevel.MEDIUM;
            case "3": return OpponentLevel.HARD;
            default:  return OpponentLevel.NONE;
        }
    }

    // Acepta "e2 e4", "e2-e4" o "e2e4". Parte el texto donde empieza la
    // segunda letra, así sirve también para filas de dos dígitos ("e10").
    private static Position[] parseMove(String input) {
        String clean = input.replace(" ", "").replace("-", "");
        int cut = -1;
        for (int i = 1; i < clean.length(); i++) {
            if (Character.isLetter(clean.charAt(i))) {
                cut = i;
                break;
            }
        }
        if (cut < 0) return null;

        Position from = Position.fromAlgebraic(clean.substring(0, cut));
        Position to = Position.fromAlgebraic(clean.substring(cut));
        if (from == null || to == null) return null;

        return new Position[]{from, to};
    }
}
