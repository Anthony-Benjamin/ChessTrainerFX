package application.chesstrainerfx.test;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RightSideWindow extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        int widthButtonSize = 150;

        HBox startAndClearBox = new HBox(5);
        startAndClearBox.setAlignment(Pos.CENTER);
        Button startBtn = new Button("Start Position");
        Button clearBoardBtn = new Button("Clear Board");
        startBtn.setMinWidth(widthButtonSize);
        clearBoardBtn.setMinWidth(widthButtonSize);
        startAndClearBox.setSpacing(10);
//        startAndClearBox.setPadding(new Insets(5));
        startAndClearBox.getChildren().addAll(startBtn, clearBoardBtn);

        HBox flipBoardBox = new HBox();
        flipBoardBox.setAlignment(Pos.CENTER);
        Button flipBoardBtn = new Button("Flip Board");
        Button importFENBtn = new Button("Export FEN");
        flipBoardBtn.setMinWidth(widthButtonSize);
        importFENBtn.setMinWidth(widthButtonSize);
        flipBoardBox.setSpacing(10);
        flipBoardBox.getChildren().addAll(flipBoardBtn, importFENBtn);

        // Move window
        VBox movesWindowLayout = new VBox();
        Label movesTitlelbl = new Label("Moves");
        movesTitlelbl.setStyle("-fx-font-weight: bold;");
        TextArea movesWindow = new TextArea();
        movesWindow.setMaxWidth(150);
        movesWindow.setMaxHeight(150);
        movesWindowLayout.setAlignment(Pos.CENTER);
        movesWindowLayout.getChildren().addAll(movesTitlelbl, movesWindow);
        // end Move window

        HBox isWhiteTurnBox = new HBox();
        isWhiteTurnBox.setAlignment(Pos.CENTER);
        ComboBox<String> whoseTurnSelector = new ComboBox<>();
        whoseTurnSelector.getItems().addAll("White to move", "Black to move");
        whoseTurnSelector.setValue("White to move");
        isWhiteTurnBox.getChildren().add(whoseTurnSelector);


        VBox castlingBox = new VBox();
        HBox castlingBoxTitle = new HBox();
        castlingBoxTitle.setAlignment(Pos.CENTER);
        Label title = new Label("Castling");
        title.setStyle("-fx-font-weight: bold;");
        castlingBoxTitle.getChildren().add(title);
        castlingBox.getChildren().add(castlingBoxTitle);

        HBox whiteCheckBox = new HBox();
        whiteCheckBox.setSpacing(4);
        Label white = new Label("White");
        CheckBox kingSideCastling = new CheckBox();
        Label lblKingSide = new Label("O-O");;
        CheckBox queenSideCastling = new CheckBox();
        Label lblQueenSide = new Label("O-O-O");;
        whiteCheckBox.getChildren().addAll(white, kingSideCastling, lblKingSide, queenSideCastling, lblQueenSide);
        whiteCheckBox.setAlignment(Pos.CENTER);

        HBox blackCheckBox = new HBox();
        blackCheckBox.setSpacing(4);
        Label blackLbl = new Label("Black");
        CheckBox kingSideCastlingBlack = new CheckBox();
        Label lblBlackKingSide = new Label("O-O");
        CheckBox blackQueenSideCastling = new CheckBox();
        Label lblBlackQueenSide = new Label("O-O-O");
        blackCheckBox.getChildren().addAll(blackLbl, kingSideCastlingBlack, lblBlackKingSide, blackQueenSideCastling, lblBlackQueenSide);
        blackCheckBox.setAlignment(Pos.CENTER);

        VBox root = new VBox();
        root.setSpacing(10);
        root.getChildren().addAll(startAndClearBox, flipBoardBox, movesWindowLayout, isWhiteTurnBox, castlingBox, whiteCheckBox, blackCheckBox);

        Scene scene = new Scene(root, 500, 400);
        stage.setTitle("Right side panel");
        stage.setScene(scene);
        stage.show();
    }
}
