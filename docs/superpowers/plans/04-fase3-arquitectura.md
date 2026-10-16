# Fase 3: desvíos de arquitectura

> **Para el agente:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Pasos con casillas `- [ ]`.

**Goal:** cerrar los desvíos de la auditoría: `Move` como value object, observers segregados (ISP), State con estados terminales (sin el booleano `gameOver`) e `IChessGame` como interfaz del puerto de entrada.

**Architecture:** cuatro tareas, cada una deja todo compilando y con los tests en verde. `nucleo/` sigue sin importar `adaptadores/`. Los adaptadores pasan a depender de `IChessGame` y de dos interfaces chicas en `puertos/`.

**Tech Stack:** Java, runner propio `ChessCoreTests`.

**Antes de empezar:** la fase 2 tiene que estar terminada (identificadores en inglés). Los nombres de este plan ya son los finales.

---

### Task 3.1: `Move` y `getMove()` en los comandos

**Files:**
- Create: `src/nucleo/Move.java`
- Modify: `src/nucleo/IMoveCommand.java`, `src/nucleo/MoveCommand.java`, `src/nucleo/PromotionCommand.java`, `src/nucleo/CastlingCommand.java`
- Test: `src/ChessCoreTests.java`

- [ ] **Step 1: Escribir los tests que fallan**

Agregar a `ChessCoreTests` (antes de la llave final de la clase) y registrarlos en `main`:

```java
    // TDD: Move tiene que ser un value object (igualdad por contenido).
    private static void testMoveValueEquality() {
        // Arrange
        Move a = new Move(at("e2"), at("e4"));
        Move b = new Move(at("e2"), at("e4"));
        Move c = new Move(at("e2"), at("e3"));

        // Act / Assert
        assertTrue("testMoveValueEquality", a.equals(b) && a.hashCode() == b.hashCode() && !a.equals(c),
            "dos Move con las mismas casillas son iguales");
    }

    // TDD: cada comando sabe decir qué jugada representa.
    private static void testCommandsExposeTheirMove() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        MoveCommand simple = new MoveCommand(board, at("e2"), at("e4"));
        CastlingCommand castling = new CastlingCommand(board, at("e1"), at("g1"), at("h1"), at("f1"));

        // Act / Assert
        assertTrue("testCommandsExposeTheirMove",
            simple.getMove().equals(new Move(at("e2"), at("e4")))
                && castling.getMove().equals(new Move(at("e1"), at("g1"))),
            "el enroque se representa con el movimiento del rey");
    }
```

```java
        testMoveValueEquality();
        testCommandsExposeTheirMove();
```

(Agregar también `import nucleo.CastlingCommand;` y `import nucleo.Move;` al principio del archivo.)

- [ ] **Step 2: Correr y ver que no compila**

Run: `./scripts/build.sh`
Expected: error de compilación `cannot find symbol: class Move`.

- [ ] **Step 3: Crear `src/nucleo/Move.java`**

```java
package nucleo;

import java.util.Objects;

// Value object inmutable: una jugada es "de esta casilla a esta otra".
// Igualdad por contenido, igual que Position.
public final class Move {
    private final Position from;
    private final Position to;

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public Position getFrom() { return from; }
    public Position getTo() { return to; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move move = (Move) o;
        return from.equals(move.from) && to.equals(move.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + "->" + to;
    }
}
```

- [ ] **Step 4: Agregar `getMove()` a `IMoveCommand`**

En `src/nucleo/IMoveCommand.java`, agregar dentro de la interfaz:

```java
    // La jugada que representa (en el enroque, el movimiento del rey).
    Move getMove();
```

- [ ] **Step 5: Implementarlo en los tres comandos**

En `MoveCommand` y `PromotionCommand`:

```java
    @Override
    public Move getMove() {
        return new Move(from, to);
    }
```

En `CastlingCommand`:

```java
    @Override
    public Move getMove() {
        return new Move(kingFrom, kingTo);
    }
```

- [ ] **Step 6: Correr los tests**

Run: `./scripts/test.sh`
Expected: `38 pasados, 0 fallados.`

- [ ] **Step 7: Commit**

```bash
git add src/nucleo/Move.java src/nucleo/IMoveCommand.java src/nucleo/MoveCommand.java src/nucleo/PromotionCommand.java src/nucleo/CastlingCommand.java src/ChessCoreTests.java
git commit -m "Move como value object y getMove en los comandos"
```

---

### Task 3.2: observers segregados y State con estados terminales

Es el cambio más grande de la fase. Se hace junto porque `ChessGame` es el que usa las dos cosas.

**Files:**
- Create: `src/puertos/IMoveListener.java`, `src/puertos/IGameStatusListener.java`
- Create: `src/nucleo/CheckmateState.java`, `src/nucleo/StalemateState.java`, `src/nucleo/HistoryEntry.java`
- Delete: `src/puertos/IGameObserver.java`
- Modify: `src/nucleo/IGameState.java`, `WhiteTurnState.java`, `BlackTurnState.java`, `IMoveCommand.java`, `MoveCommand.java`, `PromotionCommand.java`, `CastlingCommand.java`, `ChessGame.java`
- Modify: `src/adaptadores/ConsoleRenderer.java`, `src/adaptadores/SwingChessView.java`, `src/Main.java`, `src/MainSwing.java`, `src/ChessCoreTests.java`

- [ ] **Step 1: Escribir el test que falla**

Hoy `undo()` no deja deshacer una partida terminada. Con estados terminales sí. Agregar y registrar:

```java
    // TDD: se puede deshacer la jugada que terminó la partida y seguir jugando.
    private static void testUndoAfterCheckmateReopensTheGame() {
        // Arrange: mate del loco.
        ChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));
        game.tryMove(at("d8"), at("h4"));

        // Act
        boolean undone = game.undo();
        MoveOutcome again = game.tryMove(at("d8"), at("h4"));

        // Assert
        assertTrue("testUndoAfterCheckmateReopensTheGame",
            undone && again == MoveOutcome.CHECKMATE && game.isGameOver(),
            "al deshacer el mate la partida sigue; al repetir la jugada vuelve a terminar");
    }
```

```java
        testUndoAfterCheckmateReopensTheGame();
```

- [ ] **Step 2: Correr y ver que falla**

Run: `./scripts/test.sh`
Expected: `[FAIL] testUndoAfterCheckmateReopensTheGame` (hoy `undo()` devuelve `false` con la partida terminada).

- [ ] **Step 3: Crear las dos interfaces de listeners**

`src/puertos/IMoveListener.java`:

```java
package puertos;

import nucleo.Color;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;

// Puerto de salida (Clase 2), parte 1: lo que pasa con las piezas.
// Interface Segregation (Clase 3): un adaptador que solo quiere saber de
// movimientos no está obligado a implementar el estado de la partida.
public interface IMoveListener {
    void onMoveMade(Position from, Position to, Piece piece, Piece captured);
    void onCastling(Color color, boolean kingside);
    void onPromotion(Color color, Position position, PieceType newPiece);
    void onUndo();
}
```

`src/puertos/IGameStatusListener.java`:

```java
package puertos;

import nucleo.Color;

// Puerto de salida (Clase 2), parte 2: el estado de la partida.
public interface IGameStatusListener {
    void onInvalidMove(String reason);
    void onCheck(Color colorInCheck);
    void onCheckmate(Color winner);
    void onStalemate();
}
```

- [ ] **Step 4: Borrar `IGameObserver`**

Run: `git rm src/puertos/IGameObserver.java`

- [ ] **Step 5: Reescribir los estados**

`src/nucleo/IGameState.java`:

```java
package nucleo;

// State (Clase 3): cada estado sabe de quién es el turno, si la partida
// terminó y a qué estado pasa después de una jugada. Los estados terminales
// (mate, ahogado) devuelven this: de ahí no se sale jugando, solo deshaciendo.
public interface IGameState {
    Color currentColor();
    boolean isOver();
    IGameState afterMove();
}
```

`src/nucleo/WhiteTurnState.java`:

```java
package nucleo;

public class WhiteTurnState implements IGameState {
    @Override
    public Color currentColor() { return Color.WHITE; }

    @Override
    public boolean isOver() { return false; }

    @Override
    public IGameState afterMove() { return new BlackTurnState(); }
}
```

`src/nucleo/BlackTurnState.java`:

```java
package nucleo;

public class BlackTurnState implements IGameState {
    @Override
    public Color currentColor() { return Color.BLACK; }

    @Override
    public boolean isOver() { return false; }

    @Override
    public IGameState afterMove() { return new WhiteTurnState(); }
}
```

`src/nucleo/CheckmateState.java`:

```java
package nucleo;

// Jaque mate: currentColor es el bando al que le tocaba mover y no puede.
public class CheckmateState implements IGameState {
    private final Color mated;

    public CheckmateState(Color mated) {
        this.mated = mated;
    }

    @Override
    public Color currentColor() { return mated; }

    @Override
    public boolean isOver() { return true; }

    @Override
    public IGameState afterMove() { return this; }
}
```

`src/nucleo/StalemateState.java`:

```java
package nucleo;

// Ahogado: el bando que tiene el turno no está en jaque pero no tiene jugadas.
public class StalemateState implements IGameState {
    private final Color toMove;

    public StalemateState(Color toMove) {
        this.toMove = toMove;
    }

    @Override
    public Color currentColor() { return toMove; }

    @Override
    public boolean isOver() { return true; }

    @Override
    public IGameState afterMove() { return this; }
}
```

- [ ] **Step 6: Crear `HistoryEntry`**

`src/nucleo/HistoryEntry.java`:

```java
package nucleo;

// Una jugada del historial junto con el estado de la partida antes y después.
// Así deshacer es volver a stateBefore, sin trucos de alternar turnos, y
// funciona también cuando la jugada terminó la partida.
final class HistoryEntry {
    final IMoveCommand command;
    final IGameState stateBefore;
    IGameState stateAfter;

    HistoryEntry(IMoveCommand command, IGameState stateBefore) {
        this.command = command;
        this.stateBefore = stateBefore;
    }
}
```

- [ ] **Step 7: Cambiar `announce` en `IMoveCommand` y en los tres comandos**

En `IMoveCommand`, cambiar el import `puertos.IGameObserver` por `puertos.IMoveListener` y la firma:

```java
    void announce(IMoveListener listener);
```

En `MoveCommand`:

```java
    @Override
    public void announce(IMoveListener listener) {
        listener.onMoveMade(from, to, movedPiece, capturedPiece);
    }
```

En `PromotionCommand`:

```java
    @Override
    public void announce(IMoveListener listener) {
        listener.onMoveMade(from, to, originalPawn, capturedPiece);
        listener.onPromotion(promotedPiece.getColor(), to, promotedPiece.getType());
    }
```

En `CastlingCommand`:

```java
    @Override
    public void announce(IMoveListener listener) {
        boolean kingside = kingTo.getColumn() > kingFrom.getColumn();
        listener.onCastling(color, kingside);
    }
```

En los tres archivos, reemplazar `import puertos.IGameObserver;` por `import puertos.IMoveListener;`.

- [ ] **Step 8: Reemplazar `src/nucleo/ChessGame.java` por esta versión completa**

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
// los listeners (cómo se avisa lo que pasa) y las reglas especiales
// (enroque, coronación, lo que se sume). ChessGame no sabe cuáles son.
public class ChessGame implements IGameContext {
    private final Board board;
    private final List<IMoveListener> moveListeners;
    private final List<IGameStatusListener> statusListeners;
    private final List<ISpecialMove> specialMoves;
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private IGameState state;

    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves) {
        this(board, moveListeners, statusListeners, specialMoves, Color.WHITE);
    }

    // Con firstToMove se arma una partida que arranca con el turno de las negras
    // (la usa la IA para buscar jugadas desde una posición a mitad de partida).
    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves,
                     Color firstToMove) {
        this.board = board;
        this.moveListeners = moveListeners;
        this.statusListeners = statusListeners;
        this.specialMoves = specialMoves;
        this.state = firstToMove == Color.WHITE ? new WhiteTurnState() : new BlackTurnState();
    }

    @Override
    public Board getBoard() { return board; }
    public Color getTurn() { return state.currentColor(); }
    public boolean isGameOver() { return state.isOver(); }

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

        IMoveCommand command = createCommand(from, to);
        command.execute();
        HistoryEntry entry = new HistoryEntry(command, state);
        history.push(entry);

        Color mover = state.currentColor();
        Color opponent = mover.opposite();
        state = state.afterMove();

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
        } else if (check) {
            for (IGameStatusListener listener : statusListeners) listener.onCheck(opponent);
            outcome = MoveOutcome.CHECK;
        } else {
            outcome = MoveOutcome.OK;
        }
        entry.stateAfter = state;
        return outcome;
    }

    public boolean undo() {
        if (history.isEmpty()) return false;
        HistoryEntry entry = history.pop();
        entry.command.undo();
        state = entry.stateBefore;
        for (IMoveListener listener : moveListeners) listener.onUndo();
        return true;
    }

    // IGameContext: lo que las reglas especiales pueden preguntar.
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

- [ ] **Step 9: Adaptar `ConsoleRenderer` y `SwingChessView`**

En ambos archivos: reemplazar `import puertos.IGameObserver;` por

```java
import puertos.IGameStatusListener;
import puertos.IMoveListener;
```

En `ConsoleRenderer` la declaración de clase queda:

```java
public class ConsoleRenderer implements IMoveListener, IGameStatusListener {
```

En `SwingChessView` queda:

```java
public class SwingChessView extends JFrame implements IMoveListener, IGameStatusListener, IPromotionChooser {
```

Los métodos con `@Override` no cambian de firma.

- [ ] **Step 10: Adaptar `Main` y `MainSwing`**

En los dos archivos: reemplazar `import puertos.IGameObserver;` por los imports de `IGameStatusListener` e `IMoveListener`. La construcción de `ChessGame` queda así.

En `Main` (el listener es `renderer`):

```java
        ChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(renderer),
            Collections.<IGameStatusListener>singletonList(renderer),
            specialMoves);
```

En `MainSwing` (el listener es `view`):

```java
            ChessGame game = new ChessGame(board,
                Collections.<IMoveListener>singletonList(view),
                Collections.<IGameStatusListener>singletonList(view),
                specialMoves);
```

- [ ] **Step 11: Adaptar los tests**

En `ChessCoreTests`: reemplazar `import puertos.IGameObserver;` por los imports de `IGameStatusListener` e `IMoveListener`, y agregar `import java.util.ArrayList;` si no está. Reemplazar `RecordingObserver`, `newGame` y `newGameChoosing` por estas versiones (borrar las viejas):

```java
    // Mock (Clase 4): registra qué le avisaron, para verificarlo en el Assert.
    private static class RecordingObserver implements IMoveListener, IGameStatusListener {
        final List<String> events = new ArrayList<>();

        @Override public void onMoveMade(Position from, Position to, Piece piece, Piece captured) { events.add("move"); }
        @Override public void onCastling(Color color, boolean kingside) { events.add("castling"); }
        @Override public void onPromotion(Color color, Position position, PieceType newPiece) { events.add("promotion"); }
        @Override public void onUndo() { events.add("undo"); }
        @Override public void onInvalidMove(String reason) { events.add("invalid"); }
        @Override public void onCheck(Color colorInCheck) { events.add("check"); }
        @Override public void onCheckmate(Color winner) { events.add("checkmate"); }
        @Override public void onStalemate() { events.add("stalemate"); }
    }

    // El armado que hace Main, sin consola. Un solo lugar para armar partidas de prueba.
    private static ChessGame buildGame(Board board, List<IMoveListener> moveListeners,
                                       List<IGameStatusListener> statusListeners, IPromotionChooser chooser) {
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(PIECES, chooser));
        return new ChessGame(board, moveListeners, statusListeners, specialMoves);
    }

    private static ChessGame newGame(Board board) {
        return buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(),
            new AlwaysQueenChooser());
    }

    private static ChessGame newGame(Board board, RecordingObserver observer) {
        return buildGame(board, Collections.<IMoveListener>singletonList(observer),
            Collections.<IGameStatusListener>singletonList(observer), new AlwaysQueenChooser());
    }

    private static ChessGame newGameChoosing(Board board, PieceType choice) {
        return buildGame(board, new ArrayList<IMoveListener>(), new ArrayList<IGameStatusListener>(),
            new FixedChooser(choice));
    }
```

(Agregar `import java.util.Collections;` si no está.)

- [ ] **Step 12: Compilar y correr los tests**

Run: `./scripts/test.sh`
Expected: `39 pasados, 0 fallados.` (incluye `testUndoAfterCheckmateReopensTheGame`).

- [ ] **Step 13: Verificar que no quedó nada del diseño viejo**

Run: `grep -rn "IGameObserver\|gameOver\|handleMove" src | wc -l`
Expected: `0`

- [ ] **Step 14: Jugar un rato para confirmar que consola y ventana siguen andando**

Run: `printf "e2 e4\ne7 e5\nundo\nsalir\n" | java -cp out Main | tail -15`
Expected: el tablero con el peón blanco en e4 y después sin él (por el `undo`), mensaje "Se deshizo el último movimiento." y "Partida abandonada.".

Run: `java -cp out MainSwing` y cerrarla a mano: tiene que abrir la ventana sin excepciones.

- [ ] **Step 15: Commit**

```bash
git add -A
git commit -m "observers separados en dos interfaces y estados terminales"
```

---

### Task 3.3: `IChessGame`, el puerto de entrada

**Files:**
- Create: `src/nucleo/IChessGame.java`
- Modify: `src/nucleo/ChessGame.java`, `src/adaptadores/SwingChessView.java`, `src/Main.java`, `src/MainSwing.java`
- Test: `src/ChessCoreTests.java`

- [ ] **Step 1: Escribir el test que falla**

```java
    // TDD: el puerto de entrada ofrece todas las jugadas legales del que tiene el turno.
    private static void testLegalMovesAtStartIsTwenty() {
        // Arrange
        IChessGame game = newGame(BoardSetup.standard(PIECES));

        // Act
        List<Move> moves = game.legalMoves();

        // Assert
        assertTrue("testLegalMovesAtStartIsTwenty",
            moves.size() == 20 && moves.contains(new Move(at("e2"), at("e4")))
                && moves.contains(new Move(at("g1"), at("f3"))),
            "al empezar hay 20 jugadas legales (16 de peón y 4 de caballo)");
    }
```

```java
        testLegalMovesAtStartIsTwenty();
```

(Agregar `import nucleo.IChessGame;`.)

- [ ] **Step 2: Correr y ver que no compila**

Run: `./scripts/build.sh`
Expected: error `cannot find symbol: class IChessGame`.

- [ ] **Step 3: Crear `src/nucleo/IChessGame.java`**

```java
package nucleo;

import java.util.List;

// Puerto de entrada (Clase 2): lo único que el mundo exterior (consola,
// ventana, IA) necesita saber de una partida. Los adaptadores dependen de
// esta interfaz, no de ChessGame (DIP, Clase 3).
public interface IChessGame {
    MoveOutcome tryMove(Position from, Position to);
    boolean undo();
    Color getTurn();
    boolean isGameOver();
    Board getBoard();

    // Todas las jugadas legales del bando que tiene el turno (vacía si la partida terminó).
    List<Move> legalMoves();
}
```

- [ ] **Step 4: Hacer que `ChessGame` la implemente**

En `src/nucleo/ChessGame.java`, cambiar la declaración:

```java
public class ChessGame implements IChessGame, IGameContext {
```

Agregar `@Override` a `getTurn`, `isGameOver`, `tryMove` y `undo`, y agregar este método (por ejemplo después de `undo`):

```java
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
```

- [ ] **Step 5: Que los adaptadores usen la interfaz**

En `SwingChessView`: el campo y el método quedan así (y se borra cualquier import de `nucleo.ChessGame`; se agrega `import nucleo.IChessGame;`):

```java
    private IChessGame game;

    public void attachGame(IChessGame game) {
        this.game = game;
    }
```

En `Main` y `MainSwing`: la variable se declara con la interfaz (agregar `import nucleo.IChessGame;`):

```java
        IChessGame game = new ChessGame(board,
```

(en `MainSwing` con la misma sangría que ya tiene la línea.)

- [ ] **Step 6: Correr los tests**

Run: `./scripts/test.sh`
Expected: `40 pasados, 0 fallados.`

- [ ] **Step 7: Verificar que los adaptadores ya no conocen `ChessGame`**

Run: `grep -rn "ChessGame" src/adaptadores | wc -l`
Expected: `0`

Run: `grep -n "ChessGame" src/Main.java src/MainSwing.java`
Expected: solo la línea del import y la línea `new ChessGame(`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "IChessGame como puerto de entrada y legalMoves"
```

---

### Task 3.4: verificación de la fase

- [ ] **Step 1: `nucleo/` sigue sin conocer a los adaptadores**

Run: `grep -rn "import adaptadores\|import ai" src/nucleo | wc -l`
Expected: `0`

- [ ] **Step 2: Todo en verde y el buscador de español limpio**

Run: `./scripts/test.sh && python3 scripts/find_spanish_identifiers.py`
Expected: `40 pasados, 0 fallados.` y `0 identificadores en español`.

- [ ] **Step 3: Pedir revisión (`superpowers:requesting-code-review`), corregir lo que marque y `git push`.**
