package application.chesstrainerfx.imagescanner;

import application.chesstrainerfx.utils.PieceColor;
import application.chesstrainerfx.utils.PieceModel;
import application.chesstrainerfx.utils.PieceType;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    // Kolom met deze ink-fractie = kaderlijn. Gemeten over de bronnen: kaderlijnen halen
    // 0.84–1.00 (anti-aliasing en labelgaten drukken de fractie), drukste niet-kaderkolommen ≤0.49.
    private static final double FRAME_LINE = 0.75;
    // Marge waarbinnen de koning-IoU van een randstuk zijn eigen (foute) match moet
    // benaderen om als de ontbrekende koning te gelden — zie enforceSingleKing().
    private static final double KING_AMBIGUITY_MARGIN = 0.1;

    private record Template(PieceType type, boolean[][] mask) {}

    /** Eén figurine-font: elke bron (boek/uitgever) heeft zijn eigen set van 12 templates. */
    private record TemplateSet(String name, List<Template> templates) {}

    private final List<TemplateSet> sets = new ArrayList<>();
    private boolean blackPerspective = false;

    public DiagramBoardScanner() {
        // Sets staan per font in /imagescanner/templates/<naam>/; de index vermijdt
        // classpath-opsomming (onmogelijk vanuit een jar). Nieuw font = map + indexregel.
        try (InputStream is = getClass().getResourceAsStream("/imagescanner/templates/diagram-sets.txt")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    String name = line.trim();
                    if (!name.isEmpty()) loadSet(name);
                }
            }
        } catch (Exception ignored) {
            // Onleesbare index: geen sets beschikbaar, scanner levert dan lege borden op.
        }
    }

    /** Stelt in of het bord vanuit zwart is weergegeven (rank 1 boven, h-lijn links). */
    public void setBlackPerspective(boolean blackPerspective) {
        this.blackPerspective = blackPerspective;
    }

    private void loadSet(String setName) {
        List<Template> templates = new ArrayList<>();
        // 6 stuktypes × 2 kleuren (massief zwart / wit contour leveren verschillende silhouetten).
        load(templates, setName, "ROOK_W",   PieceType.ROOK);   load(templates, setName, "ROOK_B",   PieceType.ROOK);
        load(templates, setName, "KNIGHT_W", PieceType.KNIGHT); load(templates, setName, "KNIGHT_B", PieceType.KNIGHT);
        load(templates, setName, "BISHOP_W", PieceType.BISHOP); load(templates, setName, "BISHOP_B", PieceType.BISHOP);
        load(templates, setName, "QUEEN_W",  PieceType.QUEEN);  load(templates, setName, "QUEEN_B",  PieceType.QUEEN);
        load(templates, setName, "KING_W",   PieceType.KING);   load(templates, setName, "KING_B",   PieceType.KING);
        load(templates, setName, "PAWN_W",   PieceType.PAWN);   load(templates, setName, "PAWN_B",   PieceType.PAWN);
        if (!templates.isEmpty()) sets.add(new TemplateSet(setName, templates));
    }

    private void load(List<Template> templates, String setName, String name, PieceType type) {
        try (InputStream is = getClass().getResourceAsStream("/imagescanner/templates/" + setName + "/" + name + ".png")) {
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

        // Rang-labels naast het bord zijn, indien aanwezig, betrouwbaarder dan de
        // handmatige oriëntatie-instelling (boeken mixen witte en zwarte perspectieven).
        Boolean labelView = detectOrientation(ink, board);
        boolean blackView = labelView != null ? labelView : blackPerspective;

        // Fase 1: per cel het silhouet en de kleur bepalen (font-onafhankelijk, en het
        // dure deel — dit hoeft maar één keer, hoeveel template-sets er ook zijn).
        boolean[][][][] silhouettes = new boolean[8][8][][];   // genormaliseerd; null = leeg veld
        PieceColor[][] colors = new PieceColor[8][8];
        boolean[][][][] altSilhouettes = new boolean[8][8][][];
        PieceColor[][] altColors = new PieceColor[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int m = (int) (CELL_MARGIN * ch);
                // Geen marge aan de bordrand-zijde: daar staat geen buurstuk, en glyphs
                // op randvelden lopen soms tot tegen het kader (afknippen vervormt ze).
                int x0 = (int) Math.round(bx + c * cw) + (c == 0 ? 0 : m);
                int x1 = (int) Math.round(bx + (c + 1) * cw) - (c == 7 ? 0 : m);
                int y0 = (int) Math.round(by + r * ch) + (r == 0 ? 0 : m);
                int y1 = (int) Math.round(by + (r + 1) * ch) - (r == 7 ? 0 : m);
                boolean[][] cell = sub(ink, x0, y0, x1 - x0, y1 - y0, w, h);

                int tr = blackView ? 7 - r : r;
                int tc = blackView ? 7 - c : c;
                boolean[][] filled = pieceMask(cell);
                if (SilhouetteUtils.fraction(filled) < EMPTY_AREA) continue;
                colors[tr][tc] = pieceColor(cell, filled);
                silhouettes[tr][tc] = SilhouetteUtils.normalize(filled);

                // Alternatief silhouet: dezelfde extractie maar met gesloten contouren
                // (dilate → vul → erodeer terug). Repareert glyphs met een contourgat
                // waardoor de flood het stuk-inwendige leegzoog — die missen een deel
                // van hun silhouet zonder dat de voorgrond dat verraadt. Wordt alleen
                // geraadpleegd als het primaire silhouet matig matcht (fase 3) en wint
                // alleen bij een strikt betere match binnen dezelfde template-set.
                boolean[][] alt = pieceMaskClosed(cell);
                if (SilhouetteUtils.fraction(alt) >= EMPTY_AREA && SilhouetteUtils.fraction(alt) <= 0.6) {
                    altSilhouettes[tr][tc] = SilhouetteUtils.normalize(alt);
                    altColors[tr][tc] = pieceColor(cell, alt);
                }
            }
        }

        // Fase 2: binnen één diagram komt alles uit één font, dus wint de template-set
        // met de hoogste gemiddelde IoU over de bezette velden.
        Template[][] matches = null;
        double[][] matchIou = null;
        TemplateSet bestSet = null;
        double bestScore = -1;
        for (TemplateSet set : sets) {
            Template[][] m = new Template[8][8];
            double[][] v = new double[8][8];
            double sum = 0;
            int n = 0;
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++) {
                    if (silhouettes[r][c] == null) continue;
                    for (Template t : set.templates()) {
                        double iou = SilhouetteUtils.iou(silhouettes[r][c], t.mask());
                        if (iou > v[r][c]) { v[r][c] = iou; m[r][c] = t; }
                    }
                    sum += v[r][c];
                    n++;
                }
            double score = n == 0 ? 0 : sum / n;
            if (score > bestScore) { bestScore = score; matches = m; matchIou = v; bestSet = set; }
        }

        PieceModel[][] pieces = new PieceModel[8][8];
        double[][] confidence = new double[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                if (silhouettes[r][c] == null || matches == null || matches[r][c] == null) {
                    confidence[r][c] = silhouettes[r][c] == null ? 1.0 : 0.0;
                    continue;
                }
                Template match = matches[r][c];
                double iou = matchIou[r][c];
                PieceColor color = colors[r][c];
                // Randcel met matig matchend primair silhouet: als het alternatieve
                // (niet-afgeknipte) silhouet binnen dezelfde set beter matcht, wint dat.
                if (iou < 0.8 && altSilhouettes[r][c] != null && bestSet != null) {
                    for (Template t : bestSet.templates()) {
                        double v = SilhouetteUtils.iou(altSilhouettes[r][c], t.mask());
                        if (v > iou) { iou = v; match = t; color = altColors[r][c]; }
                    }
                }
                pieces[r][c] = new PieceModel(match.type(), color);
                confidence[r][c] = iou;   // lage IoU → UI markeert het veld als onzeker
            }
        enforceSingleKing(pieces, confidence, silhouettes, altSilhouettes, bestSet);
        return new ScanResult(pieces, confidence);
    }

    /**
     * Precies één koning per kleur is in elk diagram uit deze bronnen een harde
     * schaakregel-invariant (geverifieerd over alle testseries: nooit anders). Een
     * ontbrekende koning is dus altijd een fout van een ander stuk op hetzelfde veld.
     * De enige waargenomen oorzaak is een koning-glyph die in het brondiagram tegen het
     * bordkader aan gedrukt staat (dus alleen mogelijk op een randveld) en daardoor als
     * paard matcht, met een koning-IoU die nog net in de buurt komt van zijn eigen
     * (foute) match. Een schoon, zelfverzekerd stuk elders (bv. een echte toren met
     * IoU 0.87 tegen zijn eigen template) matcht een koningstemplate soms toevallig
     * ook redelijk (~0.67) puur door vorm-overlap in de 56×56-normalisatie — daarom
     * telt alleen de kandidaat waarvan de koning-IoU zijn eigen matchscore benadert
     * (marge KING_AMBIGUITY_MARGIN): dat is het randveld dat werkelijk tussen twee
     * types zweeft, niet het veld dat toevallig het hoogste absolute koning-IoU haalt.
     */
    private void enforceSingleKing(PieceModel[][] pieces, double[][] confidence,
            boolean[][][][] silhouettes, boolean[][][][] altSilhouettes, TemplateSet bestSet) {
        if (bestSet == null) return;
        List<Template> kingTemplates = new ArrayList<>();
        for (Template t : bestSet.templates()) if (t.type() == PieceType.KING) kingTemplates.add(t);
        if (kingTemplates.isEmpty()) return;
        for (PieceColor color : PieceColor.values()) {
            boolean hasKing = false;
            for (int r = 0; r < 8 && !hasKing; r++)
                for (int c = 0; c < 8; c++)
                    if (pieces[r][c] != null && pieces[r][c].getType() == PieceType.KING && pieces[r][c].getColor() == color) {
                        hasKing = true;
                        break;
                    }
            if (hasKing) continue;
            int bestR = -1, bestC = -1;
            double bestIou = -1;
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++) {
                    if (pieces[r][c] == null || pieces[r][c].getColor() != color) continue;
                    if (r != 0 && r != 7 && c != 0 && c != 7) continue;
                    double kingIou = 0;
                    for (Template t : kingTemplates) {
                        kingIou = Math.max(kingIou, SilhouetteUtils.iou(silhouettes[r][c], t.mask()));
                        if (altSilhouettes[r][c] != null) {
                            kingIou = Math.max(kingIou, SilhouetteUtils.iou(altSilhouettes[r][c], t.mask()));
                        }
                    }
                    if (kingIou < confidence[r][c] - KING_AMBIGUITY_MARGIN) continue;
                    if (kingIou > bestIou) { bestIou = kingIou; bestR = r; bestC = c; }
                }
            if (bestR >= 0) {
                pieces[bestR][bestC] = new PieceModel(PieceType.KING, color);
                confidence[bestR][bestC] = bestIou;
            }
        }
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

        // Dubbele rand: sla eventuele extra lijnen binnen een halve cel van het kader over.
        int gap = (right - left) / 16;
        for (int x = innerLeft; x < innerLeft + gap && x < w; x++)
            if (colFraction(ink, x) > 0.5) {
                while (x < w - 1 && colFraction(ink, x) > 0.5) x++;
                innerLeft = x;
            }
        for (int x = innerRight; x > innerRight - gap && x > 0; x--)
            if (colFraction(ink, x) > 0.5) {
                while (x > 0 && colFraction(ink, x) > 0.5) x--;
                innerRight = x;
            }

        int top = 0, bottom = h - 1;
        while (top < h - 1 && !ink[top][left]) top++;
        while (bottom > 0 && !ink[bottom][left]) bottom--;
        int thickness = innerLeft - left;
        int innerTop = top + thickness, innerBottom = bottom - thickness;

        // Zelfde dubbele-rand-correctie verticaal (rijfractie over het binnengebied).
        for (int y = innerTop; y < innerTop + gap && y < h; y++)
            if (rowFraction(ink, y, innerLeft, innerRight) > 0.5) {
                while (y < h - 1 && rowFraction(ink, y, innerLeft, innerRight) > 0.5) y++;
                innerTop = y;
            }
        for (int y = innerBottom; y > innerBottom - gap && y > 0; y--)
            if (rowFraction(ink, y, innerLeft, innerRight) > 0.5) {
                while (y > 0 && rowFraction(ink, y, innerLeft, innerRight) > 0.5) y--;
                innerBottom = y;
            }

        if (innerRight <= innerLeft || innerBottom <= innerTop) return new int[]{0, 0, w, h};
        return new int[]{innerLeft, innerTop, innerRight - innerLeft + 1, innerBottom - innerTop + 1};
    }

    private double rowFraction(boolean[][] ink, int y, int x0, int x1) {
        int n = 0;
        for (int x = x0; x <= x1; x++) if (ink[y][x]) n++;
        return (double) n / (x1 - x0 + 1);
    }

    private double colFraction(boolean[][] ink, int x) {
        int h = ink.length, n = 0;
        for (int y = 0; y < h; y++) if (ink[y][x]) n++;
        return (double) n / h;
    }

    // -------------------------------------------------------------------------
    // Oriëntatie-detectie
    // -------------------------------------------------------------------------

    /**
     * Leest de bordoriëntatie af aan de rang-labels links van het bord, via het aantal
     * ingesloten gaten in de cijferglyphs (font-onafhankelijk): '8' heeft er twee,
     * '1' geen. Bovenste label '8' → wit onder (false); '1' boven én '8' onder →
     * zwart onder (true). Geen of onduidelijke labels → null (val terug op de
     * handmatige instelling).
     */
    private Boolean detectOrientation(boolean[][] ink, int[] board) {
        double ch = board[3] / 8.0;
        if (board[0] < ch / 3) return null;   // geen labelstrook links van het bord
        int top = labelHoles(ink, board, 0);
        int bottom = labelHoles(ink, board, 7);
        if (top == 2 && bottom == 0) return false;
        if (top == 0 && bottom == 2) return true;
        return null;
    }

    /** Aantal gaten in de grootste glyph in de labelstrook naast rij {@code row},
     *  of -1 als daar geen plausibele glyph staat. */
    private int labelHoles(boolean[][] ink, int[] board, int row) {
        double ch = board[3] / 8.0;
        int y0 = (int) Math.round(board[1] + row * ch);
        int bandH = (int) Math.round(ch);
        boolean[][] strip = sub(ink, 0, y0, board[0], bandH, ink[0].length, ink.length);
        // Verticale lijnen (beeldrand, kaderlijn) lopen door de strook en zouden anders
        // als grootste component het cijfer verdringen.
        for (int x = 0; x < strip[0].length; x++) {
            int n = 0;
            for (int y = 0; y < strip.length; y++) if (strip[y][x]) n++;
            if (n > 0.9 * strip.length)
                for (int y = 0; y < strip.length; y++) strip[y][x] = false;
        }
        boolean[][] glyph = SilhouetteUtils.largestComponent(strip);
        int area = SilhouetteUtils.count(glyph);
        // Plausibele cijfergrootte: niet slechts wat ruis, niet zo hoog als de band zelf
        // (dat is de kaderlijn die door de strook loopt).
        if (area < 20) return -1;
        int minY = bandH, maxY = -1;
        for (int y = 0; y < glyph.length; y++)
            for (int x = 0; x < glyph[0].length; x++)
                if (glyph[y][x]) { if (y < minY) minY = y; if (y > maxY) maxY = y; }
        if (maxY - minY + 1 > 0.9 * bandH) return -1;
        boolean[][] filled = SilhouetteUtils.fillHoles(glyph);
        boolean[][] holes = new boolean[glyph.length][glyph[0].length];
        for (int y = 0; y < glyph.length; y++)
            for (int x = 0; x < glyph[0].length; x++)
                holes[y][x] = filled[y][x] && !glyph[y][x];
        return countComponents(holes);
    }

    private int countComponents(boolean[][] mask) {
        int h = mask.length, w = mask[0].length, n = 0;
        boolean[][] seen = new boolean[h][w];
        Deque<int[]> dq = new ArrayDeque<>();
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int sy = 0; sy < h; sy++)
            for (int sx = 0; sx < w; sx++)
                if (mask[sy][sx] && !seen[sy][sx]) {
                    n++;
                    seen[sy][sx] = true; dq.add(new int[]{sy, sx});
                    while (!dq.isEmpty()) {
                        int[] p = dq.poll();
                        for (int[] d : d4) {
                            int ny = p[0] + d[0], nx = p[1] + d[1];
                            if (ny >= 0 && ny < h && nx >= 0 && nx < w && mask[ny][nx] && !seen[ny][nx]) {
                                seen[ny][nx] = true; dq.add(new int[]{ny, nx});
                            }
                        }
                    }
                }
        return n;
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
        boolean[][] filled = extractPiece(fg);
        // Wit stuk met open contour op kleine scans: de flood lekt het stuk binnen en de
        // erosie versplintert de rest, zodat maar een fractie van de voorgrond overblijft.
        // De poorten sluiten normale cellen uit: een compleet stuk vult ≥75% van zijn
        // voorgrond; en de grootste kern die de erosie overleeft moet massief zijn (een
        // stukdeel vult zijn bounding box grotendeels, gemeten ~0.66) — een overlevende
        // arceringslijn van een leeg donker veld is een diagonale sliver (~0.02). Sluit
        // dan de contourgaten en extraheer opnieuw.
        boolean[][] fgCore = SilhouetteUtils.erode(fg, 1);
        boolean[][] core = SilhouetteUtils.largestComponent(fgCore);
        int coreArea = SilhouetteUtils.count(core);
        int coreBox = bboxArea(core);
        // Absolute drempel schaalt mee met de celgrootte: op zeer kleine scans (~25px cel)
        // vreet zelfs één erosie een dunne holle contour (koning, paard) bijna helemaal weg
        // (nog maar enkele pixels over), waardoor de reparatiepoort nooit opengaat en het
        // silhouet drastisch te klein blijft.
        int minCoreArea = h < 30 ? 4 : 25;
        if (SilhouetteUtils.fraction(filled) < 0.2
                && SilhouetteUtils.count(filled) * 4 < SilhouetteUtils.count(fg) * 3
                && SilhouetteUtils.count(fgCore) >= minCoreArea
                && coreBox > 0 && coreArea * 10 >= coreBox * 3) {
            // Op fonts zonder arcering (donkere velden binariseren daar nooit als inkt,
            // dus fg is al ruisvrij) is de voorgrond zelf al de juiste holle contour — de
            // erosie in extractPiece vreet die op zeer kleine scans juist te veel weg.
            // Probeer daarom eerst de voorgrond direct (alleen gaten vullen, geen erosie),
            // met een eigen dichtheidspoort zodat een arceringskluwen (wél aanwezig bij de
            // andere fonts) dit nooit haalt. Alleen als dat niet plausibel is, val terug
            // op de sluitradius-lus (die arcering wél kan bevatten en dus erosie nodig
            // heeft om te scheiden van het echte stuk).
            boolean[][] direct = SilhouetteUtils.fillHoles(SilhouetteUtils.largestComponent(fg));
            int directArea = SilhouetteUtils.count(direct);
            int directBox = bboxArea(direct);
            // Lagere dichtheidsdrempel dan de buitenste poort (0.3): een paardenprofiel
            // (smal, met oren/kaaklijn) vult zijn eigen bbox van nature maar ~0.2, terwijl
            // een arceringssliver van een leeg veld daar nog ruim onder blijft (~0.02).
            if (directBox > 0 && directArea * 10 >= directBox * 1.5
                    && SilhouetteUtils.fraction(direct) <= 0.6
                    && directArea * 2 > SilhouetteUtils.count(filled) * 3) {
                filled = direct;
            } else {
                // Kleinste sluitradius die het gat dicht wint: een grotere radius dan
                // nodig smeert fijn stukdetail (torenkantelen) dicht. Een geslaagde
                // reparatie levert substantieel meer silhouet op maar nooit een plak
                // over de hele cel.
                for (int radius = 1; radius <= 2; radius++) {
                    boolean[][] closed = SilhouetteUtils.erode(
                            SilhouetteUtils.fillHoles(SilhouetteUtils.dilate(fg, radius)), radius);
                    boolean[][] retry = extractPiece(closed);
                    if (SilhouetteUtils.count(retry) * 2 > SilhouetteUtils.count(filled) * 3
                            && SilhouetteUtils.fraction(retry) <= 0.6) {
                        filled = retry;
                        break;
                    }
                }
            }
        }
        return filled;
    }

    private int bboxArea(boolean[][] m) {
        int h = m.length, w = m[0].length;
        int minY = h, maxY = -1, minX = w, maxX = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (m[y][x]) {
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                }
        return maxY < 0 ? 0 : (maxY - minY + 1) * (maxX - minX + 1);
    }

    private boolean[][] extractPiece(boolean[][] fg) {
        int h = fg.length, w = fg[0].length;
        boolean[][] eroded = SilhouetteUtils.erode(fg, 1);
        boolean[][] cc = mergeNearbyComponents(eroded);
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

    /**
     * Grootste component plus omvangrijke fragmenten vlakbij (transitief, ≤3px):
     * witte sierbanden in een glyph kunnen de romp bij de erosie in delen knippen
     * (bv. de voet van een dame), en die delen horen bij het stuk. Arcering overleeft
     * de erosie niet en blijft dus sowieso uitgesloten.
     */
    private boolean[][] mergeNearbyComponents(boolean[][] eroded) {
        int h = eroded.length, w = eroded[0].length;
        List<boolean[][]> comps = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        boolean[][] seen = new boolean[h][w];
        Deque<int[]> dq = new ArrayDeque<>();
        int[][] d8 = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int sy = 0; sy < h; sy++)
            for (int sx = 0; sx < w; sx++)
                if (eroded[sy][sx] && !seen[sy][sx]) {
                    boolean[][] comp = new boolean[h][w];
                    int size = 0;
                    seen[sy][sx] = true; dq.add(new int[]{sy, sx});
                    while (!dq.isEmpty()) {
                        int[] p = dq.poll();
                        comp[p[0]][p[1]] = true; size++;
                        for (int[] d : d8) {
                            int ny = p[0]+d[0], nx = p[1]+d[1];
                            if (ny>=0&&ny<h&&nx>=0&&nx<w&&eroded[ny][nx]&&!seen[ny][nx]) {
                                seen[ny][nx]=true; dq.add(new int[]{ny,nx});
                            }
                        }
                    }
                    comps.add(comp); sizes.add(size);
                }
        if (comps.isEmpty()) return new boolean[h][w];
        int largest = 0;
        for (int i = 1; i < comps.size(); i++) if (sizes.get(i) > sizes.get(largest)) largest = i;

        boolean[][] union = comps.get(largest);
        boolean[] used = new boolean[comps.size()];
        used[largest] = true;
        int minSize = Math.max(8, sizes.get(largest) / 20);
        boolean grown = true;
        while (grown) {
            grown = false;
            boolean[][] reach = SilhouetteUtils.dilate(union, 3);
            for (int i = 0; i < comps.size(); i++) {
                if (used[i] || sizes.get(i) < minSize) continue;
                if (overlaps(reach, comps.get(i))) {
                    for (int y = 0; y < h; y++)
                        for (int x = 0; x < w; x++)
                            if (comps.get(i)[y][x]) union[y][x] = true;
                    used[i] = true;
                    grown = true;
                }
            }
        }
        return union;
    }

    private boolean overlaps(boolean[][] a, boolean[][] b) {
        for (int y = 0; y < a.length; y++)
            for (int x = 0; x < a[0].length; x++)
                if (a[y][x] && b[y][x]) return true;
        return false;
    }

    /** Span-gevulde variant van pieceMask: benadert het massieve silhouet ook wanneer de
     *  contour wijd openstaat (bv. een glyph die tegen het bordkader is afgeknipt),
     *  waar sluiting per dilatatie tekortschiet. */
    private boolean[][] pieceMaskClosed(boolean[][] cell) {
        boolean[][] reached = floodWhite(cell);
        int h = cell.length, w = cell[0].length;
        boolean[][] fg = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                fg[y][x] = !reached[y][x];
        // Direct op de voorgrond (niet op de geërodeerde kern): juist de dunne stroken
        // van de open contour bepalen de omtrek die de span-vulling moet overspannen.
        return SilhouetteUtils.spanFill(fg);
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

    /**
     * Kleur via inkt-overleving: witte stukken bestaan uit dunne contour- en detaillijnen
     * die een erosie niet overleven; zwarte stukken houden massieve inktvlakken over.
     * Dit werkt ook voor fonts waarin zwarte stukken wit ornament dragen en witte stukken
     * dicht gearceerd zijn — daar faalt een simpele ink-fractie in de kern.
     */
    private PieceColor pieceColor(boolean[][] cell, boolean[][] filled) {
        int h = cell.length, w = cell[0].length;
        boolean[][] pieceInk = new boolean[h][w];
        int total = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (filled[y][x] && cell[y][x]) { pieceInk[y][x] = true; total++; }
        if (total == 0) return PieceColor.WHITE;
        // Erosieradius schaalt mee met de celgrootte: op zeer kleine scans (~25px cel)
        // erodeert een vaste radius van 3 ook een compacte massieve kroon (koning) tot
        // niets, waardoor een zwarte koning ten onrechte als wit uitleest. Kleinere
        // bronnen (kp/m2/cp/book, alle ≥34px) blijven op radius 3 — alleen ruim
        // kleinere cellen gebruiken radius 1, nog altijd genoeg om een dunne witte
        // contourlijn volledig weg te vreten terwijl een massieve vlek overleeft.
        int radius = h < 30 ? 1 : 3;
        double survival = (double) SilhouetteUtils.count(SilhouetteUtils.erode(pieceInk, radius)) / total;
        return survival > 0.045 ? PieceColor.BLACK : PieceColor.WHITE;
    }
}
