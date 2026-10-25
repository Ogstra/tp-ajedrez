package adaptadores;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

// Achica imágenes sin que se pixelen. Con la pantalla escalada (por ejemplo al 125%) Java agranda
// lo que se dibuja píxel a píxel; acá la imagen se achica directo al tamaño real en pantalla,
// de a mitades y con interpolación, y se dibuja casi 1 a 1.
final class SmoothImage {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private SmoothImage() { }

    // Dibuja la imagen en un cuadrado de size x size (en unidades de Swing).
    static void draw(Graphics2D g2, Image image, int x, int y, int size) {
        int real = (int) Math.round(size * g2.getTransform().getScaleX());
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(scaled(image, real), x, y, size, size, null);
        g.dispose();
    }

    // La imagen achicada a size x size píxeles, de a mitades para que no se pierdan los bordes finos.
    static BufferedImage scaled(Image image, int size) {
        String key = System.identityHashCode(image) + "@" + size;
        BufferedImage cached = CACHE.get(key);
        if (cached != null) return cached;
        BufferedImage current = copy(image, image.getWidth(null), image.getHeight(null));
        int w = current.getWidth();
        int h = current.getHeight();
        while (w > size || h > size) {
            w = Math.max(size, w / 2);
            h = Math.max(size, h / 2);
            current = copy(current, w, h);
        }
        if (w != size || h != size) current = copy(current, size, size);
        CACHE.put(key, current);
        return current;
    }

    private static BufferedImage copy(Image image, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    // Ícono de Swing que dibuja la imagen con draw().
    static Icon icon(Image image, int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                draw((Graphics2D) g, image, x, y, size);
            }

            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}
