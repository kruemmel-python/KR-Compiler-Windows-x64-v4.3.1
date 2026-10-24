package kr.compile.windows;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class DarkTheme {

    public static Color BG_DARKEST    = new Color(13, 17, 23);
    public static Color BG_EDITOR     = new Color(13, 17, 23);
    public static Color BG_PANEL      = new Color(22, 27, 34);
    public static Color BG_TOOLBAR    = new Color(21, 25, 31);
    public static Color BG_BUTTON     = new Color(31, 36, 44);
    public static Color BG_BUTTON_HOV = new Color(38, 44, 54);
    public static Color BG_INPUT      = new Color(13, 17, 23);
    public static Color BG_SELECT     = new Color(47, 129, 247);

    public static Color ACCENT_BLUE   = new Color(47, 129, 247);
    public static Color ACCENT_GREEN  = new Color(35, 134, 54);
    public static Color ACCENT_RED    = new Color(198, 40, 40);

    public static Color FG_PRIMARY    = new Color(230, 237, 243);
    public static Color FG_SECONDARY  = new Color(139, 148, 158);
    public static Color FG_MUTED      = new Color(110, 118, 129);

    public static Color BORDER_DARK   = new Color(48, 54, 61);

    public static Font FONT_REGULAR   = new Font("Segoe UI", Font.PLAIN, 12);
    public static Font FONT_BOLD      = new Font("Segoe UI", Font.BOLD, 12);
    public static Font FONT_TITLE     = new Font("Segoe UI", Font.BOLD, 14);
    public static Font FONT_CODE      = new Font("Consolas", Font.PLAIN, 14);

    private static int verticalPadding = 6;

    private DarkTheme() {}

    public static void applySettings(UiSettings settings) {
        switch (settings.theme) {
            case MIDNIGHT_BLUE -> setPalette(
                    6, 15, 28,   8, 19, 35,   13, 29, 49,   10, 23, 40,
                    21, 43, 69,  29, 55, 86,  6, 15, 28,    48, 139, 255,
                    48, 139, 255, 38, 153, 83, 210, 55, 65,
                    235, 244, 255, 157, 177, 202, 119, 143, 171, 55, 82, 112);
            case GRAPHITE -> setPalette(
                    18, 18, 18,  22, 22, 22,  31, 31, 31,   26, 26, 26,
                    43, 43, 43,  55, 55, 55,  16, 16, 16,   77, 139, 255,
                    77, 139, 255, 48, 150, 83, 205, 58, 68,
                    245, 245, 245, 184, 184, 184, 142, 142, 142, 72, 72, 72);
            case FOREST -> setPalette(
                    8, 18, 13,   10, 22, 16,  17, 34, 25,   13, 29, 21,
                    25, 48, 35,  34, 62, 46,  7, 17, 12,    42, 143, 83,
                    67, 160, 255, 42, 151, 80, 204, 55, 65,
                    234, 244, 237, 157, 181, 165, 116, 143, 125, 49, 78, 58);
            case HIGH_CONTRAST -> setPalette(
                    0, 0, 0,     0, 0, 0,     9, 9, 9,      0, 0, 0,
                    28, 28, 28,  48, 48, 48,  0, 0, 0,      0, 103, 192,
                    72, 166, 255, 38, 166, 73, 232, 55, 68,
                    255, 255, 255, 231, 231, 231, 196, 196, 196, 255, 255, 255);
            default -> setPalette(
                    13, 17, 23,  13, 17, 23,  22, 27, 34,   21, 25, 31,
                    31, 36, 44,  38, 44, 54,  13, 17, 23,   47, 129, 247,
                    47, 129, 247, 35, 134, 54, 198, 40, 40,
                    230, 237, 243, 139, 148, 158, 110, 118, 129, 48, 54, 61);
        }

        if (settings.strongBorders && settings.theme != UiSettings.Theme.HIGH_CONTRAST) {
            BORDER_DARK = blend(BORDER_DARK, Color.WHITE, 0.35f);
            FG_SECONDARY = blend(FG_SECONDARY, Color.WHITE, 0.18f);
        }
        FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, settings.uiFontSize);
        FONT_BOLD = new Font("Segoe UI", Font.BOLD, settings.uiFontSize);
        FONT_TITLE = new Font("Segoe UI", Font.BOLD, settings.uiFontSize + 2);
        FONT_CODE = new Font("Consolas", Font.PLAIN, settings.codeFontSize);
        verticalPadding = switch (settings.density) {
            case COMPACT -> 4;
            case COMFORTABLE -> 9;
            default -> 6;
        };
    }

    private static void setPalette(int... v) {
        BG_DARKEST = c(v, 0); BG_EDITOR = c(v, 3); BG_PANEL = c(v, 6); BG_TOOLBAR = c(v, 9);
        BG_BUTTON = c(v, 12); BG_BUTTON_HOV = c(v, 15); BG_INPUT = c(v, 18); BG_SELECT = c(v, 21);
        ACCENT_BLUE = c(v, 24); ACCENT_GREEN = c(v, 27); ACCENT_RED = c(v, 30);
        FG_PRIMARY = c(v, 33); FG_SECONDARY = c(v, 36); FG_MUTED = c(v, 39); BORDER_DARK = c(v, 42);
    }

    private static Color c(int[] values, int offset) {
        return new Color(values[offset], values[offset + 1], values[offset + 2]);
    }

    private static Color blend(Color a, Color b, float amount) {
        float rest = 1f - amount;
        return new Color((int) (a.getRed() * rest + b.getRed() * amount),
                (int) (a.getGreen() * rest + b.getGreen() * amount),
                (int) (a.getBlue() * rest + b.getBlue() * amount));
    }

    public static void installDefaults() {
        UIManager.put("Panel.background", BG_PANEL);
        UIManager.put("Panel.foreground", FG_PRIMARY);

        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("OptionPane.foreground", FG_PRIMARY);
        UIManager.put("OptionPane.messageForeground", FG_PRIMARY);
        UIManager.put("OptionPane.messageArea.background", BG_PANEL);
        UIManager.put("OptionPane.buttonArea.background", BG_PANEL);

        UIManager.put("Label.foreground", FG_PRIMARY);
        UIManager.put("Label.font", FONT_REGULAR);

        UIManager.put("MenuBar.background", BG_TOOLBAR);
        UIManager.put("MenuBar.foreground", FG_PRIMARY);
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_DARK));

        UIManager.put("Menu.background", BG_TOOLBAR);
        UIManager.put("Menu.foreground", FG_PRIMARY);
        UIManager.put("Menu.selectionBackground", BG_SELECT);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("Menu.font", FONT_REGULAR);

        UIManager.put("MenuItem.background", BG_TOOLBAR);
        UIManager.put("MenuItem.foreground", FG_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", BG_SELECT);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.font", FONT_REGULAR);

        UIManager.put("PopupMenu.background", BG_TOOLBAR);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER_DARK));

        UIManager.put("Button.background", BG_BUTTON);
        UIManager.put("Button.foreground", FG_PRIMARY);
        UIManager.put("Button.font", FONT_BOLD);
        UIManager.put("Button.disabledText", FG_MUTED);

        UIManager.put("TabbedPane.background", BG_TOOLBAR);
        UIManager.put("TabbedPane.foreground", FG_PRIMARY);
        UIManager.put("TabbedPane.selected", BG_EDITOR);
        UIManager.put("TabbedPane.selectHighlight", ACCENT_BLUE);
        UIManager.put("TabbedPane.contentAreaColor", BG_EDITOR);
        UIManager.put("TabbedPane.darkShadow", BG_DARKEST);
        UIManager.put("TabbedPane.shadow", BORDER_DARK);
        UIManager.put("TabbedPane.light", BG_PANEL);
        UIManager.put("TabbedPane.highlight", BG_PANEL);
        UIManager.put("TabbedPane.focus", BG_SELECT);

        UIManager.put("ScrollPane.background", BG_EDITOR);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());

        UIManager.put("Viewport.background", BG_EDITOR);

        UIManager.put("TextArea.background", BG_INPUT);
        UIManager.put("TextArea.foreground", FG_PRIMARY);
        UIManager.put("TextArea.caretForeground", Color.WHITE);
        UIManager.put("TextArea.font", FONT_CODE);

        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", FG_PRIMARY);
        UIManager.put("TextField.caretForeground", Color.WHITE);
        UIManager.put("TextField.font", FONT_REGULAR);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DARK),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        UIManager.put("PasswordField.background", BG_INPUT);
        UIManager.put("PasswordField.foreground", FG_PRIMARY);
        UIManager.put("PasswordField.caretForeground", Color.WHITE);
        UIManager.put("PasswordField.font", FONT_REGULAR);
        UIManager.put("PasswordField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DARK),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        UIManager.put("ComboBox.background", BG_BUTTON);
        UIManager.put("ComboBox.foreground", FG_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", BG_SELECT);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("ComboBox.disabledBackground", BG_BUTTON);
        UIManager.put("ComboBox.disabledForeground", FG_SECONDARY);
        UIManager.put("ComboBox.font", FONT_REGULAR);

        UIManager.put("CheckBox.background", BG_TOOLBAR);
        UIManager.put("CheckBox.foreground", FG_PRIMARY);
        UIManager.put("CheckBox.font", FONT_REGULAR);

        UIManager.put("Spinner.background", BG_INPUT);
        UIManager.put("Spinner.foreground", FG_PRIMARY);
        UIManager.put("Spinner.font", FONT_REGULAR);

        UIManager.put("Table.background", BG_INPUT);
        UIManager.put("Table.foreground", FG_PRIMARY);
        UIManager.put("Table.selectionBackground", BG_SELECT);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", BORDER_DARK);

        UIManager.put("TableHeader.background", BG_TOOLBAR);
        UIManager.put("TableHeader.foreground", FG_PRIMARY);
        UIManager.put("TableHeader.font", FONT_BOLD);

        UIManager.put("Tree.background", BG_PANEL);
        UIManager.put("Tree.textBackground", BG_PANEL);
        UIManager.put("Tree.textForeground", FG_PRIMARY);
        UIManager.put("Tree.selectionBackground", BG_SELECT);
        UIManager.put("Tree.selectionForeground", Color.WHITE);
        UIManager.put("Tree.selectionBorderColor", ACCENT_BLUE);

        UIManager.put("SplitPane.background", BG_TOOLBAR);
        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
    }

    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setUI(new BasicButtonUI());
        btn.setBackground(bg != null ? bg : BG_BUTTON);
        btn.setForeground(fg != null ? fg : FG_PRIMARY);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DARK, 1),
                BorderFactory.createEmptyBorder(verticalPadding, 12, verticalPadding, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color normalBg = btn.getBackground();
        Color hoverBg = bg != null ? bg.brighter() : BG_BUTTON_HOV;

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hoverBg); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(normalBg); }
        });
        return btn;
    }

    public static JButton createIconButton(String iconText, String tooltip) {
        JButton btn = new JButton(iconText);
        btn.setUI(new BasicButtonUI());
        btn.setBackground(BG_BUTTON);
        btn.setForeground(FG_PRIMARY);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(28, 26));
        btn.setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(BG_BUTTON_HOV); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(BG_BUTTON); }
        });
        return btn;
    }

    /** Erzwingt lesbare Farben auch mit dem Windows-Look-and-Feel. */
    public static void styleComboBox(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton arrow = new JButton("▾");
                arrow.setUI(new BasicButtonUI());
                arrow.setBackground(BG_BUTTON);
                arrow.setForeground(FG_PRIMARY);
                arrow.setFont(FONT_BOLD);
                arrow.setFocusable(false);
                arrow.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_DARK));
                arrow.setPreferredSize(new Dimension(26, 24));
                return arrow;
            }
        });
        combo.setOpaque(true);
        combo.setBackground(BG_INPUT);
        combo.setForeground(FG_PRIMARY);
        combo.setFont(FONT_REGULAR);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_DARK));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                     boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setOpaque(true);
                label.setBackground(selected ? BG_SELECT : BG_INPUT);
                label.setForeground(combo.isEnabled() ? (selected ? Color.WHITE : FG_PRIMARY) : FG_SECONDARY);
                label.setFont(FONT_REGULAR);
                label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return label;
            }
        });
    }
}
