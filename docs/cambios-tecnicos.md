# Cambios técnicos: login, skins y ajustes de la ventana

Este documento resume, de forma técnica, todo lo que se agregó y cambió en esta etapa del trabajo: qué se hizo, por qué y cómo está implementado. Para el detalle de cada tema hay documentos propios: `docs/cambios-usuarios-y-ranking.md` y `docs/skins.md`.

El principio de siempre se mantuvo: **el núcleo (`src/core`) no depende de infraestructura**. Login y skins se conectan desde `Main` y `MainSwing`. Las correcciones de reglas de la sección 9 viven dentro del núcleo.

## Resumen

| Tema | Qué cambió |
|---|---|
| Login | La ventana y la consola arrancan con el login (entrar, registrarse o invitado). El registro tiene su propia vista. |
| Puntos | Suman las partidas contra la IA. El 1 contra 1 en la misma PC no suma. |
| Skins | Skins desbloqueables por puntos (piezas y colores del tablero): Clásico, Staunton, Medieval y Cubos (antes llamada Minecraft). Paquete `skins/`. |
| Ventana | Bordes completos con la pantalla escalada, imágenes nítidas, ícono propio, menos textos de relleno. |
| Pruebas | 66 del juego, 20 de usuarios, 10 de skins, 4 de consola y la prueba de la ventana real con skins. |

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

**Regla.** Suman las partidas contra la IA. El 1 contra 1 en la misma PC no suma: dos personas en la misma máquina no tienen por qué compartir ranking.

**Cómo se aplica.** `ScoreRecorder` sigue siendo genérico: acredita a los usuarios que recibe. La regla la aplican quienes lo crean:

- En 1 contra 1, `Main` y `SwingChessView` le pasan los dos usuarios en `null`.
- Contra la IA, solo el lado de la persona tiene usuario; la IA nunca tiene.

**Limpieza.** Se borró `ScoreRecorder.setBlackUser`. Nada lo llamaba y su comentario decía que se podía pasar a jugar contra la IA a mitad de partida, cosa que la ventana no permite. Los usuarios del `ScoreRecorder` quedaron `final`: una partida que empezó 1 contra 1 nunca puede sumar puntos. El test que lo usaba se reemplazó por `testNoWhiteUserCreditsOnlyBlack` (la persona juega con negras contra la IA y solo se le acredita a ella).

## 4. Ajustes visuales de la ventana

- **Menos textos de relleno.** Se sacaron la leyenda "PJ jugadas, G ganadas…" del ranking, el aviso "1 contra 1: no suma al ranking" de la barra de la partida y otros textos explicativos. El ranking vacío dice "Todavía no hay usuarios.", la barra muestra "1 contra 1" y un jugador sin usuario aparece como "invitado".
- **Bordes completos con la pantalla escalada (125%).** Los bordes redondeados se dibujaban con `drawRoundRect(0, 0, w - 1, h - 1)`, justo en el límite del componente. Con escala 1,25 parte de la línea caía afuera y el borde se veía cortado. Ahora `RoundedBorder.outline()` dibuja un `RoundRectangle2D` medio píxel hacia adentro con `STROKE_PURE`. Lo usan `FlatButton`, `ChoiceControl`, `RoundedBorder` y `RoundedPanel`.
- **Foco.** Los botones de la barra de la partida (Menú, Deshacer, Rehacer, Ranking) no toman el foco del teclado. Antes, Menú aparecía con un marco verde al empezar.
- **Ícono propio.** `src/assets/icono.png`. La ventana lo carga con `setIconImages` en varios tamaños ya achicados (16, 20, 24, 30, 32, 36, 40, 48, 60, 64, 72, 96, 128 y 256 px). Windows elige el que necesita, también con la pantalla escalada, en vez de reescalarlo y dejarlo borroso.
- **Imágenes nítidas (`SmoothImage`).** Antes las piezas se achicaban a tamaño "lógico" con `getScaledInstance` y después Java las agrandaba 1,25 veces sin interpolar, y quedaban pixeladas. Ahora `SmoothImage` achica cada imagen directo al tamaño real en pantalla (la escala sale del `Graphics2D`), de a mitades y con interpolación bicúbica, y guarda el resultado. Lo usan las piezas del tablero, los reyes del selector de color, las vistas previas de las skins y el ícono.

## 6. Skins

Una skin cambia las piezas y los colores del tablero. Se desbloquea cuando los puntos del ranking llegan a lo que pide; los puntos no se gastan. Detalle en `docs/skins.md`.

### Catálogo

`src/assets/skins/skins.csv`, una línea por skin: `id;nombre;puntos;casilla clara;casilla oscura;carpeta`.

| Skin | Puntos | Equivale a |
|---|---|---|
| Clásico | 0 | — |
| Staunton | 6 | 2 victorias |
| Medieval | 15 | 5 victorias |
| Cubos | 30 | 10 victorias |

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

- `src/skins/`: `Skin`, `SkinService`.
- `src/skins/ISkinStore.java`.
- `src/adapters/`: `LoginPanel`, `SkinsPanel`, `SkinImages`, `SmoothImage`, `FileSkinStore`, `SkinCatalogReader`.
- `src/SkinTests.java`.
- `src/assets/icono/` (el ícono en 7 tamaños), `src/assets/skins/` (catálogo, créditos y las carpetas de piezas).
- `docs/skins.md` y este documento.

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

- `ChessCoreTests` (66) y `AccountTests` (20): el juego, la IA, los usuarios y el ranking.
- `SkinTests` (10): desbloqueo por puntos, rechazo de una bloqueada, skin en uso, vuelta a la de siempre si bajan los puntos, invitado, archivo por usuario y lectura del catálogo.
- `test-console.sh` (4): invitado en 1 contra 1 con deshacer y repetir el mate, registro y partida contra la IA, un 1 contra 1 que no suma y login persistente.
- `SwingIntegrationTests`: la ventana real con login, registro, menú, skins, 1 contra 1, la IA en tres niveles y dos colores, deshacer, rehacer, ranking, un tablero de 10x10.

## 8. Arreglos del 26/10: multiplataforma y normas de la cátedra

| Problema | Arreglo |
|---|---|
| El ícono solo se veía en Windows | Se cargan los tamaños estándar (16 a 256) con `setIconImages` y Java elige cuál usar; en macOS se pone el del Dock con `Taskbar`. |
| El ícono se preescalaba a mano a 13 tamaños | Ya no: los PNG están en `src/assets/icono/` y el sistema elige. Las piezas usan imágenes de varias resoluciones (`SmoothImage`). |
| Las piezas no seguían el tamaño de la ventana | El tablero pasó a `BoardView`: al agrandar o achicar la ventana, las casillas se ajustan y las piezas se rehacen a ese tamaño. |
| Recursos leídos de `src/assets` con una ruta relativa | Se leen del classpath (`Assets`): andan desde cualquier carpeta y en cualquier sistema. `build.sh` copia los recursos y compila con `--release 21`. |
| Paquetes en español (`nucleo`, `adaptadores`, ...) | En inglés (`core`, `adapters`, ...): la convención de la cátedra es identificadores en inglés y comentarios en español. |
| `ports` dependía de `accounts` (y el núcleo, de forma transitiva, de los usuarios) | `IAccountStore` pasó a `accounts` e `ISkinStore` a `skins`. |
| Skins con marcas y personajes de terceros | "Age of Empires" pasó a "Medieval"; la antigua skin Minecraft se recuperó como Cubos (ver `src/assets/skins/CREDITS.md`). |
| `scripts/Capture.java` no compilaba y `list_types.py` pedía Python | Se borró `Capture.java`; la lista del UML se genera con `java scripts/ListTypes.java`. |
| Rehacer un jaque o un mate no lo volvía a anunciar | `ChessGame.redo()` vuelve a avisar el estado. `ScoreRecorder` ya no necesita la partida ni una lista aparte para rehacer. |
| Las reglas de siempre se armaban igual en tres lugares | `core/StandardRules` las arma para la consola, la ventana y la búsqueda de la IA. |
| La coronación automática de la IA estaba repetida en la ventana | Se usa el mismo Decorator (`AutoPromotionChooser`) en la consola y en la ventana. |

## 9. Correcciones de reglas e historial

- Captura al paso: la simulación ejecuta el comando especial sobre la copia del tablero, incluido retirar el peón capturado. No consulta al usuario por coronaciones hipotéticas.
- Repetición: `GamePosition` compara tablero, turno y los derechos aportados por las estrategias. El enroque cuenta aunque hoy esté bloqueado; una captura al paso imposible por jaque no cuenta como derecho disponible.
- Búsqueda: `IChessGame.copy` conserva derechos, última jugada, contador de medias jugadas, posiciones anteriores, reglas de tablas y estado terminal. La copia no tiene listeners ni comandos que puedan modificar la partida real. Su deshacer termina en el inicio de la búsqueda.
- Pruebas: `RulesRegressionTests` agrega 17 casos. El test original de repetición ahora mueve caballos: mover las torres pierde derechos y, por lo tanto, no reproduce la posición inicial.
