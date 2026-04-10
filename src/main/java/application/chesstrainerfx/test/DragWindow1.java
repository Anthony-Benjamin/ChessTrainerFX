package application.chesstrainerfx.test;

import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DragWindow1 extends VBox {
    VBox vBox;
    DragWindow1(){
        vBox = new VBox();
        Image image = new Image(getClass().getResourceAsStream("/images/BLACKROOK.png"));
        ImageView source = new ImageView(image);
        this.vBox.getChildren().add(source);

        source.setOnDragDetected(e -> {
            System.out.println("Drag detected in DragWindow1");
            Dragboard db = source.startDragAndDrop(TransferMode.MOVE);

//            SnapshotParameters params = new SnapshotParameters();
//            params.setFill(Color.TRANSPARENT);
//
//            WritableImage snapshot = source.snapshot(params, null);

//            ImageView iv = new ImageView(snapshot);
//            iv.setFitWidth(snapshot.getWidth() * 0.8);
//            iv.setFitHeight(snapshot.getHeight() * 0.8);
//            iv.setPreserveRatio(true);
//
//            WritableImage scaledImage = iv.snapshot(params, null);

            db.setDragView(source.getImage());

            ClipboardContent content = new ClipboardContent();
            content.putImage(image);
            db.setContent(content);
        });
    }

    public VBox getvBox1(){
        return this.vBox;
    }
}
