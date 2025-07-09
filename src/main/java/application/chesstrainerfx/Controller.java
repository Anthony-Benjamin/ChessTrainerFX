package application.chesstrainerfx;

public class Controller {

    private int counter;
    private Position source, target;
    private SquareView from, to;

    public void handleSquareClick(BoardModel board, SquareView view, SquareModel model) {
        System.out.println("Clicked! in Controller");

        if (counter == 0) {
            from = view;
            from.setSeletedSource();
            source = model.getPosition();
            System.out.println("Source: " + source);;
            counter = 1;
        } else  {
            // Tweede klik
            to = view;
            //to.setSelectedTarget();
            target = model.getPosition();
            System.out.println("Target: " + target);

        if(isValidBishopMove(source,target,board)) {
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
    public boolean isValidBishopMove(Position start, Position target, BoardModel boardModel) {
        int dx = Math.abs(target.getRow() - start.getRow());
        int dy = Math.abs(target.getCol() - start.getCol());

        // Check of de zet diagonaal is
        if (dx != dy) {
            return false;
        }

        // Bepaal richting
        int stepX = (target.getRow() - start.getRow()) > 0 ? 1 : -1;
        int stepY = (target.getCol() - start.getCol()) > 0 ? 1 : -1;

        // Controleer of het pad vrij is (exclusief eindpositie)
        int x = source.getRow() + stepX;
        int y = source.getCol() + stepY;
        while (x != target.getCol() && y != target.getCol()) {
            if (boardModel.getSquare(new Position(x,y)) != null) {
                return false; // Er staat een stuk in de weg
            }
            x += stepX;
            y += stepY;
        }

        // Controleer of eindpositie bezet is door eigen stuk
       /* Piece targetPiece = board[endX][endY];
        Piece currentPiece = board[startX][startY];
        if (targetPiece != null && targetPiece.getColor() == currentPiece.getColor()) {
            return false; // Eigen stuk op doelvak
        }*/

        return true;
    }

}
