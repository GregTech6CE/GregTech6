# 方块占位核对（2026-09-23，源码静态审计）

## 第三十七批后续状态

原版基岩钻机的 `oreBroken` 宿主产物已补为带破碎状态的可放置矿块，按宿主岩石显示、下落，并接入 2U 矿物处理；下落失败后的掉物也保留破碎状态与宿主。蓝云杉台阶的 `sawaxe` 已扩展至锯、斧、双刃斧和万能铲。硬币模具图案已传入世界币堆，激光计量传感器已有实际光纤 LU 读数；草垛、书架落灰书和打印机／复制机 U 盘保留行为也已接线。详情见 `REPAIR_BATCH37_2026-09-23.md`；最终新世界广域门禁 `build/repair-batch39-full.log` **914/914 项通过**，退出码 0。

本审计中列出的 53 个数值主控空壳已经按家族接线，但这只消除了“放下后只有端口”的问题，不能推出所有原版机制、贴图和生存路线完整。当前明确未完成的有：USB/HDD 16 槽交换机及邻接线网、硬币背包图标随自定义图案变化、约 300 种木材书架、Thaumcraft Flux 依赖缺席时的污染对应物，以及客户端实际视觉验收。确认 GT6 地牢中原本的 Minecraft 工作台和铁砧继续保留。

## 至第三十六批的后续修复状态

下文保留的是修复**之前**的审计快照，不代表当前代码状态。原来列出的 53 个 GT6 数值 ID 主控，现已按家族接入实际方块和方块实体：25 个储罐主阀、5 个大锅炉、12 个涡轮/发电机、8 个大坩埚、Von da Graagg、避雷针和基岩钻机。10 台原版动力磁体也另按其 GT6 数值等级注册，不再把装饰性 `magnet_*` 错认作机器。覆盖率生成表现在记录 117 项原版多方块注册：45 个被动部件、72 个主控、0 个被动主控外壳、0 个未注册 ID。这个数字只证明原版数值主控不再是纯端口，**不等于玩法已完全对齐**。

本轮还补了结构预览、六向涡轮、原版锅炉等级、储罐危险流体、生产配方、挖掘工具标签和原版中文名称。随后核对又发现并修复三类真正会影响游玩的图标壳：16 种 GT6 `BlockVanillaOresA` 原矿、六色 `BlockGrass`、物品／流体长距离管；12 种水晶矿、9 种岩层矿、三种黑砂、17 种矿床指示花、树木和流体泉也补上相应材料、采掘或加工行为。地牢房间唯一标记、钥匙和硬币材质现按整座地牢共享；GT6 原本使用的原版工作台、铁砧按确认保留。另已移除 66 种误注册的轨道图样假方块，保留 30 种真实轨道并补齐其物品模型。不能把纯贴图资源或结构墙误判为主动机器。专项新世界门禁曾有 214/214 通过；最终 `build/repair-batch36-full.log` **904/904 项广域 GameTest 全部通过**，详见 `docs/REPAIR_TRACKER_2026-09-20.md`。已知差异包括：原版基岩钻机的 `oreBroken` 宿主产物尚无对应方块、Thaumcraft Flux 方块不存在时储罐魔法污染暂以清空方块处理、原版涡轮转子与多方块外观仍需 `runClient` 目测。

## 修复前审计快照

本清单只把**原版具有主动玩法、移植版对应注册物却只能放置/转发的方块**列为确定缺口。它不是 12,099 个注册方块的逐块运行验收，也不能用来推断已经完成客户端视觉测试。本轮只读 Java/资源与 GT6 原版源码；未修改 Java，未运行 Gradle。

## 结论与容易误判的测试

`LargeMachineParts.DEFINITIONS` 共 89 项，其中 **53 项是 GT6 的多方块主控**，当前全经 `LargeMachineParts.register()` 注册为 `MultiblockPortBlock`，放下后只创建 `MultiblockPortBlockEntity`。该实体只有在被另一个主控绑定后才转发物品、流体、能源；它本身不检查结构、处理配方、存储罐内容或输出能源。另 36 项为真正的墙、线圈、加工部件、CPU/通风件，继续作被动结构件是合理的。

`data/generated/GT6MultiblockIds.java` 把上述 53 项标成 `Kind.PART`；`gametest/MultiblockCoverageTests.everyGt6MultiblockRegistrationHasAPortCounterpart` 只检查 `LargeMachineParts.find(id)` 与注册存在，**不能证明主控功能存在**。生成器 `tools/extract_gt6_multiblock_ids.py` 以 `LargeMachineParts.find` 中有没有数字 ID 判定 PART，也因此固化了这一误分类。此表的 `MISSING=0` 不等于原版内容已经可玩。

依据：移植版 `content/multiblock/LargeMachineParts.java:12-106`、`block/machine/MultiblockPortBlock.java`、`blockentity/machine/MultiblockPortBlockEntity.java`；原版 `gregtech/loaders/b/Loader_MultiTileEntities.java:1195-1283`。

## 确定只有端口外壳的 53 个 GT6 主控

下列每项均为移植版 `gregtech:<id>`，当前实现类均为 `MultiblockPortBlock`，对应 BE 均为 `MultiblockPortBlockEntity`。箭头左边为原版 GT6 数值 ID，右边为移植版注册 ID；原版类与丢失行为按家族说明。

### 储罐主阀：25 项

原版 17001 是 `MultiTileEntityTank3x3x3Wood`，17002–17027 是 `MultiTileEntityTank3x3x3Metal`，17042–17067 是 `MultiTileEntityTank5x5x5Metal`。原版按材质/稠密度决定专属墙体、容量、耐气/酸/等离子/魔法、温度熔毁；主阀可接漏斗/龙头，并按方向自动输出。现有端口外壳不具备这些能力。

- 17001 → `wood_tank_main_valve`
- 17002 → `small_stainless_steel_tank_main_valve`; 17003 → `small_tungstensteel_tank_main_valve`; 17004 → `small_tungsten_tank_main_valve`; 17005 → `small_adamantium_tank_main_valve`; 17006 → `small_titanium_tank_main_valve`; 17007 → `small_invar_tank_main_valve`
- 17022 → `small_dense_stainless_steel_tank_main_valve`; 17023 → `small_dense_tungstensteel_tank_main_valve`; 17024 → `small_dense_tungsten_tank_main_valve`; 17025 → `small_dense_adamantium_tank_main_valve`; 17026 → `small_dense_titanium_tank_main_valve`; 17027 → `small_dense_invar_tank_main_valve`
- 17042 → `large_stainless_steel_tank_main_valve`; 17043 → `large_tungstensteel_tank_main_valve`; 17044 → `large_tungsten_tank_main_valve`; 17045 → `large_adamantium_tank_main_valve`; 17046 → `large_titanium_tank_main_valve`; 17047 → `large_invar_tank_main_valve`
- 17062 → `large_dense_stainless_steel_tank_main_valve`; 17063 → `large_dense_tungstensteel_tank_main_valve`; 17064 → `large_dense_tungsten_tank_main_valve`; 17065 → `large_dense_adamantium_tank_main_valve`; 17066 → `large_dense_titanium_tank_main_valve`; 17067 → `large_dense_invar_tank_main_valve`

移植版另有实际 `tank_3x3`、`tank_5x5`（`TankControllerBlock` + `MultiblockTankControllerBlockEntity`），但不是这 25 个物品的类，容量分别固定 320,000/1,024,000 mB，结构仅接受同一个 `TANK_WALL` / `TANK_WALL_DENSE`；这不能等价覆盖原版每个主阀。原版依据 `MultiTileEntityTank.java`、`MultiTileEntityTank3x3x3.java` 与注册行 1195–1222。

### 大锅炉：5 项

原版类均为 `MultiTileEntityLargeBoiler`，不同材质对应墙体和蒸汽输出档；当前 5 项本身无燃烧、水处理、蒸汽生产、故障处理。

- 17201 → `stainless_steel_boiler_main_barometer`; 17202 → `titanium_boiler_main_barometer`; 17203 → `tungstensteel_boiler_main_barometer`; 17204 → `adamantium_boiler_main_barometer`; 17205 → `invar_boiler_main_barometer`

移植版另有 `large_boiler_main` 实际控制器，但没有使这 5 个原版主机物品作为相应等级控制器工作。原版注册行 1248–1252。

### 大型蒸汽涡轮、发电机、燃气涡轮：各 4 项，共 12 项

原版依次是 `MultiTileEntityLargeTurbineSteam`（蒸汽→RU）、`MultiTileEntityLargeDynamo`（RU→EU）、`MultiTileEntityLargeTurbineGas`（燃气热→RU）。当前这些原版命名物品没有储存、能源输入输出、结构/转子或燃料逻辑。移植版另有 `_main` 及分级控制器，不能因为它们存在就把这些同名物品算作可用。

- 17211 → `magnalium_steam_turbine_main_housing`; 17212 → `trinitanium_steam_turbine_main_housing`; 17213 → `graphene_steam_turbine_main_housing`; 17214 → `vibramantium_steam_turbine_main_housing`
- 17221 → `stainless_steel_dynamo_main_housing`; 17222 → `titanium_dynamo_main_housing`; 17223 → `tungstensteel_dynamo_main_housing`; 17224 → `adamantium_dynamo_main_housing`
- 17231 → `magnalium_gas_turbine_main_housing`; 17232 → `trinitanium_gas_turbine_main_housing`; 17233 → `graphene_gas_turbine_main_housing`; 17234 → `vibramantium_gas_turbine_main_housing`

原版注册行 1254–1267；移植版另有 `AxialGeneratorDefinitions` / `GasTurbineDefinitions`。

### 大型坩埚：8 项

原版类均为 `MultiTileEntityCrucible`，按材料设定相应墙体和耐酸等，结构不同高度的侧壁分别承担 HU 输入、坩埚操作、物品/流体接口。当前 8 个原版物品只是转发端口。另有 `large_crucible_main`，但 `LargeCrucibleControllerBlockEntity.serverTick` 在通过结构检查后没有加工动作；这是真控制器**仍缺核心处理逻辑**的独立问题。

- 17302 → `large_stainless_steel_crucible`; 17303 → `large_tungstensteel_crucible`; 17304 → `large_tungsten_crucible`; 17305 → `large_adamantium_crucible`
- 17306 → `large_titanium_crucible`; 17307 → `large_invar_crucible`; 17309 → `large_steel_crucible`; 17312 → `large_tantalum_hafnium_carbide_crucible`

原版 `MultiTileEntityCrucible.java:119-127` 与注册行 1270–1277；移植版 `LargeCrucibleControllerBlockEntity.java:50-63`。

### 独立高科技主控：3 项

- 17996 → `von_da_graagg_generator`：原版 `MultiTileEntityVonDaGraagg` 是耗 EU 的 5×5 基座+高杆机器，依据可用能量在最高 256 格方形半径内抑制普通怪物生成，对苔石留例外；当前无结构、能量消费、刷怪事件接线。
- 17998 → `lightning_rod_electric_output`：原版 `MultiTileEntityLightningRod` 是避雷针主控/蓄能/发电输出；移植版另有 `lightning_rod_main`，但此原版命名物品自身只作端口。
- 17999 → `bedrock_mining_drill_controller`：原版 `MultiTileEntityBedrockDrill` 是基岩钻机主控；移植版另有 `bedrock_drill_main`，但此原版命名物品自身只作端口。

原版 `MultiTileEntityVonDaGraagg.java`、`MultiTileEntityLightningRod.java`、`MultiTileEntityBedrockDrill.java` 与注册行 1280–1283。

## 另一确定的功能空壳：磁体注册族

`GTMagnets.java:24-46` 注册的 10 个 `magnet_iron`、`magnet_steel`、`magnet_neodymium`、`magnet_cobalt`、`magnet_alnico`、`magnet_ferrite`、`magnet_electromagnetic_steel`、`magnet_electromagnetic_aluminium`、`magnet_electromagnetic_galvanized`、`magnet_tungsten_steel` 都是 `MagnetBlock`。该类仅有材质染色与提示，没有 BE、EU/RF/MU 接口、每 tick 转换或模式状态；源码中这些 ID 除注册与语言/贴图外没有玩法引用。

**不能按名称把这 10 个方块一一对应到 GT6 原版 10 台动力磁体。** 原版 10031–10035 是 `MultiTileEntityMagnetElectric`（EU→MU），11031–11035 是 `MultiTileEntityMagnetFlux`（RF→MU），两者继承能量转换基类且有各档输入输出、朝向、启停/活动贴图。移植版 `EnergyNodeSpec.Kind` 仅 CONVERTER/TURBINE/SOLAR/STORAGE，未见磁体设备定义。应先确定这些 `magnet_*` 是否为移植版自定义的磁性装饰块；原版 10 台设备要另建明确 ID/规格和行为，不宜错误地给 `magnet_iron` 填上某一档机器功能。原版依据注册行 863–876、`MultiTileEntityMagnetElectric.java`、`MultiTileEntityMagnetFlux.java`。

## 排除项与下一批建议

- `LargeMachineParts` 剩余 36 项 180xx/181xx/182xx（墙、线圈、传动/加工部件、CPU、通风）确属 GT6 `MultiTileEntityMultiBlockPart`；作为端口/被动零件并非上面所说的主控空壳。`GTMultiblocks.part()` 中的部分普通 `Block` 是相同的被动结构类别，需要按具体原版端口权限再核，不因类名简单宣判为占位。
- `GTIconSetBlocks` 大量图标块、`GTDecorBlocks` 的面板/条栅/色玻璃、`GTTrackBlocks` 的轨道、长距离管束等有装饰或其他方块扫描用途，不能仅因不持有 BE 就算占位。`GTLasers.logistics_core` 现已是 `LogisticsCoreControllerBlock`，本清单不重复把它列为占位；CPU 显示覆盖板、硬币建模由别的工作负责。
- 优先级 1：让**原版主阀注册物本身**具备按规格配置的罐主控功能，复用/扩展现有 tank 控制器，逐项验证 25 个 ID 的结构墙、容量、流体耐性、漏斗/龙头、自动输出与存档恢复；并修改 GT6 覆盖率表，把这 25 项按功能控制器验证。这个家族适合一次实现大量有用内容。
- 优先级 2：独立实现 `von_da_graagg_generator`，因为它目前完全没有相邻同功能 `_main` 机器；结构、EU 消耗、范围、刷怪事件、苔石例外可成一组明确的 GameTest/事件测试。
- 后续按家族把锅炉/涡轮/发电机/大坩埚的原版命名 ID 接到各自规格控制器，避免同一 GT6 机器出现“能玩的 `_main` + 只有外壳的主机物品”；大坩埚还须实现实际熔炼/铸造流程。磁体设备应单独补齐。
