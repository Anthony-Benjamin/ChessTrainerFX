package application.chesstrainerfx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Chapters extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        HBox root = new HBox();
        Button[] tiles = new Button[5];

        root.setPadding(new Insets(10));
        root.setSpacing(10.0);

        for(int i = 0; i < tiles.length; i++) {
            Button tile = new Button();
            tile.setMinSize(100, 100);
            tile.setText("" + i);
            root.getChildren().add(tile);
        }

        Scene scene = new Scene(root, 450, 300);

        stage.setTitle("Chapters");
        stage.setScene(scene);
        stage.show();
    }
}
