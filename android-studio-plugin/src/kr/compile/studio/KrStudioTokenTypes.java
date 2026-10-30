package kr.compile.studio;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public final class KrStudioTokenTypes {

    public static final IElementType COMMENT = new IElementType("KR_COMMENT", KrStudioLanguage.INSTANCE);
    public static final IElementType PREPROCESSOR = new IElementType("KR_PREPROCESSOR", KrStudioLanguage.INSTANCE);
    public static final IElementType STRING = new IElementType("KR_STRING", KrStudioLanguage.INSTANCE);
    public static final IElementType NUMBER = new IElementType("KR_NUMBER", KrStudioLanguage.INSTANCE);

    public static final IElementType KEYWORD_CPP = new IElementType("KR_KEYWORD_CPP", KrStudioLanguage.INSTANCE);
    public static final IElementType KEYWORD_RUST = new IElementType("KR_KEYWORD_RUST", KrStudioLanguage.INSTANCE);
    public static final IElementType KEYWORD_GO = new IElementType("KR_KEYWORD_GO", KrStudioLanguage.INSTANCE);
    public static final IElementType KEYWORD_PYTHON = new IElementType("KR_KEYWORD_PYTHON", KrStudioLanguage.INSTANCE);
    public static final IElementType KEYWORD_COMMON = new IElementType("KR_KEYWORD_COMMON", KrStudioLanguage.INSTANCE);

    public static final IElementType IDENTIFIER = new IElementType("KR_IDENTIFIER", KrStudioLanguage.INSTANCE);
    public static final IElementType OPERATOR = new IElementType("KR_OPERATOR", KrStudioLanguage.INSTANCE);
    public static final IElementType BRACES = new IElementType("KR_BRACES", KrStudioLanguage.INSTANCE);
    public static final IElementType PARENS = new IElementType("KR_PARENS", KrStudioLanguage.INSTANCE);
    public static final IElementType PUNCTUATION = new IElementType("KR_PUNCTUATION", KrStudioLanguage.INSTANCE);

    public static final TokenSet COMMENTS = TokenSet.create(COMMENT);
    public static final TokenSet STRINGS = TokenSet.create(STRING);

    private KrStudioTokenTypes() {}
}
