# GregTech 6 移植版：剩余未完成内容清单

> 生成日期：2026-09-14　基准：`F:\Dev\GregTech6\gregtech6-master`（GT6 原版源码）↔ `F:\Dev\GregTech6\GregTech6`（1.20.1 移植版）
> 当前质量门：`All 340 required tests passed`（`gradlew runGameTestServer`）
> 配方侧：**科技物品「没有任何配方」的数量 = 0**（§16，游戏内报告 `docs/items-without-recipes.json`）
> 缺失内容 token：**49**（§32；`i:` 11 / `omd:` 2 / `tech:` 36 — 其余全是别的模组或 GT6 本就恒假的内容）
> 数字全部由脚本从两侧源码与测试报告里现算，不靠回忆：`tools/audit_porting_progress.py`、`tools/census_gt6_registrations.py`
> 产物：`docs/porting-progress.json`、`docs/gt6-registration-census.json`
> 续批记录见 §11（反馈错第一批）、§12（材料形态对齐 + 熔炉熔炼 + JEI 工具组装）、§15、§16（配方清零）、
> §17（覆盖板共享身份）、§18（形态条件 + 引用拼写）、§19（绳索/炸药）、§20+§21（战利品：世界宝箱 + 战利品袋 + 16 本手册）、
> §22（材料形态按 GT6 材料 id 查）、§23（前缀条件对齐）、§24（方块形态/石器工具头/熔融流体/微型板）、
> §25（自选战利品箱 + 战利品瓶）、§26（GT6 主类里的打印机/扫描仪/开箱机配方）、§27（石头挤出 + 配方表空壳加固）、
> §28（石头砖石族的机器行与工作台行）、§29（工具配方回归有序合成 + 工具头工作台配方）、§30（原版的早期工具：燧石/骨/黑曜石/木化石/石头）、
> §31（材料词典书 + gt.matdicts 战利品表）

---

## 0. 一句话结论

**材料与配方引擎（materials + 85 张配方表 + 机器框架）已经基本到位；缺的主要是「玩法子系统」（手册、地牢、传送门/太空、养蜂的育种机、木头字典、战利品注入）与「多方块/机器的材质等级变体」，以及配方侧两层结构性缺口（矿物词典事件层、配方钥匙物品）。**

| 维度 | 原版 GT6 | 移植版 | 完成度（可量化口径） |
|---|---|---|---|
| Java 文件 | 1228 | 748 | 61% |
| Java 行数 | 205,762 | 95,446 | **46%** |
| 材料 | 由 `MT.java` 工厂方法声明（未逐项比对） | **1154**（运行期日志） | 材料侧已接近完整，含 1.16+ 新增材料 |
| 材料物品 / 材料块 | — | 49,269 / 7,354 | 已注册（§12 按 GT6 形态表补齐） |
| 流体 | — | **914** | 已注册（去重后） |
| 物品 / 方块（总数） | — | 62,609 items / 10,969 blocks | 已注册 |
| 配方表（RecipeMap） | **85** | **85** | **100%**（表本身一张不缺） |
| 单方块机器 | **62 类**（251 条分等级注册） | 79 个机器定义 | **62/62 类全部有实现**（9 个改名 + 1 个合并，见 §4 备注） |
| 多方块结构 | 41 个机器名 + 76 个结构件（117 条注册） | **JEI 里 37 种结构** | 结构 ≈90%；**逐材质等级变体缺**（见 §4） |
| 覆盖板 | 65 个类 / 61 种 | 6 个文件 | 约 10% |
| 转译配方 | — | 1416 条注册 / 672 条被阻塞 | — |
| 缺失内容 token | — | **135**（见 §5） | — |

---

## 1. 已完成、且有测试守着（对照时不必再查）

- **材料系统**：1154 材料、49269 材料物品、7354 材料块、914 流体、432 石方块 + 432 石台阶 + 271 icon set 方块。
- **材料形态与原版对齐**（§12）：`tools/extract_gt6_form_flags.py` 从 `TD.java`/`MT.java` 导入 GT6 的
  `ITEMGENERATOR.*` 形态表（1245 材料），`MaterialPrefix` 的条件改为「原版条件 OR 移植版旧条件」，
  对照报告 `docs/material-form-gap.json`：17 个形态族里 15 个缺口归零（合计 4799 vs 4792，差 7 个是
  `Magic`/`ConstructionFoam` 这类移植版没有实体的材料）。
- **配方表**：85 张 `RecipeMap` 与原版 `RM.java` 的 85 张一一对应，**没有缺表**。
- **单方块机器**：原版 `Basic Machines` 分类共 **251 条注册 / 62 个机器类型**（每个类型按 HU/RU/KU/EU/MU/LU/CU/QU/TU
  分等级注册），移植版 79 个 `MachineDef` **把 62 类全部覆盖**——逐名比对后缺失数为 **0**，
  其中 9 类是改名对应（原版名 → 移植版名：Pressure Washer → `debarker`、Canning Machine → `canner`、
  Lightning Processor → `lightning`、Matter Fabricator → `massfab`、Matter Replicator → `replicator`、
  Molecular Scanner → `scannermolecular`、Nanoscale Fabricator → `nanofab`、Roasting Oven → `roaster`、
  Sanding Machine → `sander`；Low Heat Extruder 是原版 Extruder 的 T1 档，移植版合并为 `extruder`）。
  另有 Electric_T 电压等级、机壳材质、接受功率区间、配方表绑定与合成配方的逐项断言。
- **多方块（15 台主机）**：Large Turbine、Large Gas Turbine、Large Boiler、Large Dynamo、Coke Oven、
  Large Crucible、Distillation Tower、Cryo Distillation Tower、Tank 3×3 / 5×5、Heat Exchanger、
  Bedrock Drill、Lightning Rod、Implosion Compressor、Fusion Reactor。
- **材料形态换算**（本轮新增）：机器侧 19,986 条（Boxinator 11577 / Unboxinator 7210 / Compressor 1199），
  工作台侧 21,136 条，覆盖粉↔粉块、锭↔锭块、板↔板块、小堆/微粒→粉、板→致密板。
- **已有 GameTest 守门**：291 项，覆盖电压等级、坩埚温度、木桶发酵、矿处理路线、转译集合预算、
  多方块合成、核/聚变/物质制造、渲染与本地化等。

---

## 2. 完全缺失的玩法子系统（原版有整套实现，移植版只有零星物品）

| 子系统 | 原版规模（文件数） | 移植版 | 说明 / 连带影响 |
|---|---|---|---|
| **手册 / 书籍体系** | 9（`MultiItemBooks`、`Loader_Books`、`LoaderBookList`、`MultiTileEntityBookShelf`…） | **部分**（§21）：`Loader_Books` 的 16 本手册已转译成原版成书（`content/book/GTBooks`）+ 落灰指南书物品；`MultiItemBooks` 的其余条目、书架方块、Printer 的成书产物仍缺 | 原版所有指南、配方书、书架都建立在此之上；Printer（打印机）机器仍无配方（原版它的产物就是手册） |
| **地牢 / 结构生成** | 27（`DungeonChunk*` 全套房间/走廊/门/宝库） | **已完成（§76 + §79）**：`WorldgenDungeonGT` 的布局 + **15 个单元**（岩缝/走廊×3/活塞门/入口/兵营/空房/工坊/基岩矿/图书馆/储藏/农场×3/**下界门房/末地门房**）+ **10 把地牢钥匙**与门方块；仅剩暮色/以太/神秘三个门房（其它模组内容） | 地牢有房间、战利品、照明、矿脉与可用的下界/末地传送门 |
| **传送门 / 太空体系** | 20（`tileentity/portals`：下界/末地/暮色/以太/迷你传送门 + 太空站） | **0** | 原版用 GT 自己的一套传送门；移植版完全没有跨维度与空间站玩法 |
| **养蜂（Apiculture）** | 8（`IItemBumbleBee`、`MultiTileEntityBumbleHive`、`WorldgenHives`…） | **已完成（§73–§78）**：20 条蜂巢离心配方、遗传系统、80 蜂种表、野生蜂巢方块/世界生成、320 扫描态蜂物品 + Bumblelyzer 扫描配方、变异/组合/育种内核、**育种机 `bumbliary` + `advanced_bumbliary`（36/20 槽 + GUI）**；仅剩 GT6 的蜇人、`bumbleCanProduce` 工作地点扫描、铲子 GUI 三处**有意跳过** | 蜂巢→蜜蜂→扫描→育种→产出蜂巢/蜜的完整链条在移植版里闭环 |
| **木头字典 / 树种体系** | 34（`gregapi/wooddict` 6 + `blocks/wood` 16 + `blocks/tree` 12）+ `Loader_Woods` | 部分：`block/wood` 6（木桶、板材等） | 因此 `Loader_Recipes_Woods`（71 条配方）无法转译——它们由原版的 `WoodDictionary`/`BlocksGT.Log` 驱动（战利品侧的树苗行已按原版循环展开的部分跳过） |
| **战利品注入** | `Loader_Loot` + `ChestGenHooksChestReplacer` 等 | **已做**（§20+§21）：10 张原版宝箱表共 88 行注入 + 4 个战利品袋 + 落灰指南书；缺 GT 宝箱方块本身（`MultiTileEntityChest` 的 lootchest 变体）与 Loot Bottle（gt.bottles 整表跟着推迟） | 原版会把 GT 材料、袋子、手册塞进原版宝箱；移植版同样如此，只是没有「可配置名字的 GT 宝箱」那个方块 |
| **ComputerCraft 外设** | 5（`gregapi/computer` 3 + `tileentity/computer` 2） | 0～1 | 1.20.1 的 CC 生态不同，优先级最低 |

> 说明：**村民交易不是缺口**——原版 GT6 本身没有村民交易系统（`Villager|Trade` 命中的 15 个文件是
> 僵尸村民治疗、硬币、手册之类的顺带引用），移植版同样没有，属于一致。

---

## 3. 部分缺失的子系统（有骨架，覆盖不全）

| 子系统 | 原版 | 移植版 | 缺口的具体内容 |
|---|---|---|---|
| **覆盖板 Covers** | 65 文件 / 61 种（`gregapi/cover/covers`） | 8 文件（`content/cover`）+ 5 文件传感器 API | **§108 逐文件拆完**：61 = **48 个具体行为 + 10 个抽象基类 + 3 个纯贴图**。已有行为 **35 种**（§108 之前 26：`MachineCoverSpec` 12 + `PanelCover` 12 + drain/air_vent + pump/conveyor/robot arm；§108 加 9 种），**14 种是物流核心的 cover（移植版无物流核心 ⇒ 不适用，但物品与配方都在）**，1 种绑在移植版没有的 Canvas 物品上，"方块兼 cover"（`CoverTextureSimple`）与物流同类不接线。**宿主已补齐**：§108 时 cover 只贴机器面，**§111/§112 之后流体管道与物品管道也是宿主**（GT6 的阀门/流体滤器贴在单罐流体管道上、物品滤器/检索器贴在物品管道上；两类管道都做了面级拦截与发送面闸门）。**缺的不是"大量覆盖板"，而是 14 种物流 cover 所依赖的物流核心** |
| **传感器 / 显示器** | **20 台机器**（`gregtech/tileentity/sensors/*.java` 20 个类；注册行 `Loader_MultiTileEntities:1979-1998`，机器 id 31000–31022） | **20 台**（移植版用一个 `SensorBlockEntity` + `Kind` 枚举 + `registry/GTSensors` 实现同一批机器；§108 补齐） | **已按机器数对齐（§108）**：此前是 16 台——四个重量计（`Loader_MultiTileEntities:1988-1991`）并成了一个（`sensor_weightometric` 其实就是 GT6 的 Heavy），**TPS 计（`:1992`）完全没有**；§108 补上 `sensor_weightometer_light/medium/super_heavy` 与 `sensor_tpsmeter`，四档只差刻度（g/kg/t/kt，`MultiTileEntityWeightometer*:43,62`）。**剩余差异**：①~~这 20 台传感器都**没有合成配方**~~ **已于 §109 关闭**（20 行 `Loader_MultiTileEntities:1979-1998` 抽成 `data/generated/GTSensorRecipesGen` + `data/SensorRecipePack` 数据包，20 条配方全部加载成功，`SensorMatrixTests` 用 `RecipeManager.byKey` 钉住）②六格显示的符号贴图只覆盖了部分单位③此前的"20 vs 7"是**文件数**口径的误报 |
| **物流** | 34 文件提及（`LogisticsCore`、`MultiTileEntityLogistics*`、质量存储） | 2（`content/logistics`）+ `MassStorage` 方块/方块实体 | 大容量存储与传感器在；**原版物流核心（跨机器排序、路由、线缆）缺** |
| **GUI** | 14 个**框架**类（`gregapi/gui`：`ContainerCommon*`/`ContainerClient*` 两套基类 + 7 种槽位类；每台机器的实际界面写在各机器类里） | **12 个实现类**（`client/gui`：`BasicMachineContainerMenu`/`BasicMachineScreen`、漏斗、滤器、孵育机、垃圾箱 + 7 种槽位） | **文件数是"框架 vs 实现"，不可直接比**（§108 逐项核对）。单项核对结果：单方块机器与多方块界面、漏斗、滤器、孵育机、垃圾箱都有；**手册/书在**（`content/book/GTBooks` + `GTBooksGen` + `BookShelfBlock(+BE)` + `GTMaterialDictionary`，`BookShelfTests`/`LootInjectionTests` 覆盖）、**打印机在**（`GTMainRecipesTests` 断言它印出 `Manual_Printer`）、**扫描仪在**（GT6 的 `Behavior_Scanner` 本来就是**聊天栏输出**，移植版同样是聊天，§108 的 W3 正在移植它）⇒ §3 早先写的"聚变、扫描、打印、手册缺"里，**后三项是过期结论**。真正没有专用界面的只剩**聚变反应堆**（用通用 `BasicMachineScreen` 顶替 GT6 自己的等离子/能量面板；未逐项核对差异） |
| **网络层** | 35（`gregapi/network`） | 2（`network`） | 移植版用更少的自定义包（多数状态走菜单/NBT），属设计差异，但原版的若干同步功能未实现 |
| **世界生成** | 约 **57 个类**（`gregtech/worldgen` 17 + `worldgen/tree` 13 + `gregapi/worldgen` 13 + 其它维度 14） | **27 个特性/生成器**（`worldgen/**`）+ 地牢 21 个类 | **主世界/下界/末地的每一条都已落地**：矿脉、小矿、基岩矿、石层、岩石、地表沉积（黑沙/黏土）、水体会（海/河/沼）、流体泉、坑、草皮、蜂巢（§75）、**树 13 种（`GTTreeShapes` 含彩虹木 `rainbowood`、橡胶、柳、枫、榛、肉桂、椰子、蓝桃花心木、蓝云杉）**、倒木/树枝/发光菌（`WorldgenLog*`/`WorldgenSticks`/`WorldgenGlowtus` → `GTSurfaceFlora`）、地牢（§76/§79）。**真正没有对应物的只有**：其它维度的岩石生成（Aether/Alfheim/Erebus/Mars/Moon/planets —— 移植版没有那些维度）、`WorldgenCenter*`（GT6 自己默认**关闭**的自建世界中心）、`TwilightTreasureReplacer`/`ChestGenHooksChestReplacer`（暮色联动与 1.7.10 箱子钩子，1.20.1 用战利品表）。**§3 早先写的"地牢、树（含彩虹木）、遗迹缺"是过期结论**，§108 逐条核对后改掉 |
| **附魔 / 物品行为** | 30 个 `gregtech/items/behaviors/Behavior_*.java`；附魔 60 处提及 | **16 个行为等价物**（§108 之前是 3，§108 加 9，§110 加 4）+ 300 余条附魔相关行 | **§108 已把 30 个行为逐条判完**（不是数文件）：**9 个于 §108 落地**（胶带/扫描仪/流体活塞/打火石/打火机/四个喷雾族）、**3 个移植版已有等价**（钥匙/数据存储的 NBT 部分/活塞的块侧）、**6 个是 GT6 自身的空实现或死代码**（`SensorKit` 全注释、`Sonictron` 在主类里被注释、`Plunger_Item` 全注释、`DataOrb` 全库无引用、`Watering_Crops` 无引用、`Cropnalyzer` 无 IC2 时直接返回 0）、**7 个依赖缺失**（无实体层 ⇒ 箭/枪/铲；无 IC2/Thaumcraft/Forestry；两种染料喷雾与安放炸药缺物品）、**5 个下批候选**（建筑法杖/区块移除器/世界生成调试器/遥控器/HDD16）**全部已落地**：前 4 个在 §110（`BehaviorBuilderWand`/`BehaviorChunkEraser`/`BehaviorWorldgenDebugger`/`BehaviorRemote` + `RemoteActivatable`），**HDD16 在 §114**（16 文件存储 API + `BehaviorDataStorage16`，并顺带补上存储棒的 `BehaviorDataStorage`）。**所以"原版 30 个行为多数缺"这句话本身是错的**：可移植的是 9 + 4 = **13 个，已全部落地**（移植版行为等价物 3 → **18 个**，含 §114 的两个提示行为） |
| **伤害 / 辐射** | 50 文件 / `gregapi/damage` 15 | `damage` 4 文件（§91）+ `content/food` 3 文件（§92） | **已完成（§91 + §92）**：GT6 的 15 个伤害源全部落成 1.20.1 伤害类型（`heat/frost/chemical/bumble/crusher/shredder/spike/exploded/alcohol/caffeine/dehydration/sugar/fat` + 替代 IC2 的 `electric/radiation`），GT6 的标志映射到原版伤害标签，中英死亡消息各 15 条；`UT.Entities` 的危害 API 全量（化学/热/冻/电/温度 + 危害服检查 + 炽热锭）；**六个真实接入点**：裸导线触电、热/冷流体管道、运行中的反应堆本体、冶炼模具/炽热工件、重症辐射伤害、手持炽热锭；**§92 又补上食物/营养系统**（`EntityFoodTracker` 五条统计的四档阈值 + `FoodsGT` 20 行真表 + 进食入口），于是五种食物过量伤害类型也有了调用点。未做并记档：尖刺方块（GT6 5 个 `BlockSpike*`，GT6 资源包里没有贴图）、粉碎机/撕碎机内部伤害；**原来记在这里的"酸液浸泡/管道漏酸/瓶装饮料/农作物与食品线"四条已在 §107（管道漏酸、瓶装饮料）与 §108（酸液浸泡=世界流体的 bathing/head-inside 全套、食品线的数值与配方）收口** |
| **渲染** | 16（`gregapi/render`）+ 大量机器渲染器 | 97（`client`，含预览、模型、渲染器） | 结构完全不同，文件数不可直接比；JEI 预览与模型已覆盖多数机器 |
| **兼容模块 Compat** | 89（`gregtech/compat` 59 + `gregapi/compat` 30） | 12（`integration`：Jade/JEI 等） | 多数原版 compat 是 1.7.10 时代模组（IC2/RC/TC/BoP…）的联动，**1.20.1 没有对应模组**，属于"按需移植"而非"漏移植" |
| **ASM / coremod** | 18（`gregtech/asm`） | 0 | 1.20.1 用 Mixin/Forge 事件即可；其中少数实用功能（方块音效替换、僵尸村民转换等）若要补需另写 Mixin |

---

## 4. 多方块的「材质等级 / 结构变体」缺口（逐条核对过）

原版 `Loader_MultiTileEntities` 的 `Multiblock Machines` 分类共 **117 条注册**，逐名拆开后是
**41 个机器名称 + 76 个结构件**（墙壁、主阀、线圈、气压计、部件）。移植版有 **15 台主机**
（Large Turbine、Large Gas Turbine、Large Boiler、Large Dynamo、Coke Oven、Large Crucible、
Distillation Tower、Cryo Distillation Tower、Tank 3×3 / 5×5、Heat Exchanger、Bedrock Drill、
Lightning Rod、Implosion Compressor、Fusion Reactor）与配套的结构方块（各机器的墙、柱、线圈、部件，
见 `registry/GTMultiblocks.java`）。

**机器本体基本齐**（以下 6 台是改名/合并实现，不算缺口）：Large Batch Mixer → `largemixer`、
Large Bathing Vat → `largebath`、Large Coagulator → `largecoagulator`、Large Electric Oven → `largeoven`、
Large Matter Fabricator → `largemassfab`、Large Heat Exchanger → `HEAT_EXCHANGER_MAIN`、
Bedrock Mining Drill → `BEDROCK_DRILL_MAIN`。

**"逐材质等级"的结构变体：§113 逐条取证后，下表原先写的"缺"全部是过期结论。**

本节早先的表格是**只读 `registry/GTMultiblocks`**（15 个控制器）写的，没有读 `content/multiblock/LargeMachineParts`——而**材质变体全部在那里**，按 GT6 的数字 id 一条一条承载——也没有读 `LargeMachineLayouts`（13 台蓝图式机器）。这与 §108.7.1 记的"缺口表先看口径、再看数字"是同一类错误，而且**同一份文档第二次犯**。取证结果（`tools/extract_gt6_multiblock_ids.py`，数据源是 GT6 自己的注册行）：

| 家族 | 本节早先写的 | 实测（按 GT6 id 逐条） |
|---|---|---|
| 大坩埚（按材质） | 8 → **1**（陶瓷） | **8**：`large_{stainless_steel,tungstensteel,tungsten,adamantium,titanium,invar,steel,tantalum_hafnium_carbide}_crucible`（GT6 17302–17312） |
| 大储罐主阀 | 12 → **0** | **25**：wood + small/large × dense × 6 材料（17001–17067） |
| 大型线圈 | 6 → **1** | **6**：copper / nichrome / carborundum / osmium / iridium（18040、18042–18045）+ **niobium-titanium**（18041 → `GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL`，§113 补进 id 映射） |
| 锅炉气压计 | 5 → **0** | **5**：stainless / titanium / tungstensteel / adamantium / invar（17201–17205） |
| 多方块墙壁 | 14+ → 14 个但**材质固定** | **23** 条按 id 落地（wood / tungsten / adamantium / galvanized / steel / bronze / lead / THC / tungstensteel / titanium / invar + 9 个 Dense） |
| 发电机外壳 | 4 → **1** | **12**：蒸汽轮机 4 + 发电机 4 + 燃气轮机 4（17211–17234） |
| 其它零件 | — | 处理器 5、单元 6、粉碎轮 / 撕碎刀 / 钻头 等 |
| 机器控制器 | 41 名称 → **15 台** | **19 条全部有对应**：6 台 `*_main`（焦炉 / 蒸馏塔 / 低温蒸馏 / 热交换 / 聚变 / 内爆压缩机）+ **13 台蓝图机**（`LargeMachineLayouts`：largecentrifuge / largeelectrolyzer / largemixer / largebath / largecoagulator / largeoven / largesluice / largecrusher / largeshredder / largeautoclave / largefermenter / largesqueezer / largemassfab） |

**合计：GT6 该分类的 117 条注册 = 98 个零件 + 19 台控制器 + 0 缺失**，且这 117 条现在是 `data/generated/GT6MultiblockIds`（生成表）+ `gametest/MultiblockCoverageTests` 两项测试**机器可查**的（门禁日志：`[multiblock] rows=117 parts=98 controllers=19 missing=0`；布局侧 `layouts=13 cells=708 distinctPartIds=19`）。

> 修正（2026-09-14 续批，实机测试）：JEI 里实际显示 **37 种多方块结构**，不是本节早先写的"15 台主机"——
> 早先那个数字只数了 `registry/GTMultiblocks` 的独立控制器，漏掉了**蓝图式多方块**：
> `content/multiblock/LargeMachineLayouts` 的 13 套大型机结构、`GasTurbineDefinitions`/`AxialGeneratorDefinitions`
> 的逐等级燃气轮机与发电机组、`ControllerStructureLayouts.all()` 的 10 套控制器结构，以及
> `MultiblockInfoCategory.recipes()` 里显式登记的热交换器/基岩钻/避雷针/聚变环。
> `docs/gt6-registration-census.json` 的 `gt6MultiblockMachines` 仍然是"原版注册名"口径，两者不是同一个数。

> 复现：`python tools\census_gt6_registrations.py` 会输出原版逐分类注册量、移植版机器名清单，
> 以及"原版有、移植版按名字找不到"的两份清单（含别名处理）。数字存于 `docs/gt6-registration-census.json`。



---

## 5. 配方侧缺口

### 5.1 转译与阻塞（`docs/transpiled-recipes-coverage.json`）

| 集合（原版加载器） | 生成 | **注册** | 跳过（缺内容） |
|---|---|---|---|
| chem | 358 | 305 | 53 |
| other | 265 | 158 | 107 |
| potions | 500 | 488 | 12 |
| food | 243 | 201 | 42 |
| extruder | 322 | 112 | **210** |
| vanilla | 228 | 48 | **180** |
| temporary | 34 | 12 | 22 |
| ores | 138 | 92 | 46 |
| **合计** | 2088 | **1416** | **672** |

### 5.2 缺失内容 token：135 个（`docs/missing-content-tokens.txt`，由审计脚本从运行日志刷新）

| 类别 | 数量 | 性质 |
|---|---|---|
| `tech:*`（跨模组方块 `bop_*`/`neli_*`/`rh_*`/`tf_*`、EtFu 食物、绳索、`module_*_generator`、压弹壳模具…） | **64 token / 201 条配方** | 多为**跨模组内容**（BiomesOPlenty / Natura / Redwoods / TwilightForest / EtFu）与**需要物品本体的配方钥匙**（§5.3-2）；压弹壳模具与切刀形态已通过别名接上（§12） |
| `i:*` 各种材料形态 | **52 token / 约 200 条** | 剩余多为**原版空操作**（石头族没有齿轮/工具头，见 §7）或移植版没有实体的材料（`Magic`、`ConstructionFoam`、`U_235` 离心粉…） |
| `m:` / `f:` / `fr:` 流体 | 12 token | 零散；部分是纯粉材料（原版 `.liquid()` 同样为 null） |
| `omd:` 粉 | 7 token | 零散材料 |

> §12 之后 `i:*` 从 108 降到 52：GT6 的形态表（`TD.ItemGenerator`）已导入，缺的形态不再是「条件写窄」造成的。

### 5.3 两个**结构性**缺口（不是解析问题，写进了生成器注释，禁止硬补假配方）

1. **原版矿物词典事件层**：`Loader_Recipes_OreDict`(74) 与 `Loader_Recipes_Crops`(133) 的每条配方都写在
   `addListener("<矿物词典名>", …)` 里，输入是事件给的 `aEvent.mStack`——原版会**为每个注册到该词典名的物品各注册一次**。
   移植版要做等价物，需要一个「按 Forge 标签逐物品展开配方」的运行时能力 + 一张 GT6 词典名→标签的映射表。
   `Loader_Recipes_Woods`(71) 则依赖原版的 `WoodDictionary`/`BlocksGT.Log`（见 §2）。
2. **「配方钥匙」物品**：原版拿它们当 0 数量催化剂——
   `IL.Module_*_Generator`（Extruder 210 条卡在这里）、`IL.Rope_*`、EtFu 食物。
   补这些需要**物品本体与贴图**，属于内容而非配方。
   （`IL.Shape_Press_BulletCasing*` 与 `IL.Shape_Slicer_Eigths` 原版有、移植版也有，只是拼写不同，
   §12 已加别名接上。）

### 5.4 机器配方表里明显偏薄的行（运行期实测 `docs/material-form-conversions.json`；括号内为 §12 之后）

| 表 | 移植版 | 原版该表的通用处理器条数 | 判断 |
|---|---|---|---|
| **Mortar** | 60 → **1744** | 34 组处理器（逐材料生成） | 已补齐：原版几乎所有可碾磨材料都能手动碾粉，这是早期生存的关键机制 |
| **Press** | 56 → **2438** | 11 组 | 已补齐（含子弹压制与模具） |
| **Autoclave** | 12 → **444** | 12 组 | 已补齐（CRYSTALLISABLE 材料逐项生成） |
| **Welder** | 173 → **5111** | 46 组 | 已补齐（含 2394 条机壳焊接 / 300 材料） |
| **Anvil** | — → **8819** | 45 组 | 已补齐（含 825 条手动碎矿行） |
| **Shredder** | — → **37425** | 36 组 | 已补齐（回收行 33371 条） |
| **Furnace** | — → **200**（+1325 条进原版熔炉） | `Loader_Recipes_Furnace` 监听器逐形态生成 | §12 新增 |
| Wiremill 631 / Cutter 6392 / Sharpening 7271 / Crusher 5289 / Lathe 2289 / RollingMill 2103 | — | 8/20/20/11/22/22 组 | 规模正常 |

---

## 6. 内容形态缺口（材料前缀与物品）→ §12 起已按原版形态表对齐

| 缺口 | 状态 |
|---|---|
| `compressed` / `plateSteamcraft` / `rawOreChunk` | **仍是缺口**：原版这三个前缀的图标在 GT6 资源里也不存在（`materialicons/<style>/` 210 个形态里没有它们），移植版若注册只能给缺贴图的物品，因此保持记账跳过（`MaterialFormConversionRecipes` 的 `skipped()`）。 |
| `casingSmall` | **已解决**：原版 `OP.casingSmall` 就是移植版的 `itemCasing`（贴图名同为 `casingsmall`），已在 `PrefixRegistry` 里加别名，5 条被阻塞的装箱/工作台配方随之注册。 |
| 各材料的 `plate`/`gem`/`stick`/`foil`/`lens`/`gear`/`rail`/`plantGt*`/箭弹 | **已解决**：`tools/extract_gt6_form_flags.py` 导入原版形态表，`MaterialPrefix` 条件改为「原版条件 OR 旧条件」，17 个形态族里 15 个缺口归零（`docs/material-form-gap.json`）。 |

> 反过来说：`plateTiny`/`plateGemTiny`/`billet`/`chunkGt`/`scrapGt`/`gemChipped…gemLegendary`/`bouleGt`
> 等形态移植版都有。

---

## 7. 不会被修的「假缺口」（对照时请先看这一节）

原版这些配方本身就是空操作，移植版跳过它们是**一致行为**，不要补：

- `i:gearGt:Stone`、`i:gearGtSmall:Stone`、`i:toolHead*:Stone`（合计 144 条）：GT6 石头族形态组 `G_STONE`
  不含 `GEARS`/`TOOL_HEADS`。
- `i:chunkGt:*`、`i:billet:*`（60+ 条）：原版所有形态组都不含 `CHUNKS`。
- `m:CaCl2 / MgCl2 / … / UF4` 等：原版这些是**纯粉材料**，没有液态，`.liquid()` 同样得 null。
- **Generifier 表**：原版 `Loader_Recipes_Handlers:733-737` 的循环条件在未改动的 GT6 里永远不成立
  （`mTargetGenerifying` 初始化为材料自身，全仓无 `setTargetGenerifying` 调用点）——原版也是空表。
- **Autocrafter / Molecular Scanner / Scanner Visuals**：原版同样没有可执行配方（只有 JEI 展示用的 `addFakeRecipe`）。
- **村民交易**：原版没有。

---

## 8. 已记录的保真差异（不是"没做完"，是"做得不一样"，需逐项决定是否对齐）

| 差异 | 现状 | 原版 |
|---|---|---|
| 单等级机器被合并 | Lightning Processor 只有 1 级 | 原版 5 级 |
| Melter / Autoclave / Bath / Generifier / Coagulator / Fermenter 的机壳材料 | 统一 StainlessSteel | 原版 ANY.Iron / MT.Ceramic 等（改动会变更注册 ID，暂不动） |
| `NBT_INPUT_MIN/MAX`（接受功率区间） | 由「TU→1..16、其它→额定一半..两倍」推导 | 原版逐机硬编码（Fermenter 16..64、聚变 1..16384、蒸馏塔 1..1024） |
| 「净化矿 → 离心矿」路线 | 走离心机 | 原版走磁力分离机 |
| 工作台的形态换算匹配范围 | 只匹配 GT 物品（原版物品那一套交给原版配方） | 原版靠矿物词典，任意同形态物品都算 |
| `OM.dust/ingot/gem/solid` 的块级档位 | 未启用（`≥ U*72` 时原版会给块） | 原版有块级档位 |
| 木桶发酵 | 已按 `TileEntityBase08Barrel` 实现，但未接入 `RM.Fermenter` 通用表 | 原版共用发酵表 |
| GameTest 里临时创建的 `RecipeMap` | 会永久进入 `RECIPE_MAPS`（6 处） | — |
| 战利品：GT 宝箱方块 | **已做**（§25）：17 个可放置的「自选战利品箱」（10 个原版分类 + 7 张 GT 表），首次打开按表抽取并生成经验球；`gt.matdicts` 不给（其行是运行时生成的材料词典书） | 原版把宝箱换成带 `gt.dungeonloot` 的 GT 宝箱，玩家也能拿到 19 种「Loot Chest」物品 |
| 战利品：gt.bottles 整表 | **已做**（§25）：`loot_bottle`（Clouded Bottle）右键抽一瓶 | 由原版 Loot Bottle 发放 |
| 材料形态：工具头范围 | 只给 `TOOL_HEAD && (锭\|宝石)` 的材料 | `OP.java:239-252` 的条件是 `typemin(1)`，每个材料都有工具头（约 2.5 万件物品，移植版有意不开） |
| 材料形态：不纯粉 | 移植版给处理矿材料生成不纯粉（自己的矿石处理链要用） | 原版 `dustImpure` 的条件是**死标志** `DIRTY_DUSTS`（`TD.java:577` 声明、`MT.java` 无材料使用）→ 原版没有不纯粉 |
| 前缀总数 | 移植版注册表 453 个名字（含方块形式）、物品前缀 106 | 原版 `OP.java` 225 个带条件的物品前缀（其中 10 个名字移植版没有：管道族 8 + `casingSmall`/`chemtube`） |

---

## 9. 建议的推进顺序（按「可玩性收益 ÷ 成本」）

1. **前缀条件的运行时补完（形态对齐的最后一节）**：静态比较已把 122 个「条件写在物品前缀/方块 builder 里」的前缀对齐到 **0 个未引用标志**（§23），剩下 93 个族（线缆/导线/管道/单元/护甲/矿石变体/碎块-晶体中间物）用专用注册表实现、静态看不见条件——其中 74 个带 GT6 标志，需要逐族用**运行时断言**（像 §23 的 `PrefixConditionParityTests` 那样按标志遍历材料查物品/方块）来验证，`tools/report_unparsed_prefix_conditions.py` 已列出清单。
   ⚠ 两条先决决定：①「工具头范围」保真差异（原版 `typemin(1)` 会让每个材料多约 20 件工具头）；②死标志（`PIPES`/`TOOLS`/`WEAPONS`/`DIRTY_DUSTS`…）不要"对齐"（原版恒假）。
2. **Mortar 家族**（`Loader_Recipes_Handlers:79-112`，34 组处理器）：早期手动碾粉是 GT6 的核心生存机制，
   现在只能靠机器。需要先导入 `MORTAR`/`BRITTLE`/`FOOD` 三个材料标志（脚本化，1 小时级）。
3. **配方钥匙物品**（`module_*_generator` 89 条 + `IL.Shape_Press_*` + `IL.Rope_*`）：一次性解锁
   Extruder/Press 等表 200+ 条配方，成本是贴图与物品本体。
4. **矿物词典事件层**（OreDict 74 + Crops 133）：需要"按标签逐物品展开配方"的运行时能力，
   是配方侧最后一块结构性拼图。
5. **多方块材质等级**（坩埚 7、线圈 5、主阀 12、气压计 5、墙材质、发电机外壳 4）：纯内容+贴图，能让多方块外观与原版一致。
6. **`MultiItemBooks` 其余条目 + 书架 + Printer**：§21 已把 16 本手册与落灰指南书做完（`Loader_Books`），剩下的是书架方块（`MultiTileEntityBookShelf`）、材料词典书与 Printer 的成书产物（需要"按材料数据生成书页"的能力）。
7. **GT 宝箱方块**（`MultiTileEntityChest` 的 lootchest 变体 + `gt.dungeonloot` 可配置名字）：**§25 已完成**（17 个可放置变体 + 战利品瓶），剩下的只是 `gt.matdicts` 那张表（要等材料词典书）。
8. **地牢/传送门/养蜂/木头字典**：各自独立的大子系统，建议按玩家需求逐个立项。
9. **保真差异逐项对齐**（§8）：每项都不大，但要注意会改注册 ID 的项（机壳材质）需要迁移策略。

---

## 10. 复现命令

```powershell
# 结构规模、子系统文件数、注册量、配方与缺失 token 快照（会顺手用运行日志刷新 missing-content-tokens.txt）
python tools\audit_porting_progress.py          # -> docs/porting-progress.json

# 原版逐分类注册普查 + 机器名逐项比对（含移植版缺失清单）
python tools\census_gt6_registrations.py        # -> docs/gt6-registration-census.json

# 材料易加工标志（压缩机/碾磨机时长依赖它）
python tools\extract_gt6_workability.py         # -> docs/material-workability.json

# 材料形态表（原版 TD.ItemGenerator -> 移植版前缀条件，§12）
python tools\extract_gt6_form_flags.py          # -> docs/gt6-form-flags.json + data/generated/MaterialForms.java

# 移植版实际注册了哪些形态 / 转译配方引用了哪些 tech 物品
python tools\list_material_prefixes.py [名字...] # 列出 MaterialPrefix.java 里真正定义的形态
python tools\audit_tech_item_tokens.py          # -> docs/tech-item-tokens.json

# 质量门
.\gradlew.bat runGameTestServer --offline -PgameTestHeap=3G   # All 291 required tests passed
```

> 口径说明：**文件数只作为"结构规模"参考**，两侧包结构不同（原版把机器放在 `tileentity/*`，
> 移植版拆成 `blockentity/*` + `block/*` + `content/*`），因此 §3 的渲染/compat 等行不能直接按数量下结论；
> 凡涉及"是否实现"的结论都另有类名或运行期证据（见各表备注）。

---

## 11. 续批（同日晚）：实机测试报错的第一批修复 + 下一轮配方清单

实机测试报了一批具体缺陷，本轮修掉其中可以在不改注册 ID 的前提下完成的部分，
其余按"原版出处"列进 §11.2 的清单，不再靠猜。

### 11.1 已修（`All 266 required tests passed`）

| 问题（反馈） | 根因 | 修复 |
|---|---|---|
| **同种流体被注册多份**：`hydrogen`（无配方、默认贴图不染色）、`gas_hydrogen`（正确）、`liquid_hydrogen`（无配方但染色） | 移植版自己发明了 `gas.<material>` / `liquid.<material>` 名字，而原版 `FL.createGas`/`FL.createLiquid` 用的是**材料名本身**（`FL.java:1072,1080`）并且**复用已注册的同名流体**（`FL.java:1110`） | `registerGeneratedGasLiquidFluids` 改为：材料已有专属流体则复用并把材料绑定上去（染色/配方都落到同一个流体），否则注册**一个**以材料命名的流体；`GenGas_`/`GenLiquid_` 保留为别名键（`canonicalField`）。**注册总量 1034 → 914**（去掉 120 个重复流体） |
| **坩埚配方不该显示耗时** | JEI 分类无条件画 `Time: N ticks`，而坩埚是"到达熔点即转化" | `RecipeMap.instantRecipes()` 新标志，两张坩埚表启用；`RecipeMapCategory` 对它们不再画耗时行（温度行保留） |
| **多方块主机又被注册成单方块物品**（distillation tower、coke oven…） | 这些机器既在 `BasicMachineDefinitions` 里作为单方块注册，又有 `GTMultiblocks` 控制器 | 新增 `MULTIBLOCK_ONLY`（distillationtower / cryodistillationtower / cokeoven / fusionreactor / implosioncompressor / lightning），不再产出单方块物品；`MultiblockCraftingRecipes` 里指向这些单方块 ID 的重复条目删除（控制器条目保留），相关测试同步（多方块覆盖计数 16→13、替换表允许集改为空） |
| **部分物品没有 Forge item tag / 同材料形态没有统一 tag** | 原 `MaterialTagPack` 只写"按形态"的 tag | 新增 `gregtech:material/<material>`（**同一材料的所有形态**：锭/粉/小堆/板/杆/齿轮/块… 合并到一个 tag）；原版统一方块（铁/金/铜/红石/煤…）按材料重量补 `forge:storage_blocks/<material>` 与 `forge:ores/<material>`；GT 工具补 `forge:tools/<type>`（如 `forge:tools/wrench`） |
| 聚变配方在改名后失效 | `Loader_Recipes_Fusion` 用被删掉的 `gas.<material>` 名字查流体 | 改为按材料的**真实气相**解析（`GTGeneratedChem.materialFluid`），熔融相仍用 `molten.` 前缀；日志 `Fusion recipes: 18 / 18; unresolved: []` |

### 11.2 反馈逐条状态（本轮与 §12 两批合计，都附原版出处）

| 报告 | 现状（已核对） | 原版出处 / 下一步 |
|---|---|---|
| **管道没有配方** | **仍未做**：原版管件是**按尺寸的材料前缀**（`pipeTiny/Small/Medium/Large/Huge`），移植版没有这些前缀，只有 `PIPE_MEDIUM_STEEL` / `PIPE_MEDIUM_BRASS` 两种管方块 | `Loader_Recipes_Handlers:324-340`（`plateCurved 1 → pipeTiny 2 / pipeSmall 1`）、`:459`/`:493`（`pipeNonuple ↔ pipeSmall 9` 拆装箱）。要补得先补**管件方块族**（贴图/模型/连接逻辑），属于内容工程而不是配方 |
| **子弹没有配方** | **已修**（§11.1 之后的第二批）：`PressAmmunitionRecipes` 5142 条（压弹壳模具三种 + 装药 + Unboxinator 回收） | `Loader_Recipes_Handlers:254-255`、`:454-456` |
| **砧配方不全** | **已修**：`AnvilShreddingRecipes` 825 条 + 原有锻打族，Anvil 表 8819 条 | `Loader_Recipes_Handlers:157-172`、`AnvilMaterialCatalog.SELF_FORGING` 与 `MT.java` 的 `selforge` 集合对齐 |
| **一次性工具 / 漏斗 / 储罐 / 激光发射器 / 扳手等工具缺配方** | **部分已修**：JEI 新增「GT 工具组装」页签（43 种工具的材料形态/手柄要求，§12）；扳手/刀具等一体式工具的形态需求已与 `GTToolAssemblyRecipe` 对齐。**仍缺**原版这些方块的**本体**：`Misc Tool Blocks` 里的 Sifting Table、Grindstone、Plant Pot、Coinage Mold、Sap Bag、Bumbliary、Ender Garbage Bin/Dump、Fluid Funnel、Barrel/Drum/Canister | 原版注册表普查：`python tools\census_gt6_registrations.py`（Fluid Containers 86、Axles/Gearboxes 67、Misc Tool Blocks 51…），逐类与移植版方块比对 |
| **JEI 工具组合配方显示** | **已修**（§12）：`jei/ToolAssemblyCategory` + `recipe/ToolAssemblyCatalog`，JEI 里显示每种工具需要的材料形态与「任意可用材料」候选，`ToolAssemblyTests` 断言每个条目都能被配方本身接受 | `Loader_Tools:332-350`（每类工具一条 `AdvancedCraftingTool`） |
| **1x 导线 / 细导线 / 机器外壳 没有手工配方** | **已修**：`WelderFamilyRecipes` 2704 条 + `MachineCasingRecipes` 2394 条 / 300 材料；细导线来自 `Loader_Recipes_Parts` 的挤出机矩阵 | `Loader_Recipes_Handlers:741-790`（`addExtruderRecipe(...)` 助手） |
| **iconsets 仍是批量注册** | **仍未做**：`Loader_Blocks` 仍一次注册 271 个 icon set 方块；`gear`/`gearclockwise`/`hatch` 等在原版是**贴图集**不是独立方块 | 改动会改动注册 ID，需要与存档迁移一起考虑 |

### 11.3 验证

```
registered 914 GT6 fluids        （原 1034，去重 120）
Fusion recipes: 18 / 18; unresolved: []
========= 266 GAME TESTS COMPLETE ======================
All 266 required tests passed :)
```

改动文件：`data/RegisteredFluids.java`、`registry/GTFluids.java`、`api/fluid/FluidVisualPolicy.java`、
`loaders/c/GTGeneratedChem.java`、`loaders/c/Loader_Recipes_Fusion.java`、`api/recipe/RecipeMap.java`、
`jei/RecipeMapCategory.java`、`content/machine/BasicMachineDefinitions.java`、`data/BasicMachineRecipePack.java`、
`data/MultiblockCraftingRecipes.java`、`data/MaterialTagPack.java` + 4 个受影响测试。

---

## 12. 续批（同日夜～次日凌晨）：材料形态对齐 + 熔炉熔炼 + 三批配方族

要求「一次多移植一点」。本轮按「先量化、再动手、每步过门」推进，质量门从 280 项涨到 **291 项**。

### 12.1 材料形态与原版对齐（最大的一块）

| 项 | 内容 |
|---|---|
| 问题 | 移植版用自家材料属性推导「某材料有哪些形态」，而原版用 `TD.ItemGenerator` 的**形态位标志**（`PLATES`/`GEMS`/`STICKS`/`INGOTS`/`MULTIPLATES`…）与 `G_*` 形态组（`TD.java:557-613`）。结果：原版有的形态移植版没有（石英族的宝石/板/杆、石棉的板、纸的多重板…），相关配方只能记账跳过 |
| 导入 | 新增 `tools/extract_gt6_form_flags.py`：解析 `TD.java` 的形态位与形态组（含 `G_*` 嵌套展开）、`MT.java` 每条材料声明的形态标志**以及工厂方法体内隐含的形态**（`metal()` 内部就带 `G_INGOT_ORES`），产出 `docs/gt6-form-flags.json` 与生成表 `data/generated/MaterialForms.java`（**1245 材料**） |
| 接线 | `MaterialPrefix` 的条件改为「**原版条件 OR 移植版旧条件**」：`OP.plate = And(Or(ingot, gem.NOT), PLATES)`、`OP.stick = STICKS`、`OP.gem = GEMS`、`OP.gearGt/ring/spring/rotor/casingSmall = PARTS`、`OP.ingotHot = And(INGOTS_HOT, SMITHABLE, meltmin(800))` 等逐条照搬；只加不减，不会删掉移植版已有的形态 |
| 别名 | `PrefixRegistry` 支持「同形态不同名」：原版 `OP.casingSmall` = 移植版 `itemCasing`（贴图名都是 `casingsmall`），5 条被阻塞的装箱/工作台配方随之注册 |
| 结果 | 材料物品 **43,677 → 49,269**，材料块 **6,820 → 7,354**；`docs/material-form-gap.json`：17 个形态族里 **15 个缺口归零**（合计 4799 vs 4792，差的 7 个是 `Magic`/`ConstructionFoam` 这类移植版没有实体的材料）；转译配方 1360 → **1416** 条，缺失 token 198 → **135** |

### 12.2 `Loader_Recipes_Furnace`：矿形态回炉（核心生存机制）

| 项 | 内容 |
|---|---|
| 内容 | 新增 `content/recipe/FurnaceSmeltingRecipes.java`（**200 条**）：粉/小堆/微粒/宝石阶梯/岩石/原矿/粉碎矿按原版公式回炉成金属或宝石；外加黏土族 → 陶瓷的烧结行（`MT.java` 的 `ANY.Clay.mToThis`） |
| 公式 | `targetAmount = units(units(mTargetSmelting.mAmount, U, mTargetSolidifying.mAmount), U, 形态单位重)`，输出走 `OM.ingot` 的 块→锭→碎块→粒 级联，所以「小堆粉回炉 = 1 碎块」「粉碎矿(9/8 U) = 10 粒」与原版一致 |
| 原版熔炉 | 原版 `RM.add_smelting` 是直接写进**原版熔炉配方表**（`RM.java:805-821`），移植版原先只写自己的 `Furnace` 表。`Loader_OvenRecipes` 现在把该表镜像进原版配方管理器（本次 **1325 条**），已有数据包配方（`data/gregtech/recipes/**`，如陶瓷碗烧结）保持不变 |
| 测试 | `gametest/FurnaceSmeltingTests.java`（5 项）：与公式逐条比对、碎块/粒的级联、粉碎矿的 9/8 权重、原版熔炉里确实有该行、报告落盘 `docs/furnace-smelting-coverage.json` |

### 12.3 JEI 工具组装页签（反馈的「JEI 工具组合配方显示」）

| 项 | 内容 |
|---|---|
| 问题 | 工具配方是**无固定摆法的数据配方**（`GTToolAssemblyRecipe`），原版 JEI/NEI 也看不到输入，移植版原先只有 43 条 `data/gregtech/recipes/tools/*.json` 的「特殊配方」 |
| 做法 | 新增 `recipe/ToolAssemblyCatalog.java`（单一事实来源：每个工具需要的形态/数量、可用的材料候选、样例成品）+ `jei/ToolAssemblyCategory.java`（JEI 页签，每个所需物品一格，格内可循环所有合法材料）+ `gametest/ToolAssemblyTests.java`（2 项，含「目录里的组合必须能被配方本身接受」） |
| 细节 | 目录会把**同一材料的配方排在首位**（头部+手柄可不同材料，但同一格内展示的首个组合必须能一起合成）；`ToolAssemblyCatalog` 从 `GTToolAssemblyRecipe.directRequirements()` 读取需求，避免视图与配方漂移 |

### 12.4 其余修复与新增

| 项 | 内容 |
|---|---|
| 转译配方别名 | `GTTechnological.ALIASES`：原版 `IL.Shape_Press_BulletCasing*`（3 个压弹壳模具）与 `IL.Shape_Slicer_Eigths`（原版拼写错误）在移植版是同物品不同名，别名接上后 13 条配方恢复；审计脚本 `tools/audit_tech_item_tokens.py` 直接读 Java 里的别名表，不会漂移 |
| 分流重命名 | 配方族分流到独立类并逐一附原版出处：`MortarGrindingRecipes`(1744)、`PressAmmunitionRecipes`(5142)、`WelderFamilyRecipes`(2704)、`SharpeningRecipes`(1731)、`AutoclaveRecipes`(444)、`AnvilShreddingRecipes`(825)、`ShredderRecyclingRecipes`(33371)、`CrusherFamilyRecipes`(1227)、`MachineCasingRecipes`(2394/300 材料)、`MaterialFormConversionRecipes`(21246 + 工作台 22932) |
| 新增测试 | `RecipeFamilyCoverageTests` 扩到 7 项（含粉碎机宝石阶梯、粉碎机回收的 16×/256× 时长倍数）、`FurnaceSmeltingTests` 5 项、`ToolAssemblyTests` 2 项、`MaterialFormGapTests` 2 项（形态缺口报告） |
| 审计脚本 | `tools/list_material_prefixes.py`（列移植版实际注册的形态）、`tools/audit_tech_item_tokens.py`、`tools/extract_gt6_form_flags.py`；`tools/audit_porting_progress.py` 现在会**用运行日志刷新** `docs/missing-content-tokens.txt`，不再读旧快照 |

### 12.5 本轮之后的剩余（按可玩性排序）

1. **管道族**（反馈里唯一完全没动的一项）：需要按尺寸的管件方块/模型，不是配方问题。
2. **`Misc Tool Blocks` / `Fluid Containers` 的方块本体**：Sifting Table、Grindstone、Plant Pot、Coinage Mold、Sap Bag、Bumbliary、Fluid Funnel、Barrel/Drum/Canister（原版 86+51 条注册）。
3. **配方钥匙物品**：`IL.Module_*_Generator`（Extruder 210 条）、`IL.Rope_*`、EtFu 食物 —— 缺物品本体与贴图。
4. **矿物词典事件层**（§5.3-1）：`Loader_Recipes_OreDict`(74) 与 `Loader_Recipes_Crops`(133) 需要「按 Forge 标签逐物品展开配方」的运行时能力。
5. **`RM.DidYouKnow` 提示页**：原版 20 条 NEI 教程页（坩埚炼汞、钢的渗碳、镀锌槽、书架附魔…），移植版 `DidYouKnow` 表是空的，补进去就能在 JEI 里给新手一条引导线。
6. **剩余 135 个缺失 token**：多数是跨模组内容（`bop_*`/`neli_*`/`rh_*`/`tf_*`/`wimo_*`/`etfu_*`）与 §7 的「假缺口」。

### 12.6 验证

```
Registered 1154 materials
Queued 49269 material items for registration
Queued 7354 material blocks for registration
Registered 200 GT6 furnace smelting rows
Registered 33371 GT6 shredder recycling recipes
Registered 5142 GT6 ammunition recipes
Converted 133 vanilla smelting recipes to GT6 oven recipes; 2 un-smelted (...); 1325 GT furnace rows added to the vanilla furnace.
Transpiled GT6 recipes: 1416 added, 672 skipped (missing content)
========= 291 GAME TESTS COMPLETE ======================
All 291 required tests passed :)
```

改动文件：`data/MaterialPrefix.java`、`api/prefix/PrefixRegistry.java`、`registry/GTTechnological.java`、
`loaders/Loader_OvenRecipes.java`、`content/recipe/FurnaceSmeltingRecipes.java`、
`recipe/ToolAssemblyCatalog.java`、`recipe/GTToolAssemblyRecipe.java`、`jei/ToolAssemblyCategory.java`、
`jei/GregTechJEIPlugin.java`、`lang/en_us.json`、`lang/zh_cn.json`、
`data/generated/MaterialForms.java`（生成）+ 4 个新测试、5 个脚本。

---

## 13. 续批（次日）：把「有物品但没配方」量化，再补齐三批

反馈里最难核对的一类问题是「某个物品在生存里做不出来」。这一轮先把它变成**可复现的报告**，
再按报告补配方。

### 13.1 报告：`docs/items-without-recipes.json`（`gametest/CraftingCoverageTests`）

对 332 个科技物品 + 43 个工具逐个查「有没有任何配方把它产出来」——数据包配方（工作台/熔炉/…）
与 85 张 `RecipeMap` 的行都算。首轮结果：**134 个物品没有任何配方**，集中在：

| 族 | 数量 | 例子 |
|---|---|---|
| 机器部件（compact_*） | 37 | `compact_electric_motor_zpm/uv/puv1/xv`、`compact_robot_arm_ulv`、`compact_sensor_ulv` |
| 电路/电子 | 27 | `circuit_magic/enderium/signalum`、`circuit_board_*`、`circuit_part_*`、`crystal_*`、`usb*` |
| 物流与覆盖板 | 23 | `logistics_display_cpu_*`、`filtered_logistics_*_bus_*`、`cover_pump`、`item_retriever_cover` |
| 形状/模具 | 14 | `foodmold_shape_*`、`slicer_shape_*` |
| 电池 | 10 | `lead_acid_cell_empty/filled`、`alkaline_button_cell_*`、`nickel_cadmium_cell_*`、`lithium_*` |
| 激光发射器 | 9 | `laser_emitter_emptygas` + 8 种气体 |
| 其它 | 14 | `drain`、`air_vent`、`warning_cover`、`pressure_value` |

### 13.2 机器部件：ZPM/UV/PUV1 + 4 个 ULV 变体（28 条）

原版在 `MultiItemTechnological:405-423` 为每个电压档生成电机/泵/传送带/活塞/机械臂的合成表——
移植版只做到 LuV。按原版逐条补齐（`tools/generate_missing_component_recipes.py`）：
`MT.DATA.Electric_T` = TinAlloy/SteelGalvanized/Al/StainlessSteel/Cr/Ti/Ir/**OsmiumElemental**/Trinitanium/Trinaquadalloy，
`CABLES_01`/`WIRES_01`/`WIRES_04` 按档取铅/锡/铜/金/铝/铂/石墨烯，
`OD_CIRCUITS[i]` 对应移植版的选择器标签 `integrated_circuit_N`。

> 两个坑（生成器注释里记了）：① 原版的 `Os` 在移植版叫 **`OsmiumElemental`**（材料物品 id `*_osmiumelemental`、
> 标签 `forge:*/osmium_elemental`），但**导线 id 仍是 `wire_NN_osmium`**（导线按 `WireDefinitions` 的后缀命名）；
> ② 力场发生器的导线尺寸是 `1/2/4/6/8/10/12/14/16` 序列而不是 `2i`，写错会引用不存在的 `wire_18_*`。
> 首次跑门禁时这两处以 `Parsing error loading recipe` + `Unknown item` 暴露 —— 说明「配方加载失败」必须当失败看，
> 不能当静默跳过。

### 13.3 形状物品：食物模具与切片刀（14 条）

原版 `MultiItemTechnological:336-380`：`Shape_Foodmold_*`（空模具 + 5 种形状）与 `Shape_Slicer_*`
（刀架 + 7 种刀刃），全部是 `tool_shaped` 配方（不锈钢板/小堆板/杆/环 + 锤/锉/锯/剪线钳）。
移植版注册了这些物品却没有配方 → 按原版补齐（`tools/generate_shape_recipes.py`）。
**刻意的差异**：`allow_mirror` 设为 false —— 原版用 `CR.DEF_REV` 允许镜像，而镜像后
「圆面包模具」与「法棍模具」会变成同一张图；移植版 `ManufacturingTests` 要求每个模具方向唯一，
这条断言（连同新增的 14 条）现在在门禁里守着。

### 13.4 激光发射器（1 条工作台 + 8 条灌装）

原版 `MultiItemTechnological:385`（空发射器：银板 + 铜线缆 + 高级电路 + 玻璃 + 工具）与
`:396-403`（Canning Machine 里用 1 单位激光气体灌装成 8 种发射器）。
新增 `content/recipe/LaserEmitterRecipes.java`（8 条灌装行）+ `data/gregtech/recipes/components/laser_emitter_emptygas.json`。
这批修的是**真实卡关**：纳米制造机的配方需要氩/氪/氙发射器，而这三种发射器此前无法获得。

### 13.5 结果

| 指标 | 本轮之前 | 本轮之后 |
|---|---|---|
| 无任何配方的科技/工具物品 | 134 | **76** |
| 机器部件配方（`components/`） | 55 | **83** |
| 形状模具（`extruder_shapes/`） | 64 | **78** |
| 激光发射器可制造 | 0 / 9 | **9 / 9** |
| GameTest | 291 | **293**（新增配方覆盖报告；两个计数断言同步） |

剩余 76 个（按可玩性排序）：**电子（39）**——`circuit_magic/enderium/signalum` 的板/线/部件链、
水晶电路与处理器、USB 系列（原版 `MultiItemTechnological:560-700` 用 Press + 焊接链）；
**物流总线与覆盖板（23）**；**电池（10）**——空壳合成在原版 `:464-484`，灌装在 Canning Machine
（硫酸/蒸馏水/氯化氢/氟化氢）；**其它（4）**。
离线核对脚本：`python tools\report_items_without_recipes.py`（只看数据包配方）。

---

## 15. 续批（实机测试第三批）：漏掉的「注册式配方」+ 砧/箱/管/线/水晶电路

指出三件事：**原版有「板 + 剪线钳」做 1x 导线**、**原版有扳手/弯曲绕筒/锤子 + 弯曲板做管道的工作台配方**、
**箱子/mass storage/砧没有制作配方、砧也没有砸矿石的配方、crystal processor 没配方**。
核查后发现前两条是对的，我上一轮漏了它们——原因是**原版把配方写在 `aRegistry.add(...)` 的参数里**，
不是 `CR.shaped(...)` 调用，所以按行搜 `CR.` 一律搜不到。

### 15.1 新的检索工具（这轮的关键）

| 工具 | 用途 |
|---|---|
| `tools/search_gt6_statements.py "正则1" "正则2"` | 按**语句**（括号配平、跨行合并）搜原版源码，行级 grep 会漏掉换行写的调用 |
| `tools/extract_gt6_block_recipes.py <分类> --contains` | 从 `Loader_MultiTileEntities` 抽出每个方块的**图案 + 键**（`aRegistry.add` 的尾部参数），一个语句里多个 `add` 也能拆开 |

### 15.2 管道：原版真正的**工作台**配方（`MultiTileEntityPipeFluid:92-98`、`MultiTileEntityPipeItem:77-82`）

原版在注册每个管件时带了 `aRecipe ? new Object[]{…} : ZL` 的图案参数：

```
tiny   "sP " / "wzh"                medium "PPP" / "wzh"
small  " P " / "wzh"                large  "PPP" / "wzh" / "PPP"
huge   "PPP" / "wzh" / "PPP"（用双板 plateDouble）
限制管  " h " / "RPR" / " R "（R = 钢环，P = 同尺寸普通管；large/huge 图案略不同）
```
键：`s` 锯、`w` 扳手、`z` 弯曲绕筒、`h` 锤子（与描述完全一致）。
**更正 §14.2 的结论**：限制管不是「只有物品没有配方」，它可以由普通管 + 钢环在工作台做出来。

### 15.3 砧、箱子、mass storage（`Loader_MultiTileEntities:132-141`、`:2184-2218`）

| 方块 | 原版图案 | 键 |
|---|---|---|
| 金属箱 | `"sPw"/"RSR"/"PPP"` | P 板、R 环、S 杆；s 锯、w 扳手 |
| Mass Storage | `"TCT"/"wMd"/"TCT"` | T 螺丝、C 同材质金属箱、M 机壳；w 扳手、d 螺丝刀 |
| 抽屉 / 柜子 | `"CTC"/"TdT"/"CTC"`、`"SdS"/"LCL"/"TMT"` | C 金属箱、L 皮革、M 机壳 |
| 砧（全部 34 种） | `"RRR"/"hR "/"RRR"` | 石砧 = 原版石头；黑石/花岗岩砧 = 对应石砖块；金属砧 = 对应**锭**；h 锤子 |

### 15.4 1x 导线：板 + 剪线钳（反馈）

按描述实现：图案 `"Px"`（第一格板、第二格剪线钳）→ **1× 1x 导线**，共 29 种导线族。
依据是原版自己的 `"xP"` 图案（`Compat_Recipes_IndustrialCraft: x 剪线钳 + 板 → 2 导体`），
原版对**自家导线**只写了 Wiremill 机器配方，这一条是应要求的补齐（已在类注释里标明）。

### 15.5 水晶电路链（`Loader_Recipes_Other:147-155`、`MultiItemTechnological:765-770`）

激光雕刻机（256 EU/t，64t，宝石板 + 透镜催化剂）→ 水晶电路（钻石/红宝石/绿宝石/蓝宝石）；
工作台（`"CLC"/"LBL"/"CLC"`：终极电路 ×4 + 铂电路板 + 氦氖激光发射器）→ 水晶处理器底座；
压床（底座 + 水晶电路）→ 四种水晶处理器。

### 15.6 电池壳（`MultiItemTechnological:464-484`）

5 种空壳：铅酸（`" Fh"/"FPF"/"xF "`）、碱性/镍镉（`"KSM"/"OPF"/"CWZ"`）、
锂钴/锂锰（`"CLF"/"XSG"/"FLP"`）。灌装（硫酸/蒸馏水/氯化氢/氟化氢）是 Canning 行，下一步补。

### 15.7 砧砸矿石：端到端验证

新增 `gametest/AnvilSmashingTests`：放真砧、放矿石形态、用锤子敲，断言有产出。
结果：`crushed` / `oreRaw` / `rockGt` **都能敲出东西**（`oreRaw → 粉碎矿 1 + 小粉碎矿 6`，`crushed → 粉 1 + 1/72 粉 9`）。

> **仍需确认的一点**：原版里砧的输入是**矿石形态**（raw ore / crushed ore），不是矿石方块；
> 而移植版的矿石方块目前按原版 1.20 战利品表掉落**方块本身**（`data/BlockLootPack`），
> 原版则是挖矿直接得到粉碎/粗矿。如果你在砧上用的是「矿石方块」，那它是两台机器（Crusher/Hammer）的输入，
> 原版砧同样不接受它。要不要把矿石方块改成掉落粉碎矿（更接近原版生存节奏）？

### 15.8 验证

```
Registered 525 GT6 hand recipes from registration patterns
  (29 wires, 334 pipes, 34 anvils, 122 storage, 1 circuits, 5 cells)
Registered 173 original electronics recipes
========= 297 GAME TESTS COMPLETE ======================
All 297 required tests passed :)
```

无配方物品：**134 → 76 → 57 → 51 → 43**，剩余集中在
物流总线 14、USB 12、覆盖板 9、电池灌装 5、其它 3（水晶电路已全部补齐）。→ **§16 已全部清零**。

改动文件：`loaders/Loader_HandToolCraftingRecipes.java`（新）、`content/recipe/ElectronicsRecipes.java`、
`tools/search_gt6_statements.py`（新）、`tools/extract_gt6_block_recipes.py`（新）、
`gametest/AnvilSmashingTests.java`（新）。

---

## 69. 续批：材料词典的**附魔页行名解析**（GT6 用字段名，移植版用材料名）+ 修掉一个真错（`Ma` 不是镁）

§55 的附魔表是从 GT6 `MT.java` 的 `X.addEnchantmentForTools(...)` 生成的行，**行名是调用者**——
也就是 GT6 的**字段名**，不是材料名：`Ma`＝Magic、`Fe`＝Iron、`PO4`＝Phosphate、
`Polycarbonate`＝"Hard Plastic"、`HSLA`＝"HSLA-Steel"、`AmberGolden`＝"Golden Amber"……
§55 当时是手写一张 11 条的别名表兜住的，覆盖面有限，而且**其中一条是错的**：`Ma` 被映射成 Magnesium，
而 `MT.java:539` 写的是 `Ma, Magic = Ma = create(4000, "Magic")`——镁的字段是 `Mg`。⇒ 移植版一直把
**Magic 的附魔印在镁的词典页上**。

修法是**生成**而不是继续手写：新工具 `tools/extract_gt6_material_fields.py` 解析 `MT.java` 的字段声明
（`Field, Alias = Field = helper( id, "Name", …)`，以及 `Au , Gold = gold()` 这种没有 id 的形式），
生成 `loaders/c/GTMaterialFields.java`（**1106 个字段 → 材料名**）。`GTMaterialEnchants` 改为
`GTMaterialFields.materialOf(row)` → `GTMaterialRegistry.get(name)`（注册表按 `sanitize` 忽略空格/大小写，
所以 "Hard Plastic" 也能查到），查不到才回退到旧别名表，并且**删掉了错误的 `Ma` 条目**。

| 项 | §55 | §69 |
|---|---|---|
| 附魔表行 | 177 | 177（不变） |
| 能解析到移植版材料的行 | **147**（手写 11 条别名） | **173**（生成 1106 条字段表；其余 4 行是其它模组的材料+1 个非材料接收者） |
| `Ma` 的归属 | Magnesium（**错**） | **Magic**（＝ `MT.java` 的字段） |
| 新增 | — | `loaders/c/GTMaterialFields.java`（生成）、`tools/extract_gt6_material_fields.py` |

新增测试 `EnchantmentDictionaryTests.everyTableRowResolvesToAPortMaterial`（≥173 行可解析 + 只允许
Vinteum/Pyrotheum/Sunstone/STONES 四行落空 + 逐条抽查 `Ma`/`Fe`/`PO4`/`Polycarbonate`/`HSLA`/`Atl`）与
`fieldNamedMaterialsGetTheirPage`（18 个"只能靠字段表解析"的材料**真的有附魔页**）。

---

## 70. 续批：**原版铁轨配方被替换**（GT6 的 `DEL_OTHER_SHAPED_RECIPES`）——把 §68 记的那条尾巴关掉

§68 因为"移植版没有配方替换机制"而没有做 GT6 `Loader_Rails:141-156` 的四条原版轨配方。这批补上：
`Loader_TrackRecipes` 在 `ServerStartedEvent` 里**先把结果物是四条原版轨的配方全部移除**（GT6 的
`DEL_OTHER_SHAPED_RECIPES` 语义是"删掉冲突的原版行"，而不是再加一条路径），再按 GT6 的图案注册：

| 原版物品 | GT6 图案 | 产出 | 说明 |
|---|---|---|---|
| `minecraft:rail` | `RSR/RSR/RSR` | **4** | R＝`railGt(Iron)`、S＝处理木棍 |
| `minecraft:golden_rail` | `RSR/GDG/RSR` | **4** | G＝`railGt(Gold)`、D＝红石 |
| `minecraft:detector_rail` | `RSR/RPR/RDR` | **4** | P＝原版石压力板、D＝红石 |
| `minecraft:activator_rail` | `RSR/RTR/RSR` | **1..64** | T＝红石火把；**12 条**按材料递增产（Al/Magnalium/Bronze 1、Fe 2、Steel/HSLA 3、SS 4、Ti/W 6、TungstenSteel/TungstenCarbide 12、**Ad 64**） |

`Loader_TrackRecipes.registeredIds()` 30 → **45** 条，`skipped()` 从 4 条降为 **0**；新增测试
`TrackTests.vanillaRailRecipesAreReplaced`（**没有任何非 gregtech 命名空间的原版轨配方存活**、
四条原版物合计恰好 15 条、`minecraft:rail` 现在吃 `railGt(Iron)` 而不是铁锭、Adamantium 版 activator 出 64）。

---

## 71. 续批：材料词典的 **"Processing Data" 页**（§31 遗留清单里"目标矩阵"的最后一件事）

GT6 的 `UT.Books.addMaterialDictionary:900-923` 用**三页**印材料的加工目标：{熔炼/凝固/燃烧/粉碎/压碎}、
{弯曲/压缩/切割/锻造/砸碎}、{加工}，每行格式是 `整数.三位小数 <目标材料名>`（`nothing` / `itself` 有专门写法）。
移植版此前只有 8 页（身份/成分/形态/工具/属性/合金/附魔/矿石），**没有任何加工目标页**。

移植版的数据里只有其中 4 种（`mTargetSmelting`/`mTargetBurning`/`mTargetPulver`/`mTargetCrushing`），
所以按 GT6 的顺序与措辞印一页：

```
Processing Data
===================
Smelting:
1.000 Iron
Burning:
0.000 nothing
...
```

数值格式逐字照抄 GT6（`amount / U` 与 `(amount % U)/U*1000` 取三位小数，`U` ＝ `GTValues.U` ＝ 648648000，
与 GT6 `CS.U` 同值），目标为空或数量 ≤ 0 印 `nothing`、目标是材料自己印 `itself`。
另外 7 种目标（Solidifying/Bending/Compressing/Cutting/Forging/Smashing/Working）移植版**根本没有数据**，
不印也不编，已记档。

新增测试 `MaterialDictionaryTests.processingDataPageMatchesGt6`：≥900 个材料都有该页、四个标签按 GT6 顺序、
表头 19 个 `=`、**每一行都符合 `\d+\.\d{3} ` 格式**、≥500 行给出了真实目标、Iron 的页含 `Smelting:` 与
`itself`/`nothing`。

---

## 72. 续批：三个**审计工具纠偏**（它们此前在撒谎，会误导下一轮）

这三处都不改游戏内容，但都是"下一轮靠它们判断进度"的工具，值得修：

| 工具 | 之前 | 现在 |
|---|---|---|
| `tools/inventory_gt6_worldgen.py` | 只按"移植版源码里有没有出现这个 GT6 类名"判断 ⇒ **27 个类被误报 `NOT PORTED`**（9 种树、4 种倒木、灌木/花/书架子/木棍/石子、以及其它模组的星球岩石与世界中心结构） | 改成**显式映射表**（GT6 类 → 移植版文件 / 跳过原因），54 个类 **0 未分类**；结论与 §60 手工核对一致：真正没做的只有 **`WorldgenDungeonGT`、`WorldgenHives`** |
| `tools/check_enchant_table_coverage.py` | 拿表里的**字段名**去比移植版 Java **字段名** ⇒ 报"29 个材料缺失"（假缺口） | 走生成的字段表 + 忽略空格大小写的材料名比对 ⇒ **173/177 可解析**，4 行落空并逐条列出（Vinteum/Pyrotheum/Sunstone 是其它模组材料、STONES 是非材料接收者；其中 `Sunstone` 是移植版从 GT6 语言文件继承来的**无对应材料**的键，工具里显式标注） |
| `tools/report_missing_machine_recipes.py` | 去 `data/gregtech/recipes/**.json` 里找机器的合成配方（机器的配方**在代码里**生成）⇒ 报"258 台里 253 台没有配方" | 改为比对 `BasicMachineOriginalParams`（248 个变体）与 `BasicMachineCraftingRecipes`（245 行）+ `MultiblockCraftingRecipes`（52 行），并识别 `BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS` ⇒ **0 台真缺**（3 台多方块控制器的配方本来就在多方块表里） |

| 指标 | §68 | §69–§72 |
|---|---|---|
| GameTest | 429 | **433 全过**（+4） |
| 附魔页可解析行 | 147/177 | **173/177**（其余 4 行是其它模组/非材料） |
| 材料词典页数 | 8 | **9**（+ "Processing Data"） |
| 原版轨配方 | 4 条未替换（记档） | **已替换**（15 条 GT6 行，`skipped()` = 0） |
| 审计误报 | worldgen 27 个假"未移植"、附魔 29 个假缺口、机器 253 个假缺口 | **全部为 0** |
| 缺失 token | 49 | **49**（不变） |

改动文件：`content/book/GTMaterialEnchants.java`、`content/book/GTMaterialDictionary.java`、
`loaders/c/GTMaterialFields.java`（新，生成）、`loaders/Loader_TrackRecipes.java`、
`tools/extract_gt6_material_fields.py`（新）、`tools/inventory_gt6_worldgen.py`、
`tools/check_enchant_table_coverage.py`、`tools/report_missing_machine_recipes.py`、
`gametest/{EnchantmentDictionaryTests,TrackTests,MaterialDictionaryTests}.java`。

**没有执行 `git commit`。**

---

## 73. 续批：蜂巢（comb）的**离心配方**——GT6 把配方写在**物品类**里，转译器扫不到

移植版一直有 GT6 的 20 个蜂巢物品（`tools/transpile_gt6_multiitems.py` 读 `MultiItemFood.addItem`），但**一个都加工不了**：
GT6 把 20 条 `RM.Centrifuge.addRecipe1(...)` 写在同一个 `MultiItemFood.java:251-270`，而 `tools/transpile_gt6_chem.py`
只扫 `gregtech/loaders/c/Loader_Recipes_*.java` ⇒ 那 20 行从未进过移植版的配方表。

做法沿用既有"**生成表 + 运行时查表**"模式：新 `tools/extract_gt6_comb_recipes.py` 解析该区间生成
`loaders/c/GTCombGen.java`（20 行），`GTGeneratedChem.loadAll()` 增加 `combs` 集合（**8 → 9** 组）。

| 蜂巢 | EU/t × tick | 产出（流体 / 物品，括号为 GT6 的 10000 制概率） |
|---|---|---|
| honey | 16 × 64 | Honey 100 mB / WaxBee 粉（10000、1000） |
| water | 16 × 64 | 水 1000 mB / WaxBee 粉 |
| magic | 16 × 64 | Ambrosia 100 / WaxMagic 粉 |
| nether | 16 × 64 | Blaze 144 / WaxRefractory 粉 |
| end | 16 × 64 | Dragon Breath 125 / Endstone 粉（10000、1000、1000） |
| rock | 16 × 64 | Concrete 144 / Stone 粉 |
| jungle | 16 × 64 | Chocolate 144 / Cocoa 粉 + 线 |
| frozen | 16 × 64 | Ice 1000 / Ice 粉 |
| shroomy | 16 × 64 | Mushroom Soup 1000 / 红 + 褐蘑菇块（6000、6000） |
| sandy | 16 × 64 | Cactus Juice 100 / 沙 |
| clay | 16 × 64 | Concrete 144 / 6 种黏土粉各 2000 |
| sticky | 16 × 64 | Latex 144 / WaxBee 粉（10000、3000） |
| royal | 16 × 64 | Honey 50 + RoyalJelly 10 / WaxBee 粉 |
| soul | 16 × 64 | Soulsand Oil 50 / WaxSoulful 粉 + 灵魂沙（10000、9000） |
| amnesic | 16 × 64 | Lubricant 1000 / WaxAmnesic 粉 |
| military | 16 × 64 | Potion of Harming 50 / 骨粉 + 骨头 + 腐肉 + 蜘蛛眼 |
| pyro / cryo / aero / tera | 16 × 64 | Blaze 72 / Ice 500 / Dragon Breath 50 / Concrete 144 ＋ Blaze·Blizz·Blitz+Breeze·Basalz 小撮粉 + 棒 |

**表达不出来的逐条注释记档**（不猜）：`IL.FR_Propolis`、`IL.FR_Propolis_Pulsating`、`IL.FR_Propolis_Sticky`（林业）、
`IL.EtFu_Chorus_Fruit`（末影地）——GT6 那 4 行里的"其它模组物品"分支直接丢掉，行本身照做。

测试：新增 `gametest/CombProcessingTests` 3 项（**20 个蜂巢逐个**都能在 16 EU/t × 64 tick 下离心；honey_comb →
100 mB Honey + WaxBee 粉；royal_comb → 50 mB Honey + 10 mB RoyalJelly），`GeneratedChemistryTests` 的集合数
8 → **9** 并断言 combs 组 `added == 20 && skipped == 0`。

---

## 74. 续批：养蜂的**遗传系统**（GT6 `IItemBumbleBee.Util`）+ 80 个蜂种表

§73 之后，"养蜂"这条线还缺三块：遗传、野生蜂巢、育种机（§76/§77）。本批做前两块里的**数据与遗传**。

| 移植物 | GT6 出处 | 内容 |
|---|---|---|
| `content/bumble/BumbleBeeGenes.java` | `gregapi/item/bumble/IItemBumbleBee.java:96-184` | 堆栈 NBT `gt.bumble` 上的 14 个基因：`minhum`/`maxhum`（湿度区间）、`mintemp`/`maxtemp`（开尔文区间）、`offspring` 0..64、`work` 1..10000、`aggro` 100..10000、`life` 1200..144000、`rain`/`storm`/`day`/`night`/`outside`/`inside` 六个布尔 |
| 同上 | `:130-153` | 环境随机基因：湿度 = 群系降雨 ∓0.10 ∓ 0..0.40；温度 = 环境温度 ∓15 ∓ 0..30；`offspring` 1..4、`work` 1..10000、`aggro` 100..10000、`life` 1200..144000；有天空才拿 `outside`（并按降雨概率拿 `rain` 1/10000、`storm` 1/20000），没天空则拿 `inside` |
| 同上 | `:109-128` | 继承**不是混合**：每个基因独立从父母里随机取一个；只有 day/night 与 inside/outside 两对按 GT6 的写法 **OR** 起来，保证后代总有能活动的时候 |
| `tools/extract_gt6_bumbles.py`（新）→ `content/bumble/GTBumbleSpecies.java` | `MultiItemBumbles.make(...)` + `bumbleProductStack:189-213` | **80 个蜂种**（GT6 的 `make(<meta>, "<Name>")` 80 行，逐条比对 **0 缺 0 多**）＋ `meta / 100` → 20 种蜂巢的映射 |
| `content/bumble/BumbleBeeType.java` | `MultiItemBumbles:564-577` | 8 个类型：drone/princess/queen/dead（meta 0/1/2/4）与 4 个扫描态（5/6/7/9，**§77 才会注册物品**，`registered()` 先返回 false） |

**踩到的编译坑记档**：1.20.1 的 `Mth.clamp` **没有** `(long, long, long)` 重载（只有 int/float/double）⇒ 一个
`(long,long,long)` 调用会报"从 float 转换到 long 可能会有损失"，移植版为此自写了一个私有 `clamp`。

测试：新增 `gametest/BumbleBeeGeneticsTests` 5 项（NBT 往返；`UT.Code.bind` 钳制逐条；环境规则 → 若 day/night
不会同时为假；双亲继承每个基因都来自父母之一、且活动性 OR 生效；`activeNow` 的时段/天气/内外规则）。

结果：**441 项 GameTest 全过**（433 → 441）、蜂种表 80 条、蜂巢配方 20 条、缺失 token 仍 49。

---

## 75. 续批：野生的**蜂巢方块 + 世界生成**（`WorldgenHives`）——§60.2 缺口清单里的"养蜂"收尾

GT6 的 `WorldgenHives`（`worldgen/WorldgenHives.java:54-204`，`Loader_Worldgen:635-644` 按维度各登记一个对象）
是**四条 pass**，移植版此前只有"缺方块层"一句记档。本批把方块、方块实体、特征、资源、测试一次做完。

| pass | 扫描范围 | 条件 | 蜂种 / 颜色 |
|---|---|---|---|
| 主世界·岩缝（`:127-140`） | 距基岩 8..27（原版 y=8..27） | 天然岩石 + 不透明 + 六面里**恰好 5 面**不透明且无液体 | 500 Stoned / 浅灰 |
| 主世界·地表（`:142-190`） | 从地表往下到 y=3 | 第一个不透明且非树叶/原木/冰的方块；其下方那格的五个侧面**有一个空着**才放 | 见下表 |
| 下界（`:104-111`） | y=16..111 随机 | 下界岩同样嵌 5 面 | 300 Nether / `0xaa0000` |
| 末地（`:112-125`） | y=16..127 逐格，先掷 1/3 | 末地石嵌 5 面 | 400 End / `0x00aaaa` |

地表 pass 的**颜色/蜂种链**（GT6 的测试顺序即优先级，先命中先赢）：水邻居 → 魔法群系 → 火山群系 → 末地 →
下界 → 蘑菇 → 海洋/海滩/湖 → 丛林 → 寒冷 → 菌丝 → 红沙 → 沙/砂岩 → 砂砾/岩石 → 草 → 土 → **兜底魔法**
（GT6 自己的注释：这样魔法蜂一定拿得到）。放到新方块格子的是**上方接触方块的下一格**（`tY-1`）。

`placeHive`（`:195-204`）：按环境生成基因组 → 蜂巢方块（带 NBT 颜色）、槽位 1 放该蜂种 tier 的**蜂巢物品**
（数量＝`UT.Code.units(work, 10000, 10, T)`，**向上取整**所以是 `ceil(work/1000)` ∈ 1..10）、槽位 2 放**同基因组**的公主、
槽位 3 放 `offspring` 只工蜂（0 只则空，GT6 的 `makeInv` 会跳过空栈）。

| 移植物 | 说明 |
|---|---|
| `block/misc/BumbleHiveBlock.java` + `blockentity/misc/BumbleHiveBlockEntity.java` | 9 槽容器（GT6 的 `getDefaultInventory`），**任何面都不暴露**能力（GT6 的 `getAccessibleSlotsFromSide2` 返回空数组、两个 can* 都是 false）；只有**玩家破坏**才掉落内容（GT6 的 `mDroppable`）；工具提示复用移植版既有的温度计键 |
| `worldgen/GTBumbleHivesFeature.java` | 四条 pass + `colonyFor(...)`（纯函数重载便于测试）+ `embedded(...)`/`hasCollision(...)` + `dye(int)` + `placeHive(...)` |
| 数据包 | `configured_feature`/`placed_feature` 各 1 份（空的 placement 列表 = GT6 的每区块一次）+ **3 份** `biome_modifier`（`#minecraft:is_overworld` / `is_nether` / `is_end`，`vegetal_decoration`） |
| `tools/generate_bumble_hive_assets.py`（新） | GT6 是**运行时染色**（`BlockTextureDefault.get(colored图标, mRGBa)` 再叠 `overlay` 图标）；移植版把 16 色**烘**成资源：GT6 的 `DYE_INT_*`（`CS.java:403-418`）× `colored/{bottom,top,side}` 相乘、再叠 `overlay/*` ⇒ **48 张贴图** + 16 个模型 + 1 个 blockstate（16 variants）+ 1 个物品模型 + 中英各 1 条键 = **68 个文件**（工具幂等，`--check` 复跑为 0） |

**颜色映射**：`DYE_INT_*` 按名字对应（16 个全在表里），GT6 的三个字面值显式钉住：末地 `0x00aaaa` → 青、
下界 `0xaa0000` → 红、草地 `0xffdd99` → **黄**（它的 RGB 最近邻是**粉**＝蘑菇群系已占用，故人工指定并注明）。
烘好的贴图逐张校验过：平均色与"GT6 三通道相乘"的期望值最大偏差 0.62（整数除法取整）。

**保留差异（全部写进代码与本文）**：①颜色走**方块状态**（16 个烘好的变体）而不是 GT6 的 NBT 颜色 + 运行时 tint；
②只有主世界/下界/末地三条 pass——GT6 的 Erebus/Betweenlands/Atum/Aether/Alfheim/暮色/星球等维度移植版没有；
③地表扫描从 **1.20.1 的高度图**往下（GT6 是从固定 `206` 往下）；④岩缝带的 y 走 `GTWorldgenScale.remapY`
（1.18 的世界深了 128 格，这样"基岩上方 8..27 格"仍是同一层岩石；下界/末地保持 GT6 原值）；
⑤GT6 的其它模组群系集合（魔法/火山/湖）映射到 1.20.1 的对应物或留空：魔法 = 繁花森林 + 黑森林，
火山 = **空**（主世界没有对应物，下界 pass 已覆盖 300），湖 = 空；⑥深板岩/洞穴里的岩缝带、以及
`checkForMajorWorldgen` 的地牢保护照旧不移植。

测试：新增 `gametest/BumbleHiveTests` 6 项（方块/BE/特征/16 色资源与双语键/10 个蜂种齐全；
**殖民地表 16 条规则**逐条按 GT6 顺序（含"水邻居优先于群系"、"红沙先于沙"、"兜底＝魔法"）；
`placeHive` 的蜂巢数量＝`ceil(work/1000)`、公主/工蜂同基因组、offspring=0 时空槽、玩家破坏掉 3 份；
`embedded` 的 6 面规则与液体否决；数据包 3 份 modifier 指向同一特征 + `dye()` 的 19 个颜色值；
**两条主世界 pass 的真实放置**——在 remap 后的岩缝带里造一个"下方开口"的岩石口袋 → 特征在那一格放蜂巢，
在地表造一列草+土 → 特征在草下那格放蜂巢，且蜂巢/公主/蜂巢物品三者蜂种自洽）。

**门禁抓出的三处（都是测试自身，不是实现）**：
①`hiveisregistered` 报 16 个"缺模型"——测试把模型 id `gregtech:block/bumble_hive_white` 拼成了
`assets/gregtech/models/block/**block/**…`（模型 id 的路径本来就以 `block/` 开头）；
②`hivepassesplacecolonies` 报"该格还是石头"——GT6 的岩缝扫描是**自下而上**，而测试在口袋**下方**也放了石头，
于是 `pocketY - 1` 自己成了一个合法口袋、蜂巢落在了低一格；改成"开口朝下"后即绿；
③同一条测试的地表部分连 `top` 都是错的——**`Level#getHeight` 在区块未加载时直接返回世界底层（-64）**，
而这块测试用地从没被别的东西碰过 ⇒ 那一列被建到了世界外面；先在目标列 `getBlockState(...)` 强制加载再问高度图即绿。

结果：**447 项 GameTest 全过**（441 → 447，全绿）、蜂巢方块 0 → **1**（16 色变体）、蜂巢方块实体 0 → **1**、
世界生成特征 0 → **1**（三份 biome modifier）、贴图 +48、模型 +17、语言键中英各 +1、缺失 token 仍 **49**。

改动文件：`block/misc/BumbleHiveBlock.java`（新）、`blockentity/misc/BumbleHiveBlockEntity.java`（新）、
`worldgen/GTBumbleHivesFeature.java`（新）、`registry/{GTDecorBlocks,GTBlockEntities}.java`、`worldgen/GTFeatures.java`、
`data/gregtech/worldgen/{configured_feature,placed_feature}/gt_bumble_hives.json`（新）、
`data/gregtech/forge/biome_modifier/gt_bumble_hives{,_nether,_end}.json`（新）、
`assets/gregtech/**/bumblehive/**`（48 张烘色贴图 + 16 模型 + blockstate + 物品模型）、
`lang/{en_us,zh_cn}.json`、`tools/generate_bumble_hive_assets.py`（新）、`gametest/BumbleHiveTests.java`（新）。

**没有执行 `git commit`。**

---

## 76. 续批：**地牢**（GT6 `WorldgenDungeonGT` + 25 个 `DungeonChunk*` 单元）——§60.2 缺口清单里的最后一个大件

GT6 的地牢是一张**按区块格排布的网格地牢**：`-1` 兵营、`-2` 入口（两个"重要单元"）、`-128` 走廊、正数房间 id，
外加 GT6 自己那套"从每个已占用格向中心挖通道 + 两轮清理"的布局算法，以及 25 个各写一格（16×16）的
`DungeonChunk*` 实现。登记行是 `Loader_Worldgen:652`：

```java
new WorldgenDungeonGT("overworld.structure.dungeon.large", T, 100, 3, 7, 20, 20, 6, T, F, F, T,T,T,T,T, GEN_OVERWORLD, …)
```

| 登记值 | 含义 | 移植版 |
|---|---|---|
| probability 100 | 每 100 个"锚点区块"一次 | `GTDungeonLayout.PROBABILITY`，锚点＝`abs(chunk) % 11 == 5` 的格点 |
| min/max size 3 / 7 | 每边 5..9 格 | `MIN_SIZE`/`MAX_SIZE`，两个方向**各自**掷一次（GT6 用矩形数组） |
| min/max Y 20 / 20 | 基岩上方 20 格 | `MIN_Y`/`MAX_Y`，再走 `GTWorldgenScale.remapY` |
| room chance 6 | 房间散布概率 | `ROOM_CHANCE` |
| 2 个重要单元 | 一兵营 + 一入口 | `IMPORTANT_ROOM_COUNT` |
| 5 把地牢钥匙 | 五个传送门房/上锁保险箱用 | `KEY_COUNT`（移植版**还没有钥匙物品**，字段留空，记档） |

**结构**（一格 16×16）：走廊是 5 格高（0 地板 / 1..3 空间 / 4 天花板，天花板见天或见液体时换成 GT6 的发光玻璃，
中心四格是红石砖＋红石灯），房间是 **9 格高**（0 地板 / 1..6 空间 / 7 天花板 / 8 瓦片外层），
`Corridor4` 与活塞门到 6/7 层，兵营/入口/工坊/农场按 GT6 原值（农场的刷怪塔一直竖到第 43 格，**在单元自己的区块柱内**，
不越界）。岩石用 GT6 的 `BlocksGT.stones[rnd]` 两种（移植版换成自己的 27 种 `StoneType`），砖块变体走
`StoneVariant`（BRICKS/CRACKED/MOSSY/CHISELED/TILES/SMALL_TILES/SMALL_BRICKS/SMOOTH/BRICKS_REDSTONE/COBBLE/…），
`local Y == 2` 那一层用第二种岩石（GT6 的 `aY == 2 ? secondary : primary`）。

**关键设计差异（必须知道）**：GT6 的世界生成是"一次调用写完整个地牢"（它先 `getChunkFromChunkCoords` 把 9×9 个
区块全拉起来）。1.20.1 禁止特征写自己区块之外 ⇒ 移植版改成**逐格生成**：每个区块用"锚点 + 世界种子"重算同一套
布局（`GTDungeonLayout.layout`），只建自己那一格；GT6 共享一条随机流，移植版给每格派生一条稳定子流
（`GTDungeonFeature.cellSeed`）。布局、门槛、格子内容与写入顺序都照抄 GT6。

| 移植物 | 内容 |
|---|---|
| `worldgen/dungeon/GTDungeonLayout.java`（新） | 锚点格点、门槛（1/100 + 出生点 368 格 + 格中心基岩）、Y 偏移、两种岩石、布局算法（两个重要单元 → 散布房间 → 向中心挖通道 → GT6 的两轮清理）、连接数 |
| `worldgen/dungeon/GTDungeonData.java`（新） | `DungeonData` 的移植：局部坐标 helper 全套（砖块家族、`glass/glassglow/colored`、`lamp`、`coins`、`pile`、`chest`、`shelf`、`zpm`、`flower/pot/cup`、`liquid/seesSky/opaque`、`connected/neighbour`、`next*` 概率helper） |
| `worldgen/GTDungeonFeature.java`（新） | 调度器：按 GT6 的随机消耗顺序过门槛 → 布局 → 定位本格 → 按单元类型分派（房间用"洗牌后逐个尝试、拒绝就换下一个"等价 GT6 的拒绝循环；空房间兜底） |
| 13 个单元类（新） | `Pillar`、`Corridor`、`Corridor3`、`Corridor4`、`DoorPiston`、`Entrance`、`Barracks`、`RoomEmpty`、`RoomWorkshop`、`RoomMiningBedrock`、`RoomLibrary(+Normal)`、`RoomStorage`、`RoomFarmMobs/Crop/Fish` |
| 数据包 | 1 份配置特征 + 1 份放置特征 + 1 份 `biome_modifier`（`#minecraft:is_overworld`，`underground_ores`） |

**替换与记档**（逐条写在类 javadoc 与本节）：GT6 自己的彩色混凝土/玻璃/发光玻璃 → 原版 16 色混凝土与染色玻璃
（发光玻璃只有无色一种）；GT6 的装饰多方块（钱币堆/锭堆/板堆/宝石板堆/杯子/书架/货架/多功能活塞门）→ 移植版自己的
装饰方块（`coin_pile`/`*_pile`/`bookshelf`，堆类没有逐面 NBT、杯子退化成花盆）；箱子 → 原版箱子 + 对应 1.20.1 战利品表
（`chests/{stronghold_library,stronghold_corridor,stronghold_crossing,desert_pyramid,jungle_temple,village_weaponsmith,abandoned_mineshaft,simple_dungeon,spawn_bonus_chest}`，GT6 的**九个**分类一个不少）；
矿物/矿石 → 原版深板岩矿（矿脉本身走移植版自己的 `GTBedrockOreFeature.placeVein`，13 种材料与 GT6 一致）；
机器/木工台 → 原版工作台/铁砧（含 chipped/damaged 三态）/砂轮/熔炉/高炉/木桶；脚手架、物品管、GT6 钥匙、
**传送门房**（5 个，等传送门子系统）与其它模组内容（Mystcraft/Thaumcraft 图书馆、林业蜂、HarvestCraft 作物与鱼笼、
Hexxit/EtFuturum 方块）整条跳过并注明；GT6 的 `mLightUpdateCoords` 假光照 → **移植版把红石灯直接点亮**
（1.20.1 的 LIGHT 阶段按真实光源算光，GT6 是写死亮度 15）；`GTDungeonData.water()` 与钥匙字段暂时无使用者（记档）。

**门禁抓出的两处**（都是本批的测试/布局，不是并行任务的实现）：
①布局数组被写成了**锯齿**（每行各掷一次尺寸）⇒ `settle` 的斜向索引越界（GT6 是 `new byte[A][B]` 的矩形数组，两个尺寸各掷一次 ⇒ 已改）；
②`cellOf` 的"越界"断言取 `anchor + 2`，而 5×5 布局的中格是 i=2、+2 正好是 i=4（仍在界内）⇒ 改成 +3；
③顺带修正：`Blocks.LIT_REDSTONE_LAMP`/`Blocks.BOOKS`/`AnvilBlock.DAMAGE` 在 1.20.1 都不存在
（红石灯的亮灭是 `RedstoneLampBlock.LIT`、书是 `Items.BOOK`、铁砧的"损坏度"是**三个方块** `ANVIL/CHIPPED_ANVIL/DAMAGED_ANVIL`）。

测试：新增 `gametest/DungeonTests` 6 项（登记值 + 数据包 + 房间表 6/死端 1；锚点格点 11 周期与 ±4 归属 + 种子稳定；
布局 40 个种子——每份恰好 1 兵营 1 入口、≥2 房间、清理后走廊连接数 ≥2、边框恒空、同种子同结果；1/100 概率
（2 万种子 130..270）+ 每格子流互不相同 + 5 把钥匙 id 互不相同；走廊单元的**逐格方块断言**（主岩石瓦片地板、
第二岩石砖墙、内部空气、天花板砖、中心红石砖+亮红石灯、向东开口、东臂自带灯）+ 空格拒绝；
13 个单元类型都能被调用且不抛异常（接受的必须留下地板）+ 空房间兜底恒真）。

结果：**453 项 GameTest 全过**（447 → 453，全绿）、地牢**从"完全没有"到可生成**（布局 + 13 个单元 + 数据包）、
`WorldgenDungeonGT` 与 25 个 `DungeonChunk*` 全部落地（除 5 个传送门房与其它模组房间）、缺失 token 仍 **49**。

改动文件：`worldgen/GTDungeonFeature.java`（新）、`worldgen/dungeon/GTDungeon{Layout,Data,Chunk}.java`（新）、
`worldgen/dungeon/GTDungeonChunk*.java` × 16（新）、`worldgen/GTFeatures.java`、
`data/gregtech/worldgen/{configured_feature,placed_feature}/gt_dungeon.json`（新）、
`data/gregtech/forge/biome_modifier/gt_dungeon.json`（新）、`gametest/DungeonTests.java`（新）。

**没有执行 `git commit`。**

---

## 77. 续批：养蜂的**扫描/育种内核**——320 个"扫描态"蜂物品 + Bumblelyzer 扫描配方 + GT6 的变异/组合/育种逻辑

§75 把蜂巢做完之后，养蜂还剩三块：**扫描**（把蜂变成"已分析"）、**育种**（公主 + 工蜂 → 后代）、**机器**（
`Bumbliary` / `Bumblelyzer`）。这批做前两块的内核，机器放 §78。

| 移植物 | 内容 |
|---|---|
| `registry/GTMultiItemsGen.java` | **+320 个蜂物品**（80 蜂种 × 4 扫描态：`<prefix>_scanned_{drone,princess,queen,dead}`），多物品表 958 → **1278** 条 |
| `content/bumble/BumbleBeeType.java` | 四个 `SCANNED_*` 的 `registered` 由 false → **true**（§74 预留的 4 个类型到齐，8 个类型全部注册） |
| 语言键 | 中英各 **+320** 条（`tools/add_bumble_scanned_lang.py`，幂等；英文＝显示名，中文＝现有活体译名 + "(扫描)"），满足 `MaterialCompatibilityTests` 的"每个注册物品都要有双语键"守卫 |
| `content/recipe/GTBumbleBeeRecipes.java`（新） | GT6 `RecipeMapBumblelyzer.findRecipe:51-74` 的移植：**活体蜂 ×1 + 微型纸板 ×1 + 10 mB 蜂蜜 → 该蜂的扫描态 ×1**，**64 tick / 16 EU/t**（GT6 的 `Recipe` 构造器是 `(…, duration, eut, …)`；任务书里那句"64 EU/t × 16 tick"写反了，以 GT6 为准），基因组 NBT 整份带过去；GT6 :61 的"已扫描蜂直通"行也照做（1 tick / 16 EU/t、不吃蜂蜜与纸板）；蜂蜜按 GT6 `FL.java:139-141` 的 HONEY 标志认（Honey/HoneyGtC/HoneyBoP + Honeydew）；纸板 = `plate_tiny_paper`（GT6 `OP.plateTiny.mat(MT.Paper,1)`）；林业蜂那一段（:63-69）按"移植版没有林业"跳过并注明 |
| `content/bumble/GTBumbleMutations.java`（新） | GT6 `MultiItemBumbles` 的**变异与组合表**：组合 **20/20 条**（`bumbleCombine:378-435` 的嵌套 switch，逐条机械化比对 0 出入、0 跳过），变异按 `bumbleMutateChance:459` 的等级开关（0/1→500、2→250、3→25、其余 0）与方向开关（±10 / 无抽取 / 复制），80 个蜂种全在 0..3 级且目标全部存在；GT6 `addItems:180-185` 那六条从未实现的注释（Jungle+Water=Swamp 等）原样保留为文档 |
| `content/bumble/BumbleBreeding.java`（新） | `MultiTileEntityBumbliary.onTick2:113-272` 的育种内核：`findDroneSlot`（主槽优先、同种优先、末位覆盖，-1 表示没有）、`breed`（`princessCount = 1+rng(5)/2`；同种 → 该种 + 逐只 `mutate` 掷骰；异种 → 每只四选一 {A 公主, B 公主, combine(A,B), combine(B,A)}，工蜂用 DRONE 版；全部后代走 `BumbleBeeGenes.inherit` 继承双亲基因；`lifeSpan` = 公主的寿命基因；`queen` = 公主加冕、保留公主基因组；`droneDead` = 被杀死的工蜂；`droneCost` = 工蜂堆叠 >1 时为 2）、`sameSpecies`；扫描态在加冕/死亡时保持（S_PRINCESS→S_QUEEN、S_DRONE→S_DEAD），后代本身永远是普通态（GT6 的 `bumblePrincess/Drone` 就是 +1/+0） |
| `tools/generate_bumble_scanned_models.py`（新） | 320 个扫描态物品的 **item 模型**（GT6 资源包里没有单独的扫描贴图，逐条复制活体同族的模型：`layer0` = 蜂种贴图、`layer1` = `overlay_<type>`），幂等（复跑 0 变化） |

**已知/保留差异**：①GT6 还给每种蜂注册 2 行**展示用假配方**（`MultiItemBumbles:584-585`，蜂蜜/蜜露两条）⇒ 移植版
没做，所以 Bumblelyzer 的 JEI 页仍是空的（机器行为不受影响）；②`GTMultiItemsGen` 的**生成器**（`tools/transpile_gt6_multiitems.py`）
不知道这 320 行，重跑会丢（文件头已注明；彻底修法是把 `MultiItemBumbles:594-597` 教给它）；③扫描态死亡蜂的显示名按
任务书用 `(Dead, Scanned)`，GT6 原文是 `(Dead & Scanned)`。

测试：新增 `gametest/BumbleBeeScanningTests` **4 项**（80 蜂种 × 4 扫描态物品都在、`of/speciesOf` 回读、320 个双语键；
8 个类型全部 registered + 4 个扫描变体互异 + meta 0/1/2/4 与 5/6/7/9；活体公主 + 纸板 + 蜂蜜 → 该种的扫描公主且基因组保留
（64 tick / 16 EU/t、蜂蜜 10 mB、蜜露也认）；缺蜂蜜/9 mB/缺纸板/石头/只有纸板/水 → 无配方，已扫描蜂 → 直通行）与
`gametest/BumbleBreedingTests` **10 项**（同种育种的公主数 1..3 + `offspring` 只工蜂 + 蜂后物种/基因组/寿命；
逐基因继承（work/aggro/life/day-night 各来自双亲之一且不出现"全天不活动"）；异种育种的每只后代物种都在
{A, B, combine(A,B), combine(B,A)} 内；20 条组合表逐条钉住；80 蜂种 × 2 类型的变异机会与目标；`findDroneSlot`
的主槽/同种/末位/-1；`breed` 对空/工蜂/死蜂/蜂后/非蜂的拒绝；`droneCost` 1/2/备用槽；扫描态在蜂后/死蜂上保持而后代不带；
同种子同结果）。

结果：**467 项 GameTest 全过**（453 → 467，全绿）、蜂物品 0 → **+320**（物品总数 69,827 → **70,147**）、
多物品表 958 → **1278**、语言键中英各 +320、扫描贴图模型 +320、育种/变异/组合内核就位（机器见 §78）、缺失 token 仍 **49**。

改动文件：`registry/GTMultiItemsGen.java`、`content/bumble/BumbleBeeType.java`、`data/MachineRecipeMaps.java`、
`lang/{en_us,zh_cn}.json`、`content/recipe/GTBumbleBeeRecipes.java`（新）、
`content/bumble/{GTBumbleMutations,BumbleBreeding}.java`（新）、`tools/add_bumble_scanned_lang.py`（新）、
`tools/generate_bumble_scanned_models.py`（新）、`assets/gregtech/models/item/*_scanned_*.json` × 320、
`gametest/{BumbleBeeScanningTests,BumbleBreedingTests}.java`（新）。

**没有执行 `git commit`。**

---

## 78. 续批：养蜂的最后一环——**育种机（Bumbliary）本体**：GT6 `MultiTileEntityBumbliary` + `MultiTileEntityBumbliaryAdvanced`

§77 把扫描/育种内核做完后，养蜂只差"把内核接进机器"。GT6 的两台机器都是**非电力工具方块**（登记在 "Misc Tool Blocks"）：
`Bumbliary`（多方块 **32741**，ANY.Wood）与 `Advanced Bumbliary`（**32007**，不锈钢）。移植版把原来的**占位方块**
`GTToolBlocks.simple("bumbliary", …)` 换成真机器：`bumbliary` 与 `advanced_bumbliary` **共用同一个方块实体**（带 `advanced` 标志），
方块实体类型 `gregtech:bumbliary`、两个菜单类型（36 槽 / 20 槽）、GUI 用资源包里 GT6 自己的
`textures/gui/machines/bumbliary.png` 与 `bumbliary_advanced.png`（本来就已在仓库里，逐字节等于 GT6 的两张）。

| 项 | GT6 | 移植版 |
|---|---|---|
| 库存 | 36 槽（标准）/ 20 槽（高级） | 完全照抄：标准 `SLOT_ROYAL=13`、`SLOT_DRONE=22`、蜂巢槽 `{0,1,2,6,7,8,9,10,11,15,16,17,18,19,20,24,25,26}`、备用工蜂槽 `{3,4,5,12,14,21,23}`、死蜂槽 `{27..35}`；高级 `ROYAL=7`、`DRONE=12`、蜂巢 `{0,4,5,9,10,11,14}`、工蜂 `{1,2,3,6,8,11,13}`、死蜂 `{15..19}`（GT6 自己让 11 号同时属于蜂巢与工蜂两组，照抄并注明） |
| 自动化 | 只暴露死蜂槽（高级：蜂巢 + 死蜂），插入一律拒绝 | 同 |
| 每 tick | 每 1200 tick 刷环境（五个非底面是否淋雨、环境温度、群系降雨）；蜂后 `life` 倒计时与产蜜/产蜂巢；公主配对倒计时后育种 | 同（产出的物品表在新 `content/bumble/GTBumbleProducts.java`，即 GT6 `bumbleProductStack:189-213` 与 `bumbleProductChance:481-494`）；育种走 §77 的 `BumbleBreeding` |
| 蜂后死亡 | 蜂后与所有工蜂变死蜂、把待产的后代**先塞主工蜂槽再塞备用槽**（基因不再适应环境的直接杀死）、把备用槽里**攻击性最高**的公主升为皇家槽 | 同（升位后皇家槽因此一定由公主/蜂后占据） |
| NBT | `gt.progress`(life) / `gt.cooldown`(倒计时) / `gt.invout`(待产后代) | 同名照抄，另加 `inventory` |
| 槽位规则 | 标准 GUI：蜂巢槽只能取、主工蜂槽 22 `setCanTake(F)`、备用工蜂槽 `setCanPut(F).setCanTake(F)`、皇家槽只能放公主且 `canTakeOutOfSlotGUI` 只拦"活着的蜂后"、死蜂槽只能取；创造模式一律可取 | 同（三个断言 helper 逐槽 group 钉住）；**注意**：`helper.makeMockPlayer()` 在 Forge 里**恒为创造模式**，所以角色相关的断言必须用 `FakePlayerFactory` 显式设模式（本轮就是被这个坑连红两次） |

**跳过的（逐条注释 + javadoc 记档）**：①蜇人（GT6 :165-167，移植版没有蜜蜂伤害源、没有 hazmat 判定，攻击性只用于"升位"）；
②`bumbleCanProduce` 的**工作地点扫描**（花朵/水/地狱疣/石/冰/菌丝/仙人掌/黏土/灵魂沙，含其它模组的方块）⇒ 产出只由 `checkWork`
把关（`bumbleCanProduct` 本来就恒返回机器自身坐标，跳过不改变结果）；③GT6 的**铲子 GUI**（GUI id 1）与铲子工具路径 ⇒ 移植版用顶部普通右键；
④温度计的聊天读数（移植版没有方块级温度计动作，只留提示行）；⑤盖板/涂色与 `ITileEntityRunningSuccessfully`；⑥次级产物（GT6 自己
`bumbleProductCount` 恒为 1，没有第二产物可搬）。

**门禁四轮才绿，三处都是真问题**：①②先是两条**测试自身**的错（`getSlot` 差一位；`makeMockPlayer` 恒创造模式导致角色断言无效）；
③一条**实现错**（主工蜂槽 22 在 GT6 里是 `setCanTake(F)`，移植版漏了 ⇒ 生存模式能掏工蜂）；④**测试坐标撞车**：`BumbliaryTests`
原本用 `BASE_X=BASE_Z=32000`，而那是 `PitTests` 的地盘，且 GameTest 世界**跨轮复用** ⇒ 顺手把整批测试挪到 **60000/60000**
（每个测试类各占 2000 格，最高已用到 58000），并加了 `resetSites()` 让测试可重复跑。

**另外**：本轮把复用多轮、已累积到 **214 MB** 的 GameTest 世界移到一边，在**全新世界**上跑了最终门禁 —— 顺带发现
`PortRegressionTests.drawerremoval…` 的失败是**世界复用造成的污染**（新世界里自愈），这条经验写进交接单：测试数量变化后，
应在干净世界上重跑一次。

测试：新增 `gametest/BumbliaryTests` **9 项**（方块/物品/方块实体/菜单注册 + 36/20 槽分组 + 资源与双语键；
公主 + 同种工蜂 → 1200 tick 后育种、蜂后寿命取自公主基因、工蜂被吃、死蜂入槽、后代形状；
蜂后死亡后后代分配与公主升位（含"主工蜂槽被占时全部进备用槽"的两台机器对照）；工作蜂后产蜂巢（80 蜂种产物表 + 不在
`life%1200==600` 之前 + 恰好 1 个）；自动化只暴露死蜂槽（标准 9 / 高级 12）；GUI 逐槽规则（生存/创造两角色、标准与高级两套）；
NBT 往返（GT6 的三个键 + 基因组）；破坏运行中的机器把蜜蜂变成死蜂；高级机 20 槽布局与 `rng(20000)`）。

结果：**476 项 GameTest 全过**（467 → 476，**在全新 GameTest 世界上**）、育种机从"占位方块"到 GT6 语义可用、
方块 12,090 → **12,091**、物品 70,147 → **70,148**（`advanced_bumbliary`）、缺失 token 仍 **49**。
**养蜂这条线到此做完**（§73–§78）。

改动文件：`block/tool/BumbliaryBlock.java`、`blockentity/tool/BumbliaryBlockEntity.java`、
`client/gui/{BumbliaryContainerMenu,BumbliaryScreen}.java`、`content/bumble/GTBumbleProducts.java`、
`gametest/BumbliaryTests.java`（均新）、`registry/{GTToolBlocks,GTBlockEntities,GTMenuTypes}.java`、
`client/GregTechClient.java`、`lang/{en_us,zh_cn}.json`（各 +3 键）、`assets/gregtech/**/advanced_bumbliary*` 与
`textures/block/machines/tools/bumbliary_adv/**`（GT6 原图 6 张）。

**没有执行 `git commit`。**

---

## 79. 续批：地牢的最后一环——**5 把地牢钥匙 + 下界/末地传送门房**（`WorldgenDungeonGT` 的 27 个类至此做完）

§76 把地牢主体（布局 + 13 个单元）做完后，`DEAD_END` 里只剩储藏室，两个传送门房与**地牢钥匙**一直空着。这批补上。

| 移植物 | 内容 |
|---|---|
| **钥匙物品**（新 `item/GTDungeonKeyItem.java` + `registry/GTDungeonKeys.java`） | GT6 `IL.KEYS` 的 **10 把**（`key_brass/bronze/copper/gold/iron/lead/plastic/platinum/silver/tin`，顺序同 `IL.java:516`）；**不开新材料前缀**（GT6 的钥匙是 `OD.itemKey` 由板做成、不是材料形态）；每把带 `gt.key`（long，＝ GT6 `NBT_KEY`）与 **"Key #N"** 自定义名（同 GT6 `getWithNameAndNBT`），中英各 10 个键 + 贴图/模型（贴图取自 GT6 资源包） |
| **钥匙 id** | `GTDungeonFeature.keySeed(seed, i)` 按 GT6 `WorldgenDungeonGT:170-171` 改成**首值随机、逐个减一**（原来是各自独立掷骰 ⇒ 五把 id 有约 1% 概率撞号，被门禁抓出）；`keyStacks` 现在真的由 `keyIds` 构造（之前是空数组），兵营 4 个货架 / 工坊铁匠货架 / 图书馆货架都按 GT6 的槽位放真钥匙（图书馆顺带补回 §76 漏掉的 `next(24)` 掷骰） |
| **传送门方块**（新 `block/misc/DungeonPortal{,Block,BlockEntity}.java` + `registry/GTDungeonBlocks.java`） | `dungeon_portal_nether` / `dungeon_portal_end`：存 GT6 的 `gt.key` + `gt.active`，硬度＝黑曜石/末地石、无碰撞、开启时亮度 11；渲染走 GT6 的 13 个 pass（关闭时 12 条 `sBlockBounds` 门框，开启时 GT6 的内部立方体）；**激活规则＝GT6 `Behaviour_Key:44-64`**：钥匙 id 与门一致才开、id 0 认第一把用上去的钥匙、打火石可切换；**传送**：按 GT6 的搜索（系数 8/128、边距 128/512 m）找最近的同类已激活门，找不到就用缩放坐标 + 3×3 黑曜石落脚台，并用原版 300 tick 传送冷却防弹回 |
| **两个传送门房**（新 `GTDungeonChunkRoomPortal{,Nether,End}.java`） | **下界房**：地毯块 + 瓷砖路缘 + 下界岩/荧石地板 + 菌光/疣块天花板 + 补给箱（16 黑曜石/下界岩/荧石 + 手册 + 恶魂之泪 + 烈焰棒）+ 灵魂沙/疣行 + 黑曜石框（x=2, z6..9, y1..5）＋ 6 个带 `keyIds[0]` 的门；**末地房**：紫珀外壳（2×2 角柱 + 灯格柱）＋ 门角黑曜石/荧石 ＋ **8 个带眼末地传送门框（朝向按 GT6 meta 4..7）** ＋ 4 个带 `keyIds[1]` 的门 ＋ 门下方黑曜石。两房都进 `DEAD_END`（现在 3 个） |
| **跳过的（逐条记档）** | 暮色/以太/神秘三个传送门房（其它模组）；GT6 迷你传送门的物品/流体/红石/能量中继与比较器、客户端开门音效包、区块卸载即失效；Netherlicious 的疣 meta（保存掷骰结果丢弃）与 Hexxit 十六进制块（GT6 自己的荧石退路）；瓷砖路缘用整方块替代（移植版石头家族没有瓷砖半砖）；**走廊 3 不给钥匙**——GT6 在那里是"用 3 号钥匙锁保险箱"，移植版没有钥匙锁容器（记档）；钥匙与门**没有配方/创造标签**（GT6 用 `fPx` 合成，未验证的配方 JSON 有静默解析风险） |

**门禁抓出的两处**：①`keySeed` 的 id 撞号（真 bug，见上）；②测试自身的 `new HashSet<>(Arrays.asList(ids))` ——`Arrays.asList(long[])` 会得到一个**单元素**列表（数组本身），所以"五个 id 互不相同"永远为假 ⇒ 换成 `LongStream.of(ids).boxed()`。

**顺手修掉一个会撒谎的账本**：`tools/inventory_gt6_worldgen.py` 仍把 `WorldgenDungeonGT` 与 `WorldgenHives` 标成
`NOT PORTED`（§72 记过同类问题）⇒ 改成指向 `worldgen/GTDungeonFeature.java`（+ `worldgen/dungeon/*`）与
`worldgen/GTBumbleHivesFeature.java`；现在 54 个 GT6 世界生成类 **0 未分类、0 未移植**。

测试：新增 `gametest/DungeonKeysAndPortalsTests` **10 项**（10 把钥匙注册 + 中英键 + 贴图模型；`gt.key`/`Key #N` 与 NBT 往返；`DEAD_END` 3 个且顺序同 GT6；下界房逐格断言（框/地板/天花板/灵魂沙/箱子/6 个带钥匙 id 的门）；端点房逐格断言（紫珀 + 带眼框 + 朝向 + 4 个门 + 门底黑曜石）；同门重复生成的 tag 拒绝；正确钥匙开门、错误钥匙不开、空白门认钥匙、打火石切换；下界<->主世界与末地<->主世界往返传送、冷却防弹回、无对侧时落到落脚台）。`DungeonTests` 的两个计数同步更新（DEAD_END 1 → 3、单元类型 13 → 15）。

结果：**486 项 GameTest 全过**（476 → 486，**在全新 GameTest 世界上**）、方块 12,091 → **12,093**、物品 70,148 → **70,160**（+10 钥匙 +2 门方块物品）、缺失 token 仍 **49**。**目标 ② 地牢的 27 个类到此做完**（5 个传送门房里做了 Nether/End 两个，其余三个是其它模组内容；审计工具 `inventory_gt6_worldgen.py` 现在报 **27 vs 27**）。

改动文件：`item/GTDungeonKeyItem.java`、`registry/{GTDungeonKeys,GTDungeonBlocks}.java`、
`block/misc/DungeonPortal{,BlockEntity,B}*.java`、`worldgen/dungeon/GTDungeonChunkRoomPortal{,Nether,End}.java`、
`gametest/DungeonKeysAndPortalsTests.java`（均新）、`worldgen/GTDungeonFeature.java`、`worldgen/dungeon/GTDungeonData.java`、
`worldgen/dungeon/GTDungeonChunk{Barracks,RoomWorkshop,RoomLibrary,Corridor3}.java`、`registry/GTBlockEntities.java`、
`loaders/b/Loader_MultiTileEntities.java`、`lang/{en_us,zh_cn}.json`（各 +21 键）、`tools/wire_dungeon_keys_and_portals.py`（新）、
`assets/gregtech/**/key_*` 与 `dungeon_portal_*` 资源。

**没有执行 `git commit`。**

---

## 80–83. 续批（实机测试四项 + 一批内容）：**地牢接入 locate** / **地牢用回半砖** / **GT 三种水＝原版水** / **币堆与板堆变成真容器** / 木头字典工作台配方 + 工坊真机器 + 词典 11 种加工目标 + 蜂分析仪展示行

本轮提了四项实测问题，另加"继续推进"。四批并行做完，下面按问题分节。

### 80. 地牢接入原版 `/locate structure gregtech:gt_dungeon`

地牢在移植版里是**世界生成特征**（逐格生成），香草 `/locate` 看不见它 ⇒ 在特征之上再注册一个**什么都不生成**的 `Structure`：
`GTDungeonStructure`（`gregtech:gt_dungeon`）+ `GTDungeonAnchorPiece`（空 `postProcess`）+ `GTDungeonPlacement`（自定义 `RandomSpreadStructurePlacement` 子类，
`spacing 11 / separation 0 / salt 0`，`isPlacementChunk` = `abs(chunk)%11==5`，与 `GTDungeonLayout` 的锚点格点**逐一等价**，测试对 121×121 区块穷举验证）
+ 数据文件 `worldgen/structure/gt_dungeon.json`、`worldgen/structure_set/gt_dungeons.json`、`tags/worldgen/biome/has_structure/gt_dungeon.json`（`#minecraft:is_overworld`）、`tags/worldgen/structure/gt_dungeon.json`。
1 亿分之 1 的概率门槛在结构侧**镜像**同一掷骰（否则 99% 的锚点会被 locate 指向空地）。

**必须记档的两个 1.20.1 坑**：①`findGenerationPoint` **不能返回 empty**（`StructureCheck:91-93` + `StructureStart:116-118` 要求有 piece），否则 `getStructureGeneratingAt` 直接跳过候选、`/locate` 永远看不到 ⇒ 用一个"零方块"piece；"真的什么都不生成"由测试对整区块 16×16×384 方块状态做 diff 证明。
②`StructurePlacement.CODEC` 是 DFU 的 `KeyDispatchCodec`：**只有你的 codec 是 `MapCodecCodec` 时才用整个 JSON 对象解码**，否则它去找不存在的 `"value"` 字段 ⇒ `Not a JSON object: null`，**服务器在数据包加载阶段直接死亡而 Gradle 仍返回 0（假绿）**。修法＝ `RecordCodecBuilder.mapCodec(...)` + `ExtraCodecs.validate` 的 `MapCodec` 重载 + `.codec()`（与香草 `RandomSpreadStructurePlacement` 同形）。

### 81. 地牢用回移植版已有的半砖系统

`GTDungeonData` 新增 `slab(x,y,z,StoneVariant,Direction)`、`slab(...,topHalf)`、`smoothSlab/tilesSlab/bricksSlab`、`slabKerbs(...)`（GT6 的 `mSlabs[SIDE_*]` → 香草 `Direction`；岩石按 GT6 的 `aY==2 ? secondary : primary`）。把之前"用整方块替代半砖"的位置全部改回：
传送门房瓷砖路缘、下界房灵魂沙行、农场房田埂与四处花园边框、入口楼梯与平台，以及**之前连文档都没记的兵营内墙**。逐点与 GT6 的 `mSlabs` 调用比对：入口 64/64、农场 12/12、花园 8/8、兵营 4/4、下界 8/8 全中。

### 82. 海水 / 河水 / 沼泽水完全照搬原版水

| 症状 | 真因 | 修法 |
|---|---|---|
| 颜色太深 | 这三种是独立流体，没走香草水的贴图/染色：`seawater`/`swampwater` 没有自己的贴图 ⇒ 落到 GT 的**灰色熔融金属模板** + `0xFFFFFFFF` 无效染色；`riverwater.png` 还是**预先染好色**的图 | 新增 `api/fluid/GTWaterParity.java`（常量单一来源）＋ `FluidVisualPolicy` 对这三种**先于**资源包判定返回 `minecraft:block/water_still` + **`0xFF3F76E4`**；新增 `client/WaterFluidClientExtensions.java` 照抄 `ForgeMod.WATER_TYPE` 的客户端扩展（still/flow/overlay/水下贴图 + 群系水色 `BiomeColors` 染色） |
| 不能游泳、不能含水 | 它们的 `FluidType` 是 `canSwim(false)/canDrown(false)/canConvertToSource(false)/supportsBoating(false)/canHydrate(false)`，且**不在 `#minecraft:water`**（水下雾与水下叠加层就是 `FluidTags.WATER` 判定） | 三项流体属性**逐字段**改成与香草水相同（密度/黏度/温度/motionScale/canSwim/canDrown/supportsBoating/canHydrate/canExtinguish/canConvertToSource/fallDistanceModifier/音效/blockPathType），流动属性补 `explosionResistance(100)`；新增 `WorldWaterSource/Flowing` 复制 `WaterFluid` 的"水规则门控造源"与 `canBeReplacedWith`；新增 `data/minecraft/tags/fluids/water.json` 把三种水（含 `_flowing`）加进 `#minecraft:water` ⇒ 游泳/船/含水方块/海绵/混凝土/耕地/岩浆交互全部按水处理 |
| 顺带修掉的真 bug | Forge 1.20.1 的 `LiquidBlock(Supplier, Properties)`（模组唯一可用构造器）把 `fluid` 字段留成 **null**（bytecode + 运行时反射双证），而 `onPlace`/`neighborChanged`/`updateShape`/`pickupBlock`/`isPathfindable`/`skipRendering`/`getPickupSound` 都会解引用它，`LevelChunk.setBlockState` 每次 `setBlock` 都调 `onPlace` ⇒ **手动放置任何 GT 世界流体方块都会 NPE**（世界生成因写进 `ProtoChunk` 才没暴露） | 新增 `block/GTWorldFluidBlock`（覆写那七个方法、从 `getFluid()` 取流体），所有 `WORLD_FLUID_PATHS` 方块（水/油/气/地热泉）统一换用 |

保留差异（明确记录）：三种水仍是**独立流体**（GT6 有三种、世界生成仍按列替换），因此不会与香草水合流；桶装仍返回空（GT6 用自己的单元，移植版的 `fluid_item_*` 是创造专用）。

### 83. 硬币堆与锭/板/宝石板堆变成真容器（并从地牢里"空放"变成"装满"）

| 项 | 内容 |
|---|---|
| 方块 | `block/misc/CoinPileBlock.java` + `PileBlock.java`（`Kind` = INGOT/PLATE/GEM_PLATE，前缀 `MaterialPrefix.ingot/plate/plateGem`）+ `blockentity/misc/{CoinPile,Pile}BlockEntity.java`；四个 id、属性、BlockItem、lang 全部不变 |
| 存储 | 堆：`gt.value` 一栈（1..64，GT6 `NBT_VALUE`）；币堆：GT6 的 `gt.coin.stacksize.<0..15>` 16 面 + `gt.coin.item`，**每面上限 16**（GT6 `COIN_STACKSIZE`，任务书里写的 0..8 与源码不符，按源码实现并在 javadoc/测试写明），读档 clamp |
| 交互 | 堆：前缀+材料匹配 → 整栈合入（上限 64，超出补差额；GT6 连创造也扣手持）；不符/空手 → 从**同内容柱子最顶块**取 1，取空即 `setToAir`（GT6 `:107`）；币堆：只有顶面有效，面号 `floor(hitX*4)*4+floor(hitZ*4)`（含夹取），空手取 1、硬币 +1、非硬币什么都不做、创造不扣手持，最后一枚移除方块（GT6 `:174-176`） |
| 掉落 | 只重写 `onRemove`（`!state.is(newState.getBlock())` 守卫）+ 掉完清空 BE ⇒ 活塞/爆炸/玩家/命令全路径**恰好一次** |
| 自动化 | GT6 两者都**不**实现 `getAccessibleSlotsFromSide2`、也没有比较器 ⇒ 移植版不暴露 `ITEM_HANDLER`、不加比较器，并把这作为保真断言写进测试 |
| 地牢 | `GTDungeonData.coins()` 按 GT6 `:170-171` 的 16 面掷骰填充（每面 `next1in3()?next(8):0`，再覆盖一面 `1+next(8)`），`pile()` 按 GT6 的金属/宝石表掷材料与尺寸（`PILE_METALS`/`PILE_GEMS`，抽签次数与 GT6 一致、世界随机流不移位）⇒ 地牢里的堆与币堆不再空放 |
| 跳过（记档） | GT6 的逐面 3D 硬币渲染/形状位图/16 个 render pass、堆按 mSize 的层高与碰撞盒、逐材料硬币与 COIN_MAP 创造栏条目、硬币 tooltip、掉落物自动并入堆的钩子、每次点击的客户端数据包 |

### 83.1 另外三批内容（"继续推进"）

- **木头字典的工作台配方**：新 `tools/generate_wood_recipes.py` → `content/recipe/GTWoodRecipes.java`（**68 条**：50 有序 + 18 无序，覆盖 9 个树种；GT6 `Loader_Recipes_Woods` 的 128 条语句里 **71 条机器行**本来已由移植版覆盖、**57 条工作台行**是缺口）+ `loaders/Loader_WoodCraftingRecipes.java`（`ServerStartedEvent`，`allowMirror=false` 即 GT6 的 `DEF_NCC`）；**4 条确实无法表达**的行（缺 GT6 材料 9317 `Cinnamonwood`）进 `SKIPPED_CRAFT_ROWS`，另 120 条未译语句逐条带 GT6 行号记档；生成器现在**从 GT6 源码推导**每行的结果族（`mStick` 在 LIST_PLANKS 下是 rod、在 LIST_BEAMS 下是 long rod），写错就拒绝输出。
- **地牢工坊换回移植版真机器**：`GTDungeonChunkRoomWorkshop` 里原来的原版熔炉/高炉/砂轮/铁砧占位，换成 `GTMachines.BURNING_BOX_SOLID_*`/`SMELTING_CRUCIBLE_*`/`mold_*`（GT6 的 `next(3)` 档位联动，三者档位一致）、材料铁砧（`next(4)`）、砂轮、抽屉/大型储物、研钵/搅拌碗/浴盆，坐标与写入顺序不变。
- **材料词典的 "Processing Data" 补齐 11 种目标**：新 `tools/extract_gt6_processing_targets.py` → `data/generated/MaterialProcessingTargets.java`（**38 行 / 17 材料**，含 `H2O.setSolidifying(Ice,U)` 与 `Lava.setSolidifying(Obsidian,U)`）⇒ 词典从 1 页变 **3 页**（GT6 的 11 个标签全在）。
- **Bumblelyzer 的展示行**：GT6 每种蜂 2 行展示用假配方 → `RecipeMap.addFakeRecipe`，80 蜂种 × 3 类型 × 4 流体 = **960 行**（缺流体按 GT6 的 `if (FL.exists(...))` 跳过）；注册时机改到 `FMLCommonSetupEvent`（`MachineRecipeMaps` 的静态初始化跑在注册表填充之前，会 NPE）。

### 83.2 本轮新踩到的两个环境坑（写进交接单）

1. **GameTest 里对远处绝对坐标操作**：只 `setBlock`/`getChunkAt` 拿到的区块是 `Visibility.HIDDEN` ⇒ `Level.getEntitiesOfClass` **看不见该区块里的掉落物**，且被移除方块的 BE 不会被摘掉（`LevelChunk.removeBlockEntity` 有 `isInLevel()` 判断）。要给区块加 ticking 票（`addRegionTicket(TicketType.FORCED, chunk, 2, chunk)`，别用会落盘的 `setChunkForced`）才能做精确掉落计数与"BE 已消失"断言。
2. **`helper.makeMockPlayer()` 恒为创造模式**（`GameTestHelper:214-228`），`makeMockSurvivalPlayer()` 才是生存；"创造也扣手持"这类 GT6 语义必须用两位玩家分别断言。另外取出物品会被 `Inventory.add` 放进**手持槽**，下一次点击就变成"合入"而不是"取出" ⇒ 测试要先把手持槽"挪走"而不是覆盖清空。

测试：本轮新增/改动 `WaterFluidParityTests`(5)、`PileBlockTests`(7)、`DungeonLocateAndSlabsTests`(6)、`WoodRecipeTests`(8) 与 `DungeonTests`/`DungeonKeysAndPortalsTests`/`MaterialDictionaryTests`/`BumbleBeeScanningTests` 的扩展 ⇒ 套件 **486 → 513 项**。

**没有执行 `git commit`。**

---

## 84–90. 续批（实机测试七项）：**内存增长** / **三种水＝原版水（补完）** / **黏土表面** / **真硬币** / **四个堆的贴图建模** / **地牢储罐** / **手摇曲柄**

实机测试报了七条，六批并行处理，门禁从 513 → **534 项全过**（新增 21 项测试）。下面逐条。

### 84. 内存异常占用：三处"设计上无界增长" + 一处加固

先做**静态审计**（脚本扫全树 1034 个静态集合字段 / 1725 个写点，逐个读代码分类），结论：没有按 Level/Player/区块/BlockPos 无界增长的表、没有每 tick 累加器、没有自注册却不注销的 BE、没有 per-chunk SavedData 追加。**真正会无界增长的三处**已修：

| # | 位置 | 机制 | 修法 |
|---|---|---|---|
| 1 | `world/GarbageData.java`（垃圾箱 54 个堆位，服务器级 SavedData） | 合并键是 `ItemStack.isSameItemSameTags` ⇒ 带**每实例 NBT** 的物品（用过的工具、充过电的电池、随机基因的蜜蜂、盖板数值）**永不合并**，每丢一次多一条永久 Entry，唯一移除路径是把那堆取空 | `MAX_ENTRIES = 256` + `trim()`（超限丢**最旧**的堆）；`load()` 也按上限截断 ⇒ 内存与存档都有界（存档不再膨胀） |
| 2 | `api/energy/EnergyNet.java` 的 `GRAPHS` | 注册在 `onLoad`，只在 `onRemoved()` 释放，而它只有 `Block#onRemove` 一个调用者；**区块卸载走 `BlockEntity#setRemoved`** ⇒ 访问过的每个区块的每根线/传动轴都永久留一条 | 两个 BE 补 `setRemoved()` + `onChunkUnloaded()`（与引擎的卸载配对一致、幂等），`onRemove` 顺带剪掉"边集已空"的邻居条目；另加 `tracks(Level,BlockPos)` 供测试按身份断言 |
| 3 | `loaders/Loader_OvenRecipes.java` 的 `ADDED_TO_VANILLA` | `onServerStarted` 只增不清 ⇒ **每次进世界 +2314 个 ItemStack**（同级 loader 都先 clear） | 镜像遍历开头 `clear()` |
| 4 | `content/nuclear/ReactorNetwork.java` 的 `PENDING` | `IdentityHashMap<Level,…>` 强引用整个 ServerLevel | 改 `WeakHashMap`（Level 不重写 equals/hashCode ⇒ 语义等价） |

新增 `gametest/MemoryGrowthTests` **4 项**（每项在修复前都红）：线缆 BE 移除后 `EnergyNet` 立即不再跟踪；16 次"放置 + BE 移除"循环跟踪数不涨（且先断言"确实跟踪过"，避免空过）；只指向 A 的邻居条目 B 在 A 被移除后也被释放；垃圾箱塞 `MAX_ENTRIES+64` 个**各不相同 NBT** 的栈后条目数仍 ≤ 上限；烤箱镜像报告两次遍历后仍是一次遍历的量。

**诚实结论（写进交接单）**：这三处"按设计会涨"的缺陷不足以解释 9 GB（#1 需要约 2000 万条不同的 NBT 堆栈）。**决定性下一步是长会话后的 heap dump**：看 `GarbageData`/`CompoundTag`（SaveData 下）或**第三方模组缓存的 GT `LazyOptional` 句柄**谁占主导。另把 4 处"每次 `getCapability` 都新建 `LazyOptional`"（燃烧盒/漏斗/物品管/队列漏斗）记进清单待查。

### 85. 三种水＝原版水（补完：音效/动画/透明/含水方块）

上一批只改了"颜色 + 流体标签 + 属性"，实机测试仍不对，本批找到并修掉三个**各自独立的真实根因**（都对着 1.20.1/Forge 源码确认）：

| 症状 | 根因 | 修法 |
|---|---|---|
| 入水无音效、无游泳与水下动画 | Forge **把水的标签判断换成 `FluidType` 同一性**：`isInWater` → `isInFluidType(ForgeMod.WATER_TYPE)`、`isEyeInFluid(WATER)` → `isEyeInFluidType(WATER_TYPE)`（`Entity.java:3035-3040`、`:1320-1325`）⇒ GT 水报告自己的 FluidType 就没有入水声/游泳/FOV/水下叠加/气泡/溺水 | 三种水的**流体**改为报告 `ForgeMod.WATER_TYPE`（新 `registry/GTWorldWaterFluid.Source/Flowing` 覆写 `getFluidType()`）；6 处按 FluidType 注册键反查的代码改走 `GTFluids.entryForFluid(Fluid)`（GT 名称/提示/储罐元数据保住）；**顺带**：GT 水旁的岩浆现在真的生成黑曜石/圆石 |
| 液体不透明 | 1.20.1 按 `ItemBlockRenderTypes.getRenderLayer(fluidstate)` 选层，原版只登记 WATER/FLOWING_WATER 为 `translucent`、其余默认 **solid** ⇒ GT 水整片进不透明层（而第一批只覆盖了 still 名，**流动水仍是 solid**，这是被测试抓出来的半个修复） | `GregTechClient` 对**六个 id**（3 静水 + 3 流动）登记 `RenderType.translucent()`；新 `api/fluid/FluidRenderLayers.worldWaterLayers()` 成为"客户端遍历 & 测试断言"的**同一张表**，两边不可能再漂移 |
| 含水方块（海草/楼梯等） | 原版含水方块是**实例比较**：`SimpleWaterloggedBlock.canPlaceLiquid` 是 `fluid == Fluids.WATER`；`FlowingFluid` 在**两道私有门**上做这个判断——`getSpread → canPassThrough → canHoldFluid`（private，子类碰不到）与 `spreadToSides → canSpreadTo → spreadTo/placeLiquid`。上一批只拦了第二道 ⇒ 第一道就把楼梯从 `getSpread` 的图里丢掉，GT 水绕着它流 | 新 `api/fluid/VanillaWaterlogging.asVanillaWaterState(target, computed)`：**只**在"目标算出来的新液体是源"（即原版交 `Fluids.WATER` 给 `canPlaceLiquid` 的那一种情形：两侧源 + 下方实心）时把它换成 `Fluids.WATER.getSource(false)`，其余原样；GT 水覆写 `getNewLiquid`（唯一真相点），vanilla 自己的两道门与 `placeLiquid` 于是全部正确工作；流动态保持 GT 流体并被正确拒绝（负例测试）；含水方块都是 `LiquidBlockContainer` ⇒ 替换态永远不会被写成世界方块（原版水不会漏进 GT 海洋） |

**明确记录的不可消差异**：海草/海带在原版源码里**硬编码**返回 `Fluids.WATER.getSource(false)`（`SeagrassBlock:62-64`）⇒ 含水方块内部携带的仍是原版水（放置/保持/渲染一致，需 mixin 才能改）；用水桶倒在 GT 水上会把那格换成原版水（无钩子）；`Fluid.isSame` 是"族"判断 ⇒ 原版水与 GT 水之间可能出现渲染接缝（合并会改变铺展/造源语义，未做）。**残余小缺口（记档待做）**：`api/GTWaterloggable.getStateForPlacement` 仍用 `== Fluids.WATER` 判断（约 2 行改动），所以移植版自己的可含水方块**被放进** GT 水时是干的——流体驱动的那条路径已经正确。

### 86. 圆形黏土表面被草盖住

真因：**移植版自创的** `GTSurfaceDepositFeature`（水边彩色黏土缝，半径 4-7、深 2-4）在旱地上从**地表下一格**开始铺（`int startOffset = underWater ? 0 : 1;`，注释还写着"never exposed on land"），且宿主表里没有 `grass_block` ⇒ 草皮永远盖住黏土。修法：从**地表那一格**开始铺，宿主表改用与坑特征一致的 `#minecraft:dirt`（含草方块/灰化土/菌丝/粗土）；稀有度/半径/深度/水邻门槛/数据包门槛全不变。顺带核实：原版 1.20.1 的黏土只在水下生成（`MiscOverworldPlacements:79` 要求水位），GT6 自己唯一的陆地黏土是 48×48 的坑，而**GT6 的坑故意留一格草盖**（`WorldgenPit:69-74`，`CHANGELOG.md:3075` 写作 "Clay Veins below Grass"）——移植版的坑早就不留，为一致性本批也没加回来（要严格对齐是一行决定，已记档）。

### 87. 真硬币 + 88. 四个堆的贴图建模

- **GT6 没有 `coin` 前缀物品**（`OP.coin` 是 `unused`，硬币本体就是可放置的 32700 多方块）⇒ 按移植版自己的机制新增 `MaterialPrefix.coin`（条件照抄 `MultiTileEntityCoin:285-287`：能出微型板 **或** 铜/银/金/铂，权重 `U9`）⇒ **593 种材料的 `gregtech:coin_<material>`**（headless 探针漏了 `Loader_Materials` 的 STONE 标记那一步，Chalk/Dolomite/Gypsum/OilShale/Salt/Sylvite/Talc 这 7 个因此漏算；现在测试**在运行时从材料表推导**期望集合，不再钉常数）；`coin_` 开头的物品共 **595 个**，另两个是**非硬币**（`coin_pile` 堆方块物品、`coin_mold` 铸币模具），测试按 id 集合断言（多一个就报名字）。贴图 41 套（GT6 的 `COIN.png` 字节拷贝 + 透明 overlay）、41 个共享模型、双语 `item.gregtech.coin`/`itemGroup.gregtech.coin`/`tab_icon_coin`。币堆只收真硬币、绑定首次投入的金属、拒绝其它金属与堆方块本身；地牢币堆按 GT6 自己的硬币表（Cu×3/Ag×2/Au×2/Pt）绑定，且用"位置+世界种子"派生的随机流，**不动** GT6 的地牢随机序列。
- **四个堆的模型**：**260 个模型** + 4 个 blockstate（65/65/65/68 变体）+ 4 个物品模型 + **6 张 GT6 原贴图字节拷贝**，逐块复刻 GT6 的堆叠几何（锭 = `MultiTileEntityIngot.setBlockBounds` 的每层 8 个交替朝向；板/宝石板 = 4 个阶梯高度；硬币 = GT6 的**平面**硬币渲染 + `FILL` 量化高度），方块状态由 BE 驱动（`STACK` 0..64 = GT6 `mSize`、`COINS`/`FILL`），并按存入材料着色；空堆画一层 GT6 贴图（GT6 没有空堆，这是移植版新增的可放置空堆所需的显示）。GT6 的 16 面 3D 硬币位图（128×128 顶/底 + 每面高度）方块模型做不到，已注明跳过。

### 89. 地牢改用移植版储罐（不再用原版木桶）

`GTDungeonChunkRoomWorkshop` 的木桶占位全部换掉，并把 GT6 的档位/饮料掷骰/朝向掷骰恢复（新 `GTDungeonData#tank(...)`）：钢/不锈钢气压计气瓶（32055/32056 → `fluid_barometer_gas_cylinder(_stainless_steel)`，8000 mB）、陶瓷量壶（32738 → `fluid_measuring_pot`，空）、不锈钢桶（32716 → `drum_stainless_steel`，64000 mB 水）、铁木桶/处理木桶（32734/32714 → `wood_barrel_ironwood`/`wood_barrel_treated`，32000/16000 mB 掷出的饮料）；木瓶箱 8762 移植版无对应 ⇒ `GTStorage.DRAWER_QUAD`（保留朝向掷骰）。坐标为/朝向/写入顺序不变。

### 90. 手摇曲柄

真因是**实现里的方向约定反了**（读 1.20.1 `SignalGetter` 源码确认）：`getBestNeighborSignal(pos)` 传的方向是**从询问者指向发出者**，而原实现答的是"曲柄装在机器上的那一侧" ⇒ 手柄正前方永远 0（它自己的断言也用错了约定，所以"自洽地"通过）。改为 GT6 的单侧输出（`ACTIVE && side == FACING.getOpposite()`）、`getDirectSignal` 同步、激活后补 `updateNeighborsAt`；**删掉两条 GT6 根本没有的聊天提示**（"要装在 RU 机器上"/"机器满了"）。右键现在总是摇动曲柄并发出 10 tick 红石脉冲（自由站立时就是拉杆），装在 RU 机器上时仍按 GT6 注入 −16 RU 包。测试同时断言"询问方向"与"真实邻居"（`getBestNeighborSignal(front)==15`、安装侧 0、脉冲结束后归 0）。另修一处**测试自身的错误**：`energyInMax` 默认是 `energyIn × 2`，"机器已满"用例其实只填到一半。

结果：**534 项 GameTest 全过**（513 → 534）、物品 **70,160 → 70,754**（+593 硬币 +1 创造栏图标物品）、方块 12,093 / 材料 1156 / 流体 925 不变、缺失 token 仍 **49**。

**没有执行 `git commit`。**

---

## 91. 续批：GT6 的**伤害 / 危害体系**（15 个伤害类型 + `UT.Entities` 危害 API + 六个真实接入点）

§2/§3 一直记着"原版 15 个伤害源（电击、酸、热、辐射…）不完整"，实际清点后：移植版当时**只有 `heat` 一个**（`damage/GTDamageTypes.java` 21 行），而危险本身全在——炽热的燃烧盒、裸导线、热/冷流体管道、运行中的反应堆、冶炼模具、手持的炽热锭。这批把数据、API 与接入点一次补齐。

### 91.1 数据层：生成器产出 15 个伤害类型 + 原版伤害标签 + 28 条死亡消息

新 `tools/generate_damage_types.py`（GitHub 惯例的"生成 > 手写"）从 GT6 `gregapi/damage/DamageSource*.java` 的**源码事实**产出：

| GT6 类 | id | GT6 标志 | → 1.20.1 标签 | 死亡消息（GT6 原文） |
|---|---|---|---|---|
| `DamageSourceHeat` | `heat` | — | — | was boiled alive |
| `DamageSourceFrost` | `frost` | — | `is_freezing` | got frozen |
| `DamageSourceChem` | `chemical` | — | — | had a chemical accident |
| `DamageSourceBumble` | `bumble` | `setDamageBypassesArmor` | `bypasses_armor` | was allergic to Bumblebees |
| `DamageSourceCrusher` | `crusher` | — | — | was crushed to a pulp |
| `DamageSourceShredder` | `shredder` | — | — | was shred into flakes |
| `DamageSourceSpike` | `spike` | — | — | was impaled by a Spike! |
| `DamageSourceExploding` | `exploded` | bypassesArmor + IsAbsolute + AllowedInCreative | `bypasses_armor`+`bypasses_resistance`+`bypasses_enchantments`+`bypasses_invulnerability`+`is_explosion` | exploded |
| `DamageSourceAlcohol` | `alcohol` | bypassesArmor + IsAbsolute | 三个 bypasses | died from alcohol poisoning |
| `DamageSourceCaffeine` | `caffeine` | 同上 | 同上 | overdosed on caffeine |
| `DamageSourceDehydration` | `dehydration` | 同上 | 同上 | took more than the deadly dose of Salt |
| `DamageSourceSugar` | `sugar` | 同上 | 同上 | died of Diabetes |
| `DamageSourceFat` | `fat` | 同上 | 同上 | got a Heart Attack |
| （IC2 `DMG_ELECTRIC`） | `electric` | — | — | was electrocuted（移植版自拟） |
| （IC2 `DMG_RADIATION`） | `radiation` | — | `bypasses_armor` | was irradiated（移植版自拟） |

- `message_id` 用现有的命名空间约定 `gregtech.<id>`（`heat.json` 本来就是这么写的）⇒ 死亡消息键 `death.attack.gregtech.<id>`，`en_us.json`/`zh_cn.json` **各 +14 条**（8841 → 8855），中文按 GT6 语义译。
- 后两个是**有意替代**：GT6 的 `getElectricDamage`/`getRadioactiveDamage`/`getNukeExplosionDamage` 转交给 IC2，没有 IC2 时**退回 heat** ⇒ 原文会说触电与堆芯熔毁都是"被活活煮死"。移植版没有 IC2，于是自己注册这两个类型，并在 javadoc 与文档里写明这是替代而非照抄。GT6 的 `getCombatDamage`（每把武器的自定义死亡消息）**未做**：1.20.1 每条消息需要一个已注册的伤害类型，属于武器批次。

### 91.2 API 层：四个新类 + 一个重写

| 文件 | 内容 |
|---|---|
| `damage/GTDamageTypes.java`（重写，21 → 130 行） | 15 个 `ResourceKey<DamageType>` + 15 个工厂方法（`GT6 DamageSources.get*Damage` 的移植版）+ `ALL` + `source(level,key)` |
| `damage/GTHazmat.java`（新） | GT6 `ArmorsGT.HAZMATS_*` 八套危害服的检查：`isWearingFull*Hazmat` 系列 + GT6 硬编码的免疫名单（热：凋灵/烈焰人/僵尸猪灵/岩浆怪/恶魂；生物/昆虫/辐射/气体：凋灵/铁傀儡；创造模式全免疫）。套装用**物品标签**表达（复用已有的 `gregtech:radiation_protection`，新增 `hazmat_chem/heat/frost/electric/bio/insect/gas` 七个空标签），`isImmuneToBreathingGases` 随之就位 |
| `api/energy/GTVoltageTiers.java`（新） | GT6 `CS.V` 的 16 档电压表 + `UT.Code.tier/tierMax/tierMin` + `maxVoltageOf`/`nameOf`。**关键语义**：`tierMax` 返回的是**档位下标**而不是电压，所以 GT6 的电击伤害 `tierMax(电压) × 电流 × 4` 在 ULV（8 V，下标 0）**恰好为 0** |
| `util/GTEntityHelper.java`（重写，39 → 180 行） | `UT.Entities` 危害半边全部补齐：`applyChemDamage`（附加 `Potion.poison`，时长 `max(20, 伤害×100 + 已有剩余)`、等级 II；**只**跳过硬编码的 `EntitySkeleton` 精确类）、`applyHeatDamage`、`applyFrostDamage`、`applyElectricityDamage(电压,电流)` 与 `(瓦数)` 两个重载、`applyTemperatureDamage` 三个重载（含 GT6 管道用的 capped 版）、`applyContactTemperatureDamage`、`isCreative`/`isInvincible`/`isImmuneToBreathingGases`、`heatDamageFromItem` |

### 91.3 六个真实接入点（"从 API 变成玩法"的部分）

| # | 接入点 | GT6 出处 | 移植版改动 |
|---|---|---|---|
| 1 | **裸导线触电** | `MultiTileEntityWireElectric:203`：`onEntityCollidedBlock` → `applyElectricityDamage(entity, mWattageLast)` | `ElectricWireBlockEntity` 记 `lastWattage`（= 电压×电流，GT6 的 `mWattageLast`，不落盘）；`ElectricWireBlock.entityInside` 改用它。**原来的自造档位（按额定电压 2/4/8/20 + 原版 `generic` 伤害源）整段删掉** ⇒ 现在同一根线"没电流不电人、ULV 不电人、512 V 一次 12 点"，且绝缘线缆永不电人 |
| 2 | **热/冷管道烫伤冻伤** | `MultiTileEntityPipeFluid:460`：`applyTemperatureDamage(entity, mTemperature, 1, 5.0F)` | 新 `FluidPipeBlock.entityInside`（BE 本来就在跟踪管内温度并向环境漂移）⇒ 满满一管熔融金属会烫人，液氮会冻人 |
| 3 | **反应堆本体接触** | `MultiTileEntityReactorCore:303`：运行时 `applyHeatDamage(entity,5)` + `applyRadioactivity(entity,3,1)` | 新 `content/nuclear/ReactorHazards.contact`，由 `ReactorCoreBlock`/`ReactorCore2x2Block.entityInside` 调用（GT6 那个类正是 1x1/2x2 的公共基类）。径向辐照（`ReactorCoreBlockEntity.irradiate`）早已移植，这批补的是"贴上去" |
| 4 | **炽热工件烫手** | `SmelteryBlockEntityHelper.applyHeatDamage` 自造公式（320 K 阈值、`(T-320)/100`、上限 8） | 换成 GT6 唯一那条规则 `applyTemperatureDamage(T, 1, 5)` ⇒ 与管道、燃烧盒共用同一套数值，并顺带获得创造模式/防热服/烈焰人/抗火药剂豁免。原来"同一块熔融金属在模具上是 6.8 点、在管道上是 5 点"的不一致消失 |
| 5 | **重症辐射伤害** | GT6 把这件事交给 IC2（`getRadioactiveDamage`） | `PlayerRadiation.applySymptoms` 在剂量 ≥ 100（`SEVERE_DOSE`）时施加 `radiation` 伤害 1 点/症状 tick（50 tick 一次）⇒ 满剂量 127 的人每 2.5 秒多掉 2 HP，走 GT 的 `death.attack.gregtech.radiation` 死亡消息 |
| 6 | **手持炽热锭** | `OP.java:578` `ingotHot.mHeatDamage = 3.0F` + `GT_API_Proxy:817` 的背包扫描 + `UT.Entities.getHeatDamageFromItem` | 新 `MaterialPrefix.heatDamage` 字段（`ingotHot = 3.0F`，在静态块里按 GT6 的位置赋值）+ `GTEntityHelper.heatDamageFromItem`（GT6 `OM.anydata` 的对应物：`MaterialItem` 走实例前缀，统一的原版物品走 `ItemMaterialRegistry`——**这两条路都必须走**，因为移植版的材料物品不进 `ItemMaterialRegistry`，这是本轮测试抓到的） |

### 91.4 有意保留 / 未做（逐条记档，不是"假装做完了"）

- **食物过量的 5 种伤害**（酒/咖啡因/糖/脂肪/脱水）类型已注册，但 GT6 的 `EntityFoodTracker`（204 行，含 5 条阈值与死亡分支）未移植 ⇒ 目前**没有调用点**，属于下一批（顺带能把 GT6 的食物统计整块做掉）。
- **尖刺方块**：GT6 有 5 个 `BlockSpike*`（13 处调用），移植版没有这些方块 ⇒ 无接入点。
- **粉碎机/撕碎机内部伤害**（`MultiTileEntityCrusher:181` / `MultiTileEntityShredder:181`，5 点）：需要多方块控制器按 tick 扫描内部实体，属于多方块批次。
- **酸液浸泡 / 管道漏酸**（`BlockBaseFluid:406`、`MultiTileEntityPipeFluid:311`）：需要"流体是酸/气体/等离子"与"管道是否耐酸/耐气/耐等离子"两组标记，移植版两者都没有 ⇒ 记档。
- **危害服只移植了铅/辐射服**：GT6 的其余七套来自 IC2 等模组（`LoaderItemList:2267+` 直接登记别的模组的物品）⇒ 七个新标签现在是空的，检查逻辑已就位，将来做套装只需往标签里加物品。
- **`GTEntityHelper` 的 `TFC_DAMAGE_MULTIPLIER`**：GT6 有个 TFC 联动的伤害倍率配置，移植版没有该配置 ⇒ 按 1 处理（javadoc 写明）。

### 91.5 本轮踩到的门禁/环境坑（已写进交接单）

1. **门禁 JVM 因"提交限额"起不来**：这台机器上同时在跑游戏（commit 用量 62/66 GB，可用提交 3.4~6 GB），4G/3G 堆的 G1 版门禁直接 `mmap failed to map 1002438656 bytes for G1 virtual space`（`os::commit_memory ... errno=1455`）⇒ 解法是 **`gradlew --stop` 回收守护进程 + `--no-daemon` + `-PgameTestJvmArgs=-XX:+UseSerialGC`**（G1 的大块虚拟预留是触发点，SerialGC 不预留）。为此 `build.gradle` 新增 `gameTestJvmArgs` 属性（不传时行为与以前完全一致）。
2. **2G 堆跑不完套件**（`OutOfMemoryError: Java heap space`）⇒ 这台机器上 3G 是下限。
3. **被 OOM 中断的门禁会留下半个世界**（本轮第一次 2G 运行 OOM 后，下一轮的 `AnvilSmashingTests.oreFormsSmashWithAHammer` 开始红）⇒ 与已知的"测试数变化要换世界"合并处理：把 `build/gametest-run/world` 挪走再跑。
4. `BlockPlaceContext(level, null, …)` 在 1.20.1 会让**原版** `getNearestLookingDirection()` NPE（`Entity.getViewXRot`）⇒ 要建放置上下文就传一个 mock 玩家，别偷懒传 null。
5. 用**牛**做"精确伤害"靶子是错的：12 点以上的伤害会把它打死并把血量夹在 0，血量差就失真了 ⇒ 精确数值断言用铁傀儡（100 HP、0 护甲）。

测试：新增 `HazardDamageTests` **11 项**（15 个伤害类型全部注册且互异、`msgId` 命名空间、GT6 标志→原版标签逐条（含反向断言"普通危害不得穿甲"）、中英死亡消息逐条 + GT6 原文校验、电压档表与 `tierMax/tierMin`、电击公式（8 V 为 0 / 32 V×1 A = 4 / 32 V×2 A = 8 / 512 V = 12 / 2048 EU 为 16）与创造/防电豁免、温度三档（1000 K 上限 5、320 K 与 300 K 不动、100 K 冻结、无上限版 14、烈焰人与抗火豁免）、化学伤害的伤害+中毒等级/时长与骷髅免疫、炽热锭前缀 3.0、危害服标签（辐射服 4 件、其余 7 个标签为空、缺一件即失效、创造全免疫）、裸导线按载流瓦数电人且绝缘线缆不电人、热/冷管道 5/5/0、运行中堆芯接触 5 点+辐照且停机无伤害+铅服挡住剂量）；`WaterFluidParityTests` 扩到 6 项（新增放置路径含水断言）。

结果：**546 项 GameTest 全过**（534 → 546，+12）、物品 **70,754** / 方块 **12,093** / 材料 **1156** / 流体 **925** 均不变（这批全是数据与行为，没有新注册物）、缺失内容 token 仍 **49**；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `175B605840D4DB7E478ABE0269D069A7EB8A922CFDC473B87F80C6599AC9EB74`）。

**没有执行 `git commit`。**

---

## 92. 续批：GT6 的**食物 / 营养系统**（`EntityFoodTracker` 五条统计 + `FoodsGT` 表 + 进食入口）

§91 把 GT6 的 15 个伤害类型全部注册好之后，**酒精 / 咖啡因 / 脱水 / 糖 / 脂肪**这五种没有调用点——GT6 把它们挂在 `EntityFoodTracker` 上，而移植版没有食物系统。这批把整条链补齐：吃了什么 → 统计值上涨 → 每 50 tick 按 25/50/75/100 四档给效果 → 满 100 时用它自己的伤害类型扣 2 点血 → 每 100 tick 衰减 1（**辐射永不衰减**）。

### 92.1 数据层：`tools/extract_gt6_food_stats.py` → 20 行真表 + 47 条记档

GT6 的 `CS.FoodsGT` 是 `ItemStackMap<ItemStackContainer, int[]>`，六元组顺序是 **酒精 / 咖啡因 / 脱水 / 糖 / 脂肪 / 辐射**（`CS.java:1592-1599`），由三处填充：`MultiItemFood` 的逐物品登记、`Loader_Recipes_Food` / `Loader_Recipes_Crops` 的**矿物词典监听器**（`addListener("listAllbeefraw", …)` 这种块），以及 `RM.crop` / `RM.crop_nut` 的末尾五个参数（`RM.java:766-776`）。移植版没有矿物词典，生成器把监听器名映射到 1.20.1 里真正存在的物品上：

| GT6 名 | 1.20.1 对应 | 酒精/咖啡因/脱水/糖/脂肪 |
|---|---|---|
| `Items.rotten_flesh` | 腐肉 | 10 / 0 / 0 / 0 / 8 |
| `Items.mushroom_stew` | 蘑菇煲 | 0 / 10 / 0 / 5 / 0 |
| `Items.cookie` | 曲奇 | 0 / 0 / 0 / 10 / 0 |
| `cropCocoa` | 可可豆 | 0 / 0 / 4 / 4 / 0 |
| `Items.apple` / `Items.carrot` | 苹果 / 胡萝卜 | 0 / 0 / 0 / 8 / 0 |
| `Items.potato` / `baked_potato` / `poisonous_potato` | 三种马铃薯 | 0 / 0 / 0 / 4 / 0 |
| `listAllbeefraw` / `listAllporkraw` / `listAllmuttonraw` | 牛 / 猪 / 羊（生） | 0 / 0 / 0 / 0 / 16 |
| `listAllchickenraw` / `listAllrabbitraw` / `listAllfishcooked` | 鸡 / 兔（生）、熟鱼 | 0 / 0 / 0 / 0 / 12 |
| `foodScrapmeat` | 腐肉（碎肉） | 0 / 0 / 4 / 0 / 12 |

- **20 行**进表，**47 条逐条记档跳过**：GT6 自己的奶酪 / 豆腐 / 焦糖 / 太妃糖 / 玉米 / 咖啡 / 茶 / 椰子 / 辣椒 / 肉桂 / 咖喱叶 / 胡椒与 **15 种坚果**，以及其它模组的火鸡 / 蟹 / 鼠 / 龟 / 鸵鸟 / 鹿 / 泰坦 / 火腿 / 马 / 狗 / 九头蛇肉。理由写进生成的 `SKIPPED` 列表，测试断言"少于 40 条就算漏了"。
- GT6 的 `Compat_Recipes_*` 也填这张表，但只填**其它模组**的物品（神秘时代鸡块、收获工艺沙拉）⇒ 生成器**故意不读**它们，并在文件头写明原因。
- 生成的 `data/generated/GTFoodStatsGen.java` 带 GENERATED 头；生成器有 `--check` 模式，测试与门禁都能验它没落后于 GT6 源码。

### 92.2 状态层：`PlayerFoodStats`（GT6 `EntityFoodTracker` 的移植）

| 项 | GT6 | 移植版 |
|---|---|---|
| 存档 | `gt.props.food` 复合下的 byte 键 `a`/`c`/`d`/`s`/`f`（辐射是第六个键 `r`） | **完全一致**（Forge 持久数据里的同一复合名与同一批短键）；测试直接读 `ForgeData.gt.props.food.a` 断言结构，不只断言数值 |
| 取值范围 | `UT.Code.bind7` 0..127 | 一致（增量先 clamp 再相加，溢出不了） |
| 效果节奏 | `SERVER_TIME % 50 == 0` | 每个玩家的 `gameTime % 50`（同一节奏） |
| 衰减 | `% 100 == 0` 每项减 1；**辐射不减**（`EntityFoodTracker:193` 的注释：只能靠 Radaway 或死亡清掉） | 一致，并写进测试 |
| 过量伤害 | `if (FOOD_OVERDOSE_DEATH \|\| health >= 2) hurt(源, FOOD_OVERDOSE_DEATH ? 2 : 1)` | GT6 两个配置的默认值都是 `true`（`GT_API.java:511-512` — `DeathByOverdosingCertainFoods` / `NutritionSystem`）⇒ 恒 2 点；另一分支保留在代码里可见 |
| 死亡 | 1.7.10 的 `IExtendedEntityProperties` **不随重生复制** ⇒ 死亡即清零 | `PlayerEvent.Clone`：死亡清空、换维度保留（与 `PlayerRadiation` 同套路） |

阈值表（GT6 `EntityFoodTracker:84-185` 逐条抄，时长 1200 / 300 tick、等级 0..3）：

| 统计 | ≥25 | ≥50 | ≥75 | ≥100 |
|---|---|---|---|---|
| 酒精 | 力量 I (300) | + 反胃 I (1200) | 反胃 II + 力量 III | 反胃 III + 力量 IV + **2 点酒精伤害** |
| 咖啡因 | 急迫 I (300) | + 虚弱 I (1200) | 虚弱 II + 急迫 III | 虚弱 III + 急迫 IV + **2 点咖啡因伤害** |
| 脂肪 | 抗性 I (300) | + 缓慢 I (1200) | 缓慢 II + 抗性 III | 缓慢 III + 抗性 IV + **2 点脂肪伤害** |
| 糖 | 速度 I + 跳跃 I (300) | + 疲劳 I (1200) | 疲劳 II + 速度 III + 跳跃 III | 疲劳 III + 速度 IV + 跳跃 IV + **2 点糖伤害** |
| 脱水 | 饥饿 I (1200) | 饥饿 II | 饥饿 III | 饥饿 IV + **2 点脱水伤害** |

### 92.3 入口层：`FoodStatEvents`（GT6 `GT_API_Proxy:995-1010`）

- **进食/饮用**：`LivingEntityUseItemEvent.Finish`（1.20.1 里就是 GT6 的 `PlayerUseItemEvent.Finish`，**优先级 `LOWEST` 也照抄**）→ 查表 → 六项分别累加；辐射那一项交给 `PlayerRadiation.change`（GT6 的 `changeRadiation`）。
- **节奏**：`TickEvent.PlayerTickEvent` 里做每 50 tick 的效果/伤害与每 100 tick 的衰减（GT6 是全局 `SERVER_TIME` 门控的列表，等价）。
- **跳过的输入**：GT6 还有"瓶装饮料"这条路（流体的 `mAlcohol` 等 + `FoodsGT` 的瓶装条目）。移植版的酒/咖啡/巧克力奶流体与 `FluidFlags.ALCOHOLIC` 标记都在（245 个含"酒"的流体），但**没有可饮用的瓶子物品** ⇒ 记档，等瓶子批次。

### 92.4 未做（记档，不是"忘了"）

- **瓶装饮料**：见上（需要先有 GT6 的可饮用瓶子/罐装物品）。
- **GT6 的食物与农作物本身**（奶酪 / 豆腐 / 焦糖 / 太妃糖 / 咖啡 / 茶 / 20 种坚果作物）：整条农作线未移植 ⇒ 对应的 47 条统计记档。
- **`mHydration` / `mTemperature`**：GT6 的 `FoodStat` 还有水合与食物温度两个维度（`getHydration`/`getTemperature`，吃东西会临时改变体温与水合），移植版没有口渴/体温系统 ⇒ 未做。

### 92.5 本轮抓到的 API 坑（值得记档）

- **`PlayerEvent.Clone(newPlayer, oldPlayer, wasDeath)` 的参数顺序与字段名相反**：从 Forge 源码 jar 里读到的是 `public Clone(Player _new, Player oldPlayer, boolean wasDeath) { super(_new); … }` ⇒ **第一个参数是新玩家**（`getEntity()` 返回它），第二个才是 `getOriginal()`。本轮测试先按"直觉"写成 `(old, new, true)`，于是"死亡清零"断言红了一次（清掉的是旧玩家）。既有测试 `NuclearSurvivalTests:49-52` 恰好也是靠这个反直觉顺序才绿的——写新代码时务必照源码写。

测试：新增 `FoodStatsTests` **6 项**（20 行真表逐行核对 GT6 数值 + "GT6 没登记的物品没有统计" + 47 条跳过记档；进食事件把腐肉/蘑菇煲/曲奇的统计加上去且不在表里的面包什么都不改、累加封顶 127；五条统计的四档阈值逐条断言（含 100 档的 2 点伤害**且伤害类型就是 GT 自己的** `gregtech:alcohol`/`caffeine`/`sugar`/`fat`/`dehydration`）、25 以下完全静默；衰减每轮减 1 且到 0 停、辐射不衰减；`gt.props.food` 的 a/c/d/s/f 短键结构 + 存档往返 + 死亡清零/换维度保留；两个配置默认值与 20 行都是已注册物品且至少一项非零）。

结果：**552 项 GameTest 全过**（546 → 552）、新增 `FoodStatsTests` 6 项、物品 **70,754** / 方块 **12,093** / 材料 **1156** / 流体 **925** 均不变（这批是行为 + 一张生成表，没有新注册物）、缺失内容 token 仍 **49**；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `4EAD8559E7FE6020122820459501B4641FE59CDD4147A42509FBCBDC6687481E`）。

**没有执行 `git commit`。**

---

## 93. 续批：GT6 的**饮品**（`DrinksGT` 表 + 饮用路径）——§92 的"喝"这条入口

§92 把食物统计做完，但只接了"吃"；GT6 还有一条**喝**的路：`Loader_Fluids` 给每个可饮用流体登记一个 `FoodStatDrink`，构造函数把它塞进 `DrinksGT.REGISTER`（`FoodStatDrink.java:46-52`），而 GT6 的瓶子（`MultiItemBottles`，每瓶 250 mB）通过 `FoodStatFluid` 按流体名查这张表。

| 层 | 交付 |
|---|---|
| 数据 | 新 `tools/extract_gt6_drinks.py`（`--check` 可验）解析 GT6 `Loader_Fluids.java` 的 `new FoodStatDrink(...)` ⇒ `data/generated/GTDrinksGen.java`：**272 行**（流体 + 食物等级 + 饱和度 + 酒精/咖啡因/脱水/糖/脂肪/辐射），**73 条记档跳过**（原版药水酿造用的是另一种构造形态、以及 10 来条流体表达式不是 FL 字段/名字的行）。注意生成器必须**认得字符串里的逗号**（GT6 的 tooltip 里就有逗号，第一版把 "BAWLS" 那种行切错了） |
| 表 | 新 `content/food/GTDrinks.java`：GT6 的 `DrinksGT` 移植版（按 `Fluid` 键），解析顺序 = ①`GTFluids.still(field)`（GT6 字段名，例如 `Beer` 行的键是注册名 `beer`）②`RegisteredFluids.get(field)` ③按 `sanitizePath` 比对注册路径；`fluidForField(String)` 供测试/工具用 |
| 饮用 | `FluidItem.use`（右键）：该流体有饮品行时按 GT6 的 `FoodStat.onEaten` 处理——**食物等级 + 饱和度**走原版食物数据，六项统计进 `PlayerFoodStats`（辐射那项进 `PlayerRadiation`），播放原版饮用音效；**消耗一瓶并退回一个空玻璃瓶**（GT6 瓶子的语义）；不是饮品的流体 `pass` |
| 测试 | 新 `DrinkTests` 4 项：表解析（`registered() > 100`、啤酒 food 6/alcohol 30、咖啡 caffeine 30 + dehydration 15、红酒 alcohol 30 + sugar 10、非饮品无行）、喝啤酒涨酒精并回食物、喝咖啡涨咖啡因与脱水、右键瓶子喝掉并得到空瓶、非饮品不消耗 |

**有意保留的差异（§93.4）**：GT6 的瓶子是独立的 `MultiItemBottles` 物品族（`assets/.../item/bottles` 贴图已在仓库里），移植版暂时用它**已有的 per-fluid `FluidItem`** 承担饮用（每个 GT 流体本来就有对应物品），因此"一瓶 250 mB"这件事只体现在饮用数值上（`drink(player, new FluidStack(fluid, 250))`），瓶子物品族与它的贴图/模型留待后续批次；原版药水酿造（`potion.*` 那 70 余行）也一并记档。

**门禁过程**：这批门禁红了 **5 轮**，全是**测试侧的流体解析**问题（`RegisteredFluids.get("Beer")` 取不到——表是按 GT6 字段/注册名两种键混着来的；`GTFluidItems.forFluid(null)` 会直接 NPE 而不是返回 null；`new FluidStack(null, …)` 同样 NPE），实现本身一次没红。结论：**测试要用生产代码同一条解析路径**（`GTDrinks.fluidForField`），不要自己拼名字。

结果：**556 项 GameTest 全过**（552 → 556）、新增 `DrinkTests` 4 项、注册量 70,754 / 12,093 / 1156 / 925 不变、缺失 token 仍 49；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `BF26FA87461C30136C6874AC27498293C619025FFC337A7EB2A2DAE458C216A8`）。

**没有执行 `git commit`。**

---

## 94. 续批：§93 饮品的**收口**（物品代理覆盖统计 + 门禁规矩固化）

§93 把 `DrinksGT`（272 行）与饮用路径做完，留下一条明确差异：GT6 的瓶子是独立的 `MultiItemBottles` 物品族（每瓶 250 mB，贴图已在 `assets/.../item/bottles`），移植版暂用自身的 per-fluid `FluidItem` 承担饮用。这批**不加新物品**（不动注册量），只把差异量化并钉进门禁：

- 新 `DrinkTests.drinkFluidsHaveItemProxies`：遍历 10 个代表性 GT6 饮品字段（`Beer`/`DarkBeer`/`Wine_Grape_Red`/`Wine_Grape_White`/`Coffee`/`Juice_Apple`/`Juice_Orange`/`Milk`/`ChocolateMilk`/`Water`），统计**能解析到流体**的数量与**有物品代理**的数量，断言"至少有一种可喝"且"代理数 ≤ 饮品数"，并把 `proxies N of M` 写进断言消息（绿时不打印；一旦将来某批把代理改坏，会立刻带着数字变红）。这条为后续"瓶子物品族"批次提供基线。
- **门禁规矩固化**（§93 红了 5 轮的根因）：GT6 的饮品表键一半是 `FL` 字段名（`Beer`）、一半是 `FL.create("…")` 的注册名（`potion.coffee`、`beer`）⇒ **测试必须走生产代码同一条解析路径**（`GTDrinks.fluidForField`）；`RegisteredFluids.get("Beer")`、`GTFluids.still("Beer")`、`GTFluidItems.get("beer")` 都可能取到 null，而 `new FluidStack(null, …)` 与 `GTFluidItems.forFluid(null)` **会直接 NPE**（`ItemLike.asItem() because "p_41604_" is null`），构造前必须判空并跳过。
- 交接单新增该陷阱条目；`PORTING_AUDIT.md` 的横幅改为**由脚本整行前置**（本轮用 `tools/_prepend_banner.py` 前置后即删），从根上避免"只替换横幅开头 ⇒ 撕出孤儿行"这个已连踩两次的坑。

结果：**557 项 GameTest 全过**（556 → 557）、新增 `DrinkTests.drinkFluidsHaveItemProxies` 1 项、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `AC50F7B37D9BFDEFA7302970FB2BF8EECF29BE9C4156BE1E52107AA9C20567C5`）。

**没有执行 `git commit`。**

---

## 95. 续批：§93 饮品链的**基线数据**（把"代理覆盖"从断言变成日志确数）

§94 把"饮品物品代理覆盖"写成断言，但数字只在失败时可见。这批把 `DrinkTests.drinkFluidsHaveItemProxies` 改成把 10 个采样字段分成 `withProxy` / `withoutProxy` 两组**记进门禁日志**，并断言两组之和等于可解析总数（信息不再藏在失败消息里）。

本轮门禁实测（`build/gametest.log`）：

```
[drinks] resolvable drinks: 9 of 10 sampled; with a per-fluid item proxy:
         [Beer, DarkBeer, Wine_Grape_Red, Wine_Grape_White, Coffee, Juice_Apple, Juice_Orange, Milk, ChocolateMilk];
         without: [Water]
```

**结论（对下一批很重要）**：唯一解析不到的是 `Water`（它本来就不是 GT 饮品），而**其余 9 种饮品全都有 per-fluid 物品代理** ⇒ §93 记的"GT6 瓶子物品族未移植"这条差异，在**数值与可达性**上都不是阻塞项——右键就能喝、统计与食物等级都生效；只差 GT6 原版的**瓶子外观**与**250 mB 容器语义**（一次喝掉 250 mB 而不是无限次使用同一件物品）。下一批若做 `MultiItemBottles`，只需新增物品族与贴图（`assets/.../item/bottles` 已在仓库），不必改 `GTDrinks` 表。

结果：**557 项 GameTest 全过**（数量不变，这批只改测试与日志）、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `B9BFD3A22A5F2758B1B628C8D4D9824425CF66CB8D09C2B48164803F2A96FC82`）。

**没有执行 `git commit`。**

---

## 96. 续批：饮品表的**覆盖确数**（265/272）+ 未解析行不再静默丢弃

§95 只把"10 个采样字段"记进日志；这批把**整张表**的覆盖情况变成不变量：`GTDrinks` 现在把解析不到流体的行收进 `unresolved()`（移植版没有对应流体的 GT6 饮品行，例如别家模组的酒/名字未在移植版注册的行），`DrinkTests.drinkTableMatchesGt6` 断言

```
GTDrinks.registered() + GTDrinks.unresolved().size() == GTDrinksGen.ROWS.size()
```

即**每一行生成数据要么解析到流体、要么被明确列为未解析**，并把两个数同时写入日志。本轮门禁实测：

```
[drinks] resolvable drinks: 9 of 10 sampled; with a per-fluid item proxy: [Beer, DarkBeer, Wine_Grape_Red,
         Wine_Grape_White, Coffee, Juice_Apple, Juice_Orange, Milk, ChocolateMilk]; without: [Water]
[drinks] table: 265 of 272 GT6 rows resolve onto port fluids; unresolved: 7
```

**结论**：GT6 的 `DrinksGT` 在移植版里 **265/272 行可用**（97.4%），未解析的 7 行是移植版没有对应流体的行（不改数据、不硬编造）；结合 §95 的采样结果（9/10 有 per-fluid 物品代理，唯一没有的 `Water` 本来就不是饮品）⇒ **饮品这条链在数值与可达性上已经闭合**，剩下的只是 GT6 原版瓶子族的外观与 250 mB 容器语义。

结果：**557 项 GameTest 全过**（数量不变，这批加了 1 条不变量与 2 行日志）、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `83EFE129762F9F38299DF2E1DEA3FB63AE44BF9DB860CB43985E9242CF29C3A6`）。

**没有执行 `git commit`。**

---

## 97. 续批：把 §84 内存审计里"记档未改"的 **4 处 `getCapability` 每次新建 `LazyOptional`** 修掉

§84 的内存审计把四处"每次查询都新建 capability 句柄"列进清单但没改（当时缺 heap dump 证据）。这批把它们改成 Forge 推荐写法——**每个方向缓存一个 `LazyOptional`，并在 `invalidateCaps()` 里统一失效**：

| 文件 | 改法 |
|---|---|
| `blockentity/machine/ItemPipeBlockEntity` | 新增 `EnumMap<Direction, LazyOptional<IItemHandler>> sideCaps`，按方向 `computeIfAbsent`；`handler`（无侧面查询）保持原样；`invalidateCaps()` 失效并清空 map |
| `blockentity/machine/HopperBlockEntity` | 同上（`SidedHandler(side)` 按方向缓存） |
| `blockentity/machine/QueueHopperBlockEntity` | 同上 |
| `blockentity/machine/BurningBoxBlockEntity` | 两个与方向无关的 sink 各缓存一个句柄（`itemCap`/`fluidCap`），`invalidateCaps()` 里失效并置空 |

**为什么值得改**：管道/漏斗/燃烧盒每 tick 都会被邻居（以及第三方模组的能力缓存）查询一次 `getCapability`，原来的写法**每次查询 new 一个 `LazyOptional` 与一个 `SidedHandler`**——第三方只要把句柄存进自己的缓存，就会留住这些对象，正是 §84 结论里"9 GB 需要 heap dump 才能定性"的候选之一。改完后同一方向返回**同一实例**（`LazyOptional.cast()` 是纯类型转换，不新建对象），方块被移除/能力失效时统一 `invalidate()`。

**验证**：`compileJava` 通过、**557 项 GameTest 全过**（管道/漏斗/燃烧盒的既有测试本来就大量走 `getCapability` 路径）；**尚未加**针对"同一方向返回同一实例"的专门断言（需要按方块对象放置这四台机器，留待下一批，已记档）。

结果：**557 项 GameTest 全过**、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `A56B51C00C10AC9D331DE09BB3935AB598915201DDFDBB0F37E92BA13E1AA7E0`）。

**没有执行 `git commit`。**

---

## 98. 续批：§97 的收尾断言（capability 句柄**稳定性**的专门测试）

§97 改了四处 `getCapability` 的句柄缓存但只由既有测试间接覆盖，这批补上专门断言：新 `gametest/CapabilityHandleTests`（**3 项**）放置机器后断言

- **同一方向两次查询返回同一 `LazyOptional` 实例**（`first == second`，§97 之前每次查询都是新对象）；
- 有侧面语义的机器**按方向缓存**（`Direction.NORTH` 与 `SOUTH` 的句柄不是同一个）；
- `invalidateCaps()` **确实失效**已发出的句柄（`isPresent()` 变 false）。

| 机器 | 结果 |
|---|---|
| 漏斗 `MachineRegistry.hoppers()` / 队列漏斗 `queueHoppers()` | **断言全过**（按方向缓存 + 失效行为都被钉住） |
| 固体燃烧盒 `solidBurningBoxes()` | **只断言注册存在**：门禁实测发现它的固体变体在**任何方向都不暴露物品处理器**（燃料走自己的通道），于是"比较句柄"无对象可比 ⇒ 该变体的两个与方向无关的缓存句柄（`itemCap`/`fluidCap`）仍由代码与燃烧盒既有测试覆盖，并把这处"没有可断言对象"的原因写进测试 javadoc（不是静默跳过） |

**门禁过程**（3 红 1 绿，全是测试假设错，实现一次没错）：① 以为燃烧盒在 UP 侧提供物品处理器 ⇒ 实际前面被拒、固体变体根本不给；② 以为它按方向缓存 ⇒ 它的两个 sink 是**与方向无关**的、故意共用一个句柄；③ 失效后以为能重新拿到句柄 ⇒ 同上原因拿不到。结论记档：**写 capability 测试前先读该机器的 `getCapability` 门控条件**。

结果：**560 项 GameTest 全过**（557 → 560）、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `5BDD20A74727C8D87D2935AE7C0C5DD24A9C6E9BD8245B9E6DCA8EE2B2CD65BD`）。

**没有执行 `git commit`。**

---

## 99. 续批：把**物品管道**也纳入句柄稳定性断言（§98 漏掉的那一台）

§97 改了四处句柄缓存，§98 断言了漏斗与队列漏斗，但**漏了物品管道**——而它恰恰是"每 tick 被所有邻居查询"最频繁的那一台，且它的处理器是**按方向包装**的（`SideAwareItemHandler`）。这批补上：

- 新 `CapabilityHandleTests.itemPipeCapabilitiesAreCached`：用 `GTItemPipes.all()` 取管道方块放置后走同一套 `checkStableHandles`，断言 **①同一方向两次查询同一实例**、**②`NORTH` 与 `SOUTH` 句柄不同（按方向缓存）**、**③`invalidateCaps()` 后已发出的句柄失效**。
- 门禁一次通过（管道的 `getCapability` 对 `side != null` 一律给物品处理器，与 §98 里固体燃烧盒"任何方向都不给"的情况相反，所以三项断言都适用）。

至此 §97 改过的四处里，**三处（漏斗 / 队列漏斗 / 物品管道）有专门的身份与失效断言**，固体燃烧盒因"固体变体不暴露物品处理器"而只断言注册存在（原因已写进测试 javadoc，见 §98）。

结果：**561 项 GameTest 全过**（560 → 561）、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `D4E07ACFBF6D07CAC19DC849719D5AC567AB158CC2C39EAE3C4EBC01C99650A2`）。

**没有执行 `git commit`。**

---

## 100. 续批：抄 **TerraFirmaCraft 的"水家族"做法**修掉 GT 水的渲染接缝与水面合并

指出参考模组 `F:\Dev\GregTech6\TerraFirmaCraft-1.20.x` 有海水/淡水区分、含水方块与边缘处理。读它的实现后找到移植版缺的那一环：

**TFC 的做法（`common/fluids/RiverWaterFluid.java:28-53`、`common/blocks/RiverWaterBlock.java:33-79`）**
- 它的河水 **`extends WaterFluid`**（继承原版水的抽象类），并覆写 **`isSame(Fluid)`**：`super.isSame(fluid) || fluid == TFCFluids.RIVER_WATER.get()` ⇒ **把自家流体并进"水"这个家族**；
- `getFluidType()` → `ForgeMod.WATER_TYPE`（与移植版一致）、`getHeight()` 用 `isSame` 判断上方同类才给满高（他们多写一遍是因为自己的流体带 `FLOW` 属性）；
- 方块侧 `RiverWaterBlock extends LiquidBlock`，`pickupBlock`（两种重载）**一律返回原版水桶**（`Fluids.WATER` / `getBucket()`），并 `initFluidStateCache()` no-op。

**移植版缺的正是 `isSame`**：`GTWorldWaterFluid.Source/Flowing` 只覆写了 `getFluidType`/`canConvertToSource`/`canBeReplacedWith`/`getNewLiquid`，`isSame` 走默认的**身份比较**。而原版把**水面合并与坡度/液位计算**都挂在它上面：
- `FlowingFluid.getHeight(...)` ⇒ 只有上方是**同一流体**才返回满高 1.0，否则用自身高度；
- `FlowingFluid.getSlopeDistance(...)` ⇒ 只把**同类流体**邻居算进坡度。

所以 GT 水与 GT 水之间、GT 水与原版水之间**处处被判为"不同流体"**，这正是 §85 记档的"两族之间可能有渲染接缝"，也是"看着不如原版水"的机械原因。

**改动（一处，照 TFC 抄）**：`registry/GTWorldWaterFluid.java` 新增 `isWaterFamily(Fluid)`（`fluid.is(FluidTags.WATER)`——六个世界水 id 都在 `#minecraft:water` 里，标签**就是**家族），并在 `Source` 与 `Flowing` 两个内部类里各覆写一次 `isSame`。**为什么用标签而不是逐个 id**：标签天然同时覆盖原版水、流动水与六个 GT 世界水，且不会随注册顺序变化。

**验证**（新 `WaterFluidParityTests.worldWatersAreOneWaterFamily`，1 项）：六个世界水 id 全部存在；海水对**每一个**世界水以及原版水/流动水都报 `isSame` 为真；流动变体双向为真；**行为面**：level 4 的流动世界水单独存在时 `getHeight < 1.0`，**上方放一块原版水后 `getHeight == 1.0`**（水面合并、接缝消失的直接证据）。

**TFC 其余做法的取舍**：①它 `extends WaterFluid` 的写法在移植版行不通——移植版走 `ForgeFlowingFluid.Properties` 注册（925 个流体共用一套 loader），改成继承原版 `WaterFluid` 需要重写整条注册链，收益不抵风险；`isSame` 覆写已拿到同一效果。②`pickupBlock` 一律返回原版水桶：移植版的世界水方块**已经**是 `GTWorldFluidBlock extends LiquidBlock` 并继承原版桶行为，故未改。③海草/海带仍硬编码返回原版水（`SeagrassBlock:62-64`）——TFC 同样如此（它靠 mixin 处理别的路径），移植版**无 mixin**，继续记档为不可消差异。

结果：**562 项 GameTest 全过**（561 → 562）、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变；jar 重建为 `build/libs/gregtech-1.0.0.jar`（29.9 MB，SHA256 `A3B2F4D0FB12B7C13BE8217487F71D1EC465965D619A1A2596FF3C28F475000F`）。

**没有执行 `git commit`。**

---

## 101. 实测收口：**9 GB 内存不足的真因**（客户端实测 2h38m + 5 GB 堆快照）＋ 本轮推进计划

反馈"跑一段时间 9 G 也 OOM"。这轮**没有靠猜**：真的开了客户端（`gradlew runClient`，PID 37928，`-XX:MaxHeapSize=8.38 GB`）、进了世界、按分钟用 `jcmd GC.heap_info`/`GC.class_histogram` 采样，并在现象最明显时手动 `jcmd GC.heap_dump`（`F:\Dev\GregTech6\gt-heap-live.hprof`，**5.02 GB**）。结论与 §84/§97 的修复**不同**，必须单独记档：

### 101.1 结论一：**没有泄漏**（站住不动即完全冻结）

| 时刻 | 堆已用 | 已加载区块 | `OreBlockEntity` | `HashMap$Node` | `ItemStack` |
|---|---|---|---|---|---|
| 21:36（站住不动） | 5996 MB | 3,646 | 1,812,658 | 10,489,194 | 848,310 |
| 21:37 | 4776 MB | 3,646 | 1,812,658 | 10,489,187 | 848,306 |
| 21:38 | 4635 MB | 3,646 | 1,812,658 | 10,489,183 | 848,305 |
| 21:39 | 4385 MB | 3,646 | 1,812,658 | 10,489,240 | 848,309 |
| 21:40 | 4069 MB | 3,646 | 1,812,658 | 10,489,393 | 848,287 |

**连续 5 次采样零增长**（堆只在 4.07–5.99 GB 间随 GC 抖动）⇒ 没有按 tick 累积的泄漏、没有静态表在留对象。同期主菜单挂机 **2 小时**同样是**零增长**（2779 MB 恒定，19:05→21:00），说明**空转不漏**。

### 101.2 结论二：真因是"**每区块约 497 个矿石方块实体**"这个乘数

- 世界内实测：`OreBlockEntity` 1,812,658 ÷ 3,646 区块 = **497 个/区块**；每个 BE 还连带约 5.8 个 `HashMap$Node`（同场景 **1,049 万**个）。
- 探索时（21:28→21:35）随区块数上涨：BE 101 万 → 181 万、map 节点 697 万 → 1,049 万、堆 3.7 → 5.9 GB；**停下来就停住**，说明它跟随"服务端按 ticket 保留的区块数"，而不是泄漏。
- 3.6k 区块场景活对象 **4–6 GB**；主菜单另有 **2.72 GB 固定地板**（`BakedQuad` 88.7 万、`ItemStack` 53.2 万 + 53.2 万 lambda、`Recipe` 20.1 万、`ImmutableMapEntry` 190 万、GT 四组 55,960 的 lambda/条目）⇒ 8.38 GB 上限能舒服承载的区块数远小于默认视距的要求，**9 GB 亦然**。
- 排除项：世界内 `LazyOptional` 仅 **672** 个（§97 修复有效）、`CompoundTag` 37–38 万且平稳、`OreBakedModel` 用 512/64 有界缓存不持 BE、静态表无按坐标持 BE 者。

### 101.3 复现与取证方法（写进交接单，可复用）

```powershell
# 1) 起客户端（JAVA_TOOL_OPTIONS 会被游戏自带的 -XX:HeapDumpPath 覆盖，故 dump 用手动）
cmd /c ".\gradlew.bat runClient --offline -Dnet.minecraftforge.gradle.check.certs=false --console=plain > build\runclient.log 2>&1"
# 2) 采样（PID = 私有内存最大的 java 进程）
& 'C:\Program Files\Java\jdk-17.0.4\bin\jcmd.exe' <pid> GC.heap_info
& 'C:\Program Files\Java\jdk-17.0.4\bin\jcmd.exe' <pid> GC.class_histogram
# 3) 需要现场时
& 'C:\Program Files\Java\jdk-17.0.4\bin\jcmd.exe' <pid> GC.heap_dump F:\Dev\GregTech6\gt-heap-live.hprof
```
产物：`build/heap-samples.log`（菜单 24 次）、`build/heap-world.log`（世界 14 次）、`tools/_heap_report.py`（一条命令出上面那张表）。

### 101.4 下一批的推进计划（按收益排序）

1. **取消"每个矿石方块一个 BE"**（内存根因）：材料/岩石信息改走 `BlockState` 变体或每区块调色板，只在需要时建 BE。预期同场景省下约 **180 万个 BE + 1000 万个 map 节点**（数 GB 级）。结构性改动，涉及 `OreBlock`/`OreBlockEntity`/掉落与材料查询/`OreBakedModel` 渲染/世界生成写入/存档兼容。
2. 抬高地板：菜单态 2.72 GB 里 `ItemStack` 53.2 万（+同数 lambda）、`BakedQuad` 88.7 万、`Recipe` 20.1 万，可逐项削减。
3. §5.1b 队列：`MultiItemBottles` 瓶子族（表与数值已可用，只差外观与 250 mB 语义）、GT6 农作物/食品线、覆盖板 61 种、酸/气/等离子标记。
4. 审计工具本轮复核：`report_missing_machine_recipes.py` 报 **248 个已注册变体全部有配方行**（无缺口）；`report_missing_prefix_usage.py` 报 44 个前缀未被配方引用（多为 armor/tool/oreNether 这类由合成或世界生成获得的形态，属设计选择，非缺口）。

**没有执行 `git commit`。**

---

## 102. 更正 §101.4 的收益估算：矿石 BE **不是**数 GB 级，真正的大头是"每区块的世界数据"

§101.4 把"取消每个矿石方块一个 BE"写成"预期省数 GB"，这是**未经核算的夸大**，必须按实测数字改口（否则下一批会按错误预期排期）：

| 项 | 单个成本 | 3.6k 区块场景合计 | 占世界增量 |
|---|---|---|---|
| `OreBlockEntity` 对象本身 | ≈ 40 B（对象头 + 3 个字段 + `BlockPos`/`BlockState` 引用） | 1,812,658 × 40 B ≈ **69 MB** | 小 |
| 区块 `blockEntities` 的 `HashMap$Node` | ≈ 32 B | 1,812,658 × 32 B ≈ **55 MB** | 小 |
| 客户端 `BlockEntityInfo`（区块包内） | ≈ 32 B | ≈ **55 MB** | 小 |
| 模型 `ModelData`/属性条目 | 每 BE 若干条目 | 数百 MB 上限（取决于渲染路径是否每格查 model data） | 中 |
| **合计可归因于矿石 BE 的部分** | — | **约 0.3–0.5 GB** | **约 15–20 %** |

而实测的世界增量是：GC 后活对象 ≈ **4.0–4.4 GB**，减去主菜单 **2.72 GB** ⇒ 3.6k 区块约 **1.3–2 GB**，折合 **每区块 360–560 KB**。扣掉矿石 BE 的 15–20 %，**剩下 80 % 是区块自身的数据**：`LevelChunkSection` 的调色板与 `BlockState` 数组（GT 矿石密集 ⇒ 每 section 调色板条目多）、光照、客户端网格与 `BakedQuad`/顶点缓冲记账、以及区块包的 `[B` 缓冲。

**所以正确的优先级是**：
1. **降低"每区块的世界数据"**（收益最大，但也最难）：①减少 GT 矿石在区块里的**方块种类**（同一种材料/岩石组合复用同一个 `BlockState` 实例、避免每格引入新调色板条目）；②复核 `GTWorldFluidBlock`/矿石方块是否触发了不必要的 `ModelData` 查询；③必要时把"岩石变体"从方块状态维度降为材质映射（配合 §101 的 BE 议题一起评估）。
2. **矿石 BE 改造**（约 15–20 %）：仍值得做——它同时减少 `blockEntities` 映射、`BlockEntityInfo` 与 BE ticker 开销，但**不该按"数 GB"排期**，而应作为"每区块成本"整体优化的一部分。
3. **视距/模拟距离**（唯一无需改代码、线性生效的杠杆，客户端侧即可）：区块数直接决定 360–560 KB/区块 的总量。

**教训记档**：在把"实例数 × 单实例成本"写成收益之前，先把单实例成本量出来——本批就是靠这一步才发现"180 万实例"听起来吓人、实际只有 0.3–0.5 GB。

**没有执行 `git commit`。**

---

## 103. A / B / C 三项的**可执行方案**（已批准，等一轮充足预算开工）

批准"ABC 都做"。三项都属结构性改动，**每项都要动 5–8 个文件并跑至少两轮门禁**（约 8 分钟/轮），故先把方案与影响面钉在这里，避免开工时临时决策；同时按 §102 的实测成本排优先级。

### 103.A 每区块世界数据（收益 ≈ 世界增量的 80 %，最难）

> **硬约束（ 2026-09-17 明确）**：**只优化 GregTech 自己加进区块的东西，原版一律不动**——不改区块 section/调色板/光照/网格这类原版内部机制，不 mixin 原版类，不替换原版方块。因此本项的着力点是 **GT 自己放置的方块与它们的状态**，而不是"让区块本身更省"。

GT 自己往每个区块里塞的东西，按体积排序的排查清单：
1. **GT 石层方块（27 种 `StoneType`）**：GT 的石层特征把大量原版石头替换成 GT 石头 ⇒ 直接抬高该 section 的调色板条目数与方块种类。手段：确认同一种 GT 石头在整个区块里**复用同一个 `BlockState` 实例**（带属性的状态别每格新建）；石层写入用预建的状态常量表而不是每格 `setValue(...)`。
2. **GT 矿石方块**：矿脉、小矿、基岩矿、下界/末地矿点都在写矿石方块；`MaterialBlock` 的每种矿石 × 宿主岩石组合若各自产生新状态，调色板就会膨胀。手段：与 §103.B 的"岩石进状态属性"合并评估——**属性值组合数要压住**（理想是 27 个岩石值 × 每个矿石方块 1 个方块 = 27 个状态/方块，而不是按格新建）。
3. **GT 世界流体方块**：`GTWaterBodyFeature` 把海洋/河流/沼泽的水替换成 GT 水（三种 × 静/流）⇒ 水面区块的方块种类增加，且 `GTWorldFluidBlock` 覆写了 `skipRendering`/`updateShape`/`neighborChanged`，要确认没有因为"总是返回需要渲染/更新"而阻止面剔除或放大网格。
4. **GT 的 BE**：矿石 BE 见 §103.B；其余 GT 机器/BE 在区块里的数量按需核查（§101 已确认无泄漏）。
5. **GT 的渲染类型与模型**：核对 GT 方块是否返回了会阻止面剔除的 `skipRendering`/过宽的 `getRenderTypes`（这直接放大 `BakedQuad` 与顶点缓冲记账）。
6. **验收方法**：同存档、同位置、同视距，用 §101.3 的 `jcmd` 采样对比改动前/后：`used`、`LevelChunkSection`、`BlockState`、`BakedQuad`、`HashMap$Node` 四个计数。**原版侧的任何数字变化都不作为本项的成果**（那也是不该出现的副作用）。

### 103.B 矿石 BE → 方块状态属性（收益 ≈ 0.3–0.5 GB，机械但面广）

现状：`blockentity/OreBlockEntity.java`（91 行）只存 `String stone`（默认 `"stone"`，另有 `"bedrock"`），用途是 **渲染**（`getModelData()` → `STONE_PROPERTY`）与 **`isBedrockOre()`**；`OreBlock` 是唯一创建者。
改造步骤（顺序即开工顺序）：
1. 新增 `block/ore/OreHostStone.java`（枚举）：覆盖现有 canonical id —— 8 个原版宿主（`stone`/`granite`/`diorite`/`andesite`/`deepslate`/`tuff`/`netherrack`/`end_stone`）、`bedrock`、以及 `StoneType.registryId()`（`stone_basalt` …）；每个值给出 `id` 与贴图集名（沿用 `OreBakedModel.STONE_TEXTURES` 的键）。
2. `block/OreBlock.java`：加 `EnumProperty<OreHostStone> STONE`（默认 `stone`），`createBlockStateDefinition` 注册；提供 `stateFor(stone)` 与从状态读岩石的静态方法。
3. `client/OreBakedModel.java`：`getQuads`/`getParticleIcon` 改从 **BlockState 属性**取岩石（`extraData`/`ModelData` 分支删除）；因它是自定义 loader，blockstate JSON **不必**为 27 个值展开（assets 体量不变）。
4. 世界生成写入点：把"设 BE 的石头"改为"用 `setBlock(stateFor(stone), …)`"（`GTBedrockOreFeature`、矿脉放置、地牢矿石、`WorldgenFluidSpring` 等所有 `GTBlockEntities.ORE` 写入点；用 `grep GTBlockEntities.ORE / setStone(` 找全）。
5. `OreBlockEntity`：**保留类**供旧存档反序列化，但不再被任何新放置创建（`OreBlock#newBlockEntity` 返回 null 并注释原因）；`isBedrockOre()` 迁到方块状态（`STONE == bedrock`），基岩矿的"不可破坏/无掉落"随之上移到 `OreBlock`。
6. 次要引用点：`blockentity/SensorBlockEntity.java:223`、`blockentity/machine/BedrockDrillControllerBlockEntity.java:9` 改为读状态属性。
7. 存档兼容：旧世界的矿石 BE 数据在方块不再有 BE 类型后会被丢弃 ⇒ 那些矿石的**背景岩石回落到 `stone`**（记档为已知差异；如要保留可加一次性迁移读 BE → 写状态）。
8. 测试：①放置矿石方块后断言 `level.getBlockEntity(pos) == null`（BE 不再创建）；②`stateFor(bedrock)` 的方块 `getDestroyProgress`/掉落为空（基岩语义）；③渲染取色：断言 `OreBakedModel` 对两个不同岩石状态返回**不同**的 quad 精灵；④旧存档：手写一个带 `OreBlockEntity` NBT 的区块读入不抛异常（方块回落到默认岩石）。

### 103.C §5.1b 内容批（各自独立批次）

1. **`MultiItemBottles` 瓶子族**：`GTDrinks` 表与饮用数值**已可用**（§93–§96：272 行生成、265 行解析、采样 9/10 有 per-fluid 物品代理），差的是 GT6 原版瓶子物品族（每瓶 250 mB）与外观；贴图 `assets/gregtech/textures/item/bottles` 已在仓库 ⇒ 新增多物品组 + 模型/语言键 + 把 `FluidItem.use` 的"无限次使用"换成"消耗一次、退空瓶"。
2. ~~**GT6 农作物/食品线**~~ → **§108 已查清并收口**：物品缺口 = **0**（266/266 早就在 `GTMultiItemsGen` 里注册了），真正缺的是**数值**（`GTFoodStats` 20 → **201** 行，走 `FoodStat` 路径而不是 `FoodsGT` 监听器）；§103.C 期待的"焦糖/太妃糖/咖啡/茶/20 种坚果"**GT6 自己一个都没注册**（是 HarvestCraft 与其它模组的作物），造它等于凭空发明内容 ⇒ 记档不造。
3. ~~**覆盖板 61 种**~~ → **§108 已把 61 个文件拆完**（48 个具体行为 + 10 个抽象基类 + 3 个纯贴图）：现有行为 **35 种**（§107 之前 26 + §108 的 9），剩下 **14 种全是物流核心的 cover**（移植版没有物流核心 ⇒ 不适用，但物品与配方都在）⇒ **按"覆盖板"排期已无剩余项**，要做必须先做物流核心。
4. **酸/气/等离子标记**：给流体加 `ACID`/`GAS`/`PLASMA` 语义标记、给管道加 `mAcidProof`/`mGasProof`/`mPlasmaProof` 对应物，之后 §91 的 `applyChemDamage` 与 `isImmuneToBreathingGases` 才有真实入口（GT6 `BlockBaseFluid:406`、`MultiTileEntityPipeFluid:311`）。

**开工约定**：每项独立走"方案 → 改代码 → GameTest → 门禁（确认 `All N required tests passed`）→ 重建 jar → 更新本文件新节 + `PORTING_AUDIT.md` 横幅 + `porting-progress.json` → 刷新交接单"，全程不执行 `git commit`。

**没有执行 `git commit`。**

---

## 104. §103.B 落地：矿石宿主岩石从"每个矿石一个 BE"搬进**方块状态属性**（预计省 0.3–0.5 GB）

§103.B 做完。**矿石方块现在完全没有方块实体**：宿主岩石成为 `OreBlock` 的 `stone` 方块状态属性（`OreHostStone` 枚举，39 个值，id 与旧 BE 写的 canonical id 逐字相同）。结果：**570 项 GameTest 全过**（562 → 570）、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 全部不变、jar 重建为 `7059C5F19E80B2C098C0364C231721EE20F0ACD9D5AC6DE5CB8AC3E429F146B6`（30.0 MB），顺序不变量 `Java 最新 < 门禁日志 < jar` 回到 **True / True**（上一轮那条 False 就是本项留下的"改了没门禁"标记）。

### 104.1 改了什么（14 个文件：11 主代码 + 1 新测试 + 3 测试断言）

| 文件 | 改动 |
|---|---|
| `block/OreHostStone.java` | 宿主岩石枚举：8 个原版宿主 + `bedrock` + 27 个 `StoneType.registryId()` = **36**，**再加 `sand`/`red_sand`/`gravel` = 39**。补这三个是必须的：`GTOreBlockResolver.sedimentId` 本来就会给小矿返回这三个 id，不补就会回落 `stone`（沙里的矿丢背景），且 `GTBlackSandFeature` 的沉积物判定会失效。另加 `isBedrock()`/`isSediment()` |
| `block/OreBlock.java` | ①`EnumProperty<OreHostStone> STONE`（默认 `stone`）+ `createBlockStateDefinition` 注册；②`stateFor(...)`/`stoneOf(BlockState)`/`isBedrockOre(BlockState)`/`stoneOfStack(ItemStack)`；③**不再 `implements EntityBlock`**（见 104.2）；④掉落（含小矿宿主材质）、精准采集、爆炸抗性、破坏进度、选取方块全部改读状态；⑤物品 NBT **双写** `BlockStateTag`（原版格式）+ `BlockEntityTag`（旧格式），`getStateForPlacement` 把岩石放回状态 |
| `worldgen/GTOreBlockResolver.java` | `resolve` 直接产出带岩石的状态（新增"显式岩石"重载，供深海塔用）；`placeOre`/`placeBedrockOre` 变成**纯 `setBlock`**，删掉两处 `getBlockEntity(...) instanceof OreBlockEntity → setStone`；`stoneId`/`sedimentId` 返回枚举（`sedimentOf`） |
| `client/OreBakedModel.java` | `getQuads`（5 参与 3 参两条路径）/`getParticleIcon` 改读**状态属性**；新增按岩石固定的模型视图 `stoneModel(...)`（每宿主岩石一个，兼作物品 NBT 变体与**破坏粒子**取色）；删除 `ModelData`/`STONE_PROPERTY` 路径 |
| `client/OreClientModels.java` | 模型替换的 key 从 `#`（无属性）改为**逐状态** `BlockModelShaper#stateToModelLocation`；**资产零改动**——blockstate JSON 仍是单个 `""` 变体（`ModelBakery` 的 `""` 谓词匹配全部状态，已在反编译源里核对） |
| `worldgen/GTDeepOceanFeature.java` | 深海棱柱塔撒矿改 `placeOre(level, pos, OreHostStone.STONE, ore, false)`（GT6 用的是 background=普通石头的矿石变体），不再"先放再改 BE" |
| `worldgen/GTBlackSandFeature.java` | 沉积物判定 `isSediment(BlockState)` 读状态属性（原签名带 level/pos，已收敛） |
| `worldgen/GTFluidSpringsFeature.java` | 基岩判定改 `OreBlock.isBedrockOre(state)`：原来的 `id.getPath().startsWith("bedrock_ore")` **从来没有匹配过任何注册方块**（移植版没有 `bedrock_ore_*` 这个 id 段）＝死检查。GT6 `WorldgenFluidSpring:67` 正是"原版基岩 **或** `oreBedrock`/`oreSmallBedrock` 都算地面" ⇒ **顺带修好**（在基岩矿柱上现在会正常生泉） |
| `blockentity/OreBlockEntity.java` | 只剩旧存档迁移：`load` 记下 tag 里的岩石（`tagStone`），`onLoad` 把它写进方块状态后自我释放；渲染相关（`STONE_PROPERTY`/`getModelData`/`getUpdateTag`/`getUpdatePacket`）全部删除 |
| `blockentity/machine/BedrockDrillControllerBlockEntity.java` | 基岩矿判定读状态；`OreBlockEntity` 导入删除 |
| `gametest/OreBlockStateTests.java`（新） | **8 项**（见 104.3），日志里打出 `ore blocks=970 hosts=39 states=37830` |
| `gametest/{BedrockVeinTests,RockAndDeepOceanTests,SurvivalExtensionTests}.java` | 三处读旧 BE 的断言改读状态（`isBedrockOre(state)` / `stoneOf(state)` / `stateFor(...)`），并各加一条"矿石无 BE"断言 |

**计划里点的 `SensorBlockEntity:223` 不需要改**：那行现在是 `ReactorCoreBlockEntity.neutronTotal()`（反应堆中子数），移植版的传感器早就不读矿石 BE 了（全仓 `grep OreBlockEntity` 现在只剩迁移、类型注册与测试）。

### 104.2 关键设计决定：**不实现 `EntityBlock`**（两轮实测纠错，值得记档）

三版设计，前两版都被门禁/日志打了回来，证据如下：

| 版本 | 做法 | 实测结果 |
|---|---|---|
| v1（按 §103.B 原文） | `newBlockEntity` 返回 **null** | 门禁 570 项里 **7 红**（含我的迁移测试），并新增 **3,933 条** `Tried to load a block entity ... but failed` 警告 |
| v2 | 真造实体，但 `onLoad` 里"自封装"（写状态 + 自删） | 警告消失，但断言**仍然红**：`Level.getBlockEntity(pos)` 走 **IMMEDIATE**（`Level.java:560` → `LevelChunk:313-320`），map 里没有时会**用 `newBlockEntity` 现场造一个** ⇒ 任何一次坐标查询都会留下惰性 BE |
| **v3（定案）** | **`OreBlock` 不实现 `EntityBlock`** | 警告 3,830 → **18**（其中 2 条是我测试里故意放的旧占位 tag），570 项全过 |

两条原版机制是这次的设计依据（都在 `build/mc-src-all` 里核对过）：

1. `WorldGenRegion.setBlock:267-281`：**任何** `hasBlockEntity()` 的方块在生成期都会被写一个 `DUMMY` 占位 tag 进 proto chunk。矿石是每个区块几百个 ⇒ v1 的 null 让每次提升都失败并记一条警告，占位 tag 还留在区块里。
2. `LevelChunk.getBlockEntity(pos, IMMEDIATE):313-320` + `Level.getBlockEntity:560`：1 参 `getBlockEntity` 就是要"立即创建"，`hasBlockEntity()` 为真的方块被查一次就长出一个实体——正是 §103.B 想消灭的东西。`hasBlockEntity()` = `Block instanceof EntityBlock`，所以**不实现 `EntityBlock` 同时关掉这两条路**。

**旧存档仍然能迁移**（这是 v3 唯一的担心，已核对）：`promotePendingBlockEntity` 先 `blockentity.setLevel(this.level)`（`LevelChunk:538`）再 `addAndRegisterBlockEntity`，而后者即使 `setBlockEntity` 因为 `!hasBlockEntity()` 拒绝保存这个实体，**仍然会调用 `onLoad()`**（`LevelChunk:325-337`）⇒ 迁移照跑；`BlockEntity.loadStatic` 也不检查 `isValid(state)`，所以 `gregtech:ore` 类型照样能从旧 tag 造出实体。

### 104.3 测试（新 `gametest/OreBlockStateTests`，8 项 / BASE 22000,22000）

| # | 断言 |
|---|---|
| ① | 放置矿石后**没有** BE（`getBlockEntity` 与区块 BE 表都查）——同时是"不复活"的回归守卫 |
| ② | 旧 id 全部能解析（`bedrock`/`stone`/`stone_granite_black`）、未知 id 回落 `stone`、27 个 `StoneType` 一一对应、id 无重复、沉积物/基岩标记正确 |
| ③ | 39 个宿主岩石在 `stateFor`/`stoneOf` 之间往返；属性序列化名**就是 canonical id**（`BlockModelShaper` 的 key 依赖它）；`null`/外来状态不抛异常 |
| ④ | 基岩宿主：`getDestroyProgress == 0`、抗爆 ≥ 3,600,000、掉落为空、且**无 BE**；同一方块换普通宿主则可破坏 |
| ⑤ | 旧存档迁移：走真实提升路径（`setLevel` + `addAndRegisterBlockEntity`）后岩石进状态、实体自我释放、不留在区块里；未知岩石回落 `stone` |
| ⑥ | 旧世界的遗留 `DUMMY` 占位 tag：提升既不复活实体、也不动状态 |
| ⑦ | 真实世界生成路径（`placeOre`/`placeBedrockOre`）：石头/花岗岩/沙（小矿专属）/深板岩/基岩五种宿主都记录正确，且**都没有 BE**；普通矿不会长在沉积物里 |
| ⑧ | 状态预算：970 个矿石方块 × 39 宿主 = **37,830** 个状态，并把它打进日志（供 §103.A 复核） |

### 104.4 成本与收益（按 §102 的口径）

| 项 | 数字 |
|---|---|
| 世界内消失的对象 | 3.6k 区块场景 **1,812,658 个 `OreBlockEntity`** + 同数区块 `blockEntities` 映射项 + 客户端 `BlockEntityInfo` ⇒ §102 的 **0.3–0.5 GB（世界增量的 15–20 %）** |
| 新增的固定成本 | 37,830 个 `BlockState`（970 个方块 × 39 宿主，**比原来多 36,860 个**）＋ `StateDefinition` 的 state→state 映射项。按每状态约 200–300 B 估 **≈ 8–11 MB**，是**一次性地板**、不随区块数增长（未实测，量级估计） |
| 客户端模型 | 970 个矿石方块共用 **50** 个 overlay 模型（客户端日志实测），按岩石固定的视图 ≤ 50 × 39 = **1,950** 个小对象 |
| 警告噪声 | 门禁日志里 `Tried to load a block entity` 从 **3,830 → 18**（干净世界；其中 2 条是我测试⑥故意造的旧占位 tag） |

### 104.5 已知差异 / 副作用（诚实记账）

1. **旧世界**：已生成区块里的矿石 BE 会在该区块**下一次加载时**迁移进状态并消失（`onLoad`），随后连存档里的 tag 也一起没了（实体从未进 BE 表 ⇒ 不再被写回）。迁移前，旧存档的 `stone` 属性是默认值 `stone`（旧调色板没有该属性，反序列化按属性默认值补齐）⇒ **迁移完成前渲染短暂回落 stone**；基岩矿的"不可破坏/不可钻"在迁移后恢复。**本轮门禁是干净世界，旧世界行为未实测**。
2. **客户端日志会变长**：`ore_abyssalnite` 这类"有 blockstate JSON 但模型缺失"的矿石方块（既有问题，552 个 id）在 `ModelBakery` 里是**逐状态**警告路径（`map.forEach` 按状态遍历，已在反编译源核对），属性化后同一条警告会乘 39 ⇒ 约 552 → **约 21.5k 行**（按代码路径推算，**未实测**）。修法是让 `tools/generate_ore_block_assets.py` 覆盖全部 970 个矿石方块（它现在只覆盖 242 个材料的两种前缀），属独立资产批次，本轮**没做**。
3. `Level.getBlockEntity(矿石坐标)` 现在恒为 `null`（这是目的）；若有第三方工具依赖"矿石 BE 存在"，会拿不到数据。移植版自身已无这种依赖。
4. 小矿掉落的位置参数从"BE 坐标"改为 `LootContextParams.ORIGIN`（无 ORIGIN 的调用方会落到 `BlockPos.ZERO`，随机种子固定）——对正常破坏路径等价于同一格。
5. **未做**（原 §103.B 第 7 条的可选项）：没有单独的"一次性迁移扫描"；迁移靠区块加载时的 `onLoad` 完成，未加载过的区块本来也没有内存占用。

### 104.6 下一批（排队不变）

- **§103.A** 每区块世界数据（硬约束：只优化 GT 自己加的方块与状态，不碰原版区块 section/调色板/光照/网格、不 mixin 原版）。本轮给了它两个新数字：矿石状态从 970 → 37,830（固定成本），以及"GT 石头/矿石的 `BlockState` 复用"仍是主攻方向；验收仍用 §101.3 的 `jcmd` 采样（`used`/`LevelChunkSection`/`BlockState`/`BakedQuad`/`HashMap$Node`）。
- **§103.C** `MultiItemBottles` 瓶子族 → GT6 农作物/食品线 → 覆盖板 61 种 → 酸/气/等离子标记。

**没有执行 `git commit`。**

---

## 105. 移植版的**第一条 mixin**：让原版水也认 GT6 的水（水面双向平滑过渡）

提问"mixin 可以让水[面]平滑过渡，要不要作为前置加上"。查完的结论是：**加，但只加这一条，而且它不是前置**——§100 已经把那条渲染接缝修好了一半，剩下的一半（**原版那一侧**）确实只有 mixin 能补；它是自洽的一小块（1 个 mixin + 1 项测试），不阻塞任何其它批次，所以顺手做完。

### 105.1 到底缺哪一半（源码依据）

| 方向 | 现状 | 依据 |
|---|---|---|
| GT 水 → 认**原版水** | **§100 已修**（`GTWorldWaterFluid.Source/Flowing` 覆写 `isSame` = `#minecraft:water` 家族） | `GTWorldWaterFluid:108-110`、`:131`、`:167` |
| **原版水** → 认 GT 水 | **缺**：vanilla `WaterFluid.isSame` 是身份比较 | `WaterFluid.java:74-76`：`fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER` |

而"平滑过渡"全挂在 `isSame` 上：`LiquidBlockRenderer:44`（相邻同流体判定）与 `:329`（角点高度：`fluid.isSame(neighbourFluid)`，`fluid` 是**正在渲染的那一格**的流体）⇒ 渲染到**原版水**那一格时，原版水不会与旁边的 GT 水面合并（出现台阶），原版水也不会向 GT 水体铺展。也就是说：家族是**单向**的，缝只在一侧消失。

### 105.2 做了什么（4 个文件）

| 文件 | 内容 |
|---|---|
| `mixin/WaterFluidMixin.java`（新） | `@Mixin(WaterFluid.class)` + `@Inject(method = "isSame", at = @At("HEAD"), cancellable = true)`：判据复用 §100 的 `GTWorldWaterFluid.isWaterFamily(fluid)`（= `#minecraft:water` 家族），命中即返回 true；岩浆不在该标签 ⇒ 仍走原版答案 |
| `resources/gregtech.mixins.json`（新） | `required`、`minVersion 0.8`、`compatibilityLevel JAVA_16`（与同环境可用的 Jade 配置一致）、`refmap gregtech.refmap.json`、**`injectors.defaultRequire = 0`**（故意：万一生产环境 refmap 失效，降级为原版行为 + 一条警告，而不是让玩家开局崩溃；dev 侧有测试守门） |
| `build.gradle` | 新增 `id 'org.spongepowered.mixin' version '0.7.+'`（**MixinGradle 0.7.38**）+ `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'` + `mixin { add sourceSets.main, 'gregtech.refmap.json'; config 'gregtech.mixins.json' }` |
| `gametest/WaterFluidParityTests.java` | 新增 **1 项** `vanillaWaterKnowsTheWorldWaters`（同时是"mixin 到底有没有加载"的守门测试） |

**关键踩坑（值得记档）**：**Forge 47.4.20 没有 `mods.toml` 的 `[[mixins]]` 块**——用 `javap` 查 `fmlloader` 的 `ModInfo`，字段表里**没有 `mixins`**（那是后来才加的）。所以 dev 侧发现 config 只能靠 MixinGradle（它写 jar manifest 的 `MixinConfigs` 并在 dev 注册 config）或启动参数；"手写 refmap + manifest" 那套在 dev 里**根本不会加载**（而 dev 正是在 `runClient` 中 的方式）。这也是移植版**第一次**引入 mixin：此前全仓 mixin 数为 **0**（§2 "核心修改 asm 18 → 0"）。

### 105.3 验证

- 门禁日志（**带 `--offline` 跑**，证明 MixinGradle 不破坏离线工作流）里可见：
  `Registering mixin config: gregtech.mixins.json` → `Mixing WaterFluidMixin from gregtech.mixins.json into net.minecraft.world.level.material.WaterFluid` ✓
- `vanillaWaterKnowsTheWorldWaters`：原版水与流动水对**六个**世界水 id 都报 `isSame` 为真、岩浆仍为假、§100 的 GT→原版方向不回归、渲染器读的那条判据（`Fluids.WATER.isSame(<该格流体>)`）为真。
- **`All 571 required tests passed :)`**（570 → 571）、注册量 70,754 / 12,093 / 1156 / 925 与缺失 token 49 不变。
- **生产侧**：refmap 由 Mixin AP 生成（`build/tmp/compileJava/gregtech.refmap.json`），实测内容为 `WaterFluidMixin.isSame → Lnet/minecraft/world/level/material/WaterFluid;m_6212_(Lnet/minecraft/world/level/material/Fluid;)Z`，与独立从 `build/createSrgToMcp/output.srg` 查到的 SRG 名**逐字一致**；构建后的 jar 内含 `gregtech.refmap.json` 且 manifest 带 `MixinConfigs: gregtech.mixins.json`（见 §105.5 的复核结果）。

### 105.4 没做 / 仍记档（附本轮的新证据）

- **原版可含水方块**（`SimpleWaterloggedBlock` 家族、橡木台阶等）在 GT 水里仍**不含水**：`SimpleWaterloggedBlock:18/:22` 是身份比较，但**每个方块类都自己复制了一份**（`SlabBlock.getStateForPlacement` 里同样是 `fluidstate.getType() == Fluids.WATER`）⇒ 只 mixin 那个接口**覆盖不到放置路径**，收益不完整，本轮不做（§5.4 的记档保持）。
- **原版水桶倒在 GT 水上会换成原版水**：仍无钩子（要 mixin `BucketItem`，且"应该倒出什么"没有定论），不做。
- **海草/海带其实没问题**：`SeagrassBlock:42` 用的是**标签**判定（`fluidstate.is(FluidTags.WATER) && amount == 8`），能在 GT 水里放置 ✓；它内部持有原版水（`:63`）与 §100 的记档一致、视觉无差异。§5.4 里"海草硬编码原版水"这条**描述需要更正**：硬编码的是 `getFluidState` 的返回值，不是放置判定。
- **高速轨的 1.2 上限**（§5.2）现在**技术上可行**了（mixin 基建已就位），但它属于玩法平衡选择，仍保持"保留 GT6 数值 + 记档"，未动。

**没有执行 `git commit`。**

---

## 106. 下一批预研（§103.C 第一批：瓶子族）——**比 §5.1b 里写的要少得多**

提出"继续移植"，本轮先把它摸清（没有改代码），结论是**瓶子族已经完成了大半**，下批只需补两处：

### 106.1 现状（都在仓库里，逐项核对过）

| 项 | 现状 | 依据 |
|---|---|---|
| 瓶子物品族 | **已注册**：`registry/GTMultiItems.java` 读 `GTMultiItemsGen.ENTRIES`（1278 条多物品里的 `bottles` 类目，约 **180 个**：`mineral_water`…`beet_juice`、`medicine`、`laxative`、`ink_bottle`、`tar_bottle`、`glue_bottle`、`mercury_bottle`、`holy_water`、`purple_drink`、`green/pink/blue_slime_bottle`、`bottle_empty`…），`MultiItem extends TechItem`（带 GT6 的 tooltip 行） | `GTMultiItems:44-70`、`GTMultiItemsGen:160-330` |
| 贴图 | **171 张** `assets/gregtech/textures/item/bottles/*.png`（`beer.png`/`apple_juice.png`/`holy_water.png`…） | 目录清点 |
| 饮用语义 | **已有，但走的是"每流体代理物品"**：`item/FluidItem.use` 已实现 **250 mB** 饮用 + 消耗 + 退回空瓶（非创造模式），并接 `GTDrinks.drink` | `FluidItem:57-80`；测试 `DrinkTests.drinkingABottleItemWorks` / `drinkingGivesBackAnEmptyBottle` |
| 战利品/配方引用 | 瓶子 id 已被 `gt.bottles` 表与 JEI 提示引用（`tech:bottle_empty`/`tech:beer`/`tech:holy_water`/`tech:mercury_bottle`…），所以它们既有物品又有语言键 | `GTLootGen:138-156`、`GTMainRecipes:58-60` |

### 106.2 缺的两处（下批的工作量就在这里）

1. **瓶子物品本身不能喝**：`MultiItem` 只是普通物品（`GTMultiItems` 的类注释就写着 "behaviors (food stats, bee genetics) arrive with their systems"）。要按 GT6 的 `MultiItemBottles` 把每个瓶子接到"它代表的那瓶流体"上，然后复用 `FluidItem.use` 的逻辑（250 mB 饮用 + 消耗 + 退空瓶）。GT6 里还要处理**一个瓶子对应多种流体**（`addItem(1, "Sea Water", …, FL.Ocean.make(250), …, FL.OceanGrC.make(250), FL.Tropics_Water.make(250))`）。
2. **灌装配方**：GT6 的瓶子不是"用桶右键"，而是**无序合成**——`1000 mB 容器 + 1/2/3/4 个空瓶 → 1/2/3/4 瓶`（`MultiItemBottles:129-132`、`:143-146`、`:164-167`、`:211-224`、`:232-235`、`:268-271`、`:277-280` 各四行），覆盖 milk/honey/seed oil/juice/slime/maple sap/rainbow sap 等；另有 `OD.dropHoney/Honeydew/RoyalJelly → 1300/1301/1302` 的直接合成行。

### 106.3 下批的验收口径

- `DrinkTests` 新增"**瓶子物品**能喝（不是代理物品）"：拿 `GTMultiItems` 里的 `beer` 物品走 `use`，断言酒精 +30、瓶子消失、空瓶到手（现有两项测试已经把这个口径写好，只需换物品来源）。
- 新增"灌装配方存在且数量对"：4×空瓶 + 1000 mB 容器 → 4 瓶（牛奶/蜂蜜/种子油各一条），以及 `honeydew`/`royal_jelly` 的直接合成行。
- 允许的差异：GT6 的矿物词典容器（`OD.container1000milk` 等）在移植版是具体物品/流体，配方要用移植版的 id——按 §20/§24 的老办法逐条映射并在测试里钉住。

**没有执行 `git commit`。**

---

## 107. 大批次：§103.C 瓶子族收口 + 三路并行（流体危害 / 覆盖板 / 能力句柄 + 矿石资产）+ §103.A 审计结论

提出"可以多移植一堆东西，一次花几个小时也没事"，所以本轮一次做四路：**瓶子族收口**（§106 预研的落地），另开三个并行任务分别做**流体危害标记与执行**、**覆盖板（排水盖 + 进气盖）**、**§97 能力句柄断言 + 矿石资产补齐**；最后统一门禁 + 重建 jar。

### 107.1 瓶子族收口（GT6 `MultiItemBottles` 真的能喝了）

§106 的预研结论是"瓶子物品族已注册、贴图/语言键/模型都在位，缺的是①瓶子本身不能喝 ②灌装配方"。本轮把这两处做完：

| 文件 | 内容 |
|---|---|
| `tools/extract_gt6_bottles.py`（新）+ `data/generated/GTBottlesGen.java`（生成） | 从 GT6 `MultiItemBottles.addItem(...)` 抽出「瓶子 id → 它装的那瓶流体」，共 **169 瓶**（1 行跳过：`clouded_bottle` = 战利品瓶，本来就没有流体）。**只用生成器产表**（项目惯例），并与 `GTMultiItemsGen` 的 id **按序逐条对齐 + 显示名交叉校验**——任一侧漂移就报错退出，不会静默配错流体 |
| `content/food/GTBottles.java`（新） | 表访问（`fluidKeyOf(id)`）+ 跳过清单 |
| `item/BottleItem.java`（新） | 右键喝 **250 mB**（GT6 每瓶都是 `.make(250)`）→ 走 `GTDrinks.drink` → 消耗瓶子 + **退回 GT6 自己的空瓶 `bottle_empty`**（缺了才回落原版玻璃瓶）；非饮品（水银/墨水/胶水/润滑油/焦油/食用油）与"移植版没有的那种流体"一律 `pass`、不吃掉 |
| `registry/GTMultiItems.java` | 把这 169 个 id 注册成 `BottleItem`（其余仍是原来的展示物品）。**物品 id / 模型 / 中英语言键全不变**——实测 170 个瓶子模型 + 170 条 `en_us` + 170 条 `zh_cn` 键全部在位 |
| `loaders/Loader_BottleFillingRecipes.java`（新） | GT6 的**灌装**行：`1000 mB 容器 + N 个空瓶 → N 瓶`（N=4/3/2/1），源行 `MultiItemBottles:129-132/143-146/151-154/164-167/211-224/232-235/268-271/277-280`，共 **10 族 × 4 = 40 条**；GT6 的矿物词典容器在移植版落到"每流体 `fluid_item_*` 代理物品" |
| `gametest/DrinkTests.java` | 新增 **3 项**：瓶子物品能喝（酒精 +30、瓶子消失、空瓶到手）、表覆盖（169 行里 ≥150 是真 `BottleItem`、跳过 ≤2）、灌装配方（40 条 + `bottles/milk_x4` 在配方管理器里、4 瓶产物 + 5 个原料） |

**没做**：`OD.container250juice` 那一行（`MultiItemBottles:236`，250 mB 容器）与蜂蜜滴/蜜露/蜂王浆的直接合成行（`:169-174`，需要养蜂批的掉落物品）——前者与"1 瓶"行同形会撞车。

### 107.2 三路并行（接口先行 + 文件白名单）

并行规矩照交接单：先定接口与**文件白名单**（互不重叠）、禁止跑 gradle/git、每条只做 javac 自检。

#### 107.2.1 流体危害（代理 A）

- **GT6 的事实**（逐条回读原版）：危害不在方块也不在管道上，而在 `CS.java:1508 FluidsGT` 的三个名字集合：`FL.java:756 acid(Fluid)`、`:760 plasma(Fluid)`、`:770 gas(Fluid)`；其中 **gas/plasma 是流体的物理状态**（`FL.java:1105/:1106` 按 `STATE_GASEOUS`/`STATE_PLASMA` 分类），**acid 是材料属性**（`FL.java:1118` 按 `TD.Properties.ACID`）⇒ 三者**互相独立**（氯化氢既是 gas 又是 acid，`MT.java:1020`），GT6 也是三个 if 挨着测（`MultiTileEntityPipeFluid.java:296/:302/:308`）。
- **移植版的对应**：`api/fluid/FluidHazards.java`（新，234 行，纯函数、无缓存）从 `RegisteredFluids.FluidEntry` 的既有元数据回答同一组问题（`gas()` = `STATE_GASEOUS`、`FluidFlags.PLASMA` = `STATE_PLASMA`；acid 走 `FluidVisualPolicy.material(entry)` + `MaterialProperty.ACID`），`_flowing` 变体经 `GTFluids.entryForFluid` 一并命中 ✓；并给出真值表 `proofRejects(...)` 与**每 tick 损耗**（gas 8 / acid 16 / plasma 64，抄自 `MultiTileEntityPipeFluid.java:297/:303/:309`）。
- **执行三处**：①管道 `FluidPipeBlockEntity.checkSafety`（**原本是空实现**，`tickServer` 早已调用它 ⇒ 天然入口）：不抗则漏气/漏酸/漏等离子，酸另加 `applyChemDamage(2)` + 1% 销毁（`:311-312`）；②储罐 `TankBlockEntity`：填充闸门拒绝（`TileEntityBase08Barrel:251-254`）**并且**在 `fill` 里也补同一道闸（**只改 `isFluidValid` 不构成拒绝**——Forge 把它当查询，而 `fill` 原本直接调 `tank.fill`，这是代理发现的坑）；③逐 tick 腐蚀（`:170-186`）：酸不抗 → 全量销毁 + 变空气，气/等离子只清空、容器存活。
- **呼吸**：`content/hazard/BreathingGasEvents.java` 复刻 `GT_API_Proxy.java:520-528`（`LivingTickEvent` @ LOWEST + **眼睛所在方块** `roundDown(posY+getEyeHeight())`）与 `BlockBaseFluid.java:411-415`（**非空 `mEffectsBreathing` 才是开关** + `isImmuneToBreathingGases` 免疫 + 每 20 tick 2.0 溺水伤害）；移植版里唯一"既是世界方块又是 gas"的是**天然气泉**（`gas_natural_gas`，效果表 `Loader_Blocks.java:153`：poison 300 / confusion 120）。
- **测试 7 项**（BASE 23000/23000/100）：标记表 + HCl 同时酸/气且损耗 24、`_flowing` 变体、真值表 32 格、储罐拒/收/溶解、管道漏损、呼吸天然气（牛/生存玩家/铁傀儡/创造）、非气体与无效果气体零伤害。

#### 107.2.2 覆盖板：排水盖 + 进气盖（代理 C）

- **排水盖 `drain`**（`CoverDrain:68-159`）：节流 `%20==5`；相邻**源**流体方块 → 1000 mB，**SIMULATE 全量通过才 EXECUTE** + 方块变空（GT6 `FL.fillAll` 的"全有或全无"148/152）；重力闸门（`:147`）；接雨水（`%100==10`，量 = `max(1, rainfall*10000) × (雷暴?2:1)`）。
- **进气盖 `air_vent`**（`CoverVent:40-85`）：**回读原版纠正了我的误解**——GT6 的 `CoverVent` 是**进气口**（按面错峰 360 tick 的槽位 {30,90,150,210,270,330}，前方"可采空气"时把 **256000 mB** 空气灌进机器），不是排气口；移植版确实有那三种空气流体（`RegisteredFluids:289-291` 的 `air`/`enderair`/`netherair`）⇒ 已按原版改成进气，把我最初做反的排空支路删掉。**`FL.fill_` 是"尽量灌"不是"全有或全无"**（`FL.java:826-828` vs 严格版 `fillAll` `:833`），所以 256000 灌进小罐 = 灌满而不是整体拒绝 ✓（测试专门钉住这点：`amount == min(256000, capacity)`，若误用 `fillAll` 会得 0）。
- **节流用机器自己的 tick 计数 `coverTicks`**（该字段本来就有）而不是 `level.getGameTime()`：GameTest 在一次 game tick 内循环调 `serverTick`，世界时间全程冻结、节流守卫永不触发（现有 `tickPumpCover` 就有这个不可测问题）。
- **测试 7 项**（BASE 25000/25000/100）：id + 雨水公式、抽相邻水源、抽 GT6 海水方块（罐里是海水而不是水）、负面用例（底面朝下不抽重液体）、进气节拍（89 tick 仍空、第 90 tick 进气）、前方非空气不进、三维度映射 + 六槽位集合。
- **记档未做/未断言**：`CoverDrain` 的量子分支（移植版无量子）、无限水/湖/海 16000 mB 快捷（改为每格 1000 mB，因为移植版世界里就是真的 `gregtech:seawater` 方块）、XP 球→液态 XP（无 `FL.XP`/`MD.OB`）、`CoverVent:52-61` 的生物群系集合回退（1.20.1 只有三维度；modded 维度会拿到主世界 `Air`）、雨水的实活路径（`Level.isRaining()` 是插值 `rainLevel`，每 tick ±0.01、要 >0.2，一次性测试体等不到那 21 个真实 tick ⇒ 只钉纯公式）。

#### 107.2.3 §97 能力句柄断言 + 矿石资产补齐（代理 B）

- **§97 收尾**：`gametest/CapabilityHandleTests.java` 追加 **1 项**（`fluidBurningBoxCapabilitiesAreCached`），补上第四台机器（流体燃烧箱）的"同方向两次同实例 / 不同方向同句柄（它按 sink 缓存，与料斗/管道按方向缓存相反，javadoc 写明）/ `invalidateCaps()` 后失效**且重新查询可用**"三件事；solid 变体不覆写 `getCapability` ⇒ 记档跳过。
- **矿石资产补齐（552 → 0）**：`tools/generate_ore_block_assets.py` 的选材从"`ore(...)` 工厂 + 世界生成表"扩到 **5 个来源取并集**（`MaterialForms` 含 `ORES` / `ore()` 工厂 / 世界生成表的标识符引用 / 同表的**字符串**引用（`GTStoneLayersGen` 用字面量命名矿石）/ 磁盘上已有的矿石资产），生成结果：blockstate **484 → 1038**、block model **484 → 1038**、item model **484 → 1038**（242 → **519** 材料），**新写 1662 个文件、旧文件逐字节不变、删除 0 个**（重跑第二次输出 `Wrote 0 ore block asset files`，幂等）；3114 个文件全部是合法 JSON。
  覆盖取证：`build/runclient.log` 里 552 条矿石"missing model for variant" = 276 材料 × 2 前缀，这 276 个材料已全部生成三件套 ✓。
  新测试 `gametest/OreAssetCoverageTests.everyOreBlockShipsItsAssets` 遍历全部 `OreBlock` 断言三件套存在 + blockstate/item model 的引用不悬挂，并把 `checked/missing` 打进日志。

### 107.3 §103.A 审计结论（对照 §103.A 的五条清单，逐条给证据）

§103.A 的约束是"只优化 GT 自己加进区块的东西，原版一律不动"。本轮把清单逐条查了一遍，**结论是：五条里四条已经没有可动的余地，剩下的 80% 是"内容体积"而不是"实现浪费"**：

| §103.A 清单 | 结论与证据 |
|---|---|
| ①27 种 GT 石层方块的状态复用 | **已经是复用的**。`BlockState#setValue` 不分配——`StateHolder.setValue` 从邻居表取缓存实例；worldgen 目录下 46 处 `setValue(...)` 全在**一次性结构**（地牢装饰/树/蜂巢）里，不在逐格热路径；矿石/石层写入走 §103.B 的 `stateFor(...)` 与 `layer.stone()` 预建状态 |
| ②GT 矿石方块的属性组合数 | §103.B 已把"岩石"压成 **1 个属性**（970 块 × 39 = 37,830 个状态）。**代价要认账**：state 化让"同一矿石 × 不同岩石"在 section 调色板里算**不同条目** ⇒ 每 section 的 `bits/entry` 可能 +1（≤16 条目 4 bit、17–32 是 5 bit、33–64 是 6 bit ⇒ 最贵 4096 bit = 512 B/section）。相对"每个矿石一个 BE ≈150 B × 每区块数百个"，净收益仍是数量级差 |
| ③`GTWorldFluidBlock` 的 `skipRendering`/`updateShape`/`neighborChanged` | **逐字照抄 vanilla 语义**（这三个覆写是为绕开 Forge 的 `LiquidBlock(Supplier)` 把私有 `fluid` 留成 null 的坑，见类 javadoc）⇒ **面剔除没丢**：仍是"邻居流体 `isSame` 相同才剔除"。**而且 §105 的对称 `isSame` 让它更强**：以前 GT 水/原版水交界两侧都不剔除，现在两侧都剔除 ✓ |
| ④GT 的 BE | 矿石已由 §103.B 清零；其余机器 BE 数量按 §101 实测无异常 |
| ⑤`getRenderTypes` 是否过宽 | `block/` 下只有 `OreBakedModel` 返回 `cutoutMipped`（矿石需要），没有过宽的 `ChunkRenderTypeSet`；返回 `skipRendering=false` 的几个 GT 方块（模具/坩埚交叉）都是**非满方块**，false 正是它们该有的答案 |

**所以 §103.A 的剩余空间**：要么动"内容体积"（少铺 GT 石头/矿石/水，与移植目标冲突），要么动原版区块机制（明令不动）。**记档：在"不碰原版"的约束下，§103.A 已没有剩余的廉价收益**；真正的线性杠杆仍是视距/模拟距离（客户端侧）。

### 107.4 本轮的既有发现（记档，不改）

1. **客户端的 `Unable to load model` 噪声是全局性的**：最后一次客户端日志里有 **59,744** 条（`unit` 1086、`plant_gt_*` 各 1030、`dust*`/`scrap_gt` 各 984…），根因是材料物品**没有自己的模型文件**（设计如此），而运行时别名 `Material model aliasing: 55960 mapped, 0 missing` ✓ 覆盖了全部 ⇒ **渲染正确，只是日志噪声**。要真消掉得为约 6 万个物品生成存根模型，不值（矿石方块那一族已由代理 B 的 1038 个资产单独消掉）。
2. **`invalidateCaps()` 的"只失效不重置"**：全仓 65 处覆写里，只有少数（`BurningBoxBlockEntity`/`ReactorCoreBlockEntity`/`CapabilityRelayBlockEntity`/`EnergyRelayBlockEntity`）在失效后清空/重置字段，其余多处字段是 `final`，本就不能重置；`invalidateCaps()` 的唯一调用点是 BE 被移除（之后不再查询）⇒ **记为良性不一致**，不为它做全局清扫（要做就得把字段改成非 final + getter 惰性重建，侵入性更大且没有可观测收益）。
3. **能力句柄断言未覆盖 solid 燃烧箱**：`SolidBurningBoxBlockEntity` 不覆写 `getCapability`，任何方向都是基类 `LazyOptional.empty()`，无句柄可比（javadoc 记档）。
4. **矿石资产比注册方块多 34 个材料**：生成器的并集含 519 材料（1038 方块），而门禁 §103.B 报 970 个 `OreBlock`（485 材料）——多出的是旧石头矿 id（`ore_basalt`/`ore_marble` 等），是**惰性文件**（没有对应方块就不会被 `ModelBakery` 读），保留它们是为了不删既有合法文件、并兜住"离线无法证明的那 1 个材料"的不确定性。新测试会在门禁里打出真实 `checked/missing` 数供复核。

### 107.5 并行作业的四条教训（写进交接单）

1. **javac 会编到别人的半成品**：用 `-sourcepath src\main\java` 自检时报 `TankBlockEntity.java:110: missing return value` ——那是另一个代理正在写的中间态。正确姿势：`-cp "<classpath>;build\classes\java\main"` + `-implicit:none` + **不用 `-sourcepath`**，并把自己的每个文件（含新建的）显式列为输入。
2. **PowerShell 撕碎 `-J-Duser.language=en`**：不加引号会被解析成 `.language=en` 并报"无效的标志"；加引号后错误信息是英文，不再被 GBK 控制台编码搅乱。
3. **合并前先核实"是谁改的"**：代理报告"只做加法"，但 `grep` 发现它还动了一处既有逻辑。可靠手段是**反查上一轮的构建产物**：`javap -p -c` 直接看 `build/classes/java/main/<类>.class`，或从 `build/libs/*.jar` 里解出类再看（本轮用后者定案：00:47 的 jar 在该守卫处调的是 `Level.getGameTime()`，所以那处改动发生在本轮，已回退）。
4. **设计分叉必须回读原版**：我按类名把 `CoverVent` 当"排气口"下发了规格，代理回读 `CoverVent:40-85` 后证明它是**进气口**并指出移植版有对应空气流体 ⇒ 立刻改回原版语义。**指派任务时给出的"事实"也要允许被推翻。**

### 107.6 验证（门禁 / jar / 注册量）与两处**新踩的坑**

| 项 | 值 |
|---|---|
| GameTest | **590 项全过**（`All 590 required tests passed :)`，`build/gametest.log` 2026-09-18 01:35:08；`failed!` **0** 条、`BUILD SUCCESSFUL`） |
| 测试增量 | 571 → **590**（**+19**）：`DrinkTests` +3（瓶子族）、`FluidHazardTests` +7、`CoverAttachmentTests` +7、`CapabilityHandleTests` +1、`OreAssetCoverageTests` +1 |
| jar | `build\libs\gregtech-1.0.0.jar`，**30.4 MB**、**66,446 个条目**，SHA256 `CC7D1EACD01D8232AC94D4EEDAEE8E3FB6C5C3671EB5CACEB115FD894216A46D` |
| 注册量 | 材料 **1156** / 物品 **70,754** / 方块 **12,093** / 流体 **925**；缺失 token **49**；多方块部件 **89**（与 §103.B 基线逐项一致——本轮是行为与资产，169 个瓶子 id 只是换了实现类型，不增计数） |
| 顺序不变量 | `Java 最新 < 门禁日志 < jar` = **True / True**（`tools\handoff_check.py`） |
| 门禁自证数据 | `[gametest] ore assets: checked 970 ore blocks (485 small), missing assets 0`（§103.B 记档的 552 块缺模型 → **0**）；`[bottles] table rows: 169; BottleItems: 169; rows without a port fluid: 6; non-bottle items: 0`；`[bottles] filling rows registered: 40 (skipped: [])`；`[gametest] 03.B ore blocks=970 hosts=39 states=37830`；`[drinks] table: 265 of 272 GT6 rows resolve onto port fluids; unresolved: 7` |

#### 107.6.1 门禁第 2 次红：**世界复用 + `setBlock` 同状态是 no-op**（不是实现 bug）

第 2 次门禁只有一条红：`pipeeatswhatitsflagsdonotcover failed! the steel pipe takes the gas`。定位与结论：

1. **先证伪"管道真的拒绝了气体"**：`FluidPipeBlockEntity.fill`（`:446-456`）只有"找第一个空罐或同流体罐"这一条逻辑；`FluidTankGT.fill`（`:201-219`）在空罐上必然返回 `min(请求, 容量)`；`PipeSpec.of` 给 `pipe_tiny_plastic` 的容量是 **100**（`FluidPipeDefinitions:21` 的 baseCapacity 100 × TINY 的 `capacityMultiplier()==1`），而 `gasProof` 只被逐 tick 的 `checkSafety`（`:132`）读，**不在 fill 路径上** ⇒ 返回 0 只可能是**那个罐里本来就有别的流体**（`:216`）或**已经装满**（`space==0` ⇒ `filled<=0` ⇒ `:207`/`:219` return 0）。
2. **为什么木管绿、塑料管红**：`build/gametest-run/world` 是**跨轮复用**的世界。第 1 次门禁（全新世界）结束时：木管装过 50 mB 又漏掉 8（剩 42），塑料管装满 100 mB 且**抗气所以不漏**。第 2 次门禁里 `placePipe` 用 `level.setBlock(pos, block.defaultBlockState(), 2)` 重放**同一个状态**，而 vanilla `LevelChunk.setBlockState:224` 对完全相同的状态直接 `return null`——**不重建方块实体**（`Level.setBlock` 也返回 false）⇒ 塑料管原样带着那 100 mB：`fill(gas,1000)` 的空间为 0 ⇒ 返回 0；木管还剩 8 的空间 ⇒ 返回 8 > 0 ⇒ 照旧通过。一红一绿，正是"同一流体、容量有没有用尽"的差别。
3. **修法（测试侧自愈，生产代码零改动）**：`placePipe`/`placeTank` 先 `level.removeBlock(pos, false)` 再放方块，并把"新放的实体是空的"变成断言（`a pipe placed at … starts empty` / `a vessel placed at … starts empty`）——旧行为下这条会带确切数字先红，而不是让业务断言给出误导性的"管道拒绝了气体"。
4. **这次故意没有换新世界**：被污染的世界正好是这条回归的夹具；修完仍在**同一个复用世界**上跑，590 项全过。

**教训**：`setBlock` **不是**"重新放置"——同状态是 no-op，方块实体连同它的 NBT/内容会**跨轮留存**；凡假设"新放的方块实体是干净的"的测试，都必须先 `removeBlock`。这是 §104"红了的那条测试自己会留脏"的另一面。

#### 107.6.2 `:jar` 撞上 **65,535 条目上限** ⇒ 打开 zip64

代理 B 的矿石资产把 jar 推过了普通 ZIP 的上限：`:jar` 直接 `FAILED`——`Archive contains more than 65535 entries. To build this archive, please enable the zip64 extension.`（本轮 **66,446** 条）。**没有删资产**（每一条都是游戏要读的文件：554 个矿石方块 × blockstate + block model + item model = 1,662 个新文件），而是在 `build.gradle` 的 `jar` 任务上打开 `zip64 = true`；重建后 `:jar` → `:reobfJar` 全过，jar 内 `gregtech.mixins.json` / `gregtech.refmap.json` / `WaterFluidMixin.class` 与 manifest 的 `MixinConfigs` 四项俱在（reobf 不丢 zip64 扩展）。打开后条目数上限变成约 40 亿，这条天花板不再适用——但**资源只增不减的项目迟早会再撞上别的天花板**，记档。

#### 107.6.3 在 PowerShell 里直接跑 `gradlew` 会撕碎 `-D` 参数（白费一次门禁）

第一次门禁尝试写成 `.\gradlew.bat runGameTestServer … -Dorg.gradle.jvmargs=-Xmx512m …`（PowerShell 里直接调用），PowerShell 把 `-Dorg` 当成自己的参数名吃掉，Gradle 收到的是 `Task '.gradle.jvmargs=-Xmx512m' not found in root project`，**10 秒即 BUILD FAILED**（连世界都没碰）。**必须 `cmd /c "…"` 包一层**——交接单 §2 的命令本来就写成那样，这次是没照抄。判断法：日志里出现 `Task '.gradle…' not found in root project` 就是它。

**没有执行 `git commit`。**

---

## 108. 世界流体危害（泡在酸/油/温泉水里）+ 传感器按**机器数**补齐到 20 + 三路并行（覆盖板 / 食品物品族 / 物品行为层）

提出"继续移植，长线移植我睡醒了检查"，所以照 §107 的办法再开四路：**世界流体危害**与**传感器矩阵**，另开三个并行任务分别做**覆盖板第二批**、**GT6 食品物品族**、**GT6 物品行为层**；接口先行 + 文件白名单 + 只做 javac 自检（规矩见 `build/round108-brief.md`）。

### 108.1 世界流体危害：GT6 的两张效果表**全部**落地（关掉 §91/§107 记档的"酸液浸泡"）

§107 只做了气体（天然气泉）与管道/储罐两条路径，**"泡在里面"这条一直空着**。这一轮把 GT6 `Loader_Blocks.java:137-153` 的**每一行**都落了地——移植版对那 9 个流体都有世界方块（`Loader_Fluids.WORLD_FLUID_PATHS`），所以整张表都可玩。

| GT6 注册行 | 流体（移植版 id） | 泡在里面（bathing，`addEffectBathing`） | 头在里面（`addEffectBreathing` / `BlockWaterlike.addEffect`） | 网（`setWeb`） |
|---|---|---|---|---|
| `:146` | `watergeothermal` | **再生 100/0 + 抗性提升 2400/2** | — | — |
| `:149` | `liquid_extra_heavy_oil` | 失明 60/1（另三行是 IE 药水，见下） | 中毒 300/0 + 反胃 120/0 | **✓** |
| `:150` | `liquid_heavy_oil` | 同上 | 同上 | **✓** |
| `:151` | `liquid_medium_oil` | 同上 | 同上 | — |
| `:152` | `liquid_light_oil` | 同上 | 同上 | — |
| `:153` | `gas_natural_gas` | — | 中毒 300/0 + 反胃 120/0 | — |
| `:137` | `swampwater`（`BlockSwamp`） | — | **饥饿 300/0 + 反胃 120/0**（软泥怪免疫，`BlockSwamp:194`） | — |
| `:135`/`:136` | `riverwater` / `seawater` | **什么都没有** | **什么都没有** | — |

**三条容易做错的语义，逐条照抄**：

1. **效果表本身就是开关**：没有行的流体连溺水伤害都没有——所以原版的海水/河水完全无害（`:135,136` 一个效果都不加），"它是水"不是安全的原因。
2. **两条"头在里面"路径的免疫判定不同**：`BlockBaseFluid:412` **永远**问气密服（连是液体的油也问气密服），`BlockWaterlike:227` 按流体分——是气体问气密服、否则问**化防服**；沼泽走的是后一条，而且它的材质**就是水**（`BlockWaterlike:65` `super(aFluid, Material.water)`）⇒ **沼泽水不会淹死人**，只让你饿和晕。
3. **溺水看材质不看类型**：油与天然气是 `MaterialOil`/`MaterialGas` ⇒ 淹（每 20 tick 2.0）；温泉是 `Material.water`（`:146`）⇒ 不淹。

**实现**：新 `content/hazard/WorldFluidEffects.java`（纯函数：两张表 + `headGate`/`headInsideDrowns`/`actsLikeWeb` + `applyBathing`/`applyHeadInside`）、新 `content/hazard/BathingEffectEvents.java`（`LivingTickEvent` @ LOWEST，按实体包围盒取流体，等价于 GT6 每 tick 的方块碰撞回调）、`BreathingGasEvents` 重构为走这张表并且**效果每 tick 施加、伤害每 20 tick**（GT6 就是这个节奏：离开毒气 1 tick 也会带着 300 tick 的中毒）、`GTWorldFluidBlock.entityInside` 补上网（`BlockBaseFluid:405` `setInWeb()` = 原版蛛网的 0.25/0.05/0.25）。

**记档不做**：油的三条 IE 药水行（`ID_FLAMMABLE`/`ID_STICKY`/`ID_SLIPPERY`，`GT_API.java:786-789` 明确来自 Immersive Engineering，1.20.1 没有该模组）——只做同一行里原版的失明 60/1；`BlockBaseFluid:374` 的 `getBlocksMovement`（1.20.1 的"方块是否阻挡移动"来自 `BlockBehaviour.Properties`，`LiquidBlock` 改不了它而不替换原版流体物理）。

**测试**：新 `gametest/WorldFluidEffectTests`（BASE 35000）**5 项**：表逐行对照（并把整张表打进日志）、泡温泉拿到再生/抗性且**化防服（创造）拿不到**、沼泽只饿不淹 + 软泥怪免疫 + 油既毒又淹又失明、重油像蛛网而中油不像（两只牛的位移对比）、事件每 tick 给效果而伤害按 20 tick 的节拍。

### 108.2 传感器：按**机器数**补齐到 GT6 的 20 台（§3 那行"20 vs 7"是文件数误报）

§3 把传感器写成"原版 20 / 移植版 7"，那是**数文件**：GT6 的 20 台传感器是 20 个小类（`gregtech/tileentity/sensors/*.java`），移植版用**一个** `SensorBlockEntity` + `Kind` 枚举 + `registry/GTSensors` 实现同一批机器。按机器数对齐后移植版原本有 **16** 台，缺的正好是：

| 缺口 | GT6 依据 | §108 处置 |
|---|---|---|
| 四个重量计并成一个 | `Loader_MultiTileEntities:1988-1991`（Light/Medium/Heavy/Super Heavy） | 移植版原有的 `sensor_weightometric` **就是 Heavy**（吨、上限 65535），据此补 `sensor_weightometer_light`（克）、`sensor_weightometer_medium`（千克）、`sensor_weightometer_super_heavy`（千吨）——四者只差一个刻度（`MultiTileEntityWeightometer*:43,62`），同一台机器的四种量程 |
| TPS 计完全没有 | `:1992`（`MultiTileEntityTPSmeter`） | 新增 `sensor_tpsmeter`：**不需要邻居**（量的是服务器），每 20 tick 采一次墙钟，`值 = 20×100000 / 经过毫秒`（`:46`，20 TPS 显示 2000），上限 2000（`:57`） |

于是 20 台一一对应（机器 id 31000–31022 全部有落点）。**顺带**：`SensorMeasurements` 补上四个刻度的纯函数（含 GT6 自己的 `B[16]-1 = 65535` 饱和）、`serverTick` 的十 tick 节流拆出 `measure(...)` 以便 GameTest 在同一个 tick 里驱动（与 §107 覆盖板同样的做法）、`tools/add_sensor_variants.py`（新）幂等地补 24 张贴图 + 4 个方块模型 + 4 个 blockstate + 4 个物品模型 + 中英各 4 条语言键（重跑 `textures copied: 0`）。

**测试**：新 `gametest/SensorMatrixTests`（BASE 37000）**4 项**：20 台机器逐条对上 GT6 的机器 id 与名字（并打进日志）、四个刻度与四个饱和点、一个装着 4 个粗铁的箱子被**四台**重量计同时称（读数与纯函数逐个相等 + 单位字形是 `gramm`/`kilogramm`/`ton`/`kiloton`）、TPS 计的公式与"没有邻居也能读"。

**仍然缺的（记档）**：①这 20 台传感器**都没有合成配方**（GT6 `:1979-1998` 每台都有 `CR.shaped`，移植版连原有的 16 台也没做；补它要先把 `IL.Thermometer_Quicksilver`/`IL.Electro_Meter`/`IL.Tacho_Meter` 这几个配方钥匙物品做出来）②六个格子的符号贴图只覆盖了一部分单位（`gramm`/`kilogramm`/`kiloton` 都在，见 `textures/block/overlays/characters`）。

### 108.3 覆盖板第二批（代理 W1）：61 个文件其实是 **48 个行为 + 10 个基类 + 3 个纯贴图**

§3 那行"61 种、其余大量覆盖板缺"这次被逐文件拆开核对：**26 种早就有了**（`MachineCoverSpec` 12 + `PanelCover` 12 + `drain`/`air_vent` + pump/conveyor/robot arm 三族），**本批补 9 种**，**14 种是物流核心的 cover（移植版没有物流核心，不适用）**，**1 种（`CoverTextureCanvas`）GT6 绑在移植版没有的 Canvas 物品上**，其余是"方块兼 cover"与纯贴图 cover。

**本批补的 9 种**（每个方法的 javadoc 都带 GT6 行号）：`crafting_table_cover`（`CoverCrafting:46-59`，空手右键开**原版** `CraftingMenu`，不新增 menu 类型/网络包）、`item_retriever_cover`（`CoverRetrieverItem:55-79`，`%20==15`、每次 1–64 个、过滤 + 黑白名单）、`item_filter`/`fluid_filter`（`CoverFilterItem:115-127`、`CoverFilterFluid:117-129`）、`pressure_value`（`CoverPressureValve:44-64`）、`panel_asphalt`（`CoverAsphalt:38-41`，×1.3）、红石火把/中继器（`AbstractCoverAttachmentTorch:35-68`）、`blank_cover`/`warning_cover` 的凿子换贴图（`CoverTextureMulti:63-78`，6/20 种设计）。

**顺带查出两处真 bug（都是我这轮接线的）**：

1. **`CoverItems.portCoverId` 漏了两个 id** ⇒ `pressure_value` 与 `panel_asphalt` 的 `behavior(...)` 恒为 `null`：玩家能贴上去，但 `getCoverId` 永远不派发，行为**永不执行**。已补 `|| id.equals("pressure_value") || id.equals("panel_asphalt")`，并把代理写的"缺口哨兵"测试翻面成 `pressureValveAndAsphaltAreDispatchableCovers`（断言派发成功而不是断言缺失）。
2. **机器上红石火把的语义是反的**：GT6 的火把是**反相器**（线有电 → 火把灭，`CoverRedstoneTorch:43` + `:50-57` 的 `mVisuals==0?15:0`），中继器才是跟随器（`CoverRedstoneRepeater:43`）；移植版 `updateCoverSignals` 原本把两者都写成 `mRunning ? 15 : 0`。已改成走 `CoverUtilityBehaviors.torchVisual/repeaterVisual/torchSignal`，`BasicMachineBlockEntity` 里只留一个 `redstoneCoverSignal(side)` 私有方法。

**接线**（文件白名单外）：`tickCovers` 末尾调 `tickUtilityCovers(level,pos)`（它自己走六面、自带 `panels.stopped()` 闸门）；`BasicMachineBlock.use` 加三种交互（空手点 `crafting_table_cover` 的面 → 工作台；螺丝刀 → 翻黑白名单；软锤 → 清空滤器；持非 cover 物品 → 设为滤器）；`BasicMachineBlock.stepOn` 在**顶面**是 `panel_asphalt` 时给实体加速。

**测试**：新 `gametest/CoverBehaviorTests`（BASE 27000）**17 项**，其中 9 项走"真机贴 cover → `getCoverId` → `CoverItems.behavior` → 行为"的完整派发链。

**记档未做**：①**压力阀的宿主**——GT6 的阀是**流体管道**的 cover（`CoverPressureValve:44,51` 只收单罐管道），移植版 cover 只贴在机器面上 ⇒ 保留原版判定作纯谓词，`tickPressureValve` 改放"机器自己的输出罐满时释放"，并在 javadoc 写明这是本批唯一的语义替换 ⚠️ **已于 §111 关闭**：`FluidPipeBlockEntity` 现在是覆盖板宿主，`valveCanAttachTo` 找到了它的 caller，阀门按 `tanks[0]` 释放（机器面的宿主仍然保留）；②红石火把/中继器的宿主同理（GT6 是绝缘红石线的 cover，读的是线的 `mRedstone`；移植版用机器的 `mRunning` 顶替）；③**滤器的管道拦截**（GT6 `interceptItemInsert/Extract`、`interceptFluidFill/Drain`）本轮**已接线**：`SidedItemHandler.insertItem/extractItem/isItemValid` 与 `SidedFluidHandler.fill/drain/isFluidValid` 都过 `coverFilterPermits`/`coverFluidFilterPermits`，检索器那面按 `CoverRetrieverItem:138-139` 直接全挡（`coverBlocksItemTraffic`），并新增一项测试（`theFilterCoversFilterThePipesOnTheirOwnFace`，真机 + 真 capability）；**没有滤器 cover 的面一律放行**——这条是保持其余机器行为不变的关键，也是接线时发现代理的谓词在"无 cover"时返回"拒绝"后修掉的（否则所有管道会被静默掐断）；④滤器匹配口径差异：GT6 用 `ST.equal(...,ignoreNBT)`＝物品+meta、**忽略 NBT**，移植版无 meta ⇒ 无 tag 模板=通配、有 tag 则精确比 NBT（**比原版严**），两侧都写进了 javadoc；⑤物流 14 种 cover 不适用；⑥`CoverTextureSimple`（方块兼 cover）未接线。

### 108.4 §3 缺口表的核对：**"传感器 20 vs 7"与"世界生成 树/遗迹缺"都是过期结论**

§107 的教训里有一条"审计工具也会撒谎"，这轮顺着它把 §3 的两行逐条核对了一遍，两行都是**口径问题**（数文件 / 数旧结论），现在都改掉了：

| §3 原行 | 核对结果 | 依据 |
|---|---|---|
| 传感器"20 vs 7" | 原版 20 是**机器**（20 个小类），移植版用**一个** `SensorBlockEntity` + `Kind` 枚举实现同一批机器 ⇒ 按机器数原本 16 台、现在 **20 台**（§108.2） | `Loader_MultiTileEntities:1979-1998` 20 行注册 vs `GTSensors.registerAll` 的 20 个 `add(...)` |
| 世界生成"地牢、树（含彩虹木）、遗迹缺" | **三类都在**：树 13 种（`GTTreeShapes` 含 `rainbowood`/`rubber`/`willow`/`maple`/`hazel`/`cinnamon`/`coconut`/`blueMahoe`/`blueSpruce`）、倒木/树枝/发光菌（`GTSurfaceFlora` 复刻 `WorldgenLog*`/`WorldgenSticks`/`WorldgenGlowtus`，`SurfaceFloraTests` 覆盖）、地牢（§76/§79 已做完并记档） | `GTTreeShapes:36-52`、`GTSurfaceFloraFeature:57,165-182`、`SurfaceFloraTests:162-226` |

**顺带记档**：§3 的"世界生成 87 个文件"里包含 `build/sources/java` 的重复计数与其它维度的生成器（Aether/Alfheim/Erebus/Mars/Moon/planets，移植版没有那些维度）⇒ 已按"约 57 个类"重写，并把"真正没有对应物的只有哪几个"写明。

### 108.5 GT6 食品线（代理 W2）：**物品缺口本来就是 0**，真正缺的是**数值**

§103.C 把"GT6 农作物/食品线"写成"一批要做的新物品"（§92 还记了 47 条"移植版没有该物品"）。W2 先把清单核了一遍，结论把这条**反转**了：

| 项 | 数 | 依据 |
|---|---|---|
| `MultiItemFood.java` 的 `addItem(...)` 行 | **267** | `MultiItemFood.java:53-939` |
| 真实食品物品 | **266** | 与 `GTMultiItemsGen` 的 `food` 类目做 1:1 校验：**266/266 一致、0 处不符** |
| **移植版缺的物品** | **0** | 生成器逐行比对，不一致就 FATAL |
| 不适用 | 8 | 1 条 GT6 自己的隐藏 ID 迁移桩（`:468`）+ 7 条 `ST.make(Items.x)` 原版别名行（胡萝卜/土豆/烤土豆/毒土豆/苹果/面包/蛋） |
| 带 `FoodStat` | **207**（其中 181 条六项统计非零） | `MultiItemRandom.java:159-160`：只有带 FoodStat 的行才 `setFoodBehavior` |
| GT6 里**不可食用**的展示件 | **59** | 同上 |
| 退空容器 | 9 | 全部解析成功（苹果核/木棍/纸屑×16/铝屑×2，`FoodStat.java:146-151`） |

**§92 那 47 条 SKIPPED 的原因写错了**：不是"移植版没有那些物品"，而是 **GT6 自己从不把自家物品放进 `FoodsGT`**——`RM.java:776` 明确排除 `MultiItemRandom`，而 `Loader_Recipes_Food` 的每个监听体都带 `!ST.isGT` 守卫（只为**别的模组**的物品登记）。所以"把物品补上"根本解析不了那些行；正解是走 **`FoodStat` 路径**：`GTFoodStats` 的行数 **20 → 201**（20 条原版 + 181 条 GT6 自家食品），47 条 SKIPPED 里只留真做不到的（9 条指向移植版真有的 GT6 物品——其中 4 条数值与 FoodStat 完全相同，已随 181 行进表——其余 38 条是其它模组的物品/作物），**每条的缘由都改写成 GT6 自身的依据**。

**§103.C 原文期待的"焦糖/太妃糖/咖啡/茶/20 种坚果"：GT6 自己一个都没注册**（`foodCaramel`/`foodTaffy` 是 HarvestCraft 的，`cropCoffee`/`cropTea`/`cropPistachio` 等是其它模组作物，且 `Loader_Recipes_Food`/`Crops` 里全带 `!ST.isGT` 守卫）⇒ **造它们等于凭空发明内容**（作物还要方块），记档不造。

**落地**：新 `content/food/GTFoodItems`（表访问 + `foodProperties`/`itemProperties` + 吃掉退空容器的事件）+ 新 `registry/GTFoodItems`（缺口注册表，今天 0 条并打印对账）+ 新 `content/recipe/FoodItemRecipes`（**只依赖移植版已有物**的四族：Furnace 25 行 / Slicer 10 行 / Mixer 30 行 / Boxinator-Unboxinator 9 行）+ `tools/extract_gt6_food_items.py`（新，解析 `MultiItemFood` 并与 `GTMultiItemsGen` 对账）+ `tools/generate_food_item_assets.py`（新：266/266 贴图与模型核对通过，**造贴图 0 张**；语言键新增 **400** 条 `item.gregtech.<id>.food`，中英各 200，幂等）。

**接线（6 处）**：`Loader_Submit` 提交 `GTFoodItems` 的 DeferredRegister、`Loader_Items` 调 `registerAll()`、`GregTech` Phase C 加 `FoodItemRecipes.register()`、**`GTMultiItems` 的食品属性改用 `GTFoodItems.itemProperties(id)`**（真正的行为修复：此前 **266 个食品共用一个占位食物值** 4/0.3）、`TechItem` 增加红色食物提示行（`FoodStat.java:198` 的样式，否则新加的 400 个键无人读）、`FoodStatsTests` 的 20 → **201**。

**测试**：新 `gametest/FoodItemTests` **7 项**（BASE 29000）：266/266 注册与缺口 0、食物值与 GT6 逐条对齐、六项统计对齐、吃掉后统计变化、退空容器（含背包塞满时掉落到世界）、配方可查（Slicer/Furnace/Mixer，并**断言骡肉没有熔炼行**）、SKIPPED 有意为之。

**记档未做**：①GT6 的 `FoodStat` 药水效果行（`FoodStat.java:157-159`，含 cure_all 的 21 个效果，`PotionsGT.ID_*` 在 1.20.1 无对应）；②ENVM 体温/水合（移植版没有 ENVM tracker，数值在表里但未使用）；③合成台配方行（移植版走数据包 JSON）；④GT6 的材料单位写法（`U4`/`U/100`/`L`）折算出的流体量；⑤`replicateOrganic`/`generify`/`food_can`/`packunpack` 通用机制；⑥**如实复现 GT6 自身的一个 bug**：`MultiItemFood.java:574` 把"骡肉"那一对写成了狗肉那一对 ⇒ **骡肉在 GT6 里没有熔炼行**，移植版照样没有，测试里断言"没有"并写明缘由。

### 108.6 GT6 物品行为层（代理 W3）：30 个行为里 **9 个本批落地**，其余 21 条逐条给了判据

§3 把"物品行为"写成"原版 30 个、移植版 3 个"。W3 把 30 个 `gregtech/items/behaviors/Behavior_*.java` 逐条读完并给出判据（不是数文件就下结论）：

| 分类 | 数量 | 明细 |
|---|---|---|
| **本批实现** | **9** | `Behavior_Duct_Tape`、`Behavior_Scanner`、`Behavior_Plunger_Fluid`、`Behavior_FlintAndTinder`、`Behavior_Lighter`、`Behavior_Spray_Foam_Hardener`、`_Foam_Remover`、`_Extinguisher`、`_Color_Remover` |
| 移植版**已有等价**（不重复实现） | 3 | 钥匙（`GTDungeonKeyItem` + `DungeonPortalBlockEntity`）、数据存储的 NBT 部分（`GTMaterialDataRecipes` 已读写 `gt.usb.data`/`gt.usb.tier`）、活塞的块侧（4 处既有 `purgeFluid`/PLUNGER 分支） |
| GT6 **自身**空实现或死代码 | 6 | `SensorKit`（`:42-52` 全是注释）、`Sonictron`（`GT6_Main:176` 被注释）、`Plunger_Item`（`:42-64` 全是注释）、`DataOrb`（全库无引用）、`Watering_Crops`（`INSTANCE` 无引用）、`Cropnalyzer`（无 IC2 时 `:69` 直接返回 0） |
| **依赖缺失**（记档） | 7 | 箭×2、枪、铲（蜜蜂，需 Forestry）、要素活塞（需 Thaumcraft）、两种染料喷雾（物品未注册）、安放炸药（需电钻） |
| 下批候选 | 5 | 建筑法杖、区块移除器、世界生成调试器、遥控器、HDD 16 槽 |

**为什么是这 9 条**：它们的物品在移植版**全部已注册**（`GTMultiItemsGen:63-145`），而移植版**恰好只注册了这 4 个喷雾族**（各带 `_2` 已用变体），且 C-Foam 湿/硬化方块与 `CFoamBlockEntity.dry()` 都在 ⇒ 属于"物品本体已有 + 逻辑自包含 + GameTest 可驱动"的最优子集；GT6 里 5 个喷雾类共用同一套 empty/used/full + `gt.remaining` 结构，一次做完最省。**所有消耗数值都取原版构造参数**：胶带 quality 0/1/2 = 10000/100000/10000000 次（`MultiItemRandomTools:559,568,577`）、打火石概率 9000/10000/5000/5500、打火机 Invar 100 次 / 铂 1000 次、喷雾 256×10 次。

**落地**：新包 `item/behavior/`（`ItemBehaviors` 统一入口 + 9 个行为类，纯函数 + `Outcome`/`Consumable`/`SprayEffect` 等小值类型；`useOn`/`useOnEntity` 派发器先按**注册表 id** 判定，未命中的 multi-item 直接 `PASS`，不分配对象）。**接线**：`GTToolItem.onItemUseFirst` 在坩埚铲分支之前加 PLUNGER 与 FLINT_AND_TINDER 两条（`Behavior_Plunger_Fluid:49-62`、`Behavior_FlintAndTinder:45-61`）、`GTToolItem.hurtEnemy` 加"打火石点苦力怕"分支（`:69-78`）、`GTMultiItems.MultiItem` 加 `onItemUseFirst` + `interactLivingEntity` 两个派发入口（对全部 1278 个 multi-item 安全）。

**测试**：新 `gametest/ItemBehaviorTests` **14 项**（BASE 33000），每项都有正例与反面用例：行为表 33 个 id 全部有注册物品且未认领的物品不算、派发器两向路由、扫描仪（无 BE 计费 0 / 段数×512 / level 10 与 100 两道门 / 爆抗阈值）、胶带（遮挡面不消耗且目标不被调用 / `full→used` 计数 / 最后一击整叠消耗）、活塞（1000 mB + 100 耐久 / 创造不掉耐久 / 耐久耗尽时**罐里的流体不动**）、打火石（成功点火 + 失败 roll 仍 acted 且扣耐久）、打火机（100→99 / 最后 1 点变空壳 / 火柴失败 roll 也消耗 / 成功但无处点火则保留 / 创造不烧燃料）、硬化喷（湿→硬化、已硬化再喷无效）、清除喷（硬化块→空气、石头无效）、灭火器（3 处火 −30 / 燃烧的苦力怕 −10 且熄灭 / <10 次不能区域喷射）、除色喷（染色玻璃/板/彩瓦各 −10、普通玻璃不变）。

**记档未做**：①**无实体层**——箭矢/枪/铲都依赖 GT6 的实体类，而移植版没有 `entity/**`（且 GT6 自己的箭矢早已由 `PrefixItemProjectile` 取代）；②无 IC2 / Thaumcraft / Forestry 三个模组的接口；③GT6 自身 6 段死代码（见上表）；④两个**已就位但当前无人实现**的方块 hook：`BehaviorDuctTape.Tapeable`（GT6 唯一实现者是移植版没有的质量存储）与 `BehaviorSprayExtinguisher.Extinguishable`（`SolidBurningBoxBlock.LIT` 是现成落点）。

### 108.7 本轮的三个教训（写进交接单）

1. **缺口表先看口径、再看数字**：§3 里这三行（传感器"20 vs 7"、世界生成"树（含彩虹木）/遗迹缺"、GUI"15 vs 10"）都是**数文件**数出来的；逐条核对后——两行是**过期结论**（树 13 种都在、手册/打印机/扫描仪都在），一行是**框架 vs 实现**（GT6 的 GUI 基类 vs 移植版的实现类）。**"原版有 N 个文件"不等于"移植版缺 N 个东西"**；按**机器/内容**逐项对齐才是能用来排期的口径。§108.4 已按此改掉三行。
2. **行为写好了不等于会跑：先看派发白名单**：W1 把压力阀与沥青的行为实现并测了，但 `CoverItems.portCoverId` 的名单里没有 `pressure_value`/`panel_asphalt` ⇒ 玩家能贴上去、`behavior()` 却返回 `null`、行为**永不执行**。**加一个行为时必须顺着"物品 → 行为 id → 宿主派发 → tick/交互入口"整条链走一遍**，任何一环没接上都是"看起来做了但玩不到"。
3. **给既有热路径接线前，先问"没有这个东西时会怎样"**：滤器谓词是照 GT6 的"有滤器时"写的——`itemFilterPermits(空 cover, …)` 的原语义是**拒绝**（空白名单拦一切）。直接接进 `insertItem`/`fill` 会把**每一台没装滤器的机器**的管道全掐断。接线前第一件事是把"无 cover / 非滤器"的默认值定成**放行**，并且**为这个默认值写一条断言**（`CoverBehaviorTests.theFilterCoversFilterThePipesOnTheirOwnFace` 的第一段就是它）。

### 108.8 第一次门禁红了 10 条：**全是测试侧**，以及由此得到的五条硬教训

本轮 638 项第一次跑出 **10 条红**（三个代理的测试 9 条 + 我的 1 条）。逐条核到 GT6 原文后的结论：**实现代码只有一处真错**（食品空容器的物品 id），其余 9 条都是**测试自己的判据写错**。这个比例本身就是教训——代理只能用 javac 自检，**测试从没跑过**。

| # | 红项 | 真因（对着 GT6 原文核过） | 修法 |
|---|---|---|---|
| 1 | `filterCoversOnALiveMachineDriveTheirPredicate` | `CoverFilterItem:63-66` 的软锤**只删滤器标签**，模式（`mVisuals`）保留 ⇒ 清空后的**黑名单放行一切** | 改成两半都断言（空黑名单放行 / 翻回白名单后拦截，`:118` 的两个边界） |
| 2 | `craftingCoverOpensAVanillaWorkbenchOnARealMachine` | 我新增的管道拦截测试复用了 `site(0,0)`，而 `placeMachine` 拿**坐标**当 `RecipeMap` 键 ⇒ 第二次构造抛 `Duplicate RecipeMap key` | 我的四项挪到 `site(104/108/112/116)`，并在代码里写明原因 |
| 3 | `extinguisherDousesFireAndEntities` | 三处火**两两相邻**：移除一格时 `FireBlock.neighborChanged` 立刻让相邻火自己消失（GT6 的 `setBlock(...,3)` 同样连坐）⇒ 实际只计费 1 格 | 铺 3×3 地板 + 三处**互不相邻**的火（对角），这才真正测到"每格 10 点" |
| 4 | `eatingReturnsTheEmptyContainer` | **真 bug**：`OP.scrapGt.mat(MT.Al, 2)` 被生成器拼成 `gregtech:scrap_gt_al`，而移植版命名是 `<prefix>_<材料名>`（`MaterialPrefix:643`）⇒ 应为 `scrap_gt_aluminium`。**而且 `ForgeRegistries.ITEMS.getValue(未知 id)` 返回 `Items.AIR` 而不是 null**，所以测试里 `assertTrue(item != null)` 是假绿 | 生成器加 `GT6_MATERIAL_NAMES` 映射并重新生成表；`FoodItemRecipes` 的 `foil_al`/`scrap_gt_al` 一并改成 `..._aluminium`；测试补 `!= Items.AIR` |
| 5 | `statisticsMatchGt6Declarations` | 测试把 `FoodStat` 的**食物等级**当成了酒精值：`MultiItemFood:298` 的 16 参构造是 `(等级, 饱和, 水合, 温度, 温度效应, 酒精, 咖啡因, 脱水, 糖, 脂肪, …)` ⇒ 腌黄瓜是 `(4,0,0,4,0)`，表是对的 | 改测试的期望值 |
| 6 | `aChestIsWeighedOnAllFourScales`（我的） | 我假设"4 个粗铁不到 1 吨"——但矿石载荷带的是**材料单位**，实际是四位数公斤 ⇒ 重型表读 4 吨 | 改成与量级无关的断言：四档读数**单调不增** |
| 7 | `pressureValveVentsAGasAndHeatsWhatStandsInFront` | 阀的伤害是 `getEntitiesOfClass` 查询，而**测试用 `setBlock` 碰过的区块对实体查询不可见**（`PersistentEntitySectionManager:47` / `EntitySectionStorage:55`，`PileBlockTests:643-651` 已记档）。跑起来的游戏里区块可见、铁傀儡会被打到，实现没错 | 保留"动作/清空罐"的断言，伤害用**阀调用的同一个公共函数 + 同一个温度 + 同样两个常量**断言，并把原因写进测试 javadoc |
| 8 | `scannerChargesPerSectionAndReportsTheMachine` | 测试把 `State: ON --- Running: y` 这**一行**当成一个计费段，而 GT6 是**逐段计费**（`:959` State、`:973-981` Running、`:988` 能量、`:1003` 罐）⇒ 4 段×512=2048 是对的 | 按段计（State/Running/Mode/能量/罐/进度各 1） |
| 9 | `scannerDescribesAPlainBlockForFree` | 测试以为石头要"1 级镐"，而 GT6 `WD.java:933` 打的是**原始** `getHarvestLevel` ⇒ 原版石头就是 0 | 改成 `Pickaxe (0)` 并写明 |
| 10 | `foodRecipesAreRegistered` | 测试用 `food("slicer_shape_split")` 取分裂刀——那是**科技物品催化剂**，不在食品表里 ⇒ 返回空栈 | 改成从 `GTTechnological.get(...)` 取 |

**由此固化五条硬教训（已写进交接单 §3）**：

1. **代理的 javac 自检过 ≠ 测试过**：本轮 10 条红里 9 条来自代理，且**全部是判据错**。给代理的 brief 必须写明"你写的测试由主代理跑第一次"，并要求**先读 GT6 原文再写期望值**（尤其是构造参数顺序、逐段计费、连坐行为）。
2. **`ForgeRegistries.*.getValue(未知 id)` 返回 AIR，不是 null**：`assertTrue(x != null)` 对注册表查询是**假绿**，必须 `x != null && x != Items.AIR`（本轮食品容器的红就是被它掩盖的）。
3. **测试里的实体查询在远坐标区块里看不见**：`setBlock` 碰过的区块实体段是 HIDDEN，`Level.getEntitiesOfClass` 会跳过（`PileBlockTests:643-651`）。要断言"被查询找到"就得用结构附近的坐标；否则改成**直接调用同一条公共函数**（本轮压力阀）或用确定性通道（`getDrops`/BE 状态）。
4. **`RecipeMap` 键撞车**：项目里 `placeMachine` 用**方块坐标**当 `RecipeMap` 键，两个测试用同一个 `site(...)` 就抛 `Duplicate RecipeMap key`。新增测试**先用 grep 看该类的 `site(` 占用**，再挑没被用过的偏移。
5. **原版的"连坐"行为会把测试前提吃掉**：相邻火焰互相清除、水/岩浆/流体会流动、`setBlock` 同状态是 no-op——写"逐格计费/逐格生效"类断言时，**先让被测对象互不相邻、互不影响**。

### 108.9 第二次门禁又红了 5 条：**三条是教训 3 的复现**，两条是环境相关的既有测试

修完 §108.8 的十条后重跑，仍然红了 5 条——但这一次**没有一条是新问题**，全部落在已记录的坑里：

| # | 红项 | 真因 | 修法 |
|---|---|---|---|
| 1 | `lightningRodStructureAndEnergy`（**既有测试**，与本轮改动无关） | `captureStrike()` 要求**杆顶见天**（`LightningRodControllerBlockEntity:36`）；测试结构自己的体积是空气，但**体积之上是真实地形**，这一轮的种子把那里变成了山顶 ⇒ 断言变成种子相关 | 断言前清掉杆顶以上 90 格内的非空气方块，并补 `rod != null` |
| 2 | `craftingCoverOpensAVanillaWorkbenchOnARealMachine` | GameTest 的 mock 玩家挂在**没有 channel 的 Connection** 上，`ServerPlayer#openMenu` → `Channel.pipeline()` NPE（`Connection.channel()` 返回 null） | 改用 Forge 的 `FakePlayerFactory.getMinecraft(level)`：它的 `FakePlayerNetHandler.send(...)` 是 no-op（`FakePlayer.java:136-137`），菜单能真的开起来 |
| 3 | `extinguisherDousesFireAndEntities`（实体半边） | **§108.8 教训 3 的复现**：实现内部用 `getEntitiesOfClass` 收实体，测试远区块里的苦力怕对查询不可见 ⇒ 计费 0 | 把"单个实体"的规则抽成公共 seam `BehaviorSprayExtinguisher.extinguishEntity(level, entity, player)`（区域扫描改为调用它，规则仍只有一份实现），测试直接驱动该 seam |
| 4 | `eatingReturnsTheEmptyContainer`（掉落半边） | 同一个坑：掉落的 `ItemEntity` 查不到 | 测试改用 **`ServerLevel.getAllEntities()`**（`ServerLevel:1358` 走整张实体表，不受 section 可见性过滤）——**这就是教训 3 的通用解法**：要么给实现加 seam，要么换确定性通道 |
| 5 | `foodRecipesAreRegistered`（Slicer 那一项） | 测试自己拼 `findRecipe(items, fluids, false, **2, 2**)`：那两个计数是"输入/输出**堆数**"，而 GT6 的 `addRecipe2` 是"1 个输入 + 1 个催化剂 → 1 堆数量为 2 的产物" ⇒ 计数猜错就查不到（同一条目的另外三个断言用的是 `mRecipeList` 扫描，所以都过了） | 改用同一个 helper（`slicerHas("bread","sliced_bread")`），并在注释里写明为什么不去猜计数 |

**新增第 6 条教训（已写进交接单）**：**门禁要跑到收敛，别指望一轮全绿**。本轮走了"10 红 → 修 → 5 红 → 修 → 2 红 → 修"三轮才把 638 项压到 0：代理新写的测试与既有测试的**种子相关性**会在不同轮次轮流暴露，所以每轮红的第一件事是**判断红项属于哪一类**（测试判据错 / 环境限制 / 真 bug / 种子相关），而不是直接从第一条开始改；`tools/show_gate_failures.py` 就是为"一次看全"写的。

### 108.10 第三次门禁剩 2 条：一条**更深的环境限制**，一条**真内容缺口**

| # | 红项 | 真因（比上一轮更深一层） | 修法 |
|---|---|---|---|
| 1 | `eatingReturnsTheEmptyContainer`（掉落半边，第二次） | 上一轮改用 `ServerLevel.getAllEntities()` 仍然空——因为**新加入的实体根本还没进实体表**：往测试区块里 `addFreshEntity` 会落到 `PersistentEntitySectionManager` 的**待定集合**（section 还没被 track），所以 `getEntitiesOfClass` 与 `getAllEntities()` **在同一个 tick 里都看不见它**（`PileBlockTests:643-651` 记的是"已入段但 HIDDEN"，这次是"还没入段"） | 把交付分支抽成公共 seam **`GTFoodItems.deliverContainer(player, stack)`**（返回"进了背包"还是"掉到地上"），`onItemUseFinish` 改为调用它；测试直接断言分支语义（满背包 → `false`，有空位 → `true` 且物品在背包里），不再去找那个 `ItemEntity` |
| 2 | `foodRecipesAreRegistered`（Slicer 那一项，第二次） | **真内容缺口**：GT6 的 `IL.Food_Bread` 是 **`ST.make(Items.bread, 1, 0)`——原版别名**（`MultiItemFood.java:715`），移植版的食品表把这类行记进 `SKIPPED`（"vanilla-aliased rows"）⇒ `food("bread")` 本来就是空栈，`{"bread", …}` 那一行**被静默 `continue` 跳过**（不是配方表的问题，是输入物品取错） | `FoodItemRecipes.SLICER` 支持 `minecraft:` 前缀的输入（表里改成 `minecraft:bread`，`slicing()` 遇到 `namespace:` 就走物品注册表而不是食品表），并**把跳过的行记进 `SKIPPED`**（原来这条路径是静默 `continue`）；测试改用 `Items.BREAD` 查 `mRecipeList` |

**结论**：三次门禁的红项总数 10 → 5 → 2 → 0，每次剩下的都是**更深一层**的原因（判据错 → 环境限制 → 环境限制的更深一档 + 真内容缺口）。这就是"跑到收敛"的实证。

### 108.11 第四次门禁跑到收敛：**638 项全过** + 收尾核验（门禁 / 注册量 / jar 三项对齐）

第四次跑（全新世界归档 `world-red108c-0918-0301` ⇒ 新世界、串行 GC、4G 堆）**0 红**：`========= 638 GAME TESTS COMPLETE ======================` 与 `All 638 required tests passed :)` 同现于 1634 行日志，`failed!` 计数 **0**。

| 指标 | 上一绿点（§107 收尾） | 本批 §108 | 差 |
|---|---|---|---|
| GameTest | 590 | **638 全过** | **+48** |
| 门禁轮次 | 1 | **4**（10 红 → 5 红 → 2 红 → 0） | — |
| 方块注册量 | 12,093 | **12,097** | +4（4 台新传感器） |
| 传感器机器 | 16 | **20** | +4 |
| 覆盖板行为 | 26 | **35** | +9 |
| 食品数值行 | 20 | **201** | +181 |
| 物品行为 | 0 | **9** | +9 |
| jar 条目 | 66,446 | **66,535** | +89 |

**+48 项测试的构成**（全部为本批新增类，无一条挂在旧类上）：

| 测试类 | 项数 | 覆盖 |
|---|---|---|
| `gametest/WorldFluidEffectTests` | 5 | GT6 `Loader_Blocks:137-153` 的沐浴/溺水/蛛网/沼泽四张表 |
| `gametest/SensorMatrixTests` | 4 | 20 台传感器的量纲、TPS 公式、`MAX_COUNT=65535` |
| `gametest/CoverBehaviorTests` | 18 | 9 个新覆盖板行为（过滤拦截、红石反相、`stepOn` 等） |
| `gametest/FoodItemTests` | 7 | 201 行数值表 + 4 族配方 + 空容器交付分支 |
| `gametest/ItemBehaviorTests` | 14 | 9 个物品行为（胶带/扫描仪/柱塞/打火石/打火机/四种喷雾） |

**收尾核验（`python tools\handoff_check.py`）**：

| 核验项 | 结果 |
|---|---|
| 门禁 | `All 638 required tests passed :)` |
| 注册量 | 70,758 items / **12,097 blocks** / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件 |
| jar | `build\libs\gregtech-1.0.0.jar` **30.6 MB（32,093,157 字节）**，sha256 `4CAF4378614978E1A67F1133A6EFB7D979EC8AFDF9C9A7F946EBFC176FC268B8`，**66,535** 条目 |
| `java newest < gate log` | **True** |
| `gate log < jar` | **True** |

**本批非测试侧的改动只有三处**：① `scrap_gt_al` 的注册 id 与 `MaterialPrefix:643` 的命名规则不一致（**真 bug**，门禁第 1 轮抓到）；② GT6 `IL.Food_Bread` 是原版别名 `ST.make(Items.bread,1,0)`（`MultiItemFood.java:715`），切削台的 `{"bread", …}` 行因此**被静默 `continue` 跳过**（**真内容缺口**，门禁第 3 轮抓到）；③ 为可测性新增两个**行为等价**的公共 seam（`BehaviorSprayExtinguisher.extinguishEntity` / `GTFoodItems.deliverContainer`），实现规则仍只有一份，区域扫描改为调用它们。其余 **15 条红项全是测试判据或环境限制**，一行实现代码都没改——这也是"先分类再动手"的直接收益。

**没有执行 `git commit`。**

---

## 109. 续批：GT6 的 20 台传感器终于有配方了（生成表 + 服务端数据包）+ 四个「生成得出、加载不了」的坑

### 109.1 缺口：§108 把机器补齐到 20 台，配方一条都没有

§108.2 把传感器从 16 台补到 GT6 的 20 台（并加了 `SensorMatrixTests` 钉住"20 台都在"），但这 20 台**一台都做不出来**：`BasicMachineRecipePack` 不覆盖传感器族，也没有任何数据包给它们配方。GT6 的原文在 `Loader_MultiTileEntities.sensors:1979-1998`——20 行 `aRegistry.add(...)`，每行带 3×3 图案与键表。

**回读原文得到两条容易漏的事实**（都影响移植）：

| 事实 | GT6 原文 | 移植版的处置 |
|---|---|---|
| 图案/键表**不是** `CR.shaped` 的调用者 | 20 行的图案与键表是 `aRegistry.add(...)` 的尾参；真正发配方的是 `MultiTileEntityRegistry:208` 的 `CR.shaped(getItem(mID), CR.DEF_REV_NCC, aRecipe)` | 移植版没有"多方块注册表物品"，每台传感器就是自己的方块 ⇒ **直接以方块物品为产物** |
| 每行末尾还有一条**无形状转换配方** | `CR.shapeless(aRegistry.getItem(), CR.DEF_NCC, new Object[]{aRegistry.getItem()})`（20 行都有） | 那条是"通用注册表物品 → 专属机器物品"的转换；移植版没有这一步，**不生成**（javadoc 里记档） |

**`allow_mirror` 的判据查到了根**：`CR.DEF_REV_NCC = BUF|NO_REM|REV|NO_COLLISION_CHECK`（`CR.java:161-167`），其中**不含 `MIR`**（`CR.java:131` 定义 `MIR` = 镜像）⇒ GT6 自己的配方**不允许镜像**，与多方块包写的是同一个 `allow_mirror = false`。（`REV` 是"熔炼/粉碎时反转输出"，与合成无关；交接单原稿把这一条写成 `CR.DEF_NCC`，实际是 `DEF_REV_NCC`，`MIR` 位同缺。）

### 109.2 抽取：`tools/extract_gt6_sensor_recipes.py`（30 条键映射，0 条未命中）

| 环节 | 数字 |
|---|---|
| 解析的 `aRegistry.add` 行 | **20**（机器 id 31000–31022） |
| 键表达式 → 移植版语法的映射 | **30** 条（逐条注释），未命中 **0** |
| 工具自检 | 每行图案里的**每个字符**都必须在键表里（GT6 反过来的松弛见 §109.5.1） |

映射的形态：`OP.<prefix>.dat(MT.<mat>) → mat:<prefix>@<移植版材料名>`、`OP.wireGt01.dat(X) → wire:1@X`、`OD.*`/`Items.*` → `item:minecraft:*`、`IL.*` 仪器 → `item:gregtech:*`、`IL.SENSORS[1] → il:SENSORS`。

### 109.3 实现：`data/SensorRecipePack.java`

与 `MultiblockRecipePack` 同构（`AbstractPackResources` + `@Mod.EventBusSubscriber(BUS=MOD)` 自注册 + `AddPackFindersEvent` + `MachineRecipeIngredients.resolveAll` + `"type": "gregtech:tool_shaped"`），四处刻意的翻译：

| # | 决定 | 依据 |
|---|---|---|
| 1 | 结果物 = **方块物品**（`ForgeRegistries.BLOCKS.getValue(id).asItem()`） | 移植版一台传感器一个方块 |
| 2 | `allow_mirror = false` | `CR.DEF_REV_NCC` 无 `MIR` 位（§109.1） |
| 3 | **tier = 1** | GT6 这 20 行以 `aUtilMetal` + `null` 材料注册、**没有 tier**；唯一带 tier 的键是 `IL.SENSORS[1]`（激光功率计中心，`:1998`）＝ **LV** ⇒ tier 1 让它解析成 `gregtech:compact_sensor_lv`，其余键与 tier 无关 |
| 4 | **丢掉图案用不到的键**（`usedSymbols(...)`） | 见 §109.5.1，**不丢就是 20 条全部加载失败** |

### 109.4 门禁：三次红 → 第四次（换新世界）全绿

| 轮次 | 结果 | 红项 | 真因 |
|---|---|---|---|
| 1 | 639 项 **1 红** | `everySensorHasItsOriginalGt6Recipe` | **测试判据错**：把"键解析成 `{item: …}`"当唯一形态，而 `materialIngredient` 对多数 GT6 形态返回的是**标签**（`{tag: "gregtech:plate_double/tin_alloy"}`、`{tag: "forge:bolts/tin_alloy"}`）⇒ 66 条误报。**同一份日志里另有 20 条 `Parsing error loading recipe`** |
| 1（同轮） | 日志 | 20 条配方**全部**解析失败 | **真坑**：`JsonSyntaxException: Key defines symbols that aren't used in pattern: [R, C, G]`（§109.5.1） |
| 2 | 639 项 **1 红** | `drawerRemovalDropsAllCompartments`（**既有测试**，与 §109 无关） | 同 tick 查询刚生成的 `ItemEntity`——日志里紧跟着 `Can't keep up! … 144 ticks behind`；改成"延迟 1 tick + 走 `getAllEntities()` 整张实体表" |
| 3 | 639 项 **1 红** | 同上 | 改完报 **`spilled 277 (expected 192)`** ⇒ 前两轮的掉落物**还在这张世界上**（§109.5.4） |
| 4 | **639 项全过** | — | 挪走 `build\gametest-run\world` 换新世界 + 该测试按 §108 的规矩**先清自己的体积**（`discardItems(...)`）再动作 |

### 109.5 四个坑（都值得记档）

**1）GT6 的键表里有"图案用不到的键" ⇒ 原版直接拒绝，20 条配方全灭。** 20 行**每行**都带 `G`/`B`/`C`，而图案里往往没有这三格（温度计是 `WRW`/`RXR`/`WPW`）。GT6 自己的配方处理器容忍多余的键，**原版不容忍**：`ShapedRecipe.dissolvePattern` 把图案里出现过的符号从集合里删掉，剩下的非空格就抛 `JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set)`（`ShapedRecipe.java:174-175`），而移植版的 `gregtech:tool_shaped` 序列化器直接委派给它。修法是 `SensorRecipePack.usedSymbols(pattern)` 过滤——**并且要在过滤之后**才 `resolveAll`，否则被丢掉的键还会产生无谓的替换日志。共丢掉 **58** 个多余的键（20 行合计）。

**2）Java 的**方法实参列表**不许尾逗号（数组初始化器可以）。** 生成器把 20 行 `",".join(...)` 之后又追加 `    );` ⇒ `javac` 报 `error: illegal start of expression`，**箭头指向那个右括号而不是逗号**；"逐行括号平衡"这种自查看不出问题（每行平衡、开闭总量也对）。改成 `",\n".join(row_texts)` 再追加 `);` 即可。

**3）`cmd /c` 有命令行长度上限：`javac -cp` 会报"命令行太长"。** 门禁的 minecraft classpath 有 **17 KB**，`cmd /c "javac -cp <它> …"` 直接失败（PowerShell 扛得住长命令行、但会撕碎 `-D`）。改用 javac 的 **`@argfile`**：选项与源文件按行写进文件再 `javac @file`，**没有长度上限**；唯一例外是 **`-J` 选项不能进 argfile**（报了 `无效的标记: -J-Duser.language=en`）⇒ `-J-Duser.language=en` 留在命令行。已封装成工具 **`tools/javac_selfcheck.py`**（不带参数＝查本轮这三个文件），它还会补上 `build.gradle` 里 `compileOnly` 的模组 API（Jade/JEI）——那些 jar **不在运行时 classpath 文件里**，否则会报 `cannot access IBlockComponentProvider: class file not found`（gradle 编得过、自检编不过的假红）。

**4）门禁命令自己不会换世界——复用世界会让你连红三次。** 交接单 §2 的门禁命令**只跑 gradle**，`build\gametest-run\world` 会一直复用；§108 那轮看到的 `archived -> world-red108c-…` 是**上一轮手工挪的**，不是 gradle 干的。于是"测试数从 638 变 639"后的三轮全跑在同一张世界上：第一轮红在既有测试（同 tick 实体查询）、第二轮换查询通道后**变成"看多了"**（把前几轮遗留的掉落物也算进来 ⇒ 277）。**结论**：①测试数一变、或上一轮有红，先 `Move-Item build\gametest-run\world build\gametest-run\world-<原因>-<时间>`；②断言"世界里有几个东西"的测试**先清自己的体积**；③`getEntitiesOfClass`（按已 track 的实体段过滤，看不见刚落地的）与 `getAllEntities()`（整张实体表，看得见但也会算上前几轮的遗留）是**两把刃**，正确做法是"先清场再动作"，而不是换查询。

### 109.6 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 639 required tests passed :)`**（638 → **639**，+1；0 条 `failed!`；日志 1436 行） |
| 传感器配方日志 | `[sensor] recipe rows=20 resolved=20 missing=0 emitted=20 unusedKeys=58` |
| 配方解析错误 | **0**（第 1 轮是 20） |
| 配方替换 | **0**（`MachineRecipeIngredients.substitutions()` 在本批前后无新增） |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / 49 缺失 token / 89 多方块部件（**均未变**——本批只加配方，不加注册项） |
| jar | `build\libs\gregtech-1.0.0.jar` **30.6 MB（32,106,480 字节）**，sha256 `152D5D74E005C208C3F42E6EF1D2227139C9ED28344AD45454D6B5ED61114F91`，**66,538** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**没有执行 `git commit`。**

---

## 110. 续批：物品行为层第二批——建筑法杖 + 两个调试法杖 + 遥控起爆器（§108.6 五个候选里落地 4 个）

### 110.1 选型：§108.6 留下的五个候选

§108.6 把 GT6 的 30 个 `Behavior_*.java` 逐条判完后留了 5 个"下批候选"：建筑法杖、区块移除器、世界生成调试器、遥控器、HDD16。本批做**前 4 个**，第 5 个记档不做（§110.4）。四个的**物品在移植版早已注册**：法杖是工具 `tool_builder_wand`（`GTToolType.BUILDER_WAND`，`GTToolItems:24`），另外三个是 `GTMultiItemsGen` 的 randomtools（`chunk_eraser:128`、`worldgen_debug_wand:129`、`remote_activator:124`）。

### 110.2 落地

| 文件 | 行数 | GT6 出处 | 语义要点 |
|---|---|---|---|
| `item/behavior/BehaviorBuilderWand.java` | 136 | `Behavior_Builderwand` | 沿**点击面的垂直平面**走 ±`REACH`，把与点击方块**完全相同**（block + meta）的每一格复制到它**上方**（`:123` 传 `SIDE_TOP`，而 GT6 的 `SIDE_TOP = 1` = 原版 `Direction.UP`，`CS.java:517`）；方块从背包按 GT6 的顺序取（1.7.10 的 `mainInventory` 从尾到头 ⇒ 1.20.1 的 `35 → 0`）；每放一块扣 1 耐久、创造模式不扣 |
| `item/behavior/BehaviorChunkEraser.java` | 138 | `Behavior_Chunk_Remover` | 整区块 **y=1..249 清成空气、保留 y=0**（`:41` 的 `tY = 1; tY < 250`，`:48` 的 "except for the very Bottom"）；`erase(level, cx, cz, minY, maxY)` 是给测试用的区间缝 |
| `item/behavior/BehaviorWorldgenDebugger.java` | 153 | `Behavior_Worldgen_Debugger` | 同一个区块扫描，但**保留 GT 矿石**（`:47-49` 的 `IPrefixBlock` 分支）——这正是"把矿脉看清"的工具 |
| `item/behavior/BehaviorRemote.java` | 268 | `Behavior_Remote` | 潜行右键**绑定**（每维度最多 64 个，`:56-58`）、右键**激活** 128 格内的全部绑定（`:79-86`）；NBT 布局逐字段照抄（`gt.remote.dim.<维度>` + `c<i>/x<i>/y<i>/z<i>`） |
| `item/behavior/RemoteActivatable.java` | 55 | `ITileEntityRemoteActivateable` | 接口；移植版把坐标当参数传入（`remoteActivate(level, pos)`），因为方块自己知道位置 |

**接线 4 处**：① `ItemBehaviors.PORTED` +3 条、`useOn` +3 分支，并新增 **`useInAir(...)`**（GT6 的 `onItemRightClick` 槽）；② `GTMultiItems.MultiItem` 新增 `use(...)` → `useInAir`；③ `GTToolItem.onItemUseFirst` 增 `BUILDER_WAND` 分支（法杖是**工具**不是 multi-item，所以不走 ①）；④ `DynamiteBlock implements RemoteActivatable`——GT6 的 `ITileEntityRemoteActivateable` 只有两个实现者，炸药是其中之一（另一个是高级按钮，见 §110.4）。

### 110.3 又一例"接线前先问：没有这个东西时会怎样"（§108.7.3 的复现）

`MultiItem.use` 的新覆盖会让移植版**约 60 种 GT 食品不能再吃**：`Item.use` 的默认实现正是"可食用物品开始进食动画"的地方（`startUsingItem`），而我的第一版在没有行为认领时返回了 `pass`。修法是**没有行为认领时调 `super.use(...)`**，并为此写一条断言 `edibleMultiItemsStillStartEatingThroughUse`（`gregtech:lemon` 仍能进入进食状态）。这条在**门禁之前**（javac 通过之后的自查）就抓到了——§108.7.3 那条教训不是只对覆盖板成立。

### 110.4 记档不做 / 刻意的差异

| 项 | 处置与依据 |
|---|---|
| **HDD16**（`Behavior_DataStorage16`） | **不做**。它**只是提示文本**（列出 16 个数据槽里哪几个有文件），而移植版的数据存储是单文件语义（`GTMaterialDataRecipes` 的 `gt.usb.data`/`gt.usb.tier`）⇒ 先要有 16 文件存储 API，否则工具提示永远显示"16 槽全空"。先把存储侧做出来再谈显示 |
| **建筑法杖的 Thaumcraft 分支** | 不适用（`Behavior_Builderwand:49-79` 是给 `INode` 罩玻璃罩，移植版没有神秘时代） |
| **法杖的作用距离** | GT6 是 `主材料.mToolQuality + 1`（`:86`）；法杖头的默认材料是宝石族（`GT_Tool_Builderwand:52` 回退 `MT.Heliodor`），宝石族 `qual(3, …)`（`MT.java:210`）⇒ **4**（9×9 平面）。移植版是"一种工具一个无材料物品"（`GTToolItems:24`）⇒ 定成常量 `REACH = 4` 并记档 |
| **GT6 自身的一处不自洽** | 放置用硬编码 `SIDE_TOP`（= 上方），而权限复检用 `OFF[aSide]`（`:95`，点击面）。移植版按**生效的行为**（上方）实现，并在 javadoc 里写明这处矛盾 |
| **遥控器的第二个实现者** | 高级按钮（`MultiTileEntityButtonAdvanced:54`）没做：移植版的 `advanced_button` 是纯装饰方块、没有按压状态语义 |
| **`Behavior_Remote.addCoords`**（`:92-108`） | 没做：它唯一的调用者是 `Behavior_Place_Dynamite:77`（安放炸药时自动绑定），而那个行为依赖电钻、§108.6 已记档不移植 |
| **调试双杖不看建造权限** | 照 GT6：它们只判 `isRemote`（`Behavior_Chunk_Remover:40`、`Behavior_Worldgen_Debugger:44`），没有 `canPlayerEdit` |
| **炸药的引信** | 移植版的炸药是**立即引爆**（红石/打火石），所以 `remoteActivate` 也立即引爆并**像 GT6 一样返回 `false`**（`MultiTileEntityDynamite:193`）——那个返回值是"这个坐标还要不要留着"的契约（`:82`），不是成功标志 |
| **维度键** | GT6 用数值 `dimensionId`（`:113/:123/:133`）做 NBT 键；1.20.1 没有数值维度 id ⇒ 用 `gt.remote.dim.<namespace:path>`（`gt.remote.dim.minecraft:overworld`），布局其余部分逐字段一致 |

### 110.5 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 647 required tests passed :)`**（639 → **647**，**+8**；0 条 `failed!`；**一次就绿**） |
| 新测试 | `gametest/DebugToolBehaviorTests` **8 项**（BASE **55000**，独占一个区块——两个调试杖一次点掉 16×16×249，不能和别人共用区块）：平面复制 / 跳过被占位 / 背包没有同种方块 / 创造模式免费 / 区块擦除保留 y=0 且区间缝正确 / 世界生成杖保留矿石 / 遥控绑定+起爆+超距保留 / 食品仍能进食 |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（**均未变**——本批不加注册项） |
| jar | `build\libs\gregtech-1.0.0.jar` **30.6 MB（32,125,953 字节）**，sha256 `F678C7A981CE20E8CBA35F08C1B27B3DC0BE5CC4368051780B510304A398EF27`，**66,544** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**流程上的一条新经验**：这三个小类由并行任务写（冻结签名 + 文件白名单 + javac 自检），**并行任务反过来指出测试里有 3 处判据错**（① 填充 64 个坐标时把被点的那一个也填进去了 ⇒ 走的是"已绑定就移除"分支而不是"已满"分支；② `makeMockSurvivalPlayer()` 站在世界原点，距本站点 5.5 万格 ⇒ 128 格范围判定必然不成立，要 `player.moveTo(...)`；③ GT6 `:72` 的 `return T` 在四个分支**之后**，所以"加不进去"时它照样返回 `T`，我的 `assertFalse(acted())` 是错的）。**让实现者复查调用者的测试**比自审更有效——§108.8 那条"并行任务写的测试第一次跑几乎必红"这次被**提前**消化掉了，门禁一次就绿。

**没有执行 `git commit`。**

---

## 111. 续批：流体管道成为覆盖板宿主（GT6 的阀门/滤器本来就是贴在**管道**上的）

### 111.1 缺口与由来

§108 给压力阀与流体滤器接线时，把宿主放在了**机器面**上，并在 `CoverUtilityBehaviors` 的类注释里记下了这处替换（`:120-130`）：GT6 的 `CoverPressureValve:44` 拒绝**任何不是"单罐流体管道"的宿主**，`:51` 直接取 `((MultiTileEntityPipeFluid)te).mTanks[0]`。当时留下的 `valveCanAttachTo(tankCount, neighbourIsPipe)` 谓词注释里写着"**with a pipe-host caller in mind**"——本批就是那个 caller。

### 111.2 落地

| 文件 | 行数 | 做了什么 |
|---|---|---|
| `blockentity/machine/FluidPipeBlockEntity.java` | **1027**（原 629） | `implements PanelCoverHost`。`covers[6]` / `panels` / 面包装**全部惰性**，`hasCovers` 是 `tickServer` 唯一要测的那个布尔（**没有覆盖板的管道零额外开销**，管道是热路径）。`attachCover` 施加 GT6 的阀门宿主规则 `valveCanAttachTo(spec.tankCount(), neighbourIsFluidPipe(side))`；tick 派发阀门（送 `tanks[0]`，`CoverPressureValve:51`）、drain 与 air_vent（sink ＝ 管道自身：GT6 `MultiTileEntityPipeFluid:477` 对每个面都返回 `mTanks`，所以移植版的"面"不映射到罐号）；`getCapability` **只在该面确实挂着流体滤器时**才换成面感知包装；NBT 与机器同键 `gt_cover_<i>`；`clickFilterCover`/`configureFilterCover` 与机器同名同义（都转调 `CoverUtilityBehaviors` 的同一批静态方法） |
| `block/machine/FluidPipeBlock.java` | 334（原 303） | `use` 按机器的同一顺序接上 `PanelCoverInteraction.use` → 贴覆盖板 → 撬棍取回 → 螺丝刀/软锤配置滤器 → 滤器点击，**之后**才走原有的 `handleUse`（桶/扳手），无覆盖板且手上不是覆盖板时行为不变；`onRemove` 里把覆盖板掉出来（对应机器 `:1494-1499`） |
| `client/PipeCoverRenderer.java` | 92（**新**） | BER，逐面复用 `PanelCoverRenderer.renderFace` |
| `client/GregTechClient.java` | +1 行 | 给 `GTBlockEntities.FLUID_PIPE` 注册 BER——**一个 BE 类型覆盖全部管道**（`GTBlockEntities:145-151` 把 `GTFluidPipes` 的全部方块注册进同一个类型） |
| `content/cover/CoverUtilityBehaviors.java` | javadoc | 把"移植版的 `FluidPipeBlockEntity` 不是覆盖板宿主"这条旧结论改写成"管道宿主已存在"，并说明阀门现在按宿主取罐 |
| `gametest/PipeCoverTests.java` | 337（**新**） | **8 项**（BASE 59000，独占区块） |

### 111.3 补上的一个真语义缺口：管道间均压绕过滤器

并行任务的报告里诚实列了 9 处差异，第 4 条是**真的会漏**：管道的**管道间均压**走的是 `adjPipe.fill(...)` 直调，绕过了接收侧那一面；也就是说滤器只拦机器/外部能力查询，**拦不住邻居管道的均压**——而 GT6 的滤器恰恰是**面**上的拦截，等于"玩家装的滤器被自家均压绕过去"。

修法：在 `distribute` 的管道间分支里，先取 `directionTo(adjPipe)`，再问 `adjPipe.coverFluidFilterPermits(toTarget.getOpposite(), drained)`，不通过就 `continue`（机器侧本来就走接收方的 `getCapability`，已被同名方法过滤）。配套测试 `theFilterAlsoGatesThePipeToPipeFlow` **两头都钉**：滤器是熔岩时水不许过去、换成水之后同一对流必须通。

### 111.4 门禁第一轮：红的是我自己新写的测试，而且**负例因为错误的原因通过**

第一轮 655 项 **1 红**：`theFilterAlsoGatesThePipeToPipeFlow` 的**正例**红（"水现在应该能流过去"），而**负例**（"水不该流过去"）却是**通过**的。真因与滤器无关：**两根管道根本没连上**——`distribute` 会跳过连接属性为 false 的面（`FluidPipeBlockEntity:547` 的 `state.getValue(FluidPipeBlock.propFor(side))`），而我用 `setBlock(..., defaultBlockState())` 放的管子六面全是"未连接"，于是什么都没流，负例"顺利通过"。

**教训（已写进交接单）**：**负例断言在"什么都没发生"时也会通过**。凡是"X 不该流过 / 不该发生"的断言，都必须先钉住"**这条通路本身是通的**"（本批做法：加 `connect(...)` 帮助方法模拟放置时的连接，并加一条"两根管子确实互相连接"的前置断言）。这与 §108.8 的"先分类再动手"同族——红的第一件事是判断**这条断言有没有可能因为无关原因通过**。

### 111.5 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 655 required tests passed :)`**（647 → **655**，**+8**；0 条 `failed!`；两轮收敛：1 红 → 0） |
| 新测试 | `gametest/PipeCoverTests` **8 项**（BASE **59000**）：面级挂载/取下与派发 · 阀门的"单罐 + 不对着管子"规则 · 面级流体拦截（只挡自己那一面、别的面照旧放行） · **管道间均压也受滤器约束（正例+负例）** · 无覆盖板管道行为不变（fill/drain/tick） · 满罐气体被阀门放空 · drain 把前方的源方块抽进管道 · 打掉管道时覆盖板掉落 |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（**均未变**——本批不加注册项） |
| jar | `build\libs\gregtech-1.0.0.jar` **30.7 MB（32,143,417 字节）**，sha256 `76EC2C3342B68AB56F7B0AEFA0943D1392A149F29A04148B25DEA0163344A2B7`，**66,548** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**记档不做**：**物品管道**（`ItemPipeBlockEntity`）没有一起改——GT6 的物品管道同样能贴覆盖板，但移植版的物品管道是另一套流动实现，留给下一批；面板类覆盖板里需要"机器控制"的那几种（状态/进度/选择器/能量）在管道上是惰性的（管道没有 `MachineControl`），`attachCover` 会直接拒绝，而信号类（发射器/导体/控制器/遮板）照常工作。**刻意差异**：桶的 `handleUse` 不带面信息，所以用桶往管子里倒液体不走滤器；撬棍点在**没有覆盖板**的管道上会消耗这次点击（与机器同序）。

**没有执行 `git commit`。**

---

## 112. 续批：物品管道也成为覆盖板宿主 + 补上流体/物品两侧「发送面」的滤器闸门

### 112.1 落地

§111 只做了**流体**管道。本批把**物品**管道补齐（GT6 的物品滤器 `CoverFilterItem` 与物品检索器 `CoverRetrieverItem` 同样是贴在物品管道上的覆盖板），并补掉 §111 遗留的"只拦到达面、不拦出发面"那半个闸门。

| 文件 | 行数 | 做了什么 |
|---|---|---|
| `blockentity/machine/ItemPipeBlockEntity.java` | 632 → **1088** | 与流体管道同构的宿主：惰性 `covers`/`panels`/面包装 + `hasCovers`（**无覆盖板时 `tickServer` 只多读一个字段**）；`attachCover`/`removeCover`/`dropCovers`；**检索器**的 tick 派发（`CoverUtilityBehaviors.tickRetriever`，含机器那套"暂停—恢复"的 pending 语义）；`getCapability` **只在该面挂着物品滤器或检索器时**才换成面感知包装；`clickFilterCover`/`configureFilterCover`；NBT `gt_cover_<i>`；新增公开谓词 `coverPermitsItemTraffic(side, stack)` |
| `block/machine/ItemPipeBlock.java` | 282 → 319 | `use` 按与机器/流体管道相同的顺序接上覆盖板交互（`PanelCoverInteraction` → 贴板 → 撬棍 → 螺丝刀/软锤 → 滤器点击），**之后**才走原有交互；`onRemove` → `dropCovers()` |
| `client/ItemPipeCoverRenderer.java` | 91（**新**）+ `GregTechClient` 一行 | BER，复用 `PanelCoverRenderer.renderFace` |
| `blockentity/machine/FluidPipeBlockEntity.java` | 629 → **1045**（§111+§112） | §112 新增：`distribute` 的机器目标改为带面信息（`MachineTarget(Direction, IFluidHandler)` 记录），推送前问 `this.coverFluidFilterPermits(side, …)`；管道间均压同样加问自己那一面 |
| `gametest/ItemPipeCoverTests.java` | 380（**新**） | **6 项**（BASE 61000） |
| `gametest/PipeCoverTests.java` | 386 | 增 1 项（流体发送面，正+负），共 **9 项** |

**为什么两处都要问**：GT6 的滤器挂在**面**上，拦的是"从这个面通过"的东西——既有进来的、也有出去的。§111 只处理了接收侧（`distribute` 里问邻居那一面），于是"能拦住别人倒进来、拦不住自己送出去"。本批两侧都补齐，物品侧的两处出口（`pushItemsToPipe`、`tryPushToAdjacent`）都在**槽循环内**问 `coverPermitsItemTraffic(side, stack)`（白名单滤器因此仍能放过其它槽的物品）。

### 112.2 门禁四轮才绿：三次红都是**我自己新写的测试**，且每次都是"先证明通路是通的"救的场

| 轮 | 红项 | 真因 |
|---|---|---|
| 1 | `theFilterOnTheSendingFaceGatesOutgoingItems`（新） | **前置断言先响**：物品管道的 `inventory` **只有一个槽**（`NonNullList.withSize(1, …)`，而且 `insertItem` **忽略传入槽号**、只找第一个空或可叠加的槽），所以我"0 号放钻石、1 号放苹果"的第二句被**静默拒绝** ⇒ 管里只有钻石、滤器又是苹果 ⇒ 什么都不动 |
| 2 | 同上 | 改成分段后仍红在"苹果该走"：**GameTest 里方块实体不会自己 tick**（整套测试都手动 `serverTick`，全仓只有 5 处用 `runAfterDelay`），而物品管道**只在 `gameTime % 4 == 0` 才路由** ⇒ 新增 `pump(...)`：用 `runAfterDelay(1)` 递进真实 tick，只在 `%4` 命中时手动 tick |
| 3 | 同上 | 第 1 段过了（苹果走通——通路证明生效），第 2 段红在"给管里装钻石"：**单槽里还留着上一段没走完的苹果** ⇒ 显式 `extractItem` 清槽 + 断言清空（失败消息里带上残留量） |
| 4 | `AnvilSmashingTests.oreFormsSmashWithAHammer`（**既有**） | 与本批无关的既有 flake：`smash()` 每次在**同一坐标**放砧板且状态相同 ⇒ `setBlock` 同状态是 no-op（§107 记档的坑），**上一轮的方块实体连同工件留在里面** ⇒ 循环到第二个前缀（`oreRaw`）就查不到产物。修法就是 §107 的老方子：先 `removeBlock` 再放，并断言"砧板是新的且是空的" |
| 5 | **662 项全过** | — |

**三条教训**：①§111.4 那条"**先证明通路本身是通的**"在本批**救了三轮**——每次都是前置断言先响，而不是让业务断言给出误导消息；②**失败消息里带上被查对象的状态**（形如 `[0=4 diamond]`）让第三轮一眼定位；③"既有测试复用同一坐标 + `setBlock` 同状态 no-op"是一类**可复现**的 flake，遇到该按 §107 修，而不是靠重跑掩盖。

### 112.3 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 662 required tests passed :)`**（655 → **662**，**+7**；4 红 → 0：3 条是本批新测试、1 条是既有 flake） |
| 新测试 | `gametest/ItemPipeCoverTests` **6 项**（BASE **61000**）：面级挂载/取下与派发 · 面级物品拦截（只挡自己那一面） · 检索器两个方向都拒（插入与抽取） · **发送面闸门（三段式：无覆盖板时通路必须通 → 不匹配的滤器挡住 → 匹配的滤器放行）** · 无覆盖板管道行为不变 · 打掉管道时覆盖板掉落；`PipeCoverTests` 增 1 项（流体发送面） |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（**均未变**） |
| jar | `build\libs\gregtech-1.0.0.jar` **30.7 MB（32,163,482 字节）**，sha256 `DEB62E7212C86C35EB0229DE29E41CE015561FA575047F475BDA3FE8A78FE02F`，**66,553** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**顺手修的工具**：`tools/show_gate_failures.py` 在 GBK 控制台上会因为日志里的替换字符抛 `UnicodeEncodeError`（本轮第一次红就撞上，白丢一步）⇒ 输出改为 ASCII 转义。

**记档不做 / 刻意差异**：物品管道**没有**照搬 `CoverFilterItem:135-137` 的"放置期拒绝"（那会把"往管道上贴滤器"本身挡掉，与本批要验的路径冲突）；`dumpItemsToAdjacent` 与只读的 BFS 扫描不加闸（被拒的面本来就让推送失败，不会漏）；需要"机器控制"的面板类覆盖板在两种管道上都被 `attachCover` 拒绝（管道没有 `MachineControl`）。

**没有执行 `git commit`。**

---

## 113. 续批：多方块「材质等级 / 结构变体」缺口**逐条取证** —— §4 的整张清单是过期结论

### 113.1 起因：审计工具自己也有盲区

`tools/audit_multiblock_parts.py` 长期打印 "GT6 multiblock parts: 117, port covers 97, missing 20"。那 20 条**不是缺口**：它们全是**机器控制器**（17000 Coke Oven、17100–17114、17197–17199），而移植版的控制器是用**自己的 id** 注册的（`coke_oven_main`、`distillation_tower_main`、`largecentrifuge_stainless_steel`…），不走 `LargeMachineParts` 的 GT6 数字 id 表 ⇒ 工具"只认零件表"就必然把控制器全判为缺失——而 §4 的清单正是照这个口径写的。

### 113.2 取证结果：117 条**全部**有对应、0 缺失

| 家族 | §4 原表 | 实测（按 GT6 id 逐条） |
|---|---|---|
| 大坩埚（按材质） | 8 → **1**（陶瓷） | **8**（GT6 17302–17312） |
| 大储罐主阀 | 12 → **0** | **25**（wood + small/large × dense × 6 材料） |
| 大型线圈 | 6 → **1** | **6**（copper/nichrome/carborundum/osmium/iridium + niobium-titanium） |
| 锅炉气压计 | 5 → **0** | **5**（17201–17205） |
| 多方块墙壁 | 14+ → 14 个但**材质固定** | **23** 条按 id 落地（11 普通 + 9 Dense + 材质相邻项） |
| 发电机外壳 | 4 → **1** | **12**（蒸汽轮机 4 + 发电机 4 + 燃气轮机 4） |
| 其它零件 | — | 处理器 5、单元 6、粉碎轮 / 撕碎刀 / 钻头 等 |
| 机器控制器 | 41 名称 → **15 台** | **19 条全部有对应**（6 台 `*_main` + 13 台 `LargeMachineLayouts` 蓝图机） |

**§4 的口径错在哪**：只读了 `registry/GTMultiblocks`（15 个控制器），**没有读** `content/multiblock/LargeMachineParts`（按 GT6 数字 id 承载全部材质变体）与 `LargeMachineLayouts`（承载 13 台蓝图机）。这与 §108.7.1 记的"缺口表先看口径、再看数字"是同一族错误，而且是**同一份文档第二次犯**。§4 的表格已按本节结论重写。

### 113.3 本轮真正的实现改动只有一处

`LargeMachineParts.find(18041)` 原先返回 `null`：18041「Large Niobium-Titanium Coil」在移植版由 `GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL` 承载，但**没进 `find` 的 switch**。虽然没有代码引用它（不会立刻炸），但 `LargeMachineParts.block(id)` 对未知 id 是**抛异常**的——将来任何按 GT6 id 写的布局/配方一旦用到 18041 就会在运行期崩。已补 `case 18041 ->`，并在注释里标了 §113。

### 113.4 把"覆盖"做成**机器可查**

- 新 `tools/extract_gt6_multiblock_ids.py`：解析 GT6 `Loader_MultiTileEntities` 里 `"Multiblock Machines"` 的 117 行，按"零件表（`LargeMachineParts`）/ 19 条控制器映射"分类；`--write` 生成 `data/generated/GT6MultiblockIds.java`（117 行 + 两个计数 + `MISSING`），`--check` 校验幂等。旧的 `audit_multiblock_parts.py` 保留（它按零件口径看明细），新生成器是**权威**。
- 新 `gametest/MultiblockCoverageTests` **2 项**：
  1. **逐行走表**：PART 行必须 `LargeMachineParts.find(id) != null` 且方块已注册；CONTROLLER 行必须存在 `gregtech:<portId>` 或 `gregtech:<portId>_<材料>`（材质分级的机器正是这样命名的）；并断言 `MISSING == 0`、行数 == 117、分类计数与表一致。**日志实测**：`[multiblock] rows=117 parts=98 controllers=19 missing=0`。
  2. **遍历 13 套布局的 708 个单元**，收集全部零件 id 并逐个 `find`：任何布局里的 id 打错都会红，而不是等玩家搭到那一格才抛异常。**日志实测**：`layouts=13 cells=708 distinctPartIds=19`，19 个 id 全部可解析。

### 113.5 门禁两轮：红的是我自己写死的门槛

第 1 轮 1 红：`everyPartIdTheLayoutsUseResolves` 里那条"布局用的零件种类 ≥ 30"的门槛是**我猜的**（实测 19）。断言没抓到任何真问题，抓的是我的猜想。修法：门槛改成**低于实测值**（15）并把实测值写进注释——这样"布局表被清空"会红、正常数据不会。**教训**：给"覆盖度"写门槛时，门槛要用**实测值标定**并写进注释，否则它测的是猜想本身（与 §109.5.2"数字先量再写"同族）。

### 113.6 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 664 required tests passed :)`**（662 → **664**，+2；两轮收敛 1 红 → 0） |
| 新测试 | `gametest/MultiblockCoverageTests` **2 项**（BASE 不占区块：两项都只读注册表与布局表） |
| 覆盖日志 | `[multiblock] rows=117 parts=98 controllers=19 missing=0`、`layouts=13 cells=708 distinctPartIds=19` |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（**均未变**） |
| jar | `build\libs\gregtech-1.0.0.jar` **30.7 MB（32,172,259 字节）**，sha256 `0A363014C3B2AC8117A7A6A75B71281416CF1B01541F114DCC0C500AAAEE70C9`，**66,557** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**没有执行 `git commit`。**

---

## 114. 续批：GT6 的 USB 硬盘（16 文件存储）+ 两个数据存储提示行为 —— §108.6 的五个候选全部收口

### 114.1 GT6 的两种数据介质

| 介质 | 物品 | NBT 布局 | 提示行为 |
|---|---|---|---|
| USB **存储棒** | `usb1_stick`…`usb4_stick` | `gt.usb.data`（一份文件）+ `gt.usb.tier` | `Behavior_DataStorage`：详细形式 + `Data: USB N.0` |
| USB **硬盘** | `usb1_hdd`…`usb4_hdd` | `gt.usb.drive` 里 **16 个槽**：`gt.usb.data<i>` + `gt.usb.tier<i>`（`MultiTileEntityHDDSwitch:61-83`） | `Behavior_DataStorage16`：没写过 → "Perfectly Formatted"；写过又清空 → "Uncleanly Formatted"；否则 16 行（空槽 `Data Slot N is Empty`，有文件走**短形式**） |

§110.4 记档"HDD16 不做"的理由是**它只是提示文本**、而移植版只有单文件语义 ⇒ 先要有 16 文件存储 API。本批把缺的那一半补上了。

### 114.2 落地

| 文件 | 行数 | 做了什么 |
|---|---|---|
| `content/recipe/GTMaterialDataRecipes.java` | 339 → **474** | 新增硬盘 API：`NBT_USB_DRIVE`、`DRIVE_SLOTS = 16`、`slotDataKey`/`slotTierKey`（`gt.usb.data<i>` / `gt.usb.tier<i>`，逐字段照 GT6）、`driveTier`/`isDrive`、`driveSlot`/`scannedMaterialIn`/`firstUsedSlot`/`usedSlots`/`writeMaterialData`（写第一个空槽，满则 −1）、公开 `isReplicable`/`replicatorEnergy`；`scannedMaterial` 扩成"先看存储棒、再看硬盘里第一个该品种可读的槽"；**扫描器缓存键加上介质**（棒与盘对同一材料产出不同，原来会串），数据槽扩成 `isScannerMedium`＝棒或盘 |
| `item/behavior/BehaviorDataStorage.java` | 97（**新**） | 存储棒提示 + **共享数据行渲染器** `dataTooltip`（GT6 `UT.NBT.getDataToolTip` 的 `gt.replicator.data` 分支；`allDetails` 就是原版传入的 `T`/`F`） |
| `item/behavior/BehaviorDataStorage16.java` | 54（**新**） | 硬盘提示（三种状态 + 16 行） |
| `item/behavior/ItemBehaviors.java` | — | `PORTED` +8 条（4 棒 + 4 盘）；新增 **`tooltip(stack, lines)`** 入口——GT6 的 `getAdditionalToolTips` 槽，按 id 派发 |
| `item/TechItem.java` | — | `appendHoverText` 调 `ItemBehaviors.tooltip`（数据提示读的是栈上的 NBT，lang 键表达不了） |
| `tools/add_usb_tooltip_lang.py` | **新** | 12 个键 × 双语，幂等、插在排序位置 |

**三处刻意的决定（都写进 javadoc）**：

1. **硬盘怎么拿到数据**：GT6 靠 **HDD 交换机组**（`MultiTileEntityHDDSwitch` 的 16 个模式）在棒与盘之间拷文件，移植版没有这台机器 ⇒ 硬盘会"存在但永远拿不到数据"。本批让**分子扫描仪也接受硬盘**（`isScannerMedium`）并把文件写进第一个空槽——写出来的正是 GT6 自己的硬盘布局，逐字段一致。
2. **能量数字**：GT6 提示印 `(n+p)*65536` QU（那是它那台复制机的物质/反物质需求），移植版复制机收的是 `nucleons * REPLICATOR_EU_PER_NUCLEON` ⇒ 提示印**移植版自己收的数**，玩家读到的就是机器会扣的。
3. **一处 GT6 死代码记档**：`Behavior_DataStorage` 的 `else`（"This Stick is Empty"）在 1.7.10 里**永远到不了**（`getCompoundTag` 返回空 compound 而非 null，`tUSB != null` 恒真）⇒ 移植版对"从没写过的棒"什么都不印，与原版一致；该字符串仍提供为 lang 键，免得后人重新推导它为什么用不上。
4. **品种闸门**照 GT6 `MultiTileEntityHDDSwitch:61`：一个槽只在该槽文件的品种 `<=` 硬盘品种时可读——tier-1 硬盘读不到 tier-3 扫描仪写的文件，这正是四档硬盘存在的意义。

### 114.3 验证：一次就绿

| 项 | 值 |
|---|---|
| 门禁 | **`All 670 required tests passed :)`**（664 → **670**，**+6**；0 条 `failed!`；**一次就绿**） |
| 新测试 | `MaterialDataChainTests` +6（共 **10** 项）：16 槽往返（写入 → 逐槽读回 → 首槽/占用计数）· 装满后第 17 份被拒且前 16 份完好 · **品种闸门**（tier1 读不到 tier3 文件，tier3/tier4 能读）· 存储棒布局不受影响（没有 `gt.usb.drive`）· 扫描仪给硬盘写第 0 槽且棒路径不变（同时验证缓存键不再串）· 硬盘提示三种状态与 16 行（15 行空槽 + 1 行材料名；棒印品种行） |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（**均未变**） |
| jar | `build\libs\gregtech-1.0.0.jar` **30.7 MB（32,181,864 字节）**，sha256 `F182BE2B8ACC89B450383AF721B63DE6CA980D98C66B8F736AE2E2B06A595C6E`，**66,559** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**§108.6 的五个候选至此全部落地**：建筑法杖 / 区块移除器 / 世界生成调试器 / 遥控起爆器在 §110，HDD16 在本批。

**没有执行 `git commit`。**

---

## 115. 续批：让 GT 高速轨道真正跑出 GT6 的速度（移植版第一条**功能性** mixin）

### 115.1 关闭一条从 §68 记到现在的"刻意差异"

§68.3 第 1 条写的是：Tungstensteel / TungstenCarbide / Adamantium 的 **1.4 / 1.6 / 4.0** 被原版矿车自身上限 **1.2** 截断，"要突破得加 mixin/核心模组，本移植版没有"。§105 把 mixin 基建做出来了（MixinGradle 0.7.38 + refmap + `gregtech.mixins.json`），所以这条可以关。

**为什么非 mixin 不可**（1.20.1 源码逐行确认）：

- `AbstractMinecart.getMaxSpeedWithRail()` = `Math.min(railMaxSpeed, getCurrentCartSpeedCapOnRail())`（`AbstractMinecart:854-863`）⇒ 轨道自己的速度是对的，但被"矿车带着的上限"夹住；
- 而那个上限的 setter 是（`AbstractMinecart:844`）`currentSpeedCapOnRail = Math.min(value, getMaxCartSpeedOnRail())` ⇒ **任何赋值都被夹回矿车自己的天花板**，原版矿车是 1.2（`IForgeAbstractMinecart.getMaxCartSpeedOnRail()`，注释里还警告超过 1.1 会让矿车跑赢区块加载）。所以**Forge 的公开 API 走不通**，必须改那个 setter —— 正是 GT6 当年用 ASM（`Minecraft_RemoveCartSpeedCap`）干的事。

### 115.2 落地

| 文件 | 做了什么 |
|---|---|
| `mixin/AbstractMinecartSpeedCapMixin.java`（**新**） | `@Inject` 到 `setCurrentCartSpeedCapOnRail(F)V` 的 HEAD，把赋值变成**直接写入**（带 `value > 0` 的护栏），然后 `cancel`。作用域收得很窄：**原版轨道从不调这个 setter**，只有本移植版的 `TrackBlock` 会调，而它交出去的值就是轨道自己 `railSpeed(...)` 算出来的（直道才给满速、弯道 0.4、邻居区块没加载 1.0，`BlockBaseRail:277-289`） |
| `block/misc/TrackBlock.java` | 新增 `applySpeedToCart(...)`，三个轨道族（`Straight` / `Booster` / `Detector`）都在 `onMinecartPass` 里调用它 —— 原版每 tick 对矿车所在的那格轨道调一次（`AbstractMinecart:530`），所以这个值永远属于"脚下这条轨道"；类 javadoc 的 "Port difference" 段落改写为**已关闭** |
| `resources/gregtech.mixins.json` | `mixins` 数组加一条 |
| `gametest/TrackTests.java` | 原来的 `recipesAndTheVanillaCartCeiling` 把"1.2 天花板"当**特征**钉住（现在语义变了）⇒ 断言改为"新矿车仍是 1.2 默认"，并新增 `gtRailsHandTheirSpeedToTheCart` |

**新测试同时是 mixin 的守门测试**：断言"Adamantium 轨道把 4.00 交给了矿车"——没有 mixin 时 Forge 的 setter 会夹回 1.2，这条必红（`defaultRequire = 0` 意味着注入失败不会崩游戏，只能靠测试抓）。另外两条断言钉住"能升也能降"（换成 Tungstensteel 后降到 1.40）与"弯道仍是 0.4"。

### 115.3 两个坑（都写进交接单）

1. **mixinto Forge 自己增补的成员必须 `remap = false`**：`setCurrentCartSpeedCapOnRail` 与 `currentSpeedCapOnRail` 都在 `// Forge Start` 块里，是 **Forge 增补**、**不在 SRG 映射表**里 ⇒ 注解处理器直接**编译失败**：`Unable to locate obfuscation mapping for @Inject target setCurrentCartSpeedCapOnRail`（`@Shadow` 字段同理，只是降级为警告）。Forge 不重命名自己的增补，所以 dev 与生产同名：`@Inject(..., remap = false)` + `@Shadow(remap = false)` 即可。
2. **测试实体要先摆到被测方块上**（§110 记过的坑，本轮又踩一次）：`EntityType.MINECART.create(level)` 的矿车在世界原点，而 `getMaxSpeedWithRail()` 读的是**矿车脚下那一格**的方块 ⇒ 不 `moveTo(...)` 就必然读到空气、断言拿到 0.4。

### 115.4 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 671 required tests passed :)`**（670 → **671**，+1；0 条 `failed!`） |
| mixin 生效证据 | 门禁日志 `Mixing AbstractMinecartSpeedCapMixin from gregtech.mixins.json into net.minecraft.world.entity.vehicle.AbstractMinecart` ✓；jar 内含该类 ✓ |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（**均未变**） |
| jar | `build\libs\gregtech-1.0.0.jar` **30.7 MB（32,183,661 字节）**，sha256 `90660D18227AE587250AB7BDD05A98CBB1F7B6757CEDAECB0864DE43708A1068`，**66,560** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**已知噪声（记档，不改）**：`gregtech.mixins.json` 的 `compatibilityLevel: JAVA_16`（class 60）低于本工程的 class 61（Java 17），Mixin 会打一行 DEBUG 警告 `Class version 61 required is higher than the class version supported by the current version of Mixin (JAVA_16 supports class version 60)` —— §105 的 `WaterFluidMixin` 也一样，两条都正常注入（测试已证）。要消掉只需把 config 改成 `JAVA_17`，但没验证过该枚举值在 Mixin 0.8.5 里存在，故保持现状并记档。

**没有执行 `git commit`。**

---

## 116. 续批：错误报告-2026-9-18 的**崩溃定因**（Java 25 实验 flag 下的 C2 本地内存失败）+ 客户端资源洪泛的**三类根因**全量修复

反馈：内存一直稳在 3–4 G，却突然 `insufficient memory`。先定因（§116.1：**不是**模组、**不是**堆），再修模组侧真正存在的问题（§116.2–116.4：客户端资源错误洪泛），最后重出 jar（§116.5）。

### 116.1 崩溃定因：堆只用了 42%，爆的是 **C2 编译器的 arena**

证据全部出自 `错误报告-2026-9-18_12.02.59\hs_err_pid73332.log`：

| 证据 | 值 | 含义 |
|---|---|---|
| 失败分配 | `Native memory allocation (malloc) failed to allocate 16483608 bytes`，`Error detail: Chunk::new` | 申请 **15.7 MB** 的**本地内存**失败，不是 Java 对象分配 |
| 错误位置 | `Out of Memory Error (arena.cpp:186)` | HotSpot **arena**（编译器/临时内存池）耗尽 |
| 出错线程 | `C2 CompilerThread0`，正在编译 `ChunkSerializer::m_188230_` | 崩在**即时编译器**里，与游戏逻辑无关 |
| Java 堆 | reserved 9,437,184 K / committed 5,439,488 K / **used 4,100,152 K**（3.9 GB / 9 GB = **42 %**） | 堆**远未用满**，`-Xmx9216m` 不是瓶颈 |
| Metaspace | 189 MB | 正常 |
| 线程 | 65 个 Java 线程 | 正常 |
| JVM | **Java 25.0.4.1**，`-Xmx9216m -XX:+UseCompactObjectHeaders -XX:G1HeapRegionSize=32M -XX:G1NewRegionPercent=20 …` | 实验性 `UseCompactObjectHeaders` + 32M 大 region |
| 时机 | 会话第 **8 分 10 秒**，玩家 `/locate` 找到地牢（784,~,-608）后连续传送、猛加载区块 | 大量类首次加载 ⇒ C2 编译队列暴涨 ⇒ arena 连片申请 |

**结论**：这是一次**进程级本地内存（native）提交失败**，发生在 JVM 的 C2 编译 arena 里，不是堆溢出、也不是本模组某个分配点。实机看到"3–4 G 很稳"是对的——稳的是堆，被杀的是堆外。

**修法（启动参数，属整合包侧；`D:\Minecraft整合包\GT6移植测试\`）**：

| 动作 | 参数 | 理由 |
|---|---|---|
| 去掉实验 flag | 删 `-XX:+UseCompactObjectHeaders` | Java 25 上的实验特性，改对象头布局，编译器/本地内存路径最易踩 |
| 降堆 | `-Xmx6G`（甚至 5G） | 堆用不满 4G，留出本地内存空间；31 GB 宿主给 JVM 的提交上限反而因此更宽松 |
| 限代码缓存 | `-XX:ReservedCodeCacheSize=512m` | C2 编译产物与 arena 的主要本地内存消耗项 |
| 限编译线程 | `-XX:CICompilerCount=4` | 32 核默认会开很多编译线程，每个都有自己的 arena |
| 换 JVM | 优先 **Java 17 / 21**（本移植版目标就是 17） | 与 Forge 47.4.20 的受支持范围一致，无实验 flag |

### 116.2 模组侧确实存在"日志洪泛"（真因，两类）

`latest.log` 99 MB / **890,505 行**逐类统计（`build\tmp\scan_crash_errors.py`）：

| 错误类别 | 次数 | 来源 |
|---|---|---|
| `FileNotFoundException`（`gregtech:models/…`） | **59,192** | ✅ 已修（4 个树叶 iconset，见 §116.3 第 1 条） |
| `Exception loading blockstate definition` → `missing model for variant: 'gregtech:<id>#'` | **2,139** | ✅ 已修（2,068 个方块完全没有 blockstate 文件，见第 2 条） |
| `Unable to load model` | 0 | 本次会话没出现（是**克隆工具**留下的悬空引用，见 116.3 第 4 条） |
| `MalformedJsonException` / `JsonParseException` | 0 | — |
| `Using missing texture` | 1 | 单条，无洪泛 |

8 分钟会话里 6 万条异常、每条带完整栈（约 59 万行日志），既是**日志洪泛**也是启动/加载期大量分配与字符串拼接——它不一定直接造成 §116.1 的 native 失败，但确实让"加载期内存曲线"抖得厉害，属于该修的模组侧问题。

### 116.3 修复

| # | 问题 | 落地 | 量 |
|---|---|---|---|
| 1 | `generate_gt6_tree_assets.py` 为**每个**树种引用 `iconsets/leaves_<species>.json`，但只对 GT6 有美术资源的树种写了贴图 ⇒ 4 个模型 59,192 次 404 | 新 `tools/generate_missing_leaves_assets.py`：`blue_mahoe` 直接取 GT6 `LEAVES_BLUEMAHOE.png`；`pine`/`ebony`/`white_mahoe` 用 `LEAVES_RUBBER.png` 按 WoodSpecies 颜色 0xBB974D / 0x3A342E / 0x7993A6 做亮度重着色 | **8** 文件（4 模型 + 4 贴图） |
| 2 | **2,068 个已注册方块完全没有 blockstate**（`dust_*`/`raw_*`/`ingot_*`/`plate_*`/`gem_*`/`solid_*`/`casing_*`/`crate_gt64_*` 家族，材料来自其他模组的矿辞，生成器的材料表没覆盖） | 新 `tools/generate_missing_block_assets.py`：按家族克隆同族兄弟的三件套（blockstate / 模型 / 物品模型）。**2,014** 个克隆即成 | **6,023** 文件 |
| 3 | 余下 **54** 个 `LargeMachineParts` 部件（8 个坩埚、24 个罐阀、5 个气压计、4+4+4 个机壳、避雷针、基岩钻、Von da Graagg 发电机…）没有同族兄弟可克隆 | 扩展 `tools/restore_multiblock_appearance.py`：**直接从 `LargeMachineParts.java` 生成部件表**，贴图目录取 GT6 `Loader_MultiTileEntities.java` 的 `NBT_TEXTURE`（`crucible` / `tankmetal` / `largeboiler` / `largeturbine` / `gasturbine` / `largedynamo` / `lightningrod` / `bedrockdrill` / `vondagraagg`），缺失的 blockstate 按"匿名单变体"合成（这些部件是 `MultiblockPortBlock`，无 blockstate 属性） | blockstate **54** + 模型 **54** + 物品模型 **54** |
| 4 | 上面第 2 条的克隆工具**硬编码**了模型目录 `models/block/blocks/`，而这些方块的 blockstate 指向 `block/machine/multiblock/`、`.../fusion/`、`.../energy/` ⇒ 留下 **13 条悬空引用**（客户端会记 `Unable to load model`，直接画成 missing model 方块） | ① 根因修复 `generate_missing_block_assets.py`：**跟随模板自己的模型引用**，模板模型不存在就整块跳过并报告（永不留悬空引用）；② 13 个方块分别交给**家族专属生成器**（9 个墙/线圈 → `restore_multiblock_appearance.py`；3 个聚变部件 → `restore_fusion_assets.py`；`battery_box_ulv` → `generate_energy_node_assets.py`） | 13 方块 = 13 模型 + 若干 blockstate/物品模型修正 |
| 5 | `battery_box_ulv` 无资源：`EnergyNodeDefinitions.java:115` 后加的 ULV 电池盒不在 `TIERS`（lv..iv）循环里 | `generate_energy_node_assets.py` 显式补一条（GT6 电池盒覆盖 VN[0..9]，ULV 确实存在，锡合金外壳、8 V） | blockstate + 模型 + 物品模型 + 双语 lang |
| 6 | **64 个 JSON 带 UTF-8 BOM**（`models/item/burning_box_solid_*` / `smelting_crucible_*`）：BOM 不是空白字符，严格 JSON 读取器会拒 | `audit_missing_models.py --fix-bom` 清理，并把 BOM 纳入审计的失败项 | 64 文件 |
| 7 | 审计工具本身**两个 bug**导致长期"假绿" | ① `MODEL_DIRS` 用 `os.path.join` 拼出**反斜杠**，而 `rel` 被 `replace('\\','/')` 归一成正斜杠 ⇒ `rel.startswith(MODEL_DIRS)` **只匹配上 `blockstates/`**，`models/**` 的 4 万多个引用从未被检查；② 无命名空间的 `parent`（`block/block`、`item/generated`…）被当成 `gregtech:` 而不是 **`minecraft:`** ⇒ 一旦检查 `models/**` 就冒出 131 条误报 | 修两处 + 新增 BOM 检查与 `--fix-bom` |

### 116.4 五个坑（都值得记档）

1. **`os.path.join` 拼前缀 + 归一化后的相对路径 = 静默漏检**：审计"通过"不等于真的检查过。凡是用 `os.path.join` 拼路径前缀去 `startswith`，先确认被比较的字符串是哪种分隔符（本工程统一用 `/`）。
2. **JSON 里无命名空间的引用是 `minecraft:`，不是 `gregtech:`**——第二次误报（131 条 `block/block`、`item/generated`）就是它。
3. **克隆类生成器必须跟随模板"自己写的"模型引用**，不能假定目标目录（`block/blocks/` 只是材料家族的习惯，结构部件在 `machine/multiblock/`、机器在 `machine/<kind>/`）。要么跟随引用，要么整块跳过并报告——否则修一个错误会造出另一个错误。
4. **GT6 部件贴图目录分两类**：控制器用 `multiblockmains/<NBT_TEXTURE>`（`tankmetal`/`largeboiler`/`crucible`/`lightningrod`/`vondagraagg`…），结构件用 `multiblockparts/<folder>/<design>`（`metalwall/0`、`metalwalldense/0`、`coil/0`、`woodwall/0`）。同一工具里混用 → `FileNotFoundError` 立刻暴露（本轮踩到一次，`metalwalldense` 不在 `multiblockmains` 下）。另外**已存在的 blockstate 指向哪个目录，就是那个目录拥有这个部件**——生成器据此让位（`owned_directory()`），避免两个工具往两个目录写同一个部件。
5. **BOM 会活很久**：64 个文件的 BOM 在客户端日志里**没有任何报错**（这些物品模型当时并未被加载/引用），只有严格 JSON 读取器（我的审计）才发现。加一个"文件以 `EF BB BF` 开头就失败"的检查成本极低。

### 116.5 验证

| 项 | 值 |
|---|---|
| 模型/父模型引用审计 | `tools/audit_missing_models.py`：**49,036** 文件 / **65,135** 引用（修 bug 后覆盖 `models/**`），**0 缺失 / 0 BOM** |
| 部件资源覆盖 | `tools/restore_multiblock_appearance.py --check` → **OK**（**111** 个部件定义，其中 **74** 个来自 `LargeMachineParts`） |
| 方块资源覆盖 | `tools/generate_missing_block_assets.py --check` → **0 / 2,068** 仍缺 blockstate |
| 渲染资源审计（独立交叉验证） | `tools/check_render_resources.py` → **791** 文件，**0** 未解析声明 |
| 生成器幂等性 | 重跑 `restore_multiblock_appearance.py`：存量 111 个定义**逐字节不变**，仅 `wood_tank_main_valve` / `wood_wall` 按其 GT6 `NBT_TEXTURE`（`tankwood` / `woodwall`）从占位模型改为真贴图 |
| 门禁 | **`All 671 required tests passed :)`**（未变——本轮**只改资源、未改 Java**，`java newest < gate log` 仍 True） |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（均未变） |
| jar | `build\libs\gregtech-1.0.0.jar` **32.4 MB（34,003,117 字节）**，sha256 `59F2A0D7113F7844B271482528BB246A12388B36A1807DFBFA7E3ECD045FD278`，**72,823** 条目（66,560 → **+6,263**，全部是新增资源） |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**已知无害噪声**：`generate_energy_node_assets.py` 复制 GT6 的 `*.png.mcmeta` 时按 LF 写入，仓库里原来是 CRLF ⇒ 21 个 mcmeta 显示为 modified（**只有行尾差异**，内容与 GT6 源逐字节相同，MC 不读行尾）。

**没有执行 `git commit`。**

---

## 117. 续批：地牢储藏室终于堆上**真正的板条箱**（GT6 的 12 种 crate 多方块 → 移植版 6 个板条箱方块 + 五张材料表）

提示"现在有板条箱，可以直接在地牢中引用"——指的就是这一处：`GTDungeonChunkRoomStorage` 的类注释里一直写着 **"the port has no crates, so every stack is the port's plain shelving rack"**，四个象限全用**书架**顶替，材料表与尺寸 roll 一并丢弃。

### 117.1 缺口与已有的另一半

| 项 | 状态 |
|---|---|
| GT6 权威 | `DungeonChunkRoomStorage:135-183`（12 个 crate 条目 + 五张材料表 `:42-46` + 原始矿石方块装饰 `:179-182`） |
| 移植版板条箱 | **已有**：`MaterialPrefixes.java:644-690` 每个形态注册一个 crate 前缀；blockstates 实测 **2,911** 个（dust 984 / plate 802 / raw 539 / ingot 387 / gem 199）；`block_raw_*` **539** 个 |
| 地牢侧 | 之前用 `GTDungeonData.shelf(...)`（书架）顶替，且"材料表 + 尺寸 roll"全部丢掉 |

### 117.2 三处必须照抄的原文细节

1. **金属象限的 12 条目数组**（`:135`）：`{dust, dust, ingot×4, plate×2, 64dust, 64plate, 64ingot×2}` —— GT6 用它做**加权**抽取（dust 3/12、ingot 6/12、plate 3/12）。
2. **抽取顺序是"先方块、后材料"**：`DungeonData.java:209-210` 写成 `aBlocks[next(aBlocks.length)].placeBlock(..., aMaterials[next(aMaterials.length)].mID, ...)` —— Java **先求接收者**（方块数组下标）**再求实参**（材料下标）。顺序弄反，后面所有 roll 都会漂。
3. **尺寸 roll 必须照抽**：`next1in2`/`next1in3`/`next1in4`/`next1in6`/`next1in8` 即使移植版每种形态只有一个方块，也要消耗随机数（否则后续 roll 位置全变）。

### 117.3 落地

| 文件 | 做了什么 |
|---|---|
| `worldgen/dungeon/GTDungeonData.java` | 新增 `CrateForm`（6 形态 → `BlockMaterialPrefix`）、`crate(...)` 两个重载（对应 GT6 的 `set(IPrefixBlock[], …)` 与 `set(IPrefixBlock, …)`）、`rawBlock(...)`，以及 GT6 的五张材料表 `CRATE_DUSTS` / `CRATE_WOODS` / `CRATE_GEMS` / `CRATE_METALS` / `CRATE_ORES`（**176** 条，逐条用移植版符号门面 `GT6Materials` 写出，多重度照抄）；`pickBlock` 像 `rollPileItem` 一样**从 GT6 的随机下标开始走表**，材料缺该形态时顺延 |
| `worldgen/dungeon/GTDungeonChunkRoomStorage.java` | 五个象限全部改成真板条箱（金属象限走 12 条目数组，dust/gem/wood/raw 各自带尺寸 roll），raw 象限的 `blockRaw` 装饰与四处 `next1in4` 一并接回；类注释里那条"the port has no crates"改成已关闭 |
| `GTDungeonData.pile` | 显式传 GT6 的 `sMetals`（`CRATE_METALS`，与原来用的默认表是同一张），不再依赖默认 |
| `gametest/DungeonTests.java` | **+2**：`storageRoomPacksGtCrates`（24 个种子跑储藏室，断言 crate 体积里**再没有书架**、每个板条箱都是 GT6 六形态之一、且原始矿石方块确有出现）、`crateDrawsBlockBeforeMaterial`（在同一 `RandomSource` 种子上重放两次抽取，断言"方块先、材料后"——顺序反了会抽到**交换过的**一对） |

### 117.4 覆盖度（逐条核对，不是估计）

| GT6 表 | 形态 | 不重复材料 | 移植版有该板条箱 |
|---|---|---|---|
| `sDusts`（`:42`） | `dust` | 33 | **33/33** |
| `sWoods`（`:43`） | `plate` | 24 | **24/24** |
| `sGems`（`:44`） | `gem` | 25 | **25/25** |
| `sOres`（`:46`） | `raw` | 43 | **43/43** |
| `sMetals`（`:45`） | `ingot` | 7 | **7/7** |

⇒ GT6 摇到的**每一个**材料在移植版都有对应板条箱，不存在"摇到就什么都不放"的静默退化。

### 117.5 三个坑（写进交接单）

1. **板条箱前缀名是 `crateGt64*` 不是 `crateGt*`**：`MaterialPrefixes.java:644` 写成 `registerBlock(block("crateGt64Raw")…)`，所以 `BlockMaterialPrefix.getName()` 返回 `crateGt64Raw`。我第一版测试拿 GT6 的 `crateGtRaw` 去比 ⇒ 必红。**改法**：期望集合从 `CrateForm.values()` 现算，不写死字符串。
2. **测试里挑形态要挑"两种材料都有的形态"**：我第一版用 `PLATE_GEM` + 铜/锡，而 `plateGem` 只对宝石成立 ⇒ 50% 概率放不下、断言必红。改成 `INGOT`/`PLATE`（铜和锡两者都有）。
3. **`crateGtPlateGem` 是空家族**：前缀注册了但**没有任何材料**满足 `plateGem`（实测 blockstates 0 个）。`pickBlock` 走表遇空返回 `null`、`crate(...)` 返回 false ⇒ 只是"这一格没放东西"，不会崩；已记档。

### 117.6 顺手修掉的既有 flake（不是我引入的，但挡住了门禁）

`WorldFluidEffectTests.heavyOilsActLikeWebAndMediumOilDoesNot` 在新世界上红：`a cow walks through medium oil, moved 0.05`。

- **根因**：该测试类的流体位置是**绝对坐标**（`BASE_X/Y/Z = 35000/100/35000`），周围地形是**世界生成**出来的；这轮换了新世界（测试数变了必须换，见 §109 记档），牛东边多了一块实心方块 ⇒ `Entity.move` 被夹到贴面 0.05 格（牛半宽 0.45，正好 0.05 到方块面）⇒ 读成"几乎没动"。**这是 §111 那一类假阴性：通路本身从来没打开过。**
- **修法**：`step(...)` 在测量前**先清掉牛前进方向的两格**（dx 1..2、dy 0..1，不清它脚下那格流体），把"这条路是通的"变成前置条件。

### 117.7 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 673 required tests passed :)`**（671 → **673**，+2；0 条 `failed!`） |
| 新增测试 | `storageRoomPacksGtCrates`、`crateDrawsBlockBeforeMaterial`（`gametest/DungeonTests.java` 8 → 10 项） |
| 覆盖度 | 五张材料表 **176** 条，逐条核对**全部**有对应板条箱（§117.4） |
| jar | `build\libs\gregtech-1.0.0.jar` **32.4 MB（34,010,268 字节）**，sha256 `894C14F906300484D664EC82E98DE15144077E5E898672EB65CCAE5185F9BCBD`，**72,824** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token / **89** 多方块部件（均未变——只加方块用法，不加注册） |
| 未修的已知差异 | 储藏室的**四面流体罐墙**（GT6 `:65-132`）仍未移植（独立一批，见类注释）；尺寸 roll 抽了但移植版每形态只有一个方块，两种结果放同一个方块 |

**没有执行 `git commit`。**

---

## 118. 续批：储藏室的**四面流体罐墙**（GT6 `:65-132`）+ 基岩矿洞的**原矿柱**与**全局 RNG**——§117 交下来的"用已有方块就能关掉"的两处替换

§117 把储藏室的板条箱补齐后，同一个类注释里还剩一条"still skips all four walls"；§117 的覆盖度核对又暴露出另一处：基岩矿洞的原矿柱一直用**原版深板岩矿石**顶替（注释写的是"the port has no raw ore block"），而移植版其实**已有 539 个 `block_raw_*`**。两处一起收口。

### 118.1 储藏室的四面流体罐墙

GT6 `DungeonChunkRoomStorage:65-132` 是**四段几乎一模一样**的代码，每段对一个"没有邻居的那一面"做三件事：

| 步 | GT6 | 说明 |
|---|---|---|
| 1 | `if (aData.next1in2())` | 一半的墙保持裸岩 |
| 2 | `int tType = aData.next(tIDs.length)` | 三族容器之一：**32714 木桶 16000 mB / 32734 铁木桶 32000 mB / 32102 青铜鼓 64000 mB** |
| 3 | `if (next1in2())` → 0..3 个罐柱（`next2in3()` 提前收尾）；`else if (next1in2())` → 一个气压瓶（32055 丙烷 / 32056 氧气或氦气，8000 mB） | 每格要么一柱罐、要么一个瓶、要么空 |

四段的**循环下标在四段里都是 i 走 X、j 走 Z**（+X 墙 `set(12+i, …, 6+j)`，+Z 墙 `set(6+i, …, 12+j)`），所以移植版只写一份 `fluidWall(data, baseX, baseZ, countI, countJ)`，roll 顺序（i 外层、j 内层）照 GT6；四面墙的触发条件是 `mRoomLayout[…] == 0`，即移植版的 `!data.connected(dx, dz)`。

三族流体表（GT6 `:58-63`，共 **21** 条）逐条落到移植版的流体 id：

| 容器 | 容量 | 七种流体（移植版 id） |
|---|---|---|
| `wood_barrel_treated`（32714） | 16000 | `creosote` / `seedoil` / `lubricant` / `glue` / `latex` / `water` / `purpledrink` |
| `wood_barrel_ironwood`（32734） | 32000 | 同上，但水换成 `holywater` |
| `drum_bronze`（32102） | 64000 | `oil` ×2 / `soulsandoil` / `liquid_light_oil` / `liquid_medium_oil` / `liquid_heavy_oil` / `liquid_extra_heavy_oil` |

### 118.2 新工具：`GTDungeonData.select(...)`（GT6 全局 RNG 的替身）

GT6 的**罐内流体**不是从地牢流里抽的：`UT.Code.select(NF, tFluids[tType])` 内部是 `select(RNGSUS.nextInt(len), …)`，用的是 GT6 的**全局 RNG**（`RNGSUS`）。基线洞（`DungeonChunkRoomMiningBedrock:39`）的**矿脉材料**也是这条路径。

⇒ 移植版新增 `GTDungeonData.select(array, ax, ay, az)`：**从"自己的、按坐标播种的流"里抽**（与 `rollCoin` 同一套路、另加一个盐值区分），**一个数都不从 `data.random` 里拿**。这样"每个格子从地牢流里取了多少个数"与 GT6 一致，后面所有 roll 的位置才不漂。顺手把工坊里那个私有 `container(id)`（GT6 数字 id → 移植版方块）**提到 `GTDungeonData` 共用**。

### 118.3 基岩矿洞的两处收口

| 项 | 之前（注释记档的替换） | 现在 |
|---|---|---|
| 原矿柱（`:130-132`） | 用**原版深板岩矿石**顶替（红石/铁/绿宝石/青金石/金/铜…） | `BlocksGT.blockRaw` 的对应物 **`block_raw_<材料>`**（移植版 539 个），三个 `set` 全部走 `data.rawBlock(x, y, z, vein)`（新增**单材料**重载） |
| 矿脉材料抽取（`:39`） | `data.next(13)`（注释写着"因为 GT6 的 `UT.Code.select` 用全局 `RNGSUS`，而移植版只允许 `data.random`"） | `data.select(VEIN_MATERIALS, 0, 0, 0)`——**13 条材料的顺序与多重度照抄 GT6**（红石 ×2、硫、赤铁矿、软锰矿、磷灰石、辉钼矿、铝土矿、闪锌矿、黝铜矿、锡石、硅镁镍矿、方铅矿） |

### 118.4 坑

1. **`javac` 自查必须把"被改动的那个类"一起列进去**：这轮 `rawBlock(GTMaterial)` 新重载加在 `GTDungeonData` 上，而我只把调用方与另一个房间列进了 `tools/javac_selfcheck.py` ⇒ javac 从**过期的 `build/classes/java/main`** 里解析 `GTDungeonData`，报 `GTMaterial cannot be converted to GTMaterial[]`（看着像新代码写错了）。把 `GTDungeonData.java` 加进文件列表立刻通过——§109 记过的坑，这轮又踩一次。
2. **门禁在跑的时候不要改 Java 源码**：门禁用的是上一次 `compileJava` 的产物，改了源码就会让 `java newest < gate log` 变 False（`tools/handoff_check.py` 的判据）。本轮的文档/交接单都是在门禁跑的 12 分钟里写的（只动 Markdown/Python）。
3. 只丢两个细节：移植版的气压瓶**没有涂料通道**（GT6 给丙烷涂红、氧气浅蓝、氦气黄），GT6 的 `FL.lube` 在移植版叫 **`Lubricant`**（流体 id `lubricant`）。

### 118.5 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 676 required tests passed :)`**（673 → **676**，+3；0 条 `failed!`） |
| 新增测试 | `storageRoomBuildsFluidTankWalls`（三条墙带逐格检查：只允许 GT6 的五种容器、容量必须是 16000/32000/64000、罐内流体必须属于该容量那张表）、`tankFluidPickLeavesTheDungeonStreamAlone`（`select` 之后地牢流必须给出与未动过的一模一样的一串数）、`bedrockMineGrowsRawOrePillars`（原矿柱必须是 `blockRaw` 方块、材料必须在 GT6 的 13 种之内） |
| 覆盖度 | 三族 **21** 条流体全部落到移植版流体；13 条矿脉材料的 `block_raw_*` **12/12 种**（红石出现两次）齐备 |
| 测试修正 | 第一轮门禁 1 红，**红的是我自己新写的测试**：断言把流体拼成 `gregtech:<id>`，而移植版的 `Water` 是**原版水**（`minecraft:water`）⇒ 改成**比较流体对象**（`GTFluids.stack(field, 1).getFluid()` 的集合），不再假设命名空间 |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token（均未变——只加方块用法，不加注册） |
| jar | `build\libs\gregtech-1.0.0.jar` **32.4 MB（34,014,619 字节）**，sha256 `3714B0D2E5B79A7648C14ABFC53B59AE5DCECA587E2D937725362F1D7A337A49`，**72,823** 条目（−1：`VeinOre` 记录类随替换一起删掉） |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True**（`tools/handoff_check.py`） |

**没有执行 `git commit`。**

---

## 119. 续批：基岩矿洞的**金属脚手架**与**四个角落的炸药**——同一文件里最后两处"其实移植版早就有了"

§118 收口了基岩矿洞的原矿柱之后，类注释里还剩一条：脚手架"has no port equivalent"（四段分支**落到 air 写入**，格栅格子于是保留空房间的地板——石头而不是格栅），四个角落的炸药"are skipped as well"。

### 119.1 先查"到底有没有"（§117 的教训）

| GT6 | 移植版 | 结论 |
|---|---|---|
| 金属脚手架 `8408` 黄铜 / `8410` 钢（`MultiTileEntityScaffold`，四处竖井柱 + 格栅地板 + 两条走道，`:55,:57,:59,:61,:74,:80,:87,:89,:100,:107`） | **有**：`GTToolBlocks` 里的 **`scaffold`**（`ShapedToolBlock`），blockstate 正好是 GT6 的四个**水平**朝向 `facing=north/east/south/west` | 一一对应（`SIDE_X_POS`=东、`SIDE_X_NEG`=西、`SIDE_Z_POS`=南、`SIDE_Z_NEG`=北，与工坊 `HORIZONTAL` 表的映射一致） |
| 四个角落炸药 `32104` 爆竹 / `32713` 炸药 / `32712` 强力炸药（`:122-125`，全部 `NBT_FACING, SIDE_Y_POS`） | **有**：`GTToolBlocks.BOOMSTICK` / `DYNAMITE` / `DYNAMITE_STRONG`（`DynamiteBlock`，`DirectionalBlock`） | 三个都在，朝向可设成上 |

### 119.2 落地（坐标与朝向逐条对 GT6）

| 处 | GT6 | 移植版改动 |
|---|---|---|
| 竖井四根条件柱 | `:55/:57/:59/:61`，位置 (2,7)/(13,7)/(7,2)/(7,13)，条件是**那一侧通向另一个格子**，朝向 SIDE_X_POS/X_NEG/Z_POS/Z_NEG | 加进竖井 if 链——**优先级必须在砖墙之后、地板框之前**（GT6 就是 `else if` 顺序，挪位行为就变） |
| 格栅地板 | `:74/:80/:87/:89`，本地 y=0 的四条线上各放一个脚手架（朝向朝外） | 原来只写栏杆，现在补上 y=0 的脚手架 |
| 十字走道 | `:100/:107`，`SIDE_X_NEG` / `SIDE_Z_NEG` | 原来用**本地地板砖**顶替，现在换成脚手架 |
| 四个角落炸药 | `:122-125`，(6,6) 爆竹、(6,9)/(9,6) 炸药、(9,9) 强力炸药，全部朝上 | 新增四处写入 |

### 119.3 坑

1. **写入顺序**：GT6 把炸药放在**两次开凿之后**（`:112-116` 开 Y4、`:118-120` 开 Y3，然后才是 `:122-125`）。而 `:118-120` 的开凿**正好打开这四个角落格子** ⇒ 先写炸药会被自己抹掉。我第一版就把四处写入放在了两次开凿之间，**复查 GT6 行序时发现并挪到后面**（编译能过、门禁也未必报——因为"角落没有炸药"很可能只是测试没覆盖到的静默差异）。
2. **一条 roll 只体现一半**：GT6 的脚手架有黄铜/钢两种，由 `tBrass`（`:42`）决定；移植版只有一个 `scaffold` 方块 ⇒ 这条 roll 现在只体现在栏杆上（已写进类注释）。GT6 炸药还带 `NBT_MODE`，移植版炸药只有朝向 ⇒ 记档丢弃。
3. 走道原来用"本地地板砖"顶替是为了"有东西可站"；换成脚手架后**形状仍是 GT6 那个 16×2×16 的板**（`ShapedToolBlock:48` 的 `Block.box(0,14,0,16,16,16)`），所以玩家照样能站在上面。

### 119.4 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 677 required tests passed :)`**（676 → **677**，+1；0 条 `failed!`，**一次就绿**） |
| 新增测试 | `bedrockMineBuildsScaffoldAndExplosives`：16 个种子跑（四向连通的格子 ⇒ 四根柱与十字走道都会触发），逐点断言四个角落是 `DynamiteBlock` 且 **FACING == UP**，格栅地板（2,4)/(13,4)/(4,2)/(4,13) 与走道 (8,8) 是 `toolId()=="scaffold"` 的 `ShapedToolBlock` |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token（均未变） |
| jar | `build\libs\gregtech-1.0.0.jar` **32.4 MB（34,016,266 字节）**，sha256 `853D83F4668F19CF470810E2EA51A14149E8403F865CF395017CA47F821D9709`，**72,823** 条目 |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**没有执行 `git commit`。**

---

## 120. 续批：地牢农田房种上**GT 树苗** + 把剩下 19 条"移植版没有 X"**逐条核实**（哪些能接、哪些根本不该接）

§117/§119 都是靠"先查移植版到底有没有那个方块"捡到的，所以这轮把那句话**当审计跑了一遍**：`grep -n "port has no|no port block|no port equivalent" worldgen/dungeon/*.java` 命中 **19** 条，逐条对移植版的注册表核实。结论是 **1 条能接**（GT 树苗）、**8 条是"确实没有"**（记档别再花时间）、其余是"其它模组内容"或通用说明。

### 120.1 19 条声明逐条核实

| 声明（文件:行） | 核实 | 处置 |
|---|---|---|
| `FarmCrop:65` 移植版没有 GT 树苗 | **有**：`blockstates` 里 **30** 个 `sapling_*`（12 个树种 × 大/小/普通形态） | ✅ **本批收口** |
| `Barracks:43/157` Sky Stone 石堆（GT6 32074） | `PileBlock` 只有锭/板/宝石板三种形态，**没有"石堆"形态**；且 Sky Stone 是 AE2 内容 | 保持跳过（记档） |
| `Barracks:150`、`GTDungeonData:859` 杯子 | `*cup*` 的 14 个命中**全是材料名撞词**（`yttriumbariumcuprate` 之类） | 确实没有 ⇒ 保持（`cup()` 放花盆） |
| `Corridor3:89` 上锁容器 | `*case*`/`*strongbox*`/`*safe*` 均 **0** | 确实没有 ⇒ 保持（战利品进箱子） |
| `FarmCrop:40`、`FarmFish:23/74` Glowtus 发光植物 | 移植版有 17 个 `flower_*`，但**没有** GT6 那种会变色的发光植物 | 确实没有 ⇒ 记档 |
| `LibraryNormal:23` dungeon 配置 | 配置系统未移植 | 记档 |
| `LibraryNormal:26` GT 木半砖 | 移植版 **432** 个半砖**全是石头的**（`stone_*_slab`），木半砖 0 | 确实没有 ⇒ 保持云杉木半砖 |
| `LibraryNormal:37`、`FarmCrop:40` Hexxit/Thaumcraft | 其它模组 | 不适用 |
| `MiningBedrock:74` 黄铜栏杆 | 移植版栏杆 6 种（铁/钢/青铜/锻铁/不锈钢/钨钢），**没有黄铜** | 确实没有 ⇒ 保持青铜顶替 |
| `PortalNether:33/117` 火柴盒 | `*matchbox*` **0** | 确实没有 ⇒ 保持跳过 |
| `Workshop:108/499` 酒瓶箱（GT6 8752/8762） | `bottlecrate` **存在但只是 iconset 装饰方块**（`GTIconSetBlocks:194` 那批"每个 GT 物品一个装饰方块"），**不是**九槽展示容器 | 真容器确实没有 ⇒ 要做得**新建**（下一轮第 1 项） |

### 120.2 落地：农田房的树苗 —— 顺手挖出一个**真 bug**（四个园圃原本全是空的）

GT6 `DungeonChunkRoomFarmCrop:160-163` 每条是

```java
aData.set(14, 2, k, BlocksGT.Saplings_AB, aData.next(Saplings_AB.maxMeta()),
                      BlocksGT.Saplings_CD, aData.next(Saplings_CD.maxMeta()),
                      Blocks.sapling,      aData.next(6));
```

即 GT6 的**三块重载**：三个 meta **先求值**（从左到右），方法体里再摇一次 `next(3)` 挑块（`DungeonData:378-386`）⇒ **一个树苗花四次 roll**。移植版原来"只摇一个原版树苗"（一次 roll，白丢三次）。本批改成：

- 两族用移植版自己的树种（各 6 个：`SAPLINGS_AB` = rubber/blue_mahoe/cinnamon/hazel/maple + 原版橡树，`SAPLINGS_CD` = willow/rainbowood/pine/bluespruce/coconut + 原版桦树），六个原版树苗照旧；
- **四次 roll 与顺序照 GT6**（AB → CD → 原版 → 挑块）；
- 类注释那条"the port has no GT6 saplings"改成已关闭。

**然后测试红了，红的是个真 bug**：新测试扫四条园圃线，结果 `saw 0 GT, 0 vanilla`——**一个树苗都没有**。追下去发现：园圃的底座用的是 `GTDungeonData.pot(...)` = **原版花盆**（`Blocks.FLOWER_POT`），而 1.20.1 里花盆**不是**植物支撑面 ⇒ 写在它上面的树苗/甘蔗/花在**下一次方块更新就被弹掉**；GT6 的 `32065` 是**花槽**（一盆土），本来就能托住植物。也就是说**四个园圃一直是空的**，而且这不是本批引入的（旧代码同样种在花盆上）。修法三件：

| 处 | 改动 |
|---|---|
| 园圃底座（本地 y=1 的树苗底、y=4 的花/高植底、角上四个花槽） | 从 `data.pot(...)`（原版花盆）改成移植版自己的 **`gregtech:plant_pot`**（=GT6 的花槽），新增 `planter(...)` 助手（22 处） |
| 让植物真的活 | 新增数据包 **`data/minecraft/tags/blocks/dirt.json`**，把 `gregtech:plant_pot` 列进 `#minecraft:dirt`——原版树苗/甘蔗问的正是这个标签 |
| 仙人掌 | 原版仙人掌只认**沙子**（`CactusBlock.canSurvive` 直接比较 `Blocks.SAND`，任何方块标签都救不了）⇒ 两个仙人掌花槽按 GT6 坐标放**沙子**（`cactusPlanter`），其余三个花槽保持移植版花槽 |

（甘蔗仍然需要**相邻水**才活得下来——GT6 的花槽不需要，移植版这一点保持记档差异。）

### 120.3 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 678 required tests passed :)`**（677 → **678**，+1；0 条 `failed!`；第一轮 1 红 = 上面那个真 bug） |
| 新增测试 | `cropFarmPlantsGtSaplings`：24 个种子跑农田房，逐点扫四条园圃线（x=14/x=1 与 z=14/z=1，本地 y=2），断言每个树苗都是原版或 `gregtech:sapling_*` 且树种在移植版十二种之内，并断言 **GT 树苗与原生树苗都出现了**（旧实现只出原生 ⇒ 这条会红；花盆 bug 未修时 **0 个** ⇒ 也会红） |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token（均未变） |
| jar | `build\libs\gregtech-1.0.0.jar` **32.4 MB（34,018,447 字节）**，sha256 `91DA345B23CE470AF101AD9F0D470F3B396AD609AD922B4C80B890BD66717DAF`，**72,824** 条目（+1：新的 `data/minecraft/tags/blocks/dirt.json`，已在 jar 内核实） |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**没有执行 `git commit`。**

---

## 121. 续批：把"底座到底是不是植物支撑面"**全库排查一遍**（§120 交下来的第 2 项）——结论只有农田房中招，而且 §120 的修法顺带把花也救活了

§120 发现"园圃底座是原版花盆 ⇒ 植物全被弹掉"后，交接单第 2 项写的是"同类检查值得全库做一遍"。这轮把它做完。

### 121.1 排查口径与结果

`grep -n "data\.(flower|pot|cup|plant|sapling|crop)\w*\(" worldgen/dungeon/*.java` 的全部命中：

| 调用 | 命中 | 是否受影响 | 依据 |
|---|---|---|---|
| `cup(...)`（= `pot(...)`） | **15** 处：兵营 3、走廊 3 **4**、图书馆 **4**、普通图书馆 **5**（还有 1 处直接调 `pot`） | **不受影响** | 它放的是**原版花盆本身**（`Blocks.FLOWER_POT`），花盆是普通方块、不需要支撑面；§120 的病根是"把植物种在花盆**上面**"，与花盆自身无关 |
| `flower(...)` | **12** 处，全在农田房 | **已随 §120 修好** | 花写在本地 y=5、底座是 y=4 的那排花槽；§120 把底座从"原版花盆"换成 `gregtech:plant_pot` 并把它加进 `#minecraft:dirt` ⇒ 花与树苗一起活了 |
| 甘蔗 / 仙人掌（农田房四角） | 4 处 | 仙人掌 §120 已改沙子；**甘蔗仍需要相邻水**（GT6 的花槽不需要） ⇒ 记档差异 |

⇒ **只有农田房中招，且已经在 §120 修完**；其余房间的"杯子/花盆"是容器本身，不需要支撑面。

### 121.2 落地：把证明写进测试

`cropFarmPlantsGtSaplings` 原来只扫树苗，现在同一条四条线再扫**高一层**（本地 y=5）数**花**，并断言 `flowers > 0`——**这条断言就是"底座修好了"的判据**（修之前花也是 0）。

**小坑**：花的扫描要循环**外面**用坐标，而 `minX/minY/minZ` 是循环内声明的 ⇒ 编译报 3 个 `cannot find symbol`；改成循环里把最后一个被接受的格子记进 `scanX/scanY/scanZ` 即可（比把整段搬进循环里改动小）。

### 121.3 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 678 required tests passed :)`**（678，测试数不变——本轮是**加断言**而不是加测试；0 条 `failed!`） |
| 断言 | `cropFarmPlantsGtSaplings` 现在同时钉住"树苗里有 GT 树种"与"花也活下来了" |
| 注册量 | 70,758 items / 12,097 blocks / 1156 materials / 925 fluids / **49** 缺失 token（均未变） |
| jar | \uild\\libs\\gregtech-1.0.0.jar\ **32.4 MB（34,018,765 字节）**，sha256 \C6BCD6DF0C1FD4FF854BCBA72C6232DB5CBE56BC08603E06E1A65687DCFDAD7E\，**72,824** 条目 |
| 顺序不变量 | \java newest < gate log\ **True**、\gate log < jar\ **True** |

**没有执行 `git commit`。**

---

## 122. 续批：**九瓶箱**（GT6 的 `MultiTileEntityBottleCrate`）——地牢工坊最后一个"用别的东西顶替"的方块，这次是**真新建**

§117–§121 都是"其实移植版早就有了"；这一件相反：`bottlecrate` 虽然存在，但它只是 iconset 装饰方块（`GTIconSetBlocks:194` 那批"每个 GT 物品一个装饰方块"），**不是** GT6 那个九槽展示容器。所以这轮按 §117 的套路**新建**。

### 122.1 GT6 权威与移植版取舍

GT6 `tileentity/inventories/MultiTileEntityBottleCrate.java:54-70`：`short mDisplay[9]`——每格一个瓶子，解析成**瓶内流体**后在箱子里画成 3×3；方块是 `TileEntityBase09FacingSingle`（水平朝向），**不是 GUI 容器**，靠手右键放/取；箱子本身有调整过的碰撞箱。

| 方面 | GT6 | 移植版 |
|---|---|---|
| 存的是什么 | 瓶子解析出的**流体**（只用于显示） | 九个**瓶子物品**（移植版的瓶子本来就是物品、带自己的流体，见 §93） |
| 交互 | 手右键放入 / 取出 | 手拿瓶子右键 ⇒ 放进**第一个空格**；空手右键 ⇒ 取出**最后一瓶**（`firstFree()` / `lastFilled()`） |
| 破坏 | 掉落箱内物品 | `onRemove` 掉落九格内容（`contents()`） |
| 朝向 | `ALL_SIDES_HORIZONTAL[aData.next(4)]`（`CS:682` = 北/南/西/东） | 地牢照抄这条 roll，`HorizontalDirectionalBlock.FACING` 承载 |
| 世界内渲染 | 画 3×3 瓶子 | **不画**（记档差异：移植版不把九瓶渲染进箱子；箱子模型直接用移植版 iconset 的板条箱模型 `gregtech:block/iconsets/crate`） |

### 122.2 落地

| 文件 | 内容 |
|---|---|
| `block/inventory/BottleCrateBlock.java`（新） | `HorizontalDirectionalBlock implements EntityBlock`；`getStateForPlacement` 取玩家朝向的反面；`use` 是 GT6 的两半交互（放/取，创造模式不扣）；`isBottle(stack)` = 玻璃瓶或移植版的 `BottleItem`；`onRemove` 掉落内容；悬浮提示 |
| `blockentity/inventory/BottleCrateBlockEntity.java`（新） | `ItemStackHandler(9)`（`isItemValid` 只收瓶子）+ NBT（`gt_bottles`）+ 物品能力暴露 + `firstFree/lastFilled/insert/extractLast/contents` |
| `registry/GTBlockEntities.java` | 新增 `BOTTLE_CRATE` 方块实体类型 |
| `registry/GTStorage.java` | 新增 `bottle_crate` 方块 + 物品（`MapColor.WOOD` / `SoundType.WOOD`） |
| 资源 | `blockstates/bottle_crate.json`（四朝向）+ `models/block/inventory/bottle_crate.json`（复用 iconset 板条箱模型）+ `models/item/bottle_crate.json` + 中英双语方块名与提示（各 +2 键） |
| 配方 | `data/gregtech/recipes/bottle_crate.json`（8 木板 + 1 玻璃瓶 ⇒ 保住"无配方物品 = 0"的不变量） |
| 地牢 | `GTDungeonChunkRoomWorkshop.bottleCrate(...)` 从"抽屉顶替"改成放**移植版自己的九瓶箱**（朝向 roll 照 GT6；注释里那条旧替换记档改成已关闭） |

**小坑**：Forge 的 `ItemStackHandler` 钩子是 `protected void onContentsChanged(int slot)`（**带槽号**），少写参数会报"method does not override or implement a method from a supertype"。

### 122.3 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 679 required tests passed :)`**（678 → **679**，+1；0 条 `failed!`） |
| 新增测试 | `workshopPlacesItsBottleCrate`：放一个箱子并断言它就是 `BottleCrateBlockEntity`；连续塞 9 瓶全进、**第 10 瓶被拒**；空手取回 1 瓶后剩 8；破坏箱子后半径 4 格内掉落物**变多**（内容真的掉出来了） |
| 注册量 | items 70,758 → **70,759**、blocks 12,097 → **12,098**（新方块 + 新物品各 1；材料 1156 / 流体 925 / 缺失 token 49 / 部件 89 均未变） |
| jar | \gregtech-1.0.0.jar\ **32.5 MB（34,027,717 字节）**，sha256 \A27225F55B37B61558607E5B3D368DADAF287BC948A2EE4FBA4B8F80F057AE68\，**72,832** 条目（blockstate/模型/配方已在 jar 内核实） |
| 顺序不变量 | \java newest < gate log\ **True**、\gate log < jar\ **True** |

**没有执行 `git commit`。**

---

## 123. 续批：把"移植版没有 X"这套查法**从地牢扩到全库**（160 条声明逐条分类）——结论：**没有新的廉价收口点**，但把"别再找"的清单钉死了

§117/§119/§120 三轮都是靠"先查移植版到底有没有那个方块"捡到活的。这轮把同一句话当审计跑遍全库：

```
grep -rEi "port has no|no port (block|equivalent|version)|the port lacks" src/main/java
⇒ 70 个文件、160 条声明
```

### 123.1 逐条分类

| 类别 | 占比 | 例子 | 处置 |
|---|---|---|---|
| **其它模组内容**（不是移植版缺） | 最多 | Thaumcraft（`BehaviorBuilderWand:49`）、IC2（`BehaviorScanner:238`、`GTDamageTypes:38`）、Forestry 蜜蜂（`GTBumbleBeeRecipes:45`）、HBM/IE、AE2 | 不适用 ⇒ 保持记档 |
| **移植版没有的 API / 子系统** | 次多 | 矿石词典容器（`BasicMachineBlockEntity:1626`）、多方块大机器的分级（`BasicMachineDefinitions:84`）、管道图（`CoverUtilityBehaviors:265`）、蜜蜂伤害源（`BumbliaryBlockEntity:93`）、`FakePlayer` 物品点击（`BehaviorDuctTape:169`）、量化流体（`CoverAttachmentBehaviors:64`） | 要做得先补子系统 ⇒ 不是一批能收口的 |
| **材料形态族确实没有** | 少数但**看着最像廉价活** | `AnvilShreddingRecipes:44`（chunk / rubble / pebbles / clump / reduced / crystalline / cleanGravel / cluster）、`CrusherFamilyRecipes:34`（rawOreChunk / chunk / rubble / pebbles） | **逐条核实：这 9 个前缀在 `MaterialPrefix` 里连声明都没有**（全部 `declared=false`），物品模型 0 个（`pebbles_*`/`rubble_*`/`cluster_*` 均 0）⇒ 要加就是 **9 个形态族 × ~1000 材料** 的内容量，远不是"启用一行配方" |
| **翻译/记录性质的差异** | 少数 | 药水流体、Clay 家族、蜜蜂相关 | 保持 |

### 123.2 结论（写给下一轮）

- **地牢那批（§117–§122）是这类收口的高产期，现在它已经挖干了**：全库再扫一遍，没有新的"其实早就有了"。
- **剩下的大件是三件，都要补子系统而不是补一行**：①**物流核心**（覆盖板里 14 种绑在它上面）；②**GT6 迷你传送门的中继线**（`tileentity/portals` 20 个类）；③九瓶箱的**世界内 3×3 瓶子渲染**（GT6 会画，移植版只放方块模型；客户端 BER，门禁测不到 ⇒ 属"可见但不被门禁保护"的收尾）。
- **下一轮开始前值得先量的一条**：`tools/audit_recipe_sources.py` / `report_missing_item_prefixes.py` 能把"哪些 GT6 配方行因缺前缀被跳过"列全——若确认全部落在上面那 9 个族里，这条线就可以正式结案。

### 123.3 验证

| 项 | 值 |
|---|---|
| 本轮改动 | **只改文档**（无 Java 改动 ⇒ 不触发门禁重跑） |
| 门禁 | 仍是 **`All 679 required tests passed :)`**（2026-09-19 01:49:19）；`java newest < gate log` / `gate log < jar` 仍为 **True / True** |
| jar | `gregtech-1.0.0.jar` **32.5 MB（34,027,717 字节）**、**72,832** 条目、sha256 `A27225F55B37B61558607E5B3D368DADAF287BC948A2EE4FBA4B8F80F057AE68`（未变） |

**没有执行 `git commit`。**

---

## 124. 续批：**USB / HDD 交换机组**（GT6 多方块 `19000` / `19001`）——§114 记档"移植版没有那台机器"的那台，这次真建

§114 做出 16 槽硬盘时记档过一句：**GT6 靠 HDD 交换机组往盘里拷文件，移植版没有那台机器**，所以硬盘会"存在但拿不到数据"，当时让分子扫描仪往第一个空槽写来顶替。这轮把**机器本身**补上，§114 那半截缺口正式关闭。

### 124.1 怎么找到这一件的（口径与一个警告）

1. 跑已有的 `tools/census_gt6_registrations.py`：按 GT6 **自己的分组**列出 46 个类别（Basic Machines 251 / Multiblock Machines 117 / Fluid Containers 86 / Sensors 20 / Portals 19 / Computing 2 …）。
2. 再按"GT6 名字 → 移植版 id"对照，列出**没有对应 id** 的类别。
3. ⚠️ **这类快速匹配会高报，别当真缺口用**：它报 `Sensors 19 缺`、`Reactors 49 缺`、`Multiblock Machines 28 缺`，而这几个 §113 已经逐条证明**全都有**——移植版把它们注册成了带前缀/按材质展开的 id（`sensor_*`、`block_*`、控制器自己的名字），所以"名字不同"被当成了"没有"。这与 §113 记档的"只认一个来源 ⇒ 整体失真"是同族，**结论必须逐条回注册表核实**。
4. 逐条核实后**真正缺、而且小**的三小批：`Computing` 的 **USB Switch `19000` / HDD Switch `19001`**（§114 已记档）、`Long Distance Transport` 的两个端点（`10060`/`10061`）、`Quantum Energizers` 5 台。

### 124.2 落地

| 文件 | 内容 |
|---|---|
| `block/inventory/UsbSwitchBlock.java`（新） | 水平朝向；右键 = 把手中介质放进**源槽**（存储棒/硬盘）或**靶槽**（只收硬盘）并立刻拷贝；空手右键 = 先拷贝，没得拷就**把里面的一件取回**；破坏时掉落两格内容；提示键 |
| `blockentity/inventory/UsbSwitchBlockEntity.java`（新） | 两槽 `ItemStackHandler`（`isItemValid` 按介质判定）+ NBT `gt_switch` + 物品能力 + `copy()` + `status()` |
| `registry/GTBlockEntities.java` / `registry/GTStorage.java` | 新增 `usb_switch` 方块实体类型、方块与物品 |
| 资源 | 四朝向 blockstate + 模型（**复用移植版钢制机器外壳** `block/blocks/casing_machine_steel`）+ 物品模型 + 双语 4 键 |
| 配方 | `data/gregtech/recipes/usb_switch.json`（钢锭 + 红石 + 逻辑处理器组件） |
| 测试 | 新 `gametest/UsbSwitchTests`（**2 项**）：①存储棒里的文件被拷进空硬盘（且 `scannedMaterial(drive)` 从 `null` 变成 Iron）、同一个文件再拷一次**不变更**；②槽位只收介质（靶槽不接受存储棒、源槽不接受泥土）、**空存储棒拷不出东西** |

**语义取舍（记档）**：GT6 的交换机有 **16 个模式**（按硬盘槽逐个选）并在**通电**时每 tick 拷贝；移植版**一个方块覆盖两个 id**、只做"源的第一个已用文件 → 靶的第一个空槽"、**右键触发**（不需要电网，前期与地牢里都能用）。这正是 §114 需要的那一半：硬盘现在有办法拿到文件了。

**小坑**：配方第一版把处理器写成 `gregtech:processor_logic`（不存在），移植版的 id 是 **`logic_processor_unit`** —— 核对方式是**看物品模型文件是否存在**（`models/item/<id>.json`），比猜 id 快。

### 124.3 验证

| 项 | 值 |
|---|---|
| 门禁 | **`All 681 required tests passed :)`**（679 → **681**，+2） |
| 注册量 | items 70,759 → **70,760**、blocks 12,098 → **12,099**（`usb_switch` 方块 + 物品；材料 1156 / 流体 925 / 49 缺失 token / 89 部件未变） |
| jar | `gregtech-1.0.0.jar` **32.5 MB（34,038,423 字节）**，sha256 `44D2777830E372AD376298F7CE9C69339FD7536A51467116440689DB03032D47`，**72,840** 条目（blockstate/模型/配方已在 jar 内核实） |
| 顺序不变量 | `java newest < gate log` **True**、`gate log < jar` **True** |

**没有执行 `git commit`。**

---

## 125. 续批：**把 §124 的"三小批缺"逐条回注册表核实 ⇒ 两条是假阳性**（①量子充能器 ②长距离端点都已存在）+ **Crystal Chargers 判为 GT6 死代码** + 物流核心**结构层**落地

这一批的起点是 §124 结尾那句"真正缺的只有三小批（交换机、长距离端点 10060/10061、量子充能器 5 台）"。本批**逐条回注册表核实**（§113 的规矩：审计结论必须能被复核，不能只活在文档里），结论：**三小批里只有交换机是真缺（§124 已做完），另外两条早就实现了**。

### 125.1 核对一：量子充能器 T1–T5 **已实现**（§124 假阳性）

| 项 | 证据 |
|---|---|
| 注册 | `registry/GTLasers.java:88-92`：`quantum_energizer_ev/iv/luv/zpm/uv`（`LaserSpec.Kind.QUANTUM`, tier 1..5） |
| 数值 | `content/energy/LaserSpec.java:13-21`：入 **LU 32/128/512/2048/8192**、出 **QU 16/64/256/1024/4096**（与 GT6 `Loader_MultiTileEntities:962-966` 的 `NBT_INPUT/NBT_OUTPUT` 逐档一致）；`backInputOnly()` ⇒ 背面进 LU、正面出 QU |
| 语义 | `blockentity/energy/LaserConverterBlockEntity.java:43-52`：`output = energy*out/in`、`>= max(1, out/2)` 才发、**每 tick 清空输入缓冲**＝GT6 `TE_Behavior_Energy_Converter:92` 的 `NBT_WASTE_ENERGY=T` 分支 |
| 资产/配方 | **22** 份（blockstate 5 + `models/block/machine/quantum_energizer_*` 5 + `machine/energy/quantum_energizer{,_active}` 2 + item 模型 5 + `recipes/optical/quantum_energizer_*.json` 5）；双语 10 键 |
| 测试 | `gametest/OpticalEnergyRegressionTests.java`（4 项，含"光纤每个包只送一次""断纤停止传输""量子缓冲存读档"） |

### 125.2 核对二：长距离运输端点 **10060 / 10061 已实现**（§124 假阳性）

| 项 | 证据 |
|---|---|
| 端点 | `block/misc/LongDistEndpointBlock.java`(31) + `block/misc/LongDistEndpointBlockEntity.java`(**60**)：照 GT6 `MultiTileEntityLongDistancePipelineItem:130-178` 的"**源背面 → 管束（泛洪，同一方块同一 meta）→ 接收端正面**；接收端**背面**的库存被代理"，且要求**源唯一＝自己、接收端唯一**（`:56`），找不到目标就不代理（"disconnects immediately"） |
| 管/线 | `block/misc/LongDistPipeBlock.java`（item 管 / fluid 管 / 16 档长距离导线，带 `maximumVoltage`）、`LongDistanceTransformerBlock(.java)` + `LongDistanceTransformerBlockEntity.java`（5 档，`loss(1024)==128` 的 1/8 距离损耗） |
| 测试 | `gametest/LongDistancePowerTests.java`（5 项：损耗、线烧毁、支路、模拟不烧线、ULV/LV/…映射）+ `BlueprintRegressionTests.longDistanceItemPipelineDisconnectsImmediately` |

> **口径教训（第三次同族）**：按"GT6 名字 → 移植版 id"对照会高报——§113（多方块材质变体）、§124（Sensors/Reactors 报缺而实际全有）、本批（量子充能器 / 长距离端点）。**任何"缺"的结论落地前必须回注册表逐条核实**，并把核实方式写进文档。

### 125.3 新结论：Crystal Chargers（GT6 `10130-10139` + `10140-10149`，20 台）是 **GT6 死代码**，移植版没做是对的

移植版确实没有这 20 台（`grep crystal_charger` = 0）。逐条核实 GT6 原文后，结论是**照抄会做出永不工作的机器**：

| 步 | 事实（GT6 原文） |
|---|---|
| 1 | `Loader_MultiTileEntities:970-971`：10 台 `Crystal Charger (T0..T9)` + 10 台 `Large`，键里只有 `NBT_ENERGY_EMITTED = TD.Energy.LU`，**没有** `NBT_ENERGY_ACCEPTED` |
| 2 | `gregapi/tileentity/energy/TileEntityBase10EnergyBatBox.java:67-68`：只有 EMITTED 时 `mEnergyType = mEnergyTypeOut = LU` ⇒ 这台机器**只吃 LU** |
| 3 | 同文件 `:179`：`doInject` 第一句 `if (mReceivablePower <= 0) return 0;`；而 `mReceivablePower = mChargeableCount * mInput * 2`（`:153`） |
| 4 | `:132`：`mChargeableCount` 只数"**能注 LU 的物品**" |
| 5 | GT6 全库**没有任何物品存 LU**：`TD.Energy.LU` 只出现在 3 个文件（`Loader_MultiTileEntities`、`MultiTileEntityWireLaser`、`MultiTileEntityMagicFieldAbsorber`，全是机器/线）；物品侧能量统计全 EU —— `gregapi/item/multiitem/energy/EnergyStat.java:58-65` 的工厂，唯一外部调用点 `MultiItemTool.java:383`（`makeTool(TD.Energy.EU, …)`）与 `MultiItemRandomTools.java:517-518`（便携扫描仪/作物分析仪，EU）；`EnergyStat` 只有**单一** `mType`，没有"一个物品吃多种能量"的口子 |

⇒ `mChargeableCount ≡ 0` ⇒ `mReceivablePower ≡ 0` ⇒ 恒收不到能量 ⇒ `mActive` 恒假 ⇒ **机器完全不动**。写档为「不适用（GT6 死内容）」，与 §114"GT6 里的死代码要认出来，别照抄成活的"同族。

### 125.4 物流核心（③）现状盘点——**行为层是 0，物品层是全的**

| 层 | 现状 |
|---|---|
| 14 个物流覆盖板**物品** | **全在**：`registry/GTTechnological.java`（`:146`/`:373`，22 处 logistics）= generic / filtered × import / export / storage × item / fluid + `logistics_dump_bus_item` + 4 个 `logistics_display_cpu_*`；配方 `data/gregtech/recipes/logistics/*` **47** 份；资产 **36** 份；双语键齐全（tooltip 都写着 "For use with Logistics Cores and Wiring"） |
| 覆盖板**行为** | **0 个**：`content/cover/CoverItems.java` 的 `portCoverId(...)` 不含任何 logistics id ⇒ 能贴、**永不派发**（§108 踩过同一坑） |
| `logistics_core` 方块 | **占位**：`registry/GTLasers.java:101` = `reg("logistics_core", () -> new Block(qProps()))`（无 BE、无结构、**无配方**）；资产已在（`blockstates/logistics_core.json` + `models/block/machine/logistics_core.json`） |
| `logistics_wire` 方块 | 仅 **iconset 装饰块**（`registry/GTIconSetBlocks.java:281` 的 id 列表里），无 BE、无连接、无配方 |
| 结构零件 | **已全部注册且都是 `MultiblockPortBlock`**（可挂端口角色）：`content/multiblock/LargeMachineParts.java` — `18008 galvanized_steel_wall`、`18299 fusion_ventilation_unit`、`18200 versatile_processor_unit`、`18201 logic_processor_unit`、`18202 control_processor_unit`、`18203 storage_quadcore_processor_unit`、`18204 conversion_processor_unit` |

GT6 权威规格（本批逐行核过，写进代码注释）：结构 5×5×5 按**平方距离**分三类（`MultiTileEntityLogisticsCore:117-142`）：`d²<4` 内核 27 格（CPU 或墙）、`d²>6` **44** 格钢墙（**同时是进电面**，`:138`）、其余 **53** 格通风单元（54 面心减控制器自己一格，与 tooltip 的 "44 Walls / 53 Ventilation Units" 对得上）；CPU 计数 `:119-136`（`18200` versatile → 四表各 **+1**、`18201-18204` → 各自 **+4**、内核放 `18008` → **+0**）、`mCPU_Storage` 上限 **108**（`:64/:143`）、有效＝结构完整且**四表都 >0**（`:144`）；能量包 **256/512/1024**（`:695-697`）、超 max `explode(6)`（`:684`）、容量 `128+logic*256*conversion`（`:699`）、每操作门槛 `128+logic*64*conversion`（`:216`）、每轮固定开销 `20+logic+control+storage+conversion`（`:504`）；缓冲 108 罐 × 16000 L（`:70/:85`）+ 108 槽物品（`:711`）；网络＝从中心 5×5×5 取种子后按 **Chebyshev `<= control+2`**（`:437-443`）沿相邻 `ITileEntityLogistics` 泛洪（`:442`）；路由顺序 `:451-500`（Export↔Import → Storage → Defragmentation → Dump，一轮没搬动就把 logic 还回去 `:498`）。

### 125.5 本批落地：`LogisticsCoreStructure.java`（**157 行**，未接线）

| 内容 | 说明 |
|---|---|
| `cells()` | 124 格（125 减控制器自己），按上表平方距离分类；坐标用移植版既有约定 `(right, up, back)`（`back` 沿 `front.getOpposite()`，`back=0`＝控制器平面），与 `FusionStructure`/`LargeMachineLayouts.Cell.at` 同一套 |
| `contribution(Block)` | `18200`→(1,1,1,1)、`18201/2/3/4`→各自 +4、`18008`→ 特判（cheapstake 不计） |
| `counts(level, controller, front)` | 结构校验 + 计数 + `MAX_STORAGE_CPU_COUNT` 夹紧；任一格不匹配返回 `null` |
| `Counts` | `valid()`（四表都 >0）、`energyPerOperation()` = `128 + logic*64*conversion` |
| 经济学取值 | `operations/range/bufferTanks/throughput` 直接对应 GT6 那 4 行 tooltip |
| **未接线** | 没有注册、没有 BE、没有测试引用 ⇒ 对门禁惰性（本批只交付这一层） |

### 125.6 门禁那件事（新记档，省下一轮的十几分钟）

本批只加了一个**未被引用**的类，却跑出 3 条红：`DungeonTests.crateDrawsBlockBeforeMaterial`（"the crate was placed"）、`PipeCoverTests.aPipeDropsItsCoversWhenBroken`、`ItemPipeCoverTests.anItemPipeDropsItsCoversWhenBroken`（后两条都是 "found 2"）。同时日志里有 **`Can't keep up! … Running 11273ms or 225 ticks behind`**（客户端在跑，PID 55304 占 2.8 GB）。

- **判据**：这 3 条都在**复用世界**（`build/gametest-run/world`，`CreationTime` 2026-09-19 02:13:58，即 §124 那张）上跑；"found 2" 正是 §109 记过的"前几轮遗留的同名掉落物还在世界里"。未被引用的新类不可能影响它们。
- **处置**：`Move-Item build\gametest-run\world build\gametest-run\world-red125a-20260919-2045` ⇒ **新世界重跑**（不是改代码）。
- **推广**：**上次跑绿 ≠ 这张世界还能用**；只加类不接线也会让 `java newest < 门禁日志` 变 False，所以照样得重跑门禁才能收尾。

### 125.7 下一步（物流核心 W1→W4，详见仓库里的对接单）

| 波次 | 内容 |
|---|---|
| **W1** | `LogisticsCoreControllerBlock`（照 `CokeOvenControllerBlock` 骨架）+ `LogisticsCoreControllerBlockEntity`（`StructureController`、结构缓存 40 tick、EU 256/512/1024 + 容量/门槛/开销、NBT 用 GT6 键名 `gt.cpu.*`）；**替换 `GTLasers.java:101` 的占位**（id 仍是 `logistics_core`）；墙上进电走 `portEnergyTypes/injectPortEnergy`（照 `FusionReactorControllerBlockEntity:111-127`）；**配方走生成器**（`tools/gt6_multiblock_recipes.json` 加 `Logistics Core` 条目 + `generate_multiblock_recipe_table.py` 的 `PART_TARGETS` 加一行，GT6 的 `M` 是**机壳**不能按零件替换）；`gametest/LogisticsCoreTests.java` |
| **W2** | `api/logistics/LogisticsHost`（＝GT6 `ITileEntityLogistics.canLogistics(side)`）+ Chebyshev ≤ `control+2` 种子 + 泛洪扫描；把 `logistics_wire` 从 iconset 装饰块升级成真连接件（+ GT6 `:1819` 的配方 `PEP\|dFx\|POP`）；核心 108 罐 × 16000 L + 108 槽缓冲 |
| **W3** | 12 个实体物流覆盖板行为（GT6 `CoverLogisticsGeneric*` 37-41 行 / `CoverLogisticsItem*` 99-107 / `CoverLogisticsFluid*` 112）+ 核心路由循环（§125.4 的顺序表）+ **`CoverItems.portCoverId` 白名单**（否则贴上去永不派发） |
| **W4** | 4 个 CPU 显示覆盖板的值/视觉映射（GT6 核心 `:300-319` 的 `15 / 14-min(13, (total-used)*14/total)` 与 `10 / 9-min(8, (total-used)*9/total)`） |

> 完整对接单（含每条 `文件:行`、GT6 原文规格、下一步确切步骤、坑清单）：**`docs/HANDOFF_LOGISTICS_CORE_2026-09-19.md`**。

### 125.8 验证

| 项 | 值 |
|---|---|
| 本批 Java 改动 | **只新增 1 个类**（`content/logistics/LogisticsCoreStructure.java`，157 行，未接线）；既有 Java/资源/文档未改 |
| `javac` 自查 | `python tools/javac_selfcheck.py <该文件>` → **exit 0** |
| `compileJava` | **BUILD SUCCESSFUL**（45 warning 全是既有的 `ResourceLocation(String,String)` 等） |
| 门禁（复用世界，红） | 3 条**与本批无关**的红（见 §125.6）⇒ 世界挪走换新世界 |
| 门禁（新世界） | **`All 681 required tests passed :)`**（2026-09-19 20:55:16，0 条 `failed!`；测试数不变——本批只加了一个未被引用的类） |
| jar / 顺序不变量 | `gregtech-1.0.0.jar` **32.5 MB（34,045,205 字节）**、**72,845** 条目（72,840 → 72,845，+5＝新类的 `LogisticsCoreStructure`/`$Kind`/`$Cell`/`$Counts` 等 class 文件）、sha256 `16661AFA3C3C04847C296B49E2D9E4D334E13464A691A8407F853931B8A5C79B`；`java newest < gate log` **True**、`gate log < jar` **True**（`tools/handoff_check.py`） |
| 注册量 | **不变**（本批没有注册任何东西）：items **70,760** / blocks **12,099** / 材料 1156 / 流体 925 / 缺失 token 49 / 多方块部件 89 |

**没有执行 `git commit`。**

---

## 68. 续批：铁轨（GT6 `Loader_Rails` 的 **10 材料 × 3 族 = 30 条轨道**）+ 把"1.20.1 能不能做铁轨"这个问题查清并落地

§60.2 把铁轨列为"大，自成一档"，一直没动，原因是不确定 1.20.1 有没有**按方块给矿车限速**的钩子。这批先把这件事查清（结论：**有**），再做。

### 68.1 预研结论（关键，值得记档）

| 问题 | 结论 |
|---|---|
| 1.20.1 有没有"方块决定矿车最高速"的钩子？ | **有**。Forge 给 `BaseRailBlock` 混入了 `IForgeBaseRailBlock`，提供 **`getRailMaxSpeed(BlockState, Level, BlockPos, AbstractMinecart)`**；`AbstractMinecart.moveMinecartOnRail` 走 `getMaxSpeedWithRail()` ＝ `min(railMaxSpeed, getCurrentCartSpeedCapOnRail())`。另有 **`onMinecartPass(BlockState, Level, BlockPos, AbstractMinecart)`**，每 tick 对轨上的矿车调一次——正是 GT6 `BlockBaseRail.onMinecartPass` 的对应物。⇒ **不需要 mixin/核心模组** |
| 那速度能拿到 GT6 的值吗？ | **七档可以**。矿车自身天花板 `IForgeAbstractMinecart.getMaxCartSpeedOnRail()` 默认 **1.2**（Forge 注释：超过 1.1 会引发区块加载问题）⇒ Al 0.20 / Bronze 0.30 / Magnalium 0.60 / Steel 0.60 / SS 0.80 / W 1.00 / **Ti 1.20** 原样生效；**Tungstensteel 1.40 / TungstenCarbide 1.60 / Adamantium 4.00 会被截到 1.2**（保留差异，代码与测试都注明） |
| 容易踩的坑 | Forge 的 `getMaxSpeedWithRail()` 里有 **`if (!state.is(BlockTags.RAILS)) return 默认速度`** ⇒ 功能轨**必须**进 `#minecraft:rails`，否则速度静默失效 |
| id 冲突 | `rail_straight_*` / `rail_turned_*` / `rail_booster_*` / `rail_booster_active_*` / `rail_detector_*` / `rail_detector_active_*` **已被 iconset 装饰方块占用**（GT6 本来也给每个方块图标登记 iconset 方块），`rail_gt` 是**物品**。⇒ 功能轨改用 **`track_<mat>` / `track_booster_<mat>` / `track_detector_<mat>`**（GT6 自己的显示名就是 "… Track"，不丢信息） |

### 68.2 实现

- 新 `block/misc/TrackBlock.java`：`Straight` / `Booster` / `Detector` 三个子类（分别继承 `RailBlock` / `PoweredRailBlock` / `DetectorRailBlock`），只重写两处——
  - `getRailMaxSpeed`：GT6 `BlockBaseRail:277-289` 的规则，**只有两侧邻居同轴且都是直轨**才给满速，否则 `min(速度, 0.4)`；邻居区块未加载时 `min(速度, 1.0)`；
  - `onMinecartPass`（仅助推轨）：GT6 `BlockBaseRail:291-322` —— 通电且速度 > 0.01 ⇒ **速度 ×2**；通电但静止 ⇒ 朝远离实心方块的方向推 **0.02**；**未通电** ⇒ 速度 < 0.03 **直接停住**，否则 **减半并抹掉 Y 速度**。
- 新 `registry/GTTrackBlocks.java`：GT6 `Loader_Rails:41-72` 的 10 材料 × 3 族，速度/抗爆**逐条照抄**（Al 0.20/6、Bronze 0.30/8、Magnalium 0.60/12、Steel 0.60/12、SS 0.80/10、W 1.00/20、Ti 1.20/16、Tungstensteel 1.40/20、TungstenCarbide 1.60/24、**Adamantium 4.00/100**），硬度沿用原版铁轨 0.7、抗爆取材料值、`SoundType.METAL`；`registerAll()` 接在 `Loader_MultiTileEntities` 的 `GTDecorBlocks.registerAll()` 之后。
- 新 `data/minecraft/tags/blocks/rails.json`：30 条全部进 `#minecraft:rails`（见 68.1 的坑）。方块掉落与采掘分类**不用管**——移植版的 `BlockLootPack` 内置数据包会给每个 gregtech 方块自动生成"掉落自身"的战利品表与 `minecraft:mineable/*` 标签（`BlockHarvestPolicy` 按 `SoundType.METAL` 归到镐子、等级 0 ⇒ 任意工具可采，与 GT6 用撬棍的差异记档）。
- 新 `tools/generate_track_assets.py`：**340 个模型 + 30 个 blockstate**。模型是"vanilla 几何 + 换贴图"的一行式（`{"parent":"minecraft:block/rail_flat","render_type":"minecraft:cutout","textures":{"rail":"gregtech:block/iconsets/rail_straight_steel"}}`），几何模板直接取自原版 jar 的 `assets/minecraft/models/block/{rail_flat,rail_curved,template_rail_raised_ne,template_rail_raised_sw}.json`；贴图**仓库里早就有**（`textures/block/iconsets/rail_{straight,turned,booster,booster_active,detector,detector_active}_<mat>.png`）。两个坑：①1.20.1 的资源路径**必须小写**（第一版写成 `RAIL_STRAIGHT_STEEL` 会找不到贴图）；②轨道的透明像素要靠 `render_type: minecraft:cutout`，否则渲染成黑块。
- 新 `loaders/Loader_TrackRecipes.java`（`@Mod.EventBusSubscriber` + `ServerStartedEvent`，与 `Loader_StoneCraftingRecipes` 同一套路）：GT6 `Loader_Rails:108-139` 的非 Railcraft 分支 —— 直轨 `"RSR","RSR","RSR"`（R=该材料 `railGt`、S=**处理木棍** `OP.stick.dat(ANY.WoodTreated)`）⇒ 4；助推轨 `"RSR","GDG","RSR"`（G=GT6 逐轨指定的 Au/Ag/Electrum/Pt/Os）⇒ 4；探测轨 `"RSR","RPR","RDR"`（P=原版石压力板、D=红石）⇒ 4。
- 新 `tools/add_track_lang.py`：中英各 **30** 条键（英文用 GT6 显示名 "Aluminium Booster Track"，中文用**移植版自己的材料译名** + "轨道/助推轨道/探测轨道"，例如 "钨钢助推轨道"；**插在排序位置**，不重排整份 json）。

### 68.3 刻意的移植差异

1. **速度天花板**：Tungstensteel / TungstenCarbide / Adamantium 的 GT6 原速 1.4 / 1.6 / 4.0 被原版矿车自身上限 **1.2** 截断（要突破得加 mixin/核心模组，本移植版没有）。测试里专门钉住这条，防止以后静默漂移。 ⚠️ **已于 §115 关闭**：`mixins/AbstractMinecartSpeedCapMixin` 让 Forge 的 setter 不再夹到 1.2，三条轨道现在跑出 GT6 的 1.4 / 1.6 / 4.0（测试相应改成"轨道把速度交给矿车"）。
2. **4 条原版轨的替换**：GT6 用 `DEL_OTHER_SHAPED_RECIPES` **换掉** `minecraft:rail` / `golden_rail` / `detector_rail` / `activator_rail` 的原版配方（改用 `railGt`，activator 按材料出 1..64）。移植版**没有配方替换机制**，直接加会变成"额外路径"而不是"替换"⇒ 本批**不做**，并在 `Loader_TrackRecipes.skipped()` 里逐条记档（测试断言恰好 4 条）。
3. **采掘工具**：GT6 用撬棍 + 材料品质等级；移植版走 `BlockHarvestPolicy` 的镐子/等级 0（与原版铁轨一致）。

| 指标 | §67 | §68 |
|---|---|---|
| GameTest | 424 | **429 全过**（+5：`TrackTests`） |
| 铁轨方块 | 0（只有 iconset 装饰块与 `rail_gt` 物品） | **30**（10 材料 × 直/助推/探测） |
| 铁轨速度 | — | **7 档拿到 GT6 原速**（Ti 1.20 为满值），3 档被原版矿车 1.2 天花板截断（记档） |
| 铁轨配方 | 无 | **30 条**（GT6 的 3 种图案 × 10 材料） |
| 模型/blockstate | — | 340 模型 + 30 blockstate（工具生成） |
| 语言键 | — | 中英各 +30 |
| 缺失 token | 49 | **49**（不变） |

改动文件：`block/misc/TrackBlock.java`（新）、`registry/GTTrackBlocks.java`（新）、
`data/minecraft/tags/blocks/rails.json`（新）、`loaders/Loader_TrackRecipes.java`（新）、
`loaders/b/Loader_MultiTileEntities.java`（一行注册）、`tools/generate_track_assets.py`（新）、
`tools/add_track_lang.py`（新）、`resources/.../blockstates/track*.json`（30）、
`resources/.../models/block/tracks/*.json`（340）、`resources/.../lang/{en_us,zh_cn}.json`（各 +30）、
`gametest/TrackTests.java`（新）。

**没有执行 `git commit`。**

---

## 67. 续批：材料数据链——**分子扫描仪 → USB → 打印机印词典 / 复制机**（关掉 §54/§60.2 的"打印机印材料词典"）+ 顺带修掉扫描仪本体档位的一个真错

§60.2 清单里的"打印机印材料词典"以及 §54 结尾记档的同一件事，这批做完了。GT6 的实现方式**不是静态配方表**：三个 `RecipeMap` 子类重写 `findRecipe()`，从输入物品的 **NBT** 现算配方——

| GT6 | 行 | 作用 |
|---|---|---|
| `RecipeMapScannerMolecular` | `:46-67` | 可扫描材料物品 + `USB_Stick_3` ⇒ 同一支 U 盘写入 `gt.usb.data = {gt.replicator.data: <材料 id>}` 与 `gt.usb.tier = 3`；工时 `(质子+中子)*512` tick、功率 **512 QU/t** |
| `RecipeMapPrinter` | `:115-151` | 该 U 盘 + 纸 ⇒ 材料词典：**3 张纸**（>50 页用 **6 张**）、512/1024 EU/t × 16 tick、化学黑染料 `FL.mul(DYE_FLUIDS_CHEMICAL[Black], 1, 2 或 1, T)` ⇒ **72 / 144 mB** |
| `RecipeMapReplicator` | `:81-84, 88-114` | 该 U 盘 + `neutralmatter`（中子数 mB）+ `chargedmatter`（质子数 mB）⇒ 复原材料：`(质子+中子)*256` QU/t、1 tick |

移植版本来是纯静态表，所以先给 `RecipeMap` 加了一个**动态配方钩子** `dynamicRecipes(DynamicRecipes)`：静态表没命中时才调用（等价于 GT6 那三处 `if (rRecipe != null) return rRecipe;` 的提前返回）。新 `content/recipe/GTMaterialDataRecipes.java` 提供三个 provider，并用 `GTMaterialDictionary`（§31/§54/§55 已做好的 8 页词典）作为打印机产物。因为**空闲机器每 tick 都会重跑一次查找**，每行按材料缓存（配方是不可变的：输入输出在 `RecipeInputs.consume` / `MachineWorkOutputs.roll` 里都被复制）。

### 67.1 顺带抓出的真错：分子扫描仪被登记成了 T1

写测试时先跑了一次端到端：机器**永远不出东西**。查下去发现移植版的 `BasicMachineOriginalParams` 里 `scannermolecular` 只有 **T1、`NBT_INPUT` 32（区间 16..64）**，而扫描仪配方要 **512 QU/t** ⇒ 移植版的能量区间**结构上就拒绝**它自己的配方，这台机器从移植进来那天起就不可能扫描任何东西。

回读原版 `Loader_MultiTileEntities:1549-1553`：GT6 只登记 **T3**（id 20423、`NBT_INPUT = 512`、区间 256..1024），T1/T2/T4/T5 **在源码里是注释掉的**；而 `tools/extract_basic_machine_recipes.py` 不认注释，把 T1 也抽了出来，`tools/gt6_machine_map.py` 又只映射 tier 1 ⇒ 表里进了错行。修法是**改工具再重跑生成器**，不手改生成物：

- `tools/gt6_machine_map.py` / `tools/generate_machine_recipe_table.py` 各加一份 `TIER_NUMBERS`（`scannermolecular → [3]`）与 `EXTRA_TIERS` 里的 `replicator → [1..5]`（GT6 的复制机是 T1..T5 五档，移植版此前也只有 T1）；
- `BasicMachineDefinitions`：`MachineDef` 增 `tierBase`（扫描仪那条填 3，方块 id `scannermolecular_osmium` 不变），并把写死的 massfab T2..T5 循环推广成 `EXTRA_TIERS`（massfab 与 replicator 共用，复制机新增 `replicator_osmiridium_t2..t5` 四个档 + 中英键）；
- 两张生成表复跑后：扫描仪一行变 **T3 512/256..1024**，复制机变 **T1..T5 32/128/512/2048/8192**。

即"数值/档位必须来自原版登记行"，而"原版把哪几档注释掉了"同样属于登记事实。

### 67.2 另外两处顺手修正

- `GTMainRecipes` 里打印机那几行的黑染料**写成了 1000 mB**（GT6 的染料单位是 **144 mB**，移植版自己的 `DyeProcessingRecipes` 也一律用 144 基准）⇒ 真实配方 `GT6_Main:352` 改 **144 mB**、`Loader_Recipes_Hints:56` 改 144、两张展示行（地图/方块）改 **16 mB**（＝`FL.mul(...,1,9,...)`）。
- 动态 provider 的"可扫描"判定：GT6 要求 `TD.Prefix.SCANNABLE`，移植版没有这个前缀标志，改成**先看物品是不是 `MaterialItem`**（材料形态的 `(prefix, material)` 挂在 Item 实例上）、再看 `ItemMaterialRegistry`（原版物品/方块的组分表）。第一版只查后者 ⇒ 材料物品一个都不认，端到端测试立刻抓到。

### 67.3 三个刻意的移植差异（都写在类注释里）

1. GT6 打出的是 `Paper_Printed_Pages`，玩家再用皮革+染料装订成书（`MultiItemBooks:99-121`）；移植版的 GT6 书籍一律是**原版成书**，所以打印机**直接出装订好的词典**，移植版没有"打印纸"这一族物品。
2. GT6 用 `TD.Processing.UUM` 门槛决定材料能否复制，移植版没有这个标志 ⇒ 复制机对**一切非反物质、且有形态或流体的材料**生效。
3. GT6 的打印机/复制机还能读 **USB 线缆**（指向 USB 端口方块）；移植版没有 USB 端口方块，只读机器自己的槽位。

| 指标 | §66 | §67 |
|---|---|---|
| GameTest | 420 | **424 全过**（+4：`MaterialDataChainTests`） |
| 分子扫描仪 | T1 / 32 QU（16..64）= **永远跑不了自己的配方** | **T3 / 512 QU（256..1024）＝ GT6 登记行** |
| 复制机档位 | T1 | **T1..T5**（+4 方块与中英键） |
| 打印机印词典 | 无（只在 skipped 清单里） | **有**：U 盘 + 3 纸 + 72 mB 黑染料 ⇒ 该材料 8 页词典 |
| 缺失 token | 49 | **49**（不变） |
| 新增文件 | — | `content/recipe/GTMaterialDataRecipes.java`、`gametest/MaterialDataChainTests.java` |

改动文件：`api/recipe/RecipeMap.java`（动态钩子）、`content/recipe/GTMaterialDataRecipes.java`（新）、
`content/recipe/GTMainRecipes.java`、`content/machine/BasicMachineDefinitions.java`、`GregTech.java`、
`data/BasicMachineOriginalParams.java` 与 `data/BasicMachineCraftingRecipes.java`（生成物，由工具复跑）、
`tools/gt6_machine_map.py`、`tools/generate_machine_recipe_table.py`、`resources/.../lang/{en_us,zh_cn}.json`（+4 键）、
`gametest/MaterialDataChainTests.java`（新）。

**没有执行 `git commit`。**

---

## 66. 续批：多方块部件 id 段已挖尽 + 工具复跑后重建绿点（无净内容变化）

§65 之后把工具的 id 段从 `17001–18299` 再放宽到 **`17001–18399`**，结果是：GT6 在该区间**没有新部件**（`add_valve_parts.py` 只多找到 1 个，`fix_duplicate_machine_parts.py` 随即把它作为撞名/重复项删掉），定义表**仍是 89 个部件**。

但工具复跑本身**重写了 `LargeMachineParts.java`**（追加后又删除），使 Java 文件 mtime 晚于 §65 的门禁日志 ⇒ 按"每个检查点都要是绿的"这条约束，**重新跑了一遍门禁与 jar**（语义无变化）：

| 项 | §65 | §66 |
|---|---|---|
| 多方块部件 | 89 | **89**（id 段 18xxx 已无新部件） |
| GameTest | 420 | **420 全过**（新一次运行 00:35:51） |
| 注册量 / 缺失 token | 69,792 / 12,055 / 49 | **不变** |
| jar | `A90DBCF5…` | **`F77102D5…`**（同一份源码的重新打包） |

**结论**：多方块部件这条线**已经挖尽**（GT6 在 17xxx–18xxx 登记的部件，除被 `LargeMachineParts.find(...)` 指向 `GTMultiblocks` 的 8 个 id 与 `GTMultiblocks` 自己登记的 18 个名字之外，全部已进表）。下一轮请转向 §60.2 清单里的其它项（打印机印词典 / 铁轨 / 养蜂 / 地牢）。

**没有执行 `git commit`。**

---

## 65. 续批：多方块部件再补**墙 / 线圈 / 处理单元**等（63 → **89** 个部件）+ 两个重复注册陷阱记档

§64 之后把同一个工具的 id 段再放宽到 **18000–18299**（GT6 "Multiblock Machines" 组里剩下的墙、线圈、处理单元等），解析出该区间 GT6 实际登记的 **42** 个 id（想要的 1199 个里绝大多数是空号），其中 **35** 个移植版没有 ⇒ 追加进定义表。

**门禁连抓两次重复注册**（都是"GT6 用同一个方块名登记在别处"造成的），值得记档：

| 报错 | 根因 | 修法 |
|---|---|---|
| `Duplicate registration centrifuge_part` | GT6 的 18100/18101/18102/18105 与 18000/18002/18022/18023 **本来就被移植版当作多方块控制器自己的部件**（`LargeMachineParts.find(...)` 的 `case <id> ->` 分支把它们指向 `GTMultiblocks` 的方块）⇒ 定义表里再登记一份就撞名 | 工具与清理工具都改为**跳过 `find()` 里已有的 id**（8 条） |
| `Duplicate registration large_niobium_titanium_coil` | 同一个方块名在 `GTMultiblocks` 里已经登记过（那条 `block("name", ...)` 用了别的辅助方法，我第一版正则没抓到） | 清理工具改为把 `GTMultiblocks.java` 里**所有** id 形状的字符串字面量都当"已被占用"（18 个名字），再删掉撞名条目 |

新增 `tools/fix_duplicate_machine_parts.py`（新）：按"`find()` 的 id + `GTMultiblocks` 的名字 + 自身重名"三重去重，**幂等**，可反复运行。工具链现在是：`add_valve_parts.py`（提取并追加，已跳过 `find()` id）→ `fix_duplicate_machine_parts.py`（去重兜底）→ `add_valve_part_lang.py`（补中英语言键）。

| 指标 | §64 | §65 |
|---|---|---|
| GameTest | 420 | **420**（全绿） |
| 多方块部件 | 63 | **89**（+26：墙 / 线圈 / 处理单元 / 通风单元等） |
| 语言键 | 中英各 +12 | 中英各 **+35** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 64. 续批：多方块部件补齐**涡轮/发电机/燃气涡轮外壳**（51 → **63** 个部件）

§63 的工具只覆盖了阀门与气压计两段 id，这批把 id 段放宽到 **17001–17299（跳过 171xx 的控制器本体）**，让同一套解析把 GT6 该区间的**全部**部件都抓出来：

| 新增 | id 段 | 内容 |
|---|---|---|
| **蒸汽涡轮主机壳** | 17211–17214 | Magnalium / Trinitanium / Graphene / Vibramantium（材料映射到不锈钢/钛/钨钢/亚当满） |
| **发电机主机壳** | 17221–17224 | 不锈钢 / 钛 / 钨钢 / 亚当满 |
| **燃气涡轮主机壳** | 17231–17234 | Magnalium / Trinitanium / Graphene / Vibramantium |

工具跑完即报告"我们想要的 199 个 id 里 GT6 实际登记了 42 个"（其余是没用的空号），新增 **12** 条 `new Part(...)`（**51 → 63**）；语言键工具按同样区间补齐中英各 12 条（英文用 GT6 显示名）。**门禁一次通过**（这次没有再被 `registerednameshavetranslations` 拦下，因为语言键与部件同步生成）。

| 指标 | §63 | §64 |
|---|---|---|
| GameTest | 420 | **420**（全绿） |
| 多方块部件 | 51 | **63**（+4 蒸汽涡轮 +4 发电机 +4 燃气涡轮外壳） |
| 语言键 | 中英各 +30 | 中英各 **+12** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 63. 续批：多方块部件补齐 GT6 的**阀门 24 个 + 锅炉气压计 5 个**（21 → 51 个部件）

§60 的缺口清单里"多方块的材质层（阀门 12 / 锅炉气压计 5 / 涡轮与发电机外壳）"这一条，这批先做最容易数据化的一半。GT6 在 `Loader_MultiTileEntities` 的 "Multiblock Machines" 组里登记了：

| 家族 | id 段 | 内容 |
|---|---|---|
| **储罐主阀门** | 17001–17007 | 木质 + 6 种材质（不锈钢/钨钢/钨/亚当满/钛/殷钢）的**小型**阀门 |
| | 17022–17027 | 同 6 种材质的**小型加厚**阀门 |
| | 17042–17047 | 同 6 种材质的**大型**阀门 |
| | 17062–17067 | 同 6 种材质的**大型加厚**阀门 |
| **锅炉主气压计** | 17201–17205 | 不锈钢/钛/钨钢/亚当满/殷钢 五种 |

移植版此前只有 `LargeMachineParts.DEFINITIONS` 里 **21** 个部件（墙/线圈/加工单元/粉碎轮等），这 30 个**完全缺失**。

修法：`tools/add_valve_parts.py`（新）从 GT6 注册行提取 `aMat = …; aRegistry.add("名字", "Multiblock Machines", id, …)`，把名字转成 snake_case、材质表达式映射成移植版材料名（`MT.StainlessSteel`→`StainlessSteel`、`ANY.W`→`Tungsten`、`MT.Ad`→`Adamantium`…），生成 30 条 `new Part(id, name, material)` 追加进定义表（**21 → 51**）。这些部件走既有的 `MachineBlockRegistration.block(name, MultiblockPortBlock::new)` 通道，因此方块、物品、多方块占位与 `find(id)` 反查都会自动生效。

**门禁第一次就抓出一处遗漏**：移植版自己的 `registerednameshavetranslations` 断言报 30 个新方块**缺语言键**（如 `block.gregtech.adamantium_boiler_main_barometer`）⇒ 补 `tools/add_valve_part_lang.py`（新）按同样的解析生成中英各 30 条键（英文用 GT6 的显示名），复跑即绿。这条正是 §32 记过的同一类守卫，说明该断言确实在防漏。

| 指标 | §62 | §63 |
|---|---|---|
| GameTest | 420 | **420**（全绿） |
| 多方块部件 | 21 | **51**（+24 阀门 +5 气压计 +1 木质阀门） |
| 语言键 | — | 中英各 +30 |
| 缺失 token | 49 | **49** |

仍缺：涡轮外壳（17211–17214）、发电机外壳（17221–17224）等——同一工具扩展 id 段即可继续。

**没有执行 `git commit`。**

---

## 62. 续批：给书架补上 GT6 的三行提示（`addToolTips`）

GT6 的书架在物品提示里明说三件事（`MultiTileEntityBookShelf.addToolTips`）：**没有 GUI、点它就能互动**（`LH.NO_GUI_CLICK_TO_INTERACT`）、**用钳子取书**（`LH.TOOL_TO_TAKE_PINCERS`）、**用放大镜看详情**（`LH.TOOL_TO_DETAIL_MAGNIFYINGGLASS`）。移植版的书架此前一条提示都没有——玩家只能靠猜。

修法（`BookShelfBlock.appendHoverText`）：直接用移植版**已有的共用键**（不需要新增语言文件条目），逐条对应 GT6 的三行：

| GT6 键 | 移植版复用键 |
|---|---|
| `NO_GUI_CLICK_TO_INTERACT` | `tooltip.gregtech.machine.nogui.click_front` |
| `TOOL_TO_TAKE_PINCERS` | `tooltip.gregtech.smeltery.tool.pincers` |
| `TOOL_TO_DETAIL_MAGNIFYINGGLASS` | `tooltip.gregtech.machine.tool.magnifying_glass` |

（提示文本是纯客户端行为，门禁只需编译通过 + 既有 420 项不回归。）

| 指标 | §61 | §62 |
|---|---|---|
| GameTest | 420 | **420**（全绿） |
| 书架提示 | 无 | **GT6 的三行**（无 GUI / 钳子 / 放大镜），复用既有语言键 |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 61. 续批：书架在**任何**移除路径下都掉落内容（GT6 的 `breakBlock` 语义）

§53 把书架的掉落挂在 `playerWillDestroy` 上——只覆盖"玩家挖掉"。GT6 重写的是 `breakBlock()`，**所有**移除路径都会走它（活塞推、爆炸、`/setblock`、其它模组拆除）。1.20.1 里与之对应的是 `Block#onRemove(...)`，所以把这批逻辑搬过去，并**删掉** `playerWillDestroy` 里的那份（否则玩家挖掉时会掉两次）：

```java
@Override
public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
    if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BookShelfBlockEntity shelf) {
        for (ItemStack stack : shelf.contents()) popResource(level, pos, stack);
        shelf.inventory().deserializeNBT(new CompoundTag());
    }
    super.onRemove(state, level, pos, newState, movedByPiston);
}
```

测试（`BookShelfTests.breakingTheShelfDropsItsBooks` 就地扩展，条数不变）：①放两本书后**直接 `level.removeBlock`（完全没有玩家参与）**⇒ 地上仍掉出 ≥2 份；②再放一个书架、塞一本书，用 `level.destroyBlock(pos, true, mockPlayer)` 走玩家路径 ⇒ 同样掉落（且不会因两处都挂而掉两次）。

| 指标 | §60 | §61 |
|---|---|---|
| GameTest | 420 | **420**（就地扩展，全绿） |
| 书架掉落 | 仅玩家挖掘 | **任何移除路径（活塞/爆炸/指令/拆方块）** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 60. 续批：§47–§59 这一轮的收口核查（无代码改动，只做事实核对与缺口登记）

这批**没有改代码**：把 §47 以来的十三个批次逐条对回原版与运行时事实，核对"已关掉的差异"是否真的关掉了，并把仍然存在的缺口钉成一张清单（避免下一轮重复劳动或误以为已完成）。

### 60.1 事实核对（工具复跑）

| 核对项 | 结论 |
|---|---|
| GameTest | **420 项全过**（§47 的 390 → 420，`All 420 required tests passed`，最后一次全绿运行未改动任何源码） |
| 缺失内容 token | **49**（不变；其中 `tech:` 36 = 其它模组、`i:` 11 = GT6 自身死的形态、`omd:` 2 = 其它模组流体） |
| 无配方物品 | **0**（`docs/items-without-recipes.json`） |
| 材料/物品/方块/流体 | **1156 / 69,724 / 11,987 / 925** |
| 小撮矿表 | 主世界 36 / 下界 18 / 末地 32 = **GT6 常开条目数，0 缺 0 多 0 参数不符**（`tools/extract_gt6_small_ores.py`） |
| 大矿脉表 | 主世界 **31** / 末地 **5** = GT6 条数，**0 缺**（`tools/extract_gt6_large_ores.py`） |
| GT6 世界生成对象 | 57 个类里只剩 `WorldgenCenterBiomes/Streets/Nexus/Beacon/Testing`（原版默认关闭）没做；`WorldgenHives` **§75 已完成**、`WorldgenDungeonGT` **§76 已完成**（`tools/inventory_gt6_worldgen.py`） |
| 基岩矿 | 表：主世界 28 + 下界 7；形状：§56 已换成 GT6 的区块锚定"松饼"形 |

### 60.2 仍然存在的缺口（下一轮的候选，均已定性）

| 缺口 | 规模/依赖 |
|---|---|
| **铁轨**（`Loader_Rails` 32 种轨 + `BaseRailBlock` + 矿车速度/物理） | ~~大，自成一档~~ → **§68 已完成**（10 材料 × 直/助推/探测 = 30 条轨道，速度走 Forge 的 `IForgeBaseRailBlock.getRailMaxSpeed`；3 档最快轨被原版矿车 1.2 天花板截断、4 条原版轨配方替换未做，均记档） |
| **养蜂/蜂窝**（`MultiItemBumbles` 已有物品、`MultiTileEntityBumbleHive` + `WorldgenHives` 缺方块层） | ~~需要 `IItemBumbleBee` 的**遗传系统**~~ → **§73–§78 全部完成**（配方 + 遗传 + 蜂种表 + 蜂巢方块/世界生成 + 扫描物品/配方 + 变异/组合/育种内核 + **育种机两台**）；有意跳过的只有蜇人 / `bumbleCanProduce` 工作地点扫描 / 铲子 GUI（均记档） |
| **多方块的材质层**（阀门 12 / 锅炉气压计 5 / 涡轮与发电机外壳等） | 中；`LargeMachineParts` 目前 21 个部件 + `GTMultiblocks` 托管的若干 |
| **打印机印材料词典** | ~~需要扫描仪把材料 id 写进 USB 的 `NBT_USB_DATA`（移植版扫描仪目前只有展示行）~~ → **§67 已完成**（扫描仪 T3 + USB 数据 + 打印机词典 + 复制机，见 §67） |
| **地牢**（`WorldgenDungeonGT` + `DungeonChunk*` 27 个类） | ~~大~~ → **§76 已完成主体**（逐格生成的布局 + 13 个单元 + 数据包 + 6 项测试）；剩 5 个传送门房（缺传送门子系统与地牢钥匙）与其它模组房间 |
| **所有权**（GT6 `mOwnable` 领地判定） | 需要一套领地系统 |
| **`Loader_Recipes_Replace/OreDict/Foreign`** | 1.7.10 兼容性替换表，1.20.1 语义不同（移植版走标签/`ToolShapedRecipe`），**判断为不适用**而非缺口 |
| **`MultiTileEntityCertificate`** | GT6 的赞助者证书（读 GT6 自己的支持者名单数据文件），**判断为不移植** |
| 视觉保留差异 | 书架逐本贴图（43 项索引表）、树液袋满/空贴图（GT6 资源包里没有 `overlay_full`）、基岩矿的深板岩前置（1.20.1 本就是深板岩） |
| 世界生成保留差异 | `checkForMajorWorldgen` 地牢保护未移植（各地表特征）、`GENERATED_NO_BEDROCK_ORE` 标志未建模 |

### 60.3 结果

| 指标 | §59 | §60 |
|---|---|---|
| GameTest | 420 | **420**（未改代码，沿用同一次全绿运行） |
| 缺失 token | 49 | **49** |
| 差异化清单 | 分散在各批次 | **集中登记在 60.2**（含"判断为不适用/不移植"的两条） |

**没有执行 `git commit`。**

---

## 59. 续批：地表碎石也能落在**矮草与雪层**上（GT6 的 `WD.easyRep`）

§52 的碎石生成里，落点上方那一格的判定写成了"必须是空气"——但 GT6 用的是 `WD.easyRep(...)`：只要那一格是**可替换**的（空气、雪层、以及原版可替换标签覆盖的草/蕨/花），岩石就压上去（把矮草替换掉）。移植版因此**比原版更严格**：草地上有矮草时原版仍会落石，移植版不会。

修法（`GTRocksFeature`）：新增 `replaceable(state)`＝`isAir() || #minecraft:replaceable || 雪层`，`castRay` 用它取代 `isAir()`。

测试（`RockAndDeepOceanTests.rockRayPlacesRocksOnGround` 就地扩展，条数不变）：断言 `replaceable` 对空气/矮草/雪层为真、对石头为假；再在草方块上放一株矮草，断言**仍然能落石**（原版语义）。

| 指标 | §58 | §59 |
|---|---|---|
| GameTest | 420 | **420**（就地扩展，全绿） |
| 碎石落点判定 | 只认空气（比原版严） | **GT6 的 `easyRep`**：空气 / 雪层 / `#minecraft:replaceable` |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 58. 续批：深海柱里的矿石背景改回**普通石头**（GT6 用的是 `ores_normal`）

§52 做深海三棱柱石柱时，1/8 概率的矿石是让移植版的 `GTOreBlockResolver.placeOre(...)` 处理的，而它的"背景岩石"取**宿主方块**——宿主刚被设成三棱柱石 ⇒ 矿石的背景被记成了 `prismarine_dark`。回读原版：GT6 用的是 `BlocksGT.ores_normal[13|14]`（**普通石头**背景的矿石变体，不是"三棱柱石背景"的矿石），所以那 1/8 的矿石在原版里是"石头背景的矿"嵌在三棱柱石柱里。

修法（`GTDeepOceanFeature.setPylonBlock`）：矿石放下之后，把方块实体记录的背景**改回 `stone`**：

```java
if (!GTOreBlockResolver.placeOre(level, pos, ore, false)) return false;
if (level.getBlockEntity(pos) instanceof OreBlockEntity block) block.setStone("stone");
```

测试（`RockAndDeepOceanTests.deepOceanPylonsMatchGt6` 就地扩展，条数不变）：遍历柱体 7×7×21 范围内**每一个** `OreBlockEntity`，断言其记录的背景都是 `"stone"`（而不是 `prismarine_dark`），并断言至少检查到了一块（否则矿没生成）。

| 指标 | §57 | §58 |
|---|---|---|
| GameTest | 420 | **420**（就地扩展，全绿） |
| 柱内矿石背景 | `prismarine_dark`（自创） | **`stone` = GT6 的 `ores_normal`** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 57. 续批：书架按**点击位置**取放书（§53 的保留差异关掉，槽位数 8 → **28**）

§53 把 GT6 的书架做出来时记了一条差异："GT6 移除的是**点击到的那个槽位**，移植版取第一个非空槽"。这批回读原版时发现这条差异背后还藏着一个更大的错：**槽位数不是 8**。

原版 `MultiTileEntityBookShelf` 的命中判定：

```java
if (aSide == mFacing     ) tIndex = (tCoords[1] < PX_P[8]?  6:13) - clamp(0, 6, 8*(tCoords[0]-PX_P[1]));
if (aSide == OPOS[mFacing]) tIndex = (tCoords[1] < PX_P[8]? 20:27) - clamp(0, 6, 8*(tCoords[0]-PX_P[1]));
```

⇒ 每个面 **两行 × 七列**，正面用 **0..13**、背面用 **14..27** ⇒ 一共 **28 槽**（`rng(14)` 的地牢战利品注入也只往正面 14 格里塞）。而 GT6 的点击语义也不是"潜行才取"：**点到有东西的格子就把那本书交还给玩家**，点到空格子才把手上的书放进去（`if (slotHas(tIndex) && ST.add(aPlayer, slot(tIndex), T))` / `if (BOOK_REGISTER.containsKey(tStack)) slot(aSlot, amount(1, tStack))`）。

### 57.1 修改

* `GTBookList`：`SLOTS = 8` → **28**、新增 `COLUMNS = 7`、新增 **`slotFor(front, hitX, hitY)`**（GT6 的 tIndex 公式逐字搬运，含 `hitY < 0.5` 与 `clamp(0, 6, 8*(hitX-1/16))`）。
* `BookShelfBlock.use`：按 `hit` 算出**点击到的槽位**，实现 GT6 的语义——占用 → 交还；空且手上是书类展示物 → 放入；其余 PASS（不再需要潜行）。
* `GTBookList` 的 javadoc 与方块注释同步更正（原先写的"两行四列"是错的）。

### 57.2 测试（`BookShelfTests` 就地升级，条数不变）

| 测试 | 变化 |
|---|---|
| `bookShelfRegistration` | `SLOTS == 28 && COLUMNS == 7`；**逐个核对命中公式**：正面左下 = 6、正面右下 = 0、正面上排起点 = 13、背面 = 20 / 27 |
| `shelfStoresBooksAndCountsEnchantPower` | 改用**两个具体的命中点**（正面左下 / 右下）：点左下格子放书 ⇒ **书出现在 `slotFor` 算出的那一格**且只有 1 本；点右下空格拿石头点 ⇒ 被拒且存量不变；**再点左下（有书）⇒ 书交还玩家、不再需要潜行**；比较器改为 **8/28 ⇒ 5**（原来按 8 槽写的 15 已不成立） |

### 57.3 结果

| 指标 | §56 | §57 |
|---|---|---|
| GameTest | 420 | **420**（就地升级，全绿） |
| 书架槽位 | 8（错） | **28 = GT6 的 2 面 × 2 行 × 7 列** |
| 点击语义 | 取第一个非空槽 / 潜行才取 | **点到哪格就取哪格**（GT6 逐字） |
| 缺失 token | 49 | **49** |

仍保留差异：GT6 破坏书架前会先把**地牢战利品**注入空槽（`mDungeonLootNameFront/Back`，GT6 自己的 "dungeonloot" 表），移植版没有那套地牢战利品表；GT6 用 `BOOK_REGISTER` 的 43 项贴图索引逐本渲染，移植版只用一个模型。

**没有执行 `git commit`。**

---

## 56. 续批：基岩矿脉换成 GT6 的**真实形状**（"松饼"形矿体，§50/§47 留下的差异）

§50 与 §47 都记着同一条保留差异："移植版的基岩矿是**每 16×16 区块格一个团块**，GT6 是 `y=1..6` 的 7 层'松饼'形"。这批把形状按原版补齐。GT6 的 `WorldgenOresBedrock.generateVein` 是**锚定在区块本身**的（不是围绕某个点），四步：

1. **前置**：区块的 `(minX+8, 0, minZ+8)` 必须是基岩；
2. **基岩核心**：`x/z = 5..10` 的 6×6 区域在 y=0 上，`rand(6)` 为 0 放**整块**基岩矿、1/2 放**小撮**，再**强制**在 `(6+rand(4), 6+rand(4))` 放一块整块矿（保证矿脉一定有个可挖的核心）；
3. **松饼形矿体**：`y = 1..6` 逐层按 GT6 自己的边界数组 `tD1 = {5,4,2,1,0,2,5}`、`tD2 = {11,12,14,15,16,14,11}` 铺方环——**最宽的一层（y=4）正好占满整个 16×16 区块**，上下各层逐级收窄到 6×6；每格 `rand(6)`＝0 整块、1/2 小撮；
4. **撒点**：从矿体向上到海平面做 `5+rand(3)` 条随机游走（每步每轴 1/7 概率平移一格），2/3 的步数放小撮矿，走出区块（x/z ≤0 或 ≥15）时再放一块后停止。

### 56.1 移植

* `GTBedrockOreFeature`：`placeDeposit(...)`（围绕任意中心的近似团块）→ **`placeVein(level, random, minX, minZ, material)`**，逐条搬运上述四步；`MUFFIN_INNER`/`MUFFIN_OUTER` 两个常量就是 GT6 的 `tD1`/`tD2`（并在 javadoc 里注明"index 0 不使用"）。
* 召唤方两处都改成区块锚定：`GTBedrockOreFeature.place` 用区块原点调用（撒花与指示岩仍按网格随机点），`GTColtanFeature.placeCentreVein` 直接用中心区块的原点——**GT6 对钶钽中心本来就是调同一个 `generateVein`**。
* 保留差异（新的一条，替代旧的那条）：GT6 用 `GENERATED_NO_BEDROCK_ORE` 标志让**每个区块第一条**矿脉先把周围石头换成深板岩（或挖掉基岩）；1.20.1 的这些深度本来就是深板岩，所以移植版**跳过这一步**（已在代码里注明）。

### 56.2 测试（新增 `BedrockVeinTests` 2 项）

| 测试 | 断言 |
|---|---|
| `muffinBoundsMatchGt6` | 两个数组长度 7；**逐层等于 GT6 的 `tD1/tD2`**（5/11、4/12、2/14、1/15、**0/16**、2/14、5/11）；最宽层是整区块、顶层与核心一样是 6×6 |
| `veinFollowsGt6Shape` | 备好"基岩地板 + 深板岩"的区块调用 `placeVein`：**核心 6×6 里确有矿且带基岩背景**（`OreBlockEntity` 的 stone ＝ bedrock）；**y=4 的矿体触及区块边缘，而 y=1 与 y=6 不触及**（层宽差异）；**海平面以上一粒都没有**（撒点止于水面）；**没有基岩的区块一矿不放**（前置检查）；结束时清理场地保证可重复运行 |

### 56.3 结果

| 指标 | §55 | §56 |
|---|---|---|
| GameTest | 418 | **420**（新增 2 项，全绿） |
| 基岩矿脉形状 | 近似团块（自述差异） | **GT6 的区块锚定形状**：6×6 基岩核心 + 7 层"松饼" + 随机游走撒点 |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 55. 续批：材料词典的**附魔页**（§31 遗留清单的第二半）

§54 关掉了合金页，这批把同一处的**附魔页**也做出来。GT6 给材料按用途分别挂附魔（`addEnchantmentForTools / Damage / Weapons / Ammo / Ranged / Fishing / Armors(Enchantment.x, level)`），词典会为**每一个非空列表**单独印一页；移植版的材料数据里**完全没有**这层信息（`api/material` 与 `content/material` 里一处 enchantment 都没有）。

### 55.1 做法：从 `MT.java` 转译附魔表

* `tools/extract_enchantment_table.py`（新）：解析 GT6 `MT.java` 里 **245 行**附魔赋值（含链式调用），生成 `loaders/c/GTEnchantmentTable.java`——**507 条**（**177 个材料**）记录 `(kind, enchantment, level)`，并带一张 **1.7.10 → 1.20.1 名称映射**（含 1.7.10 里被混淆的 `field_151370_z` ＝**海洋之运**）。统计：Damage 150、Ranged 148、Tools 75、Armors 41、Weapons/Ammo/Fishing 各 31；共 21 种附魔全部有 1.20.1 对应 id（生成脚本会把映射不到的附魔打印出来，本批为 0）。
* `content/book/GTMaterialEnchants.java`（新）：按材料名查表（**带别名表**——GT6 表里用的是字段短名 `Fe`/`Au`/`Pb`/`Ad`/`Ke`/`Pt`/`Bi`/`Ni`/`Ma`/`Atl`/`PO4`，移植版材料是 `Iron`/`Gold`/…；覆盖率检查：177 个里有 **147 个**直接对上，其余 29 个是短名与少数别的模组材料，别名表覆盖短名）；并按 GT6 的语义把 `Damage` 这个简写**同时并入 Weapons 与 Ammo**（`OreDictMaterial:1091` 的转发），页面上不出现 `Damage` 一项；等级按 GT6 习惯印**罗马数字**。
* `GTMaterialDictionary`：在合金页之后、矿石页之前插入 `enchantments(material)` 页（有附魔才生成），按 GT6 的六个分组顺序（Tools / Weapons / Ammo / Ranged / Fishing / Armors）逐组列出。

### 55.2 测试（新增 `EnchantmentDictionaryTests` 3 项）

| 测试 | 断言 |
|---|---|
| `enchantmentTableMatchesGt6` | 表 ≥150 个材料、总条数 ≥500；**每一条附魔 id 都是 1.20.1 真实附魔**（`ForgeRegistries.ENCHANTMENTS`，专治"1.7.10 名字没映射"）；等级为正；六个分组名与顺序等于 GT6 |
| `lookupMatchesGt6Materials` | **`Fe` 别名能查到 Iron** 且落在武器/工具组；**`Damage` 简写已被并入、页面里不再出现**；GT6 不附魔的材料不生成附魔页；等级罗马数字（I / IV / X） |
| `dictionaryHasEnchantmentPage` | 找一个"确实有附魔"的移植版材料（不写死名字），断言它的词典里有 `Enchantments of` 页，且**每个分组名都出现在页里** |

**测试自坑一处**：先断言"等级 1..10"，门禁报 `levels are 1..10, got 20`——GT6 自己就给到 20 级（超出原版上限），是**断言写窄了**，改为"正数"即绿。

### 55.3 结果

| 指标 | §54 | §55 |
|---|---|---|
| GameTest | 415 | **418**（新增 3 项，全绿） |
| 材料词典 | 7 页（含合金页） | **8 页**（+ 附魔页，覆盖 507 条附魔） |
| 附魔数据 | 移植版**完全没有** | **177 个材料 × 507 条**，21 种附魔全部映射到 1.20.1 |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 54. 续批：材料词典的**合金页**（把 §31 留下的"合金/目标矩阵页"关掉一半）

§31 做材料词典时写明了两处没做：合金/附魔/目标矩阵页（"需要按材料反查配方的能力"）与打印机印词典。这批把**合金页**做出来：GT6 的 `UT.Books.addMaterialDictionary` 会在词典里列出"这个材料参与的合金配方"，移植版此前只能列形态、工具属性与矿石。

### 54.1 做法：把合金表**生成**成可反查的数据

移植版的合金配方不是数据文件而是代码（`loaders/c/Loader_Recipes_Alloys.java` 里的 `mix(out("Bronze", 4), "Copper", 3, "Tin", 1)` 这类调用，共 53 处 `mix` + 3 处 `smelt`）。要反查就得有反向索引，于是：

* `tools/extract_alloy_table.py`（新）：解析那份 loader，生成 `loaders/c/GTAlloyTable.java`（`record Alloy(output, units, inputs)` + `ALLOYS` 常量）。**关键细节**：loader 里大量输入写成循环变量（`for (String copper : new String[]{"Copper","AnnealedCopper"})` ⇒ `mix(..., copper, 3, "Tin", 1)`），第一版直接照抄得到 52 条、其中输入名是 `copper`（小写、根本不是材料）⇒ 工具改为**先收集循环变量取值、再展开笛卡尔积**，得到 **95 条**真实配方（Copper / AnnealedCopper 之类的变体各成一条）。
* `content/book/GTMaterialRecipes.java`（新）：`alloyedInto(material)`（这个材料能合金成什么）、`madeFrom(material)`（它本身是哪些合金的产物）、`describe(alloy)`（`Bronze x4 <- Copper x3 + Tin x1`）。
* `GTMaterialDictionary`：在"属性页"与"矿石页"之间插入 `alloys(material)` 页——有配方才生成（GT6 也省略空段），列出 `Is an alloy of:` 与 `Alloys into:` 两半，并注明"1 unit = 144 mB"。

### 54.2 测试（新增 `AlloyDictionaryTests` 3 项）

| 测试 | 断言 |
|---|---|
| `alloyTableMatchesLoader` | 表 ≥ 50 条；GT6 最经典的合金逐条对上（**Bronze = Copper + Tin**、Brass = Copper + Zinc、Invar = Iron + Nickel、**StainlessSteel = Iron + Nickel + Chromium + Manganese**）；**表里每一个输入名都是真实材料**（专门守住"循环变量没展开"这个坑） |
| `dictionaryListsAlloys` | Bronze 的 `madeFrom` 非空且那一行同时提到 Copper 与 Tin；**Bronze 的词典里确实有 `Alloys of Bronze` 页**且含 `Is an alloy of:` 与 `Copper x3`；Copper 参与的合金 ≥5 条，**Copper 的词典含 `Alloys into:` 与 `Bronze`** |
| `materialsWithoutAlloysHaveNoPage` | 没有合金配方的材料**不生成**合金页（页出现与否与反查结果严格一致） |

### 54.3 结果

| 指标 | §53 | §54 |
|---|---|---|
| GameTest | 412 | **415**（新增 3 项，全绿） |
| 材料词典页 | 6 页（身份/组成/形态/工具/属性/矿石） | **7 页**（多了合金页，覆盖 95 条合金配方） |
| 缺失 token | 49 | **49** |

（§31 留的另一半——**打印机印词典**——仍然没做：它要扫描仪把材料 id 写进 USB 的 `NBT_USB_DATA`，而移植版的扫描仪目前只有展示行；已记档。）

**没有执行 `git commit`。**

---

## 53. 续批：GT6 的书架（`MultiTileEntityBookShelf` + `LoaderBookList`）

§9 清单里挂了很久的"书架方块"，也是 §31 说明"写明没做"的最后一件事。GT6 的做法：书架是一个**朝向方块**，**八个槽位**只能放"书类展示物"，**没有 GUI**——对着书架点击就放书、潜行点击就取回；`getEnchantPowerBonus` 让书架给附魔台加成（**普通书 +1、附魔书 +2**）；破坏时掉落里面的东西。哪些物品算"展示物"来自 GT6 的 `BooksGT.BOOK_REGISTER`（书、纸、地图、命名牌、展示框、画、木按钮），而加成只算 `BOOKS_NORMAL`（普通书/书与笔/成书）与 `BOOKS_ENCHANTED`（附魔书）。

### 53.1 移植

* `content/book/GTBookList.java`（新）：GT6 那三张表的移植版——`canPlace(...)`（`BOOK_REGISTER`：凡能摆上书架的物品）、`isNormalBook/isEnchantedBook`、`enchantPower(stack)`（+1 / +2）、`enchantPower(handler)`（整架合计）、`SLOTS = 8`。**移植版自己的 16 本手册与材料词典本来就是 vanilla 成书**（§21/§31），所以自动落进 `BOOKS_NORMAL`；另外单独接受 §31 的 `dusty_material_dictionary`。
* `block/BookShelfBlock.java`（新）：`HorizontalDirectionalBlock` + `EntityBlock`（朝向、放置朝向、rotate/mirror）；`use` ＝ 潜行取回 / 手持书类物品放入；`playerWillDestroy` 掉落全部内容；`getEnchantPowerBonus` 返回书架加成（Forge 的附魔台接口——原版附魔台会沿书架扫描并调用它）；`getAnalogOutputSignal` ＝ 按已用槽位给 0..15；`getLightBlock` ＝ 15（不透明）。
* `blockentity/inventory/BookShelfBlockEntity.java`（新）：`ItemStackHandler(8)`（`isItemValid` ＝ `GTBookList.canPlace`）、NBT 存档、`IItemHandler` capability（漏斗/管道可用）、`enchantPower()`、`contents()`。**capability 直接构造时就绪**（新放的方块实体不一定走到 `onLoad()`——这是门禁里踩出来的）。
* 注册：`GTDecorBlocks.BOOKSHELF`（`gregtech:bookshelf`）+ `GTBlockEntities.BOOKSHELF`；`tools/wire_book_shelf.py`（新）生成 blockstate（4 朝向）+ 方块模型/物品模型 + 双语键。**保留差异（视觉）**：GT6 用 `LoaderBookList` 里那张 43 项的**贴图索引表**逐本渲染架子上的书，移植版只用一个书架模型（贴图取 GT6 的 `planks_wood`）。

### 53.2 测试（新增 `BookShelfTests` 4 项）

| 测试 | 断言 |
|---|---|
| `bookShelfRegistration` | 方块与方块实体类型都已注册、是 `BookShelfBlock`、默认 `facing=north`、`SLOTS == 8` |
| `bookListMatchesGt6` | 普通书 = 1 点、附魔书 = 2 点、石头 = 0 点；`BOOK_REGISTER` 的 10 种展示物都接受（书/书与笔/成书/附魔书/纸/地图/已填地图/命名牌/展示框/画）；石头与空栈被拒 |
| `shelfStoresBooksAndCountsEnchantPower` | 点击放书（每次 1 本）、**非书类物品被拒且书架的存量不变**、潜行点击取回 1 份且回到玩家背包；4 本普通书 + 4 本附魔书 ⇒ **12 点**，方块的 `getEnchantPowerBonus` 与之一致、满架比较器 = 15、capability 暴露 8 槽 |
| `breakingTheShelfDropsItsBooks` | 放两本书 → `playerWillDestroy` + 移除方块后**地上至少掉出 2 份**（GT6 破坏即掉落内容） |

门禁里踩到三个坑（都是**测试自己的问题**，实现没错）：①GameTest 的 `makeMockPlayer()` **恒为创造模式**（`isCreative()` 被覆写），所以"放书后手上少一本"测不了 ⇒ 改为断言"每点一次正好存 1 本"，并在测试里写明；②断言"石头点击后第 1 槽仍空"不成立——mock 玩家的 `setItemInHand` 不生效，第二次点击存的还是那本书 ⇒ 改成**比较总存量**而不是具体槽位；③`BookShelfBlockEntity` 的 capability 原来在 `onLoad()` 里赋值，新放的方块实体没走到那里 ⇒ `getCapability` 返回空，改成构造时就绪。

**门禁插曲（本批三次）**：`-PgameTestHeap=3G` 下连续两次在**同一处**日志停更 8 分钟（JVM 3.5 GB 堆、CPU 仍在涨，同时在跑 Minecraft）；杀掉陈旧 `gameTest` JVM 后改用 `-PgameTestHeap=4G` 重跑，**2 分 30 秒**全绿（未改任何代码）。

### 53.3 结果

| 指标 | §52 | §53 |
|---|---|---|
| GameTest | 408 | **412**（新增 4 项，全绿） |
| 书架 | 完全不存在（只有原版书架的处理配方） | **GT6 的 8 槽朝向书架 + 附魔加成 + 掉落 + capability** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 52. 续批：地表碎石（`WorldgenRocks`）与深海三棱柱石柱（`WorldgenDeepOcean`）

用 `tools/inventory_gt6_worldgen.py`（新）把 GT6 的 57 个 `Worldgen*` 类逐个对照移植版，剩下的**真正没搬**的对象只剩五个：`WorldgenRocks`（地表碎石）、`WorldgenDeepOcean`（深海柱）、`WorldgenHives`（蜂窝，缺方块层）、`WorldgenDungeonGT`（地牢）、以及 `WorldgenCenterBiomes/Streets/Nexus/Beacon/Testing`（原版默认 `F` 关闭的"世界中心"结构）。这批做前两个。

### 52.1 地表碎石 `WorldgenRocks`（`Loader_Worldgen:618`，`"overworld.rocks"`，amount 2 / probability 3）

* **骨架**是 GT6 的 `WorldgenOnSurface.generate`：先在 16×16 里**标记 `amount` 个目标格**（可重复），再对每个被标记的格掷 **1/probability**，然后从 `waterLevel - 1` 往下到 `min(height-2, waterLevel-1)` 投射**天空射线**：农田立即中断、非液体且（不是满方块或木头或树叶）就继续、否则把这格交给 `tryPlaceStuff`。
* `tryPlaceStuff`：接触面必须是 `Material.grass/ground/sand`（草/土/沙；移植版＝`#minecraft:dirt` 标签 + 沙标签 + 草方块/菌丝/灵魂沙/灵魂土），并且上方必须是空气（GT6 的 `easyRep`）⇒ 放一个**躺在地上的岩石**（GT6 32757，移植版 `gregtech:rock`）。
* **岩石带什么**（GT6 的 `ST.save(NBT_VALUE, …)` 三元表达式**逐字照抄**）：`nextInt(amount) == 0`（1/2）才有物品；有物品时 `nextInt(12) == 0`（1/12）是**陨铁**，其中 `nextInt(4) == 0`（1/4）是**原矿**、其余是 GT 岩石；否则是**燧石**。⇒ 大约 1/2 空手、1/2 燧石、1/24 陨铁。
* 群系门槛＝ GT6 的 `canGenerate` 九个集合（沙漠/恶地/针叶林/沼泽/热带草原/平原/森林/山地/荒原）；1.20.1 映射写进 `GTWorldgenBiomes`（`BIOMES_WASTELANDS` 在 1.20.1 无对应，留空并记录）。
* 顺手把 §40 里那份"放置躺地物品"的辅助方法提到 `GTRockPlacement.place(...)`（材料 / 显式物品 id / 原矿标志三件套），岩屑与地狱散落物共用。

### 52.2 深海三棱柱石柱 `WorldgenDeepOcean`（`Loader_Worldgen:580`，`"ocean.prismacorals"`）

* 只在**深海**群系；随机取区块内一列 `(3+rand9, 30+rand9, 3+rand9)`，要求那格是水；再用 `noise(minX+8, 32, minZ+8, 16)` 决定：**12/13 → 暗色三棱柱**、**14/15 → 亮色三棱柱**、其余什么都不做。
* 柱体是**上下对称的双锥**：四层方环 `l = 8..10` 半径 0、`l = 5..7` 半径 1、`l = 2..4` 半径 2、`l = 0..1` 半径 3，每层同时放在 `j+l` 与 `j-l`；每块三棱柱有 **1/8** 概率被替换成该柱的矿石（暗色→**硅镁镍矿**、亮色→**软锰矿**）。
* 移植版**不需要新方块**：`StoneType.PRISMARINE_DARK/LIGHT` 早就有了（GT6 里它们就是两种石头）。矿石用 `GTOreBlockResolver.placeOre(...)`，宿主正是刚放下的三棱柱石（背景岩石记录为 `prismarine_dark/light`）。
* 水的判定接受**原版水与 §49 的三种 GT 水**，所以与 §49 的水体替换谁先谁后都不影响（GT6 的 `WD.anywater` 本来也是认所有水方块）；数据包放在 `top_layer_modification`。

### 52.3 测试（新增 `RockAndDeepOceanTests` 4 项）

| 测试 | 断言 |
|---|---|
| `rocksMatchGt6Table` | amount 2 / probability 3 / `nextInt(12)` / `nextInt(4)`；9 个允许群系在集合里、深海/丛林/下界不在；`WASTELANDS` 为空并记录；特征 id 与数据文件已加载 |
| `rockLitterRollsMatchGt6` | 24000 次取样：空手 ≈ 1/2、陨铁 ≈ 1/24、燧石远多于陨铁、原矿占陨铁的少数（且 >0）；`minecraft:flint` 与 `gregtech:rock` 真实存在 |
| `rockRayPlacesRocksOnGround` | 草方块与沙地上**会**落石（多种子重试）；**石头接触面一颗都不放**；**农田立即中断**（GT6 明确排除） |
| `deepOceanPylonsMatchGt6` | 噪声 16／采样点 `(+8, 32, +8)`／暗 12-13 亮 14-15／1/8 矿石／四层 `l` 与半径（0/1/2/3）；直接驱动 `buildPylon` 在备好的水柱上建柱：**顶端在 +9**、**下镜像在 −9**、**最宽环半径 3**、**半径 3 之外什么都不放**、柱内确实出现 `ore_garnierite` |

写测试踩了两个坑：①石头方块 id 不是 `<type>` 而是 **`<type>_<variant>`**（`GTBlocks.getStone(type, StoneVariant.STONE)`）——第一版用 `gregtech:prismarine_dark` 查注册表直接失败；②柱体每块有 1/8 变矿石，所以形状断言不能只认三棱柱石，改成"三棱柱石**或**该柱的矿石"。

**门禁插曲**：本批三次运行里两次遇到**日志停更 7–14 分钟**的陈旧 JVM（3.5 GB 堆、CPU 仍在涨，同时在跑 Minecraft）——按记录过的处置：杀掉陈旧 `gameTest` JVM、删日志重跑，**未改代码**即全绿（本次无改动重跑 2 分 39 秒完成）。

### 52.4 结果

| 指标 | §51 | §52 |
|---|---|---|
| GameTest | 404 | **408**（新增 4 项，全绿） |
| 地表碎石 | 无（只有矿脉/基岩矿的指示岩） | **GT6 的 2 格目标 + 1/3 掷点 + 天空射线 + 燧石/陨铁岩屑** |
| 深海 | 无 | **深海三棱柱双锥柱 + 1/8 硅镁镍矿/软锰矿** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 51. 续批：大型矿脉表补齐 GT6 的 8 条（主世界 5 + 末地共享 3）

§50 用小撮矿表对齐之后，同一套办法用到了**大型矿脉**（`WorldgenOresLarge`，`Loader_Worldgen:885-924`）。先写审计工具 `tools/extract_gt6_large_ores.py`——注意原版这个大矿脉构造函数的维度不是 `GEN_*` 标志，而是**末尾的 `ORE_*` 列表**（`ORE_OVERWORLD` / `ORE_NETHER` / `ORE_END` / `ORE_MARS`…），第一版按 `GEN_*` 解析得到"GT6 0 条"的假结果，改正后：

| 维度 | GT6 常开 | 移植版（本批前） | 缺 |
|---|---|---|---|
| 主世界 `ORE_OVERWORLD` | **31** | 26 | **lignite / coal / bauxite / iodinesalt / rocksalt（5 条）** |
| 末地 `ORE_END` | **5** | 2 | **platinum / molybdenum / cassiterite（3 条，与主世界共享同一套参数）** |

移植版表里**没有多出的条目、已对齐的条目参数也全部一致**，只是这 8 条从来没有。

### 51.1 为什么当年被跳过，以及为什么该补

移植版 `OVERWORLD_VEINS` 的注释写着这几条是"有意省略"：`those resources now generate as whole-block seam layers (GT6 BlockRockOres) in the stone layer system instead`。核过原版后确认这个理由是**站不住的**——GT6 **两套都有**：同一批资源既作为**整层矿脉（seam）**出现在石头层系统里（`WorldgenStone` / `BlockRockOres`，移植版保留在 `GTStoneLayersGen.LAYERS` 的 `block_ore_lignite` / `block_ore_anthracite` / `block_ore_bauxite` / `block_ore_salt` / `block_ore_rocksalt`），也作为**大矿脉**登记在 `WorldgenOresLarge`。跳过矿脉等于只搬了 GT6 的一半生成。

| 新增矿脉（GT6 顺序） | minY–maxY | 权重 | 密度 | 尺寸 | 四种材料（顶/底/夹层/外围） |
|---|---|---|---|---|---|
| `lignite` | 50–130 | 160 | 8 | 32 | Lignite / Lignite / Lignite / Coal |
| `coal` | 50–80 | 80 | 6 | 32 | Coal / Coal / Coal / Lignite |
| `bauxite` | 50–90 | 80 | 4 | 24 | Bauxite / Bauxite / Bauxite / Ilmenite |
| `iodinesalt` | 50–60 | 30 | 3 | 24 | IodineSalt / Salt / Borax / Zeolite |
| `rocksalt` | 50–60 | 30 | 3 | 24 | Sylvite / Coltan / Lepidolite / Spodumene |
| 末地 3 条 | 与主世界同参数 | | | | platinum(Cooperite/Palladium/Sperrylite/Iridium)、molybdenum(Wulfenite/Molybdenite/Molybdenum/Powellite)、cassiterite(Stannite/Kesterite/Huebnerite/Cassiterite) |

插入位置严格按 GT6 的登记顺序（`lignite, coal` 在最前，`bauxite/iodinesalt/rocksalt` 在 `lapis` 与 `asbestos` 之间；末地表按 `platinum, molybdenum, cassiterite, naquadah, trinium`），这样噪声/权重索引与 GT6 的登记顺序一致。材料映射：GT6 `MT.KIO3` ＝ 移植版 `Materials.IodineSalt`（原版内部名 "Iodine Salt"，id 8242）、`MT.NaCl` → `Materials.Salt`、`MT.KCl` → `Materials.Sylvite`、`MT.OREMATS.*` → `OreMaterials.*`；新增用到的材料（Lignite/Coal/IodineSalt/Sylvite/Palladium/Iridium/Molybdenum/Ilmenite…）在原版形态表里都带 `ORES` ⇒ 都有 `ore_<材料>` 方块可放置。

### 51.2 测试（新增 `VeinTableTests` 3 项）

| 测试 | 断言 |
|---|---|
| `overworldVeinsMatchGt6` | 主世界 **31 条**且**顺序逐条等于 GT6**；§51 新增 5 条的 (minY,maxY,weight,density,size) 等于原版；`TOTAL_VEIN_WEIGHT` ＝ 全部权重之和 |
| `endVeinsMatchGt6` | 末地 **5 条**、顺序 = `platinum, molybdenum, cassiterite, naquadah, trinium`；三条共享矿脉的参数与主世界**逐项相同**；naquadah/trinium 仍在 |
| `everyVeinMaterialResolves` | **每条矿脉的四种材料都能解析出 `ore_*` 方块**（否则该矿脉一粒都不生成）；并且 **5 个整层矿脉（lignite/anthracite/bauxite/salt/rocksalt）仍在 `GTStoneLayersGen` 里**——把"GT6 既有层又有脉"这条事实钉死在测试里 |

写测试时踩了一个**自己的索引错误**：新增 5 条按原版顺序插在表头，但 GT6 的顺序是 `lignite, coal, apatite, lapis, bauxite, …` ⇒ 直接取 `get(0..4)` 拿到的是 lignite/coal/**apatite/lapis/bauxite**，门禁报 `bauxite = 40/60/60/3/16, iodinesalt = 20/50/40/5/16, rocksalt = 50/90/80/4/24`（正好是后三条错位一位的读数）⇒ **实现是对的、测试期望写错了**，改为按名字查表即绿。（另有一次 lambda 捕获循环变量导致编译失败，改用 `Object[][]` + for-each。）

### 51.3 结果

| 指标 | §50 | §51 |
|---|---|---|
| GameTest | 401 | **404**（新增 3 项，全绿） |
| 主世界大矿脉 | 26 | **31 = GT6 的 `ORE_OVERWORLD` 条数** |
| 末地大矿脉 | 2 | **5 = GT6 的 `ORE_END` 条数** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 50. 续批：下界三件套对齐 GT6（下界基岩矿表 + 晶体天花板规则 + GT6 唯一漏掉的 `eudialyte` 小撮矿）

这批先从**运行时事实**入手：写了一个临时 GameTest（用完即删）把 GT6 的基岩矿表、三个维度的小撮矿表逐条拿 `GTOreBlockResolver.resolve(...)` 试一遍，结果落盘成 `build/worldgen-diagnostic.txt`。结论两条：

1. 移植版注册了 **970 个** `ore_*` / `ore_small_*` 方块，**现有 28 条主世界基岩矿与 85 条小撮矿全部可放置**（没有一条因"缺矿石方块"被静默跳过）。之前用源码扫描 id 得出的"缺一大堆"是**工具误报**——移植版的按材料矿石方块是**循环注册**的（id 是变量），源码级扫描看不见（已记档）。
2. 真正缺的是**下界**那一块：GT6 为下界登记了 **7 条基岩矿**，移植版的 `GTBedrockOres` 只有主世界表，而且特征里**写死**用主世界表、数据包也只挂在主世界 ⇒ 下界一粒基岩矿都没有。

### 50.1 三处对齐

| 项 | GT6（`Loader_Worldgen`） | 移植版此前 | 本批 |
|---|---|---|---|
| **下界基岩矿** | `:758-764` 共 **7 条**：voidquartz 1/4000、glowstone 1/4000、gloomstone 1/4000、efrine 1/2000、netherquartz 1/2000、firestone 1/8000、ancientdebris 1/4000；**这 7 条都不带指示花**（只有地表岩石） | 只有 `OVERWORLD`（28 条），特征写死用它 | 新增 `GTBedrockOres.NETHER`（7 条、`flowerId = null`）+ `forDimension(...)`；特征按**维度**选表；`flowerId` 为 null 时跳过撒花；新增数据包 `gt_nether_bedrock_ores.json`（`#minecraft:is_nether`、`underground_ores`） |
| **下界晶体** | `WorldgenNetherCrystals`：只接受 **`Material.rock`** 的天花板、**明确排除下界砖**；**把天花板那一格本身**换成晶体（`setBlock(aX, aY, aZ)`），再从它往下/四周 **1500 次**尝试长成晶簇（只长进"已贴着晶体"的空气格）；要求天花板距岩浆海至少 11 格（`--aY - 10 < waterLevel`，下界 waterLevel = 31） | 只排除"下界砖 + 空气"，晶体**种在天花板下方那一格空气里** | `isRockMaterial(...)`（下界岩/黑石/玄武岩/GT 岩石/黑曜石/岩浆块/古代残骸/石英矿算岩石；**下界砖、荧石不算**）；种子改为**替换天花板方块**；晶簇以种子为起点；列循环抽成 public 的 `placeCrystalAt(...)` 供测试驱动 |
| **`ore.small.eudialyte`** | `:829`：`T, 20, 40, 4, MT.Eudialyte, GEN_OVERWORLD, …` | 表里**没有**这一条——当年写表时移植版还没有 Eudialyte（§32 才补上），注释里写着"no Eudialyte material exists in the port yet" | 按 GT6 的**顺序**（lapis 之后、azurite 之前）补进 `OVERWORLD_SMALL_ORES`，材料取 `CompoundMaterials.Eudialyte`；表注释同步更正 |

顺带产出一个**表对齐审计工具** `tools/extract_gt6_small_ores.py`（把 GT6 的 76 条 `WorldgenOresSmall` 登记按维度分组，并区分"GT6 常开"与"依赖别的模组"）：

| 维度 | GT6 常开 | 其他模组门控 | 移植版 | 缺 | 移植版多出 | Y/数量不符 |
|---|---|---|---|---|---|---|
| 主世界 | 36 | 22 | 36（本批 +eudialyte） | **0** | 0 | 0 |
| 下界 | 18 | 8 | 18 | **0** | 0 | 0 |
| 末地 | 32 | 7 | 32 | **0** | 0 | 0 |

（报告落在 `docs/gt6-small-ores.json`；"其他模组门控"＝ GT6 自己用 `MD.HEX.mLoaded` / `MD.IHL.mLoaded` / `MD.TC.mLoaded` 之类开关的条目，移植版没有那些模组，原版同样不生成。）

### 50.2 测试（新增 `NetherWorldgenTests` 4 项）

| 测试 | 断言 |
|---|---|
| `netherBedrockOresMatchGt6` | 下界表 **7 条**、名字与稀有度逐条等于 `Loader_Worldgen:758-764`、**都没有指示花**；`forDimension` 三分支正确（末地为空）、主世界表仍是 28 条；firestone(1/8000) 比 efrine(1/2000) 稀有；表里有 ancient debris |
| `netherCrystalsReplaceTheCeiling` | 距岩浆海 20 格的下界岩天花板：**天花板那一格本身**变成晶体、原下界岩消失、晶簇往下长；**下界砖天花板被拒绝**（原版明确排除）；**荧石与灵魂沙天花板被拒绝**（非 `Material.rock`）；`isRockMaterial` 对下界岩/黑石为真、对下界砖/荧石为假；**距岩浆海只有 5 格的天花板被拒绝**（11 格门槛） |
| `crystalClusterGrowsFromTheSeed` | 晶簇确实长大（>1 格）；**每一格晶体都必须贴着另一格晶体**（逐格六向检查，不允许悬空晶体） |
| `everyWorldgenTableEntryResolves` | **GT6 的每一张表（主世界 28 条基岩矿 + 下界 7 条 + 三个维度的小撮矿）逐条都能解析出 `ore_*` / `ore_small_*` 方块**——这是防止"表里有、世界里永远不生成"的守卫测试；并断言 eudialyte 条目的 (20, 40, 4) 与三个表的条数（36 / 18 / 32） |

写测试时踩到一个**自己的错**：先断言"天花板下方那一格仍然是空气"，门禁立刻报 `nethercrystalsreplacetheceiling failed! the air block below the ceiling stays air`——但 GT6 的晶簇本来就会往下长进贴着种子的空气格，是**断言写错了**（不是实现错）。改为断言"天花板那一格是晶体、且原下界岩已消失、且晶簇确实往下长"，重跑即绿。

### 50.3 结果

| 指标 | §49 | §50 |
|---|---|---|
| GameTest | 397 | **401**（新增 4 项，全绿；实际运行 401 = 注解 401） |
| 下界基岩矿 | **0**（表都不存在） | **7 条**（GT6 的名字/稀有度/无花） |
| 下界晶体 | 种在天花板下方空气里、只排除下界砖 | **替换天花板方块** + `Material.rock` 判定 |
| 主世界小撮矿 | 35 条（缺 eudialyte） | **36 条＝GT6 常开条目数** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 49. 续批：海洋/河流/沼泽的水换成 GT6 自己的水（`WorldgenOcean` / `WorldgenRiver` / `WorldgenSwamp`）

GT6 `Loader_Worldgen:576-578` 的三条登记——`new WorldgenOcean("ocean.seawater", …)`、`new WorldgenRiver("river.riverwater", …)`、`new WorldgenSwamp("swamp.dirtywater", …)`——把**海洋、河流、沼泽里的水换成 GT6 自己的水方块**（`BlocksGT.Ocean` / `River` / `Swamp`）。原版源码的注释写得很直白：`// IT IS IMPORTANT THAT OCEAN COMES BEFORE RIVER AND SWAMP`、`// IT IS IMPORTANT THAT RIVER COMES AFTER …`。

| | 海洋 `ocean.seawater` | 河流 `river.riverwater` | 沼泽 `swamp.dirtywater` |
|---|---|---|---|
| 群系门槛 | 区块内任一 `BIOMES_OCEAN` | 任一 `BIOMES_RIVER` **且该区块不沾 `BIOMES_OCEAN`** | 任一 `BIOMES_SWAMP` |
| 产物 | `BlocksGT.Ocean`（流体 `FL.Ocean` = `seawater`） | `BlocksGT.River`（`FL.River_Water` = `riverwater`） | `BlocksGT.Swamp`（`FL.Swampwater` = `swampwater`） |
| 每列规则 | 从 `waterLevel` 往下到 y=0：遇**不透明方块**即止；水方块**整列**换掉（不只是水面那一格）；其它非水非不透明格跳过 | 同左 | 同左，**并且**把 GT 自己的 waterlike 方块（前面 pass 已换成海水/河水的格子）也换成沼泽水 |

### 49.1 移植

* **前提**：移植版有这三种流体（`RegisteredFluids.Ocean` = `seawater`、`River_Water` = `riverwater`、`Swampwater` = `swampwater`），但和 §44 之前一样**没有世界方块**。复用 §44 的机制：`Loader_Fluids` 里那 6 条泉流体的集合改名 `WORLD_FLUID_PATHS` 并加入这三条路径（方块与流体互相惰性引用，`AtomicReference`）；`tools/register_water_body_blocks.py`（新）生成 3 个 blockstate（水的 `level=0..15`）+ 6 个模型 + 3 条双语键。
* **贴图沿用原版水**：GT6 的 `BlockWaterlike` 里 `getIcon(...)` 直接返回 `Blocks.water.getIcon(...)`、`colorMultiplier` 恒为白色 ⇒ GT6 的三种水在世界里**看起来就是原版水**（区别在流体身份与玩法），所以这批**不需要任何新贴图**（模型直接引用 `minecraft:block/water_still` / `water_flow`）。
* 新 `worldgen/GTWaterBodyFeature.java`：三条 pass 由**同一个特征按 GT6 的顺序**执行（`PASSES = [OCEAN, RIVER, SWAMP]`），`Kind` 枚举带上 GT6 的登记名与方块 id；`convertColumn(...)` 逐条搬运 GT6 的列循环（不透明即止、整列换水、其余跳过）；`matches(...)` / `resolve(...)` 是纯函数（后者返回"最后一条命中的 pass"，即沼泽水覆盖海水/河水，与 GT6 的登记顺序一致），供 GameTest 直接驱动。
* `worldgen/GTWorldgenBiomes.java` 增加 `BIOMES_OCEAN`（＝ `OCEAN_BEACH` 去掉 4 个海滩/海岸，正是 GT6 里两个集合的差别）。
* 数据包：`gt_water_bodies` 特征 + 配置/放置特征 + 生物群系修饰符，放在**区块管线的最后一步** `top_layer_modification`。理由：移植版其余特征（地表植被、沉积、坑、树）仍然只认原版水，放在最后一步能保证它们的行为与本批之前**完全一致**，只有最终的水体身份变了（GT6 里这些对象同属区块填充阶段，且它自己的 `WD.anywater` / `waterstream` 系列能认 waterlike 方块；移植版没有这层抽象 ⇒ **记录在案的差异**）。
* 保留差异：GT6 的水方块是 `BlockWaterlike extends BlockFluidClassic`，另外还有自己的流速、`updateTick` 里的草→砂土转变、沼泽刷史莱姆、`PLACEMENT_ALLOWED`/`UPDATE_TICK` 静态标志等；移植版用 Forge 的 `LiquidBlock`，**只搬运世界生成阶段的替换**。

### 49.2 测试（新增 `WaterBodyTests` 3 项）

| 测试 | 断言 |
|---|---|
| `waterBodiesMatchGt6Table` | 三条 pass、**顺序 = ocean → river → swamp**、登记名 = `ocean.seawater` / `river.riverwater` / `swamp.dirtywater`；三个世界方块都已注册、都是 `LiquidBlock`、**默认状态是 source 且流体 id 与自己同名**（`gregtech:seawater` 等）；特征 id 与 `gt_water_bodies.json` 已加载 |
| `biomeGatesMatchGt6` | 海洋群系跑海洋 pass 而**海滩不跑**（GT6 的 `BIOMES_OCEAN` 不含海滩）；河流群系跑河流 pass 而**同时沾海洋的区块不跑**；1.20.1 两个沼泽群系都跑；平原什么都不做；**同时沾河流与沼泽的区块由沼泽胜出**（顺序语义）；`OCEAN` = `OCEAN_BEACH` 去掉 4 项（9 / 13） |
| `columnConversionMatchesGt6` | 海洋柱：5 格水**整列**变海水、海底不透明方块保留；干燥柱（顶格是石头）**立即停止、无副作用**；非水非不透明格（海带）**跳过不换**而它下面的水照换；沼泽柱：**已换成海水的格子也会被换成沼泽水**，而普通 pass 对它无事可做 |

### 49.3 结果

| 指标 | §48 | §49 |
|---|---|---|
| GameTest | 394 | **397**（新增 3 项，全绿） |
| 世界流体方块 | 6（泉） | **9**（泉 6 + 海水/河水/沼泽水） |
| 海/河/沼泽水体 | 原版水 | **GT6 的 sea/river/swamp water**（按 GT6 顺序、整列替换） |
| 缺失 token | 49 | **49** |

**门禁插曲**：首次运行在 15:09 之后**日志停更 12 分钟**（JVM 3.5 GB 堆、CPU 仍在跑）＝记录过的环境性堆压力（同时在跑 Minecraft）；终止陈旧 `gameTest` JVM 后**未改代码**重跑即全绿（394 → 397 的增量与新增 3 项吻合）。

**没有执行 `git commit`。**

---

## 48. 续批：河里的黑磁砂（`WorldgenBlackSand`）与沼泽的泥炭（`WorldgenTurf`）——顺手拆掉一个自创的黑砂生成器

GT6 的这两个世界生成对象（`Loader_Worldgen:581-582`：`new WorldgenBlackSand("river.magnetite", T, GEN_OVERWORLD, …)` 与 `new WorldgenTurf("swamp.turf", T, GEN_OVERWORLD, …)`）是**跟黏土大坑共用同一套"挖地"骨架**的：同一个 48×48 掩码（`WorldgenPit.SHAPE`）、同一个锚点（区块起点前 16 格）、同一个竖向窗口（`waterLevel + 1` 到 `waterLevel - 12`），只是**每列最多 2 格**、宿主判定与产物不同。

| | 黑磁砂 `WorldgenBlackSand`（`river.magnetite`） | 泥炭 `WorldgenTurf`（`swamp.turf`） |
|---|---|---|
| 掷点 | `nextInt(64) > 0` 就返回（约 **1/64 区块**） | `nextInt(32) > 0` 就返回（约 **1/32 区块**） |
| 群系 | 区块内**任一**群系是**河流**才做；只要有**海洋/海滩**或**沼泽**就拒绝（GT6 拿到的是整区块的群系名字集合 `aBiomeNames`） | 区块内任一**沼泽** |
| 产物 | `BlocksGT.Sands`，meta ＝ **每个区块区域一次**噪声采样 `noise(minX/4, 360, minZ/4, 3)`（0 黑磁砂 / 1 玄武岩黑砂 / 2 花岗岩黑砂） | `BlocksGT.Diggables` **meta 2 = Turf**（材料 Peat） |
| 每列宿主 | 已经是本 meta 的黑砂（计数继续）；非实心格（已开始就停、否则跳过）；**dirt(m<2)/砂砾/沙/黏土/沙矿系列**；其余：已开始时只有**岩石**能继续，否则跳过 | 已经是 Turf；非实心格同左；**只有原版 dirt**；其余：已开始时只有**岩石**能继续，否则跳过 |
| 例外 | 坑还没开始时，**上一格是木头/瓜类**（`Material.wood`/`gourd`）就跳过——树下的土不动 | 同左 |

### 48.1 移植

* 新 `worldgen/GTWorldgenBiomes.java`：GT6 的三个群系名集合（`BIOMES_RIVER` / `BIOMES_OCEAN_BEACH` / `BIOMES_SWAMP`，映射到 1.20.1 的原版群系 id）＋ `chunkBiomes(...)`。**GT6 免费拿到的 `aBiomeNames`（1.7.10 区块自带 16×16 群系数组）**在 1.20.1 要自己取：现代群系按区块内 4×4×4 格存储 ⇒ 采样 `(minX+4k+2, minZ+4l+2)` 这 16 个格心**恰好**就是该区块的 16 个群系单元。
* 新 `worldgen/GTBlackSandFeature.java`：掷点 → 群系门槛 → 噪声选沙种（复用移植版已有的 `GTCellNoise` = GT6 `NoiseGenerator` 的移植）→ 逐列搬运 GT6 的判定（`carveColumn`，public 供测试直接驱动）。3 种黑砂用移植版既有的 `gregtech:sand_magnetite` / `sand_basalt_magnetite` / `sand_granite_magnetite`。
* 新 `worldgen/GTTurfFeature.java`：同上，产物是移植版既有的 `gregtech:turf`（＝ GT6 `Diggables` meta 2）。GT6 的 `tBlock == Blocks.dirt` 原样对应 `minecraft:dirt` + `coarse_dirt`（不用 `#minecraft:dirt` 标签，避免把灰化土等算进去）；窗口结束是**严格大于** `waterLevel - 12`（GT6 这里写的是 `tY > tLowerBound`，黑砂那边是 `>=`，照抄）。
* `GTPitFeature` 的两个私有判定（`isSandLike` / `isRock`）改为 public，由这两个新特征复用，避免三份"1.7.10 `Material.rock`"的近似各写一遍。
* **顺手拆掉重复**：老的 `GTSurfaceDepositFeature`（自创的"地表沉积"）里还有一整段**海滩/沙漠黑砂**，而 GT6 的黑砂**只在河流**里生成 ⇒ 世界里会有第二个不按 GT6 规则出黑砂的生成器。现在黑砂独家归 `GTBlackSandFeature`，沉积特征只管水边的黏土缝（与 §39 拆掉重复木棍同一类问题）。
* `tools/wire_black_sand_turf.py`（新）：注册 `GTFeatures.BLACK_SAND` / `GTFeatures.TURF` + 各 3 个数据文件（`top_layer_modification` 步）。

### 48.2 测试（新增 `BlackSandTurfTests` 4 项）

| 测试 | 断言 |
|---|---|
| `blackSandAndTurfMatchGt6Tables` | 掷点 64 / 32、窗口 +1/−12、每列 2 格上限、3 种沙与噪声高度 360、`gregtech:turf` 与三种黑砂都已注册、两个特征 id 与两个配置特征文件已加载、三个群系集合内容正确（河流/沼泽互不重叠、海洋海滩含 beach/snowy_beach/stony_shore/deep_ocean/ocean）、`chunkBiomes` 取到 1~16 个**已注册**群系 |
| `blackSandCarvesRiverColumns` | 沙柱：上面两格变黑砂、**第三格仍是原版沙**（2 格上限）；纯石柱：**一粒都不放**（GT6 未开始时岩石不算宿主）；沙→石：已开始后**岩石也能继续**；树干下的土：**保留**，其下两格变黑砂 |
| `blackSandVariantIsTheGt6Noise` | 400 个区块位置逐一与 `new GTCellNoise(seed).get(minX/4, 360, minZ/4, 3)` **逐个复算相等**（且确定性），**三种沙都取得到**；共用掩码仍是 48×48 |
| `turfCarvesSwampColumns` | 土柱：上面两格变泥炭、第三格仍是土；树干下的土保留、其下变泥炭；**砂砾会中断**（不是宿主也不是岩石）；**石头不中断**（已开始的泥炭能穿过岩石），但仍是两格上限 |

### 48.3 结果

| 指标 | §47 | §48 |
|---|---|---|
| GameTest | 390 | **394**（新增 4 项，全绿） |
| 河/沼泽世界生成 | 无（两个特征从未移植） | **黑磁砂（1/64 河流）+ 泥炭（1/32 沼泽）**，与黏土坑共用掩码与窗口 |
| 重复生成器 | 曾有两个黑砂来源 | **黑砂独家归 `GTBlackSandFeature`** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 47. 续批：GT6 的钶钽矿区（`WorldgenColtan`）——全世界只有一个中心点

GT6 的 `WorldgenColtan`（登记行 `Loader_Worldgen:779`：`new WorldgenColtan("ore.special.coltan", T, 20, 40, 32, 480, GEN_OVERWORLD, GEN_GT, GEN_PFAA, GEN_TFC)`，注释写着 "Special Bedrock Ore Generator just for HBMs Coltan"）是钶钽铁矿的**争夺区**：一张主世界地图**只有一个中心点**，由世界种子决定（`new Random(seed + 5).nextGaussian() * 1500` 连取两次当 x 与 z）；中心 **480 格**内**每个区块**都撒小撮（`WD.setSmallOre`）的 Coltan/Columbite/Tantalite，中心 **64 格**（`64*64 = 4096`）内**再加一轮**整块矿（`WD.setOre`）——**不是替换而是两个独立循环**，两轮都用同一个 `max(1, 32/2 + rand(33)/2)` 次数与同一个 `nextInt(5)` 材料表，高度一律 `y = 20..40`；并且**中心所在的那个区块**在撒矿之前先调 `WorldgenOresBedrock.generateVein(MT.OREMATS.Coltan, …)` 生成一条钶钽铁矿的基岩矿脉（＝整个矿区指向的东西）。

### 47.1 移植

* `worldgen/GTColtanFeature.java`（新）：`centre(long seed)` 逐字照抄 GT6 的中心点公式；`materialFor(roll)` ＝ GT6 的 `switch (aRandom.nextInt(5))`（0→Columbite、1→Tantalite、default→Coltan）；`count(random, amount)` ＝ GT6 的 `Math.max(1, amount/2 + random.nextInt(1+amount)/2)`；`place` 只认主世界维度；`distanceSquared` / `inRange` / `isCentreChunk` 三个纯函数分别对应 GT6 的三处判定；`scatter(...)` 就是被调用**两次**的那个循环体（`small=true` 小撮矿 / `small=false` 整块矿）；Y 走移植版的深度重映射（`GTWorldgenScale.remapY`：20 → −33、40 → −1，矿仍落在同一地质带内）。
* 中心区块的基岩矿脉交给移植版既有的基岩矿模型：`GTBedrockOreFeature` 的矿体部分抽成 public 的 `placeDeposit(level, random, centerX, centerZ, material)`（原来的 `place` 改为调用它，行为不变），`GTColtanFeature.place` 在 `isCentreChunk` 为真时按 GT6 的 `aMinX+6+rand(4)` / `aMinZ+6+rand(4)` 调它。**保留差异**：移植版的基岩矿是"世界地板附近的团块 + 基岩本体变成背景矿"，GT6 是 `y = 1..6` 的 7 层"松饼"形矿体；GT6 的 `GENERATED_NO_BEDROCK_ORE` 全局互斥标志未移植。
* `tools/wire_coltan.py`（新）：注册 `GTFeatures.COLTAN`（`gt_coltan`）+ `configured_feature` / `placed_feature` / `forge:biome_modifier` 三个数据文件（`underground_ores` 步，主世界全群系）。

### 47.2 测试（新增 `ColtanTests` 4 项）

| 测试 | 断言 |
|---|---|
| `coltanFieldMatchesGt6Constants` | 20 / 40 / 32 / 480 / 64 / 5 六个常量 ＝ GT6 登记行、特征 id ＝ `gt_coltan`、`gt_coltan.json` 已加载 |
| `centreIsSeedDerived` | 中心点对种子**确定性**、400 个种子**无重复**、`new Random(seed+5).nextGaussian()*1500` **逐字复算相等**（±1500 之外应约 32%：实测 > 100/400 ⇒ 确实乘过 1500）、x 与 z 是两次独立取样（相等次数极少） |
| `materialTableAndCount` | 5 路材料表 ＝ GT6（3 份 Coltan + 1 Columbite + 1 Tantalite）**且三种矿的整块与小撮方块都已注册**；`count` 在 2000 次采样里恰好落在 GT6 的 `[16, 32]` 区间，`amount = 0` 被 `max(1, …)` 兜住为 1 |
| `fieldScattersSmallOresAndFullOresNearCentre` | 480 格外**一粒都不放**；480 内、距中心 200 格处**只有小撮矿**（整块 0）；中心区块**小撮与整块都存在**（＝GT6 的两个循环，这一条是写完初版才发现的偏差：第一版把整块当成"替换小撮"，核对原版后改成两轮）；`isCentreChunk` 按**区块坐标**而非方块距离；整批矿的 Y 全在重映射后的带内 |

### 47.3 结果

| 指标 | §46 | §47 |
|---|---|---|
| GameTest | 386 | **390**（新增 4 项，全绿） |
| 主世界钶钽矿区 | 无（该特征从未移植） | **种子中心点 + 480 格小撮矿区 + 64 格整块 + 中心区块基岩矿脉** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 46. 续批：下界石英回到 GT6 的算法（两条噪声层，不是满高零散分布）

移植版的下界石英生成器（`GTNetherDepositFeature.placeSeams`）此前是**自己的近似**：在整个 `y = 8..120` 上做"区域噪声门控 1/24"，结果是**满高零散分布**的石英。GT6 的 `WorldgenNetherQuartz`（`Loader_Worldgen:600`）短得多也精确得多：逐列取**两个固定高度**的噪声采样——`40 + noise(x, 0, z, 200)` 与 `40 + noise(x, 64, z, 200)`，凡是落在下界岩上的就换成它的下界石英矿石（`RockOres` meta 8）⇒ 形成**两条很薄的、跨越区域的石英层**。

### 46.1 移植

* `GTNetherDepositFeature.placeSeams` 改为 GT6 的公式：`QUARTZ_BASE_Y = 40`、`QUARTZ_OPTIONS = 200`、`QUARTZ_NOISE_Y = {0, 64}` 三个常量＋逐列两次采样（噪声用移植版已有的 `GTCellNoise`——它就是 GT6 `NoiseGenerator` 的移植，含 256 格偏移表）；矿石仍是 `gregtech:block_ore_netherquartz`（对应 GT6 的 `RockOres` meta 8）。

### 46.2 测试（新增 `NetherQuartzTests` 2 项）

| 测试 | 断言 |
|---|---|
| `quartzUsesGt6NoiseLayers` | 基高 **40**、噪声选项 **200**、采样高度**恰好 0 与 64**（＝ GT6 的两个 `tY`） |
| `quartzReplacesNetherrackAtTheNoiseHeights` | 在 GT6 的两个噪声高度铺好下界岩后调用该 pass：**这些高度上确实出现 `block_ore_netherquartz`**（逐格核对，`found > 0`） |

（编译期两处小坑：`GameTestHolder` 的正确包是 `net.minecraftforge.gametest`；`FeaturePlaceContext` 不能自己 new，改成把 seam pass 设为 public 直接调用。）

### 46.3 结果

| 指标 | §45 | §46 |
|---|---|---|
| GameTest | 384 | **386**（新增 2 项，全绿） |
| 下界石英 | 满高零散（自创近似） | **GT6 的两条固定噪声层** |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 45. 续批：GT6 的施工泡沫（湿泡沫会自己干 + 刮掉不留东西）

移植版早就有 GT6 的两个泡沫方块（`cfoam`＝硬化、`cfoam_fresh`＝湿）、CFoam 流体与染色链（`DyeProcessingRecipes`），但**两个方块都只是装饰性的普通 `Block`——湿泡沫永远不会干**，而 GT6 的湿泡沫是要自己硬化的。

### 45.1 GT6 的做法（逐条核对原版）

| 环节 | 原版做法 |
|---|---|
| 干燥时机 | `MultiTileEntityCFoam.onTick2`：`aTimer >= 100 && !mFoamDried && rng(5900) == 0` ⇒ **前 100 刻不干**，之后**每刻 1/5900**（平均约 5 分钟） |
| 手动干燥 | `dryFoam(byte, Entity)`：`mFoamDried` 置真并同步（供喷枪/烘干工具调用） |
| 干湿差异 | 湿的：光照透明度同水、不可站立表面、硬度取 `BlocksGT.CFoamFresh`；干的：不透明、实心、硬度取 `BlocksGT.CFoam`（贴图也换成 hardened） |
| 刮掉 | `removeFoam`：**直接设为空气**（湿泡沫被刮掉不留任何掉落物） |
| 归属 | `mOwnable`/`mOwner`（喷在自己领地用的泡沫，只有主人能改）——移植版暂无领地系统，未移植 |

### 45.2 移植

* `block/misc/CFoamBlock`：加 `fresh` 标志 → 湿泡沫实现 `EntityBlock`（`newBlockEntity` + 服务端 `getTicker`），并实现 GT6 的掉落规则——**湿泡沫 `getDrops` 为空**（＝刮掉不留），硬化泡沫掉自己。
* 新 `blockentity/CFoamBlockEntity`：GT6 的**100 刻宽限 + 每刻 `rng(5900)`** 照搬（`GRACE_TICKS = 100`、`DRY_CHANCE = 5900`），`dry()` 用 `CFoamBlock.hardened()`（`gregtech:cfoam`）替换自身（＝ GT6 的 `mFoamDried` + hardened 贴图）；重复调用是空操作（同 GT6 的 `if (mFoamDried) return F`）。
* 注册：`cfoam_fresh` 改为 `new CFoamBlock(true, …)`，新 BE 类型 `cfoam`。

### 45.3 测试（新增 `CFoamTests` 3 项）

| 测试 | 断言 |
|---|---|
| `freshFoamIsABlockEntity` | `cfoam_fresh` 是湿的、`cfoam` 是干的；放置湿泡沫生成 BE；BE 类型覆盖该方块；`GRACE_TICKS == 100`、`DRY_CHANCE == 5900` |
| `foamDriesIntoHardenedFoam` | **宽限期 100 刻内绝不干**（逐刻调用后仍是湿的、计时器数到 100）；随后 `dry()` 把它换成 `gregtech:cfoam`、BE 记住已干、**二次调用无效** |
| `wetFoamDropsNothing` | **湿泡沫掉落为空**（GT6 的 `removeFoam` 刮掉不留）；硬化泡沫掉自己一件；`isFresh()` 能区分两种方块 |

### 45.4 结果

| 指标 | §44 | §45 |
|---|---|---|
| GameTest | 381 | **384**（新增 3 项，全绿） |
| 施工泡沫 | 湿的永不干（装饰块） | **100 刻后每刻 1/5900 自动硬化 + `dry()` 手动硬化 + 刮掉不留** |
| 方块 / 物品 | 11,983 / 69,729 | 同（升级既有方块，不新增注册） |
| 缺失 token | 49 | **49** |

**保留的差异**：GT6 的 `mOwnable`/`mOwner`（泡沫归属）依赖它的领地系统，移植版未移植；GT6 用"干/湿两套贴图 + `mFoamDried` 客户端同步"，移植版用**两个方块**（`cfoam_fresh` → `cfoam`）实现，效果一致。**没有执行 `git commit`。**

---

## 44. 续批：GT6 的流体泉（油田/气田/地热泉/岩浆泉）+ 给泉流体补上"世界流体方块"

这条链一直卡在一个前提上：**移植版从来没有"能放在世界里的流体方块"**——`Loader_Fluids` 给 925 种 GT6 流体都建了 `ForgeFlowingFluid` 的 source/flowing，但**一个 `LiquidBlock` 都没挂**，所以任何 GT 流体都无法在世界里存在，`MultiTileEntityFluidSpring` 也就无从移植。这批先把这个前提补上，再做泉本身。

### 44.1 GT6 的做法（逐条核对原版）

| 环节 | 原版做法 |
|---|---|
| 登记 | `Loader_Worldgen:782-788`：**7 条**地表泉——4 种油（特重/重/中/轻，**1/400**、指示草类型 2、6000 mB）、天然气（**1/200**、类型 1、3000 mB）、地热水（**1/100**、类型 3、500 mB）、岩浆（**1/200**、类型 1、1000 mB） |
| 前置 | 区块中心 y=0 必须是**基岩**，且该区块**没有基岩矿**（`WorldgenOresBedrock` 的静态标志） |
| 火山口 | `for (i = 0; i <= 6; i++)`：在 `y = i+1` 用石头/深板岩/下界岩填充**非实心**格；`i > 0` 时把 `y = i` 整片设为**泉的流体方块**；方形每层**内缩 1 格**（`aMinX+i .. aMaxX-i`）⇒ 6 层流体 + 1 层填充 |
| 泉本体 | 在每个**基岩柱**上以 **1/16** 概率放一个 `MultiTileEntityFluidSpring`（`i > 2` 的行才判） |
| 出液 | `onTick2`：上方空出来就**激活**，之后每刻以 **`rng(mFluid.amount) == 0`** 的概率把流体往上推（同种流体则抬高液面，否则放一个源方块）；上方是别的方块就不动 |
| 找矿 | 地表在 8×8 范围内撒 **6 处指示草**：`mIndicatorType == 3 ? 0 : 3+mIndicatorType` ⇒ GT6 草 meta **4（黄）/ 5（棕）/ 0（普通绿）**＝"气·岩浆黄、油棕、地热水普通" |

### 44.2 移植

* **世界流体方块**（新工具 `tools/register_spring_fluid_blocks.py`）：`Loader_Fluids` 里给这 6 种泉流体补 `LiquidBlock`（`GTBlocks.BLOCKS.register(path, …)`），方块与流体**互相惰性引用**（`AtomicReference` + `Properties.block(...)`，两边都不会在构造期解引用）；资产 = 6 个 blockstate（水方块标准的 `level=0..15`）+ 12 个模型（复用 GT6 流体贴图，无 `_flow` 贴图则侧/顶共用）+ 12 条双语键。
* `FluidSpringBlock` + `FluidSpringBlockEntity`：GT6 的**激活 + `1/amount` 出液**照搬（`emit()` 把流体放到上方，同种流体抬高液面）；`fluid_spring` 从装饰方块升级为 BE 方块（新 BE 类型 `fluid_spring`）。
* `worldgen/GTFluidSpringsFeature.java`：7 条登记（概率/指示类型/容量照抄）、基岩与"无基岩矿"前置、**火山口 `i = 0..6` 内缩方形**（流体层 `i>0`、填充层 `i+1`）、基岩柱 1/16 放泉、地表 6 处**指示草**（黄/棕/普通绿）、以及 `gt_fluid_springs` 的配置/放置特征 + 生物群系修饰符（`underground_decoration`）。

### 44.3 测试（新增 `FluidSpringTests` 4 项）

| 测试 | 断言 |
|---|---|
| `springsMatchGt6Table` | **7 条**泉、概率 400/400/400/400/200/100/200、指示类型 2/2/2/2/1/3/1、容量 6000×4/3000/500/1000；每条泉的**流体与流体方块都已注册且方块是 `LiquidBlock`**；火山口 7 层循环、1/16 密度、特征与配置特征已注册 |
| `fluidBlocksPlaceTheirFluid` | 逐条泉放置 → 方块是 `fluid_spring`、BE 存着对应流体与容量、`emit()` 后**上方真的出现该流体的流体状态**（7 种全部） |
| `craterFollowsGt6` | 造一块基岩地板 + 石头柱：`carve()` 后 `y=base+1` 是泉的方块、**中心到达 `i = 6` 最深层**、**每层内缩 1 格**（角落不是流体）、`hasBedrock()` 认得地板 |
| `indicatorGrassFollowsGt6` | 三种指示草分别解析成 `grassblock_yellow`/`grassblock_brown`/`grass` 且互不相同；`fluid_spring` 的 BE 类型覆盖该方块 |

（两个测试自写的坑：①七条泉最初用 `name.hashCode() & 7` 取位置，撞位的泉被上一条的流体挡住 ⇒ `emit()` 返回 false，改成逐条分配位置并清空上方；②火山口的"最深层"我按 7 层写，GT6 的循环是 `i = 0..6` ⇒ **流体层到 `base+6`**，断言改为与 GT6 一致。）

### 44.4 结果

| 指标 | §43 | §44 |
|---|---|---|
| GameTest | 377 | **381**（新增 4 项，全绿） |
| 世界流体方块 | **0** | **6**（4 种油 + 天然气 + 地热水；岩浆用原版） |
| 地表生成 | 树/倒木/木棍/发光菇/浆果丛/大坑/下界散落物 | **+ 7 种流体泉（火山口 + 泉 + 指示草）** |
| 缺失 token | 49 | **49** |

**保留的差异**：①GT6 的泉流体是 1.7.10 的"有限流体方块"（不会无限复制），移植版用 Forge 的水式流体（`canConvertToSource` 默认 false ⇒ 不会自我复制，但会像水一样流动/铺开一层）；②火山口宽度按 16×16 区块（GT6 的 `aMinX..aMaxX` 就是区块边界）✓ 一致；③GT6 的"该区块无基岩矿"标志在移植版没有对应状态，改成只判基岩。**没有执行 `git commit`。**

---

## 43. 续批：GT6 的「Did you know...?」提示页签（`RM.DidYouKnow` / `Loader_Recipes_Hints`）

GT6 把它的**机制提示页**放在一个只读的配方页签里（NEI 的 "Did you know...?"）：`Loader_Recipes_Hints` 用 `RM.DidYouKnow.addFakeRecipe(...)` 逐条登记，每个材料用 `ST.make(stack, "文字")` 带上说明（"Wait until it melts into Mercury"）。移植版**早就有这张表**（`MachineRecipeMaps.DidYouKnow`，内部键仍是 GT6 的 `gt.recipe.other`、别名 `Other`），但**一条提示行都没有**。

### 43.1 移植

* `GTMainRecipes` 的行表机制（规格字符串 + 解析失败即跳过并记录原因）直接复用：`map()` 增加 `"DidYouKnow"` 分支指向既有表（**不需要新声明**——第一版误加了一份重名声明，已撤销）。
* **材料标签**：移植版的行是纯规格字符串，没有 GT6 `ST.make(stack, "文字")` 的位置 ⇒ 给规格加了 `|<标签>` 后缀（`items()` 解析时拆开并 `setHoverName`；`firstMissing()` 用 `stripLabel` 后再判可解析性），这样提示行在 JEI 里也有说明文字。
* 5 条 GT6 提示里落地 **4 条**（都是移植版真实存在的链条）：
  1. **朱砂 → 水银**：`i:dust:Cinnabar:3` + `tech:clay_crucible` + `v:glass_bottle`（右键坩埚）= `tech:mercury_bottle`（`Loader_Recipes_Hints:38`）；
  2. **铁 → 钢**（坩埚吹气）：铁锭 + 坩埚 + 锻铁锭 = 钢锭/粉/板/棒/齿轮（`:44`）；
  3. **镀锌钢**（坩埚 → 水龙头 → 浸洗盆）：锌锭 + `tech:clay_faucet` + 钢板，熔融锌 `m:Zn:144` 进出 = 镀锌板/棒/螺丝（`:50`）；
  4. **打印机 → 手册**：`v:book` + 黑色化学染料 = `book:Manual_Printer`（`:56`）。
* 第 5 条（放大镜扫描方块）没有落地：移植版的放大镜是**工具物品**（`tool.magnifying_glass`），不在 `tech:` 规格覆盖的物品表里，规格无法解析 ⇒ 按机制"记录在案的跳过"处理（不硬编造）。

### 43.2 测试（新增 `RecipeHintTests` 3 项）

| 测试 | 断言 |
|---|---|
| `hintPageMatchesGt6` | 页面就是 GT6 的那张表（内部键 `gt.recipe.other`、名称 "Did you know...?"）、别名 `Other` 指向同一对象、已在 `RECIPE_MAP_LIST` 里（JEI 会自动出现）、槽位 6 物品 / 3 流体（＝ GT6 的声明） |
| `hintRowsAreDisplayOnlyAndLabelled` | 注册后表里**≥3 行**、**每一行都是 `mFakeRecipe`**（机器永不匹配）、每个材料非空且**都带 GT6 标签**、**0 行因内容缺失被跳过** |
| `hintsDescribePortChains` | 标签里能读到 GT6 的经典链条：Cinnabar、Mercury、Steel、Bathing Pot/Faucet |

（过程中踩的两个坑都记档：①移植版**已有** `DidYouKnow` 表，我多声明了一份导致编译失败 → 撤销；②第一版提示行用 `tech:<机器 id>` 猜移植版的机器方块 id，5 行全被跳过——移植版这些方块是按自己的命名/分层注册的 ⇒ 改成"材料/物品/流体用能解析的规格、机器名放进标签文字"。）

### 43.3 结果

| 指标 | §42 | §43 |
|---|---|---|
| GameTest | 374 | **377**（新增 3 项，全绿） |
| 提示页签 | 空表 | **4 条 GT6 提示**（朱砂→水银、铁→钢、镀锌钢、打印机→手册） |
| 配方行 | — | `GTMainRecipes` 行表新增 4 行（fake/display） |
| 缺失 token | 49 | **49** |

**保留的差异**：提示行里"机器"以**标签文字**出现（如 "Blow Air in with a running Engine"），展示的堆是材料/书/流体，因为移植版部分机器方块的 id 与 GT6 不同；放大镜那条按原版同样是"提示"，在移植版暂缺。**没有执行 `git commit`。**

---

## 42. 续批：GT6 的战利品箱（Loot Crate）——撬棍撬开，滚一次原版战利品表 + 归还箱子

移植版的 `loot_crate` 一直只是个**装饰方块**（`GTDecorBlocks` 里的普通 `Block`）。GT6 的 `MultiTileEntityLootCrate` 是个功能件：**用撬棍撬开**，掉落**一次随机原版战利品**（`ST.generateOneVanillaLoot()`）**外加箱子本体**（`IL.Crate.get(1)`）。

### 42.1 GT6 的做法（逐条核对原版）

| 环节 | 原版做法 |
|---|---|
| 触发 | `onToolClick(...)`：`aTool.equals(TOOL_crowbar)` 且服务端才生效（其它工具返回 0） |
| 结果 | `setToAir()` 成功后 `ST.drop(...)`：①`ST.generateOneVanillaLoot()` ②`IL.Crate.get(1)` |
| 原版战利品 | `ChestGenHooks.getOneItem(UT.Code.select("dungeonChest", LOOT_TABLES_VANILLA), RNGSUS)`——`UT.Code.select(默认, 列表)` 就是"**从列表里随机取一个**（空则用默认）"，即**随机一张原版宝箱表** |
| 提示 | `LH.TOOL_TO_OPEN_CROWBAR`（"用撬棍打开"） |

### 42.2 移植

* 新 `block/LootCrateBlock.java`：`use()` 里用 `GTToolHelper.matchesTool(held, GTToolType.CROWBAR)` 判定撬棍（服务端执行、非创造模式掉 1 点耐久），随后移除方块、**逐条 `popResource` 掉落**：随机原版表的掉落物 + `gregtech:crate`（GT6 的 `IL.Crate`，移植版由 iconset 注册的 `crate` 方块）✓。
* 原版表清单 = GT6 `LOOT_TABLES_VANILLA` 对应的 10 张 1.20.1 宝箱表，**地牢宝箱（`chests/simple_dungeon`）放第一位**（＝ GT6 的默认值）：simple_dungeon、abandoned_mineshaft、desert_pyramid、jungle_temple、jungle_temple_dispenser、stronghold_library、stronghold_corridor、stronghold_crossing、village/village_weaponsmith、spawn_bonus_chest。
* `1.20.1` 的 `LootTable` 用 `getRandomItems(LootParams)`（`LootContextParamSets.CHEST` + `ORIGIN`）——这是移植版第一处**主动滚原版战利品表**的代码。
* 新增提示键 `block.gregtech.loot_crate.tooltip`（双语，工具 `tools/add_loot_crate_lang.py`）。

### 42.3 测试（新增 `LootCrateTests` 3 项）

| 测试 | 断言 |
|---|---|
| `crowbarOpensTheCrate` | 撬棍右键**消耗掉箱子**、掉落里**必有 `crate` 本体**、且至少有 2 件掉落物（＝随机原版战利品确实滚出来了）、没有空栈 |
| `withoutACrowbarNothingHappens` | 拿木棍右键 → `PASS`、箱子还在、地上没有任何掉落物 |
| `vanillaLootRollsAlwaysGiveSomething` | 表清单 **10 张**且首项是 `chests/simple_dungeon`（GT6 的默认值）；40 个种子逐一验证：选出的表都在清单内、**每次滚表都非空**、没有空栈 |

（本轮又踩了一个小坑：`crowbarOpensTheCrate` 忘了写 `helper.succeed()` ⇒ 门禁报 "Didn't succeed or fail within 300 ticks"，补齐即绿。）

### 42.4 结果

| 指标 | §41 | §42 |
|---|---|---|
| GameTest | 371 | **374**（新增 3 项，全绿） |
| 战利品箱 | 装饰方块 | **撬棍可开**：随机原版战利品 + 箱子本体 |
| 方块 / 物品 | 11,977 / 69,723 | 11,977 / 69,723（`loot_crate` 从装饰升级为功能方块） |
| 缺失 token | 49 | **49** |

**保留的差异**：GT6 的箱子在生成时可能带 NBT（战利品表可配置？实际 GT6 是固定滚原版表），移植版同样固定；GT6 用 1.7.10 的 `ChestGenHooks` 名称，移植版映射到 1.20.1 的 10 张同名宝箱表。**没有执行 `git commit`。**

---

## 41. 反馈修复：自然生成的树**树叶会自己消失**——补上 GT6 的树叶判断 + 补上缺失的木头标签

**报告**："自然生成的树的树叶会自己随时间消失，没有加上原版那样的判断。"

### 41.1 根因（两处叠加，都在移植版这一侧）

1. **GT6 的树叶判断从没实现**：GT6 `BlockBaseLeaves.updateTick2` 的规则是——先把 `meta < 8`（"不腐烂"标志）的叶子直接放行；否则**按树种在固定范围内扫同种原木**（`BlockTreeLeavesAB/BlockTreeLeavesCD.getLeavesRangeSide/YNeg`，`YPos` 恒为 0，因为 GT6 的树不会把叶子放在树干下方），**找到才保留**，找不到才掉一个树苗（1/50，可可 2/50）并消失；`beginLeavesDecay` 对 `meta < 8` **连检查都不排**。移植版直接继承了原版 `LeavesBlock` 的"`distance == 7` 就烂"逻辑。
2. **移植版的原木不在 `#minecraft:logs` 里**：原版 `LeavesBlock.updateDistance` 只把 `BlockTags.LOGS` 里的方块算作"到原木距离 0"，GT6 的原木没进标签 ⇒ **每片叶子的 `distance` 永远是 7** ⇒ 原版判定它们"该烂"，随机刻一到就掉。数据包里也**没有** `#minecraft:leaves`。

### 41.2 修复

* `block/wood/WoodLeavesBlock` 重写 `randomTick`，**按 GT6 的规则判定**：
  * `persistent == true`（＝ GT6 的 `meta < 8`，例如玩家手放的叶子）→ **永不腐烂、连检查都不做**；
  * 否则用 `hasLogNearby()` 在该树种的 GT6 范围内找**同种原木**（范围表逐个照抄：橡胶 side 2 / 枫 3 / 柳 4 / 蓝桃花心 3 / 榛 3 / 桂皮 3 / 椰 4 / 彩虹木 3 / 蓝云杉 6；下垂半径 蓝桃花心 4 / 桂皮 3 / 椰 1 / 彩虹木 3 / 其余 2），找到就保留、找不到才 `dropResources` + 移除（掉落沿用 GT6 的树苗/木棍表）。
* 新工具 `tools/generate_wood_tags.py` 写出 **`data/minecraft/tags/blocks/logs.json`（12 种原木）与 `leaves.json`（12 种树叶）**——这样原版的 `distance` 传播、别的模组与移植版自己的 `BlockTags.LOGS/LEAVES` 判断都能正确识别 GT 的木头（也就是说：即便走原版那条路，树叶也不会再自己烂）。

### 41.3 回归测试（新增 `LeafDecayTests` 4 项）

| 测试 | 断言 |
|---|---|
| `standingTreeKeepsItsLeaves` | **12 个树种**各长一棵真树，对**它的每一片叶子**做 **200 轮**随机刻 ⇒ **一片都不能少**（正是反馈的场景） |
| `choppedTreeLosesItsLeaves` | 砍掉整根树干后再随机刻 ⇒ **所有叶子都烂掉**（GT6 的"没原木才掉"） |
| `persistentLeavesAndRangesMatchGt6` | `persistent` 叶子 500 次随机刻不掉；**范围表逐项等于 GT6**（橡胶 2/2、柳 4/2、椰 4/1、枫 3/2、蓝云杉 6/2、蓝桃花心 3/4、桂皮 3/3）；`hasLogNearby` 对"5 格外的叶子"为假、对旁边有原木为真 |
| `woodIsInVanillaTags` | 12 种原木都在 `#minecraft:logs`、12 种树叶都在 `#minecraft:leaves` |

### 41.4 结果

| 指标 | §40 | §41 |
|---|---|---|
| GameTest | 367 | **371**（新增 4 项，全绿） |
| 树叶行为 | 会自己消失（bug） | **站着就留、砍了才烂**（GT6 语义） |
| 木头标签 | 无 | `#minecraft:logs` / `#minecraft:leaves` 各 12 条 |
| 缺失 token | 49 | **49** |

**没有执行 `git commit`。**

---

## 40. 续批：GT6 的"地上散落物"扩展到下界（`WorldgenRacks`：宝石/燧石/古代残骸/岩石）

GT6 的 `WorldgenRacks`（`Loader_Worldgen:619`）名字里有 "Racks"，实际干的是**在**下界**地面上散落物品**——用的是和地表岩石同一个 multi-tile（32757"躺在地上的物品"）：下界石英/荧石**宝石**、燧石、古代残骸**原矿**、黑曜石/玄武岩/黑石**岩石**。移植版此前只有地表的岩石（矿脉指示）与木棍，下界地面上什么都没有。

### 40.1 GT6 的做法（逐条核对原版）

| 环节 | 原版做法 |
|---|---|
| 频率 | `if (aRandom.nextBoolean()) return F;` ⇒ **一半区块直接跳过**；每区块 **16 次**尝试 |
| 落点 | 随机列 → 从 `random.nextInt(有基岩?200:80) + 47` **向下最多 40 格**找第一块**实心**下界地面（跳过空气/植物、遇液体停）→ 上方必须是空气 |
| 1/24 表 | 按**脚下是什么方块**分派：**下界砖** → 1/4 古代残骸原矿、否则下界石英；**灵魂沙/灵魂土** → 阴郁石（Gloomstone）或下界石英宝石；**砂砾** → 燧石；其余 → 下界石英/荧石/黑曜石/玄武岩/黑石（岩石或宝石） |

### 40.2 移植

* `blockentity/RockBlockEntity` 增加 **item 模式**（对应 GT6 的 32757"躺在物品"）：存一个物品 id，`RockBlock` 的**掉落与右键拾取**都优先给这个物品（没有时仍是原来的 `rockGt` 逻辑）；`RockBlock` 的 `yields()` 增加该分支（`rawOre` 标记叠加不变）。
* 新 `worldgen/GTNetherScatterFeature.java`（`gt_nether_scatter`）：50% 跳过、16 次尝试、40 格射线、**1/24 表与三种脚下方块的特殊分派逐条搬运**；物品用移植版自己的材料物品（宝石走 `gem`、岩石走 `rockGt`、原矿走 `oreRaw`，燧石为原版 `minecraft:flint`），方块仍是 `rock`，`GROUND` 状态按脚下（下界岩）取外观。
* 数据包：`gt_nether_scatter` 配置/放置特征 + 生物群系修饰符（`#minecraft:is_nether`、`underground_decoration`）。

### 40.3 测试

新增 `gametest/NetherScatterTests`（3 项）：

| 测试 | 断言 |
|---|---|
| `scatterTableMatchesGt6` | 1/24 表长 24、16 次尝试、40 格射线、GT6 的 6 种材料在移植版都能解析、`gt_nether_scatter` 特征与配置特征已注册 |
| `scatterSpecialCases` | **下界砖**：12 次掷点里必然出现古代残骸原矿（1/4）与下界石英、且不会出现别的组合；**砂砾**：物品就是 `minecraft:flint`；**灵魂沙**：阴郁石或下界石英 |
| `groundItemYieldsItsItem` | item 模式往返（BE 存取 + 解析成物品栈）、**破坏掉落的是该物品**（`getDrops` 走 LootParams）、方块外观取脚下的 `GROUND`（下界岩） |

**本轮门禁的插曲**：第一次运行在 `=====` 进度条处**卡死 10 分钟**（JVM 3.5 GB 堆、日志不再增长）——属于此前记录过的环境性堆压力（同时在跑 Minecraft），终止陈旧 JVM 后重跑即全绿；代码侧无需改动。**教训与之前一致：这类挂起先重试，不要先去改代码。**

### 40.4 结果

| 指标 | §39 | §40 |
|---|---|---|
| GameTest | 364 | **367**（新增 3 项，全绿） |
| 下界内容 | 红黏土带 + 晶体矿簇（§早期批次） | **+ 地面散落物**（宝石/燧石/原矿/岩石） |
| 方块 / 物品 | 11,977 / 69,723 | 11,977 / 69,723（复用 `rock` 方块，仅 BE 增加 item 模式） |
| 缺失 token | 49 | **49** |

**保留的差异**：①GT6 那张 1/24 表里"宝石"是**显示成宝石的物品**（32757 的渲染是物品本身），移植版统一用岩石方块的外观、但**掉落的物品正确**（下界石英/荧石宝石、燧石、原矿）；②GT6 的落点判定用材质类别（grass/ground/sand/rock），移植版用"实心方块"近似；③GT6 的 `checkForMajorWorldgen`（中心区保护）在移植版仍无对应系统。**没有执行 `git commit`。**

---

## 39. 续批：GT6 的沙/黏土坑（`WorldgenPit` 的 48×48 大坑）+ 顺手清掉一处重复的木棍生成

移植版从早期批次起就有"地表沉积"生成器（黑磁砂 + 小片黏土缝），但它**不是** GT6 的 `WorldgenPit`：原版那种横跨 3×3 区块、48×48 轮廓、围绕海平面上下开挖的**大沙坑/黏土坑**一直没有。这批补上，并顺手修掉一处重复生成。

### 39.1 GT6 的做法（逐条核对原版）

| 环节 | 原版做法 |
|---|---|
| 稀有度 | `WorldgenPit(name, default, block, meta, chance=1, divider=320, …)`：`nextInt(320) > 0` 就返回 ⇒ 约 **1/320 区块** |
| 群系 | 只看**区块中心的群系**：`BIOMES_PLAINS` 或 `BIOMES_SAVANNA` |
| 轮廓 | **48×48 的布尔掩码** `SHAPE`（2304 格里 **2032** 格属于坑），锚点设在**区块起点前 16 格** ⇒ 一个完整的坑横跨 3×3 区块 |
| 竖向 | 从 `海平面+16` 挖到 `海平面-8`；每列**最多 7 格** |
| 每列规则 | 已经是坑料的格子 → 计数继续；**非实心**格 → 坑已开始就停、否则跳过；`dirt` 是宿主，**但坑还没开始时若其上一格是木头/树叶则跳过**（树下的土不动）；非 dirt 的格子：坑还没开始时只有**沙/黏土/GT 的沙矿**算宿主（石头不算，继续往下找），坑已开始后**只有岩石/砂砾**能继续（否则停） |
| 坑料 | 7 条登记：原版黏土 + GT6 `Diggables` 的 **Brown(1)/Red(3)/Yellow(4)/Blue(5)/White(6) Clay**（红黏土默认关闭，PFAA 的属于别的模组） |

### 39.2 移植

* 新工具 `tools/generate_pit_shape.py`：**从 GT6 源码直接生成** `GTPitShape.java`（48 行 × 48 字符的掩码常量 + 2032 格计数），避免手抄错位。
* 新 `worldgen/GTPitFeature.java`：1/320 掷点、只看中心群系的平原/热带草原门槛、48×48 掩码（锚点在区块前 16 格）、`海平面 +16 / -8` 的竖向窗口、**每列逐条搬运 GT6 的判定**（坑料计数、非实心、`dirt` 的树下例外、坑前只认沙/黏土/沙矿、坑内只认岩石/砂砾、7 格上限）。
* 5 种坑料：`minecraft:clay` + `gregtech:clay_brown/yellow/blue/white`（红黏土在原版就是默认关闭，PFAA 属于别的模组，都跳过）。
* 数据包：`gt_pits` 的配置特征/放置特征/生物群系修饰符（`top_layer_modification`）。
* **顺手修掉重复**：老的 `GTSurfaceDepositFeature` 里还留着"树下放木棍"的代码，而 §35 已经把 GT6 的 `WorldgenSticks` 搬进 `GTSurfaceFloraFeature`（原版只有**一条**登记）⇒ 世界里曾同时存在两个木棍生成器。现在沉积特征只管黑砂与黏土缝，木棍由地表植被特征独家负责（`tools/fix_deposit_twigs.py` 精确删除并校验括号平衡）。

### 39.3 测试

新增 `gametest/PitTests`（3 项）：

| 测试 | 断言 |
|---|---|
| `pitsMatchGt6Table` | `(chance, divider) = (1, 320)`、竖向 `+16/-8`、每列 7 格上限、**5 种坑料**（红黏土默认关闭）、掩码 48×48/2032 格/四角在坑外/中心在坑内、平原能生而沙漠森林不能、特征与配置特征已注册 |
| `pitCarvesGt6Columns` | 沙柱被挖且**不超过 7 格**；**纯石柱不会起坑**（GT6 只在坑内穿岩石）；**木头正下方的土保留**（GT6 的 wood/leaves 例外） |
| `twigsHaveASingleOwner` | 木棍仍是 GT6 的方块，且 `WorldgenSticks` 的 (2, 2) 参数只属于地表植被特征 |

测试里踩到一个**自己写错的假设**：最初"石头柱"下面垫了 dirt，于是 GT6 的规则照样会从石头下面的土开始挖——按原版逻辑那是对的（石头只是被跳过，土仍是宿主），改成**纯石柱**后测试才表达出真正的不变量。

### 39.4 结果

| 指标 | §38 | §39 |
|---|---|---|
| GameTest | 361 | **364**（新增 3 项，全绿） |
| 世界生成 | 树/倒木/木棍/发光菇/浆果丛/矿脉/基岩矿… | **+ 48×48 沙黏土大坑** |
| 木棍生成器 | 2 个（重复） | **1 个**（与原版一致） |
| 方块 / 物品 | 11,977 / 69,723 | 11,977 / 69,723（复用既有黏土方块） |
| 缺失 token | 49 | **49** |

**保留的差异**：①GT6 的坑也接受别的模组的沙矿（`oreSand` 系列）与 PFAA 黏土，移植版只认沙标签/黏土/自己的 `sand_*` 图标方块；②1.7.10 的 `Material.rock` 用 1.20.1 的石头标签近似（含深板岩/黑曜石/石砖）；③GT6 每列最多 7 格、掩码跨 3×3 区块且**逐区块独立掷点**（因此原版也会出现半个坑），移植版保持一致。**没有执行 `git commit`。**

---

## 38. 续批：GT6 的浆果灌木丛（四阶段生长 + 树液/灌木链补完）+ 一条测试幂等性再修

§35 做地表植被时把 `WorldgenBushes` 列进了"还缺"的清单（丛林/平原的浆果丛是 GT6 的野生食物来源）。这批把灌木丛整条做出来。

### 38.1 GT6 的做法（逐条核对原版）

| 环节 | 原版做法 |
|---|---|
| 方块 | `MultiTileEntityBush`（multi-tile **32759**）：一个贴地（或贴侧面）的小丛，`mBerry` **就是它的类型**，`mStage` 0~3 |
| 四阶段 | 0 只有丛叶、1 开花（bloom 色）、2 青果（immature 色）、3 熟果（berry 色）——阶段对应贴图与**染色** |
| 颜色 | `BushesGT.put(浆果, 丛色, 花/青果色, 青果色, 熟果色)`，GT6 一共 **8 种**：blueberry / candleberry / cranberry / black_currants / white_currants / red_currants / blackberry / raspberry（`MultiItemFood:397-425`）；**没有 berry 的丛用占位洋红 `0xff00ff`**，未知浆果走 `DEFAULT` |
| 生长 | 每 **128 刻**一次：能见天 → 长 `mSpeed` 次，**下雨再加 `mSpeed` 次**；见不到天 → 光照 > 9 才长 `mSpeed` 次；`mSpeed` = 1（可种植地面）或 2（以太附魔草，移植版没有）；每 **256 次**（GT6 的 `byte` 溢出计数器 `if (++mGrowth == 0) mStage++`）推进一个阶段 |
| 收获 | 右键成熟（stage 3）→ 给 **1~2 个**浆果并把阶段重置为 0；空手/非浆果物品无反应 |
| 定类型 | 还没类型的丛，右键手里的**浆果**即可定种（GT6 接受 `plantGtBerry` 或 `BushesGT` 里的物品） |
| 世界生成 | `WorldgenBushes`（`Loader_Worldgen:633`）：每区块 **1** 个目标列、**1/4** 掷点，**平原/森林且非冰冻**群系；类型用 GT6 的**值噪声**在 `(x/2, 300, z/2)` 取模浆果数决定；再向四个水平邻居各 50% 扩散同种浆果 |

### 38.2 移植

* `content/plant/GTBerryBushes.java`：GT6 的 8 色表（`BushesGT` 的数值逐个照抄）＋ `stageColour()` ＝ GT6 `getRenderPasses2` 里那个 switch 的取色规则。
* `block/plant/BushBlock.java` + `blockentity/BushBlockEntity.java`：`stage` 状态位（0~3）、GT6 的 128 刻周期 / 256 次一阶段 / 雨天与光照加成、右键收获（1~2 个浆果并重置）、空丛定种、`mSpeed` 的地面判定。
* `worldgen/GTBushesFeature.java`：GT6 的 1/4 概率、平原森林非冰冻的群系门槛、**用移植版已有的 `GTCellNoise`**（它就是 GT6 `NoiseGenerator` 的移植，含 256 格偏移表）在 `(x/2, 300, z/2)` 选浆果，再向四个邻居扩散。
* 资产：新工具 `tools/generate_bush_assets.py` **从 GT6 资源包复制 6 张贴图**（`colored/{bush,berries,berries_immature}` + `overlay/…`），生成 4 个阶段模型（`forge:composite` 两层：染色层 tint 0 = 丛色 / tint 1 = 阶段色，叠加灰度细节层）+ blockstate + 物品模型 + 双语键；客户端在 `GregTechClient.registerBlockColors` 里按 BE 的浆果类型与阶段取色（无 BE 时用 GT6 的占位洋红）。
* `registry/GTBushes.java` 注册 `bush` 方块与物品，`GTBlockEntities` 增加 `bush` 类型。

### 38.3 测试

新增 `gametest/BushTests`（4 项）：

| 测试 | 断言 |
|---|---|
| `bushIsRegistered` | 放置即生成 BE、BE 类型覆盖、6 项资源在 jar 内；**8 种浆果**且 blueberry/cranberry 的颜色逐位等于 GT6 的 `BushesGT`；`stageColour` 与 GT6 的 switch 一致；石头不是浆果 |
| `bushGrowsOneStagePer256Increments` | 阶段 0→1 **恰好**用掉 256 次增量（雨天自动按 GT6 的双倍算）；**没有浆果的丛永不生长**；石头地面上 `mSpeed == 0` 且不生长 |
| `bushHarvestHandsOutItsBerries` | 成熟丛右键给 **1~2 个**该种浆果并把阶段重置为 0；未熟的丛无反应；空丛右键手里的浆果即可定种 |
| `bushWorldgenMatchesGt6` | (1, 4) 参数、平原/森林可生而雪原/沙漠不可、`gt_bushes` 特征与配置特征已注册、噪声选种**同坐标可复现**且索引落在 8 种之内 |

**又踩了一次"世界目录复用"**：空丛测试一开始失败，原因是上一轮运行在该坐标留下的丛**带着浆果类型与生长计数**——`placeBush` 现在先 `removeBlock`（销毁旧 BE）再放置，测试才真正幂等。另一个坑是 GT6 **只给 8 种浆果**做了灌木配色，`gooseberry` 不在其中（测试最初拿它当"任意浆果"用，属于测试写错）。

### 38.4 结果

| 指标 | §37 | §38 |
|---|---|---|
| GameTest | 357 | **361**（新增 4 项，全绿） |
| 方块 / 物品 | 11,976 / 69,722 | **11,977 / 69,723**（新增 `bush`） |
| 地表生成 | 树 / 倒木 / 木棍 / 发光菇 | **+ 浆果丛**（平原森林、噪声选种、四向扩散） |
| 缺失 token | 49 | **49** |

**保留的差异**：①GT6 的 `mSpeed = 2`（以太附魔草）移植版没有对应方块，恒为 1；②GT6 的丛可以贴在方块**侧面**（挂墙生长）并带动画式的 `side pieces`，移植版只做贴地丛；③GT6 的丛不覆盖 `getDrops`（挖掉掉的是丛方块本身），移植版一致。**没有执行 `git commit`。**

---

## 37. 续批：GT6 的树液袋（挂在树脂孔下方自动收集）+ 手取路径的自动化

§36 留了一条明确的差异：**"GT6 的树液袋（`MultiTileEntitySapBag`，挂在孔下方自动收集）还没移植，目前只能手取"**。这批把它补上，树上取液这条链就完整了：树自己长孔 → 玩家钻孔 → 挂袋自动收 → 桶/容器取走。

### 37.1 GT6 的做法（逐条核对）

| 环节 | 原版做法 |
|---|---|
| 方块 | `MultiTileEntitySapBag`，**挂/贴在方块的侧面**（`getValidSides() = SIDES_HORIZONTAL`，形状是一个贴边的小袋子；可扳手旋转） |
| 容量 | `new FluidTankGT(8000)`（＝ 32 次 250 mB）＋**一个物品槽**（橡胶孔产的是物品而不是流体） |
| 收集 | **每 tick** 检查它面对的那格：若是树孔且满 → `extractResin()` 清空孔，流体灌进自己的罐、物品放进槽；罐或槽非空时把 `mFull` 同步给客户端（换 full 贴图） |
| 取用 | 右键：**先**把槽里的物品交给玩家（`ST.add`），**再**把罐里的液体灌进玩家手上的容器（`FL.fill`） |
| 破坏 | `breakBlock()` → `GarbageGT.trash(mTank)`（罐里的液体丢掉），槽里的物品照常掉落 |

### 37.2 移植

* `block/tool/SapBagBlock.java`（原来 `sap_bag` 只是装饰用的 `ShapedToolBlock`）：改成 `implements EntityBlock` = `getTicker` + `newBlockEntity`，右键**先给物品再灌容器**（沿用 §36 的 `FluidUtil` 路径），破坏时 **罐里液体丢失（GT6 的 `GarbageGT.trash`）**、槽里的树脂照常掉出。扳手旋转等既有工具交互（`super.use`）保持不变。
* `blockentity/SapBagBlockEntity.java`：`FluidTankGT(8000)`（`CAPACITY = 8000`，对应 GT6 的 `FluidTankGT(8000)`）+ 一个物品槽；**每 tick** 检查朝向的那格（GT6 `getAdjacentTileEntity(mFacing)`）：是满的 `TreeHoleBlock` → 清空孔、按孔的种类收物品或 250 mB 流体；自身实现 `IFluidHandler`（GT6 的袋子本来就是普通储罐，所以管道可以直接抽走）。
* 注册：`sap_bag` 现在注册为 `SapBagBlock`（`GTToolBlocks.simple()` 分支 + 新增 `SAP_BAG` 静态字段），新 BE 类型 `sap_bag`。
* 顺带给 `GTBlockEntities` 加了 `SAP_BAG`，并让 `simple()` 返回 `RegistryObject` 以便 BE 类型引用。

### 37.3 测试

新增 `gametest/SapBagTests`（4 项，均在放置后等几 tick 再断言）：

| 测试 | 断言 |
|---|---|
| `bagIsRegisteredAsABlockEntity` | `sap_bag` 是 `SapBagBlock`、放置后生成 BE、BE 类型覆盖它、**服务端有 ticker**、容量 ＝ GT6 的 **8000 mB**、对外暴露 `IFluidHandler` |
| `bagCollectsFromTheHoleItFaces` | 枫木孔 → 袋里 **250 mB 枫糖浆**且孔被清空、物品槽不填；橡胶孔 → **树脂进物品槽**、罐不填、孔被清空；**袋背后**（非朝向）的满孔**不被动** |
| `bagHandsOutItsContents` | 右键先交出物品（玩家背包里出现 `rubber_resin`）、槽清空；再右键把罐里的 250 mB 灌进手上容器、罐清空 |
| `breakingTheBagTrashesTheTankAndDropsTheItem` | 破坏前槽清空（物品已掉出）、方块消失 |

**测试踩到的两个坑（都记在本节）**：①袋子是靠 **BE tick** 工作的，而 GameTest 里远离结构区的区块**不是 ticking chunk** ⇒ 断言必须等 tick，并且测试里显式调用一次 tick 方法保持确定性（`placePair` 同时对该区块 `setChunkForced`）；②"另一个方向上的孔不受影响"这条最初写错了坐标（把同一个方块当成两个孔），改成"袋**背后**的孔"后才真正验证了朝向语义。

### 37.4 结果

| 指标 | §36 | §37 |
|---|---|---|
| GameTest | 353 | **357**（新增 4 项，全绿） |
| 方块 / 物品 | 11,976 / 69,722 | 11,976 / 69,722（`sap_bag` 从装饰方块升级为功能方块，不新增注册） |
| 树液链 | 长孔 → 钻孔 → 手取 | **+ 挂袋自动收集 / 管道抽取** |
| 缺失 token | 49 | **49** |

**保留的差异**：GT6 的袋子有 `overlay_full` 贴图表示"已满"（移植版资源包里只有 `overlay`），所以**满/空在视觉上不区分**；液体本身按 GT6 语义在破坏时丢弃（物品不丢）。**没有执行 `git commit`。**

---

## 36. 续批：GT6 的树上取液——橡胶树脂孔 + 枫/彩虹木树液孔（含 GT6 的"树叶健康度"机制）

§34 在文档里留了一条明确的缺口：**"橡胶树干上没有 GT6 的树脂孔（`MultiTileEntityResinHoleRubber`），因此暂时无法从树上取树脂"**。这批把它补上，并把 GT6 另外两个树上取液玩法（枫糖浆、彩虹木树液）一起做完。

### 36.1 GT6 的机制（逐条核对原版）

| 环节 | 原版做法 |
|---|---|
| 三个方块 | 橡胶树脂孔 32762、采脂枫木 32761、采脂彩虹木 32760（`Loader_MultiTileEntities:2026-2028`） |
| 橡胶孔怎么来 | **树自己长出来**：`BlockTreeSaplingAB` case 0 在树干上第一处"上方还剩 ≥6 格树干"的位置打孔，每棵树最多一个，朝向随机水平面 |
| 枫/彩虹木孔怎么来 | **玩家用**手钻**在水平面钻**：`BlockTreeLogA:103`（枫，meta 1）、`BlockTreeLogB:104`（彩虹木，meta 3） |
| 多久出液 | 每 **600 刻（30 秒）**检查一次：沿树干向上走到树冠，按 GT6 **自己列的树冠坐标**数该树种的树叶，再掷 `rng(260/560/420) < 树叶数 − (60/250/250)` —— 理想树冠分别有 86 / 306 / 271 片树叶 ⇒ 完美树每 30 秒约 **10% / 10% / 5%** 的出货率；**树叶被砍光的树永远不出液** |
| 怎么取 | 右键：橡胶 → 给 `IL.Resin` 物品；枫/彩虹木 → 往**手上容器**灌 250 mB 的 `Sap_Maple`/`Sap_Rainbow`；取完孔变空、重新等下一次 |
| 挖掉掉什么 | 对应树种的**原木**（`getDrops`），不掉孔本身 |

### 36.2 移植

* 3 个新方块（`resin_hole_rubber` / `tapped_maple` / `tapped_rainbowood`，新 `registry/GTTreeHoles.java`）：带 GT6 的**水平朝向**与**是否出液**两个状态。出液状态放在 **blockstate** 里 ⇒ 客户端自动同步（GT6 是 BE 里的 `mHasResin` + `updateClientData`）。
* 新 `blockentity/TreeHoleBlockEntity.java`：GT6 的三个 `onTick2` **逐行搬运**——树干上行、三套不同形状的树冠坐标表、三档掷点与阈值。因为 §34 已经把 GT6 的树形搬过来了，这里数出来的树叶数就是 GT6 的树叶数。
* `GTTreeShapes` 的橡胶树形现在**在树干上打孔**（GT6 的位置规则），§34 的那条缺口关闭。
* `WoodLogBlock.use`：拿**手钻**（`GTToolType.HAND_DRILL`）右键枫木/彩虹木的**侧面** → 变成对应的采脂孔（顶/底面无效，等同 GT6 的 `SIDES_HORIZONTAL`）；钻会掉 1 点耐久。
* 右键取液：橡胶给 `rubber_resin`；枫/彩虹木用 `FluidUtil` 往手上容器灌 250 mB（任何能接受该流体的容器，与 GT6 的 `FL.fill` 同义）。
* 资产：新工具 `tools/generate_tree_hole_assets.py` 生成 3 个 blockstate（4 朝向 × 2 状态，用 `y` 旋转复用模型）+ 6 个模型（钻孔面用 GT6 的 `log_hole_*`/`log_resin_*`/`log_sap_*` 贴图，其余面用该树种的原木贴图）**全部复用 GT6 资源包贴图**，另加 6 条双语键。

### 36.3 测试

新增 `gametest/TreeHoleTests`（5 项）：

| 测试 | 断言 |
|---|---|
| `holesAreRegistered` | 3 个方块的注册 id、GT6 的 facing/resin 状态、BE 类型覆盖、blockstate/模型都在 jar 资源里；产出＝`rubber_resin` 物品与 250 mB 树液 |
| `rubberTreesGrowAResinHole` | 4 棵自然长成的橡胶树**每棵恰好一个**树脂孔、朝向水平、初始为空、不在树干最底部 |
| `holesRefillOnlyFromHealthyTrees` | 健康树按 GT6 的坐标表数出 **> 60** 片树叶并最终出液（状态写进 blockstate）；**砍光树冠后**树叶数掉到阈值以下且 400 次检查都不出液 |
| `harvestingYieldsResinAndSap` | 空手右键满孔得到 `rubber_resin` 且孔变空、再右键无反应；枫木孔把 **250 mB 枫糖浆**灌进手上容器（自动挑一个能接受该流体的容器）并把孔清空 |
| `drillingTapsMapleAndRainbowood` | 木棍钻不动；手钻钻侧面 → 变成对应采脂孔且朝向＝被钻面；钻顶面无效 |

### 36.4 结果

| 指标 | §35 | §36 |
|---|---|---|
| GameTest | 348 | **353**（新增 5 项，全绿） |
| 方块 / 物品 | 11,973 / 69,719 | **11,976 / 69,722** |
| 树上取液 | 无 | 橡胶树脂孔 + 采脂枫木 + 采脂彩虹木 |
| 缺失 token | 49 | **49**（本批不涉及 token） |

**保留的差异**：①GT6 在打孔前会检查 256 格内是否已有别的树脂孔（进程内静态表），移植版**每棵橡胶树都打一个孔**；②GT6 的**树液袋**（`MultiTileEntitySapBag`，挂在孔下方自动收集）还没移植，目前只能手取；③出液状态存 blockstate 而不是 BE 字段（客户端同步方式不同，效果一致）。**没有执行 `git commit`。**

---

## 35. 续批：GT6 的森林地表——倒木 4 种、地上的木棍、发光菇（丛林水面）+ 一条测试幂等性修复

§34 做树时顺手量到的另一处缺失：移植版**有** GT6 的四种倒木方块（`log_dry` / `log_rotten` /
`log_mossy` / `log_frozen`，iconset 生成、带轴向、贴图就是 GT6 的 `log_side_dry/rotten/mossy/frozen`）、
**有**地上的木棍方块（`twigs`，`TwigBlock`：右键捡起、掉落原版木棍）、**有** 16 种发光菇
（`glowtus_*`，`IconSetLilyBlock` 睡莲式水面植物），但**没有任何东西会把它们放进世界**——GT6 的
`WorldgenLogDry/Rotten/Mossy/Frozen`（`Loader_Worldgen:603-606`）、`WorldgenSticks`（`:630`）、
`WorldgenGlowtus`（`:632`）在移植版一个都没有。

这批把三族生成器一次补齐（新 `worldgen/GTSurfaceFlora.java` 数据表 + `worldgen/GTSurfaceFloraFeature.java`
生成器 + `gt_surface_flora` 配置/放置特征 + 生物群系修饰符）。算法与 §34 的树同源（GT6 的
`WorldgenOnSurface`）：每区块若干目标列 → 每条 `1/概率` 掷点 → 从天空向下射线找地表/水面 → GT6 自己的落地规则。

* **倒木**（4 条，概率照原版：枯 `1/8`、腐 `1/3`、苔 `1/8`、冻 `1/8`）：生物群系取 GT6 的集合
  （枯＝平原/森林/热带草原/沙漠/恶地＋荒原、腐＝沼泽/丛林、苔＝平原/森林/沼泽、冻＝雪原族；GT6 的
  "wastelands" 在 1.20.1 无对应群系，**记录在案**）。**GT6 的三种摆放形状逐条搬运**：竖直 3~5 根
  （最底一根会替换地表方块）、沿 X 的 3~5 根平躺、沿 Z 的 3~5 根平躺，方块轴向随形状设置；苔木还会
  按 GT6 的 50% 掷点在木头上长出红/褐蘑菇。
* **木棍**（`WorldgenSticks`：每区块 2 个目标列、`1/2` 掷点）：生物群系乘数照原版——森林/沼泽 **×3**、
  河流/平原/热带草原 **×2**、针叶林/恶地 **×1**、其余 **0**；落在 GT6 的 `plantableGreens`（草/土类）上。
* **发光菇**（`WorldgenGlowtus`：每区块 16 个目标列、`1/2` 掷点）：只在丛林/沼泽，落在**水面**上，
  颜色 ＝ GT6 的 `random.nextInt(16)`（16 种染色菇）。

### 35.1 顺带修掉的一条**测试幂等性**缺陷（§34 引入）

新批次第一次跑门禁时，§34 的三项树测试**全部失败**，诊断打印出 `above=gregtech:log_rubber`、
`surface=271`——**GameTest 的世界目录在多次运行之间是复用的**，所以上一轮长出来的树还立在原地，
第二轮就被自己挡住了。这不只是本批的问题：**§34 的"全绿"只在世界是空的时候成立**，重跑门禁就会红。
修法：`TreeGrowthTests` 在每次种树前**清理场地**（以基准点为中心 17×17、上下 2~22 格，只清非空气方块，
开销可忽略），蓝云杉高度门槛测试的两个场地同样处理。本轮门禁**在上一轮树木仍在世界里的前提下**通过，
即幂等性已被实测验证。

### 35.2 测试

新增 `gametest/SurfaceFloraTests`（4 项）：

| 测试 | 断言 |
|---|---|
| `floraBlocksExist` | 4 个倒木方块都在且带轴向、木棍方块是 `TwigBlock`、16 种发光菇都能解析、`gt_surface_flora` 特征与配置特征已注册并被数据包加载 |
| `fallenLogsUseGt6Shapes` | **直接驱动真实形状代码**（`GTSurfaceFloraFeature.placeShape`）：每树种 × 8 次掷点核对竖直 3~5 根且轴向 Y（含"最底一根埋在地表下"确实会出现）、X/Z 平躺 3~5 根且轴向正确、苔木长出蘑菇、其余树种不长蘑菇 |
| `twigsAndGlowtusPlaceOnTheirGround` | 木棍能立在 GT6 草地上、石头不是 `plantableGreens`、发光菇浮在水面上、水面/地表射线都能找到对应列（穿过浮在水上的植物仍然找得到） |
| `tablesMatchGt6` | 4 条倒木的**顺序与概率**（`log_dry`/8、`log_rotten`/3、`log_mossy`/8、`log_frozen`/8）＝ `Loader_Worldgen:603-606`；木棍乘数（森林/沼泽 3、平原/河流 2、针叶林 1、雪原 0）；(2,2) 与 (16,2) 计数；16 种菇色 |

### 35.3 结果

| 指标 | §34 | §35 |
|---|---|---|
| GameTest | 344 | **348**（新增 4 项，全绿且**可重复运行**） |
| 地表生成器 | 树（8 种）+ 石头/水塘/矿脉等 | **+倒木 4 +木棍 +发光菇**（共 6 族新内容） |
| 方块 / 物品 | 11,973 / 69,719 | 11,973 / 69,719（**复用既有方块，不新增注册**） |
| 缺失 token | 49 | **49**（本批不涉及 token） |
| 无配方物品 | 0 | **0** |

改动文件：`worldgen/GTSurfaceFlora.java`（新）、`worldgen/GTSurfaceFloraFeature.java`（新）、
`worldgen/GTFeatures.java`（+1 特征）、`gametest/SurfaceFloraTests.java`（新）、
`gametest/TreeGrowthTests.java`（清场 + 幂等）、
`data/gregtech/worldgen/{configured_feature,placed_feature}/gt_surface_flora.json`（新）、
`data/gregtech/forge/biome_modifier/gt_surface_flora.json`（新）。

**保留的差异**：①GT6 的 "wastelands"（荒原）群系在 1.20.1 没有对应，枯木少一条生物群系；
②GT6 的 `checkForMajorWorldgen`（地牢/中心区保护）在移植版仍无对应系统；③GT6 用 `WD.set` 覆盖式放置
（连地表方块都会被倒木替换），移植版同样如此（`setBlock` flag 2）。**没有执行 `git commit`。**

---

## 34. 续批：GT6 的树——9 种树形 + 树苗生长 + 世界生成撒苗（外加 4 个缺失树种）

**查出来的问题**：移植版的 12 个树种树苗**全都挂着原版 `OakTreeGrower`**——任何 GT6 树苗长出来都是
**橡木**（橡木原木 + 橡树叶）；GT6 的树形（`gregtech/blocks/tree/BlockTreeSaplingAB.grow` 的 8 个
meta case + `BlockTreeSaplingCD.grow` 的 1 个）一条都没搬；GT6 世界里由
`Loader_Worldgen:608-616` 登记的 9 种树的世界生成也不存在。另外，GT6 自己的树种
**hazel / cinnamon / coconut / blue spruce** 在移植版里只是贴图装饰方块（`GTIconSetBlocks` 的
iconset 方块），没有原木/木板/树叶/梁/树苗这一套木材体系（`WoodSpecies` 里压根没有它们）。

① **树形**（新 `worldgen/GTTreeShapes.java`）：按 GT6 源码逐行搬运 9 个 case——树干高度的掷点
（`y + min + rand(max-min)`，蓝云杉是 `y + 自由高度 - rand(3)`）、**整树放弃式前置检查**
（`canPlaceTree` 在 `top-5`/`top-4` 的 5x5/7x7 区域，失败即整棵树不长）、树冠的 `|i*j| < n` 分层谓词、
椰子树的十字 + 对角叶片、蓝云杉的**锥形体**（`i*i + j*j < k*k*0.2`，k = 1..14）与
**把树下泥土变成灰化土**（GT6 `Blocks.dirt` meta 2 → 1.20.1 `podzol`）、橡胶树干上的树脂孔位判定。
GT6 的 `getMaxHeight` 语义照搬：只检查 `limit-1` 个偏移、全部可替换时返回 `limit`——这一条顺带纠正了
我最初"蓝云杉门槛恒假＝死内容"的误判（`maxHeight(…,16)&lt;16` 是**可达**的，需要 15 个自由方块）。

② **树苗生长**（新 `worldgen/GTTreeGrower.java`）：12 个树种的 `SaplingBlock` 生成器从
`OakTreeGrower` 换成 GT6 树形；`SaplingBlock` 自带的两段生长（`stage` ＝ GT6 的 `meta &lt; 8 → meta | 8`）、
骨粉路径与光照判定保留。于是"骨粉催 GT6 树苗 → 长出该树种的树"第一次成立。

③ **世界生成**（新 `worldgen/GTTreesFeature.java` + `gt_trees` 的配置特征/放置特征/生物群系修饰符）：
照 `WorldgenTree*` 的算法——每区块 1 个目标列 → `1/概率` 掷点 → 生物群系门槛 → 从天空向下射线找地表
（跳过液体/非实心/原木/树叶）→ 要求可种植方块 → 撒苗并**立即长成**。GT6 的
`BIOMES_RUBBER/MAPLE/WILLOW/BLUEMAHOE/HAZEL/CINNAMON/COCONUT/BLUESPRUCE` 映射到 1.20.1 群系名集合
（taiga 族 / forest 族 / swamp / jungle 族 / plains 族 / beach 族 / 山地族），概率逐条照原版：
橡胶 **1/5**、枫 **1/5**、柳 **1/4**、蓝桃花心 **1/3**、榛 **1/32**、桂皮 **1/3**、椰子 **1/1**、蓝云杉 **1/32**。
彩虹木（`WorldgenTreeRainbowood`）原版只登记了别的模组的 "Enchanted Forest" 群系 ⇒ 1.20.1 里与原版
不装 BoP 时一致地**不生成**，但树苗仍能长出彩虹木树形。

④ **补上四个缺失树种**：hazel / cinnamon / coconut / blue spruce 从装饰方块升级为**真树种**
（`WoodSpecies` + `GTWoods`：原木/木板/树叶/梁/树苗 × 4 ＝ 新增 **4 个方块 id**，另外 **16 个 id**
从 iconset 装饰方块转交给 GTWoods，`GTIconSetBlocks` 跳过清单同步、块数 271 → **255**）；贴图沿用 GT6
资源包里的 `log_*/planks_*/leaves_*/beam_*/sapling_small_*`，双语键补 4 条；锯切/去皮/车床配方由
`RegisteredWoodSurvivalRecipes` 自动覆盖（树种 8 → **12**，行数 184 → **252**，测试改为按
`expectedRows()` 公式断言而不是写死数字）。

⑤ **顺带修掉一个客户端显示缺陷**：GT6 树种的 `leaves_*` / `sapling_*` blockstate 只有单个 `""` 变体，
而方块类分别是 `LeavesBlock`（属性 `distance`/`persistent`）与 `SaplingBlock`（属性 `stage`）——
变体永远匹配不上任何方块状态 ⇒ 这两种方块在客户端是**丢模型**的。新工具
`tools/generate_gt6_tree_assets.py` 按方块真实属性重新生成（树叶 14 变体、树苗 2 变体，共 12 树种 × 2），
并为 4 个新树种生成树苗方块模型/物品模型。

### 34.1 测试

新增 `gametest/TreeGrowthTests`（4 项）：

| 测试 | 断言 |
|---|---|
| `everySpeciesGrowsItsOwnGt6Tree` | 12 个树种各自长出**自己树种**的原木/树叶（半径 8 内无外来原木），树干 ≥ GT6 形状的最小高度（蓝云杉按 `y+自由高度-rand(3)` 算 14 起） |
| `saplingsGrowTheirGt6Tree` | 树苗路径（`SaplingBlock` → `GTTreeGrower.growTree`）：橡胶/枫/榛/椰/蓝云杉长出本树种树干与树叶，**一个橡木方块都没有**（正是本批修掉的 bug），且树苗被最底部原木替换 |
| `blueSpruceHeightGate` | GT6 的高度门槛语义：14 个自由方块失败且不留树干、15 个成功、树干 14~16 |
| `worldgenTreeTableMatchesGt6` | 世界生成表 = GT6 的 8 条登记（树种类别 + 概率 + 群系集合非空），彩虹木不在表内但树苗树形在；`gt_trees` 特征已注册且配置特征已被数据包加载；三个木字典树种回退到最近的 GT6 树形 |

顺带按新树种数更新 `SurvivalStandardizationTests.treeSurvivalRoutesAreExecutable`（写死的 184 → 公式）。

### 34.2 结果

| 指标 | §33 | §34 |
|---|---|---|
| GameTest | 340 | **344**（新增 4 项，全绿） |
| 树种（原木/木板/树叶/梁/树苗） | 8 | **12**（+榛/桂皮/椰/蓝云杉） |
| 方块 / 物品 | 11,969 / 69,715 | **11,973 / 69,719** |
| iconset 装饰方块（被真方块接管 16 个） | 271 | **255** |
| 树木处理配方行（锯切/去皮/车床） | 184 | **252** |
| 机器配方行 | 246,853 | **247,355** |
| 缺失 token | 49 | **49**（本批不涉及 token） |
| 无配方物品 | 0 | **0**（363 项检查） |

改动文件：`worldgen/GTTreeShapes.java`（新）、`worldgen/GTTreeGrower.java`（新）、
`worldgen/GTTreesFeature.java`（新）、`block/wood/WoodSpecies.java`、`registry/GTWoods.java`、
`registry/GTIconSetBlocks.java`、`worldgen/GTFeatures.java`、`content/recipe/RegisteredWoodSurvivalRecipes.java`、
`gametest/TreeGrowthTests.java`（新）、`gametest/SurvivalStandardizationTests.java`、
`data/gregtech/worldgen/{configured_feature,placed_feature}/gt_trees.json`（新）、
`data/gregtech/forge/biome_modifier/gt_trees.json`（新）、双语语言文件、48 个树叶/树苗模型与 blockstate、
新工具 `tools/generate_gt6_tree_assets.py`、`tools/add_tree_species_lang.py`、`tools/report_wood_leaf_colours.py`。

**保留的差异（都写进代码注释）**：①橡胶树干上没有 GT6 的树脂孔（`MultiTileEntityResinHoleRubber`，
移植版还没有该方块），因此树干是整根原木、暂时无法从树上取树脂；②GT6 的 `checkForMajorWorldgen`
（地牢/中心区保护）在移植版没有对应系统；③生物群系判定按**列**查 1.20.1 群系（GT6 是整区块的群系**名字**，
含其它模组群系），所以只有原版群系的条目能生成；④`PINE` / `EBONY` / `WHITE_MAHOE` 在 GT6 里是
木字典树种（别的模组的木材，GT6 从不生成），移植版让它们借用最近的 GT6 树形（松/蓝云杉、
黑檀/枫、白桃花心/蓝桃花心），原版没有对应树形。**没有执行 `git commit`。**

---

## 33. 续批：`gt.matdicts` 宝箱（§25 故意留下的最后一张表）+ 一张进度核实

§25 做「自选战利品箱」时**故意没给 `gt.matdicts` 做变体**，理由写在代码注释里："它的行是材料词典书，等材料词典书"。
§31 把材料词典与那张表做出来了，这一步自然收尾：

* `registry/GTLootChests.java`：`TABLES` 增加第 18 个变体 **`matdicts|gt.matdicts`**（10 个原版分类 + **8** 张 GT 表）。
* `tools/generate_loot_chest_assets.py`：同样的表加一条（英文标签取原版 `LH.java:399` 的
  `loot.gt.matdicts` = "Random Material Dictionaries"）→ 重跑生成 **18 个 blockstate + 18 个物品模型 + 双语各 1 条键**
  （共享模型与贴图不变，这是 §25 就定下的做法）。
* 测试无需新增：`LootInjectionTests.lootChestsRollTheirTables` 本来就是**遍历所有战利品箱变体**、
  逐表放箱并断言箱内有战利品且只生成一次，新变体自动被覆盖。

### 33.1 顺带核实：§9 清单里第 2 条（Mortar 家族）其实**已经完成**

查 §9 第 2 条（"早期手动碾粉是核心生存机制，现在只能靠机器"）时确认它早已在更早的批次里做完：
`content/recipe/MortarGrindingRecipes.java`（手写加载器，`Loader_Recipes_Handlers:79-112` 的 34 组前缀行）
+ `gametest/MortarGrindingTests.java`（断言 8+ 条行可用、`MORTAR`/`BRITTLE` 标志由
`tools/extract_gt6_workability.py` 导入 = 195 / 78 个材料）+ 手工研钵方块（`ManualToolBlockEntity.MORTAR`）。
也就是说 §9 的第 2 条与第 3 条（配方钥匙物品，§17 已解锁挤出机族）都已完成，剩余的大项只有
**书架方块**（§9 第 6 条的尾巴）与 **地牢/传送门/养蜂/木头字典**（§9 第 8 条，各自是独立大子系统）。

### 33.2 结果

| 指标 | §32 | §33 |
|---|---|---|
| 可放置战利品箱 | 17 个变体（10 原版分类 + 7 GT 表） | **18 个**（+`gt.matdicts`） |
| 缺失 token | 49 | **49**（本批不涉及 token） |
| GameTest | 340 | **340**（现有战利品箱测试自动覆盖新变体） |

改动文件：`registry/GTLootChests.java`（+1 变体）、`tools/generate_loot_chest_assets.py`（+1 表项）、
生成的 2 个资产文件 + 两份语言文件。**没有执行 `git commit`。**

---

## 32. 续批：把最后一批**真实**内容缺口定性，并关掉其中一个（`fr:chocolatemilk`）

§9 的推进清单之外的收尾工作：先把 53 个缺失 token 逐个定性，再关掉能关的。

### 32.1 53 个 token 的定性（都有证据）

| 类别 | 数量 | 性质 |
|---|---|---|
| `tech:` | 36 | **其它模组**（BoP 的淤泥/硬土/闷烧草、Aether 沙…）——属"按需移植" |
| `i:` | 12 | 3 个 `bullet*:Empty` 哨兵 + 6 个**石头齿轮**（GT6 `TD.G_STONE` 本就无此形态）+ `gem:Ta`、`plate:Blaze`（原版恒假） |
| `omd:` | 4 | **Eudialyte**、**HydratedCoal**（真缺口，见 32.3）+ Petrotheum/Pyrotheum（其它模组流体） |
| `fr:` | 1 | `chocolatemilk`（**本批已关**，见 32.2） |

### 32.2 关掉 `fr:chocolatemilk`：1.7.10 注册名 ↔ 移植版下划线名

移植版其实**有**这个流体（`RegisteredFluids.ChocolateMilk = fluid("ChocolateMilk", "potion.chocolatemilk")`），
但 GT6 的行里用的是 1.7.10 的注册名 `chocolatemilk`（无下划线），而移植版按 `chocolate_milk` 注册，
`GTGeneratedChem.byRegistryName` 的索引里没有旧拼法 → `fr:` 规格解析失败。
修法：在该索引里补上旧拼法别名（`chocolatemilk` → `ChocolateMilk`、`darkchocolatemilk` → `ChocolateMilk_Dark`），
与 §18/§25 的"引用拼写对齐"是同一类修复。**缺失 token 53 → 52**。

### 32.3 两个真缺口（Eudialyte / Hydrated Coal）——根因在生成器，已定位

| 材料 | 原版定义 | 移植版现状 |
|---|---|---|
| **Eudialyte** | `MT.java:1513` `dcmp(8421, "Eudialyte", SET_LAPIS, 155,96,114,255, G_GEM_ORES, CRYSTAL, MORTAR, BRITTLE)`；`:2010` 矿石倍率 5；`:3242` 副产物 Zircon/RareEarth/Hf/Pb | 形态表/组成表/加工表**都已导入**，但**材料本体未注册** → `i:gem:Eudialyte`、`omd:Eudialyte` 解析失败 |
| **Hydrated Coal** | `MT.java:1534` `mixdust(8335, "Hydrated Coal", SET_LIGNITE, 70,70,100,255, BRITTLE, FURNACE, MORTAR, COAL)` | 同上（`MaterialForms` 有 `8335\|DUSTS,PLANTS`），本体缺 |

根因在 `tools/transpile_gt6_materials.py` 的 `SKIP_FACTORIES`：`mix`/`mixdust`/`dcmp` 三个工厂被整类跳过，
于是"可分解化合物"和"混合粉"全部没发射（该工具的日志自己会打印
`[ore-proc] skip Eudialyte: material not emitted`）。**但这三个工厂不能直接从跳过表里删掉**——
实测会撞上 GT6 自己用多种工厂重复声明同一材料，`modular_materials.parse_catalog` 直接报
`Duplicate readable material names in Compounds`。

**修法：走工具里已有的 `EXTRA_ENTRIES` 机制**（为"原版用被跳过的工厂"的材料逐条手工搬运，
已有 `Graphene`/`Superconductor` 两个先例），两条搬运用移植版工厂写成：

```java
Eudialyte    = gem (8421, "Eudialyte"    , 0x9B6072, MaterialTextureSet.LAPIS, MaterialProperty.GEM, MaterialProperty.ORE),
HydratedCoal = dust(8335, "Hydrated Coal", 0x464664),
```

顺带的两件事都用现成工具解决：①矿石加工搬运量 **1150 → 1154 条调用**（Eudialyte 的矿石倍率/副产物 +
Hydrated Coal 的相关行）；②新材料的语言键由 `tools/import_gt_lang.py` 补齐（1100 个源名、1057 处纠正），
这正是门禁里 `registerednameshavetranslations` 那一条测试抓出来的（第一次跑就红了，补完语言键即绿）。

### 32.4 结果

| 指标 | §31 | §32 |
|---|---|---|
| 缺失 token | 53 | **49**（`fr:chocolatemilk` + `i:gem:Eudialyte` + `omd:Eudialyte` + `omd:HydratedCoal` 关闭；剩 11 个 `i:` = 3 哨兵 + 6 石头齿轮 + `gem:Ta`/`plate:Blaze`，2 个 `omd:` = 其它模组的 Petrotheum/Pyrotheum，36 个 `tech:` = 其它模组） |
| 新材料 | — | **Eudialyte（8421）、Hydrated Coal（8335）**，含形态/矿石/语言键 |
| 矿石加工搬运 | 1150 调用 | **1154 调用** |
| GameTest | 340 | **340**（过程中被 `registerednameshavetranslations` 抓出缺语言键，补后全绿） |

改动文件：`loaders/c/GTGeneratedChem.java`（旧注册名别名）、`tools/transpile_gt6_materials.py`（`EXTRA_ENTRIES` 两条 + 注释）、
`content/material/generated/CompoundMaterials.java` 与 `data/generated/GT6Materials.java`（重跑生成）、
两份语言文件（`import_gt_lang.py`）。**没有执行 `git commit`。**

---

## 31. 续批：**材料词典书**（Material Dictionary）+ `gt.matdicts` 战利品表解冻

§9 的第 6 条（`MultiItemBooks` 其余条目）里最有价值的一块，也是 §25 留下的悬念——那张**故意没做的
`gt.matdicts` 表**——就是材料词典。

### 31.1 原版是怎么做的（逐行核对）

* 每个材料都有一本词典：`OreDictMaterial.mDictionaryBook`，由 `UT.java:762` 惰性创建为
  `ST.book("Material_Dictionary_<材料名>")`；内容由 `UT.Books.addMaterialDictionary` **按材料数据生成**
  （化学组成、参与的合金配方、副产物、工具属性、附魔、属性标志、机器/矿石标志、以及"会熔炼/凝固/燃烧/粉碎成它"
  的材料矩阵）。
* `gt.matdicts` 表由 `Loader_Loot:359-364` 在运行时**遍历所有材料**建表（每材料一行，权重 144，1~1，抽取 8~24 次），
  所以它天生是动态表，不能"转译成常量"。
* 发放方式两条：**落灰材料词典**（`MultiItemBooks:68`，`addItem(32766, …)` + `Behavior_Drop_Loot("gt.matdicts")`）
  右键 roll 该表；以及**原版宝箱**里直接出现该物品（`Loader_Loot:443` 地牢 100×1-2、`:487` 废弃矿井 6×1-2、
  `:513` 村庄铁匠 20×1-2、`:525` 要塞图书馆 20×4-16）。
* 打印机也能印（`RecipeMapPrinter:142-145`：USB 里带材料 id 时印出该材料的词典），但那条路要扫描仪/USB 的数据流，
  移植版的扫描仪目前只是显示行（§26），所以这批不做，记在下面。

### 31.2 移植内容

* `content/book/GTMaterialDictionary.java`（新）：按材料数据生成**成书**（vanilla `written_book`，与 §21 的 16 本手册同一套），
  NBT 里带 `book`/`gt.material` 映射名 + 页；第一页＝身份（名称/别名/熔点/密度/质量/状态），
  第二页＝组成（元素质子/中子，或化合物组成 + "用研钵/粉碎机磨开"提示），第三页＝**该材料在移植版真实拥有的形态清单**
  （逐个 `MaterialPrefix` 检查），第四页＝工具属性（品质/耐久/速度/工具类数），第五页＝属性标志（`MaterialProperty`），
  第六页＝矿石与副产物（矿石方块走 `BlockMaterialPrefix.ore`，原矿走 `oreRaw`）。≥256 字符的页按原版规则截断。
* `GTLootTables`：**动态建 `gt.matdicts` 表**（每个有词典的材料一行，权重 144 / 1-1，抽取 8..24），
  与 GT6 的运行时建表一致；`stackOf` 认识 `Material_Dictionary_<材料>` 规格。
* 新物品 **`dusty_material_dictionary`**（落灰材料词典，GT6 32766）：注册为 `LootBagItem("gt.matdicts")`
  （与 §21 的落灰指南书同一套机制），贴图取原版 `32766.png`（`tools/generate_material_dictionary_assets.py` 新工具）。
* 转译器：`book_loot_matdict` 从"未映射（移植版没有这个物品）"改为映射到新物品，重跑后**原版宝箱注入 93 → 97 行**
  （地牢/矿井/铁匠/图书馆四张表，权重与数量照原版）。

### 31.3 结果

| 指标 | §30 | §31 |
|---|---|---|
| 材料词典 | 0 | **每个有形态的材料一本**（成书页由材料数据生成） |
| `gt.matdicts` 表 | 不存在（§25 故意略过） | **运行时建表**，每材料一行（权重 144、1-1、8..24 抽取） |
| 新物品 | — | **`dusty_material_dictionary`**（roll `gt.matdicts`） |
| 原版宝箱注入 | 93 行 | **97 行**（+地牢/矿井/铁匠/图书馆的词典行） |
| GameTest | 337 | **340**（新增 `MaterialDictionaryTests` 3 项：每本词典页数/页长/提到自身材料/能被反查、表每材料一行且权重与抽取次数照原版、落灰词典 roll 出词典） |

### 31.4 写明没做的部分

* GT6 词典里的**合金配方页、附魔页、"熔炼/凝固/燃烧/粉碎成它"的目标矩阵页**（`UT.Books.addMaterialDictionary` 的后半）
  未移植——需要先把"按材料反查配方"的能力做出来（§9 第 4 条的 OreDict/配方展开）。
* **打印机印词典**（`RecipeMapPrinter:142-145`）需要扫描仪把材料 id 写进 USB 的数据流；移植版扫描仪目前是显示行（§26），
  等那条链路做起来再补。
* **书架方块**（`MultiTileEntityBookShelf` + `LoaderBookList` 的 667 行"哪些物品算书"表）仍缺，单独一批。

改动文件：`content/book/GTMaterialDictionary.java`（新）、`gametest/MaterialDictionaryTests.java`（新）、
`content/loot/GTLootTables.java`、`registry/GTMultiItems.java`、`tools/transpile_gt6_loot.py`（别名 + 去掉未映射条目）、
`tools/generate_material_dictionary_assets.py`（新）、`loaders/c/GTLootGen.java`（重跑转译器）、两份语言文件。**没有执行 `git commit`。**

---

## 30. 续批：原版的**早期工具**——燧石 / 骨 / 黑曜石 / 木化石 / 石头（21 条有序行）

§29 修工具配方时，`Loader_Tools:255-290` 里还有一段从没移植过的东西：**早期工具**。原版在
"手柄循环"里手写了 42 行 `CR.shaped`，让玩家在金属之前就能用**石头本身**做出工具：

| 家族 | 材料 | 行（工具 → 图案） | 原版标志 |
|---|---|---|---|
| 燧石 | `MT.Flint`（物品＝原版燧石） | 刀 `"SX"`、斧 `"XX"/"XS"`、锹 `"X"/"S"`、镐 `"XXX"/" S "` | MIR / MIR / DEF / DEF |
| 骨 | `MT.Bone`（物品＝原版骨头） | 棍棒 `"  X"/" X "/"S  "` | MIR |
| 黑曜石 | 黑曜石的 `rockGt` | 刀 / 斧 / 锹 / 镐（同上图案） | MIR / MIR / DEF / DEF |
| 木化石 | 木化石的 `rockGt` | 斧、锄 `"XX"/" S"`、锹、镐、棍棒 `" XX"/"XXX"/"SX "`、硬锤 `"XX "/"XXS"/"XX "` | MIR / MIR / DEF / DEF / MIR / MIR |
| **所有石头**（`ANY.Stone`） | 任意岩石的 `rockGt` | 斧、锄、锹、镐、棍棒、硬锤（同上图案） | 同上 |

`ANY.Stone` 在 GT6 里是 `ANY.Stone.put(STONE, …)`——即**带 `STONE` 属性的材料**（`TD.java:397-398`），
不是形态标志；移植版对应的就是 `MaterialProperty.STONE`（由 `MaterialFactories.stone(...)` 设置）。

移植做法：在 `GTToolRecipes` 里加了一个 `Gate`（每格的固定原版物品、固定工具材料、只允许某材料、
只允许石头材料、跳过"工具头形态"门槛——岩石没有镐头形态），21 条行照原版图案逐条写入；
匹配时材料取自岩石本身（燧石/骨取自物品），柄沿用「任意可用柄」。
**这让"三块石头 + 一根木棍 = 石镐"这类 GT6 早期玩法第一次在移植版里成立**，也补上了燧石刀、骨棒、
黑曜石工具与木化石工具。

结果：**有序工具行 27 → 48 条**（27 条 `OreProcessing_Tool` + 21 条早期行）、头+柄装配 20 条、工具头 35 条；
**337 项 GameTest 通过**（`ToolAssemblyTests` 新增 `earlyToolsComeFromRocksAndFlint`：三岩石+木棍做石镐且材料＝该岩石、
木棍+燧石做燧石刀且材料＝Flint、黑曜石行拒绝花岗岩而石头行接受）；`tools/check_tool_recipes.py` 现在把
原版 42 条早期行归成 **21 个（工具 × 家族）** 与移植版逐行比对 → **0 差异**（`S` 键＝移植版的 `H` 柄）。

保留差异：原版按**手柄循环**（木棍/竹/骨/塑料）逐条注册，并按**每种石头材料**各注册一遍（共 42 行/材质组合）；
移植版每种家族一行 + 「任意可用柄」+ 「任意 STONE 属性岩石」的谓词，展示材料取该家族的固定材料。

改动文件：`content/recipe/GTToolRecipes.java`（早期行 + `Gate`）、`recipe/GTToolPatternRecipe.java`（gate 匹配与展示）、
`recipe/ToolAssemblyCatalog.java`（早期行的 JEK 原料列表）、`loaders/Loader_ToolCraftingRecipes.java`（装配与有序行并存）、
`gametest/ToolAssemblyTests.java`、`tools/extract_gt6_tool_recipes.py`（早期行解析：按行取 `CR.shaped` 调用与手柄循环上下文）、
`tools/check_tool_recipes.py`（早期行比对）。**没有执行 `git commit`。**

---

## 29. 续批（实机测试报告）：工具配方回归**有序合成**，并补上原版的工具头工作台配方

反馈：**"工具制作方法是无序合成，原版是有序合成并且在工作台配方表里展示"**。核查后确认移植版错了，
而且错得比报告的还多一层：所有工具都用**一条**自定义配方（`gregtech:tool_assembly`）描述，它是一个
`CustomRecipe`——**没有 ingredient、任意摆放 2 格就能匹配**，因此既不是有序合成，也进不了工作台的配方表
（JEI 只能靠移植版自己的「工具组装」页签展示）。

### 29.1 先把原版读清楚：GT6 其实是三种配方

`Loader_Tools.java` 里有三条不同的路，逐行核对后：

| 家族 | 原版出处 | 原版形态 | 移植版此前 |
|---|---|---|---|
| **头 + 柄工具**（镐/斧/锹/锄/剑/锯/锉/凿/螺丝刀/软硬锤/双刃斧/犁/感知/建筑杖/建筑镐/宝石镐/万用铲/放大镜，19 个） | `AdvancedCraftingTool extends ShapelessOreRecipe`（`gregapi/recipes/AdvancedCraftingTool.java`） | **无序**（"SX" 之类并不存在，它就是无序配方；display 用矿词 ingredient＝所有材料） | ✅ 语义本来就对，只是**看不见** |
| **一体式工具**（扳手/猴扳手/小大弯管器/撬棍/柱塞/钳子/铲斗/刀/屠刀/剪线钳/枝剪/剪刀/棍棒/手钻/万用铲/滚轮/打火石，17 个） | `OreProcessing_Tool` 的第 8 个参数（`Loader_Tools:305-320`），例如 `WRENCH = {"PhP"," P "," P "}`、`KNIFE = {"fP","hH"}`、`BENDING_CYLINDER = {"sfh","III","III"}` | **有序** `CR.shaped`（`CR.DEF_NCC`，**不镜像**） | ❌ 做成了无序，且图案全丢 |
| **工具头**（镐头/斧头/锹头/剑刃/锯片/锉/凿/螺丝刀头/锤头/软锤头/犁头/感知头/双刃斧头/建筑镐头/建筑杖头/万用铲头/扳手头…，18 个工具 35 条图案） | 同一个注册的第 9 个参数 `mToolHeadRecipes`（`Loader_Tools:293-313`），例如 `PICKAXE = {"PII","f h"}`（锭×2+板+锉+锤）、宝石版 `{"CGG","f  "}` | **有序** | ❌ **完全没有**：工作台里做不出工具头 |

键位字母是 GT6 的（`Loader_Tools:395-425` + `CR.java:336-360`）：`A` 工具头、`H` 柄、`I` 锭、`P` 板、
`G` 宝石、`B` 弯曲板、`C` 宝石板、`S` 棒、`T` 螺丝、`O` 环、`N` 粒、`R` 岩石、`V/W/X/Y/Z` 特殊件（原版未指定时＝板），
小写＝工具键（`h` 锤、`f` 锉、`s` 锯、`d` 螺丝刀、`r` 软锤、`x` 剪线钳、`k` 刀、`y` 凿…）。

### 29.2 移植做法

* **`recipe/GTToolRecipes.java`（新）**：GT6 的图案表——一体式工具 27 条图案（金属版 + 宝石版）、工具头 35 条图案、
  头+柄 19 个工具；每条带 `mirror` 与 `normalHandle`（原版 `mUseNormalHandle`：柄是「该材料的柄材料」还是「同材料的棒」）。
* **`recipe/GTToolPatternRecipe.java`（新，基类）** + `GTToolCraftingRecipe` / `GTToolHeadRecipe` / `GTFlintAndTinderRecipe`：
  都是**真正的 `ShapedRecipe` 子类**，所以它们**就是工作台配方**，会出现在 JEI 的 crafting 分类里（这就是需要的
  "在工作台配方表里展示"）。
  匹配用「图案 + 材料一致性」判定而不是 ingredient 比对：GT6 是**每种材料注册一行**（`onOreRegistration`），
  移植版有 ~1,100 材料，照搬会是几万条配方与巨大的同步包，所以改成**一行图案 + 谓词匹配**（同一个材料贯穿所有材料格、
  `H` 按原版规则、工具键必须是可用的 GT 工具），展示用代表材料（钢）。
* **`GTToolAssemblyRecipe` 改写成 `ShapelessRecipe` 子类**：头+柄工具保持 GT6 的**无序**语义，但现在也有 ingredient，
  所以在 JEI 里显示（原先的 `CustomRecipe` 没有任何 ingredient，JEI 渲染不出来）。
* **`loaders/Loader_ToolCraftingRecipes.java`（新）**：服务端启动时注册 **27 条有序工具行 + 19 条无序装配 + 35 条工具头行**
  （共 81 条，`0 tools without a manual recipe`）；删掉 37 个旧的 `tool_assembly` JSON。
* `recipe/GTToolRecipeSerializers.java`（新）：`tool_assembly`（无序）/`tool_crafting`（有序）/`tool_head`（工具头）三个
  serializer，配方照常同步到客户端。
* `ToolAssemblyCatalog`（JEI「工具组装」页签，保留）：现在按**图案逐格**列出所有可用材料，并把同一种材料排到每一格的首位，
  保证页签展示的组合真能合成；`optionsFor` 里的工具键改成 `GTToolHelper.displayTool`（带 `GT.ToolStats` 的可用工具，
  裸物品栈不能用于合成）。

### 29.3 结果

| 指标 | §28 | §29 |
|---|---|---|
| 有序工具配方 | 0（全是无序） | **27 条图案 / 18 个工具**（GT6 的 `OreProcessing_Tool` 行） |
| 工具头工作台配方 | **0** | **35 条图案 / 18 个工具**（GT6 的 `mToolHeadRecipes`，全新内容） |
| 头+柄无序装配 | 37 条无 ingredient 的自定义配方 | **19 条真正的 `ShapelessRecipe`**（JEI 可见） |
| 无手工配方的工具 | — | **0** |
| GameTest | 334 | **336**（`ToolAssemblyTests` 改写为 4 项：每个工具都有 GT6 的配方族、有序行确实在 crafting 分类且是 `ShapedRecipe`、catalog 条目都能真合成、头+柄保持无序） |
| 与 GT6 逐行比对 | — | `tools/check_tool_recipes.py` → **OK（0 差异）**：35 个工具、27+35 条图案、19 个无序工具 |

工具头配方让「镐头」这类物品**第一次可以在工作台里做出来**（此前只能靠挤出机/砧），例如
`2 锭 + 1 板 + 锉 + 锤 → 镐头`（宝石版 `1 宝石板 + 2 宝石 + 锉`），`3 板 + 锤 → 扳手`（宝石版用宝石板 + 锉）。

### 29.4 保留的差异（写在代码注释里）

* GT6 为**每种材料各注册一行**（NEI 里能看到任意材料的组合）；移植版一行图案 + 材料一致性匹配，
  另有「工具组装」页签列出全部可选材料。**在 JEI 里按某个具体材料查配方**只会命中展示用的那一种材料。
* 一体式工具的展示材料取钢；`normalHandle` 的柄按移植版规则取「任意可用柄材料」（GT6 取该材料的 `mHandleMaterial`）。
* GT6 的电动工具（LV~HV 电钻/电锯/扳手/铆钉枪/混合器…）、枪械（手枪/卡宾枪/步枪）、口袋多功能工具
  需要电池/其它物品，移植版没有对应物品，未移植（`check_tool_recipes.py` 只比对移植版有的 35 个工具）。

改动文件：`recipe/GTToolRecipes.java`（新）、`recipe/GTToolPatternRecipe.java`（新）、`recipe/GTToolCraftingRecipe.java`（新）、
`recipe/GTToolHeadRecipe.java`（新）、`recipe/GTFlintAndTinderRecipe.java`（新）、`recipe/GTToolRecipeSerializers.java`（新）、
`loaders/Loader_ToolCraftingRecipes.java`（新）、`recipe/GTToolAssemblyRecipe.java`（改写为 `ShapelessRecipe`）、
`recipe/ToolShapedRecipe.java`（开放 `toolsUsable` 钩子）、`recipe/ToolAssemblyCatalog.java`（按图案列举）、
`loaders/a/Loader_Tools.java`（三个 serializer）、`gametest/ToolAssemblyTests.java`（改写）、
`gametest/MaterialCompatibilityTests.java`、`gametest/HandCraftingTests.java`、`api/tool/GTToolHelper.java`（display 辅助）、
`tools/extract_gt6_tool_recipes.py`（新）、`tools/check_tool_recipes.py`（新）；删除 37 个 `data/gregtech/recipes/tools/*.json`
与 `GTToolAssemblyRecipeSerializer.java`。**没有执行 `git commit`。**

---

## 28. 续批：石头的其余配方——16 个变体 × 27 种岩石的机器行（1296）与工作台行（597）

§27 做完挤出之后，`BlockStones` 还剩两半没碰：每个砖石变体在**机器里**的处理
（`BlockStones:255-487` 的锤/破碎/粉碎/熔炼/generify/洗苔/锯切），以及它们在**工作台里**的合成
（`CR.shaped`/`CR.shapeless` 图案）。移植版此前只有"石头→圆石"一条锤/破碎行，所以砖石做不出裂砖、
凿纹砖、红石砖、瓷砖、风车砖，也不能回炉、generify 或锯开。

### 28.1 机器行：GT6 的分组表逐格落地（1296 行 / 27 种岩石）

原版按 `mEqualBlocks[<变体>]` 分组，每组有自己的锤/破碎/粉碎/熔炼/generify 行。移植版的等价组是
`StoneType` 的 `_<变体>` 方块（与 §27 同一近似）。每岩石类型：

| 族 | 每个岩石类型的行数 | 参数（照原版） |
|---|---|---|
| Hammer | 16（每个变体一行） | 16 EU/t、16 刻；圆石/苔石/裂砖/苔砖/钢筋砖/红石砖是 `rockGt×4`，几率 8000 / 7000（1/10000） |
| Crusher | 16 | `16+采掘等级×16` 刻；钢筋砖与红石砖是 `64+采掘等级×64`，且带**副产品**（钢筋砖→铁小堆粉×2、红石砖→红石粉） |
| Shredder | 16 | 同上刻数；产出 `blockDust` 的移植替身＝9 个粉（移植版没有 `OP.blockDust` 方块） |
| 熔炼（Furnace） | 14 + 1 | 每个变体→该岩石的 `_stone`；**钢筋砖/红石砖不熔炼**（原版如此，它们带着第二种材料）；另有全局一行"9 粉→石头" |
| Generifier | 14 | 变体→原版方块（`stone`/`cobblestone`/`mossy_cobblestone`/`stone_brick` 四个元数据/`smooth_stone_slab`） |
| 洗苔（PressureWasher） | 2 | 苔石→圆石、苔砖→砖（原版 `cleanmoss` → `pressurewash`） |
| 锯切（Cutter） | 15 | `JUSTSTONE` 的 14 个变体 + 红石砖（多带红石小堆粉）：板×4 + 小堆粉×2，16 EU/t、16 刻、50 mB 冷却液 |

合计 **1296 行**（hammer 432 / crusher 432 / shredder 432 / 熔炼 378 / generify 378 / 洗苔 54 / 锯切 405，
另加"9 粉→石头"22 行）。每族数字都与原版逐行解析的结果相等（见 28.4 的核对工具）。

### 28.2 工作台行：24 个图案 + 镜像标志（597 行 / 27 种岩石）

原版这些行写在两个等价组循环里，图案与键都很短，但**镜像标志**各不相同（`CR.DEF` 不镜像、
`CR.DEF_MIR` 镜像）。新加载器 `Loader_StoneCraftingRecipes` 在 `ServerStartedEvent` 上按原版图案注入，
并用 `ToolShapedRecipe` 让 GT 工具按 GT6 的方式磨损：

| 组 | 图案 | 产出 | 标志 |
|---|---|---|---|
| STONE | `"XX"/"XX"` | 4 岩石 → 圆石 | DEF |
| STONE | `"X "/" f"` | 石头 + 锉刀 → 小齿轮 | DEF |
| STONE | `" X"/"XX"` | 3 岩石 → 原版石楼梯 | **DEF_MIR** |
| STONE | `"  "/"XX"` | 2 岩石 → 圆石台阶 | DEF |
| COBBL | `"  "/"XX"`、`" X"/"XX"`、`"XXX"/"XXX"` | 圆石 → 台阶×4 / 楼梯×4 / 墙×6 | DEF / **DEF_MIR** / **DEF_MIR** |
| BRICK | `"y"/"X"`、`"h"/"X"` | 砖 + 凿子 / 锤子 → 裂砖 | DEF |
| BRICK | `"Se"/"X "` | 砖 + 铁棒 + **钻头** → 钢筋砖 | DEF_MIR |
| BRICK | `"Dh"/"X "` | 砖 + 红石 + 锤子 → 红石砖 | DEF |
| SMOOTH | `"y"/"X"`、`"XX"/"XX"`、`"X"/"X"`、`"XX"`、`"X "/" X"`、`" X"/"X "` | 平滑石 → 凿纹砖 / 砖×4 / 瓷砖×2 / 小瓷砖×2 / 小砖×2 / 风车砖×2 | 全 DEF |
| 瓷砖族 | shapeless | 瓷砖↔方砖、风车砖 A↔B | — |
| 苔 | shapeless | 干净砖石 + 藤蔓 → 苔砖石（圆石/砖/裂砖三行） | — |

27 种岩石共 **597 行注册 + 132 行记录在案的跳过**（＝648），再加数据包已有的 27 条
`stone_<岩石>_bricks.json`（原版 STONE 组的"石头×4→砖×4"，§12 起就在）→ **756 = 28 × 27** 与原版逐行相等。

三条跳过都带原版理由：
* **81 行**：原版 `RM.growmoss`（`RM.java:305-310`）用**自己的苔藓物品**各写一行，移植版没有该物品（藤蔓那一行已实现）；
* **27 行**：钢筋砖那行的键 `'e'` 是 GT6 默认键表里的**钻头**（`CR.java:341`），移植版没有钻头工具；
* **24 行**：27 种岩石里只有 3 种有 `gearGtSmall` 形态（原版的石头齿轮本来就大半是死的），其余没有"石头+锉刀→小齿轮"这一行的产出。

### 28.3 顺带查清与顺带改的

1. **`Ingredient.of(新工具栈)` 能匹配组装好的工具**（工具带 `GT.ToolStats` NBT）——我一开始以为它匹配不了
   （`ItemValue` 的 NBT 比较），于是做了一套工具 tag，结果新测试直接把这个假设证伪：端口原有的手工配方一直是可用的。
   于是把那套 tag 全部撤掉，改为**用测试钉住事实**：`toolKeysAcceptRealTools` 断言工具键匹配全新与磨损的工具，
   并做**端到端合成**（磨损 25 的锤子 + 花岗岩黑砖 → 裂砖，且锤子按 GT6 的磨损量返还）。
2. `HandCraftingTests` 的"数据包行不许镜像"规则改成**只覆盖数据包行**：运行期注入的行在代码里显式声明标志
   （`Loader_StoneCraftingRecipes` 17 个 DEF + 3 个 DEF_MIR），由 §28 自己的测试守着 `stone/.../cobble_to_wall`
   镜像、`.../smooth_to_tiles` 不镜像。

### 28.4 结果

| 指标 | §27 | §28 |
|---|---|---|
| 砖石机器行 | 1,296（挤出） | **+1,296**（锤/破碎/粉碎/熔炼/generify/洗苔/锯切） |
| 砖石工作台行 | 0 | **597**（+132 记录跳过，+27 数据包行 = 756 = 28 × 27） |
| 机器配方行（`docs/items-without-recipes.json`） | 240,135 | **246,853** |
| 数据包配方产物 | 31,956 | **32,899**（+943 = 597 运行期工作台行 + 346 条熔炉镜像行） |
| GameTest | 330 | **334**（新增 `StoneVariantRecipesTests` 4 项） |
| 缺失 token | 53 | **53**（这批不引用缺失内容） |
| 无配方物品 | 0 | **0**（363 项检查全过） |

> 锯切行在 Cutter 表里是"每次调用 × 每种冷却液"：工业冷却液 6 种（水/蒸馏水/矿水/蒸馏水/两种润滑油）
> → 405 次调用展开成约 2,430 行，这是上面的机器配方行增量比 1,296 大的原因之一。

### 28.5 工具与复核

* `tools/check_masonry_rows.py`（新）：解析原版 `BlockStones` 的**每一个**分组循环里的
  `RM.Hammer/Crusher/Shredder.addRecipe1`、`RM.add_smelting`、`RM.generify`、`RM.cleanmoss`、`RM.growmoss`、
  `RM.sawing`（含 `JUSTSTONE` 循环展开）与 `CR.shaped/shapeless`（图案、`DEF_MIR`、默认键），
  与移植版的运行期报告 `docs/stone-variant-coverage.json` 和工作台加载器逐行比对：
  **OK（0 差异）**——hammer/crusher/shredder 各 16、熔炼 14+1、generify 14、洗苔 2、锯切 15、工作台 28 行/岩石类型。
  产物 `docs/gt6-masonry-parity.json`。
* `docs/stone-variant-coverage.json`（测试落盘）：机器行分族计数、工作台行 id 清单、跳过清单。

改动文件：`content/recipe/StoneVariantRecipes.java`（新）、`loaders/Loader_StoneCraftingRecipes.java`（新）、
`gametest/StoneVariantRecipesTests.java`（新）、`gametest/HandCraftingTests.java`（镜像规则限定数据包行）、
`content/recipe/StoneAndToolSurvivalRecipes.java`（分组行搬去新类）、`GregTech.java`（Phase C 入口）、
`tools/check_masonry_rows.py`（新）。**没有执行 `git commit`。**

---

## 27. 续批：石头的挤出（GT6 `BlockStones` 的 1212 行）+ 它揭出来的一个配方表缺陷

原版在 `BlockStones:286-315`（`mEqualBlocks[STONE]` 组）与 `:331-360`（`mEqualBlocks[COBBL]` 组）
各写 30 行，把这些岩石的**等价方块**挤成板/弯曲板/杆/长杆/螺栓/砖/方块/5 种原始工具头/齿轮/小齿轮/锤头，
16 EU/t、32 刻，两种模具族（`IL.Shape_Extruder_*` 与 `IL.Shape_SimpleEx_*`）。移植版此前**一行都没有**：
石头做不出板、杆、螺栓、齿轮，也拿不到石器工具头——§24 刚补上的"石器工具头"形态因此没有任何配方入口。

### 27.1 按原版的两个等价组展开（不是每个砖石变体）

原版按 `mEqualBlocks[STONE]` / `mEqualBlocks[COBBL]` 迭代（"与该石头等价的所有方块"），不是每种砖石变体。
移植版的等价物就是每个 `StoneType` 的 `_stone` 与 `_cobble` 方块。每个岩石类型 30 行：

| 行 | 内容 | 原版 |
|---|---|---|
| 13 个形态 × 2 模具族 | 板 9、弯曲板 9、杆 18、长杆 9、螺栓 64、铲头 9、剑刃 4、锄头 4、镐头 3、斧头 3、齿轮 2、小齿轮 9、锤头 1 | `:286-300` / `:331-345` |
| ingot 模具 × 2 模具族 | → 该岩石的 `_bricks` 变体（`ST.make(this, 1, BRICK)`） | `:291` / `:336` |
| block 模具 × 2 模具族 | → 该岩石的 `_stone` 变体（`ST.make(this, 1, STONE)`） | `:292` / `:337` |

27 个岩石类型 × 2 个输入方块 × 2 个模具族 × 15 行 = **1212 行**注册（102 个「岩石 + 形态」组合移植版没有该形态，
按原版"材料没有这个形态就没有这一行"跳过；996 条形态路线被测试逐条核对）。

五个标志位照原版 `addRecipe2(F, F, F, F, T, 16, 32, …)`：**不优化**（`aOptimize=F`）、**不查冲突**
（`aCheckForCollisions=F`）。这两点不是装饰——见下。

### 27.2 第一次实现把质量门炸了（诚实记录）

第一版按"每个变体方块都当输入"展开，且用默认的 `addRecipe2(true, 16, 32, …)` 注册 → Phase C 直接抛：

```
java.lang.IllegalStateException: Polymer recipe conflict in gt.recipe.extruder:
  1 dust_rubber + 1 extruder_shape_ingot -> 1 ingot_rubber
  collides with [0 air, 1 extruder_shape_ingot] -> [0 air]
```

两处根因都属于"配方表被污染"，与橡胶本身无关：

1. **自环行**：把 BRICKS 变体方块也当输入时，ingot 模具那一行的产出正是它自己（`1 bricks + ingot 模具 -> 1 bricks`）。
   `RecipeMap.make` 的优化把输入与输出互相抵消 → 输入槽变 0、输出变空，但配方**仍被登记**
   （`validate` 只看数组长度）→ 表里多出一条 `[air, 模具] -> [air]` 的空壳。
2. **空壳会污染冲突索引**：`findCollision` / `inputsSatisfiedBy` 会跳过空输入槽，于是这条空壳等价于"只要模具"，
   之后**任何**用到该模具的配方（包括聚合物那 24 行）都被判冲突 → 门禁在橡胶的第一行就崩。

修法两条：

* **按原版登记**：`aOptimize=F` 让方块模具那一行保持字面自环（原版就是如此），`aCheckForCollisions=F`
  让两个等价组共用同一批模具而不互相排斥。
* **`RecipeMap.make` 加固**（`api/recipe/RecipeMap.java`）：优化后把被抵消成 0 的输入槽**去掉**；若配方此时
  已无真实输入或真实输出，返回 `null` 而不登记空壳。原版会留下这条空壳（`Recipe` 构造器把零尺寸输入留在数组里、
  输出置空），移植版丢弃——理由就是上面第 2 条。加固带静态计数 `COLLAPSED_RECIPES_DROPPED`，
  Phase C 末尾打日志并写进 `docs/stone-extrusion-coverage.json`。

**顺带量出来的好处**：加固在当前树里挡掉 **305** 行空壳 ——
`Recipe tables: 305 rows dropped as collapsed by optimization, per map {mc.recipe.furnace=305}`，
全部来自 `mc.recipe.furnace`（原版熔炉镜像表），样例全是 `1 dust_X -> 1 dust_X` 这种自反行；
在此之前它们是 305 条"进料出同料"的空熔炉配方。新测试 `noRegisteredRecipeIsDegenerate` 把不变量钉死：
**任何**非 fake 配方都必须有真实输入且真实输出（当前检查 10 万+ 条，0 例外）。

### 27.3 结果

| 指标 | §26 | §27 |
|---|---|---|
| 石器挤出配方 | **0** | **1212**（27 岩石 × 2 组 × 2 模具族 × 15 行） |
| 挤出机表 | 57,871（= 59,083 − 1,212 的推导值） | **59,083**（+1,212） |
| 机器配方行（`docs/items-without-recipes.json`） | 238,923 | **240,135**（+1,212，与上一条一致） |
| 优化后空壳行被丢弃 | 未统计（含 305 条空熔炉行） | **305**（全部 `mc.recipe.furnace`） |
| GameTest | 326 | **330**（新增 `StoneExtrusionTests` 4 项） |
| 缺失 token | 53 | **53**（这批不引用缺失内容） |
| 无配方物品 | 0 | **0**（363 项检查全过） |

> 注意别拿 `Loader_Recipes_Parts` 日志里的 `extruder matrix: 57272 recipes` 对比：那是**转译矩阵跑完时**的快照，
> 后面还有手工加载器与 `GTGeneratedChem.loadAll()` 继续往表里加行。最终表大小以 `docs/extruder-matrix-coverage.json` 为准。

### 27.4 工具与复核

* `tools/check_stone_extrusions.py`（新）：源级逐行比对——解析原版 `BlockStones` 的 **60 行**（分组、五个标志位、
  EU/t、时长、模具、产出与数量），与移植版的形态表/模具族/标志位对齐 → **0 差异**
  （`docs/gt6-stone-extrusion-parity.json`：`gt6Rows 60`、`gt6Groups [COBBL, STONE]`、`portFlags false,false,false,false,true`）。
* `docs/stone-extrusion-coverage.json`（测试落盘）：`stoneExtrusionRecipes 1212`、`shapeRoutesVerified 996`、
  `formsThePortLacks 102`、`rockTypesWithBothGroups 27`、`collapsedRowsDropped 305`。
* `PolymerFormingRecipes` 的冲突诊断改成打印**冲突配方本身**的输入/输出（原来只说"冲突"），且只调用一次
  `findCollision`——这次正是靠它一眼定位到空壳行的。

### 27.5 明确保留的差异

* 原版 `mEqualBlocks` 是**矿物词典等价集**（其它模组注册的等价方块一起进）；移植版没有矿物词典等价机制，
  用自己注册的 `_stone` / `_cobble` 方块当等价集。
* 方块模具在 stone 组是**字面自环**（`1 <岩石> + block 模具 -> 1 <岩石>`）：原版就这么写，保留。
* GT6 会把"优化后什么都剩下不了"的行登记成空配方；移植版丢弃（305 行），差异记在此处。

改动文件：`content/recipe/StoneAndToolSurvivalRecipes.java`、`api/recipe/RecipeMap.java`、
`content/recipe/PolymerFormingRecipes.java`、`GregTech.java`、`gametest/StoneExtrusionTests.java`（新）、
`tools/check_stone_extrusions.py`（新）。**没有执行 `git commit`。**

---

## 26. 续批：GT6 主类里的配方——打印机终于能印手册，扫描仪/开箱机页签不再是空的

转译器的输入一直是 `gregtech/loaders/**`，而 GT6 还有 63 条配方写在**自己的主类**里
（`GT6_Main.java:326-403`）——既不是 `Loader_Recipes_*`，也不是注册行的图案参数，所以移植版一条都没拿到。
后果很具体：**打印机的配方表是空的**（`Printer: 0 行`），扫描仪的 JEI 页签也是空的，而"Scanner & Printer
Manual"这本手册在生存里根本拿不到（只有战利品里能捡到）。

### 26.1 移植的 12 行

| 行 | 原版 | 内容 |
|---|---|---|
| **Printer（真配方）** | `GT6_Main:352` | 普通书 + 黑色化学染料流体 → **"Scanner & Printer Manual"**（16 EU/t，256 刻）；原版 `ST.book("Manual_Printer", …)` |
| **Boxinator（真配方）** | `GT6_Main:351` | 8 纸 + 1 指南针 → 地图 |
| Scanner（Visuals，显示行） | `:331-332` | 扫描填充地图 / 工作台 → USB 棒（写入"Containing scanned …"） |
| Printer（显示行） | `:355-361` | 扫描过的 USB 棒 + 纸 → 复刻地图/方块（四种化学染料） |
| Unboxinator（显示行） | `:387-393` | 落灰指南书 → 随机手册；战利品瓶 → 随机瓶；宝石袋 → 无瑕宝石；树苗袋/种子袋/杂项袋 → 各自的战利品（这些物品正是 §21/§25 刚补上的） |

显示行用移植版 `RecipeMap.addFakeRecipe`（"只给配方查看器看，机器永远不匹配"）——与 GT6 的
`addFakeRecipe` 语义一致。

### 26.2 顺带补上 `book:` 规格

上面这些行要引用 `book:Manual_Printer` 这类规格，而化学加载器的规格解析器**不认识 `book:`**
（这个分支此前只在战利品解析器 `GTLootTables.stackOf` 里）→ 在 `GTGeneratedChem.resolveItem` 里补上
`case "book"`（委托 `GTBooks.bookStack`），两处解析器从此一致。

### 26.3 结果

| 指标 | §25 | §26 |
|---|---|---|
| 打印机配方表 | **0 行** | **3 行**（1 真 + 2 显示） |
| 扫描仪显示表 | 0 行 | 2 行 |
| 开箱机显示行 | 0 行 | 6 行 |
| GameTest | 323 | **326**（新增 `GTMainRecipesTests` 3 项：打印机印出手册且带 NBT 标题、装箱机地图行、显示行标 fake） |
| 缺失 token | 53 | 53（这些行不引用缺失内容；补的是"没被转译的行"） |

### 26.4 明确跳过（写在 `GTMainRecipes.skipped()` 里，带原版理由）

打印页（`IL.Paper_Printed_Pages`，移植版没有这些物品）、蓝图（`IL.Paper_Blueprint_*`）、
暮色森林地图、星系图纸、工业蓝本（都是别的模组）、材料词典书（`gt.matdicts` 是运行时按材料生成的）、
神秘时代/LootBags 的袋子（别的模组）。

改动文件：`content/recipe/GTMainRecipes.java`（新）、`GregTech.java`（注册入口）、
`loaders/c/GTGeneratedChem.java`（`book:` 规格 + `resolveFluidSpec` 公开）、
`gametest/GTMainRecipesTests.java`（新）。

---

## 25. 续批：GT6 的「自选战利品箱」+ 战利品瓶（把 §8 与 §21 两处推迟补完）

§8 一直记着一条保真差异「原版把宝箱换成 GT 宝箱（可配置 `gt.dungeonloot` 名字）」，§21 又把
`gt.bottles` 整张表（36 行）标成"推迟到 Loot Bottle 移植"。这一批把两处都补上——而且**不需要新箱子实现**：
移植版的 GT 金属箱就是 GT6 的 `MultiTileEntityChest`（54 格 ✓）。

### 25.1 战利品箱：17 个可放置变体

| 项 | 原版依据 | 移植版实现 |
|---|---|---|
| 机制 | 世界宝箱被换成带 `gt.dungeonloot = <表名>` 的 GT 宝箱（`ChestGenHooksChestReplacer:122`），首次打开 roll 该表（`MultiTileEntityChest:262` → `ST.generateLoot`） | `LootChestBlock extends MetalChestBlock`（复用既有箱子方块/方块实体/容器界面），表名挂在方块上 |
| 玩家自选 | `MultiTileEntityChest:275` 把 `ST.LOOT_TABLES` 每一项做成一个箱子物品 | **17 个变体** `loot_chest_<suffix>`：10 个原版分类 + 7 张 GT 表（`gt.matdicts` 不给——它的行是运行时按材料生成的材料词典书，见 §21） |
| 抽取次数 | `ChestGenHooks.getInfo("gt.books").setMin(8).setMax(24)`（`Loader_Loot:337-339`，8 张 GT 表都是 8..24） | 转译器新增 `GTLootGen.tableCounts()`；`GTLootTables.countRange(table)` 读它（原版分类沿用默认 8..24，因为原版用的是原版分类自己的计数） |
| 首次生成 | `generateDungeonLoot` 填满箱子并生成 5 个经验球（5..14 点，`MultiTileEntityChest:263-267`），然后把 `mDungeonLootName` 清空 | `MetalChestBlockEntity.generateLootIfNeeded()` → `GTLootTables.fillInto(...)` → 5 个经验球 → 存盘标记（只生成一次） |
| 触发 | 箱子 tick 时生成 | `MetalChestBlock.getTicker` 原来只在客户端返回 ticker → 服务端现在也返回（调用生成逻辑） |
| 着色 | 战利品箱用自己的（已带色的）贴图 | 客户端染色分支排除 `LootChestBlock`（否则会被材料色染一遍） |
| 资源 | GT6 `model/gt.multitileentity/lootchest.colored.png` | `tools/generate_loot_chest_assets.py`：复制贴图、写共享方块模型（金属箱模型换贴图）+ 17 个 blockstate/物品模型 + 34 条语言键（英文用 GT6 `LH.java` 的 `loot.*` 标签，如 "+Random Books+"、"+Library+"） |

### 25.2 战利品瓶（Clouded Bottle）——`gt.bottles` 解冻

GT6 `MultiItemBottles:367`：`addItem(32761, "Clouded Bottle", "Loot: A random Bottle", …,
new Behavior_Drop_Loot("gt.bottles"))` —— 它是 `gt.bottles` 的唯一发放者。移植版把它注册成
`loot_bottle`（`LootBagItem("gt.bottles")`，右键开瓶抽一瓶，原版行为），资源来自
`tools/generate_loot_bottle_assets.py`（贴图 32761 + 模型 + 双语键）。

转译器据此**不再跳过 `gt.bottles`**（19 行进入转译），并给对得上的瓶子上别名：`bottle_milk`→`milk`、
`bottle_milk_soy`→`soy_milk`、`bottle_beer`→`beer`、`bottle_blood`→`bottle_oblood`、
`bottle_glue`→`glue_bottle`、`bottle_ink`→`ink_bottle`、`bottle_slime_pink/blue`→`pink/blue_slime_bottle`。

### 25.3 顺带：两个材料别名（化学与食物各解锁一行）

| 行 | 原版 | 修法 |
|---|---|---|
| `GTChemGen:286` Roasting：铬 + 氧 → 二氧化铬 | 移植版材料叫 `ChromiumDioxide` | `registerAlias("CrO2", "ChromiumDioxide")` |
| `GTFoodGen:79` Mixer：面粉 + 水 → 面团 | GT6 `MT.java:9702` 的 `Wheat` 本地名就是 "Flour"，移植版同样 | `registerAlias("Flour", "Wheat")` |

两处都写在 `MaterialFormCorrections`（手工维护、在 `MaterialDefinitions.link()` 里执行）而不是生成的
`MaterialRegistryExtras`，避免下次重新生成时丢失。

### 25.4 结果

| 指标 | §24 | §25 |
|---|---|---|
| 新增方块 / 物品 | — | **+17 战利品箱方块 + 17 物品 + 1 战利品瓶** |
| 可用的战利品箱表 | 8 张 GT 表（只能注入原版宝箱） | **17 张可放置的箱子**（10 原版分类 + 7 GT 表） |
| GameTest | 322 | **323**（新增 `lootChestsRollTheirTables`：逐表放箱子、断言生成 >0 堆且只生成一次） |
| 缺失 token | 55 | **53** |

### 25.5 仍然留着的（带原版证据）

| token | 为什么留着 |
|---|---|
| `omd:HydratedCoal` | 原版 `MT.java:1534` 的水合煤（8335）移植版没有这个材料；它的 `BRITTLE`/`MORTAR`/`FURNACE` 在移植版属于 `MaterialWorkability` 表，要连同材料一起导入 |
| `omd:Eudialyte` | 材料 8421 缺失（§24.7 已列出） |
| `omd:Petrotheum`/`omd:Pyrotheum` | 热力膨胀（其它模组）的流体 |
| `fr:chocolatemilk` | 按 1.7.10 注册名解析的流体，待查 |
| §24.6 的那些 | `Empty` 哨兵、GT6 本就死的石头齿轮/烈焰板/钽宝石、`tech:` 其它模组 36 个 |

改动文件：`registry/GTLootChests.java`（新）、`block/inventory/LootChestBlock.java`（新）、
`block/inventory/MetalChestBlock.java`（服务端 ticker）、`blockentity/inventory/MetalChestBlockEntity.java`
（战利品生成 + 存盘标记）、`registry/GTMultiItems.java`（战利品瓶）、`client/GregTechClient.java`（箱子染色排除）、
`content/material/MaterialFormCorrections.java`（两个别名）、`gametest/LootInjectionTests.java`（战利品箱测试 + 瓶子）、
`tools/transpile_gt6_loot.py`（表计数 + 解冻 gt.bottles + 瓶名别名）、`tools/generate_loot_chest_assets.py`、
`tools/generate_loot_bottle_assets.py`（新）、`tools/report_missing_prefix_usage.py`（新，按原版引用排序缺口）、
`docs/gt6-loot-rows.json`（生成）。

---

## 24. 续批：把「形态缺口」变成内容——配方里的方块形态、石器工具头、11 个熔融流体、微型板

§23 把**条件**对齐了，这一批把「配方引用得到、物品却不存在」的缺口补成真内容。判据仍是运行时那份
token 清单（`docs/missing-content-tokens.txt`，由加载器打印的 `accepted-missing content` 现算）：
**95 → 55**，其中 `i:`（材料形态）**41 → 12**、`m:`（材料流体）**11 → 0**。

### 24.1 方块形态的配方引用现在能解析（10 个 token）

GT6 的 `OP.blockGem`/`blockIngot`/`blockPlate`/`blockRaw`/`blockSolid`/`crateGt*` 在移植版是**方块**
（`BlockPrefixRegistry`），而规格解析器的 `i:` 分支只查**物品前缀**（`MaterialPrefix`）→ 引用方块形态的
配方整行被跳过（`i:blockGem:Coal` …）。修法：`GTGeneratedChem.resolveItem` 在物品前缀查不到时回落到
`BlockPrefixRegistry.byName(...)` + `GTBlocks.getStack(...)`，并给板条箱补上 GT6 短名 ↔ 移植版
`crateGt64*` 的对应（`crateGtDust` → `crateGt64Dust`）。

### 24.2 石器工具头：原版条件是 `typemin(1)` / `qualmin(1)`（21 个 token）

| 项 | 原版（`OP.java`） | 移植版原来 | 现在 |
|---|---|---|---|
| 剑刃/镐/铲/锹/斧/锄/感知/犁 | `typemin(1)`（239-246 行：**每个**材料都有，石头也在内——GT6 的燧石/石器就是这么来的） | `TOOL_HEAD ∧ (锭∨宝石)` | `HAS_TOOL_HEAD.or(GT6_TOOL_HEAD)` |
| 锤头 | `And(typemin(1), Or(BOUNCY, STRETCHY, WOOD, qualmin(1)))`（247 行） | 同上 | 同上再 `.or(WOOD)`（原版 `Or(…, WOOD)`；木材品质是 0） |

`GT6_TOOL_HEAD` 读的是移植版的**工具品质**（`getToolQuality() >= 1`）——原版石质材料正是靠
`.qual(1, …)` 满足 `qualmin(1)`（`MT.java:1631 Stone`、`:3833 Blackstone`、`:8505 Basalt`）。
**有意不字面照搬 `typemin(1)`**：那会给泥土/水/空气也加剑刃（约 2.5 万件物品），记为保真差异（§8）。

顺带修 `SupplementalMaterials`：移植版的 Blackstone（9223）当初按「粉材料」建
（`builder(9223,"Blackstone").dust()`），没有工具属性 → `MaterialFormCorrections` 按原版
`.qual(1, 5.0, 64, 1)` 补 `setToolStats(1, 5.0F, 64, 1)`，黑石的 6 个 token 随之解析。

### 24.3 `plateTiny`：补上原版的 Paper 例外（1 个 token）

GT6 `OP.java:198 plateTiny = plate`，再加 `OP.java:617 plateTiny.forceItemGeneration(MT.Paper)`
——Paper 没有 `PLATES` 标志（`paper|DUSTS,MULTIPLATES,PLANTS`），所以要强制。移植版的 `plateTiny`
是 `plate` 的子前缀（条件已一致），补上 Paper 例外与贴图名（`material_icons/<材料>/platetiny.png`）
后，弹药压制那 15 行（弹壳 + 火药 + 黄铜微型板 → 子弹）与 Slicer 的纸微型板行都能解析。

### 24.4 11 个化合物缺熔融流体（11 个 token）

`m:Al2O3:2000` 是**流体**规格（`materialFluid`）。GT6 的 `*dcmp` 化合物工厂带 MELTING/MOLTEN
（`MT.java:1081 Al2O3`、`:1094 Fe2O3`、`:1104 CaCl2`、`:1139 Na2CO3`、`:1180 UF4` 等），所以原版有熔融
氧化铝/赤铁矿/氯化钙…；移植版导入时只留了粉 → 11 条化学配方没有流体可用。修法沿用
`FluidDefinitions.prepare()` 里已有的「补状态标志」先例：给这 11 个材料补 `MOLTEN`，随后的
`registerGeneratedMoltenFluids` 就会产出流体。

### 24.5 结果

| 指标 | §23 | §24 |
|---|---|---|
| 材料物品 | 52,835 | **55,272**（+2,437：石器工具头/原材料 + 微型板） |
| 材料方块 | 8,317 | 8,317 |
| 缺失内容 token | 95 | **55**（−40） |
| ├ `i:` 材料形态 | 41 | **12** |
| └ `m:` 材料流体 | 11 | **0** |
| GameTest | 322 | **322 全过** |

### 24.6 剩下的 55 个 token（都带原版证据）

| token | 为什么留着 |
|---|---|
| `i:bulletGt*:Empty` ×3 | 原版条件里的 `EMPTY` 是「无要求」哨兵（`Or(PROJECTILES, EMPTY)`），`Empty` 材料是隐藏伪材料（`Loader_Items:181` 不给隐藏材料注册物品）→ 原版也只给哨兵注册 |
| `i:gearGt/gearGtSmall:Stone/Basalt/Blackstone` ×6 | 原版 `TD.G_STONE = {PROJECTILES, DUSTS, PLANTS, PLATES, STICKS}` **没有 PARTS** → 原版同样没有石头齿轮 |
| `i:plate:Blaze` | 原版 Blaze（8211）的标志是 `DUSTS,PLANTS,PROJECTILES,STICKS`，**没有 PLATES** → 原版没有烈焰板 |
| `i:gem:Ta` | Tantalum（730）没有 GEMS → 原版没有钽宝石 |
| `i:gem:Eudialyte` | **原版有**（8421，`G_GEM_ORES`），移植版没有这个材料 → 见 §24.7 |
| `omd:` 6 个（CrO2/Eudialyte/Flour/HydratedCoal/Petrotheum/Pyrotheum） | 缺材料或属其它模组（Petrotheum/Pyrotheum 是热力膨胀的流体） |
| `tech:` 36 个 | 其它模组内容（bop/neli/rh/macu/wimo/tf/etfu/btl/ere/tropic/aether/salt_dirt…） |
| `fr:chocolatemilk` | 按 1.7.10 注册名解析的流体，待查 |

### 24.7 顺带量出来的数字（下一批入口）

* **材料导入缺口**（`tools/report_missing_materials.py`）：GT6 有 id 的 1081 个材料里，移植版**没有声明**的
  是 **15 个**，全是其它模组的兼容材料（Redstonia/Palis/Diamantine/VoidCrystal/Emeradic/Enori、Sunstone、
  Templerock/Mazestone/Castlerock、Hexorium×5）；另有 **Eudialyte**（只在成分表里出现、没有材料声明）。
  也就是说：非兼容材料已经**一个不缺**。
* **前缀缺口**（`tools/report_missing_item_prefixes.py`）：GT6 有条件的 215 个前缀里，移植版**既无物品也无
  方块形态**的有 **93 个**，按族分组：矿石加工中间物 13（`clump`/`crystal`/`chunk`/`rawOreChunk`…）、
  宝石形态 4（`gemRaw`/`gemUncut`/`gemPolished`/`gemOre`）、板/片 3（`sheetGt`/`compressed`/
  `plateSteamcraft`）、工具 7（移植版有 `GTToolItems`，命名不同）、护甲 5（`armor*`，移植版只有防化服）、
  导线/线缆 21、管道 5、容器 5、矿石变体 21、其它 8；另有 321 个名字**只在注册表目录里**（不等于缺口）。
  其中 `plateTiny` 这批已补，剩下的按「有配方引用才做」的原则排期。

改动文件：`loaders/c/GTGeneratedChem.java`（方块形态回落 + 板条箱短名）、`data/MaterialPrefix.java`
（工具头家族条件 + `plateTiny` 贴图与 Paper 例外）、`content/material/MaterialFormCorrections.java`
（Blackstone 工具属性）、`content/fluid/FluidDefinitions.java`（11 个化合物补 MOLTEN）、
`tools/report_missing_item_prefixes.py`、`tools/report_missing_materials.py`、
`tools/report_prefixless_delegates.py`、`tools/debug_material_ids.py`（新）。

---

## 23. 续批：前缀条件对齐（GT6 的形态标志逐条落进条件）——条件层未引用标志 6 → 0，矿石方块 +638

§22 修好了「形态**表**怎么查」，这一批做「形态**条件**怎么判」。结论：真正的条件缺口只有 6 处，
但找出来之前先把比较工具的三处失真修掉（否则会把 45 个假阳性当成缺口）。

### 23.1 先修比较工具（三处失真）

| 失真 | 原因 | 修法 |
|---|---|---|
| GT6 条件被截断 | 非贪心正则 `\.setCondition\((.*?)\)\s*(?:\.|,|$)` 遇到嵌套表达式只取到第一个 `)`（`new And(new Or(ingot, gem.NOT), PLATES)` 变成 `NOT`） | 改成括号配平取整段表达式 |
| 移植版侧看不到标志 | 只统计 `MaterialPrefix` 里的字面 `MaterialForms.has`，不展开辅助谓词（`HAS_*`、`GT6_*`、块形式的局部 `hasSmithableParts`） | 递归展开；定义行尾允许注释（`;   // GT6 OP.plantGt* = PLANTS`——否则一个定义会吞掉下一个，`GT6_PROJECTILES` 就是这样丢的） |
| 名字集不完整 | 只拿 `MaterialPrefix` 的 106 个物品前缀当"移植版有什么" | 改用 `MaterialPrefixes` 注册表 + 两侧 delegate 字段（**453** 个名字；管道/线缆/单元/护甲/矿石变体都在里面，物品前缀只是其中之一） |

修好之后数字变化：共有前缀 103 → **215**，GT6-only 122 → **10**（8 个管道族名字 + `casingSmall`/`chemtube`
的命名差异），条件层未引用的标志从「6 真 + 45 假」收敛到 **6 真**——其中 **5 处按原版修好**，
**1 处（`dustImpure`）有意不对齐**（它的条件是死标志，见 §23.3）。

### 23.2 修掉的 5 处条件 + 1 处有意不对齐

| 前缀 | GT6 条件（`OP.java`） | 移植版原来 | 现在 | 影响 |
|---|---|---|---|---|
| `dust` | `Or(DIRTY_DUSTS, DUSTS)` | `DUST \|\| GENERATE_DUST \|\| ELEMENT` | 加 `DUSTS` | 945 个 `DUSTS` 材料本来就有粉（标志与移植版属性重合），物品数不变，但条件不再靠属性近似 |
| `dustImpure` | `DIRTY_DUSTS`（**死标志**） | `GENERATE_DIRTY_DUST \|\| 处理矿` | **不改**，只加注释 | 原版**没有**不纯粉（下节），移植版保留自己的（其矿石处理链要用）→ 记为保真差异 |
| `round` | `Or(PARTS, And(PROJECTILES, nugget))` | `HAS_INGOT \|\| HAS_PARTS` | 按原版收紧 | 27 个「有锭但没有投射物标志」的材料不再凭空多出弹丸（材料物品 −27） |
| `ore` / `oreSmall` 方块 | `ORES` | 只看 `GENERATE_ORE` | 加 `ORES` | **+638 个矿石方块**：483 个 `ORES` 材料里，多数在移植版是走 `metal()`/`element()` 工厂声明的（`MaterialFactories` 不设 `GENERATE_ORE`），此前**没有矿石方块** |
| `casingMachine*` 方块（4 个） | `PARTS` | `GENERATE_PARTS ∧ SMITHABLE ∧ (锭∨宝石)` | 加 `PARTS` | 23 个材料拿到机壳：Trinium/Naquadah/Adamantium 等元素金属 + Teflon/PVC/Bakelite 等塑料（数值来自 `tools/report_block_condition_delta.py`） |

### 23.3 发现：GT6 有 8 个「死标志」

`TD.java` 声明、但 `MT.java` 里**没有任何材料**带它们（`tools/report_unused_form_flags.py`）：
`CONTAINERS_PLASMA`、`CONTAINERS_SOLID`、`DIRTY_DUSTS`、`PIPES`、`PLASMA`、`TOOLS`、`VAPORS`、`WEAPONS`。
写在这些标志上的条件在原版**恒假**——所以：

* `OP.dustImpure = DIRTY_DUSTS` → **原版不注册任何不纯粉**；移植版的不纯粉是自己的矿石处理链产物，属**有意的扩展**（不是缺口，别去"对齐"把它删掉）；
* `OP.pipe* = PIPES`、`OP.tool* = TOOLS/WEAPONS` → 原版的管道/工具不是从这些前缀来的（管道另有注册、工具走 `typemin(1)` 的工具头体系）；
* 新测试把 8 个死标志钉住（`deadGt6FlagsStayDead`）：哪天标志表让它们活过来，测试会立刻报警。
* 比较工具现在把死标志单独列出：`dead flags: 8`、`shared prefixes conditioned on a dead flag: 2
  (dust(DIRTY_DUSTS), dustImpure(DIRTY_DUSTS))`、`shared prefixes whose live GT6 flags the port never
  consults: 0`——这就是「条件层已对齐」的可复现判据。

### 23.4 结果

| 指标 | 改前 | 改后 |
|---|---|---|
| 材料物品 | 52,862 | 52,835（−27，`round` 收紧） |
| 材料方块 | 7,679 | **8,317**（+638，矿石方块） |
| 条件层未引用的 GT6 标志 | 6 | **0** |
| 共有前缀 / GT6-only 前缀 | 103 / 122 | **215 / 10** |
| GameTest | 317 | **322** |

新增 `gametest/PrefixConditionParityTests`（5 项）：①`ORES` 材料逐个断言有矿石方块与小矿石（483 个，
0 缺失）；②`PARTS` 材料逐个断言有机壳与弹丸（157 个，0 缺失）；③`DUSTS` 材料逐个断言有粉（945 个）并把
`DIRTY_DUSTS` 钉成死标志；④8 个死标志必须保持 0 承载材料；⑤报告落盘 `docs/prefix-condition-parity.json`
（`ORES 483 / PARTS 157 / DUSTS 945 / PROJECTILES 714 / PLATES 722 / GEMS 190 / WIRES 14 / DIRTY_DUSTS 0`）。

**隐藏伪材料例外**：`Loader_Items:181` 与 `Loader_Blocks:52` 都不给 `HIDDEN` 材料注册物品/方块
（`Ma`「Magic」、粒子、哨兵），测试用同一判据（`PrefixConditionParityTests.registerable`）。

### 23.5 诚实的覆盖边界（下一批的入口）

215 个共有前缀里 **122 个**的条件在静态比较里可见（物品前缀 + 方块 builder），其余 **93 个**是移植版
用专用注册表实现的族——线缆/导线 16 个尺寸、管道 5、单元/胶囊、护甲 5、矿石变体、GT6 的碎块-晶体
中间物（`clump`/`crystal`/`dustPure`…）——静态比较对它们只能说「名字存在」，其中 **74 个**带 GT6 标志
（`tools/report_unparsed_prefix_conditions.py` 逐条列出标志）。这些要靠**运行时断言**逐族补：
`WIRES`→导线、`PIPES`（死标志，跳过）、`ARMORS`→护甲、`CONTAINERS`→单元/胶囊、`ORES`→矿石变体
（已通过 `ore`/`oreSmall` 覆盖主线）。

### 23.6 工具

重写 `tools/compare_prefix_conditions.py`（括号配平 + 辅助谓词展开 + 注册表名字集，输出
`docs/prefix-condition-comparison.json`）；新增 `tools/report_unparsed_prefix_conditions.py`（93 个未解析族）、
`tools/report_block_condition_delta.py`（矿石/机壳的量化）、`tools/report_unused_form_flags.py`（8 个死标志）、
`tools/debug_prefix_conditions.py`（单前缀解析调试）；删除被取代且结论失真的 `tools/report_prefix_condition_gaps.py`；
`tools/report_gt6_only_prefixes.py`、`tools/probe_port_form_families.py` 保留（分类与"这些族在移植版哪里"）。

改动文件：`data/MaterialPrefix.java`、`data/MaterialPrefixes.java`、`gametest/PrefixConditionParityTests.java`（新）、
`tools/compare_prefix_conditions.py`（重写）+ 上述新工具、`docs/prefix-condition-comparison.json`、
`docs/prefix-condition-parity.json`（生成）。

---

## 22. 续批：材料形态对齐（形态表按 GT6 材料 **id** 查）——48 个材料找回原版形态，战利品最后 3 行清零

§21 结尾留的「材料形态对齐」这一批。根因不在条件表达式，而在**形态表的键**：

### 22.1 根因（四条，都是查表本身的问题）

| 问题 | 证据 | 后果 |
|---|---|---|
| 表按 **GT6 字段名**键控，运行时却按**移植版材料名**查 | `MaterialForms.DATA` 里是 `ke`/`nq`/`mossy`，而 `of()` 用的是 `normalize(material.getName())`（`Trinium`/`Naquadah`/`WoodMossy`） | **48 个材料**的形态标志永远查不到（`tools/report_material_form_lookup.py`：226 个可 join 的材料里 176 个能查到、48 个丢失） |
| 工厂**定义**被当成了材料声明 | `DECL_STATIC` 原先匹配 `OreDictMaterial\s+(\w+)\s*\(`，于是 `MT.java:468 static OreDictMaterial gold () {…}` 生成了一个名为 `gold` 的伪材料 | 规范化后与木材 `Gold`（9369）撞键，HashMap 后者覆盖前者；`clay()` 工厂还生成了只有 `PLATES` 的伪 `clay`，**把真 Clay 的 `ORES/DUSTS/PLANTS` 顶掉了** |
| 名字规范化后互相撞键 | `Co`（钴，270）vs `CO`（一氧化碳，9838）、`HF`（9829）vs `Hf`（铪，720）、`NO`（9837）vs `No`（锘，1020） | 撞键双方只能活一个；现在这些名字整条不写、全部由 id 行覆盖（`tools/report_material_form_key_collisions.py`：生成表 **0 个重复键**，3 组撞键均 "covered by id"） |
| 声明走**零参工厂**时拿不到 id | `Au = gold()`（`MT.java:636`）→ `gold()` 返回 `noblemetal(790, …)` | 220 个材料的 id 抓不到（金、绝大多数元素）；新增 `resolve_id()` 沿工厂链取 id 后只剩 79 个（这些靠名字键覆盖） |

### 22.2 改动

| 项 | 值 |
|---|---|
| 生成器 | `tools/extract_gt6_form_flags.py`：`DECL_STATIC` 要求赋值（不再把工厂定义当声明）；新增 `DEFINITION` 跳过工厂体；新增 `resolve_id()` 沿工厂链解析 id；`DATA` 现在同时写 **id 行**（1081 条）与**无歧义的名字行**（1151 条），撞键的名字直接不写 |
| 查表 | `MaterialForms.of()`：先按 `material.getId()` 查，再按规范化名字查（移植版自带原版 id，见 `check_material_id_agreement.py`：226 个 join 上的材料**名字全部一致**，不存在 id 错配） |
| 箭头镞条件 | `MaterialPrefix.toolHeadArrow` 由 `HAS_TOOL_HEAD` 改为 `HAS_TOOL_HEAD.or(GT6_PROJECTILES)`：原版 `OP.java:254` 的条件就是 `And(PROJECTILES, typemin(1))`——箭镞看的是投射物标志，不是工具品质（移植版保留自己那套更严的 `HAS_TOOL_HEAD` 作为并行来源） |

### 22.3 结果

| 指标 | 改前 | 改后 |
|---|---|---|
| 材料物品 | 49,486 | **52,862**（+3,376） |
| 材料块 | 7,368 | **7,679**（+311） |
| 数据包配方产物 | 30,107 | **31,746**（+1,639，新形态自动被挤出机/装箱机等表接手） |
| 机器配方行 | 215,207 | **230,900**（+15,693） |
| 转译配方集（`set.*`） | added 1,485 / skipped 603 | **added 1,493 / skipped 595**（化学 305→307、矿石 95→101） |
| 形态换算配方 | 装箱 12,346 / 压缩机 1,259 / 拆箱 7,691；工作台 24,682 | **12,996 / 1,303 / 8,109；26,027** |
| 缺失内容 token | 102 | **96**（`i:` 材料形态 token 47 → 41） |
| 战利品注入行 | 88（3 行未解析） | **91（0 行未解析）** |
| GameTest | 314 | **317** |

新增 `gametest/MaterialFormParityTests`（3 项）：①Trinium/Naquadah/Goldwood/WoodMossy 的标志按 id 查得到（`PROJECTILES`/`INGOTS`/`FOILS`…）；②撞键不再互相顶替——Clay 保留 `ORES/DUSTS`（以前被 `clay()` 伪材料顶成只剩 `PLATES`）、金属金（790）有 `RAILS/INGOTS` 而木材 Goldwood（9369）没有；③这些标志真的产生了物品（Trinium 有箭镞与木箭、Mossy Wood 与 Naquadah 有箔）。`LootInjectionTests` 的「已知缺口」清单改为**空**（原来钉住的 3 行 Trinium/Naquadah 箭镞现在能解析）。

`MaterialFormCorrections` 里 Trinium 的手工形态补丁（`MT.Ke is Trinium (1260)` 那段）**保留**：形态判定现在由原版标志说了算，但移植版另有子系统直接读 `MaterialProperty`，删掉会连带影响它们。

**完整性核对**（`tools/report_material_form_unreachable.py`）：1061 个移植版材料声明里 **1053 个**能查到原版标志，剩下 8 个在原版本来就没有形态——Photon/Neutrino/Neutron/Proton/Electron 是粒子（`MT.java:531-534` 的 `PARTICLE` 不是物品生成标志）、`Superconductor` 走 `tier()`（id −1）、`Leather`/`ClayBrick` 的 `FLAMMABLE`/`MORTAR`/`BRITTLE` 同样不是生成标志。

新增核对工具：`tools/report_material_form_lookup.py`（还有多少材料的标志查不到）、`tools/report_material_form_idless.py`（哪些声明抓不到 id）、`tools/report_material_form_unreachable.py`（全部移植版材料的可达性）、`tools/check_material_forms.py`（抽查 id 行/名字行）、`tools/report_material_form_key_collisions.py`（生成表重复键与撞键覆盖情况）、`tools/check_material_id_agreement.py`（id 与材料名是否一致）、`tools/show_material_form_entry.py`（查单个声明的 flags/sets/ids）。

### 22.4 仍然存在的形态差异（有意保留，已量化）

| 差异 | 移植版 | 原版 |
|---|---|---|
| 工具头（剑刃/镐头/斧头…）范围 | 只给 `TOOL_HEAD && (锭\|宝石)` 的材料（`MaterialPrefix.HAS_TOOL_HEAD`） | `OP.java:239-252` 的条件是 `typemin(1)`——**每个**材料都有工具头（约 1271 × 20 ≈ 2.5 万件物品） |
| 前缀总数 | 106 个 | 217 个（`docs/prefix-condition-conditions.json`：共有 103 个，其余是移植版没有的前缀） |
| 个别条件写法 | 例：`wireFine = GENERATE_WIRE \|\| (HAS_PARTS && HAS_INGOT) \|\| WIRES` | `Or(WIRES, And(PARTS, SMITHABLE))`（现在 `WIRES` 标志能查到更多材料，差异缩小但写法仍不同） |

改动文件：`tools/extract_gt6_form_flags.py`、`data/generated/MaterialForms.java`（生成）、
`data/MaterialPrefix.java`、`gametest/MaterialFormParityTests.java`（新）、`gametest/LootInjectionTests.java`、
`tools/report_material_form_lookup.py`（改）、`tools/report_material_form_idless.py`、
`tools/check_material_forms.py`、`tools/report_material_form_key_collisions.py`、
`tools/check_material_id_agreement.py`、`tools/show_material_form_entry.py`、
`tools/report_material_form_unreachable.py`（新）、`docs/gt6-form-flags.json`（生成）。

---

## 21. 续批：GT6 的书（16 本手册）+ 战利品机制**改正**（世界宝箱按原版给，GT 自己的表交给袋子）

§20 那批做完后回头核对原版机制，发现**世界宝箱那一半做错了**：原版世界宝箱里出的是
「原版分类表 + GT 加进去的行」，GT 自己的那几张表（gt.gems/flawless/misc/seeds/saplings/books）
**从来不由世界宝箱 roll**，而是由「战利品袋」和「落灰指南书」在玩家右键时 roll。这一批把它改正，
并把 GT6 的书一并移植（书 = 手册，正是 gt.books 表的内容）。

### 21.1 原版机制（这次是逐行读出来的）

| 链条 | 原版证据 |
|---|---|
| 世界宝箱被替换成 GT 宝箱 | `Loader_Loot:45-56` 给 10 个原版分类各注册一个 `ChestGenHooksChestReplacer`；`ChestGenHooksChestReplacer:122` 放下的 GT 宝箱带 `gt.dungeonloot = <原版分类名>` |
| GT 宝箱 roll **原版分类表** | `MultiTileEntityChest:262` → `ST.generateLoot` → `ChestGenHooks.getItems(名字)`（`ST.java:1026-1033`） |
| 所以进世界宝箱的是 `addLoot(ChestGenHooks.X, …)` 那批行 | `Loader_Loot` 里共 91 行（`ChestGenHooks.DUNGEON_CHEST`、`STRONGHOLD_LIBRARY`、`VILLAGE_BLACKSMITH` …）；GT6 每个地牢房间放箱子时也点名这些分类（`DungeonChunkCorridor3:48`、`DungeonChunkRoomWorkshop:53-132`、`DungeonChunkBarracks:120` …） |
| `gt.*` 表由**物品行为**发放 | `Behavior_Drop_Loot("gt.misc")`（`gregapi/item/multiitem/behaviors/Behavior_Drop_Loot.java:49-56`：右键方块 → 消耗 1 个 → 每张表 `ChestGenHooks.getOneItem` 掉 1 堆） |
| 四个袋子 + 两本书 | `MultiItemRandomTools:583-586`：Bagged Sapling→gt.saplings、Seed Pouch→gt.seeds、Gem Pouch→gt.flawless+gt.gems×2（"1 Flawless Gem and some other Gems"）、Loot Pouch→gt.misc；`MultiItemBooks:67-68`：Dusty Guide Book→gt.books、Dusty Material Dictionary→gt.matdicts |
| 手册的正式来源 | 原版宝箱里的 `IL.Book_Loot_Guide`（地牢 w50、图书馆 w40、村庄 w40）→ 右键 → gt.books → 随机一本手册 |

### 21.2 移植内容

| 项 | 值 |
|---|---|
| 世界宝箱注入（改正） | `content/loot/LootTableInjection.java`：把 GT6 的 `van.<ChestGenHooks 常量>` 行注入对应的 1.20.1 宝箱表，**10 张**：simple_dungeon(21 行)、abandoned_mineshaft(17)、stronghold_library(1)、stronghold_crossing(3)、stronghold_corridor(6)、desert_pyramid(3)、jungle_temple(7)、jungle_temple_dispenser(2)、village/village_weaponsmith(23)、spawn_bonus_chest(5)，共 **88 行**（每箱 1 次 roll，原版战利品保留） |
| 表模型 | `content/loot/GTLootTables.java`（新）：`table\|weight\|min\|max\|spec` 解析成 `Row`，`roll(table, random)` 复刻 `ChestGenHooks.getOneItem`（按权重抽 1 行，堆叠数在 min..max 之间均匀） |
| 战利品袋 | `content/loot/LootBagItem.java`（新）：右键方块 → 按 `Behavior_Drop_Loot` 逐表掉落 + 消耗 1 个 + 播放原版的布料音效；4 个袋子在 `GTMultiItems` 注册行为（Bagged Sapling / Seed Pouch / Gem Pouch / Loot Pouch） |
| 落灰指南书 | 新物品 `gregtech:dusty_guide_book`（GT6 `MultiItemBooks:67` 的 32765，贴图直接取自原版资源包），右键 → 从 `gt.books` 抽 1 本手册；lang 里带上原版 tooltip 与「右键方块开启」提示（`Behavior_Drop_Loot.getAdditionalToolTips`） |
| 书的数据 | `content/book/GTBooksGen.java`（生成）+ `content/book/GTBooks.java`：**16 本**成书，沿用原版 `UT.Books.createWrittenBook` 的语义（`title`/`author`/`pages` 写进原版 `minecraft:written_book` 的 NBT，`¶`→换行，**≥256 字符的页按原版丢弃**），页码 6~133 页 |
| 生成器 | `tools/transpile_gt6_loot.py`（改：解析 `ChestGenHooks.*` 行、meta 感知的原版物品映射、跳过表/行都带原因）、`tools/transpile_gt6_books.py`（新）、`tools/report_gt6_loot_rows.py`、`tools/report_gt6_books.py`、`tools/find_multiitem_id.py`（新，查移植版多物品 id 用） |

**meta 感知的原版映射**（否则 6 种花会变成同一种）：`Blocks.double_plant` 0..5 →
向日葵/丁香/高草/大蕨/玫瑰丛/牡丹，`Blocks.tallgrass` 0 → 枯木、`Items.dye` 3 → 可可豆，
`Items.reeds` → 甘蔗；这些行原版是带 meta 的，1.20.1 拆成了独立物品。

### 21.3 明确不做的部分（生成器 `skipped` 里逐条带原因）

| 行数 | 原因 |
|---|---|
| 102 + 9 + 1 + 1 + 1 | 其它模组的物品（`MD.*`、Thaumcraft 研究纸、EtFu/GaSu/BoP 种子……） |
| 36 | `gt.bottles` 整表**推迟**：它的发放物品是原版的「Loot Bottle」（移植版没有），行内容是各种瓶装液体 |
| 12 | 板条箱（`OP.crateGt*`）——移植版把板条箱注册成方块，不是材料物品 |
| 11 | GT 硬币（`MultiTileEntityCoin.COIN_MAP`，移植版没有硬币物品） |
| 4 | 「材料词典」书（gt.matdicts 表是运行时按材料数据生成的） |
| 4（本节当时的快照） | 原先未接入的 `Manual_Elements`/`Manual_Alloys`/`Manual_Smeltery`/`Manual_Tools`；2026-09-23 已核实后两本只含固定文本并接入，现仅剩前两本动态手册（"Book of Periods" 每个元素一页） |
| 3 + 2 + 1 + 1 + 1 | 按材料循环展开的行 / 罐头 / 打火机 / 火柴盒 / 药丸 / 化学试管 |

### 21.4 还差 3 行（已用测试钉住，属于**材料形态对齐**那一类）

| 行 | 为什么没进 |
|---|---|
| `PYRAMID_DESERT_CHEST toolHeadArrow:Nq` | Naquadah（`MT.Nq`）在原版**有**箭镞：`G_INGOT_MACHINE_ORES`（`TD.java:610`）含 `PROJECTILES`，而 `OP.toolHeadArrow` 的条件正是 `And(PROJECTILES, typemin(1))`（`OP.java:254`） |
| `PYRAMID_JUNGLE_CHEST toolHeadArrow:Ke` | Trinium（`MT.Ke`）同理 |
| `PYRAMID_JUNGLE_DISPENSER arrowGtWood:Ke` | `OP.arrowGtWood` 的条件是 `Or(toolHeadArrow, EMPTY)`（`OP.java:281`），Trinium 有箭镞就有木箭 |

移植版这边不满足：`MaterialPrefix.HAS_TOOL_HEAD` 要求 `TOOL_HEAD` + 锭/宝石，而 `GT6_PROJECTILES`
按**移植版材料名**查 `MaterialForms`，那张表却是按 **GT6 字段名**（`ke`/`nq`）键控的。
这批先用测试把这个缺口钉死（`LootInjectionTests.EXPECTED_UNRESOLVED` 精确比对 3 条），
留给「材料形态对齐」那一批一起修（`MaterialFormCorrections` 里本来就有 Trinium 的手工补丁）。

### 21.5 验证

```
GT loot: 5 rows added to minecraft:chests/spawn_bonus_chest (van.BONUS_CHEST)
GT loot: 21 rows added to minecraft:chests/simple_dungeon (van.DUNGEON_CHEST)
...（10 张表共 88 行）
========= 314 GAME TESTS COMPLETE ======================
All 314 required tests passed :)
```

无配方物品：**仍然 0**（checked 363；新区指南书属于多物品族，不在该统计内 —— 原版它也没有配方，只能捡）。

新增/改写 `gametest/LootInjectionTests`（5 项）：箱表每一行都能解析（缺口精确比对）、10 张箱表各 roll 12 次都出 GT 物品、
4 个袋子按原版的表发放（Gem Pouch 一次 3 堆）、指南书 roll 出带 NBT 的原版成书、报告落盘
`docs/gt6-loot-report.json`；书本身另断言 16 本、标题/作者/页数、每页 <256 字符。

改动文件：`content/loot/GTLootTables.java`（新）、`content/loot/LootBagItem.java`（新）、
`content/loot/LootTableInjection.java`（改写）、`content/book/GTBooks.java`、`content/book/GTBooksGen.java`（生成）、
`registry/GTMultiItems.java`、`gametest/LootInjectionTests.java`（改写）、
`tools/transpile_gt6_loot.py`（改）、`tools/transpile_gt6_books.py`（新）、
`tools/generate_guide_book_assets.py`（新）、`tools/report_gt6_loot_rows.py`、`tools/report_gt6_books.py`、
`tools/find_multiitem_id.py`（新）、`docs/gt6-loot-rows.json`、`docs/gt6-books.json`、`docs/gt6-loot-report.json`（生成）。

---

## 20. 续批：GT6 的战利品（loot）——世界宝箱开始出 GT 的东西

> **⚠ 本节机制已被 §21 改正**：世界宝箱注入的是 GT6 加进**原版分类表**的行（`addLoot(ChestGenHooks.X, …)`），
> GT 自己的表（gt.misc/gems/flawless/…）由**战利品袋 + 落灰指南书**发放，不由世界宝箱 roll。
> 本节下面写的「把宝物表塞进 8 个原版宝箱」是当时的近似做法，§21 已按原版逐行核对后替换。

§2 里「战利品表 loot（原版 2 文件 / 移植版 0）」这一格补上。

### 20.1 原版机制

`Loader_Loot`（413 处 `addLoot`，570 行）定义 8 张 GT 战利品表，并用
`ChestGenHooksChestReplacer`（`gregtech/worldgen/`）**把原版宝箱换成 GT 宝箱**——GT 宝箱按
`gt.dungeonloot` 名字（`MultiTileEntityChest:275` 把 `ST.LOOT_TABLES` 里每一项都做成一个宝箱物品）roll 某张表。
移植版没有 GT 宝箱方块，所以这一批做**决定玩家捡到什么**的那一半：把 GT 的**宝物表**加进原版那 8 个宝箱。

### 20.2 移植内容

| 项 | 值 |
|---|---|
| 新工具 | `tools/transpile_gt6_loot.py`（`addLoot(table, weight, min, max, stack)` → `table\|weight\|min\|max\|spec`，规格语法沿用化学加载器那套） |
| 生成表 | `loaders/c/GTLootGen.java`：**112 行**（gt.misc 38 / gt.flawless 20 / gt.gems 13 / gt.bottles 19 / gt.saplings 12 / gt.seeds 10） |
| 注入 | `content/loot/LootTableInjection.java`：`LootTableLoadEvent` 给原版那 8 个宝箱表加一个 GT 池（**每箱 71 行**，权重与 min/max 照原版），**原版战利品保留**（只是多一个池） |
| 涉及宝箱 | `simple_dungeon`、`abandoned_mineshaft`、`stronghold_library`、`stronghold_crossing`、`desert_pyramid`、`jungle_temple`、`village/village_weaponsmith`、`nether_bridge`（对应原版 `Loader_Loot:45-56` 的 8 个 ChestGenHooks 分类） |
| 复用 | `GTGeneratedChem.resolveSpec(...)` 提升为 public，战利品规格与机器配方共用同一套解析（材料形态、`tech:` 物品、命名空间别名都在里面） |

原版 1.7.10 → 1.20.1 的物品改名也带上了（`Items.record_13` → `music_disc_13` 等 12 张唱片、`brick_block`→`bricks`、
`hardened_clay`→`terracotta`、`snow_layer`→`snow`、`waterlily`→`lily_pad` …），另有 2 处 GT6 物品名与移植版不一致的直译
（`porcelain_cup` → `modeled_porcelain_cup`、`MT.OREMATS.Zeolite` 的材料嵌套类前缀）。

### 20.3 明确不做的部分（都在生成器的 `skipped` 里带原因）

| 行数 | 原因 |
|---|---|
| 102 | 其它模组的物品（`ST.make(MD.*, …)`）——移植版不注册那些模组 |
| 6 | 成书（`ST.book("Manual_…")`）——移植版还没有书物品 |
| 3 | 按材料循环展开的行（`OP.gem.mat(tMaterial, 1)`，原版对带 `RANDOM_SMALL_GEM_ORE` 标志的材料逐个展开） |
| 3 + 2 + 1 + 1 | 罐头食品 / 打火机 / 火柴盒 / 药丸——GT6 用变体 meta，移植版按显示名注册了另一套 id |
| 1 | 化学试管（移植版没有 chemtube 物品） |
| ~151 | 兼容模组的树苗行（`MD.<mod>.owns(...)` 守卫，连解析都没进） |

GT6 另外几张表（`gt.seeds`/`gt.saplings`/`gt.bottles`/`gt.books`/`gt.matdicts`，共 71 行）已转译但**没有注入**：
在原版里它们属于"玩家自己配置名字的 GT 宝箱"，移植版没有那个方块，硬塞进地牢箱会凭空出现种子/树苗/瓶子，
所以留着等 GT 宝箱方块那一批。

### 20.4 验证

```
GT loot: 71 rows added to minecraft:chests/simple_dungeon (0 specs unresolved)
========= 311 GAME TESTS COMPLETE ======================
All 311 required tests passed :)
```

新增 `gametest/LootInjectionTests`（2 项）：宝物表的每一行都能解析成移植版物品（0 未解析），
以及**真的 roll 得出来**——用 `LootParams` 对 8 个宝箱表各 roll 12 次，断言出现 `gregtech:` 命名空间的物品。

改动文件：`tools/transpile_gt6_loot.py`（新）、`loaders/c/GTLootGen.java`（生成）、
`content/loot/LootTableInjection.java`（新）、`loaders/c/GTGeneratedChem.java`（`resolveSpec` 公开）、
`gametest/LootInjectionTests.java`（新）、`docs/gt6-loot-rows.json`（生成）。

---


§18.3 收尾时剩下的 9 个 GT6 原生 token（4 种绳索 + `boomstick` + `strong dynamite` + `rope`/`dynamite` 的配方）
这一轮做掉，顺带发现移植版的绳索/炸药**一直在用未着色的灰度贴图渲染**（原版用材料色给同一张灰度图上色）。
## 19. 续批：绳索与炸药全家（7 个新方块）+ 两个方块的着色修正 —— GT6 原生缺失内容清零


### 19.1 七个新方块（`Loader_MultiTileEntities:2086-2091`、`:2235-2237`）

| 方块 | 原版材料（= 染色） | 说明 |
|---|---|---|
| `rope_silk` | `MT.White` | 丝绳 |
| `rope_grass` | `MT.Yellow` | 草绳 |
| `rope_vine` | `MT.Green` | 藤蔓绳 |
| `rope_plastic` | `ANY.Plastic` | 塑料绳 |
| `rope_steel` | `ANY.Steel` | 钢绳 |
| `boomstick` | `MT.Orange` | 爆竹，`NBT_QUALITY` 10 |
| `strong_dynamite` | `MT.Purple` | 强力炸药，`NBT_QUALITY` 40（**四倍爆炸半径**） |

共用 GT6 的灰度模型（`block/machine/tool/rope` 与 `/dynamite`），因此只需 blockstate + 物品模型（`tools/generate_rope_dynamite_assets.py`），
颜色来自新加的方块/物品着色器分支（`GregTechClient`）。

**顺带修的两个渲染缺陷**：`rope` 与 `dynamite` 这两个既有方块此前**没有注册着色器**，而它们的贴图是纯灰度
（实测 `rope/colored.png` 均值 (224,224,226)、`dynamite/colored|*` 均值 (135.9,135.9,135.9)），
所以它们一直渲染成灰白色；现在按原版材料上色（绳子 `MT.Brown`、炸药 `MT.Red`）。

### 19.2 配方

* **绳索**（注册行的图案参数，`CR.DEF_REV_NCC` ⇒ 戴工具、不镜像，`tools/generate_rope_dynamite_recipes.py`）：

```
rope_silk   " S"  / "SS"  / "Sq"     S 线,           q 剪刀
rope_grass  " GG" / "GGG" / "GGq"    G 干草,         q 剪刀
rope_vine   " V"  / "VV"  / "Vb"     V 藤蔓,         b 剑（原版 craftingToolBlade = 剑）
rope_steel  " P"  / "PP"  / "Px"     P 钢细导线,     x 剪线钳
boomstick   " S " / "PGP" / "DGD"    S 线, P 纸, G 火药粉, D 二氧化硅粉
```

* **炸药家族**走**压床**（`Loader_Recipes_Other:644-655`）：这些行早就转译进 `GTOtherGen` 了，
  只是产物 `tech:boomstick`/`dynamite_strong` 解析不出来；方块一注册就全部生效。

**两条有意不生成的行**（都写进了生成器注释）：

1. `rope` 本体：原版那行用矿物词典 `cropHemp`，而该词条只有 HBM / Immersive Engineering 的兼容加载器会填
   （`LoaderItemData:840-841`）——**没有那两个模组时原版这条配方同样是死的**。
2. `rope_plastic`：需要塑料细导线，而移植版 `wireFine` 的条件（`GENERATE_WIRE` 或 `PARTS+INGOT` 或形态表 `WIRES`）
   对塑料不成立 → 这是**材料形态缺口**，不是配方缺口，留给形态批次。

### 19.3 别名与审计口径

`IL.Dynamite_Strong` 转译成 `tech:dynamite_strong`，而方块按显示名注册为 `strong_dynamite` →
补别名（`GTTechnological.ALIASES`，本批第 21 条）。另外审计脚本原先只认 `.register("id"` 字面量，
看不到经辅助方法注册的 id，现已一并扫描 `rope("id"`/`dynamite("id"`。

| `docs/tech-item-tokens.json` | §18 末 | 本批后 |
|---|---|---|
| OK | 58 tokens / 477 配方 | **64 tokens / 485 配方** |
| MISSING | 43 tokens / 66 配方 | **37 tokens / 58 配方** |
| 其中 GT6 原生 | 9 | **0** |

剩下 37 个全部是**其它模组的内容**（`bop_*`/`neli_*`/`macu_*`/`etfu_*`/`tf_*`/`rh_*`/`wimo_*`/`aether_*`/`tropic_*`/`btl_*`/`ere_*`/`salt_dirt_*`），
移植版有意不注册这些模组 —— 也就是说**转译配方引用的 GT6 原生内容已全部落地**。

### 19.4 验证

```
========= 309 GAME TESTS COMPLETE ======================
All 309 required tests passed :)
```

新增 `gametest/ToolBlockTests`（3 项）：6 种绳索的注册与染色值、3 种炸药的染色与爆炸强度（10 : 40 比例）、
绳索配方存在且配料可解析且不镜像、三种炸药在压床里都有产出行。
`docs/items-without-recipes.json` 仍是 **withoutRecipe 0**（机器行 215,207）。

改动文件：`registry/GTToolBlocks.java`（7 个方块 + 2 个注册辅助）、`block/tool/RopeBlock.java`、
`block/tool/DynamiteBlock.java`（染色 + 可配置爆炸强度）、`client/GregTechClient.java`（方块/物品着色）、
`tools/generate_rope_dynamite_assets.py`（新）、`tools/generate_rope_dynamite_recipes.py`（新）、
`recipes/tool_blocks/`（5 个 json）、`assets/.../blockstates|models/item`（7 组）、`lang/{en_us,zh_cn}.json`、
`registry/GTTechnological.java`（第 21 条别名）、`tools/audit_tech_item_tokens.py`、`gametest/ToolBlockTests.java`（新）。

---

## 18. 续批：§17.3 的两条尾巴 + 引用拼写对齐（三项待办一次做完）

上一轮末尾列的三个待办，这一轮全做完：

### 18.1 石头/砖类材料的形态标志（原版 `TD.G_STONE`）

**根因不是材料表缺项，而是提取器（`tools/extract_gt6_form_flags.py`）读不到原版的工厂链**：

* 原版把工厂层层转调（`brick()` → `stone()` → `create(..., G_STONE, ...)`），而**转调的 1 参重载写在被调重载前面**；
  旧实现只按文件顺序扫一遍，于是所有"向前转调"的工厂都解析成空集。
* 同一个工厂名有多个重载，**重载决定形态集**：`metal()` 的 element 重载带 `G_INGOT_ORES`（有矿石），
  合金重载只有 `G_INGOT`（没有矿石）。旧的"取最后一个重载"会随机丢/多给矿石。

改成：按参数个数（含 `Object...` 变长参数的判定）选择重载 + 工厂链求不动点。

| 结果 | 前 | 后 |
|---|---|---|
| 有形态的材料 | 1245 | **1271** |
| 解析不出形态的声明 | 674 | **648** |
| 形态发生变化的材料 | — | 42（PLATES/STICKS/PROJECTILES 各 +17、ORES +11、DUSTS +42、PLANTS +40） |

新增形态的正是 `MT.STONES` 里用 `brick(...)`/`stonecent(...)`/`stoneelec(...)` 声明的那一族
（Blackstone、Basalt、Marble、GraniteBlack、Limestone、Komatiite…），与原版一致：
`TD.java` 的 `G_STONE = {PROJECTILES, DUSTS, PLANTS, PLATES, STICKS}` 没有齿轮/工具头。

**挤出机配方**：跳过 186 → **176**，材料物品 49,269 → **49,486**、材料块 7,354 → **7,368**；
§17.3 里那 10 条「Blackstone 缺板/杆」的行**全部落地**，剩下的 176 条是原版自己就没有形态的齿轮/工具头行。

### 18.2 `allow_mirror`：按原版逐行位掩码写入

`tools/census_gt6_recipe_mirror_flags.py` 把原版 1188 条 `CR.shaped` 按位掩码分类（`CR.java:131-163`，
`MIR = B[0]`，`DEF = BUF|NO_REM`）：**只有 93 条设了 MIR**，其中 37 种图案（11 种带工具符号），
其余 1095 条**不允许镜像**。`docs/gt6-crafting-mirror-flags.json` 落盘"图案签名 → 是否允许镜像"。

`tools/apply_recipe_mirror_flags.py` 按这张表给移植版的 `gregtech:tool_shaped` 配方写入答案：
**243 条**数据包配方补 `"allow_mirror": false`（189 条能在普查表里对上图案，其余按原版默认不允许处理），
外加两个运行期配方包 `BasicMachineRecipePack` / `MultiblockRecipePack`（62 台机器 + 多方块的注入配方）
也在代码里显式关闭镜像，还有 §16 那批 525 条运行期手工配方（`Loader_HandToolCraftingRecipes`
原来用的是默认 `true`，同批修掉）。工具带 `_mirror_flags: "gt6-census"` 标记，可重复执行、可 `--check`；
手写批次（§16/§17 生成的 130 条数据包配方）保持各自显式值不动。

**实测（游戏内报告 `docs/tool-shaped-mirror-report.json`，共 1188 条 `tool_shaped`：
数据包 373 + 机器/多方块 290 + 手工 525）**：

| 项 | 值 |
|---|---|
| `allow_mirror` 为真的行 | **0**（原版 93 条 MIR 行全是楼梯/台阶/木材造型，本移植版没有这些配方） |
| 图案左右不对称（镜像可观测）的行 | 183 |
| 配料匹配不到任何物品的行 | 0 |

新测试 `HandCraftingTests.toolShapedRecipesDoNotMirrorUnlessTheOriginalDid`：遍历全部 1188 条，
断言没有一行允许镜像、且配料都能解析；图案分布由 `mirrorRuleReportIsWritten` 落盘，
`tools/report_tool_shaped_mirror.py` 汇总复核。

### 18.3 引用拼写对齐：`tech:` token 的别名与三类假阴性

`tech:` 规格由转译器按 `snake(IL 字段)` 生成，而多物品注册表用的是 `snake(显示名)`，两者不一致的行会被当成"内容缺失"跳过。

`tools/derive_missing_tech_aliases.py`（新）从原版源码推导两者的对应关系（`MultiItem*` 的 `addItem(id, "显示名")`、
`LoaderItemList` 的 `ST.make(Items.x, …)`），生成 **20 条别名**（写进 `GTTechnological.ALIASES`，同一张表也被审计脚本读取）：

```
food_butter→butter  food_butter_salted→salted_butter  food_cheese→cheese  food_dough→dough
food_dough_abyssal→abyssal_dough  food_potatochips→potato_chips  food_chilichips→chili_chips
clay_ball_{brown,red,yellow,blue,white}→{color}_clay  remains_plant→plant_remains
crop_wheat→minecraft:wheat  bale_wheat→minecraft:hay_block
dye_bonemeal→minecraft:bone_meal  food_potato_poisonous→minecraft:poisonous_potato
module_{stone,basalt,blackstone}_generator→{...}_generator_module   (来自 §17)
```

`GTGeneratedChem` 的 `tech:` 解析现在支持**带命名空间的别名**（`minecraft:bone_meal` 等），
审计脚本 `tools/audit_tech_item_tokens.py` 也修掉四处假阴性：①只认科技物品表（现在并上多物品与方块 id）、
②别名表解析被注释里的 `");"` 截断、③带命名空间的别名一律算缺失、④多物品条目里 tooltip 列为空串的行被漏掉。

| `docs/tech-item-tokens.json` | 前 | 后 |
|---|---|---|
| OK | 36 tokens / 342 配方 | **58 tokens / 477 配方** |
| MISSING | 65 tokens / 201 配方 | **43 tokens / 66 配方** |

剩下 43 个 token 里 **34 个是其它模组的内容**（`bop_*`/`neli_*`/`macu_*`/`etfu_*`/`tf_*`/`rh_*`/`wimo_*`/`aether_*`/`tropic_*`/`btl_*`/`ere_*`，本移植版不注册这些模组；
`salt_dirt_1..3` 经核对也是原版指向 Salt 模组的 `saltDirt` 方块，同样属于外部内容）；
**9 个是 GT6 原生但移植版确实没有的物品**，列成下一批（不做无贴图的占位注册）：

| token | 原版注册 | 移植版现状 |
|---|---|---|
| `rope_silk` / `rope_vine` / `rope_plastic` / `rope_steel`（+`grass rope`） | `Loader_MultiTileEntities:2086-2091`，`MultiTileEntityRope`，6 种绳索 | 已有 `rope` 方块与 `RopeBlock`；缺 4 种变体的注册/贴图/模型 |
| `boomstick` / `dynamite_strong` | `Loader_MultiTileEntities:2235-2237`，`MultiTileEntityDynamite` | 已有 `dynamite` 方块与 `DynamiteBlock`；缺 2 种变体 |
| （`rope`/`dynamite` 本身也无配方） | 这两种方块的配方走原版的 util 机制（注册行的 `aUtilWool`/`aTNT` 参数，`Loader_MultiTileEntities:110`），移植版没有该机制 | 需要一并实现 util 合成或给出等价配方 |

### 18.4 验证

```
========= 306 GAME TESTS COMPLETE ======================
All 306 required tests passed :)
```

* `tools/apply_recipe_mirror_flags.py --check` → 0 待写
* `tools/report_extruder_skips.py` → 176（全部是原版自己也没注册形态的齿轮/工具头行）
* `tools/report_tool_shaped_mirror.py` → 1188 行、0 行镜像、183 行不对称
* `docs/items-without-recipes.json` → **withoutRecipe 0**，机器行 215,201（挤出机 +800）

改动文件：`tools/extract_gt6_form_flags.py`（工厂链 + 重载解析）、`data/generated/MaterialForms.java`（重生成）、
`tools/census_gt6_recipe_mirror_flags.py`（新）、`tools/apply_recipe_mirror_flags.py`（新）、
`tools/report_tool_shaped_mirror.py`（新）、`data/BasicMachineRecipePack.java`、`data/MultiblockRecipePack.java`、
`loader/Loader_HandToolCraftingRecipes.java`、`recipe/ToolShapedRecipe.java`（`allowMirror()` 访问器）、
`tools/derive_missing_tech_aliases.py`（新）、`tools/audit_tech_item_tokens.py`、`registry/GTTechnological.java`（20 别名）、
`loaders/c/GTGeneratedChem.java`、`gametest/HandCraftingTests.java`。

---


指出 §16.2 里我标为「有意差异」的两处应该按原版做实，于是这一轮把覆盖板的**物品身份**改成原版模型；
顺手还修掉了一个卡住整族挤出机配方的引用解析问题。

### 17.1 覆盖板：原版根本没有「泵盖板」这种物品

| 覆盖板 | 原版怎么来的 | 移植版现在怎么来 |
|---|---|---|
| 泵 / 传送带 / 机械臂 | `MultiItemTechnological:50-53` 直接给 `IL.PUMPS`/`CONVEYERS`/`ROBOT_ARMS` 挂 `CoverPump(250<<2i)` / `CoverConveyor(512>>i)` / `CoverRobotArm(512>>i)`——**同一个物品既是机器零件又是盖板** | 删掉 `cover_pump`/`cover_conveyor`/`cover_robot_arm` 三个壳物品；`compact_electric_pump_*` 等直接贴到机器面 |
| 红石火把 / 中继器 | `GT_API:799-802` 把**原版**火把（亮/灭两种状态）与中继器注册成 `CoverRedstoneTorch`/`CoverRedstoneRepeater` | 删掉两个壳物品；右键机器面直接贴原版火把/中继器 |
| 标签选择器 | `ItemIntegratedCircuit:87` 把编程电路的 16 个变体注册成 `CoverSelectorTag(i)` | 删掉 `cover_tag_selector`；`integrated_circuit_N` 当盖板时 **N 就是所选标签** |

行为也按原版带上等级：

* 泵：`CoverPump(250 << 2i)` ⇒ 每 20 刻搬运 `250 << 2i` mB（ULV 250 / LV 1000 / MV 4000 …，对应原版 "Transfers N L/sec"）
* 传送带 / 机械臂：`512 >> i` 刻搬一次（ULV 512 / LV 256 / … / UV 2）

实现落在新类 `content/cover/CoverItems.java`（`behavior(stack)` 把物品映射成盖板行为、`tierIndex`、`pumpThroughput`、`itemInterval`、`selectorTag`），
机器侧 `coverIs`/`getCoverId` 全部改走它，`BasicMachineBlock.isCoverItem` 也改为委托（因此原版火把这类非 `TechItem` 也能贴）。

清理：6 个壳物品的注册项、6 条 1:1 转换配方、6 条语言键全部删除，另加两个小工具守门——
`tools/drop_retired_cover_lang.py`（删语言键）、`tools/check_retired_cover_refs.py`（断言没有任何配方还在引用它们）。

新测试 `HandCraftingTests.vanillaBlocksAndComponentsDoubleAsCovers`：映射与等级数值正确、普通物品**不是**盖板、
机器面真的接受它们（贴原版火把后 `hasRedstoneCover()` 为真）。

> 本批生成的配方从 77 条降到 **71 条**（少掉的正是那 6 条转换配方）。

> **本批未改、留作记录的差异**：原版的泵/传送带/机械臂盖板用**螺丝刀**切换方向（visual 0/1 = 输出/输入，
> `CoverPump.onToolClick`/`CoverConveyor.onToolClick`），移植版仍是"泵=抽入、传送带=抽入、机械臂=推出"的固定方向
> （`tickPumpCover`/`tickConveyorCover`/`tickRobotArmCover`）。要用哪一侧就贴哪一侧，所以不影响可用性，
> 但与原版的"一个盖板两个方向"还不一样——需要的话下一批把 `visual` 状态加进盖板 NBT 并接螺丝刀。

### 17.2 发电机模块：引用解析修好，挤出机整族开始落地

原版 `MultiItemRandomTools:421-426` 的「石/玄武岩/黑石发电机模块」是**配方钥匙**：
`Loader_Recipes_Extruder:44-151` 的整族行把它们放在特殊槽，在没有材料输入的情况下凭空产出石头类形态。

移植版其实**早就注册了这三个物品**（`GTMultiItemsGen` 的 randomtools 组，连贴图与模型都在 `assets/.../item/randomtools/*_generator_module.png`），
但生成器里的 `tech:` 规格只在**科技物品表**里查名字，于是整族行在运行期被当成"内容缺失"跳过。
两处修：

1. `GTGeneratedChem` 的 `tech:` 解析：先过别名表 `GTTechnological.ALIASES`，再回落到物品注册表——
   原版字段名 `IL.Module_Stone_Generator` 与移植版按显示名生成的 id `stone_generator_module` 就是靠别名表接上的
   （这三个别名同时让 `tools/audit_tech_item_tokens.py` 不再把它们算成缺失内容）。
2. 补上原版的三条合成配方（`tools/generate_generator_module_recipes.py`，放进新目录 `recipes/recipe_keys/`，
   避免撞上 `ElectronicsTests`（84 条部件配方）与 `ManufacturingTests`（78 个模具）的计数）：

```
stone      "CPC" / "LMW" / "COC"   机壳(镀锌钢) + 方块挤出模具 + T4 电路×4 + 活塞 + 岩浆桶 + 水桶
basalt     "S" / "M" / "I"          灵魂沙 + 石模块 + 浮冰
blackstone "S" / "M" / "O"          灵魂沙 + 石模块 + 黑曜石
```

结果：`set.extruder` 的 **added 112 → 136、skipped 210 → 186**（转译总账 added 1416 → **1448**）；
`tech:module_*` 不再出现在缺失内容里。原版这些行共 89 条，今天能注册 **25** 条，其余卡在 §17.3。

### 17.3 剩下的 186 条挤出机行：两种原因，性质不同

| 原因 | 行数 | 判定 |
|---|---|---|
| 石头材料的**齿轮/工具头**形态（`gearGt`、`gearGtSmall`、`toolHeadRaw{Shovel,Sword,Hoe,Pickaxe,Axe}`、`toolHeadHammer`，每种 22 行 = 11 种石头材料 × 2 个模具） | 176 | **原版同样不产出**：原版 `TD.G_STONE = {PROJECTILES, DUSTS, PLANTS, PLATES, STICKS}`（`TD.java:187` 附近），石头材料没有 GEARS/TOOLHEADS 标志，原版这些行本身是死配方 |
| 移植版 `Blackstone` 缺 `plate/plateCurved/stick/stickLong/bolt`（每种 2 行） | 10 | **真缺口**：原版 `MT.STONES.Blackstone` 走 `stone(...)` 工厂（`MT.java:323-329`）⇒ 带 G_STONE ⇒ 有 PLATES/STICKS；移植版的 Blackstone 是手写的 `SupplementalMaterials` 材料，没带上这些形态标志 |

后者是下一批的内容（把 `MT.STONES.*` 的形态标志补到移植版对应材料上），先记在这里不混进本批。
复核用脚本：`tools/report_extruder_skips.py`（按原因分组打印最近一次门禁的跳过行）。

### 17.4 验证

```
========= 304 GAME TESTS COMPLETE ======================
All 304 required tests passed :)
```

游戏内报告 `docs/items-without-recipes.json`：checked **363**（= 369 减去 6 个已删除的壳物品）、
**withoutRecipe 0**、机器行 214,393；`docs/transpiled-recipes-coverage.json`：`set.extruder: added 136, skipped 186`。

改动文件：`content/cover/CoverItems.java`（新）、`blockentity/machine/BasicMachineBlockEntity.java`、
`block/machine/BasicMachineBlock.java`、`registry/GTTechnological.java`（删 6 个 id、加 3 个别名）、
`loaders/c/GTGeneratedChem.java`、`gametest/HandCraftingTests.java`、`gametest/GeneratedChemistryTests.java`、
`tools/generate_covers_usb_logistics_recipes.py`、`tools/generate_generator_module_recipes.py`（新）、
`tools/drop_retired_cover_lang.py`（新）、`tools/check_retired_cover_refs.py`（新）。

---

## 17. 续批：覆盖板回归原版的「一个物品兼两职」+ 发电机模块解锁挤出机

指出 §16.2 里我标为「有意差异」的两处应该按原版做实，于是这一轮把覆盖板的**物品身份**改成原版模型；

* 选择器电路（`integrated_circuit_0..24`）在原版**是可以合成的**（`gregapi/item/ItemIntegratedCircuit.java:58-85`），
  移植版却把它写进了"by design 永不合成"名单——而 ULV~HV 的场发生器/机械臂/传感器/信号发射器
  都用它当材料，等于那些机器在生存里**做不出来**。
* 5 种**满电池**在原版由 Canning 机灌装（`MultiItemTechnological:461-484` 的 `FluidContainerData`），移植版既没有工作台配方也没有机器行。

本批之后，**游戏内报告：369 个科技物品/工具，0 个没有任何配方（datapack 或机器行）**。

### 16.1 关键发现：原版 `CR.DEF` 不含镜像（影响的是整批工作台配方）

`gregapi/util/CR.java:161-163`：`DEF = BUF|NO_REM`，`DEF_REV = DEF|REV`，`DEF_MIR = DEF|MIR` ——
**原版默认不允许镜像匹配**；而 1.20 的 `ShapedRecipe` 默认允许。
后果：原版靠"工具符号摆在底盘哪一格"区分同一底盘的配方，在移植版会两两撞车——
物流 9 种总线的底盘同为 `"WQW"/"CPC"`，出口/导入/储存只是 `w`/`r`/`d` 在顶行的位置不同，
其中出口 `"  w"` 与储存 `"w  "` **互为镜像**，镜像开着就只能合成出其中一种。

处理：本批所有行写 `"allow_mirror": false`（与原版一致），并加测试
`HandCraftingTests.batchPatternsAreUnambiguous`：把配方自己的图案摆进工作台，断言**只有它自己匹配**（57 条成形行全覆盖）。

> **下一批可选的保真对齐**：既有 373 条 `gregtech:tool_shaped`（transpile 产出的）大多没写 `allow_mirror`，
> 即比原版"宽松"。可以统一补成 `allow_mirror: false`，代价是要重跑一遍撞车断言。

### 16.2 覆盖板 12 种（原版 `MultiItemTechnological:139-178`、`:419-422`、`GT_API:799-802`、`ItemIntegratedCircuit:87`）

| 物品 | 原版图案 / 键 | 出处 |
|---|---|---|
| `crafting_table_cover` | `"C"/"Q"`（C 工作台、Q 空白盖板） | `:140` |
| `warning_cover` | `"GB"/"YQ"`（G 胶、Y 黄染料、Q 空白盖板） | `:167` |
| `item_retriever_cover` | `"RPR"/"CQC"`（Q 物品过滤盖板、P LV 电动活塞、C T3 电路、R 弯曲琥珀金板） | `:170` |
| `drain` | `"RRR"/"RwR"/"RRR"`（R 铁杆、w 扳手） | `:159` |
| `air_vent` | `"RRR"/"RXR"/"RRR"`（R 铁杆、X 铁转子） | `:161` |
| `pressure_value` | `"TCT"/"wPd"`（T 黄铜螺丝、C 弯曲黄铜板、P 黄铜板、w 扳手、d 螺丝刀） | `:178` |
| `cover_pump` / `cover_conveyor` / `cover_robot_arm` | 原版这三个物品**本身就是盖板**（`IL.PUMPS/CONVEYERS/ROBOT_ARMS`），配方在同一循环里：`"TXO"/"dPw"/"OMT"`、`"RRR"/"MCM"/"RRR"`、`"CCC"/"MSM"/"PES"`（LV 档材料） | `:419-422` |
| `cover_redstone_torch` / `cover_redstone_repeater` | 原版直接把**原版红石火把/中继器**注册成盖板，没有独立物品 | `GT_API:799-802` |
| `cover_tag_selector` | 原版 = 编程电路（`ItemIntegratedCircuit:87` 把 meta 0..15 注册成 `CoverSelectorTag`） | `ItemIntegratedCircuit:87` |

两处当时的差异已按要求做实（**§17 已改写这两条、并删掉了相关壳物品与转换配方**）：

1. 原版 `IL.PUMPS`「电动泵」一个物品兼两职（机器零件 + 盖板）；移植版拆成两件，
   零件已经带着原配方（`components/pump_lv.json` 等），所以盖板做成**零件的 1:1 转换**，
   否则会出现"同材料同图案、只有产物不同"的两条配方（玩家能合出哪一条取决于遍历顺序）。
2. `warning_cover` 那一行的 `B` 键在原版**从未定义**（`CR.shaped` 找不到就把该格留空，等于原版这条配方本身就是残缺的），
   移植版按原版**确实定义了**的两个键实现：`"G "/"YQ"`。

### 16.3 USB 12 件（`MultiItemTechnological:796-822`）

| 物品 | 图案 | 键 | 出处 |
|---|---|---|---|
| `usb1..4_stick` | `"xWd"/"PCP"/"TCT"` | x 剪线钳、d 螺丝刀、W 1x 导线（金/铝/铂/石墨烯）、C T3~T6 电路、P/T 板+螺丝（铝/不锈钢/铬/钛） | `:796-799` |
| `usb1..4_cable` | 同上 | C 换成同等级**线缆** | `:808-811` |
| `usb1..4_hdd` | `"PLT"/"dRW"/"TCP"` | P/T 板+螺丝、L 氦激光发射器、R 唱片（原版 `OD.record` → `minecraft:music_discs`）、W 对应 USB 线缆、C T3~T6 电路 | `:819-822` |

`MT.Cr` 在移植版叫 **Chromium**（不是 "Chrome"），所以第 3 档的板/螺丝是 `forge:plates/chromium`。

### 16.4 物流总线 14 件 + 14 条环形切换（`MultiItemTechnological:773-787`、`:117-131`）

| 物品 | 图案 | 出处 |
|---|---|---|
| 显示 CPU ×4（逻辑/控制/储存/转换） | `"dL "/" Q "/" C "`、`" Ld"/" Q "/" C "`、`" L "/" Q "/"dC "`、`" L "/" Q "/" Cd"`；Q 空白盖板、C T2 电路、L 流明 1x 导线 | `:773-776` |
| 过滤总线 ×6（流体/物品 × 出口/导入/储存） | 底盘 `"WQW"/"CPC"`，顶行工具符号位区分：流体用扳手 `w`、物品用软锤 `r`、通用用螺丝刀 `d` | `:778-786` |
| `logistics_dump_bus_item` | `"   "/"WQW"/"CPC"`（无工具） | `:787` |
| 环形切换 ×14 | shapeless 1:1（原版用它们在同一位置换形态：显示 CPU 环、以及 倾倒→流体…→通用→倾倒 的环） | `:117-131` |

底盘键：`W` = 锇细导线（`wire_fine_osmiumelemental`）、`P` = 绿宝石水晶处理器、`C` = T4 电路
（按 `LoaderOreDictReRegistrations:375-383` 的 `OD_CIRCUITS` 累积链，移植版展开成 T1..T4 四种可选）。

### 16.5 选择器电路 25 件（`ItemIntegratedCircuit:58-85`）

| 物品 | 配方 |
|---|---|
| `integrated_circuit_0` | `"GhG"/"SSS"/"GwG"`：小铁齿轮 ×4、铁杆 ×3、h 锤子、w 扳手（`:58`） |
| `integrated_circuit_1..24` | **任意**选择器电路 + 螺丝刀 `d`，螺丝刀在网格里的位置决定编号（原版逐行列了 24 条，`:60-85`） |

"任意变体"在原版是通配 meta（`ST.make(this, 1, W)`）；移植版用新 tag
`data/gregtech/tags/items/integrated_circuits.json`（25 项）表达。
同时把 `CraftingCoverageTests` 与 `tools/report_items_without_recipes.py` 里
"选择器电路永不合成"的 by-design 例外**删除**——那条假设是错的。

### 16.6 电池灌装 5 行（Canning 机，`MultiItemTechnological:461-484`）

| 空壳 → 满壳 | 电解液 | 出处 |
|---|---|---|
| `lead_acid_cell_empty` → `_filled` | 硫酸 **2000 mB** | `:462` |
| `alkaline_button_cell_empty` → `_filled` | 蒸馏水 **1000 mB** | `:467` |
| `nickel_cadmium_cell_empty` → `_filled` | 蒸馏水 **1000 mB** | `:472` |
| `lithium_cobalt_cell_empty` → `_filled` | 氯化氢（气）**2000 mB** | `:477` |
| `lithium_manganese_cell_empty` → `_filled` | 氟化氢（气）**2000 mB** | `:482` |

原版这些行不是显式 `RM.Canner.addRecipe`，而是容器注册表自动生成的
（`OreDictManager:296-300` + `FL.set`），机器工时取自 Canning 的通用容器档：
`RM.java:743-746` 的 `Canner.addRecipe2(T, 16, 16, …)` = 16 EU/t、16 刻。
新增 `content/recipe/BatteryCellRecipes.java` 承载这 5 行（日志里 0 skipped = 电解液全部解析成功）。

### 16.7 验证

```
Registered 8 GT6 laser emitter filling recipes (0 skipped: [])
Registered 5 GT6 battery cell filling recipes (0 skipped: [])
Registered 525 GT6 hand recipes from registration patterns
  (29 wires, 334 pipes, 34 anvils, 122 storage, 1 circuits, 5 cells)
========= 302 GAME TESTS COMPLETE ======================
All 302 required tests passed :)
```

`docs/items-without-recipes.json`（由游戏内测试写出）：**checked 369，withoutRecipe 0**，机器行 214,361。
只看 datapack 的静态脚本仍是 74（48 电路件 / 9 水晶 / 8 激光发射器 / 5 满电池壳 + 5 空壳），
它们全部由机器行或 `Loader_HandToolCraftingRecipes` 运行时注入覆盖——脚本现在会同时打印游戏内数字，避免误读。

新增/改动文件：`tools/generate_covers_usb_logistics_recipes.py`（新，77 条配方的生成器）、
`data/gregtech/recipes/{hand_covers,hand_components,logistics}/`（77 个 json）、
`data/gregtech/tags/items/integrated_circuits.json`（新）、`content/recipe/BatteryCellRecipes.java`（新）、
`gametest/HandCraftingTests.java`（新，5 项测试）、`gametest/CraftingCoverageTests.java`、
`tools/report_items_without_recipes.py`、`GregTech.java`（注册新 loader）。

---

## 14. 续批（实机测试第二批）：JEI 卡顿的 tag、管道/导线配方、堆叠数量、金被叫成 Goldwood

反馈了 5 个具体问题，逐条修如下（这一轮之后质量门 **296 项**）。

### 14.1 `#gregtech:material` 让 JEI 卡几秒 → 去掉聚合 tag

`MaterialTagPack` 之前除了「单材料 tag」还生成一个**伞形 tag** `gregtech:material`，里面是
`#gregtech:material/<材料>` 的引用链，展开等于**全模组所有材料物品**（四万多个）。JEI 每次打开都会解析它，
所以卡几秒。现在：

| tag | 之前 | 现在 |
|---|---|---|
| `gregtech:material`（伞形） | 1150 条子 tag 引用 | **不再生成**（`MaterialTagTests.umbrellaMaterialTagIsGone` 守着） |
| `gregtech:material/<材料>` | 该材料的全部形态 + 方块 | **保留**，并补上**流体容器物品**（`RegisteredFluids.boundMaterial` 把流体绑回材料）；`unit` 形态本来就在里面 |
| 单形态 tag | `gregtech:items/<前缀>/<材料>`（文档里写错） | 实际名字是 **`gregtech:<前缀>/<材料>`**（`items` 是 tag 目录不是名字的一部分），文档与注释已改，`MaterialTagTests.formTagsStillExist` 守着 |

> 复现：`docs/material-tags.json`（铁：单材料 tag **144** 条、`gregtech:unit/iron` 有内容、`gregtech:fluid/iron`
> 有内容（熔融铁流体物品）、伞形 tag 不存在）。补这一步时发现熔融流体此前没有回绑材料
> （`FluidEntry.materialKey` 之外还有 `RegisteredFluids.BOUND_MATERIALS` 这张侧表），已在
> `createMolten` 与 tag 生成两侧都接上。

### 14.2 管道与导线：原版是怎么造出来的（附出处），以及这轮补了什么

先把原版查清，避免凭空造配方：

| 物品 | 原版出处 | 结论 |
|---|---|---|
| **金属流体管 / 物品管** | `Loader_Recipes_Handlers:324-328`（硬材料，64 tick）与 `:339-343`（`tEasyHeatable`，16×弯板数 tick）：`弯板 1/1/3/6/12 + 电路选择器 1/2/3/4/5 → tiny×2 / small / medium / large / huge` | **只有机器（Welder）**，没有工作台配方 |
| **木管** | `Loader_MultiTileEntities:1887-1891`：锯 + 软锤把台阶/木板/木梁做成 5 种尺寸 | **工作台配方**（原版唯一的手工管件配方） |
| **四联管 / 九联管** | 工作台拆解 `MultiTileEntityPipeFluid:100-101`（四联→4×中型、九联→9×小型）；装箱 `Loader_Recipes_Handlers:492`（4×中型 → 四联）与拆箱 `:458-459` | 两种都有 |
| **1x 导线** | `RM.Wiremill`（`:287-295`：锭 → 2× 1x 导线、杆 → 4× 细导线） | **只有机器**，原版没有「锭 → 导线」的工作台配方 |
| **导线尺寸换算** | 工作台 `Loader_Recipes_Handlers:624-625`（`tAmount` 根小线 ↔ 1 根大线，`tAmount < 10`）；机器侧 Loom/Unboxinator `:621-622` | 两种都有 |
| **绝缘线缆** | `MultiTileEntityWireElectric.addElectricWires`（橡胶板/箔 + 线 → 缆，Laminator） | 移植版已有 `wire_working/` 1320 条手工配方 |

**这轮补的**：`content/recipe/PipeRecipes.java`（**343 条** Welder 行，按材料决定产出物品管还是流体管）、
工作台侧导线尺寸换算（`Loader_FormConversionCraftingRecipes.registerWireSizes`）、
管件尺寸换算（工作台 `registerPipeSizes` + Boxinator/Unboxinator）、
木管 5 条工作台配方（`tools/generate_wooden_pipe_recipes.py`，用到新 tag
`gregtech:wooden_planks` / `wooden_slabs` / `wooden_beams`）。
此前**任何管道都做不出来**（只有 5 条 cover 配方把中型管当材料用）——现在生存可得。

> 仍未做：按尺寸的**管件在 JEI 里的分组**与「流体管/物品管/限制管」的说明；限制管（`pipeRestrictive*`）原版
> **只有物品、没有任何产出配方**（`MultiTileEntityPipeItem:80-82` 只做了前缀映射），移植版同样保持不可得。

### 14.3 堆叠数量按原版对齐（`tools/extract_gt6_stack_sizes.py`）

原版 `aRegistry.add(name, category, id, tab, class, quality, stackSize, …)` 的第 7 个参数就是堆叠上限，
按分类统计：Multiblock Machines 72 行全 16、Reactors/Sensors/Storage/Chests/Fluid Containers/Portals 全 16、
Misc Tool Blocks 22 行 16 + 29 行 64、Ropes 64、导线 `64/尺寸`、线缆 64/32/16/8/4、物品管 64/32/16、
流体管 tiny/small 64、medium 32、large/huge/四联/九联 16。已按此表改：

| 位置 | 之前 | 现在 |
|---|---|---|
| 单方块机器 / 多方块主机与部件（`MachineRegistry`） | 64 | **16** |
| 砧（全部材质变体） | **1** | **16** |
| mass storage / 金属箱（`GTStorage`） | 1 | **16** |
| 便携流体容器与流体附件 | 1（cell 64） | **16**（cell 64） |
| 手工工具方块 | 64 | mortar/juicer/crank/尘漏斗等 16，龙头/喷嘴/漏斗/按钮 64，绳 64，炸药 64 |
| 导线 / 线缆 | 64 | **64/尺寸** 与 64/32/16/8/4 |
| 流体管 / 物品管 | 64 | **64/32/16**（按尺寸） |

### 14.4 金被命名成 Goldwood

根因：原版 `MT.java:3952` 的字段 `Gold` 其实是 **BiomesOPlenty 的木材 "Goldwood"**（金属金是 `Au`）。
移植版的 `GTMaterials.Gold` 指向木材是正确的映射，但语言文件生成器
（`tools/import_gt_lang.py`）给「字段名」额外写了一份别名的 key，把木材的名字写进了
`material.gregtech.gold`，覆盖了金属金的条目 → 游戏里金显示为「Goldwood / 金木」。

修法：① 两个语言文件里 `material.gregtech.gold` 改回 `Gold` / `金`；② 生成器不再写那种会**顶掉别的材料**的
字段别名（`field` 命中其它材料的 canonical 名时跳过）；③ 新增 `tools/check_material_lang_collisions.py`
扫描这类冲突（当前 en/zh 都是 0）。

### 14.5 电子电路：magic / enderium / signalum 链（41 条）

`docs/items-without-recipes.json` 里 22 个电路物品此前只有物品没有配方。按原版补齐（`ElectronicsRecipes`）：

| 环节 | 原版出处 | 条数 |
|---|---|---|
| 电路导线（Thaumium/Manasteel/Mithril/Netherite/Enderium/Signalum 箔 4 + 透镜 → 导线） | `Loader_Recipes_Other:158-166`（LaserEngraver 64t） | 6 |
| 电路板基板（空板 + 导线；HSLA 板 = HSLA 板 + 金导线） | `MultiItemTechnological:573-581`（Press 64t） | 4 |
| 电路元件（导线 + 导线 + 半导体小宝石板，1×/9×） | `:598-636`（Press 16t） | 21 |
| 电路板（基板 + 4× 元件；HSLA 两种） | `:674-698`（Press 64t） | 5 |
| 成品电路（电路板 + 焊料浴；magic/enderium 用焊锡合金，signalum 三种焊料都可） | `:739-743`（Bath 64t，72 mB） | 5 |

`ElectronicsTests` 的配方计数断言同步到 **164**；无配方物品数 **76 → 57**
（剩：物流总线与覆盖板 23、USB 12、电池 10、水晶电路与处理器 5、电路 4、其它 3）。

### 14.6 验证

```
Registered 343 GT6 pipe welding rows
Registered 21246 GT6 material form conversions
========= 296 GAME TESTS COMPLETE ======================
All 296 required tests passed :)
```

改动文件：`data/MaterialTagPack.java`、`registry/GTWires.java`、`registry/GTFluidPipes.java`、
`registry/GTItemPipes.java`、`registry/GTToolBlocks.java`、`registry/GTStorage.java`、
`api/machine/MachineRegistry.java`、`content/recipe/PipeRecipes.java`、`content/recipe/ElectronicsRecipes.java`、
`loaders/Loader_FormConversionCraftingRecipes.java`、`lang/en_us.json`、`lang/zh_cn.json`、
`tools/import_gt_lang.py`、`tools/extract_gt6_stack_sizes.py`、`tools/generate_wooden_pipe_recipes.py`、
`tools/check_material_lang_collisions.py`、`tools/generate_missing_component_recipes.py`、`gametest/MaterialTagTests.java`。
