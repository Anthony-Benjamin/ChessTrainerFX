package application.chesstrainerfx.utils;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
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
    private VBox boardEditorPanel;
    private HBox boardTitle;
    private HBox blackPiecesBox;
    private HBox whitePiecesBox;
    private HBox fenBox;
    private CheckBox whiteKingSideCastling;
    private CheckBox whiteQueenSideCastling;
    private CheckBox blackKingSideCastling;
    private CheckBox blackQueenSideCastling;

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
        PieceSelectorPane blackPieces = new PieceSelectorPane(piece -> {});

        boardEditorPanel = new VBox(10);
        boardEditorPanel.setPadding(new Insets(0, 0, 0, 50));
        Label titleLabel = new Label("Board Editor");
        titleLabel.setMinHeight(100);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-font-size: 30px; -fx-font-family: Garamond; -fx-font-weight: bold;");

        boardTitle = new HBox();
        boardTitle.setAlignment(Pos.CENTER);
        boardTitle.getChildren().add(titleLabel);

        blackPiecesBox = new HBox();
        blackPiecesBox.getChildren().add(blackPieces.blackPieces());
        blackPiecesBox.setAlignment(Pos.CENTER);

        model = new BoardModel();
        model.initializeFromFEN("1r3b2/2nR4/2pNn1kp/1p2PN2/6P1/7P/r7/2B1R1K1 w - - 0 1");
        Controller controller = new Controller();

        int boardSize = 600;
        board = new BoardEditor(model, controller, isWhite, boardSize);
        board.setAlignment(Pos.CENTER);

        whitePiecesBox = new HBox();
        PieceSelectorPane whitePieces = new PieceSelectorPane(piece -> {});
        whitePiecesBox.getChildren().add(whitePieces.whitePieces());
        whitePiecesBox.setAlignment(Pos.CENTER);

        fenBox = new HBox();
        fenBox.setMinHeight(50);
        Label fenLabel = new Label("FEN: ");
        fenTextField = new TextField();
        fenTextField.setMinWidth(560);
        fenBox.getChildren().addAll(fenLabel, fenTextField);
        fenBox.setAlignment(Pos.CENTER);

        if(isWhite){
            boardEditorPanel.getChildren().addAll(boardTitle, blackPiecesBox, board, whitePiecesBox, fenBox);
        }else{
            boardEditorPanel.getChildren().addAll(boardTitle, whitePiecesBox, board,blackPiecesBox , fenBox);
        }

        return boardEditorPanel;
    }

    public VBox createSidePanel(Stage stage){
        int widthButtonSize = 150;

        HBox startAndClearBox = new HBox(5);
        startAndClearBox.setPadding(new Insets(153, 0, 0, 0));
        startAndClearBox.setAlignment(Pos.CENTER);
        startBtn = new Button("Start Position");
        startBtn.setOnAction(e -> model.initializeFromFEN(""));
        clearBoardBtn = new Button("Clear Board");
        clearBoardBtn.setOnAction(e -> model.clearBoard());
        startBtn.setMinWidth(widthButtonSize);
        clearBoardBtn.setMinWidth(widthButtonSize);
        startAndClearBox.setSpacing(10);
        startAndClearBox.getChildren().addAll(startBtn, clearBoardBtn);

        HBox flipBoardBox = new HBox();
        flipBoardBox.setAlignment(Pos.CENTER);
        flipBoardBtn = new Button("Flip Board");
        flipBoardBtn.setOnAction(e -> {
            isWhite = !isWhite;
            board.flip();
            if (isWhite) {
                boardEditorPanel.getChildren().setAll(boardTitle, blackPiecesBox, board, whitePiecesBox, fenBox);
            } else {
                boardEditorPanel.getChildren().setAll(boardTitle, whitePiecesBox, board, blackPiecesBox, fenBox);
            }
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
            boolean whiteToMove = "White to move".equals(whoseTurnSelector.getValue());
            fenTextField.setText(model.exportToFEN(whiteToMove, buildCastlingString()));
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
        whiteKingSideCastling = new CheckBox();
        Label lblKingSide = new Label("O-O");
        whiteQueenSideCastling = new CheckBox();
        Label lblQueenSide = new Label("O-O-O");
        whiteCheckBox.getChildren().addAll(white, whiteKingSideCastling, lblKingSide, whiteQueenSideCastling, lblQueenSide);
        whiteCheckBox.setAlignment(Pos.CENTER);

        HBox blackCheckBox = new HBox();
        blackCheckBox.setSpacing(4);
        Label blackLbl = new Label("Black");
        blackKingSideCastling = new CheckBox();
        Label lblBlackKingSide = new Label("O-O");
        blackQueenSideCastling = new CheckBox();
        Label lblBlackQueenSide = new Label("O-O-O");
        blackCheckBox.getChildren().addAll(blackLbl, blackKingSideCastling, lblBlackKingSide, blackQueenSideCastling, lblBlackQueenSide);
        blackCheckBox.setAlignment(Pos.CENTER);

        HBox saveButtonLayout = new HBox();
        saveButtonLayout.setPadding(new Insets(10, 0, 0, 0));
        Button saveExerciseBtn = new Button("Save exercise");

        saveExerciseBtn.setOnAction(e -> {
            if(mainDirectory == null){
                showInfo("Geen map geselecteerd", "Selecteer eerst een map om het bestand op te slaan.");
                return;
            }

            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Enter a file name to save your exercise");
            dialog.setTitle("Exercise name");
            Optional<String> fileName = dialog.showAndWait();

            if (fileName.isEmpty() || fileName.get().isBlank()) {
                return;
            }
            String name = fileName.get().trim();

            File file = new File(mainDirectory.getPath() + "/" + name + ".pgn");

            boolean whoseTurn = whoseTurnSelector.getValue().equals("White to move");
            String fen = model.exportToFEN(whoseTurn, buildCastlingString());
            String moves = movesWindow.getText();
            saveToFile(file, generatePGN(name, fen, moves));
        });

        saveButtonLayout.getChildren().add(saveExerciseBtn);

        //====================================================
        // Category section
        //====================================================
        HBox categoryHBox = new HBox(5);
        Button categoriesBtn = new Button("Set category");
        categoryHBox.setAlignment(Pos.TOP_RIGHT);
        Label setCategoryStatusLbl = new Label("☒");
        setCategoryStatusLbl.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 20px");
        categoryHBox.getChildren().addAll(categoriesBtn, setCategoryStatusLbl);

        categoriesBtn.setOnMouseClicked(e -> {
            String home = System.getProperty("user.home");
            String path = home + "/IdeaProjects/ChessTrainerFX/src/main/resources/pgn/Puzzles/";
            File initialDir = new File(path);

            // Fallback naar home directory als het ingestelde pad niet bestaat
            if (!initialDir.exists()) {
                initialDir = new File(home);
            }

            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(initialDir);
            mainDirectory = directoryChooser.showDialog(stage);

            if (mainDirectory != null) {
                setCategoryStatusLbl.setText("☑");
                setCategoryStatusLbl.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 20px");
            }
        });

        VBox root = new VBox();
        root.setSpacing(10);
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

    /** Bouwt het FEN-rokadeveld uit de checkboxes ("KQkq", "Kq", "-", ...). */
    private String buildCastlingString() {
        StringBuilder sb = new StringBuilder();
        if (whiteKingSideCastling.isSelected())  sb.append('K');
        if (whiteQueenSideCastling.isSelected()) sb.append('Q');
        if (blackKingSideCastling.isSelected())  sb.append('k');
        if (blackQueenSideCastling.isSelected()) sb.append('q');
        return sb.isEmpty() ? "-" : sb.toString();
    }

    private void saveToFile(File file, String pgn) {
        if (file != null) {
            try (BufferedWriter writer =
                         Files.newBufferedWriter(file.toPath())) {

                writer.write(pgn);

            } catch (IOException ex) {
                showInfo("Opslaan mislukt", "Kon het bestand niet opslaan: " + ex.getMessage());
            }
        }
    }


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
        if(moves.startsWith("1 -")){
            moves = moves.replace("1 -", "1.");
        }
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
