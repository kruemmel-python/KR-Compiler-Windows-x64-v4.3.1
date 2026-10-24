package kr.compile.windows;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** Start screen mirroring the supplied GitHub-dark UI mockup. */
final class MockupWelcomePanel extends JPanel {
    private static final Color CARD = new Color(17, 22, 28);
    private static final Color CARD_INNER = new Color(14, 18, 24);

    MockupWelcomePanel(Runnable open, Runnable create, Runnable agent, Runnable clone, Runnable publish) {
        super(new GridBagLayout());
        setOpaque(true);
        setBackground(DarkTheme.BG_EDITOR);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setPreferredSize(new Dimension(850, 620));

        JLabel release = label("●  Release v4.3.1  •  Windows Desktop Edition  •  Windows x64", 11, Font.PLAIN, DarkTheme.FG_SECONDARY);
        release.setFont(new Font("Consolas", Font.PLAIN, 11));
        release.setBorder(BorderFactory.createCompoundBorder(line(), BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        center(release);

        JLabel title = label("KR Compiler IDE", 36, Font.BOLD, DarkTheme.ACCENT_BLUE); center(title);
        JLabel languages = label("●  KR Unified Language (.kr)    •    C++20    •    Rust    •    Go", 13, Font.BOLD, new Color(121, 178, 255)); center(languages);
        JLabel hint = label("Wählen Sie eine Datei im Explorer links oder nutzen Sie die Schnellaktionen & die direkte GitHub-Integration:", 13, Font.PLAIN, DarkTheme.FG_SECONDARY); center(hint);

        content.add(Box.createVerticalStrut(16)); content.add(release);
        content.add(Box.createVerticalStrut(12)); content.add(title);
        content.add(Box.createVerticalStrut(7)); content.add(languages);
        content.add(Box.createVerticalStrut(18)); content.add(hint);
        content.add(Box.createVerticalStrut(20));

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.setMaximumSize(new Dimension(850, 325));
        columns.add(localCard(open, create, agent));
        columns.add(gitHubCard(clone, publish));
        content.add(columns);

        content.add(Box.createVerticalStrut(24));
        JPanel recentHeader = new JPanel(new BorderLayout()); recentHeader.setOpaque(false);
        recentHeader.add(label("ZULETZT VERWENDETE PROJEKTE & GITHUB-REPOSITORIES", 10, Font.BOLD, DarkTheme.FG_SECONDARY), BorderLayout.WEST);
        recentHeader.add(label("GitHub-Hub öffnen", 10, Font.PLAIN, DarkTheme.ACCENT_BLUE), BorderLayout.EAST);
        content.add(recentHeader); content.add(Box.createVerticalStrut(8));

        JPanel recent = new JPanel(new GridLayout(1, 3, 10, 0)); recent.setOpaque(false); recent.setMaximumSize(new Dimension(850, 54));
        recent.add(recentItem("Lokales KR-Projekt", "Zuletzt verwendeter Arbeitsbereich", ".kr"));
        recent.add(recentItem("GitHub-Repository klonen", "Remote-Projekt als Arbeitsbereich öffnen", "Clone"));
        recent.add(recentItem("Release v4.3.1 veröffentlichen", "Push nach origin/main  + .exe Artefakte", "Push"));
        content.add(recent);

        add(content, new GridBagConstraints());
    }

    private JPanel localCard(Runnable open, Runnable create, Runnable agent) {
        JPanel card = card("LOKALER ARBEITSBEREICH & KI", DarkTheme.BORDER_DARK);
        card.add(action("▣", "Projektordner öffnen …", "Bestehendes lokales Verzeichnis laden", "Strg+O", open, DarkTheme.ACCENT_BLUE));
        card.add(Box.createVerticalStrut(12));
        card.add(action("＋", "Neues Projekt erstellen …", "Polyglot-Template (.kr, C++20, Rust, Go)", "Strg+N", create, DarkTheme.ACCENT_BLUE));
        card.add(Box.createVerticalStrut(12));
        card.add(action("ϟ", "Autonomen KI-Agenten öffnen", "AST-Analyse, FFI-Synthese & Auto-Fix", "KI", agent, new Color(63, 185, 80)));
        return card;
    }

    private JPanel gitHubCard(Runnable clone, Runnable publish) {
        JPanel card = card("◉  GITHUB INTEGRATION", new Color(35, 82, 133));
        card.add(action("⇩", "Von GitHub clonen und öffnen …", "Repository klonen & direkt im Editor laden", "Clone", clone, DarkTheme.ACCENT_BLUE));
        card.add(Box.createVerticalStrut(12));
        card.add(action("⇧", "Aktuelles Projekt auf GitHub veröffentlichen …", "Quellcode committen, Repository erstellen/pushen", "Publish", publish, new Color(0, 190, 125)));
        card.add(Box.createVerticalStrut(13));
        JLabel url = label("Eigenes Repository auswählen oder neu veröffentlichen   →", 10, Font.PLAIN, new Color(111, 169, 255));
        card.add(url);
        return card;
    }

    private JPanel card(String title, Color border) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border), BorderFactory.createEmptyBorder(15, 16, 15, 16)));
        JLabel heading = label(title, 11, Font.BOLD, DarkTheme.FG_SECONDARY); heading.setAlignmentX(LEFT_ALIGNMENT);
        card.add(heading); card.add(Box.createVerticalStrut(10));
        return card;
    }

    private JPanel action(String icon, String title, String subtitle, String key, Runnable action, Color accent) {
        JPanel row = new JPanel(new BorderLayout(12, 0)); row.setBackground(CARD_INNER);
        row.setBorder(BorderFactory.createCompoundBorder(line(), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel i = label(icon, 22, Font.PLAIN, accent); i.setPreferredSize(new Dimension(36, 36)); i.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel text = new JPanel(); text.setOpaque(false); text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(title, 13, Font.BOLD, Color.WHITE)); text.add(Box.createVerticalStrut(3)); text.add(label(subtitle, 10, Font.PLAIN, DarkTheme.FG_SECONDARY));
        JButton button = DarkTheme.createStyledButton(key, CARD_INNER, accent); button.addActionListener(e -> { if (action != null) action.run(); });
        row.add(i, BorderLayout.WEST); row.add(text, BorderLayout.CENTER); row.add(button, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { if (action != null) action.run(); }});
        return row;
    }

    private JPanel recentItem(String title, String sub, String badge) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(CARD); p.setBorder(BorderFactory.createCompoundBorder(line(), BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        JPanel t = new JPanel(); t.setOpaque(false); t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS)); t.add(label(title, 11, Font.BOLD, Color.WHITE)); t.add(label(sub, 9, Font.PLAIN, DarkTheme.FG_SECONDARY));
        p.add(t, BorderLayout.CENTER); p.add(label(badge, 9, Font.PLAIN, DarkTheme.ACCENT_BLUE), BorderLayout.EAST); return p;
    }

    private static JLabel label(String text, int size, int style, Color color) { JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", style, size)); l.setForeground(color); return l; }
    private static Border line() { return BorderFactory.createLineBorder(DarkTheme.BORDER_DARK); }
    private static void center(JComponent c) { c.setAlignmentX(Component.CENTER_ALIGNMENT); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(48, 54, 61, 42));
        for (int x = 0; x < getWidth(); x += 32) g.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 32) g.drawLine(0, y, getWidth(), y);
    }
}
