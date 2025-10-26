// File: Main.java
package application.chesstrainerfx;

import application.pgnreader.io.PGNReader;
import application.pgnreader.model.Chapter;
import application.pgnreader.model.Exercise;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main extends Application {

    /** Alle beschikbare hoofdstukken (PGN-bestanden in resources) */
    private final List<String> chapterPaths = Arrays.asList(
            "/pgn/mating/chapters/1_epaulette_mate.pgn",
            "/pgn/mating/chapters/2_swallowstail_mate.pgn",
            "/pgn/mating/chapters/3_cozio_mate.pgn",
            "/pgn/mating/chapters/4_killerbox_mate.pgn",
            "/pgn/mating/chapters/5_triangle_mate.pgn",
            "/pgn/mating/chapters/6_railroad_mate.pgn",
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
            "/pgn/mating/chapters/33_two_night_mate.pgn",
            "/pgn/mating/chapters/34_suffocation_mate.pgn",
            "/pgn/mating/chapters/35_collabration_mate.pgn",
            "/pgn/mating/chapters/36_blackburne_mate.pgn"
    );

    private Scene scene;
    private Parent homeRoot;

    @Override
    public void start(Stage stage) {
        homeRoot = buildHome();
        scene = new Scene(homeRoot, 1500, 1000);
        scene.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());

        stage.setTitle("ChessTrainer — Home");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /** === Splash / Home scherm === */
    private Parent buildHome() {
        StackPane root = new StackPane();
        root.getStyleClass().add("splash-root");

        StackPane overlay = new StackPane();
        overlay.setBackground(new Background(
                new BackgroundFill(Color.color(0, 0, 0, 0.15), CornerRadii.EMPTY, Insets.EMPTY)));
        overlay.setEffect(new BoxBlur(8, 8, 2));

        HBox buttons = new HBox(12);
        buttons.setPadding(new Insets(24));
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        Button btnMate = makeTile("♛\nMating\nPatterns");
        Button btnTactics = makeTile("⚡\nTactics");
        Button btnPuzzles = makeTile("♟\nPuzzles");

        buttons.getChildren().addAll(btnMate, btnTactics, btnPuzzles);
        StackPane.setAlignment(buttons, Pos.BOTTOM_RIGHT);

        btnMate.setOnAction(e -> startTraining("MATE_PATTERNS"));
        btnTactics.setOnAction(e -> startTraining("TACTICS"));
        btnPuzzles.setOnAction(e -> startTraining("PUZZLES"));

        root.getChildren().addAll(overlay, buttons);
        return root;
    }

    private Button makeTile(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("tile");
        b.setPrefSize(140, 140);
        b.setWrapText(true);
        return b;
    }

    /** Router: bepaalt welk scherm geladen wordt */
    private void startTraining(String mode) {
        switch (mode) {
            case "MATE_PATTERNS" -> scene.setRoot(buildMatingPatterns());
            default -> showInfo("Nog niet beschikbaar", "Deze trainingscategorie is nog niet actief.");
        }
    }

    /** Bouwt alle hoofdstukken (PGN-bestanden -> Chapter objecten) */
    private List<Chapter> buildChapters(List<String> paths) {
        List<Chapter> chapters = new ArrayList<>();
        for (String path : paths) {
            List<Exercise> exercises = PGNReader.readChapter(path);
            if (!exercises.isEmpty()) {
                chapters.add(new Chapter(exercises.get(0).getTitle(), exercises, path));
            }
        }
        return chapters;
    }

    /** === View: Mating Patterns === */
    private Parent buildMatingPatterns() {
        List<Chapter> chapters = buildChapters(chapterPaths);
        List<String> titles = chapters.stream().map(Chapter::getTitle).toList();

        var view = new MatingPatternsView(titles, name -> {
            Chapter chapter = chapters.stream()
                    .filter(c -> c.getTitle().equals(name))
                    .findFirst()
                    .orElse(null);
            if (chapter != null) openChapterWindow(chapter);
        });

        Button back = new Button("← Back");
        back.setOnAction(e -> scene.setRoot(homeRoot));
        back.setStyle("""
            -fx-background-color: rgba(20,20,20,0.65);
            -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;
            -fx-padding: 6 12 6 12; -fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 8;
        """);

        StackPane wrapper = new StackPane(view, back);
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(16));
        return wrapper;
    }

    /** Venster met lijst van oefeningen in een hoofdstuk */
    private void openChapterWindow(Chapter chapter) {
        List<Exercise> exercises = chapter.getExercises();

        Label titleLbl = new Label(chapter.getTitle());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ListView<Exercise> listView = new ListView<>();
        listView.getItems().addAll(exercises);

        TextArea fenArea = new TextArea();
        fenArea.setEditable(false);
        TextArea movesArea = new TextArea();
        movesArea.setEditable(false);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, ex) -> {
            fenArea.setText(ex != null ? ex.getFen() : "");
            movesArea.setText(ex != null ? ex.getMoves() : "");
        });
        if (!exercises.isEmpty()) listView.getSelectionModel().selectFirst();

        VBox left = new VBox(8, new Label("Oefeningen"), listView);
        left.setPadding(new Insets(10));
        left.setPrefWidth(350);

        Button openBoardBtn = new Button("Open bord");
        openBoardBtn.setMaxWidth(Double.MAX_VALUE);

        VBox right = new VBox(10, new Label("FEN:"), fenArea,
                new Label("Zetten:"), movesArea, openBoardBtn);
        right.setPadding(new Insets(10));

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.4);

        VBox root = new VBox(10, titleLbl, split);
        root.setPadding(new Insets(12));

        Stage s = new Stage();
        s.setTitle(chapter.getTitle());
        s.setScene(new Scene(root, 900, 560));
        s.show();
    }

    private static void showInfo(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }

    public static void main(String[] args) { launch(args); }
}
