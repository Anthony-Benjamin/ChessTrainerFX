package application.chesstrainerfx.test;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class DragWindow2 extends VBox {
    DragWindow2(){
        DragWindow3 dragWindow3 = new DragWindow3();

        this.getChildren().add(dragWindow3.getBox3());
    }
}
