package application.chesstrainerfx.utils;

import application.pgnreader.model.Exercise;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExerciseSessionBuilderTest {

    private static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final ExerciseSessionBuilder builder = new ExerciseSessionBuilder();

    @Test
    void keepsLinearExercisesWorking() {
        ExerciseSession session = build("1. e4 e5 2. Nf3");

        assertEquals(List.of("e4"), session.getExpectedSanOptions());
        assertTrue(session.isCorrectMove(move("e2", "e4")));
        session.advancePly();

        assertEquals(List.of("e5"), session.getExpectedSanOptions());
        session.advancePly();

        assertEquals(List.of("Nf3"), session.getExpectedSanOptions());
    }

    @Test
    void acceptsMultipleFirstMovesAndFollowsChosenVariation() {
        ExerciseSession session = build("1. e4 (1. d4 d5 2. c4) e5 2. Nf3");

        assertEquals(List.of("e4", "d4"), session.getExpectedSanOptions());
        assertEquals("e4 or d4", session.getExpectedSan());

        assertTrue(session.isCorrectMove(move("d2", "d4")));
        session.advancePly();

        assertEquals(List.of("d5"), session.getExpectedSanOptions());
        session.advancePly();

        assertEquals(List.of("c4"), session.getExpectedSanOptions());
    }

    @Test
    void followsMainLineWhenMainLineMoveIsChosen() {
        ExerciseSession session = build("1. e4 (1. d4 d5 2. c4) e5 2. Nf3");

        assertTrue(session.isCorrectMove(move("e2", "e4")));
        session.advancePly();

        assertEquals(List.of("e5"), session.getExpectedSanOptions());
    }

    @Test
    void supportsVariationAfterLaterPly() {
        ExerciseSession session = build("1. e4 e5 2. Nf3 (2. Bc4 Nf6) Nc6");

        session.advancePly(); // e4
        session.advancePly(); // e5

        assertEquals(List.of("Nf3", "Bc4"), session.getExpectedSanOptions());
        assertTrue(session.isCorrectMove(move("f1", "c4")));
        session.advancePly();

        assertEquals(List.of("Nf6"), session.getExpectedSanOptions());
    }

    @Test
    void supportsNestedVariation() {
        ExerciseSession session = build("1. e4 (1. d4 d5 (1... Nf6) 2. c4) e5");

        assertTrue(session.isCorrectMove(move("d2", "d4")));
        session.advancePly();

        assertEquals(List.of("d5", "Nf6"), session.getExpectedSanOptions());
    }

    @Test
    void canAdvanceToChosenComputerVariation() {
        ExerciseSession session = build("1. e4 e5 (1... c5 2. Nf3) 2. Nf3");

        assertTrue(session.isCorrectMove(move("e2", "e4")));
        session.advancePly();

        List<ExerciseSession.Node> replies = session.getCandidateNodes();
        assertEquals(List.of("e5", "c5"), replies.stream().map(ExerciseSession.Node::getSan).toList());

        ExerciseSession.Node sicilian = replies.get(1);
        session.advanceTo(sicilian);

        assertEquals(List.of("Nf3"), session.getExpectedSanOptions());
    }

    @Test
    void buildsFromStartPositionWhenFenIsMissing() {
        Exercise exercise = new Exercise("test", "", "1. e4 e5 2. Nf3", "");
        ExerciseSession session = builder.buildSessionFromExercise(exercise);

        assertEquals(List.of("e4"), session.getExpectedSanOptions());
        assertTrue(session.isCorrectMove(move("e2", "e4")));
    }

    @Test
    void resolvesFullGameWithoutFenTag() {
        String moves = "1. e4 e5 2. Bc4 c6 3. Qe2 d6 4. c3 f5 5. d3 Nf6 6. exf5 Bxf5 7. d4 e4 "
                + "8. Bg5 d5 9. Bb3 Bd6 10. Nd2 Nbd7 11. h3 h6 12. Be3 Qe7 13. f4 h5 "
                + "14. c4 a6 15. cxd5 cxd5 16. Qf2 O-O 17. Ne2 b5 18. O-O Nb6 19. Ng3 g6 "
                + "20. Rac1 Nc4 21. Nxf5 gxf5 22. Qg3+ Qg7 23. Qxg7+ Kxg7 24. Bxc4 bxc4 25. g3 "
                + "Rab8 26. b3 Ba3 27. Rc2 cxb3 28. axb3 Rbc8 29. Rxc8 Rxc8 30. Ra1 Bb4 "
                + "31. Rxa6 Rc3 32. Kf2 Rd3 33. Ra2 Bxd2 34. Rxd2 Rxb3 35. Rc2 h4 36. Rc7+ "
                + "Kg6 37. gxh4 Nh5 38. Rd7 Nxf4 39. Bxf4 Rf3+ 40. Kg2 Rxf4 41. Rxd5 Rf3 "
                + "42. Rd8 Rd3 43. d5 f4 44. d6 Rd2+ 45. Kf1 Kf7 46. h5 f3";
        Exercise exercise = new Exercise("Bruhl - Philidor", null, moves, "");

        ExerciseSession session = builder.buildSessionFromExercise(exercise);

        assertEquals(List.of("e4"), session.getExpectedSanOptions());
        for (int ply = 0; ply < 92; ply++) {
            assertTrue(session.hasNext(), "onverwacht einde op ply " + ply);
            session.advancePly();
        }
        assertFalse(session.hasNext());
    }

    @Test
    void canRestoreCurrentVariantNodeById() {
        ExerciseSession session = build("1. e4 (1. d4 d5 2. c4) e5 2. Nf3");
        int rootId = session.getCurrentNodeId();

        assertTrue(session.isCorrectMove(move("d2", "d4")));
        session.advancePly();
        int variationId = session.getCurrentNodeId();

        session.setCurrentNodeId(rootId);
        assertEquals(List.of("e4", "d4"), session.getExpectedSanOptions());

        session.setCurrentNodeId(variationId);
        assertEquals(List.of("d5"), session.getExpectedSanOptions());
    }

    private ExerciseSession build(String moves) {
        Exercise exercise = new Exercise("test", START_FEN, moves, "");
        return builder.buildSessionFromExercise(exercise);
    }

    private static Move move(String from, String to) {
        return new Move(pos(from), pos(to));
    }

    private static Position pos(String square) {
        int[] index = CoordinateSystem.coordinateToIndex(square);
        return new Position(index[0], index[1]);
    }
}
