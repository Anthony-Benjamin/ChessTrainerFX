package application.chesstrainerfx.utils;

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
}