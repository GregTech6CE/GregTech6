# Minecraft 1.21.1 NeoForge 适配进度

当前平台模块是整合项目的启动骨架，尚未注册玩法内容。Forge 1.20.1 原有内容仍在根项目。不能把本模块编译或启动成功写成 NeoForge 的玩法移植完成。

## 构建边界

- Gradle wrapper：8.8，不升级。
- Gradle launcher：Java 17；NeoForge 编译和游戏启动：Java 21。
- Minecraft：1.21.1；NeoForge：21.1.243；ModDevGradle：2.0.142。
- modId：`gregtech`；版本读取根项目 `mod_version`，目标 `0.1.0-integration.1`。
- 产物名：Forge `gregtech-1.20.1-forge-0.1.0-integration.1.jar`；NeoForge `gregtech-neoforge-1.21.1-0.1.0-integration.1.jar`。两者版本/平台清晰区分。
- 根 Forge 和 NeoForge 共同 `implementation project(':core')`，发布 jar 各嵌入一次 `core` 的相同 Java 17 编译输出。NeoForge 开发 runs 将 `core` 源码集和平台源码集合并为同一个本地 mod，避免纯 Java 库因没有 FML mod 元数据而在开发启动时不可见。
- 平台内不得再创建 CrucibleCraft 的材料、能源、配方或注册底层。统一使用 GT6 U 和基线 `gregtech` 注册 ID；第三方代码/资产逐项记录来源，授权未决时保持原授权声明。

### Gradle / Java 兼容证据

已读取官方 [ModDevGradle 文档](https://github.com/neoforged/ModDevGradle)，其说明兼容 Gradle 8.8。官方 [TrampolinePlugin](https://github.com/neoforged/ModDevGradle/blob/main/src/java8/java/net/neoforged/moddevgradle/boot/TrampolinePlugin.java) 明确检查最低 Java 17、最低 Gradle 8.8。

另外直接读取了实际发布的 [2.0.142 模块元数据](https://maven.neoforged.net/releases/net/neoforged/moddev-gradle/2.0.142/moddev-gradle-2.0.142.module) 和 [2.0.142 jar](https://maven.neoforged.net/releases/net/neoforged/moddev-gradle/2.0.142/moddev-gradle-2.0.142.jar)：`internal/ModDevPlugin.class` 的 class major 为 61（Java 17），`boot/TrampolinePlugin.class` 为 52（Java 8），启动版本检查中包含最低 Gradle `8.8` 和 Java 17 的拒绝提示。发布元数据 JVM 属性 8 对应兼容检查入口，不能据此误认为主插件可在 Java 8 运行。

这些证据支持保持 Gradle 8.8 和 Java 17 launcher；本次项目配置、依赖解析、编译和启动结果才是可运行性验收，不能由官方兼容声明替代。

## 初始平台职责

`GregTechNeoForge` 只挂接 `FMLCommonSetupEvent` 并调用真正共享的 `MachineWorkCost`。输入 `(8,20,1,false,10000,8,32,false)` 必须返回最低功率 `8`、总功 `160`，失败直接停止初始化。日志包含 `GregTech shared core initialized`，同时说明玩法移植尚待完成。没有为了通过启动而加入示例方块、替代材料或空 GameTest。

`neoforge.mods.toml` 的必需依赖只包括目标 Minecraft 与 NeoForge。客户端/专服共享该 jar。实际客户端渲染事件与 gameplay 注册待对应系统迁移，当前没有客户端专属引用。

## 验证记录

阶段：P1 平台骨架。2026-09-30 首轮共同构建通过，日志 `work/p1-core-forge-compile-neoforge-build.log`，耗时 5m28s，13 个任务。新增 bootstrap GameTest 的下一轮验证尚待执行。

- `:core:check`：通过 64 条行为断言、7 组；不能用空 `:core:test` 的成功替代。
- `:compileJava`：Forge 共享核心接线后编译通过。
- `:neoforge:build`：初始骨架通过并生成 jar，尚未验证新增 bootstrap 测试配置。
- `:neoforge:runGameTestServer`：新增 3 个 bootstrap 测试，尚未执行。
- `:neoforge:runClient`：未执行。
- `:neoforge:runServer`：未执行。
- 注册、完整玩法、真实世界保存/正常关服/重启：未实施。

GameTest 服务器不代替真实专服持久化测试。当前没有游戏内容，骨架阶段不声明旧存档兼容。

### 开发 GameTest bootstrap

测试放在独立 `bootstrapGameTest` 源码集，不进入发布 jar。`gameTestServer` 使用该源码集和单一 `gregtech_bootstrap` namespace；三项测试真正断言共享核心的普通/廉价超频功率与总功、U 与流体物质量、符号能量和有限缓冲边界，断言后才 `succeed()`。金样与 Forge wrapper 相同。

模板直接复用项目 1：`Libs/gregtech6reborn/src/main/resources/data/gregtech/structures/test_empty.nbt`，110 bytes，SHA-256 `147ef3e2c39e8d261ff2287395af00d734ccd13b196abdea0c0334e11fdde075`。NeoForge 1.21.1 复制到 `neoforge/src/bootstrapGameTest/resources/data/gregtech_bootstrap/structure/test_empty.nbt`，只改变资源目录/namespace，保留字节与作者归属。Forge 使用 `structures/` 复数目录；NeoForge 使用 `structure/` 单数目录。

[NeoForge GameTestHooks](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/gametest/GameTestHooks.java) 扫描 mod 中 `@GameTestHolder` 并发现测试方法；`@PrefixGameTestTemplate(false)` 禁止自动添加类名前缀。[GameTestRegistry 补丁](https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/gametest/framework/GameTestRegistry.java.patch) 按 template namespace 过滤，故 bootstrap 不覆盖成 `gregtech` template namespace。

两平台官方开发 GameTestServer 自行退出，required 测试失败决定退出码。官方 [NeoForge Eula 补丁](https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/server/Eula.java.patch) 与 [Forge Eula 补丁](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/patches/minecraft/net/minecraft/server/Eula.java.patch) 明确为开发 GameTest 放行；本流程不写 `eula=true`。这仅是框架测试启动证据，不代表正式 dedicated server、玩法或存档验证。

客户端后续验证应使用隔离 gameDirectory，确认 TitleScreen 出现、加载 overlay 消失且产生可定位的本次日志，之后由 opt-in 的客户端 tick 监听正常 `Minecraft.stop()` 退出。第一 tick smoke 只说明初始化没有崩溃，不能代替世界渲染、界面交互和 gameplay 验收。正式专用服务器若要求 EULA，保留原文件并经确认；不借 GameTest 开发豁免改变正式运行配置。

## 首条玩法链的最小依赖闭包

以项目 1 的铜/锡 → 燃料热源 → 坩埚青铜 → 浇注冷却 → 基础加工为基线。先复核原有源码和运行行为，再按下列依赖顺序迁移：

1. 稳定材料身份及 U 物质量：导入统一材料性质 DTO，保留原注册 ID；材料前缀/item 到统一材料与 U 的解析由两平台适配。不能将 CC 的 144 units/锭当全局精度，CC 数据通过显式转换到 648,648,000 U/锭。
2. 热力/合金：共享核心执行质量加权混温、热包余量、整数合金配比与产量、熔融/固化/沸腾；平台只提供热源与属性、世界副作用。可评估采用 masson 的纯热模型，必须保存 LGPL 来源并与基线 GT6 数值/行为比较。
3. 平台注册闭包：材料输入/产物的 Item、热源 Block/BlockEntity、坩埚 Block/BlockEntity、模具、配方/标签和实际必需资源；每类只注册一次。1.21 Item 数据组件与 1.20 NBT 在平台转换为共享 Snapshot，防止复制第二材料目录。
4. 玩家交互与同步：投入/浇注/取出、GUI/液面温度显示、服务端权威处理、容量/方向/simulate→commit 和异常材料隔离。
5. 两版本同一流程的物质量/热量/产物差异核对，再真实保存、正常退出、重载世界。加入至少一台基础加工机后才判断该完整玩法链验收。

masson 审计详情在 `work/audit-masson.md`。其 `CrucibleProcessCore` 仍直接引用 NeoForge FluidStack/IFluidHandler、Minecraft NBT 和 MaterialCatalog，不能整体放进 `core`。优先抽热力与比例运算，按统一材料接口改造合金/组成缓存；保留世界危害与持久化 codec 在平台。
