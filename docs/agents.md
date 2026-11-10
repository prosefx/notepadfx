# How an AI session works on a file a person has open in notepadfx

A person and a coding session can keep one file open between them, for example a questions file: the session adds questions, the person types answers under them. notepadfx has two routes. Choose by asking what the file is doing.

```
notepadfx status <file>      # open? who owns it? unsaved changes? version?
```

## Route 1: sync, the person and the session edit the live text together

Use it when the file is open (it may hold the person's unsaved typing). Nothing is written to the file; the change goes into the editor, is one undo step, and the person's caret and selection stay where they are.

```
notepadfx read <file> [--lines 5-9] [--json]       # live text, with its version
notepadfx append <file> --text-stdin
notepadfx insert <file> --after-heading "## Questions" --text-stdin
notepadfx insert <file> --after-line 12 --expect "<text of line 12>" --text-stdin
notepadfx replace <file> --lines 12-14 --expect-file old.txt --text-file new.txt
```

Every change names what it expects to find. If the text has moved since you read it, notepadfx changes nothing and exits 5: read again and retry. `--version N` (from `read`) refuses unless the text is still at that version. An append cannot be stepped on, so it expects nothing. Include the blank line that separates blocks in your text. The person sees a notice in the status line, and Ctrl+Z takes the change back.

### Locking: keep other writers out while the person works

Without a lock, any program can write the file on disk while the person types in notepadfx, and they find out only when they save ("modified externally"). A locked file cannot be written by any other program: the write fails at once ("in use by another process"). Reading stays free, and notepadfx still saves. The person toggles the lock with Shift+F4; a session asks for it:

```
notepadfx lock <file> --owner <your-name>      # `status` then shows "locked: <your-name>" (or "person")
notepadfx unlock <file> --owner <your-name>    # only the owner of that lock; anyone else gets exit 4
```

While a file is locked, change it only with the verbs above (`append`, `insert`, `replace`). If your own write on disk fails with "in use by another process", the file is locked: use the verbs, do not look for a way round. Closing a locked file puts the question to the person, even for the session that opened it, unless that session set the lock. The lock lasts until the file is closed or notepadfx ends. Windows only.

## Route 2: exclusive, the session writes the file itself

Use it when you need full right over the file (a rewrite, a tool that writes it).

```
notepadfx --owner <your-name> <file>               # when you open it, say who you are
...
notepadfx close <file> --owner <your-name>         # you opened it, so you may close it
# write the file on disk
notepadfx --owner <your-name> <file>:<line>        # open it again at the line the person should look at
```

Only the session that opened a file may close it. Any other close is a request: the person gets a short question (Enter agrees, Esc keeps it open; keys are ignored for the first two seconds so a stray keystroke cannot answer it). A file with unsaved changes is never closed silently, by anyone: the person is asked to save, discard or cancel. Add `--no-ask` to never put a question: exit 6 when unsaved, 4 when you are not the owner. `--timeout <seconds>` limits the wait (default 120, exit 7).

A file opened without `--owner` has no owner, so every close of it from the command line is a request.

## Exit codes

0 done, 1 error or bad usage, 2 notepadfx is not running, 3 file not open, 4 refused by the person, 5 stale (nothing changed), 6 unsaved changes, 7 the person did not answer in time, 8 no such file or line. `notepadfx --help` lists everything.
