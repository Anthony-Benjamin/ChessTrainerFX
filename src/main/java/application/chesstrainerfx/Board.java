package application.chesstrainerfx;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;

public class Board extends GridPane {
    private Square[][] board = new Square[8][8];
    private int clickCount = 0;
    HashMap<String, String> map;

    public Board() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = new Square(row, col);
                add(board[row][col], col, row);
            }
        }
//        initializeBoard();
        map = generateImageList();
//      positionFormFEN("rnbqkbnr/ppppp1pp/8/5pB1/3P4/2NQ1N2/PPP1PPPP/R3KB1R b KQkq - 0 1");
        positionFormFEN("r1bqk2r/ppp3pp/2n1pn2/3p1p2//2N1PN2/PP3PPP/R2QKB1bPP1B21R w KQkq - 0 1");


        this.setOnMouseClicked(e -> {

            clickCount++;

            Node clickNode = e.getPickResult().getIntersectedNode();
            StackPane clickParent = (StackPane) clickNode.getParent();


            Rectangle background = (Rectangle) clickParent.getChildren().getFirst();
            background.setStroke(Color.YELLOW);
            background.setStrokeWidth(5);
            background.setWidth(95);
            background.setHeight(95);

            if (clickCount == 1) {
                System.out.println("start position");
                Integer row = GridPane.getRowIndex(clickParent);
                Integer col = GridPane.getColumnIndex(clickParent);
                System.out.print(row + ",");
                System.out.println(col);
            }
            if (clickCount == 2) {
                System.out.println("end position");
                Integer row = GridPane.getRowIndex(clickParent);
                Integer col = GridPane.getColumnIndex(clickParent);
                System.out.print(row + ",");
                System.out.println(col);
                clickCount = 0;
            }
        });
    }



    private void initializeBoard() {
        board[0][0].setPiece("file:src/images/BlackRook.png");
        board[0][1].setPiece("file:src/images/BlackKnight.png");
        board[0][2].setPiece("file:src/images/BlackBishop.png");
        board[0][3].setPiece("file:src/images/BlackQueen.png");
        board[0][4].setPiece("file:src/images/BlackKing.png");
        board[0][5].setPiece("file:src/images/BlackBishop.png");
        board[0][6].setPiece("file:src/images/BlackKnight.png");
        board[0][7].setPiece("file:src/images/BlackRook.png");

        board[1][0].setPiece("file:src/images/BlackPawn.png");
        board[1][1].setPiece("file:src/images/BlackPawn.png");
        board[1][2].setPiece("file:src/images/BlackPawn.png");
        board[1][3].setPiece("file:src/images/BlackPawn.png");
        board[1][4].setPiece("file:src/images/BlackPawn.png");
        board[1][5].setPiece("file:src/images/BlackPawn.png");
        board[1][6].setPiece("file:src/images/BlackPawn.png");
        board[1][7].setPiece("file:src/images/BlackPawn.png");

        board[6][0].setPiece("file:src/images/WhitePawn.png");
        board[6][1].setPiece("file:src/images/WhitePawn.png");
        board[6][2].setPiece("file:src/images/WhitePawn.png");
        board[6][3].setPiece("file:src/images/WhitePawn.png");
        board[6][4].setPiece("file:src/images/WhitePawn.png");
        board[6][5].setPiece("file:src/images/WhitePawn.png");
        board[6][6].setPiece("file:src/images/WhitePawn.png");
        board[6][7].setPiece("file:src/images/WhitePawn.png");

        board[7][0].setPiece("file:src/images/WhiteRook.png");
        board[7][1].setPiece("file:src/images/WhiteKnight.png");
        board[7][2].setPiece("file:src/images/WhiteBishop.png");
        board[7][3].setPiece("file:src/images/WhiteQueen.png");
        board[7][4].setPiece("file:src/images/WhiteKing.png");
        board[7][5].setPiece("file:src/images/WhiteBishop.png");
        board[7][6].setPiece("file:src/images/WhiteKnight.png");
        board[7][7].setPiece("file:src/images/WhiteRook.png");
    }

    public void addPiece(int row, int col, String path) {
        board[row][col].setPiece(path);
    }


    public void positionFormFEN(String Fen) {
        String[] FENString = Fen.split(" ");
        String FEN = FENString[0];
        String[] ranks = FEN.split("/");
        ArrayList<String> boardArray = new ArrayList<>();
        // loop through ranks and make a list with characters in FEN string
        for (int i = 0; i < ranks.length; i++) {
            String row = ranks[i];    // row
            char[] chars = row.toCharArray();
            for (char character : chars) {
            //if character is a number put a space in list instead of character
                if (isNumeric(character)) {
                    for (int j = 0; j < Integer.parseInt(String.valueOf(character)); j++) {
                        boardArray.add(" ");
                    }
                } else {
                    boardArray.add(String.valueOf(character));
                }
            }

        }
        // place pieces from boardArray on board
        int counter = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                //System.out.print(boardArray.get(counter));
                if(!boardArray.get(counter).equals(" ")){
                    board[row][col].setPiece(map.get(boardArray.get(counter)));
                }

                counter++;
            }

        }
    }

    //determine if character is a digit
    public static boolean isNumeric(char character) {
        if (Character.isDigit(character)) {
            return true;
        }
        return false;
    }

    public HashMap<String, String> generateImageList() {
        map = new HashMap<>();
        map.put("r", "file:src/images/BlackRook.png");
        map.put("n", "file:src/images/BlackKnight.png");
        map.put("b", "file:src/images/BlackBishop.png");
        map.put("q", "file:src/images/BlackQueen.png");
        map.put("k", "file:src/images/BlackKing.png");
        map.put("b", "file:src/images/BlackBishop.png");
        map.put("n", "file:src/images/BlackKnight.png");
        map.put("r", "file:src/images/BlackRook.png");
        map.put("p", "file:src/images/BlackPawn.png");

        map.put("R", "file:src/images/WhiteRook.png");
        map.put("N", "file:src/images/WhiteKnight.png");
        map.put("B", "file:src/images/WhiteBishop.png");
        map.put("Q", "file:src/images/WhiteQueen.png");
        map.put("K", "file:src/images/WhiteKing.png");
        map.put("B", "file:src/images/WhiteBishop.png");
        map.put("N", "file:src/images/WhiteKnight.png");
        map.put("R", "file:src/images/WhiteRook.png");
        map.put("P", "file:src/images/WhitePawn.png");

        map.put("E", "file:src/images/EmptySquare.png");

        return map;
    }
}
