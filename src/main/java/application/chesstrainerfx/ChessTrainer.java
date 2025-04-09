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
        Board board = new Board();
//        board.printBoard();
        //board.printCoordinates();
        //BorderPane pane = new BorderPane();
        //pane.setCenter(board);

        Scene scene = new Scene(board, 800, 800);

        stage.setScene(scene);
        stage.show();

    }
}
