package application.chesstrainerfx.imagescanner;

import application.chesstrainerfx.utils.PieceColor;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.utils.PieceType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scanner voor gerenderde (computer-gegenereerde) diagrammen zoals app- of
 * Fritz-exports: egale grijze donkere velden, anti-aliased stukken met gevulde
 * kleuren (wit = beige met donkere contour, zwart = massief donker), een donkere
 * rand rond het bord en soms gekleurde annotaties (pijlen, markeringen).
 *
 * Algoritme:
 *  1. Rand strippen: de buitenste vrijwel volledig donkere rijen/kolommen vormen
 *     de bordrand; het binnengebied is het 8×8-bord.
 *  2. Per cel: achtergrondluminantie = mediaan van de vier hoekvlakjes; stukpixels
 *     wijken daar meer dan een drempel van af. Verzadigde (gekleurde) pixels zijn
 *     annotaties en tellen als achtergrond. Grootste component + gaten vullen;
 *     een contour-only masker (beige stuk op licht veld) wordt via span-vulling
 *     massief gemaakt.
 *  3. Kleur: gemiddelde luminantie van de bronpixels in de geërodeerde kern.
 *  4. Type: silhouet aspect-behoudend genormaliseerd en vergeleken met de
 *     rendered-templates via IoU.
 *
 * Thread-safe: scan() gebruikt geen gedeelde mutable state buiten de (vooraf geladen) templates.
 */
public class RenderedBoardScanner implements BoardScanner {

    private static final double EMPTY_AREA = 0.05;   // silhouet-oppervlak onder deze fractie = leeg
    private static final double CELL_MARGIN = 0.04;  // celrand afsnijden om veldovergangen te mijden
    private static final int BORDER_LUM = 90;        // pixel donkerder dan dit kan bordrand zijn
    private static final double BORDER_ROW = 0.95;   // rij/kolom met deze donker-fractie = rand
    private static final int PIECE_DIFF = 30;        // luminantie-afwijking t.o.v. achtergrond = stuk
    private static final double MAX_SAT = 0.35;      // verzadigder dan dit = annotatiekleur

    private record Template(PieceType type, boolean[][] mask) {}

    private final List<Template> templates = new ArrayList<>();
    private boolean blackPerspective = false;

    public RenderedBoardScanner() {
        // Wit en zwart delen hier het silhouet (beide massief na vulling), maar de sets
        // verschillen subtiel in contourdikte; beide laden kost niets extra.
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
        try (InputStream is = getClass().getResourceAsStream("/imagescanner/templates/rendered/" + name + ".png")) {
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
        int w = (int) boardImage.getWidth(), h = (int) boardImage.getHeight();
        PixelReader pr = boardImage.getPixelReader();
        int[][] lum = new int[h][w];
        double[][] sat = new double[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int argb = pr.getArgb(x, y);
                lum[y][x] = SilhouetteUtils.luminance(argb);
                sat[y][x] = saturation(argb);
            }

        int[] board = detectBoard(lum);
        int bx = board[0], by = board[1];
        double cw = board[2] / 8.0, ch = board[3] / 8.0;

        PieceModel[][] pieces = new PieceModel[8][8];
        double[][] confidence = new double[8][8];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int m = (int) (CELL_MARGIN * ch);
                int cy0 = (int) Math.round(by + r * ch) + m;
                int cx0 = (int) Math.round(bx + c * cw) + m;
                int cellW = (int) Math.round(cw) - 2 * m, cellH = (int) Math.round(ch) - 2 * m;

                int tr = blackPerspective ? 7 - r : r;
                int tc = blackPerspective ? 7 - c : c;
                classifyInto(lum, sat, cx0, cy0, cellW, cellH, pieces, confidence, tr, tc);
            }
        }
        return new ScanResult(pieces, confidence);
    }

    private void classifyInto(int[][] lum, double[][] sat, int x0, int y0, int cw, int ch,
                              PieceModel[][] pieces, double[][] conf, int tr, int tc) {
        boolean[][] mask = pieceMask(lum, sat, x0, y0, cw, ch);
        if (SilhouetteUtils.fraction(mask) < EMPTY_AREA) {
            pieces[tr][tc] = null;
            conf[tr][tc] = 1.0;
            return;
        }
        PieceColor color = medianCoreLum(lum, x0, y0, mask) > 100 ? PieceColor.WHITE : PieceColor.BLACK;

        boolean[][] nm = SilhouetteUtils.normalize(mask);
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
     * Stript de donkere rand rond het bord: {x, y, breedte, hoogte}.
     * De buitenste rij/kolom kan lichter zijn (anti-aliased beeldrand), dus de rand
     * loopt tot en met de laatste donkere rij/kolom binnen het buitenste kwart.
     */
    private int[] detectBoard(int[][] lum) {
        int h = lum.length, w = lum[0].length;
        int top = 0, bottom = h - 1, left = 0, right = w - 1;
        for (int y = 0; y < h / 4; y++) if (rowDark(lum, y, 0, w - 1)) top = y + 1;
        for (int y = h - 1; y > h - h / 4; y--) if (rowDark(lum, y, 0, w - 1)) bottom = y - 1;
        for (int x = 0; x < w / 4; x++) if (colDark(lum, x, top, bottom)) left = x + 1;
        for (int x = w - 1; x > w - w / 4; x--) if (colDark(lum, x, top, bottom)) right = x - 1;
        return new int[]{left, top, right - left + 1, bottom - top + 1};
    }

    private boolean rowDark(int[][] lum, int y, int x0, int x1) {
        int n = 0, dark = 0;
        for (int x = x0; x <= x1; x++) { n++; if (lum[y][x] < BORDER_LUM) dark++; }
        return dark >= n * BORDER_ROW;
    }

    private boolean colDark(int[][] lum, int x, int y0, int y1) {
        int n = 0, dark = 0;
        for (int y = y0; y <= y1; y++) { n++; if (lum[y][x] < BORDER_LUM) dark++; }
        return dark >= n * BORDER_ROW;
    }

    // -------------------------------------------------------------------------
    // Silhouet-extractie
    // -------------------------------------------------------------------------

    /** Achtergrondluminantie van een cel = mediaan over de vier hoekvlakjes
     *  (daar staat vrijwel nooit stuk). */
    private int backgroundLum(int[][] lum, int x0, int y0, int cw, int ch) {
        int p = Math.max(2, cw / 5);
        int[] vals = new int[4 * p * p];
        int i = 0;
        int[][] corners = {{0, 0}, {cw - p, 0}, {0, ch - p}, {cw - p, ch - p}};
        for (int[] c : corners)
            for (int y = 0; y < p; y++)
                for (int x = 0; x < p; x++)
                    vals[i++] = lum[y0 + c[1] + y][x0 + c[0] + x];
        Arrays.sort(vals);
        return vals[vals.length / 2];
    }

    /** Stukpixels = luminantie wijkt af van de veld-achtergrond; verzadigde
     *  annotatiekleuren tellen als achtergrond. Contour-only maskers (beige stuk
     *  op licht veld met open contour) worden via span-vulling massief gemaakt. */
    private boolean[][] pieceMask(int[][] lum, double[][] sat, int x0, int y0, int cw, int ch) {
        int bg = backgroundLum(lum, x0, y0, cw, ch);
        boolean[][] fg = new boolean[ch][cw];
        for (int y = 0; y < ch; y++)
            for (int x = 0; x < cw; x++)
                fg[y][x] = sat[y0 + y][x0 + x] < MAX_SAT
                        && Math.abs(lum[y0 + y][x0 + x] - bg) > PIECE_DIFF;
        // Sluiting (dilate→…→erode) verbindt fragmenten van stukken met glans-middentonen
        // die tegen de grijze achtergrond wegvallen, zodat één component overblijft.
        boolean[][] filled = SilhouetteUtils.erode(
                SilhouetteUtils.fillHoles(SilhouetteUtils.largestComponent(SilhouetteUtils.dilate(fg, 1))), 1);
        int area = SilhouetteUtils.count(filled);
        if (area > 0 && SilhouetteUtils.count(SilhouetteUtils.erode(filled, 2)) * 4 < area) {
            filled = SilhouetteUtils.spanFill(filled);
        }
        return filled;
    }

    /**
     * Mediaan-luminantie van de kern van het masker. De mediaan is ongevoelig voor de
     * highlights op zwarte stukken én voor de donkere contourlijnen op witte stukken
     * (een gemiddelde kantelt daardoor bij detailrijke stukken zoals de dame).
     */
    private int medianCoreLum(int[][] lum, int x0, int y0, boolean[][] mask) {
        boolean[][] core = SilhouetteUtils.erode(mask, 3);
        if (SilhouetteUtils.count(core) == 0) core = SilhouetteUtils.erode(mask, 1);
        if (SilhouetteUtils.count(core) == 0) core = mask;
        List<Integer> vals = new ArrayList<>();
        for (int y = 0; y < mask.length; y++)
            for (int x = 0; x < mask[0].length; x++)
                if (core[y][x]) vals.add(lum[y0 + y][x0 + x]);
        if (vals.isEmpty()) return 0;
        vals.sort(null);
        return vals.get(vals.size() / 2);
    }

    private static double saturation(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        return max == 0 ? 0 : (double) (max - min) / max;
    }
}
