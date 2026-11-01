# KR Compiler IDE

**KR Unified Language für C++20, Rust, Go und Python**
Windows x64 · Linux x64 · Android ARM64 · Android Studio · Visual Studio 2022 · Antigravity

KR Compiler IDE ist eine Entwicklungsumgebung und Build-Pipeline für KR-Projekte. Eine `.kr`-Datei kann Bereiche aus C++20, Rust, Go und Python zusammenführen. Die KR-Pipeline erzeugt die nötigen ABI-Brücken und übergibt die jeweiligen Sprachanteile an die vorhandenen nativen Toolchains. Die Entscheidung, welche Sprache verwendet wird, bleibt beim Entwickler.

> Aktuelle stabile Ausgabe: **v4.3.2** · [Downloads und Prüfsummen](https://github.com/kruemmel-python/KR-Compiler-Windows-x64-v4.3.1/releases/tag/v4.3.2)

## Was ist enthalten?

| Bereich | Funktion |
| --- | --- |
| KR Pipeline | Multi-Datei-Analyse, Sprachsegmentierung, ABI-Generierung und Fehlerzuordnung |
| Windows IDE | Swing-Desktop-IDE mit Editor, Projekt-Explorer, Build-Konsole, Artefakten und Agenten-Dialog |
| Plattform-Builds | Windows x64 (`.exe`), Linux x64 (ELF) und Android ARM64 (APK/Native Libraries) |
| Android Studio | `.kr`-Dateityp, Syntaxhervorhebung, Diagnosen, Tool-Window und Android-Build-Adapter |
| Visual Studio 2022 | Syntaxhervorhebung, Live-Diagnosen, KR-Buildfenster sowie Konsolen- und GUI-Projektvorlage |
| Antigravity | Agenten-Workflow zum Prüfen, Bauen und gezielten Reparieren von KR-Projekten |

## Downloads

Alle Dateien gehören zum [Release v4.3.2](https://github.com/kruemmel-python/KR-Compiler-Windows-x64-v4.3.1/releases/tag/v4.3.2).

| Download | Zweck |
| --- | --- |
| `KR-Compiler-Windows-x64-v4.3.2.zip` | Vollständige portable Windows-IDE mit Laufzeit und Toolchain |
| `KR-Compiler-Android-IDE-v4.3.2.apk` | Signierte Android-IDE für ARM64 |
| `KR-Compiler-Linux-x64-v4.3.2.tar.xz.part-000` bis `002` | Vollständige portable Linux-Ausgabe, in GitHub-taugliche Teile aufgeteilt |
| `KR-Unified-Language-AndroidStudio-Plugin-v4.3.2.zip` | Android-Studio-Plugin (`Install Plugin from Disk…`) |
| `KR-Unified-Language-Visual-Studio-2022-v1.0.4.vsix` | Visual-Studio-2022-Erweiterung |
| `KR-Compile-IDE-Plugin-for-Antigravity-v1.0.1.zip` | Antigravity-Plugin |
| `KR-Compiler-IDE-und-Plugins-Dokumentation-v4.3.2.zip` | Bebildertes Handbuch für IDE und Plugins |

Die Linux-Teile werden vor dem Entpacken wieder zusammengesetzt. Die genaue Anleitung steht in `README-LINUX-REASSEMBLY-v4.3.2.txt`; die Dateien `*.sha256` enthalten die Prüfsummen.

## Schnellstart: Windows

1. `KR-Compiler-Windows-x64-v4.3.2.zip` herunterladen und beispielsweise nach `C:\KR-Compiler\KR-Compiler-Windows-x64-v4.3.2` entpacken.
2. `Start-KR-IDE.bat` starten, oder für Diagnoseausgaben `Start-KR-IDE-Console.bat`.
3. Über **Projekt → Neues Projekt erstellen** eine Vorlage anlegen oder einen bestehenden Projektordner öffnen.
4. Das Build-Ziel auswählen und mit **Build / F5** kompilieren.

Die Distribution bringt ihre Java-Laufzeit unter `runtime\jdk` sowie die zugehörige Toolchain unter `toolchain\` mit. Es ist kein global installiertes JDK nötig.

```cmd
runtime\jdk\bin\java.exe -jar dist\KR-Compiler-IDE.jar
```

### Eigener Build aus dem Repository

```cmd
build.bat
```

Das Skript erstellt `dist\KR-Compiler-IDE.jar`. Für die Android-Studio-Erweiterung steht zusätzlich `build-android-studio-plugin.ps1` bereit.

## Plugins

### Android Studio

1. Die Windows-IDE v4.3.2 entpacken.
2. In Android Studio: **Settings → Plugins → ⚙ → Install Plugin from Disk…**.
3. `KR-Unified-Language-AndroidStudio-Plugin-v4.3.2.zip` auswählen und Android Studio neu starten.
4. Im Tool-Window **KR Unified Language** den KR-Engine-Pfad wählen oder `KR_COMPILER_HOME` setzen.

Das Plugin bevorzugt `KR-Compiler-Windows-x64-v4.3.2` und akzeptiert v4.3.1 weiterhin als Rückfallpfad.

### Visual Studio 2022

`KR-Unified-Language-Visual-Studio-2022-v1.0.4.vsix` ausführen. Anschließend stehen im Dialog **Neues Projekt erstellen** die Vorlagen **KR Unified Console Application** und **KR Unified GUI Application** bereit. Die Erweiterung enthält außerdem Syntaxhervorhebung, Diagnosen und ein KR-Build-Toolfenster.

### Antigravity

`KR-Compile-IDE-Plugin-for-Antigravity-v1.0.1.zip` gemäß der enthaltenen `INSTALLATION.md` installieren. Der Workflow verwendet den KR-Compiler für Prüfung, Build und Reparaturhinweise. Beispiele und Regeln befinden sich im Paket unter `skills/kr-compile` und `rules/`.

## KR-Projekte

Ein KR-Projekt verwendet mindestens eine `.kr`-Datei; für Projektmetadaten kann eine `.krproject`-Datei ergänzt werden. Die IDE bietet Vorlagen für einzelne KR-Dateien, modulare KR-Projekte sowie reine C++-, Rust- und Go-Projekte.

Ein vollständiges Android-Beispiel mit allen vier Sprachbereichen liegt unter [examples/KR_ANDROID_STUDIO_4LANG_DEMO](examples/KR_ANDROID_STUDIO_4LANG_DEMO).

## Architektur

```text
.kr / .krproject
      │
      ▼
KR Analyse, Sprachsegmentierung und ABI-Brücke
      │
      ├── C++20 ──► native C++-Toolchain
      ├── Rust  ──► Cargo / rustc
      ├── Go    ──► Go-Compiler
      └── Python ─► AOT_NATIVE_CPP20_BRIDGE (plattformabhängig)
      │
      ▼
Windows EXE · Linux ELF · Android ARM64 APK
```

KR ersetzt die jeweiligen Backend-Compiler nicht. Es koordiniert die von Ihnen gewählten Sprachanteile, ihre Schnittstellen und den plattformspezifischen Build.

## Dokumentation

- [Vollständiges bebildertes Handbuch](docs/KR-Compiler-IDE-Handbuch.md)
- [Android-Studio-Plugin-Dokumentation](DOKUMENTATION-Android-Studio-Plugin.md)
- [Android-Studio-Handbuch](KR_Android_Studio_Handbuch.md)
- [Änderungen in v4.3.2](RELEASE_NOTES-v4.3.2.md)

## Repository-Struktur

```text
src/                         KR-Kern und Windows-Oberfläche
android-studio-plugin/       Quellcode und Ressourcen des Android-Studio-Plugins
examples/                    Beispielprojekte
docs/                        Handbuch und Screenshots
dist/                        Gebaute JARs und Plugin-Pakete
runtime/                     Mitgelieferte Java-Laufzeit
toolchain/                   Portable Compiler-Toolchain
```

## Lizenz und Autor

Entwickelt von **Ralf Krümmel**. Hinweise zu Nutzung, Installation und Release-Dateien sind in den jeweiligen Paketdokumentationen enthalten.
