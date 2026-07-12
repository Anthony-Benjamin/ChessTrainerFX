package application.chesstrainerfx.imagescanner;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Scanpaneel voor het omzetten van een schaakbord-afbeelding naar een positie,
 * permanent ingebed in de Position Editor.
 *
 * Stroom:
 *  1. Bestand kiezen → afbeelding getoond in de preview
 *  2. Sleep over het bord-gebied om het bij te snijden (optioneel; standaard = hele afbeelding)
 *  3. Scan ▶ → scanner loopt; het resultaat gaat via de ScanListener naar het editor-bord
 *     Oranje velden = lage scanner-confidence → controleer/corrigeer via drag-drop
 */
public class ImageScanPane extends VBox {

    /** Callback waarmee een afgeronde scan aan de omliggende editor wordt doorgegeven. */
    public interface ScanListener {
        void onScanCompleted(ScanResult result, boolean blackPerspective);
    }

    private static final int PREVIEW_SIZE = 400;

    // onthoudt de map van de laatst gekozen afbeelding tussen sessies
    private static final Preferences PREFS = Preferences.userNodeForPackage(ImageScanPane.class);
    private static final String PREF_LAST_IMAGE_DIR = "lastImageDir";

    private static final String WHITE_BOTTOM = "White at bottom";
    private static final String BLACK_BOTTOM = "Black at bottom";

    private final ImageScannerPresenter presenter = new ImageScannerPresenter();
    private final ScanListener listener;

    private Image loadedImage;
    private final ImageView previewImageView = new ImageView();
    private final Rectangle cropRect        = new Rectangle();
    private double dragStartX, dragStartY;
    // crop in view-coördinaten (worden bij scan omgezet naar image-coördinaten)
    private double cropVX, cropVY, cropVW, cropVH;
    private boolean hasCrop = false;

    private final Button scanBtn      = new Button("Scan ▶");
    private final Button resetCropBtn = new Button("Reset crop");
    private final ComboBox<String> perspectiveBox = new ComboBox<>();
    private final Label statusLabel = new Label("Load an image to begin.");

    /** Bouwt het scanpaneel; Stage is nodig voor de FileChooser. */
    public ImageScanPane(Stage stage, ScanListener listener) {
        super(10);
        this.listener = listener;

        Label title = styledLabel("Image", 16, true);

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
        previewPane.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: rgba(245,222,179,0.35); -fx-border-width: 1;");

        installCropHandlers(previewPane);

        Button chooseBtn = new Button("Choose file…");
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

        Label cropHint = styledLabel("Drag across the board to crop (optional)", 11, false);
        cropHint.setTextFill(Color.web("#f5deb3", 0.7));

        perspectiveBox.setStyle("-fx-background-color: #d7b77e; -fx-background-radius: 8;");
        perspectiveBox.getItems().addAll(WHITE_BOTTOM, BLACK_BOTTOM);
        perspectiveBox.setValue(WHITE_BOTTOM);
        // Een ander perspectief vereist een nieuwe cel→FEN mapping → opnieuw scannen.
        perspectiveBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (loadedImage == null) return;
            onScan();
        });
        Label perspLabel = styledLabel("Perspective:", 12, false);
        HBox perspRow = new HBox(10, perspLabel, perspectiveBox);
        perspRow.setAlignment(Pos.CENTER_LEFT);

        HBox btnRow = new HBox(10, chooseBtn, scanBtn, resetCropBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel.setStyle("-fx-text-fill: #f5deb3; -fx-font-size: 12;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(PREVIEW_SIZE);

        setPadding(new Insets(20));
        setStyle("-fx-background-color: #5A1F0E;");
        setPrefWidth(PREVIEW_SIZE + 40);
        setMaxWidth(PREVIEW_SIZE + 40);
        setAlignment(Pos.TOP_LEFT);
        getChildren().addAll(title, previewPane, cropHint, perspRow, btnRow, statusLabel);
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

    private void chooseFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select a chessboard image");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        File lastDir = new File(PREFS.get(PREF_LAST_IMAGE_DIR, ""));
        if (lastDir.isDirectory()) fc.setInitialDirectory(lastDir);

        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        File parent = file.getParentFile();
        if (parent != null) PREFS.put(PREF_LAST_IMAGE_DIR, parent.getAbsolutePath());

        loadedImage = new Image(file.toURI().toString());
        previewImageView.setImage(loadedImage);
        hasCrop = false;
        cropRect.setVisible(false);
        resetCropBtn.setDisable(true);
        scanBtn.setDisable(false);
        statusLabel.setText("Image loaded. Drag to crop or scan right away.");
    }

    private void onScan() {
        if (loadedImage == null) return;

        Image toScan = hasCrop ? cropToImage() : loadedImage;

        scanBtn.setDisable(true);
        statusLabel.setText("Scanning…");

        boolean black = BLACK_BOTTOM.equals(perspectiveBox.getValue());
        presenter.setBlackPerspective(black);

        ScanResult result = presenter.scan(toScan);

        int low = result.getLowConfidenceSquares().size();
        statusLabel.setText(low == 0
            ? "Scan complete — all squares recognized with high confidence."
            : "Scan complete — " + low + " square(s) uncertain (orange). Check and correct.");

        listener.onScanCompleted(result, black);
        scanBtn.setDisable(false);
    }

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

    private Label styledLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle(String.format(
            "-fx-text-fill: #f5deb3; -fx-font-size: %dpx; -fx-font-weight: %s;",
            size, bold ? "bold" : "normal"));
        return l;
    }

    private void styleButton(Button btn, boolean primary) {
        String weight = primary ? "-fx-font-weight: bold; " : "";
        btn.setStyle("-fx-background-color: #d7b77e; -fx-background-radius: 8; "
                + weight + "-fx-padding: 6 14; -fx-cursor: hand;");
    }
}
