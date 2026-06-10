package application.chesstrainerfx.utils;

import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class PieceSelectorPane extends VBox {

    private final PieceSelectionListener listener;

    public interface PieceSelectionListener {
        void onPieceSelected(PieceModel piece);
    }

    public PieceSelectorPane(PieceSelectionListener listener) {
        setSpacing(10);
        setPadding(new Insets(10));

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        this.listener = listener;

        // Verwijderknop
        Button clearBtn = new Button("🗑 Verwijder");
        clearBtn.setOnAction(e -> listener.onPieceSelected(null));

        getChildren().addAll(grid, clearBtn);
    }

    private void addPieceLbl(GridPane grid, int col, int row, String imageName, PieceType type, PieceColor color) {
        Image image = new Image(getClass().getResourceAsStream("/images/" + imageName));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setPreserveRatio(true);

        Label lbl = new Label();

        lbl.setMinSize(80, 80);
        lbl.setGraphic(imageView);

        lbl.setOnDragDetected(event -> {
            Dragboard db = lbl.startDragAndDrop(TransferMode.MOVE);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            // geschaalde drag-afbeelding
            ImageView iv = new ImageView(image);
            iv.setFitWidth(lbl.getWidth() * 0.8);
            iv.setFitHeight(lbl.getHeight() * 0.8);

            WritableImage scaledImage = iv.snapshot(params, null);

            db.setDragView(scaledImage, 30, 30);

            ClipboardContent content = new ClipboardContent();
            content.putImage(imageView.getImage());
            db.setContent(content);

            DragContext.draggedPiece = new PieceModel(type, color);

            event.consume();
        });

        grid.add(lbl, col, row);
    }

    public GridPane blackPieces(){
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);

        // Black pieces
        addPieceLbl(grid, 0, 1, "BLACKPAWN.png", PieceType.PAWN, PieceColor.BLACK);
        addPieceLbl(grid, 1, 1, "BLACKROOK.png", PieceType.ROOK, PieceColor.BLACK);
        addPieceLbl(grid, 2, 1, "BLACKKNIGHT.png", PieceType.KNIGHT, PieceColor.BLACK);
        addPieceLbl(grid, 3, 1, "BLACKBISHOP.png", PieceType.BISHOP, PieceColor.BLACK);
        addPieceLbl(grid, 4, 1, "BLACKQUEEN.png", PieceType.QUEEN, PieceColor.BLACK);
        addPieceLbl(grid, 5, 1, "BLACKKING.png", PieceType.KING, PieceColor.BLACK);

        return grid;
    }

    public GridPane whitePieces(){
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);

        addPieceLbl(grid, 0, 0, "WHITEPAWN.png", PieceType.PAWN, PieceColor.WHITE);
        addPieceLbl(grid, 1, 0, "WHITEROOK.png", PieceType.ROOK, PieceColor.WHITE);
        addPieceLbl(grid, 2, 0, "WHITEKNIGHT.png", PieceType.KNIGHT, PieceColor.WHITE);
        addPieceLbl(grid, 3, 0, "WHITEBISHOP.png", PieceType.BISHOP, PieceColor.WHITE);
        addPieceLbl(grid, 4, 0, "WHITEQUEEN.png", PieceType.QUEEN, PieceColor.WHITE);
        addPieceLbl(grid, 5, 0, "WHITEKING.png", PieceType.KING, PieceColor.WHITE);

        return grid;
    }
}
