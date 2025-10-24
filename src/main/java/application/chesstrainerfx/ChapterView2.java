package application.chesstrainerfx;

import application.pgnreader.io.PGNReader;
import application.pgnreader.model.Exercise;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class ChapterView2 extends Application {

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
        FlowPane buttons = new FlowPane();
        buttons.setHgap(10);
        buttons.setVgap(10);
        buttons.setPadding(new Insets(12));

        for (String path : chapterPaths) {
            String label = toNiceTitle(path);
            Button b = new Button(label);
            b.setOnAction(e -> openChapterWindow(label, path));
            buttons.getChildren().add(b);
        }

        ScrollPane sc = new ScrollPane(buttons);
        sc.setFitToWidth(true);

        Scene scene = new Scene(sc, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Chapters – FEN & Moves → BoardView");
        stage.show();
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

        BoardViewCopy boardView = new BoardViewCopy(boardModel, controller, whiteToMove);

        // Bord leeg → FEN laden → UI refresh
        for (SquareModel sq : boardModel.getSquares()) sq.setPiece(null);
        boardModel.initializeFromFEN(fen == null ? "" : fen);
        boardView.onBoardUpdated();

        VBox root = new VBox(boardView);
        root.setPadding(new Insets(10));

        Stage s = new Stage();
        s.setTitle(windowTitle);
        s.setScene(new Scene(root, 1400, 1000)); // afgestemd op jouw BoardView (~865x865 achtergrond)
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

    public static void main(String[] args) {
        launch(args);
    }
}
