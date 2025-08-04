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
            case KING -> isValidKingMove(board, piece, from, to);
            case PAWN -> isValidPawnMove(board, piece, from, to);

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
            if(board.getSquare(new Position(r, c)).getPiece() != null){
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
    private static boolean isValidKingMove(BoardModel board, PieceModel king, Position from, Position to){
        if (!isOnBoard(from) || !isOnBoard(to)) return false;

        int dx = Math.abs(to.getColumn() - from.getColumn());
        int dy = Math.abs(to.getRow() - from.getRow());

        // Normale koningszet (1 vak in elke richting)
        if ((dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0)) {
            PieceModel target = board.getSquare(to).getPiece();
            return target == null || !target.getColor().equals(king.getColor());
        }

        // Rokade: check alleen horizontale zet van koning over 2 kolommen
        if (dy == 0 && dx == 2) {
            return isValidCastleMove(board, king, from, to);
        }

        return false;
    }
    private static boolean isValidCastleMove(BoardModel board, PieceModel king, Position from, Position to) {
        if (king.hasMoved()) return false;

        int row = from.getRow();
        int colFrom = from.getColumn();
        int colTo = to.getColumn();

        int direction = (colTo - colFrom) > 0 ? 1 : -1;

        // Korte rokade
        if (direction == 1 && colTo == 6) {
            Position rookPos = new Position(row, 7);
            PieceModel rook = board.getSquare(rookPos).getPiece();
            if (rook == null || rook.getType() != PieceType.ROOK || rook.hasMoved()) return false;

            // Check of vakken tussen koning en toren leeg zijn
            if (board.getSquare(new Position(row, 5)).getPiece() != null ||
                    board.getSquare(new Position(row, 6)).getPiece() != null) return false;

            // Extra: voeg hier controle toe of koning door/over schaak beweegt
            return true;
        }

        // Lange rokade
        if (direction == -1 && colTo == 2) {
            Position rookPos = new Position(row, 0);
            PieceModel rook = board.getSquare(rookPos).getPiece();
            if (rook == null || rook.getType() != PieceType.ROOK || rook.hasMoved()) return false;

            // Check of vakken tussen koning en toren leeg zijn
            if (board.getSquare(new Position(row, 1)).getPiece() != null ||
                    board.getSquare(new Position(row, 2)).getPiece() != null ||
                    board.getSquare(new Position(row, 3)).getPiece() != null) return false;

            // Extra: voeg hier controle toe of koning door/over schaak beweegt
            return true;
        }

        return false;
    }
    private static boolean isValidPawnMove(BoardModel board, PieceModel pawn, Position from, Position to) {
        if (!isOnBoard(from) || !isOnBoard(to) || from.equals(to)) return false;

        int direction = pawn.getColor() == PieceColor.WHITE ? -1 : 1;
        int startRow = pawn.getColor() == PieceColor.WHITE ? 6 : 1;
        int finalRow = pawn.getColor() == PieceColor.WHITE ? 0 : 7;

        int dy = to.row - from.row;
        int dx = Math.abs(to.column - from.column);

        PieceModel target = board.getSquare(to).getPiece();

        // 1 stap vooruit
        if (dx == 0 && dy == direction && target == null) {
            return true;
        }

        // 2 stappen vooruit vanaf beginpositie
        if (dx == 0 && dy == 2 * direction && from.row == startRow) {
            Position between = new Position(from.row + direction, from.column);
            if (board.getSquare(between).getPiece() == null && target == null) {
                return true;
            }
        }

        // Diagonaal slaan
        if (dx == 1 && dy == direction) {
            // Normaal slaan
            if (target != null && !target.getColor().equals(pawn.getColor())) {
                return true;
            }

            // En passant

            Position capturedPawnPos = new Position(from.row, to.column);

            PieceModel epPawn = board.getSquare(capturedPawnPos).getPiece();
            Position lastDouble = board.getLastDoubleStepPawnPosition();

            if (epPawn != null && epPawn.getType() == PieceType.PAWN &&
                    !epPawn.getColor().equals(pawn.getColor()) &&
                    capturedPawnPos.equals(lastDouble)) {
                     return true;
            }
            System.out.println("En passant poging:");
            System.out.println("lastDoubleStep = " + lastDouble);
            System.out.println("captured pos = " + capturedPawnPos);
        }

        return false;
    }


}
