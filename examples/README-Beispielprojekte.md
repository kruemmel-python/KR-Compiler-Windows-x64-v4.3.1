# KR Compiler IDE – Beispielprojekte und Build-Matrix

Dieses Paket enthält fünf mit der KR Compiler IDE erzeugte Beispielprojekte. Jedes Projekt beinhaltet ein eigenständiges Testprogramm mit mehr als 5.000 Quellcodezeilen. Die Beispiele dienen als reproduzierbarer Belastungs- und Plattformtest.

## Schnellstart

1. `KR-Compiler-Beispielprojekte.zip` entpacken.
2. Die KR Compiler IDE starten.
3. **Projekt → Projektordner öffnen …** wählen und einen der fünf Projektordner öffnen.
4. Im Menü **Build** die Zielplattform wählen und **Projekt kompilieren** starten.

Die Projekte benötigen keine Dateien außerhalb ihres jeweiligen Ordners. Build-Ausgaben und temporäre Dateien werden in `KR_BUILD/` innerhalb des Projekts erstellt.

## Zielplattformen

| Ziel | Ergebnis | Werkzeugkette |
|---|---|---|
| Windows x64 | `KR_BUILD/programm.exe` | integriertes MinGW/GCC |
| Linux x64 | `KR_BUILD/programm` (ELF) | portable Zig-Toolchain |
| Android ARM64 | `KR_BUILD/programm-android-arm64.apk` | Android NDK und Runner-Template |

Die Linux-Toolchain befindet sich in `toolchain/zig/` neben der IDE. Sie ist portabel und wird automatisch verwendet. Sie erzeugt echte ELF-Dateien, keine umbenannten Windows-EXE-Dateien.

## Die fünf Projekte

### 1. `stress_modular_kr_forge`

Diese Vorlage überprüft die modulare KR-FORGE-Projektanlage mit `forge.krproject`. Für den Belastungstest wurde die KR-Quelle auf ein kompaktes C++-Segment mit 5.000 Funktionen reduziert, damit die Plattform-Builds unabhängig von optionalen OpenCL-, SQLite- oder SDL-Bibliotheken reproduzierbar sind.

- Haupteinstieg: `main.kr`
- Konfiguration: `forge.krproject`
- Test: 5.000 Funktionen `stress_0()` bis `stress_4999()`; `main()` addiert den ersten und letzten Rückgabewert.

### 2. `stress_single_kr`

Ein einzelnes KR-Projekt ohne zusätzliche Projektstruktur. Die Datei `main.kr` enthält ein C++20-kompatibles KR-Segment mit mehr als 5.000 Zeilen.

- Haupteinstieg: `main.kr`
- Test: 5.000 Funktionen sowie eine Konsolenausgabe des Ergebnisses.

### 3. `stress_cpp_project`

Ein C++20/CMake-Beispiel. Für Windows wird die vorhandene CMake-Konfiguration verwendet; für Linux erzeugt die IDE aus den C++-Quellen direkt ein ELF-Artefakt mit Zig.

- Haupteinstieg: `main.cpp`
- Konfiguration: `CMakeLists.txt`
- Test: 5.000 `static`-Funktionen und ein C++-Konsolenprogramm.

### 4. `stress_rust_project`

Ein Cargo/Rust-Beispiel. Zusätzlich enthält die Vorlage `src/lib.rs` als `cdylib`, damit der Android-Runner eine ARM64-Shared-Library erhält.

- Haupteinstieg: `src/main.rs`
- Android-Bibliothek: `src/lib.rs`
- Konfiguration: `Cargo.toml`
- Test: 5.000 Rust-Funktionen und ein `println!`-Einstieg.

### 5. `stress_go_project`

Ein Go-Modul ohne externe Abhängigkeiten.

- Haupteinstieg: `main.go`
- Konfiguration: `go.mod`
- Test: 5.000 Go-Funktionen und ein `fmt.Println`-Einstieg.

## Verifizierte Build-Änderungen

Während des Tests wurden folgende Fehler behoben:

- KR bindet die Adaptive-Compute-/OpenCL-Laufzeit nur noch ein, wenn GPU-fähiger Code vorhanden ist.
- Generierter KR-C++-Code bindet den Adaptive-Compute-Header nur ein, wenn er tatsächlich existiert.
- Rust erstellt für Android eine `cdylib`; der Android-Build verwendet den NDK-Linker.
- Go setzt für Windows, Linux und Android die passenden Zielplattformvariablen.
- Linux-C++- und Linux-KR-Builds verwenden Zig mit dem Ziel `x86_64-linux-musl`.
- Linux-Rust-Builds verwenden einen Zig-Wrapper als Linker für `x86_64-unknown-linux-gnu`.
- Deklarierte, aber nicht verwendete optionale Bibliotheken werden beim Linux-Cross-Build nicht gelinkt.
- Compiler-Caches (`TEMP`, `TMP`, Cargo, Go und Gradle) werden unter `KR_BUILD/.cache/` abgelegt, nicht im Benutzerprofil.

## Testnachweis

Die Quelldateien in diesem Paket sind die tatsächlich erzeugten Belastungstests. Sie können die Zeilenzahl beispielsweise im Editor prüfen. Bei jedem Build wird das Artefakt zusätzlich am Dateiformat geprüft:

- Windows: PE-Kennung `MZ`
- Linux: ELF-Kennung `7F 45 4C 46`
- Android: APK als ZIP-Kennung `PK`

Damit lässt sich nachvollziehen, dass nicht nur eine Datei umbenannt wurde, sondern das Artefakt für die jeweilige Plattform erzeugt wurde.
