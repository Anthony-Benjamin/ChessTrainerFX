package application.pgnreader.io;

import application.pgnreader.model.Chapter;
import application.pgnreader.model.Exercise;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Laadt alle PGN-bestanden uit een map als hoofdstukken.
 * Bestanden worden natuurlijk gesorteerd (1_, 2_, … 10_) zodat de
 * volgorde van genummerde hoofdstukken klopt.
 */
public final class ChapterLoader {

    private ChapterLoader() {
    }

    public static List<Chapter> loadChapters(Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }

        List<Path> pgnFiles;
        try (Stream<Path> files = Files.list(folder)) {
            pgnFiles = files
                    .filter(f -> Files.isRegularFile(f)
                            && f.getFileName().toString().toLowerCase().endsWith(".pgn"))
                    .sorted(Comparator.comparingLong(ChapterLoader::leadingNumber)
                            .thenComparing(f -> f.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }

        List<Chapter> chapters = new ArrayList<>();
        for (Path file : pgnFiles) {
            List<Exercise> exercises = PGNReader.readChapter(file);
            if (!exercises.isEmpty()) {
                chapters.add(new Chapter(chapterTitle(file, exercises), exercises));
            }
        }
        return chapters;
    }

    private static String chapterTitle(Path file, List<Exercise> exercises) {
        String title = exercises.get(0).getTitle();
        if (title != null && !title.isBlank()) {
            return title;
        }
        return titleFromFileName(file.getFileName().toString());
    }

    /** "13_backrank_mate.pgn" → "backrank mate" */
    private static String titleFromFileName(String fileName) {
        String name = fileName.replaceAll("(?i)\\.pgn$", "");
        name = name.replaceFirst("^\\d+_?", "");
        return name.replace('_', ' ').trim();
    }

    private static long leadingNumber(Path file) {
        String name = file.getFileName().toString();
        int i = 0;
        while (i < name.length() && Character.isDigit(name.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return Long.MAX_VALUE; // bestanden zonder nummer achteraan
        }
        return Long.parseLong(name.substring(0, Math.min(i, 18)));
    }
}
