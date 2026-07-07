package application.chesstrainerfx.controller;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.CoordinateSystem;
import application.chesstrainerfx.utils.ExerciseSessionBuilder;
import application.chesstrainerfx.utils.PieceColor;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.utils.PieceType;
import application.chesstrainerfx.utils.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerReplayTest {

    private final ExerciseSessionBuilder builder = new ExerciseSessionBuilder();

    @Test
    void playsMainLineMovesWithoutUserInput() {
        Controller controller = new Controller();
        BoardModel board = setUpGame(controller, "1. e4 e5 2. Nf3 Nc6", new ArrayList<>());

        controller.playNextExerciseMove(board);
        controller.playNextExerciseMove(board);

        PieceModel whitePawn = board.getSquare(pos("e4")).getPiece();
        PieceModel blackPawn = board.getSquare(pos("e5")).getPiece();
        assertNotNull(whitePawn);
        assertEquals(PieceType.PAWN, whitePawn.getType());
        assertEquals(PieceColor.WHITE, whitePawn.getColor());
        assertNotNull(blackPawn);
        assertEquals(PieceColor.BLACK, blackPawn.getColor());
        assertTrue(controller.isWhiteTurn());
    }

    @Test
    void executesCastlingDuringReplay() {
        Controller controller = new Controller();
        BoardModel board = setUpGame(controller,
                "1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 4. O-O", new ArrayList<>());

        for (int ply = 0; ply < 7; ply++) {
            controller.playNextExerciseMove(board);
        }

        PieceModel king = board.getSquare(pos("g1")).getPiece();
        PieceModel rook = board.getSquare(pos("f1")).getPiece();
        assertNotNull(king);
        assertEquals(PieceType.KING, king.getType());
        assertNotNull(rook);
        assertEquals(PieceType.ROOK, rook.getType());
        assertNull(board.getSquare(pos("h1")).getPiece());
    }

    @Test
    void reportsSolvedWithoutMateAtEndOfReplay() {
        Controller controller = new Controller();
        List<Boolean> solvedFlags = new ArrayList<>();
        BoardModel board = setUpGame(controller, "1. e4 e5 2. Nf3 Nc6", solvedFlags);

        for (int ply = 0; ply < 4; ply++) {
            controller.playNextExerciseMove(board);
        }

        assertEquals(List.of(false), solvedFlags);

        // Extra klikken na het einde doen niets
        controller.playNextExerciseMove(board);
        assertEquals(List.of(false), solvedFlags);
    }

    private BoardModel setUpGame(Controller controller, String moves, List<Boolean> solvedFlags) {
        application.pgnreader.model.Exercise exercise =
                new application.pgnreader.model.Exercise("test", null, moves, "");

        BoardModel board = new BoardModel();
        board.initializeFromFEN(BoardModel.START_FEN);

        controller.syncTurnFromFEN(BoardModel.START_FEN);
        controller.setOnExerciseSolved(solvedFlags::add);
        controller.setOnExerciseUnsolved(() -> { });
        controller.setExerciseSession(builder.buildSessionFromExercise(exercise));
        controller.startNewHistory(board);
        return board;
    }

    private static Position pos(String square) {
        int[] index = CoordinateSystem.coordinateToIndex(square);
        return new Position(index[0], index[1]);
    }
}
