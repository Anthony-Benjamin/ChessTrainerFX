package application.chesstrainerfx.utils;

import application.pgnreader.model.Exercise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawMoveTextConverterTest {

    /** Italiaans-achtige stelling: beide partijen ontwikkeld, wit aan zet. */
    private static final String ITALIAN =
            "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 0 1";

    @Test
    void restoresPieceLettersSplitsGluedTokensAndBuildsVariation() {
        String raw = """
                1.xe5!
                Grabbing a pawn.
                1...xe52.d4xd4
                2...b43.dxe5xc3+4.bxc3
                3.xd4d6
                """;

        RawMoveTextConverter.Conversion result = RawMoveTextConverter.convert(raw, ITALIAN);

        assertEquals("1. Nxe5! {Grabbing a pawn.} 1... Nxe5 2. d4 Bxd4"
                + " (2... Bb4 3. dxe5 Bxc3+ 4. bxc3) 3. Qxd4 d6", result.moveText());
        assertTrue(result.warnings().isEmpty(), () -> String.join("\n", result.warnings()));
    }

    @Test
    void convertedTextReplaysInExerciseSession() {
        String raw = """
                1.xe5xe52.d4xd4
                2...b43.dxe5xc3+4.bxc3
                3.xd4d6
                """;

        RawMoveTextConverter.Conversion result = RawMoveTextConverter.convert(raw, ITALIAN);
        Exercise exercise = new Exercise("Converted", ITALIAN, result.moveText(), "");

        assertDoesNotThrow(() -> new ExerciseSessionBuilder().buildSessionFromExercise(exercise));
    }

    @Test
    void ambiguousBareSquareDefaultsToPawnMoveWithWarning() {
        // Zowel de pion (e3-e4) als de loper (Bd3-e4) kan naar e4.
        String fen = "4k3/8/8/8/8/3BP3/8/4K3 w - - 0 1";

        RawMoveTextConverter.Conversion result = RawMoveTextConverter.convert("1.e4", fen);

        assertEquals("1. e4", result.moveText());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("Be4"));
    }

    @Test
    void resultEvaluationsAndCheckSuffixesAreHandled() {
        // Alleen de dame kan naar h5; "+-" en "1-0" verdwijnen, "+" blijft aan de zet plakken.
        String fen = "7k/8/8/8/8/8/8/3QK3 w - - 0 1";

        RawMoveTextConverter.Conversion result = RawMoveTextConverter.convert("1.h5+ +- 1-0", fen);

        assertEquals("1. Qh5+", result.moveText());
        assertTrue(result.warnings().isEmpty(), () -> String.join("\n", result.warnings()));
    }
}
