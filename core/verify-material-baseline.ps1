#requires -Version 7.0
param([string]$JdkHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$baselineCore = $PSScriptRoot
$baselineRepository = Split-Path -Parent $baselineCore
$baselineRoot = Join-Path $baselineCore ("build/imported-material-baseline-" + [Guid]::NewGuid().ToString('N'))
$baselineSources = Join-Path $baselineRoot 'sources'
$baselineClasses = Join-Path $baselineRoot 'classes'
New-Item -ItemType Directory -Path $baselineSources, $baselineClasses -Force | Out-Null
$baselineMaterialManifest = Get-Content -LiteralPath (Join-Path $baselineCore 'provenance/material-extractions.json') -Raw | ConvertFrom-Json
$baselineCoreManifest = Get-Content -LiteralPath (Join-Path $baselineCore 'provenance/saltnya-extractions.json') -Raw | ConvertFrom-Json
$baselineGit = (Get-Command git -ErrorAction Stop).Source
$baselineUtf8 = [System.Text.UTF8Encoding]::new($false)

# Read raw Git blob bytes; shell text redirection would alter source encoding/newlines.
foreach ($baselineEntry in @($baselineMaterialManifest.files) + @($baselineCoreManifest.files)) {
    $baselineRelative = $baselineEntry.original_path.Substring('src/main/java/'.Length)
    $baselineTarget = Join-Path $baselineSources $baselineRelative
    New-Item -ItemType Directory -Path (Split-Path -Parent $baselineTarget) -Force | Out-Null
    $baselineProcessInfo = [System.Diagnostics.ProcessStartInfo]::new($baselineGit)
    $baselineProcessInfo.UseShellExecute = $false
    $baselineProcessInfo.CreateNoWindow = $true
    $baselineProcessInfo.RedirectStandardOutput = $true
    $baselineProcessInfo.RedirectStandardError = $true
    $baselineProcessInfo.ArgumentList.Add('-C')
    $baselineProcessInfo.ArgumentList.Add($baselineRepository)
    $baselineProcessInfo.ArgumentList.Add('cat-file')
    $baselineProcessInfo.ArgumentList.Add('blob')
    $baselineProcessInfo.ArgumentList.Add("$($baselineMaterialManifest.source_snapshot):$($baselineEntry.original_path)")
    $baselineProcess = [System.Diagnostics.Process]::Start($baselineProcessInfo)
    $baselineFile = [System.IO.File]::Create($baselineTarget)
    try { $baselineProcess.StandardOutput.BaseStream.CopyTo($baselineFile) } finally { $baselineFile.Dispose() }
    $baselineError = $baselineProcess.StandardError.ReadToEnd()
    $baselineProcess.WaitForExit()
    if ($baselineProcess.ExitCode -ne 0) { throw "Cannot read original source: $baselineError" }
    $baselineActual = (Get-FileHash -LiteralPath $baselineTarget -Algorithm SHA256).Hash.ToLowerInvariant()
    $baselineExpected = if ($baselineEntry.source_sha256) { $baselineEntry.source_sha256 } else { $baselineEntry.sha256 }
    if ($baselineActual -ne $baselineExpected) { throw "Original Git blob hash mismatch: $($baselineEntry.original_path)" }
    $baselineProcess.Dispose()
}

$baselineStubs = Get-Content -LiteralPath (Join-Path $baselineCore 'src/test/fixtures/saltnya-boundary-stubs.json') -Raw | ConvertFrom-Json
foreach ($baselineStub in $baselineStubs.PSObject.Properties) {
    $baselineTarget = Join-Path $baselineSources $baselineStub.Name
    New-Item -ItemType Directory -Path (Split-Path -Parent $baselineTarget) -Force | Out-Null
    [System.IO.File]::WriteAllText($baselineTarget, $baselineStub.Value, $baselineUtf8)
}
$baselineInputs = @(Get-ChildItem -LiteralPath $baselineSources -Recurse -File -Filter '*.java' | ForEach-Object { $_.FullName })
$baselineInputs += @((Join-Path $baselineCore 'src/test/java/com/gregtech/gregtech/core/MaterialCatalogSnapshot.java'),
    (Join-Path $baselineCore 'src/test/fixtures/MaterialBaselineProbe.java'))
if ($JdkHome) {
    $baselineJavac = Join-Path $JdkHome 'bin/javac.exe'
    $baselineJava = Join-Path $JdkHome 'bin/java.exe'
} else {
    $baselineJavac = (Get-Command javac -ErrorAction Stop).Source
    $baselineJava = (Get-Command java -ErrorAction Stop).Source
}
& $baselineJavac --release 17 -encoding UTF-8 -d $baselineClasses $baselineInputs
if ($LASTEXITCODE -ne 0) { throw 'Original imported domain baseline did not compile' }
$baselineObservation = @(& $baselineJava -cp $baselineClasses MaterialBaselineProbe)
if ($LASTEXITCODE -ne 0) { throw 'Original imported domain baseline did not bootstrap' }
$baselineFixture = Get-Content -LiteralPath (Join-Path $baselineCore 'provenance/material-baseline.json') -Raw | ConvertFrom-Json
if ($baselineObservation -notcontains "definitions_sha256=$($baselineFixture.definitions_sha256)" -or
    $baselineObservation -notcontains "post_init_sha256=$($baselineFixture.post_init_sha256)") {
    throw 'Original imported domain no longer matches the recorded baseline'
}
$baselineObservation
Write-Output 'Original Git source hashes and complete imported domain snapshot reproduced; game boundaries were inert stubs.'
