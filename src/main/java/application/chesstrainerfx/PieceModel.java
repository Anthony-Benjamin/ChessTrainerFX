package application.chesstrainerfx;


/**
 *
 * @author ebenjamin
 */
public class PieceModel {
    private PieceType pieceType;
    private PieceColor pieceColor;
    private boolean hasMoved;

    public PieceModel(PieceType pieceType, PieceColor pieceColor) {
        this.pieceType = pieceType;
        this.pieceColor = pieceColor;
    }

    public PieceType getType(){
        return pieceType;
    }
    public PieceColor getColor(){
        return pieceColor;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean moved) {
        this.hasMoved = moved;
    }

    public String toString(){
        return pieceColor + " " + pieceType;
    }
    public char getFENChar() {
        if (pieceType == null || pieceColor == null) {
            return ' ';
        }

        char c = ' ';

        if (pieceType == PieceType.KING) {
            c = 'k';
        } else if (pieceType == PieceType.QUEEN) {
            c = 'q';
        } else if (pieceType == PieceType.ROOK) {
            c = 'r';
        } else if (pieceType == PieceType.BISHOP) {
            c = 'b';
        } else if (pieceType == PieceType.KNIGHT) {
            c = 'n';
        } else if (pieceType == PieceType.PAWN) {
            c = 'p';
        }

        return pieceColor == PieceColor.WHITE ? Character.toUpperCase(c) : c;
    }

}
