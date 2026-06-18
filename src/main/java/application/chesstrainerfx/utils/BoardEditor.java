package application.chesstrainerfx.utils;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.view.BoardChangeListener;
import application.chesstrainerfx.view.SquareView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class BoardEditor extends HBox implements BoardChangeListener {

    private final int boardSize;
    private final BoardModel boardModel;
    private final Controller controller;
    private boolean isWhitePerspective;
    private final SquareView[][] squareViews = new SquareView[8][8];
    private GridPane boardGrid;

    public BoardEditor(BoardModel boardModel, Controller controller, boolean isWhitePerspective, int boardSize) {
        this.boardSize = boardSize;
        this.boardModel = boardModel;
        boardModel.addListener(this);
        this.controller = controller;
        this.isWhitePerspective = isWhitePerspective;
        this.setSpacing(20);
        this.setAlignment(Pos.CENTER_LEFT);
        StackPane boardWithBackground = createBoardStack(boardModel, controller, isWhitePerspective);

        this.getChildren().add(boardWithBackground);
    }

    private StackPane createBoardStack(BoardModel boardModel, Controller controller, boolean isWhitePerspective) {

        // --- bordraster ---
        boardGrid = new GridPane();
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
                Color.color(1, 1, 1, 0.12), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)
        )));

        // totale voorkeursmaten (bord + frame)
        double prefW = boardSize + 2 * frame;
        double prefH = boardSize + 2 * frame;
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
            rankBox.getChildren().add(lbl);
        }
        StackPane.setAlignment(rankBox, Pos.TOP_LEFT);
        StackPane.setMargin(rankBox, new Insets(frame, 0, 0, 0)); // onder vrijlaten voor files

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
            lbl.setStyle(String.format("-fx-text-fill: #F5DEB3; -fx-font-size: %.1fpx; -fx-font-weight: normal;", fontSize));
            lbl.setPrefSize(squareSize, frame);
            lbl.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            lbl.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            lbl.setAlignment(Pos.BOTTOM_CENTER);
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

    /** Markeert de opgegeven [row, col]-velden als onzeker (eerst alle bestaande marks wissen). */
    public void markLowConfidenceSquares(java.util.List<int[]> squares) {
        clearAllMarks();
        for (int[] sq : squares) {
            squareViews[sq[0]][sq[1]].markUncertain();
        }
    }

    /** Wist alle onzeker-markeringen op het bord. */
    public void clearAllMarks() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squareViews[row][col].clearMark();
            }
        }
    }

    /** Draait het bordperspectief om zonder het model aan te raken. */
    public void flip() {
        isWhitePerspective = !isWhitePerspective;
        getChildren().setAll(createBoardStack(boardModel, controller, isWhitePerspective));
    }

    /** Zet het perspectief op een vaste waarde (draait alleen wanneer nodig). */
    public void setWhitePerspective(boolean white) {
        if (white != isWhitePerspective) flip();
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
