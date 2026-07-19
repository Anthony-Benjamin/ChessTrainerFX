package application.chesstrainerfx.utils;

import application.pgnreader.io.PGNReader;
import application.pgnreader.model.Exercise;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PgnUtilsTest {

    private static final String FEN = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w - - 0 1";

    @Test
    void commentsSurviveRoundTripWithBracesSanitized() throws Exception {
        String pgn = PgnUtils.buildExercisePgn("Mat in 2", FEN,
                "1. Qxf7+ Kd8 2. Qxf8#", "Uitleg { met accolades }\nen een newline", null);

        Path file = Files.createTempFile("exercise-comments", ".pgn");
        Files.writeString(file, pgn);
        List<Exercise> exercises = PGNReader.readChapter(file);

        assertEquals(1, exercises.size());
        assertEquals("Uitleg ( met accolades ) en een newline", exercises.getFirst().getComments());
        assertEquals("1. Qxf7+ Kd8 2. Qxf8#", exercises.getFirst().getMoves().trim());
    }

    @Test
    void blankCommentsProduceNoBraces() {
        String pgn = PgnUtils.buildExercisePgn("Titel", FEN, "1. Qxf7+", "  \n ", "  ");

        assertFalse(pgn.contains("{"));
        assertEquals(PgnUtils.buildExercisePgn("Titel", FEN, "1. Qxf7+", null, null), pgn);
    }

    @Test
    void commentWithoutMovesParsesAsCommentsOnlyExercise() throws Exception {
        String pgn = PgnUtils.buildExercisePgn("Alleen uitleg", FEN, "", "Zoek het beste plan.", null);

        Path file = Files.createTempFile("exercise-comment-only", ".pgn");
        Files.writeString(file, pgn);
        List<Exercise> exercises = PGNReader.readChapter(file);

        assertEquals(1, exercises.size());
        assertEquals("Zoek het beste plan.", exercises.getFirst().getComments());
        assertEquals("", exercises.getFirst().getMoves().trim());
    }

    @Test
    void userNoteSurvivesRoundTripSeparateFromComments() throws Exception {
        String pgn = PgnUtils.buildExercisePgn("Met notitie", FEN,
                "1. Qxf7+ Kd8 2. Qxf8#", "Auteurscommentaar.", "Eigen notitie.");

        Path file = Files.createTempFile("exercise-user-note", ".pgn");
        Files.writeString(file, pgn);
        List<Exercise> exercises = PGNReader.readChapter(file);

        assertEquals(1, exercises.size());
        assertEquals("Eigen notitie.", exercises.getFirst().getUserNote());
        assertEquals("Auteurscommentaar.", exercises.getFirst().getComments());
        assertEquals("1. Qxf7+ Kd8 2. Qxf8#", exercises.getFirst().getMoves().trim());
    }
}
