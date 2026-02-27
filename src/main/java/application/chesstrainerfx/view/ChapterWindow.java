package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.*;
import application.pgnreader.model.Exercise;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ChapterWindow extends BorderPane implements BoardChangeListener {


//    private Controller controller;
    private Controller controller = new Controller();
    private String[] parts;

    private enum Mode { LIST, BOARD }

    private final List<Exercise> exercises;
    private final String chapterTitle;
    private final Consumer<Void> onBack;

    // UI onderdelen
    private Button backBtn;
    private Label titleLabel;
    private ScrollPane theoryScroll;
    private Label theoryLabel;
    private int moveCounter;


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
        moveCounter = 0;

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

        // Listener (optioneel; jij gebruikt dit voor moveCounter/hints)
        boardModel.addListener(this);

        // ------------------------------------------------------------
        // 3) Bouw ExerciseSession uit FEN + PGN moves (zonder variaties)
        //    -> deze methode is jouw "Test-logica"
        //    LET OP: buildSessionFromExercise(ex) moet in ChapterWindow bestaan
        // ------------------------------------------------------------
        ExerciseSession session = buildSessionFromExercise(ex);
        controller.setExerciseSession(session);

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
        movesList.setPrefWidth(200);

        movesList.setCellFactory(lv -> new ListCell<>() {
            private final Label lbl = new Label();
            {
                lbl.setWrapText(true);
                lbl.setStyle("-fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 14px;");
                lbl.setMaxWidth(260);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else { lbl.setText(item); setGraphic(lbl); }
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
            String san = session.getExpectedSan();
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

        HBox row = new HBox(30, boardView, moveBox);
        row.setAlignment(Pos.CENTER_LEFT);

        // Zet de BOARD view in het center-stack en switch mode
        boardPane.getChildren().setAll(row);
        switchMode(Mode.BOARD);
    }


    @Override
    public void onBoardUpdated() {
        moveCounter++;
        System.out.println(moveCounter);
//        System.out.println(controller.);
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

        // 1) Normaliseer rare whitespace (NBSP etc.) + newlines
        String clean = moveString
                .replace('\u00A0', ' ')      // non-breaking space → normale spatie
                .replaceAll("\\r\\n?", "\n") // normalize line endings
                .replaceAll("\\s+", " ");    // alle whitespace -> single space

        // 2) Variaties weg (als je removeVariations hebt)
        clean = removeVariations(clean);

        // 3) Verwijder {...} blocks volledig (incl {[%csl ...]})
        clean = clean.replaceAll("\\{[^}]*}", " ");

        // 4) Verwijder inline tags zoals [%csl ...] / [%cal ...] als ze buiten braces staan
        clean = clean.replaceAll("\\[%[^\\]]*]", " ");

        // 5) Verwijder NAGs zoals $1
        clean = clean.replaceAll("\\$\\d+", " ");

        // 6) Verwijder resultaat (ook *), waar dan ook los staat
        clean = clean.replace("*", " ");
        clean = clean.replaceAll("(?i)\\b(1-0|0-1|1/2-1/2)\\b", " ");

        // 7) Final whitespace cleanup
        clean = clean.replaceAll("\\s+", " ").trim();

        // --- Debug: check cleaned ---
        System.out.println("CLEAN: [" + clean + "]");

        if (clean.isBlank()) return;

        // 8) Split per zetnummer. Fallback als split niets oplevert.
        parts = clean.split("(?=\\d+\\.(?:\\.\\.)?\\s)"); // <-- zonder \\b (betrouwbaarder)

        System.out.println("Size of parts " + parts.length);
        System.out.println("Parts: " + Arrays.deepToString(parts));
        int added = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                movesList.getItems().add(trimmed);
                added++;
            }
        }

        // 9) Fallback: als split niks opleverde, toon gewoon de hele string als 1 item
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
    private static Position pos(String square) {
        int[] rc = CoordinateSystem.coordinateToIndex(square); // [row, col]
        return new Position(rc[0], rc[1]);
    }

    private ExerciseSession buildSessionFromExercise(Exercise exercise) {

        String fen = exercise.getFen();

        // PGN: variaties eruit + comments/annotations eruit
        String main = removeVariations(exercise.getMoves());
        main = main.replaceAll("\\{[^}]*}", " ");                // {...} weg
        main = main.replaceAll("\\[[^]]*]", " ");                // [..] weg (soms)
        main = main.replace("*", " "); // harde fix voor losstaande *
        main = main.replaceAll("(?i)\\b(1-0|0-1|1/2-1/2)\\b", " "); // normale resultaten
        main = main.replaceAll("\\s+", " ").trim();

        ParsedMoves parsed = ChessMoveParser.parseMoves(main);

        // Interleave naar ply-volgorde: w1, b1, w2, b2, ...
        List<String> sanPlies = new ArrayList<>();
        int max = Math.max(parsed.whiteMoves.size(), parsed.blackMoves.size());
        for (int i = 0; i < max; i++) {
            if (i < parsed.whiteMoves.size()) sanPlies.add(parsed.whiteMoves.get(i));
            if (i < parsed.blackMoves.size()) sanPlies.add(parsed.blackMoves.get(i));
        }

        // Resolver gebruikt een TEMP board zodat je echte boardModel niet verandert
        BoardModel temp = new BoardModel();
        temp.initializeFromFEN(fen);

        boolean whiteToMove = fen.split("\\s+").length >= 2 && fen.split("\\s+")[1].equals("w");
        PieceColor toMove = whiteToMove ? PieceColor.WHITE : PieceColor.BLACK;

        List<Move> mainLine = new ArrayList<>();
        List<String> sanLine = new ArrayList<>();


        for (String san : sanPlies) {
            if (san == null || san.isBlank()) continue;

            Move m = resolveSanToMove(temp, san, toMove);
            if (m == null) {
                throw new IllegalStateException("Kon SAN niet resolven: " + san);
            }

            mainLine.add(m);
            sanLine.add(san); // 👈 originele SAN bewaren

            applyMoveOnTempBoard(temp, m, san, toMove);
            toMove = (toMove == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        }

        System.out.println("buildsession mainline: " + mainLine);
        return new ExerciseSession(mainLine, sanLine);
    }
    private Move resolveSanToMove(BoardModel board, String sanRaw, PieceColor color) {
        String san = sanRaw.replace("+", "").replace("#", "").trim();

        // Rokade
        if (san.equals("O-O") || san.equals("0-0")) {
            Position from = (color == PieceColor.WHITE) ? pos("e1") : pos("e8");
            Position to   = (color == PieceColor.WHITE) ? pos("g1") : pos("g8");
            return new Move(from, to);
        }
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            Position from = (color == PieceColor.WHITE) ? pos("e1") : pos("e8");
            Position to   = (color == PieceColor.WHITE) ? pos("c1") : pos("c8");
            return new Move(from, to);
        }

        // promotie: e8=Q / dxe8=Q
        String promotion = null;
        if (san.contains("=")) {
            promotion = san.substring(san.indexOf('=') + 1); // "Q" etc
            san = san.substring(0, san.indexOf('='));        // strip "=Q"
        }

        // target square = laatste 2 chars (bv "e6")
        if (san.length() < 2) return null;
        String targetSq = san.substring(san.length() - 2);
        Position to = pos(targetSq);

        // piece type bepalen
        PieceType type = PieceType.PAWN;
        int idx = 0;
        char first = san.charAt(0);
        if (Character.isUpperCase(first)) {
            type = switch (first) {
                case 'K' -> PieceType.KING;
                case 'Q' -> PieceType.QUEEN;
                case 'R' -> PieceType.ROOK;
                case 'B' -> PieceType.BISHOP;
                case 'N' -> PieceType.KNIGHT;
                default  -> PieceType.PAWN;
            };
            idx = 1;
        }

        // disambiguatie: bv Nbd2 of R1e1 of Qh4e1 (we ondersteunen file/rank)
        String middle = san.substring(idx, san.length() - 2);
        middle = middle.replace("x", ""); // capture marker eruit

        Character fromFileHint = null; // 'a'..'h'
        Character fromRankHint = null; // '1'..'8'
        for (char c : middle.toCharArray()) {
            if (c >= 'a' && c <= 'h') fromFileHint = c;
            if (c >= '1' && c <= '8') fromRankHint = c;
        }

        List<Position> candidates = new ArrayList<>();

        for (SquareModel sq : board.getSquares()) {
            PieceModel p = sq.getPiece();
            if (p == null) continue;
            if (p.getColor() != color) continue;
            if (p.getType() != type) continue;

            Position from = sq.getPosition();

            // hints filteren
            if (fromFileHint != null) {
                int fileCol = fromFileHint - 'a';
                if (from.getColumn() != fileCol) continue;
            }
            if (fromRankHint != null) {
                int rank = fromRankHint - '1';     // 0..7
                int rowExpected = 7 - rank;        // want rank '1' is row 7 in jouw systeem
                if (from.getRow() != rowExpected) continue;
            }

            if (MoveValidator.isValidMove(board, p, from, to)) {
                candidates.add(from);
            }
        }

        // Pawn capture hint: "fxe6" → file hint zit aan begin
        if (type == PieceType.PAWN && sanRaw.contains("x") && sanRaw.length() >= 1) {
            char file = sanRaw.charAt(0);
            if (file >= 'a' && file <= 'h') {
                int col = file - 'a';
                candidates.removeIf(pos -> pos.getColumn() != col);
            }
        }

        if (candidates.size() != 1) {
            // bij 0 of meerdere: voorlopig fail fast (dan weten we welke SAN lastig is)
            System.out.println("SAN resolve ambiguity: " + sanRaw + " candidates=" + candidates);
            return null;
        }

        return new Move(candidates.get(0), to);
    }

    private void applyMoveOnTempBoard(BoardModel board, Move move, String sanRaw, PieceColor color) {
        Position from = move.getFrom();
        Position to = move.getTo();

        PieceModel piece = board.getSquare(from).getPiece();
        if (piece == null) return;

        // En passant (simpel): pawn diagonaal naar leeg veld → remove captured pawn
        if (piece.getType() == PieceType.PAWN) {
            PieceModel target = board.getSquare(to).getPiece();
            int dx = Math.abs(to.getColumn() - from.getColumn());
            if (dx == 1 && target == null) {
                // captured pawn staat op (from.row, to.col)
                Position cap = new Position(from.getRow(), to.getColumn());
                board.getSquare(cap).removePiece();
            }
        }

        // Move piece
        board.movePiece(from, to);

        // Rokade: rook ook verplaatsen (BoardModel.movePiece doet dat niet)
        if (piece.getType() == PieceType.KING) {
            int dx = to.getColumn() - from.getColumn();
            if (Math.abs(dx) == 2) {
                if (dx > 0) { // korte rokade
                    Position rookFrom = new Position(from.getRow(), 7);
                    Position rookTo   = new Position(from.getRow(), 5);
                    board.movePiece(rookFrom, rookTo);
                } else {      // lange rokade
                    Position rookFrom = new Position(from.getRow(), 0);
                    Position rookTo   = new Position(from.getRow(), 3);
                    board.movePiece(rookFrom, rookTo);
                }
            }
        }

        // Promotie: als SAN "=Q" etc bevat
        if (sanRaw.contains("=")) {
            char promo = sanRaw.charAt(sanRaw.indexOf('=') + 1);
            PieceType newType = switch (promo) {
                case 'Q' -> PieceType.QUEEN;
                case 'R' -> PieceType.ROOK;
                case 'B' -> PieceType.BISHOP;
                case 'N' -> PieceType.KNIGHT;
                default -> PieceType.QUEEN;
            };
            board.getSquare(to).setPiece(new PieceModel(newType, color));
        }
    }

}
