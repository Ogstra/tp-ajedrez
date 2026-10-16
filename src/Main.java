import nucleo.IDrawRule;
import nucleo.tablas.FiftyMoveRule;
import nucleo.tablas.InsufficientMaterialRule;
import nucleo.tablas.ThreefoldRepetitionRule;
import adaptadores.ConsolePromotionChooser;
import adaptadores.ConsoleRenderer;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.IChessGame;
import nucleo.Color;
import nucleo.ISpecialMove;
import nucleo.MoveOutcome;
import nucleo.PieceFactory;
import nucleo.Position;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.PromotionMove;
import puertos.IGameStatusListener;
import puertos.IMoveListener;
import puertos.IPromotionChooser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

// Adaptador de entrada (Clase 2) + composition root: acá, y solo acá, se
// arma todo. El núcleo (ChessGame) recibe sus dependencias por constructor
// (Clase 3: inyección de dependencias), ya armadas. Sumar una pieza o una
// regla especial nueva es agregar una línea en este armado.
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        PieceFactory pieces = PieceFactory.standard();
        Board board = BoardSetup.standard(pieces);
        ConsoleRenderer renderer = new ConsoleRenderer();
        IPromotionChooser chooser = new ConsolePromotionChooser(scanner);
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(pieces, chooser));
        List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
            new InsufficientMaterialRule(),
            new FiftyMoveRule(),
            new ThreefoldRepetitionRule());

        IChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(renderer),
            Collections.<IGameStatusListener>singletonList(renderer),
            specialMoves, drawRules);

        renderer.render(board);

        System.out.println("Ajedrez: TP Ingeniería de Software (UADE)");
        System.out.println("Movimiento: 'e2 e4' (u 'e2e4'). 'undo' deshace, 'redo' rehace, 'salir' termina.");

        while (true) {
            System.out.print("\nTurno de " + (game.getTurn() == Color.WHITE ? "Blancas" : "Negras") + " > ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("salir")) {
                System.out.println("Partida abandonada.");
                break;
            }
            if (input.equalsIgnoreCase("undo")) {
                game.undo();
                renderer.render(board);
                continue;
            }
            if (input.equalsIgnoreCase("redo")) {
                game.redo();
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
            }
        }
        scanner.close();
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
