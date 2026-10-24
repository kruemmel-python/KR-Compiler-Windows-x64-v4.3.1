param(
    [string]$TargetDir = "$PSScriptRoot\toolchain",
    [string]$Url = "https://github.com/kruemmel-python/KR-Compiler/releases/download/v4.3.1/KR-Compiler-Windows-toolchain-v4.3.1.zip",
    [string]$ExpectedSha256 = $env:KR_TOOLCHAIN_SHA256
)

$ErrorActionPreference = "Stop"
$targetFull = [System.IO.Path]::GetFullPath($TargetDir)
$rootFull = [System.IO.Path]::GetFullPath($PSScriptRoot)
if (-not $targetFull.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Das Toolchain-Ziel muss innerhalb der KR-Compiler-Installation liegen."
}

$stamp = Join-Path $targetFull "kr-toolchain.stamp"
if (Test-Path -LiteralPath $stamp -PathType Leaf) {
    Write-Host "[OK] Integrierte Toolchain ist vorhanden. Die IDE prüft ihre fest eingebetteten SHA-256-Werte beim Start: $targetFull"
    return
}
if ([string]::IsNullOrWhiteSpace($ExpectedSha256) -or $ExpectedSha256 -notmatch '^[0-9a-fA-F]{64}$') {
    throw "Download abgebrochen: -ExpectedSha256 (64 Hex-Zeichen) ist zwingend erforderlich. Den Wert aus einer getrennten, vertrauenswürdigen Release-Quelle beziehen."
}
if (Test-Path -LiteralPath $targetFull) {
    throw "Das Ziel existiert unvollständig und wird aus Sicherheitsgründen nicht überschrieben: $targetFull"
}

$cacheRoot = Join-Path $PSScriptRoot "KR_BUILD\.cache"
New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null
$stage = Join-Path $cacheRoot ("toolchain-install-" + [Guid]::NewGuid().ToString('N'))
$archive = Join-Path $stage "toolchain.zip"
$payload = Join-Path $stage "payload"
New-Item -ItemType Directory -Path $stage, $payload | Out-Null
$env:TEMP = Join-Path $stage "temp"
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Path $env:TEMP | Out-Null

try {
    Write-Host "Lade portable Compiler-Toolchain in den H:-Arbeitsbereich …"
    Invoke-WebRequest -Uri $Url -OutFile $archive -UseBasicParsing
    $actual = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    if (-not $actual.Equals($ExpectedSha256, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "SHA-256 stimmt nicht überein. Erwartet: $ExpectedSha256; erhalten: $actual"
    }
    Expand-Archive -LiteralPath $archive -DestinationPath $payload

    $contentRoot = $payload
    if (-not (Test-Path -LiteralPath (Join-Path $contentRoot 'bin\g++.exe') -PathType Leaf)) {
        $children = @(Get-ChildItem -LiteralPath $payload -Directory)
        if ($children.Count -eq 1 -and (Test-Path -LiteralPath (Join-Path $children[0].FullName 'bin\g++.exe') -PathType Leaf)) {
            $contentRoot = $children[0].FullName
        } else {
            throw "Archiv besitzt nicht die erwartete Toolchain-Struktur (bin\g++.exe fehlt)."
        }
    }
    Set-Content -LiteralPath (Join-Path $contentRoot 'kr-toolchain.stamp') -Value "KR-Toolchain-Windows-x64-v4.3.1`nArchive-SHA256=$actual" -Encoding UTF8
    Move-Item -LiteralPath $contentRoot -Destination $targetFull
    Write-Host "[ERFOLG] Verifizierte Toolchain installiert: $targetFull"
} finally {
    if (Test-Path -LiteralPath $stage) {
        Remove-Item -LiteralPath $stage -Recurse -Force
    }
}
