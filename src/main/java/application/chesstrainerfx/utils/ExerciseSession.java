package application.chesstrainerfx.utils;

import java.util.List;

public class ExerciseSession {

    private final List<Move> moves;
    private final List<String> sanMoves;
    private int index = 0;

    public ExerciseSession(List<Move> moves, List<String> sanMoves) {
        this.moves = moves;
        this.sanMoves = sanMoves;
    }

    public Move getExpectedMove() {
        if (index >= moves.size()) return null;
        return moves.get(index);
    }

    public String getExpectedSan() {
        if (index >= sanMoves.size()) return null;
        return sanMoves.get(index);
    }

    public boolean isCorrectMove(Move userMove) {
        Move expected = getExpectedMove();
        if (expected == null) return false;
        return expected.equals(userMove);
    }

    public void advancePly() {
        index++;
    }

    public void rewindPly() {
        if (index > 0) {
            index--;
        }
    }

    public boolean hasNext() {
        return index < moves.size();
    }

    public int getIndex() {
        return index;
    }
}
