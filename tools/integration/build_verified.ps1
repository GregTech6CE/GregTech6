param(
    [string]$Gradle,
    [string]$ProductionEnvironment,
    [switch]$PreflightOnly
)
$ErrorActionPreference = 'Stop'
$repoPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (-not $Gradle) { $Gradle = Join-Path $repoPath 'work/gradle-runtime/gradle-8.8/bin/gradle.bat' }
if (-not (Test-Path -LiteralPath $Gradle)) { throw 'Pass -Gradle with an installed Gradle 8.8 executable.' }
if (-not $ProductionEnvironment) { $ProductionEnvironment = Join-Path $repoPath 'work/production-smoke.json' }
$modVersionLine = Select-String -LiteralPath (Join-Path $repoPath 'gradle.properties') -Pattern '^\s*mod_version\s*=\s*(\S+)\s*$' | Select-Object -First 1
if (-not $modVersionLine) { throw 'gradle.properties has no mod_version' }
$modVersion = $modVersionLine.Matches[0].Groups[1].Value
$forgeJar = "gregtech6-1.20.1-forge-$modVersion.jar"
$neoforgeJar = "gregtech6-neoforge-1.21.1-$modVersion.jar"
$probeJar = "gregtech-delivery-probe-$modVersion.jar"
Push-Location -LiteralPath $repoPath
try {
    $logDirectory = Join-Path $repoPath 'build/preflight'
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $gradleArguments = @('--offline')
    $localInit = Join-Path $repoPath 'work/emi-local.init.gradle'
    if (Test-Path -LiteralPath $localInit) { $gradleArguments += @('-I', $localInit) }
    $coreLog = Join-Path $logDirectory 'core-check.log'
    Write-Host "Running the same shared-core checks required by CI. Log: $coreLog"
    & $Gradle @gradleArguments ':core:check' *> $coreLog
    if ($LASTEXITCODE -ne 0) { throw "Shared-core checks failed; see $coreLog" }
    foreach ($platform in @('neoforge', 'forge')) {
        $logPath = Join-Path $logDirectory ($platform + '-world-creation.log')
        $runTask = if ($platform -eq 'forge') { ':runClient' } else { ':neoforge:runClient' }
        Write-Host "Starting ${platform}: create a fresh normal world and enter the game. Log: $logPath"
        & $Gradle @gradleArguments '-PdirectCoreClasspath=true' '-PdirectCoreResources=true' '-PclientCreateWorldSmoke=true' '-PclientSmoke=false' '-PclientSmokeTimeout=360' '-PclientSmokeHeap=4g' '-PemiSmoke=true' '-PemiOnlySmoke=true' $runTask *> $logPath
        if ($LASTEXITCODE -ne 0) { throw "$platform preflight Gradle failed; see $logPath" }
        $logText = Get-Content -LiteralPath $logPath -Raw -Encoding utf8
        if ($logText -match 'WORLD_CREATION_SMOKE_FAILED|java\.lang\.NullPointerException|Encountered an unexpected exception') {
            throw "$platform preflight reported an exception; see $logPath"
        }
        $receipts = [regex]::Matches($logText, 'WORLD_CREATION_SMOKE_SUCCESS (\{[^\r\n]+\})')
        if ($receipts.Count -ne 1) { throw "$platform needs exactly one actual world success receipt." }
        $receipt = $receipts[0].Groups[1].Value | ConvertFrom-Json
        if ($receipt.platform -ne $platform -or $receipt.renderedWorldFrames -lt 30 -or $receipt.recipes -lt 1000 -or $receipt.canonicalItemsChecked -lt 1000 -or ($platform -eq 'neoforge' -and $receipt.emiLoaded -ne $true) -or $receipt.legacyBatteryAliasesChecked -ne 5 -or $receipt.chemicalBatteryStackRoundTrips -ne 10 -or $receipt.sourceChemicalBatteriesChecked -ne 25 -or $receipt.sourcePoweredAssemblyRowsChecked -ne 70) {
            throw "$platform preflight receipt is incomplete."
        }
        if ($receipt.originCenterBiomesChecked -ne 9 -or $receipt.originFacilityIdentitiesChecked -ne 25 -or $receipt.originTestInventorySlots -ne 144 -or $receipt.crankNativeSignalChecks -ne 30 -or $receipt.flintKnifeFireAspect -ne 1 -or $receipt.originalCrucibleReactionChecks -ne 4 -or $receipt.originalCreativePages -ne 66 -or $receipt.gearboxInventoryModelsChecked -ne 13 -or $receipt.asphaltItemRgb -ne '808080' -or $receipt.crankItemRgb -ne 'c8c8c8' -or $receipt.originFeedbackFailures) {
            throw "$platform original-content feedback receipt is incomplete."
        }
        if ($receipt.nexusToolInventoryModelsChecked -ne 61 -or $receipt.ropeInventoryModelsChecked -ne 6 -or $receipt.filterInventoryModelsChecked -ne 4 -or $receipt.automaticAndDataSwitchColorsChecked -ne 12 -or @($receipt.renderedInventoryItems).Count -ne 61 -or @($receipt.untranslatedInventorySamples).Count -ne 0) {
            throw "$platform actual item-rendering feedback receipt is incomplete."
        }
        if ($receipt.nativeMobDropEventChecks -ne 17 -or $receipt.newFeedbackCraftingRowsChecked -ne 3 -or $receipt.lootCrateReturnAndStackLimitChecks -ne 2 -or $receipt.lootViewer -ne 'emi' -or $receipt.lootViewerTables -ne 18 -or $receipt.lootViewerRows -lt 1000 -or $receipt.mobViewerRows -lt 80 -or $receipt.emiStructureRows -lt 80 -or $receipt.emi_ore_veins -lt 30 -or $receipt.emi_stone_layers_info -lt 50 -or $receipt.emi_small_ores_info -lt 80 -or $receipt.emi_bedrock_ores -lt 20) {
            throw "$platform actual loot/structure/geology viewer receipt is incomplete."
        }
        if ($receipt.lootViewerPages -ne 18 -or $receipt.mobViewerPages -ne 19 -or $receipt.groupedOutputsIndexed -ne ($receipt.lootViewerRows + $receipt.mobViewerRows) -or -not $receipt.emiLootGridScrolled -or $receipt.emiNativePreviewInputChecks -ne 5 -or $receipt.glassSlabTranslucentModelsChecked -ne 6 -or $receipt.glassSlabTouchingFaceChecks -ne 6) {
            throw "$platform grouped loot / preview / glass receipt is incomplete."
        }
        if (@($receipt.machineFeedbackCrafting).Count -ne 50 -or @($receipt.lightningProcessorsChecked).Count -ne 5 -or $receipt.sourceEnergyCreativeGroupsChecked -ne 36 -or $receipt.longDistanceCreativeGroupsChecked -ne 30 -or $receipt.sourceMelterOperatingFlagChecks -ne 3 -or $receipt.sourceMachineInventoryModelsChecked -ne 6) {
            throw "$platform actual machine/transport crafting and rendering receipt is incomplete."
        }
        foreach ($capture in @('mobViewerScreenshot', 'lootViewerScreenshot', 'emiStructureScreenshot', 'emiVeinsScreenshot', 'emiLayersScreenshot', 'glassSlabWorldScreenshot')) {
            if (-not $receipt.$capture -or -not (Test-Path -LiteralPath $receipt.$capture)) {
                throw "$platform viewer screenshot $capture is missing."
            }
        }
        if($receipt.originTestInventoryPresent -ne 118 -or $receipt.originTestInventoryOptionalEmpty -ne 26 -or @($receipt.originTestInventoryPendingTools).Count -ne 0 -or $receipt.nexusGunUseChecks -ne 15 -or $receipt.nexusGunAndPocketCraftingRows -ne 4 -or $receipt.nexusPocketModesChecked -ne 8 -or $receipt.nexusPoweredToolsChecked -ne 14){throw "$platform native Nexus tools are incomplete."}
        if (@($receipt.flatBushVariantsChecked).Count -ne 9 -or @($receipt.flatSpringVariantsChecked).Count -ne 7 -or $receipt.flatGeneratedBranchesChecked -ne 45 -or $receipt.flatLegacyWorldConversionsChecked -ne 2 -or $receipt.flatSurfaceInventoryModelsChecked -ne 16 -or @($receipt.renderedFlatSurfaceItems).Count -ne 16 -or -not $receipt.supporterCertificateRemoved) {
            throw "$platform flat bushes/springs and certificate removal receipt is incomplete."
        }
        if ($receipt.sandwichSourceIngredientsChecked -ne 95 -or $receipt.sandwichNativeInteractionChecks -ne 14 -or $receipt.sandwichNativeConsumptionChecks -ne 7 -or $receipt.sandwichInventoryModelsChecked -ne 3 -or $receipt.sandwichLayerTooltipOrderChecked -ne 4 -or $receipt.sandwichRenderedPreviews -ne 3 -or $receipt.sandwichSourceBottleFoods -ne 35 -or -not $receipt.sandwichDroppedIngredientCountsNormalized) {
            throw "$platform sandwich interaction/food/render receipt is incomplete."
        }
        if ($receipt.cannedFoodConsumptionChecks -ne 57 -or $receipt.cannedSandwichIngredientsChecked -ne 24 -or $receipt.cannedRottenConversionsChecked -ne 42 -or $receipt.cannedAirRoundTripRoutesChecked -ne 6 -or $receipt.cannedTameAnimalFeedChecks -ne 3 -or $receipt.cannedContainerModeChecks -ne 3 -or $receipt.cannedInventoryModelsChecked -ne 57 -or $receipt.cannedCreativeVisible -ne 45 -or -not $receipt.emptyFoodCanCraftingChecked) {
            throw "$platform original canned-food receipt is incomplete."
        }
        # Source NI and absent external integrations remain explicit empty slots.
        if ($receipt.originTestInventoryPresent + $receipt.originTestInventoryOptionalEmpty + @($receipt.originTestInventoryPendingTools).Count -ne 144) {
            throw "$platform source test inventory accounting is incomplete."
        }
        $screen = Test-Path -LiteralPath $receipt.screenshot
        if (-not $screen) { throw "$platform preflight screenshot is missing." }
        $receipts[0].Groups[1].Value | Set-Content -LiteralPath (Join-Path $logDirectory ($platform + '-world-creation.json')) -Encoding utf8
        Write-Host "$platform world creation passed: $($receipt.recipes) recipes, $($receipt.canonicalItemsChecked) canonical item outputs."
    }
    if (-not $PreflightOnly) {
        $buildLog = Join-Path $logDirectory 'dual-assemble.log'
        Write-Host "Both client preflights passed. Building dual artifacts. Log: $buildLog"
        & $Gradle @gradleArguments ':assemble' ':neoforge:assemble' *> $buildLog
        if ($LASTEXITCODE -ne 0) { throw "Dual assemble failed; see $buildLog" }
        & python 'tools/integration/verify_artifacts.py' --forge "build/libs/$forgeJar" --neoforge "neoforge/build/libs/$neoforgeJar" --output (Join-Path $logDirectory 'artifacts.json')
        if ($LASTEXITCODE -ne 0) { throw 'Artifact verification failed.' }
        if (Test-Path -LiteralPath $ProductionEnvironment) {
            $production = Get-Content -LiteralPath $ProductionEnvironment -Raw -Encoding utf8 | ConvertFrom-Json
            & $Gradle @gradleArguments ':productionSmokeJar' ':neoforge:productionSmokeJar' *> (Join-Path $logDirectory 'production-probes.log')
            if ($LASTEXITCODE -ne 0) { throw 'Production startup probes did not build.' }
            foreach ($platform in @('forge', 'neoforge')) {
                $platformBuild = if ($platform -eq 'forge') { 'build' } else { 'neoforge/build' }
                $jarName = if ($platform -eq 'forge') { $forgeJar } else { $neoforgeJar }
                & python 'tools/integration/launch_production_smoke.py' --platform $platform --minecraft-root $production.minecraft_root --version $production.versions.$platform --java $production.java --jar "$platformBuild/libs/$jarName" --probe "$platformBuild/production-probe/$probeJar" --output (Join-Path $logDirectory ($platform + '-production.json'))
                if ($LASTEXITCODE -ne 0) { throw "$platform distribution client did not reach the title screen." }
            }
        }
        & python 'tools/integration/publish_verified_artifacts.py' --verification-report (Join-Path $logDirectory 'artifacts.json') --output (Join-Path $logDirectory 'publication.json')
        if ($LASTEXITCODE -ne 0) { throw 'Verified artifact publication failed.' }
        Write-Host 'Dual assemble, artifact verification and stable publication passed.'
    }
} finally { Pop-Location }
