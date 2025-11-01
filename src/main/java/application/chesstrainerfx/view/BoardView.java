package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.Position;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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

        StackPane boardWithFrame = createBoardStack(boardModel,controller,isWhitePerspective);
        getChildren().add(boardWithFrame);
    }

    private StackPane createBoardStack(BoardModel boardModel, Controller controller, boolean isWhitePerspective) {

        // --- bordraster ---
        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        double squareSize = boardSize / 8.0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int displayRow = isWhitePerspective ? row : 7 - row;
                int displayCol = isWhitePerspective ? col : 7 - col;

                Position pos = new Position(row, col);
                SquareModel squareModel = boardModel.getSquare(pos);
                SquareView sq = new SquareView(boardModel, squareModel, controller, (int) Math.round(squareSize));
                squareViews[row][col] = sq;
                boardGrid.add(sq, displayCol, displayRow);
            }
        }

        // --- frame ---
        double frame = squareSize / 3.0;
        Color frameColor = Color.web("#5A1F0E"); // mahonie

        StackPane framed = new StackPane(boardGrid);
        framed.setBackground(new Background(new BackgroundFill(frameColor, CornerRadii.EMPTY, Insets.EMPTY)));
        framed.setPadding(new Insets(frame)); // ruimte voor labels IN de rand
        framed.setBorder(new Border(new BorderStroke(
                Color.color(1,1,1,0.12), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)
        )));

        // totale voorkeursmaten (bord + frame)
        double prefW = boardSize + 2*frame;
        double prefH = boardSize + 2*frame;
        framed.setPrefSize(prefW, prefH);
        framed.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        framed.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // --- ranks LINKS (in de rand) ---
        VBox rankBox = new VBox(0);
        rankBox.setPickOnBounds(false);
        rankBox.setPrefWidth(frame);
        rankBox.setMinWidth(frame);
        rankBox.setMaxWidth(frame);

        for (int i = 0; i < 8; i++) {
            int rank = isWhitePerspective ? (8 - i) : (i + 1);
            Label lbl = new Label(String.valueOf(rank));

            double fontSize = squareSize * 0.15;
            lbl.setStyle(String.format(
                    "-fx-text-fill: #F5DEB3; -fx-font-size: %.1fpx; -fx-font-weight: normal;",
                    fontSize
            ));

            lbl.setPrefSize(frame, squareSize);
            lbl.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            lbl.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            lbl.setAlignment(Pos.CENTER);
           // lbl.setPadding(new Insets(frame , 0, 0, 0));
            rankBox.getChildren().add(lbl);
        }
        StackPane.setAlignment(rankBox, Pos.TOP_LEFT);
        StackPane.setMargin(rankBox, new Insets(frame , 0, 0, 0)); // onder vrijlaten voor files

        // --- files ONDER (in de rand) ---
        HBox fileBox = new HBox(0);
        fileBox.setPickOnBounds(false);
        fileBox.setPrefHeight(frame);
        fileBox.setMinHeight(frame);
        fileBox.setMaxHeight(frame);

        for (int i = 0; i < 8; i++) {
            char file = (char) ((isWhitePerspective ? 'a' + i : 'h' - i));
            Label lbl = new Label(String.valueOf(file));
            double fontSize = squareSize * 0.15;
            lbl.setStyle(String.format("-fx-text-fill: #F5DEB3; -fx-font-size: %.1fpx; -fx-font-weight: normal;",fontSize));
            lbl.setPrefSize(squareSize, frame);
            lbl.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            lbl.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            lbl.setAlignment(Pos.BOTTOM_CENTER);
           // lbl.setPadding(new Insets(0, 0, 6, 0));
            fileBox.getChildren().add(lbl);
        }
        StackPane.setAlignment(fileBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(fileBox, new Insets(0, 0, 0, frame)); // houd zij-frames zichtbaar

        // --- bouw één stapel: frame + labels ---
        StackPane boardStack = new StackPane(framed, rankBox, fileBox);
        boardStack.setAlignment(Pos.CENTER);
        boardStack.setPrefSize(prefW, prefH);
        boardStack.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        boardStack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // voorkom dat ouders het stretchen
        HBox.setHgrow(boardStack, Priority.NEVER);
        VBox.setVgrow(boardStack, Priority.NEVER);

        return boardStack;
    }





    /*private StackPane createBoardWithFrame() {
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
    }*/

    @Override
    public void onBoardUpdated() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squareViews[row][col].update();
            }
        }
    }
}
