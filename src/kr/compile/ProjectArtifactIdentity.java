package kr.compile;

import java.io.File;
import java.security.MessageDigest;
import java.util.Locale;

/** Stable, human-readable identity for artifacts created from one project. */
public final class ProjectArtifactIdentity {
    private ProjectArtifactIdentity() {}

    public static String displayName(File projectDir) {
        File descriptor = KrProjectConfig.findProjectConfigFile(projectDir);
        String fallback = projectDir != null ? projectDir.getName() : "KR_Projekt";
        return KrProjectConfig.parse(descriptor, fallback, false).name;
    }

    public static String artifactStem(File projectDir) {
        String readable = displayName(projectDir).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (readable.isEmpty()) readable = "kr-projekt";
        if (readable.length() > 48) readable = readable.substring(0, 48).replaceAll("-+$", "");
        return readable + "-" + fingerprint(projectDir).substring(0, 8);
    }

    /** Exactly 17 ASCII characters so it can safely replace the runner template package name. */
    public static String androidPackageId(File projectDir) {
        // Android requires every package segment to start with a letter.
        return "kr.p.a" + fingerprint(projectDir).substring(0, 11);
    }

    /** Runner template label has a fixed UTF-16 width; padding preserves binary XML offsets. */
    public static String androidLabel(File projectDir) {
        String label = displayName(projectDir).trim();
        final int width = "KR C++ Programm".length();
        if (label.length() > width) label = label.substring(0, width);
        StringBuilder result = new StringBuilder(label);
        while (result.length() < width) result.append(' ');
        return result.toString();
    }

    private static String fingerprint(File projectDir) {
        String path;
        try { path = projectDir.getCanonicalPath(); }
        catch (Exception ignored) { path = projectDir == null ? "" : projectDir.getAbsolutePath(); }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(path.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b));
            return out.toString();
        } catch (Exception impossible) {
            return String.format(Locale.ROOT, "%016x", path.hashCode() & 0xffffffffL) + "0000000000000000";
        }
    }
}
