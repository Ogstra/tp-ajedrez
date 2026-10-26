# Lo que pide la cátedra y cómo lo cumple el TP

Resumen del material de la materia (Ingeniería de Software, UADE, 2.º cuatrimestre 2026, prof. Christian Tortorella) que aplica al TPO, con el lugar del código o de la documentación donde se cumple cada punto. Sirve para revisar el avance antes de cada entrega y para preparar la defensa.

Fuentes, en orden de autoridad: el enunciado del TPO, el modelo de examen, los apuntes del alumno y las presentaciones de las unidades 0 a 6, los ejemplos de código de la cátedra y, por último, las transcripciones de clase.

## 1. El enunciado del TPO

| Pide | Dónde se cumple |
|---|---|
| Tablero 8x8, las 6 piezas con su movimiento, turnos, captura, rechazo de movimientos inválidos, jaque | `core/`, probado en `ChessCoreTests` |
| Opcional (no resta si falta): mate, enroque, captura al paso, coronación, tablas, IA, deshacer/rehacer, interfaz gráfica | Todo implementado: `core/special`, `core/draw`, `ai/`, Command en `core/`, `adapters/SwingChessView` |
| El núcleo no depende de framework, motor gráfico ni infraestructura | `core/` solo importa `java.util` y `ports/` (ver la tabla de dependencias de `docs/uml-checklist.md`) |
| Tests unitarios del núcleo sin infraestructura | `ChessCoreTests`: sin archivos, sin red, sin ventana |
| Agregar clases sin modificar las existentes, sin romper herencia ni composición | Pieza nueva, tablero de otro tamaño, regla nueva: sección "Agregar cosas sin tocar lo que ya está" del README |
| UML fiel al código y mantenido | `docs/uml-checklist.md` se genera del código con `java scripts/ListTypes.java` |
| Justificación qué / por qué / cuándo se rompería, sobre decisiones propias | `docs/justificacion.md` |
| Presentación PPTX | `docs/presentacion.pptx` (se genera con `scripts/build_deck.js`) |
| Grupo de 3 a 5 personas; defensa individual con una extensión no programada | Ver la sección 9 de este documento |

## 2. Convenciones que usa la cátedra en su código

- **Comentarios en español, identificadores en inglés.** Lo dicen los encabezados de los ejemplos de código de las unidades 1, 4 y 5. Los paquetes también son identificadores: `core`, `ports`, `adapters`, `network`, `accounts`, `skins`, `ai`.
- **Cada decisión con qué, por qué y cuándo romperla.** El enemigo transversal es la sobre-ingeniería: no se agrega un patrón, una capa o un control que no resuelva un problema del enunciado.

## 3. Unidad 0 y Unidad 1: arquitectura, SOLID y dependencias

| Concepto | Cómo aparece en el TP |
|---|---|
| Núcleo y adaptadores (hexagonal) | Las reglas en `core/`; consola, Swing, archivos, red e IA afuera. Las dependencias apuntan hacia adentro |
| Puerto de entrada | `IChessGame` (lo usan la consola, la ventana, la IA y la red) |
| Puertos de salida | `IMoveListener`, `IGameStatusListener`, `IPromotionChooser` (en `ports/`); `IAccountStore` en `accounts/` e `ISkinStore` en `skins/` |
| SRP | `BoardView` (dibujo del tablero), `LanLobby` (sockets) y `SwingChessView` (pantallas y turno) están separados |
| OCP | Pieza nueva con `PieceFactory.register`, reglas especiales y de tablas enchufables, skins agregadas solo con datos |
| LSP | Todas las `IMovementStrategy`, `ISpecialMove`, `IDrawRule` e `IOpponent` se intercambian sin sorpresas |
| ISP | Los avisos del núcleo están partidos en dos interfaces chicas |
| DIP | `ChessGame` depende de interfaces; `GameBuilder` depende de `IGameView`, no de la ventana concreta |
| Los 4 niveles de dependencias | Se usa el nivel 4 (inyección por constructor) en `ChessGame`, `AccountService`, `SkinService`, `SwingChessView`, `LanPanel`. No hay Singleton ni Service Locator |
| Composición sobre herencia | Una sola clase `Piece` que tiene una estrategia; la reina compone torre y alfil |

## 4. Unidad 1: patrones y testing

| Patrón | Variación real que resuelve |
|---|---|
| Strategy | Movimiento de cada pieza, reglas especiales, reglas de tablas, niveles de IA y el rival por LAN |
| State | Turno y estados terminales (mate, ahogado, tablas) |
| Command | Deshacer y rehacer (el enunciado lo lista como opcional) |
| Observer | Varios interesados en la misma partida: consola o ventana, `ScoreRecorder` y el rival por LAN |
| Factory | `PieceFactory`, `BoardSetup`, `Opponents`, `GameBuilder` |

Testing: pirámide (muchos unitarios del núcleo, algunos de integración de adaptadores y uno de sistema con la ventana real), Arrange-Act-Assert, stub (`AlwaysQueenChooser`), mock (`RecordingObserver`, `RecordingLobbyListener`), fake (almacenes en memoria de `AccountTests` y `SkinTests`) y tests marcados `// TDD:`.

Metodología: walking skeleton y fases cortas con todo en verde al cerrar cada una, una rama y un PR por cambio.

## 5. Unidad 3: BPO y SLA

No hay terceros contratados, pero la idea del SLA como interfaz aparece en el diseño: la IA y el rival por LAN son "proveedores" detrás de `IOpponent`. El núcleo no confía en lo que devuelven: valida cada jugada contra las legales, igual que un SLA define qué se espera sin decir cómo se hace.

## 6. Unidad 4: optimización

"Premature optimization is the root of all evil": primero se mide. En el TP solo se optimizó donde el costo era evidente y se dejó escrito:

- **Caching**: los íconos de las piezas se arman una vez por tamaño de casilla (`BoardView.pieceIcons`) y las imágenes de cada skin se leen una sola vez (`SkinImages`).
- **Batching / lazy**: al cambiar el tamaño de la ventana, las piezas se rehacen cuando se deja de arrastrar el borde (un `Timer` que se reinicia), no en cada evento.
- **Precomputation**: el recuadro común de las 12 piezas de una skin se calcula al cargarla.
- Estas técnicas viven en adaptadores, nunca en el núcleo.
- La IA busca sobre una copia del tablero (`SearchGames`); si fuera lenta, primero habría que medir con un profiler (VisualVM) antes de agregar poda.

## 7. Unidad 5: gobernanza, riesgos, ética, privacidad y propiedad intelectual

- **Registro de riesgos del TPO y política de uso de IA**: `docs/gobernanza-y-calidad.md`.
- **La salida de otro sistema no es confiable** (bonus "IA asesora de ajedrez"): lo que llega por la red se valida (`LanNames`, `LanConnection.receive`, `LanOpponent.chooseMove`) y el núcleo decide qué jugada es legal. La red propone, el núcleo arbitra.
- **Minimización de datos** (Ley 25.326): por la red solo viajan el nombre de usuario y las jugadas. No hay contraseñas ni otros datos personales; los datos de usuarios quedan en `data/`, que no se sube al repositorio.
- **Propiedad intelectual**: las piezas de terceros tienen su licencia y autor (`src/assets/skins/CREDITS.md`, README). Se sacaron la marca "Age of Empires" y la skin de Minecraft, que usaba personajes y texturas de Mojang sin una licencia que lo cubra.
- **Accesibilidad**: los colores de texto de la interfaz tienen contraste de 4,5:1 o más (`adapters/Palette`), y la ventana se puede agrandar: el tablero y las piezas crecen con ella.

## 8. Unidad 6: auditoría, métricas e integridad

- **Auditoría**: se compara contra un criterio, con evidencia. Antes de cada entrega se revisa el TP contra este documento, con el comando o el archivo que respalda cada punto (los tests, la tabla de dependencias de `docs/uml-checklist.md`).
- **KPIs y KRI del TPO**, con la contramedida a la ley de Goodhart: `docs/gobernanza-y-calidad.md`.
- **Integridad profesional**: no se tocan tests para que pasen; las simplificaciones están declaradas (sección 19 de la justificación); el uso de IA se declara según la política de `docs/gobernanza-y-calidad.md`.
- **Integridad técnica, proporcional a un TPO**: control de versiones con revisión por PR, sin dependencias externas (no hay cadena de suministro que fijar), compilación reproducible con `--release 21`.

## 9. Para la defensa individual

La consigna puede pedir en el momento, por ejemplo, una pieza nueva o un tablero de otro tamaño. Los dos casos ya están probados:

- Pieza nueva: `ChessCoreTests` agrega un "Chancellor" (torre + caballo) sin tocar ninguna clase.
- Otro tamaño: tests con 10x10 y 5x5, y `SwingIntegrationTests` abre un tablero de 10x10 en la ventana real.

Conviene que cada integrante pueda explicar, sin leer, por qué cada dependencia apunta hacia el núcleo y qué cambiaría si mañana se reemplazara Swing, el archivo CSV o la red.

## 10. Fechas

| Instancia | Fecha |
|---|---|
| 2.º parcial (unidades 4, 5 y 6) | 13/11/2026 |
| Entrega y exposición del TPO | 20/11/2026 |
| Recuperatorio o final adelantado | 27/11/2026 |
| Final regular: defensa individual del TPO | 11/12/2026 |
