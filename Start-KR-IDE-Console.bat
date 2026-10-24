@echo off
title KR Compiler IDE - Windows Desktop Edition (Console Log)
cd /d "%~dp0"

if not exist "%~dp0KR_BUILD\.cache\temp" mkdir "%~dp0KR_BUILD\.cache\temp"
if not exist "%~dp0KR_BUILD\.cache\java-prefs" mkdir "%~dp0KR_BUILD\.cache\java-prefs"
set "TEMP=%~dp0KR_BUILD\.cache\temp"
set "TMP=%~dp0KR_BUILD\.cache\temp"
if not exist "%~dp0toolchain\kr-toolchain.stamp" exit /b 2
if not exist "%~dp0runtime\jdk\bin\java.exe" exit /b 2

if exist "%~dp0dist\KR-Compiler-IDE.next.jar" (
    move /Y "%~dp0dist\KR-Compiler-IDE.next.jar" "%~dp0dist\KR-Compiler-IDE.jar" >nul
    if errorlevel 1 exit /b 3
)

if exist "%~dp0toolchain\bin" set "PATH=%~dp0toolchain\bin;%PATH%"
"%~dp0runtime\jdk\bin\java.exe" -Djava.io.tmpdir="%~dp0KR_BUILD\.cache\temp" -Djava.util.prefs.userRoot="%~dp0KR_BUILD\.cache\java-prefs" -Djava.util.prefs.systemRoot="%~dp0KR_BUILD\.cache\java-prefs" -jar "%~dp0dist\KR-Compiler-IDE.jar"
pause
