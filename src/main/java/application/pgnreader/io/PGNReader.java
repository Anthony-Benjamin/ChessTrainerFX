package application.pgnreader.io;

import application.pgnreader.model.Exercise;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PGNReader {

    /**
     * Leest een PGN-hoofdstuk (meerdere partijen/oefeningen) van schijf en splitst in Exercise-objecten.
     * Geeft een lege lijst terug als het bestand ontbreekt of niet leesbaar is.
     */
    public static List<Exercise> readChapter(Path file) {
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return splitExercises(content);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Splitst de volledige PGN-tekst in blokken, elk beginnend bij een [Event ...] tag.
     */
    private static List<Exercise> splitExercises(String content) {
        List<Exercise> result = new ArrayList<>();
        if (content == null || content.isBlank()) return result;

        // Normaliseer line-endings voor de matcher
        String src = content.replace("\r\n", "\n").replace('\r', '\n').trim();

        // (?s) = DOTALL: . matcht ook nieuwe regels
        Pattern blockPattern = Pattern.compile("(?s)(?=\\[Event\\b)(.*?)(?=(\\n\\[Event\\b|\\z))");
        Matcher matcher = blockPattern.matcher(src);

        while (matcher.find()) {
            String block = matcher.group(1).trim();

            String title = extractTag(block, "White"); // jij gebruikt de [White "..."] als titel
            String subtitle = extractTag(block, "Black"); // optionele subtitel, bv. "Position 1"
            if ("?".equals(subtitle)) subtitle = ""; // PGN-conventie voor onbekend
            String fen   = extractTag(block, "FEN");
            String moves = extractMoves(block);             // movetext zonder comments/NAGs/resultaat, met varianten
            String comments = String.join(" | ", extractComments(block)); // losse comments uit { ... }

            result.add(new Exercise(title, subtitle, fen, moves, comments));
        }
        return result;
    }

    /**
     * Haal de waarde uit een PGN-tag, bijvoorbeeld [FEN "...."] of [White "...."].
     */
    private static String extractTag(String text, String tag) {
        if (text == null) return "";
        Pattern p = Pattern.compile("\\[" + Pattern.quote(tag) + " \"(.*?)\"]");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : "";
    }

    /**
     * Haal het movetext-gedeelte op en maak het schoon:
     * - verwijder { ... } comments (multi-line)
     * - verwijder {[%eval ...]} of {[%evp ...]} engine-evaluaties
     * - behoud ( ... ) varianten voor variant-aware oefeningen
     * - verwijder NAGs $1, $2, ...
     * - verwijder resultaten (1-0/0-1/1/2-1/2/*)
     * - normaliseer whitespace
     */
    private static String extractMoves(String block) {
        if (block == null) return "";

        // Normaliseer line endings
        String normalized = block.replace("\r\n", "\n").replace('\r', '\n');

        // Pak alles NA de eerste lege regel (headers -> body)
        int split = normalized.indexOf("\n\n");
        String movetext = (split >= 0) ? normalized.substring(split).trim() : normalized;

        // Maak het 1 regel
        movetext = movetext.replace("\n", " ");

        // 1) verwijder engine-evaluatieblokken {[%...]}
        movetext = movetext.replaceAll("(?s)\\{\\s*\\[%[^}]*\\]\\s*\\}", " ");

        // 2) comments { ... } (incl. {[%csl ...]})
        movetext = movetext.replaceAll("(?s)\\{[^}]*\\}", " ");

        // 3) NAGs $n
        movetext = movetext.replaceAll("\\$\\d+", " ");

        // 4) resultaten (ook *) — vooral aan het einde
        movetext = movetext.replaceAll("(1-0|0-1|1/2-1/2|\\*)\\s*$", " ");

        // 5) whitespace normaliseren
        movetext = movetext.replaceAll("\\s+", " ").trim();

        return movetext;
    }

    /**
     * Extraheer alle comments uit het volledige block (niet uit het al-bewerkte movetext),
     * zodat de oorspronkelijke tekst behouden blijft.
     * - sla engine evaluatieblokken over ({[%eval ...]} / {[%evp ...]})
     * - sla pure diagram markers over ({[#]})
     */
    private static List<String> extractComments(String block) {
        List<String> comments = new ArrayList<>();
        if (block == null || block.isBlank()) return comments;

        Matcher m = Pattern.compile("(?s)\\{([^}]*)\\}").matcher(block);

        Pattern diagramOnly = Pattern.compile("^\\s*(\\[#\\]|#)\\s*$");
        Pattern engineEval  = Pattern.compile("^\\s*\\[%[^]]*\\]\\s*$"); // {[%eval ...]} of {[%evp ...]}

        while (m.find()) {
            String c = m.group(1).trim();

            // 1) sla engine-evaluatie comments over
            if (engineEval.matcher(c).matches()) {
                continue;
            }

            // 2) sla pure diagram-markers over
            if (diagramOnly.matcher(c).matches()) {
                continue;
            }

            // 3) verwijder inline annotaties ([%csl ...], [%cal ...], ...) en [#] markers
            c = c.replaceAll("\\[%[^]]*\\]", " ");
            c = c.replaceAll("\\[\\s*#\\s*\\]", " ");
            c = c.replaceAll("\\s+", " ").trim();

            if (!c.isEmpty()) {
                comments.add(c);
            }
        }
        return comments;
    }
}
