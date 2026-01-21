package application.chesstrainerfx.utils;

public class Move {
    private final Position from;
    private final Position to;

    // later: promotion, SAN, etc.

    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }
}
