# ChessTrainerFX — Project Context

## Goal
Release-ready JavaFX chess trainer with generic PGN input.
Branch: ClaudeUpdates2

## Target architecture
- Three fully independent modules: MatingPatterns / Tactics / Puzzles
- PGN paths configurable via `config.properties` (per module)
- Puzzles module has user-managed sub-categories (persisted as JSON)
- `BoardEditorDemo` fully integrated into Puzzles as `PositionEditor`, demo class removed
- PGN files added via `FileChooser` per module / sub-category

## Naming convention (PositionEditor — fixed, do not deviate)
- Class:      `PositionEditorController`
- View:       `position-editor.fxml`
- Menu item:  "Position Editor"

## PGN folder structure
```
<root-pgn-path>/
├── mating-patterns/   *.pgn
├── tactics/           *.pgn
└── puzzles/
    ├── <sub-category>/  *.pgn   (user-defined, e.g. "500-chess-in-one")
    └── ...
```
Folders are created automatically if they do not exist.

## Release criteria — verify all before reporting done
- Zero `System.out` or logger calls anywhere in the codebase
- Zero dead code, commented-out blocks, or unused imports
- All existing tests green
- Application starts without hardcoded paths
- Folder structure auto-created on first run
- No new dependencies added without a note in the commit message

## Constraints
- Java version: do not change
- Build tool: do not switch
- UI look-and-feel: preserve existing style (colors, fonts, layout)
- Commit per logical subtask, messages in English
