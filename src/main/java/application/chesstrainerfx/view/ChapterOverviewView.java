package application.chesstrainerfx.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

/** Tegel-overzicht met titels (hoofdstukken of sub-categorieën); klik geeft de titel door. */
public final class ChapterOverviewView extends BorderPane {

    private final TilePane grid = new TilePane(16, 16);
    private Consumer<String> onSelect = t -> {};

    public ChapterOverviewView(List<String> titles, Consumer<String> onSelect) {
        if (onSelect != null) this.onSelect = onSelect;
        getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());

        // Achtergrond
        StackPane bgLayer = new StackPane();
        ImageView bg = new ImageView(new Image(
                getClass().getResource("/images/background_chapters.png").toExternalForm()));
        bg.setPreserveRatio(true);
        bg.fitWidthProperty().bind(bgLayer.widthProperty());
        bg.fitHeightProperty().bind(bgLayer.heightProperty());
        bgLayer.getChildren().add(bg);

        // Grid
        grid.setStyle("-fx-background-color: transparent;");
        grid.setOpacity(0.5);
        grid.setPadding(new Insets(24));
        grid.setPrefTileWidth(160);
        grid.setPrefTileHeight(160);
        grid.setAlignment(Pos.TOP_LEFT);


        ScrollPane scroller = new ScrollPane(grid);
        scroller.setFitToWidth(true);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        StackPane stack = new StackPane(bgLayer, scroller);
        setCenter(stack);

        setTitles(titles);
    }

    public void setTitles(List<String> titles) {
        grid.getChildren().setAll(createTiles(titles));
    }

    /** Extra ruimte boven het grid, zodat overlay-knoppen niet over de tegels vallen. */
    public void setGridTopPadding(double top) {
        grid.setPadding(new Insets(top, 24, 24, 24));
    }

    private List<Node> createTiles(List<String> titles) {
        return titles.stream().map(title -> {
            Button b = new Button(title);
            b.getStyleClass().add("tile");
            b.setPrefSize(160, 160);
            b.setWrapText(true);
            b.setOnAction(e -> onSelect.accept(title));
            return (Node) b;
        }).toList();
    }
}
