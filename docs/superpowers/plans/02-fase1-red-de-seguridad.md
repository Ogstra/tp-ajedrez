# Fase 1: red de seguridad de tests

> **Para el agente:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Pasos con casillas `- [x]`.

**Goal:** cubrir con tests todo el alcance obligatorio del TPO que hoy no tiene test, contra la API actual, para que los renombres y la refactorización de las fases siguientes tengan una red de seguridad.

**Architecture:** solo se agregan métodos `private static void testXxx()` a `src/ChessCoreTests.java`, registrados en `main`. No se toca el código de producción.

**Tech Stack:** Java, runner propio de `ChessCoreTests`.

**Importante:** estos tests describen comportamiento que **ya existe**, así que deberían pasar a la primera. No hay paso "ver que falla". Si alguno falla, hay un bug real: usá `superpowers:systematic-debugging`, encontrá la causa, corregí el código de producción con un commit aparte (`git commit -m "arreglo ..."`) y volvé a correr. Los métodos nuevos ya se nombran en inglés (el resto se renombra en la fase 2).

---

### Task 1.1: movimiento de las piezas que faltan

**Files:**
- Modify: `src/ChessCoreTests.java` (agregar métodos antes de la llave final de la clase, y registrarlos en `main`)

- [x] **Step 1: Agregar estos métodos al final de la clase `ChessCoreTests`**

```java
    // ---------- movimiento de las piezas que faltaban ----------

    private static void testBishopSlidesAndStopsAtOwnPiece() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("c1"));
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e3"));

        // Act
        List<Position> moves = board.pieceAt(at("c1")).possibleMoves(board, at("c1"));

        // Assert
        assertTrue("testBishopSlidesAndStopsAtOwnPiece",
            moves.contains(at("d2")) && moves.contains(at("b2")) && moves.contains(at("a3"))
                && !moves.contains(at("e3")) && !moves.contains(at("f4")),
            "el alfil desliza en diagonal y frena antes de su propio peón en e3");
    }

    private static void testQueenMovesLikeRookAndBishop() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.QUEEN, Color.WHITE), at("d4"));

        // Act
        List<Position> moves = board.pieceAt(at("d4")).possibleMoves(board, at("d4"));

        // Assert
        assertTrue("testQueenMovesLikeRookAndBishop",
            moves.contains(at("d8")) && moves.contains(at("a4")) && moves.contains(at("h8"))
                && moves.contains(at("a1")) && !moves.contains(at("b5")),
            "la reina llega en línea recta y en diagonal, pero no en L");
    }

    private static void testKingMovesOneSquareInAnyDirection() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e4"));

        // Act
        List<Position> moves = board.pieceAt(at("e4")).possibleMoves(board, at("e4"));

        // Assert
        assertTrue("testKingMovesOneSquareInAnyDirection",
            moves.size() == 8 && moves.contains(at("d3")) && moves.contains(at("f5")) && !moves.contains(at("e6")),
            "el rey tiene 8 destinos a una casilla y ninguno a dos");
    }

    private static void testPawnDoubleStepOnlyFromStartAndWithFreePath() {
        // Arrange: tres tableros, el peón blanco en e2 siempre.
        Board free = new Board();
        free.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        Board blockedOnE3 = new Board();
        blockedOnE3.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        blockedOnE3.placePiece(piece(PieceType.PAWN, Color.BLACK), at("e3"));
        Board blockedOnE4 = new Board();
        blockedOnE4.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e2"));
        blockedOnE4.placePiece(piece(PieceType.PAWN, Color.BLACK), at("e4"));

        // Act
        List<Position> onFree = free.pieceAt(at("e2")).possibleMoves(free, at("e2"));
        List<Position> onE3 = blockedOnE3.pieceAt(at("e2")).possibleMoves(blockedOnE3, at("e2"));
        List<Position> onE4 = blockedOnE4.pieceAt(at("e2")).possibleMoves(blockedOnE4, at("e2"));

        // Assert
        assertTrue("testPawnDoubleStepOnlyFromStartAndWithFreePath",
            onFree.contains(at("e3")) && onFree.contains(at("e4"))
                && !onE3.contains(at("e3")) && !onE3.contains(at("e4"))
                && onE4.contains(at("e3")) && !onE4.contains(at("e4")),
            "el doble paso necesita las dos casillas libres");
    }

    private static void testPawnCapturesOnlyDiagonally() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e4"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("d5"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("f5"));
        Board empty = new Board();
        empty.placePiece(piece(PieceType.PAWN, Color.WHITE), at("e4"));

        // Act
        List<Position> withEnemies = board.pieceAt(at("e4")).possibleMoves(board, at("e4"));
        List<Position> withoutEnemies = empty.pieceAt(at("e4")).possibleMoves(empty, at("e4"));

        // Assert
        assertTrue("testPawnCapturesOnlyDiagonally",
            withEnemies.contains(at("d5")) && withEnemies.contains(at("f5")) && withEnemies.contains(at("e5"))
                && !withoutEnemies.contains(at("d5")) && !withoutEnemies.contains(at("f5")),
            "el peón captura en diagonal solo si hay una pieza rival, y avanza recto");
    }
```

- [x] **Step 2: Registrar los cinco tests en `main`**

Agregar estas líneas en `main`, justo antes de `System.out.println("\n" + pasados ...`:

```java
        testBishopSlidesAndStopsAtOwnPiece();
        testQueenMovesLikeRookAndBishop();
        testKingMovesOneSquareInAnyDirection();
        testPawnDoubleStepOnlyFromStartAndWithFreePath();
        testPawnCapturesOnlyDiagonally();
```

- [x] **Step 3: Correr los tests**

Run: `./scripts/test.sh`
Expected: `25 pasados, 0 fallados.`

- [x] **Step 4: Commit**

```bash
git add src/ChessCoreTests.java
git commit -m "tests de alfil, reina, rey y peón"
```

---

### Task 1.2: reglas de la partida

**Files:**
- Modify: `src/ChessCoreTests.java`

- [x] **Step 1: Agregar estos métodos al final de la clase**

```java
    // ---------- reglas de la partida ----------

    private static void testRookCapturesThroughTryMove() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        board.placePiece(piece(PieceType.PAWN, Color.BLACK), at("a5"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("a1"), at("a5"));

        // Assert
        assertTrue("testRookCapturesThroughTryMove",
            outcome == MoveOutcome.OK && hay(board, "a5", PieceType.ROOK)
                && board.pieceAt(at("a5")).getColor() == Color.WHITE,
            "la torre blanca debería quedar en a5 después de capturar el peón");
    }

    private static void testInvalidMoveLeavesBoardUntouched() {
        // Arrange
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("a1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        Board before = board.cloneBoard();
        ChessGame game = newGame(board);

        // Act: la torre no se mueve en diagonal.
        MoveOutcome outcome = game.tryMove(at("a1"), at("b2"));

        // Assert
        assertTrue("testInvalidMoveLeavesBoardUntouched",
            outcome == MoveOutcome.INVALID && before.equals(board),
            "un movimiento inválido no debe cambiar el tablero");
    }

    private static void testPinnedPieceCannotMove() {
        // Arrange: el alfil blanco en e2 está clavado a su rey por la torre negra de e8.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.BISHOP, Color.WHITE), at("e2"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h8"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e2"), at("d3"));

        // Assert
        assertTrue("testPinnedPieceCannotMove", outcome == MoveOutcome.INVALID,
            "mover el alfil dejaría al rey en jaque por la columna e");
    }

    private static void testStalemateEndsTheGame() {
        // Arrange: ahogado clásico. Si la reina blanca va a g3, el rey negro de h1 no tiene casillas y no está en jaque.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("f2"));
        board.placePiece(piece(PieceType.QUEEN, Color.WHITE), at("g5"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h1"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("g5"), at("g3"));

        // Assert
        assertTrue("testStalemateEndsTheGame", outcome == MoveOutcome.STALEMATE && game.isGameOver(),
            "Dg3 deja a las negras sin movimientos y sin jaque: ahogado");
    }

    private static void testFinishedGameRejectsMoves() {
        // Arrange: mate del loco.
        ChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("f2"), at("f3"));
        game.tryMove(at("e7"), at("e5"));
        game.tryMove(at("g2"), at("g4"));
        game.tryMove(at("d8"), at("h4"));

        // Act
        MoveOutcome outcome = game.tryMove(at("e2"), at("e3"));

        // Assert
        assertTrue("testFinishedGameRejectsMoves", outcome == MoveOutcome.INVALID,
            "con la partida terminada no se puede mover");
    }

    private static void testUndoRestoresTheTurn() {
        // Arrange
        ChessGame game = newGame(BoardSetup.standard(PIECES));
        game.tryMove(at("e2"), at("e4"));

        // Act
        boolean undone = game.undo();
        MoveOutcome blackFirst = game.tryMove(at("e7"), at("e5"));

        // Assert
        assertTrue("testUndoRestoresTheTurn",
            undone && game.getTurn() == Color.WHITE && blackFirst == MoveOutcome.INVALID,
            "después de deshacer la jugada blanca, vuelve a mover blancas");
    }

    private static void testInvalidMoveNotifiesObserver() {
        // Arrange
        RecordingObserver observer = new RecordingObserver();
        ChessGame game = newGame(BoardSetup.standard(PIECES), observer);

        // Act
        game.tryMove(at("e7"), at("e5"));

        // Assert
        assertTrue("testInvalidMoveNotifiesObserver", observer.events.equals(Arrays.asList("invalid")),
            "esperaba [invalid] y llegó " + observer.events);
    }
```

- [x] **Step 2: Registrar en `main`** (antes del `System.out.println("\n" + pasados ...`)

```java
        testRookCapturesThroughTryMove();
        testInvalidMoveLeavesBoardUntouched();
        testPinnedPieceCannotMove();
        testStalemateEndsTheGame();
        testFinishedGameRejectsMoves();
        testUndoRestoresTheTurn();
        testInvalidMoveNotifiesObserver();
```

- [x] **Step 3: Correr los tests**

Run: `./scripts/test.sh`
Expected: `32 pasados, 0 fallados.`

- [x] **Step 4: Commit**

```bash
git add src/ChessCoreTests.java
git commit -m "tests de captura, pieza clavada, ahogado y deshacer"
```

---

### Task 1.3: coronación y enroque, los casos que faltaban

**Files:**
- Modify: `src/ChessCoreTests.java`

- [x] **Step 1: Agregar un helper y los tests al final de la clase**

```java
    // ---------- coronación y enroque, casos que faltaban ----------

    // Stub que devuelve siempre la misma pieza, para probar coronaciones distintas de la reina.
    private static class FixedChooser implements IPromotionChooser {
        private final PieceType choice;

        FixedChooser(PieceType choice) {
            this.choice = choice;
        }

        @Override
        public PieceType choose(Color color) {
            return choice;
        }
    }

    private static ChessGame newGameChoosing(Board board, PieceType choice) {
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(),
            new PromotionMove(PIECES, new FixedChooser(choice)));
        return new ChessGame(board, Arrays.<IGameObserver>asList(), specialMoves);
    }

    private static Board boardWithPawnOnSeventh() {
        Board board = new Board();
        board.placePiece(piece(PieceType.PAWN, Color.WHITE), at("a7"));
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("h4"));
        return board;
    }

    private static void testPromotionToKnight() {
        // Arrange
        Board board = boardWithPawnOnSeventh();
        ChessGame game = newGameChoosing(board, PieceType.KNIGHT);

        // Act
        MoveOutcome outcome = game.tryMove(at("a7"), at("a8"));

        // Assert
        assertTrue("testPromotionToKnight",
            outcome == MoveOutcome.OK && hay(board, "a8", PieceType.KNIGHT)
                && board.pieceAt(at("a8")).getColor() == Color.WHITE,
            "en a8 debería haber un caballo blanco");
    }

    private static void testPromotionUndoRestoresThePawn() {
        // Arrange
        Board board = boardWithPawnOnSeventh();
        Board before = board.cloneBoard();
        ChessGame game = newGameChoosing(board, PieceType.ROOK);
        game.tryMove(at("a7"), at("a8"));

        // Act
        game.undo();

        // Assert
        assertTrue("testPromotionUndoRestoresThePawn", before.equals(board),
            "al deshacer la coronación vuelve el peón a a7 y a8 queda vacía");
    }

    private static void testCannotCastleWhileInCheck() {
        // Arrange: la torre negra de e8 da jaque al rey blanco por la columna e.
        Board board = new Board();
        board.placePiece(piece(PieceType.KING, Color.WHITE), at("e1"));
        board.placePiece(piece(PieceType.ROOK, Color.WHITE), at("h1"));
        board.placePiece(piece(PieceType.ROOK, Color.BLACK), at("e8"));
        board.placePiece(piece(PieceType.KING, Color.BLACK), at("a8"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("testCannotCastleWhileInCheck", outcome == MoveOutcome.INVALID,
            "no se puede enrocar estando en jaque");
    }

    private static void testCannotCastleWithPieceBetween() {
        // Arrange: el caballo blanco en g1 estorba el enroque corto.
        Board board = tableroParaEnrocar();
        board.placePiece(piece(PieceType.KNIGHT, Color.WHITE), at("g1"));
        ChessGame game = newGame(board);

        // Act
        MoveOutcome outcome = game.tryMove(at("e1"), at("g1"));

        // Assert
        assertTrue("testCannotCastleWithPieceBetween", outcome == MoveOutcome.INVALID,
            "con una pieza entre el rey y la torre no se puede enrocar");
    }
```

- [x] **Step 2: Registrar en `main`**

```java
        testPromotionToKnight();
        testPromotionUndoRestoresThePawn();
        testCannotCastleWhileInCheck();
        testCannotCastleWithPieceBetween();
```

- [x] **Step 3: Correr los tests**

Run: `./scripts/test.sh`
Expected: `36 pasados, 0 fallados.`

- [x] **Step 4: Commit**

```bash
git add src/ChessCoreTests.java
git commit -m "tests de coronación y enroque que faltaban"
```

---

### Cierre de la fase 1

- [x] `./scripts/test.sh` termina con `36 pasados, 0 fallados.` y código 0.
- [x] Pedir revisión (`superpowers:requesting-code-review`) y `git push`.
