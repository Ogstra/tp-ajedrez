package adaptadores;

import usuarios.AccountService;
import usuarios.Score;
import usuarios.User;

import java.util.List;
import java.util.Scanner;

// Adaptador de entrada y salida (Clase 2) de las cuentas, por consola: pregunta quién
// juega (iniciar sesión, registrarse o seguir sin usuario) y muestra el ranking.
// La lógica de las cuentas vive en AccountService; acá solo se lee y se imprime.
public final class ConsoleAccounts {

    private ConsoleAccounts() { }

    // Devuelve el usuario que entró, o null si se juega sin usuario (o se terminó la entrada).
    public static User askUser(Scanner scanner, AccountService accounts, String role) {
        while (true) {
            System.out.print("Jugador " + role + ": 1 = iniciar sesión, 2 = registrarse, 3 = sin usuario > ");
            if (!scanner.hasNextLine()) return null;
            String answer = scanner.nextLine().trim();
            if (answer.equals("3") || answer.isEmpty()) return null;
            if (answer.equals("1")) {
                User user = login(scanner, accounts);
                if (user != null) return user;
            } else if (answer.equals("2")) {
                User user = register(scanner, accounts);
                if (user != null) return user;
            } else {
                System.out.println("Elegí 1, 2 o 3.");
            }
        }
    }

    private static User login(Scanner scanner, AccountService accounts) {
        System.out.print("Usuario > ");
        if (!scanner.hasNextLine()) return null;
        User user = accounts.login(scanner.nextLine());
        if (user == null) {
            System.out.println("No existe ese usuario. Si es nuevo, elegí registrarse.");
            return null;
        }
        System.out.println("Hola, " + user.getName() + ".");
        return user;
    }

    private static User register(Scanner scanner, AccountService accounts) {
        System.out.print("Nuevo usuario > ");
        if (!scanner.hasNextLine()) return null;
        try {
            User user = accounts.register(scanner.nextLine());
            System.out.println("Listo, " + user.getName() + ". Ya estás registrado.");
            return user;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static void printRanking(AccountService accounts) {
        System.out.println(formatRanking(accounts.ranking()));
    }

    // Texto del ranking, una línea por usuario. Lo usan la consola y los tests.
    public static String formatRanking(List<Score> ranking) {
        if (ranking.isEmpty()) return "Ranking: todavía no hay usuarios.";
        StringBuilder text = new StringBuilder("Ranking\n");
        text.append(String.format("%3s  %-20s %3s  %3s  %3s  %3s  %4s%n", "#", "Usuario", "PJ", "G", "E", "P", "Pts"));
        int position = 1;
        for (Score score : ranking) {
            text.append(String.format("%3d  %-20s %3d  %3d  %3d  %3d  %4d%n",
                position++, score.getUser().getName(),
                score.getGames(), score.getWins(), score.getDraws(), score.getLosses(), score.getPoints()));
        }
        return text.toString().trim();
    }
}
