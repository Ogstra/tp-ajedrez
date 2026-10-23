package adaptadores;

import java.awt.Color;
import java.awt.Font;

// Colores y fuente del título de la ventana, en un solo lugar. Los contrastes de texto
// están medidos (WCAG): todos superan 4,5:1 sobre el fondo en que se usan.
final class Palette {
    static final Color BACKGROUND = new Color(0xFA, 0xF8, 0xF1);    // crema de la ventana
    static final Color CARD = Color.WHITE;                          // recuadros y tarjetas
    static final Color TRACK = new Color(0xE7, 0xE2, 0xD0);         // fondo de los selectores
    static final Color TRACK_HOVER = new Color(0xD8, 0xD2, 0xBC);
    static final Color INK = new Color(0x1F, 0x2A, 0x1A);           // texto principal (14:1)
    static final Color MUTED = new Color(0x4A, 0x56, 0x40);         // texto secundario (7,3:1)
    static final Color CONTROL = new Color(0x7A, 0x76, 0x60);       // bordes de los controles
    static final Color LINE = new Color(0xD9, 0xD4, 0xBF);          // separadores
    static final Color PRIMARY = new Color(0x27, 0x50, 0x1F);       // Jugar y opción elegida (blanco encima: 9:1)
    static final Color PRIMARY_HOVER = new Color(0x1D, 0x3E, 0x17);
    static final Color TINT = new Color(0xE3, 0xEE, 0xDC);          // tarjeta elegida
    static final Color ERROR = new Color(0xA1, 0x20, 0x27);
    static final Color BADGE = new Color(0x23, 0x3B, 0x1D);         // etiqueta "IA"
    static final Color BADGE_TEXT = new Color(0xF3, 0xF1, 0xDF);
    static final Color BOARD_LIGHT = new Color(0xEE, 0xEE, 0xD2);   // los del tablero
    static final Color BOARD_DARK = new Color(0x76, 0x96, 0x56);

    private Palette() { }

    // Georgia viene en Mac y Windows; si falta, Java usa una serif parecida.
    static Font title(float size) {
        return new Font("Georgia", Font.BOLD, Math.round(size));
    }
}
