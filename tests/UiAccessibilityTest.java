import kr.compile.windows.DarkTheme;
import kr.compile.windows.UiSettings;

import javax.swing.*;
import java.awt.*;

public final class UiAccessibilityTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        for (UiSettings.Theme theme : UiSettings.Theme.values()) {
            UiSettings settings = UiSettings.defaults();
            settings.theme = theme;
            DarkTheme.applySettings(settings);
            DarkTheme.installDefaults();

            double inputContrast = contrast(DarkTheme.FG_PRIMARY, DarkTheme.BG_INPUT);
            double secondaryContrast = contrast(DarkTheme.FG_SECONDARY, DarkTheme.BG_PANEL);
            require(inputContrast >= 7.0, theme + ": Eingabetext-Kontrast zu gering: " + inputContrast);
            require(secondaryContrast >= 4.5, theme + ": Sekundärtext-Kontrast zu gering: " + secondaryContrast);

            JComboBox<String> combo = new JComboBox<>(new String[]{"Windows x64 (.exe)", "Linux x64 (ELF)"});
            DarkTheme.styleComboBox(combo);
            Component rendered = combo.getRenderer().getListCellRendererComponent(
                    new JList<String>(), (String) combo.getSelectedItem(), -1, false, false);
            require(rendered.getForeground().equals(DarkTheme.FG_PRIMARY), theme + ": Auswahlfeld hat falsche Textfarbe");
            require(rendered.getBackground().equals(DarkTheme.BG_INPUT), theme + ": Auswahlfeld hat falsche Hintergrundfarbe");
        }

        String settingsPath = UiSettings.settingsFile().getCanonicalPath();
        require(settingsPath.regionMatches(true, 0, "H:\\", 0, 3),
                "Einstellungen liegen nicht auf H: " + settingsPath);

        UiSettings persisted = UiSettings.defaults();
        persisted.theme = UiSettings.Theme.FOREST;
        persisted.uiFontSize = 17;
        persisted.codeFontSize = 20;
        persisted.density = UiSettings.Density.COMFORTABLE;
        persisted.explorerWidth = 360;
        persisted.strongBorders = true;
        persisted.save();
        UiSettings loaded = UiSettings.load();
        require(loaded.theme == persisted.theme, "Theme wurde nicht korrekt gespeichert");
        require(loaded.uiFontSize == 17 && loaded.codeFontSize == 20, "Schriftgrößen wurden nicht korrekt gespeichert");
        require(loaded.density == persisted.density && loaded.explorerWidth == 360 && loaded.strongBorders,
                "Darstellungseinstellungen wurden nicht korrekt gespeichert");
        UiSettings.defaults().save();
        System.out.println("OK: 5 Themes, Kontrast, Auswahlfeld, Speicherung und H:-Speicherort geprüft.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color color) {
        double r = channel(color.getRed() / 255.0);
        double g = channel(color.getGreen() / 255.0);
        double b = channel(color.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
