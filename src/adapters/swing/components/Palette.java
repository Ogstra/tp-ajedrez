package adapters.swing.components;

import java.awt.Color;

// Colores de la ventana, en un solo lugar. Los contrastes de texto
// están medidos (WCAG): todos superan 4,5:1 sobre el fondo en que se usan.
public final class Palette {
    public static final Color BACKGROUND = new Color(0xFA, 0xF8, 0xF1);    // crema de la ventana
    public static final Color TRACK = new Color(0xE7, 0xE2, 0xD0);         // fondo de los botones
    public static final Color TRACK_HOVER = new Color(0xD8, 0xD2, 0xBC);
    public static final Color INK = new Color(0x1F, 0x2A, 0x1A);           // texto principal (14:1)
    public static final Color MUTED = new Color(0x4A, 0x56, 0x40);         // texto secundario (7,3:1)
    public static final Color CONTROL = new Color(0x7A, 0x76, 0x60);       // bordes de los controles
    public static final Color PRIMARY = new Color(0x27, 0x50, 0x1F);       // borde del botón con el foco
    public static final Color BOARD_LIGHT = new Color(0xEE, 0xEE, 0xD2);   // los del tablero
    public static final Color BOARD_DARK = new Color(0x76, 0x96, 0x56);

    private Palette() { }
}
