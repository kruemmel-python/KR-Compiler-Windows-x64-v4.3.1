package kr.compile;

import java.io.File;
import java.util.Locale;

/**
 * Headless CLI entry point for invoking the KR Unified Language Compiler Pipeline
 * from Gradle tasks, Android Studio, or command-line scripts.
 */
public final class KrCliMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Verwendung: java -cp KR-Compiler-IDE.jar kr.compile.KrCliMain <Projektordner> [WINDOWS|LINUX|ANDROID]");
            System.exit(2);
        }

        File projectDir = new File(args[0]).getCanonicalFile();
        CompilerEngine.Target target = CompilerEngine.Target.ANDROID;
        if (args.length >= 2) {
            String t = args[1].trim().toUpperCase(Locale.ROOT);
            if (t.startsWith("WIN")) target = CompilerEngine.Target.WINDOWS;
            else if (t.startsWith("LIN")) target = CompilerEngine.Target.LINUX;
            else target = CompilerEngine.Target.ANDROID;
        }

        CompilerEngine engine = new CompilerEngine(projectDir, target, false, System.out::println);
        File artifact = engine.build();
        System.out.println("[KR-CLI] Artefakt erfolgreich erstellt: " + artifact.getAbsolutePath());
    }
}
