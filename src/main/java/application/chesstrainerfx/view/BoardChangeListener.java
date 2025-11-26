package application.chesstrainerfx.view;

public interface BoardChangeListener {
    void onBoardUpdated();
    default void onTurnChanged(boolean whiteToMove) {} // nieuw
}
