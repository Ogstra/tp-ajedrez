# Revisión de la interfaz (6 pilares)

**Fecha:** 2026-10-23
**Base de comparación:** estándares abstractos de los 6 pilares (no hay UI-SPEC)
**Capturas:** renderizadas fuera de pantalla con Java (menú, partida y ranking); no se guardan en el repo
**Alcance:** `MenuPanel`, `SwingChessView` (partida) y `RankingPanel`

Cuando se hizo esta revisión el proyecto no usaba el flujo por fases de GSD (desde el 26/10 el contexto está en `.planning/`), así que la auditoría se hizo sobre el código y las capturas, y el resultado queda acá. Los comandos de búsqueda del auditor son de Tailwind y no aplican a Swing: se reemplazaron por búsquedas de `Color`, `Font`, `Insets`, `setText` y `setEnabled`.

---

## Puntajes

| Pilar | Puntaje | Hallazgo principal |
|---|---|---|
| 1. Textos | 3/4 | Claros y en español, pero el ranking vacío no dice nada y el fin de partida no ofrece un próximo paso |
| 2. Aspecto visual | 3/4 | Buen punto focal; faltan coordenadas del tablero y marcar el rey en jaque |
| 3. Color | 3/4 | Paleta coherente con el tablero; el verde del menú duplica el del tablero |
| 4. Tipografía | 3/4 | Una sola familia y un solo peso, pero 5 tamaños |
| 5. Espaciado | 3/4 | Se ve ordenado, pero hay unos 13 márgenes distintos sin escala |
| 6. Experiencia | 2/4 | Salir con Menú pierde la partida sin avisar y no hay uso de teclado |

**Total: 17/24**

---

## Las 3 correcciones prioritarias

1. **Menú abandona la partida sin confirmar** (`SwingChessView.java:175`). Un click de más pierde una partida en curso, y con usuarios ya no es solo cosmético. Pedir confirmación en el botón Menú cuando la partida no terminó y ya hubo jugadas.
2. **No se puede usar con teclado** (`MenuPanel.java`). Enter no inicia la partida ni registra, no hay botón por defecto ni atajos. Poner `getRootPane().setDefaultButton(...)` en Jugar, y que Enter en el campo de usuario nuevo registre.
3. **Faltan guías visuales en el tablero** (`SwingChessView.java:buildBoard`). Sin letras a-h ni números 1-8 es difícil leer jugadas como `e2 e4` del mensaje de estado, y más con el tablero girado para las negras. Agregar las coordenadas en las casillas de los bordes.

---

## Correcciones aplicadas

1. Menú pide confirmación ("Salir al menú" o "Seguir jugando") si la partida está en juego y el tablero cambió respecto del inicio. Si se deshizo todo o la partida terminó, sale directo.
2. Enter aprieta Jugar mientras se ve el menú, y Enter en el campo de usuario nuevo registra.
3. El tablero muestra números a la izquierda y letras abajo, en el orden en que se ven las casillas (se invierten cuando se juega con negras).

Quedan pendientes los demás hallazgos del detalle (ranking vacío, resaltado del rey en jaque, escala de márgenes, tamaño de ventana).

---

## Detalle por pilar

### 1. Textos (3/4)

- Etiquetas y botones específicos: "Jugar", "Ver ranking", "Registrar", "Deshacer", "Rehacer", "Menú". No hay textos genéricos del tipo "Enviar" o "Click aquí".
- Los errores del menú dicen qué hacer: "Registrá el usuario nuevo o elegí otra opción." (`MenuPanel.java:162`) y "Elegí usuarios distintos para blancas y negras." (`MenuPanel.java:167`). Están en voseo, igual que el resto.
- **Estado vacío sin texto:** al sacar el diálogo del ranking se perdió el aviso "Todavía no hay usuarios". Con la base vacía, `RankingPanel` muestra una tabla en blanco (`RankingPanel.java:show`).
- **Mensajes del motor sin traducir al tono de la interfaz:** `"Inválido: " + reason` (`SwingChessView.java:416`) muestra la razón tal como la da el núcleo.
- **Final de partida:** "¡Jaque mate! Ganan Blancas." queda en una línea chica abajo y no dice que se acreditaron puntos ni ofrece "Jugar otra vez".
- "Juego con" es ambiguo; "Mi color" se entiende más rápido.

### 2. Aspecto visual (3/4)

- Cada pantalla tiene un punto focal claro: el título y el botón Jugar grande en el menú, el tablero en la partida, la tabla en el ranking.
- Jerarquía por tamaño y peso: título 38 pt, botón Jugar 18 pt, estado en negrita.
- El menú está agrupado en "Partida" y "Jugadores" con títulos, y los controles quedan alineados.
- **Sin coordenadas** en el tablero (ver corrección 3).
- **El rey en jaque no se marca:** solo cambia el texto de estado (`onCheck`).
- No se marca la última jugada, por lo que cuesta ver qué movió la IA.
- El botón Menú es chico y queda lejos de la vista principal; es aceptable, pero no se destaca como salida.
- Los botones con texto no necesitan tooltip; las casillas son botones sin texto accesible (`button.setText("")`) y sin nombre para lectores de pantalla.

### 3. Color (3/4)

- Paleta de 9 colores fijos en dos archivos: tablero (`LIGHT`, `DARK`), selección (`SELECTED`), destinos (`LEGAL`, `CAPTURE`) y sus marcos (`FRAME`, `CAPTURE_FRAME`), más `ACCENT` y `ERROR` en el menú.
- El ámbar para movimientos libres y el rojizo para capturas se distinguen bien y siguen el mismo criterio que en el README.
- `ACCENT` (`0x4E6B3A`) es un verde distinto de `DARK` (`0x769656`) pero de la misma familia; conviene derivar uno del otro o declarar la paleta en un solo lugar.
- El rojo de error (`0xA32A2A`) contrasta bien sobre el gris claro del fondo.
- El texto usa los colores del tema del sistema, lo que mantiene el contraste.

### 4. Tipografía (3/4)

- Una sola familia (la del sistema) y un solo peso (negrita) sobre el peso normal.
- Tamaños en uso: 38 (título), 26 (título del ranking), 18 (Jugar), 14 (estado) y el tamaño por defecto. Son 5; lo ideal son 4 o menos. Unificar el título del menú y el del ranking en un mismo tamaño.
- Los números del ranking están centrados y el encabezado también, lo que se lee bien.

### 5. Espaciado (3/4)

- Visualmente hay aire suficiente y los grupos se separan claramente.
- Los valores se fijaron uno por uno: márgenes de 2, 4, 6, 8, 10, 12, 14, 16, 20, 22, 24, 40 y 50 (`MenuPanel.java:107-119`, `RankingPanel.java:48`, `SwingChessView.java:178`). No hay una escala común (por ejemplo 4, 8, 16, 24).
- Tamaños de controles fijos: etiqueta 95x28, control 200x28, Jugar 300x44. Funciona porque la ventana tiene tamaño fijo (`8 * SQUARE_SIZE` de ancho); si se redimensiona, el menú no crece con ella.

### 6. Experiencia (2/4)

- **Estados cubiertos:** la IA pensando bloquea Menú, Deshacer, Rehacer y los clicks (`thinking`); Dificultad y Color se deshabilitan en 1 contra 1; el lado de la IA dice "IA" y queda bloqueado; el campo de usuario nuevo aparece solo cuando hace falta; se rechaza elegir el mismo usuario para los dos lados.
- **Acción destructiva sin confirmación:** Menú abandona la partida en curso (corrección 1).
- **Teclado:** sin botón por defecto, sin atajos y sin Enter en el campo de registro (corrección 2).
- **Fin de partida:** no hay "Jugar otra vez"; hay que ir a Menú y volver a elegir todo. Tampoco se muestran los puntos ganados.
- **Ranking vacío:** sin mensaje (ver Textos).
- **Coronación:** abre un diálogo aparte; es la única excepción a la ventana única y está justificada porque el núcleo necesita la respuesta en ese momento.
- **Ventana de tamaño fijo en pantallas chicas:** con `8 * 80 + 150` de alto (790 px) no entra en pantallas de 768 px de alto.

---

## Archivos revisados

- `src/adapters/MenuPanel.java`
- `src/adapters/SwingChessView.java`
- `src/adapters/RankingPanel.java`
- `src/adapters/GameBuilder.java`
- `scripts/SwingIntegrationTests.java`
