package adaptadores;

import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import nucleo.Board;
import nucleo.Color;
import nucleo.IChessGame;
import nucleo.Move;
import nucleo.Piece;
import nucleo.PieceType;
import nucleo.Position;
import puertos.IGameStatusListener;
import puertos.IMoveListener;
import puertos.IPromotionChooser;
import usuarios.AccountService;
import usuarios.ScoreRecorder;
import usuarios.User;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Adaptador de salida Y de entrada (Clase 2): implementa los dos listeners
// igual que ConsoleRenderer (mismos puertos de salida, otra tecnología),
// implementa IPromotionChooser con un diálogo, y además traduce clicks del
// mouse en llamadas a IChessGame.tryMove(): el mismo rol que cumple Main.java
// con el teclado, pero con Swing. El núcleo no cambió para que esto funcione.
public class SwingChessView extends JFrame implements IMoveListener, IGameStatusListener, IPromotionChooser {

    private static final java.awt.Color LIGHT = new java.awt.Color(0xEE, 0xEE, 0xD2);
    private static final java.awt.Color DARK = new java.awt.Color(0x76, 0x96, 0x56);
    private static final java.awt.Color SELECTED = new java.awt.Color(0xF6, 0xF6, 0x69);
    private static final java.awt.Color LEGAL = new java.awt.Color(0xF2, 0xC9, 0x6B);   // destino libre
    private static final java.awt.Color CAPTURE = new java.awt.Color(0xE9, 0x8F, 0x7B); // destino con captura
    private static final java.awt.Color FRAME = new java.awt.Color(0x6B, 0x5B, 0x1E);         // marco de las casillas marcadas
    private static final java.awt.Color CAPTURE_FRAME = new java.awt.Color(0x8F, 0x3E, 0x2E);  // marco de las capturas
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
    private final JLabel players = new JLabel(" ", SwingConstants.CENTER);
    private final Random random = new Random();
    private final JComboBox<String> levelBox = new JComboBox<>(new String[]{"Dos jugadores", "Fácil", "Medio", "Difícil"});

    private IChessGame game;
    private AccountService accounts;     // null si no se configuraron cuentas
    private ScoreRecorder recorder;
    private User whiteUser;
    private User blackUser;
    private OpponentController opponent; // null mientras juegan dos personas
    private Position selection = null;
    private boolean thinking = false;

    public SwingChessView(int rows, int columns) {
        super("TP Ajedrez: Ingeniería de Software (UADE)");
        this.rows = rows;
        this.columns = columns;
        this.squares = new JButton[rows][columns];
        buildInterface();
    }

    // Los listeners se registran en el constructor de ChessGame, pero la vista
    // necesita la referencia al juego para poder mandarle movimientos: por
    // eso se inyecta después, desde el composition root (MainSwing).
    public void attachGame(IChessGame game) {
        this.game = game;
    }

    // Las cuentas también se inyectan después, desde el composition root. blackUser es el que
    // juega con negras cuando son dos personas; si se elige la IA, el recorder deja de acreditarlo.
    public void attachAccounts(AccountService accounts, ScoreRecorder recorder, User whiteUser, User blackUser) {
        this.accounts = accounts;
        this.recorder = recorder;
        this.whiteUser = whiteUser;
        this.blackUser = blackUser;
        updatePlayers();
    }

    private void updatePlayers() {
        String black = opponent != null ? "la IA" : blackUser == null ? "sin usuario" : blackUser.toString();
        players.setText("Blancas: " + (whiteUser == null ? "sin usuario" : whiteUser) + "   |   Negras: " + black);
    }

    private void showRanking() {
        if (accounts == null) return;
        ScoreboardDialog.show(this, accounts.ranking());
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


        levelBox.addActionListener(e -> changeOpponent(levelBox.getSelectedIndex()));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top.add(new JLabel("Oponente (juega con negras):"));
        top.add(levelBox);

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
            if (game == null || thinking) return;
            // Contra la IA se deshacen las dos: la respuesta de ella y la jugada propia.
            if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
            render(game.getBoard());
        });
        JButton redoButton = new JButton("Rehacer");
        redoButton.addActionListener(e -> {
            if (game == null || thinking) return;
            if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
            render(game.getBoard());
            playOpponentIfNeeded();
        });

        JButton rankingButton = new JButton("Ranking");
        rankingButton.addActionListener(e -> showRanking());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(rankingButton);
        buttons.add(undoButton);
        buttons.add(redoButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(players, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(columns * SQUARE_SIZE, rows * SQUARE_SIZE + 140);
        setLocationRelativeTo(null);
    }

    private java.awt.Color squareColor(int row, int col) {
        return (row + col) % 2 == 0 ? DARK : LIGHT;
    }

    // El orden de las opciones del desplegable coincide con OpponentLevel.values().
    private void changeOpponent(int index) {
        OpponentLevel level = OpponentLevel.values()[index];
        if (level == OpponentLevel.NONE || game == null) {
            opponent = null;
        } else {
            opponent = new OpponentController(game, Opponents.create(level, random), Color.BLACK);
        }
        // Contra la IA el usuario de las negras no suma ni resta; con dos personas, vuelve a acreditarse.
        if (recorder != null) recorder.setBlackUser(opponent == null ? blackUser : null);
        updatePlayers();
        playOpponentIfNeeded();
    }

    private void onSquareClicked(Position pos) {
        if (game == null || game.isGameOver() || thinking) return;

        Piece clicked = game.getBoard().pieceAt(pos);
        boolean ownPiece = clicked != null && clicked.getColor() == game.getTurn();

        if (selection == null) {
            if (ownPiece) select(pos);
            return;
        }
        if (ownPiece && !pos.equals(selection)) { // clickear otra pieza propia cambia la selección
            select(pos);
            return;
        }

        Position origin = selection;
        selection = null;
        render(game.getBoard()); // limpia el resaltado de la selección

        if (origin.equals(pos)) return; // clickear la misma casilla = deseleccionar

        game.tryMove(origin, pos);
        render(game.getBoard());
        playOpponentIfNeeded();
    }

    // Selecciona la pieza de "pos" y marca adónde puede ir, usando las jugadas legales
    // que ofrece el puerto de entrada: amarillo la pieza, ámbar los destinos libres
    // y rojizo los destinos donde captura.
    private void select(Position pos) {
        render(game.getBoard()); // limpia lo que hubiera marcado de una selección anterior
        selection = pos;
        mark(pos, SELECTED, FRAME);
        for (Move move : game.legalMoves()) {
            if (!move.getFrom().equals(pos)) continue;
            Position target = move.getTo();
            boolean captures = game.getBoard().pieceAt(target) != null;
            mark(target, captures ? CAPTURE : LEGAL, captures ? CAPTURE_FRAME : FRAME);
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

    // La IA piensa en otro hilo para no congelar la ventana, y la jugada se
    // aplica después en el hilo de la interfaz. Mientras piensa, se ignoran los clicks.
    private void playOpponentIfNeeded() {
        if (opponent == null || thinking || !opponent.isItsTurn()) return;
        thinking = true;
        levelBox.setEnabled(false);
        status.setText("El oponente piensa...");
        final OpponentController controller = opponent;
        new Thread(() -> {
            Move move = controller.chooseMove();
            SwingUtilities.invokeLater(() -> {
                controller.play(move);
                render(game.getBoard());
                thinking = false;
                levelBox.setEnabled(true);
            });
        }).start();
    }

    // IPromotionChooser: el núcleo pregunta a qué pieza corona el peón.
    // Si corona la IA, elige reina sin abrir el diálogo.
    @Override
    public PieceType choose(Color color) {
        if (opponent != null && color == opponent.getColor()) return PieceType.QUEEN;
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

    // Solo redibuja el tablero. El texto de estado lo manejan exclusivamente
    // los listeners: si render() también lo tocara, pisaría el mensaje que
    // onInvalidMove/onMoveMade acaban de poner.
    public void render(Board board) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                applyIcon(squares[row][col], board.pieceAt(new Position(row, col)));
                squares[row][col].setBackground(squareColor(row, col));
                squares[row][col].setBorderPainted(false); // quita el marco de una selección anterior
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
    public void onCastling(Color color, boolean kingside) {
        status.setText(name(color) + ": enroque " + (kingside ? "corto" : "largo") + ".");
    }

    @Override
    public void onPromotion(Color color, Position position, PieceType newPiece) {
        status.setText(name(color) + ": corona en " + position + " a " + name(newPiece) + ".");
    }

    @Override
    public void onUndo() {
        status.setText("Se deshizo el último movimiento.");
    }

    @Override
    public void onRedo() {
        status.setText("Se rehizo la jugada.");
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
    public void onCheckmate(Color winner) {
        status.setText("¡Jaque mate! Ganan " + name(winner) + ".");
        JOptionPane.showMessageDialog(this, "Jaque mate. Ganan " + name(winner) + ".");
    }

    @Override
    public void onStalemate() {
        status.setText("Ahogado: tablas.");
        JOptionPane.showMessageDialog(this, "Ahogado: tablas, no hay movimientos legales.");
    }

    @Override
    public void onDraw(String reason) {
        status.setText("Tablas: " + reason + ".");
        JOptionPane.showMessageDialog(this, "Tablas: " + reason + ".");
    }
}
