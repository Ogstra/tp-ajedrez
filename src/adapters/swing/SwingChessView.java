package adapters.swing;

import ai.OpponentController;
import ai.OpponentLevel;
import core.board.Board;
import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import core.IChessGame;
import core.Move;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Taskbar;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

// Adaptador de entrada y de salida con Swing. Implementa los dos listeners, como
// ConsoleRenderer, elige la pieza de coronación con un diálogo y convierte los clicks
// en llamadas a tryMove().
public class SwingChessView extends JFrame implements IMoveListener, IGameStatusListener, IPromotionChooser {

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

    private final GameBuilder builder;
    private final BoardView boardView = new BoardView(this::onSquareClicked);
    private final JLabel status = new JLabel("Turno de Blancas");
    private final JButton newGameButton = new JButton("Nueva partida");
    private final JButton undoButton = new JButton("Deshacer");
    private final JButton redoButton = new JButton("Rehacer");
    // Mismo orden que OpponentLevel: NONE, EASY, MEDIUM, HARD.
    private final JComboBox<String> levelBox = new JComboBox<>(new String[]{"Dos jugadores", "IA fácil", "IA media", "IA difícil"});

    private IChessGame game;
    private OpponentController opponent; // null cuando juegan dos personas
    private OpponentLevel level = OpponentLevel.NONE;
    private boolean thinking = false;
    private Position selection = null;
    private Board startBoard;        // el tablero al empezar, para saber si hubo jugadas

    // Una sola ventana con la partida. Cada partida nueva la arma el GameBuilder.
    public SwingChessView(GameBuilder builder) {
        super("Ajedrez");
        this.builder = builder;
        buildInterface();
        loadIcon();
        startGame();
        setLocationRelativeTo(null); // centrada, ya con su tamaño
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

    // Arma una partida nueva y la muestra.
    private void startGame() {
        Match match = builder.build(this, level);
        game = match.getGame();
        opponent = match.getOpponent();
        selection = null;
        startBoard = game.getBoard().cloneBoard();
        status.setText("Turno de Blancas");
        boardView.setup(game.getBoard());
        pack(); // la ventana mide lo que el tablero y la barra de arriba
    }

    // Empezar otra con una partida empezada y sin terminar la pierde: se pide confirmación.
    private void newGame() {
        if (confirmLosingGame()) startGame();
    }

    // Cambiar el rival empieza una partida nueva con ese rival.
    private void changeLevel() {
        OpponentLevel chosen = OpponentLevel.values()[levelBox.getSelectedIndex()];
        if (chosen == level) return;
        if (!confirmLosingGame()) {
            levelBox.setSelectedIndex(level.ordinal()); // se queda con el rival de antes
            return;
        }
        level = chosen;
        startGame();
    }

    // Si hay una partida empezada y sin terminar, pregunta antes de perderla.
    private boolean confirmLosingGame() {
        if (game.isGameOver() || game.getBoard().equals(startBoard)) return true;
        String[] options = {"Empezar otra", "Seguir jugando"};
        int choice = JOptionPane.showOptionDialog(this, "La partida sigue en juego. Si empezás otra, se pierde.",
            "Nueva partida", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    private void buildInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        newGameButton.addActionListener(e -> newGame());
        levelBox.addActionListener(e -> changeLevel());
        // Contra la IA se deshacen dos: su respuesta y la jugada de la persona. Lo mismo al rehacer.
        undoButton.addActionListener(e -> {
            if (game.undo() && opponent != null && opponent.isItsTurn()) game.undo();
            render();
            playOpponentIfNeeded(); // si quedó el turno de la IA, por ejemplo al deshacer un mate suyo
        });
        redoButton.addActionListener(e -> {
            if (game.redo() && opponent != null && opponent.isItsTurn()) game.redo();
            render();
            playOpponentIfNeeded();
        });

        // Arriba, una barra con Nueva partida, el rival y los botones de la partida, y abajo
        // de ella lo que acaba de pasar.
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        status.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(newGameButton);
        left.add(levelBox);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(undoButton);
        buttons.add(redoButton);
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(left, BorderLayout.WEST);
        top.add(buttons, BorderLayout.EAST);
        top.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(boardView, BorderLayout.CENTER);
        setResizable(false);
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

        if (origin.equals(pos)) return; // clickear la misma casilla deselecciona

        game.tryMove(origin, pos);
        render();
        playOpponentIfNeeded();
    }

    // La IA piensa en otro hilo para no congelar la ventana, y la jugada se aplica después
    // en el hilo de la interfaz. Mientras piensa no se puede tocar el tablero ni los botones.
    private void playOpponentIfNeeded() {
        if (opponent == null || !opponent.isItsTurn()) return;
        setThinking(true);
        status.setText("La IA está pensando...");
        final OpponentController controller = opponent;
        new Thread(() -> {
            Move move = controller.chooseMove();
            SwingUtilities.invokeLater(() -> {
                controller.play(move);
                render();
                setThinking(false);
            });
        }).start();
    }

    private void setThinking(boolean value) {
        thinking = value;
        newGameButton.setEnabled(!value);
        levelBox.setEnabled(!value);
        undoButton.setEnabled(!value);
        redoButton.setEnabled(!value);
    }

    // Selecciona la pieza de "pos" y el tablero marca adónde puede ir.
    private void select(Position pos) {
        selection = pos;
        boardView.highlight(pos, game.legalMoves());
    }

    // IPromotionChooser: el núcleo pregunta a qué pieza corona el peón de la persona. Cuando
    // corona la IA no llega acá: MainSwing usa AutoPromotionChooser.
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

    // Solo redibuja el tablero. El texto de estado lo ponen los listeners.
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
