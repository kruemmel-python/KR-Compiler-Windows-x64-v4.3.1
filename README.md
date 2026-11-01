# KR Compiler IDE – Windows Desktop Edition & Android Studio Unified Plugin (v4.3.1)

**Entwickler:** Ralf Krümmel  
**Version:** 4.3.1 (4-Sprachen-Edition: C++20 · Rust · Go · Python)  
**Plattform:** Windows x64 (10 / 11) & Android Studio (Rabbit 2 / Rabbit 3 / 2024.x–2026.x+)  
**Technologie:** Java 17/21+ (Swing Native Dark UI & IntelliJ Platform Plugin SDK), Clang/LLVM, Rust (cargo/rustc), Go, Python AOT Bridge (`AOT_NATIVE_CPP20_BRIDGE`), Zig, CMake, Ninja, Android NDK  

---

## 🌟 Übersicht

Die **KR Compiler IDE Windows Desktop Edition (v4.3.1)** vereint **vier Programmiersprachen (C++20, Rust, Go und Python)** innerhalb einer einzigen `.kr`-Quelldatei über eine gemeinsame ABI-Sprachfusion (`kr_bridge.h`) und kompiliert Projekte ohne manuelles FFI/JNI-Boilerplate direkt in:
- **Android ARM64 APKs** (`-android-arm64.apk` + `app/src/main/jniLibs/arm64-v8a/libuser.so`),
- **Windows x64 Executables** (`.exe`),
- **Linux x64 Binaries** (`ELF`).

Zusätzlich enthält dieses Repository das offizielle **KR Unified Language Plugin für Android Studio** (`android-studio-plugin/`), mit dem `.kr`-Projekte direkt in Android Studio Rabbit 2 / Rabbit 3 bearbeitet, diagnostiziert, gebaut und auf Android-Geräten gestartet werden können.

---

## ⚠️ WICHTIG FÜR ENTWICKLER & NUTZER: Voraussetzungen für das Android Studio Plugin

> **Das Android Studio Plugin (`KR-Unified-Language-AndroidStudio-Plugin-v4.3.1.zip`, ~356 KB) funktioniert NICHT allein ohne die KR Compiler IDE!**
>
> Das Plugin ist bewusst als leichtgewichtige 3-Schichten-Brücke (Editor, Compiler Bridge, Android Build Adapter) konzipiert. Es bringt keinen zweiten Compiler mit, sondern ruft im Hintergrund die **KR Compiler IDE** und deren portable **Toolchain** (`toolchain/` mit Clang++/MinGW, Rust/Cargo, Go, Zig und Android-Runner-Template) auf.

### Was Sie für die Nutzung in Android Studio benötigen (Checkliste):

| Nr. | Komponente | Warum erforderlich? | Bezugsquelle / Einrichtung |
| :--- | :--- | :--- | :--- |
| **1.** | **KR Compiler IDE (`KR-Compiler-Windows-x64-v4.3.1.zip`)** *(oder geklontes Repo inkl. `toolchain/` & `runtime/`)* | Enthält die zentrale KR 4-Sprachen-Engine (`dist/KR-Compiler-IDE.jar`), die portable Compiler-Toolchain (`toolchain/`) und die Java-Laufzeit (`runtime/jdk`). | Aus den [GitHub Releases](https://github.com/kruemmel-python/KR-Compiler-Windows-x64-v4.3.1/releases) herunterladen und z. B. nach `C:\KR-Compiler\KR-Compiler-Windows-x64-v4.3.1` oder `H:\KR-Compiler\KR-Compiler-Windows-x64-v4.3.1` entpacken *(oder im geklonten Repo `download-toolchain.ps1` + `provision-runtime.ps1` ausführen)*. |
| **2.** | **Android Studio (2024.x – 2026.x / Rabbit 2 / Rabbit 3)** + **Android SDK & NDK** | Stellt die IDE-Oberfläche, `adb.exe` (`platform-tools`), `apksigner.bat` (`build-tools/35.0.0`) sowie den ARM64-Clang++-Compiler (`ndk/28.2.13676358`) bereit. | Im Android Studio **SDK Manager** unter *SDK Tools* das **NDK (Side by side)**, **Android SDK Build-Tools 35.0.0+** und **Android SDK Platform-Tools** aktivieren. |
| **3.** | **KR Unified Language Plugin (`KR-Unified-Language-AndroidStudio-Plugin-v4.3.1.zip`)** | Erweitert Android Studio um den `.kr`-Mehrsprachen-Editor, Echtzeit-Diagnose, Gradle-Adapter und das `KR Unified Language` Tool-Window. | In Android Studio über **`Settings → Plugins → ⚙️ → Install Plugin from Disk...`** die Datei `KR-Unified-Language-AndroidStudio-Plugin-v4.3.1.zip` installieren. |
| **4.** | **Pfad-Verknüpfung zwischen Plugin & KR Compiler IDE** | Damit das Android-Studio-Plugin weiß, wo Ihre entpackte KR Compiler IDE (`toolchain/` & `dist/`) liegt. | **Entweder** im unteren Android-Studio-ToolWindow `KR Unified Language` auf **`KR-Engine Pfad…`** klicken und den Ordner `KR-Compiler-Windows-x64-v4.3.1` auswählen, **oder** die Windows-Umgebungsvariable `KR_COMPILER_HOME` auf diesen Ordner setzen. |

📘 **Ausführliches Handbuch:** Das komplette, bebilderte Handbuch für die KR Compiler IDE und alle Plugins steht unter [docs/KR-Compiler-IDE-Handbuch.md](docs/KR-Compiler-IDE-Handbuch.md). Android-spezifische Details finden sich zusätzlich in [DOKUMENTATION-Android-Studio-Plugin.md](DOKUMENTATION-Android-Studio-Plugin.md) und [KR_Android_Studio_Handbuch.md](KR_Android_Studio_Handbuch.md).  
🧪 **Fertiges 4-Sprachen-Beispielprojekt für Android Studio:** [`examples/KR_ANDROID_STUDIO_4LANG_DEMO`](examples/KR_ANDROID_STUDIO_4LANG_DEMO) (direkt über `File → Open...` in Android Studio ladbar).

---

## 🚀 Hauptfunktionen (1 zu 1 mit der mobilen IDE)

### 1. Navigation & Ansichten
- **`CODE`**:
  - Moderner Multi-Tab-Editor mit Syntax-Highlighting für KR Unified Language (`.kr`), C++20 (`.cpp`, `.h`), Rust (`.rs`), Go (`.go`), TOML (`.krproject`, `Cargo.toml`) und CMake (`CMakeLists.txt`).
  - Zeilennummerierung, Echtzeit-Bearbeitungsanzeige (`*`), Statuszeile mit Zeilen-, Spalten- und Zeichenzähler.
  - Tastatur-Shortcuts: `Strg+S` (Speichern), `Strg+Umschalt+S` (Alles speichern), `Strg+N` (Neues Projekt), `Strg+O` (Projekt öffnen), `F5` (Kompilieren), `Strg+A` (KI-Agent).
- **`DATEIEN`**:
  - Interaktiver Projekt-Dateibaum mit Rechtsklick-Kontextmenü (Neue Datei, Neuer Ordner, Umbenennen, Löschen, Im Windows Explorer öffnen).
  - Eindeutige, skalierbare Symbole für Projekte, geöffnete/geschlossene Ordner und Dateien; KR-, Projekt-, C/C++-, Rust-, Go-, Daten- und Textdateien besitzen eigene Farbakzente.
  - Sofortige Dateisystem-Synchronisation.
- **`BUILD`**:
  - Plattformauswahl: **Windows x64 (.exe)**, **Linux x64 (ELF)**, **Android ARM64 (.so)**.
  - Live-Compiler-Konsole mit detailliertem Protokoll und Fehlerdiagnostik.
  - Optionale vcpkg Online-Abhängigkeiten.
- **`ARTEFAKTE`**:
  - Übersicht aller erzeugten Binärdateien und Logs im Ordner `KR_BUILD/`.
  - **1-Klick-Ausführung (`▶️ Ausführen`)**: Startet die kompilierte `.exe` direkt in einem separaten Windows-Befehlsfenster.
  - „Im Explorer anzeigen“ und „Log im Editor öffnen“.
- **`🤖 AGENT`**:
  - Integrierter autonomer KI-Programmieragent mit ReAct-Werkzeugschleife (`list`, `read`, `write`, `build`, `done`).
  - Volle Unterstützung für alle OpenAI-kompatiblen HTTPS-APIs sowie lokale Modelle über **Ollama** (`http://127.0.0.1:11434`) oder **LM Studio** (`http://127.0.0.1:1234`).
  - **Sicherheits-Sandbox & Genehmigungssystem**: Human-in-the-Loop-Dialoge vor Dateiänderungen und Build-Starts.
  - **Revisionssicheres Audit-Log**: Protokollierung in `kr_agent_audit.jsonl`.
  - **`📋 Kopieren`**: Ein-Klick-Kopieren des gesamten Verlaufs in die Windows-Zwischenablage.
  - **`💾 Export`**: Speichert das Protokoll formatiert als `kr_agent_log.txt` im Projektordner.
  - Live-Statusbanner und ProgressBar; der Agent läuft auch bei minimiertem Fenster ununterbrochen weiter.

### Anzeige & Barrierearmut

Über **`Ansicht -> Einstellungen …`** oder das Zahnrad in der Titelleiste lässt sich die Darstellung ohne Fachkenntnisse anpassen:

- fünf kontrastgeprüfte Designs: GitHub Dunkel, Mitternachtsblau, Graphit, Waldgrün und Hoher Kontrast,
- getrennte Schriftgrößen für Oberfläche sowie Code/Konsole,
- kompakte, normale oder großzügige Abstände,
- einstellbare Breite des Datei-Explorers,
- optional stärker hervorgehobene Rahmen und Sekundärtexte.

Nach **`Übernehmen`** wird die Oberfläche automatisch mit den neuen Werten aufgebaut. Die Auswahl wird ausschließlich lokal in `config\ui-settings.properties` gespeichert. Temporäre Java-Dateien, Einstellungen und Caches der mitgelieferten Startskripte verbleiben unter `KR_BUILD\.cache` auf demselben Laufwerk wie die IDE.

---

### 2. Projekt-Vorlagen & KR-Architektur

Über das Menü **`Projekt -> Neues Projekt erstellen …`** können Sie folgende Vorlagen sofort generieren:
1. **`MODULAR_KR_FORGE`**: Vollständiges modulares Referenzprojekt mit `forge.krproject`, `main.kr`, `app/`, `gui/`, `research/`, `compute/`, `orchestration/`, `storage/`.
2. **`SINGLE_KR`**: Einzeldatei-KR-Programm (`main.kr`).
3. **`CPP_PROJECT`**: C++20-Projekt mit `CMakeLists.txt` und `main.cpp`.
4. **`RUST_PROJECT`**: Rust-Projekt mit `Cargo.toml` und `src/main.rs`.
5. **`GO_PROJECT`**: Go-Projekt mit `go.mod` und `main.go`.

---

## 📦 Schnellstart

### Ausführen
Doppelklicken Sie im Ordner `windows\` auf:
- **`Start-KR-IDE.bat`**: Startet die IDE lautlos als reine Desktop-Anwendung.
- **`Start-KR-IDE-Console.bat`**: Startet die IDE mit sichtbarem Terminal-Fenster (für Entwickler & Debugging).

Die Distribution enthält ihre eigene Java-Laufzeit unter `runtime\jdk` und lädt
beim Start weder JDK noch Compiler nach. Alternativ direkt per Befehlszeile:
```cmd
runtime\jdk\bin\java.exe -jar dist\KR-Compiler-IDE.jar
```

### Selbst neu kompilieren
Führen Sie im Ordner `windows\` folgendes Skript aus:
```cmd
build.bat
```
Das Skript kompiliert alle Java-Dateien mit `javac` und erzeugt die ausführbare `dist\KR-Compiler-IDE.jar`.

---

## 🏗️ Verzeichnisstruktur

```
windows/
├── src/
│   ├── kr/compile/
│   │   ├── KrTokenType.java        # KR Lexer Token-Typen
│   │   ├── KrToken.java            # Token-Definition
│   │   ├── KrLexer.java            # Lexikalischer Scanner
│   │   ├── KrParser.java           # AST-Grammatik-Parser
│   │   ├── KrType.java             # Typsystem
│   │   ├── KrSegment.java          # Sprach- und Modulsegmentierung
│   │   ├── KrFunction.java         # Funktions-Definitionen
│   │   ├── KrLanguage.java         # Mehrsprachigkeits-Unterstützung
│   │   ├── KrSymbolTable.java      # Symboltabelle & Scope-Verwaltung
│   │   ├── KrDiagnostic.java       # Syntax- und Typfehler
│   │   ├── KrCodeGenerator.java    # Multi-Target Code-Generator (C++20, Rust, Go)
│   │   ├── KrPipeline.java         # Multi-File Modul-Pipeline
│   │   ├── KrProjectConfig.java    # TOML-Parser für *.krproject
│   │   ├── DiagnosticParser.java   # Compiler-Fehler-Parser
│   │   ├── DocumentModel.java      # Dokumenten-Modell
│   │   ├── DocumentManager.java    # Tab- & Dateiverwaltung
│   │   ├── ProjectManager.java     # Projektverwaltung & Vorlagen
│   │   ├── ArtifactsManager.java   # Artefakt-Scanner & Starter
│   │   ├── CompilerEngine.java     # Nativer Desktop-Compiler (Clang, Rust, Go, CMake)
│   │   ├── BuildManager.java       # Asynchroner Build-Manager
│   │   ├── VcpkgManager.java       # vcpkg Paket-Verwaltung
│   │   ├── KrAgentCore.java        # Autonomer KI-Programmieragent (ReAct)
│   │   └── windows/
│   │       ├── KrWindowsIde.java         # Haupt-Anwendungsfenster (GUI)
│   │       ├── CodeEditorPanel.java      # Multi-Tab Quellcode-Editor
│   │       ├── ProjectExplorerPanel.java # Projekt-Dateibaum mit Kontextmenü
│   │       ├── ExplorerIcons.java         # Skalierbare Ordner- und Dateityp-Symbole
│   │       ├── BuildConsolePanel.java    # Build-Konsole & Plattform-Wahl
│   │       ├── ArtifactsPanel.java       # Artefakte-Tabelle mit Ausführen-Button
│   │       ├── AgentDialog.java          # KI-Agenten-Dialog mit Export & Kopieren
│   │       ├── UiSettings.java           # Designs, Schriftgrößen und Anzeigeeinstellungen
│   │       └── SyntaxHighlighting.java   # Syntax-Hervorhebung
│   └── org/json/                   # Autarkes, abhängigkeitsfreies JSON-Paket
├── bin/                            # Kompilierte .class-Dateien
├── dist/
│   └── KR-Compiler-IDE.jar         # Fertig gepacktes, ausführbares Programm
├── Start-KR-IDE.bat                # Windows-Starter
├── Start-KR-IDE-Console.bat        # Windows-Starter mit Konsole
├── build.bat                       # Re-Build-Skript
├── Manifest.txt                    # JAR-Manifest
└── README.md                       # Diese Dokumentation
```
