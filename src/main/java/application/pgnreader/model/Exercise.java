// File: Exercise.java
package application.pgnreader.model;

public record Exercise(String title, String fen, String moves, String comments) {

    @Override
    public String toString() {
        return title;
    }
}
