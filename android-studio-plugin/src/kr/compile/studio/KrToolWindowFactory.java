package kr.compile.studio;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KrToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Map<Project, KrToolWindowPanel> PANELS = new ConcurrentHashMap<>();

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        KrToolWindowPanel panel = new KrToolWindowPanel(project);
        PANELS.put(project, panel);
        Content content = ContentFactory.getInstance().createContent(panel, "KR Unified Pipeline", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static KrToolWindowPanel getPanel(Project project) {
        return project != null ? PANELS.get(project) : null;
    }
}
