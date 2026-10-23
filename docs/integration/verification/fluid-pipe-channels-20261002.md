# 多通道流体管：按流体选择接收通道

日期：2026-10-02。当前源码目录：`C:\Dev\GregTech6\GregTech6-main`。此目录是解压后的源码快照，没有 `.git`，因此不关联此前机器上的提交或旧构建结果。

## 来源与取舍

提供的新参考实际位于 `C:\Dev\gregtech6_w-main\gregtech6_w-main`。其 README 标明当前快照面向 Minecraft 26.1.2 / NeoForge / Java 25；本项目继续保持 Forge 1.20.1 / Java 17 与 NeoForge 1.21.1 / Java 21，共享领域逻辑，不整体搬入该版本的引擎适配层。

核对以下本地源码的 `MultiTileEntityPipeFluid.getFluidTankFillable2`、`distribute` 和 `fill`：

- 原 GT6：`C:\Dev\gregtech6-master\gregtech6-master\src\main\java\gregapi\tileentity\connectors\MultiTileEntityPipeFluid.java`，SHA-256 `3a6850055375e7975145b03abab44c5922746a6f4053dd8d3163de989f37b110`。
- wolfram0108 移植版：`C:\Dev\gregtech6_w-main\gregtech6_w-main\src\main\java\gregapi\tileentity\connectors\MultiTileEntityPipeFluid.java`，SHA-256 `f368a2cea36dd76b0d22e393de21e7fcc8eec0ee021e96805b552cc23e077335`。
- brokestar 的 `GTFluidPipeBlockEntity.getFluidTankFillable` 同样先匹配后找空槽，并只标记接收通道；cruciblecraft 的流体管有独立的面过滤、失败记录和存储接口，本批不引入另一套存储。

原版及 wolfram 文件保留 GregTech-6 Team 的 LGPL-3.0-or-later 头，后者另外注明 2026 年 wolfram0108 的移植修改。本批根据行为重新实现共享选择规则，没有复制整个来源文件、资产或版本适配层；来源项目保持只读。

## 修正

两平台 `FluidPipeBlockEntity` 共用 `core/api/fluid/FluidPipeChannels`：

1. 同流体已占用的槽优先于空槽；该槽已满时拒绝继续填充，不再让同一流体占掉第二条通道。
2. 邻管槽号独立。四通道管的第 2 条通道可以送入单通道管第 1 条；两根管道以不同顺序填入流体也按实际流体库存均衡。
3. 回流位只写实际接收槽，其他流体仍能向同一邻管流动。
4. 插入与指定流体抽取都使用平台完整身份（Forge NBT / NeoForge components），避免抽错同类型、不同附加数据的流体。
5. 保持现有逐对均衡及奇数差额取整策略，用差值计算避免两个 long 库存求和溢出。

注册 ID、容量、配方、NBT 键和槽顺序不变；不会主动整理旧档中已经重复的流体槽。没有声明跨版本存档迁移。

## 验证

- Java 17 独立编译全部 core 主源码与测试源码成功；四个行为入口全部通过：基础 113、材料 6160、热力 53、机器规格 34 条断言。日志：`work/core-contracts.log`。
- 新增两平台各 5 个隔离 GameTest：匹配槽优先/满槽拒绝、异槽号接单通道、不同填充顺序、回流仅影响接收流体、流体附加数据身份；包含模拟不改状态与数量守恒检查。测试在 bootstrap 源集，不打入生产 JAR。
- Gradle 的 `:core:check :build :neoforge:build :compileBootstrapGameTestJava :neoforge:compileBootstrapGameTestJava` 全部通过，首次构建耗时 32m46s，30 项任务执行，退出 0。包含 Forge / NeoForge 生产源码与两版新增测试源码；日志 `work/fluid-channels-build-retry.log`。Java 17.0.10 启动，NeoForge 使用 Java 21.0.7 工具链。
- 首次 Forge 验包拒绝新根 NOTICE 与内嵌副本不一致；同步 `core/src/main/resources/META-INF/gregtech6/NOTICE` 后已重新构建并复验通过。初次失败保留为检查发现的问题，不记作初次通过。
- Forge GameTest 服务器在 15:27:08 输出 `All 5 required tests passed`，随后正常关闭。实际已注册管道方块上的模拟/执行、数量与通道检查通过。两版运行日志：`work/fluid-channels-gametest.log`。
- NeoForge GameTest 服务器在 15:34:42 输出 `All 5 required tests passed`，随后正常保存三维度并关闭。完整运行命令退出 0，耗时 10m12s，包含首次下载 NeoForge 的 3,888 个运行资源（786 MiB）。两版均发现并执行了全部 5 项新测试，没有空测试通过。
- 这些测试直接调用真实方块实体的单 tick 入口，夹具提供管道与流体；证明选定通道行为与守恒，不证明完整玩家生存流程、真实客户端操作、独立 JVM 存档重载或成品 JAR 安装运行。

测试入口：设置 `JAVA_HOME` / `JAVA17_HOME` 为 JDK 17、`JAVA21_HOME` 为 JDK 21。

```text
gradlew.bat --console=plain :core:check :build :neoforge:build :compileBootstrapGameTestJava :neoforge:compileBootstrapGameTestJava
gradlew.bat --console=plain -PdirectCoreResources=true -PdirectCoreClasspath=true -PgameTestNamespaces=gregtech_fluid_channels -PgameTestDirectory=build/fluid-channel-gametest-run -PneoforgeGameTestDirectory=build/fluid-channel-gametest-run :runGameTestServer :neoforge:runGameTestServer
```

本机 wrapper 首次无法写缓存，获准重试后官方下载的 GitHub 跳转超时。改从 Gradle 镜像取得 8.8，并与官方 `downloads.gradle.org` 给出的 SHA-256 `a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612` 核对一致，解压到 `work/gradle-runtime/gradle-8.8`。实际命令用其中的 `bin/gradle.bat` 代替 wrapper，没有修改项目 wrapper 或关闭 TLS 校验。

最终重打包曾在 ForgeGradle `EnvironmentChecks.testServerConnection` 的无读取超时 TLS 握手处等待（线程诊断 `work/gradle-package-diagnostic.txt`）；取消该次命令后，使用 `--no-daemon --offline` 及 JVM 的 `sun.net.client.defaultConnectTimeout=15000` / `sun.net.client.defaultReadTimeout=15000` 重试。未设置 `net.minecraftforge.gradle.check.certs=false`，未禁用 TLS/证书校验，也未修改项目依赖版本。

## 最终产物

正式 `:distributionJar :neoforge:jar` 重打包成功（7m53s，退出 0，未使用 directCore 开发选项），日志 `work/fluid-channels-final-package-retry.log`。`verify_artifacts.py` 验证 585 个共享 class 与当前 core 编译结果逐字节一致，两版元数据、当前许可/NOTICE、无重复项及无测试专用条目均通过。完整结果：[验包回执](fluid-pipe-channels-artifacts-20261002.json)。

| 平台 | 相对项目根目录的文件 | 字节数 | SHA-256 |
|---|---|---:|---|
| Forge 1.20.1 | `build/libs/gregtech6-1.20.1-forge-0.0.0.jar` | 38,483,557 | `ed2e1d23def9b94d3e67dbd5a2a271ef09b197da3de8d3a87f5e2fa20e531012` |
| NeoForge 1.21.1 | `neoforge/build/libs/gregtech6-neoforge-1.21.1-0.0.0.jar` | 36,675,072 | `313e19070c7a872c1fce21bafd6fa73dabbf3e4bd33bc1eecdc2e564b5220471` |

源码开发环境 GameTest 与成品验包分别通过；本批没有额外安装最终 JAR 启动客户端或专服，不将验包扩大为安装运行证明。

## 已确认但不在本批闭合的流体差异

- 原版向炼药锅补水的特殊分配路径尚未落入当前 `distribute`。
- 当前均衡仍是既有逐对算法，不等同于原版的多目标均分、随机目标顺序和二次压力分配。
- 外部面能力写入的回流标记、关闭接收面与拆管转移的完整语义仍需另批对照。
- 大网络性能、真实世界重载、完整覆盖板组合和生存链路不由本批单 tick 夹具证明。
