# 单方块机器合成配方对齐原版、机器参数表与续批缺口（2026-09-14）

本轮针对「单方块机器的合成配方不对」做集中修复，并把机器的并行/能耗/硬度参数改成原版数据。
期间另外发现三类此前未记录的问题（27 个空配方表、多方块机器无配方、旧测试断言过严），一并记录在本文件与
[审计清单](#本轮发现但未修复需下一批处理)。

## 1. 问题根因

`data/BasicMachineRecipePack.java` 旧实现按能源类型套用三种通用图案：

```
HU      -> PhP / RMR / PwP
RU, KU  -> GhG / RMR / PwP
其它    -> PRP / GMG / hwP
```

原版每台机器的图案都不同，因此 257 个单方块机器的工作台配方**全部与原版不一致**。示例：

| 机器 | 原版 GT6 图案 | 旧实现图案 |
|---|---|---|
| Oven | `wMh` / `BCB` | `PhP` / `RMR` / `PwP` |
| Shredder | `GDG` / `hMw` | `GhG` / `RMR` / `PwP` |
| Crusher | `DMD` / `hSw` | `GhG` / `RMR` / `PwP` |
| Lathe | `TDS` / `dMG` | `GhG` / `RMR` / `PwP` |
| Sifter | `WxW` / `RMR` / `SwS` | `GhG` / `RMR` / `PwP` |
| Smelter | `wUh` / `PMP` / `BCB` | `PhP` / `RMR` / `PwP` |

旧的图案同时缺少锤子 `h`、扳手 `w`、螺丝刀 `d`、剪线钳 `x` 这些原版工具字符。

## 2. 修复内容

### 2.1 配方表改为从原版源码生成（不手抄）

新增 `tools/extract_basic_machine_recipes.py`：从 `Loader_MultiTileEntities.java` 的
"Basic Machines" 标签解析出 251 条注册（机器名、等级材料、图案行、键值、NBT 参数），
落盘 `tools/gt6_basic_machine_recipes.json`；同一个脚本可用 `--tab "Multiblock Machines"`
解析多方块表（117 条）。

新增 `tools/generate_machine_recipe_table.py`：把原版图案映射到移植版机型与等级，
生成 `src/main/java/com/gregtech/gregtech/data/BasicMachineCraftingRecipes.java`
（**241 条**，覆盖除多方块机器外的全部注册机型；等级顺序与
`MT.DATA.Heat_T / Kinetic_T / Electric_T` 一致）。

新增 `tools/gt6_machine_map.py`：移植版机型 ↔ 原版机型的唯一映射来源，供两个生成器共用。

### 2.2 原料翻译与回退

新增 `data/MachineRecipeIngredients.java`：把原版键值表达式翻译成 1.20.1 原料。

- 支持 `mat:`（材料形态）、`block:`（机壳/储块）、`pipe:`（GT6 流体管道）、`wire:`/`cabletier:`/`wiretier:`（导线与电缆）、
  `il:`（GT6 `IL.*` 等级元件）、`circuit:`（`OD_CIRCUITS[n]`，按原版累进语义接受 ≤n 级电路）、
  `part:`（多方块部件原始数字 ID）。
- 材料形态优先使用 `forge:` 标签（保持跨模组等价），其次 `gregtech:<prefix>/<material>` 标签；
  形态缺失时按 `plateQuintuple→plateQuadruple→…→plate` 之类的链回退，并记录替换。
- 原版 `OP.wireGt01.dat(ANY.Iron)` 这类 `ANY` 家族键表示「该组任意材料」，移植版表达为
  「该尺寸的任意导线」，不再编造原料。
- 工具字符 `h/w/d/x`（以及 `b/c/k/p/r/s/y`）按 `gregapi/recipes/CR.java` 的工具表补齐为
  `gregtech:tool_hammer` 等真实工具，`tool_shaped` 会按 GT6 规则消耗耐久而不是吃掉工具。

### 2.3 机器参数表（并行、能耗、硬度）

移植版此前用合成表推导：`TIER_STATS` 固定硬度、`legacyParallelLimit` 除大型机外恒为 1、单方块机器一律 32 GU/t。
原版是逐机逐等级的，因此新增 `tools/generate_machine_params_table.py` →
`data/BasicMachineOriginalParams.java`（**244 条**），`BasicMachineDefinitions` 改为读取该表，缺项时才回退旧值。

修正项示例（左＝原版，右＝修复前）：

| 机器 | 并行上限 | 能耗 | 硬度 |
|---|---|---|---|
| Crusher / Sifter / Compressor / Mixer | 4 / 8 / 16 / 32 | KU·RU 32→2048 | 7.0 / 6.0 / 9.0 / 12.5 |
| Centrifuge | 1 / 2 / 4 / 8 | RU 32→2048 | 同上 |
| Electrolyzer | 1 / 2 / 4 / 8 / 16 | EU 32→8192 | 4.0（五级相同） |
| Electric / Cryo Mixer | 4 … 64 | EU 32→8192 | 4.0 |
| Generifier | 100 | TU 1 | 6.0 |
| Melter | 1000（并行缩放时长） | HU 32 | 6.0 |
| Coke Oven | 16 | **TU 1**（原为 HU 32） | 5.0 |
| Implosion Compressor | 64 | **TU 1**（原为 HU 32） | 12.5 |
| Fusion Reactor | — | **TU 8192**（原为 QU 32） | 12.5 |
| Distillation Tower | — | **HU 512**（原为 32） | 6.0 |
| Cryo Distillation Tower | — | **CU 512**（原为 32） | 6.0 |

### 2.4 其它修复

- `MachineRecipeMaps.byMachineName` 补 `pressurewasher` / `heatmixer` / `default` 分支：
  原来这三个名字会命中 `default -> throw`。客户端菜单里 `byMachineName("default")` 的
  取值也改为直接引用 `DidYouKnow`，去掉这个崩溃点。
- 多方块机器中已有对应部件的两台补上原版配方：Coke Oven（`hRT/PMR/dRT`，火砖以焦炉墙代替）与
  Implosion Compressor（`CPC/PAP/RMR`）。`LargeMachineParts.find(int)` 新增非抛异常查询，
  并登记 18000/18023/18100/18101/18102/18105 六个部件 ID 的归属。
- 生存回路必要的 `gregtech:saplings` 物品标签（26 个 GT 树苗），Plantalyzer 配方依赖它。
- `MaterialCompatibilityTests#basicMachinesHaveSurvivalRecipes` 的断言此前假定图案没有任何空格；
  原版图案本来就有空格（如 Rolling Mill 的 `M `、Roll Bender 的 `wS `），改为「非空格子数 == 已解析原料数」。

## 3. 验证

新增 `gametest/BasicMachineCraftingTests.java`（5 项）：

1. `everyMachineUsesItsOriginalPattern`：逐格比对已加载配方与原版图案（241 个机型等级）。
2. `everyIngredientResolves`：任何解析不到的原料都会失败。
3. `tableCoversEverySingleBlockMachine`：表覆盖所有应生成配方的机型。
4. `substitutionsAreReported`：替换清单必须与文档中的允许集合完全一致（防止静默走样）。
5. `originalTableIsWellFormed`：图案矩形、每个符号都有键、每个键都被使用。

最终结果（`../machine-recipes-final.log`）：

```
========= 218 GAME TESTS COMPLETE ======================
All 218 required tests passed :)
BUILD SUCCESSFUL
```

- 配方解析错误 **0**（首轮实现缺少工具字符时是 204）。
- 原料替换 **1 条**：`wire 1 of Iron -> 该尺寸任意导线`（GT6 `ANY.Iron` 家族语义），
  见 `docs/machine-crafting-substitutions.json`。

复现命令：

```
python tools/extract_basic_machine_recipes.py --out tools/gt6_basic_machine_recipes.json
python tools/extract_basic_machine_recipes.py --tab "Multiblock Machines" --out tools/gt6_multiblock_recipes.json
python tools/generate_machine_recipe_table.py
python tools/generate_machine_params_table.py
gradlew compileJava --offline
gradlew runGameTestServer --offline -PgameTestHeap=3G
```

注意两点：

- 1.5 GB 默认堆在「Preparing start region」阶段会 OOM，验证时用 `-PgameTestHeap=3G`。
- datapack 加载失败时 `runGameTestServer` 仍可能报 `BUILD SUCCESSFUL` 而**一项测试都没跑**。
  判断通过必须看日志里的 `All N required tests passed`，不能只看 BUILD 结果。

## 4. 续批：多方块机器与部件合成配方（同日晚）

### 4.1 问题

`BasicMachineRecipePack` 跳过 16 个多方块机型，而 `coke_oven_main`、`distillation_tower_main`、
`fusion_reactor_main` 等**独立控制器方块既不在该表内、也没有任何资源包配方**，因此整套多方块机器
在生存中无法获得。新增的 `BasicMachineCraftingTests#everyMachineUsesItsOriginalPattern` 正是先暴露了这一点。

### 4.2 修复

新增 `tools/generate_multiblock_recipe_table.py` → `data/MultiblockCraftingRecipes.java`（**55 条**）
与 `data/MultiblockRecipePack.java`（第二套内置数据包，输出
`recipes/machines/multiblock/<blockId>.json`）。

映射原则（不做无依据发明）：

- GT6 部件 ID 由 `content/multiblock/LargeMachineParts.find(int)` 解析；本轮补入
  `18002 → tank_wall`（原版 "Stainless Steel Wall"）与 `18022 → tank_wall_dense`（原版 "Dense Stainless
  Steel Wall"）两个此前缺失的条目。
- 每条目标都在生成期解析全部键值；**只要有一个键无法对应到移植版实物，该目标就整条跳过并在生成日志里列出**，
  不使用替代原料蒙过去。本轮最终 0 条跳过。
- 结构键 `M` 按机型替换为移植版自己的结构方块（如大型离心的 `centrifuge_part`、大型粉碎的
  `tungstensteel_wall`、燃气轮机外壳的 `large_gas_turbine_wall`）。
- `gt:re-batteryN` 接为移植版 `battery_<lv|mv|hv|ev>`。
- 材质名称差异按原版材质表对齐；顺带补上材料别名 `Osmium → OsmiumElemental`
  （GT6 写作 Osmium，移植版注册名为 OsmiumElemental，此前 GT6 派生内容引用 "Osmium" 会解析失败）。

### 4.3 覆盖

| 类别 | 结果 |
|---|---|
| 旧版 `large*` 单方块机型 | 13 台全部有配方（ID 按各机型实际机壳材料，如 `largecentrifuge_tungsten_steel`、`largesluice_titanium`、`largeoven_invar`） |
| 独立控制器方块 | 13 个（焦炉、内爆、蒸馏塔、低温蒸馏塔、聚变、换热器、基岩钻、避雷针、大锅炉、大坩埚、大发电机、燃气轮机、蒸汽轮机）全部有配方 |
| 多方块结构部件 | 29 条（离心部件、电解部件、传热器、蒸馏塔部件、溜槽部件、破碎轮、撕碎刀片、5 种线圈、4 种四核处理器、聚变通风单元等） |

覆盖报告写入 `docs/multiblock-recipes-coverage.json`。

### 4.4 验证

```
========= 222 GAME TESTS COMPLETE ======================
All 222 required tests passed :)
```

新增 4 项多方法测试（`gametest/MultiblockCraftingTests.java`）：每个目标方块都必须有配方且逐格匹配原版图案、
旧版多方块机型与 13 个控制器必须被覆盖、原料必须全部可解析、覆盖情况必须落盘。

同轮修掉的两个实现层问题：提取器把「键值为引号字符串」（如 `'B', "gt:re-battery1"`）误当成图案行；
以及 `mat:blockPlate`（应为方块形态 `block:blockPlate@Material`）导致的原料回退。

## 5. 续批：机器产出投递与并行上限（同日晚，对应缺口 C）

### 5.1 修掉的产品投递错误

`api/recipe/MachineWorkOutputs#flush` 原实现分两趟寻找输出位置，第二趟的条件是「任意非空格子」，
于是当待投递产物在输出区找不到同类堆叠时，会**合并进一个装着别的物品的格子**：
`present.copyWithCount(present.getCount()+take)` 让那个物品凭空变多，同时 `pending.shrink(take)`
把真正的产物抹掉。也就是说堵塞的输出区会造成物品替换与凭空增殖。

修复后两趟的语义明确：第一趟合并到同类堆叠，第二趟使用空格子；不同物品的格子一律不动。
流体同理（`fill` 本身类型安全，但仍统一语义）。

### 5.2 修掉并行批次造成的永久卡住

`BasicMachineBlockEntity#computeParallel` 原来只按**输入**二分出最大批次数，不看输出空间。
当一批产物放不下时，`doActive` 会每 tick 重试投递并停在 100 % 进度，永不结束——
`largemixer`（并行上限 256）在只声明 1 个物品输出槽的 Mixer 表上就会落到这个状态。

现在按原版 `MultiTileEntityBasicMachine.canOutput` 增加输出空间限流
（`outputLimitedParallel` + `itemsFit`/`fluidsFit`，投递规则与 `flush` 完全一致：
先补同类、再用空位），空间不足时批次上限自动降到能放下的规模；输出区完全占满时不再开工，
而不是开工后卡死。

### 5.3 验证

新增 `gametest/MachineOutputTests.java`（4 项）：不合并到不同物品的格子、放不下的产物保持待投递、
同类堆叠优先补满、流体输出遵守储罐内容。其中第一项在修复前必然失败（旧实现会把铁锭并进铜锭格）。

```
========= 226 GAME TESTS COMPLETE ======================
All 226 required tests passed :)
```

## 6. 续批：空配方表——化学转译器补齐（同日晚，对应缺口 A 的第一部分）

### 6.1 根因

移植版已有 `tools/transpile_gt6_chem.py`（把原版 `Loader_Recipes_Chem` 的 `RM.*.addRecipe*`
转成 `GTChemGen.java` 的数据行，运行时由 `GTGeneratedChem` 解析），但**只转出 181 / 402 条**。
原因是解析器的正则不容忍原版源码的对齐空格：

```
OP.dust .mat(MT.OREMATS.Bauxite, 1)      # 前缀名与 .mat 之间有空格
MT.Al .liquid(U*2, T)                     # MT.X 与 .liquid 之间有空格
gemChipped.mat(MT.Ice, 1)                 # 静态导入，没有 OP. 前缀
```

这类写法在原版里很常见（GT6 用空格对齐参数），于是整张配方表空着。**Autoclave 与 Fermenter
两台机器因此在生存中完全不能加工**。

### 6.2 修复（只做形式容错，不做语义猜测）

- `OP.<prefix> .mat(` / `<prefix>.mat(` 允许空格与省略 `OP.` 前缀。
- `MT.X .gas/liquid/fluid(` 、`FL.X .make(` 允许空格；`is_fluid_expr` 同步修正
  （此前它用旧正则判断，导致带空格的流体表达式被当成物品，整条配方被丢弃）。
- 补 `KNOWN_PREFIXES`（`crushedCentrifugedTiny`、`crushedPurifiedTiny`、`plateDense` 等
  原版实际使用的形态）。
- 代入原版常量 `STEAM_PER_WATER=160`、`STEAM_PER_EU=2`、`EU_PER_LAVA=80`（值取自
  `gregapi/data/CS.java`）。
- **修正一处保真缺陷**：`FL.DistW.make(n)` 原被映射成普通水 `w:`，现在按自身流体 `f:DistW:` 处理
  （`w:` 在运行时精确解析为 `minecraft:water`，会把蒸馏水变成普通水）。
- 3 个以上流体数组（原版允许）由「直接放弃」改为「第二组起并入输出流体」，不再整条丢弃。

### 6.3 结果

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 转译成功 | 181 / 402 | **353 / 402** |
| 运行时注册 | 163 added / 18 skipped | **303 added / 50 skipped** |
| Autoclave 配方 | 0 | **12**（铝土矿 + KOH/NaOH + 蒸汽 → KAlO2/NaAlO2 + 钛铁矿/二氧化钛 + 蒸馏水，拜耳法） |
| Fermenter 配方 | 0 | 2 |
| Cryo Distillation Tower / Freezer | 0 | 3 / 2 |
| Mixer / Electrolyzer / Drying / Roasting | 40 / 14 / 30 / 34 | 91 / 49 / 39 / 39 |

因此 **autoclave、largeautoclave、fermenter、largefermenter 四个机器方块从「永远无法加工」变为可用**。

被跳过的 50 条记录在 `docs/chem-missing-content.json`（29 种缺失内容，如 `m:AquaRegia`、
`m:UF4`、`omd:Eudialyte`），属于移植版尚未注册的流体/形态，不是解析失败。

### 6.4 验证

新增 `gametest/GeneratedChemistryTests.java`（3 项）：Autoclave/Fermenter/Cryo Distillation Tower
必须有配方且既有化学链不得退化；转译注册数不得低于 300、跳过数不得高于 60（防止解析器回归）；
缺失内容清单必须落盘。

```
========= 229 GAME TESTS COMPLETE ======================
All 229 required tests passed :)
```

复现：`python tools/transpile_gt6_chem.py`（生成 `GTChemGen.java`），
`python tools/diagnose_chem_transpile.py`（逐条报告未转译原因，便于继续补）。

## 7. 续批：锤子/凿子敲方块的掉落转换（同日晚，对应缺口 D 的主要部分）

### 7.1 问题

移植版把**近千条配方注册进了没有人读取的配方表**：`Hammer`（锤击）与 `Chisel`（凿刻）两张表
有写入方（`StoneAndToolSurvivalRecipes`、`VanillaBlockProcessingRecipes` 的 974 条原版方块加工、
`VanillaProcessingRecipes`、`Loader_Recipes_OreProcessing`），但全仓没有任何游戏内读取方。
结果是：**用锤子敲方块没有任何效果**——GT6 早期最核心的玩法（石头→圆石、矿石→粉碎矿石、
箱子→内容物）在移植版里是死的。原版读取点是
`GT_Tool_HardHammer#convertBlockDrops`（以及 `ToolCompat` 的凿子分支）。

### 7.2 修复

新增 `data/ToolBlockConversionEvents.java`，按原版语义实现：

- `HARD_HAMMER → RM.Hammer`、`CHISEL → RM.Chisel`、`CROWBAR / UNIVERSAL_SPADE → RM.Unboxinator`
  （后两者对应原版 `GT_Tool_Crowbar` / `GT_Tool_UniversalSpade` 的同一套 `convertBlockDrops`）。
- 转换算法与原版一致：方块自身有配方时替换整份掉落（带方块实体的方块不参与这一支），
  否则**逐掉落物**转换；转换产物按原版 `getOutputs(random)` 掷概率。
- 耐久按原版常数计费：锤 25（破方块）+ 50/次转换，凿 50 + 100/次，铲 100 + 100/次，撬棍 50 + 100/次；
  **耐久不足时不做转换**（原版同样以可用耐久为上限），方块按常规破坏。
- 1.20.1 没有掉落修改事件，因此实现方式是在 `BlockEvent.BreakEvent` 拦截破坏、自行移除方块并把
  转换后的掉落物生成出来（等价结果）。未命中任何配方时完全不干预，交给原版流程。

### 7.3 效果

| 玩法 | 修复前 | 修复后 |
|---|---|---|
| 锤子敲石头/矿石 | 无效果 | 石头→圆石、矿石层→2 份原矿、`Hammer` 表全部路线可达 |
| 凿子凿方块 | 无效果 | `Chisel` 表路线可达 |
| 撬棍/铲子开箱 | 无效果 | `Unboxinator` 表路线可达（拆箱、导线捆） |

按表规模计，**约 1000 条此前不可达的配方重新可用**（`Hammer` 表 1000+ 条）。

### 7.4 验证

新增 `gametest/ToolBlockConversionTests.java`（5 项）：锤子敲铝土矿层必须得到 2 份原矿
（证明表已被读取）、无配方方块保持原掉落、非锤凿工具不转换、`Hammer`/`Chisel` 表不得为空、
撬棍按 `Unboxinator` 拆箱且产物替换原物。

```
========= 234 GAME TESTS COMPLETE ======================
All 234 required tests passed :)
```

## 8. 续批：矿处理缺失的两条下游路线（同日晚，对应缺口 A 的第二部分）

### 8.1 问题

对照原版 `Loader_Recipes_Ores` 第 455–461 行，**净化矿（crushedPurified）之后原版有两条下游路线，移植版一条都没有**：

| 原版 | 输入 | 输出 | 时间/EU | 移植版原状 |
|---|---|---|---|---|
| Magnetic Separator（`tMagnet` 概率 `{10000,600,600,600,600,600}`） | 净化矿 ×1 | 离心矿 ×1（必出）+ 5 种副产物细粉 ×18（各 6 %） | 144 / 16 | 无 |
| Sifting（概率 `{9,90,360,1350,1800,3600,4500}`） | 净化矿 ×1 | 传奇宝石×8(0.09 %)→精致×4(0.9 %)→无瑕×2(3.6 %)→宝石×1(13.5 %)→瑕疵×2(18 %)→碎宝石×4(36 %)→材料自身粉末(45 %) | 144 / 16 | 无 |

另有对应的「细粉」变体（`crushedPurifiedTiny`，时间 16，概率各为十分之一）。

后果：**磁力分离机 5 个等级与筛分机 9 个等级（sifter ×4 + electricsifter ×5）共 14 个机器方块完全没有配方可用**，
并且原版矿处理链缺了一环。移植版此前把「净化矿 → 离心矿」放在了**离心机**上——原版那一步其实是磁力分离机
（见下节记录）。

### 8.2 修复

在 `loaders/c/Loader_Recipes_OreProcessing` 新增 `processPurifiedRoutes()`，按原版逐条实现：

- **磁力分离机**：每个有副产物的材料 2 条（普粉 144 tick / 细粉 16 tick），概率照抄 `tMagnet`；
  副产物顺序按原版 `Loader_Recipes_Ores:389-393` —— **非磁性副产物在前、磁性在后**（用移植版的
  `MaterialProperty.MAGNETIC` 还原该排序；原版分 PASSIVE/ACTIVE 两类，移植版只有一位标志）。
  缺失的副产物按原版 `UT.Code.select(i, aMat, tMagnetList)` 的语义回退到材料自身。
- **筛分机**：每个有宝石形态的材料 2 条（144 tick / 16 tick），7 个产出与概率照抄原版，
  稀有档位按原版 `ST.amount(8/4/2, tGem)` 的倍率给 8/4/2 个。
- 两者都不需要新增任何内容——用到的形态（`crushedPurified`、`crushedPurifiedTiny`、
  `crushedCentrifuged`、`crushedCentrifugedTiny`、6 档宝石、粉末）移植版全部已有。

### 8.3 结果

| 配方表 | 修复前 | 修复后 |
|---|---|---|
| MagneticSeparator | 0 | **600** |
| Sifting | 0 | **216** |

**磁力分离机（5 个等级）、筛分机（4 个等级）、电动筛分机（5 个等级）共 14 个机器方块恢复可用。**

### 8.4 验证

新增 `gametest/OrePurificationRoutesTests.java`（3 项）：磁力分离机必须对铝土矿有净化矿路线且概率/时长/EU
与原版一致、细粉变体为 16 tick；筛分必须存在原版宝石阶梯（7 产出、8/4/2 倍率、细粉变体）；
覆盖数量落盘 `docs/ore-routes-coverage.json`。断言采用数据驱动（从配方反查形态与材料），不依赖硬编码材料名。

```
========= 237 GAME TESTS COMPLETE ======================
All 237 required tests passed :)
```

### 8.5 记录的保真差异

移植版把「净化矿 → 离心矿」也注册在**离心机**上（`processWashing`，原版是磁力分离机的那一步）；
原版离心机在净化矿上做的是「→ 9 份离心细粉 + 3 种副产物流体」。本轮**只新增、不删除**，
以免破坏既有生存链与回归测试；该重复路线已记入下一批待对齐项。

## 9. 启动故障记录：IDE 启动报模块冲突（2026-09-14 补充）

### 现象

从 IDEA 启动 runClient 时在 modlauncher 阶段直接崩溃，Gradle 控制台也会显示
`> Task :cpw.mods.bootstraplauncher.BootstrapLauncher.main() FAILED`：

```
java.lang.module.ResolutionException: Modules main and gregtech export package
    com.gregtech.gregtech.block.wood to module minecraft
    at cpw.mods.modlauncher.ModuleLayerHandler.buildLayer(ModuleLayerHandler.java:75)
```

### 根因（已复现、已定位）

`build/classpath/<run>_minecraftClasspath.txt` **只在 Gradle 真正执行 run 任务时才会写出**。
IDEA 的 runClient 配置（`genIntellijRuns` 生成）是直接启动 `BootstrapLauncher`，只通过
`-DlegacyClassPath.file=` 引用该文件，其前置任务 `:prepareRunClientCompile` **不会生成它**
（已实测：删掉文件后跑 `prepareRunClientCompile` / `prepareRunClient` 都不会重新生成）。

于是上一次 Jar 构建（`build` 会重建 `build/`）删掉该文件之后，从 IDE 启动就会缺文件；
Forge 随即退回把 `build/classes/java/main` 当成模块 `main`，而 `MOD_CLASSES` 里同一个模组又是模块
`gregtech`，两者导出同一个包 → 模块解析失败。

复现验证（同一套 IDEA 参数、同一 classpath）：

| legacyClassPath.file | 结果 |
|---|---|
| 文件存在 | 正常启动到主界面 |
| 文件不存在 | 复现上述 ResolutionException |

### 修复

`build.gradle` 新增 `ensureRunClasspaths` 任务：若某个 `*_minecraftClasspath.txt` 缺失，就从同目录
任一其它变体恢复（四个变体内容相同，均为 Minecraft/Forge 的 classpath，实测均为 17583 字节），
并把它 `finalizedBy` 挂到四个 `prepareRun*Compile` 任务上——也就是 IDEA 启动前必走的步骤，
因此以后 `clean` 或构建 Jar 之后 IDE 启动可自愈。若四个文件全部缺失（全新 clean），
任务会打印明确提示：先跑一次 `./gradlew runClient`。

### 备注

- 已验证与陈旧 `build/libs/gregtech-1.0.0.jar` **无关**：把它加进 classpath 也不会触发该错误。
- 从 Gradle 启动（`./gradlew runClient`）始终正常，因为它自己会先生成该文件。

## 10. 本轮发现但未修复（需下一批处理）

### A. 27 个配方表为空 → 56 / 257 个机器方块永远无法加工

**已部分完成**：第 6 节解冻 autoclave / largeautoclave / fermenter / largefermenter（4 个方块），
第 8 节解冻 sifter / electricsifter / magneticseparator（14 个方块）。剩余机型的阻塞点如下
（都不是「抄一条配方」能解决的，缺的是上游内容）：

| 机型（等级变体数） | 阻塞点 |
|---|---|
| printer(5) | 原版配方在 `GT6_Main`，产出是**手册/指南书**（移植版没有手册体系） |
| scannervisuals(5) | 原版 14 条**全部是 `addFakeRecipe`**（JEI 展示用），没有可执行配方——空表是忠实的；移植版若要显示应走 JEI |
| autocrafter(5) | **原版同样没有任何注册**（全仓只有 `Recipe.java` 的字段别名与机器注册），空表是忠实的 |
| scannermolecular(1) | 原版 0 条注册，空表是忠实的 |
| plantalyzer(5) / bumblelyzer(5) | 依赖尚未移植的作物与养蜂体系 |
| cryomixer(5) | 原版 9 条在 `Loader_Recipes_Food` / `MultiItemFood`（需要 `FL.Cream`/`Tea_*`/冰淇淋物品） |
| sluice(4) + largesluice(1) | 需要 `FL.Sluice` 流体、`MT.SluiceSand` 与 sluice 方块物品 |
| replicator(1) | 原版经 `RM.replicator()` 辅助方法注册（UUM/Biomass 流体链） |
| generifier(1) | **原版同样是空的，不要补**（见下） |

**Generifier 不是缺口，不要为它编造配方。** 原版 `Loader_Recipes_Handlers.java:733-737` 的循环条件是
`tMaterial != tMaterial.mTargetGenerifying.mMaterial`，而 `mTargetGenerifying` 在
`OreDictMaterial.java:295` 初始化为材料自身，全仓没有任何 `setTargetGenerifying` 调用点，
因此原版 Generifier 表在未改动的 GT6 里就是空的（该机器是给整合包脚本预留的挂钩）。
移植版保持为空才是忠实行为；`MachineRecipeMaps.generify()` 没有调用点同样正确。

无机器使用的空表：Microwave、DidYouKnow/Other、ToolHeads、Cooking、BlastFurnace、VacuumFreezer、
Assembler、CNC、CrucibleSmelting、BedrockOreList、Calciner、BumbleQueens、Trees。

### B. 多方块机器与独立控制器仍无合成配方

**已于同日续批完成**，见上文第 4 节：55 条配方覆盖 13 台旧版大型机、13 个控制器方块与 29 个结构部件。

仍缺的多方块目标（生成器已记录，缺的是移植版对应方块而非原料数据）：GT6 的 5 种锅炉气压计
（移植版只有 1 台大锅炉）、4 种大型储罐主阀（移植版为 `tank_3x3`/`tank_5x5` 两种罐）、
7 种其余材质的大坩埚（移植版只有 1 台）、`Large Copper Coil` 与 `Storage Quadcore Processor Unit`
（移植版没有这两个方块）。

### C. 输出空间不足时会永久卡住（潜在软锁）

**已于同日续批完成**，见上文第 5 节。同时修掉了 `MachineWorkOutputs.flush` 把产物并进异类格子的
物品替换缺陷（该缺陷本身比软锁更严重）。

### D. 死配方

**主要部分已完成**（第 7 节）：`Hammer`/`Chisel`/`Unboxinator` 现在由工具的世界交互读取。

仍无读取方的表：`ByProductList` 与 `CrucibleAlloying`（仅 `addFakeRecipe`，JEI 展示用——这是设计意图，
不是缺陷）；`FuelRecipeMaps.Plasma / Turbine / FluidBed / Magic`（既无配方也无读取方）。
另外原版 `TileEntityBase08Barrel` 会用 `RM.Fermenter` 发酵（流体+物品），移植版木桶尚未接入该表。

### E. 其它已确认差异

- 单等级机器在移植版被合并：GT6 的 Lightning Processor 是 5 级，移植版只有 1 级；
  Melter/Autoclave/Bath/Generifier/Coagulator/Fermenter 的机壳材料也与原版不同
  （原版 ANY.Iron / MT.Ceramic 等，移植版统一为 StainlessSteel）。因改动会变更注册 ID，暂不动。
- 原版 `NBT_INPUT_MIN/MAX`（Fermenter 16..64、聚变 1..16384、蒸馏塔 1..1024）尚未接入
  `BasicMachineSpec`，目前仍由「TU→1..16、其它→额定值一半..两倍」推导。
- GameTest 中新建的临时 `RecipeMap` 会永久进入 `RECIPE_MAPS`（6 处）。

## 5. 下一批建议顺序

1. ~~**B（小而有界）**：补齐部件 ID 映射，用已提取的多方块图案给 16 个机型与独立控制器补配方。~~ **已完成（第 4 节）**
2. ~~**C（风险最高）**：`canOutput` 式输出限流 + 输出数量校验，避免大批量并行机器卡死。~~ **已完成（第 5 节）**
3. **A（继续）**：剩余空表都需先补上游内容——`cryomixer`（需要 `FL.Cream` 等食品流体）、
   `sluice`（需要 `FL.Sluice` + sluice 物品）、`replicator`（UUM 链）；`printer` 需要手册体系；
   `plantalyzer`/`bumblelyzer` 需要作物与养蜂。**不要再补的**：autocrafter / scannermolecular /
   scannervisuals / generifier —— 原版同样是空表或只有 JEI 展示配方。
4. **E**：单等级机器材料与 min/max 功率对齐；把「净化矿 → 离心矿」从离心机改回磁力分离机
   （第 8.5 节的保真差异）；木桶接入 `RM.Fermenter`。

## 11. 续批：电压等级 ↔ 机壳材质整体错位一级（反馈）+ 接受功率区间对齐

### 11.1 问题（反馈）

> ULV 不是镀锌钢而是锡合金，绝大多数机器没有 ULV 等级；LV 是镀锌钢，MV 是铝……你现在正好错位了一级。

对照 GT6 `gregapi/data/MT.java:3690-3691`：

```
Electric_T = {TinAlloy, SteelGalvanized, Al, StainlessSteel, Cr, Ti, Ir, Os, ...}   // index 0 = ULV
Flux_T     = {Sn,       Pb,              Invar, Electrum, EnderiumBase, Enderium, ...}
Kinetic_T  = {Wood,     Bronze,          Steel, Ti, TungstenSteel, ...}
Heat_T     = {Stone,    Steel,           Invar, Ti, TungstenCarbide, ...}
```

GT6 的 Basic Machines（`Loader_MultiTileEntities` 的 "Basic Machines" 段）、电动机（10021-25）、
发电机（10111-15）、加热器（10001-05）、冷却器（10161-65）、激光器（10101-05）**一律用
`Electric_T[1..5]`，也就是 LV..IV**——ULV 档根本不存在；只有变压器（10040+，第一个是
`Transformer (ULV-LV)`，用 `Electric_T[0]`）与电池盒（10080+，`VN[0..9]`）才有 0 级。
移植版在 `EnergyNodeDefinitions` 里把这一切整体当成 `Electric_T[i+1]` 用了，于是**每一档都高一级**。

### 11.2 修复（`content/energy/EnergyNodeDefinitions.java`）

| 设备 | 修复前（= Electric_T[i+1]） | 修复后（GT6 原值） |
|---|---|---|
| 电动机 / 发电机 / 储能柜 / 电池盒 `tiers` | lv=Al、mv=StainlessSteel、hv=Cr、ev=Ti、iv=Tungstensteel | lv=**SteelGalvanized**、mv=**Al**、hv=**StainlessSteel**、ev=**Cr**、iv=**Ti** |
| 电池盒 | 只有 lv..iv | **补上 ULV 电池盒（8 V / TinAlloy）**——GT6 10080 从 `VN[0]` 起 |
| 变压器 `steps` | 每档再错一级 | ulv_lv=**TinAlloy**、lv_mv=**SteelGalvanized**、mv_hv=**Al**、hv_ev=**StainlessSteel**、ev_iv=**Cr**（10040-10044，输入 `V[i+1]`、输出 `V[i]`） |
| 通量电动机 / 通量发电机 | Al / StainlessSteel | **Pb / Invar**（`Flux_T[1..2]`） |
| 太阳能板 | 硅=StainlessSteel、锗=Cr | 硅=**TinAlloy**（`Electric_T[0]`，10050）、锗=**Al**（`Electric_T[2]`，10051） |
| 高级电池方块 `battery_eu_8..2048` | SteelGalvanized/Al/StainlessSteel/Cr/Ti | **TinAlloy/SteelGalvanized/Al/StainlessSteel/Cr** |
| 电加热器 / 电冷却器 | 含 GT6 不存在的 `ulv` 档，材料再高一级 | **删掉 ULV 档**，材料对齐；加热器换算 4:1 → **2:1**（10001：32 EU→16 HU），冷却器输出由 HU 改为 **CU**（10161：32 EU→8 CU，余下按 GT6 作为 HU 浪费） |

同一批还有配方表里的**部件取件**错位：`MachineRecipeIngredients.componentIngredient` 取的是
`IL_TIER_SUFFIX[tier-1]`，于是 **tier 1 要的是 `compact_electric_motor_ulv`**。GT6 的 `IL.*`
数组同样是 ULV-first（`IL.MOTORS[0] = Electric_Motor_ULV`），而机器配方引用的是 `IL.MOTORS[1]`
（`Electric Mixer (LV)`，20351），所以索引必须**等于**等级：改为取 `IL_TIER_SUFFIX[tier]`，
并在原处写明该约定。修复后 LV 机器（electricmixer 等）在 JEI 里要求 `..._motor_lv`。

可见面：能耗设备与机器方块的着色都取自 `spec.material().getColor()`
（`client/GregTechClient.java:288,338`），因此这次修复直接改变了游戏内的外壳颜色与悬浮提示。

### 11.3 接受功率区间（Batch E 的 min/max 项，顺带完成）

GT6 `MultiTileEntityBasicMachine.readFromNBT2:126-128` 的语义是：只给 `NBT_INPUT` 时区间为
`input/2 .. input*2`，`NBT_INPUT_MIN` / `NBT_INPUT_MAX` 各自覆盖。移植版此前在运行时**现算**这两个
边界，忽略了原版显式给出的 10 条记录：

- `tools/generate_machine_params_table.py` 增出 `inputMin` / `inputMax`（缺省即 `input/2 .. input*2`），
  `BasicMachineOriginalParams.Params` 增加 `energyInputMin` / `energyInputMax` 两个分量；
- `BasicMachineSpec` 把原来现算的 `energyInMin()` / `energyInMax()` 改为**记录分量**，`Builder`
  新增 `energyRange(min, max)`，缺省仍按 GT6 语义推导；
- `BasicMachineDefinitions` 与 `MultiblockDefinitions`（蒸馏塔控制器）改用原版值。

实际变化：发酵罐 1..16 → **16..64**；蒸馏塔 / 低温蒸馏塔 256..1024 → **1..1024**；其余不变。

### 11.4 验证

新增 `gametest/VoltageTierAlignmentTests.java`（8 项），期望值全部按 GT6 的 `MT.java` 表写死在测试里，
**不引用移植版自己的常量表**，因此任何方向的错位都会失败：

1. `tieredElectricDevicesUseTheirOwnCasing` —— 电机/发电机/加热器/冷却器/储能柜/电池盒的机壳
   必须是 `Electric_T[索引]`，输入必须是 `V[索引]`；
2. `onlyTransformersAndBatteryBoxesExistAtUln` —— 除变压器与电池盒外不得有任何 `_ulv` 设备；
3. `transformersStepDownOneTier` —— 五档变压器的机壳与 `V[i+1]→V[i]`；
4. `fluxDevicesUseTheFluxTierTable`；
5. `basicMachineCasingsFollowTheTierTable` —— 225 个多等级变体的机壳 = `Electric_T/Heat_T/Kinetic_T[tier]`
   （EU 机器同时校验 `energyIn == V[tier]`）；
6. `machineComponentsUseTheSameTierIndex` —— `IL.*` 取件索引 = 机器等级；
7. `energyRangeMatchesTheOriginalRegistration` —— 区间等于原版记录，且**额定输入必须落在自身区间内**
   （这条不变式正是「机器永远跑不起来」那一类缺陷的守门人）；
8. `noMachineRecipeUsesAUlnComponent` —— 单方块机器配方中不得出现任何 `_ulv` 部件。

门禁：

```
========= 245 GAME TESTS COMPLETE ======================
All 245 required tests passed :)
```

（237 → 244 → 245：其中 244 那次是加入翻译与多方块控制器排除之前的中间态。）

首轮跑挂的两条及处理：`registerednameshavetranslations` —— 新增的 ULV 电池盒缺 `en_us`/`zh_cn`
条目（已补）；`basicmachinecasingsfollowthetiertable` —— 「同机型多等级」判据把**多方块控制器**
（`distillation_tower_main` 与单方块 `distillationtower_stainless_steel` 同名）也算进去了，
而 GT6 的蒸馏塔本来就是 StainlessSteel，已按 `BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS` 排除。

## 12. 续批：淘洗槽（Sluice）路线（Batch A 的 sluice 项）

### 12.1 问题

原版 `Loader_Recipes_Ores:415-420` 为每个矿石材料注册两条淘洗路线，移植版一条都没有，
因此 **Sluice 四个等级 + Large Sluice 共 5 个机器方块永远是空表**。

### 12.2 原版配方（逐字对照）

```java
long[] tSluice = {10000, 300, 300, 300, 300, 300, 300, 300, 300};
for (FluidStack tWater : FL.waters(900))
  RM.Sluice.addRecipe1(T,T,F,F,T, 16, 144, tSluice, tCrushed,     tWater, FL.Sluice.make(900),
      crushedPurified(aMat,1), crushedPurifiedTiny(p[0],9) … crushedPurifiedTiny(p[6],9));   // 8 产出
for (FluidStack tWater : FL.waters(100))
  RM.Sluice.addRecipe1(T,T,F,F,T, 16,  16, tSluice, tCrushedTiny, tWater, FL.Sluice.make(100),
      crushedPurifiedTiny(aMat,1), crushedPurifiedTiny(p[0],1) … crushedPurifiedTiny(p[6],1));
```

- `FL.waters(n)` = **水 / 矿泉水和 / 蒸馏水 / 灵液露**四种水（`FL.java:689`），每种各一条配方；
- `p[i] = i < aMat.mByProducts.size() ? aMat.mByProducts.get(i) : aMat`（第 416 行），
  也就是说**按注册顺序取前 7 个副产物**（与磁力分离机的「非磁性优先」排序不同），不足则回退到材料自身；
- 概率数组 `tSluice` 有 9 项而配方只有 8 个产出，实际生效的是前 8 项：主产物 100 %，7 份细粉各 3 %；
- 输入是**水**、输出是 `FL.Sluice` 流体，所以这是「用水换净化矿」的水力路线。

### 12.3 实现

`loaders/c/Loader_Recipes_OreProcessing` 新增 `processSluiceRoutes()`（在 `processPurifiedRoutes()`
之后调用）与两个小工具方法 `waterVariants(mB)`（按 GT6 的四种水顺序取 `FluidStack`，缺哪种就少一条）
和 `fluidStack(field, mB)`。需要的内容移植版**全部已有**：`RegisteredFluids.Sluice`、
`MaterialPrefix.crushed/crushedTiny/crushedPurified/crushedPurifiedTiny`、四种水。

一处坑：`RegisteredFluids.Water` 用的是 `FluidTextureMode.VANILLA_WATER`，而 `Loader_Fluids`
**会跳过**这两种模式（复用原版流体），所以 `GTFluids.still("Water")` 返回 `null`。
`fluidStack()` 因此对 `VANILLA_WATER`/`VANILLA_LAVA` 直接返回原版水/岩浆的 `FluidStack`
（与既有的 `WATER_MB` 一致），否则四种水只会注册出三种，配方数对不上原版。

### 12.4 结果

| 配方表 | 修复前 | 修复后 |
|---|---|---|
| Sluice | 0 | **3056** |

**Sluice 四个等级 + Large Sluice 恢复可用**（382 个材料 × 2 种形态 × 4 种水）。

### 12.5 验证

`gametest/OrePurificationRoutesTests` 新增 `sluiceWashesCrushedOreIntoPurifiedOre`：概率数组必须是
`{10000,300×7}`、产出必须是 8 个（主产物 + 7 份细粉）、144 tick / 16 EU/t、产出物必须是净化矿、
输入必须是 900 mB 水、**输出流体必须就是注册的 `gregtech:sluice`**、细粉形态走 16 tick / 100 mB、
且同一材料必须有 **4 条**（四种水各一条）。覆盖报告新增 `sluiceRecipes`。

```
========= 246 GAME TESTS COMPLETE ======================
All 246 required tests passed :)
```

### 12.6 仍未完成的 Batch A（阻塞点已核实）

`printer`（需要手册体系）、`scannervisuals`/`autocrafter`/`scannermolecular`/`generifier`
（原版本来就没有可执行配方，**不要补**）、`plantalyzer`/`bumblelyzer`（需要作物与养蜂）、
`cryomixer`（需要 `FL.Cream`/`Tea_*`/冰淇淋等食品流体与物品）、`replicator`（UUM/Biomass 链）。

## 13. 续批：密封木桶发酵 + 原版食品发酵链

### 13.1 先更正我上一轮的判断

第 12.6 节曾写「移植版根本没有 GT6 的木桶」——**这是错的**，指出后复核确认：移植版早已有
完整木桶族（`content/fluid/TankDefinitions` 的 `WOODS`：`wood_barrel`、`wood_barrel_treated`、
`wood_barrel_skyroot`… 共 9 种木质容器，容量 16 000/32 000/64 000 L，另有 22 种金属桶与塑料罐）。
我上一轮用 `grep Barrel`（大小写敏感）只匹配到测试里引用的原版 `BarrelBlockEntity`，
因此漏掉了 `wood_barrel`。教训：查内容是否存在要连小写 ID 一起搜。

### 13.2 原版语义（`gregapi/tileentity/tank/TileEntityBase08Barrel`）

- `mMode & B[1]` = **密封**：软锤切换（`onToolClick2`，空桶时只能解除密封），进度存 `NBT_PROGRESS`；
- 未密封时 `mMode & B[0]` 才是「向上下相邻储罐自流」；密封时木桶改为**发酵**：

```java
if ((mMode & B[1]) != 0) {
  mRecipe = RM.Fermenter.findRecipe(this, mRecipe, T, Long.MAX_VALUE, NI, FL.array(mTank.getFluid()), ST.tag(0));
  if (mRecipe 的进出流体都非气体)
    mMaxSealedTime = divup(max(1, |mEUt * mDuration|) * max(1, mTank.amount()), max(1, mRecipe.mFluidInputs[0].amount));
  if (mSealedTime < mMaxSealedTime) mSealedTime++;
  else mTank.setFluid(FL.mul(mRecipe.mFluidOutputs[0], mTank.amount(), mRecipe.mFluidInputs[0].amount));
}
```

要点：**只按流体匹配**（不消耗物品、不耗电）、**拒收气体**（桶不能承压，所以
`Biomass → Methane` 这条化学配方对桶无效）、耗时与储量成正比、完成时**整桶替换**并按摩尔比缩放。

### 13.3 移植版实现（`TankBlockEntity`）

- `softHammerState` 此前只是个「切换了但没人用」的标志位，现在就是 GT6 的密封位；
  `toggleSoftHammerState()` 按原版语义：空桶 → 解除密封，否则取反，两种情况都清零发酵进度。
- 新增 `tickFermentation`：按流体类型查找 `MachineRecipeMaps.Fermenter` 中首个「进出均非气体」的配方，
  缓存配方 + 记录匹配用的流体（换液即重算）；进度按原版公式走，满进度**下一 tick** 转换（与 GT6 的
  `if (mSealedTime < mMaxSealedTime) ++ else convert` 一致）；完成后 `resetSeal()`。
- `sealedTime` / `maxSealedTime` 写入 NBT（原版也是 `NBT_PROGRESS`），断线/拆装不丢进度。
- 放大镜信息从占位文字 `State: Enabled/Disabled` 改为原版的 `Normal` / `Sealed` /
  `Sealed (t / max)`，并显示正在发酵成什么（`TankTooltips.sendTankInfo`）。

### 13.4 原版食品发酵链（`Loader_Recipes_Food:605-649`）

移植版此前**只有两条化学发酵**（`Biomass → Methane`），所以桶与发酵机几乎没有可做的事。
新增 `content/recipe/FermenterFoodRecipes`，照抄原版：

| 链 | 例 |
|---|---|
| 奶 | `Milk`/`MilkGrC` → `Milk_Spoiled` |
| 蜜酒 | `Honeydew` → `ShortMead` |
| 果汁 → 酒/苹果酒 | `Juice_Apple` → `Cider_Apple`、`Juice_Grape_Red` → `Wine_Grape_Red`、`Juice_Peach` → `Cider_Peach`… |
| 醪 → 酒 | `Mash_Rice` → `Sake`、`Mash_Wheat` → `Whiskey_Scotch`、`Mash_WheatHops` → `Beer`、`Mash_Hops` → `Beer_Dark`、`Juice_Reed` → `Rum_White` |
| 二次陈化/变酸 | `Cider_Apple` → `Vinegar_Apple`、`Whiskey_Scotch` → `Whiskey_GlenMcKenner`、`Beer_Dark` → `Beer_Dragonblood`、`Wine_*` → `Vinegar_Grape` |
| 原版的两组循环（第 646-649 行） | 所有 `WINE` 标记流体 → `Wine_Fortified`；所有 `FRUIT_JUICE` 标记流体 → `Wine_Fruit` |

速率全部照抄：16 EU/t，进 50 L，常规出 25 L / 64 tick，二次加工出 10 L / 128 tick。
两组循环在移植版用 `FluidFlags.WINE` / `FluidFlags.FRUIT_JUICE` 表达（原版用
`FluidsGT.WINE` / `FluidGroups.FRUIT_JUICE` 字符串表）。

踩到的三个坑（都已修）：

1. **`ST.tag(0)` 是必需的物品输入。** 原版每个食品发酵配方的物品输入是
   `ST.tag(0)`，即**电路选择器（Circuit Selector，damage 0）**；移植版 `RecipeMap.validate`
   要求至少一个输入栈（`mMinimalInputItems`），所以纯流体配方会被直接拒绝（静默 skip，
   93 条配方一条都进不了表）。改为与化学转译一致的 `GTTechnological.selectorTag(0)` 后正常。
2. **两组循环必须用 50 L 而不是 1 L。** 我最初为了枚举流体直接用了 `flagged()` 返回的 1 mB 栈，
   于是给每种果汁多注册了一条 1 L 的 `→ Wine_Fruit`；由于 `RecipeMap.mRecipeList` 是
   **HashSet（无序）**，桶的「首个匹配」会随机命中那条 1 L 配方，导致耗时算成 51 200 tick。
   改成 50 L 后，冲突检测会把「已有专属配方」的流体上的组配方丢弃（果汁→苹果酒、酒→醋优先），
   每种输入流体只剩一条配方，桶的查找重新变成确定性的。
3. **`mRecipeList` 是无序集合**（GT6 是有序表）。这是移植版的既有差异，本轮不扩大改动，
   仅记录：`FermenterFoodRecipes` 的组循环已排序以保证注册顺序稳定。

### 13.5 结果

| 项 | 修复前 | 修复后 |
|---|---|---|
| Fermenter 配方 | 2（仅化学） | **72**（化学 2 + 食品 70） |
| 密封木桶 | 标志位无人读取，**完全不发酵** | 按原版时间/比例发酵，进度存盘 |

### 13.6 验证

新增 `gametest/BarrelFermentationTests`（6 项）：

1. `sealedBarrelFermentsItsContents` —— 钢铁桶装 50 L 苹果汁、软锤密封：`sealedDuration` 必须是
   1024 tick；1023 tick 时仍是果汁，满进度那一 tick 尚未转换，再一 tick 后**整桶变 25 L 苹果酒**且进度清零；
2. `fermentationScalesWithTheFillLevel` —— 只装 10 L 时耗时为 `ceil(1024*10/50)=205`，产出 5 L（原版按储量缩放）；
3. `unsealedBarrelDoesNotFerment` —— 不密封 1200 tick 毫无变化（软锤就是开关）；
4. `gaseousFermentationIsRejected` —— `Biomass → Methane` 对桶无效（气体），密封后也不转换；
5. `foodFermentationChainIsRegistered` —— 13 组关键配对（奶/蜜酒/苹果酒/醋/葡萄酒/清酒/威士忌/啤酒/朗姆/
   加强酒/果酒）必须在表内，且 `missingFluids()` 为空；
6. `foodChainUsesTheOriginalRates` —— 16 EU/t、50 L 进、64 tick 出 25 L，二次加工 128 tick 出 10 L。

```
========= 252 GAME TESTS COMPLETE ======================
All 252 required tests passed :)
```

（246 → 252。）

## 14. 续批：坩埚的温度要求与「铁必须走坩埚」（反馈）

### 14.1 问题（反馈）

> 目前坩埚配方没有原版的温度等要求，没有铁变锻铁的配方。
> gt6 原版铁矿不能直接烧成铁锭，必须走坩埚。

先做了一次**运行时诊断**（临时 GameTest 落盘 `docs/crucible-chain-diagnostics.json`），确认了三件事，然后再动手：

| 项 | 诊断结果 |
|---|---|
| `RM.CrucibleSmelting`（坩埚熔炼表） | **0 条** —— JEI 的「Crucible Smelting」页签完全空白 |
| `RM.CrucibleAlloying`（组合熔炼表） | 159 条 fake 配方，`mSpecialValue` 已带温度，但 JEI **不显示**（移植版 `RecipeMap` 没有原版的 `aNEISpecialValuePre/Multiplier/Post`） |
| 熔炉表 | `minecraft:iron_ore → gregtech:dust_iron`、`ore_raw_magnetite → **minecraft:iron_ingot**`……**铁系金属照样能从熔炉里烧出来** |

对照原版：
- `RM.CrucibleSmelting` 是 `RecipeMapCrucible`：不存配方，而是**按物品的材料数据现算**——
  输入形态 → `OM.ingotOrDust(mTargetSmelting)`，特殊值（温度）= **输入材料的熔点**；
  `getNEIRecipes` 把这些形态列进配方查看器。原版把熔炼坩埚同时注册到两张表
  （`Loader_MultiTileEntities:294` 的 `mRecipeMachineList.addAll`），所以「坩埚熔炼」页签就是
  玩家查**某材料在多少 K 熔化、熔成什么**的地方。
- `MT.java:1724` `PigIron … .setSmelting(WroughtIron, U)`：**猪铁 → 锻铁**，温度 2011 K；
  这是原版铁系里锻铁的唯一来路（`MT.java:3348` 再 `WroughtIron + Air → Steel`）。
- `MT.java:414` `Fe` 带 `NEVER_FURNACE`；`Loader_Recipes_Furnace:122-130` 会把**任何产出材料带
  该标记的原版熔炉配方**改成产出 `scrapGt`（「Unsmelt things that really do not belong in Furnace
  Recipes」）。所以原版里铁矿进熔炉只能得到**铁屑**，要铁锭必须走坩埚。

### 14.2 修复

**A. 坩埚熔炼表（新增 `content/recipe/CrucibleSmeltingRecipes`）**
按原版 `RecipeMapCrucible#getRecipeFor` / `#getNEIRecipes` 生成 **1343 条**显示配方：
对每个带 `MELTING` 的材料，把它的各个形态（dust / crushed / crushedPurified / crushedCentrifuged /
oreRaw；当输入材料不是自身熔炼目标时再加 ingot / gem）→ `OM.ingotOrDust(mTargetSmelting, 转换量)`，
温度 = 该材料熔点。数量与换算照抄原版（`UT.Code.units(inputAmount, U, targetAmount, false)`）。
移植版没有的形态（blockIngot/blockGem/blockDust/chunk/rubble/pebbles/cluster/gravel/crystalline/reduced）
不伪造，缺的记进 `missingContent()`。**这些配方是 `addFakeRecipe`（只展示）**：真正的转化由
`SmeltingCrucibleBlockEntity` 在达到熔点后本地完成，原版的方块实体同样从不查这张表——若把它们
做成可执行配方，机器就会「不加温凭空熔化」。

**B. JEI 温度显示**
`RecipeMap` 补上原版的 `aNEISpecialValuePre / aNEISpecialValueMultiplier / aNEISpecialValuePost`
（`specialValueLabel(...)`），两张坩埚表设为 `"Temperature: " / 1 / " K"`（与 `RM.java:128-129` 一致），
`RecipeMapCategory` 在配方页画出「Temperature: 1811 K」；新增 lang 键
`gregtech.jei.special_value`（en/zh）。

**C. `NEVER_FURNACE` 熔炉闸门**
- `MaterialProperty` 新增 `NEVER_FURNACE`（原版 `TD.Processing.NEVER_FURNACE`），
  `GTMaterialRegistry.postInit` 按 `MT.java` 给 **Iron / WroughtIron / Steel / Titanium / Tungsten**
  打上标记；
- `MachineRecipeMaps.add_smelting(...)` 统一走新闸门：产出材料的标记命中时，输出改为该材料的
  `scrapGt`（数量照原版 `(materialAmount × count) / scrapAmount`），并新增 `neverFurnaceOutput()` /
  `isNeverFurnace()` 供其它调用点复用；
- `Loader_OvenRecipes` 除镜像原版熔炉配方外，现在还**改写原版配方本身**：
  `RecipeManager.replaceRecipes(...)` 把结果材料带 `NEVER_FURNACE` 的熔炼/高炉配方改成铁屑
  （GT6 `Loader_Recipes_Furnace` 的等价实现）。运行日志：
  `Converted 133 vanilla smelting recipes to GT6 oven recipes; 2 un-smelted`。

### 14.3 结果

| 项 | 修复前 | 修复后 |
|---|---|---|
| Crucible Smelting 配方 | 0 | **1343**（全部带温度，`Pig Iron → Wrought Iron @2011 K` 在列） |
| JEI 坩埚页签温度 | 不显示 | 「Temperature: N K」 |
| 熔炉烧铁矿 | 出铁粉/铁锭 | **出铁屑**，铁系金属一律不能从熔炉得到 |
| 原版熔炉（vanilla） | `iron_ore → iron_ingot` | 配方被改写为铁屑 |

### 14.4 验证

新增 `gametest/CrucibleTemperatureTests`（6 项）：

1. `crucibleSmeltingRecipesCarryTemperatures` —— 表非空、**每条都有温度**、全部是 fake（不可执行）、
   两张坩埚表都声明了温度标签；
2. `pigIronMeltsIntoWroughtIron` —— `PigIron.targetSmelting == WroughtIron`、表里有
   `Pig Iron → Wrought Iron` 且温度 = 猪铁熔点（2011 K）；同时验证坩埚的 Steel 反应以锻铁为原料；
3. `wroughtIronIsReachableThroughTheSmelter` —— 完整闭环：Melter 把铁锭/铁粉熔成熔融铁，
   Smelter 再 `molten.iron → molten.wroughtiron`（16 EU/t、16 tick，照抄 `Loader_Recipes_Alloys:35`）；
4. `neverFurnaceMetalsCannotBeSmeltedInAFurnace` —— 五种金属带标记；GT 熔炉表里**不存在**任何产出
   NEVER_FURNACE 金属的配方（只允许 scrapGt，且 scrap 产出 > 20 条）；原版熔炉的铁矿/粗铁配方
   结果不再是铁；铁屑物品存在；
5. `unSmeltingKeepsOtherRecipes` —— 改写没有误删：原版熔炼配方仍 > 100 条，铜矿依然可烧；
6. `coverageIsReported` —— 落盘 `docs/crucible-coverage.json`。

```
========= 258 GAME TESTS COMPLETE ======================
All 258 required tests passed :)
```

（252 → 258。）

### 14.5 本轮记录但未修的既有差异

诊断时发现熔炉表里有一条 **`Bauxite Raw Ore → Iron …`**（修复前是铁锭、现在是铁屑）：
Bauxite 自身的 `targetSmelting` 是它自己（报告里 `Bauxite melting 2800 K → Bauxite`），
所以这条配对来自**输入物品解析**——某个材料（铁系）的 `oreRaw` 形态取到了同一个「Bauxite Raw Ore」
物品实例，属于物品注册/形态解析的既有问题，与本轮的温度闸门无关；本轮只保证它不再产出铁金属。
另：Bauxite 的熔点被导入为 2800 K（与原版 `BLACKLISTED_SMELTER` + 无熔点的设定不符），
也一并记在这里待后续核对材料导入表。

## 15. 续批：把配方多移植一批（转译器从 1 个 GT6 加载器扩到 5 个）

### 15.1 起点与目标

此前只有 `Loader_Recipes_Chem` 被转译（303 条注册）。按原版各加载器的注册点数量排了一下，
最大的坑依次是：**`Loader_Recipes_Other` 583 条**、`Loader_Recipes_Potions` 352 条、
`Loader_Recipes_Food` 304 条、`Loader_Recipes_Extruder` 175 条——这四个都还没动过。

做法：把 `tools/transpile_gt6_chem.py` 从「单文件」改成「按集合生成」，五个集合共用同一套
运行时解析器（`GTGeneratedChem`），缺内容一律跳过并记账，绝不猜。

### 15.2 转译器补的七处解析能力（括号内为本轮解锁量）

| 修复 | 说明 |
|---|---|
| **前置布尔参数** | 原版原始重载形如 `addRecipe2(T, F, F, F, T, 16, 64, 入1, 入2, 出)`；此前解析器只吃「第一个布尔后紧跟数字」，导致**整个 Extruder 表 175 条全军覆没** |
| **按重载元数切输入/输出** | `addRecipe1` = 1 个物品输入、`addRecipe2` = 2 个……其余物品一律是输出；纯物品配方（无流体）此前会被判「没有输出」而丢弃 |
| **`ST.array(a, b, c)`** | 展开为多个输入（如 `ST.array(OP.dust.mat(MT.As,1), OP.dustSmall..., OP.dustTiny...)`） |
| **`ST.amount(n, x)`** | 重复 n 份；**n = 0 是原版的「催化剂」写法**，保留为 `:0` 并由 `withCatalystInputs` 标记 |
| **循环展开 `for (ItemStack tStack : ST.array(...))`** | 逐元素复制循环体并代入变量；同时把循环头的迭代式解析改成**括号配平扫描**——旧正则 `[^){]+?` 一遇到 `)` 就断，这正是 236 条药水蒸馏配方（含全部 `tStack` 循环）一条都没转出来的原因 |
| **`L` 常量** | 原版 `CS.java:129` `L = 144`（一个材料单位的流体量），于是 `FL.Latex.make(L)`、`FL.Latex.make(L/2)` 可算 |
| **前缀表改为「以移植版为准」** | `KNOWN_PREFIXES` 现在直接从 `MaterialPrefix.java` 读字段，自动覆盖 `chunkGt`/`bouleGt`/`billet`/`plateCurved`/`toolHeadRaw*`/`gemLegendary` 等；移植版确实没有的 `blockIngot`/`blockGem`/`blockDust`/`rawOreChunk` 仍照常发出，由运行时记为「已接受的缺失内容」 |
| **`IL.*` 物品映射** | `IL.Compound_<合金>` → 移植版锭；`IL.Shape_Extruder_*` / `IL.Shape_SimpleEx_*` / `IL.Shape_Slicer_*` → `extruder_shape_*` / `low_heat_extruder_shape_*` / `slicer_shape_*`（含 `Plate_Curved→curvedplate`、`Gear_Small→smallgear`、`Shovel→shovelhead` 等词序归一） |

运行时侧（`GTGeneratedChem`）新增 `tech:<物品id>` 规格（按 `GTTechnological` 查物品）、
把 `:0` 催化剂标记扩展到 `tech:`/`v:`/`cell:`，并新增**逐集合统计** `setStats()`。

### 15.3 一个必须记录的坑：不要把 Fusion 重复转译

`Loader_Recipes_Other` 里也有 18 条 `RM.Fusion.addRecipe`。第一次生成时它把
`FusionRegressionTests` 打挂了——移植版的 `Loader_Recipes_Fusion` 已经按原版数值精确注册了这 18 条
反应（`mSpecialValue`、EU/t、流体量逐项被断言），而转译副本先入场、把正本挤成了碰撞丢弃项。
现在 `PORT_MAPS` 显式减去 `Fusion`（注释写明原因）：**凡是已有专属、被断言过的加载器的表，不再由
通用转译器重复注册**。

### 15.4 结果

| 集合（GT6 加载器） | 转译出的调用 | 运行时**注册成功** | 跳过（缺内容） |
|---|---|---|---|
| chem（`Loader_Recipes_Chem`） | 358 / 402 | 303 | 55 |
| other（`Loader_Recipes_Other`） | 250 / 597 | 105 | 145 |
| **potions（`Loader_Recipes_Potions`）** | 500 / 655 | **480** | 20 |
| food（`Loader_Recipes_Food`） | 243 / 359 | 156 | 87 |
| extruder（`Loader_Recipes_Extruder`） | 142 / 175 | 24 | 118 |
| **合计** | **1493** | **1068**（原本 303） | 425 |

受影响机器表的规模（`docs/transpiled-recipes-coverage.json`，本轮新增量最大的是前几项）：

| 表 | 条数 | 表 | 条数 | 表 | 条数 |
|---|---|---|---|---|---|
| Extruder | 3701 | Loom | 1052 | Bath | 811 |
| Mixer | 672 | Centrifuge | 663 | Distillery | **413**（原 0） |
| ImplosionCompressor | **408** | Smelter | 325 | Melter | 323 |
| Fermenter | 154 | CokeOven | 40→**40** | CrystallisationCrucible | 48 |
| Mortar | **60**（原 0） | Squeezer | 53 | Nanofab | 45 |
| Drying | 43 | Press | 42 | Juicer | **40** |

### 15.5 仍然缺的内容（209 个 token，全部落在记录里）

`docs/chem-missing-content.json` 与运行日志都会列出，主要是四类：
**跨模组物品**（`tech:module_*_generator` 90 次、EtFu 食物、GT6 绳索）、
**移植版没有的化学中间体流体**（`m:UF4`、`m:AquaRegia`、各种 `*Vitriol`）、
**移植版没有的物品形态**（`i:blockIngot:*`、`i:chunkGt:MeatRaw` 等）、
以及少量拼写差异（`v:speckled_melon`、`v:fish` 这类 1.7.10 名字）。

**Extruder 表特例**：142 条里 90 条需要 `IL.Module_*_Generator`（原版「发电机模块」，
作为配方钥匙的 0 数量催化剂），移植版没有该物品，因此只有 24 条落地——
这条依赖关系已写进测试断言（要么注册成功，要么缺失清单里必须存在 `tech:module_` 前缀），
不会变成「悄悄不生效」。

### 15.6 验证

`GeneratedChemistryTests` 改造：

- `transpiledCoverageDoesNotRegress` —— 注册总量 ≥ 1000；五个集合都在场且 chem/other/potions/food
  各自**必须有注册**；extruder 允许为 0，但必须由「缺失清单里存在 `tech:module_`」解释；
  **chem 集合单独保留 ≤ 60 的跳过预算**（它跳过的都是化学专用中间体，不该增长）；
- 新增 `perLoaderCoverageIsReported` —— 落盘 `docs/transpiled-recipes-coverage.json`（逐集合 + 逐表）。

```
========= 259 GAME TESTS COMPLETE ======================
All 259 required tests passed :)
```

（258 → 259；Fusion 18 条、融合回归与其余断言全部保持通过。）

复现：`python tools/transpile_gt6_chem.py {chem|other|potions|food|extruder}`，
`python tools/diagnose_chem_transpile.py {集合}`（逐条报告未转译原因，用于继续补解析能力）。

## 16. 续批：配方再扩一批（原版物品名映射 + 两个新加载器）

### 16.1 本轮拿到的东西

上一轮末我列的下一步有三类，这轮做掉了前两类的主要部分：

**A. 1.7.10 → 1.20.1 原版物品名与「变体/染色」映射**（原版 GT6 用 1.7.10 注册名 + damage 值编码颜色/树种，
直接当现代 id 用会整条配方失效）：

- **改名表**（Items/Blocks 分开，因为同一个词在两边的现代 id 不同）：
  `Items.speckled_melon→glistering_melon_slice`、`Items.fish→cod`、`Items.reeds→sugar_cane`、
  `Items.netherbrick→nether_brick`、`Blocks.stonebrick→stone_bricks`、`Blocks.nether_brick→nether_bricks`、
  `Blocks.brick_block→bricks`、`Blocks.grass→grass_block`、`Blocks.waterlily→lily_pad`、
  `Blocks.melon_block→melon`、`Blocks.web→cobweb`、`Blocks.hardened_clay→terracotta` 等；
- **变体表**（damage 值 → 现代 id）：`dye`（16 色，**注意原版 dye 的 damage 顺序是黑→白**）、
  `wool/carpet/stained_glass/stained_glass_pane/stained_hardened_clay`（16 色，方块顺序是白→黑）、
  `planks/log/sapling/leaves`（6 树种）、`log2`/`leaves2`（只有金合欢/深色橡木）、
  `stone_slab`（8 种台阶）、`double_stone_slab`（**双台阶是整方块**，因此映射到 `smooth_stone/sandstone/oak_planks/…`
  而不是台阶）、`stone`（7 种）、`red_flower`（9 种）、`double_plant`（6 种）、`coal`（煤/木炭）、`fish`（4 种）；
- **GT6 的 `W`（32767 = 任意 damage）**：语义是「任一变体」，而机器配方一个槽位只能放一个物品——所以
  实现为**整条配方按变体扇出**（笛卡尔积，上限 32 组，超限只取每种变体的第一个并在文档记录），
  这样玩家能用任意颜色羊毛，与原版一致。

这一步之后 `Loader_Recipes_Extruder` 的形态物品也全部对上号（`IL.Shape_Extruder_Plate_Curved` →
`extruder_shape_curvedplate`、`IL.Shape_SimpleEx_*` → `low_heat_extruder_shape_*`），注册量 **24 → 112**。

**B. 新接入两个加载器**：

| 集合 | 说明 |
|---|---|
| `vanilla`（`Loader_Recipes_Vanilla` 235 个 RM 调用） | 只转译**机器配方**部分；该文件另有 346 个 `CR.*`（工作台）调用，属于移植版手写的 `VanillaProcessingRecipes` 等集合，不在转译范围 |
| `temporary`（`Loader_Recipes_Temporary` 90） | 原版临时代码里的配方（Bath/Squeezer/Juicer/Injector/Boxinator…） |

**C. 明确「转译不了、且不该硬转」的三个表**（写进生成器注释，避免以后有人「补」出假配方）：

- `Loader_Recipes_OreDict`（74）与 `Loader_Recipes_Crops`（133）：**每一条配方都来自原版的矿物词典事件回调**
  （`aEvent.mStack` 由事件逐物品提供）。移植版没有等价的事件层，所以这不是解析问题，而是结构性缺口；
- `Loader_Recipes_Woods`（71）：由 GT6 自己的 `BlocksGT.Log1/Log1FireProof` 方块与一张 `aEntry` 表驱动，
  移植版没有那套方块。

### 16.2 结果

| 集合 | 转译出的调用 → 生成的配方 | 运行时**注册** | 跳过 |
|---|---|---|---|
| chem | 358 / 402 → 358 | 303 | 55 |
| other | 250 / 597 → 265 | 123 | 142 |
| potions | 500 / 655 → 500 | 485 | 15 |
| food | 243 / 359 → 243 | 156 | 87 |
| extruder | 142 / 175 → **322** | **112**（原 24） | 210 |
| **vanilla（新）** | 135 / 235 → 228 | 49 | 179 |
| **temporary（新）** | 34 / 90 → 34 | 14 | 20 |
| **合计** | — | **1242**（本轮 +174，两轮累计 303→1242） | 708 |

表规模变化（`docs/transpiled-recipes-coverage.json`）：Extruder 3701→**3791**、Bath 811→**831**、
Mixer 672→**679**、Press 42→**56**、Injector 13→**25**、Distillery 413→**418**。

`vanilla` 只落地 49 条的原因也在记录里：该文件其余调用引用 IC2/Forestry 的内容，
以及移植版没有的材料形态（缺内容清单 202 个 token，其中 `i:` 110、`tech:` 46、`m:` 25、`omd:` 11）。

### 16.3 验证

`GeneratedChemistryTests` 的集合断言更新为七个集合：chem/other/potions/food/**temporary** 各自必须有注册；
extruder 允许为 0 但必须由缺失清单里的 `tech:module_` 解释（现在是 112 条已注册）；
**vanilla 至少 40 条**——这条专门守名字映射，映射一坏整组归零。逐集合与逐表报告继续落盘。

```
========= 259 GAME TESTS COMPLETE ======================
All 259 required tests passed :)
```

复现：`python tools/transpile_gt6_chem.py {chem|other|potions|food|extruder|vanilla|temporary}`。

## 17. 续批：注册顺序修正 + 矿处理加载器

### 17.1 先修一个我上一轮引入的隐患：注册顺序

`RecipeMap.addRecipe` 的碰撞检查会**保留先注册的那条**。上一轮把批量转译塞进了
`Loader_Recipes_Chem.run()` 的末尾——它在 phase C 里排在 `Loader_Recipes_Alloys/Decomp/Fuels`
之后，但**排在 `Loader_Recipes_Fusion`、`Loader_Recipes_Matter` 与所有 `content/recipe/*Recipes` 手写集合之前**
（后者都在 76–105 行）。也就是说转译数据有机会抢占碰撞位、把**逐条核对过的手写配方**挤掉。

修正：把 `GTGeneratedChem.loadAll()` 从 `Loader_Recipes_Chem` 里移出，作为 **phase C 的最后一步**执行
（`GregTech#commonSetup`，注释写明理由）。这样批量转译只能**填补空表**，永远不会取代
任何手写配方。副作用立刻可见且是正确方向的：

| 集合 | 修正前 | 修正后 |
|---|---|---|
| other（含 Massfab/Replicator，与 `Loader_Recipes_Matter` 重叠） | 123 | **103** |
| vanilla | 49 | **46** |

少掉的 23 条正是被 `Loader_Recipes_Matter` 等手写集合正确接管的部分。

### 17.2 新接入 `Loader_Recipes_Ores`（280 个调用）

原版这个文件是矿处理的**手写主表**（Sifting 87、Bath 84、Centrifuge 61、MagneticSeparator 18…）。
移植版此前用 `Loader_Recipes_OreProcessing` 手写覆盖了其中一部分（洗矿、净化矿两条下游、
淘洗槽），因此本轮转译的 138 条里大部分会与手写版碰撞——**这正是想要的**：碰撞检查会把重复的丢掉，
只留下手写版没有的。

结果：`ores` 集合 **77 条落地**（61 条缺内容），净增 **+54** 条，主要落在
**Centrifuge 663 → 701**、**Bath 831 → 837**、**Mixer 679 → 689**、**Nanofab 45 → 49**、**Drying 43 → 46**。

### 17.3 累计（八轮以来的配方线）

| 集合 | 生成配方 | 注册 | 跳过 |
|---|---|---|---|
| chem | 358 | 303 | 55 |
| other | 265 | 103 | 162 |
| potions | 500 | 485 | 15 |
| food | 243 | 156 | 87 |
| extruder | 322 | 112 | 210 |
| vanilla | 228 | 46 | 182 |
| temporary | 34 | 14 | 20 |
| **ores（新）** | 138 | 77 | 61 |
| **合计** | **2088** | **1296** | 792 |

（本轮 1242 → 1296。转译线三轮累计：**303 → 1068 → 1242 → 1296**。）

### 17.4 验证

集合数断言更新为八个，chem/other/potions/food/temporary/**ores** 各自必须有注册；
extruder 仍需由 `tech:module_` 缺失解释（现 112 条）；vanilla ≥ 40（守名字映射）。
逐集合与逐表报告继续落盘 `docs/transpiled-recipes-coverage.json`。

```
========= 259 GAME TESTS COMPLETE ======================
All 259 required tests passed :)
```

### 17.5 下一步的两个结构性缺口（本轮确认，不是解析问题）

1. **原版矿物词典事件层**：`Loader_Recipes_OreDict`(74) 与 `Loader_Recipes_Crops`(133) 的每条配方都写在
   `addListener("<矿物词典名>", …)` 里，输入是 `aEvent.mStack`——原版为**每个注册到该词典名的物品**各注册一次。
   移植版要做等价物，需要一个「按标签逐物品展开配方」的运行时能力（类似本轮新加的
   变体扇出，但数据源改为 Forge 标签），以及一张 GT6 词典名 → 移植版标签的映射表。
   本轮已把这两条从生成器里**移除并注释原因**，避免有人硬补假配方。
2. **「配方钥匙」物品**：`IL.Module_*_Generator`（Extruder 210 条卡在这里）、`IL.Shape_Press_*`、
   `IL.Rope_*`、EtFu 食物——原版拿它们作 0 数量催化剂。补这些物品需要方块/物品本体 + 贴图，
   属于内容而非配方。

## 18. 续批：补内容（材料形态与流体），并按原版形态组判定「缺口 vs 原版空操作」

之前几轮把「转译时缺的内容」都记成了 236 个 token，但**其中一部分根本不是移植版的缺口**——
原版自己那些配方也是空操作。本轮先用原版的**形态组表**（`gregapi/data/TD.java`）把它们分开，
再补真正的缺口。

### 18.1 判定依据：原版的形态组

```
G_INGOT_MACHINE      = {PROJECTILES, DUSTS, PLANTS, PLATES, STICKS, ARMORS, INGOTS, INGOTS_HOT,
                        MULTIINGOTS, DENSEPLATES, MULTIPLATES, FOILS, PARTS}     // TD.java:609
G_GEM                = {PROJECTILES, DUSTS, PLANTS, PLATES, STICKS, GEMS, ARMORS} // TD.java:605
G_QUARTZ             = {PROJECTILES, DUSTS, PLANTS, PLATES, STICKS, GEMS}         // TD.java:604
G_STONE              = {PROJECTILES, DUSTS, PLANTS, PLATES, STICKS}               // TD.java:605
metalnd/alloymachnd  = 「无粉」家族（WroughtIron/AnnealedCopper/IronCompressed/CastIron）
```
用 `tools/rank_missing_content.py`（本轮新增）把缺失 token 按**被阻塞的配方条数**排序后，
可以逐条判定它属于哪一类。

### 18.2 补掉的真缺口（都有原版出处）

| 缺口 | 原版依据 | 处理 |
|---|---|---|
| **合金没有粉**（青铜粉、钢粉、黄铜粉、殷钢粉…） | `TD.G_INGOT_MACHINE` 含 `DUSTS`（TD.java:609），而移植版 `MaterialFactories.alloy()` 漏了这一项 | 工厂补上 `DUST/GENERATE_DUST`，并在 `MaterialFormCorrections` 里对**所有**带锭形态的金属/合金做一次兜底扫描（排除原版 `*nd` 家族） |
| **石英族没有宝石形态**（下界石英、赛特斯石英、充能赛特斯石英、流明） | 原版 `quartz()` 用 `G_QUARTZ`（含 `GEMS`） | 四个材料补 `GEM + PLATE/STICKS/PLANT/PROJECTILE` |
| **酸/矾类没有液态**（王水、氯金酸、氯铂酸、氯化锡、九种矾） | 原版 `lqudacid*` 工厂（`AquaRegia` 9827 等）给它们 `LIQUID` | 在 `FluidDefinitions` 里补 `LIQUID`（并**移到 molten/gas/liquid 生成之前**，否则标志来不及生效） |
| **硫没有熔融态** | 原版 `sulfur()` 带 `MELTING + MOLTEN`（MT.java:160） | 补 `MOLTEN` → 生成 `molten.Sulfur` |
| **咖啡/巧克力奶等 7 种原版药水流体缺失** | 原版 `Loader_Recipes_Food` 的咖啡链（`FL.make_("potion.coffee")` 等） | 在 `RegisteredFluids` 补 7 个条目（含中英文本地化） |

### 18.3 判定为「原版同样是空操作，**不该补**」的部分

| 缺失 token | 判定 |
|---|---|
| `i:gearGt:Stone`、`i:gearGtSmall:Stone`、`i:toolHead*:Stone`（合计 144 条） | GT6 的石头族用 `G_STONE`＝只含 `DUSTS/PLATES/STICKS/PLANTS/PROJECTILES`，**没有齿轮与工具头**；原版这些挤出配方构造出的物品本就是 null → 原版也不生效。移植版按「解析不到就跳过」处理，行为一致 |
| `i:chunkGt:CoalCoke`、`i:chunkGt:LigniteCoke`、`i:billet:*`（合计 60+ 条） | 原版所有形态组里**都不含 CHUNKS**（全仓仅 3 处提及），这些调用在原版同样得到 null |
| `m:CaCl2 / MgCl2 / MnCl2 / Na2CO3 / MgCO3 / CaF2 / Al2O3 / Fe2O3 / UF4 / U235F4 / U238F4` | 原版这些是 `dustdcmp/oredustdcmp/fluorite` 的**纯粉材料**，没有液态；调用 `.liquid()` 得到的也是 null |

### 18.4 结果

| 指标 | 本轮前 | 本轮后 |
|---|---|---|
| 材料物品注册 | 43101 | **43677**（+576：合金粉、石英宝石及其板/棒/弹丸） |
| 转译配方注册 | 1296 | **1362** |
| 被阻塞的配方 | 792 | **726** |
| 缺失 token | 236 | **198**（其中 38 个是本轮补齐的） |

本轮补齐的 28 个 token（`i:dust:*` 合金粉、`m:` 酸/矾/硫、`omd:*`）+ 后续石英族/咖啡流体，
合计让 **66 条**此前无法注册的配方落地。

### 18.5 验证

```
========= 259 GAME TESTS COMPLETE ======================
All 259 required tests passed :)
```

新增物品全部通过 `registerednameshavetranslations`（7 个新流体已补 en_us/zh_cn），
材料物品数、转译配方数、缺失 token 数都由既有测试与报告持续跟踪
（`docs/missing-content-tokens.txt`、`docs/transpiled-recipes-coverage.json`）。

### 18.6 剩余缺口的分类（198 个 token）

| 类别 | 数量级 | 性质 |
|---|---|---|
| `tech:*`（`module_*_generator` 89 条、EtFu 食物、绳索、压弹壳模具） | 68 token / 213 条 | **真缺口**：需要物品本体 + 贴图 |
| `i:` 各种材料形态（多为原版空操作，见 18.3） | 112 token / 380 条 | 多数为原版空操作；剩下的是零散材料形态 |
| `m:` / `fr:` / `f:` 流体 | 21 token | 零散；`f:Lava`/`f:Lava_Pahoe` 之类是原版岩浆别名 |
| `omd:` 粉 | 7 token | 零散材料 |

## 19. 续批：材料形态的堆叠换算（问题）+ 逐表缺口报告

### 19.1 问题与实测答案（本轮之前）

> 你有添加粉、粉块、小堆小搓粉的转换 锭块 板块转换吗

**实测回答：一条都没有。** 六组方块前缀（`blockRaw` / `blockDust` / `blockGem` / `blockIngot` /
`blockPlate` / `blockPlateGem`）在移植版早就注册了（`api/prefix/BlockMaterialPrefix.java`，随材料块一起进
43677 个材料物品），但**方块形态与物品形态之间没有任何配方**：全仓唯一的压缩类配方是
`content/recipe/StructuralPartRecipes.java:32-35` 手写的 **Adamantium 9 板 → 致密板**一条。
后果是玩家在 JEI 里看不到「9 粉 → 粉块」，也没有任何机器能把粉块拆回 9 粉——`Boxinator`/
`Unboxinator`/`Compressor` 三张表此前只有线材、车辆、药品等零星手写条目。

### 19.2 原版的注册点：不是逐条写，而是一张通用表

GT6 用通用前缀处理器（`RecipeMapHandlerPrefix`）**按材料逐个生成**，所以移植版要抄的是表，不是 2 万条配方：

| 原版位置 | 表 | 内容（左→右为 GT6 写法） |
|---|---|---|
| `Loader_Recipes_Handlers:217-233` | `RM.Compressor` | `dust 1 → plateGem 1`、`plate 9 → plateDense 1`、`plateTriple 3 → plateDense 1`、`blockPlate 1 → plateDense 1`、`blockSolid 1 → plateDense 1`、`ingot 1 → compressed 1`、`billet 1 → plateSteamcraft 1` |
| `:451-489` | `RM.Unboxinator` | `blockIngot 1 → ingot 9`、`blockDust 1 → dust 9`、`blockGem/blockPlate/blockPlateGem/blockRaw` 同理、`ingot 1 → nugget 9`、`billet 1 → nugget 6`、`dust 1 → dustTiny 9`、`dustSmall 1 → dustDiv72 18`、`dustTiny 1 → dustDiv72 8`、`crushed/crushedPurified/crushedCentrifuged 1 → *Tiny 9` |
| `:492-550` | `RM.Boxinator` | 反方向，且**装箱要电路**（`ST.tag(n)`）：`ingot 9 + tag9 → blockIngot 1`、`dust 9 + tag9 → blockDust 1`、`gem 9 → blockGem 1`、`plate 9 → blockPlate 1`、`plateGem 9 → blockPlateGem 1`、`oreRaw 9 → blockRaw 1`、`chunkGt 36 → blockIngot 1`、`billet 27 → blockIngot 2`、`dustTiny 9 → dust 1`、`dustSmall 4 → dust 1`、`dustDiv72 8/18 → dustTiny/dustSmall`、`nugget 9 → ingot 1`、`chunkGt 4 → ingot 1`、`billet 3/6/9 → ingot 2/4/6`、`*Tiny 9 → crushed* 1` |
| `:552-625` | 工作台（`AdvancedCraftingXToY` / `AdvancedCrafting1ToY`） | 同一批换算的手工版：9 锭 ↔ 锭块、9 粉 ↔ 粉块、9 宝石 ↔ 宝石块、9 板 ↔ 板块、9 宝石板 ↔ 宝石板块、9 矿 → 矿块、9 小堆 → 粉、4 小堆 → 粉、9 粒 → 锭、4 碎块 → 锭、8 碎粉粒 → 小堆… |

时长与能耗也照抄原版：这些处理器一律 **16 EU/t**；装箱/拆箱固定 16 tick，压缩机走
`RecipeMapHandlerPrefix#getCosts()`＝`ceil(材料单位 / U × 倍率 × (1 + toolQuality))`，其中倍率 256 那一支
只作用于**难加工**材料，易加工材料（原版 `:58` `Or tEasyWorkable = new Or<>(FURNACE, SOFT)`）走固定的
16（`dust→plateGem`）与 144（9 单位 → 致密板）tick。

### 19.3 移植版没有 SOFT/FURNACE 材料属性 → 新增一张生成式形态表

移植版的 `MaterialProperty` 只有 `SMITHABLE` / `FLAMMABLE` 之类，没有 `SOFT`/`FURNACE`，而这两个标志决定
压缩机走固定时长还是按品质放大（差 1–2 个数量级）。**不靠猜**：新增 `tools/extract_gt6_workability.py`，
按 `MT.java` 的声明逐条抽取这两个标志，生成
`data/generated/MaterialWorkability.java`（SOFT 37 + FURNACE 56，并集 **76** 个易加工材料）与
`docs/material-workability.json`。名字用「小写去非字母数字」归一化匹配（原版内部符号 `copper`/`aluminium`
与移植版注册名 `Copper`/`Aluminium` 不同源），匹配不上只会落到难加工分支，属保守失败。

### 19.4 实现

| 文件 | 作用 |
|---|---|
| `content/recipe/MaterialFormConversionRecipes.java`（新） | 机器侧三张表，phase C 中排在**所有手写加载器之后、批量转译之前**（沿用第 17 节的顺序约定：碰撞检查保留先注册者，此表可以填表但不能挤掉手写配方） |
| `loaders/Loader_FormConversionCraftingRecipes.java`（新） | 工作台侧，按第 9 节同样的办法在 `ServerStartedEvent` 用 `RecipeManager.replaceRecipes(...)` 注入；每材料一条 `ShapelessRecipe` |
| `data/generated/MaterialWorkability.java`（生成） | 19.3 的标志表 |
| `tools/extract_gt6_workability.py`（新） | 复现命令：`python tools/extract_gt6_workability.py` |
| `api/prefix/PrefixRegistry.java` / `BlockPrefixRegistry.java` | 新增 `byName(...)`，让配方表按**原版前缀名**书写（`"blockIngot"`、`"dustSmall"`），解析不到就跳过 |

原版有、移植版表达不了的形态**一律记账不猜**（`MaterialFormConversionRecipes.skipped()` /
`Loader_FormConversionCraftingRecipes.skipped()`）：`compressed`（锭压块）、`plateSteamcraft`、
`casingSmall`、`rawOreChunk` 四种前缀移植版没有（`plateTiny`/`plateGemTiny` 已有，卡住的是 `casingSmall`）；
另有 4 对**同输入不同产出**的配方
（`ingot 1 → chunkGt 4` vs `ingot 1 → nugget 9` 等）只注册原版先列的那一条——原版靠有序配方表决定，
而 1.20.1 的配方表是无序 map，两条都注册会让玩家随机拿到其中一个。

**工作台配方的物品匹配是有意选择**：GT6 靠矿物词典，任何一个铜锭都算；移植版对 13 种形态做了「产出统一到
原版物品」的改写（`MaterialUnification`：铜/铁/金锭、金/铁粒、钻石/绿宝石/煤/石英、红石/糖/火药粉…），
而原版自带 9 锭 ↔ 方块的配方。若这里同时接受两种物品，两套配方会抢同一个工作台格子且没有确定赢家，
所以工作台侧**只匹配 GT 物品**，把原版物品的那一套留给原版配方；机器侧则沿用 `MaterialEquivalence`
（`RecipeInputs.matches`）——两种锭都能投料，产出统一到原版物品，与既有机器一致。

### 19.5 结果

| 项 | 本轮前 | 本轮后 |
|---|---|---|
| Compressor 形态换算 | 1（手写 Adamantium） | **1199** |
| Unboxinator 形态换算 | 0 | **7210** |
| Boxinator 形态换算 | 0 | **11577** |
| 工作台形态换算配方 | 0 | **21136** |
| 被记账的原版对（机器 / 工作台） | — | 5 / 7 |

（`docs/material-form-conversions.json` 逐类落盘；`docs/material-workability.json` 记形态标志。）

### 19.6 验证

新增 `gametest/MaterialFormConversionTests`（7 项）：

1. `machineConversionCoverage` —— 三张表各有量级（compressor > 100、boxinator/unboxinator > 1000），
   并落盘 `docs/material-form-conversions.json`（含**全部 24 张通用处理器表的条数**，供下一轮挑缺口）；
2. `copperStorageRoutes` —— 铜的七条要点路线逐条断言：9 锭 ↔ 锭块、9 粉 ↔ 粉块、9 小堆 → 粉、
   4 小堆 → 粉、9 板 → 致密板、1 粉 → 9 小堆；
3. `compressionDurationFollowsWorkability` —— 铜（SOFT+FURNACE）9 板 → 致密板 = **144 tick**；
   再挑一个难加工材料，断言时长 = **9 × 256 × (品质 + 1)**；
4. `workabilityTableMatchesGT6` —— 铜/锡/铅/金/银/铝/青铜/黄铜 判为易加工，钨/钛/不锈钢不是；
5. `craftingGridConversions` —— 工作台真的能合成：9 铜锭 → 铜锭块、锭块 → 9 锭（结果按形态等价断言，
   因为产出被统一成原版铜锭）、4 小堆 → 1 粉；
6. `craftingGridRejectsMixedMaterials` —— 8 铜锭 + 1 锡锭**不出任何配方**（每材料一条配方不能互相串味）；
7. `boxinatorNeedsTheMatchingCircuit` —— 装箱配方必须带原版的 `ST.tag(9)` 电路。

```
========= 266 GAME TESTS COMPLETE ======================
All 266 required tests passed :)
```

（259 → 266。）

### 19.7 下一轮的依据：逐表条数（`docs/material-form-conversions.json`）

本轮顺手把 24 张通用处理器表的运行期条数落了盘，下一轮挑缺口不必再猜：

| 表 | 条数 | 表 | 条数 | 表 | 条数 |
|---|---|---|---|---|---|
| Boxinator | 11591 | Anvil | 7580 | Unboxinator | 7673 |
| Cutter | 6392 | Sharpening | 5540 | Extruder | 3791 |
| Shredder | 3739 | Crusher | 3702 | Sluice | 3056 |
| Lathe | 2289 | RollingMill | 2103 | RollBender | 1205 |
| Loom | 1052 | AnvilBendBig | 903 | Wiremill | 631 |
| AnvilBendSmall | 301 | ClusterMill | 299 | RollFormer | 299 |
| Sifter | 231 | Welder | 173 | **Mortar** | **60** |
| **Press** | **56** | **Autoclave** | **12** | Compressor | 1217 |

原版对应的通用处理器数量是 Mortar 34、Press 11、Autoclave 12、Welder 46、Wiremill 8、Shredder 36、
Crusher 11——**Mortar 60 / Press 56 / Autoclave 12 明显偏薄**，是下一轮最值得核对的三张表
（原版 `Loader_Recipes_Handlers:152-172` 的 Shredder/Mortar 段与 `Loader_Recipes_Parts` 的 Press 段）。








