package application.chesstrainerfx.imagescanner;

import application.chesstrainerfx.utils.PieceColor;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.utils.PieceType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Scanner voor zwart-wit figurine-diagrammen (zoals geëxporteerd uit schaakboeken,
 * o.a. 500ChessExercises.pdf): witte stukken = contour, zwarte stukken = massief,
 * donkere velden = diagonale arcering, coördinaat-labels los gedrukt buiten een
 * zwart kader dat het 8×8-bord omsluit.
 *
 * Algoritme (arcering-onafhankelijk):
 *  1. Binariseer (luminantie &lt; 128 = ink).
 *  2. Detecteer de verticale kaderlijnen (vrijwel volledig zwarte kolommen) en leid
 *     daaruit het binnengebied van het bord af; zonder kader valt de detectie terug
 *     op de volledige afbeelding.
 *  3. Per cel: white-flood-fill vanaf de celrand → het niet-bereikbare deel (na een lichte
 *     erosie die dunne arcering-bruggen knipt) is het stuk-silhouet; gaten vullen.
 *     - oppervlak ~0 → leeg veld.
 *     - kleur: ink-fractie in de geërodeerde kern (massief = zwart, contour = wit).
 *     - type: silhouet aspect-behoudend genormaliseerd (56×56) en vergeleken met de
 *       stuk-templates via IoU, gewogen met de silhouet-hoogte t.o.v. de cel
 *       (onderscheidt kleine stukken zoals pionnen van grote zoals lopers).
 *  4. Oriëntatie (wit of zwart onder) wordt extern gezet en bepaalt de veld-mapping naar FEN.
 *
 * Thread-safe: scan() gebruikt geen gedeelde mutable state buiten de (vooraf geladen) templates.
 */
public class DiagramBoardScanner implements BoardScanner {

    private static final double EMPTY_AREA = 0.05;     // silhouet-oppervlak onder deze fractie = leeg
    private static final double CELL_MARGIN = 0.06;    // celrand afsnijden om buurstukken te mijden
    private static final double FRAME_LINE = 0.85;     // kolom met deze ink-fractie = kaderlijn

    private record Template(PieceType type, boolean[][] mask) {}

    private final List<Template> templates = new ArrayList<>();
    private boolean blackPerspective = false;

    public DiagramBoardScanner() {
        // 6 stuktypes × 2 kleuren (massief zwart / wit contour leveren verschillende silhouetten).
        load("ROOK_W",   PieceType.ROOK);   load("ROOK_B",   PieceType.ROOK);
        load("KNIGHT_W", PieceType.KNIGHT); load("KNIGHT_B", PieceType.KNIGHT);
        load("BISHOP_W", PieceType.BISHOP); load("BISHOP_B", PieceType.BISHOP);
        load("QUEEN_W",  PieceType.QUEEN);  load("QUEEN_B",  PieceType.QUEEN);
        load("KING_W",   PieceType.KING);   load("KING_B",   PieceType.KING);
        load("PAWN_W",   PieceType.PAWN);   load("PAWN_B",   PieceType.PAWN);
    }

    /** Stelt in of het bord vanuit zwart is weergegeven (rank 1 boven, h-lijn links). */
    public void setBlackPerspective(boolean blackPerspective) {
        this.blackPerspective = blackPerspective;
    }

    private void load(String name, PieceType type) {
        try (InputStream is = getClass().getResourceAsStream("/imagescanner/templates/book/" + name + ".png")) {
            if (is == null) return;
            Image img = new Image(is);
            int w = (int) img.getWidth(), h = (int) img.getHeight();
            PixelReader pr = img.getPixelReader();
            boolean[][] raw = new boolean[h][w];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    raw[y][x] = SilhouetteUtils.luminance(pr.getArgb(x, y)) < 128;   // piece = zwart
            boolean[][] mask = SilhouetteUtils.normalize(raw);
            templates.add(new Template(type, mask));
            if (type == PieceType.KNIGHT) {
                // Bord vanuit zwart spiegelt het asymmetrische paard horizontaal.
                templates.add(new Template(type, SilhouetteUtils.mirror(mask)));
            }
        } catch (Exception ignored) {
            // Ontbrekende/onleesbare template: type niet beschikbaar, scanner werkt door.
        }
    }

    // -------------------------------------------------------------------------
    // Scan
    // -------------------------------------------------------------------------

    @Override
    public ScanResult scan(Image boardImage) {
        boolean[][] ink = binarize(boardImage);
        int h = ink.length, w = ink[0].length;
        int[] board = detectBoard(ink);
        int bx = board[0], by = board[1];
        double cw = board[2] / 8.0, ch = board[3] / 8.0;

        PieceModel[][] pieces = new PieceModel[8][8];
        double[][] confidence = new double[8][8];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int cy0 = (int) Math.round(by + r * ch);
                int cx0 = (int) Math.round(bx + c * cw);
                int cellH = (int) Math.round(ch), cellW = (int) Math.round(cw);
                int m = (int) (CELL_MARGIN * ch);
                boolean[][] cell = sub(ink, cx0 + m, cy0 + m, cellW - 2 * m, cellH - 2 * m, w, h);

                int tr = blackPerspective ? 7 - r : r;
                int tc = blackPerspective ? 7 - c : c;
                classifyInto(cell, pieces, confidence, tr, tc);
            }
        }
        return new ScanResult(pieces, confidence);
    }

    private void classifyInto(boolean[][] cell, PieceModel[][] pieces, double[][] conf, int tr, int tc) {
        boolean[][] filled = pieceMask(cell);
        if (SilhouetteUtils.fraction(filled) < EMPTY_AREA) {
            pieces[tr][tc] = null;
            conf[tr][tc] = 1.0;
            return;
        }
        boolean[][] interior = SilhouetteUtils.erode(filled, 4);
        if (SilhouetteUtils.count(interior) == 0) interior = filled;
        PieceColor color = inkFraction(cell, interior) > 0.5 ? PieceColor.BLACK : PieceColor.WHITE;

        boolean[][] nm = SilhouetteUtils.normalize(filled);
        double best = 0;
        PieceType bestType = PieceType.PAWN;
        for (Template t : templates) {
            double v = SilhouetteUtils.iou(nm, t.mask());
            if (v > best) { best = v; bestType = t.type(); }
        }
        pieces[tr][tc] = new PieceModel(bestType, color);
        conf[tr][tc] = best;     // lage IoU → UI markeert het veld als onzeker
    }

    // -------------------------------------------------------------------------
    // Bord-detectie
    // -------------------------------------------------------------------------

    /**
     * Zoekt het binnengebied van het bordkader: {x, y, breedte, hoogte}.
     * De verticale kaderlijnen zijn de enige vrijwel volledig zwarte kolommen;
     * de coördinaat-labels (losse glyphs) en arcering blijven daar ruim onder.
     * De verticale omvang volgt uit de ink-uitloop van de linkerkaderlijn zelf,
     * omdat de boven-/onderlijn in gescande diagrammen niet altijd volledig is.
     */
    private int[] detectBoard(boolean[][] ink) {
        int h = ink.length, w = ink[0].length;
        int left = -1, right = -1;
        for (int x = 0; x < w; x++) if (colFraction(ink, x) > FRAME_LINE) { left = x; break; }
        for (int x = w - 1; x > left; x--) if (colFraction(ink, x) > FRAME_LINE) { right = x; break; }
        if (left < 0 || right - left < w / 2) return new int[]{0, 0, w, h};

        int innerLeft = left, innerRight = right;
        while (innerLeft < w - 1 && colFraction(ink, innerLeft) > 0.5) innerLeft++;
        while (innerRight > 0 && colFraction(ink, innerRight) > 0.5) innerRight--;

        int top = 0, bottom = h - 1;
        while (top < h - 1 && !ink[top][left]) top++;
        while (bottom > 0 && !ink[bottom][left]) bottom--;
        int thickness = innerLeft - left;
        int innerTop = top + thickness, innerBottom = bottom - thickness;

        if (innerRight <= innerLeft || innerBottom <= innerTop) return new int[]{0, 0, w, h};
        return new int[]{innerLeft, innerTop, innerRight - innerLeft + 1, innerBottom - innerTop + 1};
    }

    private double colFraction(boolean[][] ink, int x) {
        int h = ink.length, n = 0;
        for (int y = 0; y < h; y++) if (ink[y][x]) n++;
        return (double) n / h;
    }

    // -------------------------------------------------------------------------
    // Silhouet-extractie
    // -------------------------------------------------------------------------

    /** Arcering-robuust silhouet: white-flood vanaf rand → erodeer (knip hatch-bruggen) →
     *  grootste component → dilateer terug → vul gaten. */
    private boolean[][] pieceMask(boolean[][] cell) {
        boolean[][] reached = floodWhite(cell);
        int h = cell.length, w = cell[0].length;
        boolean[][] fg = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                fg[y][x] = !reached[y][x];
        boolean[][] eroded = SilhouetteUtils.erode(fg, 1);
        boolean[][] cc = SilhouetteUtils.largestComponent(eroded);
        boolean[][] back = SilhouetteUtils.dilate(cc, 1);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                back[y][x] = back[y][x] && fg[y][x];
        boolean[][] filled = SilhouetteUtils.fillHoles(back);
        // Contourstuk met een gat in de omtrek (bv. ontbrekende basislijn in de druk):
        // de flood lekt het stuk binnen en alleen de contourstreek blijft over, zodat
        // fillHoles niets kan vullen. Herkenbaar doordat een erosie vrijwel niets
        // overlaat; benader dan het massieve silhouet via span-vulling.
        int area = SilhouetteUtils.count(filled);
        if (area > 0 && SilhouetteUtils.count(SilhouetteUtils.erode(filled, 2)) * 4 < area) {
            filled = SilhouetteUtils.spanFill(filled);
        }
        return filled;
    }

    /** reached = witte achtergrond bereikbaar vanaf de celrand (4-connectief over ink==false). */
    private boolean[][] floodWhite(boolean[][] cell) {
        int h = cell.length, w = cell[0].length;
        boolean[][] reached = new boolean[h][w];
        Deque<int[]> dq = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            seed(cell, reached, dq, 0, x);
            seed(cell, reached, dq, h - 1, x);
        }
        for (int y = 0; y < h; y++) {
            seed(cell, reached, dq, y, 0);
            seed(cell, reached, dq, y, w - 1);
        }
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!dq.isEmpty()) {
            int[] p = dq.poll();
            for (int[] d : d4) {
                int ny = p[0] + d[0], nx = p[1] + d[1];
                if (ny >= 0 && ny < h && nx >= 0 && nx < w && !cell[ny][nx] && !reached[ny][nx]) {
                    reached[ny][nx] = true; dq.add(new int[]{ny, nx});
                }
            }
        }
        return reached;
    }

    private void seed(boolean[][] cell, boolean[][] reached, Deque<int[]> dq, int y, int x) {
        if (!cell[y][x] && !reached[y][x]) { reached[y][x] = true; dq.add(new int[]{y, x}); }
    }

    // -------------------------------------------------------------------------
    // Pixel-helpers
    // -------------------------------------------------------------------------

    private boolean[][] binarize(Image img) {
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        PixelReader pr = img.getPixelReader();
        boolean[][] ink = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                ink[y][x] = SilhouetteUtils.luminance(pr.getArgb(x, y)) < 128;
        return ink;
    }

    private boolean[][] sub(boolean[][] ink, int x0, int y0, int w, int h, int imgW, int imgH) {
        boolean[][] out = new boolean[Math.max(1, h)][Math.max(1, w)];
        for (int y = 0; y < out.length; y++)
            for (int x = 0; x < out[0].length; x++) {
                int sy = y0 + y, sx = x0 + x;
                out[y][x] = sy >= 0 && sy < imgH && sx >= 0 && sx < imgW && ink[sy][sx];
            }
        return out;
    }

    private double inkFraction(boolean[][] cell, boolean[][] within) {
        int n = 0, ink = 0;
        for (int y = 0; y < within.length; y++)
            for (int x = 0; x < within[0].length; x++)
                if (within[y][x]) { n++; if (cell[y][x]) ink++; }
        return n == 0 ? 0 : (double) ink / n;
    }
}
