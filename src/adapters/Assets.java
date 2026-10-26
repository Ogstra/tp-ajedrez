package adapters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Los recursos del juego (ícono, piezas y catálogo de skins) se leen del classpath, no de una
// ruta en disco como "src/assets/...". Así se encuentran igual en Windows, macOS y Linux, desde
// IntelliJ, desde la terminal y desde cualquier carpeta en la que se ejecute (antes, correr el
// juego desde otra carpeta dejaba la ventana sin ícono y sin piezas). build.sh copia src/assets
// a out/assets, e IntelliJ copia los recursos solo.
public final class Assets {

    private static final String ROOT = "/assets/";

    private Assets() { }

    // La imagen en assets/<path>, o null si no existe o no se puede leer.
    public static BufferedImage image(String path) {
        try (InputStream in = Assets.class.getResourceAsStream(ROOT + path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    // Las líneas del archivo de texto en assets/<path>; vacía si no existe.
    public static List<String> lines(String path) {
        List<String> lines = new ArrayList<>();
        InputStream in = Assets.class.getResourceAsStream(ROOT + path);
        if (in == null) return lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) lines.add(line);
        } catch (IOException e) {
            // se devuelve lo que se pudo leer
        }
        return lines;
    }
}
