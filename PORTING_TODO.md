> 最新：生存坩埚、岩层、工具与矿石掉落。

> 最新：生存加工、物品标准化与标准中文。

> 最新：方块挖掘、掉落与兼容测试 JAR。

> 当前集中交接与验收入口：[剩余差异、测试及后续工作（2026-09-13）](PROJECT_HANDOFF_2026-09-13.md)。

> 最新修正：材料标签、矿处理、锅炉仪表与本地化。含原版材料绑定、炼药锅粉碎目标修正、全注册名称缺键审计，以及仍需核对的矿处理 / Tooltip 差异。

> 最新续批：原版方块加工与木材生存链：1,398 条配方。包含冷却液变体、木材去皮/切板/焦化、家具拆解与现代石英回收防复制调整。

> 最新续批：原版全物品组成与 422 条生存配方。1254 种原版物品分类覆盖，含多材料、损耗、容器保护、回收与车辆装拆；现代参考映射限制见报告。

> 最新续批：430 条生存加工配方。含橡胶/胶水上游、造纸纺织、染整、鞍与用品制造，以及未闭环范围。

> 最新一轮：资源同步、流体外观与 348 条配方。含 processResources 实测、流体染色修复和 runClient 验收顺序。

> 最新一轮：红石信号线移植。新增六种信号线、九条制造路线，恢复四个覆盖板的原版线材配方。

> 最新十批：覆盖板与机器自动化移植。12 种面板/传感器/传导与快门联动，含宿主范围、临时配方差异和 runClient 验收步骤。

> 最新连续三批：加工机器控制覆盖板。补实 8 种开关/重启、4 种检测器及制造；通用覆盖板宿主和完整物流协议仍待推进。

> 最新连续三轮：通用扩展器、能源转发与机器控制。含 runClient 验收顺序；物流核心、覆盖板协议和世界生态等仍未完成。

> 最新续批：四种原版过滤器，含旧装置行为变化和 runClient 测试清单。

> 最新续批：原版扩展器、桥接器与胶囊制造，含 runClient 测试步骤和剩余范围。

> 最新续批：胶囊容器、机器外壳与核核心制造，包含本轮测试清单与剩余范围。

> 最新源码差距核对见 2026-09-12 大蓝图审计。本文件保留历史计划；注册数量不代表功能已完成。

> 2026-09-11 审计更新：当前状态、已发现行为缺陷和框架重构见 [PORTING_AUDIT.md](PORTING_AUDIT.md)。本文件的旧 Wave 完成标记不等于当前验收结果；旧「永久砍除」建议已被完整还原目标取代。

# GregTech6 → 1.20.1 移植待办盘点

> 基于 `gregtech6-master` 源码与本仓库现状的对照（截至 Wave 23）。
> 原版参照路径：`F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main`。

## 一、已完成概览

| 领域 | 状态 |
|---|---|
| 材料体系（GT6Materials 转译、tooltip、燃烧值/物质含量） | ✅ |
| 矿石方块（单方块+NBT 背景石、OreBakedModel、基岩矿不可破坏） | ✅ |
| 世界生成（区域化石层 132 层、矿脉/小矿/矿床、基岩矿+指示花、黑沙/黏土/石英层、石子木棍、-64 下延、下界/末地专属） | ✅ |
| 矿物处理链 + 通用加工配方（OreDict/Extruder/Furnace） | ✅ |
| 合金/分解/燃料表/化学（手写 18 + 转译 91 条在线） | ✅（剩余见下） |
| 基础机器 66 种 + 大型批量机器 13 种（含 GUI、FaceConfig、自动 IO、16x/64x/256x 并行） | ✅ |
| 坩埚冶炼系统（坩埚/模具/浇铸盆/水槽/龙头） | ✅ |
| 燃烧箱、蒸汽机/强力蒸汽机（含引擎管道虚拟接口渲染） | ✅ |
| 料斗/队列料斗、储罐 | ✅ |
| 流体/物品管道、导线/电缆（动态烘焙模型：细伸入粗、restrictor 叠层、绝缘圈、选框同步） | ✅ |
| 覆盖板框架（六面槽、遮断、红石开关、面板渲染、客户端同步） | ✅（GUI 见下） |
| JEI（配方/催化剂/GU/t/FluidItem/世界生成信息页） | ✅ |
| 集成电路 0-24、901 个 MultiItem（工具/瓶/食物/蜜蜂）、tech 物品 | ✅ |
| iconset 装饰方块、石子/木棍随机模型 | ✅ |

## 二、待完成（按优先级）

### 高优先级（核心玩法链路）

1. **多方块框架 + 多方块机器**（框架与蒸馏塔已完成 Wave 24c：BasicMachine 结构门控 + heat_transmitter/distillation_tower_part 部件 + 3×3×8 结构校验；其余多方块沿用此框架）（任务 #9，GT6 `Loader_MultiTileEntities` "Multiblock Machines" 段）
   - GT6 模式：控制器 + `ONLY_ENERGY_IN` / `ONLY_ITEM_FLUID` / `ONLY_FLUID_OUT` 模式部件方块。
   - 蒸馏塔（3×3 热传输底座 + 3×3×8 塔体）、低温蒸馏塔多方块版（当前是单块简化版）。
   - 贴图类别 `multiblockmains/`、`multiblockparts/` 未移植。
   - 大型批量机器的 **并行处理**（16x/64x/256x，`NBT_PARALLEL`）已实现（Wave 24a）。

2. **能源网节点设备**（Wave 24b 已移植 32 个：电动机/发电机 LV-IV、变压器 5 级降压、蒸汽轮机 5 材质、太阳能 2 种、电池盒/储能柜 LV-IV——电池盒为简化内置缓存无电池物品槽；模式切换/升压、Flux 系、轮机转子耐久未做）
   - `transformers/` 变压器（升降压）
   - `batteries/` 电池盒（充放电、电池物品交互）
   - `energystorages/` 储能柜
   - `dynamos/` 发电机（KU→EU）与 `motors/` 电动机（EU→KU）
   - `solarpanels/` 太阳能板
   - `turbines/` 轮机（蒸汽/燃气/等离子）
   - `generators/reactor_core_1x1`、`reactor_core_2x2`、`reactor_rods/` 核反应堆（路线图 #11）

3. **化学配方剩余 ~276 条**（`tools/transpile_gt6_chem.py` 标记为不可译部分）
   - 依赖：单元格（cell）物品体系、跨模组物品（IC2/林业等，可改为本模组等价物或砍掉）、染料数组。
   - 建议先补 GT6 单元格物品（罐装/抽取逻辑），可一并解锁 `MultiItemCans`。

### 中优先级（独立子系统）

4. **加热/冷却器**：`heaters/`（heat_electric、heat_flux 电热器）、`cooler/`。HU 能量已有，缺方块。
5. **泵与管线**：`pump/`（抽液泵）、`pipelines/`（长距离流体管线端点）。
6. **红石装置**：`redstone/`（红石接收/发射器、按钮盒等）+ 传感器（路线图 #11，GT6 用覆盖板实现的显示型传感器也在此类）。
7. **存储类**：`massstorage/`（大宗存储）、`drawers/`（抽屉）、`safes/`（保险箱）、`lockers/`（储物柜）、`endergarbage/`（末影垃圾桶）。
8. **自动化类**：`automation/`（物品分配/卸载器）、`extenders/`、`usb/`（数据棒充能/传输）、`hdd/`。
9. **工作台类**：`craftingtables/`（各材质工作台/砧台）、`autotools/`（自动工具台）。
10. **覆盖板后续**：过滤器配置 GUI、显示型覆盖板（仪表/屏幕）、`covers/` 面板专用贴图（当前面板用物品贴图渲染）。
11. **树木系统**（路线图 #11）：`blocks/tree`（Log1/A/B/C + 防火变体、双类树叶/树苗）、`Loader_Woods`、树木世界生成；橡胶树采胶玩法。
12. **作物/植物**：`blocks/plants`（FlowersA/B 部分已做 iconset 花，Glowtus 已做）、`plants/`、`plantpot/` 花盆、`Loader_Recipes_Crops`（依赖 IC2 作物的可砍）。
13. **MultiItem 剩余两类**：`MultiItemBooks`（手册/书）、`MultiItemCans`（罐头，依赖单元格/食物体系）。
14. **工具剩余**：`items/tools/` 下 `electric/`（电动工具）、`guns/`（弓弩/枪）、`pocket/`（口袋多功能工具）、`machine/`、`crafting/` —— 现有 GTToolItems 覆盖早期手动工具，其余未移植。

### 低优先级（外围/锦上添花）

15. **杂项方块**：沥青、混凝土（普通/强化）、建筑泡沫 CFoam、透明/发光玻璃、铁路道路（`Loader_Rails`）——iconset 已含部分（path/bale 等），需逐一对照补缺。
16. **Loader_Books**：成就书/手册文本。
17. **Loader_Loot**：战利品箱注入。
18. **Loader_Recipes_Food / Potions / Vanilla / Woods**：食物加工、药水、原版物品配方替换。
19. **附魔**（路线图 #11）：GT6 自定义附魔。
20. **跨模组联动**（`magicenergyabsorber/`、`magnets/`、`lasers/`、`laserabsorbers/`、`quantumenergizer/`、`placeables/` 等）：多数依赖 Thaumcraft/IC2 等 1.7.10 模组，建议评估后砍掉或做本模组内等价物。
21. **Loader_BlockResistance / Loader_Recipes_Replace**：原版方块属性调整，按需。

## 三、已知小问题（技术债）

- 管道 ModelData 在**跨区块边界初次加载**时可能短暂陈旧（细伸入粗的延伸段晚一拍出现）；放置/更新邻居后即修正。端盖已内缩 0.015px，不会再闪烁。
- 管道/线缆的 blockstate JSON 仍是 multipart 格式，运行时被动态模型整体替换，仅作兜底，可择机清理。
- 化学转译器对部分配方做了机器改派（SO2→SO3 入 BurnMixer 等），后续做多方块时需回顾。
- `largecentrifuge` 等大型机器硬度统一用了 6.0（GT6 个别为 12.5）。

## 四、机器贴图类别对照表（textures/blocks/machines，39 类）

| 已移植 (6) | 未移植 (33) |
|---|---|
| basicmachines（79/79）、engines、generators（燃烧箱+反应堆贴图已拷贝，反应堆功能未做）、tanks、hopper、queuehopper | automation, autotools, batteries, cooler, covers, craftingtables, drawers, dynamos, endergarbage, energystorages, extenders, hdd, heaters, laserabsorbers, lasers, lockers, magicenergyabsorber, magnets, massstorage, motors, multiblockmains, multiblockparts, pipelines, placeables, plantpot, plants, pump, quantumenergizer, redstone, safes, solarpanels, tools, transformers, turbines, usb |

> 注：`generators/` 下 burning_* 已作为燃烧箱移植；reactor_* 贴图在列但反应堆未实现。
