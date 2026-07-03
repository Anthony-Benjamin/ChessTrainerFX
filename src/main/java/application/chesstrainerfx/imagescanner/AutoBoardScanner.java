package application.chesstrainerfx.imagescanner;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * Kiest per afbeelding automatisch de juiste scanner:
 *  - boekscans (DiagramBoardScanner) zijn vrijwel bilevel: bijna alle pixels zijn
 *    zwart of wit, arcering incluis;
 *  - gerenderde diagrammen (RenderedBoardScanner) hebben egale grijze velden en
 *    daarmee een grote midden-grijze pixelpopulatie.
 * De fractie pixels met luminantie in het middengebied bepaalt de keuze.
 */
public class AutoBoardScanner implements BoardScanner {

    private static final int MID_LOW = 80, MID_HIGH = 180;   // "grijs" luminantiebereik
    private static final double RENDERED_MIN = 0.15;         // grijs-fractie boven dit = gerenderd

    private final DiagramBoardScanner diagramScanner = new DiagramBoardScanner();
    private final RenderedBoardScanner renderedScanner = new RenderedBoardScanner();

    /** Stelt in of het bord vanuit zwart is weergegeven (rank 1 boven, h-lijn links). */
    public void setBlackPerspective(boolean blackPerspective) {
        diagramScanner.setBlackPerspective(blackPerspective);
        renderedScanner.setBlackPerspective(blackPerspective);
    }

    @Override
    public ScanResult scan(Image boardImage) {
        BoardScanner scanner = isRendered(boardImage) ? renderedScanner : diagramScanner;
        return scanner.scan(boardImage);
    }

    private boolean isRendered(Image img) {
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        PixelReader pr = img.getPixelReader();
        int mid = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int lum = SilhouetteUtils.luminance(pr.getArgb(x, y));
                if (lum >= MID_LOW && lum <= MID_HIGH) mid++;
            }
        return mid > (long) w * h * RENDERED_MIN;
    }
}
