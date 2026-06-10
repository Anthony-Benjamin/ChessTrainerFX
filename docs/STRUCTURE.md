# ChessTrainerFX — Codebase Structure (after cleanup)

Status: post Phase-1 cleanup (commit `fc5f14d`), before module refactor.

## Build & runtime

- Maven project (`pom.xml`), Java 23, JavaFX 17.0.6 (`javafx-controls`, `javafx-fxml`), JUnit 5 (no tests exist yet).
- JPMS module `application.chesstrainerfx` (`module-info.java`).
- Entry point: `application.chesstrainerfx.Main` (configured in javafx-maven-plugin, run via `mvn javafx:run`).

## Packages

```
application.chesstrainerfx
├── Main.java                 — JavaFX Application: home screen met 3 tegels
│                               (Mating Patterns / Tactics / Puzzles); alleen
│                               Mating Patterns is actief. Hardcoded lijst van
│                               36 classpath-PGN-paden (/pgn/mating/chapters/…)
├── controller/
│   ├── Controller.java       — bordinteractie per oefening: selectie, zet-
│   │                           validatie, promotie, en passant, beurtwissel,
│   │                           oefensessie-check, undo/redo via FEN-snapshots
│   └── ChapterPresenter.java — presenter voor ChapterWindow: bouwt BoardModel
│                               + Controller + ExerciseSession per oefening,
│                               levert move-regels en hinttekst
├── model/
│   ├── BoardModel.java       — 8×8 SquareModel-grid, FEN import/export,
│   │                           movePiece, en-passant-tracking, listeners
│   └── SquareModel.java      — positie + (optioneel) stuk
├── view/
│   ├── BoardChangeListener.java — interface (onBoardUpdated/onTurnChanged/onCheck)
│   ├── BoardView.java        — speelbord met frame en rank/file-labels + turn-label
│   ├── SquareView.java       — één veld: kleuren, selectie-overlay, stuk-icoon,
│   │                           drag-and-drop (voor de editor)
│   ├── MatePatternsView.java — tegel-overzicht van hoofdstukken
│   └── ChapterWindow.java    — hoofdstukscherm: theorie-header, oefening-tegels
│                               (LIST) en bord + moves/hint/undo/redo (BOARD)
└── utils/
    ├── BoardEditor.java      — bordweergave voor de editor (flipbaar)
    ├── BoardEditorDemo.java  — standalone editor-Application: stukken plaatsen,
    │                           FEN exporteren, oefening als PGN opslaan
    │                           → wordt in fase 4 PositionEditor in Puzzles
    ├── PieceSelectorPane.java— sleepbare stukkenrij voor de editor
    ├── DragContext.java      — gedeelde drag-state (gesleept stuk)
    ├── MoveValidator.java    — pseudo-legale + legale zetvalidatie, schaak/mat
    ├── ExerciseSession.java  — verwachte zettenreeks + voortgang (ply-index)
    ├── ExerciseSessionBuilder.java — SAN → Move-resolver op een tijdelijk bord
    ├── ChessMoveParser.java  — movetext → wit/zwart SAN-lijsten
    ├── ParsedMoves.java      — datadrager voor ChessMoveParser
    ├── PgnUtils.java         — movetext schonen (varianten, comments, NAGs)
    ├── CoordinateSystem.java — "e4" ↔ [row, col]
    ├── Move.java / Position.java / PieceModel.java / PieceColor / PieceType

application.pgnreader
├── io/PGNReader.java         — leest PGN van de classpath, splitst op [Event],
│                               haalt White-tag (titel), FEN, movetext, comments
└── model/Chapter.java, Exercise.java — datadragers
```

## Resources

- `/images/…` — stukken, achtergronden, iconen
- `/pgn/mating/`, `/pgn/tactics/`, `/pgn/Puzzles/` — gebundelde PGN's (classpath)
- `splash.css`, `listview-style.css`, `css/alert.css`

## Flow (huidig)

1. `Main.start` → home met 3 tegels; alleen "Mating Patterns" werkt.
2. `buildMatingPatterns` leest 36 hardcoded classpath-PGN's via `PGNReader` → `MatePatternsView`.
3. Klik op hoofdstuk → `ChapterWindow` (+ `ChapterPresenter`).
4. Klik op oefening → presenter bouwt `BoardModel`/`Controller`/`ExerciseSession` → bordweergave; speler speelt de oplossing, computer antwoordt, mat → melding.

## Bekende beperkingen t.o.v. de release-doelen

- PGN-paden zijn hardcoded en komen uit de classpath i.p.v. een configureerbare map (`config.properties`) — fase 3.
- Tactics en Puzzles zijn niet geïmplementeerd ("Nog niet beschikbaar") — fase 3.
- `BoardEditorDemo` is een losse Application, nog niet geïntegreerd als PositionEditor — fase 4.
- Geen PGN-import via FileChooser — fase 5.
- Geen sub-categoriebeheer voor Puzzles — fase 6.
- Er zijn geen unit tests.
