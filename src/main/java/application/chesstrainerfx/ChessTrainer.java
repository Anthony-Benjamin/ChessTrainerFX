package application.chesstrainerfx;

import javafx.application.Application;

import javafx.stage.Stage;


public class ChessTrainer extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BoardModel model = new BoardModel();

        model.initializeFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");


        for (SquareModel square : model.getSquares()) {
            System.out.println(square.getPiece());

        }


    }
}
