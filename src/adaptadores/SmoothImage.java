package adaptadores;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;

// Imágenes nítidas con la pantalla escalada (125%, 150%, Retina). Se arma una imagen con varias
// resoluciones del mismo dibujo y Java dibuja la que corresponde a la escala de la pantalla, en
// vez de agrandar píxel a píxel la de 100%, que se ve borrosa.
final class SmoothImage {

    private static final double[] SCALES = {1, 1.25, 1.5, 2};

    private SmoothImage() { }

    // La imagen para mostrar en un cuadrado de size x size (en unidades de Swing).
    static Image of(Image image, int size) {
        Image[] variants = new Image[SCALES.length];
        for (int i = 0; i < SCALES.length; i++) variants[i] = resize(image, (int) Math.round(size * SCALES[i]));
        return new BaseMultiResolutionImage(variants);
    }

    // Achica promediando áreas (SCALE_SMOOTH), que conserva los bordes finos aunque se achique mucho.
    private static BufferedImage resize(Image image, int pixels) {
        BufferedImage result = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image.getScaledInstance(pixels, pixels, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return result;
    }
}
