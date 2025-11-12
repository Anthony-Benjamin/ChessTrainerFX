package application.chesstrainerfx.view;

import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

public class SquareView extends StackPane {
    private final SquareModel model;
    private final Rectangle background;
    private final Rectangle highlight;   // semi-transparante overlay voor selectie
    private final ImageView pieceImageView;

    /* --- Thema-kleuren (warm, passend bij jouw achtergrond) --- */
    private static final Color LIGHT_SQUARE = Color.web("#D7B77E");  // goudbeige
    private static final Color DARK_SQUARE  = Color.web("#6B2E1A");  // mahoni
    private static final Color BORDER_COLOR = Color.web("#3B1E0C");  // subtiele rand

    // Selection overlays (semi-transparant, schijnt mooi over de bordkleur heen)
    private static final Color SELECTION_SOURCE = Color.color(1.0, 0.84, 0.55, 0.55); // warm licht
    private static final Color SELECTION_TARGET = Color.color(0.95, 0.55, 0.25, 0.55); // warm oranje
    private static final Color SELECTION_NONE   = Color.TRANSPARENT;
    private Label lbl;

    public SquareView(BoardModel boardModel, SquareModel model, Controller controller, int size) {
        this.model = model;

        background = new Rectangle(size, size);
        background.setStroke(BORDER_COLOR);
        background.setStrokeWidth(0.6);

        // overlay voor selectie/hover etc.
        highlight = new Rectangle(size, size);
        highlight.setFill(SELECTION_NONE);

        setOnMouseClicked(e -> controller.handleSquareClick(boardModel, this, model));

        setSquareBackground();

        pieceImageView = new ImageView();
        pieceImageView.setFitWidth(size * 0.92);
        pieceImageView.setFitHeight(size * 0.92);
        pieceImageView.setPreserveRatio(true);



        getChildren().addAll(background, lbl,highlight, pieceImageView);
        update();
    }

    /** Zet de bordkleur op basis van vak-coördinaten. */
    public void setSquareBackground() {
        if (model != null && model.getPosition() != null) {
            int row = model.getPosition().getRow();
            int col = model.getPosition().getColumn();
            lbl = new Label();
            lbl.setFont(Font.font(15));
            lbl.setPadding(new Insets(50, 60,0,0));
            if(row == 7){
                if( col== 0){
                    lbl.setText("a");
                    lbl.setStyle("-fx-text-fill:white");
                }
            }
            boolean light = ((row + col) % 2 == 0);
            background.setFill(light ? LIGHT_SQUARE : DARK_SQUARE);
        }
    }

    /** Laadt het stuk-icoon of wist het. */
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

    /** Bronveld (bijv. aangeklikte eigen stuk). */
    public void setSelectedSource() {
        highlight.setFill(SELECTION_SOURCE);
    }

    /** Doelveld (legale zet-doel). */
    public void setSelectedTarget() {
        highlight.setFill(SELECTION_TARGET);
    }

    /** Reset selectie/hover. */
    public void removeSelection() {
        highlight.setFill(SELECTION_NONE);
        // basisbordkleur blijft intact
    }
}
