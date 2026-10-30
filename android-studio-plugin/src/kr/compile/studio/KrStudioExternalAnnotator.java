package kr.compile.studio;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import kr.compile.KrDiagnostic;
import kr.compile.KrFunction;
import kr.compile.KrLanguage;
import kr.compile.KrParser;
import kr.compile.KrSegment;
import kr.compile.KrSymbolTable;

import java.util.ArrayList;
import java.util.List;

public final class KrStudioExternalAnnotator extends ExternalAnnotator<KrStudioExternalAnnotator.CollectedInfo, KrStudioExternalAnnotator.AnnotationResult> {

    public static final class CollectedInfo {
        public final String fileName;
        public final String text;

        public CollectedInfo(String fileName, String text) {
            this.fileName = fileName;
            this.text = text;
        }
    }

    public static final class AnnotationResult {
        public final List<KrSegment> segments = new ArrayList<>();
        public final List<KrDiagnostic> diagnostics = new ArrayList<>();
        public final KrSymbolTable symbolTable = new KrSymbolTable();
    }

    @Override
    public CollectedInfo collectInformation(PsiFile file) {
        if (file == null) return null;
        return new CollectedInfo(file.getName(), file.getText());
    }

    @Override
    public CollectedInfo collectInformation(PsiFile file, Editor editor, boolean hasErrors) {
        return collectInformation(file);
    }

    @Override
    public AnnotationResult doAnnotate(CollectedInfo info) {
        if (info == null || info.text == null) return null;
        AnnotationResult res = new AnnotationResult();
        KrParser parser = new KrParser(info.fileName, info.text);
        List<KrSegment> parsed = parser.parse();
        res.segments.addAll(parsed);
        res.diagnostics.addAll(parser.getDiagnostics());

        for (KrSegment seg : parsed) {
            for (KrFunction fn : seg.declaredFunctions) {
                res.symbolTable.registerFunction(fn);
            }
        }
        res.symbolTable.resolveCalls(parsed);
        res.diagnostics.addAll(res.symbolTable.getDiagnostics());
        return res;
    }

    @Override
    public void apply(PsiFile file, AnnotationResult result, AnnotationHolder holder) {
        if (file == null || result == null || holder == null) return;
        Document doc = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (doc == null) return;

        int lineCount = doc.getLineCount();
        int docLength = doc.getTextLength();

        // 1. Highlight KR Diagnostics (Errors & Warnings) on exact source lines
        for (KrDiagnostic diag : result.diagnostics) {
            int lineIdx = Math.max(0, Math.min(lineCount - 1, diag.line - 1));
            if (lineCount == 0) continue;
            int startOff = doc.getLineStartOffset(lineIdx);
            int endOff = doc.getLineEndOffset(lineIdx);
            if (endOff <= startOff) endOff = Math.min(docLength, startOff + 1);
            TextRange range = new TextRange(startOff, endOff);

            HighlightSeverity sev = diag.severity == KrDiagnostic.Severity.ERROR
                    ? HighlightSeverity.ERROR
                    : HighlightSeverity.WARNING;
            holder.newAnnotation(sev, "[KR Unified Compiler] " + diag.message)
                    .range(range)
                    .create();
        }

        // 2. Annotate detected KR language segments & cross-language ABI functions
        for (KrSegment seg : result.segments) {
            for (KrFunction fn : seg.declaredFunctions) {
                int lineIdx = Math.max(0, Math.min(lineCount - 1, fn.line - 1));
                if (lineCount == 0) continue;
                int startOff = doc.getLineStartOffset(lineIdx);
                int endOff = doc.getLineEndOffset(lineIdx);
                if (endOff <= startOff) continue;

                boolean exported = result.symbolTable.isExported(fn.name);
                String strategyNote = fn.definingLanguage == KrLanguage.PYTHON
                        ? " | Android-Strategie: AOT_NATIVE_CPP20_BRIDGE (libuser.so)"
                        : "";
                String abiNote = exported ? " [KR Cross-Language ABI Export -> kr_bridge.h]" : "";
                String tooltip = "KR Sprachsegment: " + fn.definingLanguage.getDisplayName()
                        + " | Signatur: " + fn + abiNote + strategyNote;

                holder.newAnnotation(HighlightSeverity.INFORMATION, tooltip)
                        .range(new TextRange(startOff, endOff))
                        .tooltip(tooltip)
                        .create();
            }
        }
    }
}
