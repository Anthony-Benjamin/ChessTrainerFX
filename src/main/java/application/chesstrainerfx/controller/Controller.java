package application.chesstrainerfx.controller;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.view.SquareView;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Alert;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.List;


public class Controller {

    private boolean setupMode = false;
    private PieceModel selectedSetupPiece;
    // Aantal gespeelde halfzetten vanaf de start-FEN van de oefening.
    // 0 = beginpositie, 1 = na eerste zet, 2 = na tweede zet, etc.
    private int currentPly = 0;
    // Verste punt dat de speler daadwerkelijk bereikt heeft.
    //  Hiermee voorkom je straks dat je verder navigeert dan hij al gespeeld heeft.
    private int maxReachedPly = 0;

    // ---------------- Move history voor navigatie ---------------- //

    private static class BoardSnapshot {
        String fen;                         // Bordstand in FEN
        boolean whiteTurn;                  // Wie is aan zet in deze stand
        Position lastDoubleStepPawn;        // Voor en passant (mag null zijn)

        BoardSnapshot(String fen, boolean whiteTurn, Position lastDoubleStepPawn) {
            this.fen = fen;
            this.whiteTurn = whiteTurn;
            this.lastDoubleStepPawn = lastDoubleStepPawn;
        }
    }

    private final java.util.List<BoardSnapshot> history = new java.util.ArrayList<>();
    private int historyIndex = -1; // index in history; -1 = nog geen snapshot

    public void setExerciseSession(ExerciseSession session) {

        this.exerciseSession = session;

        // Ply-teller resetten bij start van nieuwe oefening
        this.currentPly = 0;
        this.maxReachedPly = 0;
    }

    public String getExtractedLastMove() {
        return extractedLastMove;
    }

    private String extractedLastMove;

    private enum SelectionStage {
        NONE,
        SOURCE_SELECTED
    }

    public enum ExerciseStage {
        PLAYER_TO_MOVE,
        COMPUTER_TO_MOVE,
        NONE
    }

    ;

    private ExerciseStage exerciseStage = ExerciseStage.NONE;
    private SelectionStage stage = SelectionStage.NONE;
    private ExerciseSession exerciseSession;

    public void setExerciseStage(ExerciseStage exerciseStage) {
        this.exerciseStage = exerciseStage;
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

    public void setWhiteTurn(boolean whiteTurn) {
        this.whiteTurn = whiteTurn;
    }

    private boolean whiteTurn;


    public SquareView getLastViewMove() {
        return lastViewMove;
    }

    private SquareView lastViewMove;


    // telt het aantal zetten op het bord
    private int moveCounter = 0;

    public int getMoveCounter() {
        return moveCounter;
    }

    public void resetMoveCounter() {
        this.moveCounter = 0;
    }

    public void incrementMoveCounter() {
        this.moveCounter++;
    }
    // ---------------- Public API ---------------- //

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    public boolean isSetupMode() {
        return setupMode;
    }

    public void toggleSetupMode() {
        this.setupMode = !setupMode;
    }

    public void setSelectedPieceForSetup(PieceModel piece) {
        this.selectedSetupPiece = piece;
    }

    public void handleSquareClick(BoardModel board, SquareView view, SquareModel model) {


        if (setupMode) {
            handleSetupPlacement(model, view);
            return;
        }

        if (stage == SelectionStage.NONE) {
            handleSourceSelection(model, view);
        } else {
            handleMove(board, view, model);
        }
    }

    // ---------------- Setup Logic ---------------- //

    private void handleSetupPlacement(SquareModel model, SquareView view) {
        if (selectedSetupPiece == null) {
            model.setPiece(null);
        } else {
            model.setPiece(new PieceModel(
                    selectedSetupPiece.getType(),
                    selectedSetupPiece.getColor()
            ));
        }
        view.update();
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


        boolean valid = MoveValidator.isValidMove(board, selectedPiece, sourcePos, targetPos);


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
            if (isExerciseFinished()) {
                showMateMessage();
                return;
            }

            // 3) Tegenzet (als die er is)
            playOpponentMoveIfAny(board);

        } else {
            resetInvalidSelection(view);
        }
    }

    private void switchSourceSelection(SquareModel model, SquareView view) {
        System.out.println("Switching selected source...");

        if (sourceView != null) {
            sourceView.removeSelection();
        }

        view.setSelectedSource();
        sourceView = view;
        sourcePos = model.getPosition();
        selectedPiece = model.getPiece();
        stage = SelectionStage.SOURCE_SELECTED;
        System.out.println("New source: " + model.getPiece() + " at " + model.getPosition());
    }

    private void executeMove(BoardModel board, SquareView targetView, Position targetPos) {

        handlePawnSpecials(board, sourcePos, targetPos);
        extractedLastMove = extractLastMove(board, targetPos);

        board.movePiece(sourcePos, targetPos);
        handlePromotion(board, targetView, targetPos);
        //Beurt wisselen
        toggleTurn();
        board.notifyListenersTurnChanged(whiteTurn);

        //Nieuwe bordtoestand +beurt vastleggen in geschiedenis
        saveSnapshot(board);

        lastViewMove = targetView;
        cleanupSelection();

    }

    private String extractLastMove(BoardModel board, Position targetPos) {
        String s;
        if (board.getSquare(targetPos).getPiece() == null) {
            s = board.getSquare(sourcePos).getPiece().letterPiece() + CoordinateSystem.indexToCoordinate(new int[]{targetPos.row, targetPos.column});
        } else {
            s = board.getSquare(sourcePos).getPiece().letterPiece() + "x" + CoordinateSystem.indexToCoordinate(new int[]{targetPos.row, targetPos.column});
        }
        return s;
    }

    private void resetInvalidSelection(SquareView targetView) {
        System.out.println("Move invalid. Resetting selection.");
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
            dialog.setTitle("Promotie");
            dialog.setHeaderText("Kies een promotie voor de pion:");
            dialog.setContentText("Promoveer naar:");

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
        System.out.println("Whose turn is it? " + (whiteTurn ? "White" : "Black"));

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

        Move expected = exerciseSession.getExpectedMove();
        if (expected == null) return;

        Position from = expected.getFrom();
        Position to = expected.getTo();

        PieceModel piece = board.getSquare(from).getPiece();
        if (piece == null) return; // safety

        // check of de kleur klopt met de beurt
        if (piece.getColor() != currentTurnColor()) return;

        // kleine vertraging voordat de tegenzet wordt uitgevoerd
        PauseTransition pause = new PauseTransition(Duration.millis(1000)); // 300ms = 0.3s
        pause.setOnFinished(evt -> {
            // voer tegenzet uit
            board.movePiece(from, to);

            // turn wisselen (computer heeft gezet)
            toggleTurn();
            board.notifyListenersTurnChanged(whiteTurn);

            //snapshot van deze stand
            saveSnapshot(board);

            // ply vooruit
            exerciseSession.advancePly();
            if (isExerciseFinished()) {
                showMateMessage();
            }
        });
        pause.play();
    }

    private void showWrongMoveMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Onjuiste zet");
        alert.setHeaderText(null);
        alert.setContentText("Dit is niet de juiste zet. Probeer opnieuw.");
        alert.showAndWait();
    }

    private void showMateMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Oefening klaar");
        alert.setHeaderText(null);
        alert.setContentText("Mat! 🎉");
        alert.showAndWait();
    }

    private boolean isExerciseFinished() {
        return exerciseSession != null && !exerciseSession.hasNext();
    }


    public int getCurrentPly() {
        return currentPly;
    }
    // Sla de huidige bordstand + beurt op als snapshot
    private void saveSnapshot(BoardModel board) {
        // Als we eerder een undo/redo-achtig iets doen: snij "toekomst" af
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }

        String fen = board.exportToFEN(whiteTurn); // alleen de stukken; metadata komt later wel
        Position lastDouble = board.getLastDoubleStepPawnPosition();

        BoardSnapshot snap = new BoardSnapshot(fen, whiteTurn, lastDouble);
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
        if (historyIndex <= 0) {
            System.out.println("Geen zet om terug te draaien.");
            return;
        }

        historyIndex--;
        BoardSnapshot snap = history.get(historyIndex);

        System.out.println("Undo to index " + historyIndex + " / size " + history.size());

        board.initializeFromFEN(snap.fen);
        board.setLastDoubleStepPawnPosition(snap.lastDoubleStepPawn);

        this.whiteTurn = snap.whiteTurn;

        if (exerciseSession != null) {
            exerciseSession.rewindPly();
            System.out.println("Exercise ply index after undo = " + exerciseSession.getIndex());
        }
    }
}
