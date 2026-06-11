package application.chesstrainerfx.view;

import application.chesstrainerfx.utils.DragContext;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SquareView extends StackPane {
    private final SquareModel model;
    private final BoardModel boardModel;
    private final Controller controller;
    private final int size;
    private final Rectangle background;
    private final Rectangle highlight;   // semi-transparante overlay voor selectie
    private final ImageView pieceImageView;

    /* --- Thema-kleuren (warm, passend bij de achtergrond) --- */
    private static final Color LIGHT_SQUARE = Color.web("#D7B77E");  // goudbeige
    private static final Color DARK_SQUARE  = Color.web("#6B2E1A");  // mahonie
    private static final Color BORDER_COLOR = Color.web("#3B1E0C");  // subtiele rand

    // Selection overlays (semi-transparant, schijnt over de bordkleur heen)
    private static final Color SELECTION_SOURCE = Color.web("#8a5216");
    private static final Color SELECTION_TARGET = Color.color(0.95, 0.55, 0.25, 0.55); // warm oranje
    private static final Color SELECTION_NONE   = Color.TRANSPARENT;

    public SquareView(BoardModel boardModel, SquareModel model, Controller controller, int size) {
        this.model = model;
        this.boardModel = boardModel;
        this.controller = controller;
        this.size = size;

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

        this.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasImage()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        this.setOnDragDetected(event -> {
            if (model.getPiece() == null) {
                event.consume();
                return;
            }
            Dragboard db = pieceImageView.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putImage(pieceImageView.getImage());
            db.setContent(content);

            Image dragView = scaledDragImage();
            if (dragView != null) {
                db.setDragView(dragView, dragView.getWidth() / 2, dragView.getHeight() / 2);
            }

            if (controller.isEditorMode()) {
                // Vrije plaatsing: onthoud het gesleepte stuk voor de drop.
                DragContext.draggedPiece = model.getPiece();
            } else {
                // Speelbord: selecteer het bronveld via de Controller (zoals klik-klik).
                controller.handleSquareClick(boardModel, this, model);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (controller.isEditorMode()) {
                if (db.hasImage()) {
                    model.setPiece(DragContext.draggedPiece);
                    update();
                }
                event.setDropCompleted(true);
            } else {
                // Speelbord: voer de zet uit via de Controller (validatie, tegenzet, history).
                controller.handleSquareClick(boardModel, this, model);
                // false → dragDone wist het bronveld NIET; de Controller heeft het stuk al verplaatst.
                event.setDropCompleted(false);
            }
            event.consume();
        });

        this.setOnDragDone(event -> {
            // Alleen in editor-mode het bronstuk weghalen na een geslaagde MOVE.
            if (controller.isEditorMode() && event.getTransferMode() == TransferMode.MOVE) {
                this.model.removePiece();
                update();
            }
            event.consume();
        });

        getChildren().addAll(background, highlight, pieceImageView);
        update();
    }

    /** Zet de bordkleur op basis van vak-coördinaten. */
    public void setSquareBackground() {
        if (model == null || model.getPosition() == null) return;

        int row = model.getPosition().getRow();
        int col = model.getPosition().getColumn();

        boolean light = ((row + col) % 2 == 0);
        background.setFill(light ? LIGHT_SQUARE : DARK_SQUARE);
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
                pieceImageView.setImage(null);
            }
        } else {
            pieceImageView.setImage(null);
        }
    }

    /** Geschaalde, transparante drag-afbeelding ter grootte van een vak (i.p.v. de rauwe PNG). */
    private Image scaledDragImage() {
        Image image = pieceImageView.getImage();
        if (image == null) return null;

        ImageView iv = new ImageView(image);
        iv.setFitWidth(size * 0.92);
        iv.setFitHeight(size * 0.92);
        iv.setPreserveRatio(true);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return iv.snapshot(params, null);
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
    }
}
