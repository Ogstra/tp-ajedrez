# Fase 2: identificadores en inglés

> **Para el agente:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Pasos con casillas `- [ ]`.

**Goal:** que todos los identificadores (clases, métodos, campos, variables, constantes y métodos de test) estén en inglés. Comentarios y textos para el usuario quedan en español.

**Architecture:** es un renombre puro: cero cambios de comportamiento. Va en un solo commit, con los tests en verde antes y después.

**Tech Stack:** Python 3 (solo para los dos scripts de apoyo), Java.

**No se renombran:** los paquetes `nucleo`, `puertos`, `adaptadores`, `especiales`, `estrategias` (vocabulario de la cátedra), ni las líneas `package` e `import`.

---

### Task 2.1: scripts de apoyo

**Files:**
- Create: `scripts/find_spanish_identifiers.py`
- Create: `scripts/rename_identifier.py`

- [ ] **Step 1: Crear `scripts/find_spanish_identifiers.py`**

```python
#!/usr/bin/env python3
"""Lista los identificadores en español de los .java de src/.

Ignora comentarios, textos entre comillas y las líneas package/import.
Un identificador se marca si alguna de sus palabras (separadas por camelCase
o guion bajo) está en la lista SPANISH. Sale con código 1 si encuentra alguno.
"""
import pathlib
import re
import sys

SPANISH = {
    "fila", "filas", "columna", "columnas", "casilla", "casillas", "pieza", "piezas",
    "tablero", "jugada", "jugadas", "turno", "destino", "destinos", "origen", "enroque",
    "coronacion", "peon", "caballo", "torre", "alfil", "reina", "rey", "paso", "pasos",
    "salto", "saltos", "estado", "seleccion", "tamano", "clara", "oscura", "claro",
    "oscuro", "nombre", "nombres", "simbolo", "simbolos", "letra", "letras", "inicial",
    "captura", "entrada", "movimiento", "movimientos", "parsear", "armar", "aplicar",
    "como", "hay", "deshacer", "blanca", "blancas", "blanco", "negra", "negras", "negro",
    "jaque", "mate", "ahogado", "oponente", "tiene", "legales", "candidatos", "simulado",
    "limpio", "corte", "boton", "imagen", "imagenes", "clave", "mapa", "carpeta",
    "opciones", "eleccion", "abajo", "cargar", "definir", "direcciones", "deslizante",
    "dos", "interfaz", "casilleros", "elegida", "vacia", "vacio", "propia",
    "atacada", "atacado", "movido", "tocada", "tocado", "derecho", "algebraico",
    "texto", "linea", "lineas", "ventana", "partida", "juego", "regla", "reglas",
    "resultado", "intento", "hacia", "comando", "ultima", "ultimo", "antes", "despues",
    "jugador", "pasar", "saltar", "estaba", "fueron", "cada",
}
ALLOWED = {"nucleo", "puertos", "adaptadores", "especiales", "estrategias"}

STRING = re.compile(r'"(?:\\.|[^"\\])*"')
CHAR = re.compile(r"'(?:\\.|[^'\\])'")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT = re.compile(r"//.*")
IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
WORDS = re.compile(r"[A-Z]?[a-z0-9]+|[A-Z]+(?![a-z])")


def clean(text):
    text = STRING.sub('""', text)
    text = CHAR.sub("''", text)
    text = BLOCK_COMMENT.sub(lambda m: "\n" * m.group(0).count("\n"), text)
    return LINE_COMMENT.sub("", text)


def main():
    found = 0
    for path in sorted(pathlib.Path("src").rglob("*.java")):
        text = clean(path.read_text(encoding="utf-8"))
        for number, line in enumerate(text.splitlines(), 1):
            stripped = line.strip()
            if stripped.startswith("package ") or stripped.startswith("import "):
                continue
            for identifier in IDENT.findall(line):
                if identifier in ALLOWED:
                    continue
                for word in WORDS.findall(identifier):
                    if word.lower() in SPANISH:
                        print(f"{path}:{number}: {identifier} (palabra: {word.lower()})")
                        found += 1
                        break
    print(f"{found} identificadores en español")
    sys.exit(1 if found else 0)


main()
```

- [ ] **Step 2: Crear `scripts/rename_identifier.py`**

```python
#!/usr/bin/env python3
"""Uso: python3 scripts/rename_identifier.py VIEJO NUEVO

Reemplaza VIEJO por NUEVO como palabra completa en todos los .java de src/,
excepto en las líneas package e import. No toca textos entre comillas ni
comentarios: usa el reemplazo solo en código.
"""
import pathlib
import re
import sys

STRING = re.compile(r'"(?:\\.|[^"\\])*"')

if len(sys.argv) != 3:
    sys.exit("uso: rename_identifier.py VIEJO NUEVO")
old, new = sys.argv[1], sys.argv[2]
pattern = re.compile(r"\b" + re.escape(old) + r"\b")
total = 0

for path in pathlib.Path("src").rglob("*.java"):
    lines = path.read_text(encoding="utf-8").split("\n")
    changed = False
    for index, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("package ") or stripped.startswith("import "):
            continue
        code, _, comment = line.partition("//")
        # separa los textos entre comillas para no tocarlos
        parts = STRING.split(code)
        strings = STRING.findall(code)
        replaced = [pattern.sub(new, part) for part in parts]
        rebuilt = replaced[0]
        for i, s in enumerate(strings):
            rebuilt += s + replaced[i + 1]
        if rebuilt != code:
            lines[index] = rebuilt + (("//" + comment) if comment or "//" in line else "")
            total += 1
            changed = True
    if changed:
        path.write_text("\n".join(lines), encoding="utf-8")
print(f"{total} líneas cambiadas")
```

- [ ] **Step 3: Probar el buscador**

Run: `python3 scripts/find_spanish_identifiers.py | tail -3`
Expected: una lista larga y la última línea `NN identificadores en español` con NN mayor a cero.

- [ ] **Step 4: Commit de los scripts**

```bash
git add scripts/find_spanish_identifiers.py scripts/rename_identifier.py
git commit -m "scripts para encontrar y renombrar identificadores en español"
```

---

### Task 2.2: renombrar

**Files:**
- Modify: todos los `.java` de `src/`
- Rename: `src/nucleo/estrategias/MovimientoDeslizante.java` a `SlidingMovement.java`

**Método:** repetir "buscar, renombrar de a un identificador, compilar" hasta que el buscador no marque nada. Mantené los tests en verde: después de cada tanda corré `./scripts/test.sh`.

- [ ] **Step 1: Renombrar la clase y su archivo**

```bash
git mv src/nucleo/estrategias/MovimientoDeslizante.java src/nucleo/estrategias/SlidingMovement.java
python3 scripts/rename_identifier.py MovimientoDeslizante SlidingMovement
python3 scripts/rename_identifier.py enDirecciones inDirections
./scripts/test.sh
```

Expected: `36 pasados, 0 fallados.`

- [ ] **Step 2: Renombrar los identificadores ya conocidos**

Cada fila es un comando `python3 scripts/rename_identifier.py VIEJO NUEVO`. Hacelos en tandas por archivo y corré `./scripts/test.sh` entre tandas.

Estrategias y `SlidingMovement`: `comoTorre` a `asRook`, `comoAlfil` a `asBishop`, `comoCaballo` a `asKnight`, `DIRECCIONES` a `DIRECTIONS`, `SALTOS` a `JUMPS`, `direcciones` a `directions`, `unPaso` a `oneStep`, `dosPasos` a `twoSteps`, `filaInicial` a `startRow`, `destino` a `destination`, `salto` a `jump`, `fila` a `row`, `diagonal` queda igual.

`ChessGame`: `turnoActual` a `currentTurn`, `oponente` a `opponent`, `jaque` a `check`, `tieneMovimientos` a `hasMoves`, `candidatos` a `candidates`, `legales` a `legal`, `simulado` a `simulated`.

`ConsoleRenderer` y `ConsolePromotionChooser`: `letras` a `letters`, `nombre` a `name`, `inicial` a `initial`, `blanca` a `isWhite`, `captura` a `captureText`, `eleccion` a `choice`.

`Main`: `entrada` a `input`, `movimiento` a `move`, `parsearMovimiento` a `parseMove`, `limpio` a `clean`, `corte` a `cut`.

`SwingChessView`: `TAMANO_CASILLA` a `SQUARE_SIZE`, `TAMANO_PIEZA` a `PIECE_SIZE`, `CLARA` a `LIGHT`, `OSCURA` a `DARK`, `SELECCIONADA` a `SELECTED`, `IMAGENES` a `IMAGES`, `LETRAS` a `LETTERS`, `SIMBOLOS_BLANCOS` a `WHITE_SYMBOLS`, `SIMBOLOS_NEGROS` a `BLACK_SYMBOLS`, `NOMBRES` a `NAMES`, `definir` a `define`, `filas` a `rows`, `columnas` a `columns`, `casillas` a `squares`, `estado` a `status`, `seleccion` a `selection`, `cargarImagenes` a `loadImages`, `armarInterfaz` a `buildInterface`, `colorDeCasilla` a `squareColor`, `onCasillaClickeada` a `onSquareClicked`, `origen` a `origin`, `aplicarIcono` a `applyIcon`, `boton` a `button`, `pieza` a `piece`, `imagen` a `image`, `claveImagen` a `imageKey`, `letra` a `letter`, `simbolo` a `symbol`, `opciones` a `options`, `deshacer` a `undoButton`, `abajo` a `bottom`, `tablero` a `boardPanel`, `carpeta` a `folder`, `clave` a `key`, `mapa` a `map`.

`ChessCoreTests`: renombrar los 20 métodos de test viejos:

| Viejo | Nuevo |
|---|---|
| `testPeonAvanzaUnPaso` | `testPawnAdvancesOneSquare` |
| `testPeonNoPuedeCapturarDeFrente` | `testPawnCannotCaptureStraightAhead` |
| `testCaballoSaltaEnL` | `testKnightJumpsInL` |
| `testTorreNoAtraviesaPiezas` | `testRookCannotJumpOverPieces` |
| `testReyNoPuedeMoverseACasillaAtacada` | `testKingCannotMoveToAttackedSquare` |
| `testNegrasNoPuedenMoverPrimero` | `testBlackCannotMoveFirst` |
| `testJaqueSeDetectaYNoEsMate` | `testCheckIsDetectedAndIsNotMate` |
| `testMoveCommandUndoRestauraElTablero` | `testMoveCommandUndoRestoresBoard` |
| `testJaqueMateDelLoco` | `testFoolsMate` |
| `testEnroqueCortoMueveReyYTorre` | `testShortCastlingMovesKingAndRook` |
| `testEnroqueLargoMueveReyYTorre` | `testLongCastlingMovesKingAndRook` |
| `testNoSePuedeEnrocarSiElReyYaSeMovio` | `testCannotCastleIfKingAlreadyMoved` |
| `testNoSePuedeEnrocarPorCasillaAtacadaPorPeon` | `testCannotCastleThroughSquareAttackedByPawn` |
| `testDeshacerRecuperaElDerechoDeEnroque` | `testUndoRestoresCastlingRight` |
| `testPeonCoronaAReina` | `testPawnPromotesToQueen` |
| `testPiezaNuevaSinTocarClasesExistentes` | `testNewPieceWithoutTouchingExistingClasses` |
| `testTableroDeOtroTamano` | `testBoardOfAnotherSize` |
| `testCoronacionEnTableroChico` | `testPromotionOnSmallBoard` |
| `testObserverRecibeSoloEnroque` | `testObserverReceivesOnlyCastling` |
| `testObserverRecibeMovimientoYCoronacion` | `testObserverReceivesMoveAndPromotion` |

Helpers del test: `tableroParaEnrocar` a `castlingBoard`, `hay` a `hasPiece`, `tableroAntes` a `boardBefore`, `ChancellorMovementStrategy` ya está en inglés.

- [ ] **Step 3: Pasar el buscador y renombrar lo que falte**

Run: `python3 scripts/find_spanish_identifiers.py`

Por cada línea que marque, elegí un nombre en inglés claro y usá `rename_identifier.py`. Repetí hasta que imprima `0 identificadores en español` y salga con código 0.

Cuidado con falsos positivos: si una palabra marcada es una palabra inglesa válida (por ejemplo `no` o `con` dentro de un nombre), no la renombres: agregá la palabra a la lista de excepciones `ALLOWED` del script y dejá un comentario en el commit.

- [ ] **Step 4: Alinear los rótulos de los tests (opcional pero recomendado)**

Los textos del primer argumento de `assertTrue` de los 20 tests viejos (por ejemplo `"Peon_AvanzaUnPaso"`) quedan como están o se cambian al nombre inglés del método. Elegí una de las dos y aplicala a todos por igual.

- [ ] **Step 5: Verificar que no cambió el comportamiento**

Run: `./scripts/test.sh`
Expected: `36 pasados, 0 fallados.`

Run: `java -cp out Main < /dev/null | head -12`
Expected: el tablero impreso con las piezas en su lugar (no tiene que fallar ni lanzar excepciones).

- [ ] **Step 6: Verificar que ninguna línea nueva tiene la raya y que los paquetes no cambiaron**

Run: `grep -rn $'\xe2\x80\x94' src | wc -l`
Expected: `0`

Run: `ls src`
Expected: `ChessCoreTests.java  Main.java  MainSwing.java  adaptadores  assets  nucleo  puertos`

- [ ] **Step 7: Commit único**

```bash
git add -A
git commit -m "renombro los identificadores a inglés"
```

---

### Cierre de la fase 2

- [ ] `python3 scripts/find_spanish_identifiers.py` termina con `0 identificadores en español`.
- [ ] `./scripts/test.sh` termina con `36 pasados, 0 fallados.`
- [ ] `git push`.
