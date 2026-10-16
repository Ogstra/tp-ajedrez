# Lista para dibujar el UML

Generada con `python3 scripts/list_types.py` a partir del código. Si el código cambia, se vuelve a generar y el UML se revisa contra esta lista.

Cómo leerla: **Implementa** es una flecha de realización (línea punteada con triángulo vacío); **Extiende** es herencia; **Tiene** son las asociaciones por campo (el diamante o la flecha simple, según si es parte de o solo referencia); **Usa** son dependencias (flecha punteada).

## Paquete `(raíz, sin paquete)`

### Main (class)
- Usa: AutoPromotionChooser, Board, BoardSetup, CastlingMove, ChessGame, Color, ConsolePromotionChooser, ConsoleRenderer, EnPassantMove, FiftyMoveRule, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, IPromotionChooser, ISpecialMove, InsufficientMaterialRule, MoveOutcome, OpponentController, OpponentLevel, Opponents, PieceFactory, Position, PromotionMove, ThreefoldRepetitionRule

### MainSwing (class)
- Usa: Board, BoardSetup, CastlingMove, ChessGame, EnPassantMove, FiftyMoveRule, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, ISpecialMove, InsufficientMaterialRule, PieceFactory, PromotionMove, SwingChessView, ThreefoldRepetitionRule

## Paquete `adaptadores`

### ConsolePromotionChooser (class)
- Implementa: IPromotionChooser
- Usa: Color, PieceType

### ConsoleRenderer (class)
- Implementa: IMoveListener, IGameStatusListener
- Tiene: PieceType
- Usa: Board, Color, Piece, Position

### SwingChessView (class)
- Extiende: JFrame
- Implementa: IMoveListener, IGameStatusListener, IPromotionChooser
- Tiene: IChessGame, OpponentController, PieceType, Position
- Usa: Board, Color, Move, OpponentLevel, Opponents, Piece

## Paquete `ai`

### AutoPromotionChooser (class)
- Implementa: IPromotionChooser
- Tiene: Color, IPromotionChooser
- Usa: PieceType

### FixedPromotionChooser (class)
- Implementa: IPromotionChooser
- Tiene: PieceType
- Usa: Color

### GreedyOpponent (class)
- Implementa: IOpponent
- Usa: IChessGame, Move, Piece, PieceType, PieceValues, Position

### IOpponent (interface)
- Usa: IChessGame, Move

### MinimaxOpponent (class)
- Implementa: IOpponent
- Usa: Board, Color, IChessGame, Move, MoveOutcome, Piece, PieceValues, Position, SearchGames

### OpponentController (class)
- Tiene: Color, IChessGame, IOpponent
- Usa: Move

### OpponentLevel (enum)

### Opponents (class)
- Usa: GreedyOpponent, IOpponent, MinimaxOpponent, OpponentLevel, RandomOpponent

### PieceValues (class)
- Usa: PieceType

### RandomOpponent (class)
- Implementa: IOpponent
- Usa: IChessGame, Move

### SearchGames (class)
- Usa: CastlingMove, ChessGame, EnPassantMove, FixedPromotionChooser, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, ISpecialMove, PieceFactory, PieceType, PromotionMove

## Paquete `nucleo`

### BlackTurnState (class)
- Implementa: IGameState
- Usa: Color, WhiteTurnState

### Board (class)
- Tiene: Piece, Position
- Usa: Color, PieceType

### BoardSetup (class)
- Tiene: PieceType
- Usa: Board, Color, PieceFactory, Position

### CastlingCommand (class)
- Implementa: IMoveCommand
- Tiene: Board, Color, Position
- Usa: IMoveListener, Move

### CheckmateState (class)
- Implementa: IGameState
- Tiene: Color

### ChessGame (class)
- Implementa: IChessGame, IGameContext
- Tiene: Board, Color, HistoryEntry, IDrawRule, IGameState, IGameStatusListener, IMoveListener, ISpecialMove
- Usa: BlackTurnState, CheckmateState, DrawState, IMoveCommand, Move, MoveCommand, MoveOutcome, Piece, Position, StalemateState, WhiteTurnState

### Color (enum)

### DrawState (class)
- Implementa: IGameState
- Tiene: Color

### EnPassantCommand (class)
- Implementa: IMoveCommand
- Tiene: Board, Piece, Position
- Usa: IMoveListener, Move

### HistoryEntry (class)
- Tiene: Board, IGameState, IMoveCommand

### IChessGame (interface)
- Usa: Board, Color, Move, MoveOutcome, Position

### ICommand (interface)

### IDrawRule (interface)
- Usa: IGameContext

### IGameContext (interface)
- Usa: Board, Color, Move, Position

### IGameState (interface)
- Usa: Color

### IMoveCommand (interface)
- Extiende: ICommand
- Usa: IMoveListener, Move, Position

### IMovementStrategy (interface)
- Usa: Board, Color, Position

### ISpecialMove (interface)
- Usa: IGameContext, IMoveCommand, Position

### Move (class)
- Tiene: Position

### MoveCommand (class)
- Implementa: IMoveCommand
- Tiene: Board, Piece, Position
- Usa: IMoveListener, Move, PieceType

### MoveOutcome (enum)

### Piece (class)
- Tiene: Color, IMovementStrategy, PieceType
- Usa: Board, Position

### PieceFactory (class)
- Tiene: IMovementStrategy, PieceType
- Usa: BishopMovementStrategy, Color, KingMovementStrategy, KnightMovementStrategy, PawnMovementStrategy, Piece, QueenMovementStrategy, RookMovementStrategy

### PieceType (class)

### Position (class)

### PromotionCommand (class)
- Implementa: IMoveCommand
- Tiene: Board, Piece, Position
- Usa: IMoveListener, Move

### StalemateState (class)
- Implementa: IGameState
- Tiene: Color

### WhiteTurnState (class)
- Implementa: IGameState
- Usa: BlackTurnState, Color

## Paquete `nucleo.especiales`

### CastlingMove (class)
- Implementa: ISpecialMove
- Usa: Board, CastlingCommand, Color, IGameContext, IMoveCommand, Piece, PieceType, Position

### EnPassantMove (class)
- Implementa: ISpecialMove
- Usa: Board, Color, EnPassantCommand, IGameContext, IMoveCommand, Move, Piece, PieceType, Position

### PromotionMove (class)
- Implementa: ISpecialMove
- Tiene: IPromotionChooser, PieceFactory
- Usa: Board, Color, IGameContext, IMoveCommand, Piece, PieceType, Position, PromotionCommand

## Paquete `nucleo.estrategias`

### BishopMovementStrategy (class)
- Implementa: IMovementStrategy
- Usa: Board, Color, Position, SlidingMovement

### KingMovementStrategy (class)
- Implementa: IMovementStrategy
- Usa: Board, Color, Position

### KnightMovementStrategy (class)
- Implementa: IMovementStrategy
- Usa: Board, Color, Position

### PawnMovementStrategy (class)
- Implementa: IMovementStrategy
- Usa: Board, Color, Position

### QueenMovementStrategy (class)
- Implementa: IMovementStrategy
- Tiene: BishopMovementStrategy, RookMovementStrategy
- Usa: Board, Color, Position

### RookMovementStrategy (class)
- Implementa: IMovementStrategy
- Usa: Board, Color, Position, SlidingMovement

### SlidingMovement (class)
- Usa: Board, Color, Position

## Paquete `nucleo.tablas`

### FiftyMoveRule (class)
- Implementa: IDrawRule
- Usa: IGameContext

### InsufficientMaterialRule (class)
- Implementa: IDrawRule
- Usa: Board, Color, IGameContext, Piece, PieceType, Position

### ThreefoldRepetitionRule (class)
- Implementa: IDrawRule
- Usa: IGameContext

## Paquete `puertos`

### IGameStatusListener (interface)
- Usa: Color

### IMoveListener (interface)
- Usa: Color, Piece, PieceType, Position

### IPromotionChooser (interface)
- Usa: Color, PieceType

## Dependencias entre paquetes

| Paquete | Importa de |
|---|---|
| `(raíz, sin paquete)` | `adaptadores`, `ai`, `nucleo`, `nucleo.especiales`, `nucleo.tablas`, `puertos` |
| `adaptadores` | `ai`, `nucleo`, `puertos` |
| `ai` | `nucleo`, `nucleo.especiales`, `puertos` |
| `nucleo` | `nucleo.estrategias`, `puertos` |
| `nucleo.especiales` | `nucleo`, `puertos` |
| `nucleo.estrategias` | `nucleo` |
| `nucleo.tablas` | `nucleo` |
| `puertos` | `nucleo` |

## Dónde está cada patrón (para marcarlo en el diagrama)

- Strategy: `IMovementStrategy` y sus seis implementaciones; `ISpecialMove` (`CastlingMove`, `EnPassantMove`, `PromotionMove`); `IDrawRule` (`InsufficientMaterialRule`, `FiftyMoveRule`, `ThreefoldRepetitionRule`); `IOpponent` (`RandomOpponent`, `GreedyOpponent`, `MinimaxOpponent`).
- Factory: `PieceFactory`, `BoardSetup`, `Opponents`.
- Command: `ICommand`, `IMoveCommand`, `MoveCommand`, `CastlingCommand`, `PromotionCommand`, `EnPassantCommand`.
- State: `IGameState`, `WhiteTurnState`, `BlackTurnState`, `CheckmateState`, `StalemateState`, `DrawState`.
- Observer: `IMoveListener` e `IGameStatusListener`, implementadas por `ConsoleRenderer` y `SwingChessView`.
- Puertos: de entrada `IChessGame`; de salida `IMoveListener`, `IGameStatusListener`, `IPromotionChooser`.
- Composition roots: `Main` y `MainSwing`.
