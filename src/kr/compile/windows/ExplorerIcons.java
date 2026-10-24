package kr.compile.windows;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.Locale;

/** Kleine, skalierbare Explorer-Symbole ohne Abhängigkeit von Emoji- oder Systemschriftarten. */
final class ExplorerIcons {

    private ExplorerIcons() {}

    static Icon project() { return new FolderIcon(true, true); }
    static Icon folder(boolean open) { return new FolderIcon(open, false); }
    static Icon file(File file) { return new FileIcon(fileType(file)); }

    enum FileType {
        KR("KR", new Color(47, 129, 247)),
        PROJECT("P", new Color(163, 113, 247)),
        CPP("C", new Color(65, 174, 255)),
        RUST("R", new Color(224, 120, 72)),
        GO("G", new Color(0, 173, 216)),
        DATA("D", new Color(218, 164, 48)),
        DOC("T", new Color(66, 181, 102)),
        GENERIC("", new Color(139, 148, 158));

        final String mark;
        final Color accent;
        FileType(String mark, Color accent) { this.mark = mark; this.accent = accent; }
    }

    private static FileType fileType(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".krproject")) return FileType.PROJECT;
        if (name.endsWith(".kr")) return FileType.KR;
        if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".c") ||
                name.endsWith(".h") || name.endsWith(".hpp") || name.equals("cmakelists.txt")) return FileType.CPP;
        if (name.endsWith(".rs") || name.equals("cargo.toml")) return FileType.RUST;
        if (name.endsWith(".go") || name.equals("go.mod") || name.equals("go.sum")) return FileType.GO;
        if (name.endsWith(".json") || name.endsWith(".toml") || name.endsWith(".xml") ||
                name.endsWith(".yaml") || name.endsWith(".yml")) return FileType.DATA;
        if (name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".log")) return FileType.DOC;
        return FileType.GENERIC;
    }

    private static int iconSize() {
        return Math.max(17, Math.min(23, DarkTheme.FONT_REGULAR.getSize() + 5));
    }

    private static final class FolderIcon implements Icon {
        private final boolean open;
        private final boolean project;
        private final int size = iconSize();

        FolderIcon(boolean open, boolean project) {
            this.open = open;
            this.project = project;
        }

        @Override public int getIconWidth() { return size + 2; }
        @Override public int getIconHeight() { return size; }

        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float scale = size / 18f;
                g.translate(x + 1, y + 1);
                g.scale(scale, scale);

                Color body = project ? DarkTheme.ACCENT_BLUE : new Color(218, 166, 68);
                Color edge = project ? body.brighter() : new Color(247, 196, 91);
                g.setColor(edge);
                g.fillRoundRect(1, 2, 8, 5, 2, 2);
                g.setColor(body);
                g.fillRoundRect(1, 5, 16, 11, 2, 2);

                if (open) {
                    Path2D front = new Path2D.Float();
                    front.moveTo(2, 8);
                    front.lineTo(17, 8);
                    front.lineTo(14.5, 16);
                    front.lineTo(0.5, 16);
                    front.closePath();
                    g.setColor(project ? new Color(82, 157, 255) : new Color(255, 205, 99));
                    g.fill(front);
                }

                g.setStroke(new BasicStroke(1f));
                g.setColor(blend(body, Color.BLACK, 0.32f));
                g.drawRoundRect(1, 5, 16, 11, 2, 2);
                if (project) {
                    g.setColor(Color.WHITE);
                    g.fillOval(7, 9, 4, 4);
                }
            } finally {
                g.dispose();
            }
        }
    }

    private static final class FileIcon implements Icon {
        private final FileType type;
        private final int size = iconSize();

        FileIcon(FileType type) { this.type = type; }

        @Override public int getIconWidth() { return size + 2; }
        @Override public int getIconHeight() { return size; }

        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float scale = size / 18f;
                g.translate(x + 2, y + 1);
                g.scale(scale, scale);

                Path2D sheet = new Path2D.Float();
                sheet.moveTo(2, 1);
                sheet.lineTo(11, 1);
                sheet.lineTo(16, 6);
                sheet.lineTo(16, 17);
                sheet.lineTo(2, 17);
                sheet.closePath();
                g.setColor(new Color(231, 237, 243));
                g.fill(sheet);
                g.setColor(type.accent);
                g.fillRoundRect(2, 12, 14, 5, 1, 1);

                Path2D fold = new Path2D.Float();
                fold.moveTo(11, 1);
                fold.lineTo(11, 6);
                fold.lineTo(16, 6);
                fold.closePath();
                g.setColor(new Color(164, 177, 190));
                g.fill(fold);
                g.setColor(new Color(72, 83, 94));
                g.draw(sheet);

                if (!type.mark.isEmpty()) {
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 5));
                    FontMetrics fm = g.getFontMetrics();
                    int tx = 9 - fm.stringWidth(type.mark) / 2;
                    g.drawString(type.mark, tx, 16);
                }
            } finally {
                g.dispose();
            }
        }
    }

    private static Color blend(Color a, Color b, float amount) {
        float rest = 1f - amount;
        return new Color((int) (a.getRed() * rest + b.getRed() * amount),
                (int) (a.getGreen() * rest + b.getGreen() * amount),
                (int) (a.getBlue() * rest + b.getBlue() * amount));
    }
}
