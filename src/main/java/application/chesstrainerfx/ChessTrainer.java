package application.chesstrainerfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.ArrayList;

public class ChessTrainer extends Application {
//    Board board;

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        BoardModel model = new BoardModel();
        ArrayList<Character> board = model.initializeFromFEN("");
        /*String s  ="prq3bnKy";
        for (char c: s.toCharArray()) {
            System.out.println(model.piecModelformFENChar(c));
        }*/
        model.getSquares().get(0).setPiece(new PieceModel(PieceType.KING,PieceColor.BLACK));
        model.getSquares().get(1).setPiece(new PieceModel(PieceType.BISHOP,PieceColor.WHITE));
        ArrayList<Character> charList = model.initializeFromFEN("");
        int counter = 0;

        for (SquareModel square: model.getSquares()){
            if (Character.isLetter(charList.get(counter))){
                square.setPiece(model.pieceModelformFENChar(charList.get(counter)));
            }


        }
        for (SquareModel square: model.getSquares()){
            if (Character.isLetter(charList.get(counter))){
                System.out.println(square.getPiece());
            }


        }
    }
}
