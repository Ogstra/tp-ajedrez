package adapters;

import ports.IGameStatusListener;
import ports.IMoveListener;
import ports.IPromotionChooser;

// Lo que una partida necesita de la pantalla que la muestra: enterarse de las jugadas y del
// estado, y preguntar a qué pieza se corona. GameBuilder depende de esto y no de SwingChessView
// (DIP): así los tests arman partidas con cualquier vista y el armado no conoce Swing.
public interface IGameView extends IMoveListener, IGameStatusListener, IPromotionChooser {
}
