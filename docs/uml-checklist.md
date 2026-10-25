# Lista para dibujar el UML

Generada con `python3 scripts/list_types.py` a partir del código. Si el código cambia, se vuelve a generar y el UML se revisa contra esta lista.

Cómo leerla: **Implementa** es una flecha de realización (línea punteada con triángulo vacío); **Extiende** es herencia; **Tiene** son las asociaciones por campo (el diamante o la flecha simple, según si es parte de o solo referencia); **Usa** son dependencias (flecha punteada).

## Paquete `(raíz, sin paquete)`

### AccountTests (class)
- Usa: AccountService, Board, BoardSetup, CastlingMove, ChessGame, Color, EnPassantMove, FiftyMoveRule, FileAccountStore, IAccountStore, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, IPromotionChooser, ISpecialMove, InsufficientMaterialRule, Piece, PieceFactory, PieceType, Position, PromotionMove, Result, Score, ScoreRecorder, ThreefoldRepetitionRule, User

### LanTests (class)
- Usa: Board, BoardSetup, CastlingMove, ChessGame, Color, EnPassantMove, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, ISpecialMove, LanAnnouncer, LanBrowser, LanConnection, LanGame, LanOpponent, Move, Piece, PieceFactory, PieceType, Position, PromotionMove

### Main (class)
- Usa: AccountService, AutoPromotionChooser, Board, BoardSetup, CastlingMove, ChessGame, Color, ConsoleAccounts, ConsolePromotionChooser, ConsoleRenderer, EnPassantMove, FiftyMoveRule, FileAccountStore, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, IPromotionChooser, ISpecialMove, InsufficientMaterialRule, MoveOutcome, OpponentController, OpponentLevel, Opponents, PieceFactory, Position, PromotionMove, ScoreRecorder, ThreefoldRepetitionRule, User

### MainSwing (class)
- Usa: AccountService, BoardSetup, CastlingMove, ChessGame, EnPassantMove, FiftyMoveRule, FileAccountStore, FileSkinCatalog, FileSkinStore, GameBuilder, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, IPromotionChooser, ISpecialMove, InsufficientMaterialRule, LanOpponent, Match, OpponentController, OpponentLevel, Opponents, PieceFactory, PromotionMove, ScoreRecorder, SkinService, SwingChessView, ThreefoldRepetitionRule

### SkinTests (class)
- Tiene: Skin
- Usa: AccountService, FileSkinCatalog, FileSkinStore, IAccountStore, ISkinStore, Result, Score, SkinService, User

## Paquete `adaptadores`

### BoardStrip (class)
- Extiende: JComponent
- Usa: Palette

### ChoiceControl (class)
- Extiende: JComponent
- Usa: Color, Palette, RoundedBorder, SmoothImage

### ConsoleAccounts (class)
- Usa: AccountService, Score, User

### ConsolePromotionChooser (class)
- Implementa: IPromotionChooser
- Usa: Color, PieceType

### ConsoleRenderer (class)
- Implementa: IMoveListener, IGameStatusListener
- Tiene: PieceType
- Usa: Board, Color, Piece, Position

### FileAccountStore (class)
- Implementa: IAccountStore
- Usa: Score, User

### FileSkinCatalog (class)
- Usa: Skin

### FileSkinStore (class)
- Implementa: ISkinStore

### FlatButton (class)
- Extiende: JButton
- Usa: Color, FlatComboBox, Palette, RoundedBorder

### FlatComboBox (class)
- Extiende: JComboBox
- Usa: Palette, RoundedBorder

### GameBuilder (interface)
- Usa: Color, LanOpponent, Match, OpponentLevel, SwingChessView, User

### LanPanel (class)
- Extiende: JPanel
- Tiene: FlatButton, LanAnnouncer, LanBrowser
- Usa: BoardStrip, Color, LanConnection, LanGame, MenuPanel, Palette

### LoginPanel (class)
- Extiende: JPanel
- Tiene: AccountService, FlatButton, User
- Usa: BoardStrip, FlatComboBox, MenuPanel, Palette, RoundedBorder

### Match (class)
- Tiene: IChessGame, OpponentController

### MenuPanel (class)
- Extiende: JPanel
- Tiene: ChoiceControl, FlatButton
- Usa: BoardStrip, Color, OpponentLevel, Palette, SwingChessView

### Palette (class)
- Tiene: Color

### RankingPanel (class)
- Extiende: JPanel
- Tiene: RoundedPanel
- Usa: BoardStrip, FlatButton, Palette, Score

### RoundedBorder (class)
- Extiende: AbstractBorder
- Usa: Palette

### RoundedPanel (class)
- Extiende: JPanel
- Usa: Palette, RoundedBorder

### SkinImages (class)
- Usa: Color, Skin

### SkinsPanel (class)
- Extiende: JPanel
- Tiene: SkinService, User
- Usa: BoardStrip, Color, FlatButton, MenuPanel, Palette, RoundedPanel, Skin, SkinImages, SmoothImage

### SmoothImage (class)

### SwingChessView (class)
- Extiende: JFrame
- Implementa: IMoveListener, IGameStatusListener, IPromotionChooser
- Tiene: AccountService, Board, GameBuilder, IChessGame, LanOpponent, LanPanel, LoginPanel, MenuPanel, OpponentController, PieceType, Position, RankingPanel, Skin, SkinService, SkinsPanel, User
- Usa: Color, FlatButton, LanConnection, Match, Move, OpponentLevel, Palette, Piece, SkinImages, SmoothImage

## Paquete `ai`

### AutoPromotionChooser (class)
- Implementa: IPromotionChooser
- Tiene: Color, IPromotionChooser
- Usa: PieceType

### Evaluation (class)
- Usa: Board, Color, Piece, PieceType, PieceValues, Position

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
- Usa: Color, Evaluation, IChessGame, Move, MoveOutcome, SearchGames

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

### IAccountStore (interface)
- Usa: Score

### IGameStatusListener (interface)
- Usa: Color

### IMoveListener (interface)
- Usa: Color, Piece, PieceType, Position

### IPromotionChooser (interface)
- Usa: Color, PieceType

### ISkinStore (interface)

## Paquete `red`

### LanAnnouncer (class)

### LanBrowser (class)
- Tiene: LanGame
- Usa: LanAnnouncer

### LanConnection (class)

### LanGame (class)

### LanOpponent (class)
- Implementa: IOpponent, IMoveListener, IPromotionChooser
- Tiene: Color, IChessGame, IPromotionChooser, LanConnection, PieceType
- Usa: Move, Piece, Position

## Paquete `skins`

### Skin (class)

### SkinService (class)
- Tiene: AccountService, ISkinStore, Skin
- Usa: User

## Paquete `usuarios`

### AccountService (class)
- Tiene: IAccountStore
- Usa: Result, Score, User

### Result (enum)

### Score (class)
- Tiene: User
- Usa: Result

### ScoreRecorder (class)
- Implementa: IGameStatusListener, IMoveListener
- Tiene: AccountService, IChessGame, User
- Usa: Color, Piece, PieceType, Position, Result

### User (class)

## Dependencias entre paquetes

| Paquete | Importa de |
|---|---|
| `(raíz, sin paquete)` | `adaptadores`, `ai`, `nucleo`, `nucleo.especiales`, `nucleo.tablas`, `puertos`, `red`, `skins`, `usuarios` |
| `adaptadores` | `ai`, `nucleo`, `puertos`, `red`, `skins`, `usuarios` |
| `ai` | `nucleo`, `nucleo.especiales`, `puertos` |
| `nucleo` | `nucleo.estrategias`, `puertos` |
| `nucleo.especiales` | `nucleo`, `puertos` |
| `nucleo.estrategias` | `nucleo` |
| `nucleo.tablas` | `nucleo` |
| `puertos` | `nucleo`, `usuarios` |
| `red` | `ai`, `nucleo`, `puertos` |
| `skins` | `puertos`, `usuarios` |
| `usuarios` | `nucleo`, `puertos` |

## Dónde está cada patrón (para marcarlo en el diagrama)

- Strategy: `IMovementStrategy` y sus seis implementaciones; `ISpecialMove` (`CastlingMove`, `EnPassantMove`, `PromotionMove`); `IDrawRule` (`InsufficientMaterialRule`, `FiftyMoveRule`, `ThreefoldRepetitionRule`); `IOpponent` (`RandomOpponent`, `GreedyOpponent`, `MinimaxOpponent`).
- Factory: `PieceFactory`, `BoardSetup`, `Opponents` y `GameBuilder` (lo implementa `MainSwing` y devuelve un `Match`).
- Command: `ICommand`, `IMoveCommand`, `MoveCommand`, `CastlingCommand`, `PromotionCommand`, `EnPassantCommand`.
- State: `IGameState`, `WhiteTurnState`, `BlackTurnState`, `CheckmateState`, `StalemateState`, `DrawState`.
- Observer: `IMoveListener` e `IGameStatusListener`, implementadas por `ConsoleRenderer`, `SwingChessView` y `ScoreRecorder`.
- Puertos: de entrada `IChessGame`; de salida `IMoveListener`, `IGameStatusListener`, `IPromotionChooser` e `IAccountStore` (lo implementa `FileAccountStore`).
- Composition roots: `Main` y `MainSwing`.
