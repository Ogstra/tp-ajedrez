package adapters.swing;

import core.Move;
import core.board.Board;
import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// El tablero de la ventana: las casillas, las coordenadas, las piezas y las marcas de la pieza
// elegida. SwingChessView se ocupa de la partida y los textos; el dibujo del tablero vive acá
// (SRP: cada clase tiene una sola razón para cambiar).
public class BoardView extends JPanel {

    private static final int SQUARE = 80;          // lado de cada casilla, en píxeles
    private static final int PIECE = 68;           // lado de la imagen de la pieza, con un margen
    private static final int COORDINATE_SIZE = 18; // ancho de los números y alto de las letras

    private static final java.awt.Color LIGHT = new java.awt.Color(0xEE, 0xEE, 0xD2);
    private static final java.awt.Color DARK = new java.awt.Color(0x76, 0x96, 0x56);
    private static final java.awt.Color SELECTED = new java.awt.Color(0xF6, 0xF6, 0x69);
    private static final java.awt.Color LEGAL = new java.awt.Color(0xF2, 0xC9, 0x6B);          // destino libre
    private static final java.awt.Color CAPTURE = new java.awt.Color(0xE9, 0x8F, 0x7B);        // destino con captura
    private static final java.awt.Color FRAME = new java.awt.Color(0x6B, 0x5B, 0x1E);          // marco de las casillas marcadas
    private static final java.awt.Color CAPTURE_FRAME = new java.awt.Color(0x8F, 0x3E, 0x2E); // marco de las capturas

    // Letra de la imagen (wK.png, bQ.png...) y símbolo de texto de respaldo, por tipo de pieza.
    // Una pieza que este adaptador no conoce (una nueva del núcleo) se muestra con la inicial de
    // su nombre, sin tocar esta clase.
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

    // Set de piezas "Cburnett" (Wikimedia Commons / lichess.org), CC BY-SA 3.0. Se cargan y se
    // achican una sola vez desde assets/piezas/. Si falta alguna, esa pieza se muestra con su símbolo.
    private static final Map<String, Icon> ICONS = loadIcons();

    private static void define(PieceType type, String letter, String white, String black) {
        LETTERS.put(type, letter);
        WHITE_SYMBOLS.put(type, white);
        BLACK_SYMBOLS.put(type, black);
    }

    private final Consumer<Position> onSquareClicked;
    private final JPanel grid = new JPanel();
    private final JPanel rankLabels = new JPanel();
    private final JPanel fileLabels = new JPanel();
    private JButton[][] squares = new JButton[0][0];
    private Board board;

    public BoardView(Consumer<Position> onSquareClicked) {
        super(new BorderLayout());
        this.onSquareClicked = onSquareClicked;
        grid.setName("board");
        // Números a la izquierda y letras abajo, alineadas con las columnas.
        JPanel fileRow = new JPanel(new BorderLayout());
        fileRow.add(Box.createHorizontalStrut(COORDINATE_SIZE), BorderLayout.WEST);
        fileRow.add(fileLabels, BorderLayout.CENTER);
        add(grid, BorderLayout.CENTER);
        add(rankLabels, BorderLayout.WEST);
        add(fileRow, BorderLayout.SOUTH);
    }

    // Arma las casillas para este tablero, con las blancas abajo.
    public void setup(Board board) {
        this.board = board;
        int rows = board.getRows();
        int columns = board.getColumns();
        squares = new JButton[rows][columns];
        grid.removeAll();
        grid.setLayout(new GridLayout(rows, columns));
        grid.setPreferredSize(new Dimension(columns * SQUARE, rows * SQUARE));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                final Position pos = new Position(rows - 1 - i, j);
                JButton button = new JButton();
                button.setOpaque(true);
                button.setBorderPainted(false);
                button.setFocusable(false); // se juega con el mouse: sin el aro de foco sobre una casilla
                button.addActionListener(e -> onSquareClicked.accept(pos));
                squares[pos.getRow()][pos.getColumn()] = button;
                grid.add(button);
            }
        }
        buildCoordinates(rows, columns);
        render(board);
    }

    // Redibuja las piezas y los colores de las casillas. Borra lo que hubiera marcado.
    public void render(Board board) {
        this.board = board;
        for (int row = 0; row < squares.length; row++) {
            for (int col = 0; col < squares[row].length; col++) {
                JButton button = squares[row][col];
                applyIcon(button, board.pieceAt(new Position(row, col)));
                button.setBackground((row + col) % 2 == 0 ? DARK : LIGHT);
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

    // Una imagen de assets/, leída del classpath y no de una ruta en disco como "src/assets/...":
    // así se encuentra igual desde IntelliJ o desde la terminal, y aunque el juego se corra desde
    // otra carpeta. build.sh copia src/assets junto a las clases. Devuelve null si falta el archivo.
    static BufferedImage asset(String path) {
        try (InputStream in = BoardView.class.getResourceAsStream("/assets/" + path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static Map<String, Icon> loadIcons() {
        Map<String, Icon> map = new HashMap<>();
        for (String key : new String[]{"wK", "wQ", "wR", "wB", "wN", "wP", "bK", "bQ", "bR", "bB", "bN", "bP"}) {
            BufferedImage image = asset("piezas/" + key + ".png");
            if (image != null) map.put(key, new ImageIcon(image.getScaledInstance(PIECE, PIECE, Image.SCALE_SMOOTH)));
        }
        return map;
    }

    // Números (filas) y letras (columnas) en el mismo orden en que se ven las casillas.
    private void buildCoordinates(int rows, int columns) {
        rankLabels.removeAll();
        rankLabels.setLayout(new GridLayout(rows, 1));
        rankLabels.setPreferredSize(new Dimension(COORDINATE_SIZE, 0));
        for (int i = 0; i < rows; i++) {
            rankLabels.add(new JLabel(String.valueOf(rows - i), SwingConstants.CENTER));
        }
        fileLabels.removeAll();
        fileLabels.setLayout(new GridLayout(1, columns));
        fileLabels.setPreferredSize(new Dimension(0, COORDINATE_SIZE));
        for (int j = 0; j < columns; j++) {
            fileLabels.add(new JLabel(String.valueOf((char) ('a' + j)), SwingConstants.CENTER));
        }
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
        Icon icon = piece == null ? null : ICONS.get(imageKey(piece));
        button.setIcon(icon);
        button.setText(piece != null && icon == null ? symbol(piece) : ""); // imagen faltante o pieza desconocida: texto
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
