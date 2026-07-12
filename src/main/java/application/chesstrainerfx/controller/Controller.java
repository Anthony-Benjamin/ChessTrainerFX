package application.chesstrainerfx.controller;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.view.SquareView;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Alert;
import javafx.animation.PauseTransition;
import javafx.scene.control.DialogPane;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;


public class Controller {

    private boolean editorMode = false;

    // ---------------- Move history voor navigatie ---------------- //

    private static class BoardSnapshot {
        String fen;                         // Bordstand in FEN
        boolean whiteTurn;                  // Wie is aan zet in deze stand
        Position lastDoubleStepPawn;        // Voor en passant (mag null zijn)
        Integer exerciseNodeId;             // Actieve variant-node in de oefensessie

        BoardSnapshot(String fen, boolean whiteTurn, Position lastDoubleStepPawn, Integer exerciseNodeId) {
            this.fen = fen;
            this.whiteTurn = whiteTurn;
            this.lastDoubleStepPawn = lastDoubleStepPawn;
            this.exerciseNodeId = exerciseNodeId;
        }
    }

    private final java.util.List<BoardSnapshot> history = new java.util.ArrayList<>();
    private int historyIndex = -1; // index in history; -1 = nog geen snapshot

    public void setExerciseSession(ExerciseSession session) {
        this.exerciseSession = session;
    }

    private enum SelectionStage {
        NONE,
        SOURCE_SELECTED
    }

    public enum ExerciseStage {
        PLAYER_TO_MOVE,
        COMPUTER_TO_MOVE,
        NONE
    }

    private ExerciseStage exerciseStage = ExerciseStage.NONE;
    private SelectionStage stage = SelectionStage.NONE;
    private ExerciseSession exerciseSession;
    private Consumer<Boolean> onExerciseSolved;
    private Runnable onExerciseUnsolved;

    public void setExerciseStage(ExerciseStage exerciseStage) {
        this.exerciseStage = exerciseStage;
    }

    /**
     * Handler die de eindmelding inline naast het bord toont i.p.v. in een dialoog.
     * Het argument geeft aan of de eindstand schaakmat is (true) of dat de
     * oefening zonder mat is afgerond (false).
     */
    public void setOnExerciseSolved(Consumer<Boolean> onExerciseSolved) {
        this.onExerciseSolved = onExerciseSolved;
    }

    /** Handler die een eerder getoonde opgelost/mat-melding wist. */
    public void setOnExerciseUnsolved(Runnable onExerciseUnsolved) {
        this.onExerciseUnsolved = onExerciseUnsolved;
    }

    public String getExpectedSan() {
        if (exerciseSession == null) {
            return null;
        }
        return exerciseSession.getExpectedSan();
    }

    public ExerciseStage getExerciseStage() {
        return exerciseStage;
    }


    private SquareView sourceView;
    private Position sourcePos;
    private PieceModel selectedPiece;
    private boolean whiteTurn;
    private SquareView lastViewMove;

    // ---------------- Public API ---------------- //

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    /** In editor-mode verloopt slepen via vrije plaatsing i.p.v. de zet-/selectie-flow. */
    public boolean isEditorMode() {
        return editorMode;
    }

    public void setEditorMode(boolean editorMode) {
        this.editorMode = editorMode;
    }

    public void handleSquareClick(BoardModel board, SquareView view, SquareModel model) {
        if (stage == SelectionStage.NONE) {
            handleSourceSelection(model, view);
        } else {
            handleMove(board, view, model);
        }
    }

    // ---------------- Selection Logic ---------------- //

    private void handleSourceSelection(SquareModel model, SquareView view) {
        PieceModel piece = model.getPiece();
        if (piece == null || piece.getColor() != currentTurnColor()) {
            return;
        }

        selectedPiece = piece;
        sourcePos = model.getPosition();
        sourceView = view;

        sourceView.setSelectedSource();
        if (lastViewMove != null) {
            lastViewMove.removeSelection();
        }

        stage = SelectionStage.SOURCE_SELECTED;

    }

    // ---------------- Move Logic ---------------- //

    private void handleMove(BoardModel board, SquareView view, SquareModel model) {
        PieceColor targetColor = (model.getPiece() != null) ? model.getPiece().getColor() : null;

        if (targetColor == currentTurnColor()) {
            switchSourceSelection(model, view);
            return;
        }

        Position targetPos = model.getPosition();
        view.setSelectedTarget();


        boolean valid = MoveValidator.isLegalMove(board, selectedPiece, sourcePos, targetPos);


        if (valid) {
            Move userMove = new Move(sourcePos, targetPos);

            // 1) Controleer oefenzet
            if (exerciseSession != null && !exerciseSession.isCorrectMove(userMove)) {
                resetInvalidSelection(view);
                showWrongMoveMessage();
                return;
            }

            // 2) Speler zet uitvoeren
            executeMove(board, view, targetPos);

            // ply vooruit (speler heeft juiste zet gedaan)
            if (exerciseSession != null) {
                exerciseSession.advancePly();
            }
            saveSnapshot(board);

            // Schaak / mat detecteren op het bord
            if (checkGameState(board)) {
                return;
            }
            if (isExerciseFinished()) {
                showExerciseFinishedMessage(false);
                return;
            }

            // 3) Tegenzet (als die er is)
            playOpponentMoveIfAny(board);

        } else {
            resetInvalidSelection(view);
        }
    }

    private void switchSourceSelection(SquareModel model, SquareView view) {
        if (sourceView != null) {
            sourceView.removeSelection();
        }

        view.setSelectedSource();
        sourceView = view;
        sourcePos = model.getPosition();
        selectedPiece = model.getPiece();
        stage = SelectionStage.SOURCE_SELECTED;
    }

    private void executeMove(BoardModel board, SquareView targetView, Position targetPos) {
        handlePawnSpecials(board, sourcePos, targetPos);
        board.movePiece(sourcePos, targetPos);
        handlePromotion(board, targetView, targetPos);
        //Beurt wisselen
        toggleTurn();
        board.notifyListenersTurnChanged(whiteTurn);

        lastViewMove = targetView;
        cleanupSelection();

    }

    private void resetInvalidSelection(SquareView targetView) {
        if (sourceView != null) sourceView.removeSelection();
        targetView.removeSelection();
        resetSelection();
    }

    // ---------------- Pawn & Promotion Logic ---------------- //

    private void handlePawnSpecials(BoardModel board, Position from, Position to) {
        if (selectedPiece.getType() != PieceType.PAWN) return;

        int dx = to.getColumn() - from.getColumn();
        int dy = to.getRow() - from.getRow();
        int dir = (selectedPiece.getColor() == PieceColor.WHITE) ? -1 : 1;

        // En passant
        if (Math.abs(dx) == 1 && dy == dir && board.getSquare(to).getPiece() == null) {
            Position capturePos = new Position(from.getRow(), to.getColumn());
            board.getSquare(capturePos).setPiece(null);
        }

        // Double step tracking
        if (Math.abs(dy) == 2) {
            board.setLastDoubleStepPawnPosition(to);
        } else {
            board.setLastDoubleStepPawnPosition(null);
        }
    }

    private void handlePromotion(BoardModel board, SquareView view, Position pos) {
        if (selectedPiece.getType() == PieceType.PAWN && (pos.getRow() == 0 || pos.getRow() == 7)) {
            List<PieceType> options = List.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT);

            ChoiceDialog<PieceType> dialog = new ChoiceDialog<>(PieceType.QUEEN, options);
            dialog.setTitle("Promotion");
            dialog.setHeaderText("Choose a promotion for the pawn:");
            dialog.setContentText("Promote to:");

            PieceType choice = dialog.showAndWait().orElse(PieceType.QUEEN);

            board.getSquare(pos).setPiece(new PieceModel(choice, selectedPiece.getColor()));
            view.update();
        }
    }

    // ---------------- Selection Reset ---------------- //

    private void cleanupSelection() {
        if (sourceView != null) sourceView.removeSelection();
        resetSelection();
    }

    private void resetSelection() {
        stage = SelectionStage.NONE;
        sourceView = null;
        selectedPiece = null;
        sourcePos = null;
    }

    // ---------------- Turn Handling ---------------- //

    private PieceColor currentTurnColor() {
        return whiteTurn ? PieceColor.WHITE : PieceColor.BLACK;
    }

    public void toggleTurn() {
        whiteTurn = !whiteTurn;
    }

    public void syncTurnFromFEN(String fen) {
        try {
            String[] parts = fen.split("\\s+");
            if (parts.length >= 2) {
                whiteTurn = parts[1].equals("w");
            }
        } catch (Exception ignored) {
        }
    }

    private void playOpponentMoveIfAny(BoardModel board) {
        if (exerciseSession == null) return;

        List<ExerciseSession.Node> candidates = exerciseSession.getCandidateNodes();
        if (candidates.isEmpty()) return;

        ExerciseSession.Node selectedReply = selectOpponentReply(candidates);
        if (selectedReply == null) return;

        Move expected = selectedReply.getMove();

        Position from = expected.getFrom();
        Position to = expected.getTo();

        PieceModel piece = board.getSquare(from).getPiece();
        if (piece == null) return; // safety

        // check of de kleur klopt met de beurt
        if (piece.getColor() != currentTurnColor()) return;

        // kleine vertraging voordat de tegenzet wordt uitgevoerd
        PauseTransition pause = new PauseTransition(Duration.millis(1000));
        pause.setOnFinished(evt -> {
            // voer tegenzet uit (incl. rokade/en passant/promotie)
            ExerciseMoveExecutor.apply(board, expected, selectedReply.getSan(), piece.getColor());

            // turn wisselen (computer heeft gezet)
            toggleTurn();
            board.notifyListenersTurnChanged(whiteTurn);

            // ply vooruit
            exerciseSession.advanceTo(selectedReply);

            //snapshot van deze stand
            saveSnapshot(board);

            // Schaak / mat detecteren na de tegenzet
            if (checkGameState(board)) {
                return;
            }
            if (isExerciseFinished()) {
                showExerciseFinishedMessage(false);
            }
        });
        pause.play();
    }

    /**
     * Speelt de volgende zet uit de hoofdvariant automatisch (naspelen van een
     * partij zonder te raden). Eén ply per aanroep; no-op als de oefening
     * klaar is of de bordstand niet meer bij de sessie past.
     */
    public void playNextExerciseMove(BoardModel board) {
        if (exerciseSession == null) return;

        List<ExerciseSession.Node> candidates = exerciseSession.getCandidateNodes();
        if (candidates.isEmpty()) return;

        ExerciseSession.Node next = candidates.get(0); // hoofdvariant
        Move move = next.getMove();

        PieceModel piece = board.getSquare(move.getFrom()).getPiece();
        if (piece == null || piece.getColor() != currentTurnColor()) return;

        cleanupSelection();
        ExerciseMoveExecutor.apply(board, move, next.getSan(), piece.getColor());
        toggleTurn();
        board.notifyListenersTurnChanged(whiteTurn);

        exerciseSession.advanceTo(next);
        saveSnapshot(board);

        if (checkGameState(board)) {
            return;
        }
        if (isExerciseFinished()) {
            showExerciseFinishedMessage(false);
        }
    }

    private ExerciseSession.Node selectOpponentReply(List<ExerciseSession.Node> candidates) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        ChoiceDialog<ExerciseSession.Node> dialog = new ChoiceDialog<>(candidates.get(0), candidates);
        dialog.setTitle("Choose computer move");
        dialog.setHeaderText("Choose the variation the computer plays:");
        dialog.setContentText("Computer move:");

        return dialog.showAndWait().orElse(candidates.get(0));
    }

    private void showWrongMoveMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/css/alert.css").toExternalForm()
        );

        alert.setTitle("Wrong move");
        alert.setHeaderText(null);
        alert.setContentText("That is not the right move. Try again.");
        alert.showAndWait();
    }

    private void showExerciseFinishedMessage(boolean mate) {
        // Bij voorkeur inline naast het bord; val terug op een dialoog als er geen handler is.
        if (onExerciseSolved != null) {
            onExerciseSolved.accept(mate);
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exercise finished");
        alert.setHeaderText(null);
        alert.setContentText(mate ? "Checkmate! 🎉" : "Solved! 🎉");

        alert.showAndWait();
    }

    private boolean isExerciseFinished() {
        return exerciseSession != null && !exerciseSession.hasNext();
    }

    /**
     * Detecteert schaak/mat voor de partij die nu aan zet is, meldt de
     * schaakstatus aan de listeners en toont bij mat een melding.
     *
     * @return true als de partij door schaakmat is afgelopen.
     */
    private boolean checkGameState(BoardModel board) {
        PieceColor sideToMove = currentTurnColor();
        boolean inCheck = MoveValidator.isKingInCheck(board, sideToMove);
        boolean mate = inCheck && !MoveValidator.hasAnyLegalMove(board, sideToMove);

        board.notifyCheck(inCheck && !mate);

        if (mate) {
            showExerciseFinishedMessage(true);
            return true;
        }
        return false;
    }

    /** Is de partij die nu aan zet is schaakmat gezet? */
    private boolean isMate(BoardModel board) {
        PieceColor sideToMove = currentTurnColor();
        return MoveValidator.isKingInCheck(board, sideToMove)
                && !MoveValidator.hasAnyLegalMove(board, sideToMove);
    }


    // Sla de huidige bordstand + beurt op als snapshot
    private void saveSnapshot(BoardModel board) {
        // Als we eerder een undo/redo-achtig iets doen: snij "toekomst" af
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }

        String fen = board.exportToFEN(whiteTurn);
        Position lastDouble = board.getLastDoubleStepPawnPosition();
        Integer exerciseNodeId = exerciseSession == null ? null : exerciseSession.getCurrentNodeId();

        BoardSnapshot snap = new BoardSnapshot(fen, whiteTurn, lastDouble, exerciseNodeId);
        history.add(snap);
        historyIndex = history.size() - 1;
    }
    // Aanroepen als je een nieuw board / nieuwe oefening start.
    public void startNewHistory(BoardModel board) {
        history.clear();
        historyIndex = -1;
        saveSnapshot(board); // beginstand (ply 0)
    }
    public void undoLastMove(BoardModel board) {
        // We willen 1 "volledige zet" terug: max 2 plies
        if (historyIndex <= 0) {
            return;
        }

        // Hoeveel stappen kúnnen we terug? (1 of 2)
        int delta = Math.min(2, historyIndex);

        // Geschiedenis-index terugzetten
        historyIndex -= delta;
        BoardSnapshot snap = history.get(historyIndex);

        // 1) Bord + turn terugzetten
        board.initializeFromFEN(snap.fen);
        board.setLastDoubleStepPawnPosition(snap.lastDoubleStepPawn);
        this.whiteTurn = snap.whiteTurn;

        // 2) Oefensessie ook delta plies terug
        if (exerciseSession != null && snap.exerciseNodeId != null) {
            exerciseSession.setCurrentNodeId(snap.exerciseNodeId);
        }

        updateSolvedStateAfterHistoryRestore(board);
    }
    public void redoMove(BoardModel board) {
        if (historyIndex >= history.size() - 1) {
            return;
        }

        // Maximaal 2 plies vooruit, maar niet voorbij het einde
        int maxForward = history.size() - 1 - historyIndex;
        int delta = Math.min(2, maxForward);

        historyIndex += delta;
        BoardSnapshot snap = history.get(historyIndex);

        // 1) Bord + turn herstellen
        board.initializeFromFEN(snap.fen);
        board.setLastDoubleStepPawnPosition(snap.lastDoubleStepPawn);
        this.whiteTurn = snap.whiteTurn;

        // 2) Oefensessie delta plies vooruit
        if (exerciseSession != null && snap.exerciseNodeId != null) {
            exerciseSession.setCurrentNodeId(snap.exerciseNodeId);
        }

        updateSolvedStateAfterHistoryRestore(board);
    }

    private void updateSolvedStateAfterHistoryRestore(BoardModel board) {
        if (isExerciseFinished()) {
            if (onExerciseSolved != null) {
                onExerciseSolved.accept(isMate(board));
            }
        } else if (onExerciseUnsolved != null) {
            onExerciseUnsolved.run();
        }
    }
}
