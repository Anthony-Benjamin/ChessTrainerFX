package application.chesstrainerfx.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Aangepaste lijstcel met vinkje voor MapItem-objecten.
 */
public class MapCel extends ListCell<MapItem> {

    private final HBox inhoud = new HBox(12);
    private final CheckBox checkbox = new CheckBox();
    private final Text mapicoon = new Text("📁");
    private final Text naamText = new Text();

    private final String tekst;
    private final String paneel;
    private final String accent;
    private final String accentHelder;
    private final String subtiel;

    public MapCel(String tekst, String paneel, String accent, String accentHelder, String subtiel) {
        this.tekst = tekst;
        this.paneel = paneel;
        this.accent = accent;
        this.accentHelder = accentHelder;
        this.subtiel = subtiel;

        naamText.setFont(Font.font("Segoe UI", 13));
        mapicoon.setFont(Font.font(14));

        inhoud.getChildren().addAll(checkbox, mapicoon, naamText);
        inhoud.setAlignment(Pos.CENTER_LEFT);
        inhoud.setPadding(new Insets(6, 12, 6, 12));

        // Wissel achtergrond bij hover
        inhoud.setOnMouseEntered(e -> {
            if (!isEmpty()) {
                inhoud.setStyle("-fx-background-color: " + paneel + "; -fx-background-radius: 6;");
            }
        });
        inhoud.setOnMouseExited(e -> {
            if (!isEmpty()) {
                setNormaalStijl(getItem());
            }
        });
    }

    @Override
    protected void updateItem(MapItem item, boolean empty) {
        super.updateItem(item, empty);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");

        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        naamText.setText(item.naam);
        naamText.setFill(Color.web(tekst));

        // Koppel checkbox aan property (voorkomen dubbele listeners)
        checkbox.selectedProperty().unbindBidirectional(item.aangevinkt);
        checkbox.setSelected(item.aangevinkt.get());
        checkbox.selectedProperty().bindBidirectional(item.aangevinkt);

        // Kleur de tekst als aangevinkt
        item.aangevinkt.addListener((obs, oud, nieuw) -> setNormaalStijl(item));

        setNormaalStijl(item);
        setGraphic(inhoud);
    }

    private void setNormaalStijl(MapItem item) {
        if (item != null && item.aangevinkt.get()) {
            naamText.setFill(Color.web(accentHelder));
            inhoud.setStyle("-fx-background-color: transparent;");
        } else {
            naamText.setFill(Color.web(tekst));
            inhoud.setStyle("-fx-background-color: transparent;");
        }
    }
}
