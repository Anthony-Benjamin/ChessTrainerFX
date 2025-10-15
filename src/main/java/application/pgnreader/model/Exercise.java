package application.pgnreader.model;

public class Exercise {
    private final String title;
    private final String fen;
    private final String moves;


    private final String comments;

    public Exercise(String title, String fen, String moves, String comments) {
        this.title = title;
        this.fen = fen;
        this.moves = moves;
        this.comments = comments;
    }

    public String getTitle() {
        return title;
    }

    public String getFen() {
        return fen;
    }

    public String getMoves() {
        return moves;
    }
    public String getComments() {
        return comments;
    }


    @Override
    public String toString() {
        return title;
    }
}
