package kr.compile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Central, fail-closed validation for all paths originating from a project. */
public final class SecureProjectPaths {
    private static final LinkOption[] NO_FOLLOW = {LinkOption.NOFOLLOW_LINKS};

    private SecureProjectPaths() {}

    public static File resolveExisting(File projectRoot, String relativePath) throws IOException {
        Path root = trustedRoot(projectRoot);
        Path candidate = resolveRelative(root, relativePath);
        verifyExistingPath(root, candidate);
        if (!Files.exists(candidate, NO_FOLLOW)) {
            throw new IOException("Projektpfad existiert nicht: " + relativePath);
        }
        return candidate.toFile();
    }

    public static File resolveExistingCandidate(File projectRoot, File candidate) throws IOException {
        Path root = trustedRoot(projectRoot);
        Path path = candidate.toPath().toAbsolutePath().normalize();
        requireLexicalContainment(root, path);
        verifyExistingPath(root, path);
        return path.toFile();
    }

    public static File resolveForWrite(File projectRoot, String relativePath) throws IOException {
        Path root = trustedRoot(projectRoot);
        Path candidate = resolveRelative(root, relativePath);
        verifyExistingParents(root, candidate);
        if (Files.exists(candidate, NO_FOLLOW)) {
            verifyExistingPath(root, candidate);
        }
        return candidate.toFile();
    }

    public static void createParentDirectories(File projectRoot, File target) throws IOException {
        Path root = trustedRoot(projectRoot);
        Path path = target.toPath().toAbsolutePath().normalize();
        requireLexicalContainment(root, path);
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("Zieldatei besitzt kein Elternverzeichnis.");
        }
        Path current = root;
        for (Path part : root.relativize(parent)) {
            current = current.resolve(part);
            if (Files.exists(current, NO_FOLLOW)) {
                rejectLinkOrSpecial(current);
                if (!Files.isDirectory(current, NO_FOLLOW)) {
                    throw new IOException("Pfadkomponente ist kein Verzeichnis: " + current);
                }
            } else {
                Files.createDirectory(current);
            }
        }
        verifyExistingPath(root, parent);
    }

    public static List<File> collectRegularFiles(File projectRoot) throws IOException {
        Path root = trustedRoot(projectRoot);
        List<File> result = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                rejectLinkOrSpecial(dir);
                verifyRealContainment(root, dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                rejectLinkOrSpecial(file);
                verifyRealContainment(root, file);
                if (attrs.isRegularFile()) {
                    result.add(file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(result, (left, right) -> left.getAbsolutePath().compareTo(right.getAbsolutePath()));
        return result;
    }

    public static void deleteTree(File trustedProjectRoot, File target) throws IOException {
        Path root = trustedRoot(trustedProjectRoot);
        Path path = target.toPath().toAbsolutePath().normalize();
        requireLexicalContainment(root, path);
        if (path.equals(root)) {
            throw new IOException("Das Projektwurzelverzeichnis darf nicht gelöscht werden.");
        }
        if (!Files.exists(path, NO_FOLLOW)) {
            return;
        }
        verifyExistingPath(root, path);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                rejectLinkOrSpecial(dir);
                verifyRealContainment(root, dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                rejectLinkOrSpecial(file);
                verifyRealContainment(root, file);
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String relativeName(File projectRoot, File file) throws IOException {
        Path root = trustedRoot(projectRoot);
        Path path = file.toPath().toAbsolutePath().normalize();
        requireLexicalContainment(root, path);
        verifyExistingPath(root, path);
        return root.relativize(path).toString().replace(File.separatorChar, '/');
    }

    public static String relativeNameForWrite(File projectRoot, File file) throws IOException {
        Path root = trustedRoot(projectRoot);
        Path path = file.toPath().toAbsolutePath().normalize();
        requireLexicalContainment(root, path);
        verifyExistingParents(root, path);
        return root.relativize(path).toString().replace(File.separatorChar, '/');
    }

    private static Path trustedRoot(File projectRoot) throws IOException {
        if (projectRoot == null) throw new IOException("Projektwurzel fehlt.");
        Path root = projectRoot.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(root, NO_FOLLOW)) {
            throw new IOException("Ungültige Projektwurzel: " + root);
        }
        rejectLinkOrSpecial(root);
        return root.toRealPath(LinkOption.NOFOLLOW_LINKS);
    }

    private static Path resolveRelative(Path root, String relativePath) throws IOException {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IOException("Leerer Projektpfad.");
        }
        String value = relativePath.trim();
        if (value.startsWith("/") || value.startsWith("\\") || value.matches("^[A-Za-z]:.*")) {
            throw new IOException("Absolute Projektpfade sind nicht erlaubt: " + relativePath);
        }
        for (String component : value.split("[\\\\/]+")) {
            if (component.isEmpty() || component.equals(".") || component.equals("..")) {
                throw new IOException("Unsichere Pfadkomponente: " + relativePath);
            }
        }
        Path relative = Paths.get(value.replace('\\', File.separatorChar).replace('/', File.separatorChar));
        if (relative.isAbsolute()) {
            throw new IOException("Absolute Projektpfade sind nicht erlaubt: " + relativePath);
        }
        Path candidate = root.resolve(relative).normalize();
        requireLexicalContainment(root, candidate);
        return candidate;
    }

    private static void verifyExistingParents(Path root, Path candidate) throws IOException {
        Path parent = candidate.getParent();
        if (parent == null) throw new IOException("Projektpfad besitzt kein Elternverzeichnis.");
        Path current = root;
        for (Path part : root.relativize(parent)) {
            current = current.resolve(part);
            if (!Files.exists(current, NO_FOLLOW)) break;
            rejectLinkOrSpecial(current);
            verifyRealContainment(root, current);
            if (!Files.isDirectory(current, NO_FOLLOW)) {
                throw new IOException("Pfadkomponente ist kein Verzeichnis: " + current);
            }
        }
    }

    private static void verifyExistingPath(Path root, Path candidate) throws IOException {
        requireLexicalContainment(root, candidate);
        Path current = root;
        for (Path part : root.relativize(candidate)) {
            current = current.resolve(part);
            if (!Files.exists(current, NO_FOLLOW)) {
                throw new IOException("Projektpfad existiert nicht: " + current);
            }
            rejectLinkOrSpecial(current);
            verifyRealContainment(root, current);
        }
    }

    private static void rejectLinkOrSpecial(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attrs.isSymbolicLink() || attrs.isOther()) {
            throw new IOException("Symbolische Links, Junctions und Spezialdateien sind nicht erlaubt: " + path);
        }
    }

    private static void verifyRealContainment(Path root, Path path) throws IOException {
        Path real = path.toRealPath();
        if (!real.startsWith(root)) {
            throw new IOException("Projektpfad verlässt den Arbeitsbereich: " + path);
        }
    }

    private static void requireLexicalContainment(Path root, Path candidate) throws IOException {
        if (!candidate.startsWith(root)) {
            throw new IOException("Projektpfad verlässt den Arbeitsbereich: " + candidate);
        }
    }
}
