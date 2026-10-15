# 2026-10-15 多动态配方 provider

查找顺序保持为固定表、动态 provider、带世界的 context provider。`dynamicRecipes` 替换最前面的内置 provider。`addDynamicRecipes` 追加到后面，第一个非空结果生效。

## 编译

Java 21 启动 Gradle，EMI 从本机 `work/local-maven` 解析。`:compileJava` 与 `:neoforge:compileJava` 在 36 秒内通过，3 个任务实际执行。

## 新测试

命名空间 `gregtech_dynamic_recipes`，目录 `build/card03-dynamic-providers-3` 与 `neoforge/build/card03-dynamic-providers-3`。Forge 09:25:44、NeoForge 09:28:13 各报告 `All 3 required tests passed`。整次 Gradle 7 分 53 秒，`BUILD SUCCESSFUL`。

三项分别锁住：前一个 provider 返回空时后一个被调用并生效；前一个返回配方时后一个不被调用；固定行命中时两个 provider 都不被调用。

## 已有分子数据链

命名空间 `gregtech_material_data`，目录 `build/card03-material-data` 与 `neoforge/build/card03-material-data`。这组覆盖分子扫描、打印机和复制机上现有的 `dynamicRecipes`。Forge 09:34:31、NeoForge 09:37:34 各 3 项通过。整次 8 分 3 秒，`BUILD SUCCESSFUL`。

## Bumblelyzer 扫描

`BumbleBeeScanningTests` 改到命名空间 `gregtech_bumble_scan`，默认 GameTest 列表带上它，所以整套默认测试仍然会跑这 5 项。NeoForge 没有对应测试。

第一次用 1536m 堆、新目录 `build/card03-bumble-scan`，服务器在准备出生区时 `OutOfMemoryError`，测试没有开始。这次 Gradle 仍打印了 `BUILD SUCCESSFUL`，不能当成通过。

第二次用 3g 堆、视距 4，目录 `build/card03-bumble-scan-2`。5 项里 4 项通过，`everySpeciesHasItsFourScannedBees` 失败：语言文件里的死亡扫描蜂是 GT6 原文 `Wild Bumblebee (Dead & Scanned)`，测试还在要任务书里的逗号写法。断言改成已发布的名字后重跑。10:44:52 报告 `All 5 required tests passed`，Gradle `BUILD SUCCESSFUL in 3m 3s`。

## gt6-crops 的种子扫描

扫描实现在 gt6-crops，不在核心里。没扫过的种子产出扫过的同一颗，64 tick、16 EU/t。已经扫过的种子 1 tick、16 EU/t 原样出来，16 EU/t 是 LV Plantalyzer 的最低功率。另外写了一条假配方给配方查看器用，机器不会按它加工。每种作物一条展示行是 issue #13 里的事，这次没有做。issue #13 没有回复。

本地用刚打的 `gregtech6-neoforge-1.21.1-0.0.0.jar` 换掉了 gt6-crops 里原来的 CI 包，原来的包留在本仓库 `work/gregtech6-1.21.1-neoforge.ci.jar`。`libs/gt6ce` 里记录的哈希和构建提交仍指向 CI 包 `602518728aefabcc2a155e83d59ea422fe8b5e5c`，没有改。`update-gt.ps1` 会把本地 jar 换回 CI 包，那时候种子扫描就编不过，要等带 `addDynamicRecipes` 的包发布。

GameTest 服务器 `build/seed-scan-gametest` 在 10:07:17 先报告 `All 3 required tests passed`，Gradle `BUILD SUCCESSFUL in 2m 38s`。那三次只调用了 `Plantalyzer.findRecipe`。

随后补了两台放进世界的 LV 分析仪。第一次 10:48:18 失败：输入被拿走了，产出却不在输出槽。分析仪的空输出面会把成品扔进世界。六面用石头挡住之后重跑。10:51:42 报告 `All 5 required tests passed`，Gradle `BUILD SUCCESSFUL in 1m 45s`。没扫过的种子在前 63 个 16 EU 包之后输出槽仍是空的，第 64 包之后槽里是同一组属性的已扫描种子。已经扫过的种子在第 1 包之后原样回到输出槽。
