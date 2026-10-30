package kr.compile.studio;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.Icon;

public final class KrStudioFileType extends LanguageFileType {

    public static final KrStudioFileType INSTANCE = new KrStudioFileType();

    private KrStudioFileType() {
        super(KrStudioLanguage.INSTANCE);
    }

    @Override
    public String getName() {
        return "KR Unified Language";
    }

    @Override
    public String getDescription() {
        return "KR Unified Language source file (C++20, Rust, Go, Python)";
    }

    @Override
    public String getDefaultExtension() {
        return "kr";
    }

    @Override
    public Icon getIcon() {
        return KrStudioIcons.KR_FILE_ICON;
    }
}
