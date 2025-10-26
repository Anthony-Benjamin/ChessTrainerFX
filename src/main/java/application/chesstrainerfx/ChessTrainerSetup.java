package application.chesstrainerfx;

import application.chesstrainerfx.view.BoardView;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ChessTrainerSetup extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        BoardModel model = new BoardModel();
        model.initializeFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        Controller controller = new Controller();
        BoardViewSetup boardView = new BoardViewSetup(model, controller,true,400);
        boardView.setAlignment(Pos.TOP_LEFT);
        StackPane container = new StackPane();
        container.getChildren().add(boardView);
        StackPane root = new StackPane(container);
        Scene scene = new Scene(root,1000,600);

        stage.setTitle("ChesstrainerFX");
        stage.setScene(scene);
        stage.show();
    }
}
