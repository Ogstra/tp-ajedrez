package adapters.swing;

import adapters.swing.components.FlatButton;
import adapters.swing.components.Palette;
import core.board.Board;
import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import ai.IOpponent;
import ai.OpponentController;
import ai.OpponentLevel;
import core.IChessGame;
import core.Move;
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
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.event.ComponentAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.Image;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Adaptador de entrada y de salida con Swing. Implementa los dos listeners, como
// ConsoleRenderer, elige la pieza de coronación con un diálogo y convierte los clicks
// en llamadas a tryMove(). También maneja el menú, el ranking, el turno de la IA, la lista
// de jugadas, la pista, las tablas, el abandono y la revisión de la partida terminada.
public class SwingChessView extends JFrame implements IMoveListener, IGameStatusListener, IPromotionChooser {

    private static final int INITIAL_SQUARE = 80; // al empezar; si el tablero no entra en la pantalla, menos
    private static final Dimension MIN_CONTENT = new Dimension(460, 690); // lo que necesita el menú
    private static final int TOP_BAR_HEIGHT = 52;  // botón de 36 más el margen
    private static final int SIDE_WIDTH = 210;     // la columna de las jugadas, a la derecha del tablero

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
    private final IOpponent hint;
    private final MoveListPanel moveList = new MoveListPanel(this::reviewMove);
    private final JButton undoButton = new FlatButton("Deshacer", FlatButton.Style.SECONDARY);
    private final JButton redoButton = new FlatButton("Rehacer", FlatButton.Style.SECONDARY);
    private final JButton hintButton = new FlatButton("Pista", FlatButton.Style.SECONDARY);
    private final JButton drawButton = new FlatButton("Tablas", FlatButton.Style.SECONDARY);
    private final JButton resignButton = new FlatButton("Abandonar", FlatButton.Style.SECONDARY);
    private final JButton previousButton = new FlatButton("Anterior", FlatButton.Style.SECONDARY);
    private final JButton nextButton = new FlatButton("Siguiente", FlatButton.Style.SECONDARY);
    private final JLabel reviewLabel = new JLabel(" ");
    private final JPanel playActions = new JPanel(new BorderLayout(0, 6));
    private final JPanel reviewActions = new JPanel(new BorderLayout(0, 6));

    private IChessGame game;
    private OpponentController opponent; // null mientras juegan dos personas
    private Position selection = null;
    private Move hintMove = null;    // la jugada que marcó la pista, hasta el próximo click
    private boolean thinking = false;
    private Board startBoard;        // el tablero al empezar, para saber si hubo jugadas
    private String currentCard = MENU;
    private String cardBeforeRanking = MENU;
    private int reviewIndex = -1;    // la posición que se revisa al terminar; -1 mientras se juega
    private boolean agreed = false;  // terminó por abandono o tablas acordadas: ya no se deshace

    // Una sola ventana con tres pantallas (menú, partida y ranking) que se cambian con un CardLayout.
    // Cada vez que se aprieta Jugar se arma una partida nueva con el GameBuilder. "hint" es la
    // IA que sugiere la jugada cuando se pide una pista.
    public SwingChessView(AccountService accounts, GameBuilder builder, IOpponent hint) {
        super("Ajedrez");
        this.accounts = accounts;
        this.builder = builder;
        this.hint = hint;
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
        reviewIndex = -1;
        agreed = false;
        startBoard = game.getBoard().cloneBoard();
        menuButton.setEnabled(true);
        players.setText("Blancas: " + playerName(Color.WHITE) + "   |   Negras: " + playerName(Color.BLACK));
        showStatus("Turno de Blancas");
        Board board = game.getBoard();
        boardView.setup(board, menu.humanColor() == Color.BLACK, initialSquareSize(board));
        fitWindow(board.getRows(), board.getColumns(), boardView.getSquareSize());
        showCard(GAME);
        refreshSide();
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
        undoButton.addActionListener(e -> {
            if (thinking) return;
            // Contra la IA se deshacen las dos: la respuesta de ella y la jugada propia.
            if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
            render();
            playOpponentIfNeeded(); // si la IA abre la partida y se deshizo su primera jugada, vuelve a jugarla
        });
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
        JPanel side = buildSide();
        gamePanel.add(side, BorderLayout.EAST);

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

    // La columna de la derecha: la lista de jugadas y abajo los botones. Mientras se juega
    // están Pista, y Tablas y Abandonar en una fila; al terminar, Anterior y Siguiente para
    // revisar la partida (también con las flechas del teclado).
    private JPanel buildSide() {
        JLabel title = new JLabel("Jugadas");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(Palette.INK);

        hintButton.addActionListener(e -> showHint());
        drawButton.addActionListener(e -> offerDraw());
        resignButton.addActionListener(e -> resign());
        previousButton.addActionListener(e -> stepReview(-1));
        nextButton.addActionListener(e -> stepReview(1));
        JPanel endings = new JPanel(new GridLayout(1, 2, 6, 0));
        endings.add(drawButton);
        endings.add(resignButton);
        playActions.add(hintButton, BorderLayout.NORTH);
        playActions.add(endings, BorderLayout.SOUTH);
        reviewLabel.setForeground(Palette.MUTED);
        JPanel arrows = new JPanel(new GridLayout(1, 2, 6, 0));
        arrows.add(previousButton);
        arrows.add(nextButton);
        reviewActions.add(reviewLabel, BorderLayout.NORTH);
        reviewActions.add(arrows, BorderLayout.CENTER);
        for (JButton button : new JButton[]{hintButton, drawButton, resignButton, previousButton, nextButton}) {
            button.setFocusable(false);
        }

        JPanel actions = new JPanel(new BorderLayout());
        actions.add(playActions, BorderLayout.NORTH);
        actions.add(reviewActions, BorderLayout.SOUTH);
        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 8));
        side.setPreferredSize(new Dimension(SIDE_WIDTH, 0));
        side.add(title, BorderLayout.NORTH);
        side.add(moveList, BorderLayout.CENTER);
        side.add(actions, BorderLayout.SOUTH);
        for (JPanel panel : new JPanel[]{side, actions, playActions, endings, reviewActions, arrows}) panel.setOpaque(false);

        // Las flechas izquierda y derecha recorren la partida terminada.
        side.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous");
        side.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
        side.getActionMap().put("previous", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { stepReview(-1); }
        });
        side.getActionMap().put("next", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { stepReview(1); }
        });
        return side;
    }

    // Casillas de 80 px al empezar; si el tablero tiene muchas filas o columnas y no entra en la
    // pantalla, más chicas. Después siguen el tamaño de la ventana (BoardView.fitTo).
    private int initialSquareSize(Board board) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension available = new Dimension(screen.width - SIDE_WIDTH, screen.height - 60 - TOP_BAR_HEIGHT); // 60: barra de título
        return Math.min(INITIAL_SQUARE, BoardView.squareSizeFor(available, board.getRows(), board.getColumns()));
    }

    // La ventana mide el tablero más la columna de números, la de las jugadas, la barra de
    // arriba y la fila de letras, y nunca menos de lo que necesita el menú.
    private void fitWindow(int rows, int columns, int squareSize) {
        Dimension size = new Dimension(
            Math.max(MIN_CONTENT.width, columns * squareSize + BoardView.COORDINATE_SIZE + SIDE_WIDTH),
            Math.max(MIN_CONTENT.height, rows * squareSize + TOP_BAR_HEIGHT + BoardView.COORDINATE_SIZE));
        if (size.equals(getContentPane().getPreferredSize()) && isDisplayable()) return;
        getContentPane().setPreferredSize(size);
        pack();
    }

    private void onSquareClicked(Position pos) {
        if (game.isGameOver() || thinking) return;

        // Con una pista marcada, un click en su destino la juega; cualquier otro click sigue
        // como siempre (la pieza de la pista se elige con un solo click).
        if (hintMove != null) {
            Move hinted = hintMove;
            hintMove = null;
            if (selection == null && pos.equals(hinted.getTo())) {
                game.tryMove(hinted.getFrom(), hinted.getTo());
                render();
                playOpponentIfNeeded();
                return;
            }
            render(); // borra las marcas de la pista
        }

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

        if (origin.equals(pos)) return; // clickear la misma casilla deselecciona

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
        refreshSide();
        showStatus("El oponente piensa...");
        final OpponentController controller = opponent;
        new Thread(() -> {
            Move move = controller.chooseMove();
            SwingUtilities.invokeLater(() -> {
                controller.play(move);
                thinking = false;
                menuButton.setEnabled(true);
                render();
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

    // Pide la jugada a la IA de las pistas, en otro hilo como el oponente, y la deja marcada
    // sin elegir la pieza: un click en el destino la juega y un click en la pieza la elige.
    private void showHint() {
        if (thinking || game.isGameOver()) return;
        thinking = true;
        menuButton.setEnabled(false);
        refreshSide();
        showStatus("Buscando una pista...");
        final IChessGame current = game;
        new Thread(() -> {
            Move move = hint.chooseMove(current);
            SwingUtilities.invokeLater(() -> {
                thinking = false;
                menuButton.setEnabled(true);
                refreshSide();
                if (current != game) return; // mientras tanto se empezó otra partida
                selection = null;
                hintMove = move;
                boardView.showHint(move);
                showStatus("Pista: mover de " + move.getFrom() + " a " + move.getTo() + ".");
            });
        }).start();
    }

    // Contra la IA, ella decide si acepta; entre dos personas, el rival contesta en un diálogo.
    private void offerDraw() {
        if (thinking || game.isGameOver()) return;
        Color offering = game.getTurn();
        if (opponent != null) {
            if (opponent.acceptsDraw()) game.agreeDraw();
            else showStatus("La IA rechaza las tablas.");
        } else {
            String[] options = {"Aceptar", "Rechazar"};
            int choice = JOptionPane.showOptionDialog(this,
                "Las " + lower(offering) + " ofrecen tablas. ¿Las " + lower(offering.opposite()) + " aceptan?",
                "Tablas", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if (choice == 0) game.agreeDraw();
            else showStatus("Tablas rechazadas, sigue la partida.");
        }
        agreed = game.isGameOver();
        selection = null;
        render();
    }

    // Abandona el bando que tiene el turno; contra la IA, siempre la persona.
    private void resign() {
        if (thinking || game.isGameOver()) return;
        String[] options = {"Abandonar", "Seguir jugando"};
        int choice = JOptionPane.showOptionDialog(this, "¿Las " + lower(game.getTurn()) + " abandonan la partida?",
            "Abandonar", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice != 0) return;
        game.resign();
        agreed = true;
        selection = null;
        render();
    }

    // Redibuja el tablero y la columna de las jugadas. El texto de estado lo ponen los listeners.
    // Con la partida terminada, el tablero muestra la posición que se está revisando (una
    // copia), que empieza en la última. La última jugada queda teñida en el tablero.
    private void render() {
        hintMove = null; // deshacer, rehacer o una jugada dejan vieja a la pista
        List<Board> positions = game.positions();
        int last = positions.size() - 1;
        if (!game.isGameOver()) reviewIndex = -1;
        else if (reviewIndex < 0 || reviewIndex > last) reviewIndex = last;
        int shown = reviewIndex >= 0 ? reviewIndex : last;
        boardView.setLastMove(shown > 0
            ? changedSquares(positions.get(shown - 1), positions.get(shown))
            : Collections.<Position>emptyList());
        boardView.render(shown < last ? positions.get(shown) : game.getBoard());
        refreshSide();
    }

    private void refreshSide() {
        List<String> moves = game.moveList();
        boolean over = game.isGameOver();
        int shown = over && reviewIndex >= 0 ? reviewIndex : moves.size();
        moveList.show(moves, shown - 1, over);
        for (JButton button : new JButton[]{hintButton, drawButton, resignButton}) button.setEnabled(!over && !thinking);
        undoButton.setEnabled(!agreed && !thinking);
        redoButton.setEnabled(!agreed && !thinking);
        playActions.setVisible(!over);
        reviewActions.setVisible(over);
        if (over) {
            reviewLabel.setText(describe(moves, shown));
            previousButton.setEnabled(shown > 0);
            nextButton.setEnabled(shown < moves.size());
        }
    }

    // Qué posición se está viendo, con la numeración de la lista: "Después de 9... h6".
    private static String describe(List<String> moves, int shown) {
        if (shown == 0) return "Posición inicial";
        int index = shown - 1;
        int number = index / 2 + 1;
        return "Después de " + number + (index % 2 == 0 ? ". " : "... ") + moves.get(index);
    }

    // Las casillas que cambiaron entre dos posiciones: de dónde salió y adónde llegó cada pieza.
    private static List<Position> changedSquares(Board before, Board after) {
        List<Position> changed = new java.util.ArrayList<>();
        for (int row = 0; row < after.getRows(); row++) {
            for (int col = 0; col < after.getColumns(); col++) {
                Position pos = new Position(row, col);
                if (!java.util.Objects.equals(before.pieceAt(pos), after.pieceAt(pos))) changed.add(pos);
            }
        }
        return changed;
    }

    private void stepReview(int delta) {
        if (game == null || !game.isGameOver()) return;
        reviewIndex = Math.max(0, Math.min(game.moveList().size(), reviewIndex + delta));
        render();
    }

    // Click en una jugada de la lista: se ve la posición que quedó después de ella.
    private void reviewMove(int index) {
        if (!game.isGameOver()) return;
        reviewIndex = index + 1;
        render();
    }

    private String lower(Color color) {
        return name(color).toLowerCase();
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

    @Override
    public void onResignation(Color winner) {
        showStatus(name(winner.opposite()) + " abandonan. Ganan " + name(winner) + ".");
    }
}
