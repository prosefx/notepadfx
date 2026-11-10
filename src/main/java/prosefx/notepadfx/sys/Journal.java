package prosefx.notepadfx.sys;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A folder with one file per day (a logseq graph's journals folder, or any dated notes): which file is a day's, and which days exist. The folder and the file-name pattern, such as `yyyy_MM_dd.md`, are the user's own settings. */
public final class Journal {
    public static final String PATTERN = "yyyy_MM_dd.md";

    public static String name(String pattern, LocalDate day) { return format(pattern).format(day) + ext(pattern); }

    public static Path file(String folder, String pattern, LocalDate day) { return Path.of(folder).resolve(name(pattern, day)); }

    /** The day a file name stands for; empty for any other file. Read back through `name`, so `2026_02_30.md` is no day. */
    public static Optional<LocalDate> dayOf(String pattern, Path file) {
        var n = file.getFileName().toString();
        try {
            var day = LocalDate.parse(n.substring(0, Math.max(0, n.length() - ext(pattern).length())), format(pattern));
            return name(pattern, day).equals(n) ? Optional.of(day) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** The nearest day that has a file before (direction below 0) or after `from`, skipping the days with none. */
    public static Optional<Path> neighbour(String folder, String pattern, LocalDate from, int direction) {
        var byDay = Map.Entry.<LocalDate, Path>comparingByKey();
        try (var files = Files.list(Path.of(folder))) {
            return files.flatMap(f -> dayOf(pattern, f).map(d -> Map.entry(d, f)).stream())
                .filter(e -> direction < 0 ? e.getKey().isBefore(from) : e.getKey().isAfter(from))
                .min(direction < 0 ? byDay.reversed() : byDay).map(Map.Entry::getValue);
        } catch (IOException | InvalidPathException e) {
            return Optional.empty();
        }
    }

    /** The file of a day, made empty when it is not there yet; the folder itself must exist, so a mistyped folder is told and not made. */
    public static Path open(String folder, String pattern, LocalDate day) throws IOException {
        if (!Files.isDirectory(Path.of(folder))) throw new IOException("the journal folder does not exist: " + folder);
        var f = file(folder, pattern, day);
        try { Files.createFile(f); } catch (FileAlreadyExistsException present) { }
        return f;
    }

    /** `today` or nothing, `yesterday`, `-1` / `+2` days from today, or a date as 2026-11-04 or 2026_11_04. */
    public static LocalDate day(String arg, LocalDate today) {
        var a = arg == null ? "" : arg.strip();
        if (a.isEmpty() || a.equals("today")) return today;
        if (a.equals("yesterday")) return today.minusDays(1);
        if (a.matches("[+-]?\\d{1,3}")) return today.plusDays(Integer.parseInt(a));
        try { return LocalDate.parse(a.replace('_', '-')); }
        catch (DateTimeException e) { throw new IllegalArgumentException("not a day: " + a + " (use -1, +1, yesterday or 2026-11-04)"); }
    }

    /** The date part of the pattern: everything before the extension. */
    static DateTimeFormatter format(String pattern) {
        var p = pattern(pattern);
        return DateTimeFormatter.ofPattern(p.substring(0, p.length() - ext(p).length()));
    }

    static String ext(String pattern) {
        var p = pattern(pattern);
        return p.lastIndexOf('.') < 0 ? "" : p.substring(p.lastIndexOf('.'));
    }

    static String pattern(String p) { return p == null || p.isBlank() ? PATTERN : p.strip(); }

    private Journal() {}
}
