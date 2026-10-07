package nucleo;

import nucleo.estrategias.BishopMovementStrategy;
import nucleo.estrategias.KingMovementStrategy;
import nucleo.estrategias.KnightMovementStrategy;
import nucleo.estrategias.PawnMovementStrategy;
import nucleo.estrategias.QueenMovementStrategy;
import nucleo.estrategias.RookMovementStrategy;

import java.util.HashMap;
import java.util.Map;

// Factory (Clase 3) con registro. Antes era un switch cerrado sobre un enum:
// una pieza nueva obligaba a editar el switch. Ahora la fábrica guarda un
// mapa "tipo de pieza -> estrategia de movimiento" y se le agregan tipos
// nuevos con register(), sin modificar esta clase ni ninguna otra.
// Las estrategias no tienen estado, por eso se comparte una instancia por tipo.
public class PieceFactory {

    private final Map<PieceType, IMovementStrategy> strategies = new HashMap<>();

    public void register(PieceType type, IMovementStrategy strategy) {
        strategies.put(type, strategy);
    }

    public Piece create(PieceType type, Color color) {
        IMovementStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Tipo de pieza no registrado: " + type);
        }
        return new Piece(type, color, strategy);
    }

    // Las seis piezas del ajedrez clásico ya registradas.
    public static PieceFactory standard() {
        PieceFactory factory = new PieceFactory();
        factory.register(PieceType.PAWN, new PawnMovementStrategy());
        factory.register(PieceType.ROOK, new RookMovementStrategy());
        factory.register(PieceType.KNIGHT, new KnightMovementStrategy());
        factory.register(PieceType.BISHOP, new BishopMovementStrategy());
        factory.register(PieceType.QUEEN, new QueenMovementStrategy());
        factory.register(PieceType.KING, new KingMovementStrategy());
        return factory;
    }
}
