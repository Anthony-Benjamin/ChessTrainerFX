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
 * donkere velden = diagonale arcering, met een massieve zwarte coördinaat-labelbalk
 * links en onder.
 *
 * Algoritme (arcering-onafhankelijk):
 *  1. Binariseer (luminantie &lt; 128 = ink).
 *  2. Detecteer de labelbalken (vrijwel volledig zwarte rand-kolommen/rijen) en isoleer het 8×8-bord.
 *  3. Per cel: white-flood-fill vanaf de celrand → het niet-bereikbare deel (na een lichte
 *     erosie die dunne arcering-bruggen knipt) is het stuk-silhouet; gaten vullen.
 *     - oppervlak ~0 → leeg veld.
 *     - kleur: ink-fractie in de geërodeerde kern (massief = zwart, contour = wit).
 *     - type: genormaliseerd silhouet (56×56) vergeleken met de stuk-templates via IoU.
 *  4. Oriëntatie (wit of zwart onder) wordt extern gezet en bepaalt de veld-mapping naar FEN.
 *
 * Thread-safe: scan() gebruikt geen gedeelde mutable state buiten de (vooraf geladen) templates.
 */
public class DiagramBoardScanner implements BoardScanner {

    private static final int TPL = 56;                 // genormaliseerde template-grootte
    private static final double EMPTY_AREA = 0.05;     // silhouet-oppervlak onder deze fractie = leeg
    private static final double CELL_MARGIN = 0.06;    // celrand afsnijden om buurstukken te mijden

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
        try (InputStream is = getClass().getResourceAsStream("/imagescanner/templates/" + name + ".png")) {
            if (is == null) return;
            Image img = new Image(is);
            int w = (int) img.getWidth(), h = (int) img.getHeight();
            PixelReader pr = img.getPixelReader();
            boolean[][] raw = new boolean[h][w];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    raw[y][x] = luminance(pr.getArgb(x, y)) < 128;   // piece = zwart
            boolean[][] mask = normalize(raw);
            templates.add(new Template(type, mask));
            if (type == PieceType.KNIGHT) {
                // Bord vanuit zwart spiegelt het asymmetrische paard horizontaal.
                templates.add(new Template(type, mirror(mask)));
            }
        } catch (Exception ignored) {
            // Ontbrekende/onleesbare template: type niet beschikbaar, scanner werkt door.
        }
    }

    private boolean[][] mirror(boolean[][] m) {
        int h = m.length, w = m[0].length;
        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out[y][x] = m[y][w - 1 - x];
        return out;
    }

    // -------------------------------------------------------------------------
    // Scan
    // -------------------------------------------------------------------------

    @Override
    public ScanResult scan(Image boardImage) {
        boolean[][] ink = binarize(boardImage);
        int h = ink.length, w = ink[0].length;
        int left = leftBar(ink), bottom = bottomBar(ink);
        double bw = w - left, bh = h - bottom;
        double cw = bw / 8.0, ch = bh / 8.0;

        PieceModel[][] pieces = new PieceModel[8][8];
        double[][] confidence = new double[8][8];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int cy0 = (int) Math.round(r * ch);
                int cx0 = (int) Math.round(left + c * cw);
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
        if (fraction(filled) < EMPTY_AREA) {
            pieces[tr][tc] = null;
            conf[tr][tc] = 1.0;
            return;
        }
        boolean[][] interior = erode(filled, 4);
        if (count(interior) == 0) interior = filled;
        PieceColor color = inkFraction(cell, interior) > 0.5 ? PieceColor.BLACK : PieceColor.WHITE;

        boolean[][] nm = normalize(filled);
        double best = 0, second = 0;
        PieceType bestType = PieceType.PAWN;
        for (Template t : templates) {
            double v = iou(nm, t.mask());
            if (v > best) { second = best; best = v; bestType = t.type(); }
            else if (v > second) second = v;
        }
        pieces[tr][tc] = new PieceModel(bestType, color);
        conf[tr][tc] = best;     // lage IoU → UI markeert het veld als onzeker
    }

    // -------------------------------------------------------------------------
    // Bord-detectie
    // -------------------------------------------------------------------------

    private int leftBar(boolean[][] ink) {
        int h = ink.length, L = 0;
        while (L < 60 && colFraction(ink, L) > 0.9) L++;
        return L;
    }

    private int bottomBar(boolean[][] ink) {
        int h = ink.length, B = 0;
        while (B < 60 && rowFraction(ink, h - 1 - B) > 0.9) B++;
        return B;
    }

    private double colFraction(boolean[][] ink, int x) {
        int h = ink.length, n = 0;
        for (int y = 0; y < h; y++) if (ink[y][x]) n++;
        return (double) n / h;
    }

    private double rowFraction(boolean[][] ink, int y) {
        int w = ink[0].length, n = 0;
        for (int x = 0; x < w; x++) if (ink[y][x]) n++;
        return (double) n / w;
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
        boolean[][] eroded = erode(fg, 1);
        boolean[][] cc = largestComponent(eroded);
        boolean[][] back = dilate(cc, 1);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                back[y][x] = back[y][x] && fg[y][x];
        return fillHoles(back);
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

    private boolean[][] largestComponent(boolean[][] mask) {
        int h = mask.length, w = mask[0].length;
        boolean[][] seen = new boolean[h][w];
        boolean[][] best = new boolean[h][w];
        int bestSize = 0;
        int[][] d8 = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        Deque<int[]> dq = new ArrayDeque<>();
        for (int sy = 0; sy < h; sy++) {
            for (int sx = 0; sx < w; sx++) {
                if (mask[sy][sx] && !seen[sy][sx]) {
                    dq.clear();
                    List<int[]> comp = new ArrayList<>();
                    seen[sy][sx] = true; dq.add(new int[]{sy, sx});
                    while (!dq.isEmpty()) {
                        int[] p = dq.poll(); comp.add(p);
                        for (int[] d : d8) {
                            int ny = p[0]+d[0], nx = p[1]+d[1];
                            if (ny>=0&&ny<h&&nx>=0&&nx<w&&mask[ny][nx]&&!seen[ny][nx]) {
                                seen[ny][nx]=true; dq.add(new int[]{ny,nx});
                            }
                        }
                    }
                    if (comp.size() > bestSize) {
                        bestSize = comp.size();
                        best = new boolean[h][w];
                        for (int[] p : comp) best[p[0]][p[1]] = true;
                    }
                }
            }
        }
        return best;
    }

    private boolean[][] erode(boolean[][] m, int k) {
        int h = m.length, w = m[0].length;
        boolean[][] cur = m;
        for (int i = 0; i < k; i++) {
            boolean[][] e = new boolean[h][w];
            for (int y = 1; y < h-1; y++)
                for (int x = 1; x < w-1; x++)
                    e[y][x] = cur[y][x] && cur[y-1][x] && cur[y+1][x] && cur[y][x-1] && cur[y][x+1];
            cur = e;
        }
        return cur;
    }

    private boolean[][] dilate(boolean[][] m, int k) {
        int h = m.length, w = m[0].length;
        boolean[][] cur = m;
        for (int i = 0; i < k; i++) {
            boolean[][] d = new boolean[h][w];
            for (int y = 0; y < h; y++) System.arraycopy(cur[y], 0, d[y], 0, w);
            for (int y = 1; y < h-1; y++)
                for (int x = 1; x < w-1; x++)
                    if (cur[y-1][x] || cur[y+1][x] || cur[y][x-1] || cur[y][x+1]) d[y][x] = true;
            cur = d;
        }
        return cur;
    }

    /** Vult ingesloten gaten: achtergrond niet-bereikbaar vanaf de rand hoort bij het silhouet. */
    private boolean[][] fillHoles(boolean[][] mask) {
        int h = mask.length, w = mask[0].length;
        boolean[][] reached = new boolean[h][w];
        Deque<int[]> dq = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            seedBg(mask, reached, dq, 0, x);
            seedBg(mask, reached, dq, h-1, x);
        }
        for (int y = 0; y < h; y++) {
            seedBg(mask, reached, dq, y, 0);
            seedBg(mask, reached, dq, y, w-1);
        }
        int[][] d4 = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!dq.isEmpty()) {
            int[] p = dq.poll();
            for (int[] d : d4) {
                int ny=p[0]+d[0], nx=p[1]+d[1];
                if (ny>=0&&ny<h&&nx>=0&&nx<w&&!mask[ny][nx]&&!reached[ny][nx]) {
                    reached[ny][nx]=true; dq.add(new int[]{ny,nx});
                }
            }
        }
        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out[y][x] = mask[y][x] || !reached[y][x];
        return out;
    }

    private void seedBg(boolean[][] mask, boolean[][] reached, Deque<int[]> dq, int y, int x) {
        if (!mask[y][x] && !reached[y][x]) { reached[y][x] = true; dq.add(new int[]{y, x}); }
    }

    // -------------------------------------------------------------------------
    // Normalisatie & vergelijking
    // -------------------------------------------------------------------------

    /** Snijdt het silhouet op zijn bounding box en schaalt (nearest) naar TPL×TPL. */
    private boolean[][] normalize(boolean[][] mask) {
        int h = mask.length, w = mask[0].length;
        int minY = h, maxY = -1, minX = w, maxX = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (mask[y][x]) {
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                }
        boolean[][] out = new boolean[TPL][TPL];
        if (maxY < 0) return out;
        int bh = maxY - minY + 1, bw = maxX - minX + 1;
        for (int ty = 0; ty < TPL; ty++)
            for (int tx = 0; tx < TPL; tx++) {
                int sy = minY + ty * bh / TPL;
                int sx = minX + tx * bw / TPL;
                out[ty][tx] = mask[sy][sx];
            }
        return out;
    }

    private double iou(boolean[][] a, boolean[][] b) {
        int inter = 0, union = 0;
        for (int y = 0; y < TPL; y++)
            for (int x = 0; x < TPL; x++) {
                boolean p = a[y][x], q = b[y][x];
                if (p && q) inter++;
                if (p || q) union++;
            }
        return union == 0 ? 0 : (double) inter / union;
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
                ink[y][x] = luminance(pr.getArgb(x, y)) < 128;
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

    private double fraction(boolean[][] m) {
        return (double) count(m) / (m.length * m[0].length);
    }

    private int count(boolean[][] m) {
        int n = 0;
        for (boolean[] row : m) for (boolean b : row) if (b) n++;
        return n;
    }

    private double inkFraction(boolean[][] cell, boolean[][] within) {
        int n = 0, ink = 0;
        for (int y = 0; y < within.length; y++)
            for (int x = 0; x < within[0].length; x++)
                if (within[y][x]) { n++; if (cell[y][x]) ink++; }
        return n == 0 ? 0 : (double) ink / n;
    }

    private static int luminance(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }
}
