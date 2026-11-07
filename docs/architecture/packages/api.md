# API：契约与扩展声明

返回[包索引](README.md)。先区分 `api` 模块的纯契约与 Minecraft 层的游戏类型适配，再决定接口形态。`public` 不自动意味着可对外承诺长期兼容；以实际消费入口、SDK 内容和版本声明为准。

## 独立公共 API

源码入口为 [api/gregtech/api](../../../api/common/src/main/java/gregtech/api)，`common/` 接为 `:api`。模块使用 Java 17，core 和游戏侧 API 单向依赖它；来源、版本与消费方式见[附属 API](../../ADDON_API.zh-CN.md#独立库与-sdk-构建)。以下表内路径相对于 `gregtech`。

| 包 | 应当承担 | 代表入口/关注点 |
| --- | --- | --- |
| `api` | API 包的总体契约说明 | `package-info.java` |
| `api.addon` | 附属发现、身份、生命周期、命名限制与 SPI | `GregTechAddon`、`GregTechAddons`、`AddonRuntime`；声明失败应有上下文 |
| `api.addon.material` | 材料、前缀、成分、形态、类型化 metadata、分类 tag 和替代组声明 | `MaterialDeclaration`、`MaterialMetadata`、`MaterialTagDeclaration`、`MaterialGroupDeclaration`、`GregTechMaterials` |
| `api.addon.machine` | 机器、行为、部件、布局及参数声明 | `AddonMachine.withStructure` 显式绑定；`MultiblockMachine`、`BehaviorMachine`、`LayoutPart`、`AddonMachineContributor` |
| `api.addon.recipe` | 配方表、坩埚反应、模具、保留输入与概率声明 | `RecipeMachine`、`CrucibleReactionDeclaration`、`RecipeOutputChances` |
| `api.addon.pipe` | 附属物品/流体管道的定义 | `ItemPipeDeclaration`、`FluidPipeDeclaration` |
| `api.addon.worldgen` | 矿脉与小矿生成声明及命名规则 | `OreVeinDeclaration`、`SmallOreDeclaration` |
| `api.material` | 材料公共模型、成分、性质、原子属性和数量常量 | `GTMaterial`、`MaterialComponent`、`GTValues`；不可引入物品栈 |
| `api.energy` | 能源类型、输入输出与转移契约 | `EnergyType`、`EnergyInput`、`EnergyOutput`、`GTVoltageTiers` |
| `api.machine` | 机器面、方向掩码、基础参数、热处理等协议 | `MachineFace`、`MachineFaceMasks`、`BasicMachineParameters` |
| `api.multiblock` | 布局、单元、角色、重复段、匹配约束和位置端口规则 | `StructureLayout`、`StructurePattern`、`StructureConstraint`、`CellMatch`、`StructurePorts`；不直接查询世界 |
| `api.recipe` | 配方包入口说明 | 仅 `package-info.java`；`RecipeMapSpec` 属于宿主 `gregtech.content.recipe` |
| `api.sensor` | 压力、温度、重量和进度等观测源 | `TemperatureSource`、`ProgressSensorSource` |
| `api.signal` | GT 信号的发送、接收和承载契约 | `GtSignalSource`、`GtSignalReceiver`、`GtSignalCarrier` |
| `api.crop` | 作物扫描协议 | `CropScanSource` |
| `api.mod` | 模组存在性等公共信息接口 | `ModData`；具体加载器绑定在启动阶段 |

`StructureGrid` 与 `RecipeMapSpec` 已移回宿主，分别位于 `gregtech.content.multiblock` 与 `gregtech.content.recipe`；它们不属于附属契约。

## Minecraft 层的 API

入口为 [minecraft/gregtech/api](../../../api/minecraft/src/main/java/gregtech/api)。共享源码由 `:api-platform:<node>` 经 Stonecutter 分别编译为 Forge / NeoForge API；编译路径不包含 core 或模组 main 输出。

| 包 | 应当承担 | 边界 |
| --- | --- | --- |
| `api.addon.platform` | 附属使用的物品、流体、能源、加载器、平台服务桥接 | `StructureAccess.cells()` 提供成功匹配快照，`MachineBehavior` 提供结构状态回调；允许游戏类型，由宿主绑定 |
| `api.machine` | 游戏中的机器控制接口 | 当前有 `MachineControl`，与 core 同包但不同源码层 |
| `api.transport` | 游戏世界中管道的连接契约 | `FluidPipeConnectable`、`ItemPipeConnectable`；避免公开整个内部网络 |

## 宿主接入

先确认外部消费者需要什么，再定义最小契约；由 engine 提供执行，bootstrap/platform 绑定，content 消费。

宿主入口是 `ControllerDeclaration.multiblock(namespace, path, factory, binding)`。`ControllerBinding` 明确给出主控实体类、方块实体注册 ID 和结构工厂；配方机另外绑定单元布局供应器。声明在 `MultiblockRegistration` 注册后，共同驱动运行时识别、JEI/EMI 和建筑之杖；独立主控的方块实体适用方块列表也从声明生成。启动时扫描实际方块注册表，校验漏登记、主控类/实体类型不匹配和空结构。

多方块声明、端口和诊断的消费契约见[附属 API](../../ADDON_API.zh-CN.md)，内部执行见[engine 多方块运行时](engine.md#多方块运行时)。

## 验收

- 公共契约只定义可消费的协议，不放注册执行、世界访问、客户端渲染或内部缓存。
- `api/common` 使用 Java 17，运行时仅依赖 JDK；`api/minecraft` 含游戏类型，由 `:api-platform:<node>` 分别编译，不依赖 core 或模组 main。
- 修改公开签名、默认方法、异常或生命周期声明时，说明二进制、源码与数据兼容影响；`public` 不自动意味着长期兼容。
- 扩展输入在进入运行时前验证身份、范围、冲突与引用。
- 修改后运行 JUnit 与 `apiContracts`；修改 `api/minecraft` 的共享游戏侧 API 时编译两个平台。`api/common` 运行时仅依赖 JDK，不需要平台编译。
- 格式检查的覆盖缺口见 [format](../../development/validation/checks/format.md)。

## 自动化测试范围

- JUnit
  - `api/common/src/test`：公共契约、能源、机器、材料、多方块与附属声明；包含显式结构绑定、数量与同类方块约束的失败校验。
  - `api/minecraft/src/test`：平台 API 契约与附属平台服务桥接。
  - `apiContracts` 用打包后的 API JAR 运行外部消费者，检查签名闭包、能源契约与宿主未绑定时的失败行为；它不属于 JUnit，但属于公共 API 的自动检查。
- GameTest
  - 公共契约层不使用 GameTest；平台运行期由 `MultiblockDeclarationTests` 检查原版主控声明、实体工厂与预览一致性，由 `MultiblockBehaviorApiTests` 检查仅调用公共 API 的附属注册、数量约束、同类方块、重复层及结构回调，`MultiblockPortApiTests` 检查真实能力调用、四向旋转、槽位/资源过滤、模拟守恒和失效后的缓存视图。
