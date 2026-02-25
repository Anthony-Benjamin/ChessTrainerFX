package application.chesstrainerfx.utils;

import java.util.*;
import java.util.regex.*;

public class ChessMoveParser {

    public static void main(String[] args) {
        String input = "1. Rxe6+ fxe6 2. Bg6+ Rf7 3. Qxf7+ Kd8 4... Qxd7";

        ParsedMoves result = parseMoves(input);

        System.out.println("Wit: " + result.whiteMoves);
        System.out.println("Zwart: " + result.blackMoves);
    }

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

//    static class ParsedMoves {
//        List<String> whiteMoves;
//        List<String> blackMoves;
//
//        ParsedMoves(List<String> w, List<String> b) {
//            this.whiteMoves = w;
//            this.blackMoves = b;
//        }
//    }
}
