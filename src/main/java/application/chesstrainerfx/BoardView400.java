package application.chesstrainerfx;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

public class BoardView400 extends StackPane implements BoardChangeListener {

    private final BoardModel boardModel;
    private final ChapterSquareView[][] squareViews = new ChapterSquareView[8][8];
    private final boolean isWhitePerspective;
    private final Controller400 controller;

    // Doelgrootte voor compact bord
    private static final double BOARD_SIZE = 400;

    public BoardView400(BoardModel boardModel, Controller400 controller, boolean isWhitePerspective) {
        this.boardModel = boardModel;
        this.controller = controller;
        this.isWhitePerspective = isWhitePerspective;

        setAlignment(Pos.CENTER);
        setPrefSize(BOARD_SIZE, BOARD_SIZE);
        setMinSize(BOARD_SIZE, BOARD_SIZE);
        setMaxSize(BOARD_SIZE, BOARD_SIZE);

        boardModel.addListener(this);

        StackPane boardWithBackground = createBoardStack(boardModel, controller, isWhitePerspective);
        getChildren().add(boardWithBackground);
    }

    public BoardView400() {
        boardModel = null;
        isWhitePerspective = false;
        controller = null;
    }

    private StackPane createBoardStack(BoardModel boardModel, Controller400 controller, boolean isWhitePerspective) {
        String imagePath = isWhitePerspective ? "/images/chessboard_white.png" : "/images/chessboard_black.png";
        Image backgroundImage = new Image(getClass().getResource(imagePath).toExternalForm());

        // Achtergrond exact 400x400 laten tekenen
        BackgroundSize bgSize = new BackgroundSize(BOARD_SIZE, BOARD_SIZE, false, false, false, false);
        BackgroundImage bgImage = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                bgSize
        );

        StackPane boardWithBackground = new StackPane();
        boardWithBackground.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        boardWithBackground.setMinSize(BOARD_SIZE, BOARD_SIZE);
        boardWithBackground.setMaxSize(BOARD_SIZE, BOARD_SIZE);
        boardWithBackground.setBackground(new Background(bgImage));

        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        // Zorg dat het grid binnen 400x400 blijft
        boardGrid.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        boardGrid.setMinSize(BOARD_SIZE, BOARD_SIZE);
        boardGrid.setMaxSize(BOARD_SIZE, BOARD_SIZE);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int displayRow = isWhitePerspective ? row : 7 - row;
                int displayCol = isWhitePerspective ? col : 7 - col;
                Position pos = new Position(row, col);
                SquareModel squareModel = boardModel.getSquare(pos);
                ChapterSquareView squareView = new ChapterSquareView(boardModel, squareModel, controller);

                // Optioneel: cellen laten mee-schalen binnen 400x400
                squareView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                GridPane.setFillWidth(squareView, true);
                GridPane.setFillHeight(squareView, true);

                squareViews[row][col] = squareView;
                boardGrid.add(squareView, displayCol, displayRow);
            }
        }

        // Acht gelijke kolommen/rijen zodat cellen mooi verdelen over 400x400
        for (int i = 0; i < 8; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 8.0);
            boardGrid.getColumnConstraints().add(cc);

            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / 8.0);
            boardGrid.getRowConstraints().add(rc);
        }

        boardWithBackground.getChildren().add(boardGrid);
        return boardWithBackground;
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
