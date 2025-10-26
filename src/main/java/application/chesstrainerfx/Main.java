package application.chesstrainerfx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Root met achtergrond (uit CSS)
        StackPane root = new StackPane();
        root.getStyleClass().add("splash-root");

        // Subtiele overlay om de knoppen leesbaarder te maken
        StackPane overlay = new StackPane();
        overlay.setBackground(new Background(
                new BackgroundFill(Color.color(0, 0, 0, 0.15), CornerRadii.EMPTY, Insets.EMPTY)));
        overlay.setEffect(new BoxBlur(8, 8, 2));

        // Knoppen rechts-onder naast elkaar
        HBox buttons = new HBox(12);
        buttons.setPadding(new Insets(24));
        buttons.setAlignment(Pos.BOTTOM_RIGHT);


        Button btnMate = makeTile("♛\nMating\nPatterns");
        Button btnTactics = makeTile("⚡\nTactics");
        Button btnPuzzles = makeTile("♟\nPuzzles");

        buttons.getChildren().addAll(btnPuzzles, btnMate, btnTactics);
        StackPane.setAlignment(buttons, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(buttons, new Insets(0,250,70,0));


        // Event-handlers (later vervangen door echte schermen)
        btnMate.setOnAction(e -> startTraining("MATE_PATTERNS"));
        btnTactics.setOnAction(e -> startTraining("TACTICS"));
        btnPuzzles.setOnAction(e -> startTraining("PUZZLES"));

        root.getChildren().addAll(overlay, buttons);

        Scene scene = new Scene(root, 1500, 1000);
        System.out.println(scene.getWidth());
        scene.getStylesheets().add(getClass().getResource("/splash.css").toExternalForm());

        stage.setTitle("ChessTrainer — Home");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    private Button makeTile(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("tile");
        b.setPrefSize(140, 140);
        b.setWrapText(true);
        return b;
    }

    private void startTraining(String mode) {
        System.out.println("Start training: " + mode);
        // Hier kun je straks een nieuw venster openen voor elke trainingstype

    }

    public static void main(String[] args) {
        launch(args);
    }
}
