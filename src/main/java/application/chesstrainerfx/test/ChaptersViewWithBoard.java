package application.chesstrainerfx.test;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.utils.Position;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.view.BoardView;
import application.pgnreader.io.PGNReader;
import application.pgnreader.model.Exercise;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.util.regex.Pattern;

/**
 * ChaptersView die integreert met jouw BoardModel en BoardView.
 * - Toont per chapter een button.
 * - Per chapter-venster: commentaar (eerste oefening), lijst met oefeningen, rechts bord + navigatie.
 * - Speelt zetten af als UCI (e.g. "e2e4 e7e5 g1f3 ...").
 */
public class ChaptersViewWithBoard extends Application {

    // ----- PAS DIT AAN naar jouw echte chapter-bestanden -----
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
    // ----------------------------------------------------------

    @Override
    public void start(Stage stage) {
        FlowPane buttons = new FlowPane(Orientation.HORIZONTAL);
        buttons.setPadding(new Insets(12));
        buttons.setHgap(10);
        buttons.setVgap(10);

        for (String path : chapterPaths) {
            String label = toNiceTitle(path);
            Button b = new Button(label);
            b.setOnAction(e -> openChapterWindow(label, path));
            buttons.getChildren().add(b);
        }

        ScrollPane scroll = new ScrollPane(buttons);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 1100, 800);
        stage.setScene(scene);
        stage.setTitle("Chapters");
        stage.show();
    }

    private void openChapterWindow(String chapterTitle, String pgnPath) {
        List<Exercise> exercises = PGNReader.readChapter(pgnPath);

        Label titleLbl = new Label(chapterTitle);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextArea commentsArea = new TextArea();
        commentsArea.setEditable(false);
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(4);

        // Links: oefeningen
        ListView<Exercise> listView = new ListView<>();
        listView.getItems().addAll(exercises);
        VBox left = new VBox(8, new Label("Oefeningen"), listView);
        left.setPadding(new Insets(10));
        left.setPrefWidth(340);

        // Rechts: BoardView + navigatie
        Controller controller = new Controller(); // zie dummy-implementatie onderaan
        controller.setWhiteTurn(true);            // beginwaarde, kan uit FEN worden gelezen als je wilt

        BoardModel boardModel = new BoardModel();
        BoardView boardView = new BoardView(boardModel, controller, /*isWhitePerspective*/ true, 600);

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

        // Selectiehandler
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

        // standaard eerste selecteren
        if (!exercises.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        } else {
            commentsArea.setText("Geen oefeningen gevonden.");
        }

        // Navigatie
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

            // Omdat BoardModel geen echte undo heeft: reset en speel tot cursor-1
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
        s.setTitle(chapterTitle);
        s.setScene(new Scene(root, 1200, 900));
        s.show();
    }

    // --- Helpers ---

    // Reset: wis alle stukken, laad FEN, forceer redraw (BoardView.onBoardUpdated)
    private static void resetToFEN(BoardModel model, BoardView view, String fen) {
        for (SquareModel sq : model.getSquares()) {
            sq.setPiece(null);
        }
        model.initializeFromFEN(nullToEmpty(fen));
        view.onBoardUpdated();
    }

    // Past één UCI-zet toe met jouw BoardModel (zonder legality/regels; puur verplaatsen)
    private static boolean applyUCIMove(BoardModel model, String uci) {
        // formaat: e2e4 of e7e8q (promotieletter negeren we hier)
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

    private static int fileToCol(char file) {
        return file - 'a'; // a->0 ... h->7
    }

    private static int rankToRow(char rank) {
        int r = rank - '0'; // '1'..'8'
        // jouw board rows: 0 = bovenste rij (8), 7 = onderste rij (1)
        return 8 - r;
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

    private static List<String> tokenizeUCI(String raw) {
        if (raw == null) return Collections.emptyList();
        String s = raw.replaceAll("\\{[^}]*}", " "); // strip {commentaar}
        s = removeParenthesized(s);                  // strip (varianten)
        s = s.replaceAll("\\$\\d+", " ");            // strip NAGs
        s = s.replaceAll("\\b\\d+\\s*\\.\\.\\.|\\b\\d+\\s*\\.", " "); // strip zetnummers
        s = s.replaceAll("\\b(1-0|0-1|1/2-1/2|\\*)\\b", " "); // strip resultaat
        s = s.replaceAll("\\s+", " ").trim();

        if (s.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>();
        for (String t : s.split(" ")) {
            String tt = t.trim();
            if (tt.isEmpty()) continue;
            if (UCI_PATTERN.matcher(tt).matches()) out.add(tt);
            // SAN wordt genegeerd; wil je SAN ondersteunen, laat het weten.
        }
        return out;
    }

    // verwijder ( ... ) incl. geneste haakjes
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

    public static void main(String[] args) {
        launch(args);
    }

    // ----- MINI Controller zodat BoardView kan draaien -----
    // Pas aan / vervang door jouw echte Controller-klasse

}
