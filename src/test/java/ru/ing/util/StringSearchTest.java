package ru.ing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;

class StringSearchTest {

    @Test
    void testIsValidLine() {
        StringSearch stringSearch = new StringSearch();

        assertTrue(stringSearch.isValidLine("\"a\";\"b\";\"c\""));
        assertTrue(stringSearch.isValidLine("\"a\";\"\";\"c\""));
        assertTrue(stringSearch.isValidLine("\"a\""));
        assertTrue(stringSearch.isValidLine("\"a\";\"b\""));

        assertFalse(stringSearch.isValidLine("\"a\"b\";\"c\""));
        assertFalse(stringSearch.isValidLine("a;b;c"));
        assertFalse(stringSearch.isValidLine("\"a\";b;\"c\""));
        assertFalse(stringSearch.isValidLine("\"a\";\"b\";\"c"));
    }

    @Test
    void testGroupRecords() {
        StringSearch stringSearch = new StringSearch();

        List<List<String>> records1 = Arrays.asList(
                Arrays.asList("1", "2", "3"),
                Arrays.asList("1", "5", "6"),
                Arrays.asList("7", "8", "3")
        );

        List<Set<List<String>>> groups1 = stringSearch.groupRecords(records1);
        assertEquals(1, groups1.size());

        List<List<String>> records2 = Arrays.asList(
                Arrays.asList("1", "2", "3"),
                Arrays.asList("4", "5", "6"),
                Arrays.asList("7", "8", "9")
        );

        List<Set<List<String>>> groups2 = stringSearch.groupRecords(records2);
        assertEquals(3, groups2.size());

        List<List<String>> records3 = Arrays.asList(
                Arrays.asList("1", "2", "3"),
                Arrays.asList("1", "5", "6"),
                Arrays.asList("7", "8", "9"),
                Arrays.asList("10", "8", "11")
        );

        List<Set<List<String>>> groups3 = stringSearch.groupRecords(records3);
        assertEquals(2, groups3.size());
    }

    @Test
    void testReadDataFromFile(@TempDir Path tempDir) throws IOException {
        File testFile = tempDir.resolve("test.csv.gz").toFile();

        try (OutputStream out = new FileOutputStream(testFile);
             GZIPOutputStream gzipOut = new GZIPOutputStream(out);
             Writer writer = new OutputStreamWriter(gzipOut)) {

            writer.write("\"1\";\"2\";\"3\"\n");
            writer.write("\"4\";\"5\";\"6\"\n");
            writer.write("\"1\";\"2\";\"3\"\n");
            writer.write("invalid line\n");
        }

        StringSearch stringSearch = new StringSearch();
        List<List<String>> records = stringSearch.readDataFromFile(testFile.getAbsolutePath());

        assertEquals(2, records.size());
        assertEquals(Arrays.asList("1", "2", "3"), records.get(0));
        assertEquals(Arrays.asList("4", "5", "6"), records.get(1));
    }
}