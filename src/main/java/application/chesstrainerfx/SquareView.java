package application.chesstrainerfx;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SquareView extends StackPane {
    private SquareModel model;
    private Rectangle background;
    private ImageView pieceImageView;

    public SquareView(BoardModel boardModel, SquareModel model, Controller controller) {
        this.model = model;
        //this.setPrefSize(100,100);
        background = new Rectangle(100, 100);

        setOnMouseClicked(mouseEvent -> {
            System.out.println("Clicked! in View");
            controller.handleSquareClick(boardModel, this, model);
            //background.setFill(Color.GRAY);
        });

        setSquareBackground();
        pieceImageView = new ImageView();
        pieceImageView.setFitWidth(85);
        pieceImageView.setFitHeight(85);

        getChildren().addAll(background, pieceImageView);
        update();
    }

    public void setSquareBackground(){
        if (model != null && model.getPosition() != null) {
            int row = model.getPosition().getRow();
            int col = model.getPosition().getColumn();

            if ((row + col) % 2 == 0) {
                background.setFill(Color.WHITE);
            } else {
                background.setFill(Color.DEEPSKYBLUE);
            }
        }
    }

    public void update() {
        PieceModel piece = model.getPiece();


        if (piece != null) {
            String fileName = piece.getColor().toString() + piece.getType().toString() + ".png";
            try {
                Image image = new Image(getClass().getResource("/images/" + fileName).toExternalForm());
                pieceImageView.setImage(image);
            } catch (Exception e) {
                System.out.println("Kon afbeelding niet laden: " + fileName);
                pieceImageView.setImage(null);
            }
        } else {
            pieceImageView.setImage(null);
        }
    }

    public void setSeletedSource() {

        background.setFill(Color.GRAY);
    }

    public void setSelectedTarget() {
        background.setFill(Color.ORANGE);
    }


    public void removeSelection() {
        setSquareBackground();
    }
}   


