package adapters.swing;

import core.board.Board;
import core.board.Color;
import core.board.Piece;
import core.board.PieceType;
import core.board.Position;
import core.IChessGame;
import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Taskbar;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

// Adaptador de salida Y de entrada (Clase 2): implementa los dos listeners
// igual que ConsoleRenderer (mismos puertos de salida, otra tecnología),
// implementa IPromotionChooser con un diálogo, y además traduce clicks del
// mouse en llamadas a IChessGame.tryMove(): el mismo rol que cumple Main.java
// con el teclado, pero con Swing.
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

    private IChessGame game;
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
        game = builder.build(this);
        selection = null;
        startBoard = game.getBoard().cloneBoard();
        status.setText("Turno de Blancas");
        boardView.setup(game.getBoard());
        pack(); // la ventana mide lo que el tablero y la barra de arriba
    }

    // Empezar otra con una partida empezada y sin terminar la pierde: se pide confirmación.
    private void newGame() {
        if (!game.isGameOver() && !game.getBoard().equals(startBoard)) {
            String[] options = {"Empezar otra", "Seguir jugando"};
            int choice = JOptionPane.showOptionDialog(this, "La partida sigue en juego. Si empezás otra, se pierde.",
                "Nueva partida", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (choice != 0) return;
        }
        startGame();
    }

    private void buildInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        newGameButton.addActionListener(e -> newGame());
        JButton undoButton = new JButton("Deshacer");
        undoButton.addActionListener(e -> {
            game.undo();
            render();
        });
        JButton redoButton = new JButton("Rehacer");
        redoButton.addActionListener(e -> {
            game.redo();
            render();
        });

        // Arriba, una barra con Nueva partida, lo que acaba de pasar y los botones de la partida.
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(undoButton);
        buttons.add(redoButton);
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(newGameButton, BorderLayout.WEST);
        top.add(status, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(boardView, BorderLayout.CENTER);
        setResizable(false);
    }

    private void onSquareClicked(Position pos) {
        if (game.isGameOver()) return;

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
    }

    // Selecciona la pieza de "pos" y el tablero marca adónde puede ir.
    private void select(Position pos) {
        selection = pos;
        boardView.highlight(pos, game.legalMoves());
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
