package application.chesstrainerfx;

import application.chesstrainerfx.config.AppConfig;
import application.chesstrainerfx.config.ResourceSeeder;
import application.chesstrainerfx.controller.PositionEditorController;
import application.chesstrainerfx.view.ChapterOverviewView;
import application.chesstrainerfx.view.ChapterWindow;
import application.pgnreader.io.ChapterLoader;
import application.pgnreader.model.Chapter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Main extends Application {
    public String title = "ChessTrainer — Home";

    /** Module-sleutels voor navigatie en caching. */
    private static final String MATING_PATTERNS = "MATING_PATTERNS";
    private static final String TACTICS = "TACTICS";
    private static final String PUZZLES = "PUZZLES";

    private Scene scene;
    private Parent homeRoot;
    private final Map<String, Parent> moduleRoots = new HashMap<>();
    private AppConfig config;

    public Stage getStage() {
        return stage;
    }

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.config = AppConfig.load();

        // Eerste start: meegeleverde PGN's naar de geconfigureerde mappen kopiëren
        ResourceSeeder.seedIfEmpty(config.matingPatternsDir(), "/pgn/mating/chapters");
        ResourceSeeder.seedIfEmpty(config.tacticsDir(), "/pgn/tactics");

        homeRoot = buildHome();
        scene = new Scene(homeRoot, 1500, 1000);
        scene.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());
        stage.setTitle(title);
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

        btnMate.setOnAction(e -> openModule(MATING_PATTERNS));
        btnTactics.setOnAction(e -> openModule(TACTICS));
        btnPuzzles.setOnAction(e -> openModule(PUZZLES));

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

    /** Router: bepaalt welk module-scherm geladen wordt (lazy, daarna gecachet). */
    private void openModule(String module) {
        Parent root = moduleRoots.computeIfAbsent(module, this::buildModuleRoot);
        scene.setRoot(root);
    }

    private Parent buildModuleRoot(String module) {
        return switch (module) {
            case MATING_PATTERNS -> buildChapterModule(module, config.matingPatternsDir());
            case TACTICS -> buildChapterModule(module, config.tacticsDir());
            case PUZZLES -> buildPuzzlesModule();
            default -> homeRoot;
        };
    }

    /** Hoofdstuk-module (Mating Patterns / Tactics): tegels per PGN-bestand in de modulemap. */
    private Parent buildChapterModule(String module, Path pgnDir) {
        List<Chapter> chapters = ChapterLoader.loadChapters(pgnDir);

        ChapterOverviewView view = new ChapterOverviewView(
                chapters.stream().map(Chapter::getTitle).toList(),
                name -> openChapter(chapters, name, () -> scene.setRoot(moduleRoots.get(module))));

        return withBackButton(view, () -> scene.setRoot(homeRoot));
    }

    /** Puzzles-module: tegels per sub-categorie (sub-map), daarna hoofdstukken per sub-categorie. */
    private Parent buildPuzzlesModule() {
        List<String> subCategories = listSubCategories(config.puzzlesDir());

        ChapterOverviewView view = new ChapterOverviewView(subCategories, this::openPuzzleSubCategory);
        StackPane wrapper = withBackButton(view, () -> scene.setRoot(homeRoot));

        MenuItem editorItem = new MenuItem("Position Editor");
        editorItem.setOnAction(e -> openPositionEditor());
        MenuButton menu = new MenuButton("☰", null, editorItem);
        menu.setStyle("""
        -fx-background-color: rgba(20,20,20,0.65);
        -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;
        -fx-padding: 6 12 6 12; -fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 8;
    """);
        StackPane.setAlignment(menu, Pos.TOP_RIGHT);
        StackPane.setMargin(menu, new Insets(10));
        wrapper.getChildren().add(menu);

        return wrapper;
    }

    /** Laadt de Position Editor (position-editor.fxml) als scherm binnen de Puzzles-module. */
    private void openPositionEditor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/position-editor.fxml"));
            Parent editorRoot = loader.load();
            PositionEditorController controller = loader.getController();
            controller.init(stage, config.puzzlesDir());

            StackPane wrapper = withBackButton(editorRoot, () -> scene.setRoot(moduleRoots.get(PUZZLES)));
            wrapper.setStyle("-fx-background-color: white;");
            scene.setRoot(wrapper);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Position Editor");
            alert.setHeaderText("Kon de Position Editor niet openen");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }

    private void openPuzzleSubCategory(String subCategory) {
        Path dir = config.puzzlesDir().resolve(subCategory);
        List<Chapter> chapters = ChapterLoader.loadChapters(dir);

        Runnable backToPuzzles = () -> scene.setRoot(moduleRoots.get(PUZZLES));
        ChapterOverviewView view = new ChapterOverviewView(
                chapters.stream().map(Chapter::getTitle).toList(),
                name -> openChapter(chapters, name, backToPuzzles));

        scene.setRoot(withBackButton(view, backToPuzzles));
    }

    private void openChapter(List<Chapter> chapters, String name, Runnable onBack) {
        Chapter chapter = chapters.stream()
                .filter(c -> c.getTitle().equals(name))
                .findFirst().orElse(null);
        if (chapter != null) {
            scene.setRoot(new ChapterWindow(
                    chapter.getTitle(),
                    chapter.getExercises(),
                    v -> onBack.run(), this.stage
            ));
            stage.setTitle(chapter.getExercises().getFirst().toString());
        }
    }

    private static List<String> listSubCategories(Path puzzlesDir) {
        if (!Files.isDirectory(puzzlesDir)) {
            return List.of();
        }
        try (Stream<Path> dirs = Files.list(puzzlesDir)) {
            return dirs.filter(Files::isDirectory)
                    .map(d -> d.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Legt een gestylede terug-knop over de gegeven view heen. */
    private StackPane withBackButton(Parent content, Runnable onBack) {
        Button back = new Button("← Back");
        back.setOnAction(e -> onBack.run());
        back.setStyle("""
        -fx-background-color: rgba(20,20,20,0.65);
        -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;
        -fx-padding: 6 12 6 12; -fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 8;
    """);

        StackPane wrapper = new StackPane(content, back);
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(10));

        return wrapper;
    }

    public static void main(String[] args) { launch(args); }
}
