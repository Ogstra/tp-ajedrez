package adaptadores;

import nucleo.Board;
import nucleo.ChessGame;
import nucleo.Color;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;
import puertos.IGameObserver;
import puertos.IPromotionChooser;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Adaptador de salida Y de entrada (Clase 2): implementa IGameObserver
// igual que ConsoleRenderer (mismo puerto de salida, otra tecnología),
// implementa IPromotionChooser con un diálogo, y además traduce clicks del
// mouse en llamadas a ChessGame.tryMove(): el mismo rol que cumple Main.java
// con el teclado, pero con Swing. ChessGame no se modificó en absoluto para
// que esto funcione: es exactamente el punto de la frontera núcleo/adaptador.
public class SwingChessView extends JFrame implements IGameObserver, IPromotionChooser {

    private static final java.awt.Color LIGHT = new java.awt.Color(0xEE, 0xEE, 0xD2);
    private static final java.awt.Color DARK = new java.awt.Color(0x76, 0x96, 0x56);
    private static final java.awt.Color SELECTED = new java.awt.Color(0xF6, 0xF6, 0x69);
    private static final int SQUARE_SIZE = 80;
    private static final int PIECE_SIZE = 68; // deja un margen chico dentro de cada casilla

    // Set de piezas "Cburnett" (Wikimedia Commons / lichess.org), CC BY-SA 3.0.
    // Se cargan una sola vez desde src/assets/piezas/ y se escalan al tamaño
    // de casilla. Si falta algún archivo, o la pieza es de un tipo que este
    // adaptador no conoce, se cae a un símbolo de texto.
    private static final Map<String, Image> IMAGES = loadImages();
    private static final Map<PieceType, String> LETTERS = new HashMap<>();
    private static final Map<PieceType, String> WHITE_SYMBOLS = new HashMap<>();
    private static final Map<PieceType, String> BLACK_SYMBOLS = new HashMap<>();
    private static final Map<PieceType, String> NAMES = new HashMap<>();

    static {
        define(PieceType.KING, "K", "♔", "♚", "Rey");
        define(PieceType.QUEEN, "Q", "♕", "♛", "Reina");
        define(PieceType.ROOK, "R", "♖", "♜", "Torre");
        define(PieceType.BISHOP, "B", "♗", "♝", "Alfil");
        define(PieceType.KNIGHT, "N", "♘", "♞", "Caballo");
        define(PieceType.PAWN, "P", "♙", "♟", "Peón");
    }

    private static void define(PieceType type, String letter, String white, String black, String name) {
        LETTERS.put(type, letter);
        WHITE_SYMBOLS.put(type, white);
        BLACK_SYMBOLS.put(type, black);
        NAMES.put(type, name);
    }

    private final int rows;
    private final int columns;
    private final JButton[][] squares;
    private final JLabel status = new JLabel("Turno de Blancas", SwingConstants.CENTER);

    private ChessGame game;
    private Position selection = null;

    public SwingChessView(int rows, int columns) {
        super("TP Ajedrez: Ingeniería de Software (UADE)");
        this.rows = rows;
        this.columns = columns;
        this.squares = new JButton[rows][columns];
        buildInterface();
    }

    // El observer se registra en el constructor de ChessGame, pero la vista
    // necesita la referencia al juego para poder mandarle movimientos: por
    // eso se inyecta después, desde el composition root (MainSwing).
    public void attachGame(ChessGame game) {
        this.game = game;
    }

    private static Map<String, Image> loadImages() {
        Map<String, Image> map = new HashMap<>();
        String folder = "src" + File.separator + "assets" + File.separator + "piezas" + File.separator;
        for (String key : new String[]{"wK", "wQ", "wR", "wB", "wN", "wP", "bK", "bQ", "bR", "bB", "bN", "bP"}) {
            try {
                BufferedImage image = ImageIO.read(new File(folder + key + ".png"));
                map.put(key, image);
            } catch (IOException e) {
                map.put(key, null);
            }
        }
        return map;
    }

    private void buildInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(rows, columns));
        for (int row = rows - 1; row >= 0; row--) {
            for (int col = 0; col < columns; col++) {
                final Position pos = new Position(row, col);
                JButton button = new JButton();
                button.setBackground(squareColor(row, col));
                button.setOpaque(true);
                button.setBorderPainted(false);
                button.addActionListener(e -> onSquareClicked(pos));
                squares[row][col] = button;
                boardPanel.add(button);
            }
        }

        JButton undoButton = new JButton("Deshacer");
        undoButton.addActionListener(e -> {
            if (game != null && game.undo()) {
                render(game.getBoard());
            }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(undoButton, BorderLayout.EAST);

        add(boardPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(columns * SQUARE_SIZE, rows * SQUARE_SIZE + 60);
        setLocationRelativeTo(null);
    }

    private java.awt.Color squareColor(int row, int col) {
        return (row + col) % 2 == 0 ? DARK : LIGHT;
    }

    private void onSquareClicked(Position pos) {
        if (game == null || game.isGameOver()) return;

        if (selection == null) {
            Piece piece = game.getBoard().pieceAt(pos);
            if (piece != null && piece.getColor() == game.getTurn()) {
                selection = pos;
                squares[pos.getRow()][pos.getColumn()].setBackground(SELECTED);
            }
            return;
        }

        Position origin = selection;
        selection = null;
        render(game.getBoard()); // limpia el resaltado de la selección

        if (origin.equals(pos)) return; // clickear la misma casilla = deseleccionar

        game.tryMove(origin, pos);
        render(game.getBoard());
    }

    // IPromotionChooser: el núcleo pregunta a qué pieza corona el peón.
    @Override
    public PieceType choose(Color color) {
        String[] options = {"Reina", "Torre", "Alfil", "Caballo"};
        int choice = JOptionPane.showOptionDialog(this, "¿A qué pieza corona el peón?",
            "Coronación", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);
        switch (choice) {
            case 1: return PieceType.ROOK;
            case 2: return PieceType.BISHOP;
            case 3: return PieceType.KNIGHT;
            default: return PieceType.QUEEN;
        }
    }

    // Solo redibuja el tablero. El texto de estado lo maneja exclusivamente
    // IGameObserver: si render() también lo tocara, pisaría el mensaje que
    // onInvalidMove/onMoveMade acaban de poner.
    public void render(Board board) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                applyIcon(squares[row][col], board.pieceAt(new Position(row, col)));
                squares[row][col].setBackground(squareColor(row, col));
            }
        }
    }

    private void applyIcon(JButton button, Piece piece) {
        if (piece == null) {
            button.setIcon(null);
            button.setText("");
            return;
        }
        Image image = IMAGES.get(imageKey(piece));
        if (image == null) {
            button.setIcon(null);
            button.setText(symbol(piece)); // asset faltante o pieza desconocida: texto
            return;
        }
        button.setText("");
        button.setIcon(new ImageIcon(image.getScaledInstance(PIECE_SIZE, PIECE_SIZE, Image.SCALE_SMOOTH)));
    }

    private String imageKey(Piece piece) {
        String letter = LETTERS.get(piece.getType());
        if (letter == null) return null;
        return (piece.getColor() == Color.WHITE ? "w" : "b") + letter;
    }

    // Símbolo de texto: respaldo si falta un PNG o la pieza no tiene imagen.
    private String symbol(Piece piece) {
        boolean isWhite = piece.getColor() == Color.WHITE;
        String symbol = (isWhite ? WHITE_SYMBOLS : BLACK_SYMBOLS).get(piece.getType());
        if (symbol != null) return symbol;
        String initial = piece.getType().getName().substring(0, 1);
        return isWhite ? initial.toUpperCase() : initial.toLowerCase();
    }

    private String name(PieceType type) {
        String name = NAMES.get(type);
        return name != null ? name : type.getName();
    }

    private String name(Color color) {
        return color == Color.WHITE ? "Blancas" : "Negras";
    }

    @Override
    public void onMoveMade(Position from, Position to, Piece piece, Piece captured) {
        String captureText = captured != null ? " (captura " + name(captured.getType()) + ")" : "";
        status.setText(name(piece.getColor()) + ": " + name(piece.getType())
            + " " + from + " -> " + to + captureText);
    }

    @Override
    public void onInvalidMove(String reason) {
        status.setText("Inválido: " + reason);
    }

    @Override
    public void onCheck(Color colorInCheck) {
        status.setText("¡Jaque a " + name(colorInCheck) + "!");
    }

    @Override
    public void onCheckmate(Color winnerColor) {
        status.setText("¡Jaque mate! Ganan " + name(winnerColor) + ".");
        JOptionPane.showMessageDialog(this, "Jaque mate. Ganan " + name(winnerColor) + ".");
    }

    @Override
    public void onStalemate() {
        status.setText("Ahogado: tablas.");
        JOptionPane.showMessageDialog(this, "Ahogado: tablas, no hay movimientos legales.");
    }

    @Override
    public void onUndo() {
        status.setText("Se deshizo el último movimiento.");
    }

    @Override
    public void onCastling(Color color, boolean kingSide) {
        status.setText(name(color) + ": enroque " + (kingSide ? "corto" : "largo") + ".");
    }

    @Override
    public void onPromotion(Color color, Position pos, PieceType newPiece) {
        status.setText(name(color) + ": corona en " + pos + " a " + name(newPiece) + ".");
    }
}
