package kr.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.MessageDigest;
import java.util.*;

/**
 * Verwaltet die integrierte, portable Compiler-Toolchain für Windows.
 * Genau wie in der Android-Version (ToolchainInstaller) macht dies die Windows IDE
 * vollkommen autark: Der Benutzer muss KEINE Compiler, SDKs oder MSYS2 vorinstallieren.
 */
public final class ToolchainManager {

    private static ToolchainManager instance;
    private File toolchainDir;
    private File verifiedToolchainDir;

    private static final Map<String, String> PINNED_SHA256 = Map.ofEntries(
            Map.entry("bin/g++.exe", "f6eaf80bfa0a4a01c09971c8d528be117f74e37d9272d72d619c216450af39be"),
            Map.entry("bin/gcc.exe", "6b79eab38d1d331b257fcfc804f90fb41ea17b34989c6b4ec0c13c3a7d778bdf"),
            Map.entry("go/bin/go.exe", "7d828191ba32519a9c9361789ab647486236ed45c660889196c7770a8ff1985c"),
            Map.entry("rust/bin/cargo.exe", "41e71e5b21356b87bf061f931fdbd4af7769bfaa269494c07292d3acc43f2d8a"),
            Map.entry("rust/bin/rustc.exe", "c3d5ce2bcdac26fd3c9394d30720b8f320e1e0f4ad03e337e014b3d3a592d0e5"),
            Map.entry("zig/zig-x86_64-windows-0.17.0/zig.exe", "f54b14f02a4d7d8ac62a94a0663486eb166bba3745d85b466238c1b93e723d46"),
            Map.entry("zig/zig-linux-cc.cmd", "1775f1001378811ca359065417492ad07343a440dd96b5a38f884897f4a25050"),
            Map.entry("android/runner-template.apk", "fa99c08ee75c8e20c1bbf603a7c2d1474ef6ab996f867d45802566fa82b56897"),
            Map.entry("android/libc++_shared.so", "ab4e6c71b96b851de45a8a9bd86369e7dbc2130a44b3b4520564be94847910f2"),
            Map.entry("android/libOpenCL.so", "fee345c303f3d224405bdc6bd77312aaa06c3772709b4909c2b320f902de0974"),
            Map.entry("android/libSDL3.so", "efd37dba05ce508e8d2b9770a0f75128a0a69c46ac484525d7a3c598cd10befa"),
            Map.entry("android/libsqlite3.so", "42e3cd67ae16bbb82f6e3b1190f45ee01970dcff2cdb56585e66eb9198e3b46d")
    );

    public static synchronized ToolchainManager getInstance() {
        if (instance == null) {
            instance = new ToolchainManager();
        }
        return instance;
    }

    private ToolchainManager() {
        locateToolchain();
    }

    private void locateToolchain() {
        File baseDir = getApplicationDirectory();

        // 1. Direkt im Verzeichnis "toolchain" neben dem Programm (z.B. windows/toolchain)
        File candidate = new File(baseDir, "toolchain");
        if (isValidToolchain(candidate)) {
            toolchainDir = candidate;
            return;
        }

        // 2. Im übergeordneten Ordner (falls die JAR in dist/ gestartet wurde)
        if (baseDir.getParentFile() != null) {
            candidate = new File(baseDir.getParentFile(), "toolchain");
            if (isValidToolchain(candidate)) {
                toolchainDir = candidate;
                return;
            }
        }

        // Standard-Fallback
        toolchainDir = new File(baseDir, "toolchain");
    }

    public synchronized boolean isValidToolchain(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        try {
            File canonical = dir.getCanonicalFile();
            if (Files.isSymbolicLink(dir.toPath()) || Files.readAttributes(dir.toPath(), java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther()) return false;
            if (verifiedToolchainDir != null && verifiedToolchainDir.equals(canonical)) return true;
            for (Map.Entry<String, String> entry : PINNED_SHA256.entrySet()) {
                File file = new File(canonical, entry.getKey()).getCanonicalFile();
                if (!file.toPath().startsWith(canonical.toPath()) || !file.isFile() || Files.isSymbolicLink(file.toPath())) return false;
                if (!sha256(file).equals(entry.getValue())) return false;
            }
            verifiedToolchainDir = canonical;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[128 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
        }
        StringBuilder value = new StringBuilder(64);
        for (byte b : digest.digest()) value.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return value.toString();
    }

    public File getToolchainDir() {
        return toolchainDir;
    }

    public boolean isEmbeddedToolchainPresent() {
        return isValidToolchain(toolchainDir);
    }

    public String getCompilerExecutable() {
        if (isValidToolchain(toolchainDir)) {
            File bin = new File(toolchainDir, "bin");
            File gpp = new File(bin, "g++.exe");
            if (gpp.isFile()) return gpp.getAbsolutePath();
            File clang = new File(bin, "clang++.exe");
            if (clang.isFile()) return clang.getAbsolutePath();
        }
        return null;
    }

    /** Portable Zig compiler used for genuine Linux ELF cross-builds on Windows. */
    public String getZigExecutable() {
        if (toolchainDir == null) return null;
        File zigRoot = new File(toolchainDir, "zig");
        File[] bundles = zigRoot.listFiles(File::isDirectory);
        if (bundles != null) {
            Arrays.sort(bundles, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
            for (File bundle : bundles) {
                File zig = new File(bundle, "zig.exe");
                if (zig.isFile()) return zig.getAbsolutePath();
            }
        }
        File direct = new File(zigRoot, "zig.exe");
        return direct.isFile() ? direct.getAbsolutePath() : null;
    }

    public String getZigLinuxLinkerExecutable() {
        String zig = getZigExecutable();
        if (zig == null || toolchainDir == null) return null;
        File wrapper = new File(new File(toolchainDir, "zig"), "zig-linux-cc.cmd");
        try {
            Files.writeString(wrapper.toPath(), "@echo off\r\n\"" + zig + "\" cc -target x86_64-linux-gnu %*\r\n", StandardCharsets.UTF_8);
            return wrapper.getAbsolutePath();
        } catch (Exception ignored) {
            return null;
        }
    }

    public String getAndroidCompilerExecutable() {
        for (File ndk : androidNdkCandidates()) {
            File bin = new File(ndk, "toolchains/llvm/prebuilt/windows-x86_64/bin");
            if (bin.isDirectory()) {
                for (int api = 35; api >= 21; api--) {
                    File cmd = new File(bin, "aarch64-linux-android" + api + "-clang++.cmd");
                    if (cmd.isFile()) return cmd.getAbsolutePath();
                    File exe = new File(bin, "aarch64-linux-android" + api + "-clang++.exe");
                    if (exe.isFile()) return exe.getAbsolutePath();
                }
            }
        }
        return null;
    }

    public String getAndroidCCompilerExecutable() {
        for (File ndk : androidNdkCandidates()) {
            File bin = new File(ndk, "toolchains/llvm/prebuilt/windows-x86_64/bin");
            if (bin.isDirectory()) {
                for (int api = 35; api >= 21; api--) {
                    File cmd = new File(bin, "aarch64-linux-android" + api + "-clang.cmd");
                    if (cmd.isFile()) return cmd.getAbsolutePath();
                    File exe = new File(bin, "aarch64-linux-android" + api + "-clang.exe");
                    if (exe.isFile()) return exe.getAbsolutePath();
                }
            }
        }
        return null;
    }

    public File getNdkDir() {
        for (File ndk : androidNdkCandidates()) {
            File toolchain = new File(ndk, "build/cmake/android.toolchain.cmake");
            if (toolchain.isFile()) return ndk;
        }
        return null;
    }

    /** Accepts both an NDK installation itself and an SDK ndk/ directory containing versions. */
    private static List<File> androidNdkCandidates() {
        String[] ndkRoots = {
                System.getenv("ANDROID_NDK_HOME"),
                System.getenv("NDK_HOME"),
                System.getenv("ANDROID_NDK_ROOT")
        };
        List<File> candidates = new ArrayList<>();
        for (String root : ndkRoots) {
            if (root == null || root.isBlank()) continue;
            File candidate = new File(root);
            if (!candidate.isDirectory()) continue;
            if (new File(candidate, "build/cmake/android.toolchain.cmake").isFile()) {
                candidates.add(candidate);
                continue;
            }
            File[] versions = candidate.listFiles(File::isDirectory);
            if (versions != null) {
                Arrays.sort(versions, (left, right) -> right.getName().compareTo(left.getName()));
                for (File version : versions) {
                    if (new File(version, "build/cmake/android.toolchain.cmake").isFile()) candidates.add(version);
                }
            }
        }
        return candidates;
    }

    public File getAndroidCMakeToolchainFile() {
        File ndk = getNdkDir();
        if (ndk != null) {
            File tc = new File(ndk, "build/cmake/android.toolchain.cmake");
            if (tc.isFile()) return tc;
        }
        return null;
    }

    public String getMakeExecutable() {
        if (isValidToolchain(toolchainDir)) {
            File bin = new File(toolchainDir, "bin");
            File make = new File(bin, "mingw32-make.exe");
            if (make.isFile()) return make.getAbsolutePath();
            make = new File(bin, "make.exe");
            if (make.isFile()) return make.getAbsolutePath();
        }
        return null;
    }

    public String getNinjaExecutable() {
        if (isValidToolchain(toolchainDir)) {
            File bin = new File(toolchainDir, "bin");
            File ninja = new File(bin, "ninja.exe");
            if (ninja.isFile()) return ninja.getAbsolutePath();
        }
        String[] sdkRoots = {
                System.getenv("ANDROID_HOME"),
                System.getenv("ANDROID_SDK_ROOT")
        };
        for (String root : sdkRoots) {
            if (root == null) continue;
            File cmakeDir = new File(root, "cmake");
            if (cmakeDir.isDirectory()) {
                File[] versions = cmakeDir.listFiles(File::isDirectory);
                if (versions != null) {
                    Arrays.sort(versions, (a, b) -> b.getName().compareTo(a.getName()));
                    for (File v : versions) {
                        File ninja = new File(v, "bin/ninja.exe");
                        if (ninja.isFile()) return ninja.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

    public File getIncludeDir() {
        if (toolchainDir != null) {
            File inc = new File(toolchainDir, "include");
            if (inc.isDirectory()) return inc;
        }
        return null;
    }

    public File getLibDir() {
        if (toolchainDir != null) {
            File lib = new File(toolchainDir, "lib");
            if (lib.isDirectory()) return lib;
        }
        return null;
    }

    public String getGccExecutable() {
        if (isValidToolchain(toolchainDir)) {
            File bin = new File(toolchainDir, "bin");
            File gcc = new File(bin, "gcc.exe");
            if (gcc.isFile()) return gcc.getAbsolutePath();
        }
        return null;
    }

    public String getGoExecutable() {
        if (toolchainDir != null && toolchainDir.isDirectory()) {
            File goBin = new File(toolchainDir, "go/bin/go.exe");
            if (goBin.isFile()) return goBin.getAbsolutePath();
            File binGo = new File(toolchainDir, "bin/go.exe");
            if (binGo.isFile()) return binGo.getAbsolutePath();
        }
        return null;
    }

    public File getGoRoot() {
        if (toolchainDir != null && toolchainDir.isDirectory()) {
            File goDir = new File(toolchainDir, "go");
            if (new File(goDir, "bin/go.exe").isFile()) return goDir;
        }
        return null;
    }

    public String getCargoExecutable() {
        if (toolchainDir != null && toolchainDir.isDirectory()) {
            File rBin = new File(toolchainDir, "rust/bin/cargo.exe");
            if (rBin.isFile()) return rBin.getAbsolutePath();
            File binCargo = new File(toolchainDir, "bin/cargo.exe");
            if (binCargo.isFile()) return binCargo.getAbsolutePath();
        }
        return null;
    }

    public String getRustcExecutable() {
        if (toolchainDir != null && toolchainDir.isDirectory()) {
            File rBin = new File(toolchainDir, "rust/bin/rustc.exe");
            if (rBin.isFile()) return rBin.getAbsolutePath();
            File binRustc = new File(toolchainDir, "bin/rustc.exe");
            if (binRustc.isFile()) return binRustc.getAbsolutePath();
        }
        return null;
    }

    public void injectEnvironment(Map<String, String> env) {
        if (toolchainDir != null && toolchainDir.isDirectory()) {
            File bin = new File(toolchainDir, "bin");
            File goBin = new File(toolchainDir, "go/bin");
            File rustBin = new File(toolchainDir, "rust/bin");
            StringBuilder extra = new StringBuilder();
            if (bin.isDirectory()) extra.append(bin.getAbsolutePath()).append(File.pathSeparator);
            if (goBin.isDirectory()) extra.append(goBin.getAbsolutePath()).append(File.pathSeparator);
            if (rustBin.isDirectory()) extra.append(rustBin.getAbsolutePath()).append(File.pathSeparator);

            String existing = env.getOrDefault("PATH", "");
            env.put("PATH", extra.toString() + existing);

            File goRoot = getGoRoot();
            if (goRoot != null) {
                env.put("GOROOT", goRoot.getAbsolutePath());
            }
            env.put("CGO_ENABLED", "1");
            String gcc = getGccExecutable();
            if (gcc != null) {
                env.put("CC", gcc);
            }
        }
    }

    public static File getApplicationDirectory() {
        try {
            File jar = new File(ToolchainManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jar.isFile()) {
                return jar.getParentFile();
            }
        } catch (Exception ignored) {}
        return new File(System.getProperty("user.dir", "."));
    }
}
