package application.chesstrainerfx.utils;

import java.util.Objects;

public record Move(Position from, Position to) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move move)) return false;
        return Objects.equals(from, move.from) && Objects.equals(to, move.to);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}