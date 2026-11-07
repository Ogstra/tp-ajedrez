package adapters.console;

import core.board.Color;
import core.board.PieceType;
import ports.IPromotionChooser;

import java.util.Scanner;

// Elige la pieza de coronación preguntando por teclado. Usa el mismo Scanner que Main.
public class ConsolePromotionChooser implements IPromotionChooser {

    private final Scanner scanner;

    public ConsolePromotionChooser(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public PieceType choose(Color color) {
        while (true) {
            System.out.print("Coronación: elegí pieza (Q=Reina, R=Torre, B=Alfil, N=Caballo, Enter=Reina) > ");
            String choice = scanner.hasNextLine() ? scanner.nextLine().trim().toUpperCase() : "Q";
            switch (choice) {
                case "Q": case "": return PieceType.QUEEN;
                case "R": return PieceType.ROOK;
                case "B": return PieceType.BISHOP;
                case "N": return PieceType.KNIGHT;
                default: System.out.println("No entendí, probá con Q, R, B o N.");
            }
        }
    }
}
