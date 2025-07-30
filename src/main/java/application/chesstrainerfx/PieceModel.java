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
}
