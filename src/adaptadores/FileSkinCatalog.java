package adaptadores;

import skins.Skin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Lee el catálogo de skins de un archivo de texto (src/assets/skins/skins.csv), una línea por skin:
// id;nombre;puntos;casilla clara;casilla oscura;carpeta de las piezas. Las líneas que empiezan con
// "#" son comentarios y las mal formadas se saltean. Agregar una skin es sumar una línea y una
// carpeta con las 12 imágenes, sin tocar código.
public final class FileSkinCatalog {

    private FileSkinCatalog() { }

    public static List<Skin> load(Path file) {
        List<Skin> skins = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Skin skin = parse(line);
                if (skin != null) skins.add(skin);
            }
        } catch (IOException e) {
            // sin catálogo queda solo la de siempre
        }
        if (skins.isEmpty()) skins.add(new Skin("clasico", "Clásico", 0, "EEEED2", "769656", "piezas"));
        return skins;
    }

    private static Skin parse(String line) {
        String text = line.trim();
        if (text.isEmpty() || text.startsWith("#")) return null;
        String[] parts = text.split(";");
        if (parts.length != 6) return null;
        try {
            int points = Integer.parseInt(parts[2].trim());
            Integer.parseInt(parts[3].trim(), 16); // los colores tienen que ser hexadecimales válidos
            Integer.parseInt(parts[4].trim(), 16);
            return new Skin(parts[0].trim(), parts[1].trim(), points, parts[3].trim(), parts[4].trim(), parts[5].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
