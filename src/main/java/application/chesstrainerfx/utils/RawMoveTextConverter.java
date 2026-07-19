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
 * Werkt per woord, zodat proza en zetten op dezelfde regel mogen staan.
 * Herstelt weggevallen stukletters (figurines, bv. "xf6" → "Nxf6") aan de hand
 * van de stelling, splitst aan elkaar geplakte tokens ("e4e5", "a73.xd8"),
 * maakt van proza inline {commentaar} bij de voorafgaande zet en herkent
 * variaties aan terugspringende zetnummers. De zetten worden intern als boom
 * bijgehouden zodat een variatie op een eerdere zet — ook als de hoofdlijn al
 * verder is — op de juiste plek tussen haakjes belandt.
 */
public final class RawMoveTextConverter {

    /** Resultaat van een conversie: de opgeschoonde movetext plus waarschuwingen. */
    public record Conversion(String moveText, List<String> warnings) {
    }

    // Volgorde is betekenisvol: resultaat vóór zetnummer ("1-0" vs "1."),
    // rokade vóór SAN, evaluaties ("+-", ook met en/em-dash) vóór het losse schaak-plusje.
    private static final Pattern TOKEN = Pattern.compile(
            "(?<result>1-0|0-1|1/2-1/2|½-½)"
                    + "|(?<number>\\d+)(?<dots>\\.{1,3})"
                    + "|(?<castle>O-O-O|O-O|0-0-0|0-0)"
                    + "|(?<san>[KQRBN][a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?"
                    + "|[a-h]x[a-h][1-8](?:=[QRBN])?"
                    + "|x[a-h][1-8](?:=[QRBN])?"
                    + "|[a-h][1-8](?:=[QRBN])?)"
                    + "|(?<eval>\\+[-–—]|[-–—]\\+|\\+/-|-/\\+|[±∓⩱⩲=∞])"
                    + "|(?<suffix>[+#])"
                    + "|(?<glyph>[!?]+)");

    private static final char[] PIECE_LETTERS = {'N', 'B', 'R', 'Q', 'K'};

    private final List<String> warnings = new ArrayList<>();
    private final Deque<LineCtx> stack = new ArrayDeque<>();
    private final Node root = new Node(0, null, null, null, null, null);
    private final List<String> proseRun = new ArrayList<>();
    private String pendingResult;
    private Integer pendingNumber;
    private PieceColor pendingColor;

    public static Conversion convert(String rawText, String fen) {
        return new RawMoveTextConverter(fen).run(rawText);
    }

    private RawMoveTextConverter(String fen) {
        LineCtx main = new LineCtx();
        main.board = new BoardModel();
        main.board.initializeFromFEN(fen);
        String[] parts = fen.trim().split("\\s+");
        main.toMove = parts.length >= 2 && parts[1].equals("b") ? PieceColor.BLACK : PieceColor.WHITE;
        main.moveNumber = 1;
        main.current = root;
        main.anchor = root;
        stack.push(main);
    }

    private Conversion run(String rawText) {
        for (String word : rawText.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String movePart = asMoveWord(word);
            if (movePart == null) {
                proseRun.add(stripBrackets(word));
                continue;
            }
            flushProse();
            processTokens(movePart);
        }
        flushProse();

        String moveText = serialize();
        if (moveText.isBlank()) {
            warnings.add("No moves recognized in the text.");
            return new Conversion(rawText, warnings);
        }
        return new Conversion(moveText, warnings);
    }

    /**
     * Levert de verwerkbare vorm van een woord dat zetmateriaal is, of null voor
     * proza. Een zetwoord bestaat (na eventuele openingshaakjes) uit aaneengesloten
     * tokens vanaf het begin, met hooguit loze leestekens als staart. Een woord dat
     * alleen uit een groot getal bestaat (bv. een jaartal "2002.") blijft proza.
     */
    private String asMoveWord(String word) {
        String s = word.replaceAll("^[(\\[{]+", "");
        Matcher m = TOKEN.matcher(s);
        int pos = 0;
        boolean onlyLargeNumbers = true;
        while (m.find() && m.start() == pos) {
            if (m.group("number") == null || Integer.parseInt(m.group("number")) <= 99) {
                onlyLargeNumbers = false;
            }
            pos = m.end();
        }
        if (pos == 0 || onlyLargeNumbers) {
            return null;
        }
        return s.substring(pos).matches("[.,;:)\\]}]*") ? s : null;
    }

    private static String stripBrackets(String word) {
        return word.replaceAll("^[(\\[{]+", "").replaceAll("[)\\]}]+$", "");
    }

    private void processTokens(String word) {
        Matcher m = TOKEN.matcher(word);
        while (m.find()) {
            if (m.group("result") != null) {
                pendingResult = m.group("result");
                continue;
            }
            if (m.group("eval") != null) {
                continue; // evaluaties horen niet in movetext
            }
            if (m.group("suffix") != null || m.group("glyph") != null) {
                Node tip = stack.peek().current;
                if (tip.san != null) {
                    tip.san += m.group();
                }
                continue;
            }
            if (m.group("number") != null) {
                PieceColor color = m.group("dots").length() >= 2 ? PieceColor.BLACK : PieceColor.WHITE;
                handleNumberToken(Integer.parseInt(m.group("number")), color);
                continue;
            }
            String token = m.group("castle") != null
                    ? m.group("castle").replace('0', 'O')
                    : m.group("san");
            playToken(token);
        }
    }

    /**
     * Registreert een zetnummer. De structurele beslissing (voortzetten, variatie
     * openen of terugkeren naar een omliggende lijn) valt pas bij de eerstvolgende
     * zet, zodat de zet zelf kan meebeslissen in welke lijn hij herleidbaar is.
     */
    private void handleNumberToken(int number, PieceColor color) {
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
        pendingNumber = number;
        pendingColor = color;
    }

    /**
     * Kiest bij een zetnummer + zet de lijn waarin die zet thuishoort. Lijnen die
     * dit nummer als voortzetting verwachten gaan voor; daarbinnen beslist
     * herleidbaarheid van de zet. Verwachten meerdere lijnen dezelfde voortzetting
     * en is de zet overal herleidbaar, dan geldt de boekconventie dat proza een
     * variatie afsluit: draagt de variatietip al commentaar, dan wint de
     * omliggende lijn. Anders wordt een variatie geopend op de open lijn die deze
     * zet eerder speelde.
     */
    private void selectContext(int number, PieceColor color, String token) {
        List<LineCtx> expecting = new ArrayList<>();
        for (LineCtx ctx : stack) {
            if (ctx.expects(number, color)) {
                expecting.add(ctx);
            }
        }
        if (!expecting.isEmpty()) {
            List<LineCtx> resolving = new ArrayList<>();
            for (LineCtx ctx : expecting) {
                if (trialResolves(ctx.board, ctx.toMove, token)) {
                    resolving.add(ctx);
                }
            }
            LineCtx chosen = resolving.isEmpty() ? expecting.get(0) : resolving.get(0);
            if (resolving.size() > 1 && chosen == stack.peek() && stack.size() > 1
                    && chosen.current.san != null && !chosen.current.comment.isEmpty()) {
                chosen = resolving.get(1);
            }
            popTo(chosen);
            return;
        }

        LineCtx branchOwner = null;
        Node replaced = null;
        for (LineCtx ctx : stack) {
            Node candidate = ctx.findPlayed(number, color);
            if (candidate == null) {
                continue;
            }
            if (branchOwner == null) {
                branchOwner = ctx;
                replaced = candidate;
            }
            BoardModel trial = new BoardModel();
            trial.initializeFromFEN(candidate.fenBefore);
            trial.setLastDoubleStepPawnPosition(candidate.epBefore);
            if (trialResolves(trial, candidate.color, token)) {
                branchOwner = ctx;
                replaced = candidate;
                break;
            }
        }
        if (branchOwner != null) {
            popTo(branchOwner);
            branchFrom(replaced);
            return;
        }
        warnings.add("Unexpected move number " + number + (color == PieceColor.BLACK ? "..." : ".")
                + " — check the numbering around that move.");
    }

    private boolean trialResolves(BoardModel board, PieceColor toMove, String token) {
        String san = reconstruct(board, toMove, 0, token, new ArrayList<>());
        return SanResolver.resolve(board, san, toMove) != null;
    }

    private void popTo(LineCtx ctx) {
        while (stack.peek() != ctx) {
            stack.pop();
        }
    }

    /** Opent een variatie die de gegeven zet vervangt: zelfde ouder, stelling van vóór die zet. */
    private void branchFrom(Node replaced) {
        LineCtx ctx = new LineCtx();
        ctx.board = new BoardModel();
        ctx.board.initializeFromFEN(replaced.fenBefore);
        ctx.board.setLastDoubleStepPawnPosition(replaced.epBefore);
        ctx.toMove = replaced.color;
        ctx.moveNumber = replaced.number;
        ctx.current = replaced.parent;
        ctx.anchor = replaced.parent;
        stack.push(ctx);
    }

    private void playToken(String token) {
        if (pendingNumber != null) {
            selectContext(pendingNumber, pendingColor, token);
            pendingNumber = null;
            pendingColor = null;
        }
        LineCtx ctx = stack.peek();
        String san = reconstruct(ctx.board, ctx.toMove, ctx.moveNumber, token, warnings);
        Move move = SanResolver.resolve(ctx.board, san, ctx.toMove);
        String fenBefore = ctx.board.exportToFEN(ctx.toMove == PieceColor.WHITE);
        Position epBefore = ctx.board.getLastDoubleStepPawnPosition();
        if (move == null) {
            warnings.add("Could not resolve \"" + token + "\" as a legal move for " + name(ctx.toMove)
                    + " at move " + ctx.moveNumber + ".");
        } else {
            ExerciseMoveExecutor.apply(ctx.board, move, san, ctx.toMove);
        }
        Node node = new Node(ctx.moveNumber, ctx.toMove, san, ctx.current, fenBefore, epBefore);
        ctx.current.children.add(node);
        ctx.current = node;
        ctx.moved = true;
        if (ctx.toMove == PieceColor.BLACK) {
            ctx.moveNumber++;
        }
        ctx.toMove = (ctx.toMove == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        pendingResult = null;
    }

    /** Hangt verzameld proza (plus een direct voorafgaand resultaat) als commentaar aan de laatste zet. */
    private void flushProse() {
        if (proseRun.isEmpty()) {
            return;
        }
        String text = String.join(" ", proseRun).trim().replaceAll("[;,\\s]+$", "");
        proseRun.clear();
        if (pendingResult != null) {
            text = (pendingResult + " " + text).trim();
            pendingResult = null;
        }
        String comment = PgnUtils.sanitizeComment(text);
        if (comment.isEmpty()) {
            return;
        }
        Node target = stack.peek().current;
        target.comment = target.comment.isEmpty() ? comment : target.comment + " " + comment;
    }

    /**
     * Vult een weggevallen stukletter aan: "xf6" wordt bv. "Nxf6" als precies één
     * stuksoort die slag kan spelen. Een kaal veld ("e4") wordt als pionzet
     * gelezen als die legaal is; een legale stukzet ernaast levert een waarschuwing.
     */
    private String reconstruct(BoardModel board, PieceColor toMove, int moveNumber,
                               String token, List<String> sink) {
        if (Character.isUpperCase(token.charAt(0)) || token.matches("[a-h]x.*")) {
            return token; // stukletter of pion-slag met lijnletter is al compleet
        }
        if (token.charAt(0) == 'x') {
            List<String> options = pieceOptions(board, toMove, token, true);
            if (options.size() == 1) {
                return options.get(0);
            }
            sink.add(ambiguityWarning(toMove, moveNumber, token, options));
            return token;
        }
        boolean pawnLegal = SanResolver.resolve(board, token, toMove) != null;
        List<String> options = pieceOptions(board, toMove, token, false);
        if (pawnLegal) {
            if (!options.isEmpty()) {
                sink.add("\"" + token + "\" at move " + moveNumber + " was read as a pawn move, but "
                        + String.join(", ", options) + " is also legal — check which one is meant.");
            }
            return token;
        }
        if (options.size() == 1) {
            return options.get(0);
        }
        sink.add(ambiguityWarning(toMove, moveNumber, token, options));
        return token;
    }

    private String ambiguityWarning(PieceColor toMove, int moveNumber, String token, List<String> options) {
        return options.isEmpty()
                ? "No piece can play \"" + token + "\" for " + name(toMove) + " at move "
                        + moveNumber + " — check the position."
                : "\"" + token + "\" at move " + moveNumber + " is ambiguous ("
                        + String.join(", ", options) + ") — pick the right one manually.";
    }

    private List<String> pieceOptions(BoardModel board, PieceColor toMove, String token, boolean capture) {
        List<String> options = new ArrayList<>();
        if (capture && !enemyOnTarget(board, toMove, token)) {
            return options;
        }
        for (char piece : PIECE_LETTERS) {
            if (SanResolver.resolve(board, piece + token, toMove) != null) {
                options.add(piece + token);
            }
        }
        return options;
    }

    private boolean enemyOnTarget(BoardModel board, PieceColor toMove, String token) {
        String core = token.contains("=") ? token.substring(0, token.indexOf('=')) : token;
        int[] rc = CoordinateSystem.coordinateToIndex(core.substring(core.length() - 2));
        SquareModel sq = board.getSquare(new Position(rc[0], rc[1]));
        PieceModel piece = sq == null ? null : sq.getPiece();
        return piece != null && piece.getColor() != toMove;
    }

    private static String name(PieceColor color) {
        return color == PieceColor.WHITE ? "white" : "black";
    }

    // ---- serialisatie van de zettenboom naar PGN-movetext ----

    private String serialize() {
        StringBuilder sb = new StringBuilder();
        if (!root.comment.isEmpty()) {
            append(sb, "{" + root.comment + "}");
        }
        serializeChildren(root, sb, true);
        return sb.toString().trim();
    }

    /** Eerste kind is de hoofdvoortzetting; overige kinderen worden variaties tussen haakjes. */
    private void serializeChildren(Node parent, StringBuilder sb, boolean forceNumber) {
        if (parent.children.isEmpty()) {
            return;
        }
        Node main = parent.children.get(0);
        appendMove(sb, main, forceNumber);
        boolean interrupted = false;
        if (!main.comment.isEmpty()) {
            append(sb, "{" + main.comment + "}");
            interrupted = true;
        }
        for (int i = 1; i < parent.children.size(); i++) {
            append(sb, "(");
            Node variation = parent.children.get(i);
            appendMove(sb, variation, true);
            if (!variation.comment.isEmpty()) {
                append(sb, "{" + variation.comment + "}");
            }
            serializeChildren(variation, sb, !variation.comment.isEmpty());
            sb.append(')');
            interrupted = true;
        }
        serializeChildren(main, sb, interrupted);
    }

    private void appendMove(StringBuilder sb, Node node, boolean forceNumber) {
        String prefix = node.color == PieceColor.WHITE
                ? node.number + ". "
                : (forceNumber ? node.number + "... " : "");
        append(sb, prefix + node.san);
    }

    private void append(StringBuilder sb, String token) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '(') {
            sb.append(' ');
        }
        sb.append(token);
    }

    /** Eén zet in de boom, met de stelling van vóór de zet om variaties te kunnen aftakken. */
    private static final class Node {
        final int number;
        final PieceColor color;
        String san;
        String comment = "";
        final Node parent;
        final String fenBefore;
        final Position epBefore;
        final List<Node> children = new ArrayList<>();

        Node(int number, PieceColor color, String san, Node parent, String fenBefore, Position epBefore) {
            this.number = number;
            this.color = color;
            this.san = san;
            this.parent = parent;
            this.fenBefore = fenBefore;
            this.epBefore = epBefore;
        }
    }

    /** Eén open (hoofd- of variatie)lijn: bordstand, wie aan zet is en de aanhechting in de boom. */
    private static final class LineCtx {
        BoardModel board;
        PieceColor toMove;
        int moveNumber;
        boolean moved;
        Node current;
        Node anchor;

        boolean expects(int number, PieceColor color) {
            return number == moveNumber && color == toMove;
        }

        /** Zoekt de gespeelde zet met dit nummer binnen het eigen segment van deze lijn. */
        Node findPlayed(int number, PieceColor color) {
            for (Node n = current; n != null && n != anchor; n = n.parent) {
                if (n.number == number && n.color == color) {
                    return n;
                }
            }
            return null;
        }
    }
}
