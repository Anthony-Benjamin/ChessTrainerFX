package application.chesstrainerfx.view;

import application.chesstrainerfx.controller.Controller;
import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.*;
import application.pgnreader.model.Exercise;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static application.chesstrainerfx.utils.ChessMoveParser.parseMoves;

public class Test extends Application  {

    private BoardModel boardModel;
    Controller controller;



    private Exercise exercise;


    public static void main(String[] args){
        launch();
    }

    static String removeVariations(String pgn){
        StringBuilder result = new StringBuilder();
        int depth = 0;

        System.out.println(pgn.toCharArray());

        for(char c : pgn.toCharArray()){
            if(c == '('){
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0) {
                result.append(c);
            }
//            System.out.println("depth: " + depth);
        }

        return result.toString();
    }
    private static Position pos(String square) {
        int[] rc = CoordinateSystem.coordinateToIndex(square); // [row, col]
        return new Position(rc[0], rc[1]);
    }
    @Override
    public void start(Stage stage) throws Exception {

        StackPane root = new StackPane();

        // 1) Maak de exercise eerst (zodat je FEN + moves hebt)
     /*   exercise = new Exercise(
                "test",
                "4kr2/3b1p2/4pQ1p/q5b1/8/2p4P/1rB2PP1/3RR1K1 w - - 0 1",
                "{[#]} 1. Rxe6+ fxe6 (1... Bxe6 2.Ba4+ Qxa4 (2... Rb5 3. Bxb5+ Qxb5 4. Rd8#) 3.Rd8#) 2. Bg6+ Rf7 3. Qxf7+ Kd8 4.Qxd7#",
                ""
        );*/
        // test exercise
        exercise = new Exercise(
                "Example 2",
                "8/5p1k/1p3Qpp/2q5/3b4/1b1B3P/1r3PP1/R5K1 w - - 0 1",
                "{[#]} 1. Bxg6+ fxg6 2. Ra7+ Kg8 3. Ra8+ Kh7 4. Rh8# *",
                ""
        );


        // 2) Initialiseer board vanuit de exercise FEN
        boardModel = new BoardModel();
        boardModel.initializeFromFEN(exercise.getFen());

        // 3) Zet "side to move" uit de FEN
        boolean whiteToMove = exercise.getFen().split("\\s+")[1].equals("w");

        // 4) Controller + session koppelen
        controller = new Controller();
        controller.setWhiteTurn(whiteToMove);

        ExerciseSession session = buildSessionFromExercise(exercise);
        controller.setExerciseSession(session);

        // 5) View bouwen met correcte turn
        BoardView boardView = new BoardView(boardModel, controller, whiteToMove, 720);
        root.getChildren().add(boardView);

        // 6) Scene
        Scene scene = new Scene(root, 900, 900);
        stage.setTitle("ChessTrainer — " + exercise.getTitle());
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }
    private ExerciseSession buildSessionFromExercise(Exercise exercise) {

        String fen = exercise.getFen();

        // PGN: variaties eruit + comments/annotations eruit
        String main = removeVariations(exercise.getMoves());
        main = main.replaceAll("\\{[^}]*}", " ");                // {...} weg
        main = main.replaceAll("\\[[^]]*]", " ");                // [..] weg (soms)
        main = main.replace("*", " "); // harde fix voor losstaande *
        main = main.replaceAll("(?i)\\b(1-0|0-1|1/2-1/2)\\b", " "); // normale resultaten
        main = main.replaceAll("\\s+", " ").trim();

        ParsedMoves parsed = ChessMoveParser.parseMoves(main);

        // Interleave naar ply-volgorde: w1, b1, w2, b2, ...
        List<String> sanPlies = new ArrayList<>();
        int max = Math.max(parsed.whiteMoves.size(), parsed.blackMoves.size());
        for (int i = 0; i < max; i++) {
            if (i < parsed.whiteMoves.size()) sanPlies.add(parsed.whiteMoves.get(i));
            if (i < parsed.blackMoves.size()) sanPlies.add(parsed.blackMoves.get(i));
        }

        // Resolver gebruikt een TEMP board zodat je echte boardModel niet verandert
        BoardModel temp = new BoardModel();
        temp.initializeFromFEN(fen);

        boolean whiteToMove = fen.split("\\s+").length >= 2 && fen.split("\\s+")[1].equals("w");
        PieceColor toMove = whiteToMove ? PieceColor.WHITE : PieceColor.BLACK;

        List<Move> mainLine = new ArrayList<>();

        for (String san : sanPlies) {
            if (san == null || san.isBlank()) continue;

            Move m = resolveSanToMove(temp, san, toMove);
            if (m == null) {
                throw new IllegalStateException("Kon SAN niet resolven: " + san + " voor " + toMove);
            }

            mainLine.add(m);
            applyMoveOnTempBoard(temp, m, san, toMove);

            // wissel kant
            toMove = (toMove == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        }

        return new ExerciseSession(mainLine);
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
        String promotion = null;
        if (san.contains("=")) {
            promotion = san.substring(san.indexOf('=') + 1); // "Q" etc
            san = san.substring(0, san.indexOf('='));        // strip "=Q"
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

        // disambiguatie: bv Nbd2 of R1e1 of Qh4e1 (we ondersteunen file/rank)
        String middle = san.substring(idx, san.length() - 2);
        middle = middle.replace("x", ""); // capture marker eruit

        Character fromFileHint = null; // 'a'..'h'
        Character fromRankHint = null; // '1'..'8'
        for (char c : middle.toCharArray()) {
            if (c >= 'a' && c <= 'h') fromFileHint = c;
            if (c >= '1' && c <= '8') fromRankHint = c;
        }

        List<Position> candidates = new ArrayList<>();

        for (SquareModel sq : board.getSquares()) {
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
                int rank = fromRankHint - '1';     // 0..7
                int rowExpected = 7 - rank;        // want rank '1' is row 7 in jouw systeem
                if (from.getRow() != rowExpected) continue;
            }

            if (MoveValidator.isValidMove(board, p, from, to)) {
                candidates.add(from);
            }
        }

        // Pawn capture hint: "fxe6" → file hint zit aan begin
        if (type == PieceType.PAWN && sanRaw.contains("x") && sanRaw.length() >= 1) {
            char file = sanRaw.charAt(0);
            if (file >= 'a' && file <= 'h') {
                int col = file - 'a';
                candidates.removeIf(pos -> pos.getColumn() != col);
            }
        }

        if (candidates.size() != 1) {
            // bij 0 of meerdere: voorlopig fail fast (dan weten we welke SAN lastig is)
            System.out.println("SAN resolve ambiguity: " + sanRaw + " candidates=" + candidates);
            return null;
        }

        return new Move(candidates.get(0), to);
    }

    private void applyMoveOnTempBoard(BoardModel board, Move move, String sanRaw, PieceColor color) {
        Position from = move.getFrom();
        Position to = move.getTo();

        PieceModel piece = board.getSquare(from).getPiece();
        if (piece == null) return;

        // En passant (simpel): pawn diagonaal naar leeg veld → remove captured pawn
        if (piece.getType() == PieceType.PAWN) {
            PieceModel target = board.getSquare(to).getPiece();
            int dx = Math.abs(to.getColumn() - from.getColumn());
            if (dx == 1 && target == null) {
                // captured pawn staat op (from.row, to.col)
                Position cap = new Position(from.getRow(), to.getColumn());
                board.getSquare(cap).removePiece();
            }
        }

        // Move piece
        board.movePiece(from, to);

        // Rokade: rook ook verplaatsen (BoardModel.movePiece doet dat niet)
        if (piece.getType() == PieceType.KING) {
            int dx = to.getColumn() - from.getColumn();
            if (Math.abs(dx) == 2) {
                if (dx > 0) { // korte rokade
                    Position rookFrom = new Position(from.getRow(), 7);
                    Position rookTo   = new Position(from.getRow(), 5);
                    board.movePiece(rookFrom, rookTo);
                } else {      // lange rokade
                    Position rookFrom = new Position(from.getRow(), 0);
                    Position rookTo   = new Position(from.getRow(), 3);
                    board.movePiece(rookFrom, rookTo);
                }
            }
        }

        // Promotie: als SAN "=Q" etc bevat
        if (sanRaw.contains("=")) {
            char promo = sanRaw.charAt(sanRaw.indexOf('=') + 1);
            PieceType newType = switch (promo) {
                case 'Q' -> PieceType.QUEEN;
                case 'R' -> PieceType.ROOK;
                case 'B' -> PieceType.BISHOP;
                case 'N' -> PieceType.KNIGHT;
                default -> PieceType.QUEEN;
            };
            board.getSquare(to).setPiece(new PieceModel(newType, color));
        }
    }




}
