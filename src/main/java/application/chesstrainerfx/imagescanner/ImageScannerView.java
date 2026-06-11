package application.chesstrainerfx.imagescanner;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.utils.BoardEditor;
import application.chesstrainerfx.utils.PieceSelectorPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

/**
 * UI voor het omzetten van een schaakbord-afbeelding naar FEN.
 *
 * Stroom:
 *  1. Bestand kiezen → afbeelding getoond in linker preview
 *  2. Sleep over het bord-gebied om het bij te snijden (optioneel; standaard = hele afbeelding)
 *  3. Scan ▶ → scanner loopt; resultaat verschijnt in rechter BoardEditor
 *     Oranje velden = lage scanner-confidence → controleer/corrigeer via drag-drop
 *  4. Selecteer wie aan zet is; klik "FEN kopiëren" of "Gebruik FEN" om te exporteren
 *
 * Bereikbaar als scherm binnen de Puzzles-module (zie Main.openImageScanner).
 * Als onAccept niet null is (via createView(Stage, Consumer)), verschijnt ook "Accept position".
 */
public class ImageScannerView {

    private static final int PREVIEW_SIZE = 400;
    private static final int BOARD_SIZE   = 480;

    // ---- linker panel ----
    private Image loadedImage;
    private final ImageView previewImageView = new ImageView();
    private final Rectangle cropRect        = new Rectangle();
    private double dragStartX, dragStartY;
    // crop in view-coördinaten (worden bij scan omgezet naar image-coördinaten)
    private double cropVX, cropVY, cropVW, cropVH;
    private boolean hasCrop = false;

    // ---- rechter panel ----
    private final ImageScannerPresenter presenter = new ImageScannerPresenter();
    private BoardEditor boardEditor;
    private final Label statusLabel   = new Label("Laad een afbeelding om te beginnen.");
    private final TextField fenField  = new TextField();
    private final ComboBox<String> turnBox = new ComboBox<>();

    // ---- knoppen ----
    private final Button scanBtn      = new Button("Scan ▶");
    private final Button resetCropBtn = new Button("Reset crop");
    private final ComboBox<String> perspectiveBox = new ComboBox<>();

    // null = standalone modus (geen "Accept position"-knop)
    private Consumer<String> onAccept;

    /** Bouwt het scanner-scherm; Stage is nodig voor de FileChooser. */
    public Parent createView(Stage stage) {
        return createView(stage, null);
    }

    /**
     * Bouwt het scanner-scherm met optionele accept-callback.
     * Als onAccept niet null is, verschijnt een "Accept position"-knop die de FEN
     * teruggeeft aan de aanroeper (bijv. PositionEditor).
     */
    public Parent createView(Stage stage, Consumer<String> onAccept) {
        this.onAccept = onAccept;

        Controller editorController = new Controller();
        editorController.setEditorMode(true); // vrije plaatsing voor correctie via drag

        boardEditor = new BoardEditor(
            presenter.getResultModel(),
            editorController,
            true,
            BOARD_SIZE
        );

        HBox root = new HBox(20, buildLeftPanel(stage), buildRightPanel());
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2b2b2b;");
        return root;
    }

    // -------------------------------------------------------------------------
    // Linker panel: afbeelding + crop + scan
    // -------------------------------------------------------------------------

    private VBox buildLeftPanel(Stage stage) {
        Label title = styledLabel("Afbeelding", 16, true);

        // Preview-container: vaste grootte, afbeelding geschaald erin
        previewImageView.setFitWidth(PREVIEW_SIZE);
        previewImageView.setFitHeight(PREVIEW_SIZE);
        previewImageView.setPreserveRatio(true);

        cropRect.setFill(Color.TRANSPARENT);
        cropRect.setStroke(Color.CORNFLOWERBLUE);
        cropRect.setStrokeWidth(2);
        cropRect.setVisible(false);

        StackPane previewPane = new StackPane(previewImageView, cropRect);
        previewPane.setPrefSize(PREVIEW_SIZE, PREVIEW_SIZE);
        previewPane.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
        previewPane.setMaxSize(PREVIEW_SIZE, PREVIEW_SIZE);
        previewPane.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #555; -fx-border-width: 1;");

        installCropHandlers(previewPane);

        Button chooseBtn = new Button("Bestand kiezen…");
        styleButton(chooseBtn, false);
        chooseBtn.setOnAction(e -> chooseFile(stage));

        styleButton(scanBtn, true);
        scanBtn.setDisable(true);
        scanBtn.setOnAction(e -> onScan());

        styleButton(resetCropBtn, false);
        resetCropBtn.setDisable(true);
        resetCropBtn.setOnAction(e -> {
            hasCrop = false;
            cropRect.setVisible(false);
            resetCropBtn.setDisable(true);
        });

        Label cropHint = styledLabel("Sleep over het bord om bij te snijden (optioneel)", 11, false);
        cropHint.setTextFill(Color.GRAY);

        perspectiveBox.getItems().addAll("Wit onder", "Zwart onder");
        perspectiveBox.setValue("Wit onder");
        perspectiveBox.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        Label perspLabel = styledLabel("Perspectief:", 12, false);
        HBox perspRow = new HBox(10, perspLabel, perspectiveBox);
        perspRow.setAlignment(Pos.CENTER_LEFT);

        HBox btnRow = new HBox(10, chooseBtn, scanBtn, resetCropBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(10, title, previewPane, cropHint, perspRow, btnRow);
        panel.setPrefWidth(PREVIEW_SIZE + 20);
        return panel;
    }

    private void installCropHandlers(StackPane pane) {
        pane.setOnMousePressed(e -> {
            if (loadedImage == null) return;
            dragStartX = e.getX();
            dragStartY = e.getY();
            cropRect.setX(dragStartX);
            cropRect.setY(dragStartY);
            cropRect.setWidth(0);
            cropRect.setHeight(0);
            cropRect.setVisible(true);
        });

        pane.setOnMouseDragged(e -> {
            if (loadedImage == null) return;
            double x = Math.min(e.getX(), dragStartX);
            double y = Math.min(e.getY(), dragStartY);
            double w = Math.abs(e.getX() - dragStartX);
            double h = Math.abs(e.getY() - dragStartY);
            cropRect.setX(x);
            cropRect.setY(y);
            cropRect.setWidth(w);
            cropRect.setHeight(h);
        });

        pane.setOnMouseReleased(e -> {
            if (loadedImage == null) return;
            cropVX = cropRect.getX();
            cropVY = cropRect.getY();
            cropVW = cropRect.getWidth();
            cropVH = cropRect.getHeight();
            hasCrop = cropVW > 10 && cropVH > 10;
            resetCropBtn.setDisable(!hasCrop);
        });
    }

    // -------------------------------------------------------------------------
    // Rechter panel: bord + status + FEN
    // -------------------------------------------------------------------------

    private VBox buildRightPanel() {
        Label title = styledLabel("Gecorrigeerde positie", 16, true);

        turnBox.getItems().addAll("Wit aan zet", "Zwart aan zet");
        turnBox.setValue("Wit aan zet");
        turnBox.setStyle("-fx-background-color: #444; -fx-text-fill: white;");

        Button copyBtn = new Button("FEN kopiëren");
        styleButton(copyBtn, false);
        copyBtn.setOnAction(e -> {
            String fen = presenter.exportFEN(isWhiteToMove());
            fenField.setText(fen);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(fen);
            Clipboard.getSystemClipboard().setContent(cc);
            statusLabel.setText("FEN gekopieerd naar klembord.");
        });

        fenField.setEditable(false);
        fenField.setStyle("-fx-background-color: #333; -fx-text-fill: #ccc; -fx-font-family: monospace;");
        fenField.setPrefWidth(500);

        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");

        HBox controlRow;
        if (onAccept != null) {
            Button acceptBtn = new Button("Accept position");
            styleButton(acceptBtn, true);
            acceptBtn.setOnAction(e -> onAccept.accept(presenter.exportFEN(isWhiteToMove())));
            controlRow = new HBox(10, turnBox, copyBtn, acceptBtn);
        } else {
            controlRow = new HBox(10, turnBox, copyBtn);
        }
        controlRow.setAlignment(Pos.CENTER_LEFT);

        // Stuk-palet voor correctie: sleep het juiste stuk op een (oranje) veld, of leeg via trash.
        HBox blackPieces = new HBox(new PieceSelectorPane(piece -> {}).blackPieces());
        HBox whitePieces = new HBox(new PieceSelectorPane(piece -> {}).whitePieces());

        VBox panel = new VBox(10, title, blackPieces, boardEditor, whitePieces, statusLabel, controlRow, fenField);
        panel.setAlignment(Pos.TOP_LEFT);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Acties
    // -------------------------------------------------------------------------

    private void chooseFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecteer een schaakbord-afbeelding");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Afbeeldingen", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        loadedImage = new Image(file.toURI().toString());
        previewImageView.setImage(loadedImage);
        hasCrop = false;
        cropRect.setVisible(false);
        resetCropBtn.setDisable(true);
        scanBtn.setDisable(false);
        statusLabel.setText("Afbeelding geladen. Sleep voor crop of scan direct.");
        fenField.clear();
        boardEditor.clearAllMarks();
    }

    private void onScan() {
        if (loadedImage == null) return;

        Image toScan = hasCrop ? cropToImage() : loadedImage;

        scanBtn.setDisable(true);
        statusLabel.setText("Scannen…");

        presenter.setBlackPerspective("Zwart onder".equals(perspectiveBox.getValue()));
        ScanResult result = presenter.scan(toScan);

        boardEditor.markLowConfidenceSquares(result.getLowConfidenceSquares());

        int low = result.getLowConfidenceSquares().size();
        statusLabel.setText(low == 0
            ? "Scan voltooid — alle velden herkend met hoge zekerheid."
            : "Scan voltooid — " + low + " veld(en) onzeker (oranje). Controleer en corrigeer.");

        fenField.setText(presenter.exportFEN(isWhiteToMove()));
        scanBtn.setDisable(false);
    }

    // -------------------------------------------------------------------------
    // Hulpmethoden
    // -------------------------------------------------------------------------

    /**
     * Berekent de werkelijk weergegeven afbeeldingsbounds binnen de 400×400 preview
     * (rekening houdend met preserve-ratio letterboxing).
     * Geeft [offsetX, offsetY, renderedW, renderedH] terug.
     */
    private double[] renderedBounds() {
        double imgW = loadedImage.getWidth();
        double imgH = loadedImage.getHeight();
        double scale = Math.min(PREVIEW_SIZE / imgW, PREVIEW_SIZE / imgH);
        double rW = imgW * scale;
        double rH = imgH * scale;
        return new double[]{(PREVIEW_SIZE - rW) / 2.0, (PREVIEW_SIZE - rH) / 2.0, rW, rH};
    }

    /** Crop de afbeelding op basis van de door de gebruiker getekende rechthoek. */
    private Image cropToImage() {
        double[] b = renderedBounds();
        double scale = b[2] / loadedImage.getWidth();

        int imgX = (int) Math.max(0, (cropVX - b[0]) / scale);
        int imgY = (int) Math.max(0, (cropVY - b[1]) / scale);
        int imgW = (int) Math.min(cropVW / scale, loadedImage.getWidth()  - imgX);
        int imgH = (int) Math.min(cropVH / scale, loadedImage.getHeight() - imgY);

        if (imgW <= 0 || imgH <= 0) return loadedImage;

        WritableImage out = new WritableImage(imgW, imgH);
        PixelReader pr    = loadedImage.getPixelReader();
        PixelWriter  pw   = out.getPixelWriter();
        for (int row = 0; row < imgH; row++)
            for (int col = 0; col < imgW; col++)
                pw.setArgb(col, row, pr.getArgb(imgX + col, imgY + row));
        return out;
    }

    private boolean isWhiteToMove() {
        return "Wit aan zet".equals(turnBox.getValue());
    }

    private Label styledLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle(String.format(
            "-fx-text-fill: white; -fx-font-size: %dpx; -fx-font-weight: %s;",
            size, bold ? "bold" : "normal"));
        return l;
    }

    private void styleButton(Button btn, boolean primary) {
        String bg = primary ? "#4a7cac" : "#555";
        btn.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;", bg));
    }
}
