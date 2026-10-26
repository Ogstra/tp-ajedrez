package adapters;

import ports.ISkinStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Adaptador de salida (Clase 2): implementa ISkinStore con un archivo de texto, una línea por
// usuario: idUsuario;idSkin. Si el archivo todavía no existe, nadie eligió skin.
public class FileSkinStore implements ISkinStore {

    private final Path file;

    public FileSkinStore(Path file) {
        this.file = file;
    }

    @Override
    public String load(int userId) {
        for (String[] parts : lines()) {
            if (parts[0].equals(String.valueOf(userId))) return parts[1];
        }
        return null;
    }

    @Override
    public void save(int userId, String skinId) {
        List<String> updated = new ArrayList<>();
        for (String[] parts : lines()) {
            if (!parts[0].equals(String.valueOf(userId))) updated.add(parts[0] + ";" + parts[1]);
        }
        updated.add(userId + ";" + skinId);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, updated, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + file, e);
        }
    }

    // Las líneas bien formadas, ya separadas en [idUsuario, idSkin].
    private List<String[]> lines() {
        List<String[]> result = new ArrayList<>();
        if (!Files.exists(file)) return result;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.trim().split(";");
                if (parts.length == 2) result.add(parts);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + file, e);
        }
        return result;
    }
}
