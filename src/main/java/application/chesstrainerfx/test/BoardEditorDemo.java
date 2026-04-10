package application.chesstrainerfx.test;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.utils.PieceSelectorPane;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BoardEditorDemo extends Application {
    private Button startBtn;
    private BoardModel model;
    private Button flipBoardBtn;
    private Button clearBoardBtn;
    private BoardEditor board;
    private boolean isWhite;
    private HBox root;

    @Override
    public void start(Stage stage) throws Exception {
        isWhite = true;

        root = new HBox(10);
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(createBoardEditor(isWhite), createSidePanel());

        stage.setTitle("Board Editor");
        stage.setScene(new Scene(root, 1200, 1000));
        stage.show();
    }

    public VBox createBoardEditor(boolean isWhite){
        PieceSelectorPane blackPieces = new PieceSelectorPane(new PieceSelectorPane.PieceSelectionListener() {
            @Override
            public void onPieceSelected(PieceModel piece) {
                System.out.println(piece.getType() + "" + piece.getColor());
            }
        });

        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0, 0, 0, 50));
        Label titleLabel = new Label("Board Editor");
        titleLabel.setMinHeight(100);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-font-size: 30px; -fx-font-family: Garamond; -fx-font-weight: bold;");

        HBox boardTitle = new HBox();
        boardTitle.setAlignment(Pos.CENTER);
        boardTitle.getChildren().add(titleLabel);

        HBox blackPiecesBox = new HBox();
        blackPiecesBox.getChildren().add(blackPieces.blackPieces());
        blackPiecesBox.setAlignment(Pos.CENTER);

        model = new BoardModel();
        Controller controller = new Controller();

        int boardSize = 600;
        board = new BoardEditor(model, controller, isWhite, boardSize);
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

        if(isWhite){
            panel.getChildren().addAll(boardTitle, blackPiecesBox, board, whitePiecesBox, fenBox);
        }else{
            panel.getChildren().addAll(boardTitle, whitePiecesBox, board,blackPiecesBox , fenBox);
        }

        return panel;
    }

    public VBox createSidePanel(){
        int widthButtonSize = 150;

        HBox startAndClearBox = new HBox(5);
        startAndClearBox.setAlignment(Pos.CENTER);
        startBtn = new Button("Start Position");
        startBtn.setOnAction(e ->{
            model.initializeFromFEN("");
        });
        clearBoardBtn = new Button("Clear Board");
        clearBoardBtn.setOnAction(e -> {
            model.clearBoard();
        });
        startBtn.setMinWidth(widthButtonSize);
        clearBoardBtn.setMinWidth(widthButtonSize);
        startAndClearBox.setSpacing(10);
//        startAndClearBox.setPadding(new Insets(5));
        startAndClearBox.getChildren().addAll(startBtn, clearBoardBtn);

        HBox flipBoardBox = new HBox();
        flipBoardBox.setAlignment(Pos.CENTER);
        flipBoardBtn = new Button("Flip Board");
        flipBoardBtn.setOnAction(e -> {
            System.out.println(root.getChildren());

            root.getChildren().clear();

            isWhite = !isWhite;
            System.out.println(isWhite);
            root.getChildren().addAll(createBoardEditor(isWhite),createSidePanel());


        });
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

        HBox saveButtonLayout = new HBox();
        saveButtonLayout.setPadding(new Insets(10, 0, 0, 0));
        Button saveExerciseBtn = new Button("Save exercise");
        saveButtonLayout.getChildren().add(saveExerciseBtn);
//        saveButtonLayout.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox();
        root.setSpacing(10);
//        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(165, 0, 0, 75));
        root.getChildren().addAll(
                startAndClearBox,
                flipBoardBox,
                movesWindowLayout,
                isWhiteTurnBox,
                castlingBox,
                whiteCheckBox,
                blackCheckBox,
                saveButtonLayout
        );
        return root;
    }
}
