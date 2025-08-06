package application.chesstrainerfx;

public class Controller {

    private int counter;
    private Position source, target;
    private SquareView from, to;
    private PieceModel piece;

    public boolean isSetupMode() {
        return setupMode;
    }

    public void toggleSetupMode() {
        this.setupMode = !setupMode;
        System.out.println("SetupMode " + setupMode);
    }

    private boolean setupMode = false;

    public void setSelectedPieceForSetup(PieceModel selectedPieceForSetup) {
        this.selectedPieceForSetup = selectedPieceForSetup;
    }

    private PieceModel selectedPieceForSetup = null; // null = verwijderen


    public void handleSquareClick(BoardModel board, SquareView view, SquareModel model) {
        System.out.println("Clicked! in Controller");
        if (setupMode) {
            if (selectedPieceForSetup == null) {
                model.setPiece(null); // verwijderen
            } else {
                model.setPiece(new PieceModel(
                        selectedPieceForSetup.getType(),
                        selectedPieceForSetup.getColor()
                ));
            }
            view.update(); // herteken GUI
            return;
        }

        if (counter == 0) {
            from = view;
            from.setSeletedSource();
            source = model.getPosition();
            System.out.println("Source: " + source + model.getPiece());
            piece = model.getPiece();
            counter = 1;
        } else {
            // Tweede klik
            to = view;
            to.setSelectedTarget();
            target = model.getPosition();
            System.out.println("Target: " + target);

            boolean result = MoveValidator.isValidMove(board, piece, source, target);


            System.out.println(result);
            if (result) {
                if (piece.getType() == PieceType.PAWN) {
                    int dx = target.getColumn() - source.getColumn();
                    int dy = target.getRow() - source.getRow();
                    int direction = piece.getColor() == PieceColor.WHITE ? -1 : 1;

                    //En passant
                    if (Math.abs(dx) == 1 && dy == direction && board.getSquare(target).getPiece() == null) {
                        Position captured = new Position(source.getRow(), target.getColumn());
                        board.getSquare(captured).setPiece(null);
                        // GUI update
                        view.update();

                    }
                    // Dubbele stap
                    if (Math.abs(dy) == 2) {
                        board.setLastDoubleStepPawnPosition(target);
                    } else {
                        board.setLastDoubleStepPawnPosition(null);
                    }

                }
                board.movePiece(source, target);

                // Promotie
                if (piece.getType() == PieceType.PAWN && (target.getRow() == 0 || target.getRow() == 7)) {
                    PieceModel promoted = new PieceModel(PieceType.QUEEN, piece.getColor());
                    board.getSquare(target).setPiece(promoted);
                    view.update();
                }
            }


            if (from != null && to != null) {
                from.removeSelection();
                to.removeSelection();
            }

            // reset
            source = null;
            target = null;
            from = null;
            to = null;
            counter = 0;
        }


    }
}

