package kr.compile.studio;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import kr.compile.CompilerEngine;

public final class KrBuildAndRunDeviceAction extends AnAction {

    public KrBuildAndRunDeviceAction() {
        super("KR: Android ARM64 APK bauen & auf Gerät starten (ADB)",
                "Kompiliert das KR-Projekt als signierte ARM64-APK, installiert sie per ADB und startet sie auf dem verbundenen Android-Gerät",
                KrStudioIcons.KR_TOOL_WINDOW_ICON);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("KR Unified Language");
        if (tw != null) {
            tw.activate(() -> {
                KrToolWindowPanel panel = KrToolWindowFactory.getPanel(project);
                if (panel != null) {
                    panel.runBuild(CompilerEngine.Target.ANDROID, true);
                }
            });
        }
    }
}
