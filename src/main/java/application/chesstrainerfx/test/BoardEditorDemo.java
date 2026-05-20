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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class BoardEditorDemo extends Application {
    private Button startBtn;
    private BoardModel model;
    private Button flipBoardBtn;
    private Button clearBoardBtn;
    private BoardEditor board;
    private boolean isWhite;
    private HBox root;
    private TextField fenTextField;
    private File mainDirectory;
    private ComboBox<Object> whoseTurnSelector;
    private TextArea movesWindow;

    @Override
    public void start(Stage stage) throws Exception {
        isWhite = true;

        root = new HBox(10);
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(createBoardEditor(isWhite), createSidePanel(stage));

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
        model.initializeFromFEN("r6r/8/8/8/8/8/8/R6R w - - 0 1");
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
        fenTextField = new TextField();
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

    public VBox createSidePanel(Stage stage){
        int widthButtonSize = 150;

        HBox startAndClearBox = new HBox(5);
        startAndClearBox.setPadding(new Insets(153, 0, 0, 0));
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
            root.getChildren().addAll(createBoardEditor(isWhite),createSidePanel(stage));


        });

        HBox isWhiteTurnBox = new HBox();
        isWhiteTurnBox.setAlignment(Pos.CENTER);
        whoseTurnSelector = new ComboBox<>();
        whoseTurnSelector.getItems().addAll("White to move", "Black to move");
        whoseTurnSelector.setValue("White to move");
        isWhiteTurnBox.getChildren().add(whoseTurnSelector);

        Button exportFENBtn = new Button("Export FEN");
        flipBoardBtn.setMinWidth(widthButtonSize);
        exportFENBtn.setMinWidth(widthButtonSize);
        exportFENBtn.setOnAction(e -> {
            System.out.println(whoseTurnSelector.getValue());
//            if(whoseTurnSelector.getValue().equals("White to move")){
//                fenTextField.setText(model.exportToFEN(true));;
//            }else {
//                fenTextField.setText(model.exportToFEN(false));;
//            }
////            fenTextField.setText(model.exportToFEN(false));
////            fenTextField.setText(model.exportToFEN(false));

            boolean whiteToMove = "White to move".equals(whoseTurnSelector.getValue());
            fenTextField.setText(model.exportToFEN(whiteToMove));
        });
        flipBoardBox.setSpacing(10);
        flipBoardBox.getChildren().addAll(flipBoardBtn, exportFENBtn);

        // Move window
        VBox movesWindowLayout = new VBox();
        Label movesTitlelbl = new Label("Moves");
        movesTitlelbl.setStyle("-fx-font-weight: bold;");
        movesWindow = new TextArea();
        movesWindow.setMaxWidth(150);
        movesWindow.setMaxHeight(150);
        movesWindowLayout.setAlignment(Pos.CENTER);
        movesWindowLayout.getChildren().addAll(movesTitlelbl, movesWindow);
        // end Move window




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

        saveExerciseBtn.setOnAction(e -> {
            if(mainDirectory == null){
                System.out.println("Please select directory to save file!");
                return;
            }

            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Enter a file name to save your exercise");
            dialog.setTitle("Exercise name");
            Optional<String> fileName = dialog.showAndWait();

            File file = new File(mainDirectory.getPath() + "/"+ fileName.get() + ".pgn");
            System.out.println("file: " + file);

            boolean whoseTurn = whoseTurnSelector.getValue().equals("White to move");
            String fen = model.exportToFEN(whoseTurn);
            String moves = movesWindow.getText();
            saveToFile(file, generatePGN(fileName.get(), model.exportToFEN(whoseTurn), moves));

//            TextInputDialog dialog = new TextInputDialog();
//            dialog.setHeaderText("Enter a file name to save your exercise");
//            dialog.setTitle("Exercise name");
//            Optional<String> fileName = dialog.showAndWait();
//
//            if(fileName.isPresent()){
//                String titel = fileName.get();
//                System.out.println("File name: " + titel);
//                boolean whoseTurn = whoseTurnSelector.getValue().equals("White to move");
//                String fen = model.exportToFEN(whoseTurn);
//                String moves = movesWindow.getText();
//                saveToFile(file, generatePGN(titel, fen, moves));
////                System.out.println(generatePGN(titel, fen, moves));
////                System.out.println(fileName + " saved to " + mainDirectory.getName());
//            }
        });

        saveButtonLayout.getChildren().add(saveExerciseBtn);
//        saveButtonLayout.setAlignment(Pos.CENTER_LEFT);

        //====================================================
        // Category section
        //====================================================
        HBox categoryHBox = new HBox();
        Button categoriesBtn = new Button("Set category");
        categoryHBox.setAlignment(Pos.TOP_RIGHT);
        categoryHBox.getChildren().add(categoriesBtn);
//        String path = "/home/ebenjamin/IdeaProjects/ChessTrainerFX/src/main/resources/pgn";

//        System.out.println(path);

        categoriesBtn.setOnMouseClicked(e -> {
            String home = System.getProperty("user.home");
            String path = home + "/IdeaProjects/ChessTrainerFX/src/main/resources/pgn/Puzzles/";
            mainDirectory = new File(path);
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(mainDirectory);
            mainDirectory = directoryChooser.showDialog(stage);
            System.out.println("New main directory: " + mainDirectory);
//            System.out.println(mainDirectory);
        });

        VBox root = new VBox();
        root.setSpacing(10);
//        root.setAlignment(Pos.CENTER);
//        root.setPadding(new Insets(165, 0, 0, 75));
        root.setPadding(new Insets(10, 0, 0, 75));
        root.getChildren().addAll(
                categoryHBox,
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

    private void saveToFile(File file, String pgn) {
        if (file != null) {
            try (BufferedWriter writer =
                         Files.newBufferedWriter(file.toPath())) {

                writer.write(pgn);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

//    String generatePGN(String title){
////        [Event "F"]
////        [Site "S"]
////        [Date "YYYY.MM.DD"]
////        [Round "R"]
////        [White "W"]
////        [Black "B"]
////        [Result "1-0"]
//
////[Event "?"]
////[Site "?"]
////[Date "????.??.??"]
////[Round "?"]
////[White "31. Boden's Mate"]
////[Black "?"]
////[Result "*"]
////[Annotator ""]
////[SetUp "1"]
////[FEN "2kr4/3p4/8/8/8/6B1/8/5BK1 w - - 0 1"]
////[PlyCount "1"]
////[EventDate "2023.02.03"]
////[SourceVersionDate "2025.08.18"]
//
//        StringBuilder pgn = new StringBuilder();
//        pgn.append("[Event \"" + "F\"]\n");
//        pgn.append("[Site \"" + "S\"]\n");
//        pgn.append("[Date " + "\"YYYY.MM.DD\"]\n");
//        pgn.append("[Round " + "\"R\"]\n");
//        pgn.append("[White \"" + title + "\"]\n");
//        pgn.append("[Black " + "\"B\"]\n");
//        pgn.append("[Result " + "\"1-0\"]\n");
//
//        return pgn.toString();
//    }

public static String generatePGN(String title, String fen, String moves) {
    StringBuilder pgn = new StringBuilder();

    appendTag(pgn, "Event", "?");
    appendTag(pgn, "Site", "?");
    appendTag(pgn, "Date", "????.??.??");
    appendTag(pgn, "Round", "?");
    appendTag(pgn, "White", title);
    appendTag(pgn, "Black", "?");
    appendTag(pgn, "Result", "*");


    appendTag(pgn, "FEN", fen);

    pgn.append("\n");
    pgn.append(moves);

    return pgn.toString();
}

    private static void appendTag(StringBuilder sb, String key, String value) {
        sb.append("[")
                .append(key)
                .append(" \"")
                .append(value)
                .append("\"]\n");
    }
    private static void showInfo(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }
}
