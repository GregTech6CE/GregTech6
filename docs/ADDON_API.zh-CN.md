# 附属与脚本扩展入口

本页提供源码导览与接入边界，不是完整 API 参考或已验证的示例工程。先读[API 包职责](architecture/packages/api.md)，开发版附属需固定匹配的 GT6CE 运行时、平台和 SDK 构建。

## 按需求找契约

| 需求 | 入口 | 时机/限制 |
| --- | --- | --- |
| 注册附属及配方就绪回调 | [GregTechAddon](../api/common/src/main/java/gregtech/api/addon/GregTechAddon.java)、[GregTechAddons](../api/common/src/main/java/gregtech/api/addon/GregTechAddons.java) | 在附属模组构造阶段注册；配方就绪后不接受迟注册 |
| 材料、形态、前缀、成分与矿物生成 | [AddonMaterialContributor](../api/common/src/main/java/gregtech/api/addon/material/AddonMaterialContributor.java) | ServiceLoader 早期收集纯声明，`link()` 在后续链接阶段执行 |
| 配方表、机器、布局、部件与管道 | [AddonMachineContributor](../api/common/src/main/java/gregtech/api/addon/machine/AddonMachineContributor.java) | 材料初始化后、机器注册前收集，不在回调中手工注册方块 |
| 能源/物品/流体/加载器等游戏能力 | [api.addon.platform](../api/minecraft/src/main/java/gregtech/api/addon/platform) | Minecraft 类型桥接，使用匹配版本和加载器的 SDK |
| KubeJS 脚本扩展 | 两节点的 `gregtech.compat.kubejs`、core 的 `engine.kubejs` | 原生插件接入与纯声明规则分层；使用对应平台版本 |

材料与机器 SPI 分别使用资源文件 `META-INF/services/gregtech.api.addon.material.AddonMaterialContributor`、`META-INF/services/gregtech.api.addon.machine.AddonMachineContributor` 列出实现类。按接口声明提供附属自身 ID 和纯数据；命名、范围与冲突规则以接口和 `AddonNames` 等校验为准。不要新用已标记弃用的 `GregTechMaterialContributor`。

`GregTechAddons` 的生命周期是单次的，重复 ID、迟注册或回调失败会明确报错；附属不调用平台负责的 `dispatchRecipesReady`。需要全新生命周期的测试应使用独立 JVM，不添加生产 reset 入口绕过约束；测试边界见[验证与验收](development/validation/README.md)。API 数字版本不能替代 SDK 与运行时构建匹配。

## 独立库与 SDK 构建

所有 API 源码内联自独立仓库 [GregTech6API](https://github.com/GregTech6CE/GregTech6API)（锁定 a9c879176）；主库的 `api/` 是直接跟踪的源码，不是 Git 子模块，普通 `git clone` 即可。修改公共签名时直接在本仓库 `api/` 内更新，并在提交信息中记录 API 版本变化；主库构建不自动跟随远程分支。

命令在仓库根执行：

```bash
./gradlew :api:check :api:assemble
./gradlew :minecraft:1.20.1-forge:addonSdk
./gradlew :minecraft:1.21.1-neoforge:addonSdk
```

构建 SDK 时按目标平台选择一条命令；修改共享 API 源码后的验收按[验证与验收](development/validation/README.md)执行。Windows 使用 `.\gradlew.bat`。`:api` 使用 Java 17，仅依赖 JDK，不配置 Minecraft、Stonecutter 或模组资源；可通过 `JAVA17_HOME` 提供工具链。

| 产物坐标 | 内容与输出 |
| --- | --- |
| `gregtech:gt6ce-api:1.2.0` | Java 17 公共协议、模型、声明和宿主服务接口；`api/common/build/libs/` |
| `gregtech:gt6ce-addon-sdk-1.20.1-forge:1.2.0` | Java 17、Mojang 名称的 Forge 游戏侧 API；对应节点 `build/addon-sdk/` |
| `gregtech:gt6ce-addon-sdk-1.21.1-neoforge:1.2.0` | Java 21 的 NeoForge 游戏侧 API；对应节点 `build/addon-sdk/` |

这些是构建配置中的坐标，不表示已发布到公共 Maven 仓库。版本以 [api/common/build.gradle](../api/common/build.gradle) 为准，与模组版本独立；`GregTechAddons.apiVersion()` 返回 `major * 1000 + minor`（1.2 为 1002），不能据此推断某个模组构建兼容。

`addonSdk` 同时构建公共库与目标平台 SDK 的 binary、sources 和 javadoc JAR。平台源码位于 `api/minecraft/src/main/java/`，由独立 `:api-platform:<node>` 项目经 Stonecutter 编译，不复制公共库类，也不包含 core/main 实现。原始产物在 `api/minecraft/versions/<node>/build/libs/`；主库的兼容命令将 SDK 复制到对应模组节点 `build/addon-sdk/`。`addonSdkJar` 仅导出平台编译 JAR。两平台 SDK 不能互换，消费者须配置对应 Minecraft/加载器开发环境。

公共库配置了 `api` Maven publication；独立平台项目配置了 `addonApi` publication，其 POM 仅声明公共 API 库的编译依赖。可用 `:api:generatePomFileForApiPublication` 或 `:api-platform:1.20.1-forge:generatePomFileForAddonApiPublication` 检查元数据，不必实际发布。主库保留同名 POM 生成委托入口。发布仓库需另行配置，发布操作不属于普通构建。

附属通过 Maven 使用平台 SDK 时应选择 `compileOnly`；直接使用本地文件时须同时提供公共 API JAR 和目标平台 SDK JAR。运行时由 GT6CE 模组携带唯一的一份公共及游戏 API，附属不可重新打包这些类。SDK 不能作为独立模组放进 `mods/`。

`AddonRuntime`、`AddonPlatform`、`AddonLoader` 的实现仍由宿主在原有初始化阶段绑定。独立库允许声明和纯模型操作；材料目录、物品与配方查询仍要求宿主就绪。SPI provider 资源留在宿主或附属，不能跟随接口复制进公共库。客户端接口只能由客户端入口加载。

## API 1.2

主库当前锁定 API `1.2.0`。API 版本与模组版本独立；`GregTechAddons.apiVersion()` 返回 `major * 1000 + minor`，1.2 为 `1002`。附属使用 `compileOnly` 依赖匹配的公共 API 与目标平台 SDK，由宿主在初始化阶段绑定服务和发现 SPI provider。

宿主专用的 `StructureGrid` 与 `RecipeMapSpec` 不属于附属契约：它们分别位于 `gregtech.content.multiblock` 与 `gregtech.content.recipe`，编码内置多方块几何和配方表，附属不应依赖。

公共 API 的契约检查、SDK 与平台检查见[验证与验收](development/validation/README.md)。移动到 API 包的可变模型或内部辅助类不自动获得长期兼容保证。

## KubeJS 与示例状态

开发运行用 `-PkubejsRuntime=true` 加载对应原生运行依赖；插件入口由各节点 `kubejs.plugins.txt` 声明。先看目标节点的插件、事件与声明处理代码，再决定脚本形态，不能照搬另一 MC 版本的语法。

当前工作树没有 `examples/addon-sample/` 或 `examples/kubejs-sample/`。本页不提供未经编译/运行验证的替代样例；将来新增可维护消费者示例放根 `examples/`，说明依赖、构建/运行命令与已验证平台，临时消费探针放 `work/`。

验收路线与游戏内场景见[验证与验收](development/validation/README.md)。临时输出放 `work/`，长期维护的跨平台接入说明放 [integration](integration/README.md)。
