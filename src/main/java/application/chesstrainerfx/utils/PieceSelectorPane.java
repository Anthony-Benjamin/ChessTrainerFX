package application.chesstrainerfx.utils;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
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


// White pieces
//        addPieceButton(grid, 0, 0, "WHITEPAWN.png", PieceType.PAWN, PieceColor.WHITE, listener);
//        addPieceButton(grid, 1, 0, "WHITEROOK.png", PieceType.ROOK, PieceColor.WHITE, listener);
//        addPieceButton(grid, 2, 0, "WHITEKNIGHT.png", PieceType.KNIGHT, PieceColor.WHITE, listener);
//        addPieceButton(grid, 3, 0, "WHITEBISHOP.png", PieceType.BISHOP, PieceColor.WHITE, listener);
//        addPieceButton(grid, 4, 0, "WHITEQUEEN.png", PieceType.QUEEN, PieceColor.WHITE, listener);
//        addPieceButton(grid, 5, 0, "WHITEKING.png", PieceType.KING, PieceColor.WHITE, listener);

//        // Black pieces
//        addPieceButton(grid, 0, 1, "BLACKPAWN.png", PieceType.PAWN, PieceColor.BLACK, listener);
//        addPieceButton(grid, 1, 1, "BLACKROOK.png", PieceType.ROOK, PieceColor.BLPieceSelectionListenerACK, listener);
//        addPieceButton(grid, 2, 1, "BLACKKNIGHT.png", PieceType.KNIGHT, PieceColor.BLACK, listener);
//        addPieceButton(grid, 3, 1, "BLACKBISHOP.png", PieceType.BISHOP, PieceColor.BLACK, listener);
//        addPieceButton(grid, 4, 1, "BLACKQUEEN.png", PieceType.QUEEN, PieceColor.BLACK, listener);
//        addPieceButton(grid, 5, 1, "BLACKKING.png", PieceType.KING, PieceColor.BLACK, listener);

        // Verwijderknop
        Button clearBtn = new Button("🗑 Verwijder");
        clearBtn.setOnAction(e -> listener.onPieceSelected(null));

        getChildren().addAll(grid, clearBtn);
    }

    private void addPieceButton(GridPane grid, int col, int row, String imageName, PieceType type, PieceColor color, PieceSelectionListener listener) {
//        Button btn = new Button(label);
//        btn.setMinSize(40, 40);
//        btn.setOnAction(e -> listener.onPieceSelected(new PieceModel(type, color)));
//        grid.add(btn, col, row);
        Image image = new Image(getClass().getResourceAsStream("/images/" + imageName));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setPreserveRatio(true);

        Button btn = new Button();

        btn.setMinSize(80, 80);
        btn.setGraphic(imageView);
        btn.setOnDragDetected(event -> {
            Dragboard db = btn.startDragAndDrop(TransferMode.MOVE);

            // Maak snapshot van node
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            WritableImage img = btn.snapshot(params, null);

            // 👉 schaal de afbeelding (bijv. 50%)
            ImageView iv = new ImageView(image);
            iv.setFitWidth(btn.getWidth() * 0.8);
            iv.setFitHeight(btn.getHeight() * 0.8);

            WritableImage scaledImage = iv.snapshot(params, null);

            db.setDragView(scaledImage);

            ClipboardContent content = new ClipboardContent();
            content.putString(btn.getText());
            db.setContent(content);

            event.consume();
        });
        btn.setOnAction(e -> listener.onPieceSelected(new PieceModel(type, color)));

        grid.add(btn, col, row);
    }

    public GridPane blackPieces(){
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);

        // Black pieces
        addPieceButton(grid, 0, 1, "BLACKPAWN.png", PieceType.PAWN, PieceColor.BLACK, listener);
        addPieceButton(grid, 1, 1, "BLACKROOK.png", PieceType.ROOK, PieceColor.BLACK, listener);
        addPieceButton(grid, 2, 1, "BLACKKNIGHT.png", PieceType.KNIGHT, PieceColor.BLACK, listener);
        addPieceButton(grid, 3, 1, "BLACKBISHOP.png", PieceType.BISHOP, PieceColor.BLACK, listener);
        addPieceButton(grid, 4, 1, "BLACKQUEEN.png", PieceType.QUEEN, PieceColor.BLACK, listener);
        addPieceButton(grid, 5, 1, "BLACKKING.png", PieceType.KING, PieceColor.BLACK, listener);

        return grid;
    }

    public GridPane whitePieces(){
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);

        addPieceButton(grid, 0, 0, "WHITEPAWN.png", PieceType.PAWN, PieceColor.WHITE, listener);
        addPieceButton(grid, 1, 0, "WHITEROOK.png", PieceType.ROOK, PieceColor.WHITE, listener);
        addPieceButton(grid, 2, 0, "WHITEKNIGHT.png", PieceType.KNIGHT, PieceColor.WHITE, listener);
        addPieceButton(grid, 3, 0, "WHITEBISHOP.png", PieceType.BISHOP, PieceColor.WHITE, listener);
        addPieceButton(grid, 4, 0, "WHITEQUEEN.png", PieceType.QUEEN, PieceColor.WHITE, listener);
        addPieceButton(grid, 5, 0, "WHITEKING.png", PieceType.KING, PieceColor.WHITE, listener);

        return grid;
    }
}
