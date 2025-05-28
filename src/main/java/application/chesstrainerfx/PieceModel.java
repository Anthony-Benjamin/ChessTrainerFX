package application.chesstrainerfx;


/**
 *
 * @author ebenjamin
 */
public class PieceModel {
    private PieceType pieceType;
    private PieceColor pieceColor;

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
    
    public String toString(){
        return pieceColor + " " + pieceType; 
    }
}
