# Cambios técnicos: usuarios y ranking

Este documento explica qué se agregó para tener un login básico (por nombre de usuario) y un ranking, cómo está armado y por qué.

## 1. Qué hace

- Al arrancar, cada jugador humano puede iniciar sesión, registrarse o jugar sin usuario.
- No hay contraseña. Se entra solo con el nombre (en la ventana se elige de una lista del menú). El id lo asigna el sistema al registrarse, es interno y no se le pide ni se le muestra a la persona.
- Cuando una partida termina se acredita el resultado a los usuarios que participaron: 3 puntos por victoria, 1 por empate, 0 por derrota.
- El ranking se ve con el comando `ranking` en la consola y con el botón Ranking en la ventana (dentro de la misma ventana, sin diálogos).
- Los datos persisten en `data/usuarios.csv`.

## 2. Dónde encaja en la arquitectura

El núcleo no cambió: ninguna clase de `nucleo/` conoce a los usuarios. Todo se enchufa por los puertos que ya existían.

| Pieza | Paquete | Rol |
|---|---|---|
| `User` | `usuarios` | Valor inmutable (id y nombre). Dos usuarios son iguales si tienen el mismo id. |
| `Result` | `usuarios` | `WIN`, `DRAW`, `LOSS`. |
| `Score` | `usuarios` | Valor inmutable con victorias, empates y derrotas. Calcula partidas y puntos. `plus` devuelve un puntaje nuevo. |
| `IAccountStore` | `puertos` | Puerto de salida: `loadAll()` y `saveAll()`. Es lo único que el servicio sabe de la persistencia. |
| `AccountService` | `usuarios` | Registrar, entrar, ranking, acreditar y revertir. Recibe el almacén por constructor (nivel 4 de DI). |
| `ScoreRecorder` | `usuarios` | Observador de la partida. Implementa `IMoveListener` e `IGameStatusListener`. |
| `FileAccountStore` | `adaptadores` | Adaptador de salida: guarda en CSV. |
| `ConsoleAccounts` | `adaptadores` | Adaptador de entrada por consola: login, registro y tabla. |
| `MenuPanel`, `RankingPanel` | `adaptadores` | Lo mismo para la ventana Swing: el menú elige modo, dificultad, color y usuarios, y registra; el ranking es una tabla. |

`Main` y `MainSwing` (los composition roots) crean el almacén, el servicio y el recorder, y pasan el recorder en las listas de listeners de `ChessGame`. Es la misma inyección por constructor que ya se usaba.

## 3. Registro y login

`AccountService.register(name)` valida el nombre (no vacío, hasta 20 caracteres, sin `;` ni caracteres de control), rechaza duplicados sin distinguir mayúsculas y asigna como id el mayor existente más uno. `login(name)` devuelve el usuario con ese nombre (sin distinguir mayúsculas), o `null` si no existe. El id sirve para identificar al usuario por dentro (igualdad, archivo) y no interviene en el login.

## 4. Cómo se acredita un resultado

`ScoreRecorder` escucha los avisos de estado de la partida:

- `onCheckmate(winner)`: gana quien dio el mate y pierde el otro.
- `onStalemate()` y `onDraw(...)`: empate para los dos.

Si un lado no tiene usuario (jugó anónimo o es la IA), a ese lado no se le acredita nada, pero el otro sí.

### Deshacer y rehacer

Una partida terminada puede reabrirse con `undo`. Si no se hiciera nada, al volver a terminar se sumaría dos veces. Por eso el recorder guarda lo que acreditó (`Credit`) y, en `onUndo`, lo revierte. En `onRedo` vuelve a aplicarlo solo si la partida quedó otra vez terminada (`game.isGameOver()`). Rehacer una jugada anterior a la final no suma nada. Esto está cubierto por tests.

### Contra la IA en la ventana

El menú de la ventana crea el `ScoreRecorder` de cada partida con los usuarios elegidos. Contra la IA el lado de la IA no tiene usuario, así que solo se acredita a la persona, juegue con blancas o con negras.

## 5. Persistencia

`FileAccountStore` escribe `id;nombre;victorias;empates;derrotas`, una línea por usuario, en UTF-8. Para no dejar el archivo a medias escribe primero a un temporal y después lo mueve (de forma atómica si el sistema lo permite). Si el archivo no existe devuelve una lista vacía y las líneas mal formadas se saltean. Como el contenido está en memoria solo durante la operación, `AccountService` lee y guarda en cada cambio.

La carpeta es `data/` y se cambia con `-Dajedrez.data=carpeta`. `data/` está en `.gitignore`.

## 6. Ranking

Orden: puntos de mayor a menor, después más victorias, después nombre alfabético. Incluye a todos los registrados, también los que no jugaron. En la consola se ve como tabla de columnas fijas (`#`, `Usuario`, `PJ`, `G`, `E`, `P`, `Pts`) y en la ventana en una `JTable`.

## 7. Pruebas

- `AccountTests` (20): registro, validaciones, login, orden del ranking, persistencia en CSV (incluida la línea mal formada), acreditación por mate, ahogado y tablas, deshacer y rehacer, usuario anónimo, cambio del usuario de negras. Usa un `InMemoryAccountStore` como fake. Marcados con `// TDD:` los escritos antes del código.
- `scripts/test-console.sh`: tres escenarios de punta a punta por consola con una carpeta de datos temporal (partida anónima, registro con ranking, segunda ejecución con login y ranking persistente).
- `scripts/test.sh` corre todo junto: 65 pruebas del juego, 20 de usuarios y la consola.

## 8. Decisiones y límites

- Sin contraseña, como se pidió: cualquiera puede entrar como cualquier usuario registrado. Alcanza para un trabajo práctico, no para datos reales.
- El archivo CSV no está pensado para dos procesos escribiendo a la vez. Si hiciera falta, se escribe otro adaptador de `IAccountStore` y no cambia nada más.
- No se usó Singleton ni estado global: el servicio se inyecta.

## 9. Historial del repositorio

Se había subido una implementación paralela de login y ranking en dos commits del 23/10. Se pidió borrarlos del historial y rehacerlo sin tener en cuenta esa versión. Los commits se respaldaron antes en la rama local `backup/commits-del-23-10` y en un bundle de git, y se reescribió `main` con `--force-with-lease`. Quien tenga un clon con esos commits tiene que resetearlo a `origin/main`.

## 10. Una sola ventana (menú, partida y ranking)

`SwingChessView` pasó a tener tres pantallas dentro del mismo `JFrame`, que se cambian con un `CardLayout`:

- `MenuPanel`: modo (1 contra 1 o contra la IA), dificultad, color propio, usuario de cada lado y los botones Jugar y Ranking. Cada lado ofrece "Sin usuario", "Nuevo usuario..." (justo debajo) y los registrados; solo al elegir esta última opción aparece el campo para registrar. El lado que juega la IA muestra "IA" y queda bloqueado.
- La partida: el tablero, el botón Menú, la etiqueta de jugadores y de estado, y los botones Ranking, Deshacer y Rehacer. Si la persona juega con negras el tablero se gira.
- `RankingPanel`: la tabla y un botón Volver.

Cada vez que se aprieta Jugar la ventana pide una partida nueva a un `GameBuilder` (una interfaz de una sola función que implementa `MainSwing`, el composition root). Así la ventana no conoce las reglas concretas y no hace falta reiniciar la aplicación para jugar otra vez. Se sacaron los diálogos de login y de fin de partida: el resultado queda en la etiqueta de estado. El único diálogo que queda es el de coronación, porque el núcleo necesita la respuesta antes de seguir.

Se eligió no cambiar la dificultad en medio de la partida: se vuelve al menú. El botón Menú se bloquea mientras la IA piensa, para que su jugada no caiga sobre una partida vieja.
