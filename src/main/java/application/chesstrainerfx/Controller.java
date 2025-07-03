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
            System.out.println("Source: " + source);
            counter = 1;
        } else  {
            // Tweede klik
            to = view;
            to.setSelectedTarget();
            target = model.getPosition();
            System.out.println("Target: " + target);


            board.movePiece(source, target);


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
