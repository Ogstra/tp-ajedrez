package core;

import ports.IGameStatusListener;
import ports.IMoveListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Núcleo del sistema (Clase 2): orquesta la partida. No sabe nada de
// consola, UI ni ningún detalle técnico: solo reglas de negocio del
// ajedrez. Todo lo que puede variar entra por constructor, inyectado y
// contra una interfaz (Clase 3: DIP + inyección de dependencias nivel 4):
// los listeners (cómo se avisa lo que pasa), las reglas especiales
// (enroque, coronación, captura al paso) y las reglas de tablas.
// ChessGame no sabe cuáles son ni cuántas.
public class ChessGame implements IChessGame, IGameContext {
    private final Board board;
    private final List<IMoveListener> moveListeners;
    private final List<IGameStatusListener> statusListeners;
    private final List<ISpecialMove> specialMoves;
    private final List<IDrawRule> drawRules;
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();
    private final Board initialBoard;
    private final Color initialColor;
    private IGameState state;

    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves,
                     List<IDrawRule> drawRules) {
        this(board, moveListeners, statusListeners, specialMoves, drawRules, Color.WHITE);
    }

    // Con firstToMove se arma una partida que arranca con el turno de las negras
    // (la usa la IA para buscar jugadas desde una posición a mitad de partida).
    public ChessGame(Board board, List<IMoveListener> moveListeners,
                     List<IGameStatusListener> statusListeners, List<ISpecialMove> specialMoves,
                     List<IDrawRule> drawRules, Color firstToMove) {
        this.board = board;
        this.moveListeners = moveListeners;
        this.statusListeners = statusListeners;
        this.specialMoves = specialMoves;
        this.drawRules = drawRules;
        this.initialBoard = board.cloneBoard();
        this.initialColor = firstToMove;
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

        redoStack.clear(); // una jugada nueva borra lo que se podía rehacer
        IMoveCommand command = createCommand(from, to);
        command.execute();
        HistoryEntry entry = new HistoryEntry(command, state, board.cloneBoard());
        history.push(entry);

        Color mover = state.currentColor();
        Color opponent = mover.opposite();
        state = state.afterMove();
        entry.stateAfter = state;

        for (IMoveListener listener : moveListeners) command.announce(listener);

        boolean check = isInCheck(opponent, board);
        boolean hasMoves = hasAnyLegalMove(opponent);

        MoveOutcome outcome;
        String drawReason = null;
        if (check && !hasMoves) {
            state = new CheckmateState(opponent);
            outcome = MoveOutcome.CHECKMATE;
        } else if (!check && !hasMoves) {
            state = new StalemateState(opponent);
            outcome = MoveOutcome.STALEMATE;
        } else {
            drawReason = findDrawReason();
            if (drawReason != null) {
                state = new DrawState(opponent, drawReason);
                outcome = MoveOutcome.DRAW;
            } else {
                outcome = check ? MoveOutcome.CHECK : MoveOutcome.OK;
            }
        }
        entry.stateAfter = state;
        entry.outcome = outcome;
        entry.drawReason = drawReason;
        announceStatus(entry, mover);
        return outcome;
    }

    // Avisa cómo quedó la partida después de una jugada. Lo usa también redo(): rehacer una
    // jugada que dio jaque, mate o tablas lo vuelve a anunciar, igual que la primera vez.
    private void announceStatus(HistoryEntry entry, Color mover) {
        for (IGameStatusListener listener : statusListeners) {
            switch (entry.outcome) {
                case CHECKMATE: listener.onCheckmate(mover); break;
                case STALEMATE: listener.onStalemate(); break;
                case DRAW:      listener.onDraw(entry.drawReason); break;
                case CHECK:     listener.onCheck(mover.opposite()); break;
                default:        break;
            }
        }
    }

    @Override
    public boolean undo() {
        if (history.isEmpty()) return false;
        HistoryEntry entry = history.pop();
        entry.command.undo();
        state = entry.stateBefore;
        redoStack.push(entry);
        for (IMoveListener listener : moveListeners) listener.onUndo();
        return true;
    }

    @Override
    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        HistoryEntry entry = redoStack.pop();
        entry.command.execute();
        history.push(entry);
        state = entry.stateAfter;
        for (IMoveListener listener : moveListeners) {
            entry.command.announce(listener);
            listener.onRedo();
        }
        announceStatus(entry, entry.stateBefore.currentColor());
        return true;
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

    // IGameContext: lo que las reglas pueden preguntar.
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

    @Override
    public Move lastMove() {
        return history.isEmpty() ? null : history.peek().command.getMove();
    }

    @Override
    public int halfMoveClock() {
        int count = 0;
        for (HistoryEntry entry : history) { // de la más reciente a la más vieja
            if (entry.command.resetsHalfMoveClock()) break;
            count++;
        }
        return count;
    }

    @Override
    public int occurrencesOfCurrentPosition() {
        Color toMove = state.currentColor();
        int count = (initialColor == toMove && initialBoard.equals(board)) ? 1 : 0;
        for (HistoryEntry entry : history) {
            if (entry.stateAfter.currentColor() == toMove && entry.boardAfter.equals(board)) count++;
        }
        return count;
    }

    private String findDrawReason() {
        for (IDrawRule rule : drawRules) {
            String reason = rule.reason(this);
            if (reason != null) return reason;
        }
        return null;
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
}
