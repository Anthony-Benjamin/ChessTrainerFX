// File: PGNReader.java
package application.pgnreader.io;

import application.pgnreader.model.Exercise;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class PGNReader {

    public static List<Exercise> readChapter(String path) {
        try (InputStream in = PGNReader.class.getResourceAsStream(path)) {
            if (in == null)
                throw new IllegalArgumentException("Bestand niet gevonden: " + path);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return splitExercises(content);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static List<Exercise> splitExercises(String content) {
        List<Exercise> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?s)(?=\\[Event\\b)(.*?)(?=(\\n\\[Event\\b|\\Z))");
        Matcher matcher = pattern.matcher(content.trim());

        while (matcher.find()) {
            String block = matcher.group(1).trim();
            String title = extractTag(block, "White");
            String fen = extractTag(block, "FEN");
            String moves = extractMoves(block);
            String comments = String.join(" | ", extractComments(block));
            result.add(new Exercise(title, fen, moves, comments));
        }
        return result;
    }

    private static String extractTag(String text, String tag) {
        Pattern p = Pattern.compile("\\[" + tag + " \"(.*?)\"]");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private static String extractMoves(String block) {
        int lastBracket = block.lastIndexOf("]");
        String moves = (lastBracket != -1) ? block.substring(lastBracket + 1) : block;
        moves = moves.replaceAll("\\{.*?}", ""); // verwijder commentaar
        return moves.replaceAll("\\s+", " ").trim();
    }

    private static List<String> extractComments(String moves) {
        List<String> comments = new ArrayList<>();
        Matcher m = Pattern.compile("\\{(.*?)}").matcher(moves);
        while (m.find()) comments.add(m.group(1).trim());
        return comments;
    }
}
