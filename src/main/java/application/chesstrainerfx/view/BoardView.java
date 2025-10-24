package application.chesstrainerfx.view;

import application.chesstrainerfx.*;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class BoardView extends HBox implements BoardChangeListener {

    private final BoardModel boardModel;
    private final SquareView[][] squareViews = new SquareView[8][8];
//    private final Button setupBtn = new Button("Position Setup");
//    private final Button startPosBtn = new Button("Start Position");
//    private final VBox controlPane;
    private final boolean isWhitePerspective;
//    private final Circle circle = new Circle();
    private PieceSelectorPane pieceSelector = null;
    private Controller controller;
    private int boardSize;   // size board
    private float backgroundsize;

    public BoardView(BoardModel boardModel, Controller controller, boolean isWhitePerspective, int boardSize) {
        this.boardSize = boardSize;
        backgroundsize = boardSize+(boardSize * 0.07f);
        this.boardModel = boardModel;
        this.controller = controller;
        this.isWhitePerspective = isWhitePerspective;
        this.setSpacing(20);
        this.setAlignment(Pos.CENTER_LEFT);
        boardModel.addListener(this);
        //this.setBorder(new Border(new BorderStroke(Color.BLUEVIOLET, BorderStrokeStyle.SOLID, null , null)));

        StackPane boardWithBackground = createBoardStack(boardModel, controller, isWhitePerspective);
        this.getChildren().add(boardWithBackground);
        

        pieceSelector = new PieceSelectorPane(selected -> controller.setSelectedPieceForSetup(selected));
//        controlPane = new VBox(10);
//        controlPane.setAlignment(Pos.TOP_CENTER);

//        setupBtn.setOnAction(event -> toggleSetupMode());
//        Button exportFENBtn = new Button("Export FEN");
//        TextField fenField = new TextField();
//        fenField.setEditable(false);
//        fenField.setPrefWidth(450);
//        startPosBtn.setOnAction(event -> {
//
//            for (SquareModel sq: boardModel.getSquares()){
//                sq.setPiece(null);
//            }
//            boardModel.initializeFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
//            //TODO turn not working
//            //controller.setWhiteTurn(true);
//            onBoardUpdated();
//
//        });

//        exportFENBtn.setOnAction(event -> {
//            String fen = boardModel.exportToFEN();
//            fenField.setText(fen);
//            System.out.println("FEN: " + fen);
//        });
//        circle.setRadius(20);
//        circle.setFill(Color.WHITE);
//        circle.setStroke(Color.BLACK);
//
//        controlPane.getChildren().add(setupBtn);
//        controlPane.getChildren().addAll(exportFENBtn, fenField, startPosBtn, circle);
//        this.getChildren().add(controlPane);
    }

//    private void toggleSetupMode() {
//        controller.toggleSetupMode(); // toggelt boolean
//
//        if (controller.isSetupMode()) {
//            System.out.println("Entering setup mode");
//            if (!controlPane.getChildren().contains(pieceSelector)) {
//                controlPane.getChildren().add(pieceSelector);
//            }
//            setupBtn.setText("Leave Setup");
//        } else {
//            System.out.println("Leaving setup mode");
//            controlPane.getChildren().remove(pieceSelector);
//            setupBtn.setText("Position Setup");
//        }
//    }

    private StackPane createBoardStack(BoardModel boardModel, Controller controller, boolean isWhitePerspective) {
        String imagePath = isWhitePerspective ? "/images/chessboard_white.png" : "/images/chessboard_black.png";
        Image backgroundImage = new Image(getClass().getResource(imagePath).toExternalForm());

        BackgroundSize bgSize = new BackgroundSize(backgroundsize, backgroundsize, false, false, false, false);
        BackgroundImage bgImage = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                bgSize
        );

        StackPane boardWithBackground = new StackPane();
        boardWithBackground.setPrefSize(backgroundsize, backgroundsize);
        boardWithBackground.setBackground(new Background(bgImage));

        GridPane boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int displayRow = isWhitePerspective ? row : 7 - row;
                int displayCol = isWhitePerspective ? col : 7 - col;
                Position pos = new Position(row, col);
                SquareModel squareModel = boardModel.getSquare(pos);
                SquareView squareView = new SquareView(boardModel, squareModel, controller, boardSize / 8);
                squareViews[row][col] = squareView;
                boardGrid.add(squareView, displayCol, displayRow);
            }
        }

        boardWithBackground.getChildren().add(boardGrid);
        return boardWithBackground;
    }


    @Override
    public void onBoardUpdated() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squareViews[row][col].update();
            }
        }
//        System.out.println("Turn: " + controller.isWhiteTurn());
//        Color color = controller.isWhiteTurn() ? Color.BLACK : Color.WHITE;
//
//        System.out.println(color);
//        circle.setFill(color);
//        if(color==Color.WHITE){
//            circle.setStroke(Color.BLACK);
//        }
    }
}
