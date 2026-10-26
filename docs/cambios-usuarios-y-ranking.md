# Cambios técnicos: usuarios y ranking

Este documento explica qué se agregó para tener un login básico (por nombre de usuario) y un ranking, cómo está armado y por qué.

## 1. Qué hace

- Al arrancar se inicia sesión, se registra un usuario o se entra como invitado. Suman puntos las partidas contra la IA y por LAN; el 1 contra 1 en la misma PC no.
- No hay contraseña. Se entra solo con el nombre, en la pantalla de login. El id lo asigna el sistema al registrarse, es interno y no se le pide ni se le muestra a la persona.
- Cuando una partida contra la IA o por LAN termina se acredita el resultado a la persona: 3 puntos por victoria, 1 por empate, 0 por derrota.
- El ranking se ve con el comando `ranking` en la consola y con el botón Ranking en la ventana (dentro de la misma ventana, sin diálogos).
- Los datos persisten en `data/usuarios.csv`.

## 2. Dónde encaja en la arquitectura

El núcleo no cambió: ninguna clase de `core/` conoce a los usuarios. Todo se enchufa por los puertos que ya existían.

| Pieza | Paquete | Rol |
|---|---|---|
| `User` | `accounts` | Valor inmutable (id y nombre). Dos usuarios son iguales si tienen el mismo id. |
| `Result` | `accounts` | `WIN`, `DRAW`, `LOSS`. |
| `Score` | `accounts` | Valor inmutable con victorias, empates y derrotas. Calcula partidas y puntos. `plus` devuelve un puntaje nuevo. |
| `IAccountStore` | `accounts` | Puerto de salida: `loadAll()` y `saveAll()`. Es lo único que el servicio sabe de la persistencia. |
| `AccountService` | `accounts` | Registrar, entrar, ranking, acreditar y revertir. Recibe el almacén por constructor (nivel 4 de DI). |
| `ScoreRecorder` | `accounts` | Observador de la partida. Implementa `IMoveListener` e `IGameStatusListener`. |
| `FileAccountStore` | `adapters` | Adaptador de salida: guarda en CSV. |
| `ConsoleAccounts` | `adapters` | Adaptador de entrada por consola: login, registro y tabla. |
| `MenuPanel` | `adapters` | Para la ventana Swing: el menú elige modo, dificultad y color. |
| `LoginPanel` | `adapters` | Primera pantalla de la ventana, con dos vistas: login (Entrar con el nombre o como invitado) y registro (Crear usuario). |
| `RankingPanel` | `adapters` | El ranking como tabla. |

`Main` y `MainSwing` (los composition roots) crean el almacén, el servicio y el recorder, y pasan el recorder en las listas de listeners de `ChessGame`. Es la misma inyección por constructor que ya se usaba.

## 3. Registro y login

`AccountService.register(name)` valida el nombre (no vacío, hasta 20 caracteres, sin `;` ni caracteres de control), rechaza duplicados sin distinguir mayúsculas y asigna como id el mayor existente más uno. `login(name)` devuelve el usuario con ese nombre (sin distinguir mayúsculas), o `null` si no existe. El id sirve para identificar al usuario por dentro (igualdad, archivo) y no interviene en el login.

## 4. Cómo se acredita un resultado

`ScoreRecorder` escucha los avisos de estado de la partida:

- `onCheckmate(winner)`: gana quien dio el mate y pierde el otro.
- `onStalemate()` y `onDraw(...)`: empate para los dos.

Si un lado no tiene usuario (jugó anónimo o es la IA), a ese lado no se le acredita nada, pero el otro sí. `ScoreRecorder` es genérico: acredita a los usuarios que recibe. La regla de qué partidas cuentan la aplican `Main` y la ventana, que en 1 contra 1 le pasan los dos usuarios en `null`.

### Deshacer y rehacer

Una partida terminada puede reabrirse con `undo`. Si no se hiciera nada, al volver a terminar se sumaría dos veces. Por eso el recorder guarda lo que acreditó (`Credit`) y, en `onUndo`, lo revierte. En `onRedo` vuelve a aplicarlo solo si la partida quedó otra vez terminada (`game.isGameOver()`). Rehacer una jugada anterior a la final no suma nada. Esto está cubierto por tests.

### Contra la IA y por LAN

Acreditan puntos las partidas contra la IA y por LAN. Por LAN, cada PC acredita a su propio usuario en su propio archivo (ver `docs/juego-en-red.md`). En los dos casos se acredita a la persona, juegue con blancas o con negras. Los usuarios quedan fijos al crear el `ScoreRecorder`, y la ventana no deja cambiar a la IA a mitad de partida (el modo, la dificultad y el color solo se eligen en el menú, y cada Jugar arma una partida nueva). Por eso se borró `setBlackUser`, el setter que lo permitía: una partida que empezó 1 contra 1 nunca puede sumar puntos.

## 5. Persistencia

`FileAccountStore` escribe `id;nombre;victorias;empates;derrotas`, una línea por usuario, en UTF-8. Para no dejar el archivo a medias escribe primero a un temporal y después lo mueve (de forma atómica si el sistema lo permite). Si el archivo no existe devuelve una lista vacía y las líneas mal formadas se saltean. Como el contenido está en memoria solo durante la operación, `AccountService` lee y guarda en cada cambio.

La carpeta es `data/` y se cambia con `-Dajedrez.data=carpeta`. `data/` está en `.gitignore`.

## 6. Ranking

Orden: puntos de mayor a menor, después más victorias, después nombre alfabético. Incluye a todos los registrados, también los que no jugaron. En la consola se ve como tabla de columnas fijas (`#`, `Usuario`, `PJ`, `G`, `E`, `P`, `Pts`) y en la ventana en una `JTable`.

## 7. Pruebas

- `AccountTests` (20): registro, validaciones, login, orden del ranking, persistencia en CSV (incluida la línea mal formada), acreditación por mate, ahogado y tablas, deshacer y rehacer, usuario anónimo, la persona con negras contra la IA. Usa un `InMemoryAccountStore` como fake. Marcados con `// TDD:` los escritos antes del código.
- `scripts/test-console.sh`: cuatro escenarios de punta a punta por consola con una carpeta de datos temporal (invitado en 1 contra 1 con deshacer y repetir el mate, registro y partida contra la IA, un 1 contra 1 que no suma al ranking, segunda ejecución con login y ranking persistente).
- `scripts/test.sh` corre todo junto: 65 pruebas del juego, 20 de usuarios y la consola.

## 8. Decisiones y límites

- Sin contraseña, como se pidió: cualquiera puede entrar como cualquier usuario registrado. Alcanza para un trabajo práctico, no para datos reales.
- El archivo CSV no está pensado para dos procesos escribiendo a la vez. Si hiciera falta, se escribe otro adaptador de `IAccountStore` y no cambia nada más.
- No se usó Singleton ni estado global: el servicio se inyecta.

## 9. Historial del repositorio

Se había subido una implementación paralela de login y ranking en dos commits del 23/10. Se pidió borrarlos del historial y rehacerlo sin tener en cuenta esa versión. Los commits se respaldaron antes en la rama local `backup/commits-del-23-10` y en un bundle de git, y se reescribió `main` con `--force-with-lease`. Quien tenga un clon con esos commits tiene que resetearlo a `origin/main`.

## 10. Una sola ventana (login, menú, partida y ranking)

`SwingChessView` pasó a tener cuatro pantallas dentro del mismo `JFrame`, que se cambian con un `CardLayout`:

- `MenuPanel`: modo (1 contra 1 o contra la IA), dificultad, color propio y los botones Jugar y Ver ranking. No tiene usuarios.
- `LoginPanel`: la primera pantalla, con dos vistas en un `CardLayout`. El login tiene el campo de usuario, Entrar y los links Registrarse y Entrar como invitado; el registro tiene su campo, Crear usuario y Volver. Enter aprieta el botón principal de la vista que se ve. El menú tiene Cerrar sesión para volver.
- La partida: el tablero, el botón Menú, la etiqueta de jugadores y de estado, y los botones Ranking, Deshacer y Rehacer. Si la persona juega con negras el tablero se gira.
- `RankingPanel`: la tabla y un botón Volver.

Cada vez que se aprieta Jugar la ventana pide una partida nueva a un `GameBuilder` (una interfaz de una sola función que implementa `MainSwing`, el composition root). Así la ventana no conoce las reglas concretas y no hace falta reiniciar la aplicación para jugar otra vez. Se sacaron los diálogos de login y de fin de partida: el resultado queda en la etiqueta de estado. El único diálogo que queda es el de coronación, porque el núcleo necesita la respuesta antes de seguir.

Se eligió no cambiar la dificultad en medio de la partida: se vuelve al menú. El botón Menú se bloquea mientras la IA piensa, para que su jugada no caiga sobre una partida vieja.

También: el botón Menú pide confirmación si la partida sigue en juego, Enter aprieta Entrar en el login, Crear usuario en el registro y Jugar en el menú, y el tablero muestra las coordenadas a-h y 1-8 (invertidas si se juega con negras). El detalle de la revisión está en `docs/ui-review.md`.

### Diseño de la ventana

Los colores están en `Palette` (un solo lugar) y sus contrastes de texto superan 4,5:1. El menú usa `ChoiceControl` para modo, dificultad y color (segmentos y tarjetas dibujados a mano, porque con el aspecto del sistema Swing ignora el fondo de los botones) y `FlatButton` para Jugar, Ver ranking y los botones de la partida, y también para Entrar, Crear usuario, Registrarse, Entrar como invitado, Volver y Cerrar sesión (los dos principales en verde y el resto como links, igual que Jugar y Ver ranking). La dificultad muestra una línea que explica cómo juega cada nivel. En la partida, Menú, el estado (en una o dos filas), los jugadores y los botones Deshacer, Rehacer y Ranking van en una sola barra arriba, y la ventana mide justo el tablero con casillas cuadradas. El ranking usa el mismo estilo que el menú: título, casillas de adorno y la tabla en un recuadro blanco con el puntero resaltado; si no hay usuarios muestra un aviso. Los bordes redondeados se dibujan medio píxel adentro (`RoundedBorder.outline`) para que con la pantalla escalada no se corte ningún lado, y los botones de la barra de la partida no toman el foco.

## 11. Login separado del modo

Antes el menú tenía dos desplegables de usuario (uno por lado) además del modo, la dificultad y el color. Ahora el menú solo elige cómo se juega, y el usuario se pide aparte y solo contra la IA: en la ventana, al apretar Jugar contra la IA aparece `LoginPanel` (iniciar sesión de la lista, registrarse por nombre o seguir sin usuario, con Empezar y Volver al menú), y en la consola solo se pregunta el usuario de la persona (blancas). En 1 contra 1 Jugar empieza directo y los dos usuarios llegan en `null`.

El porqué: en 1 contra 1 en la misma máquina el ranking no tiene sentido, y permitiría que dos cuentas se sumen puntos entre sí. Solo las partidas contra la IA acreditan puntos.

Sobre el cambio a la IA a mitad de partida: la ventana no lo permite. `ScoreRecorder.setBlackUser` no tenía ningún llamador en `src/`, y el modo, la dificultad y el color solo se eligen en el menú; cada Jugar arma un `Match` y un `ScoreRecorder` nuevos con el `GameBuilder`. Lo mínimo y consistente fue borrar ese setter (código muerto, con un comentario que decía algo falso), dejar el usuario de negras `final` como el de blancas y reemplazar el test que lo usaba por uno de la persona con negras contra la IA. Con los usuarios fijos desde el constructor, una partida que empezó 1 contra 1 nunca puede sumar puntos.

Después se movió el login al principio: la ventana arranca en `LoginPanel` (Entrar con el nombre, Registrarse o Entrar como invitado) y recién después va al menú, que tiene Cerrar sesión. La pantalla intermedia contra la IA ya no existe: contra la IA se usa el usuario con el que se entró (`null` si es invitado) y en 1 contra 1 los dos llegan en `null`. En la consola el login también va antes de elegir el modo.
