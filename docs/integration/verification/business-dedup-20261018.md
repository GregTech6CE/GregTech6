# 2026-10-18 / 正式身份清理与双版业务去重

要求删除重复旧 ID，不保留别名／重映射，并在 Stonecutter 分支实际合并业务实现。当前分支已由 `experiment/stonecutter-emi` 改名为 `experiment/stonecutter`；本批不推送、不合并 main。

## 正式身份

清理本批审计的 66 个旧身份：61 个原别名及 5 个 `battery_eu_*` 测试电池。删除运行时别名表、注册别名、MissingMappings 订阅、旧电池物品迁移与实体替换入口；配方、世界生成、掉落、结构、创造目录和查看器直接使用正式身份。旧 ID 的物品模型／方块状态及双语物品名称声明也已删除。

主要对应关系包括十种 `key_*` → `<material>_key`、`diggable_clay` → `clay_brown`、`diggable_peat` → `turf`、测试储罐 → 对应原版阀门、旧锅炉／坩埚／涡轮／发电机主控 → 原版按材料区分的主控、旧壁板 → 对应正式部件、旧电池 → 正式化学电池。正式物品的不同材料、厚度和等级不因此合并。

这次选择明确撤销旧 ID 存档读取兼容。历史文档中“保留旧 ID 为别名”的结论仅描述旧版本。审计／测试中用于确认旧 ID 不存在的字符串不是运行时兼容入口；`mass_storage`、`bumble_hive` 等仍有效的方块实体类型 ID、分类标识、原始贴图名和原版配方符号也不属于重复物品 ID。

## 业务实现

| 领域 | 本批共用实现 |
| --- | --- |
| 原版多方块主控与结构部件 | `GTMultiblocks`、`MultiblockBlockFactory`；`LargeMachineParts` 查询同一个原版编号目录 |
| 涡轮与发电机 | `AxialGeneratorDefinitions`、`GasTurbineDefinitions` 引用上述正式注册，不再重复建造相同方块 |
| 远距离线路 | `GTLongDistance` 共用规格、方块和物品工厂；Forge 旧类只持有正式对象引用 |
| 电动工具 | `GTElectricItems` 使用同一份工具目录和物品工厂 |
| 建筑与装饰 | `GTConstructionBlocks`、`GTSpikes`、`GTBars`、`GTSurfaceBlocks`、`GTMaterialPiles`、`GTCoinPiles`、`GTBookShelves`、`GTSandwich`、`GTBumbleBlocks`、`SharedDecorBlocks` |
| 手工配方 | `OriginalHandCraftingRows` 统一齿轮箱、变压器、旋转引擎／泵、线管、砧、栏杆、电路、电池单元、储物和轴的配方生成；Forge reload 与 NeoForge pack 各保留加载适配 |
| 注册句柄 | `RegisteredBlock`／`RegisteredItem` 将共用工厂与加载器句柄类型分开，不注册第二个 ID |

已无实际注册入口的测试大锅炉方块、实体和 `LegacyBoilerProcessing` 删除，正式五种原版大锅炉继续使用 `OriginalLargeBoilerControllerBlock` 系列。

清理同时修正 NeoForge 两处地牢生成的旧储物箱查询、地热泉绿色指示草引用，以及耐火砖属性查询的反向旧名映射。绿色指示草使用现有 `grassblock_normal`，不会将草垛当作指示地表。建筑工厂合并采用共同的玻璃属性，荧光玻璃两版硬度／抗爆统一为 0.75／5；这属于历史参数差异收敛，并非逐字节迁移。

原生 main 当前共用 990 类，Forge 专有 65 类，NeoForge 专有 129 类；core 473 类。迁移前对应为 978／69／140／476。类数量仅描述源码位置；本批还消除了共同文件内按版本重复的注册循环和配方实现。生成目录内每版一份源码是正常编译输出。

不同名／不同包的其它版本专有实现仍应按业务逐项审查，本记录不声称全仓库已无任何重复。冻结的 `migration-manifest.json` 继续保存迁移历史，不为本批业务变更重写；当前树使用 `verifyStonecutterSources` 核验。

## 验证

构建及检查的最终结果见 [JSON 回执](business-dedup-20261018.json)。模型引用检查覆盖 Forge 56,479、NeoForge 55,634 份模型／方块状态声明，无本地模型引用缺失；资源配方／标签／分组无本批 66 个旧身份引用。451 个 REMI 分组重新生成并核对一致。更新后的多方块配方生成器产出与保留的 68 条实际配方逐条一致。

仅做本批所需的核心合同、编译、源策略／双语及资源检查；没有重新运行完整测试、启动游戏、构建交付 JAR 或验证旧世界。本批的编译不能代替渲染和生存验收。化学电池 GameTest 与客户端探针代码编译通过也不表示已执行。

本机 Gradle 8.8 分发包缺失，已从官方分发地址取得并核对官方 SHA-256。原 EMI Maven 超时，采用临时本地 Maven 镜像，使用 Modrinth 上相同固定版本的官方发布包并校验发布元数据 SHA-512；其它 JEI／Jade 依赖从本机缓存取回并核对 SHA-1。临时镜像中 EMI 的 `api` classifier 使用包含同版 API 的完整官方模组 JAR，不是单独下载的 API-only 包。未修改生产依赖版本或仓库配置，相关文件均位于忽略的 `work/business-dedup-20261018/`。

## 能量代码入口

能量类型在 `core/.../data/GregTechTags.java` 的 `Energy` 中，由 `energyTag()` 加入内部标签表；机器接口和传输在共用原生 `api/energy/IEnergyBlock.java`、`EnergyTransfer.java`。`EU = ELECTRICITY` 等是原版 API 对同一标签对象的简写，不是本次删除的重复物品注册。
