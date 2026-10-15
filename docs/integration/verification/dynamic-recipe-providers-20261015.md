# 2026-10-15 多动态配方 provider

查找顺序保持为固定表、动态 provider、带世界的 context provider。`dynamicRecipes` 替换最前面的内置 provider。`addDynamicRecipes` 追加到后面，第一个非空结果生效。

## 编译

Java 21 启动 Gradle，EMI 从本机 `work/local-maven` 解析。`:compileJava` 与 `:neoforge:compileJava` 在 36 秒内通过，3 个任务实际执行。

## 新测试

命名空间 `gregtech_dynamic_recipes`，目录 `build/card03-dynamic-providers-3` 与 `neoforge/build/card03-dynamic-providers-3`。Forge 09:25:44、NeoForge 09:28:13 各报告 `All 3 required tests passed`。整次 Gradle 7 分 53 秒，`BUILD SUCCESSFUL`。

三项分别锁住：前一个 provider 返回空时后一个被调用并生效；前一个返回配方时后一个不被调用；固定行命中时两个 provider 都不被调用。

## 已有分子数据链

命名空间 `gregtech_material_data`，目录 `build/card03-material-data` 与 `neoforge/build/card03-material-data`。这组覆盖分子扫描、打印机和复制机上现有的 `dynamicRecipes`。Forge 09:34:31、NeoForge 09:37:34 各 3 项通过。整次 8 分 3 秒，`BUILD SUCCESSFUL`。

## 还没跑

`BumbleBeeScanningTests` 的持有者是 `gregtech`，和整套生存测试在一起，这次没有单独重跑。gt6-crops 的 Plantalyzer 种子扫描没有实现，也没有在开发环境里看 64 tick、16 EU/t 和已扫描种子的 1 tick 直通。issue #13 没有回复。
