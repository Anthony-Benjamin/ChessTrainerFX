package application.chesstrainerfx.utils;

import java.util.List;

public class ExerciseSession {
    private List<List<Move>> solutionLines; // hoofdvariant + evt. alternatieven
    private int currentPly = 0;
    private List<Move> mainLine;

//    public ExerciseSession(List<List<Move>> solutionLines) {
//        this.solutionLines = solutionLines;
//    }

    public ExerciseSession(List<Move> mainLine){
        this.mainLine = mainLine;
    }

    public int getCurrentPly() { return currentPly; }
    public void advancePly() { currentPly++; }

    // later: methods als getExpectedMovesAtCurrentPly(), getHint(), etc.

    public boolean isCorrectMove(Move move) {
        // voorlopig: altijd true, zodat gedrag identiek blijft
        return true;
    }
}
