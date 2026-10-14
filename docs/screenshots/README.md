# Screenshots

Drop release-ready captures in this folder using these exact filenames so the root
`README.md` and the landing page (`index.html`) pick them up:

| File | Screen | Notes |
| --- | --- | --- |
| `timer.png` | Timer setup | Default state, a chime selected |
| `session.png` | Active session | Mid-sit with the circular progress |
| `journal.png` | Journal | Entries or Overview tab |
| `teachings.png` | Teachings | Topic list |
| `widget.png` | Home widget | Optional |

Recommended: portrait `1080 x 2340` (any 9:19.5 phone is fine), PNG, status bar trimmed.

## Capture with adb (Windows PowerShell)

1. Connect the device and launch the debug build (package `com.buddho.timer.debug`).
2. Navigate to the screen you want, then run:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell screencap -p /sdcard/timer.png
& $adb pull /sdcard/timer.png docs/screenshots/timer.png
```

3. Repeat for each screen, changing the filename (`session.png`, `journal.png`, `teachings.png`, `widget.png`).

> Tip: `adb exec-out screencap -p > file.png` can corrupt the PNG on PowerShell
> because of text line-ending translation. Prefer `screencap` + `pull` as above.
