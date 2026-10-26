package adapters;

import accounts.AccountService;
import accounts.User;
import ai.OpponentController;
import ai.OpponentLevel;
import core.Board;
import core.Color;
import core.IChessGame;
import core.Move;
import core.Piece;
import core.PieceType;
import core.Position;
import skins.SkinService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Adaptador de salida Y de entrada (Clase 2): implementa los dos listeners
// igual que ConsoleRenderer (mismos puertos de salida, otra tecnología),
// implementa IPromotionChooser con un diálogo, y además traduce clicks del
// mouse en llamadas a IChessGame.tryMove(): el mismo rol que cumple Main.java
// con el teclado, pero con Swing. El núcleo no cambió para que esto funcione.
//
// Esta clase maneja las pantallas y el turno; el dibujo del tablero está en BoardView
// y el armado de cada partida en el GameBuilder que recibe.
public class SwingChessView extends JFrame implements IGameView {

    private static final int MAX_START_SQUARE = 80; // al empezar, casillas de 80 px si el tablero entra en la pantalla
    private static final Dimension MIN_CONTENT = new Dimension(460, 690); // lo que necesita el menú
    private static final int TOP_BAR_HEIGHT = 52;  // botón de 36 más el margen
    // Tamaños estándar del ícono (como los de un .ico). setIconImages le da la lista a Java y cada
    // sistema usa el que le conviene según dónde lo muestre: 16 en el título, 32 o 48 en la barra
    // de tareas, más grande con la pantalla escalada. No hace falta achicar nada a mano.
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    private static final Map<PieceType, String> NAMES = new HashMap<>();

    static {
        NAMES.put(PieceType.KING, "Rey");
        NAMES.put(PieceType.QUEEN, "Reina");
        NAMES.put(PieceType.ROOK, "Torre");
        NAMES.put(PieceType.BISHOP, "Alfil");
        NAMES.put(PieceType.KNIGHT, "Caballo");
        NAMES.put(PieceType.PAWN, "Peón");
    }

    private static final String MENU = "menu";
    private static final String LOGIN = "login";
    private static final String SKINS = "skins";
    private static final String GAME = "game";
    private static final String RANKING = "ranking";

    private final AccountService accounts;
    private final SkinService skins;
    private final GameBuilder builder;
    private final BoardView boardView = new BoardView(this::onSquareClicked);
    private final JPanel boardHolder = new JPanel(new GridBagLayout());
    private final JLabel status = new JLabel("Turno de Blancas");
    private final JLabel players = new JLabel(" ");
    private final JButton menuButton = new FlatButton("Menú", FlatButton.Style.SECONDARY);
    private final JButton undoButton = new FlatButton("Deshacer", FlatButton.Style.SECONDARY);
    private final JButton redoButton = new FlatButton("Rehacer", FlatButton.Style.SECONDARY);
    private final MenuPanel menu;
    private final LoginPanel login;
    private final SkinsPanel skinsPanel;
    private final RankingPanel ranking;

    private IChessGame game;
    private OpponentController opponent; // null mientras juegan dos personas en la misma PC
    private User user;                  // el que entró en el login; null si entró como invitado
    private User human;                 // el usuario de la persona contra la IA; null en 1 contra 1 o invitado
    private Position selection = null;
    private boolean thinking = false;
    private Board startBoard;        // el tablero al empezar, para saber si hubo jugadas
    private String currentCard = MENU;
    private String cardBeforeRanking = MENU;

    // Una sola ventana con cinco pantallas (login, menú, skins, partida y ranking) que se cambian
    // con un CardLayout. Cada partida nueva la arma el GameBuilder.
    public SwingChessView(AccountService accounts, SkinService skins, GameBuilder builder) {
        super("Ajedrez");
        this.accounts = accounts;
        this.skins = skins;
        this.builder = builder;
        this.menu = new MenuPanel(this::startGame, this::showRanking, this::showSkins, this::logout);
        this.skinsPanel = new SkinsPanel(skins, this::showMenu);
        this.login = new LoginPanel(accounts, this::loggedIn);
        this.ranking = new RankingPanel(() -> showCard(cardBeforeRanking));
        buildInterface();
        loadIcon();
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

    // Ícono de la aplicación, igual en todos los sistemas:
    // - Windows y Linux usan el de la ventana: se le pasan todos los tamaños con setIconImages.
    // - macOS no usa el de la ventana, sino el del Dock, que se pone con Taskbar (Java 9+). En
    //   Windows Taskbar existe pero no admite ICON_IMAGE, por eso se pregunta antes de usarlo.
    // Las imágenes salen del classpath (Assets), así se encuentran desde cualquier carpeta.
    // Sin los archivos queda el ícono de Java.
    private void loadIcon() {
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES) {
            Image icon = Assets.image("icono/icono-" + size + ".png");
            if (icon != null) icons.add(icon);
        }
        if (icons.isEmpty()) return;
        setIconImages(icons);
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(icons.get(icons.size() - 1)); // el Dock usa el más grande
        }
    }

    // Arma la partida con lo elegido en el menú y pasa a la pantalla de juego.
    private void startGame() {
        OpponentLevel level = menu.versusAi() ? menu.level() : OpponentLevel.NONE;
        // Solo contra la IA se usa el usuario: el 1 contra 1 en la misma PC no suma al ranking.
        human = menu.versusAi() ? user : null;
        User white = menu.humanColor() == Color.WHITE ? human : null;
        User black = menu.humanColor() == Color.BLACK ? human : null;
        begin(builder.local(this, white, black, level, menu.aiColor()), menu.humanColor() == Color.BLACK);
    }

    private void begin(Match match, boolean flipped) {
        game = match.getGame();
        opponent = match.getOpponent();
        selection = null;
        thinking = false;
        startBoard = game.getBoard().cloneBoard();
        menuButton.setEnabled(true);
        undoButton.setEnabled(true);
        redoButton.setEnabled(true);
        players.setText(opponent == null ? "1 contra 1"
            : "Blancas: " + playerName(Color.WHITE) + "   |   Negras: " + playerName(Color.BLACK));
        showStatus("Turno de Blancas");
        Board board = game.getBoard();
        // La skin es de quien entró, en cualquier modo.
        boardView.setup(board, flipped, skins.selected(user), startSquareSize(board));
        fitWindow(boardView.getSquareSize(), board.getRows(), board.getColumns());
        // Si la persona ya había agrandado la ventana, fitWindow no la cambia: el tablero ocupa el lugar que hay.
        if (boardHolder.getWidth() > 0) boardView.fitTo(boardHolder.getSize());
        boardView.render(board);
        showCard(GAME);
        playOpponentIfNeeded();
    }

    private String playerName(Color color) {
        if (opponent != null && opponent.getColor() == color) return "la IA";
        return human == null ? "invitado" : human.getName();
    }

    private void showCard(String card) {
        currentCard = card;
        // Enter aprieta Entrar en el login, Jugar en el menú; en el resto no hace nada.
        getRootPane().setDefaultButton(card.equals(MENU) ? menu.getPlayButton()
            : card.equals(LOGIN) ? login.getDefaultButton() : null);
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
        // Para usar las dos filas el texto va como HTML; se escapa el contenido.
        status.setText(fits ? text : "<html>" + escapeHtml(text) + "</html>");
        players.setVisible(fits);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void showMenu() {
        showCard(MENU);
    }

    private void showSkins() {
        skinsPanel.show(user);
        showCard(SKINS);
    }

    private void showRanking() {
        cardBeforeRanking = currentCard;
        ranking.show(accounts.ranking());
        showCard(RANKING);
    }

    // El menú usa las piezas de siempre para mostrar el color de cada lado.
    static Image pieceImage(String key) {
        return SkinImages.load(SkinImages.DEFAULT_FOLDER, key);
    }

    private void buildInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new CardLayout());

        menuButton.addActionListener(e -> leaveGame());
        JButton rankingButton = new FlatButton("Ranking", FlatButton.Style.SECONDARY);
        rankingButton.addActionListener(e -> showRanking());
        undoButton.addActionListener(e -> {
            if (thinking) return;
            // Contra la IA se deshacen las dos: la respuesta de ella y la jugada propia.
            if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
            boardView.render(game.getBoard());
            playOpponentIfNeeded(); // si la IA abre la partida y se deshizo su primera jugada, vuelve a jugarla
        });
        redoButton.addActionListener(e -> {
            if (thinking) return;
            if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
            boardView.render(game.getBoard());
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
        // En la partida se juega con el mouse: los botones no toman el foco, así no aparecen marcados al empezar.
        for (JButton button : new JButton[]{menuButton, undoButton, redoButton, rankingButton}) button.setFocusable(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(undoButton);
        buttons.add(redoButton);
        buttons.add(rankingButton);
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(menuButton, BorderLayout.WEST);
        top.add(text, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.EAST);

        JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(top, BorderLayout.NORTH);
        // El tablero va centrado a su tamaño, así las casillas siempre son cuadradas aunque la
        // ventana no tenga la misma proporción que el tablero. Cuando la ventana cambia de tamaño,
        // el tablero se ajusta al lugar que queda.
        boardHolder.add(boardView);
        boardHolder.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boardView.fitTo(boardHolder.getSize());
            }
        });
        gamePanel.add(boardHolder, BorderLayout.CENTER);

        // Fondo crema en toda la ventana (los paneles de adentro son transparentes).
        getContentPane().setBackground(Palette.BACKGROUND);
        for (JPanel panel : new JPanel[]{top, text, buttons, boardHolder}) panel.setOpaque(false);
        gamePanel.setOpaque(true);
        gamePanel.setBackground(Palette.BACKGROUND);
        add(login, LOGIN);
        add(menu, MENU);
        add(skinsPanel, SKINS);
        add(gamePanel, GAME);
        add(ranking, RANKING);
        fitWindow(MAX_START_SQUARE, 8, 8); // tamaño inicial; cada partida lo ajusta a su tablero

        setLocationRelativeTo(null);
    }

    // Al empezar: casillas de 80 px, o más chicas si el tablero no entra en la pantalla. Así un
    // tablero de otro tamaño se juega sin cambiar nada más. Después la ventana se puede agrandar.
    private int startSquareSize(Board board) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension available = new Dimension(screen.width, screen.height - 60 - TOP_BAR_HEIGHT); // 60: barra de título
        return Math.min(MAX_START_SQUARE, BoardView.squareSizeFor(available, board.getRows(), board.getColumns()));
    }

    // La ventana mide el tablero más la columna de números, la barra de arriba y la fila de
    // letras, y nunca menos de lo que necesita el menú. Se puede agrandar y achicar, pero no
    // por debajo de lo que necesita el menú.
    private void fitWindow(int squareSize, int rows, int columns) {
        Dimension size = new Dimension(
            Math.max(MIN_CONTENT.width, columns * squareSize + BoardView.COORDINATE_SIZE),
            Math.max(MIN_CONTENT.height, rows * squareSize + TOP_BAR_HEIGHT + BoardView.COORDINATE_SIZE));
        if (!size.equals(getContentPane().getPreferredSize()) || !isDisplayable()) {
            getContentPane().setPreferredSize(size);
            pack();
        }
        Insets frame = getInsets();
        setMinimumSize(new Dimension(MIN_CONTENT.width + frame.left + frame.right,
            MIN_CONTENT.height + frame.top + frame.bottom));
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
        boardView.render(game.getBoard()); // limpia el resaltado de la selección

        if (origin.equals(pos)) return; // clickear la misma casilla = deseleccionar

        game.tryMove(origin, pos);
        boardView.render(game.getBoard());
        playOpponentIfNeeded();
    }

    // Selecciona la pieza de "pos" y marca adónde puede ir, usando las jugadas legales
    // que ofrece el puerto de entrada.
    private void select(Position pos) {
        selection = pos;
        boardView.highlight(pos, game.legalMoves());
    }

    // La IA piensa en otro hilo para no congelar la ventana, y la jugada se
    // aplica después en el hilo de la interfaz. Mientras piensa, se ignoran los clicks.
    private void playOpponentIfNeeded() {
        if (opponent == null || thinking || !opponent.isItsTurn()) return;
        thinking = true;
        menuButton.setEnabled(false); // se espera a que termine la IA
        showStatus("El oponente piensa...");
        final OpponentController controller = opponent;
        new Thread(() -> {
            Move move = controller.chooseMove();
            SwingUtilities.invokeLater(() -> {
                controller.play(move);
                boardView.render(game.getBoard());
                thinking = false;
                menuButton.setEnabled(true);
            });
        }, "oponente").start();
    }

    // IPromotionChooser: el núcleo pregunta a qué pieza corona el peón de la persona. Cuando
    // corona la IA no se llega acá: la envuelve AutoPromotionChooser, que elige reina.
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

    // Solo redibuja el tablero. El texto de estado lo manejan exclusivamente
    // los listeners: si render() también lo tocara, pisaría el mensaje que
    // onInvalidMove/onMoveMade acaban de poner.
    public void render(Board board) {
        boardView.render(board);
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
