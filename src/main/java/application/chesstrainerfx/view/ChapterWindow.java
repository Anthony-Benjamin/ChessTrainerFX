package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.Position;
import application.pgnreader.model.Exercise;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class ChapterWindow extends BorderPane {

    private enum Mode { LIST, BOARD }

    private final List<Exercise> exercises;
    private final String chapterTitle;
    private final Consumer<Void> onBack;

    // UI onderdelen
    private Button backBtn;
    private Label titleLabel;
    private ScrollPane theoryScroll;
    private Label theoryLabel;

    private final StackPane centerStack = new StackPane(); // stapelt LIST en BOARD
    private TilePane tilesGrid;    // LIST
    private ScrollPane tilesScroll; // LIST (scrollbaar)

    private VBox boardPane;        // BOARD (bord + moves)
    private BoardView boardView;
    private ListView<String> movesList;

    private Mode mode = Mode.LIST;

    public ChapterWindow(String chapterTitle, List<Exercise> exercises, Consumer<Void> onBack) {
        this.exercises = exercises;
        this.chapterTitle = chapterTitle;
        this.onBack = onBack;

        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent;");
        this.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());
        buildLayout();
        switchMode(Mode.LIST);
    }

    private void buildLayout() {
        // === Achtergrondfoto (onderlaag) ===
        StackPane rootStack = new StackPane();
        rootStack.setBackground(Background.EMPTY);


        var bgUrl = getClass().getResource("/images/background_chapters_blur.png");
        ImageView bg = new ImageView(new Image(bgUrl.toExternalForm()));
        bg.setPreserveRatio(false);
        bg.setSmooth(true);

        //older pc slow
        rootStack.setCache(true);
        rootStack.setCacheHint(javafx.scene.CacheHint.SPEED);
        bg.setCache(true);
        bg.setCacheHint(javafx.scene.CacheHint.SPEED);
        centerStack.setCache(true);
        centerStack.setCacheHint(javafx.scene.CacheHint.SPEED);

       
        bg.fitWidthProperty().bind(rootStack.widthProperty());
        bg.fitHeightProperty().bind(rootStack.heightProperty());
        bg.setMouseTransparent(true);

        // === Bovenlaag content ===
        BorderPane content = new BorderPane();
        content.setBackground(Background.EMPTY);

        buildHeader(content);
        buildListCenter();   // LIST-laag
        buildBoardCenter();  // BOARD-laag (onzichtbaar tot klik)

        // stapel list + board in center
        centerStack.getChildren().setAll(tilesScroll, boardPane);
        content.setCenter(centerStack);

        rootStack.getChildren().addAll(bg, content);
        setCenter(rootStack);
    }

    /* ---------- TOP: header + scrollbare theorie ---------- */
    private void buildHeader(BorderPane parent) {
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        backBtn = new Button("← Back");
        backBtn.setStyle("""
                -fx-background-color: rgba(20,20,20,0.65);
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-padding: 6 12 6 12;
                -fx-border-color: rgba(255,255,255,0.35);
                -fx-border-radius: 8;
        """);
        backBtn.setOnAction(e -> {
            if (mode == Mode.BOARD) {
                switchMode(Mode.LIST);  // terug naar exercises-overzicht
            } else {
                onBack.accept(null);    // terug naar Mating Patterns
            }
        });

        titleLabel = new Label(chapterTitle);
        titleLabel.setStyle("-fx-text-fill: beige; -fx-font-size: 20px; -fx-font-weight: bold;");


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(backBtn, titleLabel, spacer);

        String theoryText = exercises.isEmpty() ? "" : exercises.getFirst().getComments();
        theoryLabel = new Label(theoryText == null ? "" : theoryText);
        theoryLabel.setWrapText(true);
        theoryLabel.setStyle("""
            -fx-text-fill: #f5deb3;
            -fx-font-size: 15px;
        """);
        theoryLabel.setMaxWidth(600);
        theoryLabel.setPrefWidth(600);

        theoryScroll = new ScrollPane(theoryLabel);
        theoryScroll.setFitToWidth(true);
        theoryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        theoryScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        theoryScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        theoryScroll.setMaxHeight(140);
        theoryScroll.setPrefHeight(120);
        theoryScroll.setPrefViewportWidth(600);
        theoryScroll.setMaxWidth(600);
        theoryScroll.setPrefWidth(600);

        Region theoryBg = new Region();
        theoryBg.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(0,0,0,0.40)),
                        new Stop(1, Color.color(0,0,0,0.08))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        VBox theoryBox = new VBox(8, headerRow, theoryScroll);
        theoryBox.setMaxWidth(600);  // belangrijke toevoeging


        StackPane topStack = new StackPane(
                theoryBg,
               theoryBox
        );
        topStack.setPadding(new Insets(16, 24, 12, 24));
        topStack.setAlignment(Pos.CENTER_LEFT);

        parent.setTop(topStack);
    }

    /* ---------- CENTER: LIST (tegeloverzicht van exercises) ---------- */
    private void buildListCenter() {
        tilesGrid = new TilePane(16, 16);
        tilesGrid.setPadding(new Insets(24));
        tilesGrid.setPrefTileWidth(160);
        tilesGrid.setPrefTileHeight(160);
        tilesGrid.setAlignment(Pos.TOP_LEFT);
        tilesGrid.setStyle("-fx-background-color: transparent;");

        for (Exercise ex : exercises) {
            Button b = new Button(ex.getTitle());
            b.getStyleClass().add("tile");
            b.setPrefSize(160, 160);
            b.setWrapText(true);
            b.setOnAction(e -> showExercise(ex));
            tilesGrid.getChildren().add(b);
        }

        tilesScroll = new ScrollPane(tilesGrid);
        tilesScroll.setFitToWidth(true);
        tilesScroll.setPannable(true);
        tilesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tilesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tilesScroll.setBackground(Background.EMPTY);
        tilesScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        tilesScroll.getContent().setStyle("-fx-background-color: transparent;");
        tilesScroll.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            var vp = tilesScroll.lookup(".viewport");
            if (vp != null) vp.setStyle("-fx-background-color: transparent;");
        });
    }

    /* ---------- CENTER: BOARD (bord + moves rechts) ---------- */
    private void buildBoardCenter() {
        boardPane = new VBox(12);
        boardPane.setAlignment(Pos.TOP_CENTER);
//        boardPane.setPadding(new Insets(10, 24, 24, 24));
        boardPane.setPadding(new Insets(10, 24, 24, 24));
        boardPane.setStyle("-fx-background-color: transparent;");
        boardPane.setVisible(false);  // start onzichtbaar
        boardPane.setManaged(false);
    }

    private void showExercise(Exercise ex) {
        // Bouw bord
        String fen = ex.getFen() == null ? "" : ex.getFen().trim();


        BoardModel boardModel = new BoardModel();
        Controller controller = new Controller();
        controller.syncTurnFromFEN(fen);

        boardView = new BoardView(boardModel, controller, true, 600);

        // leeg → FEN → refresh
        for (SquareModel sq : boardModel.getSquares()) sq.setPiece(null);
        boardModel.initializeFromFEN(fen);
        boardView.onBoardUpdated();

        // Moves rechts
        movesList = new ListView<>();
        movesList.setStyle("""
            -fx-background-color: rgba(20,10,5,0.55);
            -fx-control-inner-background: transparent;
            -fx-text-fill: white;
            -fx-font-family: 'Consolas';
            -fx-font-size: 14px;
            -fx-border-color: rgba(255,255,255,0.2);
            -fx-border-radius: 6;
        """);
        // --- CSS Toepassen ---

        // Laad het CSS-bestand (zorg ervoor dat het in dezelfde map zit als je gecompileerde klassen)
        String cssPath = getClass().getResource("/listview-style.css").toExternalForm();

        // Pas het CSS-bestand toe op de ListView
        movesList.getStylesheets().add(cssPath);
        movesList.setPrefWidth(300);
        fillMoves(ex.getMoves());
        // Eerste item in list krijgt de focus;
        movesList.getSelectionModel().select(0);

        HBox row = new HBox(30, boardView, movesList);
        row.setAlignment(Pos.CENTER_LEFT);

        boardPane.getChildren().setAll(row);
        switchMode(Mode.BOARD);
    }

    private void switchMode(Mode m) {
        mode = m;
        boolean list = (m == Mode.LIST);

        tilesScroll.setVisible(list);
        tilesScroll.setManaged(list);

        boardPane.setVisible(!list);
        boardPane.setManaged(!list);

        // Back-knop label (optioneel: je kunt ook tekst wisselen)
        // backBtn.setText(list ? "← Back" : "← Exercises");
    }

    private void fillMoves(String moveString) {
        movesList.getItems().clear();
        if (moveString == null || moveString.isBlank()) return;

        String clean = moveString
                .replaceAll("\\s+", " ")
                .replace("{", " ").replace("}", " ")
                .replaceAll("(?i)\\b(1-0|0-1|1/2-1/2|\\*)\\b", " ")
                .trim();

        String[] parts = clean.split("(?=\\b\\d+\\.(?:\\.\\.)?\\s)");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                movesList.getItems().add(trimmed);
            }
        }
    }

    private static boolean parseSideToMoveFromFen(String fen) {
        try {
            String[] parts = fen.trim().split("\\s+");
            return parts.length >= 2 && "w".equals(parts[1]);
        } catch (Exception e) {
            return true;
        }
    }

    /** Optioneel: eigen venster */
    public void showInStage(Stage owner) {
        Scene scene = new Scene(this, 1500, 1000);
        scene.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());
        Stage stage = new Stage();
        stage.setTitle(chapterTitle);
        stage.setScene(scene);
        stage.initOwner(owner);
        stage.show();
    }
}
