package kr.compile.studio;

import com.intellij.openapi.util.ScalableIcon;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class KrStudioIcons {

    public static final Icon KR_FILE_ICON = new BadgeIcon("KR", new Color(0, 175, 215), new Color(18, 26, 38), 1.0f);
    public static final Icon KR_PROJECT_ICON = new BadgeIcon("KP", new Color(165, 105, 255), new Color(25, 20, 40), 1.0f);
    public static final Icon KR_TOOL_WINDOW_ICON = new BadgeIcon("KR", new Color(0, 210, 150), new Color(16, 28, 28), 1.0f);

    private KrStudioIcons() {}

    private static final class BadgeIcon implements ScalableIcon {
        private final String label;
        private final Color accent;
        private final Color bg;
        private final float scale;

        BadgeIcon(String label, Color accent, Color bg, float scale) {
            this.label = label;
            this.accent = accent;
            this.bg = bg;
            this.scale = scale <= 0f ? 1.0f : scale;
        }

        @Override
        public float getScale() {
            return scale;
        }

        @Override
        public Icon scale(float scaleFactor) {
            if (Math.abs(this.scale - scaleFactor) < 0.001f) {
                return this;
            }
            return new BadgeIcon(label, accent, bg, scaleFactor);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int size = getIconWidth();
            int pad = Math.max(1, Math.round(scale));
            int box = Math.max(4, size - 2 * pad);
            int arc = Math.max(3, Math.round(4 * scale));

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(bg);
            g2.fillRoundRect(x + pad, y + pad, box, box, arc, arc);

            g2.setColor(accent);
            g2.setStroke(new BasicStroke(Math.max(1.0f, 1.2f * scale)));
            g2.drawRoundRect(x + pad, y + pad, box, box, arc, arc);

            int fontSize = Math.max(6, Math.round(8 * scale));
            Font font = new Font("SansSerif", Font.BOLD, fontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(label);
            int tx = x + (size - tw) / 2;
            int ty = y + ((size - fm.getHeight()) / 2) + fm.getAscent();

            g2.setColor(Color.WHITE);
            g2.drawString(label, tx, ty);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return Math.max(8, Math.round(16 * scale));
        }

        @Override
        public int getIconHeight() {
            return Math.max(8, Math.round(16 * scale));
        }
    }
}
