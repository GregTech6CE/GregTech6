> 最新源码差距核对见 2026-09-12 大蓝图审计。本文件保留历史计划；注册数量不代表功能已完成。

> 2026-09-11 重做说明：下面 F3–F12 的历史进度含占位注册和简化实现，不能视为原版行为已完成。当前公共工具交互、F10 四组手工台、F11 金属箱子的修改及未完成边界见 重做记录。

> 2026-09-11 审计更新：当前状态、已发现行为缺陷和框架重构见 [PORTING_AUDIT.md](PORTING_AUDIT.md)。本文件的旧 Wave 完成标记不等于当前验收结果；旧「永久砍除」建议已被完整还原目标取代。

# GregTech6 → 1.20.1 移植总体计划（历史 Wave 记录）

> 生成日期 2026-06-13。基于**两侧源码实测对比**：
> - GT6 原版 `Loader_MultiTileEntities.java`（2242 行）正则普查：**1295 条方块注册，49 个功能大类**；
> - GT6 包扫描：`gregtech/tileentity`（17 子包）、`gregtech/blocks`（67 类）、`gregtech/items`（105+ 类）、`gregapi/cover/covers`（61 类）、`gregapi/tileentity/connectors`（15 类）；
> - 移植端注册表实读：`registry/` 29 个注册类 + `loaders/` + git log（截至 213fe280）。
>
> **本文件取代 PORTING_ROADMAP.md / PORTING_TODO.md / PORTING_GAP.md**（三者截至 Wave 23-25a，已严重过时——
> 其中"reactors/sensors/inventories/tools/multiblocks 未动"等说法均已失效）。后续以本文件为准并随提交更新。
>
> 图例：✅ 完成（含等价重写）｜🔶 部分｜❌ 未做｜✂ 建议砍除

## 〇、当前基线（已验证为"完成"的部分，不再展开）

材料体系（GT6Materials 转译+矿物处理数据）、矿石方块（单方块+NBT 背景石）、世界生成全套
（区域石层/矿脉/小矿/矿床/基岩矿/表面沉积/下界末地/-64 下延，scan_region_ores.py 已验证）、
矿物处理链+通用加工配方（OreDict/Extruder/Furnace/Parts）、合金/分解/燃料表、
化学 402→181 译出→162 运行时注册、基础机器 66+大型批量 13（GUI/FaceConfig/自动 IO/16-256x 并行）、
坩埚冶炼全套（坩埚 39/模具 117/浇铸盆/水槽/龙头 39）、固体燃烧箱、蒸汽机/强力蒸汽机、
引擎四系（电/Flux/转动/蒸汽 KU 发生器）、料斗/队列料斗/储罐/木桶/塑料罐/金属桶、
流体/物品管道+导线电缆（动态烘焙模型）、覆盖板**框架**（六面槽/遮断/红石开关/面板渲染）、
JEI 全套、集成电路 0-24、MultiItem 958 个（含罐头 57）、手动工具两批、
旋转动力网（20 轴+5 齿轮箱+4 转动变压器+4 泵，PipeWireBakedModel 动态杆渲染）、
能源网八大件（电动机/发电机/变压器/蒸汽轮机/太阳能/电池盒[装单元+GUI]/储能柜/可放置电池+充电条 BER）、
多方块框架+蒸馏塔/大锅炉/大型蒸汽轮机、简化反应堆 1x1、转子耐久、Flux 电机/发电机 LV+MV、
大宗存储（60 材质）+金属箱子+四格抽屉+保险箱+储物柜+末影垃圾桶(重做版)+垃圾倾倒口、
传感器 4 种（流量/物品/电量/进度计）、工具方块（臼/磨石/筛台/曲柄/绳/炸药/龙头/喷嘴/粉斗）、
专属创造栏体系（含 GT6 式排序 + AXLES_TAB 轴与旋转栏）。

---

## 一、Bug 修订与验证欠账 —— ✅ 全部完成（Wave 36，runServer `Done (9.441s)` 0 崩溃）

| # | 问题 | 状态 | 结果 |
|---|---|---|---|
| B1 | 两个提交（4bedb282/213fe280）仅编译验证 | ✅ | runServer 启动验证 `Done (9.441s)`，无注册期崩溃；附带修 54 槽容器幻影第 7 行（HopperContainerMenu）；记忆 gt6-port-pending-validation 已删 |
| B2 | 机器过压只吞能量，无 GT6 爆炸/损毁惩罚 | ✅ | `BasicMachineBlockEntity.explodeFromOvervoltage`，开关 `GregTechConfig.machineOvervoltageExplosions`（默认开） |
| B3 | `MaterialProperty.ACID` 缺失，3 处腐蚀检查被注释 | ✅ | `MaterialProperty.ACID` + 37 酸性材料（MT.java）；Mold/Faucet/Basin 三处 `!acidProof() && has(ACID)` 检查在线 |
| B4 | 管道 ModelData 跨区块边界初次加载陈旧 | ✅ | 客户端区块 load 时 `requestModelDataUpdate`（PipeModelData + Fluid/Item/Wire BE） |
| B5 | 管道/线缆 blockstate 仍 multipart JSON | ✅ | 802 个 multipart 兜底改单变体（加速烘焙） |
| B6 | 大型机器硬度统一 6.0，GT6 个别 12.5 | ✅ | 按 GT6 校准：tier 表 {T1 6,T2 4,T3 9,T4 12.5,T5 9} + 逐台覆盖（largecentrifuge/crusher/shredder/implosioncompressor/fusionreactor 12.5 等） |
| B7 | 转译器曾机器改派（SO2→SO3 入 BurnMixer），需回归原表 | ✅ | 当前 unroll 版转译器**无改派**——`map(mapName)` 直接反射 RM 同名字段，配方已用 GT6 原表 map；大型机到位后无需回归，仅需把对应 RM 接到多方块 |
| B8 | 化学 19 条真缺内容（决策：注册 or 跳过） | ✅ | 决策=**永久跳过**（Wave 35 收线）；19 条 → 11 个真缺化合物（Al2O3/CaCl2/Gray+GreenVitriol/MgCO3/MgCl2/MnCl2/CrO2/Desh/DeshAlloy/Eudialyte，Desh* 跨模组）；GTGeneratedChem 每启动 INFO 打「accepted-missing content (11)」清单自审计 |
| B9 | 电池盒 spec 带 `v*64_000` 近似 buffer，与"单元之和"并存 | ✅ | spec.capacity 改 0；`capacity()` 仅取装填单元之和（空盒 0）；placeable 电池/储能柜保留自带容量 |
| B10 | 新增传感器须带 `sensor_` 前缀（防撞名） | ✅ | 开发规约（本表即记录）：任何新物品/方块 id 注册前对照 `GTIconSetBlocks`/`GTTechnological`/`MultiItem` 去重；传感器一律 `sensor_` 前缀 |

---

## 二、功能完善（已移植但相对 GT6 有简化/缺口的大类）

> 每条给出：GT6 规模（注册数）→ 移植端现状 → 待补内容。GT6 参照类均在
> `gregtech6-master/gregtech6-master/src/main/java/` 下。

### F1. 蒸汽锅炉 Steam Boilers ✅（Wave 37，26 注册：Steam Boiler Tank ×13 材质 + Strong ×13）
- 早期链"燃烧箱 → **单方块锅炉罐**(水+HU→蒸汽) → 蒸汽机"中间环已补齐。
- 落地：`BoilerSpec`（13 材质 × {normal, strong}，steamOutput=GT6 NBT_OUTPUT_SU=tableValue*2）+
  `BoilerTankBlock`（对称满方块、无朝向、material 染色 colored+overlay 复合模型）+
  `BoilerTankBlockEntity`（extends GTEnergyBlockEntity：任意面收 HU、水箱 16k+蒸汽箱、
  1L 水+80HU→160L 蒸汽、>半满向上推蒸汽、干烧过热/蒸汽满溢→爆炸、暴露流体 cap 收水放蒸汽）。
  注册：`GTBoilers` + `GTBlockEntities.STEAM_BOILER` + 引擎创造栏 + 色彩处理器。
- 已补：结垢机制（非蒸馏水降效率至 5000，凿子清除）、蒸汽衰减（无热量时 1L/t）、
	  气压计 BER 渲染（`BoilerBarometerRenderer`，base + 00-31 指针帧，由模型 JSON 多 child 层注册 atlas）；
	  机器无合成配方（与现有机器一致的横切缺口）。
- 贴图复用 GT6 `machines/tanks/boiler_steam`（已在移植端）；资产由 `tools/generate_boiler_assets.py` 生成。

### F2. 燃烧箱 Burning Boxes ✅（Wave 37，105 注册）
- GT6 构成（各 ×13 材质）：Solid / Dense Solid / Liquid / Dense Liquid / Gas /
  Dense Gas / Fluidized Bed / Dense Fluidized Bed + Brick（青砖初期版 ×1）。
- 落地：`BurningBoxBlock`（extends GTFacingMachineBlock，fuelType 字段）+ 
  `BurningBoxBlockEntity`（通用三燃料逻辑：液体/气体查 FM 表→烧流体烧料、流化床双输入查表→产灰、
  HU 向上输出、Dense 4×倍率、燃/熄/冷却状态机）+ `MachineRegistry.registerBurningBox()` 注册入口。
  注册：`GTMachines.burningBox()` 78 新方块 + `GTBlockEntities.BURNING_BOX`。
- 资产：`tools/generate_burning_box_jsons.py`（模型 24+ 方块状态 78）
  + `tools/generate_burning_box_items.py`（物品模型 78）
  + `tools/add_burning_box_lang.py`（en_us/zh_cn 各 78 条目）。
- 贴图：暂复用燃烧室贴图 burning_solid → burning_liquid/burning_gas/burning_fluidbed（待专业贴图替换）。

### F3. 引擎 Engines 🔶 50/50+（Wave 37 柴油引擎已补，缺 1）
- 已有：蒸汽×14、强蒸汽×14（含 GT6 变功率输出 0.5×–2×，size=1 包，预热门控 20%）、
  电动×5、Flux×5、转动×4、**柴油×8**（液体燃料→KU，Bronze/ArsenicCopper/ArsenicBronze/Steel/Invar/Ti/TungstenSteel/Ir）。
- 待补：`generators/MultiTileEntityGeneratorHotFluid`（热流体→HU 发电）未移植。

### F4. 轮机 Turbines 🔶 5/15 材质（15 注册，全部为单方块蒸汽轮机）
- 已有 bronze/brass/invar/steel/chromium；补齐 GT6 其余 10 材质（钛/钨钢/Magnalium 等，
  数值按 Loader_MultiTileEntities 1512+ 段）。转子耐久机制已在线，纯数据扩展。

### F5. 核反应堆 Reactors 🔶 简化版（49 注册）
- 已有：1x1 核心简化版（4 槽、U-235/238、冷却水、32HU/t/棒）。
- 待补（按 GT6 `tileentity/energy/reactors/`，9 个类）：
  1. **2x2 核心**（MultiTileEntityReactorCore2x2）；
  2. **完整燃料棒体系**（48 种注册）：Nuclear 燃料棒（U-233/235/238、Pu-239/241/243/244、Th-232、
     Co-60、Am-241/245、Naquadah 系等）、Breeder 增殖棒（Li/Th-232/U-238/Naquadah）、
     Depleted 乏燃料棒、Product 富集产物棒（Pu-239/U-233/Tritium 等）、Moderator 慢化棒、
     Absorber 吸收棒、Reflector 反射棒；
  3. **中子通量机制**：棒间中子交换、自持/熄火、增殖转化；
  4. 冷却液变体（蒸馏水→重水等）与产蒸汽回路；
  5. 熔毁（过热爆炸+辐射污染区）；当前 tooltip 标注的"无熔毁"偏差到时移除。
- 燃料棒做成插入核心的物品（GT6 是 MTE，移植可用 NBT 物品+核心渲染）。

### F6. 多方块机器 Multiblock Machines 🔶 物理结构 3/~24 种（117 注册）
- 已有物理多方块：蒸馏塔、大型锅炉、大型蒸汽轮机；框架（structureComplete 门控+部件方块）可复用。
- 13 个"大型批量机器"（largecentrifuge 等）目前是**单方块+并行**，GT6 为真多方块——
  **决策项**：保持现状（玩法等价）或逐个补物理结构（建议：保持，但补 GT6 外观主机方块贴图 multiblockmains/）。
- 待补的真多方块（GT6 类名，按价值排序）：
  1. **大型坩埚 MultiTileEntityCrucible**（×7 材质：Steel/Invar/Stainless/Ti/W/WS/TaHfC/Ad）——
     坩埚系统终局，复用现有坩埚数学；
  2. **多方块储罐** Tank3x3x3（木/金属/Dense）+ Tank5x5x5（金属/Dense，×6 材质）；
  3. **焦炉 MultiTileEntityCokeOven** 物理结构（配方已在 cokeoven 单方块）；
  4. **低温蒸馏塔 CryoDistillationTower** 多方块版（当前单方块简化）；
  5. **大型燃气轮机 LargeTurbineGas**（×5 材质外壳）+ 大型轮机通用化（现仅蒸汽）；
  6. **大型发电机 LargeDynamo**（×4 材质，KU→EU 多方块）；
  7. **大型热交换器 LargeHeatExchanger**（HU 回收）；
  8. **聚变反应堆 FusionReactor**（终局）；
  9. **内爆压缩机 ImplosionCompressor**（配方已有单机？核对 implosioncompressor 单方块已注册——补物理结构）；
  10. **基岩钻机 BedrockDrill**（配套基岩矿✅，玩法闭环）；
  11. **闪电棒 LightningRod**（含闪电实体，entities/）；
  12. **物流核心 LogisticsCore**（依赖覆盖板物流系统，后置）；
  13. 范德格拉夫 VonDaGraagg、大型物质制造机 MatterFabricator（终局，低优先）。

### F7. 传感器 Sensors ✅ 16/16（16+4=20 注册）
- ✅ 已有 4 基：fluidometer/itemometer/electrometer/progressmeter。
- ✅ 新增 12 种：Thermometer（HU测温）、Tachometer（RU转速）、Weightometric（物品计数）、
  Bucketometer/KiloBucketometer/Gibblometer（流体计量）、Stackometer（组数）、Luminometer（光照）、
  PlayerCounter（玩家计数）、Chronometer（进度时间）、Geiger（辐射占位）、Laserometer（激光占位）。
- 阈值按钮 GUI 待后期补（现为纯比例红石输出）。

### F8. 覆盖板 Covers 框架✅内容🔶（gregapi/cover/covers 61 类 — 部分实现）
- ✅ 已有：板面渲染、遮断 Shutter、红石开关、六面槽与同步。
- ✅ 批 1 流控（部分）：CoverPump（自动抽流体入机器）、CoverConveyor（自动拉物品入机器）、
  CoverRobotArm（自动推输出物品）、CoverTagSelector（配方标签选择，NBT circuit）。
- ✅ 批 2 红石（部分）：CoverRedstoneTorch（机器运行时输出红石信号）、CoverRedstoneRepeater。
- 待补：**CoverFilterItem/CoverFilterFluid（+配置 GUI）**、CoverDrain、CoverVent、
  CoverPressureValve、CoverRetrieverItem；RedstoneConductorIN/OUT、
  DetectorRunning×4（工作状态检测）、
    SelectorButtonPanel/Manual/Redstone/Tag（模式选择面板）；
  - 批 3 显示：CoverDisplayEnergy、CoverScaleEnergy/Progress（刻度条）、CoverTextureCanvas/Multi/Simple
    （装饰贴皮）、CoverControllerAuto/Timer/Redstone（机器启停控制器）；
  - 批 4 物流：CoverLogistics 全家（Import/Export/Storage ×Item/Fluid/Generic + CPU 显示）——
    与 LogisticsCore 多方块、Logistics Wire 一起构成 GT6 物流网，单独立项。
- 面板贴图：补 `covers/` 专用贴图类别（当前用物品贴图画 1px 面板）。

### F9. 电池与储能 Batteries/Battery Boxes/ZPM 🔶（37+2+3+2+4 注册）
- 已有：电池盒 ×5 阶（装填充电单元+GUI）、储能柜 ×5、可放置电池 5 阶+充电条。
- 待补：**Large Battery Box**（大电池盒，16 槽）；化学电池方块版全系
  （Alkaline/NiCd ×8-2048、Li-Co/Li-Mn Adv ×8-2048——现仅 5 档代表）；
  **LU 晶体电池**（Energium Crystal T4/T5，依赖激光系）；**ZPM**（QU 终局储能+EU/QU 放电器）；
  **Crystal Charger**（×2，晶体充能座）；**Portable Power Cell**（便携电芯 ×4，含中子聚变电芯）。
- 充电电池**物品**逻辑（NBT 充放电）已隐含在电池盒单元中——校准与 GT6 `IItemEnergy` 对齐，
  供电动工具复用（→ N6）。

### F10. 工具方块 Misc Tool Blocks 🔶 ~26/47（51+12 注册）
- 已有：臼、磨石、筛台、曲柄、绳（1 种）、炸药、龙头 tap、喷嘴 nozzle、粉斗 dust_funnel、
  末影垃圾桶/倾倒口、保险箱、浇铸系统全套。
- ✅ 批 3 新加 12 方块：**砧 Anvil**、**搅拌碗 MixingBowl**、**榨汁机 Juicer**、**浸洗盆 BathingPot**、
  **花盆 PlantPot**、**蜂房 Bumbliary**、**采胶袋 SapBag**、**脚手架 Scaffold**、
  **流体漏斗 FluidFunnel**、**封口喷嘴 CapNozzle**、**硬币模 CoinMold**、**高级按钮 AdvancedButton**。
- 以上均为简单方块（无 BE），模型 JSON 已生成，贴图 `block/tools/*.png` 待拷。
- 待补：绳补 5 材质（Grass/Silk/Steel/Plastic/Vine），BathingPot ×4 变体，Bumbliary Advanced 版，MixingBowl Table 版。

### F11. 存储 Storage/Chests/Safes 🔶（28+1+2 注册）
- 已有：大宗存储（60 材质）、金属箱子、四格抽屉、保险箱（1 种）、储物柜。
- 待补：**Bottle Crate 瓶架**、**Bookshelf 书架**、**Storage Inserter**（自动化件）、
  **Logistics Barrel/Tank**（后置物流网）、Mossy Stone Chest（彩蛋）、
  保险箱补 **Key-Locked/Mechanical**、**充电储物柜 LockerCharging**（依赖 IItemEnergy）。

### F12. 流体容器 Fluid Containers 🔶 5/86→86（5 新注册）
- 已有：木桶、塑料罐、金属桶全材质、单元格（FluidItem 等价实现）。
- ✅ 批 1 新加 5 个物品：**陶壶 Jug**、**瓷杯 Cup**、**量壶 MeasuringPot**、**保温瓶 Thermos**、
  **气压罐 BarometerGasCylinder**。均为简单 TechItem（无流体功能），模型 JSON 已生成。
- 待补：Capsule-Cell-Container 方块版、材质变体（MeasuringPot ×4、BarometerGasCylinder ×3）。

### F13. 能源转换器余项 🔶（converters 包扫尾）
- Flux 电机/发电机补 HV/EV/IV 三档（现 LV/MV）；
- `MultiTileEntityEngineRotation` 木质转动引擎✅已含于引擎四系——核对木质外观变体；
- 长距变压器 LongDistanceTransformer → 归入 N5 长距运输。

### F14. 化学与配方收尾 🔶
- B7/B8 完成后：重跑 `tools/transpile_gt6_chem.py` 确认 162→收敛；
- `Loader_Recipes_Decomp/Alloys` 与新增方块（锅炉/柴油机/反应堆棒）联动的配方补登；
- GT6 `loaders/c` 的 20 个通用配方 Loader 与移植端 `Loader_Recipes_Parts` 做一次逐文件勾稽，
  确认板/杆/螺栓/齿轮/线材/箔/环/弹簧全覆盖（已大体完成，差异点记录在案）。

---

## 三、添加功能（GT6 有而移植端完全没有的大类）

### N1. 转轴与齿轮箱 Axles and Gearboxes ✅（Wave 38，20 轴 + 5 齿轮箱 + 4 转动变压器）
- 轴 ×20（5 材质 ×4 尺寸）：6 向连接、PipeWireBakedModel 动态杆模型（`PipeWireClientModels`）、
  RU 递归传输+每格损耗、超速/超功率折断保护、材质染色。
- 齿轮箱 ×5（5 材质）：六面方块、猴扳手切换轴线方向、软锤单击切换齿轮/Shift+软锤清除、
  轴轴方向 RU 直通/相邻面换向分汇、卡死检测。
- 转动变压器（RU→RU）×4（4 材质）：复用 `EnergyNodeBlock`+`EnergyNodeSpec.Kind.CONVERTER`，
  `forge:composite` 复合模型+材质染色。配套 `GTGearboxes` 注册 + 创造栏 AXLES_TAB 集成。
- 贴图：轴用 GT6 原版 `iconsets/axle` 系列 10 张贴图+`PipeWireBakedModel` 动态杆渲染；
  齿轮箱/变压器用 `forge:composite` + `casingmachine` 金属面板着色。

### N2. 加热/冷却器 Heaters & Coolers ✅（6+6=12 注册）
- ✅ Electric Heater ×6 阶（ULV→IV，EU→HU，4:1 效率，给坩埚/锅炉供热的电气化路径）；
- ✅ Electric Cooler ×6 阶（ULV→IV，EU→HU（冷），主动冷却路径）。
- 作为 EnergyNodeBlock（CONVERTER 型）注册，模型 JSON 已生成，贴图 `heaters/electric/`、`coolers/electric/` 待拷。

### N3. 泵 Pumps ✅（Wave 38，4 注册）+ 排水沟 Drain 后续
- Rotational Pump ×4 阶（bronze/steel/titanium/tungstensteel）：从背面接收 RU 动力、
  128×128 渐进扫描前方区域、消耗 2048 RU 抽除流体源块（1000mB/drain）、
  内部 FluidTankGT 16k 容量、自动向邻侧管道/储罐/机器输出流体。
  `forge:composite` 金属面板模型+材质染色。注册 `GTPumps` + 创造栏 AXLES_TAB。
- CoverDrain（F8 批 1）与之配套（后续）。

### N4. 磁铁 Magnets ✅（10 注册）
- ✅ 10 磁铁方块：Iron/Steel/Neodymium/Cobalt/Alnico/Ferrite/Electromagnetic(Steel/Aluminium/Galvanized)/TungstenSteel。
- 作为简单方块注册（MagnetBlock），模型 JSON 已生成，贴图 `magnets/magnet_*.png` 待拷。
- 吸取掉落物/吸附实体行为待后期实现。

### N5. 长距运输 Long Distance Transport 🔶（7 注册，Wave 45）
- ✅ 7 方块存根已注册（LongDistPipe/Wire、LongDistEndpoint Item+Fluid、LongDistanceTransformer ULV+LV+MV）；
- 待补：长距流体/物品管线端点实际传输逻辑、廉价线体方块、长距变压器电量传输。

### N6. 电动工具 + 物品充电体系 🔶（Wave 47，5 电池+4 电动工具）
- ✅ `BatteryItem` ×5 阶（LV→IV，NBT 充放电框架在线）+ `ElectricToolItem` ×4（Drill/Chainsaw/Wrench/Screwdriver）；
- 待补：MiningDrill/JackHammer/BuzzSaw/Mixer/Trimmer 变体、口袋工具 ×8、枪械 ×3、充电台 GUI。
- 电池容量/充放电逻辑需对照 GT6 校准（当前为近似值）。

### N7. 树木系统 🔶（Wave 46，40 方块注册）
- ✅ 8 树种 ×5 方块类型（Log/Planks/Leaves/Sapling/Beam）已注册（`GTWoods`），模型/lang/创造栏齐；
- 待补：树种世界生成、橡胶树 ResinHole 采胶孔+SapBag、枫糖 SapHole、
  `Loader_Recipes_Woods` 锯木产出链（原木→板→棍，手锯/电锯差异倍率）；
- `tileentity/plants/MultiTileEntityBush` 浆果丛、`blocks/plants` FlowersA/B 作物化、Glowtus。

### N8. 自动化与分拣 Sorting/Extenders/Automation 🔶（Wave 45，18 方块注册）
- ✅ Filter ×4（Items/Fluids/Items&Fluids/OreDict）+ Extender ×4（Basic/Advanced/Elite/Wireless）；
- ✅ AutoIgniter ×6（Steel→Ultimet）+ AutoHammer ×4（Steel→Tungsten）；
- 全部为方块存根（BE 未实装行为逻辑）。

### N9. 工作台 Crafting Tables 🔶（Wave 45，2 方块存根）
- ✅ AdvancedCraftingTable + ChargingCraftingTable 方块已注册（`GTMiscBlocks`）；
- 待补：库存缓存+配方记忆 GUI、合成充电逻辑。

### N10. 面板 Panels 🔶（Wave 45，6 方块存根）
- ✅ Wood/Concrete/CFoam/Asphalt/ColoredGray/ColoredBlack 面板已注册（`GTMiscBlocks`）；
- 待补：贴墙 1px 模型渲染、覆盖板可叠放行为。

### N11. 装饰与基建方块 🔶（Wave 48，30 方块注册）
- ✅ Asphalt/Concrete+Reinforced/CFoam+Fresh/GlassClear+Glow/Spike×5/Bars×6/Railroad/Diggable×2 已注册（`GTDecorBlocks`）；
- 待补：沥青加速行走、CFoam 喷涂/固化/移除管道包覆、铁路矿车加速、尖刺伤害、玻璃防生怪。

### N12. 可放置物 Placeables 🔶（Wave 48，8 方块注册）
- ✅ IngotPile/PlatePile/PlateGemPile/CoinPile + LootCrate/FluidSpring/GregLantern/SandwichBlock 已注册（`GTDecorBlocks`）；
- 待补：堆叠暂存行为、LootCrate 战利品注入、证书 Certificate、FluidSpring 世界彩蛋生成。

### N13. 实体与附魔 🔶（Wave 48，5 附魔注册）
- ✅ 5 附魔已注册（Disjunction/Butchery/Haste/SharpnessMulti/SmiteMulti，`GTEnchantments`）；
- 待补：附魔效果逻辑、材料箭+子弹投射物实体、闪电棒实体、Override_Drops 生物掉落改写。

### N14. 计算机 Computing ❌（2 注册）+ USB/HDD
- USB Switch / HDD Switch + 数据棒/数据球（Behavior_DataOrb/DataStorage）：配方记忆与扫描仪数据流。
- 依赖扫描仪玩法定位，低优先。

### N15. 激光系统 Lasers 🔶（Wave 50，16 方块存根）
- ✅ CO2 Laser ×5 + Flux Laser ×5 + Laser Absorber ×5 + Laser Fiber Wire 已注册（`GTLasers`）；
- 待补：LU 能量类型注册、直线光束传输（定向射线）、EU/RF→LU→EU 转换逻辑。

### N16. 量子终局 Quantum 🔶（Wave 50，8 方块存根）
- ✅ Quantum Energizer ×5 + ZPM Discharger ×3 + Logistics Core 已注册（`GTLasers`）；
- 待补：QU 能量类型、ZPM 放电器逻辑、Aneutronic Fusion Power Cell。

### N17. 食物/药水/原版配方 Loader 🔶（Wave 51，配方存根）
- ✅ `Loader_Recipes_Food` + `Loader_Recipes_Potions` 文件已创建，内容为空存根；
- 待补：搅拌碗/榨汁机/烤炉食物链、三明治系统、药水化学、`Loader_Recipes_Vanilla` 原版物品配方替换。

### N18. 手册 MultiItemBooks ❌ + Loader_Books
- GT6 内置手册文本量大；建议改做 **JEI 信息页 + 进度树（advancements）** 等价物，文本分批转。

### 历史裁剪建议（已撤销，改为待适配/待评估）
- `compat/` 59 类、`asm/` 18 类（1.7.10 跨模组+字节码补丁）；
- `portals/` 20 类（Aether/Betweenlands 等 1.7.10 模组传送门）；
- Magic Energy Absorber、Buildcraft Laser、Thaumcraft/Botania 材质机器变体（保留材料本身）；
- IC2 作物体系（Recipes_Crops 中依赖 IC2 的部分；自有 Bush/Flowers 保留）；
- `Loader_Recipes_Foreign/Temporary`。
- 已等价替代：NEI→JEI✅、RF 桥接→Forge Energy capability✅。

---

## 四、施工顺序建议（Wave 36 起，每 wave 提交+runServer 验证）

| Wave | 内容 | 出处 |
|---|---|---|
| 36 | ✅ **B1-B10 全部 bug/欠账清账**（B1 first；runServer 验证通过） | 一 |
| 37 | ✅ 蒸汽锅炉 F1 + 燃烧箱变体 F2 + 柴油引擎 F3（早期能源链补全，柴油引擎 8 台已注册） | 二 |
| 38 | ✅ **N1 + N3** 转轴/齿轮箱/转动变压器 + 泵（旋转动力网，runServer 验证通过） | 三 |
| 39 | 🔶 反应堆 F5（2x2 核心+反应堆外壳方块已注册，缺 48 种燃料棒+中子通量+冷却液+熔毁逻辑） | 二 |
| 40 | 🔶 多方块批 1 F6.1-3（大型坩埚+3x3x3/5x5x5 罐+焦炉控制器+壁板方块已注册，缺处理逻辑） | 二 |
| 41 | 🔶 覆盖板批 1+2 F8（泵/输送/机械臂/红石火炬/红石中继器/标签选择器 6 种覆盖行为已实现，其余 40+ 种未动） | 二 |
| 42 | ✅ 传感器补全 F7（+12 种新传感器，共 16 种）+ 加热/冷却 N2（12 EU↔HU 节点）+ 磁铁 N4（10 磁铁方块） | 二/三 |
| 43 | 🔶 存储/流体容器扫尾 F11+F12 + 工具方块批 3 F10（12 简单工具方块 + 5 流体容器物品注册，缺功能性实现） | 二 |
| 44 | 🔶 多方块批 2 F6.4-7（低温蒸馏塔/大型燃气轮机/大型发电机/热交换器，4 控制器 BE 结构验证存根，缺处理逻辑） | 二 |
| 45 | 🔶 自动化/分拣/工作台/面板 N8+N9+N10 + 长距运输 N5（33 方块注册+模型/lang/创造栏齐，全部 BE 存根） | 三 |
| 46 | 🔶 树木系统 N7（40 方块注册：8 树种×5 类型，模型/lang/创造栏齐，缺世界生成+采胶+锯木产出链） | 三 |
| 47 | 🔶 电动工具 N6（5 电池+4 电动工具物品注册，NBT 充放电框架在线，缺完整电池体系+工具行为） | 三 |
| 48 | 🔶 装饰基建 N11 + 可放置物 N12 + 附魔 N13（30 方块+5 附魔注册+模型/lang/创造栏齐，缺方块行为+实体+战利品逻辑） | 三 |
| 49 | 🔶 多方块批 3 F6.9-12（基岩钻机/闪电棒/内爆/聚变，4 控制器 BE 结构验证存根，缺处理逻辑） | 二 |
| 50 | 🔶 激光 N15 → 量子 N16 + 物流网（25 方块注册+模型/lang/创造栏齐，LU/QU 能量类型+光束传输+物流均未实装） | 三 |
| 51 | 🔶 食物/药水/原版配方 N17+N18+N12（3 配方 Loader 空存根，手册/战利品未动） | 三 |
| 52 | 🔶 抛光：机器动画 overlay 帧、multiblockmains 贴图、JEI 页、双语言校对、B 类回归（待功能就绪后执行） | — |

> 原则：每 wave 内"方块+贴图+配方+本地化+JEI+创造栏"一次到位；新物品 id 注册前
> 必须对照 GTIconSetBlocks/GTTechnological 去重（mud/grass、electrometer 两次撞名教训）。

## 五、GT6 49 大类总览表（普查数据存档）

| 大类 | GT6 注册数 | 状态 | 计划项 |
|---|---|---|---|
| Basic Machines | 251 | ✅ | — |
| Molds | 117 | ✅ | — |
| Multiblock Machines | 117 | 🔶 | F6 |
| Burning Boxes | 105 | ✅ | F2（Wave 37） |
| Fluid Containers | 86 | 🔶 | F12 |
| Axles and Gearboxes | 67 | ✅ | N1（Wave 38） |
| Misc Tool Blocks | 51 | 🔶 | F10 |
| Reactors | 49 | 🔶 | F5 |
| Engines | 46 | ✅ | F3（Wave 37，50/50+） |
| Smelting Crucibles | 39 | ✅ | — |
| Crucibles Faucets | 39 | ✅ | — |
| Batteries | 37 | 🔶 | F9 |
| Storage | 28 | 🔶 | F11 |
| Steam Boilers | 26 | ✅ | F1（Wave 37） |
| Sensors | 20 | ✅ | F7（Wave 42，16/16） |
| Portals | 19 | ✂ | — |
| Untyped | 18 | 🔶 | N12 |
| Turbines | 15 | 🔶 | F4 |
| Lasers | 15 | 🔶 | N15（Wave 50，16 方块存根） |
| Heaters / Coolers | 10+10 | ✅ | N2（Wave 42，12 EU↔HU 节点） |
| Motors / Dynamos | 10+10 | 🔶 | F13 |
| Magnets | 10 | ✅ | N4（Wave 42，10 方块） |
| Automatic Tools | 10 | 🔶 | N8 |
| Transformers | 9 | 🔶 | F13/N1/N5 |
| Heat Exchangers | 8 | 🔶 | F6.7（Wave 44，控制器 BE 存根） |
| Extenders | 8 | 🔶 | N8（Wave 45，4 方块存根） |
| Long Distance Transport | 7 | 🔶 | N5（Wave 45，7 方块存根） |
| Panels | 6 | 🔶 | N10（Wave 45，6 方块存根） |
| Ropes | 6 | 🔶 | F10.11 |
| Quantum Energizers | 5 | 🔶 | N16（Wave 50，5 方块存根） |
| Laser Absorbers | 5 | 🔶 | N15（Wave 50，5 方块存根） |
| Logistics | 4 | 🔶 | F8/N16（Wave 50，Logistics Core 方块存根） |
| Portable Power Cells | 4 | ❌ | F9 |
| Pumps | 4 | ✅ | N3（Wave 38，4 阶） |
| Sorting | 4 | 🔶 | N8（Wave 45，4 方块存根） |
| ZPM | 3 | 🔶 | F9/N16（Wave 50，3 方块存根） |
| Safes | 2 | 🔶 | F11 |
| Crafting Tables | 2 | 🔶 | N9（Wave 45，2 方块存根） |
| Battery Boxes | 2 | 🔶 | F9 |
| Solar Panels | 2 | ✅ | — |
| Crystal Chargers | 2 | ❌ | F9 |
| Computing | 2 | ❌ | N14 |
| Chests | 1 | ✅+ | （超额：60 材质金属箱） |
| Magical Energy Production | 1 | ✂ | — |
| Laser Wires | 1 | 🔶 | N15（Wave 50，1 方块存根） |
| C-Foam | 1 | 🔶 | N11（Wave 48，2 方块存根） |
| Coins | 1 | 🔶 | F10/N12（Wave 48，coin_pile 方块存根） |

**非 MTE 体系**：covers 61 类（F8）、connectors 中 axle/redstone wire/laser wire/logistics wire 未移植
（N1/N8/N15/F8 批 4）、blocks 树木+装饰（N7/N11）、items 电动/枪/口袋工具+书（N6/N18）、
enchants 5（N13）、entities 4（N13）、外围 Loader 9 个（N17 等）。
