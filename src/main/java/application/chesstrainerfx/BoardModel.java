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
    public void initializeFromFEN(String fen){
        fen = "rn1qK1nR/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        String[] splitArray = fen.split(" ");

//        System.out.println("FEN: " + splitArray[0]);
        String[] rows = splitArray[0].split("/");
        for (SquareModel squareModel:squares){

        }
        for (String row : rows) {
            PieceColor pieceColor;
            for (int i = 0; i < row.length(); i++) {
                System.out.println(" " + row.charAt(i));
                if(Character.isLetter(row.charAt(i)) && Character.isUpperCase(row.charAt(i))){
                    System.out.println(row.charAt(i) +
                            " White ");

                } else if (Character.isLetter(row.charAt(i)) && Character.isLowerCase(row.charAt(i))){
                    System.out.println(row.charAt(i) + " Black");
                }
            }



        }
    }
}
