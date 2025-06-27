package application.chesstrainerfx;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

public class BoardView extends StackPane implements BoardChangeListener {
    private final BoardModel boardModel;
    private final SquareView[][] squareViews = new SquareView[8][8];
    private Controller controller;

    public BoardView(BoardModel boardModel, Controller controller, boolean isWhitePerspective) {
        this.boardModel = boardModel;
        this.setAlignment(Pos.CENTER);
        this.controller = controller;
        // Achtergrondafbeelding instellen (bijv. PNG met coördinaten)
        String imagePath = isWhitePerspective ? "/images/chessboard_white.png" : "/images/chessboard_black.png";
        Image backgroundImage = new Image(getClass().getResource(imagePath).toExternalForm());
        BackgroundSize bgSize = new BackgroundSize(865, 865, false, false, false, false);
        BackgroundImage bgImage = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                bgSize
        );
        this.setBackground(new Background(bgImage));

        // Bord tekenen bovenop de achtergrond
        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);


        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int displayRow = isWhitePerspective ? row : 7 - row;
                int displayCol = isWhitePerspective ? col : 7 - col;
                Position pos = new Position(row, col);
                SquareModel squareModel = boardModel.getSquare(pos);
                SquareView squareView = new SquareView(boardModel,squareModel, controller);
                //squareView.setPrefSize(100, 100);
                squareViews[row][col] = squareView;
                boardGrid.add(squareView, displayCol, displayRow);
            }
        }
        //boardGrid.setMaxSize(800,800);
        this.getChildren().add(boardGrid);
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
