package application.chesstrainerfx.utils;

import java.util.ArrayList;
import java.util.List;

public class ExerciseSession {

    private final List<VariationNode> rootOptions;
    private List<VariationNode> currentOptions;
    private final List<List<VariationNode>> history = new ArrayList<>();

    public ExerciseSession(List<VariationNode> rootOptions) {
        this.rootOptions = new ArrayList<>(rootOptions);
        this.currentOptions = new ArrayList<>(rootOptions);
    }

    public List<Move> getExpectedMoves() {
        List<Move> moves = new ArrayList<>();
        for (VariationNode node : currentOptions) {
            moves.add(node.getMove());
        }
        return moves;
    }

    public Move getExpectedMove() {
        return currentOptions.isEmpty() ? null : currentOptions.get(0).getMove();
    }

    public List<String> getExpectedSans() {
        List<String> sans = new ArrayList<>();
        for (VariationNode node : currentOptions) {
            sans.add(node.getSan());
        }
        return sans;
    }

    public String getExpectedSan() {
        return currentOptions.isEmpty() ? null : currentOptions.get(0).getSan();
    }

    public boolean isCorrectMove(Move userMove) {
        for (VariationNode node : currentOptions) {
            if (node.getMove().equals(userMove)) {
                return true;
            }
        }
        return false;
    }

    public void advanceWithMove(Move playedMove) {
        for (VariationNode node : currentOptions) {
            if (node.getMove().equals(playedMove)) {
                history.add(new ArrayList<>(currentOptions));
                currentOptions = new ArrayList<>(node.getNextMoves());
                return;
            }
        }
    }

    public void advancePly() {
        if (currentOptions.isEmpty()) {
            return;
        }
        history.add(new ArrayList<>(currentOptions));
        currentOptions = new ArrayList<>(currentOptions.get(0).getNextMoves());
    }

    public void rewindPly() {
        if (!history.isEmpty()) {
            currentOptions = history.remove(history.size() - 1);
        }
    }

    public boolean hasNext() {
        return !currentOptions.isEmpty();
    }

    public int getIndex() {
        return history.size();
    }

    public void reset() {
        history.clear();
        currentOptions = new ArrayList<>(rootOptions);
    }
    public boolean hasMultipleOptions() {
        return currentOptions.size() > 1;
    }
}