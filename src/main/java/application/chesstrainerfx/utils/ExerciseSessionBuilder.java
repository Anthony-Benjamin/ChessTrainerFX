package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
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
            Move move = SanResolver.resolve(board, sanChild.san, toMove);
            if (move == null) {
                throw new IllegalStateException("Could not resolve SAN: " + sanChild.san);
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
                    throw new IllegalArgumentException("Variation without a preceding move: " + moveText);
                }
                stack.push(new ParseContext(current, lastMoveParent));
                current = lastMoveParent;
                lastMoveParent = null;
                continue;
            }
            if (token.equals(")")) {
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Unexpected closing variation bracket in PGN: " + moveText);
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
            throw new IllegalArgumentException("Unclosed variation in PGN: " + moveText);
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

    private void applyMoveOnTempBoard(BoardModel board, Move move, String sanRaw, PieceColor color) {
        ExerciseMoveExecutor.apply(board, move, sanRaw, color);
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
