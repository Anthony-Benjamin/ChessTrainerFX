package application.pgnreader.io;

import application.pgnreader.model.Exercise;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PGNReaderTest {

    @Test
    void preservesVariationsInMoveText() throws Exception {
        Path file = Files.createTempFile("exercise-variations", ".pgn");
        Files.writeString(file, """
                [Event "?"]
                [White "test"]
                [Black "?"]
                [Result "*"]
                [SetUp "1"]
                [FEN "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"]

                1. Nf3 (1. d4 d5 2. c4) 1... Nf6 2. g3 *
                """);

        List<Exercise> exercises = PGNReader.readChapter(file);

        assertEquals(1, exercises.size());
        assertTrue(exercises.getFirst().getMoves().contains("(1. d4 d5 2. c4)"));
    }
}
