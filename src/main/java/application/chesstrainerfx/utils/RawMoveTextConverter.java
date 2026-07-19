package application.chesstrainerfx.utils;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zet ruwe, uit boeken of apps geplakte zettentekst om naar nette PGN-movetext.
 * Herstelt weggevallen stukletters (figurines, bv. "xf6" → "Nxf6") aan de hand
 * van de stelling, splitst aan elkaar geplakte tokens ("e4e5", "a73.xd8"),
 * maakt van proza-regels inline {commentaar} en herkent variaties aan
 * terugspringende zetnummers (boek-layout: de variatie volgt direct op de zet
 * waarvan ze afwijkt).
 */
public final class RawMoveTextConverter {

    /** Resultaat van een conversie: de opgeschoonde movetext plus waarschuwingen. */
    public record Conversion(String moveText, List<String> warnings) {
    }

    private static final Pattern PROSE = Pattern.compile("[A-Za-z]{4,}");

    // Volgorde is betekenisvol: resultaat vóór zetnummer ("1-0" vs "1."),
    // rokade vóór SAN, evaluaties ("+-") vóór het losse schaak-plusje.
    private static final Pattern TOKEN = Pattern.compile(
            "(?<result>1-0|0-1|1/2-1/2|½-½)"
                    + "|(?<number>\\d+)(?<dots>\\.{1,3})"
                    + "|(?<castle>O-O-O|O-O|0-0-0|0-0)"
                    + "|(?<san>[KQRBN][a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?"
                    + "|[a-h]x[a-h][1-8](?:=[QRBN])?"
                    + "|x[a-h][1-8](?:=[QRBN])?"
                    + "|[a-h][1-8](?:=[QRBN])?)"
                    + "|(?<eval>\\+-|-\\+|\\+/-|-/\\+|[±∓⩱⩲=∞])"
                    + "|(?<suffix>[+#])"
                    + "|(?<glyph>[!?]+)");

    private static final char[] PIECE_LETTERS = {'N', 'B', 'R', 'Q', 'K'};

    private RawMoveTextConverter() {
    }

    public static Conversion convert(String rawText, String fen) {
        List<String> warnings = new ArrayList<>();
        Output out = new Output();
        Deque<LineCtx> stack = new ArrayDeque<>();
        stack.push(LineCtx.fromFen(fen));

        for (String line : rawText.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (PROSE.matcher(trimmed).find()) {
                String comment = PgnUtils.sanitizeComment(trimmed);
                if (!comment.isEmpty()) {
                    out.appendComment(comment);
                }
                continue;
            }
            Matcher m = TOKEN.matcher(trimmed);
            while (m.find()) {
                if (m.group("result") != null || m.group("eval") != null) {
                    continue; // resultaat komt uit de Result-tag; evaluaties horen niet in movetext
                }
                if (m.group("suffix") != null) {
                    out.appendToLastMove(m.group("suffix"));
                    continue;
                }
                if (m.group("glyph") != null) {
                    out.appendToLastMove(m.group("glyph"));
                    continue;
                }
                if (m.group("number") != null) {
                    PieceColor color = m.group("dots").length() >= 2 ? PieceColor.BLACK : PieceColor.WHITE;
                    handleNumber(Integer.parseInt(m.group("number")), color, stack, out, warnings);
                    continue;
                }
                String token = m.group("castle") != null
                        ? m.group("castle").replace('0', 'O')
                        : m.group("san");
                playToken(token, stack.peek(), out, warnings);
            }
        }

        while (stack.size() > 1) {
            out.closeVariation();
            stack.pop();
        }
        String moveText = out.toString();
        if (moveText.isBlank()) {
            warnings.add("No moves recognized in the text.");
            return new Conversion(rawText, warnings);
        }
        return new Conversion(moveText, warnings);
    }

    /**
     * Verwerkt een zetnummer-token: bevestigt de verwachte voortzetting, keert
     * terug naar een omliggende lijn (variatie sluiten) of opent een variatie als
     * het nummer de laatst gespeelde zet van een open lijn herhaalt.
     */
    private static void handleNumber(int number, PieceColor color, Deque<LineCtx> stack,
                                     Output out, List<String> warnings) {
        LineCtx current = stack.peek();
        if (!current.moved && stack.size() == 1) {
            // Het eerste zetnummer bepaalt de nummering van de oefening.
            current.moveNumber = number;
            if (color != current.toMove) {
                warnings.add("The text starts with " + name(color) + " to move, but the position says "
                        + name(current.toMove) + " to move.");
            }
            return;
        }
        if (current.expects(number, color)) {
            return;
        }

        int closes = 0;
        for (LineCtx ctx : stack) {
            if (ctx.expects(number, color)) {
                popVariations(stack, out, closes);
                return;
            }
            closes++;
        }
        closes = 0;
        for (LineCtx ctx : stack) {
            if (ctx.lastMoveWas(number, color)) {
                popVariations(stack, out, closes);
                out.openVariation();
                stack.push(ctx.branchBeforeLastMove());
                return;
            }
            closes++;
        }
        warnings.add("Unexpected move number " + number + (color == PieceColor.BLACK ? "..." : ".")
                + " — check the numbering around that move.");
    }

    private static void popVariations(Deque<LineCtx> stack, Output out, int count) {
        for (int i = 0; i < count; i++) {
            out.closeVariation();
            stack.pop();
        }
    }

    private static void playToken(String token, LineCtx ctx, Output out, List<String> warnings) {
        String san = reconstruct(ctx, token, warnings);
        Move move = SanResolver.resolve(ctx.board, san, ctx.toMove);
        if (move == null) {
            warnings.add("Could not resolve \"" + token + "\" as a legal move for " + name(ctx.toMove)
                    + " at move " + ctx.moveNumber + ".");
        } else {
            ctx.snapshotBeforeMove();
            ExerciseMoveExecutor.apply(ctx.board, move, san, ctx.toMove);
        }
        out.appendMove(ctx.moveNumber, ctx.toMove, san);
        ctx.recordMove();
    }

    /**
     * Vult een weggevallen stukletter aan: "xf6" wordt bv. "Nxf6" als precies één
     * stuksoort die slag kan spelen. Een kaal veld ("e4") wordt als pionzet
     * gelezen als die legaal is; een legale stukzet ernaast levert een waarschuwing.
     */
    private static String reconstruct(LineCtx ctx, String token, List<String> warnings) {
        if (Character.isUpperCase(token.charAt(0)) || token.matches("[a-h]x.*")) {
            return token; // stukletter of pion-slag met lijnletter is al compleet
        }
        if (token.charAt(0) == 'x') {
            List<String> options = pieceOptions(ctx, token, true);
            if (options.size() == 1) {
                return options.get(0);
            }
            warnings.add(ambiguityWarning(ctx, token, options));
            return token;
        }
        boolean pawnLegal = SanResolver.resolve(ctx.board, token, ctx.toMove) != null;
        List<String> options = pieceOptions(ctx, token, false);
        if (pawnLegal) {
            if (!options.isEmpty()) {
                warnings.add("\"" + token + "\" at move " + ctx.moveNumber + " was read as a pawn move, but "
                        + String.join(", ", options) + " is also legal — check which one is meant.");
            }
            return token;
        }
        if (options.size() == 1) {
            return options.get(0);
        }
        warnings.add(ambiguityWarning(ctx, token, options));
        return token;
    }

    private static String ambiguityWarning(LineCtx ctx, String token, List<String> options) {
        return options.isEmpty()
                ? "No piece can play \"" + token + "\" for " + name(ctx.toMove) + " at move "
                        + ctx.moveNumber + " — check the position."
                : "\"" + token + "\" at move " + ctx.moveNumber + " is ambiguous ("
                        + String.join(", ", options) + ") — pick the right one manually.";
    }

    private static List<String> pieceOptions(LineCtx ctx, String token, boolean capture) {
        List<String> options = new ArrayList<>();
        if (capture && !enemyOnTarget(ctx, token)) {
            return options;
        }
        for (char piece : PIECE_LETTERS) {
            if (SanResolver.resolve(ctx.board, piece + token, ctx.toMove) != null) {
                options.add(piece + token);
            }
        }
        return options;
    }

    private static boolean enemyOnTarget(LineCtx ctx, String token) {
        String core = token.contains("=") ? token.substring(0, token.indexOf('=')) : token;
        int[] rc = CoordinateSystem.coordinateToIndex(core.substring(core.length() - 2));
        SquareModel sq = ctx.board.getSquare(new Position(rc[0], rc[1]));
        PieceModel piece = sq == null ? null : sq.getPiece();
        return piece != null && piece.getColor() != ctx.toMove;
    }

    private static String name(PieceColor color) {
        return color == PieceColor.WHITE ? "white" : "black";
    }

    /** Eén (hoofd- of variatie)lijn: bordstand, wie aan zet is en de nummering. */
    private static final class LineCtx {
        BoardModel board;
        PieceColor toMove;
        int moveNumber;
        boolean moved;
        int lastNumber;
        PieceColor lastColor;
        String fenBeforeLast;
        Position epBeforeLast;

        static LineCtx fromFen(String fen) {
            LineCtx ctx = new LineCtx();
            ctx.board = new BoardModel();
            ctx.board.initializeFromFEN(fen);
            String[] parts = fen.trim().split("\\s+");
            ctx.toMove = parts.length >= 2 && parts[1].equals("b") ? PieceColor.BLACK : PieceColor.WHITE;
            ctx.moveNumber = 1;
            return ctx;
        }

        boolean expects(int number, PieceColor color) {
            return number == moveNumber && color == toMove;
        }

        boolean lastMoveWas(int number, PieceColor color) {
            return moved && fenBeforeLast != null && number == lastNumber && color == lastColor;
        }

        LineCtx branchBeforeLastMove() {
            LineCtx branch = new LineCtx();
            branch.board = new BoardModel();
            branch.board.initializeFromFEN(fenBeforeLast);
            branch.board.setLastDoubleStepPawnPosition(epBeforeLast);
            branch.toMove = lastColor;
            branch.moveNumber = lastNumber;
            return branch;
        }

        void snapshotBeforeMove() {
            fenBeforeLast = board.exportToFEN(toMove == PieceColor.WHITE);
            epBeforeLast = board.getLastDoubleStepPawnPosition();
        }

        void recordMove() {
            lastNumber = moveNumber;
            lastColor = toMove;
            moved = true;
            if (toMove == PieceColor.BLACK) {
                moveNumber++;
            }
            toMove = (toMove == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        }
    }

    /** Bouwt de movetext op met nette nummering, spaties en haakjes. */
    private static final class Output {
        private final StringBuilder sb = new StringBuilder();
        private boolean lastWasMove;
        private boolean needsBlackNumber = true;

        void appendMove(int number, PieceColor color, String san) {
            String prefix = color == PieceColor.WHITE
                    ? number + ". "
                    : (needsBlackNumber ? number + "... " : "");
            appendToken(prefix + san);
            lastWasMove = true;
            needsBlackNumber = false;
        }

        void appendComment(String comment) {
            appendToken("{" + comment + "}");
            lastWasMove = false;
            needsBlackNumber = true;
        }

        void openVariation() {
            appendToken("(");
            lastWasMove = false;
            needsBlackNumber = true;
        }

        void closeVariation() {
            sb.append(')');
            lastWasMove = false;
            needsBlackNumber = true;
        }

        void appendToLastMove(String suffix) {
            if (lastWasMove) {
                sb.append(suffix);
            }
        }

        private void appendToken(String token) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '(') {
                sb.append(' ');
            }
            sb.append(token);
        }

        @Override
        public String toString() {
            return sb.toString().trim();
        }
    }
}
