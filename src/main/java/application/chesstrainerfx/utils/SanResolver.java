package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Herleidt een SAN-zet (bv. "Nbd2", "fxe6", "O-O") tot een concrete zet op een
 * bord, met MoveValidator voor legaliteit en ondersteuning voor file/rank-hints.
 * Levert null als de zet niet tot precies één kandidaat herleid kan worden.
 */
public final class SanResolver {

    private SanResolver() {
    }

    public static Move resolve(BoardModel board, String sanRaw, PieceColor color) {
        String san = sanRaw.replace("+", "").replace("#", "").trim();

        // Rokade
        if (san.equals("O-O") || san.equals("0-0")) {
            Position from = (color == PieceColor.WHITE) ? pos("e1") : pos("e8");
            Position to   = (color == PieceColor.WHITE) ? pos("g1") : pos("g8");
            return new Move(from, to);
        }
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            Position from = (color == PieceColor.WHITE) ? pos("e1") : pos("e8");
            Position to   = (color == PieceColor.WHITE) ? pos("c1") : pos("c8");
            return new Move(from, to);
        }

        // promotie: e8=Q / dxe8=Q
        if (san.contains("=")) {
            san = san.substring(0, san.indexOf('=')); // strip "=Q" etc (promotion afhandeling doen we later bij apply)
        }

        // target square = laatste 2 chars (bv "e6")
        if (san.length() < 2) return null;
        String targetSq = san.substring(san.length() - 2);
        Position to = pos(targetSq);

        // piece type bepalen
        PieceType type = PieceType.PAWN;
        int idx = 0;
        char first = san.charAt(0);
        if (Character.isUpperCase(first)) {
            type = switch (first) {
                case 'K' -> PieceType.KING;
                case 'Q' -> PieceType.QUEEN;
                case 'R' -> PieceType.ROOK;
                case 'B' -> PieceType.BISHOP;
                case 'N' -> PieceType.KNIGHT;
                default  -> PieceType.PAWN;
            };
            idx = 1;
        }

        // disambiguatie: bv Nbd2 of R1e1 (we ondersteunen file/rank hints)
        String middle = san.substring(idx, san.length() - 2);
        middle = middle.replace("x", "");

        Character fromFileHint = null; // 'a'..'h'
        Character fromRankHint = null; // '1'..'8'

        for (char c : middle.toCharArray()) {
            if (c >= 'a' && c <= 'h') fromFileHint = c;
            if (c >= '1' && c <= '8') fromRankHint = c;
        }

        // Kandidaten zoeken: scan 8x8 via getSquare()
        List<Position> candidates = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                SquareModel sq = board.getSquare(new Position(r, c));
                if (sq == null) continue;

                PieceModel p = sq.getPiece();
                if (p == null) continue;
                if (p.getColor() != color) continue;
                if (p.getType() != type) continue;

                Position from = sq.getPosition();

                // hints filteren
                if (fromFileHint != null) {
                    int fileCol = fromFileHint - 'a';
                    if (from.getColumn() != fileCol) continue;
                }
                if (fromRankHint != null) {
                    int rank = fromRankHint - '1'; // 0..7
                    int rowExpected = 7 - rank;    // rank '1' is row 7
                    if (from.getRow() != rowExpected) continue;
                }

                if (MoveValidator.isValidMove(board, p, from, to)) {
                    candidates.add(from);
                }
            }
        }

        // Pawn capture hint: "fxe6" → file hint zit aan begin
        if (type == PieceType.PAWN && sanRaw.contains("x") && !sanRaw.isEmpty()) {
            char file = sanRaw.charAt(0);
            if (file >= 'a' && file <= 'h') {
                int col = file - 'a';
                candidates.removeIf(pos -> pos.getColumn() != col);
            }
        }

        if (candidates.size() != 1) {
            return null;
        }

        return new Move(candidates.get(0), to);
    }

    private static Position pos(String square) {
        int[] rc = CoordinateSystem.coordinateToIndex(square); // [row, col]
        return new Position(rc[0], rc[1]);
    }
}
