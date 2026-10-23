// Genera docs/presentacion.pptx (19 slides).
//
// Uso, desde la raíz del repo:
//   npm install pptxgenjs     (una sola vez; node_modules/ está ignorado por git)
//   node scripts/build_deck.js
//
// Si existe docs/uml.png (el diagrama que dibuja Franco) se usa en la slide 4.
// Las imágenes y los datos de la portada están al principio del archivo.

const fs = require("fs");
const path = require("path");
const pptxgen = require("pptxgenjs");

const ROOT = path.join(__dirname, "..");
const OUT = path.join(ROOT, "docs", "presentacion.pptx");
const CAPTURE = path.join(ROOT, "docs", "captura.png");
const UML = path.join(ROOT, "docs", "uml.png");

const AUTHORS = "Franco Beron";
const DATE = "20 de noviembre de 2026";
const TEST_COUNT = "65";

// Paleta: verde del tablero como único acento, el resto neutros.
const INK = "1F2933";
const MUTED = "6B7280";
const ACCENT = "466A38";
const ACCENT_SOFT = "EAF0E4";
const PANEL = "F5F6F2";
const LINE = "D4D8CC";
const WHITE = "FFFFFF";
const ON_DARK = "D5DBCE";
const HEAD = "Cambria";
const BODY = "Calibri";
const MONO = "Courier New";

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE"; // 13.33 x 7.5 pulgadas
pres.title = "TP Ajedrez: núcleo y adaptadores";
pres.author = AUTHORS;

let slideNumber = 0;

// ---------- piezas de diseño ----------

function content(kicker, title, notes) {
  const slide = pres.addSlide();
  slideNumber += 1;
  slide.background = { color: WHITE };
  slide.addText(kicker.toUpperCase(), {
    x: 0.7, y: 0.42, w: 9, h: 0.3, margin: 0,
    fontFace: BODY, fontSize: 12, bold: true, color: ACCENT, charSpacing: 3,
  });
  slide.addText(title, {
    x: 0.7, y: 0.78, w: 11.9, h: 0.85, margin: 0, valign: "top",
    fontFace: HEAD, fontSize: 30, bold: true, color: INK,
  });
  slide.addText("TP Ajedrez · Ingeniería de Software · UADE", {
    x: 0.7, y: 7.02, w: 8, h: 0.25, margin: 0,
    fontFace: BODY, fontSize: 10, color: MUTED,
  });
  slide.addText(String(slideNumber), {
    x: 11.9, y: 7.02, w: 0.7, h: 0.25, margin: 0, align: "right",
    fontFace: BODY, fontSize: 10, color: MUTED,
  });
  slide.addNotes(notes);
  return slide;
}

function dark(notes) {
  const slide = pres.addSlide();
  slideNumber += 1;
  slide.background = { color: INK };
  slide.addNotes(notes);
  return slide;
}

function panel(slide, x, y, w, h, fill) {
  slide.addShape(pres.shapes.RECTANGLE, {
    x, y, w, h,
    fill: { color: fill || PANEL },
    line: { color: LINE, width: 0.75 },
  });
}

function box(slide, text, x, y, w, h, o) {
  const opts = o || {};
  slide.addShape(pres.shapes.RECTANGLE, {
    x, y, w, h,
    fill: { color: opts.fill || WHITE },
    line: { color: opts.line || LINE, width: opts.lineWidth || 1 },
  });
  slide.addText(text, {
    x, y, w, h, margin: 0.06, align: opts.align || "center", valign: "middle",
    fontFace: opts.mono ? MONO : BODY, fontSize: opts.size || 14,
    bold: opts.mono ? true : !!opts.bold, color: opts.color || INK,
  });
}

function arrow(slide, x1, y1, x2, y2, color) {
  const opts = {
    x: Math.min(x1, x2), y: Math.min(y1, y2),
    w: Math.abs(x2 - x1), h: Math.abs(y2 - y1),
    line: { color: color || MUTED, width: 1.5, endArrowType: "triangle" },
  };
  if (x2 < x1) opts.flipH = true;
  if (y2 < y1) opts.flipV = true;
  slide.addShape(pres.shapes.LINE, opts);
}

function rule(slide, x1, y1, x2, y2, color) {
  slide.addShape(pres.shapes.LINE, {
    x: Math.min(x1, x2), y: Math.min(y1, y2),
    w: Math.abs(x2 - x1), h: Math.abs(y2 - y1),
    line: { color: color || MUTED, width: 1.25 },
  });
}

function label(slide, text, x, y, w, o) {
  const opts = o || {};
  slide.addText(text, {
    x, y, w, h: opts.h || 0.35, margin: 0, valign: opts.valign || "top",
    align: opts.align || "left",
    fontFace: opts.mono ? MONO : (opts.head ? HEAD : BODY),
    fontSize: opts.size || 16, bold: opts.mono ? true : !!opts.bold, italic: !!opts.italic,
    color: opts.color || INK, charSpacing: opts.spacing || 0,
  });
}

// Filas con un cuadradito de acento adelante (no es una viñeta de texto).
function rows(slide, items, x, y, w, rowHeight, size) {
  items.forEach((item, i) => {
    const top = y + i * rowHeight;
    slide.addShape(pres.shapes.RECTANGLE, {
      x, y: top + (rowHeight - 0.14) / 2, w: 0.14, h: 0.14,
      fill: { color: ACCENT }, line: { color: ACCENT, width: 0.5 },
    });
    slide.addText(item, {
      x: x + 0.32, y: top, w: w - 0.32, h: rowHeight, margin: 0, valign: "middle",
      fontFace: BODY, fontSize: size || 17, color: INK,
    });
  });
}

function code(slide, text, x, y, w, h, size) {
  slide.addShape(pres.shapes.RECTANGLE, {
    x, y, w, h, fill: { color: "F1F2EC" }, line: { color: LINE, width: 0.75 },
  });
  slide.addText(text, {
    x: x + 0.2, y: y + 0.12, w: w - 0.4, h: h - 0.24, margin: 0, valign: "top",
    fontFace: MONO, fontSize: size || 14, bold: true, color: "111827",
  });
}

function callout(slide, head, text, x, y, w) {
  slide.addText(head, {
    x, y, w, h: 0.4, margin: 0, fontFace: HEAD, fontSize: 20, bold: true, color: ACCENT,
  });
  slide.addText(text, {
    x, y: y + 0.42, w, h: 0.7, margin: 0, valign: "top",
    fontFace: BODY, fontSize: 16, color: INK,
  });
}

// ---------- 1. portada ----------
{
  const s = dark("Presentación del trabajo: un ajedrez con núcleo y adaptadores, hecho para el TPO de Ingeniería de Software.");
  label(s, "TRABAJO PRÁCTICO OBLIGATORIO", 0.8, 1.55, 6.5, { size: 13, bold: true, color: "A9C79B", spacing: 3 });
  label(s, "Ajedrez", 0.8, 2.0, 6.5, { head: true, size: 66, bold: true, color: WHITE, h: 1.2 });
  label(s, "Núcleo y adaptadores", 0.8, 3.3, 6.5, { head: true, size: 32, color: "C7D6BD", h: 0.7 });
  label(s, "Ingeniería de Software · UADE", 0.8, 4.35, 6.5, { size: 18, color: ON_DARK });
  label(s, AUTHORS, 0.8, 5.5, 6.5, { size: 22, bold: true, color: WHITE });
  label(s, DATE, 0.8, 5.95, 6.5, { size: 16, color: "AAB3A2" });
  const h = 6.1;
  const w = h * (658 / 710);
  s.addShape(pres.shapes.RECTANGLE, { x: 7.5, y: 0.64, w: w + 0.12, h: h + 0.12, fill: { color: WHITE }, line: { color: WHITE, width: 0 } });
  s.addImage({ path: CAPTURE, x: 7.56, y: 0.7, w, h });
}

// ---------- 2. consigna ----------
{
  const s = content("La consigna", "Lo que pedía el TPO y lo que hicimos",
    "Todo el alcance obligatorio está hecho y también todos los opcionales de la consigna. Lo importante no es la lista sino cómo está armado, que es lo que sigue.");
  panel(s, 0.7, 1.9, 5.8, 4.85);
  panel(s, 6.8, 1.9, 5.8, 4.85);
  label(s, "Obligatorio", 1.0, 2.1, 5.2, { head: true, size: 22, bold: true });
  label(s, "Opcional, también hecho", 7.1, 2.1, 5.2, { head: true, size: 22, bold: true });
  rows(s, [
    "Tablero de 8x8",
    "Las 6 piezas, cada una con su movimiento",
    "Alternancia de turnos",
    "Captura de piezas",
    "Validación de movimientos inválidos",
    "Detección de jaque",
  ], 1.0, 2.8, 5.3, 0.6, 17);
  rows(s, [
    "Jaque mate y ahogado",
    "Enroque, captura al paso y coronación",
    "Tablas: material, 50 movimientos, repetición",
    "Deshacer y rehacer",
    "Interfaz gráfica",
    "Oponente con IA en tres niveles",
  ], 7.1, 2.8, 5.3, 0.6, 17);
}

// ---------- 3. arquitectura general ----------
{
  const s = content("Arquitectura", "Tres zonas y una regla: todo apunta hacia adentro",
    "El núcleo tiene las reglas. Los puertos son las interfaces con las que habla con el exterior. Los adaptadores son la consola, la ventana y la IA. Las flechas muestran quién depende de quién: nunca el núcleo de un adaptador.");
  const cols = [
    { x: 0.7, title: "ADAPTADORES", fill: "4B5563", items: ["Consola y ventana Swing", "FileAccountStore (CSV)", "Main y MainSwing (arman todo)", "ai/: el oponente"] },
    { x: 4.95, title: "PUERTOS", fill: ACCENT_SOFT, text: ACCENT, items: ["IChessGame (entrada)", "Dos listeners (Observer)", "IPromotionChooser", "IAccountStore"] },
    { x: 9.2, title: "NÚCLEO", fill: ACCENT, items: ["ChessGame", "Board, Piece, estrategias", "Comandos y estados", "Reglas especiales y de tablas"] },
  ];
  cols.forEach((c) => {
    panel(s, c.x, 1.95, 3.4, 3.55);
    s.addShape(pres.shapes.RECTANGLE, { x: c.x, y: 1.95, w: 3.4, h: 0.55, fill: { color: c.fill }, line: { color: c.text ? ACCENT : c.fill, width: 0.75 } });
    s.addText(c.title, { x: c.x, y: 1.95, w: 3.4, h: 0.55, align: "center", valign: "middle", margin: 0, fontFace: BODY, fontSize: 15, bold: true, charSpacing: 2, color: c.text || WHITE });
    c.items.forEach((item, i) => {
      s.addText(item, { x: c.x + 0.25, y: 2.7 + i * 0.68, w: 2.95, h: 0.55, margin: 0, valign: "middle", fontFace: BODY, fontSize: 16, color: INK });
    });
  });
  arrow(s, 4.15, 3.72, 4.9, 3.72, ACCENT);
  arrow(s, 8.4, 3.72, 9.15, 3.72, ACCENT);
  panel(s, 0.7, 5.85, 11.9, 0.85, WHITE);
  s.addText([
    { text: "nucleo", options: { fontFace: MONO, bold: true, color: ACCENT } },
    { text: " no importa nada de ", options: { fontFace: BODY, color: INK } },
    { text: "adaptadores", options: { fontFace: MONO, bold: true, color: ACCENT } },
    { text: " ni de ", options: { fontFace: BODY, color: INK } },
    { text: "ai", options: { fontFace: MONO, bold: true, color: ACCENT } },
    { text: ". Por eso la ventana y la IA se agregaron sin tocar el núcleo.", options: { fontFace: BODY, color: INK } },
  ], { x: 1.0, y: 5.85, w: 11.3, h: 0.85, margin: 0, valign: "middle", fontSize: 18 });
}

// ---------- 4. UML ----------
{
  const s = content("Diagrama de clases", "El UML refleja el código real",
    "Recorrer el diagrama por paquetes: nucleo, puertos, adaptadores y ai. Señalar la interfaz IChessGame como única entrada y las estrategias de movimiento.");
  label(s, "Qué mirar", 0.7, 1.95, 3.8, { head: true, size: 20, bold: true });
  rows(s, [
    "Una sola entrada: IChessGame",
    "Piece con su estrategia de movimiento",
    "Reglas especiales y de tablas enchufables",
    "El núcleo sin flechas hacia afuera",
  ], 0.7, 2.55, 3.9, 0.85, 16);
  if (fs.existsSync(UML)) {
    s.addImage({ path: UML, x: 4.9, y: 1.85, w: 7.7, h: 4.95, sizing: { type: "contain", w: 7.7, h: 4.95 } });
  } else {
    s.addShape(pres.shapes.RECTANGLE, { x: 4.9, y: 1.85, w: 7.7, h: 4.95, fill: { color: WHITE }, line: { color: MUTED, width: 1.25, dashType: "dash" } });
    s.addText("Acá va el diagrama de clases (UML)", { x: 4.9, y: 3.7, w: 7.7, h: 0.4, align: "center", margin: 0, fontFace: BODY, fontSize: 18, color: MUTED });
    s.addText("docs/uml.png, y después node scripts/build_deck.js", { x: 4.9, y: 4.15, w: 7.7, h: 0.35, align: "center", margin: 0, fontFace: MONO, fontSize: 13, color: MUTED });
  }
}

// ---------- 5. núcleo vs adaptadores ----------
{
  const s = content("Núcleo y adaptadores", "Una sola pregunta separa las dos zonas",
    "El criterio de la cátedra: si cambia la tecnología, ¿esto cambia? Si la respuesta es no, es núcleo. La prueba de que funciona es que la ventana Swing se agregó después sin modificar ChessGame.");
  label(s, "¿Esto cambia si cambia la tecnología?", 0.7, 1.85, 11.9, { head: true, size: 26, italic: true, color: ACCENT, h: 0.5 });
  panel(s, 0.7, 2.65, 5.8, 3.75);
  panel(s, 6.8, 2.65, 5.8, 3.75);
  label(s, "Núcleo: no cambia", 1.0, 2.85, 5.2, { head: true, size: 21, bold: true });
  label(s, "Adaptadores: sí cambian", 7.1, 2.85, 5.2, { head: true, size: 21, bold: true });
  rows(s, [
    "Cómo se mueve cada pieza",
    "Turnos, jaque, mate y ahogado",
    "Enroque, coronación y captura al paso",
    "Tablas y deshacer",
  ], 1.0, 3.5, 5.3, 0.65, 17);
  rows(s, [
    "Consola y ventana Swing",
    "Teclado, mouse y diálogos",
    "Las imágenes de las piezas",
    "El oponente con IA",
  ], 7.1, 3.5, 5.3, 0.65, 17);
  label(s, "La ventana Swing se agregó después sin tocar ChessGame.", 0.7, 6.55, 11.9, { size: 16, italic: true, color: MUTED });
}

// ---------- 6. puertos ----------
{
  const s = content("Puertos", "Una entrada y cuatro salidas",
    "El mundo exterior le habla al núcleo por una sola interfaz, IChessGame, aunque haya teclado, mouse y IA. Hacia afuera hay un puerto chico por cada cosa que se pide: lo que pasa con las piezas, el estado de la partida, a qué pieza se corona y dónde se guardan los usuarios.");
  const widths = [2.0, 1.9, 1.9, 2.5, 2.0];
  const gap = 0.4;
  const xs = [];
  let cursor = 0.7;
  widths.forEach((w) => { xs.push(cursor); cursor += w + gap; });
  const top = 2.7;
  const height = 2.6;
  label(s, "PUERTO DE ENTRADA", xs[1] - 0.2, 2.2, widths[1] + 0.4, { size: 11, bold: true, color: ACCENT, spacing: 2, align: "center" });
  label(s, "PUERTOS DE SALIDA", xs[3], 2.2, widths[3], { size: 11, bold: true, color: ACCENT, spacing: 2, align: "center" });
  box(s, "Main\nMainSwing\nLa IA", xs[0], top, widths[0], height, { size: 15 });
  box(s, "IChessGame", xs[1], top, widths[1], height, { mono: true, bold: true, size: 14, line: ACCENT, lineWidth: 2, fill: ACCENT_SOFT });
  box(s, "ChessGame\n\n(núcleo)", xs[2], top, widths[2], height, { size: 16, bold: true });
  const outputs = ["IMoveListener", "IGameStatusListener", "IPromotionChooser", "IAccountStore"];
  outputs.forEach((name, i) => {
    box(s, name, xs[3], top + i * 0.68, widths[3], 0.56, { mono: true, size: 12, line: ACCENT, lineWidth: 1.5, fill: ACCENT_SOFT });
  });
  box(s, "ConsoleRenderer\nSwingChessView\n\nFileAccountStore", xs[4], top, widths[4], height, { size: 14 });
  const mid = top + height / 2;
  arrow(s, xs[0] + widths[0], mid, xs[1], mid);
  arrow(s, xs[2], mid, xs[1] + widths[1], mid);
  arrow(s, xs[2] + widths[2], mid, xs[3], mid);
  arrow(s, xs[4], mid, xs[3] + widths[3], mid);
  rows(s, [
    "Un solo puerto de entrada, aunque haya varios que lo usen",
    "Un puerto de salida por cada cosa distinta que el núcleo pide afuera",
  ], 0.7, 5.5, 11.9, 0.5, 16);
  label(s, "Las flechas van de quien depende o implementa hacia la interfaz.", 0.7, 6.55, 11.9, { size: 13, italic: true, color: MUTED });
}

// ---------- 7. inyección de dependencias ----------
{
  const s = content("Inyección de dependencias", "Todo entra por el constructor",
    "ChessGame no crea nada: recibe el tablero, los listeners, las reglas especiales y las de tablas ya armadas. Solo Main y MainSwing arman el conjunto. Por eso en los tests se le pasa un stub o un mock.");
  code(s,
    "IChessGame game = new ChessGame(\n    board,\n    moveListeners,\n    statusListeners,\n    specialMoves,\n    drawRules);",
    0.7, 2.0, 6.4, 3.0, 20);
  callout(s, "Nivel 4", "Todo se inyecta por constructor, contra interfaces.", 7.7, 2.0, 4.9);
  callout(s, "Un solo lugar", "Main y MainSwing son los únicos que crean y conectan.", 7.7, 3.35, 4.9);
  callout(s, "Se prueba solo", "Los tests usan un stub y un mock en lugar de la consola.", 7.7, 4.7, 4.9);
  label(s, "Cinco colaboradores no son un problema de responsabilidad: cada uno es algo externo distinto.", 0.7, 6.15, 11.9, { size: 16, italic: true, color: MUTED });
}

// ---------- 8. composición sobre herencia ----------
{
  const s = content("Strategy y composición", "Una sola clase Piece, que tiene su movimiento",
    "No hay una subclase por pieza. Piece guarda tipo, color y una estrategia de movimiento. La reina no hereda de la torre ni del alfil: compone una de cada una. Una pieza nueva es una estrategia nueva.");
  box(s, "Piece\n\ntype\ncolor\nmovement", 0.7, 2.0, 2.4, 1.9, { size: 15, bold: true });
  box(s, "IMovementStrategy", 4.0, 2.55, 3.1, 0.8, { mono: true, bold: true, size: 14, line: ACCENT, lineWidth: 2, fill: ACCENT_SOFT });
  arrow(s, 3.1, 2.95, 4.0, 2.95, MUTED);
  label(s, "tiene una", 3.05, 2.55, 1.0, { size: 11, color: MUTED, align: "center", h: 0.3 });
  const names = ["Pawn", "Rook", "Knight", "Bishop", "Queen", "King"];
  names.forEach((n, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    box(s, n, 8.5 + col * 2.1, 2.0 + row * 0.8, 1.95, 0.62, { size: 14, mono: true });
  });
  arrow(s, 8.4, 2.95, 7.1, 2.95, MUTED);
  label(s, "implementan", 7.15, 2.55, 1.2, { size: 11, color: MUTED, align: "center", h: 0.3 });
  label(s, "La reina compone, no hereda", 0.7, 4.3, 5.5, { head: true, size: 20, bold: true });
  box(s, "QueenMovementStrategy", 1.8, 4.95, 3.8, 0.6, { mono: true, size: 12, bold: true });
  box(s, "RookMovementStrategy", 0.7, 6.1, 2.8, 0.55, { mono: true, size: 11 });
  box(s, "BishopMovementStrategy", 3.9, 6.1, 2.9, 0.55, { mono: true, size: 11 });
  rule(s, 2.1, 5.55, 2.1, 6.1, ACCENT);
  rule(s, 5.3, 5.55, 5.3, 6.1, ACCENT);
  rows(s, [
    "Seis piezas, una sola clase",
    "Una pieza nueva es una estrategia nueva",
    "Sin árbol de herencia que mantener",
  ], 7.4, 4.5, 5.2, 0.7, 17);
}

// ---------- 9. factory ----------
{
  const s = content("Factory", "Quién crea qué",
    "PieceFactory es un registro: se le suman tipos con register(). PieceType es una clase de valor y no un enum porque un enum es cerrado y agregar una pieza obligaba a editar la fábrica. BoardSetup arma el tablero inicial de cualquier tamaño y Opponents crea el oponente por nivel.");
  const cards = [
    { x: 0.7, name: "PieceFactory", text: "Un registro de tipo de pieza a estrategia. Una pieza nueva se agrega sin editar nada.", code: "factory.register(\n  type, strategy);" },
    { x: 4.75, name: "BoardSetup", text: "Arma el tablero inicial de cualquier tamaño.", code: "BoardSetup.standard(\n  pieces);" },
    { x: 8.8, name: "Opponents", text: "Crea el oponente según el nivel que se elige.", code: "Opponents.create(\n  level, random);" },
  ];
  cards.forEach((c) => {
    panel(s, c.x, 1.95, 3.8, 3.6);
    label(s, c.name, c.x + 0.25, 2.15, 3.3, { mono: true, size: 18, bold: true, color: ACCENT });
    s.addText(c.text, { x: c.x + 0.25, y: 2.7, w: 3.3, h: 1.2, margin: 0, valign: "top", fontFace: BODY, fontSize: 16, color: INK });
    code(s, c.code, c.x + 0.25, 4.15, 3.3, 1.1, 14);
  });
  panel(s, 0.7, 5.85, 11.9, 0.85, WHITE);
  s.addText([
    { text: "PieceType", options: { fontFace: MONO, bold: true, color: ACCENT } },
    { text: " es una clase de valor y no un enum: un enum es un conjunto cerrado y obligaba a editar la fábrica.", options: { fontFace: BODY, color: INK } },
  ], { x: 1.0, y: 5.85, w: 11.3, h: 0.85, margin: 0, valign: "middle", fontSize: 17 });
}

// ---------- 10. command ----------
{
  const s = content("Command", "Cada jugada es un objeto que se puede deshacer",
    "Cada comando guarda lo necesario para revertir: la pieza capturada, la torre del enroque, el peón de la coronación. ChessGame tiene un historial y una pila de rehacer. Además, las casillas que toca cada jugada sirven para saber si el rey o la torre ya se movieron, sin guardar banderas.");
  box(s, "ICommand", 2.6, 1.95, 2.4, 0.6, { mono: true, size: 14, bold: true });
  box(s, "IMoveCommand", 2.6, 2.95, 2.4, 0.6, { mono: true, size: 14, bold: true, line: ACCENT, lineWidth: 2, fill: ACCENT_SOFT });
  arrow(s, 3.8, 2.95, 3.8, 2.55, MUTED);
  const kids = ["MoveCommand", "CastlingCommand", "PromotionCommand", "EnPassantCommand"];
  const kidWidth = 1.58;
  kids.forEach((k, i) => {
    const x = 0.7 + i * (kidWidth + 0.1);
    box(s, k, x, 4.2, kidWidth, 0.7, { mono: true, size: 11 });
    rule(s, x + kidWidth / 2, 3.9, x + kidWidth / 2, 4.2, ACCENT);
  });
  rule(s, 0.7 + kidWidth / 2, 3.9, 0.7 + 3 * (kidWidth + 0.1) + kidWidth / 2, 3.9, ACCENT);
  rule(s, 3.8, 3.55, 3.8, 3.9, ACCENT);
  s.addText("Cada comando guarda lo necesario para revertir: la pieza capturada, la torre del enroque, el peón de la coronación.", {
    x: 0.7, y: 5.3, w: 6.5, h: 1.0, margin: 0, valign: "top", fontFace: BODY, fontSize: 16, italic: true, color: MUTED,
  });
  // pilas
  label(s, "HISTORIAL", 7.5, 1.95, 2.2, { size: 11, bold: true, color: ACCENT, spacing: 2 });
  label(s, "REHACER", 10.7, 1.95, 1.9, { size: 11, bold: true, color: ACCENT, spacing: 2 });
  ["jugada 3", "jugada 2", "jugada 1"].forEach((t, i) => {
    box(s, t, 7.5, 2.35 + i * 0.7, 2.2, 0.58, { size: 14, fill: i === 0 ? ACCENT_SOFT : WHITE, line: i === 0 ? ACCENT : LINE });
  });
  box(s, "jugada 4", 10.7, 2.35, 1.9, 0.58, { size: 14 });
  arrow(s, 9.75, 2.5, 10.65, 2.5, ACCENT);
  arrow(s, 10.65, 2.85, 9.75, 2.85, ACCENT);
  label(s, "undo()", 9.8, 2.17, 0.9, { size: 12, mono: true, color: ACCENT, align: "center", h: 0.3 });
  label(s, "redo()", 9.8, 2.95, 0.9, { size: 12, mono: true, color: ACCENT, align: "center", h: 0.3 });
  rows(s, [
    "Deshacer vuelve al estado anterior",
    "Rehacer reaplica la jugada guardada",
    "Una jugada nueva vacía lo que se podía rehacer",
    "Las casillas tocadas dan el derecho de enroque",
  ], 7.5, 4.45, 5.1, 0.6, 16);
}

// ---------- 11. state ----------
{
  const s = content("State", "El turno y el final de la partida son estados",
    "El turno alterna entre dos estados. Cuando la partida termina, el estado pasa a uno terminal: mate, ahogado o tablas. De un estado terminal no se sale jugando, solo deshaciendo, que vuelve al estado anterior. No hay un booleano gameOver.");
  box(s, "WhiteTurnState", 2.0, 2.0, 3.4, 0.9, { mono: true, size: 15, bold: true });
  box(s, "BlackTurnState", 7.9, 2.0, 3.4, 0.9, { mono: true, size: 15, bold: true });
  arrow(s, 5.4, 2.3, 7.9, 2.3, ACCENT);
  arrow(s, 7.9, 2.6, 5.4, 2.6, ACCENT);
  label(s, "afterMove()", 5.4, 1.95, 2.5, { size: 12, mono: true, color: ACCENT, align: "center", h: 0.3 });
  panel(s, 0.7, 4.0, 11.9, 1.85, WHITE);
  label(s, "ESTADOS TERMINALES", 1.0, 4.15, 5, { size: 11, bold: true, color: ACCENT, spacing: 2 });
  ["CheckmateState", "StalemateState", "DrawState"].forEach((n, i) => {
    box(s, n, 1.0 + i * 3.85, 4.8, 3.4, 0.7, { mono: true, size: 14, fill: PANEL });
  });
  arrow(s, 3.7, 2.9, 3.7, 4.0, MUTED);
  arrow(s, 9.6, 2.9, 9.6, 4.0, MUTED);
  label(s, "mate, ahogado o tablas", 3.85, 3.3, 3.2, { size: 13, italic: true, color: MUTED });
  rows(s, [
    "De un estado terminal no se sale jugando, solo deshaciendo",
    "Ya no hace falta un booleano gameOver",
  ], 0.7, 6.0, 11.9, 0.45, 16);
}

// ---------- 12. observer ----------
{
  const s = content("Observer", "El núcleo avisa sin saber quién escucha",
    "El observer se partió en dos interfaces para que quien solo quiere saber de movimientos no tenga que implementar el estado de la partida. Los dos adaptadores los implementan y los tests usan un mock que registra los avisos.");
  panel(s, 0.7, 1.95, 4.0, 3.6);
  panel(s, 5.2, 1.95, 3.8, 3.6);
  panel(s, 9.5, 1.95, 3.1, 3.6);
  label(s, "IMoveListener", 0.95, 2.15, 3.5, { mono: true, size: 16, bold: true, color: ACCENT });
  label(s, "IGameStatusListener", 5.45, 2.15, 3.4, { mono: true, size: 16, bold: true, color: ACCENT });
  label(s, "Quién escucha", 9.75, 2.15, 2.7, { head: true, size: 18, bold: true });
  const moveMethods = ["onMoveMade", "onCastling", "onPromotion", "onUndo", "onRedo"];
  const statusMethods = ["onInvalidMove", "onCheck", "onCheckmate", "onStalemate", "onDraw"];
  moveMethods.forEach((m, i) => label(s, m, 0.95, 2.85 + i * 0.5, 3.5, { mono: true, size: 14 }));
  statusMethods.forEach((m, i) => label(s, m, 5.45, 2.85 + i * 0.5, 3.3, { mono: true, size: 14 }));
  ["ConsoleRenderer", "SwingChessView", "RecordingObserver (mock de los tests)"].forEach((m, i) => {
    s.addText(m, { x: 9.75, y: 2.85 + i * 0.85, w: 2.65, h: 0.75, margin: 0, valign: "top", fontFace: MONO, fontSize: 13, bold: true, color: INK });
  });
  arrow(s, 9.45, 3.9, 9.05, 3.9, ACCENT);
  rows(s, [
    "Cada adaptador implementa solo lo que usa (Interface Segregation)",
  ], 0.7, 5.9, 11.9, 0.5, 17);
}

// ---------- 13. reglas enchufables ----------
{
  const s = content("Reglas enchufables", "Una regla nueva es una clase nueva",
    "ISpecialMove cubre las jugadas que no son mover a una casilla que el movimiento de la pieza ya permite: enroque, captura al paso y coronación. IDrawRule cubre las tablas. ChessGame las recibe por constructor y no sabe cuáles son ni cuántas.");
  panel(s, 0.7, 1.95, 5.8, 3.6);
  panel(s, 6.8, 1.95, 5.8, 3.6);
  label(s, "ISpecialMove", 1.0, 2.15, 5.2, { mono: true, size: 18, bold: true, color: ACCENT });
  label(s, "IDrawRule", 7.1, 2.15, 5.2, { mono: true, size: 18, bold: true, color: ACCENT });
  label(s, "Jugadas que el movimiento normal no cubre", 1.0, 2.6, 5.2, { size: 14, color: MUTED });
  label(s, "Condiciones de tablas", 7.1, 2.6, 5.2, { size: 14, color: MUTED });
  ["CastlingMove", "EnPassantMove", "PromotionMove"].forEach((n, i) => box(s, n, 1.0, 3.15 + i * 0.75, 5.2, 0.6, { mono: true, size: 14 }));
  ["InsufficientMaterialRule", "FiftyMoveRule", "ThreefoldRepetitionRule"].forEach((n, i) => box(s, n, 7.1, 3.15 + i * 0.75, 5.2, 0.6, { mono: true, size: 14 }));
  panel(s, 0.7, 5.85, 11.9, 0.85, WHITE);
  s.addText("Regla nueva: una clase y una línea en el armado. ChessGame no se toca (Open/Closed).", {
    x: 1.0, y: 5.85, w: 11.3, h: 0.85, margin: 0, valign: "middle", fontFace: BODY, fontSize: 18, color: INK,
  });
}

// ---------- 14. extensibilidad ----------
{
  const s = content("Extensibilidad", "Lo que se puede pedir en la defensa",
    "Tres cosas que pueden pedir en la defensa individual: una pieza nueva, otro tamaño de tablero y una regla nueva. Las tres se hacen sin editar clases existentes y cada una tiene un test que lo demuestra.");
  const cards = [
    { x: 0.7, title: "Una pieza nueva", text: "Una estrategia y un registro.", code: "factory.register(\n  new PieceType(\n    \"CHANCELLOR\"),\n  new Chancellor());", test: "testNewPieceWithout\nTouchingExistingClasses" },
    { x: 4.75, title: "Otro tablero", text: "El tamaño es un dato.", code: "Board board =\n  new Board(10, 10);", test: "testBoardOfAnotherSize" },
    { x: 8.8, title: "Una regla de tablas", text: "Se inyecta por constructor.", code: "drawRules.add(\n  new MiRegla());", test: "testNewDrawRuleWithout\nTouchingExistingClasses" },
  ];
  cards.forEach((c) => {
    panel(s, c.x, 1.95, 3.8, 4.35);
    label(s, c.title, c.x + 0.25, 2.15, 3.3, { head: true, size: 20, bold: true });
    label(s, c.text, c.x + 0.25, 2.65, 3.3, { size: 15, color: MUTED });
    code(s, c.code, c.x + 0.25, 3.2, 3.3, 1.65, 14);
    label(s, "TEST", c.x + 0.25, 5.1, 3.3, { size: 10, bold: true, color: ACCENT, spacing: 2 });
    s.addText(c.test, { x: c.x + 0.25, y: 5.35, w: 3.3, h: 0.7, margin: 0, valign: "top", fontFace: MONO, fontSize: 11, bold: true, color: INK });
  });
}

// ---------- 15. tests ----------
{
  const s = content("Tests", "Núcleo probado sin ninguna infraestructura",
    "Los tests corren solo con javac, sin librerías: ./scripts/test.sh compila y corre todo. Usan Arrange, Act, Assert, un stub y un mock. Los marcados TDD se escribieron antes que el código. Además hay pruebas de integración de la ventana Swing.");
  s.addText(TEST_COUNT, { x: 0.7, y: 1.7, w: 5.2, h: 2.2, margin: 0, valign: "middle", fontFace: HEAD, fontSize: 130, bold: true, color: ACCENT });
  label(s, "tests del núcleo y 20 de usuarios, todos pasan", 0.7, 3.95, 5.2, { size: 20, color: INK });
  code(s, "./scripts/test.sh", 0.7, 4.7, 4.2, 0.75, 17);
  rows(s, [
    "Arrange, Act, Assert en cada uno",
    "Un stub, un mock y un fake como dobles de prueba",
    "Los marcados TDD se escribieron antes que el código",
    "Pieza nueva, tablero de otro tamaño y regla nueva, probados",
    "Pruebas de integración de la consola y de la ventana Swing",
  ], 6.6, 2.1, 6.0, 0.8, 17);
}

// ---------- 16. IA ----------
{
  const s = content("Oponente", "Tres niveles, afuera del núcleo",
    "La IA no forma parte de las reglas, por eso vive en el paquete ai y solo conoce IChessGame. Piensa sobre una copia de la partida, así que nunca toca la pantalla. El nivel difícil es un minimax de dos jugadas que evalúa con el material y un poco de criterio posicional (centro, desarrollo, seguridad del rey); no ve más allá de dos jugadas y puede equivocarse con el enroque, porque la copia no tiene historial.");
  const cards = [
    { x: 0.7, name: "Fácil", cls: "RandomOpponent", text: "Elige una jugada legal al azar." },
    { x: 4.75, name: "Medio", cls: "GreedyOpponent", text: "Prefiere la captura de la pieza de más valor. Si no hay, al azar." },
    { x: 8.8, name: "Difícil", cls: "MinimaxOpponent", text: "Prueba su jugada y la respuesta del rival, y se queda con la que deja mejor material y posición." },
  ];
  cards.forEach((c) => {
    panel(s, c.x, 1.95, 3.8, 3.0);
    label(s, c.name, c.x + 0.25, 2.15, 3.3, { head: true, size: 26, bold: true });
    label(s, c.cls, c.x + 0.25, 2.8, 3.3, { mono: true, size: 13, color: ACCENT, bold: true });
    s.addText(c.text, { x: c.x + 0.25, y: 3.3, w: 3.3, h: 1.7, margin: 0, valign: "top", fontFace: BODY, fontSize: 16, color: INK });
  });
  rows(s, [
    "Solo conoce IChessGame y piensa en una copia de la partida",
    "Puntaje: material más centro, desarrollo y rey seguro; el mate vale más que todo",
  ], 0.7, 5.3, 11.9, 0.65, 17);
}

// ---------- 17. usuarios y ranking ----------
{
  const s = content("Usuarios y ranking", "Un puerto de salida más, sin tocar el núcleo",
    "Las cuentas no son reglas del ajedrez: viven en el paquete usuarios. ScoreRecorder escucha la partida con los mismos listeners que la consola y la ventana, y acredita el resultado. Guardar es un puerto, IAccountStore; hoy lo implementa un CSV y mañana podría ser una base de datos sin cambiar nada más.");
  box(s, "ChessGame", 0.7, 2.0, 2.6, 0.8, { mono: true, bold: true, size: 14 });
  box(s, "ScoreRecorder\n(Observer)", 4.0, 2.0, 2.9, 0.8, { mono: true, size: 13, line: ACCENT, lineWidth: 1.5, fill: ACCENT_SOFT });
  box(s, "AccountService", 7.6, 2.0, 2.6, 0.8, { mono: true, bold: true, size: 14 });
  box(s, "IAccountStore", 7.6, 3.35, 2.6, 0.7, { mono: true, size: 13, line: ACCENT, lineWidth: 1.5, fill: ACCENT_SOFT });
  box(s, "FileAccountStore\n(usuarios.csv)", 10.6, 3.25, 2.0, 0.9, { mono: true, size: 12 });
  box(s, "Menú de la ventana\ny consola", 10.6, 2.0, 2.0, 0.8, { size: 13 });
  arrow(s, 3.3, 2.4, 4.0, 2.4, ACCENT);
  arrow(s, 6.9, 2.4, 7.6, 2.4, ACCENT);
  arrow(s, 10.6, 2.4, 10.2, 2.4, ACCENT);
  arrow(s, 8.9, 2.8, 8.9, 3.35, ACCENT);
  arrow(s, 10.6, 3.7, 10.2, 3.7, ACCENT);
  rows(s, [
    "Se entra con el nombre; el id lo asigna el sistema y no hay contraseña",
    "3 puntos por victoria y 1 por empate; ranking por puntos, victorias y nombre",
    "Deshacer el mate devuelve los puntos y rehacerlo los vuelve a sumar",
    "Contra la IA solo suma la persona, juegue con blancas o con negras",
  ], 0.7, 4.45, 11.9, 0.55, 17);
}

// ---------- 18. decisiones ----------
{
  const s = content("Decisiones", "Qué se decidió y cuándo se rompería",
    "Resumen del documento de justificación: cada decisión se explica en tres partes, qué, por qué y cuándo se rompería. Acá va solo la primera y la última. El detalle está en docs/justificacion.");
  const head = (t) => ({ text: t, options: { bold: true, color: WHITE, fill: { color: ACCENT }, fontFace: BODY, fontSize: 15 } });
  const cell = (t, mono) => ({ text: t, options: { fontFace: mono ? MONO : BODY, fontSize: mono ? 13 : 15, color: INK } });
  const data = [
    [head("Decisión"), head("Cuándo se rompería")],
    [cell("Núcleo separado de los adaptadores"), cell("En un prototipo descartable de pocos días")],
    [cell("Una Piece con su estrategia de movimiento"), cell("Si las piezas compartieran mucho estado propio")],
    [cell("PieceType abierto y fábrica con registro"), cell("Si el conjunto de piezas fuera fijo para siempre")],
    [cell("Command con historial"), cell("Si no hiciera falta deshacer ni rehacer")],
    [cell("State con estados terminales"), cell("Con solo dos estados alcanzaría un enum")],
    [cell("Reglas especiales y de tablas enchufables"), cell("Con una única excepción alcanzaría un if")],
    [cell("Usuarios detrás de su propio puerto"), cell("Con usuarios reales: contraseña y base de datos")],
  ];
  s.addTable(data, {
    x: 0.7, y: 1.95, w: 11.9, colW: [5.9, 6.0], rowH: 0.55,
    border: { type: "solid", pt: 0.75, color: LINE }, valign: "middle", margin: [0.05, 0.15, 0.05, 0.15],
  });
  label(s, "Detalle completo, con el por qué de cada una, en docs/justificacion.", 0.7, 6.5, 11.9, { size: 14, italic: true, color: MUTED });
}

// ---------- 19. demostración ----------
{
  const s = dark("Demostración en vivo: abrir MainSwing. En el menú, registrar dos usuarios y jugar 1 contra 1 con enroque, coronación y captura al paso; deshacer y rehacer; ver el ranking. Después, contra la IA en nivel difícil jugando con negras (el tablero se gira). Cerrar mostrando en el código cómo se suma una pieza nueva.");
  label(s, "DEMOSTRACIÓN", 0.8, 0.9, 6.5, { size: 13, bold: true, color: "A9C79B", spacing: 3 });
  label(s, "Probémoslo", 0.8, 1.3, 6.5, { head: true, size: 44, bold: true, color: WHITE, h: 0.9 });
  const steps = [
    "Menú: usuarios, 1 contra 1 o contra la IA",
    "Enroque, coronación y captura al paso",
    "Deshacer, rehacer y ranking",
    "Contra la IA con negras: el tablero se gira",
    "Una pieza nueva, sin tocar el núcleo",
  ];
  steps.forEach((t, i) => {
    const y = 2.7 + i * 0.7;
    s.addText(String(i + 1), { x: 0.8, y, w: 0.5, h: 0.55, margin: 0, valign: "middle", fontFace: HEAD, fontSize: 24, bold: true, color: "A9C79B" });
    s.addText(t, { x: 1.4, y, w: 5.6, h: 0.55, margin: 0, valign: "middle", fontFace: BODY, fontSize: 20, color: WHITE });
  });
  label(s, "Gracias", 0.8, 6.35, 6.5, { head: true, size: 28, bold: true, color: WHITE, h: 0.6 });
  const h = 6.1;
  const w = h * (624 / 711);
  s.addShape(pres.shapes.RECTANGLE, { x: 7.5, y: 0.64, w: w + 0.12, h: h + 0.12, fill: { color: WHITE }, line: { color: WHITE, width: 0 } });
  s.addImage({ path: CAPTURE, x: 7.56, y: 0.7, w, h });
}

pres.writeFile({ fileName: OUT }).then(() => console.log("escrito " + OUT + " con " + slideNumber + " slides"));
