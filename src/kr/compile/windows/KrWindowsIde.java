package kr.compile.windows;

import kr.compile.BuildManager;
import kr.compile.CompilerEngine;
import kr.compile.DocumentManager;
import kr.compile.GitHubService;
import kr.compile.KrAgentCore;
import kr.compile.KrAdaptiveComputeAnalyzer;
import kr.compile.KrComputePlan;
import kr.compile.KrProjectConfig;
import kr.compile.ProjectManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public final class KrWindowsIde extends JFrame {

    private final ProjectExplorerPanel explorerPanel;
    private final CodeEditorPanel editorPanel;
    private final BuildConsolePanel buildPanel;
    private final ArtifactsPanel artifactsPanel;
    private final KrAgentCore agentCore;
    private final AgentDialog agentDialog;

    private final JPanel mainCardPanel;
    private final CardLayout cardLayout;

    private final JButton btnNavCode;
    private final JButton btnNavFiles;
    private final JButton btnNavBuild;
    private final JButton btnNavArtifacts;
    private final JButton btnNavAgent;

    private final JLabel statusProject;
    private final JLabel statusTarget;
    private final JLabel statusAgent;
    private ProjectManager.Listener projectListener;
    private BuildManager.Listener buildListener;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DarkTheme.applySettings(UiSettings.get());
            DarkTheme.installDefaults();
            new KrWindowsIde().setVisible(true);
        });
    }

    public KrWindowsIde() {
        super("KR Compiler IDE v4.3.2 – Windows Desktop Edition");
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(DarkTheme.BG_DARKEST);

        agentCore = new KrAgentCore();
        agentDialog = AgentDialog.getInstance(this, agentCore);

        // Menüleiste einrichten
        setupMenuBar();

        // Oberer Navigationsbereich (1 zu 1 wie Mobile IDE: CODE | DATEIEN | BUILD | ARTEFAKTE | AGENT)
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 6));
        navBar.setBackground(DarkTheme.BG_TOOLBAR);
        navBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DarkTheme.BORDER_DARK));

        btnNavCode = createNavButton("CODE", () -> showView("CODE"));
        btnNavFiles = createNavButton("DATEIEN", () -> showView("FILES"));
        btnNavBuild = createNavButton("BUILD", () -> showView("BUILD"));
        btnNavArtifacts = createNavButton("ARTEFAKTE", () -> showView("ARTIFACTS"));
        btnNavAgent = createNavButton("AGENT", () -> agentDialog.setVisible(true));
        JButton btnGitHub = createNavButton("◉ GITHUB", this::showCloneGitHubDialog);
        btnGitHub.setForeground(DarkTheme.FG_PRIMARY);

        navBar.add(btnNavCode);
        navBar.add(btnNavFiles);
        navBar.add(btnNavBuild);
        navBar.add(btnNavArtifacts);
        navBar.add(btnNavAgent);
        navBar.add(btnGitHub);
        navBar.add(Box.createHorizontalStrut(22));
        JButton clone = DarkTheme.createStyledButton("⇩  Von GitHub clonen & öffnen", DarkTheme.BG_BUTTON, DarkTheme.FG_PRIMARY);
        clone.addActionListener(e -> showCloneGitHubDialog());
        navBar.add(clone);
        JButton publish = DarkTheme.createStyledButton("⇧  Auf GitHub veröffentlichen", DarkTheme.ACCENT_GREEN, Color.WHITE);
        publish.addActionListener(e -> showPublishGitHubDialog());
        navBar.add(publish);
        navBar.add(Box.createHorizontalStrut(12));
        JComboBox<CompilerEngine.Target> targetSelect = new JComboBox<>(CompilerEngine.Target.values());
        DarkTheme.styleComboBox(targetSelect);
        targetSelect.setSelectedItem(BuildManager.getInstance().getCurrentTarget());
        targetSelect.addActionListener(e -> BuildManager.getInstance().setCurrentTarget((CompilerEngine.Target) targetSelect.getSelectedItem()));
        targetSelect.setPreferredSize(new Dimension(260, 30));
        navBar.add(targetSelect);
        JButton quickBuild = DarkTheme.createStyledButton("▶  Build F5", new Color(29, 78, 146), Color.WHITE);
        quickBuild.addActionListener(e -> {
            File project = ProjectManager.getInstance().getActiveProjectDir();
            if (project != null) BuildManager.getInstance().startBuild(project, BuildManager.getInstance().getCurrentTarget(), BuildManager.getInstance().isOnlineDependencies());
        });
        navBar.add(quickBuild);

        getContentPane().add(navBar, BorderLayout.NORTH);

        // Hauptbereich mit SplitPane: Links Projekt-Explorer, Rechts Arbeitsansichten
        explorerPanel = new ProjectExplorerPanel(this::chooseProjectFolder, this::showCloneGitHubDialog, this::showPublishGitHubDialog);
        editorPanel = new CodeEditorPanel(this::chooseProjectFolder, this::showNewProjectDialog,
                () -> agentDialog.setVisible(true), this::showCloneGitHubDialog, this::showPublishGitHubDialog);
        buildPanel = new BuildConsolePanel();
        artifactsPanel = new ArtifactsPanel();

        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        mainCardPanel.setBackground(DarkTheme.BG_EDITOR);
        mainCardPanel.add(editorPanel, "CODE");
        mainCardPanel.add(buildPanel, "BUILD");
        mainCardPanel.add(artifactsPanel, "ARTIFACTS");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, explorerPanel, mainCardPanel);
        splitPane.setDividerLocation(UiSettings.get().explorerWidth);
        splitPane.setDividerSize(3);
        splitPane.setBorder(null);

        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Statusleiste unten
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        statusBar.setBackground(DarkTheme.BG_DARKEST);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DarkTheme.BORDER_DARK));

        statusProject = new JLabel("Projekt: Kein Projekt");
        statusProject.setForeground(DarkTheme.FG_PRIMARY);

        boolean hasTc = kr.compile.ToolchainManager.getInstance().isEmbeddedToolchainPresent();
        statusTarget = new JLabel("Ziel: Windows x64 (" + (hasTc ? "Integrierte Toolchain" : "System") + ")");
        statusTarget.setForeground(DarkTheme.FG_SECONDARY);

        statusAgent = new JLabel("Agent: Bereit");
        statusAgent.setForeground(DarkTheme.ACCENT_GREEN);

        JLabel infoVersion = new JLabel("v4.3.2 Windows Edition | Entwickler: Ralf Krümmel");
        infoVersion.setForeground(DarkTheme.FG_MUTED);

        statusBar.add(statusProject);
        statusBar.add(statusTarget);
        statusBar.add(statusAgent);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(infoVersion);

        getContentPane().add(statusBar, BorderLayout.SOUTH);

        // Listener registrieren
        projectListener = new ProjectManager.Listener() {
            @Override public void onProjectOpened(File p) {
                statusProject.setText("Projekt: " + p.getName());
                setTitle("KR Compiler IDE v4.3.2 – " + p.getName());
                openDefaultProjectFile(p);
            }
            @Override public void onProjectClosed() {
                statusProject.setText("Projekt: Kein Projekt");
                setTitle("KR Compiler IDE v4.3.2 – Windows Desktop Edition");
            }
            @Override public void onTreeRefreshed(File p) {}
        };
        ProjectManager.getInstance().addListener(projectListener);

        buildListener = new BuildManager.Listener() {
            @Override public void onLogLine(String l) {}
            @Override public void onStatusChanged(String s, boolean b) {
                statusTarget.setText("Ziel: " + BuildManager.getInstance().getCurrentTarget().name() + " (" + s + ")");
            }
            @Override public void onBuildFinished(boolean ok, String r) {
                statusTarget.setText("Ziel: " + BuildManager.getInstance().getCurrentTarget().name() + (ok ? " (OK)" : " (Fehler)"));
            }
        };
        BuildManager.getInstance().addListener(buildListener);

        agentCore.setStatusListener((statusText, isRunning) -> {
            if (isRunning) {
                btnNavAgent.setText("AGENT (aktiv)");
                btnNavAgent.setBackground(DarkTheme.ACCENT_GREEN);
                btnNavAgent.setForeground(Color.WHITE);
                statusAgent.setText("Agent: " + statusText);
                statusAgent.setForeground(DarkTheme.ACCENT_GREEN);
            } else {
                btnNavAgent.setText("AGENT");
                btnNavAgent.setBackground(DarkTheme.BG_BUTTON);
                btnNavAgent.setForeground(DarkTheme.FG_PRIMARY);
                statusAgent.setText("Agent: " + statusText);
                statusAgent.setForeground(DarkTheme.FG_SECONDARY);
            }
        });

        showView("CODE");
    }

    private JButton createNavButton(String text, Runnable action) {
        JButton b = DarkTheme.createStyledButton(text, DarkTheme.BG_BUTTON, DarkTheme.FG_PRIMARY);
        b.setFont(DarkTheme.FONT_BOLD);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BORDER_DARK, 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        b.addActionListener(e -> action.run());
        return b;
    }

    private void showView(String view) {
        if ("FILES".equals(view)) {
            explorerPanel.setVisible(true);
        } else {
            cardLayout.show(mainCardPanel, view);
            if ("ARTIFACTS".equals(view)) {
                artifactsPanel.refreshArtifacts();
            }
        }
        updateNavSelection(view);
    }

    private void updateNavSelection(String active) {
        btnNavCode.setBackground("CODE".equals(active) ? DarkTheme.ACCENT_BLUE : DarkTheme.BG_BUTTON);
        btnNavCode.setForeground("CODE".equals(active) ? Color.WHITE : DarkTheme.FG_PRIMARY);
        btnNavBuild.setBackground("BUILD".equals(active) ? DarkTheme.ACCENT_BLUE : DarkTheme.BG_BUTTON);
        btnNavBuild.setForeground("BUILD".equals(active) ? Color.WHITE : DarkTheme.FG_PRIMARY);
        btnNavArtifacts.setBackground("ARTIFACTS".equals(active) ? DarkTheme.ACCENT_BLUE : DarkTheme.BG_BUTTON);
        btnNavArtifacts.setForeground("ARTIFACTS".equals(active) ? Color.WHITE : DarkTheme.FG_PRIMARY);
    }

    private void setupMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(DarkTheme.BG_TOOLBAR);
        mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DarkTheme.BORDER_DARK));
        mb.setPreferredSize(new Dimension(0, 36));

        JLabel emblem = new JLabel(" KR ");
        emblem.setOpaque(true); emblem.setBackground(DarkTheme.ACCENT_BLUE); emblem.setForeground(Color.WHITE);
        emblem.setFont(new Font("Consolas", Font.BOLD, 11)); emblem.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        mb.add(Box.createHorizontalStrut(8)); mb.add(emblem); mb.add(Box.createHorizontalStrut(8));
        JLabel appTitle = new JLabel("KR Compiler IDE  "); appTitle.setFont(new Font("Segoe UI", Font.BOLD, 12)); appTitle.setForeground(Color.WHITE); mb.add(appTitle);
        JLabel edition = new JLabel("v4.3.2 – Windows Desktop Edition   "); edition.setFont(DarkTheme.FONT_REGULAR); edition.setForeground(DarkTheme.FG_SECONDARY); mb.add(edition);
        JSeparator titleSep = new JSeparator(SwingConstants.VERTICAL); titleSep.setMaximumSize(new Dimension(1, 18)); mb.add(titleSep); mb.add(Box.createHorizontalStrut(10));

        // Menü: Projekt
        JMenu mProject = new JMenu("Projekt");

        JMenuItem mNew = new JMenuItem("Neues Projekt erstellen …");
        mNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        mNew.addActionListener(e -> showNewProjectDialog());
        mProject.add(mNew);

        JMenuItem mOpen = new JMenuItem("Projektordner öffnen …");
        mOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        mOpen.addActionListener(e -> chooseProjectFolder());
        mProject.add(mOpen);

        mProject.addSeparator();

        JMenuItem mCloneGitHub = new JMenuItem("Von GitHub klonen und öffnen …");
        mCloneGitHub.addActionListener(e -> showCloneGitHubDialog());
        mProject.add(mCloneGitHub);

        JMenuItem mPublishGitHub = new JMenuItem("Aktuelles Projekt auf GitHub veröffentlichen …");
        mPublishGitHub.addActionListener(e -> showPublishGitHubDialog());
        mProject.add(mPublishGitHub);

        mProject.addSeparator();

        JMenuItem mSave = new JMenuItem("Aktive Datei speichern");
        mSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        mSave.addActionListener(e -> {
            try {
                DocumentManager.getInstance().saveActiveDocument();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage());
            }
        });
        mProject.add(mSave);

        JMenuItem mSaveAll = new JMenuItem("Alle Dateien speichern");
        mSaveAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        mSaveAll.addActionListener(e -> {
            try {
                DocumentManager.getInstance().saveAll();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage());
            }
        });
        mProject.add(mSaveAll);

        mProject.addSeparator();

        JMenuItem mClose = new JMenuItem("Projekt schließen");
        mClose.addActionListener(e -> ProjectManager.getInstance().closeProject());
        mProject.add(mClose);

        JMenuItem mExit = new JMenuItem("Beenden");
        mExit.addActionListener(e -> System.exit(0));
        mProject.add(mExit);

        styleMenu(mProject);
        mb.add(mProject);

        // Menü: Build
        JMenu mBuild = new JMenu("Build");

        JMenuItem mRunBuild = new JMenuItem("Projekt kompilieren");
        mRunBuild.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        mRunBuild.addActionListener(e -> {
            File p = ProjectManager.getInstance().getActiveProjectDir();
            if (p != null) {
                showView("BUILD");
                BuildManager.getInstance().startBuild(p, BuildManager.getInstance().getCurrentTarget(), BuildManager.getInstance().isOnlineDependencies());
            } else {
                JOptionPane.showMessageDialog(this, "Kein Projekt geöffnet.");
            }
        });
        mBuild.add(mRunBuild);

        JMenuItem mStopBuild = new JMenuItem("Build abbrechen");
        mStopBuild.addActionListener(e -> BuildManager.getInstance().cancelBuild());
        mBuild.add(mStopBuild);

        JMenuItem mCompute = new JMenuItem("Adaptive Compute analysieren …");
        mCompute.addActionListener(e -> showAdaptiveComputeDialog());
        mBuild.addSeparator();
        mBuild.add(mCompute);

        styleMenu(mBuild);
        mb.add(mBuild);

        JMenu mGitHub = new JMenu("◉ GitHub");
        JMenuItem ghClone = new JMenuItem("Von GitHub clonen und öffnen …"); ghClone.addActionListener(e -> showCloneGitHubDialog()); mGitHub.add(ghClone);
        JMenuItem ghPublish = new JMenuItem("Aktuelles Projekt veröffentlichen …"); ghPublish.addActionListener(e -> showPublishGitHubDialog()); mGitHub.add(ghPublish);
        styleMenu(mGitHub); mb.add(mGitHub);

        // Menü: Agent
        JMenu mAgent = new JMenu("Agent");

        JMenuItem mOpenAgent = new JMenuItem("Autonomen KI-Agenten öffnen");
        mOpenAgent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        mOpenAgent.addActionListener(e -> agentDialog.setVisible(true));
        mAgent.add(mOpenAgent);

        styleMenu(mAgent);
        mb.add(mAgent);

        JMenu mView = new JMenu("Ansicht");
        JMenuItem mSettings = new JMenuItem("Einstellungen …");
        mSettings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.CTRL_DOWN_MASK));
        mSettings.addActionListener(e -> showSettingsDialog());
        mView.add(mSettings);
        styleMenu(mView);
        mb.add(mView);

        // Menü: Hilfe
        JMenu mHelp = new JMenu("Hilfe");

        JMenuItem mAbout = new JMenuItem("Über KR IDE");
        mAbout.addActionListener(e -> showAboutDialog());
        mHelp.add(mAbout);

        styleMenu(mHelp);
        mb.add(mHelp);

        mb.add(Box.createHorizontalGlue());
        JLabel view = new JLabel("Ansicht:  Startbildschirm (Kein Projekt)    Projekt aktiv  ");
        view.setForeground(DarkTheme.FG_SECONDARY); view.setFont(new Font("Segoe UI", Font.PLAIN, 11)); mb.add(view);
        JButton settings = titleButton("⚙"); settings.setToolTipText("Anzeigeeinstellungen"); settings.addActionListener(e -> showSettingsDialog()); mb.add(settings);
        JButton minimize = titleButton("—"); minimize.addActionListener(e -> setState(Frame.ICONIFIED)); mb.add(minimize);
        JButton maximize = titleButton("□"); maximize.addActionListener(e -> setExtendedState(getExtendedState() == Frame.MAXIMIZED_BOTH ? Frame.NORMAL : Frame.MAXIMIZED_BOTH)); mb.add(maximize);
        JButton close = titleButton("×"); close.addActionListener(e -> System.exit(0)); mb.add(close);

        final Point[] drag = {null};
        mb.addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { drag[0] = e.getPoint(); } });
        mb.addMouseMotionListener(new MouseAdapter() { public void mouseDragged(MouseEvent e) { if (drag[0] != null && getExtendedState() != Frame.MAXIMIZED_BOTH) setLocation(e.getXOnScreen() - drag[0].x, e.getYOnScreen() - drag[0].y); } });

        setJMenuBar(mb);
    }

    private JButton titleButton(String text) {
        JButton b = new JButton(text); b.setUI(new javax.swing.plaf.basic.BasicButtonUI()); b.setBackground(DarkTheme.BG_DARKEST); b.setForeground(DarkTheme.FG_SECONDARY);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14)); b.setFocusPainted(false); b.setBorder(null); b.setPreferredSize(new Dimension(40, 34)); return b;
    }

    private void showAdaptiveComputeDialog() {
        File project = ProjectManager.getInstance().getActiveProjectDir();
        if (project == null || !project.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Kein lokales KR-Projekt geöffnet.",
                    "Adaptive Compute", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            KrProjectConfig config = KrProjectConfig.parse(
                    KrProjectConfig.findProjectConfigFile(project), project.getName(), false);
            KrComputePlan plan = KrAdaptiveComputeAnalyzer.analyzeProject(project, config.computeMode);
            JTextArea report = new JTextArea(KrAdaptiveComputeAnalyzer.formatReport(plan), 28, 88);
            report.setEditable(false);
            report.setLineWrap(true);
            report.setWrapStyleWord(true);
            report.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            JOptionPane.showMessageDialog(this, new JScrollPane(report),
                    "Adaptive Compute", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Adaptive Compute – Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleMenu(JMenu menu) {
        menu.setForeground(DarkTheme.FG_PRIMARY);
        menu.setBackground(DarkTheme.BG_TOOLBAR);
        menu.setFont(DarkTheme.FONT_REGULAR);
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null) {
                item.setBackground(DarkTheme.BG_TOOLBAR);
                item.setForeground(DarkTheme.FG_PRIMARY);
                item.setFont(DarkTheme.FONT_REGULAR);
            }
        }
    }

    private void showSettingsDialog() {
        UiSettings saved = UiSettings.get();
        JDialog dialog = new JDialog(this, "Anzeigeeinstellungen", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(DarkTheme.BG_PANEL);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(DarkTheme.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));

        JLabel title = new JLabel("Darstellung & Lesbarkeit");
        title.setFont(new Font("Segoe UI", Font.BOLD, Math.max(16, DarkTheme.FONT_REGULAR.getSize() + 4)));
        title.setForeground(DarkTheme.FG_PRIMARY);
        GridBagConstraints titleGc = new GridBagConstraints();
        titleGc.gridx = 0; titleGc.gridy = 0; titleGc.gridwidth = 2; titleGc.anchor = GridBagConstraints.WEST;
        titleGc.insets = new Insets(0, 0, 5, 0);
        form.add(title, titleGc);

        JLabel hint = new JLabel("Wählen Sie nur die Optionen, die Ihre Arbeit angenehmer machen.");
        hint.setForeground(DarkTheme.FG_SECONDARY);
        GridBagConstraints hintGc = new GridBagConstraints();
        hintGc.gridx = 0; hintGc.gridy = 1; hintGc.gridwidth = 2; hintGc.anchor = GridBagConstraints.WEST;
        hintGc.insets = new Insets(0, 0, 16, 0);
        form.add(hint, hintGc);

        JComboBox<UiSettings.Theme> theme = new JComboBox<>(UiSettings.Theme.values());
        DarkTheme.styleComboBox(theme);
        theme.setSelectedItem(saved.theme);

        JSpinner uiSize = new JSpinner(new SpinnerNumberModel(saved.uiFontSize, 11, 20, 1));
        JSpinner codeSize = new JSpinner(new SpinnerNumberModel(saved.codeFontSize, 12, 28, 1));
        JComboBox<UiSettings.Density> density = new JComboBox<>(UiSettings.Density.values());
        DarkTheme.styleComboBox(density);
        density.setSelectedItem(saved.density);
        JSpinner explorerWidth = new JSpinner(new SpinnerNumberModel(saved.explorerWidth, 220, 440, 10));
        JCheckBox strongBorders = new JCheckBox("Rahmen und Sekundärtexte stärker hervorheben", saved.strongBorders);
        strongBorders.setBackground(DarkTheme.BG_PANEL);
        strongBorders.setForeground(DarkTheme.FG_PRIMARY);
        strongBorders.setFont(DarkTheme.FONT_REGULAR);

        styleSpinner(uiSize);
        styleSpinner(codeSize);
        styleSpinner(explorerWidth);
        addSettingRow(form, 2, "Farbschema", theme, "Fünf abgestimmte, dunkle Anzeigedesigns");
        addSettingRow(form, 3, "Schriftgröße Oberfläche", uiSize, "Menüs, Schaltflächen und Beschriftungen");
        addSettingRow(form, 4, "Schriftgröße Code & Konsole", codeSize, "Editor, Build-Ausgabe und Protokolle");
        addSettingRow(form, 5, "Abstände", density, "Kompakt bis großzügig");
        addSettingRow(form, 6, "Breite Datei-Explorer", explorerWidth, "220 bis 440 Pixel");

        GridBagConstraints checkGc = new GridBagConstraints();
        checkGc.gridx = 0; checkGc.gridy = 7; checkGc.gridwidth = 2; checkGc.anchor = GridBagConstraints.WEST;
        checkGc.insets = new Insets(13, 0, 3, 0);
        form.add(strongBorders, checkGc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        actions.setBackground(DarkTheme.BG_TOOLBAR);
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DarkTheme.BORDER_DARK));
        JButton defaults = DarkTheme.createStyledButton("Standard", DarkTheme.BG_BUTTON, DarkTheme.FG_PRIMARY);
        JButton cancel = DarkTheme.createStyledButton("Abbrechen", DarkTheme.BG_BUTTON, DarkTheme.FG_PRIMARY);
        JButton apply = DarkTheme.createStyledButton("Übernehmen", DarkTheme.ACCENT_BLUE, Color.WHITE);
        defaults.addActionListener(e -> {
            UiSettings d = UiSettings.defaults();
            theme.setSelectedItem(d.theme);
            uiSize.setValue(d.uiFontSize);
            codeSize.setValue(d.codeFontSize);
            density.setSelectedItem(d.density);
            explorerWidth.setValue(d.explorerWidth);
            strongBorders.setSelected(d.strongBorders);
        });
        cancel.addActionListener(e -> dialog.dispose());
        apply.addActionListener(e -> {
            UiSettings next = new UiSettings();
            next.theme = (UiSettings.Theme) theme.getSelectedItem();
            next.uiFontSize = (Integer) uiSize.getValue();
            next.codeFontSize = (Integer) codeSize.getValue();
            next.density = (UiSettings.Density) density.getSelectedItem();
            next.explorerWidth = (Integer) explorerWidth.getValue();
            next.strongBorders = strongBorders.isSelected();
            try {
                next.save();
                UiSettings.setCurrent(next);
                dialog.dispose();
                restartWithSettings(next);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog, "Einstellungen konnten nicht gespeichert werden:\n" + ex.getMessage(),
                        "Einstellungen", JOptionPane.ERROR_MESSAGE);
            }
        });
        actions.add(defaults);
        actions.add(cancel);
        actions.add(apply);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(590, 470);
        dialog.setMinimumSize(new Dimension(540, 430));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addSettingRow(JPanel form, int row, String labelText, JComponent input, String explanation) {
        JLabel label = new JLabel(labelText);
        label.setFont(DarkTheme.FONT_BOLD);
        label.setForeground(DarkTheme.FG_PRIMARY);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = row; left.anchor = GridBagConstraints.NORTHWEST;
        left.insets = new Insets(8, 0, 8, 24);
        form.add(label, left);

        JPanel value = new JPanel();
        value.setLayout(new BoxLayout(value, BoxLayout.Y_AXIS));
        value.setOpaque(false);
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(270, 34));
        JLabel description = new JLabel(explanation);
        description.setFont(new Font("Segoe UI", Font.PLAIN, Math.max(10, DarkTheme.FONT_REGULAR.getSize() - 1)));
        description.setForeground(DarkTheme.FG_SECONDARY);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.add(input);
        value.add(Box.createVerticalStrut(3));
        value.add(description);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = row; right.weightx = 1; right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = GridBagConstraints.WEST; right.insets = new Insets(6, 0, 6, 0);
        form.add(value, right);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(DarkTheme.FONT_REGULAR);
        spinner.setBackground(DarkTheme.BG_INPUT);
        spinner.setForeground(DarkTheme.FG_PRIMARY);
        spinner.setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER_DARK));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(DarkTheme.BG_INPUT);
            defaultEditor.getTextField().setForeground(DarkTheme.FG_PRIMARY);
            defaultEditor.getTextField().setCaretColor(DarkTheme.FG_PRIMARY);
            defaultEditor.getTextField().setFont(DarkTheme.FONT_REGULAR);
        }
    }

    private void restartWithSettings(UiSettings settings) {
        File activeProject = ProjectManager.getInstance().getActiveProjectDir();
        if (projectListener != null) ProjectManager.getInstance().removeListener(projectListener);
        if (buildListener != null) BuildManager.getInstance().removeListener(buildListener);
        explorerPanel.disposePanel();
        editorPanel.disposePanel();
        buildPanel.disposePanel();
        AgentDialog.resetInstance();
        dispose();

        DarkTheme.applySettings(settings);
        DarkTheme.installDefaults();
        KrWindowsIde replacement = new KrWindowsIde();
        replacement.setVisible(true);
        if (activeProject != null && activeProject.isDirectory()) {
            ProjectManager.getInstance().openProject(activeProject);
        }
    }

    private void chooseProjectFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Projektordner auswählen");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            ProjectManager.getInstance().openProject(selected);
        }
    }

    private void showNewProjectDialog() {
        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField txtName = new JTextField("MeinKrProjekt");
        JComboBox<ProjectManager.Template> cbTemplate = new JComboBox<>(new ProjectManager.Template[]{
                ProjectManager.Template.MODULAR_KR_FORGE,
                ProjectManager.Template.SINGLE_KR,
                ProjectManager.Template.CPP_PROJECT,
                ProjectManager.Template.RUST_PROJECT,
                ProjectManager.Template.GO_PROJECT
        });
        DarkTheme.styleComboBox(cbTemplate);

        p.add(new JLabel("Projektname:"));
        p.add(txtName);
        p.add(new JLabel("Vorlage:"));
        p.add(cbTemplate);

        int res = JOptionPane.showConfirmDialog(this, p, "Neues Projekt anlegen", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            if (name.isEmpty()) return;

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Zielordner für neues Projekt wählen");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File parent = fc.getSelectedFile();
                try {
                    ProjectManager.Template t = (ProjectManager.Template) cbTemplate.getSelectedItem();
                    ProjectManager.getInstance().createProject(parent, name, t);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Erstellen: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showCloneGitHubDialog() {
        if (!ensureGitAvailable()) return;
        JTextField url = new JTextField("https://github.com/benutzer/repository.git");
        JPanel form = new JPanel(new GridLayout(1, 2, 8, 8));
        form.add(new JLabel("Repository-URL:"));
        form.add(url);
        if (JOptionPane.showConfirmDialog(this, form, "GitHub-Projekt klonen", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Zielordner für das geklonte Projekt wählen");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        runBackground("GitHub-Projekt wird geklont …", () -> {
            File project = GitHubService.cloneRepository(url.getText(), chooser.getSelectedFile());
            SwingUtilities.invokeLater(() -> ProjectManager.getInstance().openProject(project));
            return "Projekt wurde geklont und geöffnet:\n" + project.getAbsolutePath();
        });
    }

    private void showPublishGitHubDialog() {
        File project = ProjectManager.getInstance().getActiveProjectDir();
        if (project == null) {
            JOptionPane.showMessageDialog(this, "Öffnen Sie zuerst das Projekt, das Sie veröffentlichen möchten.", "GitHub", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!ensureGitAvailable()) return;

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField name = new JTextField(project.getName());
        JTextField description = new JTextField();
        JCheckBox privateRepository = new JCheckBox("Privates Repository", false);
        JTextField authorName = new JTextField();
        JTextField authorEmail = new JTextField();
        JPasswordField token = new JPasswordField();
        token.setToolTipText("Optional: Ein lokal angemeldeter GitHub-Account wird zuerst verwendet.");
        form.add(new JLabel("Repository-Name:")); form.add(name);
        form.add(new JLabel("Beschreibung:")); form.add(description);
        form.add(new JLabel("Sichtbarkeit:")); form.add(privateRepository);
        form.add(new JLabel("Git-Autor (optional):")); form.add(authorName);
        form.add(new JLabel("Git-E-Mail (optional):")); form.add(authorEmail);
        form.add(new JLabel("GitHub Token (optional):")); form.add(token);
        int result = JOptionPane.showConfirmDialog(this, form, "Projekt auf GitHub veröffentlichen", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        char[] typedToken = token.getPassword();
        String tokenValue = new String(typedToken);
        java.util.Arrays.fill(typedToken, '\0');
        runBackground("GitHub-Repository wird erstellt und Projekt hochgeladen …", () -> {
            GitHubService.Repository repository = GitHubService.publishNewProject(
                    project, tokenValue, name.getText().trim(), description.getText().trim(),
                    privateRepository.isSelected(), authorName.getText(), authorEmail.getText());
            return "Projekt erfolgreich auf GitHub veröffentlicht.\n" + repository.htmlUrl();
        });
    }

    private boolean ensureGitAvailable() {
        if (GitHubService.checkGit() != null) return true;
        JOptionPane.showMessageDialog(this,
                "Git wurde nicht gefunden. Installieren Sie Git for Windows und starten Sie die IDE anschließend neu.",
                "Git erforderlich", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private void runBackground(String title, BackgroundOperation operation) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return operation.run(); }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    JOptionPane.showMessageDialog(KrWindowsIde.this, get(), title, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(KrWindowsIde.this, cause.getMessage(), title + " – Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    @FunctionalInterface
    private interface BackgroundOperation { String run() throws Exception; }

    private void openDefaultProjectFile(File projectDir) {
        File forge = new File(projectDir, "forge.krproject");
        if (forge.isFile()) { openDoc(projectDir, forge); return; }
        File mainKr = new File(projectDir, "main.kr");
        if (mainKr.isFile()) { openDoc(projectDir, mainKr); return; }
        File mainCpp = new File(projectDir, "main.cpp");
        if (mainCpp.isFile()) { openDoc(projectDir, mainCpp); return; }
        File mainRs = new File(new File(projectDir, "src"), "main.rs");
        if (mainRs.isFile()) { openDoc(projectDir, mainRs); return; }
        File mainGo = new File(projectDir, "main.go");
        if (mainGo.isFile()) { openDoc(projectDir, mainGo); return; }
    }

    private void openDoc(File root, File file) {
        try {
            DocumentManager.getInstance().openDocument(root, file);
            showView("CODE");
        } catch (IOException ignored) {}
    }

    private void showAboutDialog() {
        boolean hasTc = kr.compile.ToolchainManager.getInstance().isEmbeddedToolchainPresent();
        String tcStatus = hasTc ? "Aktiv (" + kr.compile.ToolchainManager.getInstance().getToolchainDir().getName() + ")" : "Host-System";

        JDialog dialog = new JDialog(this, "Über KR IDE", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(DarkTheme.BG_PANEL);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(DarkTheme.BG_PANEL);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = new JLabel("KR Compiler IDE – Windows Desktop Edition");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel versionLabel = new JLabel("Version 4.3.2");
        versionLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        versionLabel.setForeground(DarkTheme.ACCENT_BLUE);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel authorLabel = new JLabel("Entwickler: Ralf Krümmel");
        authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        authorLabel.setForeground(new Color(78, 201, 176));
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tcLabel = new JLabel("Compiler-Status: " + tcStatus);
        tcLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tcLabel.setForeground(DarkTheme.FG_SECONDARY);
        tcLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel projectLink = new JLabel("<html><u>Originalprojekt auf GitHub öffnen</u></html>");
        projectLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        projectLink.setForeground(DarkTheme.ACCENT_BLUE);
        projectLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        projectLink.setToolTipText("https://github.com/kruemmel-python/KR-Compiler");
        projectLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        projectLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                try { Desktop.getDesktop().browse(new java.net.URI("https://github.com/kruemmel-python/KR-Compiler")); }
                catch (Exception ex) { JOptionPane.showMessageDialog(dialog, "Link konnte nicht geöffnet werden: " + ex.getMessage()); }
            }
        });

        JLabel featuresTitle = new JLabel("Funktionsumfang:");
        featuresTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        featuresTitle.setForeground(Color.WHITE);
        featuresTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] features = {
                "• Vollständig integrierte, portable C/C++20 Toolchain (ohne externe SDK-Installation)",
                "• KR Unified Language (.kr) mit vollem Multi-File Support (*.krproject)",
                "• C++20, Rust und Go Übersetzung direkt auf Windows",
                "• Multi-Target Builds: Windows x64 (.exe), Linux x64 (ELF), Android ARM64 (.apk)",
                "• Autonomer KI-Programmierassistent („AGENT“) mit Tool-Loop, Audit-Log und Freigabesystem",
                "• Integrierter vcpkg Paket-Manager"
        };

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(versionLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(authorLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(projectLink);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(tcLabel);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(featuresTitle);
        mainPanel.add(Box.createVerticalStrut(8));

        for (String feat : features) {
            JLabel featLabel = new JLabel(feat);
            featLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            featLabel.setForeground(new Color(225, 225, 225));
            featLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            mainPanel.add(featLabel);
            mainPanel.add(Box.createVerticalStrut(3));
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setBackground(DarkTheme.BG_TOOLBAR);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        JButton okBtn = DarkTheme.createStyledButton("Schließen", DarkTheme.ACCENT_BLUE, Color.WHITE);
        okBtn.setPreferredSize(new Dimension(100, 32));
        okBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(okBtn);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
