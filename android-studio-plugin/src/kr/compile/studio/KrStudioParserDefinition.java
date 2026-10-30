package kr.compile.studio;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

public final class KrStudioParserDefinition implements ParserDefinition {

    public static final IFileElementType FILE = new IFileElementType(KrStudioLanguage.INSTANCE);
    private static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);

    @Override
    public Lexer createLexer(Project project) {
        return new KrStudioLexer();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new PsiParser() {
            @Override
            public ASTNode parse(IElementType root, PsiBuilder builder) {
                PsiBuilder.Marker rootMarker = builder.mark();
                while (!builder.eof()) {
                    builder.advanceLexer();
                }
                rootMarker.done(root);
                return builder.getTreeBuilt();
            }
        };
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @Override
    public TokenSet getCommentTokens() {
        return KrStudioTokenTypes.COMMENTS;
    }

    @Override
    public TokenSet getStringLiteralElements() {
        return KrStudioTokenTypes.STRINGS;
    }

    @Override
    public PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new KrStudioPsiFile(viewProvider);
    }

    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
