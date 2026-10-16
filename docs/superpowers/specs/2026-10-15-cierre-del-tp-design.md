# Cierre del TP de ajedrez: spec

Fecha: 2026-10-15. Entrega y exposición del TPO: 20/11. Final con defensa individual: 11/12.

## 1. Objetivo

Dejar el TP listo para entregar: cerrar los desvíos de arquitectura que marcó la auditoría, completar los tests, sumar las reglas opcionales (captura al paso, tablas, rehacer), agregar un oponente con IA, y preparar los entregables (documento de justificación, PPTX y la lista de clases para el UML).

Estado de partida: el repo `Ogstra/tp-ajedrez` en `main`, con 20 tests pasando, consola y ventana Swing funcionando, y enroque/coronación como reglas enchufables. Todo lo que ya existe está explicado en `README.md`.

## 2. Qué entra y qué no

Entra:

1. Desvíos de arquitectura: interfaz del puerto de entrada, State coherente, observer segregado, identificadores en inglés.
2. Tests que faltan del alcance obligatorio, con runner propio (sin JUnit).
3. Reglas opcionales: captura al paso, tablas (repetición, 50 movimientos, material insuficiente) y rehacer.
4. Oponente con IA en tres niveles. No se defiende en la exposición: es para jugar mejor, así que no puede ensuciar `nucleo/`.
5. Entregables: documento de justificación (`.md` y `.docx`), PPTX de unas 18 slides, `docs/uml-checklist.md`, README al día.

No entra:

- El diagrama UML. Lo dibuja Franco, una sola vez, al final, con la lista que deje el agente.
- JUnit, Maven, Gradle ni ninguna librería externa.
- Nada que no se haya visto en la materia dentro de `nucleo/`.

## 3. Reglas para el agente

Van tal cual en el `AGENTS.md` de la raíz (lo crea la fase 0):

- Instalar superpowers si no está instalado, antes de empezar (ver sección 4).
- Commits cortos, en español, escritos como los escribiría una persona. Sin trailer `Co-Authored-By`, sin "generated with", sin emojis, sin prefijos tipo `feat:`.
- Sin guiones largos (el carácter raya) en código, comentarios, README ni documentos.
- Comentarios en español, identificadores en inglés. Los textos que ve el usuario quedan en español.
- Nada fuera de la materia en `nucleo/`: sin streams, `Optional`, threads, reflection ni librerías. Los patrones permitidos son Strategy, Observer, Factory, State y Command.
- Cada cambio deja compilando y con `scripts/test.sh` en verde antes de commitear.
- No tocar `docs/superpowers/specs/` salvo para corregir un error de la spec.
- Los documentos de la entrega (justificación, PPTX): formato académico sobrio, Times New Roman o Calibri en negro, sin la estética típica de IA.

## 4. Instalación de superpowers

Si el agente no tiene el plugin, lo instala según su plataforma (fuente: https://github.com/obra/superpowers):

- Claude Code: `/plugin install superpowers@claude-plugins-official`
- Cursor: desde el marketplace de plugins del agente.
- Codex: decirle "Fetch and follow instructions from https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/.codex/INSTALL.md"
- OpenCode: instrucciones en `.opencode/INSTALL.md` del mismo repo.
- Gemini CLI: `gemini extensions install https://github.com/obra/superpowers`

Después verifica la instalación con la sección "Verify Installation" del README oficial. Skills a usar durante el trabajo: `executing-plans` (o `subagent-driven-development`), `test-driven-development`, `verification-before-completion`, `systematic-debugging` y `requesting-code-review` al cerrar cada fase.

## 5. Arquitectura objetivo

### 5.1 Puerto de entrada

```java
public interface IChessGame {
    MoveOutcome tryMove(Position from, Position to);
    boolean undo();
    boolean redo();
    Color getTurn();
    boolean isGameOver();
    Board getBoard();
    List<Move> legalMoves();   // jugadas legales del que tiene el turno
}
```

`Move` es un value object inmutable en `nucleo/` con `from` y `to`, `equals`/`hashCode`. `ChessGame` implementa `IChessGame`; `Main`, `MainSwing`, los adaptadores y la IA dependen solo de la interfaz.

### 5.2 State

`IGameState` pasa a tener `Color currentColor()`, `boolean isOver()` y `IGameState afterMove()`. Estados: `WhiteTurnState`, `BlackTurnState` (alternan) y terminales `CheckmateState`, `StalemateState`, `DrawState` (su `afterMove()` devuelve `this`). El booleano `gameOver` de `ChessGame` desaparece.

`MoveOutcome` suma `DRAW`. El historial guarda, junto a cada comando, el estado anterior y el posterior, así `undo` y `redo` los restauran sin trucos y se puede deshacer una partida terminada.

### 5.3 Observers segregados

`IGameObserver` se reemplaza por dos interfaces chicas en `puertos/`:

- `IMoveListener`: `onMoveMade`, `onCastling`, `onPromotion`, `onUndo`, `onRedo`.
- `IGameStatusListener`: `onInvalidMove`, `onCheck`, `onCheckmate`, `onStalemate`, `onDraw(String reason)`.

`ChessGame` recibe una lista de cada una. `IMoveCommand.announce` pasa a recibir `IMoveListener`.

### 5.4 Constructor de `ChessGame` (inyección nivel 4)

```java
ChessGame(Board board,
          List<IMoveListener> moveListeners,
          List<IGameStatusListener> statusListeners,
          List<ISpecialMove> specialMoves,
          List<IDrawRule> drawRules)
ChessGame(..., Color firstToMove)   // sobrecarga: para partidas de búsqueda de la IA
```

### 5.5 Reglas opcionales

- **Captura al paso**: `EnPassantMove implements ISpecialMove` y `EnPassantCommand`. `IMoveCommand` suma `Move getMove()` e `IGameContext` suma `Move lastMove()` (la última jugada que sigue en pie, o `null`). Simplificación documentada: la simulación de legalidad no quita el peón capturado, así que no detecta el jaque descubierto horizontal por captura al paso.
- **Tablas**: `IDrawRule` con `String reason(IGameContext context)` (devuelve `null` si no hay tablas). Implementaciones: `InsufficientMaterialRule` (rey solo, rey y alfil o caballo contra rey, y alfiles de igual color), `FiftyMoveRule` (100 medias jugadas sin captura ni movimiento de peón) y `ThreefoldRepetitionRule` (misma posición y mismo turno 3 veces; se ignoran derechos de enroque y de captura al paso, y se documenta). `IGameContext` suma `int halfMoveClock()` y `int occurrencesOfCurrentPosition()`; `IMoveCommand` suma `boolean resetsHalfMoveClock()`. `ChessGame` guarda una foto del tablero por jugada para contar repeticiones.
- **Rehacer**: pila de redo en `ChessGame`. `undo` mueve la entrada a la pila, `redo` la reejecuta (la coronación no vuelve a preguntar porque el comando ya tiene la pieza elegida), una jugada nueva la vacía.

### 5.6 IA (paquete `ai/`, fuera de `nucleo/`)

```java
public interface IOpponent { Move chooseMove(IChessGame game); }
```

Tres implementaciones: `RandomOpponent` (fácil), `GreedyOpponent` (medio: prefiere la captura de más valor, desempata al azar) y `MinimaxOpponent(depth)` (difícil, profundidad 2 por defecto, evaluación por material). Juegan sobre una partida de búsqueda propia, armada desde `board.cloneBoard()` con `Color firstToMove` y sin listeners, así nunca tocan la pantalla ni abren diálogos (su `PromotionMove` usa un `FixedPromotionChooser` que devuelve reina). `OpponentController` decide cuándo juega la IA (después de la jugada humana, si es su turno y la partida sigue) y ejecuta la jugada con `IChessGame.tryMove`. En Swing la búsqueda corre fuera del hilo de la interfaz para no congelar la ventana. Limitación conocida: la partida de búsqueda no conoce el historial, así que la IA puede subestimar o sobreestimar el enroque; las reglas reales las aplica siempre la partida verdadera.

Selección del nivel: en la ventana, un desplegable "Oponente" (Ninguno, Fácil, Medio, Difícil; la IA juega con negras). En la consola, un menú al arrancar.

### 5.7 Identificadores en inglés

Todo identificador (clases, métodos, campos, variables, métodos de test) pasa a inglés. Comentarios y textos para el usuario quedan en español. El plan trae la tabla de renombres conocidos y un comando para detectar lo que falte.

## 6. Tests

Siguen con el runner propio. `scripts/test.sh` compila y corre `ChessCoreTests`, que termina con código de salida distinto de cero si algo falla. Cobertura obligatoria adicional: pieza clavada, alfil, reina, rey, doble paso del peón y su bloqueo, captura diagonal del peón, enroque bloqueado y en jaque, coronación a otras piezas y su undo, undo y redo de `ChessGame` (turno y estado), ahogado, y partida terminada que rechaza jugadas. Cada test escrito antes que el código se marca con un comentario `// TDD:` en la línea de arriba. Hay dobles de prueba: stub (`AlwaysQueenChooser`) y mock (`RecordingObserver`).

## 7. Entregables

- `docs/justificacion.md` y `docs/justificacion.docx`: cada decisión propia del grupo en formato Qué / Por qué / Cuándo se rompería.
- `docs/presentacion.pptx`: unas 18 slides, sobrias, con captura de la ventana y fragmentos de código cortos.
- `docs/uml-checklist.md`: lista exacta de clases, interfaces, relaciones y paquetes, para que Franco dibuje el UML y coincida con el código.
- `README.md` actualizado.

## 8. Orden de las fases

0. Base del agente: `AGENTS.md`, `scripts/build.sh`, `scripts/test.sh`, código de salida en los tests.
1. Red de seguridad: tests que faltan, contra la API actual (todo debe pasar sin tocar el código de producción, salvo que aparezca un bug).
2. Renombres a inglés (un commit propio, antes de cambiar nada más).
3. Arquitectura: `IChessGame` y `Move`, observers segregados, State con terminales.
4. Reglas opcionales, en TDD: captura al paso, tablas, rehacer.
5. IA y su integración en consola y Swing.
6. Entregables y cierre: documento, PPTX, lista del UML, README, push.

Cambio respecto del diseño conversado: los tests van antes que la arquitectura. Así los 20 tests actuales más los nuevos funcionan como red de seguridad durante los renombres y la refactorización.

## 9. Riesgos

- Los renombres tocan casi todos los archivos: por eso van solos en un commit y con los tests en verde antes y después.
- Repetición y cincuenta movimientos dependen del historial: hay que mantener coherentes `undo`, `redo` y la foto de posiciones.
- La IA con minimax puede ser lenta con profundidad 3: dejar 2 por defecto y medir antes de subir.
- Cambiar el constructor de `ChessGame` rompe a todos sus usuarios (`Main`, `MainSwing`, tests, IA): se actualizan en la misma tarea.

## 10. Criterios de aceptación

- `scripts/test.sh` termina en verde, con tests nuevos para cada regla y para el alcance obligatorio.
- La consola y la ventana juegan una partida completa, con enroque, coronación, captura al paso, rehacer, tablas y los tres niveles de IA.
- `grep` no encuentra identificadores en español (comando en el plan).
- `nucleo/` no importa nada de `adaptadores/` ni de `ai/`.
- Se puede agregar una pieza nueva, una regla especial nueva, una regla de tablas nueva y un tablero de otro tamaño sin editar clases existentes (hay un test para cada una).
- Los commits no tienen trailers de coautor y ningún archivo tiene guiones largos.
- Los cuatro entregables existen y están commiteados.
