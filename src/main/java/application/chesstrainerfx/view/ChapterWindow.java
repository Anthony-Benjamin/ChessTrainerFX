package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.utils.ExerciseSessionBuilder;
import application.pgnreader.model.Exercise;
import javafx.beans.binding.Bindings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterWindow extends BorderPane implements BoardChangeListener {


//    private Controller controller;
    private Controller controller = new Controller();
    private String[] parts;
    private enum Mode { LIST, BOARD }

    private final List<Exercise> exercises;
    private final String chapterTitle;
    private final Consumer<Void> onBack;
    private final ExerciseSessionBuilder exerciseSessionBuilder = new ExerciseSessionBuilder();
    private Mode mode = Mode.LIST;


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

        if (theoryText == null) theoryText = "";

        // Normaliseer line endings
        theoryText = theoryText.replace("\r\n", "\n");

        // Forceer wrapping binnen regels door lange stukken te splitsen op spaties
        theoryText = theoryText.replaceAll("(?<=\\S)(?=\\p{Lu})", " "); // voeg spaties toe voor hoofdletters
        theoryText = theoryText.replaceAll("\\s+", " "); // dubbele spaties weg

        theoryLabel = new Label(theoryText);
        theoryLabel.setWrapText(true);
        theoryLabel.setStyle("""
            -fx-text-fill: #f5deb3;
            -fx-font-size: 15px;
        """);
        int widthLabel =900;

        theoryLabel.setMaxWidth(widthLabel);
        theoryLabel.setPrefWidth(widthLabel);

        theoryScroll = new ScrollPane(theoryLabel);
        theoryScroll.setFitToWidth(true);
        theoryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        theoryScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        theoryScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        theoryScroll.setMaxHeight(140);
        theoryScroll.setPrefHeight(120);
        theoryScroll.setPrefViewportWidth(widthLabel);
        theoryScroll.setMaxWidth(widthLabel);
        theoryScroll.setPrefWidth(widthLabel);

        Region theoryBg = new Region();
        theoryBg.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(0,0,0,0.40)),
                        new Stop(1, Color.color(0,0,0,0.08))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        VBox theoryBox = new VBox(8, headerRow, theoryScroll);
        theoryBox.setMaxWidth(widthLabel);  // belangrijke toevoeging


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
        // ------------------------------------------------------------
        // 1) Reset state voor nieuwe exercise
        // ------------------------------------------------------------
        controller.resetMoveCounter();

        // ------------------------------------------------------------
        // 2) Pak FEN en maak een nieuw BoardModel + Controller
        //    (Belangrijk: eerst model initten, dan pas BoardView bouwen)
        // ------------------------------------------------------------
        String fen = (ex.getFen() == null) ? "" : ex.getFen().trim();

        BoardModel boardModel = new BoardModel();
        boardModel.initializeFromFEN(fen);

        controller = new Controller();
        controller.syncTurnFromFEN(fen);              // zet whiteTurn op basis van "w" / "b" in FEN
        controller.setExerciseStage(Controller.ExerciseStage.PLAYER_TO_MOVE);
        controller.resetMoveCounter();
        // Listener (optioneel; jij gebruikt dit voor moveCounter/hints)
        boardModel.addListener(this);

        // ------------------------------------------------------------
        // 3) Bouw ExerciseSession uit FEN + PGN moves (zonder variaties)
        //    -> deze methode is jouw "Test-logica"
        //    LET OP: buildSessionFromExercise(ex) moet in ChapterWindow bestaan
        // ------------------------------------------------------------
        controller.setExerciseSession(
                exerciseSessionBuilder.buildSessionFromExercise(ex)
        );
        // ------------------------------------------------------------
        // 4) Bouw BoardView (met correcte side-to-move)
        // ------------------------------------------------------------
        boardView = new BoardView(boardModel, controller, controller.isWhiteTurn(), 600);

        // ------------------------------------------------------------
        // 5) Rechts: Moves ListView (UI mag blijven zoals jij het had)
        // ------------------------------------------------------------
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

        String cssPath = getClass().getResource("/listview-style.css").toExternalForm();
        movesList.getStylesheets().add(cssPath);
        //movesList.setPrefWidth(200);
        movesList.setPrefWidth(320);
        movesList.setMinWidth(320);



        movesList.setCellFactory(lv -> new ListCell<>() {
            private final Label lbl = new Label();

            {
                lbl.setWrapText(false);
                lbl.setStyle("-fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 14px;");
                // Belangrijk: label maxWidth volgt de ListView breedte (minus scrollbar/padding)
                lbl.maxWidthProperty().bind(lv.widthProperty().subtract(35));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lbl.setText(item);
                    setGraphic(lbl);
                }
            }
        });

        // Vul de zichtbare moves-lijst (alleen voor UI / debug)
        fillMoves(ex.getMoves());
        System.out.println("moves: " + ex.getMoves());
        System.out.println("MovesList size = " + movesList.getItems().size());
        if (!movesList.getItems().isEmpty()) {
            movesList.getSelectionModel().select(0);
        }
        movesList.setVisible(false);

        // ------------------------------------------------------------
        // 6) Buttons: Show/Hide moves + Hint
        // ------------------------------------------------------------
        Button showHideMovesBtn = new Button("Show moves!");
        showHideMovesBtn.setPrefHeight(30);
        showHideMovesBtn.setStyle("-fx-background-color: #d7b77e; -fx-background-radius: 8;");

        showHideMovesBtn.textProperty().bind(
                Bindings.when(movesList.visibleProperty())
                        .then("Hide moves!")
                        .otherwise("Show moves!")
        );

        Button btnHint = new Button("Hint");
        btnHint.setVisible(true);

        Label lblHint = new Label();
        lblHint.setTextFill(Color.WHEAT);

        // Hint: toon de volgende verwachte zet uit session
        // (simpel; later kun je dit mooier maken, bv. highlight squares)
        btnHint.setOnAction(e -> {
            String san = controller.getExpectedSan();
            if (san != null) {
                lblHint.setText("Next: " + san);
            } else {
                lblHint.setText("Exercise finished.");
            }
        });


        // Toggle on click
        showHideMovesBtn.setOnAction(e -> {
            movesList.setVisible(!movesList.isVisible());
            // Hint knop alleen tonen wanneer moves verborgen zijn (jouw oude logica)
            btnHint.setVisible(!movesList.isVisible());
        });

        // ------------------------------------------------------------
        // 7) Layout: board + moveBox rechts
        // ------------------------------------------------------------
        VBox moveBox = new VBox(22);
        moveBox.setPadding(new Insets(32, 0, 0, 0));
        moveBox.getChildren().setAll(showHideMovesBtn, movesList, btnHint, lblHint);
        moveBox.setPrefWidth(320);
        moveBox.setMinWidth(320);
        HBox row = new HBox(30, boardView, moveBox);
        row.setAlignment(Pos.CENTER_LEFT);

        // Zet de BOARD view in het center-stack en switch mode
        boardPane.getChildren().setAll(row);
        switchMode(Mode.BOARD);
    }


    @Override
    public void onBoardUpdated() {
        controller.incrementMoveCounter();
        System.out.println(controller.getMoveCounter());
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
        if (moveString == null) return;

        // --- Debug: check input ---
        System.out.println("RAW moveString: [" + moveString + "]");

        // gebruik nu de util
        String clean = PgnUtils.cleanMoveString(moveString);

        // --- Debug: check cleaned ---
        System.out.println("CLEAN: [" + clean + "]");

        if (clean.isBlank()) return;


        //  Split per zetnummer. Fallback als split niets oplevert.
        Pattern moveBlockPattern = Pattern.compile("\\d+\\.(?:\\.\\.)?\\s.*?(?=\\d+\\.(?:\\.\\.)?\\s|$)");
        Matcher matcher = moveBlockPattern.matcher(clean);

        int added = 0;
        while (matcher.find()) {
            String part = matcher.group().trim();
            if (!part.isEmpty()) {
                movesList.getItems().add(part);
                added++;
            }
        }

        System.out.println("Size of parts " + added);
        System.out.println("Parts: " + movesList.getItems());

        // 9) Fallback: als regex om wat voor reden dan ook niets vond,
        //    dan tonen we gewoon de hele string als één item:
        if (added == 0) {
            movesList.getItems().add(clean);
        }

        System.out.println("MovesList size = " + movesList.getItems().size());
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

    static String removeVariations(String pgn){
        StringBuilder result = new StringBuilder();
        int depth = 0;

        System.out.println(pgn.toCharArray());

        for(char c : pgn.toCharArray()){
            if(c == '('){
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0) {
                result.append(c);
            }

        }
        return result.toString();
    }

}
