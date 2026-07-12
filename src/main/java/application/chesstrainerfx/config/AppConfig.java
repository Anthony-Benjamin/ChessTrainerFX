package application.chesstrainerfx.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Applicatieconfiguratie uit {@code config.properties} in de werkmap.
 * Ontbreekt het bestand, dan wordt het met standaardwaarden aangemaakt.
 * De PGN-map per module wordt automatisch aangemaakt als die nog niet bestaat.
 */
public final class AppConfig {

    public static final String KEY_MATING_PATTERNS = "pgn.mating-patterns.path";
    public static final String KEY_TACTICS = "pgn.tactics.path";
    public static final String KEY_PUZZLES = "pgn.puzzles.path";

    private static final Path CONFIG_FILE = Path.of("config.properties");

    private final Path matingPatternsDir;
    private final Path tacticsDir;
    private final Path puzzlesDir;

    private AppConfig(Path matingPatternsDir, Path tacticsDir, Path puzzlesDir) {
        this.matingPatternsDir = matingPatternsDir;
        this.tacticsDir = tacticsDir;
        this.puzzlesDir = puzzlesDir;
    }

    public static AppConfig load() {
        Properties props = new Properties();
        boolean configExists = Files.exists(CONFIG_FILE);
        if (configExists) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException e) {
                // onleesbare configuratie: val terug op standaardwaarden
            }
        }

        Path defaultRoot = Path.of(System.getProperty("user.home"), "ChessTrainerFX", "pgn");
        Path mating = pathFrom(props, KEY_MATING_PATTERNS, defaultRoot.resolve("mating-patterns"));
        Path tactics = pathFrom(props, KEY_TACTICS, defaultRoot.resolve("tactics"));
        Path puzzles = pathFrom(props, KEY_PUZZLES, defaultRoot.resolve("puzzles"));

        AppConfig config = new AppConfig(mating, tactics, puzzles);
        config.ensureDirectories();

        if (!configExists) {
            config.writeDefaultFile();
        }
        return config;
    }

    public Path matingPatternsDir() {
        return matingPatternsDir;
    }

    public Path tacticsDir() {
        return tacticsDir;
    }

    public Path puzzlesDir() {
        return puzzlesDir;
    }

    private static Path pathFrom(Properties props, String key, Path fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Path.of(value.trim());
    }

    private void ensureDirectories() {
        for (Path dir : new Path[]{matingPatternsDir, tacticsDir, puzzlesDir}) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                // map kon niet worden aangemaakt; de module toont dan een lege lijst
            }
        }
    }

    private void writeDefaultFile() {
        Properties props = new Properties();
        props.setProperty(KEY_MATING_PATTERNS, matingPatternsDir.toString());
        props.setProperty(KEY_TACTICS, tacticsDir.toString());
        props.setProperty(KEY_PUZZLES, puzzlesDir.toString());
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "ChessTrainerFX — PGN folders per module");
        } catch (IOException e) {
            // niet fataal: bij de volgende start gelden opnieuw de standaardwaarden
        }
    }
}
