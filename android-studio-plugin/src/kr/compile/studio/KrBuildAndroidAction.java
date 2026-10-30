package kr.compile.studio;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import kr.compile.CompilerEngine;

public final class KrBuildAndroidAction extends AnAction {

    public KrBuildAndroidAction() {
        super("KR: Android ARM64 APK bauen (C++ / Rust / Go / Python)",
                "Kompiliert alle .kr-Dateien im Projekt über die KR Unified Pipeline zu einer signierten Android ARM64 APK und synchronisiert jniLibs/arm64-v8a",
                KrStudioIcons.KR_FILE_ICON);
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
                    panel.runBuild(CompilerEngine.Target.ANDROID, false);
                }
            });
        }
    }
}
