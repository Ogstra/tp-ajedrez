# TP Ajedrez

Ajedrez para dos personas o contra la computadora, en consola o en una ventana, hecho para el TPO de Ingeniería de Software (UADE). La idea del trabajo es que las reglas del juego no dependan de cómo se muestra ni de cómo se juega, así que casi todo el código está pensado para poder cambiarse sin romper el resto.

## Cómo correrlo

Se abre la carpeta en IntelliJ y se corre `Main` (consola), `MainSwing` (ventana) o `ChessCoreTests` (tests). Se necesita JDK 21 o superior. También por terminal con Bash (Git Bash en Windows), desde la raíz del repo:

```bash
./scripts/build.sh
java -cp out Main
java -cp out MainSwing
./scripts/test.sh
```

Al empezar, la consola pregunta el oponente: dos jugadores o la IA en nivel fácil, medio o difícil. Se mueve escribiendo `e2 e4`. Con `undo` se deshace, con `redo` se rehace y con `salir` se termina. La ventana abre directo en la partida: se hace click en la pieza (se marcan a dónde puede ir: en ámbar los destinos libres y en rojizo las capturas) y después en el destino. Arriba están Nueva partida, el desplegable del rival (dos jugadores o la IA en fácil, medio o difícil) y los botones Deshacer y Rehacer, y debajo lo que acaba de pasar. La IA juega con negras; contra ella, deshacer vuelve atrás su respuesta y la jugada propia.

## Pruebas

`./scripts/test.sh` corre las 61 pruebas del juego y de la IA y la regresión de consola (deshacer y repetir un mate). Para comprobar la ventana real (partida, marcas, deshacer, rehacer, nueva partida y un tablero de 10x10):

```bash
java --class-path out scripts/SwingIntegrationTests.java
```

## Qué reglas tiene

Todas las piezas con sus movimientos, turnos, capturas, jaque, jaque mate y ahogado, enroque, coronación (se elige la pieza), tablas por material insuficiente, por la regla de los 50 movimientos y por repetición, y deshacer y rehacer. También se puede jugar contra la IA en tres niveles: fácil (una jugada al azar), medio (captura la pieza de más valor) y difícil (minimax mirando dos jugadas, con material y algo de criterio posicional).

## Cómo está armado

```
src/
  core/          el núcleo: ChessGame, su interfaz de entrada y las reglas de siempre
    board/         tablero, piezas, posiciones y colores
    state/         turnos y finales de partida (State)
    command/       cada jugada como comando para deshacer y rehacer (Command)
    movement/      cómo se mueve cada pieza (Strategy)
    special/       enroque y coronación
    draw/          las condiciones de tablas
  ports/         interfaces con las que el núcleo avisa y pregunta al exterior
  ai/            el oponente: un nivel por clase (Strategy) y quien decide cuándo juega
  adapters/      lo que depende de una tecnología, separado por tecnología
    console/       consola
    swing/         la ventana: tablero y armado de cada partida
  Main.java      arma todo para jugar por consola
  MainSwing.java arma todo para jugar con ventana
  ChessCoreTests.java
```

El núcleo (`core`) no conoce a los adaptadores: son ellos los que dependen de él. Por eso la ventana se pudo agregar sin tocar `ChessGame`. Los paquetes y los identificadores van en inglés, como pide la convención de la cátedra (comentarios en español, identificadores en inglés).

Lo que se ve en la materia está así:

- **Núcleo y adaptadores**: lo que se llama desde afuera es `IChessGame`. Para avisar lo que pasa el núcleo usa `IMoveListener` e `IGameStatusListener`, y para preguntar a qué pieza se corona usa `IPromotionChooser`.
- **Inyección por constructor**: `ChessGame` recibe el tablero, los listeners, las reglas especiales y las reglas de tablas ya armadas. `Main` y `MainSwing` arman las partidas.
- **Composición sobre herencia**: hay una sola clase `Piece`, que tiene una `IMovementStrategy`. La reina combina las estrategias de la torre y el alfil.
- **Strategy**: el movimiento de cada pieza, las reglas especiales (`ISpecialMove`) y las tablas (`IDrawRule`).
- **Factory**: `PieceFactory` es un registro de piezas, `BoardSetup` arma el tablero inicial y `StandardRules` arma las reglas de siempre.
- **Observer**: los dos listeners, que implementan la consola y la ventana.
- **State**: el turno y los estados terminales (mate, ahogado, tablas).
- **Command**: cada jugada es un comando que se puede deshacer y rehacer.
- **IA**: cada nivel implementa `IOpponent` (Strategy) y `Opponents` los crea (Factory). La IA usa el núcleo como cualquier adaptador, a través de `IChessGame`, y piensa sobre una copia de la partida. `AutoPromotionChooser` (Decorator) hace que la IA corone sola sin preguntar.
- **Tests sin interfaz**: `ChessCoreTests`, con un stub y un mock como dobles de prueba, y los marcados `// TDD:` escritos antes del código.

## Agregar cosas sin tocar lo que ya está

- **Una pieza nueva**: se escribe su `IMovementStrategy`, se crea un `PieceType` y se registra con `factory.register(...)`. Hay un test que lo hace con una pieza inventada que se mueve como torre y caballo.
- **Otro tamaño de tablero**: `BoardSetup.custom(10, 10, filaTrasera, piezas)` en el `GameBuilder` de `MainSwing` (o `new Board(10, 10)`). Todo pregunta a `board.isInside()` en vez de asumir 8x8, el peón corona en la última fila de ese tablero y la ventana toma el tamaño del tablero.
- **Una regla especial o de tablas nueva**: una clase que implemente `ISpecialMove` o `IDrawRule` y una línea en `StandardRules`.

## Cosas a tener en cuenta

La repetición de posición ignora los derechos de enroque.

## Piezas

Las imágenes están en `src/assets/piezas/` y son el set Cburnett de Wikimedia Commons (el mismo de lichess.org), licencia CC BY-SA 3.0, autor Colin M.L. Burnett. Si falta alguna, la ventana usa un símbolo de texto.
