package application.chesstrainerfx;

import application.pgnreader.io.PGNReader;
import application.pgnreader.model.Exercise;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChapterView extends Application {

    // Alle chapter-paths hier netjes in een lijst
    private final List<String> chapterPaths = Arrays.asList(
            "/pgn/mating/chapters/1_epaulette_mate.pgn",
            "/pgn/mating/chapters/2_swallowstail_mate.pgn",
            "/pgn/mating/chapters/3_cozio_mate.pgn",
            "/pgn/mating/chapters/4_killerbox_mate.pgn",
            "/pgn/mating/chapters/5_triangle_mate.pgn",
            "/pgn/mating/chapters/6_triangle_mate.pgn",
            "/pgn/mating/chapters/7_maxlange_mate.pgn",
            "/pgn/mating/chapters/8_balestra_mate.pgn",
            "/pgn/mating/chapters/9_buckingbranco_mate.pgn",
            "/pgn/mating/chapters/10_sneakingstallion_mate.pgn",
            "/pgn/mating/chapters/11_damiano_mate.pgn",
            "/pgn/mating/chapters/12_loli_mate.pgn",
            "/pgn/mating/chapters/13_backrank_mate.pgn",
            "/pgn/mating/chapters/14_blindswine_mate.pgn",
            "/pgn/mating/chapters/15_lawnmower_mate.pgn",
            "/pgn/mating/chapters/16_hfile_mate.pgn",
            "/pgn/mating/chapters/17_opera_mate.pgn",
            "/pgn/mating/chapters/18_mayet_mate.pgn",
            "/pgn/mating/chapters/19_reti_mate.pgn",
            "/pgn/mating/chapters/20_pillsbury_mate.pgn",
            "/pgn/mating/chapters/21_morphy_mate.pgn",
            "/pgn/mating/chapters/22_creco_mate.pgn",
            "/pgn/mating/chapters/23_corner_mate.pgn",
            "/pgn/mating/chapters/24_anastasia_mate.pgn",
            "/pgn/mating/chapters/25_arabian_mate.pgn",
            "/pgn/mating/chapters/26_vukovic_mate.pgn",
            "/pgn/mating/chapters/27_hook_mate.pgn",
            "/pgn/mating/chapters/28_andersen_mate.pgn",
            "/pgn/mating/chapters/29_diagondal_corridor_mate.pgn",
            "/pgn/mating/chapters/30_bombardier_mate.pgn",
            "/pgn/mating/chapters/31_boden_mate.pgn",
            "/pgn/mating/chapters/32_smothered_mate.pgn",
            "/pgn/mating/chapters/33_boden_mate.pgn",
            "/pgn/mating/chapters/34_boden_mate.pgn",
            "/pgn/mating/chapters/35_collabration_mate.pgn",
            "/pgn/mating/chapters/36_collabration_mate.pgn"
    );

    @Override
    public void start(Stage stage) {
        // Container met scroll voor alle chapter-buttons
        FlowPane buttons = new FlowPane(Orientation.HORIZONTAL);
        buttons.setHgap(10);
        buttons.setVgap(10);
        buttons.setPadding(new Insets(12));

        for (String path : chapterPaths) {
            String btnLabel = toNiceTitle(path);
            Button b = new Button(btnLabel);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> openChapterWindow(btnLabel, path));
            buttons.getChildren().add(b);
        }

        ScrollPane scroll = new ScrollPane(buttons);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Chapters");
        stage.show();
    }

    private void openChapterWindow(String chapterTitle, String pgnPath) {
        List<Exercise> exercises;
        try {
            exercises = PGNReader.readChapter(pgnPath);
        } catch (Exception ex) {
            exercises = new ArrayList<>();
            ex.printStackTrace();
        }

        // Commentaar uit de eerste PGN (eerste Exercise)
        String firstComment = exercises.isEmpty() ? "Geen commentaar gevonden."
                : safe(exercises.get(0).getComments());

        Label chapterLbl = new Label(chapterTitle);
        chapterLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextArea commentsArea = new TextArea(firstComment);
        commentsArea.setEditable(false);
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(4);

        // Lijst met oefeningen links
        ListView<Exercise> listView = new ListView<>();
        listView.getItems().addAll(exercises);

        // Detailpane rechts: FEN + Moves
        Label fenLbl = new Label("FEN");
        TextArea fenArea = new TextArea();
        fenArea.setEditable(false);
        fenArea.setWrapText(true);

        Label movesLbl = new Label("Moves");
        TextArea movesArea = new TextArea();
        movesArea.setEditable(false);
        movesArea.setWrapText(true);

        VBox detailBox = new VBox(8, fenLbl, fenArea, movesLbl, movesArea);
        detailBox.setPadding(new Insets(10));

        // SplitPane: links list, rechts details
        SplitPane split = new SplitPane();
        VBox leftBox = new VBox(8, new Label("Oefeningen"), listView);
        leftBox.setPadding(new Insets(10));
        leftBox.setPrefWidth(300);

        split.getItems().addAll(leftBox, detailBox);
        split.setDividerPositions(0.33);

        // Klik op oefening => toon FEN + moves
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, ex) -> {
            if (ex != null) {
                fenArea.setText(safe(ex.getFen()));
                movesArea.setText(safe(ex.getMoves()));
            } else {
                fenArea.clear();
                movesArea.clear();
            }
        });

        VBox root = new VBox(10, chapterLbl, commentsArea, split);
        root.setPadding(new Insets(12));

        Stage s = new Stage();
        s.setTitle(chapterTitle);
        s.setScene(new Scene(root, 1000, 700));
        s.show();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // Maak van het pad een nette titel (bv. "1 Epaulette Mate")
    private static String toNiceTitle(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1).replace(".pgn", "");
        // "1_epaulette_mate" -> "1 Epaulette Mate"
        String[] parts = file.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" ");
            // hoofdletters
            sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
