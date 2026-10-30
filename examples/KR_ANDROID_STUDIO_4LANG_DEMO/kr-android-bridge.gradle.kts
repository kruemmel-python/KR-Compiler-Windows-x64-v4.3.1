// Automatisch generierter KR Android Build Adapter fuer Android Studio Rabbit 2
// Verbindet Gradle mit der bestehenden KR Unified Language Pipeline (C++20, Rust, Go, Python).

val krCompilerHome = System.getenv("KR_COMPILER_HOME") ?: "H:/KR-Compiler/KR-Compiler-Windows-x64-v4.3.1"

tasks.register<Exec>("compileKrUnifiedAndroidArm64") {
    group = "kr unified language"
    description = "Kompiliert alle .kr-Dateien (C++, Rust, Go, Python) ueber die KR-Pipeline nach KR_BUILD und jniLibs/arm64-v8a"
    workingDir = project.rootDir
    commandLine(
        "$krCompilerHome/runtime/jdk/bin/java.exe",
        "-Dkr.compiler.home=$krCompilerHome",
        "-cp",
        "$krCompilerHome/dist/KR-Compiler-IDE.jar",
        "kr.compile.KrCliMain",
        project.rootDir.absolutePath,
        "ANDROID"
    )
}
