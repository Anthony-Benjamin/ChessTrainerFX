package application.chesstrainerfx;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import java.net.URL;

public class BoardView extends HBox implements BoardChangeListener {

    private final BoardModel boardModel;
    private final SquareView[][] squareViews = new SquareView[8][8];
    private final Button setupBtn = new Button("Setup");


    private Controller controller;

    public BoardView(BoardModel boardModel, Controller controller, boolean isWhitePerspective) {
        this.boardModel = boardModel;
        this.controller = controller;
        this.setAlignment(Pos.CENTER);
        boardModel.addListener(this);
        setupBtn.setOnAction(event -> {
            controller.setSetupMode(true);

        });

        // Bordachtergrond + grid
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
        StackPane boardWithBackground = new StackPane();
        boardWithBackground.setPrefSize(865, 865);
        boardWithBackground.setBackground(new Background(bgImage));

        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int displayRow = isWhitePerspective ? row : 7 - row;
                int displayCol = isWhitePerspective ? col : 7 - col;
                Position pos = new Position(row, col);
                SquareModel squareModel = boardModel.getSquare(pos);
                SquareView squareView = new SquareView(boardModel, squareModel, controller);
                squareViews[row][col] = squareView;
                boardGrid.add(squareView, displayCol, displayRow);
            }
        }

        boardWithBackground.getChildren().add(boardGrid);
        this.getChildren().add(boardWithBackground);
        this.getChildren().add(setupBtn);

        // ➕ Voeg het selectie-paneel toe als setupMode actief is
        if (controller.isSetupMode()) {

            PieceSelectorPane pieceSelector = new PieceSelectorPane(selected -> {
                controller.setSelectedPieceForSetup(selected);
            });
            this.getChildren().add(pieceSelector);
        }
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
