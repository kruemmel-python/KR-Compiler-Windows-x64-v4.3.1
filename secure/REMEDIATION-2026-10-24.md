# Sicherheitsreparaturen vom 24.10.2026

## Ergebnis

Alle 18 bestätigten Befunde aus `findings.json` wurden an der jeweiligen
Sicherheitsgrenze behoben. Zusätzlich wurden vier Plattformfehler repariert,
die der vollständige Projekt-Matrixlauf sichtbar gemacht hat. Die Windows-,
Linux- und Android-Quellen wurden synchronisiert. Temporäre Dateien und Caches
der Prüfung lagen ausschließlich unter `H:\KR-Compiler`.

## Behobene Befunde

| Nr. | Befund | Umsetzung und Nachweis |
|---:|---|---|
| 1 | Eingebetteter Android-Privatschlüssel | Export und Asset-Verteilung wurden entfernt. Android-Runner werden mit einem nicht exportierbaren RSA-3072-Schlüssel aus dem Android Keystore signiert. Desktop-Runner verwenden eine zufällig erzeugte lokale PKCS12-Identität unter `config/security`, die nicht ausgeliefert wird. IDE- und Runner-Zertifikat waren im Test verschieden; das Release-APK enthielt null Schlüssel-/Keystore-/Kennwort-Assets. Alte Schlüssel gelten als kompromittiert und müssen bei einer offiziellen Veröffentlichung rotiert werden. |
| 2 | QASM-Stacküberlauf | Der Parser lehnt Gate-Tokens ab 64 Byte vor jedem Schreibzugriff ab. Der native Regressionstest deckt den überlangen Token ab. |
| 3 | Agent-Pfadausbruch | `SecureProjectPaths` erzwingt relative Pfade, kanonische Root-Begrenzung, `NOFOLLOW_LINKS` und eine erneute Prüfung unmittelbar vor Lese- und Schreibzugriffen. Source-Map, Namenssuche, Diagnosepfade und Fallbacks benutzen denselben Resolver. |
| 4 | GitHub-ZIP-Slip | ZIP-Namen werden zuerst auf `/` normalisiert. Absolute Pfade, Laufwerkspräfixe, NUL, leere Bestandteile, `.` und `..` werden abgewiesen; jedes Ziel muss kanonisch unter dem Zielordner liegen. Anzahl und entpackte Gesamtgröße sind begrenzt. Teilweise Git-/ZIP-Importe werden vor dem Fallback beziehungsweise bei Fehlern sicher entfernt. |
| 5 | Symlink-Löschung des Buildordners | Rekursives Löschen erfolgt über einen Link-verweigernden Dateibaum-Lauf mit Root- und Realpfadprüfung. Die Projektwurzel selbst kann nicht gelöscht werden. |
| 6 | Negativer Mycel-Feedback-Zähler | Negative Zähler werden vor Schleife und Konvertierung abgewiesen; Arraygrenzen werden zusätzlich intern geprüft. |
| 7 | Ungültige Mycel-Checkpoints | Checkpoints werden vollständig in temporäre Strukturen gelesen. Dimensionen, Zähler, Indizes, Leseergebnisse, Freiliste, Alive-Zustände und Nachbarbeziehungen werden vor dem atomaren Zustandswechsel validiert. |
| 8 | Simulierte VRAM-Rohadressen | Die öffentliche API benutzt undurchsichtige Handles aus einer synchronisierten Allokationstabelle. Unbekannte, bereits freigegebene oder außerhalb liegende Bereiche werden abgewiesen. |
| 9 | Verkürzte Agent-Freigabe | Die Freigabe zeigt vollständigen Alt- und Neuinhalt sowie Pfad, Byteanzahl und beide SHA-256-Werte. Unmittelbar vor dem Commit werden Existenz und alter Hash erneut geprüft. |
| 10 | Veralteter SAF-Arbeitsbereich | Jeder Import erhält einen UUID-Arbeitsbereich. Alte Arbeitsbereiche werden vor dem Import link-sicher gelöscht; ein Fehlschlag beendet den Vorgang geschlossen. |
| 11 | Agent über Klartext-HTTP | Remote-Endpunkte benötigen HTTPS. HTTP ist ausschließlich tokenlos für die exakte Loopback-Schnittstelle erlaubt; Redirects sind aus. Android sperrt Klartext global und erlaubt in der Netzwerkkonfiguration nur localhost/127.0.0.1. |
| 12 | Symlink-Leck im Runner-Paket | Projektdateien werden ausschließlich durch `SecureProjectPaths.collectRegularFiles` gesammelt; Links und Dateien außerhalb des kanonischen Projektroots werden nicht gelesen. |
| 13 | Unverifizierte Toolchain | Windows-Archive benötigen einen separat bezogenen SHA-256-Wert und werden erst nach Prüfung gestaged. Der Termux-Paketindex benötigt einen extern festgelegten SHA-256-Wert; danach wird jedes Paket gegen den Hash aus diesem verifizierten Index geprüft. SDL, Ubuntu-Sysroot und JDK besitzen feste SHA-256-Werte. Kritische installierte Toolchain-Dateien werden beim IDE-Start gegen eingebettete Hashes geprüft. |
| 14 | Desktop-Offline-Schalter wirkungslos | Der Schalter setzt Cargo `--offline`, CMake `FETCHCONTENT_FULLY_DISCONNECTED`, Gradle `--offline`, `GOPROXY=off`, `GOSUMDB=off` und getrennte Projekt-Caches. |
| 15 | Quantum-Shiftüberlauf | Eine zentrale Dimensionsfunktion prüft Schichtbreite, Potenz-von-zwei und ein praktisches Speicherlimit. Alle betroffenen Hostpfade verwenden sie und prüfen Gate-Indizes gegen die tatsächliche Dimension. |
| 16 | Mycel-Allokationsüberlauf | Element- und Byteanzahlen benutzen geprüfte Multiplikation/Addition und ein 512-MiB-Limit. Jede Allokation wird vor der ersten Verwendung geprüft. |
| 17 | CTC-Integerüberlauf | `L_b > (INT_MAX-1)/2` wird abgewiesen; DP- und Bytegrößen werden geprüft berechnet und begrenzt. |
| 18 | Android-Onlinezugriff standardmäßig an | Feld- und Preference-Default sind `false`. Erst eine sichtbare, gespeicherte Benutzerfreigabe aktiviert Online-Pakete. |

## Zusätzlich im Matrixlauf gefundene Projektfehler

1. Ein korrekt erzeugtes Linux-ELF wurde bei aufeinanderfolgenden Builds durch
   eine ältere Windows-PE-Datei überschrieben. Die Fallback-Kopie greift jetzt
   nur noch, wenn kein Linux-Artefakt existiert.
2. `ANDROID_NDK_HOME` erkannte bisher nur einen Ordner mit mehreren
   NDK-Versionen, nicht eine konkrete NDK-Installation. Beide Formen werden nun
   unterstützt.
3. `apksigner` benötigt bei gemeinsamem Store-/Key-Passwort zwei Eingabezeilen.
   Die lokale Kennwortdatei wird entsprechend sicher erzeugt beziehungsweise
   migriert.
4. Rust-Linux-Artefakte ohne `.exe` wurden trotz erfolgreichem Cross-Build nicht
   gefunden. Die Artefaktsuche erkennt nun ELF-Magic-Bytes und Shared Objects.

## Reproduzierbare Prüfungen

Alle folgenden Arbeitsverzeichnisse müssen auf H: liegen:

```powershell
$env:TEMP='H:\KR-Compiler\_temp'
$env:TMP='H:\KR-Compiler\_temp'
$env:GRADLE_USER_HOME='H:\KR-Compiler\_gradle'
$env:ANDROID_USER_HOME='H:\KR-Compiler\_android-home'
```

Durchgeführte Ergebnisse:

- Windows-Desktopquellen: Java-Kompilierung erfolgreich.
- Linux-Desktopquellen: Java-Kompilierung erfolgreich.
- Android-App und Runner: Release-Kompilierung erfolgreich (76 Tasks).
- Android-Release: APK-Signatur erfolgreich verifiziert; keine privaten
  Schlüssel-/Keystore-/Kennwortdateien im Archiv.
- Native Windows-Bibliothek: neu gebaut; Regressionstest für Handles,
  Double-Free, QASM-Grenze und CTC-Grenze erfolgreich.
- Native Linux-Bibliothek: neu gebaut; Magic `7F 45 4C 46`, ELF64 little-endian,
  Maschine `0x003E` (x86-64).
- Projektpfad-/Signaturtest: 13 von 13 Prüfungen erfolgreich.
- Projektmatrix: 15 von 15 Builds erfolgreich.

Projektmatrix:

| Projektvorlage | Windows x64 | Linux x64 | Android ARM64 |
|---|---|---|---|
| Modular KR Forge | PE bestanden | ELF bestanden | APK bestanden |
| Einzelne KR-Datei | PE bestanden | ELF bestanden | APK bestanden |
| C++-Projekt | PE bestanden | ELF bestanden | APK bestanden |
| Rust-Projekt | PE bestanden | ELF bestanden | APK bestanden |
| Go-Projekt | PE bestanden | ELF bestanden | APK bestanden |

Jedes Matrixprojekt enthielt mindestens 5.000 generierte Quellcodezeilen. Der
dauerhafte Ergebnisnachweis liegt neben diesem Bericht als
`matrix-results-2026-10-24.txt`.

## Schlüsselrotation und Veröffentlichung

- Der ehemals eingebettete Schlüssel darf nicht mehr verwendet werden.
- Ein offizieller IDE-Release-Schlüssel muss außerhalb des Repositorys und der
  Build-Artefakte verwahrt werden.
- Bei Google Play ist die vorgesehene App-Signing-Key-Upgrade-/Reset-Prozedur zu
  verwenden. Außerhalb eines Stores ist bei nicht mehr sicher rotierbarer
  Signatur eine neue Paketidentität erforderlich.
- Desktop-Runner-Schlüssel entstehen erst auf der Zielinstallation. Wird
  `config/security` bewusst gelöscht, entsteht beim nächsten Android-Build eine
  neue Identität; vorhandene Runner benötigen dann eine bewusste Neuinstallation.
- Android-Runner-Schlüssel liegen nicht exportierbar im Android Keystore und
  werden durch Löschen der IDE-App-Daten rotiert.

## Audit-Grenzen

Die Reparaturprüfung deckt die 18 gemeldeten Pfade und die zugehörigen
plattformübergreifenden Kopien ab. Android-Keystore-Verhalten auf realer
Hardware wurde statisch und durch erfolgreichen Android-Build geprüft, aber in
dieser Windows-Umgebung nicht auf einem physischen Android-Gerät ausgeführt.
Die Offline-Schalter unterbinden die Paketmanagerpfade kooperativ; gegen bewusst
bösartige beliebige Build-Skripte ist zusätzlich eine Betriebssystem-Sandbox
ohne Netzwerk erforderlich.
