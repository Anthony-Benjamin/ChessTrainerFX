package application.chesstrainerfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ChessTrainer extends Application {
//    Board board;

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        BoardModel model = new BoardModel();
        model.initializeFromFEN("");
        /*String s  ="prq3bnKy";
        for (char c: s.toCharArray()) {
            System.out.println(model.piecModelformFENChar(c));
        }*/

    }
}
