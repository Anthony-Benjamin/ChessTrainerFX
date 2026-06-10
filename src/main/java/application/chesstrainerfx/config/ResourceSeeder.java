package application.chesstrainerfx.config;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Kopieert de meegeleverde PGN-bestanden (resources) eenmalig naar de
 * geconfigureerde module-map, zodat de app na de eerste start direct
 * inhoud heeft. Bestaande bestanden worden nooit overschreven.
 */
public final class ResourceSeeder {

    private ResourceSeeder() {
    }

    /** Vult {@code targetDir} met *.pgn uit {@code resourceFolder} als de map nog geen PGN's bevat. */
    public static void seedIfEmpty(Path targetDir, String resourceFolder) {
        if (containsPgn(targetDir)) {
            return;
        }

        URL url = ResourceSeeder.class.getResource(resourceFolder);
        if (url == null) {
            return;
        }

        try {
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    copyPgnFiles(fs.getPath(resourceFolder), targetDir);
                }
            } else {
                copyPgnFiles(Path.of(uri), targetDir);
            }
        } catch (Exception e) {
            // seeden is best-effort; zonder seed blijft de module gewoon leeg
        }
    }

    private static boolean containsPgn(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(ResourceSeeder::isPgn);
        } catch (IOException e) {
            return false;
        }
    }

    private static void copyPgnFiles(Path sourceDir, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (Stream<Path> files = Files.list(sourceDir)) {
            for (Path source : files.filter(ResourceSeeder::isPgn).toList()) {
                Path target = targetDir.resolve(source.getFileName().toString());
                if (!Files.exists(target)) {
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static boolean isPgn(Path file) {
        return Files.isRegularFile(file)
                && file.getFileName().toString().toLowerCase().endsWith(".pgn");
    }
}
