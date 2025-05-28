package application.chesstrainerfx;

import java.util.ArrayList;
import java.util.List;

public class BoardModel {
    private List<SquareModel> squares;

    public BoardModel(){
        squares = new ArrayList<>(64);
    }

    public void initializeBoard(){
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                squares.add(new SquareModel(new Position(row, col)));
            }
        }
    }
}
