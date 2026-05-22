package application.chesstrainerfx.view;

import application.chesstrainerfx.utils.DragContext;
import application.chesstrainerfx.utils.CoordinateSystem;
import application.chesstrainerfx.utils.PieceColor;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.PieceType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
//    private static final Color SELECTION_SOURCE = Color.color(1.0, 0.84, 0.55, 0.55); // warm licht
    private static final Color SELECTION_SOURCE = Color.web("#8a5216");
    private static final Color SELECTION_TARGET = Color.color(0.95, 0.55, 0.25, 0.55); // warm oranje
    private static final Color SELECTION_NONE   = Color.TRANSPARENT;

    private final Label fileLabel = new Label();
    private final Label rankLabel = new Label();

    public SquareView(BoardModel boardModel, SquareModel model, Controller controller, int size) {
        this.model = model;

        background = new Rectangle(size, size);
        background.setStroke(BORDER_COLOR);
        background.setStrokeWidth(0.6);
        fileLabel.setFont(Font.font(13));
        fileLabel.setPadding(new Insets(0, 0, 0, 5));
        fileLabel.setMouseTransparent(true);
        rankLabel.setFont(Font.font(13));
        rankLabel.setPadding(new Insets(5, 0, 0, 0));
        rankLabel.setMouseTransparent(true);

        StackPane.setAlignment(fileLabel, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(rankLabel, Pos.TOP_RIGHT);
        // overlay voor selectie/hover etc.
        highlight = new Rectangle(size, size);
        highlight.setFill(SELECTION_NONE);

        setOnMouseClicked(e -> controller.handleSquareClick(boardModel, this, model));

        setSquareBackground();

        pieceImageView = new ImageView();
        pieceImageView.setFitWidth(size * 0.92);
        pieceImageView.setFitHeight(size * 0.92);
        pieceImageView.setPreserveRatio(true);


        this.setOnDragOver(event ->{
            System.out.println("Set on Drag Over");
           if (event.getGestureSource() != this && event.getDragboard().hasImage()) {
               System.out.println("Event.getGestureSource " + event.getGestureSource());
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
    }
    event.consume();
        });

        this.setOnDragDropped(event ->{
            System.out.println("Set on Drag Dropped");
            Dragboard db = event.getDragboard();
//            System.out.println("x=" + event.getX() + "y=" + event.getY());
            if(db.hasImage()){
                System.out.println("db has image");
                //this.pieceImageView.setImage(db.getImage());
                model.setPiece(DragContext.draggedPiece);
                update();
            }
            event.setDropCompleted(true);
            event.consume();
        });


        this.setOnDragDetected(event -> {
            System.out.println("Dragged detected on: " + (SquareView) event.getSource());
            Dragboard db = pieceImageView.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putImage(pieceImageView.getImage());
            DragContext.draggedPiece = model.getPiece();
            db.setContent(content);
            event.consume();
        });

        this.setOnDragDone(event -> {
            if (event.getTransferMode() == TransferMode.MOVE) {
                // Verwijder de source hier
//                this.setVisible(false); // of uit lijst verwijderen
                this.model.removePiece();
                update();
            }
            event.consume();
        });

        getChildren().addAll(background /*fileLabel, rankLabel*/, highlight, pieceImageView);
        update();
    }

    /** Zet de bordkleur op basis van vak-coördinaten. */
    public void setSquareBackground() {
        if (model == null || model.getPosition() == null) return;

        int row = model.getPosition().getRow();
        int col = model.getPosition().getColumn();

        boolean light = ((row + col) % 2 == 0);
        background.setFill(light ? LIGHT_SQUARE : DARK_SQUARE);

        // Alleen op onderste rij de file-letters tonen
        if (row == 7) {
            char file = (char) ('a' + col);
            fileLabel.setText(String.valueOf(file));
            // Contrasterende tekstkleur: op donker veld licht, op licht veld donker
            fileLabel.setStyle(light
                    ? "-fx-text-fill: #6B2E1A;"  // op licht veld: donkere tekst
                    : "-fx-text-fill: #D7B77E;"); // op donker veld: lichte tekst
            fileLabel.setVisible(true);
        }
        if(col==7){
            int rank = 8 - row;
            rankLabel.setText(String.valueOf(rank));
            rankLabel.setStyle(light
                    ? "-fx-text-fill: #6B2E1A;"  // op licht veld: donkere tekst
                    : "-fx-text-fill: #D7B77E;"); // op donker veld: lichte tekst;
            rankLabel.setVisible(true);
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
