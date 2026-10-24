package kr.compile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Creates one local runner identity per IDE installation; no private key is distributed. */
public final class RunnerSigningManager {
    public static final String ALIAS = "kr-runner-local";

    public record SigningIdentity(File keyStore, File passwordFile, String alias) {}

    private RunnerSigningManager() {}

    public static synchronized SigningIdentity getOrCreate(File toolchainDir) throws IOException, InterruptedException {
        File appRoot = toolchainDir != null ? toolchainDir.getParentFile() : null;
        if (appRoot == null || !appRoot.isDirectory()) {
            throw new IOException("Installationsverzeichnis für lokale Android-Signatur nicht gefunden.");
        }
        Path root = appRoot.toPath().toAbsolutePath().normalize().toRealPath(LinkOption.NOFOLLOW_LINKS);
        rejectLink(root);
        Path config = root.resolve("config");
        createSafeDirectory(root, config);
        Path security = config.resolve("security");
        createSafeDirectory(root, security);

        Path keyStore = security.resolve("runner-signing.p12");
        Path password = security.resolve("runner-signing.password");
        if (Files.exists(keyStore, LinkOption.NOFOLLOW_LINKS) || Files.exists(password, LinkOption.NOFOLLOW_LINKS)) {
            rejectLinkIfPresent(keyStore);
            rejectLinkIfPresent(password);
            if (!Files.isRegularFile(keyStore, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(password, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Lokale Android-Signatur ist unvollständig. Bitte config/security prüfen.");
            }
            // apksigner liest bei identischem Store- und Key-Passwort zwei
            // aufeinanderfolgende Zeilen aus derselben file:-Quelle.
            List<String> passwords = Files.readAllLines(password, StandardCharsets.US_ASCII);
            if (passwords.isEmpty() || passwords.get(0).isBlank()) {
                throw new IOException("Lokales Android-Signaturkennwort ist leer.");
            }
            if (passwords.size() < 2 || !passwords.get(0).equals(passwords.get(1))) {
                String passphrase = passwords.get(0).trim();
                Files.writeString(password, passphrase + System.lineSeparator() + passphrase + System.lineSeparator(), StandardCharsets.US_ASCII);
                restrictToOwner(password.toFile());
            }
            return new SigningIdentity(keyStore.toFile(), password.toFile(), ALIAS);
        }

        byte[] random = new byte[36];
        new SecureRandom().nextBytes(random);
        String passphrase = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Files.writeString(password, passphrase + System.lineSeparator() + passphrase + System.lineSeparator(), StandardCharsets.US_ASCII);
        restrictToOwner(password.toFile());

        File keytool = new File(new File(appRoot, "runtime/jdk/bin"), isWindows() ? "keytool.exe" : "keytool");
        if (!keytool.isFile()) {
            Files.deleteIfExists(password);
            throw new IOException("Portables keytool für lokale Android-Signatur fehlt: " + keytool);
        }
        List<String> command = new ArrayList<>(List.of(
                keytool.getAbsolutePath(), "-genkeypair", "-noprompt",
                "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "3072",
                "-sigalg", "SHA256withRSA", "-validity", "3650",
                "-dname", "CN=KR Runner Local, OU=Per-Installation Key, O=KR Compiler, C=DE",
                "-storetype", "PKCS12", "-keystore", keyStore.toString(),
                "-storepass", passphrase, "-keypass", passphrase));
        ProcessBuilder builder = new ProcessBuilder(command).directory(appRoot).redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        Path build = root.resolve("KR_BUILD");
        Path cache = build.resolve(".cache");
        Path signingTemp = cache.resolve("signing-temp");
        createSafeDirectory(root, build);
        createSafeDirectory(root, cache);
        createSafeDirectory(root, signingTemp);
        File temp = signingTemp.toFile();
        env.put("TEMP", temp.getAbsolutePath());
        env.put("TMP", temp.getAbsolutePath());
        Process process = builder.start();
        byte[] output = process.getInputStream().readNBytes(64 * 1024);
        int exit = process.waitFor();
        if (exit != 0 || !Files.isRegularFile(keyStore, LinkOption.NOFOLLOW_LINKS)) {
            Files.deleteIfExists(password);
            Files.deleteIfExists(keyStore);
            throw new IOException("Lokale Android-Signatur konnte nicht erzeugt werden: " + new String(output, StandardCharsets.UTF_8));
        }
        restrictToOwner(keyStore.toFile());
        return new SigningIdentity(keyStore.toFile(), password.toFile(), ALIAS);
    }

    private static void createSafeDirectory(Path root, Path directory) throws IOException {
        if (!directory.normalize().startsWith(root)) throw new IOException("Signaturpfad verlässt die Installation.");
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            rejectLink(directory);
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Signaturpfad ist kein Verzeichnis: " + directory);
        } else {
            Files.createDirectory(directory);
        }
        if (!directory.toRealPath().startsWith(root)) throw new IOException("Signaturpfad verlässt die Installation.");
    }

    private static void rejectLinkIfPresent(Path path) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) rejectLink(path);
    }

    private static void rejectLink(Path path) throws IOException {
        if (Files.isSymbolicLink(path) || Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther()) {
            throw new IOException("Verknüpfungen sind für Signaturmaterial nicht erlaubt: " + path);
        }
    }

    private static void restrictToOwner(File file) {
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
