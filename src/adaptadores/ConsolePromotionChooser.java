package adaptadores;

import nucleo.Color;
import nucleo.PieceType;
import puertos.IPromotionChooser;

import java.util.Scanner;

// Adaptador de salida (Clase 2): implementa el puerto IPromotionChooser
// preguntando por teclado. Comparte el Scanner con Main, que es quien lo crea.
public class ConsolePromotionChooser implements IPromotionChooser {

    private final Scanner scanner;

    public ConsolePromotionChooser(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public PieceType choose(Color color) {
        while (true) {
            System.out.print("Coronación: elegí pieza (Q=Reina, R=Torre, B=Alfil, N=Caballo, Enter=Reina) > ");
            String eleccion = scanner.hasNextLine() ? scanner.nextLine().trim().toUpperCase() : "Q";
            switch (eleccion) {
                case "Q": case "": return PieceType.QUEEN;
                case "R": return PieceType.ROOK;
                case "B": return PieceType.BISHOP;
                case "N": return PieceType.KNIGHT;
                default: System.out.println("No entendí, probá con Q, R, B o N.");
            }
        }
    }
}
