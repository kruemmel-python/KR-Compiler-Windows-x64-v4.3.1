package kr.compile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete Android & Desktop Execution Strategy for Python in KR Unified Language:
 * 1. Emits pure Python 3 module (`KR_BUILD/python_module/kr_python_module.py`) with ctypes FFI bindings.
 * 2. Emits Android execution strategy manifest (`KR_BUILD/python_module/execution_strategy.json`).
 * 3. Compiles Python segments Ahead-of-Time (AOT) into native C++20 (`KR_BUILD/python_module/kr_python_bridge.cpp`)
 *    with `extern "C"` ABI symbols so Python functions execute natively inside `libuser.so` on Android ARM64
 *    and `.exe` / ELF on desktop without requiring an external 60 MB CPython interpreter inside the APK.
 */
public final class KrPythonTranspiler {

    private static final Pattern DEF_HEADER_COLON = Pattern.compile(
            "^([ \\t]*)def[ \\t]+([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]*\\(([^)]*)\\)(?:[ \\t]*->[ \\t]*([^:{]+))?[ \\t]*:(.*)$");
    private static final Pattern DEF_HEADER_BRACE = Pattern.compile(
            "^([ \\t]*)def[ \\t]+([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]*\\(([^)]*)\\)(?:[ \\t]*->[ \\t]*([^:{]+))?[ \\t]*\\{(.*)$");
    private static final Pattern FOR_RANGE_PATTERN = Pattern.compile(
            "^for[ \\t]+([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]+in[ \\t]+range[ \\t]*\\((.*)\\)[ \\t]*:$");
    private static final Pattern TYPED_ASSIGN_PATTERN = Pattern.compile(
            "^([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]*:[ \\t]*([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]*=[ \\t]*(.+)$");
    private static final Pattern SIMPLE_ASSIGN_PATTERN = Pattern.compile(
            "^([a-zA-Z_][a-zA-Z0-9_]*)[ \\t]*=[ \\t]*(?!=)(.+)$");

    private KrPythonTranspiler() {}

    public static final class TranspileResult {
        public final File pythonDir;
        public final File pythonModuleFile;
        public final File strategyManifestFile;
        public final File bridgeCppFile;

        public TranspileResult(File pythonDir, File pythonModuleFile, File strategyManifestFile, File bridgeCppFile) {
            this.pythonDir = pythonDir;
            this.pythonModuleFile = pythonModuleFile;
            this.strategyManifestFile = strategyManifestFile;
            this.bridgeCppFile = bridgeCppFile;
        }
    }

    public static TranspileResult generate(
            File buildDir,
            List<KrSegment> segments,
            KrSymbolTable symbolTable,
            KrSourceMap sourceMap) throws IOException {

        File pyDir = new File(buildDir, "python_module");
        if (!pyDir.isDirectory() && !pyDir.mkdirs()) {
            throw new IOException("Python-Modulverzeichnis konnte nicht erstellt werden: " + pyDir);
        }

        File pyFile = new File(pyDir, "kr_python_module.py");
        File strategyFile = new File(pyDir, "execution_strategy.json");
        File bridgeCpp = new File(pyDir, "kr_python_bridge.cpp");

        // 1. Write pure Python module
        StringBuilder pySb = new StringBuilder();
        pySb.append("# -*- coding: utf-8 -*-\n");
        pySb.append("# Automatisch generiertes KR Unified Language Python-Modul\n");
        pySb.append("# Android-Ausfuehrungsstrategie: AOT_NATIVE_CPP20_BRIDGE (libuser.so / ARM64)\n\n");
        for (KrSegment seg : segments) {
            if (seg.language == KrLanguage.PYTHON) {
                pySb.append("# --- Segment aus ").append(seg.file).append(":").append(seg.startLine).append(" ---\n");
                pySb.append(seg.source).append("\n\n");
            }
        }
        writeText(pyFile, pySb.toString());

        // 2. Write Android execution strategy manifest
        List<String> pyFuncNames = new ArrayList<>();
        for (KrSegment seg : segments) {
            if (seg.language == KrLanguage.PYTHON) {
                for (KrFunction fn : seg.declaredFunctions) {
                    pyFuncNames.add(fn.name);
                }
            }
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"language\": \"Python\",\n");
        json.append("  \"androidExecutionStrategy\": \"AOT_NATIVE_CPP20_BRIDGE\",\n");
        json.append("  \"description\": \"KR kompiliert Python-Segmente Ahead-of-Time in native C++20/ARM64-ABI-Funktionen (libuser.so) mit direkter C++/Rust/Go-Sprachfusion und Android-Logcat-Ausgabe.\",\n");
        json.append("  \"targetAbis\": [\"arm64-v8a\", \"x86_64\"],\n");
        json.append("  \"nativeBridgeSource\": \"python_module/kr_python_bridge.cpp\",\n");
        json.append("  \"pythonSourceArtifact\": \"python_module/kr_python_module.py\",\n");
        json.append("  \"exportedFunctions\": [");
        for (int i = 0; i < pyFuncNames.size(); i++) {
            if (i > 0) json.append(", ");
            json.append("\"").append(pyFuncNames.get(i)).append("\"");
        }
        json.append("]\n}\n");
        writeText(strategyFile, json.toString());

        // 3. Generate native C++20 AOT bridge for Python functions
        StringBuilder cpp = new StringBuilder();
        cpp.append("// Automatisch generierte KR Python-zu-C++20 AOT Native Bridge (Android ARM64 & Desktop)\n");
        cpp.append("#include \"../kr_bridge.h\"\n");
        cpp.append("#include <iostream>\n");
        cpp.append("#include <sstream>\n");
        cpp.append("#include <string>\n");
        cpp.append("#include <cstdint>\n");
        cpp.append("#include <cmath>\n");
        cpp.append("#include <algorithm>\n");
        cpp.append("#ifdef __ANDROID__\n");
        cpp.append("#include <android/log.h>\n");
        cpp.append("#include <jni.h>\n");
        cpp.append("#endif\n\n");

        // External declarations for C++, Rust, and Go functions called from Python
        Set<KrFunction> externCalls = new LinkedHashSet<>();
        externCalls.addAll(symbolTable.getPythonCallsCpp().values());
        externCalls.addAll(symbolTable.getPythonCallsRust().values());
        externCalls.addAll(symbolTable.getPythonCallsGo().values());

        if (!externCalls.isEmpty()) {
            cpp.append("#ifdef __cplusplus\nextern \"C\" {\n#endif\n");
            for (KrFunction fn : externCalls) {
                cpp.append(fn.returnType.toCType()).append(" ").append(fn.name).append("(");
                if (fn.parameters.isEmpty()) {
                    cpp.append("void");
                } else {
                    for (int i = 0; i < fn.parameters.size(); i++) {
                        if (i > 0) cpp.append(", ");
                        cpp.append(fn.parameters.get(i).type.toCType()).append(" ").append(fn.parameters.get(i).name);
                    }
                }
                cpp.append(");\n");
            }
            cpp.append("#ifdef __cplusplus\n}\n#endif\n\n");
        }

        // Runtime helpers for Python built-ins (print, abs, min, max)
        cpp.append("namespace kr_py {\n");
        cpp.append("    template <typename... Args>\n");
        cpp.append("    inline void print(Args&&... args) {\n");
        cpp.append("        std::ostringstream oss;\n");
        cpp.append("        bool first = true;\n");
        cpp.append("        ((oss << (first ? \"\" : \" \") << args, first = false), ...);\n");
        cpp.append("        std::string out = oss.str();\n");
        cpp.append("        std::cout << out << std::endl;\n");
        cpp.append("#ifdef __ANDROID__\n");
        cpp.append("        __android_log_print(ANDROID_LOG_INFO, \"KR_PYTHON\", \"%s\", out.c_str());\n");
        cpp.append("#endif\n");
        cpp.append("    }\n");
        cpp.append("}\n\n");

        cpp.append("#ifdef __cplusplus\nextern \"C\" {\n#endif\n\n");

        for (KrSegment seg : segments) {
            if (seg.language == KrLanguage.PYTHON) {
                int startGenLine = countLines(cpp.toString()) + 1;
                cpp.append("// --- Python-Segment aus ").append(seg.file).append(":").append(seg.startLine).append(" ---\n");
                transpilePythonSegment(seg, symbolTable, cpp);
                int endGenLine = countLines(cpp.toString());
                if (sourceMap != null) {
                    List<String> fnNames = new ArrayList<>();
                    for (KrFunction fn : seg.declaredFunctions) fnNames.add(fn.name);
                    sourceMap.addMapping(new KrSourceMap.SegmentMapping(
                            "python_module/kr_python_bridge.cpp",
                            seg.file,
                            startGenLine,
                            endGenLine,
                            seg.startLine,
                            seg.endLine,
                            "PYTHON",
                            fnNames
                    ));
                }
            }
        }

        KrFunction mainFn = symbolTable.getMainFunction();
        if (mainFn != null && mainFn.definingLanguage == KrLanguage.PYTHON) {
            cpp.append("\nint32_t KrLanguageMain(void) {\n");
            if (mainFn.returnType.kind == KrType.Kind.VOID) {
                cpp.append("    ").append(mainFn.name).append("();\n");
                cpp.append("    return 0;\n");
            } else {
                cpp.append("    return static_cast<int32_t>(").append(mainFn.name).append("());\n");
            }
            cpp.append("}\n");
        }

        cpp.append("\n#ifdef __ANDROID__\n");
        cpp.append("extern \"C\" int kr_linked_main(int argc, char** argv) __asm__(\"main\");\n");
        cpp.append("extern \"C\" JNIEXPORT jint JNICALL\n");
        cpp.append("Java_kr_compile_studio_demo4lang_MainActivity_nativeRunKrMain(JNIEnv* env, jobject /*thiz*/) {\n");
        cpp.append("    (void)env;\n");
        cpp.append("    char arg0[] = \"kr-android-studio\";\n");
        cpp.append("    char* argv[] = { arg0, nullptr };\n");
        cpp.append("    return static_cast<jint>(kr_linked_main(1, argv));\n");
        cpp.append("}\n");
        cpp.append("#endif\n");
        cpp.append("\n#ifdef __cplusplus\n}\n#endif\n");
        writeText(bridgeCpp, cpp.toString());

        return new TranspileResult(pyDir, pyFile, strategyFile, bridgeCpp);
    }

    private static void transpilePythonSegment(KrSegment seg, KrSymbolTable symbolTable, StringBuilder out) {
        String[] rawLines = seg.source.replace("\r\n", "\n").split("\n");
        int i = 0;
        while (i < rawLines.length) {
            String line = rawLines[i];
            String trimmed = stripPythonComment(line).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("import ") || trimmed.startsWith("from ")) {
                i++;
                continue;
            }

            Matcher mColon = DEF_HEADER_COLON.matcher(stripPythonComment(line));
            Matcher mBrace = DEF_HEADER_BRACE.matcher(stripPythonComment(line));
            if (mColon.matches()) {
                int defIndent = countIndent(mColon.group(1));
                String fnName = mColon.group(2);
                String inlineAfterColon = mColon.group(5) != null ? mColon.group(5).trim() : "";

                KrFunction fn = findFunction(seg, symbolTable, fnName);
                emitCppFunctionHeader(fn, fnName, out);

                Set<String> localVars = new HashSet<>();
                if (fn != null) {
                    for (KrFunction.Parameter p : fn.parameters) {
                        localVars.add(p.name);
                    }
                }

                if (!inlineAfterColon.isEmpty()) {
                    out.append("    ").append(translateStatement(inlineAfterColon, localVars, fn)).append("\n");
                    i++;
                } else {
                    i++;
                    Deque<Integer> indentStack = new ArrayDeque<>();
                    while (i < rawLines.length) {
                        String bodyRaw = rawLines[i];
                        String bodyClean = stripPythonComment(bodyRaw);
                        String bodyTrim = bodyClean.trim();
                        if (bodyTrim.isEmpty()) {
                            i++;
                            continue;
                        }
                        int curIndent = countIndent(bodyClean);
                        if (curIndent <= defIndent) {
                            break; // Dedented back to top-level
                        }

                        if (indentStack.isEmpty()) {
                            indentStack.push(curIndent);
                        } else {
                            while (indentStack.size() > 1 && curIndent < indentStack.peek()) {
                                indentStack.pop();
                                out.append(indentSpaces(indentStack.size())).append("}\n");
                            }
                            if (curIndent < indentStack.peek()) {
                                indentStack.pop();
                                indentStack.push(curIndent);
                            }
                        }

                        int depth = indentStack.size();
                        if (bodyTrim.startsWith("elif ") && bodyTrim.endsWith(":")) {
                            String cond = bodyTrim.substring(5, bodyTrim.length() - 1).trim();
                            out.append(indentSpaces(depth)).append("else if (").append(translateExpr(cond)).append(") {\n");
                            int nextIndent = peekNextIndent(rawLines, i + 1, curIndent + 4);
                            indentStack.push(nextIndent);
                        } else if (bodyTrim.equals("else:")) {
                            out.append(indentSpaces(depth)).append("else {\n");
                            int nextIndent = peekNextIndent(rawLines, i + 1, curIndent + 4);
                            indentStack.push(nextIndent);
                        } else if (bodyTrim.startsWith("if ") && bodyTrim.endsWith(":")) {
                            String cond = bodyTrim.substring(3, bodyTrim.length() - 1).trim();
                            out.append(indentSpaces(depth)).append("if (").append(translateExpr(cond)).append(") {\n");
                            int nextIndent = peekNextIndent(rawLines, i + 1, curIndent + 4);
                            indentStack.push(nextIndent);
                        } else if (bodyTrim.startsWith("while ") && bodyTrim.endsWith(":")) {
                            String cond = bodyTrim.substring(6, bodyTrim.length() - 1).trim();
                            out.append(indentSpaces(depth)).append("while (").append(translateExpr(cond)).append(") {\n");
                            int nextIndent = peekNextIndent(rawLines, i + 1, curIndent + 4);
                            indentStack.push(nextIndent);
                        } else if (FOR_RANGE_PATTERN.matcher(bodyTrim).matches()) {
                            Matcher fm = FOR_RANGE_PATTERN.matcher(bodyTrim);
                            fm.matches();
                            String loopVar = fm.group(1);
                            String rangeArgs = fm.group(2);
                            localVars.add(loopVar);
                            out.append(indentSpaces(depth)).append(translateForRange(loopVar, rangeArgs)).append(" {\n");
                            int nextIndent = peekNextIndent(rawLines, i + 1, curIndent + 4);
                            indentStack.push(nextIndent);
                        } else {
                            out.append(indentSpaces(depth)).append(translateStatement(bodyTrim, localVars, fn)).append("\n");
                        }
                        i++;
                    }
                    while (indentStack.size() > 1) {
                        indentStack.pop();
                        out.append(indentSpaces(indentStack.size())).append("}\n");
                    }
                }
                out.append("}\n\n");
            } else if (mBrace.matches()) {
                String fnName = mBrace.group(2);
                KrFunction fn = findFunction(seg, symbolTable, fnName);
                emitCppFunctionHeader(fn, fnName, out);
                Set<String> localVars = new HashSet<>();
                if (fn != null) {
                    for (KrFunction.Parameter p : fn.parameters) localVars.add(p.name);
                }
                int braceDepth = 1;
                i++;
                while (i < rawLines.length && braceDepth > 0) {
                    String bClean = stripPythonComment(rawLines[i]).trim();
                    for (int c = 0; c < bClean.length(); c++) {
                        if (bClean.charAt(c) == '{') braceDepth++;
                        else if (bClean.charAt(c) == '}') braceDepth--;
                    }
                    if (braceDepth > 0 && !bClean.isEmpty()) {
                        out.append("    ").append(translateStatement(bClean, localVars, fn)).append("\n");
                    }
                    i++;
                }
                out.append("}\n\n");
            } else {
                i++;
            }
        }
    }

    private static KrFunction findFunction(KrSegment seg, KrSymbolTable symbolTable, String name) {
        for (KrFunction fn : seg.declaredFunctions) {
            if (fn.name.equals(name)) return fn;
        }
        return symbolTable != null ? symbolTable.getSymbols().get(name) : null;
    }

    private static void emitCppFunctionHeader(KrFunction fn, String fallbackName, StringBuilder out) {
        if (fn == null) {
            out.append("int32_t ").append(fallbackName).append("(void) {\n");
            return;
        }
        out.append(fn.returnType.toCType()).append(" ").append(fn.name).append("(");
        if (fn.parameters.isEmpty()) {
            out.append("void");
        } else {
            for (int i = 0; i < fn.parameters.size(); i++) {
                if (i > 0) out.append(", ");
                out.append(fn.parameters.get(i).type.toCType()).append(" ").append(fn.parameters.get(i).name);
            }
        }
        out.append(") {\n");
    }

    private static String translateForRange(String loopVar, String rangeArgs) {
        List<String> parts = splitTopLevelArgs(rangeArgs);
        if (parts.size() == 1) {
            return "for (int64_t " + loopVar + " = 0; " + loopVar + " < (" + translateExpr(parts.get(0)) + "); ++" + loopVar + ")";
        } else if (parts.size() == 2) {
            return "for (int64_t " + loopVar + " = (" + translateExpr(parts.get(0)) + "); " + loopVar + " < (" + translateExpr(parts.get(1)) + "); ++" + loopVar + ")";
        } else if (parts.size() >= 3) {
            String step = translateExpr(parts.get(2));
            return "for (int64_t " + loopVar + " = (" + translateExpr(parts.get(0)) + "); " + loopVar + " < (" + translateExpr(parts.get(1)) + "); " + loopVar + " += (" + step + "))";
        }
        return "for (int64_t " + loopVar + " = 0; " + loopVar + " < 0; ++" + loopVar + ")";
    }

    private static String translateStatement(String stmt, Set<String> localVars, KrFunction fn) {
        String s = stmt.trim();
        if (s.endsWith(";")) s = s.substring(0, s.length() - 1).trim();
        if (s.equals("pass")) return "/* pass */";

        if (s.equals("return") || s.equals("return None")) {
            if (fn != null && fn.returnType.kind != KrType.Kind.VOID) {
                return "return 0;";
            }
            return "return;";
        }
        if (s.startsWith("return ")) {
            String expr = s.substring(7).trim();
            return "return " + translateExpr(expr) + ";";
        }

        if (s.startsWith("print(") && s.endsWith(")")) {
            String inside = s.substring(6, s.length() - 1).trim();
            List<String> args = splitTopLevelArgs(inside);
            StringBuilder pb = new StringBuilder("kr_py::print(");
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) pb.append(", ");
                pb.append(translateExpr(args.get(i)));
            }
            pb.append(");");
            return pb.toString();
        }

        Matcher mTyped = TYPED_ASSIGN_PATTERN.matcher(s);
        if (mTyped.matches()) {
            String varName = mTyped.group(1);
            String typeName = mTyped.group(2);
            String rhs = translateExpr(mTyped.group(3));
            if (localVars.add(varName)) {
                KrType kt = KrType.fromString(typeName);
                String cType = (kt.kind != KrType.Kind.UNKNOWN && kt.kind != KrType.Kind.STRUCT) ? kt.toCType() : "auto";
                return cType + " " + varName + " = " + rhs + ";";
            } else {
                return varName + " = " + rhs + ";";
            }
        }

        Matcher mAssign = SIMPLE_ASSIGN_PATTERN.matcher(s);
        if (mAssign.matches()) {
            String varName = mAssign.group(1);
            String rhs = translateExpr(mAssign.group(2));
            if (localVars.add(varName)) {
                return "auto " + varName + " = " + rhs + ";";
            } else {
                return varName + " = " + rhs + ";";
            }
        }

        return translateExpr(s) + ";";
    }

    private static String translateExpr(String expr) {
        if (expr == null) return "";
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = expr.length();
        while (i < n) {
            char c = expr.charAt(i);
            if (c == '"' || c == '\'') {
                char quote = c;
                out.append('"');
                i++;
                while (i < n) {
                    char ch = expr.charAt(i++);
                    if (ch == '\\' && i < n) {
                        out.append('\\').append(expr.charAt(i++));
                    } else if (ch == quote) {
                        break;
                    } else if (ch == '"') {
                        out.append("\\\"");
                    } else {
                        out.append(ch);
                    }
                }
                out.append('"');
                continue;
            }
            if (c == '/' && i + 1 < n && expr.charAt(i + 1) == '/') {
                out.append('/');
                i += 2;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    i++;
                }
                String word = expr.substring(start, i);
                switch (word) {
                    case "True" -> out.append("true");
                    case "False" -> out.append("false");
                    case "None" -> out.append("0");
                    case "and" -> out.append("&&");
                    case "or" -> out.append("||");
                    case "not" -> out.append("!");
                    case "abs" -> out.append("std::abs");
                    case "min" -> out.append("std::min");
                    case "max" -> out.append("std::max");
                    default -> out.append(word);
                }
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString().trim();
    }

    private static List<String> splitTopLevelArgs(String args) {
        List<String> list = new ArrayList<>();
        if (args == null || args.trim().isEmpty()) return list;
        int depth = 0;
        boolean inQuote = false;
        char quoteChar = 0;
        int start = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (inQuote) {
                if (c == '\\') i++;
                else if (c == quoteChar) inQuote = false;
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    if (depth > 0) depth--;
                } else if (c == ',' && depth == 0) {
                    list.add(args.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        String tail = args.substring(start).trim();
        if (!tail.isEmpty()) list.add(tail);
        return list;
    }

    private static String stripPythonComment(String line) {
        if (line == null) return "";
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '\\') i++;
                else if (c == quoteChar) inQuote = false;
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (c == '#') {
                    return line.substring(0, i);
                }
            }
        }
        return line;
    }

    private static int countIndent(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') count++;
            else if (c == '\t') count += 4;
            else break;
        }
        return count;
    }

    private static int peekNextIndent(String[] lines, int startIdx, int fallback) {
        int idx = findNextNonEmptyLine(lines, startIdx);
        if (idx < lines.length) {
            return countIndent(stripPythonComment(lines[idx]));
        }
        return fallback;
    }

    private static int findNextNonEmptyLine(String[] lines, int startIdx) {
        for (int i = startIdx; i < lines.length; i++) {
            String trimmed = stripPythonComment(lines[i]).trim();
            if (!trimmed.isEmpty()) return i;
        }
        return lines.length;
    }

    private static String indentSpaces(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("    ");
        return sb.toString();
    }

    private static int countLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        return lines;
    }

    private static void writeText(File file, String text) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
