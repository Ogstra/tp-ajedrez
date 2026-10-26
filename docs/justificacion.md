# Justificación de decisiones de diseño

TPO de Ingeniería de Software (UADE): implementación de un ajedrez.

Cada decisión está explicada en tres partes: qué se decidió, por qué, y en qué condiciones se rompería esa decisión (cuándo dejaría de convenir). Al final se listan los patrones que no se usaron y por qué.

## 1. El núcleo no conoce a los adaptadores

**Qué.** Las reglas del ajedrez viven en el paquete `core`, que no importa nada de `adapters`, `network`, `accounts`, `skins` ni `ai`. La consola, la ventana, la red, los usuarios y la IA son piezas externas que dependen del núcleo, nunca al revés.

**Por qué.** La prueba es una sola pregunta: si cambia la tecnología, ¿esto cambia? Las reglas no cambian si pasamos de consola a ventana, por eso son núcleo. Gracias a esto la ventana Swing se agregó después sin tocar `ChessGame`, y el núcleo se prueba sin levantar ninguna infraestructura.

**Cuándo se rompería.** Si el TP fuera solo la consola, sin ventana, IA ni red, `ChessGame` podría imprimir directo y los puertos serían de más. La frontera pagó cuando agregamos la ventana, la IA, los usuarios y la LAN sin tocar `ChessGame`.

## 2. Un puerto de entrada y puertos de salida chicos

**Qué.** El mundo exterior le habla al núcleo por una sola interfaz, `IChessGame`. El núcleo le habla al exterior por tres puertos de salida: `IMoveListener` (lo que pasa con las piezas), `IGameStatusListener` (el estado de la partida) e `IPromotionChooser` (a qué pieza se corona).

**Por qué.** Hay un solo puerto de entrada aunque haya dos adaptadores de entrada (teclado y mouse) y la IA; y un puerto de salida por cada cosa distinta que el núcleo necesita del exterior. El observer original tenía ocho métodos mezclados: se partió en dos para que quien solo quiere saber de movimientos no tenga que implementar el estado de la partida (Interface Segregation).

**Cuándo se rompería.** Si ningún adaptador necesitara solo una de las dos mitades, volver a una sola interfaz sería más simple. Mientras haya clientes que usan una sola parte, separar conviene.

## 3. Inyección de dependencias por constructor

**Qué.** `ChessGame` recibe el tablero, los listeners, las reglas especiales y las reglas de tablas ya armadas, por constructor. `Main` y `MainSwing` crean y conectan las partidas de juego; `SearchGames` arma la copia de búsqueda y los tests arman sus propios colaboradores. En la ventana, cada partida la arma un `GameBuilder` que implementa `MainSwing`, con dos métodos: `local` (1 contra 1 o contra la IA) y `overLan` (contra otra PC). Devuelve el juego, el que acredita los puntos y, si corresponde, la IA o el rival por LAN. `GameBuilder` recibe un `IGameView` (lo que la partida necesita de una pantalla), no la ventana concreta. La vista no crea ninguno de esos colaboradores con `new`.

**Por qué.** Es el nivel 4 de inyección. Permite probar el núcleo con dobles de prueba (un stub que siempre corona a reina, un mock que registra los avisos) y cambiar el comportamiento sin editar `ChessGame`. El tablero y las cuatro listas son colaboradores distintos; el constructor deja esas dependencias explícitas.

**Cuándo se rompería.** Si el constructor creciera con parámetros que son configuración y no colaboradores, convendría agruparlos en un objeto de configuración. Y para algo tan estable como un `String` no hace falta abstraer.

## 4. Una sola clase `Piece` que compone una estrategia de movimiento

**Qué.** No hay una subclase por pieza. `Piece` tiene un tipo, un color y una `IMovementStrategy`. Cada pieza tiene su estrategia, y la reina compone las de la torre y el alfil.

**Por qué.** Con herencia, una pieza nueva era una rama nueva del árbol y la reina tendría que heredar de dos clases. Con composición, una pieza nueva es una estrategia nueva y la reina reutiliza lo que ya existe sin heredar.

**Cuándo se rompería.** Si las piezas compartieran mucho estado propio (puntos de vida, historial propio), una jerarquía corta y estable podría ser más clara. Acá una pieza es un valor inmutable con un comportamiento.

## 5. `PieceType` es una clase de valor y `PieceFactory` es un registro

**Qué.** El tipo de pieza dejó de ser un enum: es una clase de valor con igualdad por nombre. `PieceFactory` es un registro `tipo -> estrategia` al que se le suman piezas con `register()`.

**Por qué.** Un enum es un conjunto cerrado: agregar una pieza obligaba a editar el enum y un `switch`, o sea a modificar clases existentes. Con esta decisión una pieza nueva (hay un test con un "Chancellor" que se mueve como torre y caballo) se agrega sin tocar nada.

**Cuándo se rompería.** Si el juego tuviera un conjunto fijo y cerrado de piezas para siempre, un enum sería más simple y el compilador avisaría si falta un caso. Se eligió abierto porque la consigna evalúa justamente la escalabilidad.

## 6. El tamaño del tablero es un dato

**Qué.** `Board(rows, columns)` conoce su tamaño y todos preguntan `board.isInside()`. `BoardSetup.custom` arma la posición inicial con al menos cuatro filas y una pieza trasera por columna. El peón blanco arranca en la segunda fila y el negro en la anteúltima; cada uno corona en el extremo opuesto.

**Por qué.** Antes el 8 estaba repartido por varias clases. Con el tamaño como dato, cambiar de tablero es una llamada distinta (hay tests con 10x10 y 5x5) y no un cambio de código.

**Cuándo se rompería.** Si el tablero fuera siempre 8x8 y nunca se pidiera otro, una constante alcanzaría. Se prefirió el dato porque la defensa individual puede pedir otro tamaño.

## 7. Command con historial para deshacer y rehacer

**Qué.** Cada jugada es un comando con `execute` y `undo`: `MoveCommand`, `CastlingCommand`, `PromotionCommand` y `EnPassantCommand`. `ChessGame` guarda un historial y una pila de rehacer.

**Por qué.** El comando guarda lo necesario para revertir la jugada (la pieza capturada, la torre del enroque, el peón de la coronación). Deshacer y rehacer salen del historial sin código especial por tipo de jugada. También sirve para saber qué casillas se tocaron.

**Cuándo se rompería.** Si no hiciera falta deshacer, llamar al tablero directamente sería más simple y el patrón sería ceremonia de más.

## 8. Las jugadas especiales son reglas enchufables

**Qué.** El enroque, la captura al paso y la coronación implementan `ISpecialMove`. `ChessGame` las recibe por constructor y no sabe cuáles son: cada una declara qué destinos extra habilita y qué comando arma.

**Por qué.** Antes estaban como `if` dentro de `ChessGame`, con un `instanceof`. Agregar una regla nueva obligaba a modificarlo. Ahora es una clase nueva y una línea en `StandardRules`, que arma las reglas de siempre para la consola, la ventana y la búsqueda de la IA (Open/Closed).

**Cuándo se rompería.** Con una sola excepción a las reglas, un `if` sería más directo. Con tres y la posibilidad de más, la estrategia compensa.

## 9. State con estados terminales

**Qué.** El turno es un estado (`WhiteTurnState`, `BlackTurnState`) y el fin de la partida también (`CheckmateState`, `StalemateState`, `DrawState`). Cada estado dice de quién es el turno y si la partida terminó. Las transiciones a mate, ahogado o tablas no las deciden los estados: las decide `ChessGame` después de cada jugada, porque es el único que conoce el tablero y las reglas.

**Por qué.** No hay un booleano `gameOver` suelto: el historial guarda el estado de antes y el de después de cada jugada, y deshacer o rehacer vuelve a ese estado sin recalcular nada.

**Cuándo se rompería.** Es un State a medio camino, y lo decimos así: si aparecieran más fases (abandono, tiempo agotado, tablas por acuerdo), convendría que cada estado decida su transición, porque si no hay que tocar `ChessGame` por cada una. Si el grupo prefiere lo más simple, con el color al turno y un resultado alcanzaría.

## 9 bis. Dónde vive la validación de jugadas

**Qué.** La estrategia de cada pieza dice a dónde podría moverse según sus reglas (jugadas pseudo-legales). `ChessGame` filtra las que dejarían al propio rey en jaque, porque es el único que conoce el estado completo de la partida.

**Por qué.** Poner la validación completa dentro de cada pieza obligaría a que cada una conozca al rey y al resto del tablero, y a recalcular constantemente. Separar "cómo se mueve" de "es legal en esta partida" mantiene a las piezas simples.

**Cuándo se rompería.** Si el costo de simular cada jugada sobre una copia del tablero fuera un problema medido, habría que optimizar esa parte (por ejemplo, detectando clavadas), pero solo después de medir.

## 10. Las tablas son reglas inyectadas y el derecho de enroque sale del historial

**Qué.** Las condiciones de tablas (`InsufficientMaterialRule`, `FiftyMoveRule`, `ThreefoldRepetitionRule`) implementan `IDrawRule` y se inyectan. Para el enroque, que el rey y la torre "no se movieron" se calcula con las casillas tocadas del historial, sin banderas.

**Por qué.** Una regla de tablas nueva es una clase nueva. Y al no guardar banderas sueltas, deshacer devuelve el derecho de enroque solo, que es la fuente típica de errores.

**Cuándo se rompería.** Si el historial fuera enorme y consultarlo en cada jugada pesara, habría que guardar el dato calculado. Con partidas de ajedrez no es el caso.

## 11. La IA vive afuera del núcleo

**Qué.** El oponente está en el paquete `ai`: una interfaz `IOpponent` con tres niveles (azar, captura codiciosa y minimax de profundidad 2). El minimax evalúa con el material y un poco de criterio posicional (`Evaluation`: peones y piezas hacia el centro, desarrollo de caballos y alfiles, y el rey en su fila mientras haya damas), para que no juegue al azar cuando ninguna jugada gana material. Piensa sobre una partida de búsqueda propia y juega su jugada por `IChessGame`.

**Por qué.** No es parte de las reglas del juego, así que no puede ensuciar el núcleo. Como solo conoce la interfaz del puerto de entrada, se agregó sin modificar ninguna clase del núcleo, y no abre diálogos ni toca la pantalla mientras piensa.

**Cuándo se rompería.** Si hiciera falta una IA fuerte, el minimax simple no alcanza (haría falta poda y una mejor evaluación), y probablemente convendría un proceso externo detrás de otro puerto.

## 11 bis. Usuarios y ranking también viven afuera del núcleo

**Qué.** El paquete `accounts` tiene `User` y `Score` (valores inmutables), `AccountService` (registrar, entrar, armar el ranking) y `ScoreRecorder`, un observador que escucha la partida y acredita el resultado. Guardar los datos es un puerto de salida (`IAccountStore`, en el mismo paquete `accounts`: es el puerto que necesitan las cuentas, no el núcleo) con un adaptador de archivo CSV (`FileAccountStore`). Se entra solo con el nombre de usuario, sin contraseña; el id lo asigna el sistema y es interno.

**Por qué.** Quién juega y cuántas partidas ganó no es una regla del ajedrez. Como `ScoreRecorder` implementa los mismos `IMoveListener` e `IGameStatusListener` que ya usaban la consola y la ventana, no se tocó ninguna clase del núcleo. Cambiar el CSV por otra forma de guardar es escribir otro adaptador de `IAccountStore`. Deshacer la jugada final revierte el resultado y rehacerla lo vuelve a sumar, para que una misma partida no cuente dos veces.

**Cuándo se rompería.** Con usuarios reales haría falta contraseña, y el CSV no soporta bien dos procesos escribiendo a la vez; ahí convendría una base de datos detrás del mismo puerto.

## 12. La red es otro adaptador, y el núcleo arbitra

**Qué.** El juego por LAN vive en `network/`. El rival de la otra PC (`LanOpponent`) es un `IOpponent` más: en vez de pensar la jugada, la espera por la red. Cada PC tiene su propio `ChessGame` y por la red solo viajan jugadas. Lo que llega se valida: el nombre del rival se limpia (`LanNames`), una línea más larga de lo que el juego usa se corta, y una jugada mal formada o que no está entre las legales de la partida cierra la conexión.

**Por qué.** Es la idea de la Unidad 5 con la IA: el otro lado propone y el núcleo decide. Otra PC puede tener otra versión del juego o alguien puede mandar cualquier cosa al puerto, y eso no puede trabar ni romper la partida. Como el rival entra por las mismas interfaces que la IA (`IOpponent`, `IMoveListener`, `IPromotionChooser`), el núcleo no cambió para que exista la red.

**Cuándo se rompería.** Si hubiera que jugar por internet o con desconocidos, haría falta autenticación y un servidor que arbitre; validar en cada punta deja de alcanzar.

## 13. Sin puerto fijo para la partida

**Qué.** La partida usa un puerto libre que elige el sistema y que viaja en el aviso; el único fijo es el de los avisos (47123, configurable con `-Dajedrez.lan.port`).

**Por qué.** La primera versión usaba el 5000, que en macOS ocupa AirPlay. El detalle está en `docs/juego-en-red.md`.

**Cuándo se rompería.** Si una red bloqueara el broadcast (pasa en redes de facultad o de invitados), la lista quedaría vacía y haría falta poder escribir la IP a mano.

## 14. La pantalla no maneja sockets

**Qué.** `LanLobby` (en `network/`) busca partidas, crea una y espera rival, o se une a una de la lista, cada cosa en su hilo. `LanPanel` solo muestra la lista y los botones, y recibe el lobby por constructor.

**Por qué.** Antes la pantalla abría el `ServerSocket`, lanzaba los hilos y armaba el rival: tenía dos razones para cambiar (cómo se ve y cómo se conecta). Separado, cada una cambia sola (SRP) y el lobby se prueba sin ventana (`LanTests.testHostedGameUsesAFreePortAndConnects`).

**Cuándo se rompería.** Si la conexión fuera un solo botón sin estados, una clase aparte sería indirección de más.

## 15. El tablero de la ventana es su propio componente y sigue a la ventana

**Qué.** `BoardView` dibuja las casillas, las coordenadas y las piezas; `SwingChessView` maneja las pantallas y el turno. Cuando la ventana cambia de tamaño, `BoardView.fitTo` calcula el lado de casilla que entra y las piezas se vuelven a armar a ese tamaño, con imágenes de varias resoluciones (`SmoothImage`) para que se vean nítidas con la pantalla escalada (125 %, 150 %, Retina).

**Por qué.** La ventana tenía unas 670 líneas con todo mezclado. Separar el tablero lo hizo más fácil de leer y permitió el requisito no funcional de que se vea bien en cualquier tamaño y pantalla. Rehacer doce piezas en varias resoluciones cuesta, así que se rehacen cuando se deja de arrastrar el borde (Unidad 4: no pagar el costo en cada evento). Es una decisión de experiencia de uso: no se midió con un profiler.

**Cuándo se rompería.** Si la ventana tuviera un tamaño fijo, recalcular las casillas sería trabajo de más.

## 16. Los recursos se leen del classpath

**Qué.** El ícono, las piezas y el catálogo de skins se leen del classpath (`Assets`), no de `src/assets/...`. El ícono va con `setIconImages` y, en macOS, con `Taskbar`.

**Por qué.** Con la ruta relativa el juego dependía de la carpeta desde la que se ejecutaba, y el ícono solo aparecía en Windows. Es un requisito no funcional: tiene que andar igual en las PCs del grupo.

**Cuándo se rompería.** Si los recursos tuvieran que poder cambiarse sin recompilar (por ejemplo, skins que agrega el usuario), habría que leerlos de una carpeta configurable además del classpath.

## 17. Las skins son datos, y sin derechos de terceros

**Qué.** El catálogo de skins es un archivo (`assets/skins/skins.csv`) más una carpeta con las 12 piezas. `SkinService` decide cuáles están desbloqueadas por los puntos del ranking y la elegida se guarda por el puerto `ISkinStore`. Solo se incluyen imágenes con licencia que lo permita y con autor citado.

**Por qué.** Una skin nueva no toca código (Open/Closed llevado a datos). Y la Unidad 5 pide respetar la propiedad intelectual: por eso "Age of Empires" pasó a llamarse "Medieval" (era una marca) y se sacó la skin de Minecraft, que usaba personajes y texturas de Mojang sin una licencia que cubra este uso. `SkinTests` revisa que ninguna skin del catálogo use esas marcas.

**Cuándo se rompería.** Si las skins necesitaran comportamiento propio (animaciones, sonidos), un archivo de datos no alcanzaría y cada una sería una clase.

## 18. Tests del núcleo con un runner propio

**Qué.** Los tests están en `ChessCoreTests` (y los de usuarios en `AccountTests`, con un almacén en memoria como fake), con formato Arrange-Act-Assert, un stub (`AlwaysQueenChooser`), un mock (`RecordingObserver`) y los marcados con `// TDD:` escritos antes que el código. Se corren con `./scripts/test.sh` y no usan librerías. Afuera del núcleo hay tests de integración de los adaptadores: `LanTests` (dos partidas conectadas por la propia máquina), `SkinTests` y `scripts/SwingIntegrationTests.java`, que maneja la ventana real (pirámide de testing: muchos unitarios, menos de integración y uno de sistema).

**Por qué.** Corren solo con `javac`, sin infraestructura, que es lo que pide la consigna, y no hay nada para instalar ni explicar en la defensa.

**Cuándo se rompería.** En un proyecto real con un equipo grande convendría JUnit, por el reporte, los filtros y la integración con las herramientas.

## 19. Simplificaciones declaradas

- La repetición de posición ignora los derechos de enroque y de captura al paso.
- La simulación de captura al paso no retira el peón capturado: puede permitir un jaque descubierto horizontal y también rechazar una captura que eliminaría al peón que da jaque.
- La búsqueda de la IA no conserva el historial: puede evaluar de forma imprecisa el enroque, la captura al paso y las tablas por repetición o por 50 movimientos. La jugada final se filtra contra las jugadas legales de la partida real.

## Patrones que no se usaron

- **Singleton**: no hay nada que deba existir una sola vez; sería estado global sin necesidad.
- **Service Locator**: se prefirió inyectar por constructor, que deja explícitas las dependencias.
- **Facade**: `IChessGame` ya es la entrada única; otra capa encima no agregaría nada.
- **Object Pool**: no hay un problema de rendimiento medido que lo justifique.
Fuera del núcleo, `AutoPromotionChooser` sí usa Decorator: envuelve al selector humano y elige reina automáticamente para el bando de la IA, en la consola y en la ventana.
