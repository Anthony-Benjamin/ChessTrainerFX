package application.chesstrainerfx;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Square extends StackPane {
    private Rectangle background;
    int row;
    int col;
    Label label;

    public Square(int row, int col) {

        this.row = row;
        this.col = col;
        background = new Rectangle(100, 100);
        label = new Label();

        if ((row + col) % 2 == 0) {
            background.setFill(Color.BEIGE);
//            System.out.println("Beige");
        } else {
            background.setFill(Color.BROWN);
//            System.out.println("Brown");
        }
        getChildren().add(background);
    }

//    public void printCoordinate() {
//        int[] index = {row, col};
//
//        String coordinate = CoordinateSystem.indexToCoordinate(index);
//        label = new Label(coordinate);
//        background.setStroke(Color.YELLOW);
//        background.setStrokeWidth(5);
//        background.setWidth(95);
//        background.setHeight(95);
////        getChildren().add(label);
//    }

    public void setPiece(String path) {
        Image image = new Image(path);

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        getChildren().add(imageView);
    }
    public void setStroke(){
        background.setStroke(Color.YELLOW);
        background.setStrokeWidth(5);
        background.setWidth(95);
        background.setHeight(95);
    }

    public void resetSquare() {
        background.setStrokeWidth(0);
        background.setWidth(100);
        background.setHeight(100);
    }
}

