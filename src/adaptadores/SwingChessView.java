package adaptadores;

import ai.OpponentController;
import ai.OpponentLevel;
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
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private static final int MAX_SQUARE = 80;    // casillas de 80 px salvo que el tablero no entre en la pantalla
    private static final int MIN_SQUARE = 32;
    private static final Dimension MIN_CONTENT = new Dimension(460, 690); // lo que necesita el menú
    private static final int TOP_BAR_HEIGHT = 52;  // botón de 36 más el margen
    private static final int COORDINATE_SIZE = 18; // ancho de los números y alto de las letras

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
    private static final String LOGIN = "login";
    private static final String GAME = "game";
    private static final String RANKING = "ranking";

    private final AccountService accounts;
    private final GameBuilder builder;
    private final JPanel boardPanel = new JPanel();
    private final JPanel rankLabels = new JPanel();
    private final JPanel fileLabels = new JPanel();
    private final JLabel status = new JLabel("Turno de Blancas");
    private final JLabel players = new JLabel(" ");
    private final JButton menuButton = new FlatButton("Menú", FlatButton.Style.SECONDARY);
    private final MenuPanel menu;
    private final LoginPanel login;
    private final RankingPanel ranking;

    private JButton[][] squares;
    private IChessGame game;
    private OpponentController opponent; // null mientras juegan dos personas
    private User user;                  // el que entró en el login; null si entró como invitado
    private User human;                 // el usuario de la persona contra la IA; null en 1 contra 1 o invitado
    private Position selection = null;
    private boolean thinking = false;
    private int squareSize = MAX_SQUARE;
    private Board startBoard;        // el tablero al empezar, para saber si hubo jugadas
    private String currentCard = MENU;
    private String cardBeforeRanking = MENU;

    // Una sola ventana con cuatro pantallas (login, menú, partida y ranking) que se cambian con un CardLayout.
    // Cada vez que se aprieta Jugar se arma una partida nueva con el GameBuilder.
    public SwingChessView(AccountService accounts, GameBuilder builder) {
        super("Ajedrez");
        this.accounts = accounts;
        this.builder = builder;
        this.menu = new MenuPanel(this::startGame, this::showRanking, this::logout);
        this.login = new LoginPanel(accounts, this::loggedIn);
        this.ranking = new RankingPanel(() -> showCard(cardBeforeRanking));
        buildInterface();
        showCard(LOGIN); // la primera pantalla es el login (y Enter aprieta Entrar)
    }

    private void loggedIn(User user) {
        this.user = user;
        showMenu();
    }

    private void logout() {
        user = null;
        login.clear();
        showCard(LOGIN);
    }

    // Arma la partida con lo elegido en el menú y pasa a la pantalla de juego.
    private void startGame() {
        OpponentLevel level = menu.versusAi() ? menu.level() : OpponentLevel.NONE;
        // Solo contra la IA se usa el usuario: el 1 contra 1 no suma al ranking.
        human = menu.versusAi() ? user : null;
        User white = menu.humanColor() == Color.WHITE ? human : null;
        User black = menu.humanColor() == Color.BLACK ? human : null;
        Match match = builder.build(this, white, black, level, menu.aiColor());
        game = match.getGame();
        opponent = match.getOpponent();
        selection = null;
        thinking = false;
        startBoard = game.getBoard().cloneBoard();
        menuButton.setEnabled(true);
        players.setText(opponent == null ? "1 contra 1"
            : "Blancas: " + playerName(Color.WHITE) + "   |   Negras: " + playerName(Color.BLACK));
        showStatus("Turno de Blancas");
        squareSize = squareSizeFor(game.getBoard());
        buildBoard(game.getBoard(), menu.humanColor() == Color.BLACK);
        fitWindow(game.getBoard().getRows(), game.getBoard().getColumns());
        render(game.getBoard());
        showCard(GAME);
        playOpponentIfNeeded();
    }

    private String playerName(Color color) {
        if (opponent != null && opponent.getColor() == color) return "la IA";
        return human == null ? "invitado" : human.getName();
    }

    private void showCard(String card) {
        currentCard = card;
        // Enter aprieta Entrar en el login y Jugar en el menú; en el resto no hace nada.
        getRootPane().setDefaultButton(card.equals(MENU) ? menu.getPlayButton()
            : card.equals(LOGIN) ? login.getLoginButton() : null);
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

    // El estado va en la primera fila y los jugadores en la segunda. Si el mensaje no entra en
    // una fila, usa las dos (los jugadores se ocultan hasta el próximo mensaje); si tampoco
    // entra en dos, se corta con "...". La barra nunca crece: el tablero no se mueve.
    private void showStatus(String text) {
        int available = status.getWidth();
        java.awt.FontMetrics fm = status.getFontMetrics(status.getFont());
        boolean fits = available == 0 || fm.stringWidth(text) <= available;
        int limit = 2 * available - 70; // margen por las palabras que pasan enteras a la segunda fila
        if (!fits && fm.stringWidth(text) > limit) {
            // Se sacan palabras enteras del final hasta que entre.
            while (fm.stringWidth(text + "...") > limit && text.lastIndexOf(' ') > 0) {
                text = text.substring(0, text.lastIndexOf(' '));
            }
            text += "...";
        }
        status.setText(fits ? text : "<html>" + text + "</html>");
        players.setVisible(fits);
    }

    private void showMenu() {
        showCard(MENU);
    }

    private void showRanking() {
        cardBeforeRanking = currentCard;
        ranking.show(accounts.ranking());
        showCard(RANKING);
    }

    // El menú usa las mismas imágenes para mostrar el color de cada lado.
    static Image pieceImage(String key) {
        return IMAGES.get(key);
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
        JButton rankingButton = new FlatButton("Ranking", FlatButton.Style.SECONDARY);
        rankingButton.addActionListener(e -> showRanking());
        JButton undoButton = new FlatButton("Deshacer", FlatButton.Style.SECONDARY);
        undoButton.addActionListener(e -> {
            if (thinking) return;
            // Contra la IA se deshacen las dos: la respuesta de ella y la jugada propia.
            if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
            render(game.getBoard());
            playOpponentIfNeeded(); // si la IA abre la partida y se deshizo su primera jugada, vuelve a jugarla
        });
        JButton redoButton = new FlatButton("Rehacer", FlatButton.Style.SECONDARY);
        redoButton.addActionListener(e -> {
            if (thinking) return;
            if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
            render(game.getBoard());
            playOpponentIfNeeded();
        });

        // Una sola barra arriba, del alto de un botón: Menú, el texto en dos filas (qué pasó y
        // quiénes juegan) y los botones de la partida. Así el tablero ocupa todo el resto.
        status.setFont(status.getFont().deriveFont(Font.BOLD, 13f));
        status.setForeground(Palette.INK);
        players.setFont(players.getFont().deriveFont(12f));
        players.setForeground(Palette.MUTED);
        JPanel text = new JPanel(new BorderLayout());
        text.setPreferredSize(new Dimension(0, 36)); // siempre dos filas como máximo
        text.add(status, BorderLayout.CENTER);
        text.add(players, BorderLayout.SOUTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(undoButton);
        buttons.add(redoButton);
        buttons.add(rankingButton);
        // En la partida se juega con el mouse: los botones no toman el foco, así no aparecen marcados al empezar.
        for (JButton button : new JButton[]{menuButton, undoButton, redoButton, rankingButton}) button.setFocusable(false);
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(menuButton, BorderLayout.WEST);
        top.add(text, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.EAST);

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
        // El tablero va centrado a su tamaño, así las casillas siempre son cuadradas aunque la
        // ventana sea más grande (por ejemplo, un tablero chico con el alto mínimo del menú).
        boardPanel.setName("board");
        JPanel boardHolder = new JPanel(new GridBagLayout());
        boardHolder.add(boardArea);
        gamePanel.add(boardHolder, BorderLayout.CENTER);

        // Fondo crema en toda la ventana (los paneles de adentro son transparentes).
        getContentPane().setBackground(Palette.BACKGROUND);
        for (JPanel panel : new JPanel[]{top, text, buttons, gamePanel, boardHolder, boardArea, fileRow, rankLabels, fileLabels}) {
            panel.setOpaque(false);
        }
        gamePanel.setOpaque(true);
        gamePanel.setBackground(Palette.BACKGROUND);
        add(login, LOGIN);
        add(menu, MENU);
        add(gamePanel, GAME);
        add(ranking, RANKING);
        fitWindow(8, 8); // tamaño inicial; cada partida lo ajusta a su tablero

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
        boardPanel.setPreferredSize(new Dimension(columns * squareSize, rows * squareSize));
        boardPanel.revalidate();
        buildCoordinates(rows, columns, flipped);
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
        rankLabels.revalidate();
        fileLabels.revalidate();
    }

    // Casillas de 80 px; si el tablero tiene muchas filas o columnas y no entra en la pantalla,
    // se achican. Así un tablero de otro tamaño se juega sin cambiar nada más.
    private int squareSizeFor(Board board) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int byHeight = (screen.height - 60 - TOP_BAR_HEIGHT - COORDINATE_SIZE) / board.getRows(); // 60: barra de título
        int byWidth = (screen.width - COORDINATE_SIZE) / board.getColumns();
        return Math.max(MIN_SQUARE, Math.min(MAX_SQUARE, Math.min(byHeight, byWidth)));
    }

    // La ventana mide el tablero más la columna de números, la barra de arriba y la fila de
    // letras, y nunca menos de lo que necesita el menú.
    private void fitWindow(int rows, int columns) {
        Dimension size = new Dimension(
            Math.max(MIN_CONTENT.width, columns * squareSize + COORDINATE_SIZE),
            Math.max(MIN_CONTENT.height, rows * squareSize + TOP_BAR_HEIGHT + COORDINATE_SIZE));
        if (size.equals(getContentPane().getPreferredSize()) && isDisplayable()) return;
        getContentPane().setPreferredSize(size);
        pack();
    }

    private static JLabel coordinate(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Palette.MUTED);
        return label;
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
        showStatus("El oponente piensa...");
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
        button.setIcon(new ImageIcon(image.getScaledInstance(squareSize * 85 / 100, squareSize * 85 / 100, Image.SCALE_SMOOTH)));
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
        showStatus(name(piece.getColor()) + ": " + name(piece.getType())
            + " " + from + " -> " + to + captureText);
    }

    @Override
    public void onCastling(Color color, boolean kingside) {
        showStatus(name(color) + ": enroque " + (kingside ? "corto" : "largo") + ".");
    }

    @Override
    public void onPromotion(Color color, Position position, PieceType newPiece) {
        showStatus(name(color) + ": corona en " + position + " a " + name(newPiece) + ".");
    }

    @Override
    public void onUndo() {
        showStatus("Se deshizo el último movimiento.");
    }

    @Override
    public void onRedo() {
        showStatus("Se rehizo la jugada.");
    }

    @Override
    public void onInvalidMove(String reason) {
        showStatus("Inválido: " + reason);
    }

    @Override
    public void onCheck(Color colorInCheck) {
        showStatus("¡Jaque a " + name(colorInCheck) + "!");
    }

    @Override
    public void onCheckmate(Color winner) {
        showStatus("¡Jaque mate! Ganan " + name(winner) + ".");
    }

    @Override
    public void onStalemate() {
        showStatus("Ahogado: tablas.");
    }

    @Override
    public void onDraw(String reason) {
        showStatus("Tablas: " + reason + ".");
    }
}
