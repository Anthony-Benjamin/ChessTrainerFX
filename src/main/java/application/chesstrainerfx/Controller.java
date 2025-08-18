package application.chesstrainerfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;

public class Controller {


    private boolean setupMode = false;
    private PieceModel selectedSetupPiece;
    private BoardModel board;

    private enum SelectionStage {NONE, SOURCE_SELECTED}

    private SelectionStage stage = SelectionStage.NONE;
    private SquareView sourceView;
    private Position sourcePos;
    private PieceModel selectedPiece;
    private boolean whiteTurn = true;
    private SquareView lastmove;

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
        System.out.println("Clicked in Controller");

        if (setupMode) {
            placeOrRemovePiece(model, view);
            return;
        }

        if (stage == SelectionStage.NONE) {
            attemptSourceSelection(model, view);
        } else {
            attemptMove(board, view, model);

        }
    }

    private void placeOrRemovePiece(SquareModel model, SquareView view) {
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

    private void attemptSourceSelection(SquareModel model, SquareView view) {
        PieceModel piece = model.getPiece();
        if (piece == null || piece.getColor() != currentColor()) {

            return;
        }
        selectedPiece = piece;
        sourcePos = model.getPosition();
        sourceView = view;
        sourceView.setSeletedSource();
        stage = SelectionStage.SOURCE_SELECTED;
        if (lastmove != null) {
            lastmove.removeSelection();
        }
        System.out.println("Source chosen: " + sourcePos + " " + piece);
    }

    private void attemptMove(BoardModel board, SquareView view, SquareModel model) {
        Position targetPos = model.getPosition();
        view.setSelectedTarget();
        System.out.println("Target chosen: " + targetPos);

        boolean valid = MoveValidator.isValidMove(board, selectedPiece, sourcePos, targetPos);
        System.out.println("Move valid? " + valid);

        if (valid) {
            handlePawnSpecials(board, sourcePos, targetPos);
            board.movePiece(sourcePos, targetPos);
            handlePromotion(board, view, targetPos);
            toggleTurn();
            cleanupSelection();
            lastmove = view;
            //view.removeSelection();

        } else {
            view.removeSelection();
        }


    }

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
            board.getSquare(pos).setPiece(new PieceModel(PieceType.QUEEN, selectedPiece.getColor()));
            view.update();
        }
    }

    private void cleanupSelection() {
        if (sourceView != null) sourceView.removeSelection();
        // targetView highlighting is handled in move
        resetSelection();
    }

    private void resetSelection() {
        stage = SelectionStage.NONE;
        sourceView = null;
        selectedPiece = null;
        sourcePos = null;

    }

    private PieceColor currentColor() {
        return whiteTurn ? PieceColor.WHITE : PieceColor.BLACK;
    }

    private void toggleTurn() {
        whiteTurn = !whiteTurn;

    }


}





