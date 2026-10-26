# API：契约与扩展声明

返回[包索引](README.md)。先区分 core 的纯契约与 Minecraft 层的游戏类型适配，再决定接口形态。`public` 不自动意味着可对外承诺长期兼容；以实际消费入口、SDK 内容和版本声明为准。

## core 的 API

源码入口为 [core/gregtech/api](../../../core/src/main/java/gregtech/api)。以下表内路径相对于 `gregtech`。

| 包 | 应当承担 | 代表入口/关注点 |
| --- | --- | --- |
| `api` | API 包的总体契约说明 | `package-info.java` |
| `api.addon` | 附属发现、身份、生命周期、命名限制与 SPI | `GregTechAddon`、`GregTechAddons`、`AddonRuntime`；声明失败应有上下文 |
| `api.addon.material` | 材料、前缀、成分、形态与熔炼声明 | `MaterialDeclaration`、`MaterialComposition`、`GregTechMaterials` |
| `api.addon.machine` | 机器、行为、部件、布局及参数声明 | `AddonMachineDefinition`、`AddonMachineLayout`、`BehaviorMachine` |
| `api.addon.recipe` | 配方表、坩埚反应、模具、保留输入与概率声明 | `RecipeMachine`、`CrucibleReactionDeclaration`、`RecipeOutputChances` |
| `api.addon.pipe` | 附属物品/流体管道的定义 | `ItemPipeDeclaration`、`FluidPipeDeclaration` |
| `api.addon.worldgen` | 矿脉与小矿生成声明及命名规则 | `OreVeinDeclaration`、`SmallOreDeclaration` |
| `api.material` | 材料公共模型、成分、性质、原子属性和数量常量 | `GTMaterial`、`MaterialDefinition`、`GTValues`；不可引入物品栈 |
| `api.energy` | 能源类型、输入输出与转移契约 | `EnergyType`、`EnergyInput`、`EnergyOutput`、`GTVoltageTiers` |
| `api.machine` | 机器面、方向掩码、基础参数、热处理等协议 | `MachineFace`、`MachineFaceMasks`、`BasicMachineParameters` |
| `api.multiblock` | 布局、单元、角色、重复段与匹配结果 | `StructureLayout`、`StructureGrid`、`LayoutCell`；不直接查询世界 |
| `api.recipe` | 配方表规格 | `RecipeMapSpec`；具体配方和执行在 content/engine |
| `api.sensor` | 压力、温度、重量和进度等观测源 | `TemperatureSource`、`ProgressSensorSource` |
| `api.signal` | GT 信号的发送、接收和承载契约 | `GtSignalSource`、`GtSignalReceiver`、`GtSignalCarrier` |
| `api.crop` | 作物扫描协议 | `CropScanSource` |
| `api.mod` | 模组存在性等公共信息接口 | `ModData`；具体加载器绑定在启动阶段 |

这些包只定义必要可消费的协议，不放注册执行、游戏世界访问、客户端渲染或内部缓存。扩展输入在进入运行时前验证身份、范围、冲突与引用，避免把错误拖到机器 tick 才暴露。

## Minecraft 层的 API

入口为 [minecraft/gregtech/api](../../../minecraft/src/main/java/gregtech/api)。

| 包 | 应当承担 | 边界 |
| --- | --- | --- |
| `api.addon.platform` | 附属使用的物品、流体、能源、加载器、平台服务桥接 | 允许游戏类型；具体绑定由 `engine.addon` 和平台实现完成 |
| `api.machine` | 游戏中的机器控制接口 | 当前有 `MachineControl`，与 core 同包但不同源码层 |
| `api.transport` | 游戏世界中管道的连接契约 | `FluidPipeConnectable`、`ItemPipeConnectable`；避免公开整个内部网络 |

## 修改顺序与验证

先确认外部消费者需要什么，再定义最小契约；由 engine 提供执行，bootstrap/platform 绑定，content 消费。接口新增字段、方法或默认行为时，检查两个加载器的实现与附属生命周期，说明二进制、源码和数据兼容影响。

SDK 打包并非“把所有 api 名字的类都收进去”。当前构建包含 core 的 `gregtech/api/**` 与 `sourceSets.api` 输出，而上述 Minecraft API 位于 `src/main/java`。修改对外接口必须检查最终 SDK 的实际类，详见[附属 API 说明](../../ADDON_API.zh-CN.md)。验证至少包括 core 检查、双平台编译和相关 SDK 产物检查。
