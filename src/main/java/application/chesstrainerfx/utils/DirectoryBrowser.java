package application.chesstrainerfx.utils;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX applicatie: Mappenverkenner met aanvinkbare lijst
 * - Toont alle submappen van een gekozen map
 * - Mappen kunnen worden aangevinkt
 * - Nog niet bestaande mappen kunnen worden aangemaakt
 */
public class DirectoryBrowser extends Application {

    private File huidigePad = null;
    private final ObservableList<MapItem> mappenLijst = FXCollections.observableArrayList();
    private ListView<MapItem> listView;
    private Label statusLabel;
    private Label padLabel;
    private TextField nieuweMapVeld;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("📁 Mappenverkenner");

        // ── Kleurenpalet ──────────────────────────────────────────────────
        String achtergrond  = "#1C1C2E";
        String paneel       = "#2A2A40";
        String accent       = "#7C5CBF";
        String accentHelder = "#9B7FE0";
        String tekst        = "#E8E0FF";
        String subtiel      = "#8880A0";
        String succes       = "#4CAF8A";
        String waarschuwing = "#E07070";

        // ── Bovenbalk: pad-kiezer ─────────────────────────────────────────
        Label titellabel = new Label("📂 Mappenverkenner");
        titellabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titellabel.setTextFill(Color.web(tekst));

        padLabel = new Label("Geen map geselecteerd");
        padLabel.setFont(Font.font("Segoe UI", 12));
        padLabel.setTextFill(Color.web(subtiel));
        padLabel.setMaxWidth(Double.MAX_VALUE);

        Button kiesMapKnop = new Button("📁 Kies Map");
        stijlKnop(kiesMapKnop, accent, accentHelder, tekst);
        kiesMapKnop.setOnAction(e -> kiesMap(primaryStage));

        Button verversKnop = new Button("🔄 Verversen");
        stijlKnop(verversKnop, paneel, "#3A3A55", subtiel);
        verversKnop.setOnAction(e -> { if (huidigePad != null) laadMappen(); });

        HBox bovenbalk = new HBox(10, kiesMapKnop, verversKnop);
        bovenbalk.setAlignment(Pos.CENTER_LEFT);

        VBox kopVak = new VBox(6, titellabel, padLabel, bovenbalk);
        kopVak.setPadding(new Insets(20, 20, 14, 20));
        kopVak.setStyle("-fx-background-color: " + paneel + "; " +
                        "-fx-border-color: " + accent + "; " +
                        "-fx-border-width: 0 0 2 0;");

        // ── Aanvinkbare mappenlijst ───────────────────────────────────────
        listView = new ListView<>(mappenLijst);
        listView.setStyle(
            "-fx-background-color: " + achtergrond + ";" +
            "-fx-border-color: transparent;"
        );
        listView.setCellFactory(lv -> new MapCel(tekst, paneel, accent, accentHelder, subtiel));
        listView.setPrefHeight(320);

        // Selecteer-alles / geen-enkele knoppen
        Button selecteerAlles = new Button("☑ Alles");
        Button selecteerGeenEen = new Button("☐ Geen");
        stijlKnopKlein(selecteerAlles, paneel, subtiel);
        stijlKnopKlein(selecteerGeenEen, paneel, subtiel);
        selecteerAlles.setOnAction(e -> mappenLijst.forEach(m -> m.aangevinkt.set(true)));
        selecteerGeenEen.setOnAction(e -> mappenLijst.forEach(m -> m.aangevinkt.set(false)));

        HBox selectieBalk = new HBox(8, new Label("  Selectie:"), selecteerAlles, selecteerGeenEen);
        selectieBalk.setAlignment(Pos.CENTER_LEFT);
        selectieBalk.setPadding(new Insets(6, 12, 6, 12));
        selectieBalk.setStyle("-fx-background-color: " + paneel + ";");
        Label selLabel = new Label("  Selectie:");
        selLabel.setTextFill(Color.web(subtiel));
        selLabel.setFont(Font.font("Segoe UI", 11));
        selectieBalk.getChildren().set(0, selLabel);

        VBox lijstVak = new VBox(0, selectieBalk, listView);
        lijstVak.setPadding(new Insets(10, 16, 4, 16));

        // ── Nieuwe map aanmaken ───────────────────────────────────────────
        Label nieuweMapLabel = new Label("Nieuwe map aanmaken:");
        nieuweMapLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        nieuweMapLabel.setTextFill(Color.web(tekst));

        nieuweMapVeld = new TextField();
        nieuweMapVeld.setPromptText("Naam van de nieuwe map...");
        nieuweMapVeld.setStyle(
            "-fx-background-color: " + paneel + ";" +
            "-fx-text-fill: " + tekst + ";" +
            "-fx-prompt-text-fill: " + subtiel + ";" +
            "-fx-border-color: " + accent + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8 12 8 12;" +
            "-fx-font-size: 13px;"
        );
        HBox.setHgrow(nieuweMapVeld, Priority.ALWAYS);

        Button maakAanKnop = new Button("➕ Aanmaken");
        stijlKnop(maakAanKnop, succes, "#5DBF9A", "#1A2A22");
        maakAanKnop.setOnAction(e -> maakNieuweMap());
        nieuweMapVeld.setOnAction(e -> maakNieuweMap()); // Enter werkt ook

        HBox invoerRij = new HBox(10, nieuweMapVeld, maakAanKnop);
        invoerRij.setAlignment(Pos.CENTER_LEFT);

        VBox aanmaakVak = new VBox(8, nieuweMapLabel, invoerRij);
        aanmaakVak.setPadding(new Insets(12, 16, 12, 16));
        aanmaakVak.setStyle(
            "-fx-background-color: " + paneel + ";" +
            "-fx-border-color: " + accent + ";" +
            "-fx-border-width: 2 0 0 0;"
        );

        // ── Statusbalk ───────────────────────────────────────────────────
        statusLabel = new Label("Kies een map om te beginnen.");
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web(subtiel));
        statusLabel.setPadding(new Insets(8, 16, 10, 16));

        // ── Geselecteerde mappen tonen ────────────────────────────────────
        Button toonGeselecteerdKnop = new Button("📋 Toon aangevinkte mappen");
        stijlKnopKlein(toonGeselecteerdKnop, paneel, accentHelder);
        toonGeselecteerdKnop.setOnAction(e -> toonAangevinkteMappen());

        HBox onderBalk = new HBox(12, statusLabel, new Spacer(), toonGeselecteerdKnop);
        onderBalk.setAlignment(Pos.CENTER_LEFT);
        onderBalk.setPadding(new Insets(6, 12, 10, 12));

        // ── Samenstellen ──────────────────────────────────────────────────
        VBox root = new VBox(0, kopVak, lijstVak, aanmaakVak, onderBalk);
        root.setStyle("-fx-background-color: " + achtergrond + ";");

        Scene scene = new Scene(root, 540, 560);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(480);
        primaryStage.show();
    }

    // ── Map kiezen via bestandsdialoog ────────────────────────────────────
    private void kiesMap(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Kies een map om te verkennen");
        if (huidigePad != null) chooser.setInitialDirectory(huidigePad);
        File gekozen = chooser.showDialog(stage);
        if (gekozen != null) {
            huidigePad = gekozen;
            padLabel.setText(huidigePad.getAbsolutePath());
            laadMappen();
        }
    }

    // ── Submappen inladen ─────────────────────────────────────────────────
    private void laadMappen() {
        mappenLijst.clear();
        if (huidigePad == null || !huidigePad.isDirectory()) {
            toonStatus("❌ Ongeldig pad.", "#E07070");
            return;
        }

        File[] inhoud = huidigePad.listFiles(File::isDirectory);
        if (inhoud == null || inhoud.length == 0) {
            toonStatus("ℹ️ Geen submappen gevonden.", "#8880A0");
            return;
        }

        for (File map : inhoud) {
            mappenLijst.add(new MapItem(map.getName()));
        }

        toonStatus("✅ " + inhoud.length + " map(pen) geladen.", "#4CAF8A");
    }

    // ── Nieuwe map aanmaken ────────────────────────────────────────────────
    private void maakNieuweMap() {
        String naam = nieuweMapVeld.getText().trim();
        if (naam.isEmpty()) {
            toonStatus("⚠️ Voer een mapnaam in.", "#E0C060");
            return;
        }
        if (huidigePad == null) {
            toonStatus("⚠️ Kies eerst een bovenliggende map.", "#E0C060");
            return;
        }

        // Ongeldige tekens opsporen
        if (naam.matches(".*[\\\\/:*?\"<>|].*")) {
            toonStatus("❌ Mapnaam bevat ongeldige tekens.", "#E07070");
            return;
        }

        File nieuw = new File(huidigePad, naam);
        if (nieuw.exists()) {
            toonStatus("ℹ️ Map bestaat al: " + naam, "#8880A0");
            nieuweMapVeld.clear();
            return;
        }

        if (nieuw.mkdirs()) {
            toonStatus("✅ Map aangemaakt: " + naam, "#4CAF8A");
            nieuweMapVeld.clear();
            laadMappen(); // lijst verversen
        } else {
            toonStatus("❌ Aanmaken mislukt. Controleer rechten.", "#E07070");
        }
    }

    // ── Aangevinkte mappen tonen in dialoogvenster ─────────────────────────
    private void toonAangevinkteMappen() {
        List<String> aangevinkt = new ArrayList<>();
        for (MapItem item : mappenLijst) {
            if (item.aangevinkt.get()) aangevinkt.add(item.naam);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aangevinkte mappen");
        alert.setHeaderText(aangevinkt.isEmpty()
            ? "Geen mappen aangevinkt"
            : aangevinkt.size() + " map(pen) aangevinkt:");
        alert.setContentText(aangevinkt.isEmpty()
            ? "Vink mappen aan in de lijst."
            : String.join("\n", aangevinkt));
        alert.showAndWait();
    }

    private void toonStatus(String bericht, String kleur) {
        statusLabel.setText(bericht);
        statusLabel.setTextFill(Color.web(kleur));
    }

    // ── Hulpklassen ──────────────────────────────────────────────────────

    /** Stijl voor grote knoppen */
    private void stijlKnop(Button knop, String bg, String hover, String fg) {
        String basis = String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; " +
            "-fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-padding: 8 18 8 18; -fx-background-radius: 8; -fx-cursor: hand;", bg, fg);
        String hoverStijl = String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; " +
            "-fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-padding: 8 18 8 18; -fx-background-radius: 8; -fx-cursor: hand;", hover, fg);
        knop.setStyle(basis);
        knop.setOnMouseEntered(e -> knop.setStyle(hoverStijl));
        knop.setOnMouseExited(e -> knop.setStyle(basis));
    }

    /** Stijl voor kleine knoppen */
    private void stijlKnopKlein(Button knop, String bg, String fg) {
        String basis = String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; " +
            "-fx-font-size: 11px; -fx-padding: 4 12 4 12; " +
            "-fx-background-radius: 6; -fx-cursor: hand;", bg, fg);
        knop.setStyle(basis);
        knop.setOnMouseEntered(e -> knop.setStyle(basis.replace(bg, "#3A3A55")));
        knop.setOnMouseExited(e -> knop.setStyle(basis));
    }

    /** Lege ruimte-vulling voor HBox */
    static class Spacer extends Region {
        Spacer() { HBox.setHgrow(this, Priority.ALWAYS); }
    }
}
