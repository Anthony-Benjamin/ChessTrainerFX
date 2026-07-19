package application.chesstrainerfx.utils;

import application.pgnreader.io.PGNReader;

public class PgnUtils {

    private PgnUtils() {
        // utility class, no instances
    }

    /**
     * Verwijdert ( ... ) varianten uit een PGN-string.
     */
    public static String removeVariations(String pgn) {
        StringBuilder result = new StringBuilder();
        int depth = 0;

        for (char c : pgn.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0) {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Maakt een ruwe moves-string schoon:
     * - normaliseert whitespace
     * - haalt varianten, comments, NAGs, resultaten, tags weg
     */
    public static String cleanMoveString(String moveString) {
        if (moveString == null) {
            return "";
        }

        // 1) Normaliseer rare whitespace (NBSP etc.) + newlines
        String clean = moveString
                .replace('\u00A0', ' ')      // non-breaking space → normale spatie
                .replaceAll("\\r\\n?", "\n") // normalize line endings
                .replaceAll("\\s+", " ");    // alle whitespace -> single space

        // 2) Variaties weg
        clean = removeVariations(clean);

        // 3) Verwijder {...} blocks volledig (incl. {[%csl ...]})
        clean = clean.replaceAll("\\{[^}]*}", " ");

        // 4) Verwijder inline tags zoals [%csl ...] / [%cal ...] als ze buiten braces staan
        clean = clean.replaceAll("\\[%[^\\]]*]", " ");

        // 5) Verwijder NAGs zoals $1
        clean = clean.replaceAll("\\$\\d+", " ");

        // 6) Verwijder resultaat (ook *), waar dan ook los staat
        clean = clean.replace("*", " ");
        clean = clean.replaceAll("(?i)\\b(1-0|0-1|1/2-1/2)\\b", " ");

        // 7) Final whitespace cleanup
        clean = clean.replaceAll("\\s+", " ").trim();

        return clean;
    }

    /**
     * Bouwt een minimale PGN-oefening: standaard tags, de FEN van de
     * opgezette positie, optioneel commentaar, de (optionele) zettenreeks en
     * een optionele eigen notitie als {[%unote] ...}-blok vóór het resultaat.
     */
    public static String buildExercisePgn(String title, String fen, String moves, String comments, String userNote) {
        StringBuilder pgn = new StringBuilder();

        appendTag(pgn, "Event", "?");
        appendTag(pgn, "Site", "?");
        appendTag(pgn, "Date", "????.??.??");
        appendTag(pgn, "Round", "?");
        appendTag(pgn, "White", title);
        appendTag(pgn, "Black", "?");
        appendTag(pgn, "Result", "*");

        appendTag(pgn, "SetUp", "1");
        appendTag(pgn, "FEN", fen);

        pgn.append("\n");
        if (moves.startsWith("1 -")) {
            moves = moves.replace("1 -", "1.");
        }
        String comment = sanitizeComment(comments);
        if (!comment.isEmpty()) {
            pgn.append("{").append(comment).append("} ");
        }
        pgn.append(moves);
        if (!moves.isBlank()) pgn.append(" ");
        String note = sanitizeComment(userNote);
        if (!note.isEmpty()) {
            pgn.append("{").append(PGNReader.USER_NOTE_MARKER).append(" ").append(note).append("} ");
        }
        pgn.append("*\n");

        return pgn.toString();
    }

    /**
     * Maakt commentaartekst veilig voor een {...}-blok: accolades zouden het blok
     * vroegtijdig sluiten en een newline vóór "[Event" zou het bestand in twee
     * games splitsen, dus die worden vervangen respectievelijk samengevouwen.
     */
    public static String sanitizeComment(String comments) {
        if (comments == null) return "";
        return comments
                .replace('{', '(')
                .replace('}', ')')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void appendTag(StringBuilder sb, String key, String value) {
        sb.append("[")
                .append(key)
                .append(" \"")
                .append(value)
                .append("\"]\n");
    }
}