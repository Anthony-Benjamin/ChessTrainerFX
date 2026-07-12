package application.pgnreader.io;

import application.pgnreader.model.Exercise;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PGNCommentWriterTest {

    private static final String THREE_GAMES = """
            [Event "?"]
            [White "Eerste"]
            [Result "*"]
            [SetUp "1"]
            [FEN "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1"]

            {[#]} 1. Re8# {Auteursuitleg eerste spel.} *

            [Event "?"]
            [White "Tweede"]
            [Result "*"]
            [SetUp "1"]
            [FEN "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1"]

            {[#]} 1. Re8# *

            [Event "?"]
            [White "Derde"]
            [Result "*"]
            [SetUp "1"]
            [FEN "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1"]

            {[#]} 1. Re8# *
            """;

    private Path tempPgn(String content) throws Exception {
        Path file = Files.createTempFile("note-writer", ".pgn");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void addsNoteToMiddleGameWithoutTouchingOthers() throws Exception {
        Path file = tempPgn(THREE_GAMES);

        assertTrue(PGNCommentWriter.writeUserNote(file, 1, "Let op de achterste rij."));

        List<Exercise> exercises = PGNReader.readChapter(file);
        assertEquals(3, exercises.size());
        assertEquals("", exercises.get(0).getUserNote());
        assertEquals("Let op de achterste rij.", exercises.get(1).getUserNote());
        assertEquals("", exercises.get(2).getUserNote());

        // De notitie hoort niet in de gewone comments te belanden.
        assertEquals("", exercises.get(1).getComments());
        assertEquals("Auteursuitleg eerste spel.", exercises.get(0).getComments());

        // Andere spellen blijven tekstueel onaangeroerd.
        String content = Files.readString(file);
        assertTrue(content.contains("{[#]} 1. Re8# {Auteursuitleg eerste spel.} *"));
        assertTrue(content.contains("{[%unote] Let op de achterste rij.} *"));
    }

    @Test
    void replacesAndRemovesExistingNote() throws Exception {
        Path file = tempPgn(THREE_GAMES);
        assertTrue(PGNCommentWriter.writeUserNote(file, 2, "Eerste versie"));
        assertTrue(PGNCommentWriter.writeUserNote(file, 2, "Tweede versie"));

        List<Exercise> exercises = PGNReader.readChapter(file);
        assertEquals("Tweede versie", exercises.get(2).getUserNote());
        String content = Files.readString(file);
        assertFalse(content.contains("Eerste versie"));

        // Lege invoer verwijdert de notitie weer.
        assertTrue(PGNCommentWriter.writeUserNote(file, 2, "  "));
        exercises = PGNReader.readChapter(file);
        assertEquals("", exercises.get(2).getUserNote());
        assertFalse(Files.readString(file).contains("[%unote]"));
    }

    @Test
    void preservesByteOrderMark() throws Exception {
        Path file = tempPgn("﻿" + THREE_GAMES);

        assertTrue(PGNCommentWriter.writeUserNote(file, 0, "Notitie"));

        byte[] bytes = Files.readAllBytes(file);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        assertEquals("Notitie", PGNReader.readChapter(file).getFirst().getUserNote());
    }

    @Test
    void sanitizesBracesAndNewlinesInNote() throws Exception {
        Path file = tempPgn(THREE_GAMES);

        assertTrue(PGNCommentWriter.writeUserNote(file, 0, "Pas op {valstrik}\nop de e-lijn"));

        List<Exercise> exercises = PGNReader.readChapter(file);
        assertEquals(3, exercises.size()); // notitie splitst het bestand niet in extra spellen
        assertEquals("Pas op (valstrik) op de e-lijn", exercises.getFirst().getUserNote());
    }

    @Test
    void rejectsMissingFileOrIndex() throws Exception {
        Path file = tempPgn(THREE_GAMES);

        assertFalse(PGNCommentWriter.writeUserNote(null, 0, "x"));
        assertFalse(PGNCommentWriter.writeUserNote(file, -1, "x"));
        assertFalse(PGNCommentWriter.writeUserNote(file, 99, "x"));
    }
}
