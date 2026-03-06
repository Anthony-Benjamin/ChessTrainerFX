package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;

public final class MoveExecutor {

    private MoveExecutor() {
        // utility class
    }

    public static void executeMove(BoardModel board, Move move) {
        executeMove(board, move, null);
    }

    public static void executeMove(BoardModel board, Move move, PieceType promotionPieceType) {
        if (board == null || move == null) return;

        Position from = move.from();
        Position to = move.to();

        if (from == null || to == null) return;

        SquareModel source = board.getSquare(from);
        SquareModel target = board.getSquare(to);

        if (source == null || target == null) return;

        PieceModel piece = source.getPiece();
        if (piece == null) return;

        // reset default en-passant info; wordt hieronder opnieuw gezet als het een dubbele pionzet is
        board.setLastDoubleStepPawnPosition(null);

        // 1. en passant capture
        handleEnPassant(board, piece, from, to);

        // 2. rokade: eerst koning verplaatsen, daarna toren
        boolean isCastling = isCastlingMove(piece, from, to);

        // 3. gewone zet
        board.movePiece(from, to);

        // 4. rokade toren mee verplaatsen
        if (isCastling) {
            moveRookForCastling(board, from, to);
        }

        // 5. dubbele pionzet tracken
        handleDoublePawnStep(board, piece, from, to);

        // 6. promotie
        handlePromotion(board, piece, to, promotionPieceType);

        // 7. hasMoved bijwerken
        PieceModel movedPiece = board.getSquare(to).getPiece();
        if (movedPiece != null) {
            movedPiece.setHasMoved(true);
        }
    }

    private static void handleEnPassant(BoardModel board, PieceModel piece, Position from, Position to) {
        if (piece.getType() != PieceType.PAWN) return;

        int dx = Math.abs(to.getColumn() - from.getColumn());
        int dy = to.getRow() - from.getRow();
        int dir = piece.getColor() == PieceColor.WHITE ? -1 : 1;

        SquareModel targetSquare = board.getSquare(to);
        if (targetSquare == null) return;

        boolean diagonalStep = dx == 1 && dy == dir;
        boolean targetEmpty = targetSquare.getPiece() == null;

        if (diagonalStep && targetEmpty) {
            Position capturedPawnPos = new Position(from.getRow(), to.getColumn());
            SquareModel capturedSquare = board.getSquare(capturedPawnPos);
            if (capturedSquare != null) {
                capturedSquare.removePiece();
            }
        }
    }

    private static void handleDoublePawnStep(BoardModel board, PieceModel piece, Position from, Position to) {
        if (piece.getType() != PieceType.PAWN) return;

        int dy = Math.abs(to.getRow() - from.getRow());
        if (dy == 2) {
            board.setLastDoubleStepPawnPosition(to);
        }
    }

    private static boolean isCastlingMove(PieceModel piece, Position from, Position to) {
        if (piece.getType() != PieceType.KING) return false;

        int dx = Math.abs(to.getColumn() - from.getColumn());
        int dy = Math.abs(to.getRow() - from.getRow());

        return dy == 0 && dx == 2;
    }

    private static void moveRookForCastling(BoardModel board, Position from, Position to) {
        int row = from.getRow();
        int dx = to.getColumn() - from.getColumn();

        if (dx > 0) {
            // korte rokade
            Position rookFrom = new Position(row, 7);
            Position rookTo = new Position(row, 5);
            board.movePiece(rookFrom, rookTo);

            PieceModel rook = board.getSquare(rookTo).getPiece();
            if (rook != null) {
                rook.setHasMoved(true);
            }
        } else {
            // lange rokade
            Position rookFrom = new Position(row, 0);
            Position rookTo = new Position(row, 3);
            board.movePiece(rookFrom, rookTo);

            PieceModel rook = board.getSquare(rookTo).getPiece();
            if (rook != null) {
                rook.setHasMoved(true);
            }
        }
    }

    private static void handlePromotion(BoardModel board, PieceModel originalPiece, Position to, PieceType promotionPieceType) {
        if (originalPiece.getType() != PieceType.PAWN) return;

        int promotionRow = originalPiece.getColor() == PieceColor.WHITE ? 0 : 7;
        if (to.getRow() != promotionRow) return;

        PieceType finalType = (promotionPieceType != null) ? promotionPieceType : PieceType.QUEEN;
        PieceModel promoted = new PieceModel(finalType, originalPiece.getColor());
        promoted.setHasMoved(true);
        board.getSquare(to).setPiece(promoted);
    }
}