# KR Compiler IDE v4.3.2

## Korrekturen

- Android-Go-Module erhalten einen stabilen ELF-SONAME (`libkr_go.so`), damit der Android-Linker keine Windows-Pfade als Laufzeitabhängigkeit übernimmt.
- `libsqlite3.so` wird nur noch in Android-APKs aufgenommen, wenn ein Projekt SQLite verwendet.
- Projektressourcen aus `assets/` werden zusätzlich als native Android-UI-Assets bereitgestellt.

## Enthalten

- KR Compiler IDE für Windows x64
- KR Compiler IDE für Android ARM64
- KR Compiler IDE für Linux x64
- Antigravity-, Android-Studio- und Visual-Studio-2022-Plugin
- Bebildertes Handbuch und Installationsdokumentation
