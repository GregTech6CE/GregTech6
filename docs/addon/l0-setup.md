# L0：工程、发现与生命周期

本页用于配置附属依赖、注册附属身份并选择 SPI；声明内容前先确认这些启动约束。

返回[附属编写指南](README.md)。

## 独立库与 SDK 构建

公共 API 使用 Java 17；Forge 1.20.1 SDK 使用 Java 17，NeoForge 1.21.1 SDK 使用 Java 21。固定目标 Minecraft、加载器和 GT6CE 构建后，再选择 SDK。

所有 API 源码内联自独立仓库 [GregTech6API](https://github.com/GregTech6CE/GregTech6API)（锁定 a9c879176，基线 API 1.2.0）；主库的 `api/` 是直接跟踪的源码，不是 Git 子模块，普通 `git clone` 即可。修改公共签名时直接在本仓库 `api/` 内更新，并在提交信息中记录 API 版本变化；主库构建不自动跟随远程分支。

命令在仓库根执行：

```bash
./gradlew :api:check :api:assemble
./gradlew :minecraft:1.20.1-forge:addonSdk
./gradlew :minecraft:1.21.1-neoforge:addonSdk
```

构建 SDK 时按目标平台选择一条命令；修改共享 API 源码后的验收按[验证与验收](../development/validation/README.md)执行。Windows 使用 `.\gradlew.bat`。`:api` 使用 Java 17，仅依赖 JDK，不配置 Minecraft、Stonecutter 或模组资源；可通过 `JAVA17_HOME` 提供工具链。

| 产物坐标 | 内容与输出 |
| --- | --- |
| `gregtech:gt6ce-api:1.10.0` | Java 17 公共协议、模型、声明和宿主服务接口；`api/common/build/libs/` |
| `gregtech:gt6ce-addon-sdk-1.20.1-forge:1.10.0` | Java 17、Mojang 名称的 Forge 游戏侧 API；对应节点 `build/addon-sdk/` |
| `gregtech:gt6ce-addon-sdk-1.21.1-neoforge:1.10.0` | Java 21 的 NeoForge 游戏侧 API；对应节点 `build/addon-sdk/` |

这些是构建配置中的坐标，不表示已发布到公共 Maven 仓库。版本规则见下节[API 版本](#api-版本)。

`addonSdk` 同时构建公共库与目标平台 SDK 的 binary、sources 和 javadoc JAR。平台源码位于 `api/minecraft/src/main/java/`，由独立 `:api-platform:<node>` 项目经 Stonecutter 编译，不复制公共库类，也不包含 core/main 实现。原始产物在 `api/minecraft/versions/<node>/build/libs/`；主库的兼容命令将 SDK 复制到对应模组节点 `build/addon-sdk/`。`addonSdkJar` 仅导出平台编译 JAR。两平台 SDK 不能互换，消费者须配置对应 Minecraft/加载器开发环境。

公共库配置了 `api` Maven publication；独立平台项目配置了 `addonApi` publication，其 POM 仅声明公共 API 库的编译依赖。可用 `:api:generatePomFileForApiPublication` 或 `:api-platform:1.20.1-forge:generatePomFileForAddonApiPublication` 检查元数据，不必实际发布。主库保留同名 POM 生成委托入口。发布仓库需另行配置，发布操作不属于普通构建。

附属通过 Maven 使用平台 SDK 时应选择 `compileOnly`；直接使用本地文件时须同时提供公共 API JAR 和目标平台 SDK JAR。运行时由 GT6CE 模组携带唯一的一份公共及游戏 API，附属不可重新打包这些类。SDK 不能作为独立模组放进 `mods/`。

`AddonRuntime`、`AddonPlatform`、`AddonLoader` 的实现仍由宿主在原有初始化阶段绑定。独立库允许声明和纯模型操作；材料目录、物品与配方查询仍要求宿主就绪。SPI provider 资源留在宿主或附属，不能跟随接口复制进公共库。客户端接口只能由客户端入口加载。

## API 版本

当前分支使用 API `1.10.0`，版本独立于模组版本，以 [api/common/build.gradle](../../api/common/build.gradle) 为准。`GregTechAddons.apiVersion()` 返回 `major * 1000 + minor`，1.10 为 `1010`。可以用 `GregTechAddons.apiVersion()` 或 `GregTechAddon.Context.apiVersion()` 判断特性版本，例如 1.10 使用 `>= 1010`。从旧版升级时，按[旧入口迁移](compatibility.md#旧入口迁移)更新代码，并使用匹配目标加载器和运行时构建的 SDK 重新编译附属。修改内联 API 时同步构建配置与 `GregTechAddons` 的版本。

宿主专用的 `StructureGrid` 与 `RecipeMapSpec` 不属于附属契约：它们分别位于 `gregtech.content.multiblock` 与 `gregtech.content.recipe`，编码内置多方块几何和配方表，附属不应依赖。

公共 API 的契约检查、SDK 与平台检查见[验证与验收](../development/validation/README.md)。移动到 API 包的可变模型或内部辅助类不自动获得长期兼容保证。

## 附属身份与生命周期

`GregTechAddon.id()` 必须返回附属自己的加载器 mod ID，而不是 `gt6ce`、`gregtech6` 或 `minecraft`。附属在模组构造阶段调用 `GregTechAddons.register(addon)`，宿主在所有材料、物品链接和内置机器配方就绪后调用一次 `onRecipesReady(Context)`。

生命周期约束：

- 同一附属 ID 只能注册一次；重复 ID、生命周期关闭后的迟注册和重复 dispatch 会明确报错。
- `dispatchRecipesReady` 开始后不能继续注册附属或自定义配方文档类型；附属不调用平台负责的 dispatch 方法。
- 回调失败会把生命周期置为 `FAILED`；已经执行完的附属不会回滚。
- 生产代码没有 reset 入口。需要全新生命周期的测试使用独立 JVM；测试 hook 不是附属可用的启动开关。
- `onRecipesReady` 适合添加或查询已经就绪的配方、材料和目录；需要早期纯数据声明的材料或机器仍通过 L1 的贡献接口提交。

## 八个 `ServiceLoader` 扩展点

除 `GregTechAddon` 的显式注册外，宿主通过 Java `ServiceLoader` 收集下列 SPI。provider 必须提供对应接口要求的 `addonId()`；资源文件放在附属 JAR 的 `META-INF/services/` 下。

| SPI | 服务文件 | 作用与提交时机 |
| --- | --- | --- |
| `AddonMaterialContributor` | `META-INF/services/gregtech.api.addon.material.AddonMaterialContributor` | 材料开放期收集纯声明；`link()` 在内置材料链接后执行 |
| `AddonMachineContributor` | `META-INF/services/gregtech.api.addon.machine.AddonMachineContributor` | 材料初始化后、机器和管道注册前收集配方表、机器、布局与部件声明 |
| `MachineTuningContributor` | `META-INF/services/gregtech.api.addon.machine.MachineTuningContributor` | 在内置机器目录变成运行时规格前，提交内置机器的显式参数覆盖 |
| `BehaviorMachineContributor` | `META-INF/services/gregtech.api.addon.platform.BehaviorMachineContributor` | 平台侧为行为机类型提供工厂；每个已放置方块实体创建一次实例 |
| `LayoutPartContributor` | `META-INF/services/gregtech.api.addon.platform.LayoutPartContributor` | 平台侧为功能部件提供工厂；每个已放置方块实体创建一次实例 |
| `AddonSensorContributor` | `META-INF/services/gregtech.api.addon.platform.AddonSensorContributor` | 在方块注册前提交自定义传感器种类；宿主注册传感器方块和方块物品 |
| `BehaviorMachineClient` | `META-INF/services/gregtech.api.addon.platform.BehaviorMachineClient` | 仅在客户端收集行为机菜单屏幕和附加渲染器 |
| `LayoutPartClient` | `META-INF/services/gregtech.api.addon.platform.LayoutPartClient` | 仅在客户端收集功能部件附加渲染器 |

`GregTechAddon` 不是 `ServiceLoader` 接口；它必须在模组构造阶段显式注册。`AddonPlatform.Services`、`AddonLoader.Services`、`AddonRuntime.Services` 和 `MaterialInfoTool.Host` 是宿主实现契约，不是可供附属替换的 SPI。

provider 的覆盖声明必须完整。`BehaviorMachineContributor.machineTypes()` 要覆盖该附属声明的全部行为机类型，`LayoutPartContributor.partNames()` 要覆盖需要行为的全部功能部件；缺失会在创建对应方块实体时失败。客户端 provider 只在实际客户端入口加载，不能把客户端类型泄漏到公共 API 或服务端通用代码。

## 名称、身份和注册边界

宿主会校验 addon ID、机器类型、配方表名、资源路径和纹理路径。材料名、前缀名、世界生成名和管道后缀不能与同域已有条目冲突。机器类型和方块实体 ID 也不要占用内置机器名称。

[L1 的贡献接口](l1-declarations.md)用于把纯声明交给宿主注册，不用于手工注册方块、方块实体、菜单或网络包。需要附属自己的普通内容时使用目标加载器的注册系统；需要和 GT 机器或传输系统互通时，再实现 [L4 的接口](l4-interop.md)。

下一步：[L1 声明式内容](l1-declarations.md)；只写数据包时可直接读 [L3 数据包与 KubeJS](l3-data-and-scripts.md)。
