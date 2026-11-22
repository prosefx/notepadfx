# notepadfx

A fast markdown and text editor for the desktop, in JavaFX.

![notepadfx 0.1.2: workspaces, projects and files on the left, vertical tabs, the markdown editor with folding and line numbers, the outline on the right](docs/screenshots/notepadfx-0.1.2.png)

*Screenshot of notepadfx 0.1.2, taken 2026-10-26. When the app looks different from this, take a new one and name it after the version shown.*

- Workspaces, with a projects pane and a files pane, each with a regex filter.
- Vertical tabs that show the whole file name, wrapped.
- An outline of the open file (headings, bullets and paragraphs) that wraps long entries and follows the caret.
- Several windows in one process. Launching notepadfx again opens a new window in the running process; a tab can be dragged out into its own window, or between windows.
- Keyboard first: every action is on a function key, and the legend along the bottom is clickable.
- Light and dark themes.
- Fenced code blocks (triple backticks) are code: typed text, Tab and indentation stay literal, and a block in a list item, indented with a tab or spaces, is saved byte for byte.
- YAML front matter at the top of a markdown file is shown as a compact property block (key and value per line) and edited as text; an untouched file is saved byte for byte.

Markdown files open in the prosefx editor (WYSIWYG, saved losslessly); other text files in a plain editor.

## Keys

F1 settings, F2 save, F3 dictate (start, pause, resume), F4 close tab, F5 refresh, F6 next pane, F7 files panes, Shift+F7 spelling and grammar (on, then next issue), F8 outline, F9 read aloud from the caret or the selection, Shift+F9 move tab to a new window, F10 new window, F11 theme, F12 open, Shift+F12 recent files. Shift+F5 and Shift+F6 jump to the previous and next `>>` comment. Shift+F1 opens today's journal, Shift+F2 and Shift+F3 step to the day before and after. Shift+F4 locks or unlocks the open file (see below). Esc stops reading or dictation. Ctrl+Tab and Ctrl+PageUp/PageDown move between tabs.

## Daily journal

Point Settings (F1) at a folder that holds one file per day, such as the journals folder of a notes graph, and give the file name pattern (default `yyyy_MM_dd.md`). `notepadfx journal` opens today's file, making it empty if it is not there yet, and works even when notepadfx is not running; `notepadfx journal -1` is yesterday, `+1` tomorrow, `notepadfx journal 2026-11-04` a given day. In the window, Shift+F1 is today and Shift+F2 / Shift+F3 step to the nearest day before and after that has a file.

## Command line, for people and AI sessions

`notepadfx <file>:<line>` opens a file and selects that line; `notepadfx --help` lists everything. A coding session can read and change the live text of an open file together with the person typing in it (`read`, `append`, `insert`, `replace`), or take exclusive right over a file it opened (`close`, write, open again). To keep other programs from writing a file the person is typing in, `notepadfx lock <file> --owner <name>` (or Shift+F4) holds it: any other program's write fails at once, reading stays free, and sessions change it through the notepadfx commands. See [docs/agents.md](docs/agents.md). Locking works on Windows only.

## Install and update

Download `notepadfx.exe` from the latest release, then run `notepadfx install`: it goes to your user programs folder, on PATH, with Start Menu and Desktop shortcuts. `notepadfx update` updates it; the app also checks once a day and offers the update in its status bar. Install also adds "Open with notepadfx" to Explorer's right-click menu for files and folders; `notepadfx explorer-menu off` (or Settings) removes it.

## Build

Java 25 and Maven. `mvn verify` builds `target/notepadfx.jar` and `target/jr/notepadfx.exe` (a jarrunner launcher with the JVM in-process and an AOT cache).

State lives in `~/prosefx/notepadfx/session.json`.
