package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.pgnreader.model.Exercise;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExerciseSessionBuilder {

    public ExerciseSession buildSessionFromExercise(Exercise exercise) {
        String fen = exercise.getFen();
        if (fen == null || fen.isBlank()) {
            // PGN zonder [FEN]-tag: volledige partij vanaf de startopstelling
            fen = BoardModel.START_FEN;
        }

        SanNode sanRoot = parseSanTree(exercise.getMoves());

        BoardModel temp = new BoardModel();
        temp.initializeFromFEN(fen);

        String[] fenParts = fen.split("\\s+");
        boolean whiteToMove = fenParts.length >= 2 && fenParts[1].equals("w");
        PieceColor toMove = whiteToMove ? PieceColor.WHITE : PieceColor.BLACK;

        List<ExerciseSession.Node> nodesById = new ArrayList<>();
        ExerciseSession.Node root = newSessionNode(nodesById, null, null, null);
        resolveChildren(sanRoot, root, temp, toMove, nodesById);

        return new ExerciseSession(root, nodesById);
    }

    private void resolveChildren(
            SanNode sanNode,
            ExerciseSession.Node sessionNode,
            BoardModel board,
            PieceColor toMove,
            List<ExerciseSession.Node> nodesById) {
        for (SanNode sanChild : sanNode.children.values()) {
            Move move = resolveSanToMove(board, sanChild.san, toMove);
            if (move == null) {
                throw new IllegalStateException("Kon SAN niet resolven: " + sanChild.san);
            }

            ExerciseSession.Node sessionChild = newSessionNode(nodesById, sessionNode, move, sanChild.san);

            BoardModel branchBoard = copyBoard(board, toMove);
            applyMoveOnTempBoard(branchBoard, move, sanChild.san, toMove);
            PieceColor nextToMove = (toMove == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
            resolveChildren(sanChild, sessionChild, branchBoard, nextToMove, nodesById);
        }
    }

    private ExerciseSession.Node newSessionNode(
            List<ExerciseSession.Node> nodesById,
            ExerciseSession.Node parent,
            Move move,
            String san) {
        ExerciseSession.Node node = new ExerciseSession.Node(nodesById.size(), parent, move, san);
        nodesById.add(node);
        if (parent != null) {
            parent.addChild(node);
        }
        return node;
    }

    private BoardModel copyBoard(BoardModel board, PieceColor toMove) {
        BoardModel copy = new BoardModel();
        copy.initializeFromFEN(board.exportToFEN(toMove == PieceColor.WHITE));
        copy.setLastDoubleStepPawnPosition(board.getLastDoubleStepPawnPosition());
        return copy;
    }

    private SanNode parseSanTree(String moveText) {
        SanNode root = new SanNode(null);
        SanNode current = root;
        SanNode lastMoveParent = null;
        Deque<ParseContext> stack = new ArrayDeque<>();

        for (String token : tokenizeMoveText(moveText)) {
            if (token.equals("(")) {
                if (lastMoveParent == null) {
                    throw new IllegalArgumentException("Variant zonder voorafgaande zet: " + moveText);
                }
                stack.push(new ParseContext(current, lastMoveParent));
                current = lastMoveParent;
                lastMoveParent = null;
                continue;
            }
            if (token.equals(")")) {
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Onverwachte sluitende variant-haak in PGN: " + moveText);
                }
                ParseContext context = stack.pop();
                current = context.current;
                lastMoveParent = context.lastMoveParent;
                continue;
            }

            if (!isSanToken(token)) {
                continue;
            }

            SanNode child = current.children.computeIfAbsent(token, SanNode::new);
            lastMoveParent = current;
            current = child;
        }

        if (!stack.isEmpty()) {
            throw new IllegalArgumentException("Niet-afgesloten variant in PGN: " + moveText);
        }
        return root;
    }

    private List<String> tokenizeMoveText(String moveText) {
        if (moveText == null || moveText.isBlank()) {
            return List.of();
        }

        String clean = moveText
                .replace('\u00A0', ' ')
                .replaceAll("(?s)\\{[^}]*}", " ")
                .replaceAll("\\[%[^\\]]*]", " ")
                .replaceAll("\\$\\d+", " ")
                .replaceAll("(?i)\\b(1-0|0-1|1/2-1/2)\\b", " ")
                .replace("*", " ")
                .replace("(", " ( ")
                .replace(")", " ) ")
                .replaceAll("\\s+", " ")
                .trim();

        if (clean.isBlank()) {
            return List.of();
        }

        String[] rawTokens = clean.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String raw : rawTokens) {
            String token = raw
                    .replaceAll("^\\d+\\.+", "")
                    .replaceAll("^\\.+", "")
                    .replaceAll("[!?]+$", "")
                    .trim();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean isSanToken(String token) {
        return !token.equals("(")
                && !token.equals(")")
                && !token.matches("\\d+\\.+")
                && !token.isBlank();
    }

    private Move resolveSanToMove(BoardModel board, String sanRaw, PieceColor color) {
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

    private void applyMoveOnTempBoard(BoardModel board, Move move, String sanRaw, PieceColor color) {
        ExerciseMoveExecutor.apply(board, move, sanRaw, color);
    }

    private static Position pos(String square) {
        int[] rc = CoordinateSystem.coordinateToIndex(square); // [row, col]
        return new Position(rc[0], rc[1]);
    }

    private static class SanNode {
        private final String san;
        private final Map<String, SanNode> children = new LinkedHashMap<>();

        private SanNode(String san) {
            this.san = san;
        }
    }

    private record ParseContext(SanNode current, SanNode lastMoveParent) {
    }
}
