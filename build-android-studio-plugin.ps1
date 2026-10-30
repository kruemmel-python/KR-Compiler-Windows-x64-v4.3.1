param(
    [string]$AndroidStudioHome = "H:\Program Files\Android\Android Studio",
    [string]$UserPluginsDir = "$env:APPDATA\Google\AndroidStudio2026.2.2\plugins"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$JdkBin = Join-Path $Root "runtime\jdk\bin"
$Javac = Join-Path $JdkBin "javac.exe"
$JarExe = Join-Path $JdkBin "jar.exe"

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  Baue KR Unified Language Plugin fuer Android Studio Rabbit 2" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Kompiliere den bestehenden KR-Compiler-Kern (inkl. 4-Sprachen-Support: C++, Rust, Go, Python)
$CoreBin = Join-Path $Root "bin"
if (-not (Test-Path $CoreBin)) { New-Item -ItemType Directory -Force -Path $CoreBin | Out-Null }

$CoreSources = (Get-ChildItem -Recurse (Join-Path $Root "src\*.java")).FullName
& $Javac -encoding UTF-8 --release 21 -d $CoreBin $CoreSources
if ($LASTEXITCODE -ne 0) { throw "Kompilierung des KR-Compiler-Kerns fehlgeschlagen." }

$DistDir = Join-Path $Root "dist"
if (-not (Test-Path $DistDir)) { New-Item -ItemType Directory -Force -Path $DistDir | Out-Null }
& $JarExe cfm (Join-Path $DistDir "KR-Compiler-IDE.jar") (Join-Path $Root "Manifest.txt") -C $CoreBin .
if ($LASTEXITCODE -ne 0) { throw "Erstellung von KR-Compiler-IDE.jar fehlgeschlagen." }

# 2. Kompiliere das Android Studio Plugin gegen die IntelliJ Platform (262.10315.125)
$StudioLibWildcard = Join-Path $AndroidStudioHome "lib\*"
$PluginSrcDir = Join-Path $Root "android-studio-plugin\src"
$PluginResDir = Join-Path $Root "android-studio-plugin\resources"
$PluginBinDir = Join-Path $Root "android-studio-plugin\bin"

if (Test-Path $PluginBinDir) { Remove-Item -Recurse -Force $PluginBinDir }
New-Item -ItemType Directory -Force -Path $PluginBinDir | Out-Null

# Kopiere den KR-Compiler-Kern in das Plugin-Ausgabeverzeichnis, damit das Plugin autark laeuft
Copy-Item -Recurse -Force (Join-Path $CoreBin "*") $PluginBinDir
Copy-Item -Recurse -Force (Join-Path $PluginResDir "*") $PluginBinDir

$PluginSources = (Get-ChildItem -Recurse (Join-Path $PluginSrcDir "*.java")).FullName
$Classpath = "$CoreBin;$StudioLibWildcard"

$StudioJbrBin = Join-Path $AndroidStudioHome "jbr\bin"
$StudioJavac = if (Test-Path (Join-Path $StudioJbrBin "javac.exe")) { Join-Path $StudioJbrBin "javac.exe" } else { $Javac }

& $StudioJavac -encoding UTF-8 -cp $Classpath -d $PluginBinDir $PluginSources
if ($LASTEXITCODE -ne 0) { throw "Kompilierung des KR Android Studio Plugins fehlgeschlagen." }

# 3. Erzeuge Plugin-JAR und installierbares Plugin-ZIP (fuer 'Install Plugin from Disk...')
$BuildStage = Join-Path $Root "android-studio-plugin\stage"
if (Test-Path $BuildStage) { Remove-Item -Recurse -Force $BuildStage }
$StageLibDir = Join-Path $BuildStage "kr-unified-language\lib"
New-Item -ItemType Directory -Force -Path $StageLibDir | Out-Null

$PluginJarPath = Join-Path $StageLibDir "kr-unified-language.jar"
& $JarExe cf $PluginJarPath -C $PluginBinDir .
if ($LASTEXITCODE -ne 0) { throw "Erstellung von kr-unified-language.jar fehlgeschlagen." }

$PluginZipPath = Join-Path $DistDir "KR-Unified-Language-AndroidStudio-Plugin-v4.3.1.zip"
if (Test-Path $PluginZipPath) { Remove-Item -Force $PluginZipPath }
Compress-Archive -Path (Join-Path $BuildStage "kr-unified-language") -DestinationPath $PluginZipPath -Force
Write-Host "[OK] Installierbares Plugin-ZIP erstellt: $PluginZipPath" -ForegroundColor Green

# 4. Installiere das Plugin im offiziellen Benutzer-Plugin-Verzeichnis (%APPDATA%\Google\AndroidStudio*\plugins)
#    WICHTIG: Niemals in H:\Program Files\Android\Android Studio\plugins kopieren, da der JetBrains Patch-Updater
#    sonst beim Android-Studio-Update "Unexpected file" im Installationsverzeichnis meldet.
$GoogleAppData = Join-Path $env:APPDATA "Google"
if (Test-Path $GoogleAppData) {
    Get-ChildItem -Path $GoogleAppData -Directory -Filter "AndroidStudio*" | ForEach-Object {
        $TargetPluginsDir = Join-Path $_.FullName "plugins"
        New-Item -ItemType Directory -Force -Path $TargetPluginsDir | Out-Null
        $TargetUserPlugin = Join-Path $TargetPluginsDir "kr-unified-language"
        if (Test-Path $TargetUserPlugin) { Remove-Item -Recurse -Force $TargetUserPlugin }
        Copy-Item -Recurse -Force (Join-Path $BuildStage "kr-unified-language") $TargetUserPlugin
        Write-Host "[OK] Plugin installiert in Benutzer-Plugins: $TargetUserPlugin" -ForegroundColor Green
    }
}

# Entferne ggf. alte Kopie aus dem IDE-Systemordner, damit Android-Studio-Updates konfliktfrei durchlaufen
$StudioSystemPlugin = Join-Path $AndroidStudioHome "plugins\kr-unified-language"
if (Test-Path $StudioSystemPlugin) {
    try {
        Remove-Item -Recurse -Force $StudioSystemPlugin
        Write-Host "[OK] Systemordner bereinigt (verhindert Update-Konflikte): $StudioSystemPlugin" -ForegroundColor Green
    } catch {
        Write-Host "[HINWEIS] Bitte im Update-Dialog 'Proceed' (Delete) klicken." -ForegroundColor Yellow
    }
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  KR Unified Language Plugin erfolgreich gebaut & installiert!" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
