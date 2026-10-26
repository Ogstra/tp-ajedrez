# Skins

Una skin cambia las piezas y los colores del tablero. Se desbloquea cuando los puntos del ranking llegan a lo que pide; los puntos no se gastan. Se eligen desde el menú, en **Skins**: la que está en uso lo dice, las desbloqueadas tienen **Usar** y las bloqueadas muestran cuántos puntos piden. La skin es de quien entró y se usa en cualquier modo (1 contra 1, contra la IA y por LAN, donde cada PC ve la suya).

## Cómo agregar una skin

1. Crear una carpeta en `src/assets/skins/`, por ejemplo `src/assets/skins/neon/`, con las 12 imágenes PNG de las piezas:
   `wK wQ wR wB wN wP bK bQ bR bB bN bP` (w blancas, b negras; K rey, Q reina, R torre, B alfil, N caballo, P peón). Conviene que sean cuadradas y de 180 px o más, con fondo transparente.
2. Sumar una línea a `src/assets/skins/skins.csv`:

   ```
   neon;Neón;40;1B1B2F;3F2B96;skins/neon
   ```

   Los campos son: id, nombre, puntos para desbloquearla, color de la casilla clara, color de la casilla oscura (hexadecimal, sin `#`) y carpeta de las piezas dentro de `src/assets`. Las imágenes se leen del classpath, así que después de agregar una hay que volver a compilar (`./scripts/build.sh` copia los recursos; IntelliJ lo hace solo).

No hay que tocar código. Si falta alguna imagen, se usa la del set de siempre. La primera línea del catálogo es la skin de siempre y tiene que pedir 0 puntos. No hace falta recortar las imágenes: el juego quita el margen transparente sobrante con un mismo recuadro para las 12 piezas de la carpeta, así no cambian los tamaños relativos ni la alineación de las bases.

## Skins incluidas

| Skin | Puntos | Equivale a | Piezas |
|---|---|---|---|
| Clásico | 0 | — | Set Cburnett de siempre (`src/assets/piezas`). |
| Staunton | 6 | 2 victorias | Piezas Staunton con contorno marcado (`skins/staunton`). |
| Medieval | 15 | 5 victorias | Ejércitos medievales: azul las blancas y rojo las negras (`skins/medieval`). |

Los puntos crecen con lo vistoso de cada una: Staunton es un primer premio rápido y Medieval queda como meta. Hasta el 26/10 la Medieval se llamaba "Age of Empires" y había una de Minecraft: se cambiaron porque usaban marcas y personajes de otros juegos (Unidad 5, propiedad intelectual). Una skin nueva tiene que usar imágenes propias o con una licencia que lo permita, y citar al autor en `CREDITS.md`. Los créditos y licencias de las imágenes están en `src/assets/skins/CREDITS.md`.

## Cómo está armado

Igual que los usuarios: la lógica no sabe de archivos ni de Swing, y se guarda por un puerto.

| Clase | Paquete | Qué hace |
|---|---|---|
| `Skin` | `skins` | Value object: id, nombre, puntos que pide, los dos colores (como texto hexadecimal, sin depender de Swing) y la carpeta de las piezas. |
| `SkinService` | `skins` | Qué skins hay, si una está desbloqueada (`puntos del usuario >= puntos que pide`), cuál se usa y elegir una (rechaza las bloqueadas). |
| `ISkinStore` | `skins` | Puerto de salida: dónde se guarda la skin elegida por cada usuario. |
| `FileSkinStore` | `adapters` | Lo implementa con `data/skins.csv`, una línea `idUsuario;idSkin`. |
| `SkinCatalogReader` | `adapters` | Lee el catálogo `assets/skins/skins.csv` del classpath (saltea comentarios y líneas mal formadas). |
| `SkinImages` | `adapters` | Lee las imágenes de cada skin una sola vez, les quita el margen sobrante con un recuadro común y, si falta alguna, usa la de siempre. |
| `SkinsPanel` | `adapters` | La pantalla de skins, con una vista previa de cada una. |

`AccountService.pointsOf(user)` da los puntos del ranking (0 para un invitado). `MainSwing` arma el `SkinService` con el catálogo y el archivo, y se lo pasa a la ventana, que pide la skin en uso al empezar cada partida.

## Reglas

- Un invitado tiene 0 puntos: usa las skins gratis, y la que elija dura esa sesión.
- Si los puntos bajan (se deshizo una victoria) y ya no alcanzan para la skin elegida, se usa la de siempre hasta volver a llegar; la elección no se pierde.

## Pruebas

`src/SkinTests.java` (corre en `./scripts/test.sh`): invitado solo con la gratis, desbloqueo al llegar a los puntos, rechazo de una bloqueada, la elegida se usa, vuelta a la de siempre si bajan los puntos, elección de invitado en la sesión, el archivo recuerda a cada usuario la lectura del catálogo y que cada skin del catálogo real traiga sus 12 piezas sin marcas de terceros. `SwingIntegrationTests` abre la pantalla de skins con 0 puntos.
