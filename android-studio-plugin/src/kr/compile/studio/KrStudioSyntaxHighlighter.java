package kr.compile.studio;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.JBColor;

import java.awt.Color;
import java.awt.Font;

public final class KrStudioSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey COMMENT_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey PREPROCESSOR_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_PREPROCESSOR", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey STRING_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_NUMBER", DefaultLanguageHighlighterColors.NUMBER);

    // Distinct language accent colors for the 4 KR languages
    public static final TextAttributesKey CPP_KEYWORD_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_CPP_KEYWORD",
            new TextAttributes(new JBColor(new Color(0, 95, 195), new Color(86, 156, 214)), null, null, null, Font.BOLD));

    public static final TextAttributesKey RUST_KEYWORD_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_RUST_KEYWORD",
            new TextAttributes(new JBColor(new Color(185, 75, 20), new Color(235, 135, 75)), null, null, null, Font.BOLD));

    public static final TextAttributesKey GO_KEYWORD_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_GO_KEYWORD",
            new TextAttributes(new JBColor(new Color(0, 130, 145), new Color(78, 201, 176)), null, null, null, Font.BOLD));

    public static final TextAttributesKey PYTHON_KEYWORD_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_PYTHON_KEYWORD",
            new TextAttributes(new JBColor(new Color(145, 45, 165), new Color(197, 134, 192)), null, null, null, Font.BOLD));

    public static final TextAttributesKey COMMON_KEYWORD_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_COMMON_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey IDENTIFIER_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey OPERATOR_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey BRACES_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey PARENS_KEY = TextAttributesKey.createTextAttributesKey(
            "KR_PARENS", DefaultLanguageHighlighterColors.PARENTHESES);

    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT_KEY};
    private static final TextAttributesKey[] PREPROCESSOR_KEYS = new TextAttributesKey[]{PREPROCESSOR_KEY};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING_KEY};
    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER_KEY};
    private static final TextAttributesKey[] CPP_KEYS = new TextAttributesKey[]{CPP_KEYWORD_KEY};
    private static final TextAttributesKey[] RUST_KEYS = new TextAttributesKey[]{RUST_KEYWORD_KEY};
    private static final TextAttributesKey[] GO_KEYS = new TextAttributesKey[]{GO_KEYWORD_KEY};
    private static final TextAttributesKey[] PYTHON_KEYS = new TextAttributesKey[]{PYTHON_KEYWORD_KEY};
    private static final TextAttributesKey[] COMMON_KEYS = new TextAttributesKey[]{COMMON_KEYWORD_KEY};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER_KEY};
    private static final TextAttributesKey[] OPERATOR_KEYS = new TextAttributesKey[]{OPERATOR_KEY};
    private static final TextAttributesKey[] BRACES_KEYS = new TextAttributesKey[]{BRACES_KEY};
    private static final TextAttributesKey[] PARENS_KEYS = new TextAttributesKey[]{PARENS_KEY};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @Override
    public Lexer getHighlightingLexer() {
        return new KrStudioLexer();
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType == KrStudioTokenTypes.COMMENT) return COMMENT_KEYS;
        if (tokenType == KrStudioTokenTypes.PREPROCESSOR) return PREPROCESSOR_KEYS;
        if (tokenType == KrStudioTokenTypes.STRING) return STRING_KEYS;
        if (tokenType == KrStudioTokenTypes.NUMBER) return NUMBER_KEYS;
        if (tokenType == KrStudioTokenTypes.KEYWORD_CPP) return CPP_KEYS;
        if (tokenType == KrStudioTokenTypes.KEYWORD_RUST) return RUST_KEYS;
        if (tokenType == KrStudioTokenTypes.KEYWORD_GO) return GO_KEYS;
        if (tokenType == KrStudioTokenTypes.KEYWORD_PYTHON) return PYTHON_KEYS;
        if (tokenType == KrStudioTokenTypes.KEYWORD_COMMON) return COMMON_KEYS;
        if (tokenType == KrStudioTokenTypes.IDENTIFIER) return IDENTIFIER_KEYS;
        if (tokenType == KrStudioTokenTypes.OPERATOR) return OPERATOR_KEYS;
        if (tokenType == KrStudioTokenTypes.BRACES) return BRACES_KEYS;
        if (tokenType == KrStudioTokenTypes.PARENS) return PARENS_KEYS;
        return EMPTY_KEYS;
    }
}
