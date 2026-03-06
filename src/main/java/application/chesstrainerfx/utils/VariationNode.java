package application.chesstrainerfx.utils;

import java.util.ArrayList;
import java.util.List;

public class VariationNode {

    private final Move move;
    private final String san;
    private final List<VariationNode> nextMoves = new ArrayList<>();

    public VariationNode(Move move, String san) {
        this.move = move;
        this.san = san;
    }

    public Move getMove() {
        return move;
    }

    public String getSan() {
        return san;
    }

    public List<VariationNode> getNextMoves() {
        return nextMoves;
    }

    public void addNext(VariationNode child) {
        if (child != null) {
            nextMoves.add(child);
        }
    }
}