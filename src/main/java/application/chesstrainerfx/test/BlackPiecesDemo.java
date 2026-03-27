package application.chesstrainerfx.test;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.utils.PieceSelectorPane;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BlackPiecesDemo extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        VBox root = new VBox(10);
        PieceSelectorPane blackPieces = new PieceSelectorPane(new PieceSelectorPane.PieceSelectionListener() {
            @Override
            public void onPieceSelected(PieceModel piece) {

            }
        });

        Label titleLabel = new Label("Board board");
        titleLabel.setMinHeight(100);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-font-size: 30px; -fx-font-family: Garamond; -fx-font-weight: bold;");

        HBox boardTitle = new HBox();
        boardTitle.setAlignment(Pos.CENTER);
        boardTitle.getChildren().add(titleLabel);

        HBox blackPiecesBox = new HBox();
        blackPiecesBox.getChildren().add(blackPieces.blackPieces());
        blackPiecesBox.setAlignment(Pos.CENTER);

       BoardModel model = new BoardModel();
       Controller controller = new Controller();
       boolean isWhite = true;
       int boardSize = 600;
       BoardEditor board = new BoardEditor(model, controller, isWhite, boardSize);
       board.setAlignment(Pos.CENTER);

       HBox whitePiecesBox = new HBox();
       PieceSelectorPane whitePieces = new PieceSelectorPane(new PieceSelectorPane.PieceSelectionListener() {
           @Override
           public void onPieceSelected(PieceModel piece) {

           }
       });
       whitePiecesBox.getChildren().add(whitePieces.whitePieces());
       whitePiecesBox.setAlignment(Pos.CENTER);

       HBox fenBox = new HBox();
       fenBox.setMinHeight(50);
       Label fenLabel = new Label("FEN: ");
       TextField fenTextField = new TextField();
       fenTextField.setMinWidth(560);
       fenBox.getChildren().addAll(fenLabel, fenTextField);
       fenBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(boardTitle, blackPiecesBox, board, whitePiecesBox, fenBox);
        stage.setTitle("Board Editor");
        stage.setScene(new Scene(root, 1500, 1000));
        stage.show();
    }
}
