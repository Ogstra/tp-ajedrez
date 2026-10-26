package adapters;

import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;

// Lo que una partida necesita de la pantalla que la muestra: enterarse de las jugadas y del
// estado, y preguntar a qué pieza se corona. GameBuilder depende de esto y no de SwingChessView
// (DIP): el contrato del armado de partidas no nombra a Swing, así cambiar la ventana por otra
// tecnología no toca GameBuilder ni MainSwing.StandardGames. Hoy la implementa solo SwingChessView.
public interface IGameView extends IMoveListener, IGameStatusListener, IPromotionChooser {
}
