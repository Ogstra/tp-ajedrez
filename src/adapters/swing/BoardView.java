package adapters.swing;

import adapters.swing.components.Palette;
import adapters.swing.components.SmoothImage;
import core.board.Board;
import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import core.Move;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

// El tablero de la ventana: casillas, coordenadas, piezas y marcas de la pieza elegida.
// SwingChessView se ocupa de las pantallas, la partida y los textos.
// Cuando la ventana cambia de tamaño, fitTo() recalcula el lado de las casillas.
public class BoardView extends JPanel {

    static final int COORDINATE_SIZE = 18; // ancho de los números y alto de las letras
    static final int MIN_SQUARE = 32;
    static final int MAX_SQUARE = 160;     // agrandando la ventana, hasta acá crecen las casillas

    private static final java.awt.Color SELECTED = new java.awt.Color(0xF6, 0xF6, 0x69);
    private static final java.awt.Color LEGAL = new java.awt.Color(0xF2, 0xC9, 0x6B);          // destino libre
    private static final java.awt.Color CAPTURE = new java.awt.Color(0xE9, 0x8F, 0x7B);        // destino con captura
    private static final java.awt.Color FRAME = new java.awt.Color(0x6B, 0x5B, 0x1E);          // marco de las casillas marcadas
    private static final java.awt.Color CAPTURE_FRAME = new java.awt.Color(0x8F, 0x3E, 0x2E); // marco de las capturas
    private static final java.awt.Color HINT = new java.awt.Color(0x9D, 0xC4, 0xE8);          // pista, en azul para no confundirla
    private static final java.awt.Color HINT_FRAME = new java.awt.Color(0x2B, 0x5C, 0x8F);
    private static final int ICON_PERCENT = 85; // la pieza ocupa el 85% de la casilla

    // Letra de la imagen (wK.png, bQ.png...) y símbolo de texto de respaldo, por tipo de pieza.
    // Una pieza que este adaptador no conoce (por ejemplo, una inventada) se muestra
    // con la inicial de su nombre, sin tocar esta clase.
    private static final Map<PieceType, String> LETTERS = new HashMap<>();
    private static final Map<PieceType, String> WHITE_SYMBOLS = new HashMap<>();
    private static final Map<PieceType, String> BLACK_SYMBOLS = new HashMap<>();

    static {
        define(PieceType.KING, "K", "♔", "♚");
        define(PieceType.QUEEN, "Q", "♕", "♛");
        define(PieceType.ROOK, "R", "♖", "♜");
        define(PieceType.BISHOP, "B", "♗", "♝");
        define(PieceType.KNIGHT, "N", "♘", "♞");
        define(PieceType.PAWN, "P", "♙", "♟");
    }

    // Set de piezas "Cburnett" (Wikimedia Commons / lichess.org), CC BY-SA 3.0. Se cargan una
    // sola vez desde assets/piezas/. Si falta alguna, esa pieza se muestra con su símbolo.
    private static final Map<String, Image> IMAGES = loadImages();

    private static void define(PieceType type, String letter, String white, String black) {
        LETTERS.put(type, letter);
        WHITE_SYMBOLS.put(type, white);
        BLACK_SYMBOLS.put(type, black);
    }

    private final Consumer<Position> onSquareClicked;
    private final JPanel grid = new JPanel();
    private final JPanel rankLabels = new JPanel();
    private final JPanel fileLabels = new JPanel();
    private final Map<String, Icon> pieceIcons = new HashMap<>(); // "wK" -> ícono al tamaño actual
    private final Timer rescale = new Timer(150, e -> refreshIcons());
    private JButton[][] squares = new JButton[0][0];
    private Board board;
    private int squareSize = MIN_SQUARE;
    private Set<Position> lastMove = new HashSet<>(); // casillas de la última jugada, teñidas de amarillo

    public BoardView(Consumer<Position> onSquareClicked) {
        super(new BorderLayout());
        this.onSquareClicked = onSquareClicked;
        rescale.setRepeats(false);
        grid.setName("board");
        // Números a la izquierda y letras abajo, alineadas con las columnas.
        JPanel fileRow = new JPanel(new BorderLayout());
        fileRow.add(Box.createHorizontalStrut(COORDINATE_SIZE), BorderLayout.WEST);
        fileRow.add(fileLabels, BorderLayout.CENTER);
        add(grid, BorderLayout.CENTER);
        add(rankLabels, BorderLayout.WEST);
        add(fileRow, BorderLayout.SOUTH);
        for (JPanel panel : new JPanel[]{this, fileRow, rankLabels, fileLabels}) panel.setOpaque(false);
    }

    // Arma las casillas para este tablero. Si la persona juega con negras se gira, para verlas abajo.
    public void setup(Board board, boolean flipped, int squareSize) {
        this.board = board;
        this.lastMove = new HashSet<>();
        int rows = board.getRows();
        int columns = board.getColumns();
        squares = new JButton[rows][columns];
        grid.removeAll();
        grid.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                final Position pos = new Position(flipped ? i : rows - 1 - i, flipped ? columns - 1 - j : j);
                JButton button = new JButton();
                button.setOpaque(true);
                button.setBorderPainted(false);
                button.setFocusable(false); // se juega con el mouse: sin el aro de foco sobre una casilla
                button.addActionListener(e -> onSquareClicked.accept(pos));
                squares[pos.getRow()][pos.getColumn()] = button;
                grid.add(button);
            }
        }
        buildCoordinates(rows, columns, flipped);
        resize(squareSize);
        refreshIcons();
    }

    // El lado de casilla más grande que entra en el espacio disponible, entre MIN y MAX.
    public static int squareSizeFor(Dimension available, int rows, int columns) {
        int byWidth = (available.width - COORDINATE_SIZE) / columns;
        int byHeight = (available.height - COORDINATE_SIZE) / rows;
        return Math.max(MIN_SQUARE, Math.min(MAX_SQUARE, Math.min(byWidth, byHeight)));
    }

    // Se llama cuando cambia el espacio para el tablero (la ventana se agrandó o se achicó).
    public void fitTo(Dimension available) {
        if (board == null) return;
        int size = squareSizeFor(available, board.getRows(), board.getColumns());
        if (size == squareSize) return;
        resize(size);
        rescale.restart(); // las piezas se rehacen cuando se deja de arrastrar
    }

    public int getSquareSize() { return squareSize; }

    // Las casillas de la última jugada (de dónde salió y adónde llegó) quedan teñidas, así se
    // ve qué movió el rival y, al revisar, qué cambió. Se aplican en el próximo render().
    public void setLastMove(Collection<Position> squares) {
        lastMove = new HashSet<>(squares);
    }

    // Redibuja las piezas y los colores de las casillas. Borra lo que hubiera marcado, salvo la
    // última jugada.
    public void render(Board board) {
        this.board = board;
        for (int row = 0; row < squares.length; row++) {
            for (int col = 0; col < squares[row].length; col++) {
                JButton button = squares[row][col];
                Position pos = new Position(row, col);
                applyIcon(button, board.pieceAt(pos));
                java.awt.Color base = (row + col) % 2 == 0 ? Palette.BOARD_DARK : Palette.BOARD_LIGHT;
                button.setBackground(lastMove.contains(pos) ? mix(base, SELECTED) : base);
                button.setBorderPainted(false); // quita el marco de una selección anterior
            }
        }
    }

    // Marca la pieza elegida y sus destinos, usando las jugadas legales que ofrece el puerto de
    // entrada: amarillo la pieza, ámbar los destinos libres y rojizo los destinos donde captura.
    public void highlight(Position selected, List<Move> legalMoves) {
        render(board);
        mark(selected, SELECTED, FRAME);
        for (Move move : legalMoves) {
            if (!move.getFrom().equals(selected)) continue;
            boolean captures = board.pieceAt(move.getTo()) != null;
            mark(move.getTo(), captures ? CAPTURE : LEGAL, captures ? CAPTURE_FRAME : FRAME);
        }
    }

    // Marca la jugada que sugiere la pista, en azul: de dónde sale y adónde va.
    public void showHint(Move move) {
        render(board);
        mark(move.getFrom(), HINT, HINT_FRAME);
        mark(move.getTo(), HINT, HINT_FRAME);
    }

    // El menú usa las mismas imágenes para mostrar el color de cada lado.
    static Image pieceImage(String key) {
        return IMAGES.get(key);
    }

    // Lee una imagen de assets/ desde el classpath, así se encuentra igual desde IntelliJ o la
    // terminal y en cualquier sistema. Devuelve null si falta el archivo.
    static BufferedImage asset(String path) {
        try (InputStream in = BoardView.class.getResourceAsStream("/assets/" + path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static Map<String, Image> loadImages() {
        Map<String, Image> map = new HashMap<>();
        for (String key : new String[]{"wK", "wQ", "wR", "wB", "wN", "wP", "bK", "bQ", "bR", "bB", "bN", "bP"}) {
            map.put(key, asset("piezas/" + key + ".png"));
        }
        return map;
    }

    private void resize(int size) {
        squareSize = size;
        Dimension gridSize = new Dimension(squares.length == 0 ? 0 : squares[0].length * size, squares.length * size);
        grid.setPreferredSize(gridSize);
        grid.setMinimumSize(gridSize); // con menos lugar no se deforma: las casillas siguen cuadradas
        revalidate();
    }

    private void refreshIcons() {
        pieceIcons.clear();
        if (board != null) render(board);
    }

    // Números (filas) y letras (columnas) en el mismo orden en que se ven las casillas.
    private void buildCoordinates(int rows, int columns, boolean flipped) {
        rankLabels.removeAll();
        rankLabels.setLayout(new GridLayout(rows, 1));
        rankLabels.setPreferredSize(new Dimension(COORDINATE_SIZE, 0));
        for (int i = 0; i < rows; i++) {
            rankLabels.add(coordinate(String.valueOf(flipped ? i + 1 : rows - i)));
        }
        fileLabels.removeAll();
        fileLabels.setLayout(new GridLayout(1, columns));
        fileLabels.setPreferredSize(new Dimension(0, COORDINATE_SIZE));
        for (int j = 0; j < columns; j++) {
            fileLabels.add(coordinate(String.valueOf((char) ('a' + (flipped ? columns - 1 - j : j)))));
        }
    }

    private static JLabel coordinate(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Palette.MUTED);
        return label;
    }

    // Mitad y mitad de los dos colores: la casilla clara y la oscura siguen distinguiéndose.
    private static java.awt.Color mix(java.awt.Color a, java.awt.Color b) {
        return new java.awt.Color((a.getRed() + b.getRed()) / 2, (a.getGreen() + b.getGreen()) / 2, (a.getBlue() + b.getBlue()) / 2);
    }

    // Pinta la casilla y le pone un marco, así dos casillas marcadas pegadas
    // no se funden en una sola mancha.
    private void mark(Position pos, java.awt.Color fill, java.awt.Color frame) {
        JButton button = squares[pos.getRow()][pos.getColumn()];
        button.setBackground(fill);
        button.setBorder(BorderFactory.createLineBorder(frame, 3));
        button.setBorderPainted(true);
    }

    private void applyIcon(JButton button, Piece piece) {
        String key = piece == null ? null : imageKey(piece);
        Image image = key == null ? null : IMAGES.get(key);
        if (image == null) {
            button.setIcon(null);
            button.setText(piece == null ? "" : symbol(piece)); // imagen faltante o pieza desconocida: texto
            return;
        }
        button.setText("");
        Icon icon = pieceIcons.get(key);
        if (icon == null) {
            icon = new ImageIcon(SmoothImage.of(image, squareSize * ICON_PERCENT / 100));
            pieceIcons.put(key, icon);
        }
        button.setIcon(icon);
    }

    private static String imageKey(Piece piece) {
        String letter = LETTERS.get(piece.getType());
        if (letter == null) return null;
        return (piece.getColor() == Color.WHITE ? "w" : "b") + letter;
    }

    // Símbolo de texto: respaldo si falta un PNG o la pieza no tiene imagen.
    private static String symbol(Piece piece) {
        boolean isWhite = piece.getColor() == Color.WHITE;
        String symbol = (isWhite ? WHITE_SYMBOLS : BLACK_SYMBOLS).get(piece.getType());
        if (symbol != null) return symbol;
        String initial = piece.getType().getName().substring(0, 1);
        return isWhite ? initial.toUpperCase() : initial.toLowerCase();
    }
}
