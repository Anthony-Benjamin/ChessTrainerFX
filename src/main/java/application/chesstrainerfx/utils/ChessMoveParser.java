package application.chesstrainerfx.utils;

import java.util.ArrayList;
import java.util.List;

public class ChessMoveParser {

    public static ParsedMoves parseMoves(String input) {
        List<String> white = new ArrayList<>();
        List<String> black = new ArrayList<>();

        // 1. Verwijder zetnummers zoals "1." en "4..."
        String cleaned = input.replaceAll("\\d+\\.\\.\\.|\\d+\\.", "").trim();

        // 2. Split op spaties
        String[] moves = cleaned.split("\\s+");

        boolean whiteTurn = true;

        for (String move : moves) {
            move = move.replace("+", "");
            if (whiteTurn) {
                white.add(move);
            } else {
                black.add(move);
            }
            whiteTurn = !whiteTurn;
        }

        return new ParsedMoves(white, black);
    }
}
