package kr.compile.studio;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;

public final class KrStudioPsiFile extends PsiFileBase {

    public KrStudioPsiFile(FileViewProvider viewProvider) {
        super(viewProvider, KrStudioLanguage.INSTANCE);
    }

    @Override
    public FileType getFileType() {
        return KrStudioFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "KR Unified Language File";
    }
}
