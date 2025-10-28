package application.chesstrainerfx.view;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class SplashScreen {

    private final Stage stage = new Stage(StageStyle.UNDECORATED);
    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label("Loading…");

    public SplashScreen() {
        // Root pane met achtergrond
        StackPane root = new StackPane();
        root.getStyleClass().add("splash-root");

        // Achtergrondafbeelding via CSS (zie splash.css)
        // Extra blur-pane (subtiel)
        StackPane blurPane = new StackPane();
        blurPane.setBackground(new Background(new BackgroundFill(Color.color(0,0,0,0.20), CornerRadii.EMPTY, Insets.EMPTY)));
        blurPane.setEffect(new BoxBlur(12, 12, 2));

        // Branding/tekst
        VBox box = new VBox(10);
        box.setAlignment(Pos.BOTTOM_LEFT);
        box.setPadding(new Insets(24));
        Label title = new Label("ChessTrainer");
        Label subtitle = new Label("Puzzles • Mate Patterns • Tactics");
        title.getStyleClass().add("splash-title");
        subtitle.getStyleClass().add("splash-subtitle");

        progress.setPrefWidth(360);
        progress.setProgress(0);
        progress.setStyle("-fx-accent: #ffcc33;");

        status.getStyleClass().add("splash-status");
        status.setTextFill(Color.WHITE);

        box.getChildren().addAll(title, subtitle, progress, status);

        root.getChildren().addAll(blurPane, box);

        Scene scene = new Scene(root, 820, 520, Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        // Fade-in
        FadeTransition ft = new FadeTransition(Duration.millis(350), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    public void show() { stage.show(); }

    public void closeWithFade(Runnable afterClose) {
        FadeTransition ft = new FadeTransition(Duration.millis(250), stage.getScene().getRoot());
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            stage.close();
            if (afterClose != null) Platform.runLater(afterClose);
        });
        ft.play();
    }

    public void setProgress(double p) {
        progress.setProgress(p);
    }

    public void setStatus(String text) {
        status.setText(text == null ? "" : text);
    }
}
