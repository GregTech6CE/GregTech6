# 附属与脚本扩展入口

本页提供源码导览与接入边界，不是完整 API 参考或已验证的示例工程。先读[API 包职责](architecture/packages/api.md)，开发版附属需固定匹配的 GT6CE 运行时、平台和 SDK 构建。

## 按需求找契约

| 需求 | 入口 | 时机/限制 |
| --- | --- | --- |
| 注册附属及配方就绪回调 | [GregTechAddon](../core/src/main/java/gregtech/api/addon/GregTechAddon.java)、[GregTechAddons](../core/src/main/java/gregtech/api/addon/GregTechAddons.java) | 在附属模组构造阶段注册；配方就绪后不接受迟注册 |
| 材料、形态、前缀、成分与矿物生成 | [AddonMaterialContributor](../core/src/main/java/gregtech/api/addon/material/AddonMaterialContributor.java) | ServiceLoader 早期收集纯声明，`link()` 在后续链接阶段执行 |
| 配方表、机器、布局、部件与管道 | [AddonMachineContributor](../core/src/main/java/gregtech/api/addon/machine/AddonMachineContributor.java) | 材料初始化后、机器注册前收集，不在回调中手工注册方块 |
| 能源/物品/流体/加载器等游戏能力 | [api.addon.platform](../minecraft/src/main/java/gregtech/api/addon/platform) | Minecraft 类型桥接，需核对 SDK 实际打包边界 |
| KubeJS 脚本扩展 | 两节点的 `gregtech.compat.kubejs`、core 的 `engine.kubejs` | 原生插件接入与纯声明规则分层；使用对应平台版本 |

材料与机器 SPI 分别使用资源文件 `META-INF/services/gregtech.api.addon.material.AddonMaterialContributor`、`META-INF/services/gregtech.api.addon.machine.AddonMachineContributor` 列出实现类。按接口声明提供附属自身 ID 和纯数据；命名、范围与冲突规则以接口和 `AddonNames` 等校验为准。不要新用已标记弃用的 `GregTechMaterialContributor`。

`GregTechAddons` 的生命周期是单次的，重复 ID、迟注册或回调失败会明确报错；附属不调用平台负责的 `dispatchRecipesReady`。需要全新生命周期的测试应使用独立 JVM，不添加生产 reset 入口绕过约束。API 数字版本不能替代 SDK 与运行时构建匹配。

## SDK 构建与实际边界

命令在仓库根执行：

```bash
./gradlew :minecraft:1.20.1-forge:addonSdk
./gradlew :minecraft:1.21.1-neoforge:addonSdk
```

`addonSdk` 构建编译用 SDK、sources 与 javadoc JAR，输出到对应节点 `build/addon-sdk/`。只需编译 JAR 可调用 `addonSdkJar`。SDK 是附属的编译依赖，不能放入游戏 `mods/` 当模组运行，也不应把 GT6CE 类重新打入附属发行包。

当前 [addon-sdk.gradle](../gradle/addon-sdk.gradle)打包 core 输出中的 `gregtech/api/**` 与 `sourceSets.api` 输出。构建声明了 `api` 源码集，但当前工作树没有对应 `src/api` 源码；Minecraft 的 `api.addon.platform` 等类型实际位于 `src/main/java`。因此不能宣称当前 SDK 已含所有游戏侧 API。接入前检查 JAR 类列表与编译结果，遇到缺失应作为 SDK 边界问题处理，不能悄悄依赖整套内部实现来冒充稳定公共接口。

新增/迁移公开类型时同时审查其引用类型是否也在 SDK、两平台处理后源码是否匹配，以及 sources/javadoc 是否完整。不要只改包名就认为 SDK 已可用。

## KubeJS 与示例状态

开发运行用 `-PkubejsRuntime=true` 加载对应原生运行依赖；插件入口由各节点 `kubejs.plugins.txt` 声明。先看目标节点的插件、事件与声明处理代码，再决定脚本形态，不能照搬另一 MC 版本的语法。

当前工作树没有 `examples/addon-sample/` 或 `examples/kubejs-sample/`。本页不提供未经编译/运行验证的替代样例；将来新增可维护消费者示例放根 `examples/`，说明依赖、构建/运行命令与已验证平台，临时消费探针放 `work/`。

验收包括 SDK 编译/类覆盖、附属发现顺序、冲突失败信息、基础启动与启用集成两种运行，结果整理到 [integration](integration/README.md)。
