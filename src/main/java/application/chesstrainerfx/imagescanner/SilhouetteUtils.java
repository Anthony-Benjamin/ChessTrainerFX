package application.chesstrainerfx.imagescanner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Gedeelde silhouet-bewerkingen voor bord-scanners: morfologie, componentanalyse,
 * normalisatie en vergelijking van binaire maskers (boolean[y][x], true = stukpixel).
 */
final class SilhouetteUtils {

    /** Genormaliseerde template-grootte (zijde in pixels). */
    static final int TPL = 56;

    private SilhouetteUtils() {
    }

    /** Luminantie gecomposit over een witte achtergrond: (deels) transparante pixels
     *  zijn visueel (deels) wit, ook als hun eigen kleur donker is. */
    static int luminance(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int lum = (r * 299 + g * 587 + b * 114) / 1000;
        return (lum * a + 255 * (255 - a)) / 255;
    }

    static boolean[][] mirror(boolean[][] m) {
        int h = m.length, w = m[0].length;
        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out[y][x] = m[y][w - 1 - x];
        return out;
    }

    static boolean[][] erode(boolean[][] m, int k) {
        int h = m.length, w = m[0].length;
        boolean[][] cur = m;
        for (int i = 0; i < k; i++) {
            boolean[][] e = new boolean[h][w];
            for (int y = 1; y < h - 1; y++)
                for (int x = 1; x < w - 1; x++)
                    e[y][x] = cur[y][x] && cur[y - 1][x] && cur[y + 1][x] && cur[y][x - 1] && cur[y][x + 1];
            cur = e;
        }
        return cur;
    }

    static boolean[][] dilate(boolean[][] m, int k) {
        int h = m.length, w = m[0].length;
        boolean[][] cur = m;
        for (int i = 0; i < k; i++) {
            boolean[][] d = new boolean[h][w];
            for (int y = 0; y < h; y++) System.arraycopy(cur[y], 0, d[y], 0, w);
            for (int y = 1; y < h - 1; y++)
                for (int x = 1; x < w - 1; x++)
                    if (cur[y - 1][x] || cur[y + 1][x] || cur[y][x - 1] || cur[y][x + 1]) d[y][x] = true;
            cur = d;
        }
        return cur;
    }

    static boolean[][] largestComponent(boolean[][] mask) {
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

    /** Vult ingesloten gaten: achtergrond niet-bereikbaar vanaf de rand hoort bij het silhouet. */
    static boolean[][] fillHoles(boolean[][] mask) {
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

    private static void seedBg(boolean[][] mask, boolean[][] reached, Deque<int[]> dq, int y, int x) {
        if (!mask[y][x] && !reached[y][x]) { reached[y][x] = true; dq.add(new int[]{y, x}); }
    }

    /** Vult per rij én per kolom tussen de eerste en laatste silhouetpixel en neemt de
     *  doorsnede: een massieve benadering van een contour die ergens openstaat. */
    static boolean[][] spanFill(boolean[][] m) {
        int h = m.length, w = m[0].length;
        boolean[][] rows = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            int lo = -1, hi = -1;
            for (int x = 0; x < w; x++) if (m[y][x]) { if (lo < 0) lo = x; hi = x; }
            for (int x = lo; x >= 0 && x <= hi; x++) rows[y][x] = true;
        }
        boolean[][] out = new boolean[h][w];
        for (int x = 0; x < w; x++) {
            int lo = -1, hi = -1;
            for (int y = 0; y < h; y++) if (m[y][x]) { if (lo < 0) lo = y; hi = y; }
            for (int y = lo; y >= 0 && y <= hi; y++) out[y][x] = rows[y][x];
        }
        return out;
    }

    /**
     * Snijdt het silhouet op zijn bounding box en schaalt (nearest) uniform naar TPL×TPL,
     * gecentreerd. Aspectratio blijft behouden: die onderscheidt bv. een brede lage pion
     * van een hoge slanke loper — informatie die bij anisotroop oprekken verloren gaat.
     */
    static boolean[][] normalize(boolean[][] mask) {
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
        int side = Math.max(bh, bw);
        int offY = (side - bh) / 2, offX = (side - bw) / 2;
        for (int ty = 0; ty < TPL; ty++)
            for (int tx = 0; tx < TPL; tx++) {
                int sy = minY + ty * side / TPL - offY;
                int sx = minX + tx * side / TPL - offX;
                out[ty][tx] = sy >= minY && sy <= maxY && sx >= minX && sx <= maxX && mask[sy][sx];
            }
        return out;
    }

    static double iou(boolean[][] a, boolean[][] b) {
        int inter = 0, union = 0;
        for (int y = 0; y < TPL; y++)
            for (int x = 0; x < TPL; x++) {
                boolean p = a[y][x], q = b[y][x];
                if (p && q) inter++;
                if (p || q) union++;
            }
        return union == 0 ? 0 : (double) inter / union;
    }

    static double fraction(boolean[][] m) {
        return (double) count(m) / (m.length * m[0].length);
    }

    static int count(boolean[][] m) {
        int n = 0;
        for (boolean[] row : m) for (boolean b : row) if (b) n++;
        return n;
    }
}
