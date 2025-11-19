package application.chesstrainerfx.test;

import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.model.BoardModel;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ChapterSquareView extends StackPane {
    private final SquareModel model;
    private final Rectangle background;
    private final ImageView pieceImageView;

    public ChapterSquareView(BoardModel boardModel, SquareModel model, Controller400 controller) {
        this.model = model;

        // Laat de cel mooi passen in 400x400 → 8x8 → ~50x50
        setMinSize(0, 0);
        setPrefSize(50, 50);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Achtergrond rechthoek die meeschalend is met de cel
        background = new Rectangle();
        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());

        // Klikhandler blijft gelijk aan jouw oude API
        setOnMouseClicked(e -> controller.handleSquareClick(boardModel, this, model));

        // Init square-achtergrond obv coördinaat
        setSquareBackground();

        // Stukafbeelding die schaalt met de cel (90% van cel, behoud verhouding)
        pieceImageView = new ImageView();
        pieceImageView.setPreserveRatio(true);
        pieceImageView.setSmooth(true);
        pieceImageView.setCache(true);
        pieceImageView.fitWidthProperty().bind(widthProperty().multiply(0.9));
        pieceImageView.fitHeightProperty().bind(heightProperty().multiply(0.9));

        getChildren().addAll(background, pieceImageView);
        update();
    }

    private void setSquareBackground() {
        if (model != null && model.getPosition() != null) {
            int row = model.getPosition().getRow();
            int col = model.getPosition().getColumn();
            if ((row + col) % 2 == 0) {
                background.setFill(Color.WHITE);
            } else {
                background.setFill(Color.DEEPSKYBLUE);
            }
        } else {
            background.setFill(Color.LIGHTGRAY);
        }
    }

    public void update() {
        PieceModel piece = model.getPiece();
        if (piece != null) {
            // Behoud jouw bestandsnaam-conventie (bv. "WHITEQUEEN.png")
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

    public void setSelectedSource() {
        background.setFill(Color.GRAY);
    }

    public void setSelectedTarget() {
        background.setFill(Color.rgb(255, 205, 146));
    }

    public void removeSelection() {
        setSquareBackground();
    }
}
