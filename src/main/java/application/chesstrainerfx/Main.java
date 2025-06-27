package application.chesstrainerfx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        BoardModel model = new BoardModel();
        Controller controller = new Controller();
        model.initializeFromFEN("rnbqk1nr/ppp2ppp/3bp3/3p4/3P1B2/5N2/PPP1PPPP/RN1QKB1R w KQkq - 0 1");

        BoardView view = new BoardView(model, controller,false);

        Label oefeningen;
        oefeningen = new Label("Oefeningen ");
        BackgroundFill backgroundFill= new BackgroundFill(
                Color.valueOf("#6cd3fb"),
                new CornerRadii(0),
                new Insets(0)
        );
        Background background = new Background(backgroundFill);
        oefeningen.setBackground(background);

        VBox vBox = new VBox(10);
        vBox.getChildren().addAll(oefeningen, new Label(""));
        vBox.setStyle("-fx-background-color: light-blue;");

        HBox bottom = new HBox();
        Button bordOmdraaien = new Button("Bord omdraaien");
        Button hint = new Button("Hint");
        Button volgende = new Button("Volgende");
        bottom.getChildren().addAll(bordOmdraaien, hint, volgende);
        bottom.setPadding(new Insets(20));
        bottom.setSpacing(10);

        BorderPane mainLayout;
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        mainLayout.setTop(vBox);
        mainLayout.setCenter(view);
        mainLayout.setLeft(new Label("Left"));
        mainLayout.setRight(new Label("Hint"));
        mainLayout.setBottom(bottom);

        Scene scene = new Scene(mainLayout);

        stage.setScene(scene);
        stage.show();
    }
}
