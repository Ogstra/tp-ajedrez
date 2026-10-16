# Fase 4: reglas opcionales (rehacer, tablas, captura al paso)

> **Para el agente:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Pasos con casillas `- [x]`. Esta fase es TDD de verdad: en cada tarea el test se escribe primero, se ve fallar, y recién después se implementa.

**Goal:** agregar rehacer (redo), las tablas (material insuficiente, 50 movimientos, repetición) y la captura al paso, sin romper nada de lo anterior.

**Architecture:** rehacer es una pila en `ChessGame`. Las tablas son reglas inyectadas (`IDrawRule`) que llevan a un estado terminal `DrawState`. La captura al paso es un `ISpecialMove` más, como el enroque. Todo se arma en el composition root.

**Tech Stack:** Java, runner propio `ChessCoreTests`.

**Antes de empezar:** fases 0 a 3 terminadas. Estado esperado: `./scripts/test.sh` con `40 pasados, 0 fallados.`

---

### Task 4.1: rehacer (redo)

**Files:**
- Modify: `src/nucleo/IChessGame.java`, `src/nucleo/ChessGame.java`, `src/puertos/IMoveListener.java`
- Modify: `src/adaptadores/ConsoleRenderer.java`, `src/adaptadores/SwingChessView.java`, `src/Main.java`
- Test: `src/ChessCoreTests.java`

- [x] **Step 1: Escribir los tests que fallan**

Agregar a `ChessCoreTests` y registrar en `main`:

```java
    // Stub que cuenta cuántas veces se le preguntó.
    private static class CountingChooser implements IPromotionChooser {
        int calls = 0;

        @Override
        public PieceType choose(Color color) {
            calls++;
            return PieceType.QUEEN;
        }
    }

    // TDD: rehacer vuelve a aplicar la jugada deshecha.
    private static void testRedoRestoresTheMove() {
        // Arrange
        Board board = BoardSetup.standard(PIECES);
        IChessGame game = newGame(board);
        game.tryMove(at("e2"), at("e4"));
        Board afterMove = board.cloneBoard();
        game.undo();

        // Act
        boolean redone = game.redo();

        // Assert
        assertTrue("testRedoRestoresTheMove",
            redone && afterMove.equals(board) && game.getTurn() == Color.BLACK,
            "después de rehacer, el peón vuelve a e4 y le toca a las negras");
    }

    // TDD
    private static void testRedoWithNothingToRedoReturnsFalse() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act / Assert
        assertTrue("testRedoWithNothingToRedoReturnsFalse", !game.redo(),
            "sin nada deshecho no hay qué rehacer");
    }

    // TDD: una jugada nueva borra lo que se podía rehacer.
    private static void testNewMoveClearsRedo() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("e2"), at("e4"));
        game.undo();
        game.tryMove(at("d2"), at("d4"));

        // Act
        boolean redone = game.redo();

        // Assert
        assertTrue("testNewMoveClearsRedo", !redone,
            "después de jugar algo distinto ya no se puede rehacer la jugada vieja");
    }

    // TDD: rehacer una coronación no vuelve a preguntar la pieza.
    private static void testRedoOfPromotionDoesNotAskAgain() {
        // Arrange
        Board board = boardWithPawnOnSeventh();
        CountingChooser chooser = new CountingChooser();
        IChessGame game = buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(), chooser);
        game.tryMove(at("a7"), at("a8"));
        game.undo();

        // Act
        game.redo();

        // Assert
        assertTrue("testRedoOfPromotionDoesNotAskAgain",
            chooser.calls == 1 && hasPiece(board, "a8", PieceType.QUEEN),
            "el comando ya guarda la pieza elegida: se pregunta una sola vez");
    }
```

```java
        testRedoRestoresTheMove();
        testRedoWithNothingToRedoReturnsFalse();
        testNewMoveClearsRedo();
        testRedoOfPromotionDoesNotAskAgain();
```

(Si todavía no se llama `hasPiece` en tu copia, usá el nombre que le dejó la fase 2 al helper que antes era `hay`.)

- [x] **Step 2: Correr y ver que no compila**

Run: `./scripts/build.sh`
Expected: error `cannot find symbol: method redo()`.

- [x] **Step 3: Agregar `redo()` al puerto de entrada**

En `src/nucleo/IChessGame.java`, después de `boolean undo();`:

```java
    boolean redo();
```

- [x] **Step 4: Agregar `onRedo()` al listener de movimientos**

En `src/puertos/IMoveListener.java`, después de `void onUndo();`:

```java
    void onRedo();
```

- [x] **Step 5: Implementar en `ChessGame`**

Agregar el campo junto a `history`:

```java
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();
```

Agregar `@Override` a `undo()` si no lo tiene, y reemplazar `undo()` por esta versión más el nuevo `redo()`:

```java
    @Override
    public boolean undo() {
        if (history.isEmpty()) return false;
        HistoryEntry entry = history.pop();
        entry.command.undo();
        state = entry.stateBefore;
        redoStack.push(entry);
        for (IMoveListener listener : moveListeners) listener.onUndo();
        return true;
    }

    @Override
    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        HistoryEntry entry = redoStack.pop();
        entry.command.execute();
        history.push(entry);
        state = entry.stateAfter;
        for (IMoveListener listener : moveListeners) {
            entry.command.announce(listener);
            listener.onRedo();
        }
        return true;
    }
```

En `tryMove`, justo antes de la línea `IMoveCommand command = createCommand(from, to);`, agregar:

```java
        redoStack.clear(); // una jugada nueva borra lo que se podía rehacer
```

- [x] **Step 6: Adaptadores**

`ConsoleRenderer`, agregar:

```java
    @Override
    public void onRedo() {
        System.out.println("Se rehizo la jugada.");
    }
```

`SwingChessView`, agregar:

```java
    @Override
    public void onRedo() {
        status.setText("Se rehizo la jugada.");
    }
```

(El botón "Rehacer" de la ventana se agrega en la fase 5, donde se entrega el archivo completo de `SwingChessView`.)

`Main`: agregar, justo después del bloque que maneja `undo`:

```java
            if (input.equalsIgnoreCase("redo")) {
                game.redo();
                renderer.render(board);
                continue;
            }
```

y cambiar el texto de ayuda a: `"Movimiento: 'e2 e4' (u 'e2e4'). 'undo' deshace, 'redo' rehace, 'salir' termina."`

- [x] **Step 7: Actualizar el mock del test**

En `RecordingObserver` agregar:

```java
        @Override public void onRedo() { events.add("redo"); }
```

- [x] **Step 8: Correr los tests**

Run: `./scripts/test.sh`
Expected: `44 pasados, 0 fallados.`

- [x] **Step 9: Commit**

```bash
git add -A
git commit -m "rehacer jugadas"
```

---

### Task 4.2: tablas

**Files:**
- Create: `src/nucleo/IDrawRule.java`, `src/nucleo/DrawState.java`
- Create: `src/nucleo/tablas/InsufficientMaterialRule.java`, `src/nucleo/tablas/FiftyMoveRule.java`, `src/nucleo/tablas/ThreefoldRepetitionRule.java`
- Modify: `src/nucleo/HistoryEntry.java`, `src/nucleo/ChessGame.java`, `src/nucleo/IGameContext.java`, `src/nucleo/IMoveCommand.java`, `src/nucleo/MoveCommand.java`, `src/nucleo/PromotionCommand.java`, `src/nucleo/CastlingCommand.java`, `src/nucleo/MoveOutcome.java`
- Modify: `src/puertos/IGameStatusListener.java`, `src/adaptadores/ConsoleRenderer.java`, `src/adaptadores/SwingChessView.java`, `src/Main.java`, `src/MainSwing.java`, `AGENTS.md`, `scripts/find_spanish_identifiers.py`
- Test: `src/ChessCoreTests.java`

- [x] **Step 1: Escribir los tests que fallan**

Primero, `buildGame` pasa a recibir las reglas de tablas. Reemplazar el método `buildGame` actual por estos cuatro métodos:

```java
    private static ChessGame buildGame(Board board, List<IMoveListener> moveListeners,
                                       List<IGameStatusListener> statusListeners,
                                       IPromotionChooser chooser, List<IDrawRule> drawRules) {
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(PIECES, chooser));
        return new ChessGame(board, moveListeners, statusListeners, specialMoves, drawRules);
    }

    private static List<IDrawRule> standardDrawRules() {
        return Arrays.<IDrawRule>asList(
            new InsufficientMaterialRule(), new FiftyMoveRule(), new ThreefoldRepetitionRule());
    }

    private static ChessGame buildGame(Board board, List<IMoveListener> moveListeners,
                                       List<IGameStatusListener> statusListeners, IPromotionChooser chooser) {
        return buildGame(board, moveListeners, statusListeners, chooser, standardDrawRules());
    }

    private static ChessGame newGameWithDrawRules(Board board, IDrawRule... rules) {
        return buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(),
            new AlwaysQueenChooser(), Arrays.asList(rules));
    }
```

(`newGame(Board)`, `newGame(Board, RecordingObserver)` y `newGameChoosing` no cambian: siguen llamando a la versión de cuatro parámetros, que ahora usa las reglas de tablas estándar.)

Agregar `import nucleo.IDrawRule;`, `import nucleo.tablas.FiftyMoveRule;`, `import nucleo.tablas.InsufficientMaterialRule;`, `import nucleo.tablas.ThreefoldRepetitionRule;`.

El mock suma `onDraw`:

```java
        @Override public void onDraw(String reason) { events.add("draw"); }
```

Con material mínimo, coronar a caballo da tablas. Para que `testPromotionToKnight` siga probando solo la coronación, agregar un peón negro a `boardWithPawnOnSeventh()`:

```java
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("h7"));
```

Ahora los tests nuevos:

```java
    // TDD: rey contra rey son tablas.
    private static void testKingAgainstKingIsDraw() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        IChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("e2"));

        // Assert
        assertTrue("testKingAgainstKingIsDraw", outcome == MoveOutcome.DRAW && game.isGameOver(),
            "con solo dos reyes no hay forma de ganar");
    }

    // TDD
    private static void testInsufficientMaterialCases() {
        // Arrange: cuatro tableros. Caso por caso: [rey+alfil vs rey, rey+torre vs rey,
        // alfiles del mismo color, alfiles de distinto color].
        Board bishopVsKing = new Board();
        bishopVsKing.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        bishopVsKing.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        bishopVsKing.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));

        Board rookVsKing = new Board();
        rookVsKing.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        rookVsKing.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        rookVsKing.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));

        Board sameColorBishops = new Board();
        sameColorBishops.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        sameColorBishops.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        sameColorBishops.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        sameColorBishops.placePiece(piece(PieceType.BISHOP, Color.BLACK), at("f8"));

        Board oppositeColorBishops = new Board();
        oppositeColorBishops.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        oppositeColorBishops.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        oppositeColorBishops.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        oppositeColorBishops.placePiece(piece(PieceType.BISHOP, Color.BLACK), at("c8"));

        InsufficientMaterialRule rule = new InsufficientMaterialRule();

        // Act
        boolean a = rule.reason(newGameWithDrawRules(bishopVsKing)) != null;
        boolean b = rule.reason(newGameWithDrawRules(rookVsKing)) != null;
        boolean c = rule.reason(newGameWithDrawRules(sameColorBishops)) != null;
        boolean d = rule.reason(newGameWithDrawRules(oppositeColorBishops)) != null;

        // Assert
        assertTrue("testInsufficientMaterialCases", a && !b && c && !d,
            "alfil solo y alfiles del mismo color no alcanzan; torre sola y alfiles de colores distintos sí");
    }

    // TDD: 100 medias jugadas sin captura ni movimiento de peón (acá el límite es 4 para no jugar 100).
    private static void testFiftyMoveRuleDeclaresDraw() {
        // Arrange
        Board board = rooksOnTheHFile();
        IChessGame game = newGameWithDrawRules(board, new FiftyMoveRule(4));
        String[][] moves = {{"h1", "h2"}, {"h8", "h7"}, {"h2", "h1"}, {"h7", "h8"}};

        // Act
        MoveOutcome last = null;
        MoveOutcome beforeLast = null;
        for (int i = 0; i < moves.length; i++) {
            MoveOutcome outcome = game.tryMove(at(moves[i][0]), at(moves[i][1]));
            if (i == moves.length - 2) beforeLast = outcome;
            last = outcome;
        }

        // Assert
        assertTrue("testFiftyMoveRuleDeclaresDraw", beforeLast == MoveOutcome.OK && last == MoveOutcome.DRAW,
            "las tablas llegan recién en la cuarta media jugada sin captura ni peón");
    }

    private static Board rooksOnTheHFile() {
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("h8"));
        return board;
    }

    // TDD: la misma posición con el mismo turno tres veces.
    private static void testThreefoldRepetitionDeclaresDraw() {
        // Arrange: las dos torres van y vienen; la posición inicial reaparece cada 4 medias jugadas.
        Board board = rooksOnTheHFile();
        IChessGame game = newGameWithDrawRules(board, new ThreefoldRepetitionRule());
        String[][] cycle = {{"h1", "h2"}, {"h8", "h7"}, {"h2", "h1"}, {"h7", "h8"}};

        // Act
        boolean earlyDraw = false;
        MoveOutcome last = null;
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < cycle.length; i++) {
                last = game.tryMove(at(cycle[i][0]), at(cycle[i][1]));
                boolean isFinalMove = round == 1 && i == cycle.length - 1;
                if (last == MoveOutcome.DRAW && !isFinalMove) earlyDraw = true;
            }
        }

        // Assert
        assertTrue("testThreefoldRepetitionDeclaresDraw", !earlyDraw && last == MoveOutcome.DRAW,
            "la posición inicial aparece por tercera vez recién en la jugada 8");
    }

    // TDD: se puede deshacer la jugada que declaró las tablas.
    private static void testUndoAfterDrawReopensTheGame() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("e2"));

        // Act
        boolean undone = game.undo();

        // Assert
        assertTrue("testUndoAfterDrawReopensTheGame", undone && !game.isGameOver(),
            "al deshacer las tablas la partida sigue");
    }

    // TDD: los listeners de estado se enteran de las tablas.
    private static void testStatusListenerReceivesDraw() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        RecordingObserver observer = new RecordingObserver();
        IChessGame game = newGame(board, observer);

        // Act
        game.tryMove(at("e1"), at("e2"));

        // Assert
        assertTrue("testStatusListenerReceivesDraw", observer.events.equals(Arrays.asList("move", "draw")),
            "esperaba [move, draw] y llegó " + observer.events);
    }
```

```java
        testKingAgainstKingIsDraw();
        testInsufficientMaterialCases();
        testFiftyMoveRuleDeclaresDraw();
        testThreefoldRepetitionDeclaresDraw();
        testUndoAfterDrawReopensTheGame();
        testStatusListenerReceivesDraw();
```

- [x] **Step 2: Correr y ver que no compila**

Run: `./scripts/build.sh`
Expected: errores `cannot find symbol` (`IDrawRule`, `InsufficientMaterialRule`, etc.).

- [x] **Step 3: Crear `IDrawRule`, `DrawState` y las tres reglas**

`src/nucleo/IDrawRule.java`:

```java
package nucleo;

// Strategy (Clase 3) para las condiciones de tablas. ChessGame las recibe por
// constructor y las consulta después de cada jugada. Una regla nueva es una
// clase nueva más una línea en el composition root.
public interface IDrawRule {
    // El motivo de las tablas, o null si esta regla no las declara.
    String reason(IGameContext context);
}
```

`src/nucleo/DrawState.java`:

```java
package nucleo;

// Tablas: estado terminal, como el mate y el ahogado.
public class DrawState implements IGameState {
    private final Color toMove;
    private final String reason;

    public DrawState(Color toMove, String reason) {
        this.toMove = toMove;
        this.reason = reason;
    }

    public String getReason() { return reason; }

    @Override
    public Color currentColor() { return toMove; }

    @Override
    public boolean isOver() { return true; }

    @Override
    public IGameState afterMove() { return this; }
}
```

`src/nucleo/tablas/InsufficientMaterialRule.java`:

```java
package nucleo.tablas;

import nucleo.Board;
import nucleo.Color;
import nucleo.IDrawRule;
import nucleo.IGameContext;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

// Tablas por material insuficiente: ninguno de los dos bandos puede dar mate.
// Casos: rey contra rey, rey y un solo alfil o caballo contra rey, y solo
// alfiles todos en casillas del mismo color. Cualquier otra pieza (peón,
// torre, reina o una pieza nueva) cuenta como material suficiente.
public class InsufficientMaterialRule implements IDrawRule {

    @Override
    public String reason(IGameContext context) {
        Board board = context.getBoard();
        List<Position> positions = new ArrayList<>(board.positionsOfColor(Color.WHITE));
        positions.addAll(board.positionsOfColor(Color.BLACK));

        int minorPieces = 0;
        boolean onlyBishops = true;
        int bishopSquareColor = -1;
        boolean bishopsOnSameColor = true;

        for (Position position : positions) {
            Piece piece = board.pieceAt(position);
            if (piece.getType().equals(PieceType.KING)) continue;
            if (piece.getType().equals(PieceType.BISHOP)) {
                int squareColor = (position.getRow() + position.getColumn()) % 2;
                if (bishopSquareColor == -1) bishopSquareColor = squareColor;
                else if (bishopSquareColor != squareColor) bishopsOnSameColor = false;
                minorPieces++;
            } else if (piece.getType().equals(PieceType.KNIGHT)) {
                minorPieces++;
                onlyBishops = false;
            } else {
                return null; // peón, torre, reina o pieza desconocida: hay material para dar mate
            }
        }

        if (minorPieces == 0) return "rey contra rey";
        if (minorPieces == 1) return "un solo alfil o caballo contra rey";
        if (onlyBishops && bishopsOnSameColor) return "solo alfiles en casillas del mismo color";
        return null;
    }
}
```

`src/nucleo/tablas/FiftyMoveRule.java`:

```java
package nucleo.tablas;

import nucleo.IDrawRule;
import nucleo.IGameContext;

// Tablas por la regla de los 50 movimientos: 100 medias jugadas seguidas sin
// ninguna captura ni movimiento de peón. El límite se puede cambiar para probar.
public class FiftyMoveRule implements IDrawRule {

    private final int halfMoves;

    public FiftyMoveRule() {
        this(100);
    }

    public FiftyMoveRule(int halfMoves) {
        this.halfMoves = halfMoves;
    }

    @Override
    public String reason(IGameContext context) {
        return context.halfMoveClock() >= halfMoves ? "regla de los 50 movimientos" : null;
    }
}
```

`src/nucleo/tablas/ThreefoldRepetitionRule.java`:

```java
package nucleo.tablas;

import nucleo.IDrawRule;
import nucleo.IGameContext;

// Tablas por repetición: la misma posición, con el mismo bando al turno, tres
// veces. Simplificación: se ignoran los derechos de enroque y de captura al
// paso (las reglas oficiales los tienen en cuenta).
public class ThreefoldRepetitionRule implements IDrawRule {

    @Override
    public String reason(IGameContext context) {
        return context.occurrencesOfCurrentPosition() >= 3 ? "repetición de la posición tres veces" : null;
    }
}
```

- [x] **Step 4: `MoveOutcome` y los listeners de estado**

`src/nucleo/MoveOutcome.java`:

```java
package nucleo;

public enum MoveOutcome {
    INVALID, OK, CHECK, CHECKMATE, STALEMATE, DRAW
}
```

En `src/puertos/IGameStatusListener.java`, agregar:

```java
    void onDraw(String reason);
```

- [x] **Step 5: Ampliar `IGameContext` y `IMoveCommand`**

`src/nucleo/IGameContext.java` completo:

```java
package nucleo;

// Lo que una regla (ISpecialMove, IDrawRule) puede consultar de la partida,
// sin conocer a ChessGame (DIP: las reglas dependen de esta abstracción).
public interface IGameContext {
    Board getBoard();

    // true si, en alguna jugada que sigue en pie (no deshecha), una pieza
    // salió de esta casilla o llegó a ella. Una casilla sin tocar sigue
    // teniendo a la pieza con la que arrancó la partida.
    boolean hasBeenTouched(Position pos);

    // true si alguna pieza rival de "color" ataca la casilla.
    boolean isAttacked(Position pos, Color color);

    // La última jugada que sigue en pie, o null si todavía no se jugó ninguna.
    Move lastMove();

    // Cuántas medias jugadas seguidas (de las últimas) no movieron un peón ni capturaron.
    int halfMoveClock();

    // Cuántas veces apareció la posición actual (con el mismo bando al turno), contando esta.
    int occurrencesOfCurrentPosition();
}
```

En `IMoveCommand`, agregar:

```java
    // true si la jugada mueve un peón o captura (reinicia el contador de los 50 movimientos).
    boolean resetsHalfMoveClock();
```

En `MoveCommand`:

```java
    @Override
    public boolean resetsHalfMoveClock() {
        return movedPiece.getType().equals(PieceType.PAWN) || capturedPiece != null;
    }
```

En `PromotionCommand` (mueve un peón):

```java
    @Override
    public boolean resetsHalfMoveClock() {
        return true;
    }
```

En `CastlingCommand`:

```java
    @Override
    public boolean resetsHalfMoveClock() {
        return false;
    }
```

- [x] **Step 6: `HistoryEntry` con foto del tablero**

`src/nucleo/HistoryEntry.java` completo:

```java
package nucleo;

// Una jugada del historial junto con el estado de la partida antes y después,
// y una foto del tablero después de la jugada (para contar repeticiones).
// Así deshacer es volver a stateBefore, sin trucos de alternar turnos.
final class HistoryEntry {
    final IMoveCommand command;
    final IGameState stateBefore;
    final Board boardAfter;
    IGameState stateAfter;

    HistoryEntry(IMoveCommand command, IGameState stateBefore, Board boardAfter) {
        this.command = command;
        this.stateBefore = stateBefore;
        this.boardAfter = boardAfter;
    }
}
```

- [x] **Step 7: Reemplazar `src/nucleo/ChessGame.java` por la versión completa**

```java
package nucleo;

import puertos.IGameStatusListener;
import puertos.IMoveListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Núcleo del sistema (Clase 2): orquesta la partida. No sabe nada de
// consola, UI ni ningún detalle técnico: solo reglas de negocio del
// ajedrez. Todo lo que puede variar entra por constructor, inyectado y
// contra una interfaz (Clase 3: DIP + inyección de dependencias nivel 4):
// los listeners (cómo se avisa lo que pasa), las reglas especiales
// (enroque, coronación, captura al paso) y las reglas de tablas.
// ChessGame no sabe cuáles son ni cuántas.
public class ChessGame implements IChessGame, IGameContext {
    private final Board board;
    private final List<IMoveListener> moveListeners;
    private final List<IGameStatusListener> statusListeners;
    private final List<ISpecialMove> specialMoves;
    private final List<IDrawRule> drawRules;
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();
    private final Board initialBoard;
    private final Color initialColor;
    private IGameState state;

    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves,
                     List<IDrawRule> drawRules) {
        this(board, moveListeners, statusListeners, specialMoves, drawRules, Color.WHITE);
    }

    // Con firstToMove se arma una partida que arranca con el turno de las negras
    // (la usa la IA para buscar jugadas desde una posición a mitad de partida).
    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves,
                     List<IDrawRule> drawRules, Color firstToMove) {
        this.board = board;
        this.moveListeners = moveListeners;
        this.statusListeners = statusListeners;
        this.specialMoves = specialMoves;
        this.drawRules = drawRules;
        this.initialBoard = board.cloneBoard();
        this.initialColor = firstToMove;
        this.state = firstToMove == Color.WHITE ? new WhiteTurnState() : new BlackTurnState();
    }

    @Override
    public Board getBoard() { return board; }

    @Override
    public Color getTurn() { return state.currentColor(); }

    @Override
    public boolean isGameOver() { return state.isOver(); }

    @Override
    public MoveOutcome tryMove(Position from, Position to) {
        if (state.isOver()) {
            notifyInvalid("La partida ya terminó.");
            return MoveOutcome.INVALID;
        }
        if (!board.isInside(from) || !board.isInside(to)) {
            notifyInvalid("Esa casilla no existe en el tablero.");
            return MoveOutcome.INVALID;
        }

        Piece piece = board.pieceAt(from);
        if (piece == null || piece.getColor() != state.currentColor()) {
            notifyInvalid("En " + from + " no hay una pieza propia para mover.");
            return MoveOutcome.INVALID;
        }

        if (!legalMovesFrom(from).contains(to)) {
            notifyInvalid("Movimiento inválido de " + from + " a " + to + ".");
            return MoveOutcome.INVALID;
        }

        redoStack.clear(); // una jugada nueva borra lo que se podía rehacer
        IMoveCommand command = createCommand(from, to);
        command.execute();
        HistoryEntry entry = new HistoryEntry(command, state, board.cloneBoard());
        history.push(entry);

        Color mover = state.currentColor();
        Color opponent = mover.opposite();
        state = state.afterMove();
        entry.stateAfter = state;

        for (IMoveListener listener : moveListeners) command.announce(listener);

        boolean check = isInCheck(opponent, board);
        boolean hasMoves = hasAnyLegalMove(opponent);

        MoveOutcome outcome;
        if (check && !hasMoves) {
            state = new CheckmateState(opponent);
            for (IGameStatusListener listener : statusListeners) listener.onCheckmate(mover);
            outcome = MoveOutcome.CHECKMATE;
        } else if (!check && !hasMoves) {
            state = new StalemateState(opponent);
            for (IGameStatusListener listener : statusListeners) listener.onStalemate();
            outcome = MoveOutcome.STALEMATE;
        } else {
            String drawReason = findDrawReason();
            if (drawReason != null) {
                state = new DrawState(opponent, drawReason);
                for (IGameStatusListener listener : statusListeners) listener.onDraw(drawReason);
                outcome = MoveOutcome.DRAW;
            } else if (check) {
                for (IGameStatusListener listener : statusListeners) listener.onCheck(opponent);
                outcome = MoveOutcome.CHECK;
            } else {
                outcome = MoveOutcome.OK;
            }
        }
        entry.stateAfter = state;
        return outcome;
    }

    @Override
    public boolean undo() {
        if (history.isEmpty()) return false;
        HistoryEntry entry = history.pop();
        entry.command.undo();
        state = entry.stateBefore;
        redoStack.push(entry);
        for (IMoveListener listener : moveListeners) listener.onUndo();
        return true;
    }

    @Override
    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        HistoryEntry entry = redoStack.pop();
        entry.command.execute();
        history.push(entry);
        state = entry.stateAfter;
        for (IMoveListener listener : moveListeners) {
            entry.command.announce(listener);
            listener.onRedo();
        }
        return true;
    }

    @Override
    public List<Move> legalMoves() {
        List<Move> moves = new ArrayList<>();
        if (state.isOver()) return moves;
        for (Position from : board.positionsOfColor(state.currentColor())) {
            for (Position to : legalMovesFrom(from)) {
                moves.add(new Move(from, to));
            }
        }
        return moves;
    }

    // IGameContext: lo que las reglas pueden preguntar.
    @Override
    public boolean hasBeenTouched(Position pos) {
        for (HistoryEntry entry : history) {
            if (entry.command.touchedSquares().contains(pos)) return true;
        }
        return false;
    }

    @Override
    public boolean isAttacked(Position pos, Color color) {
        return isAttackedOn(board, pos, color);
    }

    @Override
    public Move lastMove() {
        return history.isEmpty() ? null : history.peek().command.getMove();
    }

    @Override
    public int halfMoveClock() {
        int count = 0;
        for (HistoryEntry entry : history) { // de la más reciente a la más vieja
            if (entry.command.resetsHalfMoveClock()) break;
            count++;
        }
        return count;
    }

    @Override
    public int occurrencesOfCurrentPosition() {
        Color toMove = state.currentColor();
        int count = (initialColor == toMove && initialBoard.equals(board)) ? 1 : 0;
        for (HistoryEntry entry : history) {
            if (entry.stateAfter.currentColor() == toMove && entry.boardAfter.equals(board)) count++;
        }
        return count;
    }

    private String findDrawReason() {
        for (IDrawRule rule : drawRules) {
            String reason = rule.reason(this);
            if (reason != null) return reason;
        }
        return null;
    }

    // La primera regla especial que reconozca la jugada arma su comando; si
    // ninguna, es un movimiento común.
    private IMoveCommand createCommand(Position from, Position to) {
        for (ISpecialMove rule : specialMoves) {
            IMoveCommand command = rule.commandFor(this, from, to);
            if (command != null) return command;
        }
        return new MoveCommand(board, from, to);
    }

    private void notifyInvalid(String reason) {
        for (IGameStatusListener listener : statusListeners) listener.onInvalidMove(reason);
    }

    // Movimientos legales de una pieza: los pseudo-legales de su Strategy
    // (+ los destinos extra de las reglas especiales), filtrando los que
    // dejarían al propio rey en jaque.
    private List<Position> legalMovesFrom(Position from) {
        Piece piece = board.pieceAt(from);
        List<Position> candidates = new ArrayList<>(piece.possibleMoves(board, from));
        for (ISpecialMove rule : specialMoves) {
            candidates.addAll(rule.extraDestinations(this, from));
        }

        List<Position> legal = new ArrayList<>();
        for (Position destination : candidates) {
            Board simulated = board.cloneBoard();
            simulated.movePiece(from, destination);
            if (!isInCheck(piece.getColor(), simulated)) {
                legal.add(destination);
            }
        }
        return legal;
    }

    // ¿El rey de "color" está atacado por alguna pieza rival, en "b"?
    private boolean isInCheck(Color color, Board b) {
        Position kingPos = b.findKing(color);
        if (kingPos == null) return false; // no debería pasar en una partida real
        return isAttackedOn(b, kingPos, color);
    }

    private boolean isAttackedOn(Board b, Position pos, Color color) {
        for (Position p : b.positionsOfColor(color.opposite())) {
            Piece rival = b.pieceAt(p);
            if (rival.attackedSquares(b, p).contains(pos)) {
                return true;
            }
        }
        return false;
    }

    // ¿"color" tiene al menos un movimiento legal disponible? Se usa para
    // detectar jaque mate (si está en jaque y no tiene movimientos) y
    // ahogado (si no está en jaque pero tampoco tiene movimientos).
    private boolean hasAnyLegalMove(Color color) {
        for (Position pos : board.positionsOfColor(color)) {
            if (!legalMovesFrom(pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
```

- [x] **Step 8: Adaptadores y composition roots**

`ConsoleRenderer`:

```java
    @Override
    public void onDraw(String reason) {
        System.out.println("Tablas: " + reason + ".");
    }
```

`SwingChessView`:

```java
    @Override
    public void onDraw(String reason) {
        status.setText("Tablas: " + reason + ".");
        JOptionPane.showMessageDialog(this, "Tablas: " + reason + ".");
    }
```

`Main` y `MainSwing`: agregar los imports `nucleo.IDrawRule`, `nucleo.tablas.FiftyMoveRule`, `nucleo.tablas.InsufficientMaterialRule`, `nucleo.tablas.ThreefoldRepetitionRule`, armar la lista y pasarla como quinto argumento:

```java
        List<IDrawRule> drawRules = Arrays.<IDrawRule>asList(
            new InsufficientMaterialRule(),
            new FiftyMoveRule(),
            new ThreefoldRepetitionRule());
```

```java
        IChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(renderer),
            Collections.<IGameStatusListener>singletonList(renderer),
            specialMoves,
            drawRules);
```

(En `MainSwing` el listener es `view`.)

- [x] **Step 9: Documentar el paquete nuevo**

En `AGENTS.md`, en la regla de estilo de los idiomas, sumar `tablas` a la lista de paquetes que no se renombran. En `scripts/find_spanish_identifiers.py`, agregar `"tablas"` al conjunto `ALLOWED`.

- [x] **Step 10: Correr los tests**

Run: `./scripts/test.sh`
Expected: `50 pasados, 0 fallados.`

Si `testInsufficientMaterialCases` falla en el caso de los alfiles de distinto color, revisá el cálculo del color de casilla: `c1` es fila 0, columna 2 (suma par) y `c8` es fila 7, columna 2 (suma impar).

- [x] **Step 11: Commit**

```bash
git add -A
git commit -m "tablas por material, 50 movimientos y repetición"
```

---

### Task 4.3: captura al paso

**Files:**
- Create: `src/nucleo/EnPassantCommand.java`, `src/nucleo/especiales/EnPassantMove.java`
- Modify: `src/Main.java`, `src/MainSwing.java`
- Test: `src/ChessCoreTests.java`

- [x] **Step 1: Escribir los tests que fallan**

`buildGame` suma la regla nueva (para que los tests la tengan armada). Reemplazar la creación de `specialMoves` dentro del `buildGame` de 5 parámetros por:

```java
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new EnPassantMove(),
            new PromotionMove(PIECES, chooser));
```

(con `import nucleo.especiales.EnPassantMove;`). Después agregar y registrar:

```java
    // El tablero de los tests de captura al paso: el peón blanco de e5 espera al peón negro de d7.
    private static Board enPassantBoard() {
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e5"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("d7"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("e8"));
        return board;
    }

    // TDD: justo después del doble paso del peón rival se puede capturar al paso.
    private static void testEnPassantIsAllowedRightAfterTheDoubleStep() {
        // Arrange
        Board board = enPassantBoard();
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));   // las blancas esperan
        game.tryMove(at("d7"), at("d5"));   // doble paso negro, al lado del peón blanco

        // Act
        MoveOutcome outcome = game.tryMove(at("e5"), at("d6"));

        // Assert
        assertTrue("testEnPassantIsAllowedRightAfterTheDoubleStep",
            outcome == MoveOutcome.OK && hasPiece(board, "d6", PieceType.PAWN)
                && board.pieceAt(at("d6")).getColor() == Color.WHITE && board.pieceAt(at("d5")) == null,
            "el peón blanco queda en d6 y el negro de d5 desaparece");
    }

    // TDD: la oportunidad dura una sola jugada.
    private static void testEnPassantExpiresAfterOneMove() {
        // Arrange
        Board board = enPassantBoard();
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));
        game.tryMove(at("d7"), at("d5"));
        game.tryMove(at("f1"), at("g1"));   // las blancas no capturan
        game.tryMove(at("e8"), at("f8"));   // las negras mueven el rey

        // Act
        MoveOutcome outcome = game.tryMove(at("e5"), at("d6"));

        // Assert
        assertTrue("testEnPassantExpiresAfterOneMove", outcome == MoveOutcome.INVALID,
            "ya no se puede capturar al paso una jugada después");
    }

    // TDD: deshacer la captura al paso devuelve el peón capturado.
    private static void testEnPassantUndoRestoresTheCapturedPawn() {
        // Arrange
        Board board = enPassantBoard();
        IChessGame game = newGame(board);
        game.tryMove(at("e1"), at("f1"));
        game.tryMove(at("d7"), at("d5"));
        Board afterDoubleStep = board.cloneBoard();
        game.tryMove(at("e5"), at("d6"));

        // Act
        game.undo();

        // Assert
        assertTrue("testEnPassantUndoRestoresTheCapturedPawn", afterDoubleStep.equals(board),
            "al deshacer vuelven el peón blanco a e5 y el negro a d5");
    }
```

```java
        testEnPassantIsAllowedRightAfterTheDoubleStep();
        testEnPassantExpiresAfterOneMove();
        testEnPassantUndoRestoresTheCapturedPawn();
```

- [x] **Step 2: Correr y ver que no compila**

Run: `./scripts/build.sh`
Expected: `cannot find symbol: class EnPassantMove`.

- [x] **Step 3: Crear `EnPassantCommand`**

`src/nucleo/EnPassantCommand.java`:

```java
package nucleo;

import puertos.IMoveListener;

import java.util.Arrays;
import java.util.List;

// Command (Clase 3) de la captura al paso: el peón se mueve en diagonal a una
// casilla vacía y el peón capturado, que está al lado, sale del tablero.
public class EnPassantCommand implements IMoveCommand {
    private final Board board;
    private final Position from;
    private final Position to;
    private final Position capturedPosition;
    private Piece movedPawn;
    private Piece capturedPawn;

    public EnPassantCommand(Board board, Position from, Position to, Position capturedPosition) {
        this.board = board;
        this.from = from;
        this.to = to;
        this.capturedPosition = capturedPosition;
    }

    @Override
    public void execute() {
        this.movedPawn = board.pieceAt(from);
        this.capturedPawn = board.pieceAt(capturedPosition);
        board.removePiece(capturedPosition);
        board.movePiece(from, to);
    }

    @Override
    public void undo() {
        board.movePiece(to, from);
        board.placePiece(capturedPawn, capturedPosition);
    }

    @Override
    public List<Position> touchedSquares() {
        return Arrays.asList(from, to, capturedPosition);
    }

    @Override
    public Move getMove() {
        return new Move(from, to);
    }

    @Override
    public boolean resetsHalfMoveClock() {
        return true;
    }

    @Override
    public void announce(IMoveListener listener) {
        listener.onMoveMade(from, to, movedPawn, capturedPawn);
    }
}
```

- [x] **Step 4: Crear `EnPassantMove`**

`src/nucleo/especiales/EnPassantMove.java`:

```java
package nucleo.especiales;

import nucleo.Board;
import nucleo.Color;
import nucleo.EnPassantCommand;
import nucleo.IGameContext;
import nucleo.IMoveCommand;
import nucleo.ISpecialMove;
import nucleo.Move;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;

import java.util.ArrayList;
import java.util.List;

// Regla de captura al paso: si el peón rival acaba de avanzar dos casillas y
// quedó al lado de mi peón, puedo capturarlo como si hubiera avanzado una.
// Solo vale en la jugada inmediata. Simplificación: la simulación de
// legalidad no quita el peón capturado, así que no detecta el jaque descubierto
// horizontal que a veces vuelve ilegal esta captura.
public class EnPassantMove implements ISpecialMove {

    @Override
    public List<Position> extraDestinations(IGameContext context, Position from) {
        List<Position> destinations = new ArrayList<>();
        Position target = targetSquare(context, from);
        if (target != null) destinations.add(target);
        return destinations;
    }

    @Override
    public IMoveCommand commandFor(IGameContext context, Position from, Position to) {
        Position target = targetSquare(context, from);
        if (target == null || !target.equals(to)) return null;
        Position capturedPosition = new Position(from.getRow(), to.getColumn());
        return new EnPassantCommand(context.getBoard(), from, to, capturedPosition);
    }

    // La casilla a la que el peón de "from" puede capturar al paso, o null.
    private Position targetSquare(IGameContext context, Position from) {
        Board board = context.getBoard();
        Piece pawn = board.pieceAt(from);
        if (pawn == null || !pawn.getType().equals(PieceType.PAWN)) return null;

        Move last = context.lastMove();
        if (last == null) return null;
        Piece moved = board.pieceAt(last.getTo());
        if (moved == null || !moved.getType().equals(PieceType.PAWN) || moved.getColor() == pawn.getColor()) {
            return null;
        }

        boolean doubleStep = Math.abs(last.getTo().getRow() - last.getFrom().getRow()) == 2;
        boolean besideMyPawn = last.getTo().getRow() == from.getRow()
            && Math.abs(last.getTo().getColumn() - from.getColumn()) == 1;
        if (!doubleStep || !besideMyPawn) return null;

        int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
        return new Position(from.getRow() + direction, last.getTo().getColumn());
    }
}
```

- [x] **Step 5: Armarla en los composition roots**

En `Main` y `MainSwing`: importar `nucleo.especiales.EnPassantMove` y sumarla a la lista, entre el enroque y la coronación:

```java
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new EnPassantMove(),
            new PromotionMove(pieces, chooser));
```

(En `MainSwing` el segundo argumento de `PromotionMove` es `view`.)

- [x] **Step 6: Correr los tests**

Run: `./scripts/test.sh`
Expected: `53 pasados, 0 fallados.`

- [x] **Step 7: Jugar una captura al paso por consola**

Run: `printf "e2 e4\na7 a6\ne4 e5\nd7 d5\ne5 d6\nsalir\n" | java -cp out Main | tail -14`
Expected: el peón blanco en d6, d5 vacía y el mensaje `Blancas: Peón e5 -> d6 (captura Peón)`.

- [x] **Step 8: Commit**

```bash
git add -A
git commit -m "captura al paso"
```

---

### Task 4.4: verificación de la fase

- [x] **Step 1: Se puede extender sin tocar clases existentes**

Los tests `testNewPieceWithoutTouchingExistingClasses` y `testBoardOfAnotherSize` ya cubren pieza nueva y tablero. Agregar un último test que pruebe una regla de tablas propia, definida dentro del test, y registrarlo:

```java
    // Una regla de tablas inventada en el test: declara tablas apenas hay dos jugadas hechas.
    private static class AlwaysDrawRule implements IDrawRule {
        @Override
        public String reason(IGameContext context) {
            return context.lastMove() != null ? "regla de prueba" : null;
        }
    }

    private static void testNewDrawRuleWithoutTouchingExistingClasses() {
        // Arrange
        IChessGame game = newGameWithDrawRules(BoardSetup.standard(PIECES), new AlwaysDrawRule());

        // Act
        MoveOutcome outcome = game.tryMove(at("e2"), at("e4"));

        // Assert
        assertTrue("testNewDrawRuleWithoutTouchingExistingClasses", outcome == MoveOutcome.DRAW,
            "una regla de tablas nueva se enchufa por constructor sin editar ChessGame");
    }
```

(con `import nucleo.IGameContext;` y registrando `testNewDrawRuleWithoutTouchingExistingClasses();`).

Run: `./scripts/test.sh`
Expected: `54 pasados, 0 fallados.`

- [x] **Step 2: Nada de nucleo importa hacia afuera**

Run: `grep -rn "import adaptadores\|import ai" src/nucleo | wc -l`
Expected: `0`

- [x] **Step 3: Sin identificadores en español ni raya**

Run: `python3 scripts/find_spanish_identifiers.py && grep -rn $'\xe2\x80\x94' src | wc -l`
Expected: `0 identificadores en español` y `0`.

- [x] **Step 4: Commit del test de extensibilidad, revisión y push**

```bash
git add -A
git commit -m "test de una regla de tablas nueva"
git push
```

Pedir revisión con `superpowers:requesting-code-review` antes de seguir.
