import adapters.console.ConsoleAccounts;
import adapters.console.ConsolePromotionChooser;
import adapters.console.ConsoleRenderer;
import adapters.storage.FileAccountStore;
import core.board.Board;
import core.board.BoardSetup;
import core.board.Color;
import core.board.PieceFactory;
import core.board.Position;
import ai.AutoPromotionChooser;
import ai.IOpponent;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import core.ChessGame;
import core.IChessGame;
import core.Move;
import core.MoveOutcome;
import core.StandardRules;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;
import accounts.AccountService;
import accounts.ScoreRecorder;
import accounts.User;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

// Adaptador de entrada por consola y composition root: acá se arma todo.
// ChessGame recibe sus dependencias ya armadas por constructor.
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

        ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
        IChessGame game = new ChessGame(board,
            Arrays.<IMoveListener>asList(renderer, recorder),
            Arrays.<IGameStatusListener>asList(renderer, recorder),
            StandardRules.specialMoves(pieces, chooser),
            StandardRules.drawRules());

        // La IA juega con negras; el humano con blancas.
        Random random = new Random();
        OpponentController opponent = level == OpponentLevel.NONE
            ? null
            : new OpponentController(game, Opponents.create(level, random), Color.BLACK);
        // La pista la da el nivel difícil, para el bando que tenga el turno.
        IOpponent hint = Opponents.create(OpponentLevel.HARD, random);

        renderer.render(board);
        System.out.println("Ajedrez");
        System.out.println("Blancas: " + (white == null ? "sin usuario" : white)
            + " | Negras: " + (level != OpponentLevel.NONE ? "la IA" : black == null ? "sin usuario" : black));
        System.out.println("Movimiento: 'e2 e4' (u 'e2e4'). 'undo' deshace, 'redo' rehace, 'ranking' muestra la tabla, 'salir' termina.");
        System.out.println("También: 'jugadas', 'pista', 'tablas', 'abandonar' y, al terminar, 'revisar'.");

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
            if (input.equalsIgnoreCase("jugadas")) {
                System.out.println(numbered(game.moveList()));
                continue;
            }
            if (input.equalsIgnoreCase("pista")) {
                if (game.isGameOver()) {
                    System.out.println("La partida ya terminó.");
                } else {
                    Move move = hint.chooseMove(game);
                    System.out.println("Pista: " + move.getFrom() + " " + move.getTo());
                }
                continue;
            }
            if (input.equalsIgnoreCase("tablas")) {
                if (game.isGameOver()) {
                    System.out.println("La partida ya terminó.");
                } else if (opponent != null && !opponent.acceptsDraw()) {
                    System.out.println("La IA rechaza las tablas.");
                } else if (opponent != null || accepts(scanner, game.getTurn().opposite())) {
                    game.agreeDraw();
                    printRankingIfOver(game, accounts, white, black);
                } else {
                    System.out.println("Tablas rechazadas, sigue la partida.");
                }
                continue;
            }
            if (input.equalsIgnoreCase("abandonar")) {
                game.resign();
                printRankingIfOver(game, accounts, white, black);
                continue;
            }
            if (input.equalsIgnoreCase("revisar")) {
                if (!game.isGameOver()) {
                    System.out.println("La partida se revisa cuando termina.");
                } else {
                    review(scanner, game, renderer);
                }
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
                printRankingIfOver(game, accounts, white, black);
            }
        }
        scanner.close();
    }

    // Al terminar la partida, el ranking ya incluye el resultado.
    private static void printRankingIfOver(IChessGame game, AccountService accounts, User white, User black) {
        if (game.isGameOver() && (white != null || black != null)) {
            ConsoleAccounts.printRanking(accounts);
        }
    }

    // Entre dos personas, el rival dice si acepta las tablas.
    private static boolean accepts(Scanner scanner, Color rival) {
        System.out.print("¿Las " + (rival == Color.WHITE ? "blancas" : "negras") + " aceptan las tablas? (s/n) > ");
        return scanner.hasNextLine() && scanner.nextLine().trim().equalsIgnoreCase("s");
    }

    // Las jugadas numeradas de a dos: "1. e4 e5 2. Cf3".
    private static String numbered(List<String> moves) {
        if (moves.isEmpty()) return "Todavía no hay jugadas.";
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) text.append(i / 2 + 1).append(". ");
            text.append(moves.get(i)).append(' ');
        }
        return text.toString().trim();
    }

    // Recorre la partida terminada jugada por jugada, sin tocarla: muestra copias del tablero.
    private static void review(Scanner scanner, IChessGame game, ConsoleRenderer renderer) {
        List<Board> positions = game.positions();
        List<String> moves = game.moveList();
        int index = 0;
        while (true) {
            renderer.render(positions.get(index));
            System.out.println(index == 0 ? "Posición inicial."
                : "Después de la jugada " + ((index + 1) / 2) + ": " + moves.get(index - 1));
            System.out.print("Enter = siguiente, 'a' = anterior, 'fin' = volver > ");
            if (!scanner.hasNextLine()) return;
            String answer = scanner.nextLine().trim();
            if (answer.equalsIgnoreCase("fin")) return;
            if (answer.equalsIgnoreCase("a")) index = Math.max(0, index - 1);
            else index = Math.min(positions.size() - 1, index + 1);
        }
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
