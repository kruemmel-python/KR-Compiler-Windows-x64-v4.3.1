package kr.compile.studio;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class KrStudioLexer extends LexerBase {

    private static final Set<String> CPP_KEYWORDS = new HashSet<>();
    private static final Set<String> RUST_KEYWORDS = new HashSet<>();
    private static final Set<String> GO_KEYWORDS = new HashSet<>();
    private static final Set<String> PYTHON_KEYWORDS = new HashSet<>();
    private static final Set<String> COMMON_KEYWORDS = new HashSet<>();
    private static final Set<String> CPP_PREPROCESSOR = new HashSet<>();

    static {
        Collections.addAll(CPP_KEYWORDS,
                "class", "namespace", "template", "typename", "constexpr", "consteval", "constinit",
                "concept", "requires", "decltype", "using", "virtual", "override", "final",
                "explicit", "friend", "operator", "noexcept", "static_assert",
                "static_cast", "dynamic_cast", "const_cast", "reinterpret_cast",
                "new", "delete", "this", "throw", "try", "catch", "std", "cout", "cin", "endl",
                "int8_t", "int16_t", "int32_t", "int64_t", "uint8_t", "uint16_t", "uint32_t", "uint64_t", "size_t");

        Collections.addAll(RUST_KEYWORDS,
                "fn", "pub", "impl", "trait", "match", "let", "mut", "use", "mod", "crate",
                "super", "where", "unsafe", "async", "await", "loop", "move", "ref",
                "i8", "i16", "i32", "i64", "i128", "isize",
                "u8", "u16", "u32", "u64", "u128", "usize",
                "f32", "f64", "println");

        Collections.addAll(GO_KEYWORDS,
                "func", "package", "import", "var", "type", "chan", "defer", "select",
                "fallthrough", "range", "nil", "make", "append",
                "int8", "int16", "int32", "int64",
                "uint8", "uint16", "uint32", "uint64", "uintptr",
                "float32", "float64", "complex64", "complex128");

        Collections.addAll(PYTHON_KEYWORDS,
                "def", "elif", "pass", "None", "True", "False", "and", "or", "not",
                "lambda", "yield", "with", "from", "nonlocal", "except", "finally", "raise", "print", "in");

        Collections.addAll(COMMON_KEYWORDS,
                "struct", "enum", "if", "else", "for", "while", "return", "const",
                "break", "continue", "switch", "case", "default", "bool", "true", "false",
                "int", "void", "char", "short", "long", "float", "double", "unsigned", "signed", "string", "str");

        Collections.addAll(CPP_PREPROCESSOR,
                "include", "define", "undef", "if", "ifdef", "ifndef", "else", "elif",
                "endif", "pragma", "error", "warning", "line");
    }

    private CharSequence buffer = "";
    private int startOffset = 0;
    private int endOffset = 0;
    private int tokenStart = 0;
    private int tokenEnd = 0;
    private IElementType tokenType = null;

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer != null ? buffer : "";
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenStart = startOffset;
        this.tokenEnd = startOffset;
        this.tokenType = null;
        advance();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        tokenStart = tokenEnd;
        if (tokenStart >= endOffset) {
            tokenType = null;
            return;
        }

        char c = buffer.charAt(tokenStart);

        // 1. Whitespace
        if (Character.isWhitespace(c)) {
            int p = tokenStart + 1;
            while (p < endOffset && Character.isWhitespace(buffer.charAt(p))) {
                p++;
            }
            tokenEnd = p;
            tokenType = TokenType.WHITE_SPACE;
            return;
        }

        // 2. Line or block comment // or /*
        if (c == '/' && tokenStart + 1 < endOffset) {
            char next = buffer.charAt(tokenStart + 1);
            if (next == '/') {
                int p = tokenStart + 2;
                while (p < endOffset && buffer.charAt(p) != '\n') p++;
                tokenEnd = p;
                tokenType = KrStudioTokenTypes.COMMENT;
                return;
            } else if (next == '*') {
                int p = tokenStart + 2;
                while (p < endOffset) {
                    if (buffer.charAt(p) == '*' && p + 1 < endOffset && buffer.charAt(p + 1) == '/') {
                        p += 2;
                        break;
                    }
                    p++;
                }
                tokenEnd = p;
                tokenType = KrStudioTokenTypes.COMMENT;
                return;
            }
        }

        // 3. '#' Preprocessor (#include, #[...]) or Python comment (# ...)
        if (c == '#') {
            int p = tokenStart + 1;
            while (p < endOffset && buffer.charAt(p) != '\n') p++;
            String lineText = buffer.subSequence(tokenStart, p).toString().trim();
            tokenEnd = p;
            if (isPreprocessorOrAttr(lineText)) {
                tokenType = KrStudioTokenTypes.PREPROCESSOR;
            } else {
                tokenType = KrStudioTokenTypes.COMMENT;
            }
            return;
        }

        // 4. Strings: "...", '...', `...`
        if (c == '"' || c == '\'' || c == '`') {
            char quote = c;
            int p = tokenStart + 1;
            while (p < endOffset) {
                char ch = buffer.charAt(p++);
                if (ch == '\\' && quote != '`' && p < endOffset) {
                    p++;
                } else if (ch == quote) {
                    break;
                } else if (ch == '\n' && quote != '`') {
                    break;
                }
            }
            tokenEnd = p;
            tokenType = KrStudioTokenTypes.STRING;
            return;
        }

        // 5. Numbers
        if (Character.isDigit(c)) {
            int p = tokenStart + 1;
            while (p < endOffset) {
                char ch = buffer.charAt(p);
                if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '_') p++;
                else break;
            }
            tokenEnd = p;
            tokenType = KrStudioTokenTypes.NUMBER;
            return;
        }

        // 6. Identifiers and keywords (C++, Rust, Go, Python, Common)
        if (Character.isLetter(c) || c == '_') {
            int p = tokenStart + 1;
            while (p < endOffset) {
                char ch = buffer.charAt(p);
                if (Character.isLetterOrDigit(ch) || ch == '_') p++;
                else break;
            }
            tokenEnd = p;
            String word = buffer.subSequence(tokenStart, tokenEnd).toString();
            if (PYTHON_KEYWORDS.contains(word)) {
                tokenType = KrStudioTokenTypes.KEYWORD_PYTHON;
            } else if (RUST_KEYWORDS.contains(word)) {
                tokenType = KrStudioTokenTypes.KEYWORD_RUST;
            } else if (GO_KEYWORDS.contains(word)) {
                tokenType = KrStudioTokenTypes.KEYWORD_GO;
            } else if (CPP_KEYWORDS.contains(word)) {
                tokenType = KrStudioTokenTypes.KEYWORD_CPP;
            } else if (COMMON_KEYWORDS.contains(word)) {
                tokenType = KrStudioTokenTypes.KEYWORD_COMMON;
            } else {
                tokenType = KrStudioTokenTypes.IDENTIFIER;
            }
            return;
        }

        // 7. Braces, Parens, Operators, Punctuation
        tokenEnd = tokenStart + 1;
        switch (c) {
            case '{', '}' -> tokenType = KrStudioTokenTypes.BRACES;
            case '(', ')', '[', ']' -> tokenType = KrStudioTokenTypes.PARENS;
            case '+', '-', '*', '/', '%', '=', '<', '>', '!', '&', '|', '^', '~' -> tokenType = KrStudioTokenTypes.OPERATOR;
            default -> tokenType = KrStudioTokenTypes.PUNCTUATION;
        }
    }

    private static boolean isPreprocessorOrAttr(String text) {
        if (text == null || !text.startsWith("#")) return false;
        String rest = text.substring(1).trim();
        if (rest.startsWith("[") || rest.startsWith("![")) return true;
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < rest.length(); i++) {
            char ch = rest.charAt(i);
            if (Character.isLetter(ch) || ch == '_') word.append(ch);
            else break;
        }
        return CPP_PREPROCESSOR.contains(word.toString().toLowerCase(Locale.ROOT));
    }

    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}
