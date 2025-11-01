package application.chesstrainerfx.test;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.view.BoardView;
import javafx.application.Application;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class ChessTrainer extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BoardModel model = new BoardModel();
        //model.initializeFromFEN("rnbqk1nr/ppp2ppp/3bp3/3p4/3P1B2/5N2/PPP1PPPP/RN1QKB1R w KQkq - 0 1");
        //model.initializeFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        model.initializeFromFEN("r2qkbnr/pP3ppp/4p3/8/2pp4/8/P1PPPPPP/RNBQKBNR w KQkq - 0 1");
        Controller controller = new Controller();

        BoardView boardView = new BoardView(model, controller,false, 800);
        boardView.setAlignment(Pos.CENTER);
        StackPane container = new StackPane();
        container.setMaxSize(600,600);
        container.getChildren().add(boardView);
        StackPane root = new StackPane(container);
        Scene scene = new Scene(root,1200,1000);

        primaryStage.setTitle("ChesstrainerFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
