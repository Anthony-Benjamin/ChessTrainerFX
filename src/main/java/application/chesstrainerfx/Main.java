package application.chesstrainerfx;

public class Main {
    public static void main(String[] args) {
        BoardModel model = new BoardModel();
        model.initializeBoard();

        model.printBoard();
    }
}
