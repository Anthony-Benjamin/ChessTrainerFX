/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package application.chesstrainerfx;


/**
 *
 * @author ebenjamin
 */
public class MoveValidator {
    public static boolean isValidMove(BoardModel board, PieceModel piece, Position from, Position to){
        System.out.println(piece.getType());
        return switch(piece.getType()){
            case BISHOP -> isValidBishopMove(board, piece, from, to);
            case ROOK -> isValidRookMove(board, piece, from, to);
            case QUEEN -> isValidQueenMove(board, piece, from, to);    
            case KNIGHT -> isValidKnightMove(board, piece, from, to);    
            default -> false;
        };
    }
    
    public static boolean isOnBoard(Position position){
        return position.getRow() >= 0 && position.getRow() < 8 && position.getColumn() >= 0 && position.getColumn() < 8;
    }
    
    public static boolean isValidBishopMove(BoardModel board, PieceModel bishop, Position from, Position to){

//        if (!(isOnBoard(from) || isOnBoard(to) || !from.equals(to))){
//            
//            return false;
//        }
        if(!isOnBoard(from) || !isOnBoard(to) || from.equals(to)){
            
            return false;
        }
        int dy = Math.abs(to.getRow() - from.getRow());
        int dx = Math.abs(to.getColumn() - from.getColumn());
        if(dy != dx){
            return false;
        }
        
        // Direction (+1, or -1)
        int stepRow = Integer.compare(to.row, from.row);
        int stepCol = Integer.compare(to.column, from.column);
        
        int currentRow = from.row + stepRow;
        int currentCol = from.column + stepCol;
        while(currentRow != to.row && currentCol != to.column){
            if(board.getSquare(new Position(currentRow, currentCol)).getPiece() != null){
                return false;
            }
            currentRow += stepRow;
            currentCol += stepCol;
        }
        
        PieceModel target = board.getSquare(new Position(to.row, to.column)).getPiece();
        return target == null || !target.getColor().equals(bishop.getColor());
        
    }
    private static boolean isValidRookMove(BoardModel board, PieceModel rook, Position from, Position to){
        if(!isOnBoard(from) || !isOnBoard(to) || from.equals(to)){
            return false;
        }
        
        boolean sameRow = from.row == to.row;
        boolean sameCol = from.column == to.column;
        if(!sameRow && !sameCol){
            return false;
        }
        
        int stepRow = Integer.compare(to.row, from.row);
        int stepCol = Integer.compare(to.column, from.column);
        
        int r = from.row + stepRow;
        int c = from.column + stepCol;
        while(r != to.row || c != to.column){
            if(board.getSquare(new Position(r, c)) != null){
                return false;
            }
            r+= stepRow;
            c += stepCol;
        }
        
        PieceModel target = board.getSquare(new Position(to.row, to.column)).getPiece();
        return target == null || !target.getColor().equals(rook.getColor());
        
    }
    private static boolean isValidQueenMove(BoardModel board, PieceModel queen, Position from, Position to){
        return isValidBishopMove(board, queen, from, to) || isValidRookMove(board, queen, from, to);
    }
    private static boolean isValidKnightMove(BoardModel board, PieceModel knight, Position from, Position to){
        if(!isOnBoard(from) || !isOnBoard(to) || from.equals(to)){
            return false;
        }
        int dx = Math.abs(to.column - from.column);
        int dy = Math.abs(to.row - from.row);
        
        if(!((dx == 2 && dy ==1) || (dx == 1 && dy ==2))){
            return false;
        }
        
        PieceModel target = board.getSquare(new Position(to.row, to.column)).getPiece();
        return target == null || !target.getColor().equals(knight.getColor());
    }
}
