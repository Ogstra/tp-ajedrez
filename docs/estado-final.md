# Estado final del trabajo

Las fases 0 a 6 estan implementadas y revisadas. El codigo y los entregables se publican en main de Ogstra/tp-ajedrez.

| Fase | Resultado | Pruebas del runner al cerrar |
|---|---|---|
| 0 | Scripts de compilacion y tests con salida correcta; reglas del agente locales | 20 |
| 1 | Red de seguridad del alcance obligatorio | 36 |
| 2 | Identificadores en ingles y paquetes conservados | 36 |
| 3 | Move, puerto de entrada, listeners separados y estados terminales | 40 |
| 4 | Rehacer, tres reglas de tablas y captura al paso | 54 |
| 5 | Tres niveles de IA, consola y Swing | 63 |
| 6 | Captura, lista para UML, justificacion Markdown y Word, PPTX y README | 63 |

## Verificacion

- ./scripts/test.sh: 63 pasados y 0 fallados, mas la regresion de consola que deshace y repite un mate.
- java --class-path out scripts/SwingIntegrationTests.java: niveles medio y dificil, respuesta asincronica, deshacer y rehacer contra la IA, selector bloqueado al pensar y respuesta al rehacer despues de cambiar de modo.
- Buscador de identificadores: 0 coincidencias.
- Nucleo sin imports de adaptadores o ai, sin las herramientas excluidas por la consigna.
- Sin raya en los archivos de texto versionados ni en el contenido XML de los documentos.
- Word de cinco paginas renderizado y revisado. PPTX de 18 diapositivas renderizado y revisado, validadores de estructura y geometria sin hallazgos.
- Revision independiente al cerrar las fases y revision completa final sin hallazgos criticos o importantes pendientes.

## Ajustes detectados durante la ejecucion

- La consola acepta deshacer y rehacer despues del fin de partida hasta salir o EOF.
- Minimax contrasta la jugada elegida con las legales reales, aunque busque sobre una copia sin historial.
- El nivel medio valora el peon capturado al paso aunque el destino este vacio.
- Swing bloquea el selector mientras piensa y solicita respuesta de la IA cuando rehacer deja su turno sin una respuesta guardada.
- Los textos de entrega se contrastaron con el codigo y se aclararon los limites de la simulacion.
- Por pedido posterior del usuario, AGENTS.md se retiro del seguimiento y del historial de main con force-with-lease. Se conserva ignorado en la copia de trabajo local.

## Mejoras menores diferidas

1. Rehacer un mate o unas tablas restaura el estado terminal correctamente, pero el mensaje de Swing solo dice que se rehizo la jugada. Se podria volver a informar el resultado sin recalcularlo.
2. La condicion que impide el doble paso fuera de la fila inicial es correcta, pero el test actual solo comprueba el camino libre y sus bloqueos desde la fila inicial. Se podria sumar el caso fuera de esa fila.

Estas observaciones no bloquean la entrega. Las simplificaciones funcionales aceptadas se explican en la seccion 19 de docs/justificacion.md.

## Datos y UML que completa el grupo

La portada de la presentacion lleva a Franco Beron y la fecha de entrega. Para sumar integrantes o cambiar la fecha se editan AUTHORS y DATE al principio de scripts/build_deck.js y se regenera.

Franco dibuja el UML usando docs/uml-checklist.md. No se genero ningun diagrama. Cuando lo exporte como docs/uml.png, ejecutar node scripts/build_deck.js para insertarlo en la diapositiva 4.

## Despues del cierre (23/10)

| Cambio | Pruebas |
|---|---|
| Usuarios y ranking: login por nombre, puntos 3/1/0, deshacer revierte el resultado, CSV detras de IAccountStore | 65 del nucleo y 20 de usuarios |
| Ventana unica con menu (modo, dificultad, color, usuarios), partida y ranking; paleta con contraste medido | integracion Swing |
| La vista ya no crea colaboradores: GameBuilder (en MainSwing) devuelve el juego, el recorder y la IA en un Match | integracion Swing |
| La ventana se adapta al tamano del tablero (probado con 10x10) | integracion Swing |
| README, justificacion, lista del UML, captura y presentacion (19 diapositivas) al dia | |

Verificacion actual: ./scripts/test.sh da 65 y 20 pasados mas tres escenarios de consola; java --class-path out scripts/SwingIntegrationTests.java pasa el recorrido completo y el tablero de 10x10. El UML sigue pendiente.

## Despues del 26/10: multiplataforma y normas de la catedra

Rama `fix/catedra-multiplataforma` (sale de `feat/login-separado`, con `main` unido). El detalle esta en la seccion 8 de `docs/cambios-tecnicos.md` y las decisiones en las secciones 12 a 17 de `docs/justificacion.md`.

| Cambio | Pruebas |
|---|---|
| Paquetes en ingles (`core`, `ports`, `adapters`, `network`, `accounts`) y puertos de cuentas y skins en su modulo | todas, sin cambios de comportamiento |
| Icono en todos los tamanos con `setIconImages` y en el Dock de macOS con `Taskbar` | integracion Swing |
| Recursos por classpath; `build.sh` copia los assets y compila con `--release 21` | integracion Swing y prueba desde otra carpeta |
| Tablero en `BoardView`, que sigue el tamano de la ventana | integracion Swing (80 -> 117 px al agrandar) |
| LAN sin el puerto 5000: puerto libre para la partida, avisos en 47123 configurable, `SO_REUSEPORT` | 15 de LAN |
| `LanLobby` separado de la pantalla; `GameBuilder.overLan` y `IGameView` | 15 de LAN e integracion Swing |
| Validacion de lo que llega por la red | 15 de LAN |
| Skins sin marcas de terceros | 10 de skins |
| `normativa-catedra.md`, `gobernanza-y-calidad.md`, justificacion, README y lista del UML al dia | |

Verificacion: `./scripts/test.sh` da 65, 20, 15 y 10 pasados mas cuatro escenarios de consola; `java --class-path out scripts/SwingIntegrationTests.java` pasa los cinco recorridos. Probado en Windows 11 con JDK 26 compilando con `--release 21`. Falta probar en una Mac real (ver `.planning/AUDIT.md`). La presentacion y `justificacion.docx` hay que regenerarlas (`node scripts/build_deck.js` y `scripts/md_to_docx.py`), y el UML sigue pendiente.
