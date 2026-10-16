package nucleo;

import puertos.IGameStatusListener;
import puertos.IMoveListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Núcleo del sistema (Clase 2): orquesta la partida. No sabe nada de
// consola, UI ni ningún detalle técnico: solo reglas de negocio del
// ajedrez. Todo lo que puede variar entra por constructor, inyectado y
// contra una interfaz (Clase 3: DIP + inyección de dependencias nivel 4):
// los listeners (cómo se avisa lo que pasa) y las reglas especiales
// (enroque, coronación, lo que se sume). ChessGame no sabe cuáles son.
public class ChessGame implements IChessGame, IGameContext {
    private final Board board;
    private final List<IMoveListener> moveListeners;
    private final List<IGameStatusListener> statusListeners;
    private final List<ISpecialMove> specialMoves;
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private IGameState state;

    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves) {
        this(board, moveListeners, statusListeners, specialMoves, Color.WHITE);
    }

    // Con firstToMove se arma una partida que arranca con el turno de las negras
    // (la usa la IA para buscar jugadas desde una posición a mitad de partida).
    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves,
                     Color firstToMove) {
        this.board = board;
        this.moveListeners = moveListeners;
        this.statusListeners = statusListeners;
        this.specialMoves = specialMoves;
        this.state = firstToMove == Color.WHITE ? new WhiteTurnState() : new BlackTurnState();
    }

    @Override
    public Board getBoard() { return board; }
    @Override
    public Color getTurn() { return state.currentColor(); }
    @Override
    public boolean isGameOver() { return state.isOver(); }

    @Override
    public MoveOutcome tryMove(Position from, Position to) {
        if (state.isOver()) {
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
        HistoryEntry entry = new HistoryEntry(command, state);
        history.push(entry);

        Color mover = state.currentColor();
        Color opponent = mover.opposite();
        state = state.afterMove();

        for (IMoveListener listener : moveListeners) command.announce(listener);

        boolean check = isInCheck(opponent, board);
        boolean hasMoves = hasAnyLegalMove(opponent);

        MoveOutcome outcome;
        if (check && !hasMoves) {
            state = new CheckmateState(opponent);
            for (IGameStatusListener listener : statusListeners) listener.onCheckmate(mover);
            outcome = MoveOutcome.CHECKMATE;
        } else if (!check && !hasMoves) {
            state = new StalemateState(opponent);
            for (IGameStatusListener listener : statusListeners) listener.onStalemate();
            outcome = MoveOutcome.STALEMATE;
        } else if (check) {
            for (IGameStatusListener listener : statusListeners) listener.onCheck(opponent);
            outcome = MoveOutcome.CHECK;
        } else {
            outcome = MoveOutcome.OK;
        }
        entry.stateAfter = state;
        return outcome;
    }

    @Override
    public boolean undo() {
        if (history.isEmpty()) return false;
        HistoryEntry entry = history.pop();
        entry.command.undo();
        state = entry.stateBefore;
        for (IMoveListener listener : moveListeners) listener.onUndo();
        return true;
    }

    // IGameContext: lo que las reglas especiales pueden preguntar.
    @Override
    public boolean hasBeenTouched(Position pos) {
        for (HistoryEntry entry : history) {
            if (entry.command.touchedSquares().contains(pos)) return true;
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
        for (IGameStatusListener listener : statusListeners) listener.onInvalidMove(reason);
    }

    // Movimientos legales de una pieza: los pseudo-legales de su Strategy
    // (+ los destinos extra de las reglas especiales), filtrando los que
    // dejarían al propio rey en jaque.
    private List<Position> legalMovesFrom(Position from) {
        Piece piece = board.pieceAt(from);
        List<Position> candidates = new ArrayList<>(piece.possibleMoves(board, from));
        for (ISpecialMove rule : specialMoves) {
            candidates.addAll(rule.extraDestinations(this, from));
        }

        List<Position> legal = new ArrayList<>();
        for (Position destination : candidates) {
            Board simulated = board.cloneBoard();
            simulated.movePiece(from, destination);
            if (!isInCheck(piece.getColor(), simulated)) {
                legal.add(destination);
            }
        }
        return legal;
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
    @Override
    public List<Move> legalMoves() {
        List<Move> moves = new ArrayList<>();
        if (state.isOver()) return moves;
        for (Position from : board.positionsOfColor(state.currentColor())) {
            for (Position to : legalMovesFrom(from)) {
                moves.add(new Move(from, to));
            }
        }
        return moves;
    }
}
