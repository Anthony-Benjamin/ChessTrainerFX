package application.chesstrainerfx.controller;

import application.chesstrainerfx.imagescanner.ImageScanPane;
import application.chesstrainerfx.imagescanner.ScanResult;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.BoardEditor;
import application.chesstrainerfx.utils.PgnUtils;
import application.chesstrainerfx.utils.PieceSelectorPane;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Controller voor de Position Editor (position-editor.fxml) in de Puzzles-module:
 * stukken slepen, FEN exporteren en de positie als PGN-oefening opslaan in een
 * puzzles-sub-categorie.
 */
public class PositionEditorController {

    private static final String WHITE_TO_MOVE = "White to move";
    private static final String BLACK_TO_MOVE = "Black to move";

    private static final Preferences PREFS = Preferences.userNodeForPackage(PositionEditorController.class);
    private static final String PREF_LAST_DIR = "lastSaveDirectory";

    @FXML private VBox boardColumn;
    @FXML private VBox scanColumn;
    @FXML private Label categoryStatusLbl;
    @FXML private Label categoryPathLbl;
    @FXML private TextArea movesWindow;
    @FXML private ComboBox<String> whoseTurnSelector;
    @FXML private CheckBox whiteKingSideCastling;
    @FXML private CheckBox whiteQueenSideCastling;
    @FXML private CheckBox blackKingSideCastling;
    @FXML private CheckBox blackQueenSideCastling;

    private BoardModel model;
    private BoardEditor board;
    private ImageScanPane scanPane;
    private boolean isWhite = true;
    private File saveDirectory;
    private Stage stage;
    private Path puzzlesDir;

    private HBox boardTitleBox;
    private HBox blackPiecesBox;
    private HBox whitePiecesBox;
    private HBox fenBox;
    private TextField fenTextField;

    @FXML
    private void initialize() {
        whoseTurnSelector.getItems().addAll(WHITE_TO_MOVE, BLACK_TO_MOVE);
        whoseTurnSelector.setValue(WHITE_TO_MOVE);

        model = new BoardModel();
        model.initializeFromFEN("");
        Controller editorController = new Controller();
        editorController.setEditorMode(true);
        board = new BoardEditor(model, editorController, isWhite, 600);
        board.setAlignment(Pos.CENTER);

        buildBoardColumn();
        layoutBoardColumn();

        // FEN-veld direct mee laten bewegen met de gekozen zijde-aan-zet.
        whoseTurnSelector.valueProperty().addListener((obs, oldV, newV) ->
                fenTextField.setText(model.exportToFEN(WHITE_TO_MOVE.equals(newV), buildCastlingString())));
    }

    /** Stage en puzzles-map (startmap voor "Set category") aanreiken na het laden van de FXML. */
    public void init(Stage stage, Path puzzlesDir) {
        this.stage = stage;
        this.puzzlesDir = puzzlesDir;

        String saved = PREFS.get(PREF_LAST_DIR, null);
        if (saved != null) {
            File dir = new File(saved);
            if (dir.isDirectory()) {
                saveDirectory = dir;
                showSelectedCategory();
            }
        }
    }

    private void buildBoardColumn() {
        Label titleLabel = new Label("Position Editor");
        titleLabel.setMinHeight(100);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-font-size: 30px; -fx-font-family: Garamond; -fx-font-weight: bold;");
        boardTitleBox = new HBox(titleLabel);
        boardTitleBox.setAlignment(Pos.CENTER);

        blackPiecesBox = new HBox(new PieceSelectorPane(piece -> {}).blackPieces());
        blackPiecesBox.setAlignment(Pos.CENTER);

        whitePiecesBox = new HBox(new PieceSelectorPane(piece -> {}).whitePieces());
        whitePiecesBox.setAlignment(Pos.CENTER);

        Label fenLabel = new Label("FEN: ");
        fenTextField = new TextField();
        fenTextField.setMinWidth(490);
        fenTextField.setEditable(true);
        fenTextField.setPromptText("Plak of typ een FEN-string…");
        Button loadFenBtn = new Button("Load FEN");
        loadFenBtn.setOnAction(e -> onLoadFen());
        fenBox = new HBox(5, fenLabel, fenTextField, loadFenBtn);
        fenBox.setMinHeight(50);
        fenBox.setAlignment(Pos.CENTER);
    }

    private void layoutBoardColumn() {
        if (isWhite) {
            boardColumn.getChildren().setAll(boardTitleBox, blackPiecesBox, board, whitePiecesBox, fenBox);
        } else {
            boardColumn.getChildren().setAll(boardTitleBox, whitePiecesBox, board, blackPiecesBox, fenBox);
        }
    }

    @FXML
    private void onStartPosition() {
        model.initializeFromFEN("");
    }

    @FXML
    private void onClearBoard() {
        model.clearBoard();
    }

    @FXML
    private void onFlipBoard() {
        isWhite = !isWhite;
        board.flip();
        layoutBoardColumn();
    }

    /** Toont/verbergt het scanpaneel; lazy aanmaken bewaart afbeelding en crop over toggles heen. */
    @FXML
    private void onImportFromImage() {
        if (scanPane == null) {
            scanPane = new ImageScanPane(stage, this::applyScanResult);
            scanColumn.getChildren().add(scanPane);
        }
        boolean show = !scanColumn.isVisible();
        scanColumn.setVisible(show);
        scanColumn.setManaged(show);
    }

    /**
     * Zet een scanresultaat op het editor-bord. Het bord draait mee met het perspectief
     * van de afbeelding en de partij onderaan is aan zet. Markeren moet ná de
     * perspectiefwissel: flip() herbouwt de SquareViews en zou de marks wissen.
     */
    private void applyScanResult(ScanResult result, boolean blackPerspective) {
        boolean whiteToMove = !blackPerspective;
        String fen = result.toFEN(whiteToMove);
        model.initializeFromFEN(fen);
        isWhite = whiteToMove;
        board.setWhitePerspective(isWhite);
        layoutBoardColumn();
        board.markLowConfidenceSquares(result.getLowConfidenceSquares());
        whoseTurnSelector.setValue(whiteToMove ? WHITE_TO_MOVE : BLACK_TO_MOVE);
        fenTextField.setText(fen);
    }

    private void onLoadFen() {
        String fen = fenTextField.getText().trim();
        if (!fen.isBlank()) {
            model.initializeFromFEN(fen);
            setTurnSelectorFromFen(fen);
        }
    }

    /** Zet de zijde-aan-zet selector op basis van het actieve-kleur veld in een FEN. */
    private void setTurnSelectorFromFen(String fen) {
        String[] parts = fen.trim().split("\\s+");
        boolean whiteToMove = parts.length < 2 || !parts[1].equals("b");
        whoseTurnSelector.setValue(whiteToMove ? WHITE_TO_MOVE : BLACK_TO_MOVE);
    }

    @FXML
    private void onExportFen() {
        boolean whiteToMove = WHITE_TO_MOVE.equals(whoseTurnSelector.getValue());
        fenTextField.setText(model.exportToFEN(whiteToMove, buildCastlingString()));
    }

    @FXML
    private void onSetCategory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Kies een puzzles-sub-categorie");

        File initialDir = saveDirectory != null ? saveDirectory
                : (puzzlesDir != null && Files.isDirectory(puzzlesDir)) ? puzzlesDir.toFile()
                : new File(System.getProperty("user.home"));
        directoryChooser.setInitialDirectory(initialDir);

        File chosen = directoryChooser.showDialog(stage);
        if (chosen != null) {
            saveDirectory = chosen;
            showSelectedCategory();
        }
    }

    /** Toont het groene vinkje en het pad van de gekozen categorie-map. */
    private void showSelectedCategory() {
        categoryStatusLbl.setText("☑");
        categoryStatusLbl.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 20px");
        categoryPathLbl.setText(saveDirectory.getAbsolutePath());
    }

    @FXML
    private void onSaveExercise() {
        if (saveDirectory == null) {
            showInfo("Geen map geselecteerd", "Selecteer eerst een categorie (map) om het bestand op te slaan.");
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

        File file = new File(saveDirectory, name + ".pgn");

        boolean whiteToMove = WHITE_TO_MOVE.equals(whoseTurnSelector.getValue());
        String fen = model.exportToFEN(whiteToMove, buildCastlingString());
        String moves = movesWindow.getText();
        saveToFile(file, PgnUtils.buildExercisePgn(name, fen, moves));
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
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            writer.write(pgn);
            PREFS.put(PREF_LAST_DIR, file.getParent());
        } catch (IOException ex) {
            showInfo("Opslaan mislukt", "Kon het bestand niet opslaan: " + ex.getMessage());
        }
    }

    private static void showInfo(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }
}
