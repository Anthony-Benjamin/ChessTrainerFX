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
//        positionFormFEN("rnbqkbnr/ppppp1pp/8/5pB1/3P4/2NQ1N2/PPP1PPPP/R3KB1R b KQkq - 0 1");
        positionFormFEN("rnbqkbnr/ppppp1pp/8/5pB1/3P4/2NQ1N2/PPP1PPPP/R3KB1R");
//        HashMap<String, String> map = generateImageList();

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

//            Node clickNode = e.getPickResult().getIntersectedNode();
//            Node clickParent = clickNode.getParent();
//            System.out.println(clickParent);
//            Integer row = GridPane.getRowIndex(clickParent);
//            Integer col = GridPane.getColumnIndex(clickParent);
//            System.out.print(row + ",");
//            System.out.println(col);
        });
    }

//    private void printCoordinate(int row, int col) {
//        int[] coordinate = {row, col};
//        String coordintate = CoordinateSystem.indexToCoordinate(coordinate);
//
//        System.out.println(coordintate);
//    }


//    public void printBoard(){
//        for (int row = 0; row < 8; row++) {
//            for (int col = 0; col < 8; col++) {
//                add(board[row][col], col, row);
//                //board[row][col].printCoordinate();
//            }

    /// /            System.out.println();
//        }
//    }

//    public void printCoordinates(){
//        for (int row = 0; row < 8; row++) {
//            for (int col = 0; col < 8; col++) {
//                int index[] ={row,col};
//
//                String coordinate = CoordinateSystem.indexToCoordinate(index);
//                board[row][col].setLabelText(coordinate);
//            }
//        }
//    }
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
//        System.out.println("path? " + path);
        board[row][col].setPiece(path);
    }


//    public void positionFormFEN(String FEN) {
//        //3BQKBNR/ppppp1pp/8/5pB1/3P4/2NQ1N2/PPP1PPPP/R3KB1R"+
//
//        String[] pieces = FEN.split("/");
//        for (int row = pieces.length - 1; row > 0; row--) {
//            for (int col = 0; col < pieces[row].length(); col++) {
//                if (!isInt(pieces[row].charAt(col) + "")) {
//                    addPiece(0, col, map.get(pieces[row].charAt(col) + ""));
//                } else {
//                    col = col + Integer.parseInt(pieces[row].charAt(col) + "") - 1;
//                }
//            }
//        }
//    }

    public void positionFormFEN(String Fen) {
        String FEN = "bqr1krnb/pppppppp/4n3/8/8/8/PPPPPPPP/BQRNKRNB";
////        String FEN = "rbqnbkrn/pppppppp/8/8/3P4/2N1P3/PPP2PPP/RBQ1BKRN";
//        String[] output = FEN.split("/");
//
//        for (int row = 0; row < output.length; row++) {
////            System.out.print(row + ",");
//            for (int col = 0; col < output[row].length(); col++) {
////                System.out.print(col + " ");
////                System.out.print(row + "," + col + " ");
//                String piece = output[row].charAt(col) + "";
//                if (isNumeric(piece)) {
//                    int space = Integer.parseInt(piece);
//
//                    System.out.println("Number of spaces? " + space + " ");
//                    int diffRow;
//                    int diffCol;
//
//                    continue;
//                }
//
//                addPiece(row, col, map.get(piece));
//            }
////            System.out.println();
//        }
        String[] rijen = FEN.split("/");
        ArrayList<String> array2 = new ArrayList<>();
        for (int k = 0; k < rijen.length; k++) {
            String rij = rijen[k];    // row

            char[] chars = rij.toCharArray();

            for (char character : chars) {
                // System.out.println(character);

                if (isNumeric(String.valueOf(character))) {
                    for (int j = 0; j < Integer.parseInt(String.valueOf(character)); j++) {
                        array2.add(" ");
                    }
                } else {
                    array2.add(String.valueOf(character));
                }
                System.out.println();
            }
        }
        System.out.println(array2);
    }


    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }

        try {
            int number = Integer.parseInt(strNum);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
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
