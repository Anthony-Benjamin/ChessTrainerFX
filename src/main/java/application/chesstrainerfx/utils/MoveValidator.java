package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;

public class MoveValidator {

    public static boolean isValidMove(BoardModel board, PieceModel piece, Position from, Position to){
        return switch(piece.getType()){
            case BISHOP -> isValidBishopMove(board, piece, from, to);
            case ROOK -> isValidRookMove(board, piece, from, to);
            case QUEEN -> isValidQueenMove(board, piece, from, to);
            case KNIGHT -> isValidKnightMove(board, piece, from, to);
            case KING -> isValidKingMove(board, piece, from, to);
            case PAWN -> isValidPawnMove(board, piece, from, to);
        };
    }
    
    public static boolean isOnBoard(Position position){
        return position.getRow() >= 0 && position.getRow() < 8 && position.getColumn() >= 0 && position.getColumn() < 8;
    }
    
    public static boolean isValidBishopMove(BoardModel board, PieceModel bishop, Position from, Position to){
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
            return board.getSquare(new Position(row, 5)).getPiece() == null &&
                    board.getSquare(new Position(row, 6)).getPiece() == null;

            // Extra: voeg hier controle toe of koning door/over schaak beweegt
        }

        // Lange rokade
        if (direction == -1 && colTo == 2) {
            Position rookPos = new Position(row, 0);
            PieceModel rook = board.getSquare(rookPos).getPiece();
            if (rook == null || rook.getType() != PieceType.ROOK || rook.hasMoved()) return false;

            // Check of vakken tussen koning en toren leeg zijn
            return board.getSquare(new Position(row, 1)).getPiece() == null &&
                    board.getSquare(new Position(row, 2)).getPiece() == null &&
                    board.getSquare(new Position(row, 3)).getPiece() == null;

            // Extra: voeg hier controle toe of koning door/over schaak beweegt
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
        }

        return false;
    }

    // ---------------- Schaak / mat detectie ---------------- //

    /** Zoekt de positie van de koning van de gegeven kleur (null als die ontbreekt). */
    public static Position findKing(BoardModel board, PieceColor color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                PieceModel p = board.getSquare(new Position(row, col)).getPiece();
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }

    private static PieceColor opposite(PieceColor color) {
        return color == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
    }

    /** Wordt {@code square} aangevallen door minstens één stuk van {@code byColor}? */
    public static boolean isSquareAttackedBy(BoardModel board, Position square, PieceColor byColor) {
        if (square == null) return false;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position from = new Position(row, col);
                PieceModel attacker = board.getSquare(from).getPiece();
                if (attacker == null || attacker.getColor() != byColor) continue;
                if (isValidMove(board, attacker, from, square)) return true;
            }
        }
        return false;
    }

    /** Staat de koning van {@code color} schaak? */
    public static boolean isKingInCheck(BoardModel board, PieceColor color) {
        Position kingPos = findKing(board, color);
        if (kingPos == null) return false;
        return isSquareAttackedBy(board, kingPos, opposite(color));
    }

    /**
     * Simuleert een zet en kijkt of de eigen koning daarna schaak staat.
     * De bordtoestand wordt na de controle volledig hersteld.
     */
    public static boolean moveLeavesKingInCheck(BoardModel board, PieceModel piece,
                                                Position from, Position to) {
        SquareModel fromSq = board.getSquare(from);
        SquareModel toSq = board.getSquare(to);
        if (fromSq == null || toSq == null) return false;

        PieceModel moving = fromSq.getPiece();
        PieceModel captured = toSq.getPiece();

        // En-passant: een pion die schuin naar een leeg veld gaat, slaat de pion ernaast.
        SquareModel epSquare = null;
        PieceModel epPawn = null;
        if (moving != null && moving.getType() == PieceType.PAWN
                && from.getColumn() != to.getColumn() && captured == null) {
            epSquare = board.getSquare(new Position(from.getRow(), to.getColumn()));
            if (epSquare != null) epPawn = epSquare.getPiece();
        }

        // Zet simuleren.
        fromSq.setPiece(null);
        toSq.setPiece(moving);
        if (epSquare != null) epSquare.setPiece(null);

        boolean inCheck = isKingInCheck(board, piece.getColor());

        // Bord herstellen.
        fromSq.setPiece(moving);
        toSq.setPiece(captured);
        if (epSquare != null) epSquare.setPiece(epPawn);

        return inCheck;
    }

    /** Volledig legale zet: geometrisch geldig én laat de eigen koning niet schaak staan. */
    public static boolean isLegalMove(BoardModel board, PieceModel piece,
                                      Position from, Position to) {
        return isValidMove(board, piece, from, to)
                && !moveLeavesKingInCheck(board, piece, from, to);
    }

    /** Heeft {@code color} nog minstens één legale zet? */
    public static boolean hasAnyLegalMove(BoardModel board, PieceColor color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position from = new Position(row, col);
                PieceModel p = board.getSquare(from).getPiece();
                if (p == null || p.getColor() != color) continue;
                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        if (isLegalMove(board, p, from, new Position(tr, tc))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
