package kr.compile.studio;

import kr.compile.CompilerEngine;
import kr.compile.DiagnosticParser;
import kr.compile.KrDiagnostic;
import kr.compile.KrFunction;
import kr.compile.KrLanguage;
import kr.compile.KrParser;
import kr.compile.KrPipeline;
import kr.compile.KrSegment;
import kr.compile.KrSymbolTable;
import kr.compile.ProjectArtifactIdentity;
import kr.compile.ToolchainManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Layer 2 & 3: KR Compiler Bridge + KR Android Build Adapter for Android Studio Rabbit 2.
 * Reuses the existing KR Compiler Engine (KrPipeline, CompilerEngine, ToolchainManager)
 * without duplicating or replacing the compiler.
 */
public final class KrStudioCompilerBridge {

    public static final String DEFAULT_KR_HOME = "H:\\KR-Compiler\\KR-Compiler-Windows-x64-v4.3.2";
    public static final String GITHUB_RELEASE_URL = "https://github.com/kruemmel-python/KR-Compiler-Windows-x64-v4.3.1/releases";

    static {
        ensureToolchainConfigured();
    }

    private static File getConfigFile() {
        return new File(System.getProperty("user.home", "."), ".kr-studio-config.properties");
    }

    public static void saveCustomKrHome(File krHomeDir) {
        if (krHomeDir == null || !krHomeDir.isDirectory()) return;
        try {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("kr.compiler.home", krHomeDir.getAbsolutePath());
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(getConfigFile())) {
                props.store(out, "KR Unified Language Android Studio Plugin Configuration");
            }
            System.setProperty("kr.compiler.home", krHomeDir.getAbsolutePath());
            ToolchainManager.getInstance().setCustomCompilerHome(krHomeDir);
        } catch (Exception ignored) {}
    }

    public static File resolveKrCompilerHome() {
        // 1. Gespeicherte Benutzereinstellung (~/.kr-studio-config.properties)
        File cfg = getConfigFile();
        if (cfg.isFile()) {
            try (java.io.FileInputStream in = new java.io.FileInputStream(cfg)) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                String saved = props.getProperty("kr.compiler.home");
                if (saved != null && !saved.isBlank()) {
                    File f = new File(saved.trim());
                    if (new File(f, "toolchain").isDirectory()) return f;
                }
            } catch (Exception ignored) {}
        }

        // 2. Umgebungsvariable KR_COMPILER_HOME oder System-Property
        String propHome = System.getProperty("kr.compiler.home");
        if (propHome != null && !propHome.isBlank() && new File(propHome, "toolchain").isDirectory()) {
            return new File(propHome);
        }
        String envHome = System.getenv("KR_COMPILER_HOME");
        if (envHome != null && !envHome.isBlank() && new File(envHome, "toolchain").isDirectory()) {
            return new File(envHome);
        }

        // 3. Bekannte Standard-Installationspfade pruefen
        String userHome = System.getProperty("user.home", ".");
        String[] candidates = {
                DEFAULT_KR_HOME,
                "C:\\KR-Compiler\\KR-Compiler-Windows-x64-v4.3.2",
                "D:\\KR-Compiler\\KR-Compiler-Windows-x64-v4.3.2",
                new File(userHome, "KR-Compiler-Windows-x64-v4.3.2").getAbsolutePath(),
                new File(userHome, "Desktop\\KR-Compiler-Windows-x64-v4.3.2").getAbsolutePath(),
                "H:\\KR-Compiler\\KR-Compiler-Windows-x64-v4.3.1",
                "C:\\KR-Compiler\\KR-Compiler-Windows-x64-v4.3.1"
        };
        for (String c : candidates) {
            File dir = new File(c);
            if (new File(dir, "toolchain").isDirectory()) {
                return dir;
            }
        }
        return new File(DEFAULT_KR_HOME);
    }

    public static void ensureToolchainConfigured() {
        File home = resolveKrCompilerHome();
        if (home.isDirectory()) {
            System.setProperty("kr.compiler.home", home.getAbsolutePath());
            ToolchainManager.getInstance().setCustomCompilerHome(home);
        }
    }

    public static final class SegmentEntry {
        public final String file;
        public final KrLanguage language;
        public final int startLine;
        public final int endLine;
        public final String functionsSummary;
        public final String crossCallsSummary;
        public final boolean abiExported;

        public SegmentEntry(String file, KrLanguage language, int startLine, int endLine,
                            String functionsSummary, String crossCallsSummary, boolean abiExported) {
            this.file = file;
            this.language = language;
            this.startLine = startLine;
            this.endLine = endLine;
            this.functionsSummary = functionsSummary;
            this.crossCallsSummary = crossCallsSummary;
            this.abiExported = abiExported;
        }
    }

    public static final class AnalysisReport {
        public final File projectDir;
        public final List<File> krFiles = new ArrayList<>();
        public final List<KrSegment> rawSegments = new ArrayList<>();
        public final List<SegmentEntry> segmentEntries = new ArrayList<>();
        public final List<DiagnosticParser.Item> diagnostics = new ArrayList<>();
        public final KrSymbolTable symbolTable = new KrSymbolTable();

        public AnalysisReport(File projectDir) {
            this.projectDir = projectDir;
        }
    }

    public static AnalysisReport analyzeProject(File projectDir) {
        ensureToolchainConfigured();
        AnalysisReport report = new AnalysisReport(projectDir);
        if (projectDir == null || !projectDir.isDirectory()) return report;

        List<File> files = KrPipeline.findKrFiles(projectDir);
        report.krFiles.addAll(files);

        for (File f : files) {
            try {
                String rel = projectDir.toPath().relativize(f.toPath()).toString().replace('\\', '/');
                String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                KrParser parser = new KrParser(rel, content);
                List<KrSegment> segs = parser.parse();
                report.rawSegments.addAll(segs);

                for (KrDiagnostic d : parser.getDiagnostics()) {
                    report.diagnostics.add(new DiagnosticParser.Item(
                            d.file, d.line, d.column,
                            d.severity == KrDiagnostic.Severity.ERROR ? DiagnosticParser.Severity.ERROR : DiagnosticParser.Severity.WARNING,
                            d.message, d.toString()));
                }
                for (KrSegment seg : segs) {
                    for (KrFunction fn : seg.declaredFunctions) {
                        report.symbolTable.registerFunction(fn);
                    }
                }
            } catch (Exception ex) {
                report.diagnostics.add(new DiagnosticParser.Item(
                        f.getName(), 1, 1, DiagnosticParser.Severity.ERROR,
                        "Fehler beim Lesen der .kr-Datei: " + ex.getMessage(), ex.toString()));
            }
        }

        report.symbolTable.resolveCalls(report.rawSegments);
        for (KrDiagnostic d : report.symbolTable.getDiagnostics()) {
            report.diagnostics.add(new DiagnosticParser.Item(
                    d.file, d.line, d.column,
                    d.severity == KrDiagnostic.Severity.ERROR ? DiagnosticParser.Severity.ERROR : DiagnosticParser.Severity.WARNING,
                    d.message, d.toString()));
        }

        for (KrSegment seg : report.rawSegments) {
            StringBuilder fnSb = new StringBuilder();
            boolean anyExported = false;
            for (int i = 0; i < seg.declaredFunctions.size(); i++) {
                KrFunction fn = seg.declaredFunctions.get(i);
                if (i > 0) fnSb.append(", ");
                fnSb.append(fn.name).append("(");
                for (int p = 0; p < fn.parameters.size(); p++) {
                    if (p > 0) fnSb.append(", ");
                    fnSb.append(fn.parameters.get(p).type);
                }
                fnSb.append(")->").append(fn.returnType);
                if (report.symbolTable.isExported(fn.name)) anyExported = true;
            }
            if (fnSb.length() == 0) fnSb.append("<Top-Level / Direktiven>");

            StringBuilder callSb = new StringBuilder();
            for (String called : seg.calledFunctionNames) {
                KrFunction target = report.symbolTable.getSymbols().get(called);
                if (target != null && target.definingLanguage != seg.language) {
                    if (callSb.length() > 0) callSb.append(", ");
                    callSb.append(called).append(" [").append(target.definingLanguage.getDisplayName()).append("]");
                }
            }
            if (callSb.length() == 0) callSb.append("—");

            report.segmentEntries.add(new SegmentEntry(
                    seg.file, seg.language, seg.startLine, seg.endLine,
                    fnSb.toString(), callSb.toString(), anyExported));
        }

        return report;
    }

    public static File buildProject(
            File projectDir,
            CompilerEngine.Target target,
            CompilerEngine.Logger logger,
            List<DiagnosticParser.Item> outDiagnostics) throws Exception {

        ensureToolchainConfigured();
        AnalysisReport analysis = analyzeProject(projectDir);
        if (outDiagnostics != null) {
            outDiagnostics.addAll(analysis.diagnostics);
        }

        CompilerEngine engine = new CompilerEngine(projectDir, target, false, line -> {
            if (logger != null) logger.line(line);
            DiagnosticParser.Item parsed = DiagnosticParser.parseLine(line);
            if (parsed != null && outDiagnostics != null) {
                DiagnosticParser.Item mapped = KrPipeline.mapDiagnostic(parsed, analysis.rawSegments);
                outDiagnostics.add(mapped);
            }
        });

        return engine.build();
    }

    public static void installAndLaunchApk(File projectDir, File apkFile, CompilerEngine.Logger logger) throws Exception {
        if (apkFile == null || !apkFile.isFile()) {
            throw new IllegalArgumentException("APK-Datei existiert nicht: " + apkFile);
        }
        String adb = findAdbExecutable();
        if (adb == null) {
            throw new IllegalStateException("Android SDK adb.exe wurde nicht gefunden.");
        }

        logger.line("[ADB] Prüfe verbundene Android-Geräte über: " + adb);
        List<String> devices = runSimpleCommand(Arrays.asList(adb, "devices"), projectDir);
        String serial = null;
        for (String line : devices) {
            String trimmed = line.trim();
            if (trimmed.endsWith("\tdevice") || trimmed.matches("^\\S+\\s+device$")) {
                serial = trimmed.split("\\s+")[0];
                break;
            }
        }

        if (serial == null) {
            logger.line("[ADB] Kein aktives Android-Gerät/Emulator verbunden ('adb devices' leer).");
            logger.line("[ADB] Die signierte ARM64-APK liegt bereit unter: " + apkFile.getAbsolutePath());
            return;
        }

        logger.line("[ADB] Verbundenes Gerät erkannt: " + serial);
        logger.line("[ADB] Installiere APK: " + apkFile.getName() + " …");
        for (String l : runSimpleCommand(Arrays.asList(adb, "-s", serial, "install", "-r", apkFile.getAbsolutePath()), projectDir)) {
            logger.line("  " + l);
        }

        String pkg = ProjectArtifactIdentity.androidPackageId(projectDir);
        logger.line("[ADB] Starte Android-App-Paket '" + pkg + "' auf Gerät " + serial + " …");
        for (String l : runSimpleCommand(Arrays.asList(
                adb, "-s", serial, "shell", "monkey", "-p", pkg, "-c", "android.intent.category.LAUNCHER", "1"), projectDir)) {
            logger.line("  " + l);
        }
        logger.line("[ADB] App erfolgreich auf Gerät gestartet!");
    }

    public static String findAdbExecutable() {
        List<String> candidates = new ArrayList<>();
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            candidates.add(localAppData + "\\Android\\Sdk\\platform-tools\\adb.exe");
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            candidates.add(userHome + "\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe");
        }
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null) {
            candidates.add(androidHome + "\\platform-tools\\adb.exe");
        }
        for (String c : candidates) {
            File f = new File(c);
            if (f.isFile()) return f.getAbsolutePath();
        }
        return null;
    }

    private static List<String> runSimpleCommand(List<String> cmd, File cwd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null && cwd.isDirectory()) pb.directory(cwd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) lines.add(l);
        }
        p.waitFor();
        return lines;
    }

    /**
     * Generates `kr-android-bridge.gradle.kts` in the project directory to connect Gradle builds
     * with the KR Unified Language Pipeline and `app/src/main/jniLibs/arm64-v8a`.
     */
    public static File generateGradleBridge(File projectDir) throws Exception {
        File gradleBridge = new File(projectDir, "kr-android-bridge.gradle.kts");
        String content = "// Automatisch generierter KR Android Build Adapter fuer Android Studio Rabbit 2\n"
                + "// Verbindet Gradle mit der bestehenden KR Unified Language Pipeline (C++20, Rust, Go, Python).\n\n"
                + "val krCompilerHome = System.getenv(\"KR_COMPILER_HOME\") ?: \"H:/KR-Compiler/KR-Compiler-Windows-x64-v4.3.2\"\n\n"
                + "tasks.register<Exec>(\"compileKrUnifiedAndroidArm64\") {\n"
                + "    group = \"kr unified language\"\n"
                + "    description = \"Kompiliert alle .kr-Dateien (C++, Rust, Go, Python) ueber die KR-Pipeline nach KR_BUILD und jniLibs/arm64-v8a\"\n"
                + "    workingDir = project.rootDir\n"
                + "    commandLine(\n"
                + "        \"$krCompilerHome/runtime/jdk/bin/java.exe\",\n"
                + "        \"-Dkr.compiler.home=$krCompilerHome\",\n"
                + "        \"-cp\",\n"
                + "        \"$krCompilerHome/dist/KR-Compiler-IDE.jar\",\n"
                + "        \"kr.compile.KrCliMain\",\n"
                + "        project.rootDir.absolutePath,\n"
                + "        \"ANDROID\"\n"
                + "    )\n"
                + "}\n";
        Files.writeString(gradleBridge.toPath(), content, StandardCharsets.UTF_8);
        return gradleBridge;
    }

    /**
     * Creates a complete 4-language KR Android Studio test project (`main.kr`, `project.krproject`,
     * `kr-android-bridge.gradle.kts`, `app/src/main/jniLibs/arm64-v8a`) demonstrating C++20, Rust, Go,
     * and Python working together via KR language fusion.
     */
    public static File createFourLanguageDemoProject(File targetDir) throws Exception {
        if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
            throw new IllegalStateException("Zielordner konnte nicht erstellt werden: " + targetDir);
        }

        File jniArm64 = new File(targetDir, "app/src/main/jniLibs/arm64-v8a");
        jniArm64.mkdirs();

        String krProjectToml = "[project]\n"
                + "name = \"" + targetDir.getName() + "\"\n"
                + "version = \"1.0.0\"\n"
                + "edition = \"2026\"\n"
                + "python_android_strategy = \"AOT_NATIVE_CPP20_BRIDGE\"\n\n"
                + "[build]\n"
                + "optimization = \"O3\"\n"
                + "warnings = \"all\"\n";
        Files.writeString(new File(targetDir, "project.krproject").toPath(), krProjectToml, StandardCharsets.UTF_8);

        String mainKr = "// ============================================================================\n"
                + "// KR Unified Language 4-Sprachen-Projekt fuer Android Studio Rabbit 2\n"
                + "// Vereint C++20 (Native), Rust (Rules), Go (Services) und Python (Logic)\n"
                + "// in einer einzigen .kr-Quelldatei mit gemeinsamer ABI-Sprachfusion.\n"
                + "// ============================================================================\n\n"
                + "#include <iostream>\n"
                + "#include <cstdint>\n\n"
                + "// 1. [RUST] Sicherheits- und Regelpruefung (kompiliert via Cargo -> libkr_rust.a)\n"
                + "pub fn rust_security_rule(seed: i32, factor: i32) -> i32 {\n"
                + "    let mut acc: i32 = seed * factor;\n"
                + "    if acc < 100 {\n"
                + "        acc = acc + 42;\n"
                + "    }\n"
                + "    acc\n"
                + "}\n\n"
                + "// 2. [GO] Service-Orchestrierung (kompiliert via Go -> libkr_go.so / libkr_go.a)\n"
                + "package main\n\n"
                + "func go_dispatch_service(ruleScore int32, nodes int32) int32 {\n"
                + "    var total int32 = ruleScore + (nodes * 15)\n"
                + "    return total\n"
                + "}\n\n"
                + "# 3. [PYTHON] Intelligente Auswertungslogik (Android-Strategie: AOT_NATIVE_CPP20_BRIDGE)\n"
                + "# Ruft zudem sprachuebergreifend die Rust-Funktion rust_security_rule() auf!\n"
                + "def python_evaluate_logic(service_val: int32, bonus: int32) -> int32:\n"
                + "    verified = rust_security_rule(bonus, 3)\n"
                + "    combined = service_val + verified\n"
                + "    if combined > 200:\n"
                + "        print(\"[KR-PYTHON] Hoher Score erreicht:\", combined)\n"
                + "        return combined + 50\n"
                + "    else:\n"
                + "        print(\"[KR-PYTHON] Standard-Score:\", combined)\n"
                + "        return combined\n\n"
                + "// 4. [C++20] Nativer Haupteinstiegspunkt & Android/Desktop-Koordinator\n"
                + "int main(int argc, char** argv) {\n"
                + "    (void)argc; (void)argv;\n"
                + "    std::cout << \"=== KR Unified Language 4-Sprachen-Fusion (C++20 + Rust + Go + Python) ===\" << std::endl;\n"
                + "    int32_t rustVal = rust_security_rule(20, 4);\n"
                + "    std::cout << \"[1/4 RUST]   rust_security_rule(20, 4)        = \" << rustVal << std::endl;\n"
                + "    int32_t goVal = go_dispatch_service(rustVal, 6);\n"
                + "    std::cout << \"[2/4 GO]     go_dispatch_service(\" << rustVal << \", 6)     = \" << goVal << std::endl;\n"
                + "    int32_t pyVal = python_evaluate_logic(goVal, 10);\n"
                + "    std::cout << \"[3/4 PYTHON] python_evaluate_logic(\" << goVal << \", 10)  = \" << pyVal << std::endl;\n"
                + "    std::cout << \"[4/4 C++20]  Gesamtergebnis der 4-Sprachen-Fusion = \" << pyVal << std::endl;\n"
                + "    return (pyVal == 334) ? 0 : 1;\n"
                + "}\n";

        File mainKrFile = new File(targetDir, "main.kr");
        Files.writeString(mainKrFile.toPath(), mainKr, StandardCharsets.UTF_8);
        generateGradleBridge(targetDir);
        ensureAndroidStudioGradleProjectFiles(targetDir);
        return mainKrFile;
    }

    /**
     * Ensures that a folder can be opened directly in Android Studio ("File -> Open...") as a complete
     * Android Gradle project with MainActivity + JNI integration to libuser.so.
     */
    public static void ensureAndroidStudioGradleProjectFiles(File targetDir) throws Exception {
        File settingsGradle = new File(targetDir, "settings.gradle.kts");
        if (!settingsGradle.isFile()) {
            String settingsContent = "pluginManagement {\n"
                    + "    repositories {\n"
                    + "        google()\n"
                    + "        mavenCentral()\n"
                    + "        gradlePluginPortal()\n"
                    + "    }\n"
                    + "}\n"
                    + "dependencyResolutionManagement {\n"
                    + "    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n"
                    + "    repositories {\n"
                    + "        google()\n"
                    + "        mavenCentral()\n"
                    + "    }\n"
                    + "}\n\n"
                    + "rootProject.name = \"" + targetDir.getName() + "\"\n"
                    + "include(\":app\")\n";
            Files.writeString(settingsGradle.toPath(), settingsContent, StandardCharsets.UTF_8);
        }

        File rootBuildGradle = new File(targetDir, "build.gradle.kts");
        if (!rootBuildGradle.isFile()) {
            String rootBuildContent = "// Root build.gradle.kts fuer KR Unified Language Android Studio Projekt\n"
                    + "plugins {\n"
                    + "    id(\"com.android.application\") version \"8.13.2\" apply false\n"
                    + "}\n\n"
                    + "apply(from = \"kr-android-bridge.gradle.kts\")\n";
            Files.writeString(rootBuildGradle.toPath(), rootBuildContent, StandardCharsets.UTF_8);
        }

        File gradleProps = new File(targetDir, "gradle.properties");
        if (!gradleProps.isFile()) {
            String propsContent = "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n"
                    + "android.useAndroidX=true\n"
                    + "android.nonTransitiveRClass=true\n";
            Files.writeString(gradleProps.toPath(), propsContent, StandardCharsets.UTF_8);
        }

        File localProps = new File(targetDir, "local.properties");
        if (!localProps.isFile()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            String sdkPath = (localAppData != null ? localAppData + "\\Android\\Sdk" : "C:\\Users\\ralfk\\AppData\\Local\\Android\\Sdk")
                    .replace("\\", "\\\\").replace(":", "\\:");
            Files.writeString(localProps.toPath(), "sdk.dir=" + sdkPath + "\n", StandardCharsets.UTF_8);
        }

        File appDir = new File(targetDir, "app");
        appDir.mkdirs();
        File appBuildGradle = new File(appDir, "build.gradle.kts");
        if (!appBuildGradle.isFile()) {
            String appBuildContent = "plugins {\n"
                    + "    id(\"com.android.application\")\n"
                    + "}\n\n"
                    + "android {\n"
                    + "    namespace = \"kr.compile.studio.demo4lang\"\n"
                    + "    compileSdk = 35\n\n"
                    + "    defaultConfig {\n"
                    + "        applicationId = \"kr.compile.studio.demo4lang\"\n"
                    + "        minSdk = 26\n"
                    + "        targetSdk = 35\n"
                    + "        versionCode = 1\n"
                    + "        versionName = \"4.3.2\"\n\n"
                    + "        ndk {\n"
                    + "            abiFilters += listOf(\"arm64-v8a\")\n"
                    + "        }\n"
                    + "    }\n\n"
                    + "    compileOptions {\n"
                    + "        sourceCompatibility = JavaVersion.VERSION_17\n"
                    + "        targetCompatibility = JavaVersion.VERSION_17\n"
                    + "    }\n"
                    + "}\n\n"
                    + "// Automatischer KR 4-Sprachen-Vorlauf vor dem Android-Build:\n"
                    + "tasks.matching { it.name == \"preBuild\" }.configureEach {\n"
                    + "    dependsOn(rootProject.tasks.named(\"compileKrUnifiedAndroidArm64\"))\n"
                    + "}\n";
            Files.writeString(appBuildGradle.toPath(), appBuildContent, StandardCharsets.UTF_8);
        }

        File manifestFile = new File(targetDir, "app/src/main/AndroidManifest.xml");
        manifestFile.getParentFile().mkdirs();
        if (!manifestFile.isFile()) {
            String manifestXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                    + "    <application\n"
                    + "        android:label=\"KR 4-Sprachen Demo (C++/Rust/Go/Python)\"\n"
                    + "        android:theme=\"@android:style/Theme.DeviceDefault.NoActionBar\">\n"
                    + "        <activity\n"
                    + "            android:name=\".MainActivity\"\n"
                    + "            android:exported=\"true\">\n"
                    + "            <intent-filter>\n"
                    + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                    + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                    + "            </intent-filter>\n"
                    + "        </activity>\n"
                    + "    </application>\n"
                    + "</manifest>\n";
            Files.writeString(manifestFile.toPath(), manifestXml, StandardCharsets.UTF_8);
        }

        File activityFile = new File(targetDir, "app/src/main/java/kr/compile/studio/demo4lang/MainActivity.java");
        activityFile.getParentFile().mkdirs();
        if (!activityFile.isFile()) {
            String activityJava = "package kr.compile.studio.demo4lang;\n\n"
                    + "import android.app.Activity;\n"
                    + "import android.graphics.Color;\n"
                    + "import android.graphics.Typeface;\n"
                    + "import android.os.Bundle;\n"
                    + "import android.widget.LinearLayout;\n"
                    + "import android.widget.ScrollView;\n"
                    + "import android.widget.TextView;\n\n"
                    + "/**\n"
                    + " * Android Studio Activity fuer die KR Unified Language 4-Sprachen-Fusion\n"
                    + " * (C++20 + Rust + Go + Python in main.kr -> libuser.so & libkr_go.so).\n"
                    + " */\n"
                    + "public class MainActivity extends Activity {\n\n"
                    + "    private static String loadError = null;\n\n"
                    + "    static {\n"
                    + "        try {\n"
                    + "            System.loadLibrary(\"c++_shared\");\n"
                    + "            System.loadLibrary(\"kr_go\");\n"
                    + "            System.loadLibrary(\"user\");\n"
                    + "        } catch (Throwable t) {\n"
                    + "            loadError = t.toString();\n"
                    + "        }\n"
                    + "    }\n\n"
                    + "    public native int nativeRunKrMain();\n\n"
                    + "    @Override\n"
                    + "    protected void onCreate(Bundle savedInstanceState) {\n"
                    + "        super.onCreate(savedInstanceState);\n\n"
                    + "        ScrollView scroll = new ScrollView(this);\n"
                    + "        scroll.setBackgroundColor(Color.parseColor(\"#101824\"));\n"
                    + "        LinearLayout layout = new LinearLayout(this);\n"
                    + "        layout.setOrientation(LinearLayout.VERTICAL);\n"
                    + "        layout.setPadding(48, 64, 48, 64);\n\n"
                    + "        TextView title = new TextView(this);\n"
                    + "        title.setText(\"KR Unified Language v4.3.2\\n4-Sprachen-Fusion (ARM64)\");\n"
                    + "        title.setTextColor(Color.parseColor(\"#00E5A3\"));\n"
                    + "        title.setTextSize(22f);\n"
                    + "        title.setTypeface(Typeface.DEFAULT_BOLD);\n"
                    + "        layout.addView(title);\n\n"
                    + "        TextView body = new TextView(this);\n"
                    + "        body.setTextColor(Color.parseColor(\"#E6EDF3\"));\n"
                    + "        body.setTextSize(15f);\n"
                    + "        body.setTypeface(Typeface.MONOSPACE);\n"
                    + "        body.setPadding(0, 36, 0, 0);\n\n"
                    + "        if (loadError != null) {\n"
                    + "            body.setText(\"[FEHLER beim Laden von libuser.so]\\n\" + loadError);\n"
                    + "        } else {\n"
                    + "            int exitCode = nativeRunKrMain();\n"
                    + "            body.setText(\n"
                    + "                \"Quelle: main.kr\\n\"\n"
                    + "                + \"----------------------------------------\\n\"\n"
                    + "                + \"[1/4 RUST]   rust_security_rule(20, 4)   = 122\\n\"\n"
                    + "                + \"[2/4 GO]     go_dispatch_service(122, 6) = 212\\n\"\n"
                    + "                + \"[3/4 PYTHON] python_evaluate_logic       = 334\\n\"\n"
                    + "                + \"             (inkl. Cross-Call -> Rust)\\n\"\n"
                    + "                + \"[4/4 C++20]  Exit-Code aus main.kr       = \" + exitCode + \"\\n\"\n"
                    + "                + \"----------------------------------------\\n\"\n"
                    + "                + \"Python-Strategie: AOT_NATIVE_CPP20_BRIDGE\\n\"\n"
                    + "                + \"Status: \" + (exitCode == 0 ? \"ERFOLGREICH VERIFIZIERT (334)\" : \"FEHLER\")\n"
                    + "            );\n"
                    + "        }\n\n"
                    + "        layout.addView(body);\n"
                    + "        scroll.addView(layout);\n"
                    + "        setContentView(scroll);\n"
                    + "    }\n"
                    + "}\n";
            Files.writeString(activityFile.toPath(), activityJava, StandardCharsets.UTF_8);
        }
    }
}
