package kr.compile.studio;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import kr.compile.CompilerEngine;
import kr.compile.DiagnosticParser;
import kr.compile.ToolchainManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class KrToolWindowPanel extends JPanel {

    private final Project project;
    private final JComboBox<CompilerEngine.Target> targetCombo;
    private final DefaultTableModel segmentsModel;
    private final DefaultTableModel diagnosticsModel;
    private final JTextArea consoleArea;
    private final JLabel statusLabel;

    public KrToolWindowPanel(Project project) {
        super(new BorderLayout(6, 6));
        this.project = project;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        KrStudioCompilerBridge.ensureToolchainConfigured();

        // Top Toolbar + Status Banner
        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

        targetCombo = new JComboBox<>(CompilerEngine.Target.values());
        targetCombo.setSelectedItem(CompilerEngine.Target.ANDROID);

        JButton openKrBtn = new JButton("main.kr öffnen");
        JButton analyzeBtn = new JButton("Sprachsegmente analysieren");
        JButton buildBtn = new JButton("KR Build starten");
        JButton adbRunBtn = new JButton("APK bauen & auf Gerät starten (ADB)");
        JButton gradleBtn = new JButton("Gradle-Bridge erzeugen");
        JButton demoBtn = new JButton("4-Sprachen-Demo erzeugen");
        JButton enginePathBtn = new JButton("KR-Engine Pfad…");

        controls.add(new JLabel("Ziel:"));
        controls.add(targetCombo);
        controls.add(openKrBtn);
        controls.add(analyzeBtn);
        controls.add(buildBtn);
        controls.add(adbRunBtn);
        controls.add(gradleBtn);
        controls.add(demoBtn);
        controls.add(enginePathBtn);

        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 12f));
        updateStatusBanner();

        topPanel.add(controls, BorderLayout.NORTH);
        topPanel.add(statusLabel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Left Tabs: 1) Language Segments & ABI, 2) Diagnostics
        segmentsModel = new DefaultTableModel(
                new Object[]{"Datei", "Sprache", "Zeilen", "Deklarierte Funktionen", "Cross-Language Aufrufe", "ABI Export"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable segmentsTable = new JTable(segmentsModel);
        segmentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = segmentsTable.getSelectedRow();
                    if (row >= 0) {
                        String relFile = String.valueOf(segmentsModel.getValueAt(row, 0));
                        String lines = String.valueOf(segmentsModel.getValueAt(row, 2));
                        int line = parseStartLine(lines);
                        navigateToFileLine(relFile, line, 1);
                    }
                }
            }
        });

        diagnosticsModel = new DefaultTableModel(
                new Object[]{"Schweregrad", "Datei (.kr)", "Zeile", "Spalte", "Diagnosemeldung"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable diagnosticsTable = new JTable(diagnosticsModel);
        diagnosticsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = diagnosticsTable.getSelectedRow();
                    if (row >= 0) {
                        String relFile = String.valueOf(diagnosticsModel.getValueAt(row, 1));
                        int line = parseIntSafe(diagnosticsModel.getValueAt(row, 2), 1);
                        int col = parseIntSafe(diagnosticsModel.getValueAt(row, 3), 1);
                        navigateToFileLine(relFile, line, col);
                    }
                }
            }
        });

        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.addTab("Sprachsegmente & ABI (C++ / Rust / Go / Python)", new JScrollPane(segmentsTable));
        leftTabs.addTab("Diagnose & Fehlerzuordnung", new JScrollPane(diagnosticsTable));

        // Right Panel: Live Compiler & ADB Console
        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane consoleScroll = new JScrollPane(consoleArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabs, consoleScroll);
        splitPane.setResizeWeight(0.52);
        add(splitPane, BorderLayout.CENTER);

        // Button Actions
        openKrBtn.addActionListener(e -> openFirstKrFile());
        analyzeBtn.addActionListener(e -> refreshAnalysis());
        buildBtn.addActionListener(e -> runBuild((CompilerEngine.Target) targetCombo.getSelectedItem(), false));
        adbRunBtn.addActionListener(e -> {
            targetCombo.setSelectedItem(CompilerEngine.Target.ANDROID);
            runBuild(CompilerEngine.Target.ANDROID, true);
        });
        gradleBtn.addActionListener(e -> createGradleBridge());
        demoBtn.addActionListener(e -> createFourLangDemo());
        enginePathBtn.addActionListener(e -> chooseKrEnginePath());

        if (!ToolchainManager.getInstance().isEmbeddedToolchainPresent()) {
            appendConsole("[WICHTIG] Das KR Unified Language Plugin benötigt eine installierte KR Compiler IDE (inkl. 'toolchain/' und 'runtime/').");
            appendConsole("[DOWNLOAD] Release herunterladen unter: " + KrStudioCompilerBridge.GITHUB_RELEASE_URL);
            appendConsole("[EINRICHTUNG] Klicke oben auf 'KR-Engine Pfad…' und wähle den entpackten Ordner 'KR-Compiler-Windows-x64-v4.3.1' aus (oder setze die Umgebungsvariable KR_COMPILER_HOME).");
        }

        refreshAnalysis();
    }

    private void updateStatusBanner() {
        File krHome = KrStudioCompilerBridge.resolveKrCompilerHome();
        boolean tcOk = ToolchainManager.getInstance().isEmbeddedToolchainPresent() || new File(krHome, "toolchain").isDirectory();
        File ndkDir = ToolchainManager.getInstance().getNdkDir();
        String tcInfo = "KR Engine v4.3.1 | KR-IDE: " + (tcOk ? krHome.getAbsolutePath() : "FEHLT (bitte 'KR-Engine Pfad…' wählen!)")
                + " | Android NDK: " + (ndkDir != null ? ndkDir.getName() : "Nicht gefunden")
                + " | Python-Strategie: AOT_NATIVE_CPP20_BRIDGE";
        statusLabel.setText(tcInfo);
    }

    public void chooseKrEnginePath() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(KrStudioCompilerBridge.resolveKrCompilerHome());
        chooser.setDialogTitle("KR Compiler IDE Installationsordner wählen (enthält 'toolchain' & 'dist')");
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null) {
                KrStudioCompilerBridge.saveCustomKrHome(selected);
                updateStatusBanner();
                appendConsole("[KONFIGURATION] KR Compiler IDE Pfad gespeichert: " + selected.getAbsolutePath());
            }
        }
    }

    public void refreshAnalysis() {
        saveAllDocuments();
        File root = getProjectRootDir();
        if (root == null) return;

        KrStudioCompilerBridge.AnalysisReport report = KrStudioCompilerBridge.analyzeProject(root);
        segmentsModel.setRowCount(0);
        for (KrStudioCompilerBridge.SegmentEntry s : report.segmentEntries) {
            segmentsModel.addRow(new Object[]{
                    s.file,
                    s.language.getDisplayName(),
                    s.startLine + " - " + s.endLine,
                    s.functionsSummary,
                    s.crossCallsSummary,
                    s.abiExported ? "Ja (kr_bridge.h)" : "Lokal"
            });
        }

        diagnosticsModel.setRowCount(0);
        for (DiagnosticParser.Item d : report.diagnostics) {
            diagnosticsModel.addRow(new Object[]{
                    d.severity, d.file != null ? d.file : "—", d.line, d.column, d.message
            });
        }
    }

    public void runBuild(CompilerEngine.Target target, boolean installAndRunOnDevice) {
        saveAllDocuments();
        File root = getProjectRootDir();
        if (root == null) {
            appendConsole("[FEHLER] Kein geöffnetes Android Studio Projektverzeichnis gefunden.");
            return;
        }

        refreshAnalysis();
        consoleArea.setText("");
        appendConsole("=== KR Unified Language Build in Android Studio Rabbit 2 ===");
        appendConsole("Projektpfad: " + root.getAbsolutePath());
        appendConsole("Zielplattform: " + target.getDisplayName());

        Thread worker = new Thread(() -> {
            List<DiagnosticParser.Item> buildDiags = new ArrayList<>();
            try {
                File artifact = KrStudioCompilerBridge.buildProject(root, target, this::appendConsole, buildDiags);
                SwingUtilities.invokeLater(() -> {
                    for (DiagnosticParser.Item d : buildDiags) {
                        diagnosticsModel.addRow(new Object[]{
                                d.severity, d.file != null ? d.file : "—", d.line, d.column, d.message
                        });
                    }
                    LocalFileSystem.getInstance().refreshIoFiles(java.util.Collections.singletonList(root), true, true, null);
                });
                appendConsole("[ERFOLG] Artefakt erstellt: " + artifact.getAbsolutePath() + " (" + artifact.length() + " Bytes)");

                if (installAndRunOnDevice && target == CompilerEngine.Target.ANDROID) {
                    KrStudioCompilerBridge.installAndLaunchApk(root, artifact, this::appendConsole);
                }
            } catch (Exception ex) {
                appendConsole("[FEHLER] Build fehlgeschlagen: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    for (DiagnosticParser.Item d : buildDiags) {
                        diagnosticsModel.addRow(new Object[]{
                                d.severity, d.file != null ? d.file : "—", d.line, d.column, d.message
                        });
                    }
                });
            }
        }, "KR-AndroidStudio-Build-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    public void createGradleBridge() {
        File root = getProjectRootDir();
        if (root == null) return;
        try {
            File bridge = KrStudioCompilerBridge.generateGradleBridge(root);
            appendConsole("[GRADLE] KR Android Build Adapter erstellt: " + bridge.getAbsolutePath());
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(bridge);
            if (vf != null) {
                new OpenFileDescriptor(project, vf, 0, 0).navigate(true);
            }
        } catch (Exception ex) {
            appendConsole("[FEHLER] Gradle-Bridge konnte nicht erstellt werden: " + ex.getMessage());
        }
    }

    public void openFirstKrFile() {
        File root = getProjectRootDir();
        if (root == null) return;
        File mainKr = new File(root, "main.kr");
        if (!mainKr.isFile()) {
            List<File> krFiles = kr.compile.KrPipeline.findKrFiles(root);
            if (!krFiles.isEmpty()) {
                mainKr = krFiles.get(0);
            } else {
                appendConsole("[HINWEIS] Keine .kr-Datei gefunden. Klicke auf '4-Sprachen-Demo erzeugen', um main.kr anzulegen.");
                return;
            }
        }
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(mainKr);
        if (vf != null) {
            new OpenFileDescriptor(project, vf, 0, 0).navigate(true);
        }
    }

    public void createFourLangDemo() {
        File root = getProjectRootDir();
        if (root == null) return;
        try {
            File mainKr = KrStudioCompilerBridge.createFourLanguageDemoProject(root);
            appendConsole("[DEMO] 4-Sprachen-Projekt (C++20, Rust, Go, Python) erstellt: " + mainKr.getAbsolutePath());
            refreshAnalysis();
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(mainKr);
            if (vf != null) {
                new OpenFileDescriptor(project, vf, 0, 0).navigate(true);
            }
        } catch (Exception ex) {
            appendConsole("[FEHLER] 4-Sprachen-Demo konnte nicht erstellt werden: " + ex.getMessage());
        }
    }

    private void saveAllDocuments() {
        try {
            ApplicationManager.getApplication().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
        } catch (Exception ignored) {}
    }

    private File getProjectRootDir() {
        if (project == null || project.getBasePath() == null) return null;
        return new File(project.getBasePath());
    }

    private void navigateToFileLine(String relOrAbsFile, int line1Based, int col1Based) {
        File root = getProjectRootDir();
        if (root == null || relOrAbsFile == null) return;
        File target = new File(relOrAbsFile);
        if (!target.isAbsolute()) {
            target = new File(root, relOrAbsFile);
        }
        if (!target.isFile()) return;
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target);
        if (vf != null) {
            new OpenFileDescriptor(project, vf, Math.max(0, line1Based - 1), Math.max(0, col1Based - 1)).navigate(true);
        }
    }

    private void appendConsole(String line) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(line + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    private static int parseStartLine(String range) {
        if (range == null) return 1;
        int dash = range.indexOf('-');
        String first = dash > 0 ? range.substring(0, dash).trim() : range.trim();
        return parseIntSafe(first, 1);
    }

    private static int parseIntSafe(Object val, int fallback) {
        if (val == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(val).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
