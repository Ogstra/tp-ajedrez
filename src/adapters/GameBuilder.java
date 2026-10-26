package adapters;

import accounts.User;
import ai.OpponentLevel;
import core.Color;
import network.LanConnection;

// Arma una partida nueva con todo lo que necesita: el juego con sus reglas, quién acredita los
// puntos y, si corresponde, la IA o el rival de la otra PC. La ventana lo usa cada vez que empieza
// una partida y lo implementa MainSwing (el composition root), así la ventana no crea
// colaboradores con new (inyección de dependencias, nivel 4).
public interface GameBuilder {

    // En esta PC: 1 contra 1 (level NONE) o contra la IA, que juega con aiColor.
    Match local(IGameView view, User whiteUser, User blackUser, OpponentLevel level, Color aiColor);

    // Por LAN, ya conectados: localUser juega con localColor en esta PC y el rival en la suya.
    Match overLan(IGameView view, User localUser, Color localColor, LanConnection connection, String rivalName);
}
