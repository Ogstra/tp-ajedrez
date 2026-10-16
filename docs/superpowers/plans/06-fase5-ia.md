# Fase 5: oponente con IA

> **Para el agente:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Pasos con casillas `- [ ]`. TDD: test primero, verlo fallar, implementar.

**Goal:** poder jugar contra la computadora en consola y en la ventana, con tres niveles: fácil (al azar), medio (prefiere capturar la pieza de más valor) y difícil (minimax de profundidad 2).

**Architecture:** todo vive en un paquete nuevo `ai/`, fuera de `nucleo/`. La IA depende solo de `IChessGame`. Para pensar usa una partida de búsqueda propia (copia del tablero, sin listeners), así nunca toca la pantalla ni abre diálogos. `OpponentController` decide cuándo juega. El oponente **no se defiende** en la exposición: tiene que ser simple de leer, pero no hace falta que sea sofisticado.

**Tech Stack:** Java (`java.util.Random` y listas comunes; el paquete `ai/` puede usar lo que quiera de la biblioteca estándar, `nucleo/` sigue sin hacerlo).

**Antes de empezar:** fases 0 a 4 terminadas, `./scripts/test.sh` con `54 pasados, 0 fallados.`

---

### Task 5.1: piezas de la IA y niveles

**Files:**
- Create: `src/ai/IOpponent.java`, `src/ai/PieceValues.java`, `src/ai/FixedPromotionChooser.java`, `src/ai/AutoPromotionChooser.java`, `src/ai/SearchGames.java`
- Create: `src/ai/RandomOpponent.java`, `src/ai/GreedyOpponent.java`, `src/ai/MinimaxOpponent.java`
- Create: `src/ai/OpponentLevel.java`, `src/ai/Opponents.java`, `src/ai/OpponentController.java`
- Test: `src/ChessCoreTests.java`

- [ ] **Step 1: Escribir los tests que fallan**

Agregar los imports `ai.AutoPromotionChooser`, `ai.GreedyOpponent`, `ai.MinimaxOpponent`, `ai.OpponentController`, `ai.RandomOpponent`, `java.util.Random`. Agregar a `ChessCoreTests` y registrar en `main`:

```java
    // TDD: el nivel fácil siempre elige una jugada legal.
    private static void testRandomOpponentChoosesALegalMove() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        Move move = new RandomOpponent(new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testRandomOpponentChoosesALegalMove", game.legalMoves().contains(move),
            "la jugada elegida tiene que estar entre las legales");
    }

    // TDD: el nivel medio prefiere capturar la pieza de más valor.
    private static void testGreedyOpponentTakesTheQueen() {
        // Arrange: la torre blanca puede capturar la reina de a6 (valor 9) o el peón de c1 (valor 1).
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.QUEEN, Color.BLACK), at("a6"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("c1"));
        IChessGame game = newGame(board);

        // Act
        Move move = new GreedyOpponent(new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testGreedyOpponentTakesTheQueen", move.equals(new Move(at("a1"), at("a6"))),
            "entre la reina y el peón, elige la reina; eligió " + move);
    }

    // TDD: el nivel difícil encuentra un mate en una jugada.
    private static void testMinimaxOpponentFindsMateInOne() {
        // Arrange: después de 1.f3 e5 2.g4 las negras tienen Dh4#.
        IChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));

        // Act
        Move move = new MinimaxOpponent(2, new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testMinimaxOpponentFindsMateInOne", move.equals(new Move(at("d8"), at("h4"))),
            "la única jugada que da mate es Dh4; eligió " + move);
    }

    // TDD: pensar no toca la partida real ni avisa a los listeners.
    private static void testSearchDoesNotTouchTheRealGame() {
        // Arrange
        Board board = BoardSetup.standard(PIECES);
        RecordingObserver observer = new RecordingObserver();
        IChessGame game = newGame(board, observer);
        game.tryMove(at("e2"), at("e4"));
        Board before = board.cloneBoard();

        // Act
        new MinimaxOpponent(2, new Random(7)).chooseMove(game);

        // Assert
        assertTrue("testSearchDoesNotTouchTheRealGame",
            before.equals(board) && observer.events.equals(Arrays.asList("move")),
            "la búsqueda trabaja sobre una copia: el tablero y los avisos quedan como estaban");
    }

    // TDD: el controlador solo juega cuando es el turno de la IA.
    private static void testOpponentControllerPlaysOnlyOnItsTurn() {
        // Arrange
        Board board = BoardSetup.standard(PIECES);
        IChessGame game = newGame(board);
        OpponentController controller = new OpponentController(game, new RandomOpponent(new Random(7)), Color.BLACK);

        // Act
        boolean playedOnWhitesTurn = controller.playIfItsTurn();
        game.tryMove(at("e2"), at("e4"));
        boolean playedOnItsTurn = controller.playIfItsTurn();

        // Assert
        assertTrue("testOpponentControllerPlaysOnlyOnItsTurn",
            !playedOnWhitesTurn && playedOnItsTurn && game.getTurn() == Color.WHITE,
            "no juega en el turno de las blancas; juega en el suyo y devuelve el turno");
    }

    // TDD: la IA corona sola, sin preguntar.
    private static void testAutoPromotionChooserDoesNotAskForTheOpponent() {
        // Arrange
        CountingChooser human = new CountingChooser();
        AutoPromotionChooser chooser = new AutoPromotionChooser(Color.BLACK, human);

        // Act
        PieceType forBlack = chooser.choose(Color.BLACK);
        chooser.choose(Color.WHITE);

        // Assert
        assertTrue("testAutoPromotionChooserDoesNotAskForTheOpponent",
            forBlack.equals(PieceType.QUEEN) && human.calls == 1,
            "para el bando de la IA corona a reina sin preguntar; para el humano pregunta");
    }

    // TDD: el nivel difícil contesta en un tiempo razonable desde la posición inicial.
    private static void testHardLevelRespondsInReasonableTime() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("e2"), at("e4"));
        long start = System.currentTimeMillis();

        // Act
        new MinimaxOpponent(2, new Random(7)).chooseMove(game);
        long elapsed = System.currentTimeMillis() - start;

        // Assert
        System.out.println("       (nivel difícil, profundidad 2: " + elapsed + " ms)");
        assertTrue("testHardLevelRespondsInReasonableTime", elapsed < 15000,
            "tardó " + elapsed + " ms; si pasa los 15 segundos hay que optimizar o bajar la profundidad");
    }
```

```java
        testRandomOpponentChoosesALegalMove();
        testGreedyOpponentTakesTheQueen();
        testMinimaxOpponentFindsMateInOne();
        testSearchDoesNotTouchTheRealGame();
        testOpponentControllerPlaysOnlyOnItsTurn();
        testAutoPromotionChooserDoesNotAskForTheOpponent();
        testHardLevelRespondsInReasonableTime();
```

- [ ] **Step 2: Correr y ver que no compila**

Run: `./scripts/build.sh`
Expected: `package ai does not exist`.

- [ ] **Step 3: Crear las interfaces y utilidades de `ai/`**

`src/ai/IOpponent.java`:

```java
package ai;

import nucleo.IChessGame;
import nucleo.Move;

// Strategy (Clase 3) de dificultad: cada nivel decide la jugada de otra forma.
public interface IOpponent {
    // Elige una jugada legal para el bando que tiene el turno. Se llama solo
    // si la partida no terminó, así que siempre hay alguna.
    Move chooseMove(IChessGame game);
}
```

`src/ai/PieceValues.java`:

```java
package ai;

import nucleo.PieceType;

// Valor de material de cada pieza, el clásico del ajedrez. Una pieza que la IA
// no conoce (una inventada) vale 0.
public final class PieceValues {

    private PieceValues() { }

    public static int of(PieceType type) {
        if (type.equals(PieceType.PAWN)) return 1;
        if (type.equals(PieceType.KNIGHT)) return 3;
        if (type.equals(PieceType.BISHOP)) return 3;
        if (type.equals(PieceType.ROOK)) return 5;
        if (type.equals(PieceType.QUEEN)) return 9;
        return 0; // el rey no se cuenta: nunca se captura
    }
}
```

`src/ai/FixedPromotionChooser.java`:

```java
package ai;

import nucleo.Color;
import nucleo.PieceType;
import puertos.IPromotionChooser;

// Puerto de salida (Clase 2) resuelto sin preguntar a nadie: siempre la misma pieza.
// Lo usa la partida de búsqueda de la IA.
public class FixedPromotionChooser implements IPromotionChooser {

    private final PieceType choice;

    public FixedPromotionChooser(PieceType choice) {
        this.choice = choice;
    }

    @Override
    public PieceType choose(Color color) {
        return choice;
    }
}
```

`src/ai/AutoPromotionChooser.java`:

```java
package ai;

import nucleo.Color;
import nucleo.PieceType;
import puertos.IPromotionChooser;

// Decorador del chooser humano: cuando corona el bando de la IA elige reina
// sin preguntar; cuando corona el humano, le pregunta como siempre.
public class AutoPromotionChooser implements IPromotionChooser {

    private final Color opponentColor;
    private final IPromotionChooser human;

    public AutoPromotionChooser(Color opponentColor, IPromotionChooser human) {
        this.opponentColor = opponentColor;
        this.human = human;
    }

    @Override
    public PieceType choose(Color color) {
        if (color == opponentColor) return PieceType.QUEEN;
        return human.choose(color);
    }
}
```

`src/ai/SearchGames.java`:

```java
package ai;

import nucleo.ChessGame;
import nucleo.IChessGame;
import nucleo.IDrawRule;
import nucleo.ISpecialMove;
import nucleo.PieceFactory;
import nucleo.PieceType;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.EnPassantMove;
import nucleo.especiales.PromotionMove;
import puertos.IGameStatusListener;
import puertos.IMoveListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Arma la partida sobre la que la IA "piensa": una copia del tablero, sin
// listeners (no avisa a la pantalla) y con una coronación que no pregunta.
// Limitación conocida: la copia no tiene el historial, así que no sabe qué
// piezas ya se movieron (derechos de enroque) ni la última jugada (captura
// al paso). Eso solo afecta cuánto "ve" la IA; las reglas de verdad las
// aplica siempre la partida real cuando la IA juega su jugada.
public final class SearchGames {

    private SearchGames() { }

    public static IChessGame from(IChessGame game) {
        PieceFactory pieces = PieceFactory.standard();
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new EnPassantMove(),
            new PromotionMove(pieces, new FixedPromotionChooser(PieceType.QUEEN)));
        return new ChessGame(
            game.getBoard().cloneBoard(),
            new ArrayList<IMoveListener>(),
            new ArrayList<IGameStatusListener>(),
            specialMoves,
            new ArrayList<IDrawRule>(),
            game.getTurn());
    }
}
```

- [ ] **Step 4: Crear los tres oponentes**

`src/ai/RandomOpponent.java`:

```java
package ai;

import nucleo.IChessGame;
import nucleo.Move;

import java.util.List;
import java.util.Random;

// Nivel fácil: una jugada legal cualquiera.
public class RandomOpponent implements IOpponent {

    private final Random random;

    public RandomOpponent(Random random) {
        this.random = random;
    }

    @Override
    public Move chooseMove(IChessGame game) {
        List<Move> moves = game.legalMoves();
        return moves.get(random.nextInt(moves.size()));
    }
}
```

`src/ai/GreedyOpponent.java`:

```java
package ai;

import nucleo.IChessGame;
import nucleo.Move;
import nucleo.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Nivel medio: la jugada que captura la pieza de más valor; si ninguna
// captura, o hay empate, una de las mejores al azar.
public class GreedyOpponent implements IOpponent {

    private final Random random;

    public GreedyOpponent(Random random) {
        this.random = random;
    }

    @Override
    public Move chooseMove(IChessGame game) {
        int bestValue = -1;
        List<Move> best = new ArrayList<>();
        for (Move move : game.legalMoves()) {
            Piece target = game.getBoard().pieceAt(move.getTo());
            int value = target == null ? 0 : PieceValues.of(target.getType());
            if (value > bestValue) {
                bestValue = value;
                best.clear();
            }
            if (value == bestValue) {
                best.add(move);
            }
        }
        return best.get(random.nextInt(best.size()));
    }
}
```

`src/ai/MinimaxOpponent.java`:

```java
package ai;

import nucleo.Board;
import nucleo.Color;
import nucleo.IChessGame;
import nucleo.Move;
import nucleo.MoveOutcome;
import nucleo.Piece;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Nivel difícil: minimax con la diferencia de material como evaluación.
// "depth" cuenta las jugadas que mira, incluida la suya: con 2 mira su jugada
// y la respuesta del rival. Juega en una partida de búsqueda propia
// (SearchGames) y la deshace con undo() después de cada rama.
public class MinimaxOpponent implements IOpponent {

    private static final int MATE = 100000;

    private final int depth;
    private final Random random;

    public MinimaxOpponent(int depth, Random random) {
        this.depth = depth;
        this.random = random;
    }

    @Override
    public Move chooseMove(IChessGame game) {
        IChessGame search = SearchGames.from(game);
        Color me = game.getTurn();

        int bestValue = Integer.MIN_VALUE;
        List<Move> best = new ArrayList<>();
        for (Move move : search.legalMoves()) {
            int value = scoreMove(search, move, depth, me);
            if (value > bestValue) {
                bestValue = value;
                best.clear();
            }
            if (value == bestValue) {
                best.add(move);
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    // Juega la jugada, mira cuánto vale la posición resultante y la deshace.
    private int scoreMove(IChessGame game, Move move, int remainingDepth, Color me) {
        Color mover = game.getTurn();
        MoveOutcome outcome = game.tryMove(move.getFrom(), move.getTo());

        int value;
        if (outcome == MoveOutcome.CHECKMATE) {
            // Se prefiere el mate más rápido: queda más profundidad sin usar.
            value = mover == me ? MATE + remainingDepth : -(MATE + remainingDepth);
        } else if (outcome == MoveOutcome.STALEMATE || outcome == MoveOutcome.DRAW) {
            value = 0;
        } else if (remainingDepth <= 1) {
            value = material(game.getBoard(), me);
        } else {
            value = bestValue(game, remainingDepth - 1, me);
        }

        game.undo();
        return value;
    }

    // El que tiene el turno elige lo mejor para él: yo maximizo, el rival minimiza.
    private int bestValue(IChessGame game, int remainingDepth, Color me) {
        boolean maximizing = game.getTurn() == me;
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : game.legalMoves()) {
            int value = scoreMove(game, move, remainingDepth, me);
            best = maximizing ? Math.max(best, value) : Math.min(best, value);
        }
        return best;
    }

    // Material propio menos material rival.
    private int material(Board board, Color me) {
        int total = 0;
        for (Color color : new Color[]{Color.WHITE, Color.BLACK}) {
            for (Position position : board.positionsOfColor(color)) {
                Piece piece = board.pieceAt(position);
                int value = PieceValues.of(piece.getType());
                total += color == me ? value : -value;
            }
        }
        return total;
    }
}
```

- [ ] **Step 5: Crear los niveles y el controlador**

`src/ai/OpponentLevel.java`:

```java
package ai;

// El orden importa: coincide con las opciones del desplegable de la ventana.
public enum OpponentLevel {
    NONE, EASY, MEDIUM, HARD
}
```

`src/ai/Opponents.java`:

```java
package ai;

import java.util.Random;

// Factory (Clase 3) de oponentes por nivel.
public final class Opponents {

    private Opponents() { }

    // Devuelve null para NONE (dos jugadores humanos).
    public static IOpponent create(OpponentLevel level, Random random) {
        switch (level) {
            case EASY:   return new RandomOpponent(random);
            case MEDIUM: return new GreedyOpponent(random);
            case HARD:   return new MinimaxOpponent(2, random);
            default:     return null;
        }
    }
}
```

`src/ai/OpponentController.java`:

```java
package ai;

import nucleo.Color;
import nucleo.IChessGame;
import nucleo.Move;

// Decide cuándo juega la IA: cuando la partida sigue y es el turno de su color.
// chooseMove y play están separados para que la ventana pueda pensar en otro
// hilo y aplicar la jugada después, en el hilo de la interfaz.
public class OpponentController {

    private final IChessGame game;
    private final IOpponent opponent;
    private final Color color;

    public OpponentController(IChessGame game, IOpponent opponent, Color color) {
        this.game = game;
        this.opponent = opponent;
        this.color = color;
    }

    public Color getColor() { return color; }

    public boolean isItsTurn() {
        return !game.isGameOver() && game.getTurn() == color;
    }

    public Move chooseMove() {
        return opponent.chooseMove(game);
    }

    public void play(Move move) {
        game.tryMove(move.getFrom(), move.getTo());
    }

    // Piensa y juega, todo junto. Devuelve false si no era su turno.
    public boolean playIfItsTurn() {
        if (!isItsTurn()) return false;
        play(chooseMove());
        return true;
    }
}
```

- [ ] **Step 6: Correr los tests**

Run: `./scripts/test.sh`
Expected: `61 pasados, 0 fallados.` y una línea `(nivel difícil, profundidad 2: NNN ms)`.

Si `testHardLevelRespondsInReasonableTime` falla por tiempo, no subas el límite: medí con un perfilador simple (`System.nanoTime` alrededor de `legalMoves` y de `tryMove`), y optimizá en `ChessGame` solo si el cambio es chico y claro; si no, dejá la profundidad en 2 y avisale a Franco.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "oponente con tres niveles de dificultad"
```

---

### Task 5.2: jugar contra la IA en la consola

**Files:**
- Modify: `src/Main.java` (reemplazo completo)

- [ ] **Step 1: Reemplazar `src/Main.java` por esta versión completa**

```java
import adaptadores.ConsolePromotionChooser;
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

        IChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(renderer),
            Collections.<IGameStatusListener>singletonList(renderer),
            specialMoves,
            drawRules);

        // La IA juega con negras; el humano con blancas.
        OpponentController opponent = level == OpponentLevel.NONE
            ? null
            : new OpponentController(game, Opponents.create(level, new Random()), Color.BLACK);

        renderer.render(board);
        System.out.println("Ajedrez: TP Ingeniería de Software (UADE)");
        System.out.println("Movimiento: 'e2 e4' (u 'e2e4'). 'undo' deshace, 'redo' rehace, 'salir' termina.");

        while (!game.isGameOver()) {
            System.out.print("\nTurno de " + (game.getTurn() == Color.WHITE ? "Blancas" : "Negras") + " > ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("salir")) {
                System.out.println("Partida abandonada.");
                break;
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
```

- [ ] **Step 2: Compilar**

Run: `./scripts/build.sh`
Expected: `compilado en out/`.

- [ ] **Step 3: Jugar contra el nivel medio**

Run: `printf "2\ne2 e4\nsalir\n" | java -cp out Main | tail -14`
Expected: el tablero con `e2 e4` jugado y, después de `El oponente piensa...`, una jugada negra (`Negras: ...`) en el tablero siguiente.

- [ ] **Step 4: Dos jugadores sigue funcionando**

Run: `printf "0\ne2 e4\ne7 e5\nundo\nsalir\n" | java -cp out Main | tail -12`
Expected: sin oponente; el `undo` quita `e7 e5`.

- [ ] **Step 5: Commit**

```bash
git add src/Main.java
git commit -m "elegir el oponente en la consola"
```

---

### Task 5.3: jugar contra la IA en la ventana

**Files:**
- Modify: `src/adaptadores/SwingChessView.java` (reemplazo completo)
- Modify: `src/MainSwing.java` (reemplazo completo)

- [ ] **Step 1: Reemplazar `src/adaptadores/SwingChessView.java` por esta versión completa**

```java
package adaptadores;

import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import nucleo.Board;
import nucleo.Color;
import nucleo.IChessGame;
import nucleo.Move;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;
import puertos.IGameStatusListener;
import puertos.IMoveListener;
import puertos.IPromotionChooser;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Adaptador de salida Y de entrada (Clase 2): implementa los dos listeners
// igual que ConsoleRenderer (mismos puertos de salida, otra tecnología),
// implementa IPromotionChooser con un diálogo, y además traduce clicks del
// mouse en llamadas a IChessGame.tryMove(): el mismo rol que cumple Main.java
// con el teclado, pero con Swing. El núcleo no cambió para que esto funcione.
public class SwingChessView extends JFrame implements IMoveListener, IGameStatusListener, IPromotionChooser {

    private static final java.awt.Color LIGHT = new java.awt.Color(0xEE, 0xEE, 0xD2);
    private static final java.awt.Color DARK = new java.awt.Color(0x76, 0x96, 0x56);
    private static final java.awt.Color SELECTED = new java.awt.Color(0xF6, 0xF6, 0x69);
    private static final int SQUARE_SIZE = 80;
    private static final int PIECE_SIZE = 68; // deja un margen chico dentro de cada casilla

    // Set de piezas "Cburnett" (Wikimedia Commons / lichess.org), CC BY-SA 3.0.
    // Se cargan una sola vez desde src/assets/piezas/ y se escalan al tamaño
    // de casilla. Si falta algún archivo, o la pieza es de un tipo que este
    // adaptador no conoce, se cae a un símbolo de texto.
    private static final Map<String, Image> IMAGES = loadImages();
    private static final Map<PieceType, String> LETTERS = new HashMap<>();
    private static final Map<PieceType, String> WHITE_SYMBOLS = new HashMap<>();
    private static final Map<PieceType, String> BLACK_SYMBOLS = new HashMap<>();
    private static final Map<PieceType, String> NAMES = new HashMap<>();

    static {
        define(PieceType.KING, "K", "♔", "♚", "Rey");
        define(PieceType.QUEEN, "Q", "♕", "♛", "Reina");
        define(PieceType.ROOK, "R", "♖", "♜", "Torre");
        define(PieceType.BISHOP, "B", "♗", "♝", "Alfil");
        define(PieceType.KNIGHT, "N", "♘", "♞", "Caballo");
        define(PieceType.PAWN, "P", "♙", "♟", "Peón");
    }

    private static void define(PieceType type, String letter, String white, String black, String name) {
        LETTERS.put(type, letter);
        WHITE_SYMBOLS.put(type, white);
        BLACK_SYMBOLS.put(type, black);
        NAMES.put(type, name);
    }

    private final int rows;
    private final int columns;
    private final JButton[][] squares;
    private final JLabel status = new JLabel("Turno de Blancas", SwingConstants.CENTER);
    private final Random random = new Random();

    private IChessGame game;
    private OpponentController opponent; // null mientras juegan dos personas
    private Position selection = null;
    private boolean thinking = false;

    public SwingChessView(int rows, int columns) {
        super("TP Ajedrez: Ingeniería de Software (UADE)");
        this.rows = rows;
        this.columns = columns;
        this.squares = new JButton[rows][columns];
        buildInterface();
    }

    // Los listeners se registran en el constructor de ChessGame, pero la vista
    // necesita la referencia al juego para poder mandarle movimientos: por
    // eso se inyecta después, desde el composition root (MainSwing).
    public void attachGame(IChessGame game) {
        this.game = game;
    }

    private static Map<String, Image> loadImages() {
        Map<String, Image> map = new HashMap<>();
        String folder = "src" + File.separator + "assets" + File.separator + "piezas" + File.separator;
        for (String key : new String[]{"wK", "wQ", "wR", "wB", "wN", "wP", "bK", "bQ", "bR", "bB", "bN", "bP"}) {
            try {
                BufferedImage image = ImageIO.read(new File(folder + key + ".png"));
                map.put(key, image);
            } catch (IOException e) {
                map.put(key, null);
            }
        }
        return map;
    }

    private void buildInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JComboBox<String> levelBox = new JComboBox<>(new String[]{"Dos jugadores", "Fácil", "Medio", "Difícil"});
        levelBox.addActionListener(e -> changeOpponent(levelBox.getSelectedIndex()));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top.add(new JLabel("Oponente (juega con negras):"));
        top.add(levelBox);

        JPanel boardPanel = new JPanel(new GridLayout(rows, columns));
        for (int row = rows - 1; row >= 0; row--) {
            for (int col = 0; col < columns; col++) {
                final Position pos = new Position(row, col);
                JButton button = new JButton();
                button.setBackground(squareColor(row, col));
                button.setOpaque(true);
                button.setBorderPainted(false);
                button.addActionListener(e -> onSquareClicked(pos));
                squares[row][col] = button;
                boardPanel.add(button);
            }
        }

        JButton undoButton = new JButton("Deshacer");
        undoButton.addActionListener(e -> {
            if (game == null || thinking) return;
            // Contra la IA se deshacen las dos: la respuesta de ella y la jugada propia.
            if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
            render(game.getBoard());
        });
        JButton redoButton = new JButton("Rehacer");
        redoButton.addActionListener(e -> {
            if (game == null || thinking) return;
            if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
            render(game.getBoard());
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(undoButton);
        buttons.add(redoButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(columns * SQUARE_SIZE, rows * SQUARE_SIZE + 110);
        setLocationRelativeTo(null);
    }

    private java.awt.Color squareColor(int row, int col) {
        return (row + col) % 2 == 0 ? DARK : LIGHT;
    }

    // El orden de las opciones del desplegable coincide con OpponentLevel.values().
    private void changeOpponent(int index) {
        OpponentLevel level = OpponentLevel.values()[index];
        if (level == OpponentLevel.NONE || game == null) {
            opponent = null;
        } else {
            opponent = new OpponentController(game, Opponents.create(level, random), Color.BLACK);
        }
        playOpponentIfNeeded();
    }

    private void onSquareClicked(Position pos) {
        if (game == null || game.isGameOver() || thinking) return;

        if (selection == null) {
            Piece piece = game.getBoard().pieceAt(pos);
            if (piece != null && piece.getColor() == game.getTurn()) {
                selection = pos;
                squares[pos.getRow()][pos.getColumn()].setBackground(SELECTED);
            }
            return;
        }

        Position origin = selection;
        selection = null;
        render(game.getBoard()); // limpia el resaltado de la selección

        if (origin.equals(pos)) return; // clickear la misma casilla = deseleccionar

        game.tryMove(origin, pos);
        render(game.getBoard());
        playOpponentIfNeeded();
    }

    // La IA piensa en otro hilo para no congelar la ventana, y la jugada se
    // aplica después en el hilo de la interfaz. Mientras piensa, se ignoran los clicks.
    private void playOpponentIfNeeded() {
        if (opponent == null || thinking || !opponent.isItsTurn()) return;
        thinking = true;
        status.setText("El oponente piensa...");
        final OpponentController controller = opponent;
        new Thread(() -> {
            Move move = controller.chooseMove();
            SwingUtilities.invokeLater(() -> {
                controller.play(move);
                render(game.getBoard());
                thinking = false;
            });
        }).start();
    }

    // IPromotionChooser: el núcleo pregunta a qué pieza corona el peón.
    // Si corona la IA, elige reina sin abrir el diálogo.
    @Override
    public PieceType choose(Color color) {
        if (opponent != null && color == opponent.getColor()) return PieceType.QUEEN;
        String[] options = {"Reina", "Torre", "Alfil", "Caballo"};
        int choice = JOptionPane.showOptionDialog(this, "¿A qué pieza corona el peón?",
            "Coronación", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);
        switch (choice) {
            case 1: return PieceType.ROOK;
            case 2: return PieceType.BISHOP;
            case 3: return PieceType.KNIGHT;
            default: return PieceType.QUEEN;
        }
    }

    // Solo redibuja el tablero. El texto de estado lo manejan exclusivamente
    // los listeners: si render() también lo tocara, pisaría el mensaje que
    // onInvalidMove/onMoveMade acaban de poner.
    public void render(Board board) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                applyIcon(squares[row][col], board.pieceAt(new Position(row, col)));
                squares[row][col].setBackground(squareColor(row, col));
            }
        }
    }

    private void applyIcon(JButton button, Piece piece) {
        if (piece == null) {
            button.setIcon(null);
            button.setText("");
            return;
        }
        Image image = IMAGES.get(imageKey(piece));
        if (image == null) {
            button.setIcon(null);
            button.setText(symbol(piece)); // asset faltante o pieza desconocida: texto
            return;
        }
        button.setText("");
        button.setIcon(new ImageIcon(image.getScaledInstance(PIECE_SIZE, PIECE_SIZE, Image.SCALE_SMOOTH)));
    }

    private String imageKey(Piece piece) {
        String letter = LETTERS.get(piece.getType());
        if (letter == null) return null;
        return (piece.getColor() == Color.WHITE ? "w" : "b") + letter;
    }

    // Símbolo de texto: respaldo si falta un PNG o la pieza no tiene imagen.
    private String symbol(Piece piece) {
        boolean isWhite = piece.getColor() == Color.WHITE;
        String symbol = (isWhite ? WHITE_SYMBOLS : BLACK_SYMBOLS).get(piece.getType());
        if (symbol != null) return symbol;
        String initial = piece.getType().getName().substring(0, 1);
        return isWhite ? initial.toUpperCase() : initial.toLowerCase();
    }

    private String name(PieceType type) {
        String name = NAMES.get(type);
        return name != null ? name : type.getName();
    }

    private String name(Color color) {
        return color == Color.WHITE ? "Blancas" : "Negras";
    }

    @Override
    public void onMoveMade(Position from, Position to, Piece piece, Piece captured) {
        String captureText = captured != null ? " (captura " + name(captured.getType()) + ")" : "";
        status.setText(name(piece.getColor()) + ": " + name(piece.getType())
            + " " + from + " -> " + to + captureText);
    }

    @Override
    public void onCastling(Color color, boolean kingside) {
        status.setText(name(color) + ": enroque " + (kingside ? "corto" : "largo") + ".");
    }

    @Override
    public void onPromotion(Color color, Position position, PieceType newPiece) {
        status.setText(name(color) + ": corona en " + position + " a " + name(newPiece) + ".");
    }

    @Override
    public void onUndo() {
        status.setText("Se deshizo el último movimiento.");
    }

    @Override
    public void onRedo() {
        status.setText("Se rehizo la jugada.");
    }

    @Override
    public void onInvalidMove(String reason) {
        status.setText("Inválido: " + reason);
    }

    @Override
    public void onCheck(Color colorInCheck) {
        status.setText("¡Jaque a " + name(colorInCheck) + "!");
    }

    @Override
    public void onCheckmate(Color winner) {
        status.setText("¡Jaque mate! Ganan " + name(winner) + ".");
        JOptionPane.showMessageDialog(this, "Jaque mate. Ganan " + name(winner) + ".");
    }

    @Override
    public void onStalemate() {
        status.setText("Ahogado: tablas.");
        JOptionPane.showMessageDialog(this, "Ahogado: tablas, no hay movimientos legales.");
    }

    @Override
    public void onDraw(String reason) {
        status.setText("Tablas: " + reason + ".");
        JOptionPane.showMessageDialog(this, "Tablas: " + reason + ".");
    }
}
```

- [ ] **Step 2: Reemplazar `src/MainSwing.java` por esta versión completa**

```java
import adaptadores.SwingChessView;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.IChessGame;
import nucleo.IDrawRule;
import nucleo.ISpecialMove;
import nucleo.PieceFactory;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.EnPassantMove;
import nucleo.especiales.PromotionMove;
import nucleo.tablas.FiftyMoveRule;
import nucleo.tablas.InsufficientMaterialRule;
import nucleo.tablas.ThreefoldRepetitionRule;
import puertos.IGameStatusListener;
import puertos.IMoveListener;

import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Composition root de la variante gráfica. Mismo esquema que Main.java:
// se arma el tablero, se crea el adaptador (SwingChessView en vez de
// ConsoleRenderer, y a la vez es quien elige la pieza de coronación y quien
// maneja al oponente) y se inyecta en ChessGame por constructor. ChessGame,
// Board y todo nucleo/ no tienen ni un import de Swing.
public class MainSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PieceFactory pieces = PieceFactory.standard();
            Board board = BoardSetup.standard(pieces);
            SwingChessView view = new SwingChessView(board.getRows(), board.getColumns());

            List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
                new CastlingMove(),
                new EnPassantMove(),
                new PromotionMove(pieces, view));
            List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
                new InsufficientMaterialRule(),
                new FiftyMoveRule(),
                new ThreefoldRepetitionRule());

            IChessGame game = new ChessGame(board,
                Collections.<IMoveListener>singletonList(view),
                Collections.<IGameStatusListener>singletonList(view),
                specialMoves,
                drawRules);

            view.attachGame(game);
            view.render(board);
            view.setVisible(true);
        });
    }
}
```

- [ ] **Step 3: Compilar y correr los tests**

Run: `./scripts/test.sh`
Expected: `61 pasados, 0 fallados.`

- [ ] **Step 4: Probar la ventana a mano**

Run: `java -cp out MainSwing`

Comprobar: (a) con "Dos jugadores" se juega como antes; (b) al elegir "Medio" y mover una pieza blanca, la ventana muestra "El oponente piensa..." y después aparece la jugada negra sin congelarse; (c) los botones Deshacer y Rehacer funcionan, y contra la IA deshacen la jugada propia y la respuesta; (d) en "Difícil" la respuesta tarda unos segundos como mucho; (e) cerrar la ventana termina el programa.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "oponente en la ventana, rehacer y desplegable de nivel"
```

---

### Task 5.4: verificación de la fase

- [ ] **Step 1: `nucleo/` no conoce a `ai/` ni a los adaptadores**

Run: `grep -rn "import ai\|import adaptadores" src/nucleo | wc -l`
Expected: `0`

- [ ] **Step 2: Sin identificadores en español ni raya**

Run: `python3 scripts/find_spanish_identifiers.py && grep -rn $'\xe2\x80\x94' src | wc -l`
Expected: `0 identificadores en español` y `0`.

- [ ] **Step 3: Revisión y push**

Pedir revisión con `superpowers:requesting-code-review` y después `git push`.
