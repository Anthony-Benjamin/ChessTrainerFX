package application.chesstrainerfx.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DragAndDropV2 extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        VBox dragWindow1 = new DragWindow1().getvBox1();
        DragWindow2 dragWindow2 = new DragWindow2();
        DragWindow3 dragWindow3 = new DragWindow3();

        HBox root = new HBox();
        root.getChildren().addAll(dragWindow1, dragWindow2);

        stage.setScene(new Scene(root, 1000, 400));
        stage.show();
    }
}
