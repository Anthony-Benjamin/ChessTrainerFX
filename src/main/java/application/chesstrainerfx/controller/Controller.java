package application.chesstrainerfx.controller;

import application.chesstrainerfx.model.BoardModel;
import application.chesstrainerfx.model.SquareModel;
import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.view.SquareView;
import javafx.scene.control.ChoiceDialog;

import java.util.List;

public class Controller {

    private boolean setupMode = false;
    private PieceModel selectedSetupPiece;
    private BoardModel board;

    public String getExtractedLastMove() {
        return extractedLastMove;
    }

    private String extractedLastMove;

    private enum SelectionStage {
        NONE,
        SOURCE_SELECTED
    }
    public enum ExerciseStage{
        PLAYER_TO_MOVE,
        COMPUTER_TO_MOVE,
        NONE};

    private ExerciseStage exerciseStage = ExerciseStage.NONE;
    private SelectionStage stage = SelectionStage.NONE;

    public void setExerciseStage(ExerciseStage exerciseStage) {

        this.exerciseStage = exerciseStage;

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
    public String lastmove;

    public SquareView getLastViewMove() {
        return lastViewMove;
    }

    private SquareView lastViewMove;

    // changes 21-01-2026
    private ExerciseSession exerciseSession;
    // end of changes 21-01-2026

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
            setExerciseStage(ExerciseStage.COMPUTER_TO_MOVE);
            executeMove(board, view, targetPos);


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
//        System.out.println("is Exercise null? " + exerciseSession);
        if (exerciseSession != null) {
            Move userMove = new Move(sourcePos, targetPos);

            if (!exerciseSession.isCorrectMove(userMove)) {
                // voorlopig: alleen print / eenvoudige feedback
                System.out.println("Incorrect move for this exercise");
                resetInvalidSelection(targetView);
                return;
            }
        }
        handlePawnSpecials(board, sourcePos, targetPos);
        extractedLastMove = extractLastMove(board,targetPos);

        board.movePiece(sourcePos, targetPos);
        handlePromotion(board, targetView, targetPos);

        toggleTurn();

        lastViewMove = targetView;

        board.notifyListenersTurnChanged(whiteTurn);
        cleanupSelection();

//        lastMove = targetView;
//        System.out.println("lastMove: " + lastMove);
    }

    private String extractLastMove(BoardModel board, Position targetPos) {
        String s;
        if (board.getSquare(targetPos).getPiece() ==  null){
            s = board.getSquare(sourcePos).getPiece().letterPiece()  +  CoordinateSystem.indexToCoordinate(new int[]{targetPos.row, targetPos.column});
        }
        else{
            s = board.getSquare(sourcePos).getPiece().letterPiece()  + "x" +  CoordinateSystem.indexToCoordinate(new int[]{targetPos.row, targetPos.column});
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
        System.out.println("Whose turn is it? " + whiteTurn);
    }

    private void notifyTurnChanged() {
        if( board != null){
            board.notifyListenersTurnChanged(whiteTurn);
        }
    }
    public void syncTurnFromFEN(String fen) {
        try {
            String[] parts = fen.split("\\s+");
            if (parts.length >= 2) {
                whiteTurn = parts[1].equals("w");
            }
        } catch (Exception ignored) {}
    }

    public void setExerciseSession(ExerciseSession session){
        this.exerciseSession = session;
    }
}
