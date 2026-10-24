package kr.compile.windows;

import kr.compile.ProjectManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class ExplorerIconsTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Projektordner und Screenshot-Ziel erforderlich");
        File project = new File(args[0]);
        File screenshot = new File(args[1]);
        if (!project.isDirectory()) throw new AssertionError("Testprojekt fehlt: " + project);

        DarkTheme.applySettings(UiSettings.get());
        DarkTheme.installDefaults();
        final Throwable[] failure = {null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                ProjectExplorerPanel panel = new ProjectExplorerPanel(() -> {}, () -> {}, () -> {});
                JFrame frame = new JFrame("Explorer-Symboltest");
                frame.setUndecorated(true);
                frame.setSize(430, 720);
                frame.add(panel);
                frame.setVisible(true);
                ProjectManager.getInstance().openProject(project);
                frame.validate();

                BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                frame.paint(graphics);
                graphics.dispose();
                ImageIO.write(image, "png", screenshot);

                assertDifferent(ExplorerIcons.folder(false), ExplorerIcons.file(new File("main.kr")),
                        "Ordner- und Dateisymbol sind optisch nicht unterscheidbar");
                assertDifferent(ExplorerIcons.file(new File("main.kr")), ExplorerIcons.file(new File("main.rs")),
                        "KR- und Rust-Dateisymbol haben keinen unterscheidbaren Farbakzent");
                panel.disposePanel();
                frame.dispose();
            } catch (Throwable ex) {
                failure[0] = ex;
            }
        });
        if (failure[0] != null) throw new RuntimeException(failure[0]);
        System.out.println("OK: Ordner, Dateien und Dateitypen besitzen unterscheidbare Symbole.");
    }

    private static void assertDifferent(Icon first, Icon second, String message) {
        BufferedImage a = paint(first);
        BufferedImage b = paint(second);
        int differences = 0;
        for (int y = 0; y < Math.min(a.getHeight(), b.getHeight()); y++) {
            for (int x = 0; x < Math.min(a.getWidth(), b.getWidth()); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) differences++;
            }
        }
        if (differences < 18) throw new AssertionError(message + " (nur " + differences + " Pixel abweichend)");
    }

    private static BufferedImage paint(Icon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        return image;
    }
}
