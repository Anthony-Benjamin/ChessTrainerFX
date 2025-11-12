package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.Position;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class BoardView extends HBox implements BoardChangeListener {

    private final BoardModel boardModel;
    private final SquareView[][] squareViews = new SquareView[8][8];
    private final boolean isWhitePerspective;
    private final Controller controller;
    private final int boardSize;          // totale breedte/hoogte van het speelvlak (8×8)
    private final double squareSize;      // grootte van één veld
    private final double frameThickness;  // dikte van de rand = 1/3 van square

    public BoardView(BoardModel boardModel, Controller controller, boolean isWhitePerspective, int boardSize) {
        this.boardModel = boardModel;
        this.controller = controller;
        this.isWhitePerspective = isWhitePerspective;
        this.boardSize = boardSize;

        this.squareSize = boardSize / 8.0;
        this.frameThickness = squareSize / 3.0;

        setSpacing(20);
        setAlignment(Pos.CENTER_LEFT);

        boardModel.addListener(this);

        StackPane boardWithFrame = createBoardWithFrame();
        getChildren().add(boardWithFrame);
    }

    private StackPane createBoardWithFrame() {
        // === 8×8 grid met jouw SquareView ===
        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(0);
        boardGrid.setVgap(0);
        boardGrid.setPadding(Insets.EMPTY);
        boardGrid.setPrefSize(boardSize, boardSize);
        boardGrid.setMinSize(boardSize, boardSize);
        boardGrid.setMaxSize(boardSize, boardSize);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int displayRow = isWhitePerspective ? row : 7 - row;
                int displayCol = isWhitePerspective ? col : 7 - col;
                Position pos = new Position(row, col);
                SquareModel squareModel = boardModel.getSquare(pos);
                SquareView squareView = new SquareView(boardModel, squareModel, controller, (int) Math.round(squareSize));
                squareViews[row][col] = squareView;
                boardGrid.add(squareView, displayCol, displayRow);
            }
        }

        // === Houten frame rondom het bord ===
        // Warme houtkleur met rode ondertoon (matcht jouw thema)
        Color frameColor = Color.web("#5A1F0E");

        StackPane frame = new StackPane(boardGrid);
        frame.setBackground(new Background(new BackgroundFill(frameColor, CornerRadii.EMPTY, Insets.EMPTY)));
        frame.setPadding(new Insets(frameThickness));

        // Een subtiele schaduw voor diepte
        DropShadow shadow = new DropShadow();
        shadow.setRadius(12);
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        shadow.setColor(Color.color(0, 0, 0, 0.40));
        frame.setEffect(shadow);

        // Totale afmetingen inclusief frame
        double total = boardSize + frameThickness * 2;
        frame.setPrefSize(total, total);
        frame.setMinSize(total, total);
        frame.setMaxSize(total, total);

        // Buitenste container (handig als je later nog overlays wilt)
        StackPane boardWithFrame = new StackPane(frame);
        boardWithFrame.setAlignment(Pos.CENTER);
        boardWithFrame.setPickOnBounds(false);
        return boardWithFrame;
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
