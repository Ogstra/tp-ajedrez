# Fase 6: entregables y cierre

> **Para el agente:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Pasos con casillas `- [ ]`.

**Goal:** dejar listos los entregables que se describen en el TPO (documento de justificación, PPTX) más la lista de clases para que Franco dibuje el UML y el README final, y subir todo a GitHub.

**Architecture:** esta fase no cambia código de producción. Genera documentos a partir del código ya terminado, así que se hace al final.

**Tech Stack:** Python 3 con `python-docx` y `python-pptx` (`pip install python-docx python-pptx`), Markdown.

**Antes de empezar:** fases 0 a 5 terminadas, `./scripts/test.sh` con `61 pasados, 0 fallados.` Todos los textos de esta fase hablan del diseño final: si encontrás algo que ya no coincide con el código, corregí el texto, no el código.

**Estilo de los documentos (regla de Franco):** formato académico sobrio. Times New Roman 12 en negro para el `.docx`, Calibri en negro y fondo blanco para el `.pptx`, sin colores de acento, sin barras decorativas, sin emojis. Nada de la raya larga.

---

### Task 6.1: captura de la ventana

**Files:**
- Create: `scripts/Capture.java`
- Modify: `docs/captura.png`

- [ ] **Step 1: Crear `scripts/Capture.java`**

```java
import adaptadores.SwingChessView;
import nucleo.Board;
import nucleo.BoardSetup;
import nucleo.ChessGame;
import nucleo.IChessGame;
import nucleo.IDrawRule;
import nucleo.ISpecialMove;
import nucleo.PieceFactory;
import nucleo.Position;
import nucleo.especiales.CastlingMove;
import nucleo.especiales.EnPassantMove;
import nucleo.especiales.PromotionMove;
import puertos.IGameStatusListener;
import puertos.IMoveListener;

import javax.imageio.ImageIO;
import javax.swing.JRootPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Dibuja la ventana real fuera de pantalla, con una partida a medias, y la guarda como PNG.
// Uso (desde la raíz del repo): java -cp out:/tmp/capture-out Capture docs/captura.png
public class Capture {
    public static void main(String[] args) throws Exception {
        PieceFactory pieces = PieceFactory.standard();
        Board board = BoardSetup.standard(pieces);
        SwingChessView view = new SwingChessView(board.getRows(), board.getColumns());
        List<ISpecialMove> specialMoves = Arrays.<ISpecialMove>asList(
            new CastlingMove(), new EnPassantMove(), new PromotionMove(pieces, view));
        IChessGame game = new ChessGame(board,
            Collections.<IMoveListener>singletonList(view),
            Collections.<IGameStatusListener>singletonList(view),
            specialMoves,
            new ArrayList<IDrawRule>());
        view.attachGame(game);

        String[][] moves = {{"e2", "e4"}, {"e7", "e5"}, {"g1", "f3"}, {"b8", "c6"}, {"f1", "c4"}, {"g8", "f6"}};
        for (String[] move : moves) {
            game.tryMove(Position.fromAlgebraic(move[0]), Position.fromAlgebraic(move[1]));
        }
        view.render(board);

        view.addNotify();
        view.setSize(640, 750);
        view.validate();
        layout(view.getRootPane());
        JRootPane root = view.getRootPane();
        BufferedImage image = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        root.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", new File(args[0]));
        System.exit(0);
    }

    private static void layout(Component component) {
        if (component instanceof Container) {
            ((Container) component).doLayout();
            for (Component child : ((Container) component).getComponents()) layout(child);
        }
    }
}
```

- [ ] **Step 2: Generar la captura**

```bash
./scripts/build.sh
mkdir -p /tmp/capture-out
javac -d /tmp/capture-out -cp out scripts/Capture.java
java -cp out:/tmp/capture-out Capture docs/captura.png
```

Expected: se crea `docs/captura.png` sin excepciones.

- [ ] **Step 3: Mirar la imagen**

Abrir `docs/captura.png` y comprobar que se ven el desplegable de oponente arriba, el tablero con las piezas grandes de la apertura italiana, y abajo la barra de estado con los botones Deshacer y Rehacer. Si se ve cortada, subir el alto de `setSize` en `Capture.java`.

- [ ] **Step 4: Commit**

```bash
git add scripts/Capture.java docs/captura.png
git commit -m "captura nueva de la ventana"
```

---

### Task 6.2: lista de clases para el UML

**Files:**
- Create: `scripts/list_types.py`
- Create: `docs/uml-checklist.md` (generado)

- [ ] **Step 1: Crear `scripts/list_types.py`**

```python
#!/usr/bin/env python3
"""Genera docs/uml-checklist.md a partir del código de src/.

Lista, por paquete, cada clase, interfaz o enum con: lo que implementa o
extiende, los campos cuyo tipo es del proyecto (asociaciones) y los otros
tipos del proyecto que usa (dependencias). Al final, qué paquetes importan
a cuáles. Es la lista para dibujar el UML: el diagrama tiene que coincidir
con esto, y esto sale del código.
"""
import pathlib
import re

SRC = pathlib.Path("src")
OUT = pathlib.Path("docs/uml-checklist.md")
DECL = re.compile(r"^(?:public\s+)?(?:(?:final|abstract)\s+)*(class|interface|enum)\s+(\w+)([^{]*)\{", re.M)
FIELD = re.compile(r"^    (?:(?:private|protected|public|static|final)\s+)*([A-Z]\w*)(?:<([A-Z]\w*)>)?(?:\[\])?\s+\w+\s*(?:=|;)", re.M)
PACKAGE = re.compile(r"^package\s+([\w.]+);", re.M)
IMPORT = re.compile(r"^import\s+([\w.]+)\.\w+;", re.M)
SKIP_FILES = {"ChessCoreTests.java"}


def names(text):
    text = re.sub(r"<[^>]*>", "", text)
    return [part.strip() for part in text.split(",") if part.strip()]


def main():
    files = [p for p in sorted(SRC.rglob("*.java")) if p.name not in SKIP_FILES]
    parsed = []
    for path in files:
        text = path.read_text(encoding="utf-8")
        package_match = PACKAGE.search(text)
        package = package_match.group(1) if package_match else "(raíz, sin paquete)"
        match = DECL.search(text)
        if not match:
            continue
        kind, name, rest = match.group(1), match.group(2), match.group(3)
        extends = re.search(r"extends\s+(.*?)(?=\s+implements|$)", rest.strip())
        implements = re.search(r"implements\s+(.*)$", rest.strip())
        parsed.append({
            "package": package, "kind": kind, "name": name, "text": text,
            "extends": names(extends.group(1)) if extends else [],
            "implements": names(implements.group(1)) if implements else [],
            "imports": sorted(set(IMPORT.findall(text))),
        })

    project_types = {item["name"] for item in parsed}
    lines = [
        "# Lista para dibujar el UML",
        "",
        "Generada con `python3 scripts/list_types.py` a partir del código. Si el código cambia, se vuelve a generar y el UML se revisa contra esta lista.",
        "",
        "Cómo leerla: **Implementa** es una flecha de realización (línea punteada con triángulo vacío); **Extiende** es herencia; **Tiene** son las asociaciones por campo (el diamante o la flecha simple, según si es parte de o solo referencia); **Usa** son dependencias (flecha punteada).",
        "",
    ]
    by_package = {}
    for item in parsed:
        by_package.setdefault(item["package"], []).append(item)

    for package in sorted(by_package):
        lines += ["## Paquete `" + package + "`", ""]
        for item in sorted(by_package[package], key=lambda i: i["name"]):
            body = item["text"]
            fields = []
            for match in FIELD.finditer(body):
                for type_name in (match.group(1), match.group(2)):
                    if type_name and type_name in project_types and type_name != item["name"]:
                        fields.append(type_name)
            words = set(re.findall(r"\b[A-Z]\w*\b", body))
            related = set(item["extends"]) | set(item["implements"]) | set(fields)
            uses = sorted(w for w in words if w in project_types and w != item["name"] and w not in related)
            lines.append("### " + item["name"] + " (" + item["kind"] + ")")
            if item["extends"]:
                lines.append("- Extiende: " + ", ".join(item["extends"]))
            if item["implements"]:
                lines.append("- Implementa: " + ", ".join(item["implements"]))
            if fields:
                lines.append("- Tiene: " + ", ".join(sorted(set(fields))))
            if uses:
                lines.append("- Usa: " + ", ".join(uses))
            lines.append("")

    lines += ["## Dependencias entre paquetes", ""]
    lines += ["| Paquete | Importa de |", "|---|---|"]
    for package in sorted(by_package):
        imported = set()
        for item in by_package[package]:
            for imp in item["imports"]:
                if imp in {i["package"] for i in parsed} and imp != package:
                    imported.add(imp)
        lines.append("| `" + package + "` | " + (", ".join("`" + i + "`" for i in sorted(imported)) or "nada del proyecto") + " |")
    lines.append("")
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("escrito " + str(OUT) + " con " + str(len(parsed)) + " tipos")


main()
```

- [ ] **Step 2: Generarla**

Run: `python3 scripts/list_types.py`
Expected: `escrito docs/uml-checklist.md con NN tipos` (alrededor de 70).

- [ ] **Step 3: Revisarla contra el código**

Abrir `docs/uml-checklist.md` y comprobar a ojo estos puntos (si alguno falla, corregir el script y volver a generar):

- `ChessGame` implementa `IChessGame` e `IGameContext`, y tiene `Board`, `IMoveListener`, `IGameStatusListener`, `ISpecialMove`, `IDrawRule`, `HistoryEntry`, `IGameState`.
- `Piece` tiene `PieceType`, `Color` e `IMovementStrategy`.
- `SwingChessView` implementa `IMoveListener`, `IGameStatusListener` e `IPromotionChooser`.
- La tabla de dependencias entre paquetes muestra que `nucleo` solo importa de `puertos` (y de `nucleo.*` internos), y nunca de `adaptadores` ni de `ai`.

- [ ] **Step 4: Agregar al final del archivo esta sección a mano (con el editor, no con el script)**

Agregar al final de `docs/uml-checklist.md`:

```markdown
## Dónde está cada patrón (para marcarlo en el diagrama)

- Strategy: `IMovementStrategy` y sus seis implementaciones; `ISpecialMove` (`CastlingMove`, `EnPassantMove`, `PromotionMove`); `IDrawRule` (`InsufficientMaterialRule`, `FiftyMoveRule`, `ThreefoldRepetitionRule`); `IOpponent` (`RandomOpponent`, `GreedyOpponent`, `MinimaxOpponent`).
- Factory: `PieceFactory`, `BoardSetup`, `Opponents`.
- Command: `ICommand`, `IMoveCommand`, `MoveCommand`, `CastlingCommand`, `PromotionCommand`, `EnPassantCommand`.
- State: `IGameState`, `WhiteTurnState`, `BlackTurnState`, `CheckmateState`, `StalemateState`, `DrawState`.
- Observer: `IMoveListener` e `IGameStatusListener`, implementadas por `ConsoleRenderer` y `SwingChessView`.
- Puertos: de entrada `IChessGame`; de salida `IMoveListener`, `IGameStatusListener`, `IPromotionChooser`.
- Composition roots: `Main` y `MainSwing`.
```

- [ ] **Step 5: Commit**

```bash
git add scripts/list_types.py docs/uml-checklist.md
git commit -m "lista de clases para el uml"
```

---

### Task 6.3: documento de justificación (.md)

**Files:**
- Create: `docs/justificacion.md`

- [ ] **Step 1: Crear `docs/justificacion.md` con este contenido**

Es la base. Si algo no coincide con el código final, corregilo; el formato es **Qué / Por qué / Cuándo se rompería** y habla de **decisiones propias del grupo**, no de las reglas de la cátedra.

````markdown
# Justificación de decisiones de diseño

TPO de Ingeniería de Software (UADE): implementación de un ajedrez.

Cada decisión está explicada en tres partes: qué se decidió, por qué, y en qué condiciones se rompería esa decisión (cuándo dejaría de convenir). Al final se listan los patrones que no se usaron y por qué.

## 1. El núcleo no conoce a los adaptadores

**Qué.** Las reglas del ajedrez viven en el paquete `nucleo`, que no importa nada de `adaptadores` ni de `ai`. La consola, la ventana y la IA son piezas externas que dependen del núcleo, nunca al revés.

**Por qué.** La prueba es una sola pregunta: si cambia la tecnología, ¿esto cambia? Las reglas no cambian si pasamos de consola a ventana, por eso son núcleo. Gracias a esto la ventana Swing se agregó después sin tocar `ChessGame`, y el núcleo se prueba sin levantar ninguna infraestructura.

**Cuándo se rompería.** Si el sistema fuera un prototipo descartable de pocos días, separar en paquetes sería sobre-ingeniería. Acá el sistema tiene que crecer (más reglas, otras interfaces) y se defiende oralmente, así que la frontera paga.

## 2. Un puerto de entrada y puertos de salida chicos

**Qué.** El mundo exterior le habla al núcleo por una sola interfaz, `IChessGame`. El núcleo le habla al exterior por tres puertos de salida: `IMoveListener` (lo que pasa con las piezas), `IGameStatusListener` (el estado de la partida) e `IPromotionChooser` (a qué pieza se corona).

**Por qué.** Hay un solo puerto de entrada aunque haya dos adaptadores de entrada (teclado y mouse) y la IA; y un puerto de salida por cada cosa distinta que el núcleo necesita del exterior. El observer original tenía ocho métodos mezclados: se partió en dos para que quien solo quiere saber de movimientos no tenga que implementar el estado de la partida (Interface Segregation).

**Cuándo se rompería.** Si ningún adaptador necesitara solo una de las dos mitades, volver a una sola interfaz sería más simple. Mientras haya clientes que usan una sola parte, separar conviene.

## 3. Inyección de dependencias por constructor

**Qué.** `ChessGame` recibe el tablero, los listeners, las reglas especiales y las reglas de tablas ya armadas, por constructor. Solo `Main` y `MainSwing` crean y conectan todo.

**Por qué.** Es el nivel 4 de inyección. Permite probar el núcleo con dobles de prueba (un stub que siempre corona a reina, un mock que registra los avisos) y cambiar el comportamiento sin editar `ChessGame`. Cuatro colaboradores inyectados no son un problema de responsabilidad: cada uno es un colaborador externo distinto.

**Cuándo se rompería.** Si el constructor creciera con parámetros que son configuración y no colaboradores, convendría agruparlos en un objeto de configuración. Y para algo tan estable como un `String` no hace falta abstraer.

## 4. Una sola clase `Piece` que compone una estrategia de movimiento

**Qué.** No hay una subclase por pieza. `Piece` tiene un tipo, un color y una `IMovementStrategy`. Cada pieza tiene su estrategia, y la reina compone las de la torre y el alfil.

**Por qué.** Con herencia, una pieza nueva era una rama nueva del árbol y la reina tendría que heredar de dos clases. Con composición, una pieza nueva es una estrategia nueva y la reina reutiliza lo que ya existe sin heredar.

**Cuándo se rompería.** Si las piezas compartieran mucho estado propio (puntos de vida, historial propio), una jerarquía corta y estable podría ser más clara. Acá una pieza es un valor inmutable con un comportamiento.

## 5. `PieceType` es una clase de valor y `PieceFactory` es un registro

**Qué.** El tipo de pieza dejó de ser un enum: es una clase de valor con igualdad por nombre. `PieceFactory` es un registro `tipo -> estrategia` al que se le suman piezas con `register()`.

**Por qué.** Un enum es un conjunto cerrado: agregar una pieza obligaba a editar el enum y un `switch`, o sea a modificar clases existentes. Con esta decisión una pieza nueva (hay un test con un "Chancellor" que se mueve como torre y caballo) se agrega sin tocar nada.

**Cuándo se rompería.** Si el juego tuviera un conjunto fijo y cerrado de piezas para siempre, un enum sería más simple y el compilador avisaría si falta un caso. Se eligió abierto porque la consigna evalúa justamente la escalabilidad.

## 6. El tamaño del tablero es un dato

**Qué.** `Board(filas, columnas)` conoce su tamaño y todos preguntan `board.isInside()`. `BoardSetup` arma la posición inicial para cualquier tamaño. El peón arranca en la anteúltima fila y corona en la última de ese tablero.

**Por qué.** Antes el 8 estaba repartido por varias clases. Con el tamaño como dato, cambiar de tablero es una llamada distinta (hay tests con 10x10 y 5x5) y no un cambio de código.

**Cuándo se rompería.** Si el tablero fuera siempre 8x8 y nunca se pidiera otro, una constante alcanzaría. Se prefirió el dato porque la defensa individual puede pedir otro tamaño.

## 7. Command con historial para deshacer y rehacer

**Qué.** Cada jugada es un comando con `execute` y `undo`: `MoveCommand`, `CastlingCommand`, `PromotionCommand` y `EnPassantCommand`. `ChessGame` guarda un historial y una pila de rehacer.

**Por qué.** El comando guarda lo necesario para revertir la jugada (la pieza capturada, la torre del enroque, el peón de la coronación). Deshacer y rehacer salen del historial sin código especial por tipo de jugada. También sirve para saber qué casillas se tocaron.

**Cuándo se rompería.** Si no hiciera falta deshacer, llamar al tablero directamente sería más simple y el patrón sería ceremonia de más.

## 8. Las jugadas especiales son reglas enchufables

**Qué.** El enroque, la captura al paso y la coronación implementan `ISpecialMove`. `ChessGame` las recibe por constructor y no sabe cuáles son: cada una declara qué destinos extra habilita y qué comando arma.

**Por qué.** Antes estaban como `if` dentro de `ChessGame`, con un `instanceof`. Agregar una regla nueva obligaba a modificarlo. Ahora es una clase nueva y una línea en el composition root (Open/Closed).

**Cuándo se rompería.** Con una sola excepción a las reglas, un `if` sería más directo. Con tres y la posibilidad de más, la estrategia compensa.

## 9. State con estados terminales

**Qué.** El turno es un estado (`WhiteTurnState`, `BlackTurnState`) y el fin de la partida también (`CheckmateState`, `StalemateState`, `DrawState`). No hay un booleano `gameOver`.

**Por qué.** Antes el patrón era dos estados triviales y un booleano aparte, es decir, el patrón usado a medias. Con los estados terminales el patrón resuelve un problema real: de un estado terminal no se sale jugando, y deshacer vuelve al estado anterior sin trucos.

**Cuándo se rompería.** Con solo dos estados estables un enum y un `switch` serían más claros. Se justifica por los estados terminales, que cambian el comportamiento de las jugadas.

## 9 bis. Dónde vive la validación de jugadas

**Qué.** La estrategia de cada pieza dice a dónde podría moverse según sus reglas (jugadas pseudo-legales). `ChessGame` filtra las que dejarían al propio rey en jaque, porque es el único que conoce el estado completo de la partida.

**Por qué.** Poner la validación completa dentro de cada pieza obligaría a que cada una conozca al rey y al resto del tablero, y a recalcular constantemente. Separar "cómo se mueve" de "es legal en esta partida" mantiene a las piezas simples.

**Cuándo se rompería.** Si el costo de simular cada jugada sobre una copia del tablero fuera un problema medido, habría que optimizar esa parte (por ejemplo, detectando clavadas), pero solo después de medir.

## 10. Las tablas son reglas inyectadas y el derecho de enroque sale del historial

**Qué.** Las condiciones de tablas (`InsufficientMaterialRule`, `FiftyMoveRule`, `ThreefoldRepetitionRule`) implementan `IDrawRule` y se inyectan. Para el enroque, que el rey y la torre "no se movieron" se calcula con las casillas tocadas del historial, sin banderas.

**Por qué.** Una regla de tablas nueva es una clase nueva. Y al no guardar banderas sueltas, deshacer devuelve el derecho de enroque solo, que es la fuente típica de errores.

**Cuándo se rompería.** Si el historial fuera enorme y consultarlo en cada jugada pesara, habría que guardar el dato calculado. Con partidas de ajedrez no es el caso.

## 11. La IA vive afuera del núcleo

**Qué.** El oponente está en el paquete `ai`: una interfaz `IOpponent` con tres niveles (azar, captura codiciosa y minimax de profundidad 2). Piensa sobre una partida de búsqueda propia y juega su jugada por `IChessGame`.

**Por qué.** No es parte de las reglas del juego, así que no puede ensuciar el núcleo. Como solo conoce la interfaz del puerto de entrada, se agregó sin modificar ninguna clase del núcleo, y no abre diálogos ni toca la pantalla mientras piensa.

**Cuándo se rompería.** Si hiciera falta una IA fuerte, el minimax simple no alcanza (haría falta poda y una mejor evaluación), y probablemente convendría un proceso externo detrás de otro puerto.

## 12. Tests del núcleo con un runner propio

**Qué.** Los tests están en `ChessCoreTests`, con formato Arrange-Act-Assert, un stub (`AlwaysQueenChooser`), un mock (`RecordingObserver`) y los marcados con `// TDD:` escritos antes que el código. Se corren con `./scripts/test.sh` y no usan librerías.

**Por qué.** Corren solo con `javac`, sin infraestructura, que es lo que pide la consigna, y no hay nada para instalar ni explicar en la defensa.

**Cuándo se rompería.** En un proyecto real con un equipo grande convendría JUnit, por el reporte, los filtros y la integración con las herramientas.

## 13. Simplificaciones declaradas

- La repetición de posición ignora los derechos de enroque y de captura al paso.
- La captura al paso no detecta el jaque descubierto horizontal que a veces la vuelve ilegal.
- La IA no conoce el historial de la partida, así que puede subestimar o sobreestimar el enroque. Las reglas reales las aplica siempre la partida verdadera.

## Patrones que no se usaron

- **Singleton**: no hay nada que deba existir una sola vez; sería estado global sin necesidad.
- **Service Locator**: se prefirió inyectar por constructor, que deja explícitas las dependencias.
- **Facade**: `IChessGame` ya es la entrada única; otra capa encima no agregaría nada.
- **Object Pool**: no hay un problema de rendimiento medido que lo justifique.
- **Decorator**: ninguna combinación de comportamientos lo pidió.
````

- [ ] **Step 2: Comprobar que no tiene la raya**

Run: `grep -c $'\xe2\x80\x94' docs/justificacion.md`
Expected: `0`

- [ ] **Step 3: Contrastar cada afirmación con el código**

Para cada sección, abrir los archivos citados y confirmar que lo que dice el texto es verdad (nombres de clases, tests que se mencionan, tamaños probados). Corregir el texto si algo cambió. Contá los tests reales con `./scripts/test.sh | tail -2` y, si el documento menciona una cantidad, que coincida.

- [ ] **Step 4: Commit**

```bash
git add docs/justificacion.md
git commit -m "documento de justificación de las decisiones"
```

---

### Task 6.4: versión Word del documento

**Files:**
- Create: `scripts/md_to_docx.py`
- Create: `docs/justificacion.docx` (generado)

- [ ] **Step 1: Instalar la dependencia**

Run: `pip install python-docx`

- [ ] **Step 2: Crear `scripts/md_to_docx.py`**

```python
#!/usr/bin/env python3
"""Convierte un .md sencillo a .docx con formato académico: Times New Roman, negro.

Soporta títulos (#, ##, ###), párrafos, listas con "-", bloques de código entre
tres comillas invertidas, **negrita** y `código` en línea.
Uso: python3 scripts/md_to_docx.py docs/justificacion.md docs/justificacion.docx
"""
import re
import sys

from docx import Document
from docx.oxml.ns import qn
from docx.shared import Pt, RGBColor

BLACK = RGBColor(0, 0, 0)
INLINE = re.compile(r"(\*\*[^*]+\*\*|`[^`]+`)")


def style_run(run, size=12, bold=False, mono=False):
    font_name = "Courier New" if mono else "Times New Roman"
    run.font.name = font_name
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), font_name)
    run.font.size = Pt(10 if mono else size)
    run.font.color.rgb = BLACK
    run.bold = bold


def add_inline(paragraph, text, size=12, bold=False):
    for part in INLINE.split(text):
        if not part:
            continue
        if part.startswith("**") and part.endswith("**"):
            style_run(paragraph.add_run(part[2:-2]), size=size, bold=True)
        elif part.startswith("`") and part.endswith("`"):
            style_run(paragraph.add_run(part[1:-1]), mono=True)
        else:
            style_run(paragraph.add_run(part), size=size, bold=bold)


def main():
    source, target = sys.argv[1], sys.argv[2]
    document = Document()
    in_code = False

    for raw in open(source, encoding="utf-8").read().splitlines():
        line = raw.rstrip()
        if line.startswith("```"):
            in_code = not in_code
            continue
        if in_code:
            paragraph = document.add_paragraph()
            paragraph.paragraph_format.left_indent = Pt(18)
            paragraph.paragraph_format.space_after = Pt(0)
            style_run(paragraph.add_run(line), mono=True)
            continue
        if not line.strip():
            continue
        heading = re.match(r"^(#{1,3})\s+(.*)$", line)
        if heading:
            level = len(heading.group(1))
            paragraph = document.add_paragraph(style="Heading " + str(level))
            add_inline(paragraph, heading.group(2), size={1: 16, 2: 14, 3: 12}[level], bold=True)
        elif line.startswith("- "):
            paragraph = document.add_paragraph(style="List Bullet")
            add_inline(paragraph, line[2:])
        else:
            paragraph = document.add_paragraph()
            add_inline(paragraph, line)

    document.save(target)
    print("escrito " + target)


main()
```

- [ ] **Step 3: Generar el `.docx`**

Run: `python3 scripts/md_to_docx.py docs/justificacion.md docs/justificacion.docx`
Expected: `escrito docs/justificacion.docx`

- [ ] **Step 4: Verificar el `.docx`**

```bash
python3 -I - <<'EOF'
from docx import Document
d = Document("docs/justificacion.docx")
text = "\n".join(p.text for p in d.paragraphs)
fonts = {r.font.name for p in d.paragraphs for r in p.runs}
colors = {str(r.font.color.rgb) for p in d.paragraphs for r in p.runs if r.font.color and r.font.color.rgb}
print("párrafos:", len(d.paragraphs))
print("tiene raya:", "\u2014" in text)
print("fuentes:", sorted(fonts))
print("colores:", sorted(colors))
EOF
```

Expected: `tiene raya: False`, fuentes `['Courier New', 'Times New Roman']` y colores `['000000']`.

- [ ] **Step 5: Mirarlo**

Si hay LibreOffice (`soffice`), convertir a PDF y mirar una página: `soffice --headless --convert-to pdf --outdir /tmp docs/justificacion.docx`. Comprobar que los títulos son negros y que los bloques de código no se salen de la página. Si no hay LibreOffice, saltear este paso.

- [ ] **Step 6: Commit**

```bash
git add scripts/md_to_docx.py docs/justificacion.docx
git commit -m "versión en word del documento de justificación"
```

---

### Task 6.5: presentación (PPTX)

**Files:**
- Create: `scripts/build_pptx.py`
- Create: `docs/presentacion.pptx` (generado)

- [ ] **Step 1: Pedirle a Franco los datos de la portada**

Preguntale a Franco los nombres de todos los integrantes del grupo y la fecha de la exposición (20/11 según el cronograma). No inventes nombres. Con esos datos completá `MEMBERS` y `DATE` al principio del script del Step 3.

- [ ] **Step 2: Instalar la dependencia**

Run: `pip install python-pptx`

- [ ] **Step 3: Crear `scripts/build_pptx.py`**

```python
#!/usr/bin/env python3
"""Genera docs/presentacion.pptx: 18 slides, formato sobrio (fondo blanco, texto negro, Calibri).

Si existe docs/uml.png (el diagrama que dibuja Franco) se usa en la slide 4.
Uso (desde la raíz del repo): python3 scripts/build_pptx.py
"""
import pathlib
import sys

from lxml import etree
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.oxml.ns import qn
from pptx.util import Inches, Pt

# Completar con los datos reales antes de generar (ver el Step 1 del plan).
MEMBERS = []
DATE = ""

OUT = pathlib.Path("docs/presentacion.pptx")
UML = pathlib.Path("docs/uml.png")
CAPTURE = pathlib.Path("docs/captura.png")
BLACK = RGBColor(0, 0, 0)

SLIDES = [
    {
        "title": "Ajedrez: núcleo y adaptadores",
        "bullets": ["TPO de Ingeniería de Software, UADE"],
        "notes": "Presentación del grupo y del trabajo.",
        "cover": True,
    },
    {
        "title": "La consigna y lo que hicimos",
        "bullets": [
            "Obligatorio: tablero 8x8, 6 piezas, turnos, captura, movimientos inválidos y jaque",
            "Opcional hecho: jaque mate, enroque, captura al paso, coronación",
            "Opcional hecho: tablas (3 reglas), deshacer y rehacer, interfaz gráfica",
            "Opcional hecho: oponente con IA en tres niveles",
            "Núcleo sin framework y probado sin infraestructura",
        ],
        "notes": "Todo lo obligatorio y todos los opcionales.",
    },
    {
        "title": "Arquitectura general",
        "bullets": [
            "nucleo: las reglas del juego",
            "puertos: lo que el núcleo pide al exterior",
            "adaptadores: consola y ventana Swing",
            "ai: el oponente, afuera del núcleo",
            "Las dependencias apuntan hacia adentro",
        ],
        "code": "adaptadores --> puertos --> nucleo\nai ------------------> nucleo\nMain / MainSwing: arman todo",
        "notes": "Regla: el núcleo no importa nada de afuera.",
    },
    {
        "title": "Diagrama de clases",
        "bullets": [
            "UML consistente con el código",
            "Una sola entrada: IChessGame",
            "Piezas con estrategia de movimiento",
            "Reglas especiales y de tablas enchufables",
        ],
        "image": "uml",
        "notes": "Mostrar el diagrama y recorrer los paquetes.",
    },
    {
        "title": "Núcleo y adaptadores",
        "bullets": [
            "La prueba: si cambia la tecnología, ¿esto cambia?",
            "Reglas del ajedrez: núcleo",
            "Consola, Swing, imágenes de las piezas: adaptadores",
            "La ventana se agregó sin tocar ChessGame",
        ],
        "notes": "El criterio de la cátedra, aplicado a nuestro caso.",
    },
    {
        "title": "Puertos",
        "bullets": [
            "Entrada: IChessGame (uno solo)",
            "Salida: IMoveListener, IGameStatusListener, IPromotionChooser",
            "Un puerto de salida por cada cosa que el núcleo pide afuera",
            "El observer se partió en dos (Interface Segregation)",
        ],
        "code": "interface IChessGame {\n  MoveOutcome tryMove(Position f, Position t);\n  boolean undo();\n  boolean redo();\n  List<Move> legalMoves();\n}",
        "notes": "Por qué dos listeners y no uno.",
    },
    {
        "title": "Inyección de dependencias",
        "bullets": [
            "Nivel 4: todo entra por constructor",
            "Solo Main y MainSwing crean y conectan",
            "Se prueba con un stub y un mock",
            "Sumar una regla es sumar una línea al armado",
        ],
        "code": "new ChessGame(board,\n  moveListeners,\n  statusListeners,\n  specialMoves,\n  drawRules);",
        "notes": "Cada colaborador inyectado es un colaborador externo distinto.",
    },
    {
        "title": "Composición sobre herencia",
        "bullets": [
            "Una sola clase Piece",
            "Piece tiene una IMovementStrategy",
            "La reina compone torre y alfil",
            "Una pieza nueva es una estrategia nueva",
        ],
        "code": "class Piece {\n  PieceType type;\n  Color color;\n  IMovementStrategy movement;\n}",
        "notes": "Sin árbol de herencia de piezas.",
    },
    {
        "title": "Factory",
        "bullets": [
            "PieceFactory es un registro tipo -> estrategia",
            "PieceType es una clase de valor, no un enum",
            "BoardSetup arma un tablero de cualquier tamaño",
            "Se agrega una pieza sin editar clases",
        ],
        "code": "factory.register(\n  new PieceType(\"CHANCELLOR\"),\n  new ChancellorStrategy());",
        "notes": "El enum era un conjunto cerrado.",
    },
    {
        "title": "Command: jugadas que se pueden deshacer",
        "bullets": [
            "MoveCommand, CastlingCommand, PromotionCommand, EnPassantCommand",
            "Cada uno guarda lo necesario para revertir",
            "Historial y pila de rehacer en ChessGame",
            "Las casillas tocadas dan el derecho de enroque",
        ],
        "code": "interface ICommand {\n  void execute();\n  void undo();\n}",
        "notes": "Deshacer y rehacer salen del historial.",
    },
    {
        "title": "State",
        "bullets": [
            "Turno: WhiteTurnState y BlackTurnState",
            "Terminales: Checkmate, Stalemate y Draw",
            "No hay un booleano gameOver",
            "Deshacer vuelve al estado anterior",
        ],
        "code": "interface IGameState {\n  Color currentColor();\n  boolean isOver();\n  IGameState afterMove();\n}",
        "notes": "El patrón resuelve un problema real: los estados terminales.",
    },
    {
        "title": "Observer",
        "bullets": [
            "El núcleo avisa lo que pasa sin saber quién escucha",
            "ConsoleRenderer y SwingChessView implementan los listeners",
            "Los tests usan un mock que registra los avisos",
        ],
        "notes": "Dos adaptadores, un mismo puerto.",
    },
    {
        "title": "Reglas enchufables",
        "bullets": [
            "ISpecialMove: enroque, captura al paso, coronación",
            "IDrawRule: material, 50 movimientos, repetición",
            "ChessGame las recibe y no sabe cuáles son",
            "Regla nueva: una clase y una línea en el armado",
        ],
        "code": "interface ISpecialMove {\n  List<Position> extraDestinations(...);\n  IMoveCommand commandFor(...);\n}",
        "notes": "Open/Closed aplicado.",
    },
    {
        "title": "Extensibilidad",
        "bullets": [
            "Pieza nueva sin tocar clases existentes",
            "Tablero de otro tamaño: new Board(10, 10)",
            "Regla de tablas nueva sin editar ChessGame",
            "Cada una tiene un test",
        ],
        "notes": "Lo que puede pedir la defensa individual.",
    },
    {
        "title": "Tests",
        "bullets": [
            "Núcleo probado sin infraestructura",
            "Arrange, Act, Assert",
            "Stub y mock como dobles de prueba",
            "Los marcados TDD se escribieron antes del código",
            "Runner propio: ./scripts/test.sh",
        ],
        "notes": "Contar cuántos tests hay con ./scripts/test.sh.",
    },
    {
        "title": "La IA, afuera del núcleo",
        "bullets": [
            "Tres niveles: azar, captura codiciosa y minimax",
            "Solo conoce IChessGame",
            "Piensa en una partida de búsqueda propia",
            "No abre diálogos ni toca la pantalla",
        ],
        "notes": "Es un extra para jugar; no forma parte de las reglas.",
    },
    {
        "title": "Decisiones y cuándo se romperían",
        "bullets": [
            "Núcleo separado: sobra en un prototipo descartable",
            "Estrategias por pieza: una jerarquía corta alcanzaría si compartieran estado",
            "PieceType abierto: un enum alcanzaría con piezas fijas",
            "State con terminales: con 2 estados bastaba un enum",
            "Detalle completo en docs/justificacion",
        ],
        "notes": "Qué, por qué y cuándo se rompería, resumido.",
    },
    {
        "title": "Demostración",
        "bullets": [
            "Partida contra la IA en la ventana",
            "Enroque, coronación y captura al paso",
            "Deshacer y rehacer",
            "Gracias",
        ],
        "image": "capture",
        "notes": "Demo en vivo: abrir MainSwing.",
    },
]


def bulletize(paragraph):
    properties = paragraph._p.get_or_add_pPr()
    properties.set("marL", "285750")
    properties.set("indent", "-285750")
    bullet = etree.SubElement(properties, qn("a:buChar"))
    bullet.set("char", "•")


def add_text(slide, left, top, width, height, size, bold=False, mono=False):
    box = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(height))
    frame = box.text_frame
    frame.word_wrap = True
    return box, frame


def style(paragraph, text, size, bold=False, mono=False):
    run = paragraph.add_run()
    run.text = text
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.name = "Courier New" if mono else "Calibri"
    run.font.color.rgb = BLACK


def main():
    if not MEMBERS or not DATE:
        sys.exit("Completá MEMBERS y DATE al principio del script (preguntale a Franco).")

    presentation = Presentation()
    presentation.slide_width = Inches(13.333)
    presentation.slide_height = Inches(7.5)
    blank = presentation.slide_layouts[6]

    for number, data in enumerate(SLIDES, 1):
        assert len(data["bullets"]) <= 6, "demasiados puntos en la slide " + str(number)
        for bullet in data["bullets"]:
            assert len(bullet) <= 100, "punto demasiado largo en la slide " + str(number) + ": " + bullet
        code = data.get("code")
        if code:
            assert len(code.splitlines()) <= 10, "demasiado código en la slide " + str(number)
            assert all(len(line) <= 50 for line in code.splitlines()), "línea de código larga en la slide " + str(number)

        slide = presentation.slides.add_slide(blank)
        slide.notes_slide.notes_text_frame.text = data["notes"]

        _, title_frame = add_text(slide, 0.7, 0.45, 11.9, 1.1, 34)
        style(title_frame.paragraphs[0], data["title"], 34, bold=True)

        has_side = bool(code) or data.get("image")
        body_width = 6.2 if has_side else 11.9
        _, body = add_text(slide, 0.7, 1.8, body_width, 5.0, 22)
        first = True
        if data.get("cover"):
            lines = data["bullets"] + ["Integrantes: " + ", ".join(MEMBERS), DATE]
            for line in lines:
                paragraph = body.paragraphs[0] if first else body.add_paragraph()
                first = False
                style(paragraph, line, 24)
                paragraph.space_after = Pt(14)
        else:
            for bullet in data["bullets"]:
                paragraph = body.paragraphs[0] if first else body.add_paragraph()
                first = False
                bulletize(paragraph)
                style(paragraph, bullet, 22)
                paragraph.space_after = Pt(12)

        if code:
            box, frame = add_text(slide, 7.2, 1.8, 5.5, 4.2, 14)
            box.fill.solid()
            box.fill.fore_color.rgb = RGBColor(0xF2, 0xF2, 0xF2)
            for index, line in enumerate(code.splitlines()):
                paragraph = frame.paragraphs[0] if index == 0 else frame.add_paragraph()
                style(paragraph, line, 14, mono=True)
        elif data.get("image") == "uml" and UML.exists():
            slide.shapes.add_picture(str(UML), Inches(7.0), Inches(1.7), width=Inches(5.9))
        elif data.get("image") == "capture" and CAPTURE.exists():
            slide.shapes.add_picture(str(CAPTURE), Inches(8.2), Inches(1.5), height=Inches(5.6))

    presentation.save(str(OUT))
    print("escrito " + str(OUT) + " con " + str(len(SLIDES)) + " slides")


main()
```

- [ ] **Step 4: Generar**

Run: `python3 scripts/build_pptx.py`
Expected: `escrito docs/presentacion.pptx con 18 slides`

- [ ] **Step 5: Verificar el contenido**

```bash
python3 -I - <<'EOF'
from pptx import Presentation
p = Presentation("docs/presentacion.pptx")
text = " ".join(sh.text_frame.text for s in p.slides for sh in s.shapes if sh.has_text_frame)
print("slides:", len(p.slides))
print("tiene raya:", "\u2014" in text)
EOF
```

Expected: `slides: 18` y `tiene raya: False`.

- [ ] **Step 6: Mirar las slides**

Si hay LibreOffice: `soffice --headless --convert-to pdf --outdir /tmp docs/presentacion.pptx` y `pdftoppm -jpeg -r 60 /tmp/presentacion.pdf /tmp/slide`, y revisar que ningún texto se salga de su caja ni se superponga con el código o la imagen. Si algo se desborda, acortar el texto de esa slide en `SLIDES` (no achicar la letra por debajo de 18) y regenerar. Si no hay LibreOffice, abrir el `.pptx` y revisar a ojo las slides con código.

- [ ] **Step 7: Commit**

```bash
git add scripts/build_pptx.py docs/presentacion.pptx
git commit -m "presentación del tpo"
```

---

### Task 6.6: README final

**Files:**
- Modify: `README.md` (reemplazo completo)

- [ ] **Step 1: Reemplazar `README.md` por este contenido**

El tono es simple y natural, sin exceso de formato. Mantener el bloque de la captura centrado.

```markdown
# TP Ajedrez

Ajedrez para jugar en consola o en una ventana, hecho para el TPO de Ingeniería de Software (UADE). La idea del trabajo es que las reglas del juego no dependan de cómo se muestra ni de cómo se juega, así que casi todo el código está pensado para poder cambiarse sin romper el resto.

<p align="center">
  <img src="docs/captura.png" alt="La ventana, con una partida a medias" width="460">
</p>

## Cómo correrlo

Se abre la carpeta en IntelliJ y se corre `Main` (consola), `MainSwing` (ventana) o `ChessCoreTests` (tests). También por terminal, desde la raíz del repo:

```bash
./scripts/build.sh
java -cp out Main
java -cp out MainSwing
./scripts/test.sh
```

En la consola se elige al arrancar contra quién se juega (dos personas o la IA en tres niveles) y se mueve escribiendo `e2 e4`. Con `undo` se deshace, con `redo` se rehace y con `salir` se termina. En la ventana se hace click en la pieza y después en el destino; arriba se elige el oponente y abajo están Deshacer y Rehacer.

## Qué reglas tiene

Todas las piezas con sus movimientos, turnos, capturas, jaque, jaque mate y ahogado, enroque, coronación (se elige la pieza), captura al paso, tablas por material insuficiente, por la regla de los 50 movimientos y por repetición, y deshacer y rehacer. El peón corona sin preguntar cuando juega la IA.

## Cómo está armado

```
src/
  nucleo/        las reglas del ajedrez, sin nada de consola ni de Swing
    especiales/    enroque, captura al paso y coronación
    estrategias/   cómo se mueve cada pieza
    tablas/        las condiciones de tablas
  puertos/       interfaces con las que el núcleo pide cosas al exterior
  adaptadores/   consola y Swing, que implementan esos puertos
  ai/            el oponente, afuera del núcleo
  Main.java      arma todo para jugar por consola
  MainSwing.java arma todo para jugar con ventana
  ChessCoreTests.java
```

El núcleo no conoce a los adaptadores ni a la IA: son ellos los que dependen de él. Por eso la ventana y la IA se pudieron agregar sin tocar `ChessGame`.

Lo que se ve en la materia está así:

- **Núcleo y adaptadores**: lo que se llama desde afuera es `IChessGame`. Para avisar lo que pasa el núcleo usa `IMoveListener` e `IGameStatusListener`, y para preguntar a qué pieza se corona usa `IPromotionChooser`.
- **Inyección por constructor**: `ChessGame` recibe el tablero, los listeners, las reglas especiales y las reglas de tablas ya armadas. El único lugar donde se crean es `Main`/`MainSwing`.
- **Composición sobre herencia**: hay una sola clase `Piece`, que tiene una `IMovementStrategy`. La reina combina las estrategias de la torre y el alfil.
- **Strategy**: el movimiento de cada pieza, las reglas especiales (`ISpecialMove`), las tablas (`IDrawRule`) y los niveles de la IA (`IOpponent`).
- **Factory**: `PieceFactory` es un registro de piezas, `BoardSetup` arma el tablero inicial y `Opponents` crea el oponente según el nivel.
- **Observer**: los dos listeners.
- **State**: el turno y los estados terminales (mate, ahogado, tablas).
- **Command**: cada jugada es un comando que se puede deshacer y rehacer.
- **Tests sin interfaz**: `ChessCoreTests`, con un stub y un mock como dobles de prueba, y los marcados `// TDD:` escritos antes del código.

## Agregar cosas sin tocar lo que ya está

- **Una pieza nueva**: se escribe su `IMovementStrategy`, se crea un `PieceType` y se registra con `factory.register(...)`. Hay un test que lo hace con una pieza inventada que se mueve como torre y caballo.
- **Otro tamaño de tablero**: `new Board(10, 10)`. Todo pregunta a `board.isInside()` en vez de asumir 8x8, y el peón corona en la última fila de ese tablero.
- **Una regla especial o de tablas nueva**: una clase que implemente `ISpecialMove` o `IDrawRule` y una línea en `Main`.

## Cosas a tener en cuenta

La repetición de posición ignora los derechos de enroque y de captura al paso. La captura al paso no detecta el jaque descubierto horizontal. La IA piensa sobre una copia de la partida sin historial, así que puede equivocarse con el enroque; las reglas reales las aplica siempre la partida verdadera.

## Documentos

En `docs/` están la justificación de las decisiones (`justificacion.md` y `.docx`), la presentación, la lista de clases para el UML y la spec y los planes con los que se terminó el trabajo.

## Piezas

Las imágenes están en `src/assets/piezas/` y son el set Cburnett de Wikimedia Commons (el mismo de lichess.org), licencia CC BY-SA 3.0, autor Colin M.L. Burnett. Si falta alguna, la ventana usa un símbolo de texto.
```

- [ ] **Step 2: Comprobar los comandos del README**

Run: `./scripts/build.sh && printf "0\ne2 e4\nsalir\n" | java -cp out Main | tail -4`
Expected: compila y muestra el tablero con el peón en e4.

- [ ] **Step 3: Sin raya**

Run: `grep -c $'\xe2\x80\x94' README.md`
Expected: `0`

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "readme al día"
```

---

### Task 6.7: verificación final y push

- [ ] **Step 1: Todo en verde**

Run: `./scripts/test.sh`
Expected: `61 pasados, 0 fallados.` (o más, si se sumaron tests) y código 0.

- [ ] **Step 2: Reglas de estilo del repo**

```bash
python3 scripts/find_spanish_identifiers.py
grep -rIn $'\xe2\x80\x94' . --exclude-dir=.git | wc -l
git log --format=%B | grep -ci "co-authored\|generated with"
grep -rn "import adaptadores\|import ai" src/nucleo | wc -l
```

Expected: `0 identificadores en español`, `0`, `0` y `0`.

- [ ] **Step 3: Los dos documentos binarios no tienen la raya**

```bash
python3 -I - <<'EOF'
from docx import Document
from pptx import Presentation
d = Document("docs/justificacion.docx")
p = Presentation("docs/presentacion.pptx")
t1 = "\n".join(x.text for x in d.paragraphs)
t2 = " ".join(sh.text_frame.text for s in p.slides for sh in s.shapes if sh.has_text_frame)
print("docx:", "\u2014" in t1, "pptx:", "\u2014" in t2)
EOF
```

Expected: `docx: False pptx: False`

- [ ] **Step 4: Entregables presentes**

Run: `ls docs/justificacion.md docs/justificacion.docx docs/presentacion.pptx docs/uml-checklist.md docs/captura.png`
Expected: los cinco archivos listados, sin errores.

- [ ] **Step 5: Revisión final y push**

Pedir una revisión completa con `superpowers:requesting-code-review` sobre todo lo hecho desde el commit `spec del cierre del tp`. Corregir lo que marque. Después:

```bash
git status --short
git push
```

Expected: `git status --short` vacío y el push a `origin/main` sin errores.

- [ ] **Step 6: Avisarle a Franco**

Contarle en pocas líneas: qué se hizo en cada fase, el número final de tests, que `docs/uml-checklist.md` ya está listo para dibujar el UML (y que cuando lo exporte como `docs/uml.png` hay que volver a correr `python3 scripts/build_pptx.py` para que aparezca en la slide 4), y que quedaron declaradas las simplificaciones de la sección 13 del documento de justificación.
