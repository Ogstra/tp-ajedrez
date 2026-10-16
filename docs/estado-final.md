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

Estas observaciones no bloquean la entrega. Las simplificaciones funcionales aceptadas se explican en la seccion 13 de docs/justificacion.md.

## Datos y UML que completa el grupo

La portada de la presentacion lleva a Franco Beron y la fecha de entrega. Para sumar integrantes o cambiar la fecha se editan AUTHORS y DATE al principio de scripts/build_deck.js y se regenera.

Franco dibuja el UML usando docs/uml-checklist.md. No se genero ningun diagrama. Cuando lo exporte como docs/uml.png, ejecutar node scripts/build_deck.js para insertarlo en la diapositiva 4.
