package application.chesstrainerfx;

import javafx.scene.control.ChoiceDialog;

import java.util.List;

public class Controller400 {

    private enum SelectionStage { NONE, SOURCE_SELECTED }

    private boolean whiteTurn = true;

    private SelectionStage stage = SelectionStage.NONE;

    private ChapterSquareView sourceView;
    private Position sourcePos;
    private PieceModel selectedPiece;

    private ChapterSquareView lastMoveView; // highlight van laatste doel

    // ---------- Public API ----------

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    public void setWhiteTurn(boolean whiteTurn) {
        this.whiteTurn = whiteTurn;
    }

    /** Klikafhandeling voor 400-UI: gebruikt ChapterSquareView in de signatuur. */
    public void handleSquareClick(BoardModel board, ChapterSquareView view, SquareModel model) {
        if (stage == SelectionStage.NONE) {
            handleSourceSelection(model, view);
        } else {
            handleMove(board, view, model);
        }
    }

    // ---------- Selectie ----------

    private void handleSourceSelection(SquareModel model, ChapterSquareView view) {
        PieceModel piece = model.getPiece();
        if (piece == null || piece.getColor() != currentTurnColor()) {
            return; // geen stuk of verkeerde kleur → negeren
        }

        // Vorige source deselecteren
        if (sourceView != null) sourceView.removeSelection();

        selectedPiece = piece;
        sourcePos = model.getPosition();
        sourceView = view;

        // Highlight
        sourceView.setSelectedSource();

        // Laatste move highlight verwijderen
        if (lastMoveView != null) lastMoveView.removeSelection();

        stage = SelectionStage.SOURCE_SELECTED;
    }

    // ---------- Verplaatsing ----------

    private void handleMove(BoardModel board, ChapterSquareView view, SquareModel model) {
        PieceColor targetColor = (model.getPiece() != null) ? model.getPiece().getColor() : null;

        // Klik op stuk eigen kleur → switch van bron
        if (targetColor == currentTurnColor()) {
            switchSourceSelection(model, view);
            return;
        }

        Position targetPos = model.getPosition();
        view.setSelectedTarget();

        boolean valid = MoveValidator.isValidMove(board, selectedPiece, sourcePos, targetPos);
        if (!valid) {
            resetInvalidSelection(view);
            return;
        }

        // En passant / double-step tracking
        handlePawnSpecials(board, sourcePos, targetPos);

        // Uitvoeren
        board.movePiece(sourcePos, targetPos);

        // Promotie (met keuze)
        handlePromotion(board, view, targetPos);

        // Beurt wisselen & UI opschonen
        toggleTurn();
        cleanupSelection();

        // Onthoud doelvlak als "laatste zet"
        lastMoveView = view;
    }

    private void switchSourceSelection(SquareModel model, ChapterSquareView view) {
        if (sourceView != null) sourceView.removeSelection();

        view.setSelectedSource();
        sourceView = view;
        sourcePos = model.getPosition();
        selectedPiece = model.getPiece();

        stage = SelectionStage.SOURCE_SELECTED;
    }

    private void resetInvalidSelection(ChapterSquareView targetView) {
        if (sourceView != null) sourceView.removeSelection();
        targetView.removeSelection();
        resetSelection();
    }

    // ---------- Pion-specials & promotie ----------

    private void handlePawnSpecials(BoardModel board, Position from, Position to) {
        if (selectedPiece.getType() != PieceType.PAWN) {
            board.setLastDoubleStepPawnPosition(null);
            return;
        }

        int dx = to.getColumn() - from.getColumn();
        int dy = to.getRow() - from.getRow();
        int dir = (selectedPiece.getColor() == PieceColor.WHITE) ? -1 : 1;

        // En passant: diagonaal bewegen naar leeg veld → sla pion achter het doelveld
        if (Math.abs(dx) == 1 && dy == dir && board.getSquare(to).getPiece() == null) {
            Position capturePos = new Position(from.getRow(), to.getColumn());
            board.getSquare(capturePos).setPiece(null);
        }

        // Double step bijhouden
        if (Math.abs(dy) == 2) {
            board.setLastDoubleStepPawnPosition(to);
        } else {
            board.setLastDoubleStepPawnPosition(null);
        }
    }

    private void handlePromotion(BoardModel board, ChapterSquareView view, Position pos) {
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

    // ---------- Turn & reset ----------

    private PieceColor currentTurnColor() {
        return whiteTurn ? PieceColor.WHITE : PieceColor.BLACK;
    }

    private void toggleTurn() {
        whiteTurn = !whiteTurn;
    }

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
}
