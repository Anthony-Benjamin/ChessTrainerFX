package application.chesstrainerfx;

import application.chesstrainerfx.view.BoardView;
import application.pgnreader.model.Chapter;
import application.pgnreader.model.Exercise;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public final class ChapterWindow extends BorderPane {

    private enum Mode { LIST, BOARD }

    private final Chapter chapter;
    private final TilePane grid = new TilePane(16, 16);
    private final Label titleLbl = new Label();
    private final TextArea theoryArea = new TextArea();
    private final Button backBtn = new Button("← Exercises");
    private final StackPane centerStack = new StackPane();

    // board subviews (lazy gemaakt)
    private VBox boardPane = null;

    public ChapterWindow(Chapter chapter) {
        this.chapter = chapter;
        getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());

        buildBackground();
        buildHeader();
        buildGrid();
        buildCenter();

        fillFromChapter(chapter);
        switchMode(Mode.LIST);
    }

    private void buildBackground() {
        // Achtergrond met blur
        StackPane bgLayer = new StackPane();
        var bgUrl = getClass().getResource("/images/background_chapters.png");
        ImageView bg = new ImageView(new Image(bgUrl.toExternalForm()));
        bg.setPreserveRatio(false);
        bg.fitWidthProperty().bind(centerStack.widthProperty());
        bg.fitHeightProperty().bind(centerStack.heightProperty());
        bg.setEffect(new GaussianBlur(18));
        bg.setMouseTransparent(true);

        bgLayer.getChildren().add(bg);
        centerStack.getChildren().add(bgLayer);
        setCenter(centerStack);
    }

    private void buildHeader() {
        titleLbl.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 12, 0.25, 0, 2);
        """);

        theoryArea.setEditable(false);
        theoryArea.setWrapText(true);
        theoryArea.setOpacity(0.93); // subtiel transparant
        theoryArea.setStyle("""
            -fx-control-inner-background: rgba(20,20,20,0.55);
            -fx-text-fill: white;
            -fx-highlight-fill: rgba(255,255,255,0.2);
            -fx-highlight-text-fill: white;
            -fx-background-insets: 0;
            -fx-background-radius: 8;
            -fx-border-color: rgba(255,255,255,0.28);
            -fx-border-radius: 8;
            -fx-font-size: 14px;
        """);

        backBtn.setVisible(false);
        backBtn.setOnAction(e -> switchMode(Mode.LIST));
        backBtn.setStyle("""
            -fx-background-color: rgba(20,20,20,0.65);
            -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;
            -fx-padding: 6 12 6 12; -fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 8;
        """);

        HBox headerTop = new HBox(10, backBtn, titleLbl);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(8, headerTop, theoryArea);
        header.setPadding(new Insets(16, 16, 8, 16));
        header.setBackground(new Background(new BackgroundFill(Color.color(0,0,0,0.08), CornerRadii.EMPTY, Insets.EMPTY)));

        setTop(header);
    }

    private void buildGrid() {
        grid.setPadding(new Insets(24));
        grid.setPrefTileWidth(160);
        grid.setPrefTileHeight(160);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setStyle("-fx-background-color: transparent;");
    }

    private void buildCenter() {
        // Scrollpane transparant boven de achtergrond
        ScrollPane scroller = new ScrollPane(grid);
        scroller.setFitToWidth(true);
        scroller.setPannable(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroller.getContent().setStyle("-fx-background-color: transparent;");
        scroller.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            var vp = scroller.lookup(".viewport");
            if (vp != null) vp.setStyle("-fx-background-color: transparent;");
        });

        centerStack.getChildren().add(scroller); // grid-laag boven de background
    }

    private void fillFromChapter(Chapter chapter) {
        // Titel
        titleLbl.setText(chapter.getTitle());

        // Theorie = comments van eerste exercise (als aanwezig)
        String theory = "";
        List<Exercise> exs = chapter.getExercises();
        if (!exs.isEmpty()) {
            var first = exs.get(0);
            if (first.getComments() != null && !first.getComments().isBlank()) {
                theory = first.getComments();
            }
        }
        if (theory.isBlank()) theory = "Geen theorie beschikbaar voor dit hoofdstuk.";
        theoryArea.setText(theory);

        // Tegels opbouwen
        grid.getChildren().setAll(exs.stream().map(this::makeExerciseTile).toList());
    }

    private Node makeExerciseTile(Exercise ex) {
        Button b = new Button(ex.getTitle());
        b.getStyleClass().add("tile");
        b.setPrefSize(160, 160);
        b.setWrapText(true);
        b.setOnAction(e -> showBoardFor(ex));
        return b;
    }

    private void showBoardFor(Exercise ex) {
        // Lazy init van boardPane
        if (boardPane == null) {
            boardPane = new VBox(10);
            boardPane.setPadding(new Insets(10));
            boardPane.setAlignment(Pos.TOP_LEFT);
            boardPane.setStyle("-fx-background-color: transparent;");

            // placeholder; echte board wordt per exercise (her)gemaakt
        }

        // Maak/ vernieuw het board voor deze exercise
        boardPane.getChildren().setAll(createBoardNodeFor(ex));

        // Toon board-laag bovenop
        if (!centerStack.getChildren().contains(boardPane)) {
            centerStack.getChildren().add(boardPane);
        }
        switchMode(Mode.BOARD);
    }

    private Node createBoardNodeFor(Exercise ex) {
        String fen = ex.getFen() == null ? "" : ex.getFen().trim();
        boolean whiteToMove = parseSideToMoveFromFen(fen);

        // Bouw jouw bord op
        BoardModel boardModel = new BoardModel();
        Controller controller = new Controller();
        controller.setWhiteTurn(whiteToMove);

        // De BoardViewSetup heeft jij al in je project
        int boardSize = 400; // pas aan naar jouw ideale maat
        BoardView boardView = new BoardView(boardModel, controller, whiteToMove, boardSize);

        // Bord leeg, dan FEN laden, dan UI verversen
        for (SquareModel sq : boardModel.getSquares()) sq.setPiece(null);
        boardModel.initializeFromFEN(fen);
        boardView.onBoardUpdated();

        // Optioneel: klein infoveldje eronder met moves/comment
        Label name = new Label(ex.getTitle());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        TextArea meta = new TextArea();
        meta.setEditable(false);
        meta.setWrapText(true);
        meta.setOpacity(0.9);
        meta.setStyle("""
            -fx-control-inner-background: rgba(20,20,20,0.55);
            -fx-text-fill: white;
            -fx-background-insets: 0;
            -fx-background-radius: 8;
            -fx-border-color: rgba(255,255,255,0.28);
            -fx-border-radius: 8;
            -fx-font-size: 13px;
        """);
        StringBuilder sb = new StringBuilder();
        if (ex.getFen() != null && !ex.getFen().isBlank()) sb.append("FEN: ").append(ex.getFen()).append("\n");
        if (ex.getMoves() != null && !ex.getMoves().isBlank()) sb.append("Moves: ").append(ex.getMoves()).append("\n");
        if (ex.getComments() != null && !ex.getComments().isBlank()) sb.append("Comment: ").append(ex.getComments());
        meta.setText(sb.toString());

        VBox box = new VBox(8, name, boardView, meta);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(16,16,24,16));
        box.setStyle("-fx-background-color: transparent;");

        return box;
    }

    private void switchMode(Mode m) {
        boolean list = (m == Mode.LIST);
        backBtn.setVisible(!list);

        // Toon/ verberg lagen (grid = tweede child; boardPane = derde child)
        if (centerStack.getChildren().size() >= 2) {
            Node gridLayer = centerStack.getChildren().get(1);
            gridLayer.setVisible(list);
            gridLayer.setManaged(list);
        }
        if (boardPane != null) {
            boardPane.setVisible(!list);
            boardPane.setManaged(!list);
        }
    }

    /** kleine helper */
    private static boolean parseSideToMoveFromFen(String fen) {
        try {
            String[] p = fen.trim().split("\\s+");
            return p.length >= 2 && "w".equals(p[1]);
        } catch (Exception e) {
            return true;
        }
    }
}
