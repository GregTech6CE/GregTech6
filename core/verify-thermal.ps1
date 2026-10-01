param([string]$JdkHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$thermalCore = $PSScriptRoot
$thermalOutput = Join-Path $thermalCore ("build/standalone-thermal-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $thermalOutput -Force | Out-Null
if ($JdkHome) {
    $thermalJavac = Join-Path $JdkHome 'bin/javac.exe'
    $thermalJava = Join-Path $JdkHome 'bin/java.exe'
} else {
    $thermalJavac = (Get-Command javac -ErrorAction Stop).Source
    $thermalJava = (Get-Command java -ErrorAction Stop).Source
}
$thermalSources = @('CrucibleMath.java', 'ThermalState.java', 'ThermalStep.java') | ForEach-Object {
    Join-Path $thermalCore "src/main/java/com/gregtech/gregtech/api/machine/crucible/$_"
}
$thermalSources += Join-Path $thermalCore 'src/test/java/com/gregtech/gregtech/core/ThermalBehaviorContracts.java'
& $thermalJavac --release 17 -encoding UTF-8 -d $thermalOutput $thermalSources
if ($LASTEXITCODE -ne 0) { throw 'Pure thermal Java17 compilation failed' }
& $thermalJava -cp $thermalOutput com.gregtech.gregtech.core.ThermalBehaviorContracts
if ($LASTEXITCODE -ne 0) { throw 'Thermal contracts failed' }
Write-Output "Fresh thermal classes: $thermalOutput"
