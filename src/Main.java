import adaptadores.ConsolePromotionChooser;
import adaptadores.ConsoleRenderer;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.Color;
import nucleo.ISpecialMove;
import nucleo.MoveOutcome;
import nucleo.PieceFactory;
import nucleo.Position;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.PromotionMove;
import puertos.IGameObserver;
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
        ChessGame game = new ChessGame(board, Collections.<IGameObserver>singletonList(renderer), specialMoves);

        renderer.render(board);

        System.out.println("Ajedrez: TP Ingeniería de Software (UADE)");
        System.out.println("Movimiento: 'e2 e4' (u 'e2e4'). 'undo' deshace. 'salir' termina.");

        while (!game.isGameOver()) {
            System.out.print("\nTurno de " + (game.getTurn() == Color.WHITE ? "Blancas" : "Negras") + " > ");
            if (!scanner.hasNextLine()) break;
            String entrada = scanner.nextLine().trim();

            if (entrada.equalsIgnoreCase("salir")) {
                System.out.println("Partida abandonada.");
                break;
            }
            if (entrada.equalsIgnoreCase("undo")) {
                game.undo();
                renderer.render(board);
                continue;
            }

            Position[] movimiento = parsearMovimiento(entrada);
            if (movimiento == null) {
                System.out.println("No entendí ese movimiento. Formato: 'e2 e4'.");
                continue;
            }

            if (game.tryMove(movimiento[0], movimiento[1]) != MoveOutcome.INVALID) {
                renderer.render(board);
            }
        }
        scanner.close();
    }

    // Acepta "e2 e4", "e2-e4" o "e2e4". Parte el texto donde empieza la
    // segunda letra, así sirve también para filas de dos dígitos ("e10").
    private static Position[] parsearMovimiento(String entrada) {
        String limpio = entrada.replace(" ", "").replace("-", "");
        int corte = -1;
        for (int i = 1; i < limpio.length(); i++) {
            if (Character.isLetter(limpio.charAt(i))) {
                corte = i;
                break;
            }
        }
        if (corte < 0) return null;

        Position from = Position.fromAlgebraic(limpio.substring(0, corte));
        Position to = Position.fromAlgebraic(limpio.substring(corte));
        if (from == null || to == null) return null;

        return new Position[]{from, to};
    }
}
