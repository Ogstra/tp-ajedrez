import adapters.console.ConsolePromotionChooser;
import ai.AutoPromotionChooser;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import adapters.console.ConsoleRenderer;
import core.board.Board;
import core.board.BoardSetup;
import core.board.Color;
import core.board.PieceFactory;
import core.board.Position;
import core.ChessGame;
import core.IChessGame;
import core.MoveOutcome;
import core.StandardRules;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;

import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

// Adaptador de entrada por consola y composition root: acá se arma todo.
// ChessGame recibe sus dependencias ya armadas por constructor.
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        OpponentLevel level = askLevel(scanner);

        PieceFactory pieces = PieceFactory.standard();
        Board board = BoardSetup.standard(pieces);
        ConsoleRenderer renderer = new ConsoleRenderer();

        // Contra la IA, ella corona siempre a reina; a la persona se le pregunta.
        IPromotionChooser human = new ConsolePromotionChooser(scanner);
        IPromotionChooser chooser = level == OpponentLevel.NONE ? human : new AutoPromotionChooser(Color.BLACK, human);
        IChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(renderer),
            Collections.<IGameStatusListener>singletonList(renderer),
            StandardRules.specialMoves(pieces, chooser),
            StandardRules.drawRules());

        // La IA juega con negras y la persona con blancas.
        OpponentController opponent = level == OpponentLevel.NONE
            ? null
            : new OpponentController(game, Opponents.create(level, new Random()), Color.BLACK);

        renderer.render(board);
        System.out.println("Ajedrez");
        System.out.println("Movimiento: 'e2 e4' (u 'e2e4'). 'undo' deshace, 'redo' rehace y 'salir' termina.");

        while (true) {
            System.out.print("\nTurno de " + (game.getTurn() == Color.WHITE ? "Blancas" : "Negras") + " > ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("salir")) {
                System.out.println("Partida abandonada.");
                break;
            }
            if (input.equalsIgnoreCase("undo")) {
                // Contra la IA se deshacen dos: su respuesta y la jugada de la persona.
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
