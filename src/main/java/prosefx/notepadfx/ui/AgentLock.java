package prosefx.notepadfx.ui;

import prosefx.notepadfx.agent.*;
import prosefx.notepadfx.doc.*;

import static prosefx.notepadfx.ui.AgentServer.reply;

/**
 * `notepadfx lock <file>` and `unlock`. A locked file cannot be written by any other program, so a session has to come
 * through notepadfx (read, append, insert, replace) and can never step on the person's typing. The person lifts any lock
 * with Shift+F4; a session lifts only a lock it set itself (same --owner).
 */
final class AgentLock {

    static String lock(Doc d, Args a) {
        var by = a.get("owner", "session");
        if (d.locked()) return reply(Exit.DONE, AgentFacts.lines(AgentFacts.facts(d), a.has("json")));
        try { d.lock(by); } catch (java.io.IOException e) { return reply(Exit.ERROR, e.getMessage() + "\n"); }
        var w = Windows.windowOf(d);
        if (w != null) w.status.flash(d.name() + " is locked by " + by + ": other programs cannot write it (Shift+F4 unlocks)");
        return reply(Exit.DONE, AgentFacts.lines(AgentFacts.facts(d), a.has("json")));
    }

    static String unlock(Doc d, Args a) {
        if (!d.locked()) return reply(Exit.DONE, AgentFacts.lines(AgentFacts.facts(d), a.has("json")));
        if (!d.lockedBy().equals(a.get("owner", ""))) return reply(Exit.REFUSED, d.name() + " is locked by " + d.lockedBy() + "; only that lock's owner or the person may lift it\n");
        d.unlock();
        var w = Windows.windowOf(d);
        if (w != null) w.status.flash(d.name() + " is unlocked");
        return reply(Exit.DONE, AgentFacts.lines(AgentFacts.facts(d), a.has("json")));
    }

    private AgentLock() {}
}
