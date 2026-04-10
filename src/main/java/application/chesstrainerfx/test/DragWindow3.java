package application.chesstrainerfx.test;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class DragWindow3 {


    private final VBox vBox;

    DragWindow3(){
        vBox = new VBox();
        Image image = new Image(getClass().getResourceAsStream("/images/BLACKROOK.png"));
        ImageView source = new ImageView(image);

        this.vBox.getChildren().add(source);

//        source.setOnDragDetected(e -> {
//            System.out.println("Drag detected!");
//        });
        source.setOnDragOver(e -> {
            System.out.println("Drag over in DragWindow2!");
        });
    }

    public VBox getBox3(){
        return vBox;
    }
}
