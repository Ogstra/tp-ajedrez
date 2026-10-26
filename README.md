# TP Ajedrez

Ajedrez para jugar en consola o en una ventana, hecho para el TPO de Ingeniería de Software (UADE). La idea del trabajo es que las reglas del juego no dependan de cómo se muestra ni de cómo se juega, así que casi todo el código está pensado para poder cambiarse sin romper el resto.

<p align="center">
  <img src="docs/captura.png" alt="La ventana, con una partida a medias" width="460">
</p>

## Cómo correrlo

Se abre la carpeta en IntelliJ y se corre `Main` (consola), `MainSwing` (ventana) o `ChessCoreTests` (tests). Se necesita JDK 21 o superior y nada más: no hay librerías externas. Anda igual en Windows, macOS y Linux. También por terminal, desde la raíz del repo (en Windows con Git Bash; en macOS y Linux con la terminal de siempre):

```bash
./scripts/build.sh
java -cp out Main
java -cp out MainSwing
./scripts/test.sh
```

`build.sh` compila con `--release 21` (aunque el JDK instalado sea más nuevo) y copia `src/assets` a `out/assets`: el ícono, las piezas y las skins se leen del classpath, así que el juego se puede correr desde cualquier carpeta (`java -cp /ruta/al/repo/out MainSwing`).

En la consola primero va el login (iniciar sesión, registrarse o invitado) y después contra quién se juega (dos personas o la IA en tres niveles); se mueve escribiendo `e2 e4`. Con `undo` se deshace, con `redo` se rehace y con `salir` se termina. La ventana es una sola y tiene cinco pantallas. Arranca en el login: se escribe el nombre de usuario y se aprieta Entrar, o Entrar como invitado. Registrarse lleva a la pantalla de registro, donde se escribe el nombre y se aprieta Crear usuario (o Volver). En el menú se elige 1 contra 1 o contra la IA y, contra la IA, la dificultad (fácil, medio o difícil) y el color con el que se juega (el tablero se gira si son las negras). Jugar empieza la partida. Skins abre las skins y Cerrar sesión vuelve al login. En la partida se hace click en la pieza (se marcan a dónde puede ir: en ámbar los destinos libres y en rojizo las capturas) y después en el destino. Arriba hay una sola barra con Menú, lo que acaba de pasar, quién juega y los botones Deshacer, Rehacer y Ranking. El ranking también se abre en la misma ventana.

## Usuarios y ranking

Al arrancar (en consola y en ventana) se inicia sesión, se registra un usuario o se entra como invitado. No hay contraseña: se entra solo con el nombre y al registrarse alcanza con escribir el nombre, porque el id lo asigna el sistema solo y no hay que anotarlo. El usuario suma puntos en las partidas contra la IA (la IA nunca tiene usuario). En 1 contra 1 (dos personas en la misma máquina) no se acreditan puntos.

Cuando una partida contra la IA termina se acredita el resultado: 3 puntos por victoria, 1 por empate, 0 por derrota. Si se deshace la jugada final, el resultado se revierte, y si se rehace vuelve a sumarse. El ranking se ordena por puntos, después por victorias y después por nombre. En la consola se ve con el comando `ranking` (y se muestra solo al terminar una partida con usuario) y en la ventana con el botón Ranking.

Los datos quedan en `data/usuarios.csv` (no se sube al repo). Ojo: `data/` es relativo a la carpeta desde la que se corre el juego (a diferencia de los recursos, que salen del classpath); conviene correrlo siempre desde la raíz del repo, o fijar la carpeta. Para usar otra carpeta: `java -Dajedrez.data=otra_carpeta -cp out Main`. Los cambios técnicos están explicados en `docs/cambios-usuarios-y-ranking.md`.

## Verificación

`./scripts/test.sh` corre 66 pruebas del juego y la IA, 17 regresiones de reglas e historial, 20 de usuarios y ranking, 10 de skins y cuatro regresiones de consola (invitado en 1 contra 1 con deshacer y repetir el mate, registro y partida contra la IA, un 1 contra 1 que no suma al ranking, login persistente). Para comprobar la ventana real (login, menú, 1 contra 1 sin ranking, la IA en los tres niveles con blancas y con negras, deshacer, rehacer, ranking, un tablero de 10x10, el ícono, el tablero que crece con la ventana):

```bash
java --class-path out scripts/SwingIntegrationTests.java
```

## Skins

Las skins cambian las piezas y los colores del tablero, y se desbloquean al llegar a un puntaje del ranking (los puntos no se gastan). Vienen Staunton (6 puntos), Medieval (15) y Cubos (30), además de la clásica. Cubos recupera las piezas de la antigua skin Minecraft; la procedencia de las imágenes está en `src/assets/skins/CREDITS.md`. Se eligen desde el menú, en Skins. Para agregar una alcanza con una carpeta con las 12 imágenes y una línea en `src/assets/skins/skins.csv`, sin tocar código; está explicado en `docs/skins.md`.

## Qué reglas tiene

Todas las piezas con sus movimientos, turnos, capturas, jaque, jaque mate y ahogado, enroque, coronación (se elige la pieza), captura al paso, tablas por material insuficiente, por la regla de los 50 movimientos y por repetición, y deshacer y rehacer. El peón corona sin preguntar cuando juega la IA.

## Cómo está armado

Los paquetes están en inglés, como los identificadores, siguiendo la convención de la cátedra (comentarios en español, identificadores en inglés).

```
src/
  core/          el núcleo: las reglas del ajedrez, sin nada de consola, Swing ni archivos
    special/       enroque, captura al paso y coronación
    movement/      cómo se mueve cada pieza
    draw/          las condiciones de tablas
  ports/         interfaces con las que el núcleo avisa y pide cosas al exterior
  adapters/      consola, Swing (pantallas y BoardView) y archivos CSV
  accounts/      cuentas, puntajes y ranking, con su puerto IAccountStore
  ai/            el oponente, afuera del núcleo
  skins/         skins desbloqueables por puntos, con su puerto ISkinStore
  assets/        ícono, piezas y skins (se leen del classpath)
  Main.java      arma todo para jugar por consola
  MainSwing.java arma todo para jugar con ventana
  ChessCoreTests.java
  AccountTests.java
  RulesRegressionTests.java
  SkinTests.java
```

El núcleo no conoce a los adaptadores ni a la IA: son ellos los que dependen de él. Por eso la ventana, la IA y los usuarios se pudieron agregar sin tocar `ChessGame`. La tabla de dependencias entre paquetes está al final de `docs/uml-checklist.md`.

Lo que se ve en la materia está así:

- **Núcleo y adaptadores**: lo que se llama desde afuera es `IChessGame`. Para avisar lo que pasa el núcleo usa `IMoveListener` e `IGameStatusListener`, y para preguntar a qué pieza se corona usa `IPromotionChooser`.
- **Inyección por constructor**: `ChessGame` recibe el tablero, los listeners, las reglas especiales y las reglas de tablas ya armadas. `Main`/`MainSwing` arman las partidas de juego y `SearchGames` crea la copia de búsqueda.
- **Composición sobre herencia**: hay una sola clase `Piece`, que tiene una `IMovementStrategy`. La reina combina las estrategias de la torre y el alfil.
- **Strategy**: el movimiento de cada pieza, las reglas especiales (`ISpecialMove`), las tablas (`IDrawRule`) y los oponentes (`IOpponent`: los tres niveles de la IA).
- **Factory**: `PieceFactory` es un registro de piezas, `BoardSetup` arma el tablero inicial, `Opponents` crea el oponente según el nivel y `GameBuilder` arma cada partida de la ventana.
- **Observer**: los dos listeners. `ScoreRecorder` es un observador más: acredita el resultado sin que el núcleo sepa que existen los usuarios.
- **SRP en la ventana**: `SwingChessView` maneja las pantallas y el turno, `BoardView` dibuja el tablero.
- **State**: el turno y los estados terminales (mate, ahogado, tablas).
- **Command**: cada jugada es un comando que se puede deshacer y rehacer.
- **Tests sin interfaz**: `ChessCoreTests`, con un stub y un mock como dobles de prueba, y los marcados `// TDD:` escritos antes del código.

## Agregar cosas sin tocar lo que ya está

- **Una pieza nueva**: se escribe su `IMovementStrategy`, se crea un `PieceType` y se registra con `factory.register(...)`. Hay un test que lo hace con una pieza inventada que se mueve como torre y caballo.
- **Otro tamaño de tablero**: `BoardSetup.custom(10, 10, filaTrasera, piezas)` en el `GameBuilder` de `MainSwing` (o `new Board(10, 10)`). Todo pregunta a `board.isInside()` en vez de asumir 8x8, el peón corona en la última fila de ese tablero y la ventana se adapta sola: casillas cuadradas, coordenadas hasta donde haga falta y casillas más chicas si no entra en la pantalla.
- **Una regla especial o de tablas nueva**: una clase que implemente `ISpecialMove` o `IDrawRule` y una línea en `core/StandardRules`, que usan la consola, la ventana y la IA.

## Cosas a tener en cuenta

La simulación de captura al paso ejecuta el comando especial sobre un tablero independiente antes de comprobar el jaque. La repetición compara tablero, turno, derechos de enroque y capturas al paso legales. La búsqueda de la IA conserva esos derechos, la última jugada, el contador de medias jugadas y las posiciones anteriores, sin compartir comandos ni listeners con la partida real. Solo puede deshacer las jugadas de la búsqueda. La dificultad sigue siendo una búsqueda de profundidad limitada, no un motor competitivo.

La ventana se puede agrandar o achicar: el tablero ocupa el lugar que queda y las piezas se rehacen a ese tamaño.

## Documentos

En `docs/`:

- `normativa-catedra.md`: lo que pide la cátedra (enunciado y unidades 0 a 6) y dónde se cumple en el TP. Es la guía para revisar el avance.
- `justificacion.md` (y `.docx`): las decisiones de diseño en formato qué / por qué / cuándo se rompería.
- `uml-checklist.md`: la lista de clases y relaciones para el UML, generada del código con `java scripts/ListTypes.java`.
- `skins.md`, `cambios-usuarios-y-ranking.md` y `cambios-tecnicos.md`: cómo funciona cada parte.
- `presentacion.pptx`: la presentación para la exposición.

Franco dibuja el UML con `docs/uml-checklist.md`. Cuando esté `docs/uml.png`, se regenera la presentación con `node scripts/build_deck.js` (hace falta `npm install pptxgenjs` una vez) para que aparezca en la diapositiva 4.

## Piezas

Las imágenes están en `src/assets/piezas/` y son el set Cburnett de Wikimedia Commons (el mismo de lichess.org), licencia CC BY-SA 3.0, autor Colin M.L. Burnett. Si falta alguna, la ventana usa un símbolo de texto.
