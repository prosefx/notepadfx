package prosefx.notepadfx.ui;

import prosefx.notepadfx.sys.Journal;
import java.io.IOException;
import java.time.LocalDate;

/** The journal one step away: today's file, and the nearest day before or after the one on screen. Folder and file-name pattern come from Settings. */
final class Journals {

    static void today(MainWindow w) {
        if (!configured(w)) return;
        var s = Windows.session;
        try { Windows.open(Journal.open(s.journalFolder(), s.journalFile(), LocalDate.now())); }
        catch (IOException e) { w.status.flash(e.getMessage()); }
    }

    /** Before (-1) or after (+1) the journal on screen, or before or after today when another file is on screen. Days with no file are skipped, so nothing is made. */
    static void step(MainWindow w, int direction) {
        if (!configured(w)) return;
        var s = Windows.session;
        var open = w.active.get();
        var from = open == null ? null : Journal.dayOf(s.journalFile(), open.path).orElse(null);
        var next = Journal.neighbour(s.journalFolder(), s.journalFile(), from == null ? LocalDate.now() : from, direction);
        if (next.isPresent()) Windows.open(next.get());
        else w.status.flash("No " + (direction < 0 ? "earlier" : "later") + " journal in " + s.journalFolder());
    }

    static boolean configured(MainWindow w) {
        var folder = Windows.session.journalFolder();
        if (folder != null && !folder.isBlank()) return true;
        w.status.flash("Fill in Journal folder in Settings first");
        SettingsDialog.show(w);
        return false;
    }

    private Journals() {}
}
