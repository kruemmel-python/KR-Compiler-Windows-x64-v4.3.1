package kr.compile.studio;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public final class KrCreateDemoProjectAction extends AnAction {

    public KrCreateDemoProjectAction() {
        super("KR: 4-Sprachen-Projektvorlage (C++ / Rust / Go / Python) initialisieren",
                "Erzeugt main.kr, project.krproject und kr-android-bridge.gradle.kts mit C++20-, Rust-, Go- und Python-Sprachfusion",
                KrStudioIcons.KR_PROJECT_ICON);
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
                    panel.createFourLangDemo();
                }
            });
        }
    }
}
