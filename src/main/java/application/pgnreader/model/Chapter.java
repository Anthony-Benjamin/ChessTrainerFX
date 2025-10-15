package application.pgnreader.model;

import java.util.List;

public class Chapter {
    private final String title;
    private final List<Exercise> exercises;

    public Chapter(String title, List<Exercise> exercises) {
        this.title = title;
        this.exercises = exercises;
    }

    public String getTitle() {
        return title;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    @Override
    public String toString() {
        return title + " (" + exercises.size() + " oefeningen)";
    }
}
