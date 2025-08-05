package application.chesstrainerfx;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class PieceSelectorPane extends VBox {

    public interface PieceSelectionListener {
        void onPieceSelected(PieceModel piece);
    }

    public PieceSelectorPane(PieceSelectionListener listener) {
        setSpacing(10);
        setPadding(new Insets(10));

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);

        // Wit
        addPieceButton(grid, 0, 0, "♙", PieceType.PAWN, PieceColor.WHITE, listener);
        addPieceButton(grid, 1, 0, "♖", PieceType.ROOK, PieceColor.WHITE, listener);
        addPieceButton(grid, 2, 0, "♘", PieceType.KNIGHT, PieceColor.WHITE, listener);
        addPieceButton(grid, 3, 0, "♗", PieceType.BISHOP, PieceColor.WHITE, listener);
        addPieceButton(grid, 4, 0, "♕", PieceType.QUEEN, PieceColor.WHITE, listener);
        addPieceButton(grid, 5, 0, "♔", PieceType.KING, PieceColor.WHITE, listener);

        // Zwart
        addPieceButton(grid, 0, 1, "♟", PieceType.PAWN, PieceColor.BLACK, listener);
        addPieceButton(grid, 1, 1, "♜", PieceType.ROOK, PieceColor.BLACK, listener);
        addPieceButton(grid, 2, 1, "♞", PieceType.KNIGHT, PieceColor.BLACK, listener);
        addPieceButton(grid, 3, 1, "♝", PieceType.BISHOP, PieceColor.BLACK, listener);
        addPieceButton(grid, 4, 1, "♛", PieceType.QUEEN, PieceColor.BLACK, listener);
        addPieceButton(grid, 5, 1, "♚", PieceType.KING, PieceColor.BLACK, listener);

        // Verwijderknop
        Button clearBtn = new Button("🗑 Verwijder");
        clearBtn.setOnAction(e -> listener.onPieceSelected(null));

        getChildren().addAll(grid, clearBtn);
    }

    private void addPieceButton(GridPane grid, int col, int row, String label, PieceType type, PieceColor color, PieceSelectionListener listener) {
        Button btn = new Button(label);
        btn.setMinSize(40, 40);
        btn.setOnAction(e -> listener.onPieceSelected(new PieceModel(type, color)));
        grid.add(btn, col, row);
    }
}
