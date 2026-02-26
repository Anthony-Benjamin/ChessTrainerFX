package application.chesstrainerfx.utils;

import java.util.Objects;

public class Move {
    private final Position from;
    private final Position to;

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public Position getFrom() { return from; }
    public Position getTo() { return to; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move move = (Move) o;
        return Objects.equals(from, move.from) && Objects.equals(to, move.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}