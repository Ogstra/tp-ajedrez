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

    private static final java.awt.Color CLARA = new java.awt.Color(0xEE, 0xEE, 0xD2);
    private static final java.awt.Color OSCURA = new java.awt.Color(0x76, 0x96, 0x56);
    private static final java.awt.Color SELECCIONADA = new java.awt.Color(0xF6, 0xF6, 0x69);
    private static final int TAMANO_CASILLA = 80;
    private static final int TAMANO_PIEZA = 68; // deja un margen chico dentro de cada casilla

    // Set de piezas "Cburnett" (Wikimedia Commons / lichess.org), CC BY-SA 3.0.
    // Se cargan una sola vez desde src/assets/piezas/ y se escalan al tamaño
    // de casilla. Si falta algún archivo, o la pieza es de un tipo que este
    // adaptador no conoce, se cae a un símbolo de texto.
    private static final Map<String, Image> IMAGENES = cargarImagenes();
    private static final Map<PieceType, String> LETRAS = new HashMap<>();
    private static final Map<PieceType, String> SIMBOLOS_BLANCOS = new HashMap<>();
    private static final Map<PieceType, String> SIMBOLOS_NEGROS = new HashMap<>();
    private static final Map<PieceType, String> NOMBRES = new HashMap<>();

    static {
        definir(PieceType.KING, "K", "♔", "♚", "Rey");
        definir(PieceType.QUEEN, "Q", "♕", "♛", "Reina");
        definir(PieceType.ROOK, "R", "♖", "♜", "Torre");
        definir(PieceType.BISHOP, "B", "♗", "♝", "Alfil");
        definir(PieceType.KNIGHT, "N", "♘", "♞", "Caballo");
        definir(PieceType.PAWN, "P", "♙", "♟", "Peón");
    }

    private static void definir(PieceType type, String letra, String blanco, String negro, String nombre) {
        LETRAS.put(type, letra);
        SIMBOLOS_BLANCOS.put(type, blanco);
        SIMBOLOS_NEGROS.put(type, negro);
        NOMBRES.put(type, nombre);
    }

    private final int filas;
    private final int columnas;
    private final JButton[][] casillas;
    private final JLabel estado = new JLabel("Turno de Blancas", SwingConstants.CENTER);

    private ChessGame game;
    private Position seleccion = null;

    public SwingChessView(int filas, int columnas) {
        super("TP Ajedrez: Ingeniería de Software (UADE)");
        this.filas = filas;
        this.columnas = columnas;
        this.casillas = new JButton[filas][columnas];
        armarInterfaz();
    }

    // El observer se registra en el constructor de ChessGame, pero la vista
    // necesita la referencia al juego para poder mandarle movimientos: por
    // eso se inyecta después, desde el composition root (MainSwing).
    public void attachGame(ChessGame game) {
        this.game = game;
    }

    private static Map<String, Image> cargarImagenes() {
        Map<String, Image> mapa = new HashMap<>();
        String carpeta = "src" + File.separator + "assets" + File.separator + "piezas" + File.separator;
        for (String clave : new String[]{"wK", "wQ", "wR", "wB", "wN", "wP", "bK", "bQ", "bR", "bB", "bN", "bP"}) {
            try {
                BufferedImage imagen = ImageIO.read(new File(carpeta + clave + ".png"));
                mapa.put(clave, imagen);
            } catch (IOException e) {
                mapa.put(clave, null);
            }
        }
        return mapa;
    }

    private void armarInterfaz() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel tablero = new JPanel(new GridLayout(filas, columnas));
        for (int fila = filas - 1; fila >= 0; fila--) {
            for (int col = 0; col < columnas; col++) {
                final Position pos = new Position(fila, col);
                JButton boton = new JButton();
                boton.setBackground(colorDeCasilla(fila, col));
                boton.setOpaque(true);
                boton.setBorderPainted(false);
                boton.addActionListener(e -> onCasillaClickeada(pos));
                casillas[fila][col] = boton;
                tablero.add(boton);
            }
        }

        JButton deshacer = new JButton("Deshacer");
        deshacer.addActionListener(e -> {
            if (game != null && game.undo()) {
                render(game.getBoard());
            }
        });

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(estado, BorderLayout.CENTER);
        abajo.add(deshacer, BorderLayout.EAST);

        add(tablero, BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);
        setSize(columnas * TAMANO_CASILLA, filas * TAMANO_CASILLA + 60);
        setLocationRelativeTo(null);
    }

    private java.awt.Color colorDeCasilla(int fila, int col) {
        return (fila + col) % 2 == 0 ? OSCURA : CLARA;
    }

    private void onCasillaClickeada(Position pos) {
        if (game == null || game.isGameOver()) return;

        if (seleccion == null) {
            Piece pieza = game.getBoard().pieceAt(pos);
            if (pieza != null && pieza.getColor() == game.getTurn()) {
                seleccion = pos;
                casillas[pos.getRow()][pos.getColumn()].setBackground(SELECCIONADA);
            }
            return;
        }

        Position origen = seleccion;
        seleccion = null;
        render(game.getBoard()); // limpia el resaltado de la selección

        if (origen.equals(pos)) return; // clickear la misma casilla = deseleccionar

        game.tryMove(origen, pos);
        render(game.getBoard());
    }

    // IPromotionChooser: el núcleo pregunta a qué pieza corona el peón.
    @Override
    public PieceType choose(Color color) {
        String[] opciones = {"Reina", "Torre", "Alfil", "Caballo"};
        int eleccion = JOptionPane.showOptionDialog(this, "¿A qué pieza corona el peón?",
            "Coronación", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, opciones, opciones[0]);
        switch (eleccion) {
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
        for (int fila = 0; fila < filas; fila++) {
            for (int col = 0; col < columnas; col++) {
                aplicarIcono(casillas[fila][col], board.pieceAt(new Position(fila, col)));
                casillas[fila][col].setBackground(colorDeCasilla(fila, col));
            }
        }
    }

    private void aplicarIcono(JButton boton, Piece pieza) {
        if (pieza == null) {
            boton.setIcon(null);
            boton.setText("");
            return;
        }
        Image imagen = IMAGENES.get(claveImagen(pieza));
        if (imagen == null) {
            boton.setIcon(null);
            boton.setText(simbolo(pieza)); // asset faltante o pieza desconocida: texto
            return;
        }
        boton.setText("");
        boton.setIcon(new ImageIcon(imagen.getScaledInstance(TAMANO_PIEZA, TAMANO_PIEZA, Image.SCALE_SMOOTH)));
    }

    private String claveImagen(Piece piece) {
        String letra = LETRAS.get(piece.getType());
        if (letra == null) return null;
        return (piece.getColor() == Color.WHITE ? "w" : "b") + letra;
    }

    // Símbolo de texto: respaldo si falta un PNG o la pieza no tiene imagen.
    private String simbolo(Piece piece) {
        boolean blanca = piece.getColor() == Color.WHITE;
        String simbolo = (blanca ? SIMBOLOS_BLANCOS : SIMBOLOS_NEGROS).get(piece.getType());
        if (simbolo != null) return simbolo;
        String inicial = piece.getType().getName().substring(0, 1);
        return blanca ? inicial.toUpperCase() : inicial.toLowerCase();
    }

    private String nombre(PieceType type) {
        String nombre = NOMBRES.get(type);
        return nombre != null ? nombre : type.getName();
    }

    private String nombre(Color color) {
        return color == Color.WHITE ? "Blancas" : "Negras";
    }

    @Override
    public void onMoveMade(Position from, Position to, Piece piece, Piece captured) {
        String captura = captured != null ? " (captura " + nombre(captured.getType()) + ")" : "";
        estado.setText(nombre(piece.getColor()) + ": " + nombre(piece.getType())
            + " " + from + " -> " + to + captura);
    }

    @Override
    public void onInvalidMove(String reason) {
        estado.setText("Inválido: " + reason);
    }

    @Override
    public void onCheck(Color colorInCheck) {
        estado.setText("¡Jaque a " + nombre(colorInCheck) + "!");
    }

    @Override
    public void onCheckmate(Color winnerColor) {
        estado.setText("¡Jaque mate! Ganan " + nombre(winnerColor) + ".");
        JOptionPane.showMessageDialog(this, "Jaque mate. Ganan " + nombre(winnerColor) + ".");
    }

    @Override
    public void onStalemate() {
        estado.setText("Ahogado: tablas.");
        JOptionPane.showMessageDialog(this, "Ahogado: tablas, no hay movimientos legales.");
    }

    @Override
    public void onUndo() {
        estado.setText("Se deshizo el último movimiento.");
    }

    @Override
    public void onCastling(Color color, boolean cortoEnEnroque) {
        estado.setText(nombre(color) + ": enroque " + (cortoEnEnroque ? "corto" : "largo") + ".");
    }

    @Override
    public void onPromotion(Color color, Position pos, PieceType nuevaPieza) {
        estado.setText(nombre(color) + ": corona en " + pos + " a " + nombre(nuevaPieza) + ".");
    }
}
