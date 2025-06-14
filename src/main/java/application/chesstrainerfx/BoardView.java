package application.chesstrainerfx;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class BoardView extends GridPane implements BoardChangeListener{
    private BoardModel boardModel;
    private final SquareView[][] squareViews = new SquareView[8][8];
    private final String[] cols = {"a","b","c","d","e","f","g","h"};

    public BoardView(BoardModel boardModel) {
        this.boardModel = boardModel;
        drawBoard();
        this.setAlignment(Pos.CENTER);

    }

    public void drawBoard() {
        this.getChildren().clear();
        // Rij-labels (1–8) aan linker- en rechterzijde
        for (int row = 0; row < 8; row++) {
            Label leftLabel = new Label("" + (8 - row));
            leftLabel.setMinSize(20, 100);
            leftLabel.setStyle("-fx-alignment: center; -fx-font-size: 18;");
            this.add(leftLabel, 0, row + 1);

            Label rightLabel = new Label("" + (8 - row));
            rightLabel.setMinSize(20, 100);
            rightLabel.setStyle("-fx-alignment: center; -fx-font-size: 18;");
            this.add(rightLabel, 9, row + 1);
        }

        // Kolom-labels (a–h) boven en onder
        for (int col = 0; col < 8; col++) {
            Label topLabel = new Label(cols[col]);
            topLabel.setMinSize(100, 20);
            topLabel.setStyle("-fx-alignment: center; -fx-font-size: 18;");
            this.add(topLabel, col + 1, 0);

            Label bottomLabel = new Label(cols[col]);
            bottomLabel.setMinSize(100, 20);
            bottomLabel.setStyle("-fx-alignment: center; -fx-font-size: 18;");
            this.add(bottomLabel, col + 1, 9);
        }
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);
                SquareModel squareModel = boardModel.getSquare(pos);
                SquareView squareView = new SquareView(squareModel);
                squareView.setPrefSize(100, 100);
                this.add(squareView, col+1, row+1);
            }
        }



    }
    public GridPane createInnerBoard(BoardModel model) {
        GridPane innerBoard = new GridPane();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);
                SquareModel squareModel = model.getSquare(pos);
                SquareView squareView = new SquareView(squareModel);
                squareView.setPrefSize(100, 100);
                innerBoard.add(squareView, col, row);
            }
        }

        return innerBoard;
    }

    @Override
    public void onBoardUpdated() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squareViews[row][col].update();
            }
        }

    }
}
