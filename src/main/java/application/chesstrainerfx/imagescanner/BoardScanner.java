package application.chesstrainerfx.imagescanner;

import javafx.scene.image.Image;

/**
 * Contract voor bord-scanners: vervangt de implementatie zonder andere code te raken.
 * Fase 1 → TemplateBoardScanner (pure Java)
 * Fase 2 → OpenCVBoardScanner
 * Fase 3 → MLBoardScanner (ONNX/TFLite)
 */
@FunctionalInterface
public interface BoardScanner {
    ScanResult scan(Image boardImage);
}
