package application.chesstrainerfx.controller;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.ExerciseSessionBuilder;
import application.chesstrainerfx.utils.PgnUtils;
import application.chesstrainerfx.view.ChapterWindow;
import application.pgnreader.model.Exercise;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterPresenter {

    private final ChapterWindow view;
    private final List<Exercise> exercises;
    private final ExerciseSessionBuilder sessionBuilder;
    private final Consumer<Void> onBack;
    private final Stage stage;

    private Controller currentController;

    public ChapterPresenter(
            ChapterWindow view,
            List<Exercise> exercises,
            ExerciseSessionBuilder sessionBuilder,
            Consumer<Void> onBack,
            Stage stage) {
        this.view = view;
        this.exercises = exercises;
        this.sessionBuilder = sessionBuilder;
        this.onBack = onBack;
        this.stage = stage;
    }

    /** Wordt aangeroepen als de gebruiker op een exercise-tegel klikt. */
    public void onExerciseSelected(Exercise ex) {
        stage.setTitle(stage.getTitle() + "-" + ex.getTitle());
        // --- 1) Model + controller opbouwen ---
        String fen = (ex.getFen() == null) ? "" : ex.getFen().trim();

        BoardModel boardModel = new BoardModel();
        boardModel.initializeFromFEN(fen);

        Controller controller = new Controller();
        controller.syncTurnFromFEN(fen);
        controller.startNewHistory(boardModel);
        controller.setExerciseStage(Controller.ExerciseStage.PLAYER_TO_MOVE);

        // ExerciseSession opbouwen
        controller.setExerciseSession(
                sessionBuilder.buildSessionFromExercise(ex)
        );

        this.currentController = controller;

        // --- 2) Moves voor de ListView voorbereiden ---
        List<String> moveLines = buildMoveLines(ex.getMoves());

        // --- 3) View laten overschakelen naar de board-weergave ---
        view.showExerciseBoard(boardModel, controller, moveLines);
    }

    /** PGN-string -> lijst met regels voor de movesList (oude fillMoves). */
    private List<String> buildMoveLines(String moveString) {
        List<String> items = new ArrayList<>();
        if (moveString == null) return items;

        String clean = PgnUtils.cleanMoveString(moveString);
        if (clean.isBlank()) {
            return items;
        }

        Pattern moveBlockPattern = Pattern.compile(
                "\\d+\\.(?:\\.\\.)?\\s.*?(?=\\d+\\.(?:\\.\\.)?\\s|$)"
        );
        Matcher matcher = moveBlockPattern.matcher(clean);

        int added = 0;
        while (matcher.find()) {
            String part = matcher.group().trim();
            if (!part.isEmpty()) {
                items.add(part);
                added++;
            }
        }

        if (added == 0) {
            items.add(clean);
        }

        return items;
    }

    /** Wordt door de view gebruikt als de Hint-knop wordt geklikt. */
    public String getHintText() {
        if (currentController == null) return "";
        String san = currentController.getExpectedSan();
        if (san != null) {
            return "Next: " + san;
        }
        return "Exercise finished.";
    }

    /** Flow voor back-knop: in board-mode terug naar lijst, anders hoofdstuk sluiten. */
    public void onBackPressed(boolean inBoardMode) {
        if (inBoardMode) {
            view.showExerciseList();
        } else {
            onBack.accept(null);
        }
    }

}