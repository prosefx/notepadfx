package prosefx.notepadfx.sys;

import java.nio.file.*;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** The day-per-file folder: names, days read back from names, the nearest day with a file, the file made on demand. */
class JournalTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 5);
    static final String P = "yyyy_MM_dd.md";

    @Test void nameAndDayAreOneAnother() {
        assertEquals("2026_11_05.md", Journal.name(P, DAY));
        assertEquals(DAY, Journal.dayOf(P, Path.of("x", "2026_11_05.md")).orElseThrow());
        assertEquals("2026-11-05.org", Journal.name("yyyy-MM-dd.org", DAY));
        assertEquals("2026_11_05.md", Journal.name("", DAY));
    }

    @Test void otherFilesAreNoDay() {
        assertTrue(Journal.dayOf(P, Path.of("2026_02_30.md")).isEmpty());
        assertTrue(Journal.dayOf(P, Path.of("2026_11_05.txt")).isEmpty());
        assertTrue(Journal.dayOf(P, Path.of("notes.md")).isEmpty());
        assertTrue(Journal.dayOf(P, Path.of("2026_11_05.md.bak")).isEmpty());
    }

    @Test void theWordsForADay() {
        assertEquals(DAY, Journal.day("", DAY));
        assertEquals(DAY, Journal.day("today", DAY));
        assertEquals(DAY.minusDays(1), Journal.day("-1", DAY));
        assertEquals(DAY.minusDays(1), Journal.day("yesterday", DAY));
        assertEquals(DAY.plusDays(2), Journal.day("+2", DAY));
        assertEquals(LocalDate.of(2026, 10, 31), Journal.day("2026-10-31", DAY));
        assertEquals(LocalDate.of(2026, 10, 31), Journal.day("2026_10_31", DAY));
        assertThrows(IllegalArgumentException.class, () -> Journal.day("lastweek", DAY));
    }

    @Test void opensTodayAndMakesItOnlyOnce(@TempDir Path dir) throws Exception {
        var f = Journal.open(dir.toString(), P, DAY);
        assertEquals(dir.resolve("2026_11_05.md"), f);
        assertEquals("", Files.readString(f));
        Files.writeString(f, "- written");
        assertEquals("- written", Files.readString(Journal.open(dir.toString(), P, DAY)));
    }

    @Test void aMissingFolderIsNotMade(@TempDir Path dir) {
        var gone = dir.resolve("typo");
        assertThrows(java.io.IOException.class, () -> Journal.open(gone.toString(), P, DAY));
        assertFalse(Files.exists(gone));
    }

    @Test void neighbourSkipsDaysWithNoFile(@TempDir Path dir) throws Exception {
        for (var n : new String[]{"2026_11_01.md", "2026_11_03.md", "2026_11_09.md", "readme.md", "2026_11_04.txt"}) Files.createFile(dir.resolve(n));
        var d = dir.toString();
        assertEquals(dir.resolve("2026_11_03.md"), Journal.neighbour(d, P, DAY, -1).orElseThrow());
        assertEquals(dir.resolve("2026_11_09.md"), Journal.neighbour(d, P, DAY, 1).orElseThrow());
        assertEquals(dir.resolve("2026_11_01.md"), Journal.neighbour(d, P, LocalDate.of(2026, 11, 3), -1).orElseThrow());
        assertTrue(Journal.neighbour(d, P, LocalDate.of(2026, 11, 1), -1).isEmpty());
        assertTrue(Journal.neighbour(d, P, LocalDate.of(2026, 11, 9), 1).isEmpty());
        assertTrue(Journal.neighbour(dir.resolve("nope").toString(), P, DAY, -1).isEmpty());
    }
}
