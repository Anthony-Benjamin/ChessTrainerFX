package application.chesstrainerfx.model;

import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.utils.Position;

public class SquareModel {
    private final Position position;
    private PieceModel piece;

    public SquareModel(Position position){
        this.position = position;
    }

    public Position getPosition(){
        return position;
    }

    public PieceModel getPiece(){
        return piece;
    }

    public void setPiece(PieceModel piece){
        this.piece = piece;
    }

    public void removePiece(){
        piece = null;
    }
}
