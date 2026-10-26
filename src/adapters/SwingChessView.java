package adapters;

import ai.OpponentController;
import ai.OpponentLevel;
import core.Board;
import core.Color;
import core.IChessGame;
import core.Move;
import core.Piece;
import core.PieceType;
import core.Position;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;
import accounts.AccountService;
import accounts.User;

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
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

// Adaptador de salida Y de entrada (Clase 2): implementa los dos listeners
// igual que ConsoleRenderer (mismos puertos de salida, otra tecnología),
// implementa IPromotionChooser con un diálogo, y además traduce clicks del
// mouse en llamadas a IChessGame.tryMove(): el mismo rol que cumple Main.java
// con el teclado, pero con Swing. El núcleo no cambió para que esto funcione.
public class SwingChessView extends JFrame implements IMoveListener, IGameStatusListener, IPromotionChooser {

    private static final int INITIAL_SQUARE = 80; // al empezar; si el tablero no entra en la pantalla, menos
    private static final Dimension MIN_CONTENT = new Dimension(460, 690); // lo que necesita el menú
    private static final int TOP_BAR_HEIGHT = 52;  // botón de 36 más el margen

    // Nombre de cada pieza para los mensajes. Una pieza nueva sin nombre acá usa el de su tipo.
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
    private static final String GAME = "game";
    private static final String RANKING = "ranking";

    private final AccountService accounts;
    private final GameBuilder builder;
    private final BoardView boardView = new BoardView(this::onSquareClicked);
    private final JLabel status = new JLabel("Turno de Blancas");
    private final JLabel players = new JLabel(" ");
    private final JButton menuButton = new FlatButton("Menú", FlatButton.Style.SECONDARY);
    private final MenuPanel menu;
    private final RankingPanel ranking;

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
        loadIcon();
        showCard(MENU); // la primera pantalla es el menú (y Enter aprieta Jugar)
    }

    // Ícono de la aplicación: Windows y Linux usan el de la ventana; macOS no, el del Dock va
    // aparte con Taskbar. Sin el archivo queda el ícono de Java.
    private void loadIcon() {
        Image icon = BoardView.asset("icono.png");
        if (icon == null) return;
        setIconImage(icon);
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(icon);
        }
    }

    // Arma la partida con lo elegido en el menú y pasa a la pantalla de juego.
    private void startGame() {
        OpponentLevel level = menu.versusAi() ? menu.level() : OpponentLevel.NONE;
        Match match = builder.build(this, menu.whiteUser(), menu.blackUser(), level, menu.aiColor());
        game = match.getGame();
        opponent = match.getOpponent();
        selection = null;
        thinking = false;
        startBoard = game.getBoard().cloneBoard();
        menuButton.setEnabled(true);
        players.setText("Blancas: " + playerName(Color.WHITE) + "   |   Negras: " + playerName(Color.BLACK));
        showStatus("Turno de Blancas");
        Board board = game.getBoard();
        boardView.setup(board, menu.humanColor() == Color.BLACK, initialSquareSize(board));
        fitWindow(board.getRows(), board.getColumns(), boardView.getSquareSize());
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
        menu.refresh();
        showCard(MENU);
    }

    private void showRanking() {
        cardBeforeRanking = currentCard;
        ranking.show(accounts.ranking());
        showCard(RANKING);
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
            render();
            playOpponentIfNeeded(); // si la IA abre la partida y se deshizo su primera jugada, vuelve a jugarla
        });
        JButton redoButton = new FlatButton("Rehacer", FlatButton.Style.SECONDARY);
        redoButton.addActionListener(e -> {
            if (thinking) return;
            if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
            render();
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
        // Los botones de la barra no toman el foco: así no queda marcado Menú al empezar la partida.
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
        // El tablero va centrado a su tamaño, así las casillas siempre son cuadradas. Cuando la
        // ventana cambia de tamaño, el tablero recalcula sus casillas con el espacio que hay.
        JPanel boardHolder = new JPanel(new GridBagLayout());
        boardHolder.add(boardView);
        boardHolder.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { boardView.fitTo(boardHolder.getSize()); }
        });
        gamePanel.add(boardHolder, BorderLayout.CENTER);

        // Fondo crema en toda la ventana (los paneles de adentro son transparentes).
        getContentPane().setBackground(Palette.BACKGROUND);
        for (JPanel panel : new JPanel[]{top, text, buttons, gamePanel, boardHolder}) {
            panel.setOpaque(false);
        }
        gamePanel.setOpaque(true);
        gamePanel.setBackground(Palette.BACKGROUND);
        add(menu, MENU);
        add(gamePanel, GAME);
        add(ranking, RANKING);
        fitWindow(8, 8, INITIAL_SQUARE); // tamaño inicial; cada partida lo ajusta a su tablero

        setLocationRelativeTo(null);
    }

    // Casillas de 80 px al empezar; si el tablero tiene muchas filas o columnas y no entra en la
    // pantalla, más chicas. Después siguen el tamaño de la ventana (BoardView.fitTo).
    private int initialSquareSize(Board board) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension available = new Dimension(screen.width, screen.height - 60 - TOP_BAR_HEIGHT); // 60: barra de título
        return Math.min(INITIAL_SQUARE, BoardView.squareSizeFor(available, board.getRows(), board.getColumns()));
    }

    // La ventana mide el tablero más la columna de números, la barra de arriba y la fila de
    // letras, y nunca menos de lo que necesita el menú.
    private void fitWindow(int rows, int columns, int squareSize) {
        Dimension size = new Dimension(
            Math.max(MIN_CONTENT.width, columns * squareSize + BoardView.COORDINATE_SIZE),
            Math.max(MIN_CONTENT.height, rows * squareSize + TOP_BAR_HEIGHT + BoardView.COORDINATE_SIZE));
        if (size.equals(getContentPane().getPreferredSize()) && isDisplayable()) return;
        getContentPane().setPreferredSize(size);
        pack();
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
        render(); // limpia el resaltado de la selección

        if (origin.equals(pos)) return; // clickear la misma casilla = deseleccionar

        game.tryMove(origin, pos);
        render();
        playOpponentIfNeeded();
    }

    // Selecciona la pieza de "pos" y el tablero marca adónde puede ir.
    private void select(Position pos) {
        selection = pos;
        boardView.highlight(pos, game.legalMoves());
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
                render();
                thinking = false;
                menuButton.setEnabled(true);
            });
        }).start();
    }

    // IPromotionChooser: el núcleo pregunta a qué pieza corona el peón de la persona. Cuando
    // corona la IA no llega acá: MainSwing la envuelve con AutoPromotionChooser (Decorator).
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
    private void render() {
        boardView.render(game.getBoard());
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
