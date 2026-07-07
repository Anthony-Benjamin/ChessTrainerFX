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
