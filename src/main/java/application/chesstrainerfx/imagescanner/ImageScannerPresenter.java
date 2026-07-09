package application.chesstrainerfx.imagescanner;

import javafx.scene.image.Image;

/**
 * Orchestreert de image-to-FEN flow zonder JavaFX UI-imports:
 * configureert het perspectief en delegeert het scannen aan de AutoBoardScanner.
 */
public class ImageScannerPresenter {

    private final AutoBoardScanner scanner = new AutoBoardScanner();

    /** Stelt in of het gescande bord vanuit zwart is weergegeven (rank 1 boven, h-lijn links). */
    public void setBlackPerspective(boolean blackPerspective) {
        scanner.setBlackPerspective(blackPerspective);
    }

    /** Scant het meegegeven (al gecropte) bord-beeld. */
    public ScanResult scan(Image boardImage) {
        return scanner.scan(boardImage);
    }
}
