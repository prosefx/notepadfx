package prosefx.notepadfx.doc;

import java.nio.file.*;
import javafx.beans.property.*;
import prosefx.notepadfx.editor.*;

public final class Doc {
    public final Path path;
    public final BooleanProperty dirty = new SimpleBooleanProperty();
    public final IntegerProperty version = new SimpleIntegerProperty();
    /** Completes when the document is closed (tab or window), for `--wait` callers such as an editor hook. */
    public final java.util.concurrent.CompletableFuture<Void> closed = new java.util.concurrent.CompletableFuture<>();
    final BooleanProperty differsFromFile = new SimpleBooleanProperty();
    Editor editor;
    FileText loaded;
    boolean source;
    boolean shownOnce;

    Doc(Path path) { this.path = path; }

    public String name() { return path.getFileName().toString(); }

    public boolean markdown() {
        var n = name().toLowerCase();
        return n.endsWith(".md") || n.endsWith(".markdown");
    }

    public boolean source() { return source; }

    /** True the first time it is asked, for the set-up an editor gets when its file is first shown (the status bar may have created the editor by then, so "has no editor yet" cannot tell). */
    public boolean firstShow() { var first = !shownOnce; shownOnce = true; return first; }

    public String text() { return editor != null ? editor.text() : file().text(); }

    public boolean hasEditor() { return editor != null; }

    /** Who opened this file from the command line (`--owner`), or null when a person opened it. Only the owner may close it without being asked about it. */
    String owner;

    public String owner() { return owner; }

    public void owner(String name) { owner = name; }

    public String encoding() {
        var f = file();
        return "UTF-8" + (Boolean.TRUE.equals(f.bom()) ? " BOM" : "") + (Boolean.TRUE.equals(f.crlf()) ? "  CRLF" : "  LF");
    }

    public Editor editor() {
        if (editor == null) attach(markdown() && !source ? new ProseEditor() : new TextEditor(), file().text());
        return editor;
    }

    void attach(Editor e, String text) {
        editor = e;
        editor.baseDirectory(path.getParent());
        editor.load(text);
        differsFromFile.set(!text.equals(file().text()));
        dirty.unbind();
        dirty.bind(editor.dirty().or(differsFromFile));
        editor.onChange(() -> version.set(version.get() + 1));
    }

    public boolean toggleSource() {
        if (!markdown()) return false;
        var old = editor();
        var text = old.text();
        var at = CaretMap.index(old.outline(), old.caret().getValue().intValue());
        old.close();
        source = !source;
        attach(source ? new TextEditor() : new ProseEditor(), text);
        CaretMap.restore(editor, at);
        version.set(version.get() + 1);
        return true;
    }

    FileText file() {
        if (loaded == null) { loaded = FileText.read(path); stamp = stampOf(path); }
        return loaded;
    }

    public void save() throws Exception {
        var t = text();
        var bytes = file().encode(t);
        if (channel != null) writeThroughLock(bytes); else Files.write(path, bytes);
        stamp = stampOf(path);
        loaded.text(t);
        differsFromFile.set(false);
        if (editor != null) editor.markSaved();
    }

    // ---- the lock: while held, no other program can write to or delete the file; only this document can, through its own handle

    private java.nio.channels.FileChannel channel;
    private String lockedBy;
    private final BooleanProperty lockedProperty = new SimpleBooleanProperty();

    public boolean locked() { return channel != null; }

    public ReadOnlyBooleanProperty lockedProperty() { return lockedProperty; }

    /** Who set the lock: "person", or the name of the session that asked for it on the command line. Null when not locked. */
    public String lockedBy() { return lockedBy; }

    /**
     * Holds the file open so that every other program's attempt to write to it fails at once ("in use by another process"),
     * while reading stays free. The operating system lets go if notepadfx ends, however it ends. Windows only.
     */
    public void lock(String by) throws java.io.IOException {
        if (channel != null) return;
        try {
            channel = java.nio.channels.FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE,
                com.sun.nio.file.ExtendedOpenOption.NOSHARE_WRITE, com.sun.nio.file.ExtendedOpenOption.NOSHARE_DELETE);
        } catch (UnsupportedOperationException e) {
            throw new java.io.IOException("locking a file needs Windows");
        } catch (java.io.IOException e) {
            throw new java.io.IOException("could not lock " + name() + ": another program has it open for writing (" + e.getMessage() + ")");
        }
        lockedBy = by;
        lockedProperty.set(true);
    }

    public void unlock() {
        if (channel == null) return;
        try { channel.close(); } catch (java.io.IOException e) { System.err.println("notepadfx: " + e); }
        channel = null;
        lockedBy = null;
        lockedProperty.set(false);
    }

    /** The new bytes go in before the file is cut to their length, so a reader never meets an empty file. */
    private void writeThroughLock(byte[] bytes) throws java.io.IOException {
        var b = java.nio.ByteBuffer.wrap(bytes);
        channel.position(0);
        while (b.hasRemaining()) channel.write(b);
        channel.truncate(bytes.length);
        channel.force(true);
    }

    // ---- the file changed under us (another editor, a script, another tool)

    /** What was found when the file on disk was compared with what this document last read or wrote. */
    public enum Disk { SAME, RELOADED, CONFLICT }

    long stamp = -1;
    FileText pending;

    static long stampOf(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis() * 31 + Files.size(p); } catch (java.io.IOException e) { return -1; }
    }

    /**
     * Compares the file with what was last read or written. Unchanged: SAME. Changed and nothing unsaved here: the new
     * text is loaded with the caret kept (RELOADED). Changed while there are unsaved edits: CONFLICT, and the disk text
     * waits for takeDisk() or keepMine().
     */
    public Disk syncFromDisk() {
        var s = stampOf(path);
        if (loaded == null || s == stamp || s == -1) return Disk.SAME;
        var disk = FileText.read(path);
        stamp = s;
        if (disk.text().equals(loaded.text())) { loaded = disk; return Disk.SAME; }
        if (editor != null && dirty.get()) { pending = disk; return Disk.CONFLICT; }
        loaded = disk;
        replaceKeepingPlace();
        return Disk.RELOADED;
    }

    /** True while the file on disk holds text this document has not taken (see CONFLICT). */
    public boolean conflict() { return pending != null; }

    /** Resolves a CONFLICT by dropping the unsaved edits for the text on disk. */
    public void takeDisk() {
        loaded = pending != null ? pending : FileText.read(path);
        pending = null;
        stamp = stampOf(path);
        replaceKeepingPlace();
    }

    /** Resolves a CONFLICT the other way: the edits stay, the document is still unsaved, and saving overwrites the disk text. */
    public void keepMine() {
        if (pending != null) loaded = pending;
        pending = null;
        differsFromFile.set(editor != null && !editor.text().equals(loaded.text()));
    }

    void replaceKeepingPlace() {
        differsFromFile.set(false);
        if (editor != null) {
            var at = editor.position();
            editor.load(loaded.text());
            editor.position(at);
        }
        version.set(version.get() + 1);
    }

    public void reload() {
        loaded = FileText.read(path);
        stamp = stampOf(path);
        differsFromFile.set(false);
        if (editor != null) editor.load(loaded.text());
        version.set(version.get() + 1);
    }
}
