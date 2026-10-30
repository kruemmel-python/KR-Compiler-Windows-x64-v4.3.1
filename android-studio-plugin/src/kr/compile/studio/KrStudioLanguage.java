package kr.compile.studio;

import com.intellij.lang.Language;

public final class KrStudioLanguage extends Language {

    public static final KrStudioLanguage INSTANCE = new KrStudioLanguage();

    private KrStudioLanguage() {
        super("KR");
    }

    @Override
    public String getDisplayName() {
        return "KR Unified Language (C++ / Rust / Go / Python)";
    }
}
