package kr.compile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Offline regression test for stable, resumable GitHub publishing preparation. */
public final class GitHubServiceRegressionTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("H:-Testverzeichnis fehlt.");
        Path project = Path.of(args[0]).toAbsolutePath().normalize();
        if (!project.toString().toUpperCase().startsWith("H:\\")) {
            throw new IllegalArgumentException("Testdaten dürfen ausschließlich auf H: liegen: " + project);
        }
        require("Authorization: Basic eC1hY2Nlc3MtdG9rZW46dGVzdC10b2tlbg=="
                        .equals(GitHubService.gitAuthorizationHeader("test-token")),
                "GitHub-PAT wird nicht als Basic-Authentifizierung übertragen.");
        require(GitHubService.prioritizeTokens(List.of("system-token", "system-token", "second-system-token"), "entered-token")
                        .equals(List.of("system-token", "second-system-token", "entered-token")),
                "System-Token müssen vor dem eingegebenen Token und ohne Duplikate verwendet werden.");
        Files.createDirectories(project.resolve("src"));
        Files.createDirectories(project.resolve("KR_BUILD/.cache"));
        Files.createDirectories(project.resolve("bin"));
        Files.createDirectories(project.resolve("runtime"));
        Files.createDirectories(project.resolve("toolchain"));
        Files.createDirectories(project.resolve("config/security"));
        Files.createDirectories(project.resolve("examples/sample/KR_BUILD/.cache"));
        Files.createDirectories(project.resolve("examples/rust/target/release"));
        Files.writeString(project.resolve("src/main.kr"), "fn main() {}\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve(".gitignore"), "custom-local-file.txt\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("KR_BUILD/.cache/changing.log"), "0\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("bin/generated.class"), "generated\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("runtime/local.bin"), "runtime\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("toolchain/compiler.exe"), "toolchain\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("config/security/private.p12"), "must-not-be-published\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("examples/sample/KR_BUILD/.cache/generated.bin"), "generated\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("examples/rust/target/release/program"), "generated\n", StandardCharsets.UTF_8);

        AtomicBoolean writing = new AtomicBoolean(true);
        Thread writer = new Thread(() -> {
            int value = 1;
            while (writing.get()) {
                try {
                    Files.writeString(project.resolve("KR_BUILD/.cache/changing.log"), value++ + "\n", StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
        }, "volatile-build-output-writer");
        writer.start();
        try {
            GitHubService.prepareLocalRepository(project.toFile(), "Regression Test", "test@invalid.local");
        } finally {
            writing.set(false);
            writer.join();
        }

        List<String> tracked = git(project, "ls-files").lines().filter(s -> !s.isBlank()).toList();
        require(tracked.contains("src/main.kr"), "Quelldatei fehlt im Commit.");
        require(tracked.contains(".gitignore"), ".gitignore fehlt im Commit.");
        require(tracked.stream().noneMatch(s -> s.startsWith("KR_BUILD/") || s.startsWith("bin/")
                || s.startsWith("runtime/") || s.startsWith("toolchain/") || s.contains("/KR_BUILD/")
                || s.contains("/target/") || s.endsWith(".p12")),
                "Generierte oder geheime Dateien wurden aufgenommen: " + tracked);
        String ignore = Files.readString(project.resolve(".gitignore"), StandardCharsets.UTF_8);
        require(ignore.contains("custom-local-file.txt"), "Vorhandene .gitignore-Regel wurde überschrieben.");
        require(ignore.contains("# KR Compiler IDE: generated and local files v2"), "KR-Sicherheitsregeln fehlen.");
        require(!git(project, "rev-parse", "--verify", "HEAD").isBlank(), "Initialer Commit fehlt.");

        Files.writeString(project.resolve("src/main.kr"), "fn main() { print(\"updated\") }\n", StandardCharsets.UTF_8);
        GitHubService.prepareLocalRepository(project.toFile(), "Regression Test", "test@invalid.local");
        require("2".equals(git(project, "rev-list", "--count", "HEAD").trim()), "Wiederaufnahme erzeugte keinen Update-Commit.");
        System.out.println("GitHub service regression tests passed: stable add, ignore rules, resume commit.");
    }

    private static String git(Path project, String... args) throws Exception {
        java.util.ArrayList<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true);
        builder.environment().put("TEMP", "H:\\KR-Compiler\\_temp");
        builder.environment().put("TMP", "H:\\KR-Compiler\\_temp");
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new AssertionError(output);
        return output;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
