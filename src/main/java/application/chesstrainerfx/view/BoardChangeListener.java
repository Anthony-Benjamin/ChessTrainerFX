package application.chesstrainerfx.view;

public interface BoardChangeListener {
    void onBoardUpdated();
    default void onTurnChanged(boolean whiteToMove){} // nieuw
    default void onCheck(boolean inCheck){} // schaakmelding voor de partij die aan zet is
}
