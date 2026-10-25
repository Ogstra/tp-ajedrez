package adaptadores;

import skins.Skin;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Las imágenes de las piezas de cada skin, leídas una sola vez desde su carpeta en src/assets.
// Si a una skin le falta alguna imagen, se usa la del set de siempre (src/assets/piezas); si
// también falta, la ventana muestra un símbolo de texto.
final class SkinImages {

    static final String DEFAULT_FOLDER = "piezas";
    private static final Map<String, Image> CACHE = new HashMap<>();

    private SkinImages() { }

    // key es el color y la pieza: "wK" rey blanco, "bQ" reina negra, etc.
    static Image piece(Skin skin, String key) {
        Image image = load(skin.getPiecesFolder(), key);
        return image != null ? image : load(DEFAULT_FOLDER, key);
    }

    static Image load(String folder, String key) {
        String path = "src" + File.separator + "assets" + File.separator + folder.replace("/", File.separator)
            + File.separator + key + ".png";
        if (CACHE.containsKey(path)) return CACHE.get(path);
        Image image = null;
        try {
            image = ImageIO.read(new File(path));
        } catch (IOException e) {
            // falta el archivo: queda null
        }
        CACHE.put(path, image);
        return image;
    }

    // "EEEED2" -> el color de Swing.
    static java.awt.Color color(String hex) {
        return new java.awt.Color(Integer.parseInt(hex, 16));
    }
}
