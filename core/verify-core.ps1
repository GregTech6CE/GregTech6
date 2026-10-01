param([string]$JdkHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$coreModule = $PSScriptRoot
$coreRepository = Split-Path -Parent $coreModule
$coreOutput = Join-Path $coreModule ("build/standalone-contracts/" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $coreOutput -Force | Out-Null
if ($JdkHome) {
    $coreJavac = Join-Path $JdkHome 'bin/javac.exe'
    $coreJava = Join-Path $JdkHome 'bin/java.exe'
} else {
    $coreJavac = (Get-Command javac -ErrorAction Stop).Source
    $coreJava = (Get-Command java -ErrorAction Stop).Source
}
$coreSources = @(Get-ChildItem -LiteralPath (Join-Path $coreModule 'src/main/java') -Recurse -Filter '*.java' -File |
    ForEach-Object { $_.FullName })
$coreSources += @(Get-ChildItem -LiteralPath (Join-Path $coreModule 'src/test/java') -Recurse -Filter '*.java' -File |
    ForEach-Object { $_.FullName })
if (-not $coreSources.Count -or -not (Test-Path -LiteralPath (Join-Path $coreModule 'src/test/java/com/gregtech/gregtech/core/CoreBehaviorContracts.java'))) {
    throw 'Core sources or required behavior contract source missing.'
}
Write-Output "Fresh standalone class output: $coreOutput"
& $coreJavac --release 17 -encoding UTF-8 -d $coreOutput $coreSources
if ($LASTEXITCODE -ne 0) { throw "Core javac failed with exit code $LASTEXITCODE" }
& $coreJava -cp $coreOutput com.gregtech.gregtech.core.CoreBehaviorContracts
if ($LASTEXITCODE -ne 0) { throw "Core contracts failed with exit code $LASTEXITCODE" }
& $coreJava -cp $coreOutput com.gregtech.gregtech.core.MaterialBehaviorContracts
if ($LASTEXITCODE -ne 0) { throw "Material contracts failed with exit code $LASTEXITCODE" }
$coreManifest = Get-Content -LiteralPath (Join-Path $coreModule 'provenance/saltnya-extractions.json') -Raw | ConvertFrom-Json
foreach ($coreEntry in $coreManifest.files) {
    $coreMoved = Join-Path $coreRepository $coreEntry.core_path
    $coreActual = (Get-FileHash -LiteralPath $coreMoved -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($coreActual -ne $coreEntry.sha256) { throw "Relocated source bytes changed: $($coreEntry.core_path)" }
    if (Test-Path -LiteralPath (Join-Path $coreRepository $coreEntry.original_path)) {
        throw "Duplicate source remains: $($coreEntry.original_path)"
    }
}
Write-Output "Provenance verified: $($coreManifest.files.Count) byte-preserving relocations; no duplicate source classes."
$coreMaterialsManifest = Get-Content -LiteralPath (Join-Path $coreModule 'provenance/material-extractions.json') -Raw | ConvertFrom-Json
foreach ($coreEntry in $coreMaterialsManifest.files) {
    $coreMoved = Join-Path $coreRepository $coreEntry.core_path
    $coreActual = (Get-FileHash -LiteralPath $coreMoved -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($coreActual -ne $coreEntry.integrated_sha256) { throw "Integrated material source hash mismatch: $($coreEntry.core_path)" }
    if (Test-Path -LiteralPath (Join-Path $coreRepository $coreEntry.original_path)) {
        throw "Duplicate material source remains: $($coreEntry.original_path)"
    }
}
Write-Output "Material provenance verified: $($coreMaterialsManifest.files.Count) actual relocations; original and integrated hashes retained."
