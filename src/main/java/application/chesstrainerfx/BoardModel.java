package application.chesstrainerfx;

import java.util.ArrayList;
import java.util.List;

public class BoardModel {
    private List<SquareModel> squares;

    public BoardModel() {
        squares = new ArrayList<>(64);
    }

    public void initializeBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares.add(new SquareModel(new Position(row, col)));
            }
        }
    }


    public ArrayList<Character> initializeFromFEN(String fen) {

        if (fen == "") {
            fen = "rn1qK1nR/pppppppp/3bbbb1/pppppppp/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        }
        String[] FENString = fen.split(" ");
        String FEN = FENString[0];
        String[] ranks = FEN.split("/");
        ArrayList<Character> boardArray = new ArrayList<>();
        // loop through ranks and make a list with characters in FEN string
        // row
        for (String rank : ranks) {
            char[] chars = rank.toCharArray();
            for (char character : chars) {
                //if character is a number put a space in list instead of character
                if (Character.isDigit(character)) {
                    for (int j = 0; j < Integer.parseInt(String.valueOf(character)); j++) {
                        boardArray.add('.');
                    }
                } else {
                    boardArray.add(character);
                }
            }

        }
        // place pieces from boardArray on board
        int counter = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                //System.out.print(boardArray.get(counter));
                if (!boardArray.get(counter).equals(" ")) {
                    System.out.print(boardArray.get(counter));
                }

                counter++;
            }
            System.out.println();
        }
        return boardArray;
    }

    //geeft een PieceModel terug dat is opgebouwd uit PieceType en PieceColor
    public PieceModel piecModelformFENChar(char c) {
        PieceColor color = null;
        PieceType type = null;
        if (Character.isLowerCase(c)) {
            color = PieceColor.BLACK;
        } else if (Character.isUpperCase(c)) {
            color = PieceColor.WHITE;
        }


        switch (Character.toLowerCase(c)) {
            case 'k':
                type = PieceType.KING;
                break;
            case 'q':
                type = PieceType.QUEEN;
                break;
            case 'r':
                type = PieceType.ROOK;
                break;
            case 'b':
                type = PieceType.BISHOP;
                break;
            case 'n':
                type = PieceType.KNIGHT;
                break;
            case 'p':
                type = PieceType.PAWN;
                break;
            default:
                System.out.println("geen geldige letter");
        }
        return new PieceModel(type, color);


    }
}
