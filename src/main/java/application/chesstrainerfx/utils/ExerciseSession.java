package application.chesstrainerfx.utils;

import java.util.List;

public class ExerciseSession {
    private final List<Move> mainLine;
    private int currentPly = 0;

    public ExerciseSession(List<Move> mainLine){
        this.mainLine = mainLine;
    }

    public int getCurrentPly() { return currentPly; }

    public boolean hasNext() {
        return currentPly < mainLine.size();
    }

    public Move getExpectedMove() {
        if (!hasNext()) return null;
        return mainLine.get(currentPly);
    }

    public boolean isCorrectMove(Move move) {
        Move expected = getExpectedMove();
        return expected != null && expected.equals(move);
    }

    public void advancePly() {
        currentPly++;
    }
}