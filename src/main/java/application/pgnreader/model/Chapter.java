// File: Chapter.java
package application.pgnreader.model;

import java.util.List;

public class Chapter {
    private final String title;
    private final List<Exercise> exercises;
    private final String sourcePath;

    public Chapter(String title, List<Exercise> exercises, String sourcePath) {
        this.title = title;
        this.exercises = exercises;
        this.sourcePath = sourcePath;
    }

    public String getTitle() { return title; }
    public List<Exercise> getExercises() { return exercises; }
    public String getSourcePath() { return sourcePath; }

    @Override
    public String toString() {
        return title + " (" + exercises.size() + " oefeningen)";
    }
}
