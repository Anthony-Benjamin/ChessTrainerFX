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
    void inlineProseCitationAndLateVariationOnOneLine() {
        // Boek-layout op één regel: proza tussen de zetten, een partij-citatie met
        // resultaat, en een variatie (2...Bb4) die pas komt nadat de hoofdlijn al
        // tot zet 3 is doorgelopen.
        String raw = "1.Nxe5 Grabbing a pawn. 1...Nxe5 2.d4 Bxd4 1-0 Someone-Else, Sometown 2002;"
                + " 3.Qxd4 d6; 2...Bb4 3.dxe5 Bxc3+ 4.bxc3";

        RawMoveTextConverter.Conversion result = RawMoveTextConverter.convert(raw, ITALIAN);

        assertEquals("1. Nxe5 {Grabbing a pawn.} 1... Nxe5 2. d4 Bxd4"
                + " {1-0 Someone-Else, Sometown 2002} (2... Bb4 3. dxe5 Bxc3+ 4. bxc3)"
                + " 3. Qxd4 d6", result.moveText());
        assertTrue(result.warnings().isEmpty(), () -> String.join("\n", result.warnings()));
    }

    @Test
    void bookFragmentWithCitationAndEnDashEvaluationsConverts() {
        // Letterlijk boekfragment: alles op één regel(afbreking), citatie met
        // resultaat, "+–" met en-dash, en de variatie 14...Ra7 nadat de hoofdlijn
        // al tot en met 16.Bxa8 is doorgelopen.
        String raw = """
                13.Nxf6+! Clearing the e4-square for the bishop. 13...Bxf6 14.Be4 e5 1-0 Grigorov-Veselinov,
                Borovec 2002; 15.Qxd8 Rxd8 16.Bxa8+–; 14...Ra7 15.Qxd8 Rxd8 16.Bxb8+–
                """;
        String fen = "rn1q1rk1/4ppbp/5np1/8/4NB2/8/PP3PBP/R2Q1RK1 w - - 0 1";

        RawMoveTextConverter.Conversion result = RawMoveTextConverter.convert(raw, fen);

        assertEquals("13. Nxf6+! {Clearing the e4-square for the bishop.} 13... Bxf6 14. Be4 e5"
                + " {1-0 Grigorov-Veselinov, Borovec 2002} (14... Ra7 15. Qxd8 Rxd8 16. Bxb8)"
                + " 15. Qxd8 Rxd8 16. Bxa8", result.moveText());
        // Eén terechte waarschuwing: "e5" kan hier ook de loperzet Be5 zijn.
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("Be5"));
    }

    @Test
    void proseClosesOneMoveVariationSoMainLineContinues() {
        // "45.Rd6 was played in the game." is een aside op wits 45e; het proza
        // sluit die variatie af, waarna 45...Rxe3 de hoofdlijn (na 45.f3) vervolgt.
        // Rxe3 is in béide lijnen legaal — pas 46.Rh8+ (toren nog op d8) beslist.
        String raw = """
                45.f3! 45.Rd6 was played in the game. 45...Rxe3 46.Rh8+ Kxh8 47.Qxg6+– Karpov-Timman,
                Groningen m 2013.
                """;
        String fen = "3R4/6pk/2p3q1/2Pp4/4rP1p/4P3/2Q2P2/5K2 w - - 0 1";

        RawMoveTextConverter.Conversion result = RawMoveTextConverter.convert(raw, fen);

        assertEquals("45. f3! (45. Rd6 {was played in the game.}) 45... Rxe3 46. Rh8+ Kxh8"
                + " 47. Qxg6 {Karpov-Timman, Groningen m 2013.}", result.moveText());
        assertTrue(result.warnings().isEmpty(), () -> String.join("\n", result.warnings()));
    }

    @Test
    void cleanPgnWithParenthesesSurvivesConversion() {
        String raw = "1. e4 e5 (1... c5 2. Nf3)";

        RawMoveTextConverter.Conversion result =
                RawMoveTextConverter.convert(raw, application.chesstrainerfx.model.BoardModel.START_FEN);

        assertEquals("1. e4 e5 (1... c5 2. Nf3)", result.moveText());
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
