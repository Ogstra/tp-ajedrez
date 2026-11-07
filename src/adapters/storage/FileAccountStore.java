package adapters.storage;

import accounts.IAccountStore;
import accounts.Score;
import accounts.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// Adaptador de salida: guarda los usuarios en un archivo de texto, una línea por usuario con
// id;nombre;victorias;empates;derrotas. Una línea mal formada se saltea.
public class FileAccountStore implements IAccountStore {

    private final Path file;

    public FileAccountStore(Path file) {
        this.file = file;
    }

    @Override
    public List<Score> loadAll() {
        List<Score> scores = new ArrayList<>();
        if (!Files.exists(file)) return scores;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Score score = parse(line);
                if (score != null) scores.add(score);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + file, e);
        }
        return scores;
    }

    @Override
    public void saveAll(List<Score> scores) {
        List<String> lines = new ArrayList<>();
        for (Score score : scores) {
            lines.add(score.getUser().getId() + ";" + score.getUser().getName() + ";"
                + score.getWins() + ";" + score.getDraws() + ";" + score.getLosses());
        }
        Path temp = null;
        try {
            Path folder = file.toAbsolutePath().getParent();
            Files.createDirectories(folder);
            // Primero se escribe un archivo temporal y después reemplaza al verdadero, así un
            // error a mitad de camino no borra lo que había.
            temp = Files.createTempFile(folder, "usuarios-", ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + file, e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // el archivo final no se toca
                }
            }
        }
    }

    // null si la línea no tiene el formato esperado.
    private Score parse(String line) {
        String[] parts = line.split(";", -1);
        if (parts.length != 5) return null;
        try {
            int id = Integer.parseInt(parts[0].trim());
            int wins = Integer.parseInt(parts[2].trim());
            int draws = Integer.parseInt(parts[3].trim());
            int losses = Integer.parseInt(parts[4].trim());
            if (parts[1].trim().isEmpty() || id < 1 || wins < 0 || draws < 0 || losses < 0) return null;
            return new Score(new User(id, parts[1].trim()), wins, draws, losses);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
