// File: MatingPatternsView.java
package application.chesstrainerfx.test;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

public final class MatePatternsView extends BorderPane {

    private final TilePane grid = new TilePane(16, 16);
    private Consumer<String> onSelect = t -> {};

    public MatePatternsView(List<String> titles, Consumer<String> onSelect) {
        if (onSelect != null) this.onSelect = onSelect;
        getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());

        // Achtergrond
        StackPane bgLayer = new StackPane();
        ImageView bg = new ImageView(new Image(
                getClass().getResource("/images/background_chapters.png").toExternalForm()));
        bg.setPreserveRatio(true);
        bg.fitWidthProperty().bind(bgLayer.widthProperty());
        bg.fitHeightProperty().bind(bgLayer.heightProperty());
        bg.setEffect(new GaussianBlur(10));
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
        scroller.setStyle("-fx-background-color: transparent;");
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        StackPane stack = new StackPane(bgLayer, scroller);
        setCenter(stack);

        setTitles(titles);
    }

    public void setTitles(List<String> titles) {
        grid.getChildren().setAll(createTiles(titles));
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
