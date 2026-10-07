package nucleo;

import puertos.IGameObserver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Núcleo del sistema (Clase 2): orquesta la partida. No sabe nada de
// consola, UI ni ningún detalle técnico: solo reglas de negocio del
// ajedrez. Todo lo que puede variar entra por constructor, inyectado y
// contra una interfaz (Clase 3: DIP + inyección de dependencias nivel 4):
// los observers (cómo se avisa lo que pasa) y las reglas especiales (enroque,
// coronación, lo que se sume). ChessGame no sabe cuáles son ni cuántas.
public class ChessGame implements IGameContext {
    private final Board board;
    private final List<IGameObserver> observers;
    private final List<ISpecialMove> specialMoves;
    private final Deque<IMoveCommand> history = new ArrayDeque<>();
    private IGameState state = new WhiteTurnState();
    private boolean gameOver = false;

    public ChessGame(Board board, List<IGameObserver> observers, List<ISpecialMove> specialMoves) {
        this.board = board;
        this.observers = observers;
        this.specialMoves = specialMoves;
    }

    @Override
    public Board getBoard() { return board; }
    public Color getTurn() { return state.currentColor(); }
    public boolean isGameOver() { return gameOver; }

    public MoveOutcome tryMove(Position from, Position to) {
        if (gameOver) {
            notifyInvalid("La partida ya terminó.");
            return MoveOutcome.INVALID;
        }
        if (!board.isInside(from) || !board.isInside(to)) {
            notifyInvalid("Esa casilla no existe en el tablero.");
            return MoveOutcome.INVALID;
        }

        Piece piece = board.pieceAt(from);
        if (piece == null || piece.getColor() != state.currentColor()) {
            notifyInvalid("En " + from + " no hay una pieza propia para mover.");
            return MoveOutcome.INVALID;
        }

        if (!legalMovesFrom(from).contains(to)) {
            notifyInvalid("Movimiento inválido de " + from + " a " + to + ".");
            return MoveOutcome.INVALID;
        }

        IMoveCommand command = createCommand(from, to);
        command.execute();
        history.push(command);

        Color turnoActual = state.currentColor();
        Color oponente = turnoActual.opposite();
        state = state.handleMove();

        for (IGameObserver o : observers) command.announce(o);

        boolean jaque = isInCheck(oponente, board);
        boolean tieneMovimientos = hasAnyLegalMove(oponente);

        if (jaque && !tieneMovimientos) {
            gameOver = true;
            for (IGameObserver o : observers) o.onCheckmate(turnoActual);
            return MoveOutcome.CHECKMATE;
        }
        if (!jaque && !tieneMovimientos) {
            gameOver = true;
            for (IGameObserver o : observers) o.onStalemate();
            return MoveOutcome.STALEMATE;
        }
        if (jaque) {
            for (IGameObserver o : observers) o.onCheck(oponente);
            return MoveOutcome.CHECK;
        }
        return MoveOutcome.OK;
    }

    public boolean undo() {
        if (history.isEmpty() || gameOver) return false;
        IMoveCommand command = history.pop();
        command.undo();
        state = state.handleMove(); // alternar el turno dos veces vuelve al mismo: sirve para deshacer.
        for (IGameObserver o : observers) o.onUndo();
        return true;
    }

    // IGameContext: lo que las reglas especiales pueden preguntar.
    @Override
    public boolean hasBeenTouched(Position pos) {
        for (IMoveCommand command : history) {
            if (command.touchedSquares().contains(pos)) return true;
        }
        return false;
    }

    @Override
    public boolean isAttacked(Position pos, Color color) {
        return isAttackedOn(board, pos, color);
    }

    // La primera regla especial que reconozca la jugada arma su comando; si
    // ninguna, es un movimiento común.
    private IMoveCommand createCommand(Position from, Position to) {
        for (ISpecialMove rule : specialMoves) {
            IMoveCommand command = rule.commandFor(this, from, to);
            if (command != null) return command;
        }
        return new MoveCommand(board, from, to);
    }

    private void notifyInvalid(String reason) {
        for (IGameObserver o : observers) o.onInvalidMove(reason);
    }

    // Movimientos legales de una pieza: los pseudo-legales de su Strategy
    // (+ los destinos extra de las reglas especiales), filtrando los que
    // dejarían al propio rey en jaque.
    private List<Position> legalMovesFrom(Position from) {
        Piece piece = board.pieceAt(from);
        List<Position> candidatos = new ArrayList<>(piece.possibleMoves(board, from));
        for (ISpecialMove rule : specialMoves) {
            candidatos.addAll(rule.extraDestinations(this, from));
        }

        List<Position> legales = new ArrayList<>();
        for (Position destino : candidatos) {
            Board simulado = board.cloneBoard();
            simulado.movePiece(from, destino);
            if (!isInCheck(piece.getColor(), simulado)) {
                legales.add(destino);
            }
        }
        return legales;
    }

    // ¿El rey de "color" está atacado por alguna pieza rival, en "b"?
    private boolean isInCheck(Color color, Board b) {
        Position kingPos = b.findKing(color);
        if (kingPos == null) return false; // no debería pasar en una partida real
        return isAttackedOn(b, kingPos, color);
    }

    private boolean isAttackedOn(Board b, Position pos, Color color) {
        for (Position p : b.positionsOfColor(color.opposite())) {
            Piece rival = b.pieceAt(p);
            if (rival.attackedSquares(b, p).contains(pos)) {
                return true;
            }
        }
        return false;
    }

    // ¿"color" tiene al menos un movimiento legal disponible? Se usa para
    // detectar jaque mate (si está en jaque y no tiene movimientos) y
    // ahogado (si no está en jaque pero tampoco tiene movimientos).
    private boolean hasAnyLegalMove(Color color) {
        for (Position pos : board.positionsOfColor(color)) {
            if (!legalMovesFrom(pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
