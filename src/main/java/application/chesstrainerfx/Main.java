package application.chesstrainerfx;

import application.chesstrainerfx.config.AppConfig;
import application.chesstrainerfx.config.ResourceSeeder;
import application.chesstrainerfx.config.SubCategoryStore;
import application.chesstrainerfx.controller.PositionEditorController;
import application.chesstrainerfx.view.ChapterOverviewView;
import application.chesstrainerfx.view.ChapterWindow;
import application.pgnreader.io.ChapterLoader;
import application.pgnreader.model.Chapter;
import application.pgnreader.model.Exercise;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private SubCategoryStore subCategoryStore;

    public Stage getStage() {
        return stage;
    }

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.config = AppConfig.load();
        this.subCategoryStore = new SubCategoryStore(config.puzzlesDir());

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
        if (PUZZLES.equals(module)) {
            stage.setTitle("ChessTrainer — Puzzles");
        }
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

        StackPane wrapper = withBackButton(view, () -> scene.setRoot(homeRoot));

        MenuItem importItem = new MenuItem("Import PGN…");
        importItem.setOnAction(e -> importPgnFiles(pgnDir, () -> refreshModule(module)));
        wrapper.getChildren().add(overlayMenu("☰", importItem));

        return wrapper;
    }

    /** Module opnieuw opbouwen (na import) en tonen. */
    private void refreshModule(String module) {
        moduleRoots.put(module, buildModuleRoot(module));
        scene.setRoot(moduleRoots.get(module));
    }

    /** Puzzles-module: tegels per sub-categorie (sub-map), daarna hoofdstukken per sub-categorie. */
    private Parent buildPuzzlesModule() {
        List<String> subCategories = subCategoryStore.load();

        ChapterOverviewView view = new ChapterOverviewView(subCategories, this::openPuzzleSubCategory);
        view.setGridTopPadding(184);
        StackPane wrapper = withBackButton(view, () -> {
            stage.setTitle(title);
            scene.setRoot(homeRoot);
        });

        Button editorBtn = overlayButton("Position Editor");
        editorBtn.setOnAction(e -> openPositionEditor());
        StackPane.setAlignment(editorBtn, Pos.TOP_LEFT);
        StackPane.setMargin(editorBtn, new Insets(58, 10, 10, 10));

        MenuItem newItem = new MenuItem("New Sub-category…");
        newItem.setOnAction(e -> createSubCategory());

        MenuItem renameItem = new MenuItem("Rename Sub-category…");
        renameItem.setOnAction(e -> renameSubCategory());

        MenuItem deleteItem = new MenuItem("Delete Sub-category…");
        deleteItem.setOnAction(e -> deleteSubCategory());

        MenuButton subCategoryMenu = overlayMenu("Sub-categories", newItem, renameItem, deleteItem);
        subCategoryMenu.setPopupSide(Side.BOTTOM);
        StackPane.setAlignment(subCategoryMenu, Pos.TOP_LEFT);
        StackPane.setMargin(subCategoryMenu, new Insets(106, 10, 10, 10));

        wrapper.getChildren().addAll(editorBtn, subCategoryMenu);

        return wrapper;
    }

    // ---------------- Sub-categoriebeheer (Puzzles) ---------------- //

    private void createSubCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Sub-category");
        dialog.setHeaderText("Name of the new sub-category:");
        Optional<String> name = dialog.showAndWait();
        if (name.isEmpty()) {
            return;
        }

        String trimmed = name.get().trim();
        String error = SubCategoryStore.validateName(trimmed, subCategoryStore.load());
        if (error != null) {
            showInfo("Invalid name", error);
            return;
        }

        try {
            subCategoryStore.create(trimmed);
            refreshModule(PUZZLES);
        } catch (IOException e) {
            showInfo("Create failed", e.getMessage());
        }
    }

    private void renameSubCategory() {
        List<String> subCategories = subCategoryStore.load();
        if (subCategories.isEmpty()) {
            showInfo("No sub-categories", "There are no sub-categories to rename yet.");
            return;
        }

        ChoiceDialog<String> choice = new ChoiceDialog<>(subCategories.get(0), subCategories);
        choice.setTitle("Rename Sub-category");
        choice.setHeaderText("Which sub-category do you want to rename?");
        Optional<String> selected = choice.showAndWait();
        if (selected.isEmpty()) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.get());
        dialog.setTitle("Rename Sub-category");
        dialog.setHeaderText("New name for \"" + selected.get() + "\":");
        Optional<String> newName = dialog.showAndWait();
        if (newName.isEmpty()) {
            return;
        }

        String trimmed = newName.get().trim();
        if (trimmed.equals(selected.get())) {
            return;
        }
        String error = SubCategoryStore.validateName(trimmed, subCategories);
        if (error != null) {
            showInfo("Invalid name", error);
            return;
        }

        try {
            subCategoryStore.rename(selected.get(), trimmed);
            refreshModule(PUZZLES);
        } catch (IOException e) {
            showInfo("Rename failed", e.getMessage());
        }
    }

    private void deleteSubCategory() {
        List<String> subCategories = subCategoryStore.load();
        if (subCategories.isEmpty()) {
            showInfo("No sub-categories", "There are no sub-categories to delete yet.");
            return;
        }

        ChoiceDialog<String> choice = new ChoiceDialog<>(subCategories.get(0), subCategories);
        choice.setTitle("Delete Sub-category");
        choice.setHeaderText("Which sub-category do you want to delete?");
        Optional<String> selected = choice.showAndWait();
        if (selected.isEmpty()) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Sub-category");
        confirm.setHeaderText("Delete \"" + selected.get() + "\"?");
        confirm.setContentText("All PGN files in this sub-category will also be deleted.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            subCategoryStore.delete(selected.get());
            refreshModule(PUZZLES);
        } catch (IOException e) {
            showInfo("Delete failed", e.getMessage());
        }
    }

    /** Laadt de Position Editor (position-editor.fxml) als scherm binnen de Puzzles-module. */
    private void openPositionEditor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/position-editor.fxml"));
            Parent editorRoot = loader.load();
            PositionEditorController controller = loader.getController();
            controller.init(stage, config.puzzlesDir());

            StackPane wrapper = withBackButton(editorRoot, () -> scene.setRoot(moduleRoots.get(PUZZLES)));

            var bgUrl = getClass().getResource("/images/background_chapters_blur.png");
            ImageView bg = new ImageView(new Image(bgUrl.toExternalForm()));
            bg.setPreserveRatio(false);
            bg.setSmooth(true);
            bg.setMouseTransparent(true);
            bg.fitWidthProperty().bind(wrapper.widthProperty());
            bg.fitHeightProperty().bind(wrapper.heightProperty());
            wrapper.getChildren().add(0, bg);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Position Editor");
            alert.setHeaderText("Could not open the Position Editor");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }

    private void openPuzzleSubCategory(String subCategory) {
        Path dir = config.puzzlesDir().resolve(subCategory);
        List<Exercise> exercises = ChapterLoader.loadChapters(dir).stream()
                .flatMap(c -> c.getExercises().stream())
                .toList();

        Runnable backToPuzzles = () -> scene.setRoot(moduleRoots.get(PUZZLES));
        ChapterOverviewView view = new ChapterOverviewView(
                exercises.stream().map(Exercise::getTitle).toList(),
                name -> openExerciseDetail(exercises, name, () -> openPuzzleSubCategory(subCategory)));

        StackPane wrapper = withBackButton(view, backToPuzzles);

        MenuItem importItem = new MenuItem("Import PGN…");
        importItem.setOnAction(e -> importPgnFiles(dir, () -> openPuzzleSubCategory(subCategory)));
        wrapper.getChildren().add(overlayMenu("☰", importItem));

        scene.setRoot(wrapper);
    }

    private void openExerciseDetail(List<Exercise> exercises, String title, Runnable onBack) {
        exercises.stream()
                .filter(e -> e.getTitle().equals(title))
                .findFirst()
                .ifPresent(exercise -> {
                    ChapterWindow window = new ChapterWindow(
                            exercise.getTitle(),
                            exercises,
                            v -> onBack.run(),
                            stage
                    );
                    scene.setRoot(window);
                    window.autoStart(exercise);
                });
    }

    /**
     * Importeert PGN-bestanden via een FileChooser naar {@code targetDir}.
     * Bestaande bestanden worden niet overschreven. Na een geslaagde import
     * wordt {@code afterImport} uitgevoerd om de weergave te verversen.
     */
    private void importPgnFiles(Path targetDir, Runnable afterImport) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import PGN files");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PGN files (*.pgn)", "*.pgn"));

        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) {
            return;
        }

        int copied = 0;
        List<String> skipped = new ArrayList<>();
        for (File file : files) {
            Path target = targetDir.resolve(file.getName());
            if (Files.exists(target)) {
                skipped.add(file.getName() + " (already exists)");
                continue;
            }
            try {
                Files.createDirectories(targetDir);
                Files.copy(file.toPath(), target);
                copied++;
            } catch (IOException e) {
                skipped.add(file.getName() + " (" + e.getMessage() + ")");
            }
        }

        StringBuilder msg = new StringBuilder(copied + " file(s) imported.");
        if (!skipped.isEmpty()) {
            msg.append("\nSkipped:\n").append(String.join("\n", skipped));
        }
        showInfo("PGN import", msg.toString());

        if (copied > 0) {
            afterImport.run();
        }
    }

    /** Gestylede overlay-knop (zelfde stijl als de menuknop en de terug-knop). */
    private Button overlayButton(String label) {
        Button button = new Button(label);
        button.setStyle("""
        -fx-background-color: rgba(20,20,20,0.65);
        -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;
        -fx-padding: 6 12 6 12; -fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 8;
    """);
        return button;
    }

    /** Gestylede menuknop rechtsboven voor module-acties. */
    private MenuButton overlayMenu(String label, MenuItem... items) {
        MenuButton menu = new MenuButton(label, null, items);
        menu.getStyleClass().add("overlay-menu");
        menu.setPopupSide(Side.LEFT);
        menu.setStyle("""
        -fx-background-color: rgba(20,20,20,0.65);
        -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;
        -fx-padding: 6 12 6 12; -fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 8;
    """);
        StackPane.setAlignment(menu, Pos.TOP_RIGHT);
        StackPane.setMargin(menu, new Insets(10));
        return menu;
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

    /** Legt een gestylede terug-knop over de gegeven view heen. */
    private StackPane withBackButton(Parent content, Runnable onBack) {
        Button back = overlayButton("← Back");
        back.setOnAction(e -> onBack.run());

        StackPane wrapper = new StackPane(content, back);
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(10));

        return wrapper;
    }

    private static void showInfo(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }

    public static void main(String[] args) { launch(args); }
}
