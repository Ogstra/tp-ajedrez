package adapters;

import core.Board;
import core.Color;
import core.Move;
import core.Piece;
import core.PieceType;
import core.Position;
import skins.Skin;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// El tablero de la ventana: las casillas, las coordenadas y las piezas de la skin. Antes vivía
// adentro de SwingChessView, que también manejaba las pantallas, la IA y los textos;
// separado, cada clase tiene una sola razón para cambiar (SRP) y el tablero se puede achicar o
// agrandar sin tocar el resto.
//
// Las casillas se adaptan al espacio que hay: cuando la ventana cambia de tamaño, fitTo() calcula
// el lado de casilla que entra y las piezas se vuelven a armar a ese tamaño (con SmoothImage, que
// trae variantes para la pantalla escalada). Rehacer los íconos cuesta, así que se espera a que
// el usuario deje de arrastrar el borde (un Timer que se reinicia con cada cambio).
public class BoardView extends JPanel {

    static final int COORDINATE_SIZE = 18; // ancho de los números y alto de las letras
    static final int MIN_SQUARE = 32;
    static final int MAX_SQUARE = 160;     // agrandando la ventana, hasta acá crecen las casillas

    private static final java.awt.Color SELECTED = new java.awt.Color(0xF6, 0xF6, 0x69);
    private static final java.awt.Color LEGAL = new java.awt.Color(0xF2, 0xC9, 0x6B);          // destino libre
    private static final java.awt.Color CAPTURE = new java.awt.Color(0xE9, 0x8F, 0x7B);        // destino con captura
    private static final java.awt.Color FRAME = new java.awt.Color(0x6B, 0x5B, 0x1E);          // marco de las casillas marcadas
    private static final java.awt.Color CAPTURE_FRAME = new java.awt.Color(0x8F, 0x3E, 0x2E); // marco de las capturas
    private static final int ICON_PERCENT = 85; // la pieza ocupa el 85% de la casilla

    // Letra de la imagen (wK.png, bQ.png...) y símbolo de texto de respaldo, por tipo de pieza.
    // Una pieza que este adaptador no conoce (por ejemplo, una agregada en la defensa) se muestra
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
    private Skin skin;
    private int squareSize = MIN_SQUARE;

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
    public void setup(Board board, boolean flipped, Skin skin, int squareSize) {
        this.board = board;
        this.skin = skin;
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

    // Redibuja las piezas y los colores de las casillas. Borra lo que hubiera marcado.
    public void render(Board board) {
        this.board = board;
        for (int row = 0; row < squares.length; row++) {
            for (int col = 0; col < squares[row].length; col++) {
                JButton button = squares[row][col];
                applyIcon(button, board.pieceAt(new Position(row, col)));
                button.setBackground(squareColor(row, col));
                button.setBorderPainted(false); // quita el marco de una selección anterior
            }
        }
    }

    // Marca la pieza elegida y sus destinos: ámbar los libres y rojizo las capturas.
    public void highlight(Position selected, List<Move> legalMoves) {
        render(board);
        mark(selected, SELECTED, FRAME);
        for (Move move : legalMoves) {
            if (!move.getFrom().equals(selected)) continue;
            boolean captures = board.pieceAt(move.getTo()) != null;
            mark(move.getTo(), captures ? CAPTURE : LEGAL, captures ? CAPTURE_FRAME : FRAME);
        }
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

    // Pinta la casilla y le pone un marco, así dos casillas marcadas pegadas
    // no se funden en una sola mancha.
    private void mark(Position pos, java.awt.Color fill, java.awt.Color frame) {
        JButton button = squares[pos.getRow()][pos.getColumn()];
        button.setBackground(fill);
        button.setBorder(BorderFactory.createLineBorder(frame, 3));
        button.setBorderPainted(true);
    }

    private java.awt.Color squareColor(int row, int col) {
        return SkinImages.color((row + col) % 2 == 0 ? skin.getDarkSquare() : skin.getLightSquare());
    }

    private void applyIcon(JButton button, Piece piece) {
        String key = piece == null ? null : imageKey(piece);
        Image image = key == null ? null : SkinImages.piece(skin, key);
        if (image == null) {
            button.setIcon(null);
            button.setText(piece == null ? "" : symbol(piece)); // asset faltante o pieza desconocida: texto
            return;
        }
        button.setText("");
        button.setIcon(pieceIcons.computeIfAbsent(key,
            k -> new ImageIcon(SmoothImage.of(image, squareSize * ICON_PERCENT / 100))));
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
