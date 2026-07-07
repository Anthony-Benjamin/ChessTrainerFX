package application.chesstrainerfx.utils;

import java.util.ArrayList;
import java.util.List;

public class ExerciseSession {

    public static class Node {
        private final int id;
        private final Node parent;
        private final Move move;
        private final String san;
        private final List<Node> children = new ArrayList<>();

        Node(int id, Node parent, Move move, String san) {
            this.id = id;
            this.parent = parent;
            this.move = move;
            this.san = san;
        }

        public int getId() {
            return id;
        }

        public Move getMove() {
            return move;
        }

        public String getSan() {
            return san;
        }

        public List<Node> getChildren() {
            return List.copyOf(children);
        }

        void addChild(Node child) {
            children.add(child);
        }

        @Override
        public String toString() {
            return san == null ? "" : san;
        }
    }

    private final Node root;
    private final List<Node> nodesById;
    private Node current;
    private Node pendingAdvance;

    public ExerciseSession(Node root, List<Node> nodesById) {
        this.root = root;
        this.nodesById = nodesById;
        this.current = root;
    }

    public ExerciseSession(List<Move> moves, List<String> sanMoves) {
        this.root = new Node(0, null, null, null);
        this.nodesById = new ArrayList<>();
        this.nodesById.add(root);

        Node node = root;
        for (int i = 0; i < moves.size(); i++) {
            String san = i < sanMoves.size() ? sanMoves.get(i) : "";
            Node child = new Node(nodesById.size(), node, moves.get(i), san);
            node.addChild(child);
            nodesById.add(child);
            node = child;
        }
        this.current = root;
    }

    public Move getExpectedMove() {
        Node expected = firstCandidate();
        return expected == null ? null : expected.move;
    }

    public String getExpectedSan() {
        List<String> candidates = getExpectedSanOptions();
        if (candidates.isEmpty()) return null;
        return String.join(" or ", candidates);
    }

    public List<String> getExpectedSanOptions() {
        return current.children.stream()
                .map(Node::getSan)
                .toList();
    }

    public List<Node> getCandidateNodes() {
        return current.getChildren();
    }

    public boolean isCorrectMove(Move userMove) {
        for (Node child : current.children) {
            if (child.move.equals(userMove)) {
                pendingAdvance = child;
                return true;
            }
        }
        return false;
    }

    public void advancePly() {
        Node next = pendingAdvance != null ? pendingAdvance : firstCandidate();
        pendingAdvance = null;
        if (next != null) {
            current = next;
        }
    }

    public void advanceTo(Node node) {
        if (node != null && current.children.contains(node)) {
            current = node;
            pendingAdvance = null;
        }
    }

    public void rewindPly() {
        pendingAdvance = null;
        if (current.parent != null) {
            current = current.parent;
        }
    }

    public boolean hasNext() {
        return !current.children.isEmpty();
    }

    public int getIndex() {
        int index = 0;
        Node node = current;
        while (node.parent != null) {
            index++;
            node = node.parent;
        }
        return index;
    }

    public int getCurrentNodeId() {
        return current.id;
    }

    public void setCurrentNodeId(int nodeId) {
        if (nodeId >= 0 && nodeId < nodesById.size()) {
            current = nodesById.get(nodeId);
            pendingAdvance = null;
        }
    }

    private Node firstCandidate() {
        return current.children.isEmpty() ? null : current.children.get(0);
    }
}
