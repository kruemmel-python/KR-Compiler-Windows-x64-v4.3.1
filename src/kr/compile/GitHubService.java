package kr.compile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Git and GitHub operations used by the desktop UI. Credentials are never persisted. */
public final class GitHubService {
    public record Repository(String cloneUrl, String htmlUrl) {}

    private static final String TOKEN_PERMISSION_HINT = "Für den vollständigen Ablauf benötigt ein Fine-grained Token "
            + "'Administration: Read and write' und 'Contents: Read and write'; ein klassischer Token benötigt "
            + "'repo' beziehungsweise bei ausschließlich öffentlichen Repositories 'public_repo'.";
    private static final String GENERATED_IGNORE_MARKER = "# KR Compiler IDE: generated and local files v2";
    private static final String GENERATED_IGNORE_BLOCK = """
            # KR Compiler IDE: generated and local files v2
            /KR_BUILD/
            **/KR_BUILD/
            /bin/
            /build/
            /dist/
            /target/
            **/target/
            /.gradle/
            **/.gradle/
            /.idea/
            **/.idea/
            /toolchain/
            /runtime/
            /tests/project-matrix-work*/
            /config/security/
            /config/ui-settings.properties
            **/_package_staging/
            *.class
            *.p12
            *.jks
            *.keystore
            *.pk8
            *.password
            """;

    private GitHubService() {}

    public static File cloneRepository(String repositoryUrl, File parentDirectory) throws Exception {
        if (parentDirectory == null || !parentDirectory.isDirectory()) {
            throw new IOException("Ungültiger Zielordner.");
        }
        String url = normalizeRepositoryUrl(repositoryUrl);
        String folderName = repositoryName(url);
        File destination = new File(parentDirectory, folderName);
        if (destination.exists()) {
            throw new IOException("Der Zielordner existiert bereits: " + destination.getAbsolutePath());
        }
        run(parentDirectory, "clone", "--progress", url);
        if (!destination.isDirectory()) {
            throw new IOException("Git hat keinen Projektordner erstellt.");
        }
        return destination;
    }

    public static Repository createRepository(String token, String name, String description, boolean isPrivate) throws Exception {
        if (token == null || token.isBlank()) throw new IOException("Ein GitHub Personal Access Token ist erforderlich.");
        if (!name.matches("[A-Za-z0-9._-]+")) throw new IOException("Der Repository-Name enthält ungültige Zeichen.");

        JSONObject data = new JSONObject()
                .put("name", name)
                .put("description", description == null ? "" : description)
                .put("private", isPrivate)
                .put("auto_init", false);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/user/repos"))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(data.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = githubClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message;
            try { message = new JSONObject(response.body()).optString("message", response.body()); }
            catch (Exception ignored) { message = response.body(); }
            if (response.statusCode() == 422) {
                Repository existing = findOwnedEmptyRepository(token, name);
                if (existing != null) return existing;
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IOException("GitHub lehnt den Token ab (HTTP " + response.statusCode()
                        + "). " + TOKEN_PERMISSION_HINT + " Antwort: " + message);
            }
            throw new IOException("GitHub konnte das Repository nicht anlegen (HTTP " + response.statusCode() + "): " + message);
        }
        JSONObject result = new JSONObject(response.body());
        return new Repository(result.getString("clone_url"), result.optString("html_url", ""));
    }

    /**
     * Vollständiger, wiederaufnehmbarer Veröffentlichungsablauf. Erst wenn der
     * lokale Commit stabil ist, wird das entfernte Repository angelegt.
     */
    public static Repository publishNewProject(File projectDir, String token, String name, String description,
                                               boolean isPrivate, String authorName, String authorEmail) throws Exception {
        prepareLocalRepository(projectDir, authorName, authorEmail);

        List<String> tokens = availableTokens(token);
        if (tokens.isEmpty()) throw new IOException("Kein GitHub-Token gefunden. Melden Sie sich mit 'gh auth login' an oder geben Sie einen Token ein.");
        Exception lastAuthenticationFailure = null;
        for (String candidate : tokens) {
            try {
                return publishPreparedProject(projectDir, candidate, name, description, isPrivate);
            } catch (Exception ex) {
                if (!isAuthenticationFailure(ex)) throw ex;
                lastAuthenticationFailure = ex;
            }
        }
        throw new IOException("Kein verfügbarer GitHub-Token konnte dieses Repository veröffentlichen. "
                + TOKEN_PERMISSION_HINT + " Git-Meldung: " + safeMessage(lastAuthenticationFailure), lastAuthenticationFailure);
    }

    private static Repository publishPreparedProject(File projectDir, String token, String name, String description,
                                                      boolean isPrivate) throws Exception {
        String currentRemote = tryRun(projectDir, "remote", "get-url", "origin");
        Repository repository;
        if (currentRemote != null && !currentRemote.isBlank()) {
            String normalized = normalizeRepositoryUrl(currentRemote.trim());
            repository = new Repository(normalized, toHtmlUrl(normalized));
        } else {
            repository = createRepository(token, name, description, isPrivate);
            run(projectDir, "remote", "add", "origin", repository.cloneUrl());
        }

        try {
            runWithBearer(projectDir, token, "push", "-u", "origin", "HEAD:refs/heads/main");
        } catch (Exception ex) {
            throw new IOException("Der lokale Commit ist fertig, aber der Push ist fehlgeschlagen. "
                    + "Sie können 'Auf GitHub veröffentlichen' erneut ausführen. " + TOKEN_PERMISSION_HINT
                    + " Git-Meldung: " + ex.getMessage(), ex);
        }
        return repository;
    }

    public static void publishProject(File projectDir, Repository repository, String authorName, String authorEmail, String token) throws Exception {
        prepareLocalRepository(projectDir, authorName, authorEmail);
        String currentRemote = tryRun(projectDir, "remote", "get-url", "origin");
        if (currentRemote == null || currentRemote.isBlank()) {
            run(projectDir, "remote", "add", "origin", repository.cloneUrl());
        }
        List<String> tokens = availableTokens(token);
        if (tokens.isEmpty()) throw new IOException("Kein GitHub-Token gefunden. Melden Sie sich mit 'gh auth login' an oder geben Sie einen Token ein.");
        Exception lastAuthenticationFailure = null;
        for (String candidate : tokens) {
            try {
                runWithBearer(projectDir, candidate, "push", "-u", "origin", "HEAD:refs/heads/main");
                return;
            } catch (Exception ex) {
                if (!isAuthenticationFailure(ex)) throw ex;
                lastAuthenticationFailure = ex;
            }
        }
        throw new IOException("Kein verfügbarer GitHub-Token konnte das Projekt hochladen. "
                + TOKEN_PERMISSION_HINT + " Git-Meldung: " + safeMessage(lastAuthenticationFailure), lastAuthenticationFailure);
    }

    /** Uses locally configured credentials first; entered tokens are a safe fallback and are never stored. */
    private static List<String> availableTokens(String enteredToken) {
        List<String> systemTokens = new ArrayList<>();
        addToken(systemTokens, System.getenv("GH_TOKEN"));
        addToken(systemTokens, System.getenv("GITHUB_TOKEN"));
        addToken(systemTokens, readGitHubCliToken());
        addToken(systemTokens, readGitCredentialToken());
        return prioritizeTokens(systemTokens, enteredToken);
    }

    static List<String> prioritizeTokens(List<String> systemTokens, String enteredToken) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (systemTokens != null) for (String token : systemTokens) addToken(unique, token);
        addToken(unique, enteredToken);
        return new ArrayList<>(unique);
    }

    private static void addToken(java.util.Collection<String> destination, String token) {
        if (token != null && !token.isBlank()) destination.add(token.trim());
    }

    private static String readGitHubCliToken() {
        return runCredentialCommand(List.of("gh", "auth", "token", "--hostname", "github.com"), null);
    }

    private static String readGitCredentialToken() {
        String response = runCredentialCommand(List.of("git", "credential", "fill"), "protocol=https\nhost=github.com\n\n");
        if (response == null) return null;
        for (String line : response.split("\\R")) {
            if (line.startsWith("password=")) return line.substring("password=".length()).trim();
        }
        return null;
    }

    private static String runCredentialCommand(List<String> command, String input) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
            configureTemporaryDirectory(builder, null);
            builder.environment().put("GIT_TERMINAL_PROMPT", "0");
            builder.environment().put("GCM_INTERACTIVE", "Never");
            Process process = builder.start();
            if (input != null) {
                process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
            }
            process.getOutputStream().close();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) return null;
            return output.isBlank() ? null : output;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isAuthenticationFailure(Exception exception) {
        String message = safeMessage(exception).toLowerCase(Locale.ROOT);
        return message.contains("authentication failed") || message.contains("invalid credentials")
                || message.contains("http 401") || message.contains("http 403") || message.contains("error 401")
                || message.contains("error 403") || message.contains("requested url returned error: 401")
                || message.contains("requested url returned error: 403");
    }

    private static String safeMessage(Exception exception) {
        return exception == null || exception.getMessage() == null ? "Unbekannter Fehler." : exception.getMessage();
    }

    static void prepareLocalRepository(File projectDir, String authorName, String authorEmail) throws Exception {
        if (projectDir == null || !projectDir.isDirectory()) throw new IOException("Kein gültiges Projekt geöffnet.");
        File gitDir = new File(projectDir, ".git");
        boolean initializedHere = !gitDir.exists();
        if (gitDir.exists() && !gitDir.isDirectory()) throw new IOException(".git ist kein gültiges Verzeichnis.");
        try {
            if (initializedHere) run(projectDir, "init", "-b", "main");
            ensureGeneratedFilesIgnored(projectDir);
            configureIdentity(projectDir, authorName, authorEmail);
            addWithStabilityRetry(projectDir);

            String staged = run(projectDir, "diff", "--cached", "--name-only");
            if (!staged.isBlank()) {
                String message = tryRun(projectDir, "rev-parse", "--verify", "HEAD") == null
                        ? "Initial commit from KR Compiler IDE"
                        : "Update from KR Compiler IDE";
                run(projectDir, "commit", "-m", message);
            } else if (tryRun(projectDir, "rev-parse", "--verify", "HEAD") == null) {
                throw new IOException("Das Projekt enthält nach Anwendung der Sicherheitsfilter keine veröffentlichbaren Dateien.");
            }
        } catch (Exception ex) {
            if (initializedHere && gitDir.exists()) {
                try { SecureProjectPaths.deleteTree(projectDir, gitDir); } catch (Exception ignored) {}
            }
            throw ex;
        }
    }

    private static void ensureGeneratedFilesIgnored(File projectDir) throws IOException {
        File ignore = SecureProjectPaths.resolveForWrite(projectDir, ".gitignore");
        String existing = ignore.isFile() ? Files.readString(ignore.toPath(), StandardCharsets.UTF_8) : "";
        if (existing.contains(GENERATED_IGNORE_MARKER)) return;
        StringBuilder updated = new StringBuilder(existing);
        if (!existing.isEmpty() && !existing.endsWith("\n") && !existing.endsWith("\r")) updated.append(System.lineSeparator());
        if (!existing.isEmpty()) updated.append(System.lineSeparator());
        updated.append(GENERATED_IGNORE_BLOCK.replace("\n", System.lineSeparator()));
        Files.writeString(ignore.toPath(), updated.toString(), StandardCharsets.UTF_8);
    }

    private static void configureIdentity(File projectDir, String authorName, String authorEmail) throws Exception {
        String name = authorName == null ? "" : authorName.trim();
        String email = authorEmail == null ? "" : authorEmail.trim();
        if (name.isBlank()) name = tryRun(projectDir, "config", "user.name");
        if (email.isBlank()) email = tryRun(projectDir, "config", "user.email");
        if (name == null || name.isBlank()) name = "KR Compiler IDE";
        if (email == null || email.isBlank()) email = "noreply@kr-compiler.local";
        run(projectDir, "config", "user.name", name);
        run(projectDir, "config", "user.email", email);
    }

    private static void addWithStabilityRetry(File projectDir) throws Exception {
        IOException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                run(projectDir, "add", "-A", "--", ".");
                return;
            } catch (IOException ex) {
                last = ex;
                String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
                if (!message.contains("unstable object source data") || attempt == 3) throw ex;
                TimeUnit.MILLISECONDS.sleep(250L * attempt);
            }
        }
        throw last == null ? new IOException("Git konnte die Projektdateien nicht stabil einlesen.") : last;
    }

    private static Repository findOwnedEmptyRepository(String token, String name) throws Exception {
        String login = authenticatedLogin(token);
        if (login == null || login.isBlank()) return null;
        URI uri = URI.create("https://api.github.com/repos/" + login + "/" + name);
        HttpRequest request = githubRequest(uri, token).GET().build();
        HttpResponse<String> response = githubClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) return null;
        JSONObject repository = new JSONObject(response.body());
        JSONObject owner = repository.optJSONObject("owner");
        if (owner == null || !login.equalsIgnoreCase(owner.optString("login"))) return null;
        String cloneUrl = repository.optString("clone_url", "");
        if (cloneUrl.isBlank()) return null;
        String refs = execute(null, token, "ls-remote", cloneUrl);
        if (!refs.isBlank()) {
            throw new IOException("Ein Repository mit diesem Namen existiert bereits und ist nicht leer. "
                    + "Bitte wählen Sie einen anderen Namen oder öffnen Sie das vorhandene Repository.");
        }
        return new Repository(cloneUrl, repository.optString("html_url", toHtmlUrl(cloneUrl)));
    }

    private static String authenticatedLogin(String token) throws Exception {
        HttpRequest request = githubRequest(URI.create("https://api.github.com/user"), token).GET().build();
        HttpResponse<String> response = githubClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) return null;
        return new JSONObject(response.body()).optString("login", "");
    }

    private static HttpRequest.Builder githubRequest(URI uri, String token) {
        return HttpRequest.newBuilder(uri)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(java.time.Duration.ofSeconds(30));
    }

    private static HttpClient githubClient() {
        return HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private static String toHtmlUrl(String cloneUrl) {
        String value = cloneUrl == null ? "" : cloneUrl.trim();
        if (value.startsWith("git@github.com:")) value = "https://github.com/" + value.substring("git@github.com:".length());
        if (value.endsWith(".git")) value = value.substring(0, value.length() - 4);
        return value;
    }

    public static String checkGit() {
        try { return run(null, "--version"); }
        catch (Exception ex) { return null; }
    }

    private static String normalizeRepositoryUrl(String value) throws IOException {
        String url = value == null ? "" : value.trim();
        if (url.startsWith("github.com/")) url = "https://" + url;
        if (!url.matches("(https://github\\.com/|git@github\\.com:).+/.+")) {
            throw new IOException("Bitte geben Sie eine GitHub-Repository-URL ein.");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String repositoryName(String url) throws IOException {
        String last = url.substring(Math.max(url.lastIndexOf('/'), url.lastIndexOf(':')) + 1);
        if (last.endsWith(".git")) last = last.substring(0, last.length() - 4);
        if (last.isBlank() || ".".equals(last) || "..".equals(last)) throw new IOException("Repository-Name konnte nicht bestimmt werden.");
        return last;
    }

    private static String run(File directory, String... gitArgs) throws Exception {
        return execute(directory, null, gitArgs);
    }

    private static String tryRun(File directory, String... gitArgs) {
        try { return run(directory, gitArgs); }
        catch (Exception ignored) { return null; }
    }

    private static String runWithBearer(File directory, String token, String... gitArgs) throws Exception {
        if (token == null || token.isBlank()) throw new IOException("Ein GitHub Token ist erforderlich.");
        return execute(directory, token, gitArgs);
    }

    private static String execute(File directory, String bearerToken, String... gitArgs) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(gitArgs));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (directory != null) builder.directory(directory);
        configureTemporaryDirectory(builder, directory);
        if (bearerToken != null) {
            builder.environment().put("GIT_CONFIG_COUNT", "1");
            builder.environment().put("GIT_CONFIG_KEY_0", "http.extraHeader");
            builder.environment().put("GIT_CONFIG_VALUE_0", gitAuthorizationHeader(bearerToken));
        }
        Process process = builder.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        process.getInputStream().transferTo(out);
        int exit = process.waitFor();
        String output = out.toString(StandardCharsets.UTF_8).trim();
        if (exit != 0) throw new IOException(output.isBlank() ? "Git-Befehl fehlgeschlagen." : output);
        return output;
    }

    /** GitHub's Git-over-HTTPS endpoint requires the PAT as the Basic-auth password. */
    static String gitAuthorizationHeader(String token) {
        String credentials = "x-access-token:" + (token == null ? "" : token.trim());
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Authorization: Basic " + encoded;
    }

    private static void configureTemporaryDirectory(ProcessBuilder builder, File directory) {
        try {
            File base = directory != null ? directory : ToolchainManager.getApplicationDirectory();
            if (base == null || !base.isDirectory()) return;
            File temp;
            if (directory != null) {
                temp = SecureProjectPaths.resolveForWrite(base, "KR_BUILD/.cache/github");
                SecureProjectPaths.createParentDirectories(base, new File(temp, ".keep"));
                if (!temp.exists()) Files.createDirectory(temp.toPath());
            } else {
                temp = new File(ToolchainManager.getApplicationDirectory(), "KR_BUILD/.cache/github");
                Files.createDirectories(temp.toPath());
            }
            builder.environment().put("TEMP", temp.getAbsolutePath());
            builder.environment().put("TMP", temp.getAbsolutePath());
        } catch (Exception ignored) {
            // Die Startskripte setzen TEMP/TMP ebenfalls auf den H:-Arbeitsbereich.
        }
    }
}
