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


    private Scene scene;       // één Scene
    private Parent homeRoot;   // bewaart het splash/home-root

    @Override
    public void start(Stage stage) {
        homeRoot = buildHome();                 // splash/home
        scene = new Scene(homeRoot, 1500, 1000);
        scene.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());

        stage.setTitle("ChessTrainer — Home");
        stage.setResizable(false);              // jij wilde vaste resolutie 1500x1000
        stage.setScene(scene);
        stage.show();
    }

    /* === Splash/home met 3 knoppen === */
    private Parent buildHome() {
        StackPane root = new StackPane();
        root.getStyleClass().add("splash-root"); // gebruikt je achtergrond-afbeelding via CSS

        StackPane overlay = new StackPane();
        overlay.setBackground(new Background(
                new BackgroundFill(Color.color(0, 0, 0, 0.15), CornerRadii.EMPTY, Insets.EMPTY)));
        overlay.setEffect(new BoxBlur(8, 8, 2));

        HBox buttons = new HBox(12);
        buttons.setPadding(new Insets(24));
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        Button btnMate    = makeTile("♛\nMating\nPatterns");
        Button btnTactics = makeTile("⚡\nTactics");
        Button btnPuzzles = makeTile("♟\nPuzzles");

        buttons.getChildren().addAll(btnMate, btnTactics, btnPuzzles);
        StackPane.setAlignment(buttons, Pos.BOTTOM_RIGHT);

        // Koppeling
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

    /* === Router === */
    private void startTraining(String mode) {
        switch (mode) {
            case "MATE_PATTERNS" -> scene.setRoot(buildMatingPatterns());
            case "TACTICS"       -> { /* TODO later */ }
            case "PUZZLES"       -> { /* TODO later */ }
            default              -> { /* no-op */ }
        }
    }

    private List<Chapter> buildChapters(List<String> chapterPaths){
        List<Chapter> chapters = new ArrayList<>();

        for (String path: chapterPaths){
            List<Exercise> exercises = PGNReader.readChapter(path);
            chapters.add(new Chapter(exercises.getFirst().getTitle(),exercises));

        }
        return chapters;
    }

    /* === View voor Mating Patterns === */
    private Parent buildMatingPatterns() {
        var titles = loadMatingPatternTitles(); // jouw provider
        var view = new MatingPatternsView(titles, name -> {
            System.out.println("Gekozen pattern: " + name);
            // TODO open les/detail
            openChapterWindow(name ,"/pgn/mating/chapters/1_epaulette_mate.pgn");
        });

        // “Terug”-knop linksboven
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

    private List<String> loadMatingPatternTitles() {

        List<String> titles = new ArrayList<>();
        for(Chapter chapter: buildChapters(chapterPaths)){
            //System.out.println(chapter.getTitle());
            for(Exercise exercise:chapter.getExercises()){
                System.out.println(exercise.getTitle());
                System.out.println(exercise.getComments());
                System.out.println(exercise.getFen());
                System.out.println(exercise.getMoves());
            }

        }

        for (String pngPath : chapterPaths) {

            List<Exercise> exercises = PGNReader.readChapter(pngPath);
            for (Exercise exercise : exercises) {
                if (exercise.getTitle().matches("^\\d.*")) {
                    System.out.println(exercise.getTitle());
                    titles.add(exercise.getTitle());
                }
            }
        }



        return titles;

    }
    private void openChapterWindow(String chapterTitle, String pgnPath) {
        List<Exercise> exercises = PGNReader.readChapter(pgnPath);

        Label titleLbl = new Label(chapterTitle);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Links: lijst met oefeningen
        ListView<Exercise> listView = new ListView<>();
        listView.getItems().addAll(exercises);
        VBox left = new VBox(8, new Label("Oefeningen"), listView);
        left.setPadding(new Insets(10));
        left.setPrefWidth(350);

        // Rechts: FEN + MOVES + knop
        Label fenLbl = new Label("FEN:");
        TextArea fenArea = new TextArea();
        fenArea.setEditable(false);
        fenArea.setWrapText(true);
        fenArea.setPrefRowCount(3);

        Label movesLbl = new Label("Moves (uit Exercise.getMoves()):");
        TextArea movesArea = new TextArea();
        movesArea.setEditable(false);
        movesArea.setWrapText(true);
        movesArea.setPrefRowCount(8);

        Button openBoardBtn = new Button("Open bord in venster");
        openBoardBtn.setMaxWidth(Double.MAX_VALUE);

        VBox right = new VBox(10, fenLbl, fenArea, movesLbl, movesArea, openBoardBtn);
        right.setPadding(new Insets(10));

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.40);

        VBox root = new VBox(10, titleLbl, split);
        root.setPadding(new Insets(12));

        // Bij selectie: FEN + MOVES tonen
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldEx, ex) -> {
            fenArea.setText(ex != null ? nullToEmpty(ex.getFen()) : "");
            movesArea.setText(ex != null ? nullToEmpty(ex.getMoves()) : "");
        });

        if (!exercises.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        }

        // Knop: open BoardView-venster met FEN
        openBoardBtn.setOnAction(e -> {
            Exercise ex = listView.getSelectionModel().getSelectedItem();
            if (ex == null) {
                showInfo("Geen oefening geselecteerd.", "Selecteer eerst een oefening.");
                return;
            }
            String fen = nullToEmpty(ex.getFen());
            boolean whiteToMove = parseSideToMoveFromFen(fen); // 'w' → true
            openBoardWindow(fen, chapterTitle + " – " + ex.getTitle(), whiteToMove);
        });

        Stage s = new Stage();
        s.setTitle(chapterTitle);
        s.setScene(new Scene(root, 900, 560));
        s.show();
    }
    /** Opent een nieuw venster met jouw bestaande BoardView en zet de stukken volgens FEN. */
    private void openBoardWindow(String fen, String windowTitle, boolean whiteToMove) {
        BoardModel boardModel = new BoardModel();
        Controller controller = new Controller();
        controller.setWhiteTurn(whiteToMove);

        BoardViewSetup boardView = new BoardViewSetup(boardModel, controller, whiteToMove,800);

        // Bord leeg → FEN laden → UI refresh
        for (SquareModel sq : boardModel.getSquares()) sq.setPiece(null);
        boardModel.initializeFromFEN(fen == null ? "" : fen);
        boardView.onBoardUpdated();

        VBox root = new VBox(boardView);
        root.setPadding(new Insets(10));

        Stage s = new Stage();
        s.setTitle(windowTitle);
        s.setScene(new Scene(root, 1500, 1000)); // afgestemd op jouw BoardView (~865x865 achtergrond)
        s.show();
    }

    // Helpers
    private static boolean parseSideToMoveFromFen(String fen) {
        try {
            String[] parts = fen.trim().split("\\s+");
            return parts.length >= 2 ? "w".equals(parts[1]) : true;
        } catch (Exception e) {
            return true;
        }
    }

    private static String toNiceTitle(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1).replace(".pgn", "");
        String[] parts = file.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static void showInfo(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }

    public static void main(String[] args) { launch(args); }
}
