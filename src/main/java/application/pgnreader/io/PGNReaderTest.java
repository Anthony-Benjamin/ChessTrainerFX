package application.pgnreader.io;

import application.pgnreader.model.Exercise;

import java.util.List;

public class PGNReaderTest {


    public static void main(String[] args) {

        String path = "/pgn/mating/chapters/2_swallowstail_mate.pgn";

        List<Exercise> pgn = PGNReader.readChapter(path);

        for (Exercise exercise:pgn){
            System.out.println(exercise.title());
            System.out.println(exercise.fen());
            System.out.println(exercise.moves());
            System.out.println(exercise.comments());
        }

    }

}
