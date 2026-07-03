package application.chesstrainerfx.imagescanner;

import application.chesstrainerfx.model.BoardModel;
import javafx.scene.image.Image;

/**
 * Orchestreert de image-to-FEN flow zonder JavaFX UI-imports.
 * Houdt één BoardModel bij dat na elke scan wordt bijgewerkt en daarna
 * via de BoardEditor door de gebruiker gecorrigeerd kan worden.
 */
public class ImageScannerPresenter {

    private final AutoBoardScanner scanner = new AutoBoardScanner();
    private final BoardModel resultModel = new BoardModel();
    private ScanResult lastResult;

    /** Stelt in of het gescande bord vanuit zwart is weergegeven (rank 1 boven, h-lijn links). */
    public void setBlackPerspective(boolean blackPerspective) {
        scanner.setBlackPerspective(blackPerspective);
    }

    /**
     * Scant het meegegeven (al gecropte) bord-beeld en laadt het resultaat
     * in het resultaat-model via een FEN round-trip.
     */
    public ScanResult scan(Image boardImage) {
        lastResult = scanner.scan(boardImage);
        // FEN round-trip hergebruikt de bestaande parser in BoardModel
        resultModel.initializeFromFEN(lastResult.toFEN(true));
        return lastResult;
    }

    /** Model dat aan de BoardEditor gekoppeld wordt; inhoud wijzigt na elke scan. */
    public BoardModel getResultModel() {
        return resultModel;
    }

    public ScanResult getLastResult() {
        return lastResult;
    }

    /** Exporteert de huidige (eventueel gecorrigeerde) bord-staat als FEN-string. */
    public String exportFEN(boolean whiteToMove) {
        return resultModel.exportToFEN(whiteToMove);
    }
}
