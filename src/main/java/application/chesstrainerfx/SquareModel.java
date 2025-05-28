package application.chesstrainerfx;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author ebenjamin
 */
public class SquareModel {
    private Position position;
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
