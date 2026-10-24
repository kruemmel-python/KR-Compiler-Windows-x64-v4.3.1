import kr.compile.CompilerEngine;
import kr.compile.ProjectManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Creates every IDE template, injects 5,000+ source lines and builds every target. */
public final class ProjectMatrixTest {
    private static final int STRESS_LINES = 5_000;

    public static void main(String[] args) throws Exception {
        File root = new File(args.length == 0 ? "tests/project-matrix-work" : args[0]).getCanonicalFile();
        if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("Testordner kann nicht erstellt werden: " + root);
        List<String> results = new ArrayList<>();
        for (ProjectManager.Template template : ProjectManager.Template.values()) {
            File project = ProjectManager.getInstance().createProject(root, "stress_" + template.name().toLowerCase(), template);
            writeStressProgram(project, template);
            for (CompilerEngine.Target target : CompilerEngine.Target.values()) {
                try {
                    File artifact = new CompilerEngine(project, target, false, line -> {}).build();
                    verifyArtifact(artifact, target);
                    results.add("PASS " + template + " / " + target + " -> " + artifact.getName());
                } catch (Exception ex) {
                    results.add("FAIL " + template + " / " + target + " -> " + ex.getMessage());
                }
            }
        }
        Files.writeString(new File(root, "matrix-results.txt").toPath(), String.join(System.lineSeparator(), results) + System.lineSeparator(), StandardCharsets.UTF_8);
        results.forEach(System.out::println);
        long failed = results.stream().filter(r -> r.startsWith("FAIL")).count();
        if (failed != 0) throw new IllegalStateException(failed + " Matrix-Build(s) fehlgeschlagen; Details in matrix-results.txt");
    }

    private static void writeStressProgram(File project, ProjectManager.Template template) throws Exception {
        switch (template) {
            case CPP_PROJECT -> Files.writeString(new File(project, "main.cpp").toPath(), cppProgram(), StandardCharsets.UTF_8);
            case SINGLE_KR -> Files.writeString(new File(project, "main.kr").toPath(), cppProgram(), StandardCharsets.UTF_8);
            case MODULAR_KR_FORGE -> {
                deleteKrSources(project.toPath());
                Files.writeString(new File(project, "main.kr").toPath(), cppProgram(), StandardCharsets.UTF_8);
            }
            case RUST_PROJECT -> Files.writeString(new File(project, "src/main.rs").toPath(), rustProgram(), StandardCharsets.UTF_8);
            case GO_PROJECT -> Files.writeString(new File(project, "main.go").toPath(), goProgram(), StandardCharsets.UTF_8);
        }
    }

    private static String cppProgram() {
        StringBuilder out = new StringBuilder("#include <iostream>\n");
        for (int i = 0; i < STRESS_LINES; i++) out.append("static int stress_").append(i).append("() { return ").append(i).append("; }\n");
        out.append("int main() { std::cout << stress_0() + stress_").append(STRESS_LINES - 1).append("() << '\\n'; return 0; }\n");
        return out.toString();
    }

    private static String rustProgram() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < STRESS_LINES; i++) out.append("fn stress_").append(i).append("() -> i32 { ").append(i).append(" }\n");
        out.append("fn main() { println!(\"{}\", stress_0() + stress_").append(STRESS_LINES - 1).append("()); }\n");
        return out.toString();
    }

    private static String goProgram() {
        StringBuilder out = new StringBuilder("package main\nimport \"fmt\"\n");
        for (int i = 0; i < STRESS_LINES; i++) out.append("func stress_").append(i).append("() int { return ").append(i).append(" }\n");
        out.append("func main() { fmt.Println(stress_0() + stress_").append(STRESS_LINES - 1).append("()) }\n");
        return out.toString();
    }

    private static void deleteKrSources(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".kr")).toList()) Files.delete(path);
        }
    }

    private static void verifyArtifact(File artifact, CompilerEngine.Target target) throws Exception {
        if (artifact == null || !artifact.isFile() || artifact.length() == 0) throw new IllegalStateException("Kein Artefakt erzeugt.");
        byte[] head = Files.readAllBytes(artifact.toPath());
        if (head.length < 4) throw new IllegalStateException("Artefakt ist zu klein.");
        if (target == CompilerEngine.Target.WINDOWS && !(head[0] == 'M' && head[1] == 'Z')) throw new IllegalStateException("Windows-Artefakt ist keine PE-Datei.");
        if (target == CompilerEngine.Target.LINUX && !(head[0] == 0x7f && head[1] == 'E' && head[2] == 'L' && head[3] == 'F')) throw new IllegalStateException("Linux-Artefakt ist keine ELF-Datei.");
        if (target == CompilerEngine.Target.ANDROID && !(head[0] == 'P' && head[1] == 'K')) throw new IllegalStateException("Android-Artefakt ist kein APK/ZIP.");
    }
}
