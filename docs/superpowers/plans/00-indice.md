# Cierre del TP de ajedrez: índice del plan

> **Para el agente:** este plan se ejecuta con `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Si no tenés superpowers instalado, instalalo antes de hacer cualquier otra cosa (ver `docs/superpowers/specs/2026-10-15-cierre-del-tp-design.md`, sección 4). Los pasos usan casillas `- [ ]` para ir tildando.

**Objetivo:** dejar el TP listo para la entrega del 20/11: desvíos de arquitectura cerrados, tests completos, reglas opcionales (captura al paso, tablas, rehacer), oponente con IA y los entregables.

**Fuente de verdad:** la spec `docs/superpowers/specs/2026-10-15-cierre-del-tp-design.md`. Si algo de este plan la contradice, gana la spec; avisale a Franco.

**Stack:** Java (JDK 21 o superior), solo `javac`/`java`, sin librerías. Python 3 solo para los scripts de apoyo y para generar los `.docx` y `.pptx`.

---

## Antes de empezar

Leé, en este orden: `README.md`, la spec, y este índice. Después ejecutá las fases en orden. Cada fase tiene su archivo.

## Fases

| Fase | Archivo | Qué hace |
|---|---|---|
| 0 | `01-fase0-base.md` | `scripts/build.sh`, `scripts/test.sh`, código de salida de los tests, `AGENTS.md` |
| 1 | `02-fase1-red-de-seguridad.md` | Tests que faltan del alcance obligatorio, contra la API actual |
| 2 | `03-fase2-renombres.md` | Identificadores en inglés, en un solo commit |
| 3 | `04-fase3-arquitectura.md` | `Move`, observers segregados, State con estados terminales, `IChessGame` |
| 4 | `05-fase4-reglas-opcionales.md` | Rehacer, tablas y captura al paso, todo en TDD |
| 5 | `06-fase5-ia.md` | Oponente con IA (3 niveles) en consola y en Swing |
| 6 | `07-fase6-entregables.md` | Documento de justificación, PPTX, lista del UML, README, push |

## Reglas que valen en todas las fases

1. Antes de cada commit: `./scripts/test.sh` en verde (después de la fase 0 existe y sale con código distinto de cero si algo falla).
2. Commits cortos, en español, escritos como los escribe una persona: `git commit -m "tests de la pieza clavada"`. Sin `Co-Authored-By`, sin "generated with", sin emojis, sin prefijos tipo `feat:`.
3. Sin el carácter raya en ningún archivo (ni código, ni comentarios, ni documentos).
4. Comentarios en español, identificadores en inglés. Los textos que ve el usuario quedan en español. **Los paquetes `nucleo`, `puertos`, `adaptadores`, `especiales` y `estrategias` no se renombran** (son el vocabulario de la cátedra). El paquete nuevo de la IA se llama `ai`.
5. En `nucleo/`: nada de streams, `Optional`, threads, reflection ni librerías. Solo lo que se vio en la materia.
6. `nucleo/` nunca importa `adaptadores/` ni `ai/`.
7. Si un test que debería pasar falla, no lo cambies para que pase: usá `superpowers:systematic-debugging`, encontrá la causa y arreglá el código o avisá.
8. Al terminar cada fase pedí revisión con `superpowers:requesting-code-review` y corré `superpowers:verification-before-completion` antes de decir que terminó.

## Estado de partida (para no sorprenderse)

- Repo `Ogstra/tp-ajedrez`, rama `main`, 20 tests pasando con `java -cp out ChessCoreTests`.
- `ChessCoreTests.java` es un runner propio (sin JUnit): cada test es un método `private static void testXxx()` con comentarios `// Arrange`, `// Act`, `// Assert`, y se registra en `main`. `assertTrue(String label, boolean condition, String detail)` imprime `[OK]` o `[FAIL]`.
- Helpers del test que ya existen: `at("e2")`, `piece(PieceType, Color)`, `newGame(board, observers...)`, `PIECES`, `AlwaysQueenChooser` (stub), `RecordingObserver` (mock).
- El juego se arma en `Main` (consola) y `MainSwing` (ventana). `ChessGame` recibe por constructor el tablero, los observers y las reglas especiales.
- Las imágenes de las piezas están en `src/assets/piezas/` y se leen con ruta relativa `src/assets/piezas/`: los programas se corren desde la raíz del repo.

## Resultado esperado al final

- `./scripts/test.sh` en verde con los tests nuevos de cada fase.
- Consola y ventana jugables con enroque, coronación, captura al paso, rehacer, tablas y tres niveles de IA.
- `docs/justificacion.md`, `docs/justificacion.docx`, `docs/presentacion.pptx` y `docs/uml-checklist.md` commiteados.
- Todo subido a `origin/main`.
