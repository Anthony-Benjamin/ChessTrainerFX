package application.chesstrainerfx.imagescanner;

import application.chesstrainerfx.utils.PieceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Uitvoer van een bord-scan: 8×8 grid van stukken + betrouwbaarheid per veld.
 * null in pieces[][] = leeg veld.
 * confidence 0.0–1.0 per veld; onder LOW_CONFIDENCE_THRESHOLD → UI markeert het oranje.
 */
public class ScanResult {

    public static final double LOW_CONFIDENCE_THRESHOLD = 0.65;

    private final PieceModel[][] pieces;
    private final double[][] confidence;

    public ScanResult(PieceModel[][] pieces, double[][] confidence) {
        this.pieces = pieces;
        this.confidence = confidence;
    }

    public PieceModel getPiece(int row, int col) {
        return pieces[row][col];
    }

    public double getConfidence(int row, int col) {
        return confidence[row][col];
    }

    /** Geeft [row, col]-paren terug voor velden waarvan de scanner niet zeker was. */
    public List<int[]> getLowConfidenceSquares() {
        List<int[]> result = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (confidence[r][c] < LOW_CONFIDENCE_THRESHOLD)
                    result.add(new int[]{r, c});
        return result;
    }

    /**
     * Genereert een FEN-string voor de stukopstelling.
     * Voldoet aan het patroon dat BoardModel.initializeFromFEN() verwacht:
     * rokade/en-passant worden weggelaten (zelfde conventie als elders in de codebase).
     */
    public String toFEN(boolean whiteToMove) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            int empty = 0;
            for (int col = 0; col < 8; col++) {
                PieceModel p = pieces[row][col];
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    sb.append(p.getFENChar());
                }
            }
            if (empty > 0) sb.append(empty);
            if (row < 7) sb.append('/');
        }
        sb.append(whiteToMove ? " w" : " b");
        sb.append(" - - 0 1");
        return sb.toString();
    }
}
