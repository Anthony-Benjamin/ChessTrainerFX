package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;

/**
 * Voert een uit de oefening afkomstige (al gevalideerde) zet uit op een bord,
 * inclusief de specials die BoardModel.movePiece niet kent: en passant,
 * de torenzet bij rokade en promotie (uit de SAN, bv. "e8=Q").
 */
public final class ExerciseMoveExecutor {

    private ExerciseMoveExecutor() {
    }

    public static void apply(BoardModel board, Move move, String san, PieceColor color) {
        Position from = move.getFrom();
        Position to = move.getTo();

        SquareModel fromSq = board.getSquare(from);
        SquareModel toSq = board.getSquare(to);
        if (fromSq == null || toSq == null) return;

        PieceModel piece = fromSq.getPiece();
        if (piece == null) return;

        // En passant: pion diagonaal naar leeg veld → geslagen pion weghalen
        if (piece.getType() == PieceType.PAWN) {
            int dx = Math.abs(to.getColumn() - from.getColumn());
            if (dx == 1 && toSq.getPiece() == null) {
                // geslagen pion staat op (from.row, to.col)
                SquareModel capSq = board.getSquare(new Position(from.getRow(), to.getColumn()));
                if (capSq != null) capSq.removePiece();
            }
        }

        board.movePiece(from, to);

        // Double-step tracking voor een eventuele en passant op de volgende zet
        if (piece.getType() == PieceType.PAWN && Math.abs(to.getRow() - from.getRow()) == 2) {
            board.setLastDoubleStepPawnPosition(to);
        } else {
            board.setLastDoubleStepPawnPosition(null);
        }

        // Rokade: toren ook verplaatsen (BoardModel.movePiece doet dat niet)
        if (piece.getType() == PieceType.KING) {
            int dx = to.getColumn() - from.getColumn();
            if (Math.abs(dx) == 2) {
                if (dx > 0) { // korte rokade
                    board.movePiece(new Position(from.getRow(), 7), new Position(from.getRow(), 5));
                } else {      // lange rokade
                    board.movePiece(new Position(from.getRow(), 0), new Position(from.getRow(), 3));
                }
            }
        }

        // Promotie: als SAN "=Q" etc bevat
        if (san != null && san.contains("=")) {
            int idx = san.indexOf('=');
            if (idx + 1 < san.length()) {
                PieceType newType = switch (san.charAt(idx + 1)) {
                    case 'R' -> PieceType.ROOK;
                    case 'B' -> PieceType.BISHOP;
                    case 'N' -> PieceType.KNIGHT;
                    default -> PieceType.QUEEN;
                };
                SquareModel toAfter = board.getSquare(to);
                if (toAfter != null) {
                    toAfter.setPiece(new PieceModel(newType, color));
                }
            }
        }
    }
}
