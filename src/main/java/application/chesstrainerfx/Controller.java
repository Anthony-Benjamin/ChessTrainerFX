package application.chesstrainerfx;

public class Controller {

    private int counter;
    private Position source, target;

    public void handleSquareClick(BoardModel board, SquareView view, SquareModel model ) {
        System.out.println("Clicked! in Controller");

        if(counter==0){
            view.setSeletedSource();
            System.out.println("Square: " + model.getPosition().getRow() + ", " + model.getPosition().getCol());
            source = model.getPosition();
            counter++;
        }else{
            view.setSelectedTarget();
            System.out.println("Square: " + model.getPosition().getRow() + ", " + model.getPosition().getCol());
            target = model.getPosition();
            counter= 0;
        }


        board.movePiece(source ,target);
    }
}
