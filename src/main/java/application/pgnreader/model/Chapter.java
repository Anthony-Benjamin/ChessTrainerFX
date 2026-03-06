// File: Chapter.java
package application.pgnreader.model;

import java.util.List;

public record Chapter(String title, List<Exercise> exercises, String sourcePath) {

    @Override
    public String toString() {
        return title + " (" + exercises.size() + " oefeningen)";
    }
}
