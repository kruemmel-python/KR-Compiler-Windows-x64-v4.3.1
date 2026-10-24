import kr.compile.KrSourceMap;
import kr.compile.RunnerSigningManager;
import kr.compile.SecureProjectPaths;
import kr.compile.ToolchainManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Regression checks for project confinement, link safety, toolchain integrity and local signing. */
public final class SecurityRegressionTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("H:-Testverzeichnis als einziges Argument angeben.");
        Path testRoot = Path.of(args[0]).toAbsolutePath().normalize();
        require(testRoot.getRoot() != null && "H:\\".equalsIgnoreCase(testRoot.getRoot().toString()),
                "Sicherheitstest darf nur auf H: arbeiten: " + testRoot);
        Files.createDirectories(testRoot);
        Path project = testRoot.resolve("project");
        Path external = testRoot.resolve("external");
        Files.createDirectories(project.resolve("src"));
        Files.createDirectories(external);
        Files.writeString(project.resolve("src/main.kr"), "fn main() {}\n", StandardCharsets.UTF_8);
        Files.writeString(external.resolve("secret.txt"), "must-stay-private\n", StandardCharsets.UTF_8);

        File normal = SecureProjectPaths.resolveExisting(project.toFile(), "src/main.kr");
        require(normal.isFile(), "Normale Projektdatei muss auflösbar sein.");
        reject(() -> SecureProjectPaths.resolveExisting(project.toFile(), "../external/secret.txt"), "Elternpfad");
        reject(() -> SecureProjectPaths.resolveExisting(project.toFile(), external.resolve("secret.txt").toString()), "Absolutpfad");
        reject(() -> SecureProjectPaths.resolveForWrite(project.toFile(), "src/../../external/overwrite.txt"), "Schreibpfad");

        KrSourceMap unsafeMap = new KrSourceMap();
        unsafeMap.addMapping(new KrSourceMap.SegmentMapping(
                "modules/main.cpp", "../external/secret.txt", 1, 2, 1, 2, "CPP", List.of("main")));
        String mapped = unsafeMap.resolveOriginalFile("modules/main.cpp", "main");
        require(mapped == null || mapped.isEmpty(), "Unsicheres Source-Map-Ziel muss verworfen werden.");

        boolean linkCreated = false;
        Path link = project.resolve("KR_BUILD");
        try {
            Files.createSymbolicLink(link, external);
            linkCreated = true;
            reject(() -> SecureProjectPaths.resolveExisting(project.toFile(), "KR_BUILD/secret.txt"), "Link-Lesezugriff");
            reject(() -> SecureProjectPaths.deleteTree(project.toFile(), link.toFile()), "Link-Löschung");
            require(Files.isRegularFile(external.resolve("secret.txt")), "Externe Datei darf niemals gelöscht werden.");
        } catch (UnsupportedOperationException | IOException ex) {
            if (linkCreated) throw ex;
            System.out.println("SKIP Link-Erzeugung vom Betriebssystem verweigert: " + ex.getMessage());
        }

        ToolchainManager manager = ToolchainManager.getInstance();
        require(manager.isEmbeddedToolchainPresent(), "Integritätsgeprüfte Toolchain muss erkannt werden.");
        RunnerSigningManager.SigningIdentity identity = RunnerSigningManager.getOrCreate(manager.getToolchainDir());
        require(identity.keyStore().isFile() && identity.keyStore().length() > 0, "Lokaler PKCS12-Schlüssel fehlt.");
        require(identity.passwordFile().isFile() && identity.passwordFile().length() >= 32, "Lokales Signaturkennwort fehlt.");
        require(identity.keyStore().getCanonicalPath().startsWith(new File("config/security").getCanonicalPath()),
                "Signaturschlüssel muss im lokalen Installationsbereich liegen.");

        System.out.println("Security regression tests passed: " + checks + " checks.");
    }

    private static void reject(CheckedAction action, String label) throws Exception {
        try {
            action.run();
            throw new AssertionError(label + " wurde nicht abgewiesen.");
        } catch (IOException expected) {
            checks++;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
