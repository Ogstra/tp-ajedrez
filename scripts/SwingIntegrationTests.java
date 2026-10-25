import adaptadores.ChoiceControl;
import adaptadores.FileAccountStore;
import adaptadores.GameBuilder;
import adaptadores.Match;
import adaptadores.SwingChessView;
import nucleo.*;
import nucleo.especiales.*;
import nucleo.tablas.*;
import puertos.*;
import ai.OpponentController;
import ai.OpponentLevel;
import ai.Opponents;
import red.LanOpponent;
import usuarios.AccountService;
import usuarios.ScoreRecorder;
import usuarios.User;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

// Prueba de integracion de la ventana real (menu, usuario, partida y ranking en una sola ventana),
// sin reflection ni librerias de tests.
public class SwingIntegrationTests {
    private static SwingChessView view;
    private static IChessGame game;
    private static JPanel boardPanel;
    private static JButton menuButton;
    private static Board before;
    private static Board after;
    private static AccountService accounts;
    private static User lastWhite;   // los usuarios con que el builder armo la ultima partida
    private static User lastBlack;

    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("ajedrez-swing");
        try {
            SwingUtilities.invokeAndWait(() -> {
                accounts = new AccountService(new FileAccountStore(data.resolve("usuarios.csv")));
                view = new SwingChessView(accounts, builder(accounts, 8));
                view.setVisible(true);
                Container root = view.getContentPane();

                // La ventana arranca en el login: un usuario inexistente se rechaza.
                require(named(root, "loginName").isShowing() && !button(root, "Jugar").isShowing(),
                    "la primera pantalla es el login");
                require(view.getRootPane().getDefaultButton() == button(root, "Entrar"),
                    "en el login, Enter debe apretar Entrar");
                ((JTextField) named(root, "loginName")).setText("Nadie");
                button(root, "Entrar").doClick();
                require(named(root, "loginName").isShowing(), "un usuario inexistente no entra");
                // Registrarse lleva a la vista de registro; Volver regresa al login.
                button(root, "Registrarse").doClick();
                require(named(root, "registerName").isShowing() && !named(root, "loginName").isShowing(),
                    "Registrarse muestra la vista de registro");
                require(view.getRootPane().getDefaultButton() == button(root, "Crear usuario"),
                    "en el registro, Enter debe apretar Crear usuario");
                button(root, "Volver").doClick();
                require(named(root, "loginName").isShowing(), "Volver regresa al login");
                require(view.getRootPane().getDefaultButton() == button(root, "Entrar"),
                    "al volver, Enter aprieta Entrar otra vez");
                // Crear a Ana entra directo al menu.
                button(root, "Registrarse").doClick();
                ((JTextField) named(root, "registerName")).setText("Ana");
                button(root, "Crear usuario").doClick();
                require(accounts.ranking().size() == 1, "el registro debe guardarse");
                require(button(root, "Jugar").isShowing(), "al registrarse se entra al menu");
                require(view.getRootPane().getDefaultButton() == button(root, "Jugar"),
                    "en el menu, Enter debe apretar Jugar");
                // Cerrar sesion vuelve al login; se entra de nuevo con el nombre.
                button(root, "Cerrar sesión").doClick();
                require(named(root, "loginName").isShowing(), "Cerrar sesion vuelve al login");
                ((JTextField) named(root, "loginName")).setText("ana");
                button(root, "Entrar").doClick();
                require(button(root, "Jugar").isShowing(), "Ana entra con su nombre");

                // 1 contra 1: la dificultad y el color no se muestran.
                require(!named(root, "level").isShowing() && !named(root, "color").isShowing(),
                    "en 1 contra 1 no se elige dificultad ni color");
                button(root, "Jugar").doClick();
                inspect(root);
                require(lastWhite == null && lastBlack == null, "en 1 contra 1 se juega sin usuarios");
                require(boardPanel.getComponentCount() == 64, "debe armarse el tablero de 64 casillas");
                require(view.getRootPane().getDefaultButton() == null, "en la partida no hay boton por defecto");
                require(label(root, "a") != null && label(root, "h") != null && label(root, "1") != null && label(root, "8") != null,
                    "el tablero muestra las coordenadas a-h y 1-8");

                // Seleccion y marcas de jugadas legales.
                java.awt.Color normalE3 = background("e3", false);
                java.awt.Color normalE5 = background("e5", false);
                java.awt.Color normalF3 = background("f3", false);
                java.awt.Color normalG3 = background("g3", false);
                square("e2", false).doClick();
                require(!background("e3", false).equals(normalE3), "al seleccionar el peon de e2 se debe marcar e3");
                require(background("e3", false).equals(background("e4", false)) && background("e5", false).equals(normalE5),
                    "al seleccionar el peon de e2 se marcan e3 y e4, y e5 queda igual");
                require(square("e3", false).isBorderPainted() && square("e4", false).isBorderPainted()
                        && !square("e5", false).isBorderPainted(),
                    "cada casilla marcada lleva su marco, para que las pegadas no se fundan");
                square("g1", false).doClick();
                require(background("e3", false).equals(normalE3) && !square("e3", false).isBorderPainted(),
                    "al cambiar de pieza se limpian los destinos anteriores y sus marcos");
                require(!background("f3", false).equals(normalF3) && background("g3", false).equals(normalG3),
                    "el caballo de g1 marca f3 y no marca g3");
                square("g1", false).doClick();
                require(background("f3", false).equals(normalF3), "clickear la misma pieza deselecciona y limpia");

                // Jugada, deshacer y volver al menu.
                square("e2", false).doClick();
                square("e4", false).doClick();
                require(game.getTurn() == nucleo.Color.BLACK, "despues de e2 e4 le toca a las negras");
                button(root, "Deshacer").doClick();
                require(before.equals(game.getBoard()), "deshacer en 1 contra 1 restaura el tablero");
                button(root, "Menú").doClick();

                // Contra la IA (medio) jugando con blancas.
                ((ChoiceControl) named(root, "mode")).setSelectedIndex(1);
                require(named(root, "level").isShowing() && named(root, "color").isShowing(),
                    "contra la IA se eligen dificultad y color");
                ((ChoiceControl) named(root, "level")).setSelectedIndex(1);
                button(root, "Jugar").doClick();
                require(lastWhite != null && "Ana".equals(lastWhite.getName()) && lastBlack == null,
                    "contra la IA con blancas, Ana juega con blancas y la IA no tiene usuario");
                square("e2", false).doClick();
                square("e4", false).doClick();
                require(!menuButton().isEnabled(), "el boton Menu debe bloquearse mientras piensa la IA");
            });
            awaitReply(nucleo.Color.WHITE);
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                require(menuButton().isEnabled(), "el boton Menu debe habilitarse al terminar la busqueda");
                after = game.getBoard().cloneBoard();
                require(!before.equals(after), "deben quedar jugadas la blanca y la negra");
                button(root, "Deshacer").doClick();
                require(before.equals(game.getBoard()), "deshacer contra IA debe restaurar el tablero inicial");
                button(root, "Rehacer").doClick();
                require(after.equals(game.getBoard()), "rehacer debe restaurar ambas jugadas");

                // Salir con la partida empezada pide confirmacion: se elige "Seguir jugando" y no se sale.
                clickLater("Seguir jugando");
                menuButton().doClick();
                require(button(root, "Deshacer").isShowing(), "si se elige Seguir jugando debe quedar la partida");
                // Y "Salir al menu" vuelve al menu.
                clickLater("Salir al menú");
                menuButton().doClick();
                require(button(root, "Jugar").isShowing(), "si se elige Salir al menu debe verse el menu");

                // Dificil, tambien con blancas.
                ((ChoiceControl) named(root, "level")).setSelectedIndex(2);
                button(root, "Jugar").doClick();
                square("d2", false).doClick();
                square("d4", false).doClick();
                require(!menuButton().isEnabled(), "el nivel dificil tambien debe bloquear el boton Menu");
            });
            awaitReply(nucleo.Color.WHITE);
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                // Contra la IA jugando con negras: el tablero se gira y la IA abre la partida.
                clickLater("Salir al menú"); // la partida difícil sigue en juego: sale confirmando
                button(root, "Menú").doClick();
                require(button(root, "Jugar").isShowing(), "debe verse el menu despues de confirmar la salida");
                ((ChoiceControl) named(root, "color")).setSelectedIndex(1);
                button(root, "Jugar").doClick();
                require(lastWhite == null && lastBlack != null && "Ana".equals(lastBlack.getName()),
                    "jugando con negras, Ana va en negras y la IA (blancas) no tiene usuario");
                require(((JButton) boardPanel.getComponent(0)).getIcon() != null
                        && boardPanel.getComponent(0) == square("h1", true),
                    "con negras el tablero se gira: la primera casilla de la grilla es h1");
            });
            awaitReply(nucleo.Color.BLACK);
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                require(!before.equals(game.getBoard()), "la IA con blancas debe abrir la partida");
                button(root, "Deshacer").doClick();
            });
            awaitReply(nucleo.Color.BLACK);

            // Un 1 contra 1 que termina en mate no le suma partidas a Ana.
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                clickLater("Salir al menú");
                button(root, "Menú").doClick();
                ((ChoiceControl) named(root, "mode")).setSelectedIndex(0);
                button(root, "Jugar").doClick();
                square("f2", false).doClick();
                square("f3", false).doClick();
                square("e7", false).doClick();
                square("e5", false).doClick();
                square("g2", false).doClick();
                square("g4", false).doClick();
                square("d8", false).doClick();
                square("h4", false).doClick();
                require(game.isGameOver(), "el mate del loco debe terminar la partida");
                require(accounts.ranking().size() == 1 && accounts.ranking().get(0).getGames() == 0,
                    "un 1 contra 1 no debe sumar partidas a Ana");
            });

            // Ranking dentro de la misma ventana.
            SwingUtilities.invokeAndWait(() -> {
                Container root = view.getContentPane();
                button(root, "Ranking").doClick();
                require(((Container) named(root, "rankingRows")).getComponentCount() == 1,
                    "el ranking debe mostrar una fila por usuario registrado");
                button(root, "Volver").doClick();
            });
            System.out.println("[OK]   Swing: login, menu, 1 contra 1 sin ranking, IA en tres niveles y dos colores, tablero girado, deshacer, rehacer y ranking");

            // Otro tamaño de tablero (lo que puede pedirse en la defensa): la ventana se adapta sola.
            SwingUtilities.invokeAndWait(() -> {
                view.dispose();
                AccountService otherAccounts = new AccountService(new FileAccountStore(data.resolve("otro.csv")));
                view = new SwingChessView(otherAccounts, builder(otherAccounts, 10));
                view.setVisible(true);
                Container root = view.getContentPane();
                button(root, "Entrar como invitado").doClick();
                button(root, "Jugar").doClick();
                inspect(root);
                require(boardPanel.getComponentCount() == 100, "un tablero de 10x10 debe tener 100 casillas");
                JButton corner = (JButton) boardPanel.getComponent(0);
                require(corner.getWidth() == corner.getHeight() && corner.getWidth() > 0,
                    "las casillas deben ser cuadradas: " + corner.getWidth() + "x" + corner.getHeight());
                require(label(root, "j") != null && label(root, "10") != null, "las coordenadas llegan hasta j y 10");
                require(view.getContentPane().getHeight() <= Toolkit.getDefaultToolkit().getScreenSize().height,
                    "la ventana debe entrar en la pantalla");
            });
            System.out.println("[OK]   Swing: tablero de 10x10 con casillas cuadradas y coordenadas hasta j y 10");

            // Por LAN: dos ventanas en esta misma PC. Una crea la partida (invitado, blancas) y la
            // otra (Ana, negras) la elige de la lista. Juegan el mate del loco y a Ana le suma la victoria.
            SwingUtilities.invokeAndWait(() -> view.dispose());
            AccountService hostAccounts = new AccountService(new FileAccountStore(data.resolve("host.csv")));
            AccountService guestAccounts = new AccountService(new FileAccountStore(data.resolve("guest.csv")));
            guestAccounts.register("Ana");
            IChessGame[] lanGames = new IChessGame[2];
            SwingChessView[] lanViews = new SwingChessView[2];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    lanViews[0] = new SwingChessView(hostAccounts, lanBuilder(hostAccounts, lanGames, 0));
                    lanViews[1] = new SwingChessView(guestAccounts, lanBuilder(guestAccounts, lanGames, 1));
                    lanViews[0].setVisible(true);
                    lanViews[1].setVisible(true);
                    Container host = lanViews[0].getContentPane();
                    Container guest = lanViews[1].getContentPane();
                    visibleButton(host, "Entrar como invitado").doClick();
                    ((ChoiceControl) named(host, "mode")).setSelectedIndex(2);
                    visibleButton(host, "Jugar").doClick();
                    require(visibleButton(host, "Crear partida") != null, "Por LAN, Jugar muestra la pantalla de LAN");
                    visibleButton(host, "Crear partida").doClick();
                    require(visibleLabel(host, "Esperando rival...") != null, "al crear la partida se espera rival");
                    ((JTextField) named(guest, "loginName")).setText("Ana");
                    visibleButton(guest, "Entrar").doClick();
                    ((ChoiceControl) named(guest, "mode")).setSelectedIndex(2);
                    visibleButton(guest, "Jugar").doClick();
                });
                waitFor(() -> visibleButton(lanViews[1].getContentPane(), "Partida de invitado") != null,
                    "la partida creada debe aparecer en la lista de la otra ventana");
                SwingUtilities.invokeAndWait(() -> visibleButton(lanViews[1].getContentPane(), "Partida de invitado").doClick());
                waitFor(() -> lanGames[0] != null && lanGames[1] != null
                        && visibleButton(lanViews[0].getContentPane(), "Deshacer") != null
                        && visibleButton(lanViews[1].getContentPane(), "Deshacer") != null,
                    "al unirse, las dos ventanas deben pasar a la partida");
                SwingUtilities.invokeAndWait(() -> {
                    require(!visibleButton(lanViews[0].getContentPane(), "Deshacer").isEnabled(),
                        "por LAN, Deshacer queda desactivado");
                    require(visibleLabel(lanViews[0].getContentPane(), "Blancas: invitado   |   Negras: Ana") != null,
                        "la barra muestra los nombres de los dos jugadores");
                });
                lanMove(lanViews, lanGames, 0, "f2", "f3");
                lanMove(lanViews, lanGames, 1, "e7", "e5");
                lanMove(lanViews, lanGames, 0, "g2", "g4");
                lanMove(lanViews, lanGames, 1, "d8", "h4");
                waitFor(() -> lanGames[0].isGameOver() && lanGames[1].isGameOver(),
                    "el mate debe verse en las dos ventanas");
                SwingUtilities.invokeAndWait(() -> {
                    require(lanGames[0].getBoard().equals(lanGames[1].getBoard()), "los dos tableros deben quedar iguales");
                    require(guestAccounts.ranking().get(0).getPoints() == 3 && hostAccounts.ranking().isEmpty(),
                        "a Ana le suma la victoria por LAN; el invitado no suma");
                });
            } finally {
                SwingUtilities.invokeAndWait(() -> {
                    for (SwingChessView lanView : lanViews) if (lanView != null) lanView.dispose();
                });
            }
            System.out.println("[OK]   Swing: por LAN, lista de partidas, conexion, mate del loco en las dos ventanas y puntos para Ana");
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (view != null) view.dispose(); });
        }
    }

    // Espera a que la IA termine de pensar y sea el turno de la persona (color indicado).
    private static void awaitReply(nucleo.Color humanTurn) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        boolean[] ready = {false};
        while (!ready[0] && System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> ready[0] = game.getTurn() == humanTurn && menuButton().isEnabled());
            if (!ready[0]) Thread.sleep(10);
        }
        require(ready[0], "la IA debe devolver el turno sin congelar la ventana");
    }

    // Dentro de un dialogo modal el hilo de la interfaz queda esperando: este temporizador
    // aprieta el boton con ese texto cuando aparece.
    private static void clickLater(String text) {
        javax.swing.Timer timer = new javax.swing.Timer(200, e -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isShowing()) {
                    JButton found = button(((JDialog) window).getContentPane(), text);
                    if (found != null) found.doClick();
                }
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static JLabel label(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel && text.equals(((JLabel) component).getText())) return (JLabel) component;
            if (component instanceof Container) {
                JLabel found = label((Container) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JButton menuButton() {
        return button(view.getContentPane(), "Menú");
    }

    // El mismo armado que MainSwing, con un tablero de 8x8 o de otro tamaño.
    private static GameBuilder builder(AccountService accounts, int size) {
        return (v, white, black, level, aiColor, lan) -> {
            PieceFactory pieces = PieceFactory.standard();
            Board board = size == 8 ? BoardSetup.standard(pieces) : BoardSetup.custom(size, size, backRow(size), pieces);
            before = board.cloneBoard();
            lastWhite = white;
            lastBlack = black;
            ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
            game = new ChessGame(board, Arrays.<IMoveListener>asList(v, recorder),
                Arrays.<IGameStatusListener>asList(v, recorder),
                Arrays.<ISpecialMove>asList(new CastlingMove(), new EnPassantMove(), new PromotionMove(pieces, v)),
                Arrays.<IDrawRule>asList(new InsufficientMaterialRule(), new FiftyMoveRule(), new ThreefoldRepetitionRule()));
            recorder.attachGame(game);
            OpponentController opponent = level == OpponentLevel.NONE
                ? null : new OpponentController(game, Opponents.create(level, new java.util.Random(7)), aiColor);
            return new Match(game, opponent);
        };
    }

    // El armado de MainSwing para una partida por LAN; guarda cada partida en games[index].
    private static GameBuilder lanBuilder(AccountService accounts, IChessGame[] games, int index) {
        return (v, white, black, level, aiColor, lan) -> {
            PieceFactory pieces = PieceFactory.standard();
            ScoreRecorder recorder = new ScoreRecorder(accounts, white, black);
            List<IMoveListener> moveListeners = new ArrayList<>(Arrays.<IMoveListener>asList(v, recorder));
            if (lan != null) moveListeners.add(lan);
            IChessGame lanGame = new ChessGame(BoardSetup.standard(pieces), moveListeners,
                Arrays.<IGameStatusListener>asList(v, recorder),
                Arrays.<ISpecialMove>asList(new CastlingMove(), new EnPassantMove(),
                    new PromotionMove(pieces, lan != null ? lan : v)),
                Arrays.<IDrawRule>asList(new InsufficientMaterialRule(), new FiftyMoveRule(), new ThreefoldRepetitionRule()));
            recorder.attachGame(lanGame);
            OpponentController opponent = null;
            if (lan != null) {
                lan.attachGame(lanGame);
                opponent = new OpponentController(lanGame, lan, lan.getRivalColor());
            }
            games[index] = lanGame;
            return new Match(lanGame, opponent);
        };
    }

    // Juega una jugada con clicks en la ventana index (0 blancas, 1 negras, que ve el tablero
    // girado) y espera a que la otra ventana la reciba por la red.
    private static void lanMove(SwingChessView[] views, IChessGame[] games, int index, String from, String to) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            lanSquare(views[index], from, index == 1).doClick();
            lanSquare(views[index], to, index == 1).doClick();
        });
        int other = 1 - index;
        waitFor(() -> games[other].getBoard().pieceAt(Position.fromAlgebraic(to)) != null
                && games[other].getBoard().pieceAt(Position.fromAlgebraic(from)) == null,
            "la jugada " + from + " " + to + " debe llegar a la otra ventana");
    }

    private static JButton lanSquare(SwingChessView lanView, String text, boolean flipped) {
        JPanel board = (JPanel) named(lanView.getContentPane(), "board");
        Position position = Position.fromAlgebraic(text);
        int index = flipped
            ? position.getRow() * 8 + (7 - position.getColumn())
            : (7 - position.getRow()) * 8 + position.getColumn();
        return (JButton) board.getComponent(index);
    }

    // Espera (hasta 8 segundos) a que se cumpla la condición, mirándola en el hilo de la interfaz.
    private static void waitFor(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.currentTimeMillis() + 8000;
        boolean[] done = {false};
        while (!done[0] && System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> done[0] = condition.getAsBoolean());
            if (!done[0]) Thread.sleep(50);
        }
        require(done[0], message);
    }

    private static JButton visibleButton(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText()) && component.isShowing()) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton found = visibleButton((Container) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JLabel visibleLabel(Container container, String text) {
        JLabel found = label(container, text);
        return found != null && found.isShowing() ? found : null;
    }

    // Fila trasera de cualquier ancho: la estándar y, si sobran columnas, torres.
    private static PieceType[] backRow(int size) {
        PieceType[] standard = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};
        PieceType[] row = new PieceType[size];
        for (int i = 0; i < size; i++) row[i] = i < standard.length ? standard[i] : PieceType.ROOK;
        return row;
    }

    private static void inspect(Container container) {
        for (Component component : container.getComponents()) {
            if ("board".equals(component.getName())) boardPanel = (JPanel) component;
            if (component instanceof Container) inspect((Container) component);
        }
    }

    private static Component named(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) return component;
            if (component instanceof Container) {
                Component found = named((Container) component, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    // El tablero se arma de nuevo en cada partida, asi que se vuelve a buscar. Con el tablero
    // girado (negras abajo) el orden de la grilla es el inverso.
    private static JButton square(String text, boolean flipped) {
        inspect(view.getContentPane());
        Position position = Position.fromAlgebraic(text);
        int index = flipped
            ? position.getRow() * 8 + (7 - position.getColumn())
            : (7 - position.getRow()) * 8 + position.getColumn();
        return (JButton) boardPanel.getComponent(index);
    }

    private static java.awt.Color background(String text, boolean flipped) {
        return square(text, flipped).getBackground();
    }

    private static JButton button(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) return (JButton) component;
            if (component instanceof Container) {
                JButton found = button((Container) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
