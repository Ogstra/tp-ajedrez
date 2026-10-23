package adaptadores;

import ai.OpponentController;
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
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
    private static final int COORDINATE_SIZE = 18; // ancho de los números y alto de las letras
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

    private static final String MENU = "menu";
    private static final String GAME = "game";
    private static final String RANKING = "ranking";

    private final AccountService accounts;
    private final GameBuilder builder;
    private final Random random = new Random();
    private final JPanel boardPanel = new JPanel();
    private final JPanel rankLabels = new JPanel();
    private final JPanel fileLabels = new JPanel();
    private final JLabel status = new JLabel("Turno de Blancas", SwingConstants.CENTER);
    private final JLabel players = new JLabel(" ", SwingConstants.CENTER);
    private final JButton menuButton = new JButton("Menú");
    private final MenuPanel menu;
    private final RankingPanel ranking;

    private JButton[][] squares;
    private IChessGame game;
    private OpponentController opponent; // null mientras juegan dos personas
    private Position selection = null;
    private boolean thinking = false;
    private Board startBoard;        // el tablero al empezar, para saber si hubo jugadas
    private String currentCard = MENU;
    private String cardBeforeRanking = MENU;

    // Una sola ventana con tres pantallas (menú, partida y ranking) que se cambian con un CardLayout.
    // Cada vez que se aprieta Jugar se arma una partida nueva con el GameBuilder.
    public SwingChessView(AccountService accounts, GameBuilder builder) {
        super("Ajedrez");
        this.accounts = accounts;
        this.builder = builder;
        this.menu = new MenuPanel(accounts, this::startGame, this::showRanking);
        this.ranking = new RankingPanel(() -> showCard(cardBeforeRanking));
        buildInterface();
        showCard(MENU); // la primera pantalla es el menú (y Enter aprieta Jugar)
    }

    // Arma la partida con lo elegido en el menú y pasa a la pantalla de juego.
    private void startGame() {
        ScoreRecorder recorder = new ScoreRecorder(accounts, menu.whiteUser(), menu.blackUser());
        game = builder.build(this, recorder);
        recorder.attachGame(game);
        opponent = menu.versusAi() ? new OpponentController(game, Opponents.create(menu.level(), random), menu.aiColor()) : null;
        selection = null;
        thinking = false;
        startBoard = game.getBoard().cloneBoard();
        menuButton.setEnabled(true);
        players.setText("Blancas: " + playerName(Color.WHITE) + "   |   Negras: " + playerName(Color.BLACK));
        status.setText("Turno de Blancas");
        buildBoard(game.getBoard(), menu.humanColor() == Color.BLACK);
        render(game.getBoard());
        showCard(GAME);
        playOpponentIfNeeded();
    }

    private String playerName(Color color) {
        if (opponent != null && opponent.getColor() == color) return "la IA";
        User user = color == Color.WHITE ? menu.whiteUser() : menu.blackUser();
        return user == null ? "sin usuario" : user.getName();
    }

    private void showCard(String card) {
        currentCard = card;
        // Enter inicia la partida solo mientras se ve el menú.
        getRootPane().setDefaultButton(card.equals(MENU) ? menu.getPlayButton() : null);
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), card);
    }

    // Salir con una partida empezada y sin terminar la pierde: se pide confirmación.
    private void leaveGame() {
        if (!game.isGameOver() && !game.getBoard().equals(startBoard)) {
            String[] options = {"Salir al menú", "Seguir jugando"};
            int choice = JOptionPane.showOptionDialog(this, "La partida sigue en juego. Si salís, se pierde.",
                "Salir al menú", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (choice != 0) return;
        }
        showMenu();
    }

    private void showMenu() {
        menu.refresh();
        showCard(MENU);
    }

    private void showRanking() {
        cardBeforeRanking = currentCard;
        ranking.show(accounts.ranking());
        showCard(RANKING);
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
        setLayout(new CardLayout());

        menuButton.addActionListener(e -> leaveGame());
        // Arriba: el botón Menú y quiénes juegan.
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        top.add(menuButton, BorderLayout.WEST);
        top.add(players, BorderLayout.CENTER);

        JButton rankingButton = new JButton("Ranking");
        rankingButton.addActionListener(e -> showRanking());
        JButton undoButton = new JButton("Deshacer");
        undoButton.addActionListener(e -> {
            if (thinking) return;
            // Contra la IA se deshacen las dos: la respuesta de ella y la jugada propia.
            if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
            render(game.getBoard());
            playOpponentIfNeeded(); // si la IA abre la partida y se deshizo su primera jugada, vuelve a jugarla
        });
        JButton redoButton = new JButton("Rehacer");
        redoButton.addActionListener(e -> {
            if (thinking) return;
            if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
            render(game.getBoard());
            playOpponentIfNeeded();
        });

        // Abajo: el estado de la partida y los botones, centrados.
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(undoButton);
        buttons.add(redoButton);
        buttons.add(rankingButton);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        bottom.add(status, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);

        JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(top, BorderLayout.NORTH);
        // El tablero con las coordenadas: números a la izquierda y letras abajo.
        JPanel boardArea = new JPanel(new BorderLayout());
        JPanel fileRow = new JPanel(new BorderLayout());
        fileRow.add(Box.createHorizontalStrut(COORDINATE_SIZE), BorderLayout.WEST);
        fileRow.add(fileLabels, BorderLayout.CENTER);
        boardArea.add(boardPanel, BorderLayout.CENTER);
        boardArea.add(rankLabels, BorderLayout.WEST);
        boardArea.add(fileRow, BorderLayout.SOUTH);
        gamePanel.add(boardArea, BorderLayout.CENTER);
        gamePanel.add(bottom, BorderLayout.SOUTH);

        add(menu, MENU);
        add(gamePanel, GAME);
        add(ranking, RANKING);
        setSize(8 * SQUARE_SIZE, 8 * SQUARE_SIZE + 150);
        setLocationRelativeTo(null);
    }

    // Arma las casillas del tablero. Si la persona juega con negras se gira, para verlas abajo.
    private void buildBoard(Board board, boolean flipped) {
        int rows = board.getRows();
        int columns = board.getColumns();
        squares = new JButton[rows][columns];
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                final Position pos = new Position(flipped ? i : rows - 1 - i, flipped ? columns - 1 - j : j);
                JButton button = new JButton();
                button.setOpaque(true);
                button.setBorderPainted(false);
                button.addActionListener(e -> onSquareClicked(pos));
                squares[pos.getRow()][pos.getColumn()] = button;
                boardPanel.add(button);
            }
        }
        boardPanel.revalidate();
        buildCoordinates(rows, columns, flipped);
    }

    // Números (filas) y letras (columnas) en el mismo orden en que se ven las casillas.
    private void buildCoordinates(int rows, int columns, boolean flipped) {
        rankLabels.removeAll();
        rankLabels.setLayout(new GridLayout(rows, 1));
        rankLabels.setPreferredSize(new Dimension(COORDINATE_SIZE, 0));
        for (int i = 0; i < rows; i++) {
            rankLabels.add(new JLabel(String.valueOf(flipped ? i + 1 : rows - i), SwingConstants.CENTER));
        }
        fileLabels.removeAll();
        fileLabels.setLayout(new GridLayout(1, columns));
        fileLabels.setPreferredSize(new Dimension(0, COORDINATE_SIZE));
        for (int j = 0; j < columns; j++) {
            fileLabels.add(new JLabel(String.valueOf((char) ('a' + (flipped ? columns - 1 - j : j))), SwingConstants.CENTER));
        }
        rankLabels.revalidate();
        fileLabels.revalidate();
    }

    private java.awt.Color squareColor(int row, int col) {
        return (row + col) % 2 == 0 ? DARK : LIGHT;
    }

    private void onSquareClicked(Position pos) {
        if (game.isGameOver() || thinking) return;

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
        menuButton.setEnabled(false); // no se puede salir mientras la IA piensa
        status.setText("El oponente piensa...");
        final OpponentController controller = opponent;
        new Thread(() -> {
            Move move = controller.chooseMove();
            SwingUtilities.invokeLater(() -> {
                controller.play(move);
                render(game.getBoard());
                thinking = false;
                menuButton.setEnabled(true);
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
        for (int row = 0; row < squares.length; row++) {
            for (int col = 0; col < squares[row].length; col++) {
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
    }

    @Override
    public void onStalemate() {
        status.setText("Ahogado: tablas.");
    }

    @Override
    public void onDraw(String reason) {
        status.setText("Tablas: " + reason + ".");
    }
}
