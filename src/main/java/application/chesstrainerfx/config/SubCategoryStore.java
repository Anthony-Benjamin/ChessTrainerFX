package application.chesstrainerfx.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Beheer van puzzles-sub-categorieën. Elke sub-categorie is een sub-map van de
 * puzzles-map; de lijst wordt gepersisteerd als JSON ({@code subcategories.json}
 * in de puzzles-map). Bij het laden worden JSON en mappen gesynchroniseerd:
 * mappen die op schijf zijn bijgekomen worden toegevoegd, verdwenen mappen
 * worden uit de JSON verwijderd.
 */
public final class SubCategoryStore {

    private static final String FILE_NAME = "subcategories.json";
    private static final Pattern JSON_STRING = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern INVALID_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private final Path puzzlesDir;

    public SubCategoryStore(Path puzzlesDir) {
        this.puzzlesDir = puzzlesDir;
    }

    /** Gesynchroniseerde lijst van sub-categorieën (alfabetisch, hoofdletterongevoelig). */
    public List<String> load() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(readJson());
        names.removeIf(name -> !Files.isDirectory(puzzlesDir.resolve(name)));
        names.addAll(listFolders());

        List<String> result = List.copyOf(names);
        writeJson(result);
        return result;
    }

    public void create(String name) throws IOException {
        Files.createDirectories(puzzlesDir.resolve(name));
        load();
    }

    public void rename(String oldName, String newName) throws IOException {
        Files.move(puzzlesDir.resolve(oldName), puzzlesDir.resolve(newName));
        load();
    }

    /** Verwijdert de sub-categorie inclusief alle bestanden erin. */
    public void delete(String name) throws IOException {
        Path dir = puzzlesDir.resolve(name);
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
                for (Path path : paths) {
                    Files.delete(path);
                }
            }
        }
        load();
    }

    /**
     * Valideert een (nieuwe) sub-categorienaam.
     *
     * @return een foutmelding, of {@code null} als de naam geldig is.
     */
    public static String validateName(String name, List<String> existing) {
        if (name == null || name.isBlank()) {
            return "Voer een naam in.";
        }
        String trimmed = name.trim();
        if (INVALID_NAME_CHARS.matcher(trimmed).find()) {
            return "De naam bevat ongeldige tekens (\\ / : * ? \" < > |).";
        }
        if (trimmed.equals(".") || trimmed.equals("..")) {
            return "Deze naam is niet toegestaan.";
        }
        if (existing.stream().anyMatch(e -> e.equalsIgnoreCase(trimmed))) {
            return "Er bestaat al een sub-categorie met deze naam.";
        }
        return null;
    }

    private List<String> listFolders() {
        if (!Files.isDirectory(puzzlesDir)) {
            return List.of();
        }
        try (Stream<Path> dirs = Files.list(puzzlesDir)) {
            return dirs.filter(Files::isDirectory)
                    .map(d -> d.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private List<String> readJson() {
        Path file = puzzlesDir.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start < 0 || end <= start) {
                return List.of();
            }

            List<String> names = new ArrayList<>();
            Matcher m = JSON_STRING.matcher(content.substring(start, end + 1));
            while (m.find()) {
                names.add(unescapeJson(m.group(1)));
            }
            return names;
        } catch (IOException e) {
            return List.of();
        }
    }

    private void writeJson(List<String> names) {
        StringBuilder json = new StringBuilder("{\n  \"subCategories\": [");
        for (int i = 0; i < names.size(); i++) {
            json.append(i == 0 ? "\n" : ",\n");
            json.append("    \"").append(escapeJson(names.get(i))).append('"');
        }
        json.append(names.isEmpty() ? "]" : "\n  ]").append("\n}\n");

        try {
            Files.createDirectories(puzzlesDir);
            Files.writeString(puzzlesDir.resolve(FILE_NAME), json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // persistentie is best-effort; bij de volgende load wordt opnieuw geprobeerd
        }
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(next); // \" \\ en \/
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
