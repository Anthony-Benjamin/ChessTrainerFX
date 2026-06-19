package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.ChapterPresenter;
import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.ExerciseSessionBuilder;
import application.pgnreader.model.Exercise;
import javafx.beans.binding.Bindings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;

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

    private enum Mode {LIST, BOARD}

    private final List<Exercise> exercises;
    private final String chapterTitle;
    private final Consumer<Void> onBack;
    private final ExerciseSessionBuilder exerciseSessionBuilder = new ExerciseSessionBuilder();
    private Mode mode = Mode.LIST;
    private boolean skipListOnBack = false;
    private ChapterPresenter presenter;

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
    private Label statusLabel;   // "Mat! 🎉" recht naast het bord

    private final Stage stage;

    public ChapterWindow(String chapterTitle, List<Exercise> exercises, Consumer<Void> onBack, Stage stage) {
        this.exercises = exercises;
        this.chapterTitle = chapterTitle;
        this.onBack = onBack;
        this.stage = stage;
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent;");
        this.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());

        // Presenter aanmaken (moet vóór layout vanwege event handlers)
        this.presenter = new ChapterPresenter(this, exercises, exerciseSessionBuilder, onBack, this.stage);

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
            presenter.onBackPressed(mode == Mode.BOARD && !skipListOnBack);
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
        int widthLabel = 900;

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
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(0, 0, 0, 0.40)),
                        new Stop(1, Color.color(0, 0, 0, 0.08))
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
            b.setOnAction(e -> presenter.onExerciseSelected(ex));
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
        boardPane.setPadding(new Insets(10, 24, 24, 24));
        boardPane.setStyle("-fx-background-color: transparent;");
        boardPane.setVisible(false);  // start onzichtbaar
        boardPane.setManaged(false);
    }

    // door presenter aangeroepen
    public void showExerciseBoard(BoardModel boardModel,
                                  Controller controller,
                                  List<String> moveLines) {

        // BoardView op basis van model + controller
        boardView = new BoardView(boardModel, controller, controller.isWhiteTurn(), 600);

        // Moves ListView (UI)
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
        movesList.setPrefWidth(250);
        movesList.setMinWidth(250);

        movesList.setCellFactory(lv -> new ListCell<>() {
            private final Label lbl = new Label();

            {
                lbl.setWrapText(false);
                lbl.setStyle("-fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 14px;");
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

        // Moves vullen met de door de presenter aangeleverde regels
        movesList.getItems().setAll(moveLines);
        if (!movesList.getItems().isEmpty()) {
            movesList.getSelectionModel().select(0);
        }
        movesList.setVisible(false);
        movesList.managedProperty().bind(movesList.visibleProperty()); // verborgen = geen ruimte reserveren

        String navStyle = "-fx-background-color: #d7b77e; -fx-background-radius: 8;";

        // Mat-melding in de open ruimte rechts van de knoppen (geen apart venster meer).
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 40px; -fx-font-weight: bold;");

        // Buttons en hint-label
        Button showHideMovesBtn = new Button("Show moves!");
        showHideMovesBtn.setStyle(navStyle);
        showHideMovesBtn.setMaxWidth(Double.MAX_VALUE);

        showHideMovesBtn.textProperty().bind(
                Bindings.when(movesList.visibleProperty())
                        .then("Hide moves!")
                        .otherwise("Show moves!")
        );

        Button btnHint = new Button("Hint");
        btnHint.setStyle(navStyle);
        btnHint.setMaxWidth(Double.MAX_VALUE);
        btnHint.setVisible(true);

        Label lblHint = new Label();
        lblHint.setTextFill(Color.WHEAT);

        btnHint.setOnAction(e -> lblHint.setText(presenter.getHintText()));

        // Toggle moves visibility
        showHideMovesBtn.setOnAction(e -> {
            movesList.setVisible(!movesList.isVisible());
            btnHint.setVisible(!movesList.isVisible());
        });

        Button btnUndo = new Button("Undo");
        Button btnRedo = new Button("Redo");
        btnUndo.setStyle(navStyle);
        btnRedo.setStyle(navStyle);
        btnUndo.setOnAction(e -> controller.undoLastMove(boardModel));
        btnRedo.setOnAction(e -> controller.redoMove(boardModel));
        HBox undoRedoRow = equalButtonRow(btnUndo, btnRedo);

        // Navigatie tussen de exercises (bv. naar de volgende puzzel zonder terug te gaan).
        Button btnPrev = new Button("◀ Prev");
        Button btnNext = new Button("Next ▶");
        btnPrev.setStyle(navStyle);
        btnNext.setStyle(navStyle);
        btnPrev.setDisable(!presenter.hasPrevious());
        btnNext.setDisable(!presenter.hasNext());
        btnPrev.setOnAction(e -> presenter.onPreviousExercise());
        btnNext.setOnAction(e -> presenter.onNextExercise());
        HBox navRow = equalButtonRow(btnPrev, btnNext);

        // Rekbare tussenruimte duwt de Prev/Next-rij naar de onderkant van het bord.
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        VBox moveBox = new VBox(22);
        moveBox.getChildren().setAll(showHideMovesBtn, movesList, btnHint, lblHint, undoRedoRow, bottomSpacer, navRow);
        moveBox.setPrefWidth(250);
        moveBox.setMinWidth(250);
        // Kolom even hoog als het bord: Show moves lijnt met de bovenkant, Prev/Next met de onderkant.
        moveBox.prefHeightProperty().bind(boardView.heightProperty());
        moveBox.maxHeightProperty().bind(boardView.heightProperty());
        // Top-inzet = hoogte beurt-label + de 10px ruimte erboven, zodat de eerste knop
        // gelijk loopt met de bovenkant van het bord (niet met het label erboven).
        moveBox.setPadding(new Insets(34, 0, 0, 0));
        boardView.turnLabelHeightProperty().addListener((o, ov, nv) ->
                moveBox.setPadding(new Insets(nv.doubleValue() + 10, 0, 0, 0)));

        // Mat-melding centreert in de open ruimte rechts van de knoppenkolom.
        StackPane statusArea = new StackPane(statusLabel);
        statusArea.setAlignment(Pos.CENTER);
        HBox.setHgrow(statusArea, Priority.ALWAYS);

        HBox row = new HBox(30, boardView, moveBox, statusArea);
        row.setAlignment(Pos.TOP_LEFT);

        boardPane.getChildren().setAll(row);
        switchMode(Mode.BOARD);
    }

    /** Twee gelijk-brede knoppen naast elkaar (zelfde stramien als Prev/Next). */
    private HBox equalButtonRow(Button... buttons) {
        HBox box = new HBox(10, buttons);
        box.setAlignment(Pos.CENTER_LEFT);
        for (Button b : buttons) {
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
        }
        return box;
    }

    /** Toont de mat-melding recht naast het bord (aangeroepen door de Controller). */
    public void showSolved() {
        if (statusLabel != null) {
            statusLabel.setText("Mat! 🎉");
        }
    }

    private void switchMode(Mode m) {
        mode = m;
        boolean list = (m == Mode.LIST);

        tilesScroll.setVisible(list);
        tilesScroll.setManaged(list);

        boardPane.setVisible(!list);
        boardPane.setManaged(!list);
    }

    public void showExerciseList() {
        if (titleLabel != null) titleLabel.setText(chapterTitle);
        switchMode(Mode.LIST);
    }

    /** Zet de koptitel op de naam van de actieve exercise (door de presenter aangeroepen). */
    public void setHeaderTitle(String title) {
        if (titleLabel != null && title != null) titleLabel.setText(title);
    }

    /** Slaat de tegelstap over en opent het bord meteen voor de eerste (of enige) exercise. */
    public void autoStart() {
        skipListOnBack = true;
        if (!exercises.isEmpty()) {
            presenter.onExerciseSelected(exercises.get(0));
        }
    }

    /** Slaat de tegelstap over en opent het bord meteen voor een specifieke exercise. */
    public void autoStart(Exercise startExercise) {
        skipListOnBack = true;
        if (startExercise != null) {
            presenter.onExerciseSelected(startExercise);
        }
    }

}
