package application.chesstrainerfx;

public class Controller {

    private int counter;
    private Position source, target;
    private SquareView from, to;
    private PieceModel piece;

    public void handleSquareClick(BoardModel board, SquareView view, SquareModel model) {
        System.out.println("Clicked! in Controller");

        if (counter == 0) {
            from = view;
            from.setSeletedSource();
            source = model.getPosition();
            System.out.println("Source: " + source + model.getPiece());
            piece = model.getPiece();
            counter = 1;
        } else  {
            // Tweede klik
            to = view;
            to.setSelectedTarget();
            target = model.getPosition();
            System.out.println("Target: " + target);

            boolean result = MoveValidator.isValidMove(board, piece, source, target);


            System.out.println(result);
            if(result){
                if (piece.getType() == PieceType.KING && Math.abs(target.getColumn() - source.getColumn()) == 2) {
                    // Rokade uitvoeren
                    int row =source.getRow();
                    if (target.getColumn() == 6) {
                        // Korte rokade
                        board.movePiece(new Position(row, 7), new Position(row, 5));
                    } else if (target.getColumn() == 2) {
                        // Lange rokade
                        board.movePiece(new Position(row, 0), new Position(row, 3));
                    }
                }
                board.movePiece(source, target);
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
