package kr.compile.windows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

/** Dauerhafte, bewusst kompakt gehaltene Anzeigeeinstellungen der Desktop-IDE. */
public final class UiSettings {

    public enum Theme {
        GITHUB_DARK("GitHub Dunkel"),
        MIDNIGHT_BLUE("Mitternachtsblau"),
        GRAPHITE("Graphit"),
        FOREST("Waldgrün"),
        HIGH_CONTRAST("Hoher Kontrast");

        private final String displayName;
        Theme(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public enum Density {
        COMPACT("Kompakt"), STANDARD("Standard"), COMFORTABLE("Großzügig");
        private final String displayName;
        Density(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    private static UiSettings current;

    public Theme theme = Theme.GITHUB_DARK;
    public Density density = Density.STANDARD;
    public int uiFontSize = 12;
    public int codeFontSize = 14;
    public int explorerWidth = 285;
    public boolean strongBorders = false;

    public static synchronized UiSettings get() {
        if (current == null) current = load();
        return current;
    }

    public static synchronized void setCurrent(UiSettings settings) {
        current = settings;
    }

    public UiSettings copy() {
        UiSettings copy = new UiSettings();
        copy.theme = theme;
        copy.density = density;
        copy.uiFontSize = uiFontSize;
        copy.codeFontSize = codeFontSize;
        copy.explorerWidth = explorerWidth;
        copy.strongBorders = strongBorders;
        return copy;
    }

    public static UiSettings defaults() { return new UiSettings(); }

    public static UiSettings load() {
        UiSettings settings = new UiSettings();
        File file = settingsFile();
        if (!file.isFile()) return settings;

        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            p.load(in);
            settings.theme = enumValue(Theme.class, p.getProperty("theme"), settings.theme);
            settings.density = enumValue(Density.class, p.getProperty("density"), settings.density);
            settings.uiFontSize = boundedInt(p.getProperty("uiFontSize"), 11, 20, settings.uiFontSize);
            settings.codeFontSize = boundedInt(p.getProperty("codeFontSize"), 12, 28, settings.codeFontSize);
            settings.explorerWidth = boundedInt(p.getProperty("explorerWidth"), 220, 440, settings.explorerWidth);
            settings.strongBorders = Boolean.parseBoolean(p.getProperty("strongBorders", "false"));
        } catch (IOException | IllegalArgumentException ignored) {
            // Eine beschädigte Datei darf den IDE-Start nicht verhindern.
        }
        return settings;
    }

    public void save() throws IOException {
        File file = settingsFile();
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Einstellungsordner konnte nicht erstellt werden: " + parent);
        }

        Properties p = new Properties();
        p.setProperty("theme", theme.name());
        p.setProperty("density", density.name());
        p.setProperty("uiFontSize", Integer.toString(uiFontSize));
        p.setProperty("codeFontSize", Integer.toString(codeFontSize));
        p.setProperty("explorerWidth", Integer.toString(explorerWidth));
        p.setProperty("strongBorders", Boolean.toString(strongBorders));
        try (FileOutputStream out = new FileOutputStream(file)) {
            p.store(out, "KR Compiler IDE - Anzeigeeinstellungen");
        }
    }

    public static File settingsFile() {
        return new File(applicationRoot(), "config" + File.separator + "ui-settings.properties");
    }

    private static File applicationRoot() {
        try {
            URI uri = UiSettings.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File location = new File(uri).getCanonicalFile();
            File root = location.isFile() ? location.getParentFile() : location;
            if (root != null && ("dist".equalsIgnoreCase(root.getName()) || "bin".equalsIgnoreCase(root.getName()))) {
                root = root.getParentFile();
            }
            if (root != null) return root;
        } catch (Exception ignored) {}
        return new File(System.getProperty("user.dir", "."));
    }

    private static int boundedInt(String value, int min, int max, int fallback) {
        if (value == null) return fallback;
        try { return Math.max(min, Math.min(max, Integer.parseInt(value))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (value == null) return fallback;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
