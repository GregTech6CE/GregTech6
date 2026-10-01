[CmdletBinding()]
param(
    [ValidateSet('forge', 'neoforge', 'all')]
    [string]$Target = 'all',
    [switch]$CheckEnvironmentOnly
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

function Find-Jdk([int]$Major, [bool]$Required) {
    $envName = "JAVA${Major}_HOME"
    $candidates = @([Environment]::GetEnvironmentVariable($envName), $env:JAVA_HOME)
    if ($Major -eq 21 -and $env:APPDATA) {
        $candidates += Join-Path $env:APPDATA '.minecraft\runtime\java-runtime-delta'
    }
    foreach ($parent in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft")) {
        if (Test-Path -LiteralPath $parent) {
            $candidates += Get-ChildItem -LiteralPath $parent -Directory | ForEach-Object FullName
        }
    }
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (-not $candidate) { continue }
        $java = Join-Path $candidate 'bin\java.exe'
        $javac = Join-Path $candidate 'bin\javac.exe'
        if (-not (Test-Path -LiteralPath $java) -or -not (Test-Path -LiteralPath $javac)) { continue }
        $version = (& $java -version 2>&1 | Out-String)
        if ($version -match "version `"$Major\.") { return $candidate }
    }
    if ($Required) { throw "Java $Major JDK not found. Set $envName to its installation directory (not bin)." }
    return $null
}

$jdk17 = Find-Jdk 17 $true
$jdk21 = Find-Jdk 21 ($Target -ne 'forge')
if ($CheckEnvironmentOnly) {
    Write-Output "Java 17: $jdk17"
    if ($jdk21) { Write-Output "Java 21: $jdk21" }
    return
}
$savedJavaHome = $env:JAVA_HOME
$savedJava17Home = $env:JAVA17_HOME
$savedJava21Home = $env:JAVA21_HOME
Push-Location $repo
try {
    $env:JAVA_HOME = $jdk17
    $env:JAVA17_HOME = $jdk17
    if ($jdk21) { $env:JAVA21_HOME = $jdk21 }
    New-Item -ItemType Directory -Force -Path (Join-Path $repo 'work') | Out-Null
    $targets = if ($Target -eq 'all') { @('forge', 'neoforge') } else { @($Target) }
    foreach ($platform in $targets) {
        $task = if ($platform -eq 'forge') { ':build' } else { ':neoforge:build' }
        $log = Join-Path $repo "work\build-$platform-$(Get-Date -Format 'yyyyMMdd-HHmmss').log"
        Write-Output "Building $platform; log: $log"
        & (Join-Path $repo 'gradlew.bat') --no-daemon --console=plain ':core:check' $task *> $log
        $buildExit = $LASTEXITCODE
        Get-Content -LiteralPath $log -Tail 20
        if ($buildExit -ne 0) { throw "$platform build failed (exit $buildExit). See $log" }
    }
} finally {
    Pop-Location
    $env:JAVA_HOME = $savedJavaHome
    $env:JAVA17_HOME = $savedJava17Home
    $env:JAVA21_HOME = $savedJava21Home
}
