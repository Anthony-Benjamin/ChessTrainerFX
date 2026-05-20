package application.chesstrainerfx.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class DragDropApp extends Application {

    private Image oldtargetImage;

    @Override
    public void start(Stage stage) {

        // Source (draggable)
//        Label source = new Label("Drag me");
        Image image = new Image(getClass().getResourceAsStream("/images/BLACKROOK.png"));
        ImageView source = new ImageView(image);
        source.setStyle("-fx-border-color: black; -fx-padding: 20;");

        // Target (drop area)
        Image image2 = new Image(getClass().getResourceAsStream("/images/BLACKQUEEN.png"));
        ImageView target = new ImageView(image2);
        target.setStyle("-fx-border-color: blue; -fx-padding: 20;");

        // 🟢 Start drag
        source.setOnDragDetected(event -> {
            Dragboard db = source.startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            content.putImage(source.getImage());
            db.setContent(content);
        source.setImage(null);
            event.consume();
        });

        // 🟡 Allow drag over target
        target.setOnDragOver(event -> {
            if (event.getGestureSource() != target &&
                    event.getDragboard().hasImage()) {

                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        // 🔵 Handle drop
        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();

            boolean success = false;
            oldtargetImage = target.getImage();
            if (db.hasImage()) {
                target.setImage(db.getImage());
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });

        // 🔴 Cleanup after drag
        source.setOnDragDone(event -> {
            if (event.getTransferMode() == TransferMode.MOVE) {

                source.setImage(oldtargetImage);

            }
            event.consume();

        });

        // Layout
        HBox root = new HBox(50, source, target);
        root.setStyle("-fx-padding: 40;");

        Scene scene = new Scene(root, 1000, 400);
        stage.setTitle("JavaFX Drag & Drop Example");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
