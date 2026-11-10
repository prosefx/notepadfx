package prosefx.notepadfx.agent;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import prosefx.notepadfx.state.Store;
import prosefx.notepadfx.sys.Journal;

/** `notepadfx journal [day]`: the journal file of a day (made empty if missing) as the file to open, from the folder and pattern in the user's settings. Needs no running notepadfx: the launch carries on and opens it. */
public final class JournalCli {

    public static Optional<String> file(List<String> argv) {
        var s = Store.load();
        var folder = s.journalFolder();
        if (folder == null || folder.isBlank()) return fail("no journal folder is set: in notepadfx press F1 (Settings) and fill in Journal folder");
        try {
            var day = Journal.day(argv.size() > 1 ? argv.get(1) : "", LocalDate.now());
            return Optional.of(Journal.open(folder, s.journalFile(), day).toString());
        } catch (IOException | RuntimeException e) {
            return fail(e.getMessage());
        }
    }

    static Optional<String> fail(String message) {
        System.err.println("notepadfx journal: " + message);
        return Optional.empty();
    }

    private JournalCli() {}
}
