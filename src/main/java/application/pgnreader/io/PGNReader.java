package application.pgnreader.io;

import application.pgnreader.model.Exercise;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PGNReader {

    public static List<Exercise> leesHoofdstuk(String resourceNaam) {
        try (InputStream in = PGNReader.class.getResourceAsStream("/pgn/Mates/Chapters/" + resourceNaam)) {
            if (in == null) throw new IllegalArgumentException("Bestand niet gevonden: " + resourceNaam);
            String inhoud = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return splitExercises(inhoud);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private static List<Exercise> splitExercises(String inhoud) {
        List<Exercise> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?s)(?=\\[Event\\b)(.*?)(?=(\\n\\[Event\\b|\\Z))");
        Matcher matcher = pattern.matcher(inhoud.trim());

        while (matcher.find()) {
            String block = matcher.group(1).trim();

            String title = extractTag(block, "White");
            String fen = extractTag(block, "FEN");
            String moves = extractMoves(block);
            String comments = String.valueOf(extractComments(block));
            result.add(new Exercise(title, fen, moves, comments));
        }

        return result;
    }

    private static String extractTag(String text, String tag) {
        String zoek = "[" + tag + " \"";
        int start = text.indexOf(zoek);
        if (start == -1) return "";
        start += zoek.length();
        int end = text.indexOf("\"", start);
        if (end == -1) return "";
        return text.substring(start, end);
    }

    private static String extractMoves(String block) {
        int lastBracket = block.lastIndexOf("]");
        String moves = (lastBracket != -1) ? block.substring(lastBracket + 1) : block;

        // 1️⃣ Verwijder commentaar tussen accolades { ... }
        StringBuilder cleaned = new StringBuilder();
        boolean inComment = false;
        for (char c : moves.toCharArray()) {
            if (c == '{') {
                inComment = true;
            } else if (c == '}') {
                inComment = false;
            } else if (!inComment) {
                cleaned.append(c);
            }
        }

        moves = cleaned.toString();

        // 2️⃣ Verwijder overbodige whitespace, tabs en nieuwe regels
        moves = moves.replaceAll("\\s+", " ").trim();

        // 3️⃣ Verwijder eventuele voorlooptekens zoals '}' die zijn blijven hangen
        while (moves.startsWith("}")) {
            moves = moves.substring(1).trim();
        }

        return moves;
    }
    private static List<String> extractComments(String moves) {
        List<String> comments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inComment = false;

        for (char c : moves.toCharArray()) {
            if (c == '{') {
                inComment = true;
                current = new StringBuilder();
            } else if (c == '}') {
                inComment = false;
                comments.add(current.toString().trim());
            } else if (inComment) {
                current.append(c);
            }
        }

        return comments;
    }



}
