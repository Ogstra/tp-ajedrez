# TP Ajedrez

Ajedrez para jugar en consola o en una ventana, hecho para el TPO de Ingeniería de Software (UADE). La idea del trabajo es que las reglas del juego no dependan de cómo se muestra ni de cómo se juega, así que casi todo el código está pensado para poder cambiarse sin romper el resto.

## Cómo correrlo

Se abre la carpeta en IntelliJ y se corre `Main` (consola), `MainSwing` (ventana) o `ChessCoreTests` (tests). También por terminal:

```bash
find src -name "*.java" | xargs javac -d out -encoding UTF-8
java -cp out Main
java -cp out MainSwing
java -cp out ChessCoreTests
```

En la consola se juega escribiendo `e2 e4`. Con `undo` se deshace la última jugada y con `salir` se termina. En la ventana se hace click en la pieza y después en el destino.

## Cómo está armado

```
src/
  nucleo/        las reglas del ajedrez, sin nada de consola ni de Swing
    especiales/    enroque y coronación
    estrategias/   cómo se mueve cada pieza
  puertos/       interfaces con las que el núcleo pide cosas al exterior
  adaptadores/   consola y Swing, que implementan esos puertos
  Main.java      arma todo para jugar por consola
  MainSwing.java arma todo para jugar con ventana
  ChessCoreTests.java
```

El núcleo no conoce a los adaptadores: son los adaptadores los que dependen de él. Por eso la ventana Swing se pudo agregar después sin tocar `ChessGame`.

Lo que se ve en la materia está así:

- **Núcleo y adaptadores**: `ChessGame` es lo que se llama desde afuera. Para avisar lo que pasa usa `IGameObserver`, y para preguntar a qué pieza se corona usa `IPromotionChooser`.
- **Inyección por constructor**: `ChessGame` recibe el tablero, los observers y las reglas especiales ya armadas. El único lugar donde se crean es `Main`/`MainSwing`.
- **Composición sobre herencia**: hay una sola clase `Piece`, que tiene una `IMovementStrategy`. No hay una subclase por pieza, y la reina combina las estrategias de la torre y el alfil.
- **Strategy**: el movimiento de cada pieza, y las reglas especiales (`ISpecialMove`).
- **Factory**: `PieceFactory` crea las piezas y `BoardSetup` arma el tablero inicial.
- **Observer**: `IGameObserver`.
- **State**: `WhiteTurnState` y `BlackTurnState` para el turno.
- **Command**: `MoveCommand`, `CastlingCommand` y `PromotionCommand`, que son los que permiten deshacer.
- **Tests sin interfaz**: `ChessCoreTests`, con un stub y un mock como dobles de prueba.

## Agregar cosas sin tocar lo que ya está

- **Una pieza nueva**: se escribe su `IMovementStrategy`, se crea un `PieceType` y se registra con `factory.register(...)`. Hay un test que lo hace con una pieza inventada que se mueve como torre y caballo.
- **Otro tamaño de tablero**: `new Board(10, 10)` y listo. Todo pregunta a `board.isInside()` en vez de asumir 8x8, y el peón corona en la última fila de ese tablero.
- **Una regla nueva** (por ejemplo la captura al paso): una clase que implemente `ISpecialMove` y una línea en `Main`.

## Enroque y coronación

Son reglas especiales que `ChessGame` recibe por constructor, sin saber cuáles son. Para el enroque, "el rey y la torre no se movieron" se saca del historial de jugadas, así que al deshacer el derecho a enrocar vuelve solo. La coronación le pregunta la pieza al puerto `IPromotionChooser`.

El peón ataca en diagonal aunque la casilla esté vacía, y eso importa para el enroque: el rey no puede pasar por una casilla que un peón ataca.

## Lo que no está

Captura al paso, tablas por repetición o por la regla de los 50 movimientos, y un oponente con IA. No se pidieron como obligatorios, y la IA usa algoritmos que no vimos en la cursada.

## Piezas

Las imágenes están en `src/assets/piezas/` y son el set Cburnett de Wikimedia Commons (el mismo de lichess.org), licencia CC BY-SA 3.0, autor Colin M.L. Burnett. Si falta alguna, la ventana usa un símbolo de texto.
