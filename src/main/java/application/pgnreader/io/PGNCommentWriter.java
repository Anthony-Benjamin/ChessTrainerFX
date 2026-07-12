package application.pgnreader.io;

import application.chesstrainerfx.utils.PgnUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Schrijft de eigen notitie van de gebruiker ({[%unote] ...}) terug in het
 * bron-PGN-bestand. Andere spellen, BOM en regeleindes blijven ongewijzigd.
 */
public class PGNCommentWriter {

    // Zelfde blokgrenzen als PGNReader.splitExercises, zodat gameIndex 1-op-1 klopt.
    private static final Pattern BLOCK_PATTERN =
            Pattern.compile("(?s)(?=\\[Event\\b)(.*?)(?=(\\n\\[Event\\b|\\z))");

    private static final Pattern EXISTING_NOTE = Pattern.compile(
            "(?s)\\s*\\{\\s*" + Pattern.quote(PGNReader.USER_NOTE_MARKER) + "[^}]*\\}");

    private static final Pattern NOTE_BLOCK = Pattern.compile(
            "(?s)\\{\\s*" + Pattern.quote(PGNReader.USER_NOTE_MARKER) + "[^}]*\\}");

    // Resultaat-token aan het einde van een blok; nieuw commentaar hoort daar direct vóór.
    private static final Pattern RESULT_TAIL = Pattern.compile("(1-0|0-1|1/2-1/2|\\*)\\s*$");

    private PGNCommentWriter() {
        // utility class, no instances
    }

    /**
     * Vervangt, plaatst of verwijdert (bij lege tekst) de notitie van het
     * gameIndex-de spel in het bestand. Geeft false terug als het bestand of
     * het spelblok niet gevonden kan worden of het schrijven mislukt.
     */
    public static boolean writeUserNote(Path file, int gameIndex, String note) {
        return rewriteBlock(file, gameIndex, block -> updateNoteInBlock(block, note));
    }

    private interface BlockEdit {
        String apply(String block);
    }

    private static boolean rewriteBlock(Path file, int gameIndex, BlockEdit edit) {
        if (file == null || gameIndex < 0) return false;
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher matcher = BLOCK_PATTERN.matcher(content);

            int index = 0;
            while (matcher.find()) {
                if (index == gameIndex) {
                    String updated = edit.apply(matcher.group(1));
                    String result = content.substring(0, matcher.start(1))
                            + updated
                            + content.substring(matcher.end(1));
                    Files.write(file, result.getBytes(StandardCharsets.UTF_8));
                    return true;
                }
                index++;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String updateNoteInBlock(String block, String note) {
        String sanitized = PgnUtils.sanitizeComment(note);
        String noteBlock = sanitized.isEmpty()
                ? ""
                : "{" + PGNReader.USER_NOTE_MARKER + " " + sanitized + "}";

        Matcher existing = EXISTING_NOTE.matcher(block);
        if (existing.find()) {
            String replacement = noteBlock.isEmpty() ? "" : " " + noteBlock;
            return existing.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        if (noteBlock.isEmpty()) {
            return block;
        }
        return insertBeforeTail(block, noteBlock);
    }

    /** Voegt een commentaarblok in vóór de eigen notitie of anders vóór het resultaat-token. */
    private static String insertBeforeTail(String block, String toInsert) {
        Matcher note = NOTE_BLOCK.matcher(block);
        if (note.find()) {
            return block.substring(0, note.start()) + toInsert + " " + block.substring(note.start());
        }
        Matcher result = RESULT_TAIL.matcher(block);
        if (result.find()) {
            return block.substring(0, result.start()) + toInsert + " " + block.substring(result.start());
        }
        return block + " " + toInsert;
    }
}
