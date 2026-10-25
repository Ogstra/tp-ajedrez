package adaptadores;

import skins.Skin;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Las imágenes de las piezas de cada skin, leídas una sola vez desde su carpeta en src/assets.
// Si a una skin le falta alguna imagen, se usa la del set de siempre (src/assets/piezas); si
// también falta, la ventana muestra un símbolo de texto.
//
// Muchos sets traen las piezas con mucho margen transparente alrededor. Para que llenen la casilla,
// se recortan las 12 piezas de una carpeta con el MISMO recuadro: el más chico que contiene a todas.
// Así se va el margen sobrante pero no cambian los tamaños relativos (el peón sigue más chico que
// el rey) ni la alineación de las bases.
final class SkinImages {

    static final String DEFAULT_FOLDER = "piezas";
    private static final String[] KEYS = {"wK", "wQ", "wR", "wB", "wN", "wP", "bK", "bQ", "bR", "bB", "bN", "bP"};
    private static final Map<String, Map<String, Image>> FOLDERS = new HashMap<>();

    private SkinImages() { }

    // key es el color y la pieza: "wK" rey blanco, "bQ" reina negra, etc.
    static Image piece(Skin skin, String key) {
        Image image = load(skin.getPiecesFolder(), key);
        return image != null ? image : load(DEFAULT_FOLDER, key);
    }

    static Image load(String folder, String key) {
        return FOLDERS.computeIfAbsent(folder, SkinImages::loadFolder).get(key);
    }

    // Lee las 12 piezas de la carpeta y las recorta con el recuadro común.
    private static Map<String, Image> loadFolder(String folder) {
        Map<String, BufferedImage> raw = new HashMap<>();
        for (String key : KEYS) {
            String path = "src" + File.separator + "assets" + File.separator + folder.replace("/", File.separator)
                + File.separator + key + ".png";
            try {
                BufferedImage image = ImageIO.read(new File(path));
                if (image != null) raw.put(key, image);
            } catch (IOException e) {
                // falta el archivo: esa pieza queda sin imagen
            }
        }
        Rectangle box = null;
        for (BufferedImage image : raw.values()) {
            Rectangle visible = visibleArea(image);
            if (visible != null) box = box == null ? visible : box.union(visible);
        }
        Map<String, Image> result = new HashMap<>();
        for (Map.Entry<String, BufferedImage> entry : raw.entrySet()) {
            BufferedImage image = entry.getValue();
            result.put(entry.getKey(), box == null ? image : square(image, box));
        }
        return result;
    }

    // El rectángulo de los píxeles que no son transparentes, o null si la imagen está vacía.
    private static Rectangle visibleArea(BufferedImage image) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 16) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    // Recorta el recuadro común y lo centra en un cuadrado, para que la casilla no deforme la pieza.
    private static BufferedImage square(BufferedImage image, Rectangle box) {
        int side = Math.max(box.width, box.height);
        BufferedImage result = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        int dx = (side - box.width) / 2, dy = (side - box.height) / 2;
        result.createGraphics().drawImage(image.getSubimage(box.x, box.y, box.width, box.height), dx, dy, null);
        return result;
    }

    // "EEEED2" -> el color de Swing.
    static java.awt.Color color(String hex) {
        return new java.awt.Color(Integer.parseInt(hex, 16));
    }
}
