# Cambios técnicos: login, juego por LAN, skins y ajustes de la ventana

Este documento resume, de forma técnica, todo lo que se agregó y cambió en esta etapa del trabajo: qué se hizo, por qué y cómo está implementado. Para el detalle de cada tema hay documentos propios: `docs/cambios-usuarios-y-ranking.md`, `docs/juego-en-red.md` y `docs/skins.md`.

El principio de siempre se mantuvo: **el núcleo (`src/core`) no cambió**. Todo lo nuevo son adaptadores, servicios afuera del núcleo o pantallas, conectados en los composition roots (`Main` y `MainSwing`).

## Resumen

| Tema | Qué cambió |
|---|---|
| Login | La ventana y la consola arrancan con el login (entrar, registrarse o invitado). El registro tiene su propia vista. |
| Puntos | Suman las partidas contra la IA y por LAN. El 1 contra 1 en la misma PC no suma. |
| Juego por LAN | Modo nuevo "Por LAN": lista de partidas de la red, sin escribir IPs. Paquete `network/`. |
| Skins | Skins desbloqueables por puntos (piezas y colores del tablero): Clásico, Staunton y Medieval (hasta el 26/10 había "Age of Empires" y "Minecraft"; ver la sección 8). Paquete `skins/`. |
| Ventana | Bordes completos con la pantalla escalada, imágenes nítidas, ícono propio, menos textos de relleno. |
| Pruebas | 65 del juego, 20 de usuarios, 6 de LAN, 9 de skins, 4 de consola y la prueba de la ventana real con LAN y skins. |

## 1. Pruebas de consola en Windows (UTF-8)

**Problema.** En Windows, Java escribe la consola en Cp1252: la "á" de "Ya estás registrado" sale como un byte (`E1`) que `grep`, que espera UTF-8 (`C3 A1`), no reconoce. La prueba fallaba aunque el programa estaba bien.

**Cambio.** `scripts/test-console.sh` corre `java -Dstdout.encoding=UTF-8 ...`, así la salida y `grep` usan la misma codificación.

## 2. Login y registro

**Antes.** El menú mezclaba la elección de modo con dos desplegables de usuario (uno por color) y un campo para registrar.

**Ahora.**

- La primera pantalla de la ventana es `LoginPanel`, con dos vistas en un `CardLayout`:
  - **Login:** campo de usuario, **Entrar** (botón principal) y los links **Registrarse** y **Entrar como invitado**.
  - **Registro:** campo de usuario, **Crear usuario** y **Volver**.
  - Enter aprieta el botón principal de la vista que se ve (`getDefaultButton()`).
- El menú (`MenuPanel`) quedó solo con modo, dificultad, color, Jugar, Ver ranking, Skins y **Cerrar sesión**, que vuelve al login.
- `SwingChessView` guarda el usuario de la sesión (`user`, `null` si es invitado) y lo usa al armar cada partida.
- En la consola, `Main` pide el login (`ConsoleAccounts.askUser`: 1 iniciar sesión, 2 registrarse, 3 invitado) antes de elegir el modo.

No hay contraseña: se entra con el nombre, como antes. `AccountService` no cambió en esto.

## 3. Qué partidas suman puntos

**Regla.** Suman las partidas contra la IA y por LAN. El 1 contra 1 en la misma PC no suma: dos personas en la misma máquina no tienen por qué compartir ranking.

**Cómo se aplica.** `ScoreRecorder` sigue siendo genérico: acredita a los usuarios que recibe. La regla la aplican quienes lo crean:

- En 1 contra 1, `Main` y `SwingChessView` le pasan los dos usuarios en `null`.
- Contra la IA, solo el lado de la persona tiene usuario; la IA nunca tiene.
- Por LAN, cada PC le pasa su propio usuario en su color, y el otro lado queda en `null` (ver sección 6).

**Limpieza.** Se borró `ScoreRecorder.setBlackUser`. Nada lo llamaba y su comentario decía que se podía pasar a jugar contra la IA a mitad de partida, cosa que la ventana no permite. Los usuarios del `ScoreRecorder` quedaron `final`: una partida que empezó 1 contra 1 nunca puede sumar puntos. El test que lo usaba se reemplazó por `testNoWhiteUserCreditsOnlyBlack` (la persona juega con negras contra la IA y solo se le acredita a ella).

## 4. Ajustes visuales de la ventana

- **Menos textos de relleno.** Se sacaron la leyenda "PJ jugadas, G ganadas…" del ranking, el aviso "1 contra 1: no suma al ranking" de la barra de la partida y otros textos explicativos. El ranking vacío dice "Todavía no hay usuarios.", la barra muestra "1 contra 1" y un jugador sin usuario aparece como "invitado".
- **Bordes completos con la pantalla escalada (125%).** Los bordes redondeados se dibujaban con `drawRoundRect(0, 0, w - 1, h - 1)`, justo en el límite del componente. Con escala 1,25 parte de la línea caía afuera y el borde se veía cortado. Ahora `RoundedBorder.outline()` dibuja un `RoundRectangle2D` medio píxel hacia adentro con `STROKE_PURE`. Lo usan `FlatButton`, `ChoiceControl`, `RoundedBorder` y `RoundedPanel`.
- **Foco.** Los botones de la barra de la partida (Menú, Deshacer, Rehacer, Ranking) no toman el foco del teclado. Antes, Menú aparecía con un marco verde al empezar.
- **Ícono propio.** `src/assets/icono.png`. La ventana lo carga con `setIconImages` en varios tamaños ya achicados (16, 20, 24, 30, 32, 36, 40, 48, 60, 64, 72, 96, 128 y 256 px). Windows elige el que necesita, también con la pantalla escalada, en vez de reescalarlo y dejarlo borroso.
- **Imágenes nítidas (`SmoothImage`).** Antes las piezas se achicaban a tamaño "lógico" con `getScaledInstance` y después Java las agrandaba 1,25 veces sin interpolar, y quedaban pixeladas. Ahora `SmoothImage` achica cada imagen directo al tamaño real en pantalla (la escala sale del `Graphics2D`), de a mitades y con interpolación bicúbica, y guarda el resultado. Lo usan las piezas del tablero, los reyes del selector de color, las vistas previas de las skins y el ícono.

## 5. Juego por LAN

Dos PCs de la misma red juegan sin escribir ninguna IP: una crea la partida y la otra la elige de una lista. El detalle completo está en `docs/juego-en-red.md`.

### Comunicación

| Para qué | Protocolo | Puerto | Mensaje |
|---|---|---|---|
| Anunciar partidas | UDP broadcast | 47123 (configurable con `-Dajedrez.lan.port`) | `AJEDREZ;<id>;<nombre>;<puerto TCP>`, cada segundo |
| Jugar | TCP | libre, lo elige el sistema (antes 5000, que en macOS ocupa AirPlay) | `HOLA;<nombre>` al conectarse; después una jugada por línea: `e2 e4`, o `e7 e8 Q` si corona; `CHAU` al salir |

- **UDP** permite mandar un mensaje a todas las PCs de la red sin saber quiénes son. Si un aviso se pierde no importa, porque llega otro al segundo.
- **TCP** garantiza que las jugadas lleguen completas y en orden, y avisa si el otro se va.
- Cada PC tiene su propio `ChessGame` y aplica las jugadas del rival con `tryMove`, con las mismas reglas. Solo viajan jugadas, nunca el tablero, y las dos partidas terminan iguales.

### Clases (`src/network`)

| Clase | Responsabilidad |
|---|---|
| `LanAnnouncer` | Manda el aviso a la dirección de broadcast de cada placa de red (wifi, cable, VPN, máquinas virtuales) y a la propia PC, para poder probar con dos ventanas en la misma máquina. |
| `LanBrowser` | Escucha los avisos y arma la lista. La IP sale del paquete recibido. Guarda cada partida una sola vez por su identificador y la olvida si pasan 3 segundos sin avisos. |
| `LanGame` | Una partida de la lista: identificador, nombre de quien la creó, IP y puerto. |
| `LanConnection` | El canal TCP: `waitForRival()` (quien crea), `connect()` (quien se une), `greet()`, `send()`, `receive()` (`null` = el rival se fue) y `close()`. |
| `LanOpponent` | El rival, visto desde la partida (ver abajo). |

### `LanOpponent`: el rival como un oponente más

Implementa tres interfaces que ya existían:

- **`IOpponent` (Strategy):** `chooseMove()` espera la línea del rival y la convierte en `Move`. Para la ventana es un oponente más, igual que los niveles de la IA, y usa el mismo camino: `playOpponentIfNeeded()` lo llama en otro hilo, así la ventana no se congela mientras se espera.
- **`IMoveListener` (Observer):** cuando se mueve una pieza propia, manda la jugada. El enroque se avisa sin casillas (`onCastling(color, kingside)`), pero el rey siempre mueve dos columnas: con dónde quedó el rey se reconstruye de dónde salió y se manda como jugada del rey.
- **`IPromotionChooser`:** si corona el rival, devuelve la pieza que vino en el mensaje. Si corona la persona, le pregunta a la ventana y guarda la elección para mandarla junto con la jugada.

### Ventana

- El menú tiene un tercer modo, **Por LAN**. Jugar abre `LanPanel`: **Crear partida** (abre el puerto, anuncia y espera en otro hilo, con **Cancelar**) o la lista **Partidas en la red** ("Partida de Ana"). Quien crea juega con blancas.
- `GameBuilder.build` recibe un parámetro más, el `LanOpponent` (`null` si no es LAN). `MainSwing` lo agrega como listener y como elector de coronación, y lo pone como oponente en un `OpponentController`.
- Por LAN no hay Deshacer ni Rehacer, porque habría que pedirle permiso al rival. Se puede salir con Menú mientras se espera la jugada del otro. Si el rival se desconecta, la barra muestra "<nombre> se desconectó."
- La primera vez Windows pregunta si deja que Java use la red; hay que permitirlo (redes privadas).

## 6. Skins

Una skin cambia las piezas y los colores del tablero. Se desbloquea cuando los puntos del ranking llegan a lo que pide; los puntos no se gastan. Detalle en `docs/skins.md`.

### Catálogo

`src/assets/skins/skins.csv`, una línea por skin: `id;nombre;puntos;casilla clara;casilla oscura;carpeta`.

| Skin | Puntos | Equivale a |
|---|---|---|
| Clásico | 0 | — |
| Staunton | 6 | 2 victorias |
| Medieval | 15 | 5 victorias |

Para agregar una skin alcanza con una carpeta con las 12 imágenes (`wK`…`bP`) y una línea en el catálogo, sin tocar código.

### Diseño (igual que los usuarios)

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `Skin` | `skins` | Value object. Los colores van como texto hexadecimal para no depender de Swing. |
| `SkinService` | `skins` | Desbloqueo (`puntos del usuario >= puntos que pide`), skin en uso y elección (rechaza las bloqueadas). Un invitado tiene 0 puntos y su elección dura la sesión. Si los puntos bajan, se vuelve a la de siempre sin perder la elección. |
| `ISkinStore` | `skins` | Puerto de salida: dónde se guarda la skin elegida por cada usuario. |
| `FileSkinStore` | `adapters` | Lo implementa con `data/skins.csv` (`idUsuario;idSkin`). |
| `SkinCatalogReader` | `adapters` | Lee el catálogo; saltea comentarios y líneas mal formadas. |
| `SkinImages` | `adapters` | Lee las imágenes una vez por carpeta. Quita el margen transparente sobrante con un único recuadro para las 12 piezas (el más chico que las contiene a todas), así llenan la casilla sin cambiar sus tamaños relativos ni la alineación de las bases. |
| `SkinsPanel` | `adapters` | Pantalla Skins: "Tus puntos", vista previa (cuatro casillas con el rey) y "En uso", **Usar** o los puntos que pide. |

Se agregó `AccountService.pointsOf(user)`, que devuelve 0 para un invitado. `MainSwing` arma el `SkinService`, y la ventana pide la skin en uso al empezar cada partida (la de quien entró, en cualquier modo).

## 7. Archivos nuevos y modificados

**Nuevos**

- `src/network/`: `LanAnnouncer`, `LanBrowser`, `LanConnection`, `LanGame`, `LanOpponent`.
- `src/skins/`: `Skin`, `SkinService`.
- `src/skins/ISkinStore.java`.
- `src/adapters/`: `LoginPanel`, `LanPanel`, `SkinsPanel`, `SkinImages`, `SmoothImage`, `FileSkinStore`, `SkinCatalogReader`.
- `src/LanTests.java`, `src/SkinTests.java`.
- `src/assets/icono/` (el ícono en 7 tamaños), `src/assets/skins/` (catálogo, créditos y las carpetas de piezas).
- `docs/juego-en-red.md`, `docs/skins.md` y este documento.

**Modificados**

- `Main`, `MainSwing`, `SwingChessView`, `MenuPanel`, `RankingPanel`, `ConsoleAccounts`, `GameBuilder`.
- `FlatButton`, `ChoiceControl`, `RoundedBorder`, `RoundedPanel`, `Palette`.
- `ScoreRecorder`, `AccountService`.
- `scripts/test.sh`, `scripts/test-console.sh`, `scripts/SwingIntegrationTests.java`, `scripts/CaptureScreenshot.java`.
- `README.md`, `docs/cambios-usuarios-y-ranking.md`, `docs/uml-checklist.md`.

## 8. Pruebas

```bash
./scripts/test.sh
java --class-path out scripts/SwingIntegrationTests.java
```

- `ChessCoreTests` (65) y `AccountTests` (20): el juego, la IA, los usuarios y el ranking.
- `LanTests` (6): conexión y saludo; el mate del loco por la red con los dos tableros iguales; coronación a caballo y enroque viajando por la red; desconexión; un aviso que aparece en la lista. Las dos "PCs" son dos partidas en el mismo programa, conectadas por `localhost`.
- `SkinTests` (9): desbloqueo por puntos, rechazo de una bloqueada, skin en uso, vuelta a la de siempre si bajan los puntos, invitado, archivo por usuario y lectura del catálogo.
- `test-console.sh` (4): invitado en 1 contra 1 con deshacer y repetir el mate, registro y partida contra la IA, un 1 contra 1 que no suma y login persistente.
- `SwingIntegrationTests`: la ventana real con login, registro, menú, skins, 1 contra 1, la IA en tres niveles y dos colores, deshacer, rehacer, ranking, un tablero de 10x10 y una partida por LAN entre dos ventanas (lista, conexión, mate del loco en las dos y puntos para quien estaba registrado).

## 8. Arreglos del 26/10: multiplataforma y normas de la cátedra

| Problema | Arreglo |
|---|---|
| El ícono solo se veía en Windows | Se cargan los tamaños estándar (16 a 256) con `setIconImages` y Java elige cuál usar; en macOS se pone el del Dock con `Taskbar`. |
| El ícono se preescalaba a mano a 13 tamaños | Ya no: los PNG están en `src/assets/icono/` y el sistema elige. Las piezas usan imágenes de varias resoluciones (`SmoothImage`). |
| Las piezas no seguían el tamaño de la ventana | El tablero pasó a `BoardView`: al agrandar o achicar la ventana, las casillas se ajustan y las piezas se rehacen a ese tamaño. |
| La partida por LAN usaba el puerto 5000 (AirPlay en macOS) | Puerto libre elegido por el sistema; avisos en el 47123, configurable. `SO_REUSEPORT` en macOS y Linux. |
| Recursos leídos de `src/assets` con una ruta relativa | Se leen del classpath (`Assets`): andan desde cualquier carpeta y en cualquier sistema. `build.sh` copia los recursos y compila con `--release 21`. |
| Paquetes en español (`nucleo`, `adaptadores`, ...) | En inglés (`core`, `adapters`, ...): la convención de la cátedra es identificadores en inglés y comentarios en español. |
| `ports` dependía de `accounts` (y el núcleo, de forma transitiva, de los usuarios) | `IAccountStore` pasó a `accounts` e `ISkinStore` a `skins`. |
| La ventana creaba el rival por LAN con `new` y la pantalla de LAN manejaba sockets | `GameBuilder.overLan` arma la partida por LAN; `LanLobby` maneja la red y `LanPanel` solo muestra. `GameBuilder` depende de `IGameView`. |
| Lo que llegaba por la red se usaba sin validar | Nombres limpios, líneas acotadas, jugadas validadas contra las legales (ver `docs/juego-en-red.md`). |
| Skins con marcas y personajes de terceros | "Age of Empires" pasó a "Medieval"; se sacó la de Minecraft (ver `src/assets/skins/CREDITS.md`). |
| `scripts/Capture.java` no compilaba y `list_types.py` pedía Python | Se borró `Capture.java`; la lista del UML se genera con `java scripts/ListTypes.java`. |
| Rehacer un jaque o un mate no lo volvía a anunciar | `ChessGame.redo()` vuelve a avisar el estado. `ScoreRecorder` ya no necesita la partida ni una lista aparte para rehacer. |
| Las reglas de siempre se armaban igual en tres lugares | `core/StandardRules` las arma para la consola, la ventana y la búsqueda de la IA. |
| La coronación automática de la IA estaba repetida en la ventana | Se usa el mismo Decorator (`AutoPromotionChooser`) en la consola y en la ventana. |
| Los listeners tenían muchos métodos vacíos y el enroque no decía las casillas | Los puertos traen métodos vacíos por defecto, y `onCastling` pasa las casillas del rey. `LanOpponent` ya no necesita la partida. |
| Controles de red de más para un riesgo bajo | Quedan los que importan: jugadas validadas contra las legales, líneas acotadas, nombres sin `<`, `>`, `;` ni caracteres de control, y 5 segundos para el saludo. |
