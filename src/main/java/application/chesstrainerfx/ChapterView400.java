package application.chesstrainerfx;

import application.pgnreader.io.PGNReader;
import application.pgnreader.model.Exercise;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.util.regex.Pattern;

public class ChapterView400 extends Application {

    // --- pas aan naar jouw echte chapter-bestanden ---
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
    // ---------------------------------------------------

    @Override
    public void start(Stage stage) {
        // hoofdoverzicht: knoppen voor elk chapter
        FlowPane grid = new FlowPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(10);
        grid.setVgap(10);

        for (String path : chapterPaths) {
            Button b = new Button(toNiceTitle(path));
            b.setOnAction(e -> openChapterWindow(toNiceTitle(path), path));
            grid.getChildren().add(b);
        }

        ScrollPane sc = new ScrollPane(grid);
        sc.setFitToWidth(true);

        Scene scene = new Scene(sc, 1100, 800);
        stage.setScene(scene);
        stage.setTitle("Chapters (400)");
        stage.show();
    }

    private void openChapterWindow(String chapterTitle, String pgnPath) {
        List<Exercise> exercises = PGNReader.readChapter(pgnPath);

        Label titleLbl = new Label(chapterTitle);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextArea commentsArea = new TextArea();
        commentsArea.setEditable(false);
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(3);

        // Links: oefeningen
        ListView<Exercise> listView = new ListView<>();
        listView.getItems().addAll(exercises);
        VBox left = new VBox(8, new Label("Oefeningen"), listView);
        left.setPadding(new Insets(10));
        left.setPrefWidth(340);

        // Rechts: 400x400 board + navigatie
        Controller400 controller = new Controller400(); // mini-controller; vervang door jouw echte
        controller.setWhiteTurn(true);

        BoardModel boardModel = new BoardModel();
        BoardView400 boardView = new BoardView400(boardModel, controller, /*white perspective*/ true);

        Label idxLbl = new Label("Zet: –/–");
        Button startBtn = new Button("|<< Start");
        Button prevBtn  = new Button("<< Vorige");
        Button nextBtn  = new Button("Volgende >>");
        Button endBtn   = new Button("Einde >>|");

        HBox nav = new HBox(8, startBtn, prevBtn, nextBtn, endBtn, new Separator(), idxLbl);
        nav.setPadding(new Insets(8));

        VBox right = new VBox(10, boardView, nav);
        right.setPadding(new Insets(10));
        VBox.setVgrow(boardView, Priority.ALWAYS);

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.36);

        VBox root = new VBox(10, titleLbl, commentsArea, split);
        root.setPadding(new Insets(12));

        // Per-oefening state (FEN + UCI-lijst + cursor)
        class ExState {
            String fen;
            List<String> uciMoves = new ArrayList<>();
            int cursor = 0;
        }
        Map<Exercise, ExState> state = new HashMap<>();

        // selectie handler
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldEx, ex) -> {
            if (ex == null) return;

            commentsArea.setText(nullToEmpty(ex.getComments()));

            ExState st = state.computeIfAbsent(ex, k -> {
                ExState s = new ExState();
                s.fen = nullToEmpty(k.getFen());
                s.uciMoves = tokenizeUCI(k.getMoves());
                s.cursor = 0;
                return s;
            });

            resetToFEN(boardModel, boardView, st.fen);
            st.cursor = 0;
            idxLbl.setText("Zet: " + st.cursor + "/" + st.uciMoves.size());
        });

        if (!exercises.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        } else {
            commentsArea.setText("Geen oefeningen gevonden.");
        }

        // navigatie
        startBtn.setOnAction(e -> {
            Exercise ex = listView.getSelectionModel().getSelectedItem();
            if (ex == null) return;
            ExState st = state.get(ex);
            resetToFEN(boardModel, boardView, st.fen);
            st.cursor = 0;
            idxLbl.setText("Zet: " + st.cursor + "/" + st.uciMoves.size());
        });

        prevBtn.setOnAction(e -> {
            Exercise ex = listView.getSelectionModel().getSelectedItem();
            if (ex == null) return;
            ExState st = state.get(ex);
            if (st.cursor == 0) return;

            // geen undo → reset en speel tot cursor-1
            resetToFEN(boardModel, boardView, st.fen);
            int target = st.cursor - 1;
            for (int i = 0; i < target; i++) {
                if (!applyUCIMove(boardModel, st.uciMoves.get(i))) {
                    warn("Zet kan niet worden uitgevoerd", "UCI: " + st.uciMoves.get(i) + "\nIndex: " + i);
                    break;
                }
            }
            st.cursor = target;
            idxLbl.setText("Zet: " + st.cursor + "/" + st.uciMoves.size());
        });

        nextBtn.setOnAction(e -> {
            Exercise ex = listView.getSelectionModel().getSelectedItem();
            if (ex == null) return;
            ExState st = state.get(ex);
            if (st.cursor >= st.uciMoves.size()) return;

            String uci = st.uciMoves.get(st.cursor);
            if (!applyUCIMove(boardModel, uci)) {
                warn("Zet kan niet worden uitgevoerd", "UCI: " + uci + "\nIndex: " + st.cursor);
                return;
            }
            st.cursor++;
            idxLbl.setText("Zet: " + st.cursor + "/" + st.uciMoves.size());
        });

        endBtn.setOnAction(e -> {
            Exercise ex = listView.getSelectionModel().getSelectedItem();
            if (ex == null) return;
            ExState st = state.get(ex);

            resetToFEN(boardModel, boardView, st.fen);
            int i = 0;
            for (; i < st.uciMoves.size(); i++) {
                if (!applyUCIMove(boardModel, st.uciMoves.get(i))) {
                    warn("Zet kan niet worden uitgevoerd", "UCI: " + st.uciMoves.get(i) + "\nIndex: " + i);
                    break;
                }
            }
            st.cursor = i;
            idxLbl.setText("Zet: " + st.cursor + "/" + st.uciMoves.size());
        });

        Stage s = new Stage();
        s.setTitle(chapterTitle + " (400)");
        s.setScene(new Scene(root, 1100, 820));
        s.show();
    }

    // --- helpers ---

    private static void resetToFEN(BoardModel model, BoardView400 view, String fen) {
        // wis alles en laad fen
        for (SquareModel sq : model.getSquares()) {
            sq.setPiece(null);
        }
        model.initializeFromFEN(nullToEmpty(fen));
        view.onBoardUpdated(); // force redraw
    }

    private static boolean applyUCIMove(BoardModel model, String uci) {
        if (!UCI_PATTERN.matcher(uci).matches()) return false;

        int fromCol = fileToCol(uci.charAt(0));
        int fromRow = rankToRow(uci.charAt(1));
        int toCol   = fileToCol(uci.charAt(2));
        int toRow   = rankToRow(uci.charAt(3));

        Position from = new Position(fromRow, fromCol);
        Position to   = new Position(toRow, toCol);

        model.movePiece(from, to);
        return true;
    }

    private static final Pattern UCI_PATTERN = Pattern.compile("^[a-h][1-8][a-h][1-8][qrbn]?$");

    private static int fileToCol(char file) { return file - 'a'; }
    private static int rankToRow(char rank) { return 8 - (rank - '0'); }

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

    private static List<String> tokenizeUCI(String raw) {
        if (raw == null) return Collections.emptyList();
        String s = raw.replaceAll("\\{[^}]*}", " ");
        s = removeParenthesized(s);
        s = s.replaceAll("\\$\\d+", " ");
        s = s.replaceAll("\\b\\d+\\s*\\.\\.\\.|\\b\\d+\\s*\\.", " ");
        s = s.replaceAll("\\b(1-0|0-1|1/2-1/2|\\*)\\b", " ");
        s = s.replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>();
        for (String t : s.split(" ")) {
            String tt = t.trim();
            if (!tt.isEmpty() && UCI_PATTERN.matcher(tt).matches()) out.add(tt);
        }
        return out;
    }

    private static String removeParenthesized(String s) {
        StringBuilder out = new StringBuilder();
        int depth = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            else if (depth == 0) out.append(c);
        }
        return out.toString();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static void warn(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Let op");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }

    /*// Mini-controller zodat BoardView400 kan draaien (vervang door jouw echte)
    public static class Controller {
        private boolean setupMode = false;
        private boolean whiteTurn = true;
        public void toggleSetupMode() { setupMode = !setupMode; }
        public boolean isSetupMode() { return setupMode; }
        public void setSelectedPieceForSetup(Object piece) {}
        public boolean isWhiteTurn() { return whiteTurn; }
        public void setWhiteTurn(boolean whiteTurn) { this.whiteTurn = whiteTurn; }
    }*/

    public static void main(String[] args) {
        launch(args);
    }
}
