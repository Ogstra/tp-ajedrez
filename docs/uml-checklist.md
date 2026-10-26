# Lista para dibujar el UML

Generada con `java scripts/ListTypes.java` a partir del código. Si el código cambia, se vuelve a generar y el UML se revisa contra esta lista.

Cómo leerla: **Implementa** es una flecha de realización (línea punteada con triángulo vacío); **Extiende** es herencia; **Tiene** son las asociaciones por campo (el diamante o la flecha simple, según si es parte de o solo referencia); **Usa** son dependencias (flecha punteada).

## Paquete `(raíz, sin paquete)`

### AccountTests (class)
- Usa: AccountService, Board, BoardSetup, CastlingMove, ChessGame, Color, EnPassantMove, FiftyMoveRule, FileAccountStore, IAccountStore, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, IPromotionChooser, ISpecialMove, InsufficientMaterialRule, Piece, PieceFactory, PieceType, Position, PromotionMove, Result, Score, ScoreRecorder, ThreefoldRepetitionRule, User

### LanTests (class)
- Usa: Board, BoardSetup, CastlingMove, ChessGame, Color, EnPassantMove, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, ISpecialMove, LanAnnouncer, LanBrowser, LanConnection, LanGame, LanLobby, LanNames, LanOpponent, Move, Piece, PieceFactory, PieceType, Position, PromotionMove

### Main (class)
- Usa: AccountService, AutoPromotionChooser, Board, BoardSetup, CastlingMove, ChessGame, Color, ConsoleAccounts, ConsolePromotionChooser, ConsoleRenderer, EnPassantMove, FiftyMoveRule, FileAccountStore, IChessGame, IDrawRule, IGameStatusListener, IMoveListener, IPromotionChooser, ISpecialMove, InsufficientMaterialRule, MoveOutcome, OpponentController, OpponentLevel, Opponents, PieceFactory, Position, PromotionMove, ScoreRecorder, ThreefoldRepetitionRule, User

### MainSwing (class)
- Usa: AccountService, BoardSetup, CastlingMove, ChessGame, Color, EnPassantMove, FiftyMoveRule, FileAccountStore, FileSkinStore, GameBuilder, IChessGame, IDrawRule, IGameStatusListener, IGameView, IMoveListener, IPromotionChooser, ISpecialMove, InsufficientMaterialRule, LanConnection, LanLobby, LanOpponent, Match, OpponentController, OpponentLevel, Opponents, PieceFactory, PromotionMove, ScoreRecorder, SkinCatalogReader, SkinService, SwingChessView, ThreefoldRepetitionRule, User

### SkinTests (class)
- Tiene: Skin
- Usa: AccountService, Assets, FileSkinStore, IAccountStore, ISkinStore, Result, Score, SkinCatalogReader, SkinService, User

## Paquete `accounts`

### AccountService (class)
- Tiene: IAccountStore
- Usa: Result, Score, User

### IAccountStore (interface)
- Usa: Score

### Result (enum)

### Score (class)
- Tiene: User
- Usa: Result

### ScoreRecorder (class)
- Implementa: IGameStatusListener, IMoveListener
- Tiene: AccountService, IChessGame, User
- Usa: Color, Piece, PieceType, Position, Result

### User (class)

## Paquete `adapters`

### Assets (class)

### BoardStrip (class)
- Extiende: JComponent
- Usa: Palette

### BoardView (class)
- Extiende: JPanel
- Tiene: Board, PieceType, Position, Skin
- Usa: Color, Move, Palette, Piece, SkinImages, SmoothImage

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

### FileSkinStore (class)
- Implementa: ISkinStore

### FlatButton (class)
- Extiende: JButton
- Usa: Color, FlatComboBox, Palette, RoundedBorder

### FlatComboBox (class)
- Extiende: JComboBox
- Usa: Palette, RoundedBorder

### GameBuilder (interface)
- Usa: Color, IGameView, LanConnection, Match, OpponentLevel, User

### IGameView (interface)
- Extiende: IMoveListener, IGameStatusListener, IPromotionChooser

### LanPanel (class)
- Extiende: JPanel
- Tiene: FlatButton, LanLobby
- Usa: BoardStrip, Color, LanConnection, LanGame, MenuPanel, Palette

### LoginPanel (class)
- Extiende: JPanel
- Tiene: AccountService, FlatButton, User
- Usa: BoardStrip, FlatComboBox, MenuPanel, Palette, RoundedBorder

### Match (class)
- Tiene: IChessGame, LanOpponent, OpponentController

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

### SkinCatalogReader (class)
- Usa: Assets, Skin

### SkinImages (class)
- Usa: Assets, Color, Skin

### SkinsPanel (class)
- Extiende: JPanel
- Tiene: SkinService, User
- Usa: BoardStrip, Color, FlatButton, MenuPanel, Palette, RoundedPanel, Skin, SkinImages, SmoothImage

### SmoothImage (class)

### SwingChessView (class)
- Extiende: JFrame
- Implementa: IGameView
- Tiene: AccountService, Board, BoardView, GameBuilder, IChessGame, LanOpponent, LanPanel, LoginPanel, MenuPanel, OpponentController, PieceType, Position, RankingPanel, SkinService, SkinsPanel, User
- Usa: Assets, Color, FlatButton, LanConnection, LanLobby, Match, Move, OpponentLevel, Palette, Piece, SkinImages

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

## Paquete `core`

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

## Paquete `core.draw`

### FiftyMoveRule (class)
- Implementa: IDrawRule
- Usa: IGameContext

### InsufficientMaterialRule (class)
- Implementa: IDrawRule
- Usa: Board, Color, IGameContext, Piece, PieceType, Position

### ThreefoldRepetitionRule (class)
- Implementa: IDrawRule
- Usa: IGameContext

## Paquete `core.movement`

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

## Paquete `core.special`

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

## Paquete `network`

### LanAnnouncer (class)
- Usa: LanNames

### LanBrowser (class)
- Tiene: LanGame
- Usa: LanAnnouncer, LanNames

### LanConnection (class)
- Usa: LanNames

### LanGame (class)

### LanLobby (class)
- Tiene: LanAnnouncer, LanBrowser
- Usa: Color, LanConnection, LanGame

### LanNames (class)

### LanOpponent (class)
- Implementa: IOpponent, IMoveListener, IPromotionChooser
- Tiene: Color, IChessGame, IPromotionChooser, LanConnection, PieceType
- Usa: Move, Piece, Position

## Paquete `ports`

### IGameStatusListener (interface)
- Usa: Color

### IMoveListener (interface)
- Usa: Color, Piece, PieceType, Position

### IPromotionChooser (interface)
- Usa: Color, PieceType

## Paquete `skins`

### ISkinStore (interface)

### Skin (class)

### SkinService (class)
- Tiene: AccountService, ISkinStore, Skin
- Usa: User

## Dependencias entre paquetes

| Paquete | Importa de |
|---|---|
| `(raíz, sin paquete)` | `accounts`, `adapters`, `ai`, `core`, `core.draw`, `core.special`, `network`, `ports`, `skins` |
| `accounts` | `core`, `ports` |
| `adapters` | `accounts`, `ai`, `core`, `network`, `ports`, `skins` |
| `ai` | `core`, `core.special`, `ports` |
| `core` | `core.movement`, `ports` |
| `core.draw` | `core` |
| `core.movement` | `core` |
| `core.special` | `core`, `ports` |
| `network` | `ai`, `core`, `ports` |
| `ports` | `core` |
| `skins` | `accounts` |

## Dónde está cada patrón (para marcarlo en el diagrama)

- Strategy: `IMovementStrategy` y sus seis implementaciones; `ISpecialMove` (`CastlingMove`, `EnPassantMove`, `PromotionMove`); `IDrawRule` (`InsufficientMaterialRule`, `FiftyMoveRule`, `ThreefoldRepetitionRule`); `IOpponent` (`RandomOpponent`, `GreedyOpponent`, `MinimaxOpponent` y `LanOpponent`, el rival de la otra PC).
- Factory: `PieceFactory`, `BoardSetup`, `Opponents` y `GameBuilder` (lo implementa `MainSwing.StandardGames`; `local` y `overLan` devuelven un `Match`).
- Command: `ICommand`, `IMoveCommand`, `MoveCommand`, `CastlingCommand`, `PromotionCommand`, `EnPassantCommand`.
- State: `IGameState`, `WhiteTurnState`, `BlackTurnState`, `CheckmateState`, `StalemateState`, `DrawState`.
- Observer: `IMoveListener` e `IGameStatusListener`, implementadas por `ConsoleRenderer`, `SwingChessView` (a través de `IGameView`), `ScoreRecorder` y `LanOpponent` (solo `IMoveListener`).
- Puertos del núcleo: de entrada `IChessGame`; de salida `IMoveListener`, `IGameStatusListener` e `IPromotionChooser` (paquete `ports`). Puertos de los otros módulos: `IAccountStore` en `accounts` (lo implementa `FileAccountStore`) e `ISkinStore` en `skins` (lo implementa `FileSkinStore`).
- Separación de responsabilidades en la ventana: `SwingChessView` (pantallas y turno), `BoardView` (tablero), `LanPanel` (pantalla de LAN) y `LanLobby` (sockets e hilos de la LAN).
- El ciclo `core` <-> `ports` de la tabla de dependencias es esperable: los puertos son parte del hexágono del núcleo (hablan en sus términos: `Color`, `Position`, `Piece`). Ningún paquete de afuera es importado por `core` ni por `ports`.
- Composition roots: `Main` y `MainSwing`.
