package application.pgnreader.model;

import java.nio.file.Path;

public class Exercise {
    private final String title;
    private final String subtitle;
    private final String fen;
    private final String moves;
    private String comments;       // auteurscommentaar; bewerkbaar vanuit de oefening
    private final Path sourceFile;   // bron-PGN-bestand, null voor in-memory oefeningen
    private final int gameIndex;     // index van het spelblok in het bronbestand, -1 zonder bron
    private String userNote;         // eigen notitie van de gebruiker ({[%unote] ...} in de PGN)

    public Exercise(String title, String fen, String moves, String comments) {
        this(title, "", fen, moves, comments);
    }

    public Exercise(String title, String subtitle, String fen, String moves, String comments) {
        this(title, subtitle, fen, moves, comments, null, -1, "");
    }

    public Exercise(String title, String subtitle, String fen, String moves, String comments,
                    Path sourceFile, int gameIndex, String userNote) {
        this.title = title;
        this.subtitle = subtitle;
        this.fen = fen;
        this.moves = moves;
        this.comments = comments;
        this.sourceFile = sourceFile;
        this.gameIndex = gameIndex;
        this.userNote = userNote == null ? "" : userNote;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getFen() { return fen; }
    public String getMoves() { return moves; }
    public String getComments() { return comments; }
    public Path getSourceFile() { return sourceFile; }
    public int getGameIndex() { return gameIndex; }
    public String getUserNote() { return userNote; }

    public void setUserNote(String userNote) {
        this.userNote = userNote == null ? "" : userNote;
    }

    public void setComments(String comments) {
        this.comments = comments == null ? "" : comments;
    }

    @Override
    public String toString() { return title; }
}
