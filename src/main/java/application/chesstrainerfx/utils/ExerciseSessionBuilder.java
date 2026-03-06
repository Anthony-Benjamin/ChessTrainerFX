package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.pgnreader.model.Exercise;

import java.util.ArrayList;
import java.util.List;

public class ExerciseSessionBuilder {

    public ExerciseSession buildSessionFromExercise(Exercise exercise) {
        String fen = exercise.fen();

        // PGN schoonmaken
        String main = PgnUtils.cleanMoveString(exercise.moves());
        main = main.replaceAll("\\{[^}]*}", " ");
        main = main.replaceAll("\\[[^]]*]", " ");
        main = main.replace("*", " ");
        main = main.replaceAll("(?i)\\b(1-0|0-1|1/2-1/2)\\b", " ");
        main = main.replaceAll("\\s+", " ").trim();

        ParsedMoves parsed = ChessMoveParser.parseMoves(main);

        // Interleave naar ply-volgorde: w1, b1, w2, b2, ...
        List<String> sanPlies = new ArrayList<>();
        int max = Math.max(parsed.whiteMoves.size(), parsed.blackMoves.size());
        for (int i = 0; i < max; i++) {
            if (i < parsed.whiteMoves.size()) sanPlies.add(parsed.whiteMoves.get(i));
            if (i < parsed.blackMoves.size()) sanPlies.add(parsed.blackMoves.get(i));
        }

        // Resolver gebruikt een temp board zodat je echte board niet verandert
        BoardModel temp = new BoardModel();
        temp.initializeFromFEN(fen);

        boolean whiteToMove = fen.split("\\s+").length >= 2 && fen.split("\\s+")[1].equals("w");
        PieceColor toMove = whiteToMove ? PieceColor.WHITE : PieceColor.BLACK;

        List<Move> mainLine = new ArrayList<>();
        List<String> sanLine = new ArrayList<>();

        for (String san : sanPlies) {
            if (san == null || san.isBlank()) continue;

            Move move = resolveSanToMove(temp, san, toMove);
            if (move == null) {
                throw new IllegalStateException("Kon SAN niet resolven: " + san);
            }

            mainLine.add(move);
            sanLine.add(san);

            applyMoveOnTempBoard(temp, move, san, toMove);
            toMove = (toMove == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        }

        System.out.println("buildsession mainline: " + mainLine);
        List<VariationNode> rootOptions = buildLinearVariation(mainLine, sanLine);
        return new ExerciseSession(rootOptions);
    }

    private Move resolveSanToMove(BoardModel board, String sanRaw, PieceColor color) {
        String san = sanRaw.replace("+", "").replace("#", "").trim();

        // Rokade
        if (san.equals("O-O") || san.equals("0-0")) {
            Position from = (color == PieceColor.WHITE) ? pos("e1") : pos("e8");
            Position to = (color == PieceColor.WHITE) ? pos("g1") : pos("g8");
            return new Move(from, to);
        }
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            Position from = (color == PieceColor.WHITE) ? pos("e1") : pos("e8");
            Position to = (color == PieceColor.WHITE) ? pos("c1") : pos("c8");
            return new Move(from, to);
        }

        // Promotie-strip: e8=Q / dxe8=Q
        if (san.contains("=")) {
            san = san.substring(0, san.indexOf('='));
        }

        // Target square = laatste 2 chars
        if (san.length() < 2) return null;
        String targetSq = san.substring(san.length() - 2);
        Position to = pos(targetSq);

        // Piece type bepalen
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
                default -> PieceType.PAWN;
            };
            idx = 1;
        }

        // Disambiguatie: bv Nbd2 of R1e1
        String middle = san.substring(idx, san.length() - 2);
        middle = middle.replace("x", "");

        Character fromFileHint = null;
        Character fromRankHint = null;

        for (char c : middle.toCharArray()) {
            if (c >= 'a' && c <= 'h') fromFileHint = c;
            if (c >= '1' && c <= '8') fromRankHint = c;
        }

        List<Position> candidates = new ArrayList<>();

        for (SquareModel sq : board.getSquares()) {
            PieceModel piece = sq.getPiece();
            if (piece == null) continue;
            if (piece.getColor() != color) continue;
            if (piece.getType() != type) continue;

            Position from = sq.getPosition();

            if (fromFileHint != null) {
                int fileCol = fromFileHint - 'a';
                if (from.getColumn() != fileCol) continue;
            }

            if (fromRankHint != null) {
                int rank = fromRankHint - '1';
                int rowExpected = 7 - rank;
                if (from.getRow() != rowExpected) continue;
            }

            if (MoveValidator.isValidMove(board, piece, from, to)) {
                candidates.add(from);
            }
        }

        // Pawn capture hint: fxe6
        if (type == PieceType.PAWN && sanRaw.contains("x") && !sanRaw.isEmpty()) {
            char file = sanRaw.charAt(0);
            if (file >= 'a' && file <= 'h') {
                int col = file - 'a';
                candidates.removeIf(pos -> pos.getColumn() != col);
            }
        }

        if (candidates.size() != 1) {
            System.out.println("SAN resolve ambiguity: " + sanRaw + " candidates=" + candidates);
            return null;
        }

        return new Move(candidates.get(0), to);
    }

    private void applyMoveOnTempBoard(BoardModel board, Move move, String sanRaw, PieceColor color) {
        PieceType promotionType = null;

        if (sanRaw.contains("=")) {
            char promo = sanRaw.charAt(sanRaw.indexOf('=') + 1);
            promotionType = switch (promo) {
                case 'Q' -> PieceType.QUEEN;
                case 'R' -> PieceType.ROOK;
                case 'B' -> PieceType.BISHOP;
                case 'N' -> PieceType.KNIGHT;
                default -> PieceType.QUEEN;
            };
        }

        MoveExecutor.executeMove(board, move, promotionType);
    }

    private static Position pos(String square) {
        int[] rc = CoordinateSystem.coordinateToIndex(square);
        return new Position(rc[0], rc[1]);
    }
    private List<VariationNode> buildLinearVariation(List<Move> moves, List<String> sans) {
        List<VariationNode> root = new ArrayList<>();

        VariationNode previous = null;

        for (int i = 0; i < moves.size(); i++) {
            VariationNode node = new VariationNode(moves.get(i), sans.get(i));

            if (previous == null) {
                root.add(node);
            } else {
                previous.addNext(node);
            }

            previous = node;
        }

        return root;
    }
}