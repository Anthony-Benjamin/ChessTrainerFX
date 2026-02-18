package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.utils.PieceColor;
import application.chesstrainerfx.utils.Position;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Test extends Application implements BoardChangeListener {

    private BoardModel boardModel;
    Controller controller;
    private boolean computerHasMoved = false;

    public static void main(String[] args){
        launch();
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
//            System.out.println("depth: " + depth);
        }

        return result.toString();
    }

    @Override
    public void start(Stage stage) throws Exception {

        boolean whosTurn = true;
        StackPane root = new StackPane();
        //Button tegenzet = new Button("Tegenzet");
        boardModel = new BoardModel();
//        boardModel.initializeFromFEN("6k1/6p1/3q3p/1p1nr3/8/3P2P1/1Q3N2/5RKR b - - 0 1");//{[#]} 1... Re2 2. Qxe2 (2. Ne4 Qb6+) 2... Qxg3# *
        boardModel.initializeFromFEN("4kr2/3b1p2/4pQ1p/q5b1/8/2p4P/1rB2PP1/3RR1K1 w - - 0 1");/*{[#]} 1. Rxe6+ fxe6 (1... Bxe6 2.Ba4+ Qxa4 (2... Rb5 3. Bxb5+ Qxb5 4. Rd8#) 3.
                                                                                                Rd8#) 2. Bg6+ Rf7 3. Qxf7+ Kd8 4.Qxd7#*/

//        boardModel.initializeFromFEN("1r2nR2/7k/3q2pp/4p2n/3pB3/3P2P1/1r1Q3P/5RK1 w - - 0 29");
        controller = new Controller();
        controller.setWhiteTurn(whosTurn);
        BoardView boardView = new BoardView(boardModel, controller, whosTurn, 720);
//       boardModel.playCounterMove("Re6", PieceColor.WHITE);
        boardModel.addListener(this);
        root.getChildren().add(boardView);
        //root.getChildren().add(tegenzet);

        Scene scene = new Scene(root, 1500, 1000);
        stage.setTitle("ChessTrainer — Home");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

//        boardModel.movePiece(new Position(7, 4), new Position(2, 4));
//        tegenzet.setOnAction(e -> boardModel.playCounterMove("Bxe6", PieceColor.BLACK));
        //tegenzet.setOnAction(e -> boardModel.playCounterMove("fxe6", PieceColor.BLACK));
    }

    @Override
    public void onBoardUpdated() {
        if (!computerHasMoved) {
            computerHasMoved = true;
            if(controller.getExerciseStage() == Controller.ExerciseStage.COMPUTERTOMOVE){
                boardModel.playCounterMove("fxe6", PieceColor.BLACK);
                controller.toggleTurn();
                System.out.println(controller.getExerciseStage());
            }
        }
    }

    @Override
    public void onTurnChanged(boolean whiteToMove) {
        BoardChangeListener.super.onTurnChanged(whiteToMove);
    }
}
