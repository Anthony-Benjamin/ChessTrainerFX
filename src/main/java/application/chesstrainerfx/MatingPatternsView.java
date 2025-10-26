package application.chesstrainerfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class MatingPatternsView extends BorderPane {


    private final TilePane grid = new TilePane(16, 16);
    private Consumer<String> onSelect = t -> {};

    public MatingPatternsView(List<String> titles) {
        this(titles, null);
    }

    public MatingPatternsView(List<String> titles, Consumer<String> onSelect) {
        if (onSelect != null) this.onSelect = onSelect;
        getStylesheets().add(
                getClass().getResource("/splash.css").toExternalForm()
        );
        // === Achtergrondlaag met blur ===
        StackPane bgLayer = new StackPane();
        bgLayer.setStyle("-fx-background-color: black;"); // fallback kleur
        ImageView bg = new ImageView(new Image(
                getClass().getResource("/images/background_chapters.png").toExternalForm(),
                0, 0, true, true
        ));

        var bgUrl = getClass().getResource("/images/background_chapters.png");


        bg.setPreserveRatio(true);
        // laat de image meeschalen met het view
        bg.fitWidthProperty().bind(bgLayer.widthProperty());
        bg.fitHeightProperty().bind(bgLayer.heightProperty());
        bg.setEffect(new javafx.scene.effect.GaussianBlur(10)); // vaag
        bgLayer.getChildren().add(bg);

        // === Content (scrollbaar grid met tegels) ===
        grid.setStyle("-fx-background-color: transparent;");
        grid.setOpacity(0.5);
        grid.setPadding(new Insets(24));
        grid.setPrefTileWidth(160);
        grid.setPrefTileHeight(160);
        grid.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroller = new ScrollPane(grid);
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        scroller.setPannable(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        //scroller.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        // volledige transparantie, inclusief viewport
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroller.getContent().setStyle("-fx-background-color: transparent;");
        scroller.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
            var vp = scroller.lookup(".viewport");
            if (vp != null) vp.setStyle("-fx-background-color: transparent;");
        });


        // === Stapel achtergrond + content ===
        StackPane stack = new StackPane(bgLayer , scroller);
        setCenter(stack);

        // initial fill
        setTitles(titles);
    }

    /** Ververs de tegels met nieuwe titels */
    public void setTitles(List<String> titles) {
        grid.getChildren().setAll(createTiles(titles));
    }

    /** Optionele click-handler (bijv. open les) */
    public void setOnSelect(Consumer<String> onSelect) {
        if (onSelect != null) this.onSelect = onSelect;
    }

    private List<Node> createTiles(List<String> titles) {
        return titles.stream().map(title -> {
            Button b = new Button(title);
            b.getStyleClass().add("tile");            // zelfde stijl als splash
            b.setPrefSize(160, 160);                  // vierkant
            b.setWrapText(true);
            b.setOnAction(e -> onSelect.accept(title));
            return (Node) b;
        }).toList();
    }
}
