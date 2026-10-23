# 物流核心批次对接单（本次核对结果与后续做法）

> **2026-09-20 审计更正（优先于下文历史叙述）**：原文“逐文件无遗漏”仅是当时自述，mtime 不能证明作者或会话归属；后续文档本身也有修改。历史 681 项通过及 jar 哈希已核对，但不证明无功能缺陷。本次未重跑 GameTest。继续工作先看 `AUDIT_NEXT_STEPS_2026-09-20.md` 和 `WEEKLY_AUDIT_2026-09-19.md`。
>
> `LongDistancePowerTests` 实际有 **3** 个测试，原文“5 项”错误。14 种物流覆盖板为 **10 种路由用途 + 4 种 CPU 显示用途**，不是 12 + 4。`LogisticsCoreStructure` 尚未接线，`range()` 只返回 control、缺原版范围的 +2；`energyPerOperation()` 当前返回启动门槛，不应当作每轮消耗。接线前先修正命名与计算。
>
> Crystal Chargers 在当前检查的 GT6 内置物品中未找到 LU 储能物品，这只能支持“内置使用链缺失”；外部物品可实现能源接口，不能据此断言机器永远不可用或从完整移植范围永久排除。量子充能器及长距离端点已存在，也不等于全部交互完成验收。后文保留为历史交接记录，遇冲突以上述更正和实际源码为准。


> 本文件写给后续接手的人，只写能核对的结论，每条都带 `文件:行`。
> 工程：`F:\Dev\GregTech6\GregTech6`；GT6 权威源：`F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java`。
> 上一份主交接单（§1 状态 / §2 命令 / §3 70+ 条陷阱）：`C:\Users\Asus\AppData\Local\Temp\gt6-port-handoff-2026-09-17.md`。

---

## §0 一句话结论

目标里排的优先级 **① 量子充能器 T1–T5、② 长距离运输端点 10060/10061 都已经实现**（§124 的「真正缺的三小批」是假阳性，2/3 错）；真正剩下的大件就是 **③ 物流核心**——移植版已有 14 个物流覆盖板**物品**+47 条配方+36 份资产，但**一个行为都没有**，`logistics_core` 还是 `new Block(...)` 占位。本次已落地物流核心的**结构层**（1 个 157 行新类，javac exit 0），其余按 §4.4 的 W1→W4 接着做。

---

## §1 本次改动清单（逐文件核对，无遗漏）

| 文件 | 时间 | 性质 | 验证状态 |
|---|---|---|---|
| `src/main/java/com/gregtech/gregtech/content/logistics/LogisticsCoreStructure.java` | 14:43:14 | **新增，157 行**（结构分类 + CPU 计数 + 经济学取值），**未接线**（无注册/无 BE/无测试引用） | `python tools/javac_selfcheck.py <该文件>` → **exit 0**；`gradlew compileJava` → **BUILD SUCCESSFUL**（45 warning 全是既有的 `ResourceLocation(String,String)` 等）；门禁见 §5 |
| `docs/gt6-registration-census.json` | 14:40:50 | 运行 `python tools/census_gt6_registrations.py` 的**副产物重写**（该文件 git 未跟踪 `??`，无内容风险） | 与 §124 口径一致（Basic Machines 62 → missing 0） |

- 除此之外，本次**没有**修改任何既有 Java / 资源 / 文档；**没有**执行 `git commit`。
- 本次工作期间其它 mtime 变化（`docs/NEXT_GOAL.md` 14:36:58、`PORTING_AUDIT.md`/`PORTING_REMAINING`/`docs/porting-progress.json` 02:21）都是**上一段收尾**留下的，不是本次产物。

---

## §2 核对结论一：§124「真正缺的三小批」里两条是假阳性

| §124 的「缺」 | 结论 | 证据 |
|---|---|---|
| USB / HDD 交换机组 | **真缺**，已在 §124 做完 | `block/misc/UsbSwitchBlock.java`(107) + `blockentity/.../UsbSwitchBlockEntity.java`(116) + `gametest/UsbSwitchTests.java`(92) |
| Long Distance 端点 **10060/10061** | **假阳性：早已实现** | `block/misc/LongDistEndpointBlock.java`(31) + `block/misc/LongDistEndpointBlockEntity.java`(**60**，严格照 GT6「源背面→管束→接收端正面；接收端背面库存被代理」，且要求 source 唯一=自己、receiver 唯一) + `LongDistPipeBlock.java`（item/fluid 管 + 线，16 档线电压）+ `LongDistanceTransformerBlock(.java)` + `LongDistanceTransformerBlockEntity.java`（5 档）+ 测试 `BlueprintRegressionTests.longDistanceItemPipelineDisconnectsImmediately`、`LongDistancePowerTests`（5 项，含 1/8 距离损耗 `loss(1024)==128`、线烧毁、支路） |
| 量子充能器 **5 台** | **假阳性：早已实现** | `registry/GTLasers.java:88-92` 注册 `quantum_energizer_ev/iv/luv/zpm/uv`（`LaserSpec.Kind.QUANTUM`, tier1..5；`LaserSpec.java:13-21`：入 LU 32/128/512/2048/8192、出 QU 16/64/256/1024/4096，背面进 LU、正面出 QU）；`LaserConverterBlockEntity.java:43-52`：`output = energy*out/in`、`>= max(1,out/2)` 才发、**每 tick 清空输入缓冲**（GT6 `NBT_WASTE_ENERGY=T` 语义）；资源 **22** 份（blockstate 5 + block 模型 7 + item 模型 5 + `data/gregtech/recipes/optical/quantum_energizer_*.json` 5）+ 双语 10 键（`block.gregtech.quantum_energizer_*`）；测试 `gametest/OpticalEnergyRegressionTests.java`（4 项） |

**⇒ 目标文档的优先级 ① 与 ② 不必再做。** 这条与 §113/§124 是同一类错：审计工具按**名字/单一来源**对照就会高报，结论必须回注册表逐条核实（本节每条都已回注册表核过）。

---

## §3 核对结论二（新挖出）：Crystal Chargers（GT6 10130–10149，20 台）是 GT6 **死代码**

移植版没有这 20 台，而 §124 的清单里也没有它们——核对后结论是**正确地没做**，证据链（每步都回 GT6 原文）：

1. `gregtech/loaders/b/Loader_MultiTileEntities.java:970-971`：`for (int i = 0; i < 10; i++)` 注册 10 台 `Crystal Charger (T0..T9)`（`NBT_INV_SIZE=4`）与 10 台 `Large Crystal Charger`（`NBT_INV_SIZE=16`）；键里只有 `NBT_ENERGY_EMITTED = TD.Energy.LU`，**没有** `NBT_ENERGY_ACCEPTED`。
2. `gregapi/tileentity/energy/TileEntityBase10EnergyBatBox.java:67-68`：只有 `NBT_ENERGY_EMITTED` 时 `mEnergyType = mEnergyTypeOut = LU`，即这台机器**只吃 LU**。
3. 同文件 `:179`：`doInject` 第一句 `if (mReceivablePower <= 0) return 0;`；而 `mReceivablePower = mChargeableCount * mInput * 2`（`:153`）。
4. `:132`：`mChargeableCount` 只统计「能注 LU 的物品」（`canEnergyInjection(mEnergyType=LU, …)`）。
5. GT6 全库**不存在任何存 LU 的物品**：`TD.Energy.LU` 只出现在 3 个文件（`Loader_MultiTileEntities`、`MultiTileEntityWireLaser`、`MultiTileEntityMagicFieldAbsorber`，全是机器/线）；物品侧能量统计全用 EU——`gregapi/item/multiitem/energy/EnergyStat.java:58-65` 的工厂，唯一外部调用点 `MultiItemTool.java:383`（`makeTool(TD.Energy.EU, …)`）与 `MultiItemRandomTools.java:517-518`（便携扫描仪/作物分析仪，EU）。`EnergyStat` 只有**单一** `mType`，没有「一个物品吃多种能量」的口子。

⇒ `mChargeableCount ≡ 0` ⇒ `mReceivablePower ≡ 0` ⇒ 永远收不到能量 ⇒ `mActive` 恒假 ⇒ **机器完全不动**。写档为「不适用（GT6 死内容）」，不要照抄成活的（§114「GT6 里的死代码要认出来」同类）。

---

## §4 真正的剩余大件：物流核心（目标优先级 ③）

### 4.1 移植版现状（已逐处核对）

| 层 | 现状（文件/行） |
|---|---|
| 14 个物流覆盖板**物品** | **全部已注册**：`registry/GTTechnological.java`（`:146`/`:373`，22 处 logistics）——generic / filtered × import / export / storage × item / fluid + `logistics_dump_bus_item` + 4 个 `logistics_display_cpu_*`；配方 `data/gregtech/recipes/logistics/*` **47** 份；资产 36 份；双语键齐全（tooltip 全是 "For use with Logistics Cores and Wiring"） |
| 覆盖板**行为** | **完全没有**：`content/cover/CoverItems.java` 的 `portCoverId(...)` 不含任何 logistics id ⇒ 能贴、永不派发（§108 记过这个坑） |
| `logistics_core` 方块 | **占位**：`registry/GTLasers.java:101` = `reg("logistics_core", () -> new Block(qProps()))`；无 BE、无结构、**无配方**；资产已在（`blockstates/logistics_core.json` + `models/block/machine/logistics_core.json`，贴图 = galvanized 墙） |
| `logistics_wire` 方块 | **仅 iconset 装饰块**：`registry/GTIconSetBlocks.java:281` 的 id 列表里；无 BE、无连接、无配方 |
| 结构零件 | **已全部注册且都是 `MultiblockPortBlock`**（可挂端口角色）：`content/multiblock/LargeMachineParts.java` — 18008 `galvanized_steel_wall`、18299 `fusion_ventilation_unit`、18200 `versatile_processor_unit`、18201 `logic_processor_unit`、18202 `control_processor_unit`、18203 `storage_quadcore_processor_unit`、18204 `conversion_processor_unit` |

### 4.2 GT6 权威规格（逐行核过的数字，直接可用）

**结构**（`gregtech/tileentity/multiblocks/MultiTileEntityLogisticsCore.java`）

- `:110` 立方体中心在控制器**背面 2 格**（`getOffsetXN(mFacing, 2)`）⇒ 控制器位于某一面中心、朝外（`:153` 的 tooltip 4 行）。
- `:117-142` 遍历 `i,j,k ∈ [-2,2]`，按**平方距离**分类：
  - `i²+j²+k² < 4` → 内核 3×3×3（**27** 格）：CPU 或墙；
  - `i²+j²+k² > 6` → **44** 格 galvanized 钢墙（**同时是进电面**，`:138` 角色 `ONLY_LOGISTICS & ONLY_ENERGY_IN`）；
  - 其余 → 通风单元，**54** 格，减去控制器自己那格 = **53**（与 tooltip 的 "44 Walls / 53 Ventilation Units" 完全对上，这个算术本次已复算）。
- `:119-136` CPU 计数：`18200` versatile → **四表各 +1**；`18201/18202/18203/18204` → 各自 **+4**；内核里放 `18008` 墙 → **+0**（"cheapstake" 分支，tooltip："You can replace CPUs with Walls"）。
- `:64/:143` `MAX_STORAGE_CPU_COUNT = 108`（storage 计数夹紧）；`:144` 有效条件 = 结构完整 **且** 四个计数**都 > 0**。

**能量**（同文件）

| 项 | 值 | 行 |
|---|---|---|
| 包大小 min / rec / max | **256 / 512 / 1024** EU | `:695-697` |
| 超 max | `explode(6)` | `:684` |
| 容量 | `128 + logic*256*conversion` | `:699` |
| 每操作门槛 | `mEnergy >= 128 + logic*64*conversion` | `:216` |
| 每轮固定开销 | `20+logic+control+storage+conversion` EU（"plus more per moved Item/Fluid"） | `:504`、`:666` |
| 移动计费 | `mEnergy -= tMoved` | `:485` |

**缓冲 / 网络 / 路由**

- 缓冲：`mTanks[108]` 每罐 **16000 L**（`:70/:85/:105`）+ 核心自带 **108 槽**物品库存（`:711`）。
- 网络：种子 = 中心 5×5×5 内所有 `ITileEntityLogistics`（`:270-273`）；对每个 `canLogistics(side)` 的面取 Chebyshev 距离 `max(|dx|,|dy|,|dz|)`，**`<= mCPU_Control + 2`** 才允许（`:437-443`），再沿相邻 `ITileEntityLogistics` 泛洪（`:442`）；tooltip："Range: (2+control)m, Cubic AoE"（`:668`）。
- 路由顺序（`:451-500`，每 logic 一次操作）：①Export↔Import（export 表序 Filtered→Semi→Generic × import 表序 Generic→Semi→Filtered，先流体 `moveFluids` 后物品 `moveStacks`，`:455-461`）→ ②Storage 版同构（`:463-469`）→ ③Defragmentation（Storage Generic→Filtered、Generic→Semi，`:473-476`）→ ④Dump（}`:479-494`，每次最多 `conversion` 次 `ST.move`）；**一整轮没搬动就把 logic 还回去**（`:498`）。
- 接口：`gregapi/tileentity/logistics/ITileEntityLogistics.java`（29 行，唯一方法 `boolean canLogistics(byte side)`，继承 `ITileEntityCoverable`）；`ITileEntityLogisticsStorage`（36 行，存储优先级/过滤）；`ITileEntityLogisticsSemiFilteredItem`（31 行）；连接件 `gregapi/tileentity/connectors/MultiTileEntityWireLogistics.java`（**60** 行：`canConnect` 只看对面 `canLogistics`；`canLogistics(side) = connected(side) || 无效面`）。

**注册事实**

| 机器 | GT6 id | 名称/组 | 材料 | 硬度/抗性 | 关键 NBT | 配方 |
|---|---|---|---|---|---|---|
| Logistics Core | **17997** | Logistics Core / Logistics | SteelGalvanized | 6/6 | `NBT_TEXTURE="logisticscore"`、`NBT_ENERGY_ACCEPTED=EU`（**无 NBT_INPUT**） | `CCC\|PMP\|CCC`：M=`OP.casingMachine.dat(aMat)`、P=`IL.Processor_Crystal_Emerald`、C=`OD_CIRCUITS[6]`（`Loader_MultiTileEntities:1281`） |
| Logistics Wire | **24901** | Logistics Wire / Logistics | MT.NULL | 1/2 | `NBT_DIAMETER=PX_P[6]`、`NBT_CONTACTDAMAGE=F` | `PEP\|dFx\|POP`：F=item 24900、O=`OP.wireFine.dat(MT.Os)`、P=`OP.foil.dat(ANY.Plastic)`、E=`OP.gem.dat(ANY.Emerald)`（`:1819`） |

**覆盖板契约**：`gregapi/cover/covers/AbstractCoverAttachmentLogistics.java`（113 行）+ `AbstractCoverAttachmentLogisticsDisplay.java`（55 行）；12 个实体覆盖板（generic 37-41 行 / item 99-107 / fluid 112）+ 4 个 CPU 显示（各 55 行，其值/视觉映射写在核心 `:300-319`）。

### 4.3 本次已落地的一半

`src/main/java/com/gregtech/gregtech/content/logistics/LogisticsCoreStructure.java`（**157 行**，javac exit 0）：

- `cells()`：124 格（125 减控制器自己），按 §4.2 的平方距离分类，坐标用端口既有约定 `(right, up, back)`（`back` 沿 `front.getOpposite()`，`back=0` 就是控制器平面 —— 与 `FusionStructure`/`LargeMachineLayouts.Cell.at` 同一套）；
- `contribution(Block)`：18200→(1,1,1,1)、18201/2/3/4→各自 +4、18008→`null` 特判（cheapstake 不计）；
- `counts(level, controller, front)`：结构校验 + 计数 + `MAX_STORAGE_CPU_COUNT` 夹紧，任一格不匹配返回 `null`；
- `Counts.valid()`（四计数都 >0）、`Counts.energyPerOperation()`（`128 + logic*64*conversion`）；
- 经济学取值 `operations/range/bufferTanks/throughput` 直接对应 GT6 那 4 行 tooltip。
- **未接线**：没有注册、没有 BE、没有测试 ⇒ 对门禁惰性。

### 4.4 下一步确切做什么（建议按 W1→W4 分批，每批独立跑绿）

**W1 核心方块 + BE + 配方（下一批，可独立绿）**

1. `block/machine/LogisticsCoreControllerBlock.java`：照 `block/machine/CokeOvenControllerBlock.java`(82 行) 骨架（`HorizontalDirectionalBlock implements EntityBlock` + `FACING` + `getStateForPlacement` + `getTicker` + `appendHoverText` + `onRemove` 掉落）；tooltip 抄 GT6 那 10 行结构说明（`:150-159`）。
2. `blockentity/machine/LogisticsCoreControllerBlockEntity.java`：`extends GTEnergyBlockEntity implements StructureController`（照 `CokeOvenControllerBlockEntity`），用 `LogisticsCoreStructure.counts(...)` 做 `isStructureOk()`（缓存 40 tick）；实现 `ITileEntityEnergy` 全套（min/rec/max = 256/512/1024、容量 `128+logic*256*conversion`、`doInject` 超限 explode）；NBT 抄 GT6 键名（`gt.cpu.logic|control|storage|conversion` 与 `.used` 四个、`gt.energy`、`gt.tank.<i>`）。
3. **替换占位注册**：`registry/GTLasers.java:101` 的 `new Block(qProps())` → 新控制器方块（**id 必须仍是 `logistics_core`**，资产与 lang 已在）；`registry/GTBlockEntities.java` 加 `LOGISTICS_CORE`（照 `COKE_OVEN` 的 `BlockEntityType.Builder.of(...)` 写法）；
4. 进电角色：墙（18008）走 `portEnergyTypes/injectPortEnergy/portEnergyDemanded`（照 `blockentity/machine/FusionReactorControllerBlockEntity.java:111-127`）；
5. **配方**（生成物，别手改 Java）：`tools/gt6_multiblock_recipes.json` 的 `entries` 加一条 `Logistics Core`（`registryId "17997"`、`tier "MT.SteelGalvanized"`、`pattern ["CCC","PMP","CCC"]`、`keys {M:"OP.casingMachine.dat(aMat)", P:"IL.Processor_Crystal_Emerald", C:"OD_CIRCUITS[6]"}`、`line 1281`），并把 `machines` 列表 + `count` 同步；`tools/generate_multiblock_recipe_table.py` 的 `PART_TARGETS` 加 `"Logistics Core": "logistics_core"`（**不要**放进 `TARGETS`：那会把 `M` 换成结构零件，而 GT6 的 `M` 是机壳）；然后重跑生成器。
6. `gametest/LogisticsCoreTests.java`（新测类，先 `grep BASE_X` 选没被占用的 base，照 §3 陷阱表）：格子数（44/53/27）、四计数（versatile=+1、quadcore=+4、内核放墙=+0）、缺任一类型 → 无效、`energyPerOperation()` 数值、NBT 往返、`MAX_STORAGE_CPU_COUNT` 夹紧、配方 `logistics_core` 能 `RecipeManager.byKey` 加载。

**W2 网络层**：端口新增 `api/logistics/LogisticsHost`（= GT6 `ITileEntityLogistics` 的 `canLogistics(side)`），核心实现 Chebyshev ≤ `control+2` 的种子 + 泛洪扫描；把 `logistics_wire` 从 iconset 装饰块升级为**真连接件**（照 `block/energy/SignalWireBlock(.java)`/`blockentity/energy/SignalWireBlockEntity.java` 或 `ElectricWireBlock` 的连接/渲染写法），补配方（GT6 `:1819` 的 `PEP|dFx|POP`）；核心的 108 罐（16000 L）+ 108 槽缓冲。

**W3 覆盖板行为 + 路由**：12 个实体物流覆盖板行为（GT6 逐文件：`CoverLogisticsGeneric{Import,Export,Storage,Dump}` 37-41 行、`CoverLogisticsItem{Import,Export,Storage}` 99-107、`CoverLogisticsFluid{...}` 112）+ 核心的路由循环（§4.2 的顺序表）+ **必须同时**把 id 加进 `CoverItems.portCoverId`（否则贴上去永不派发）。

**W4 CPU 显示覆盖板**：4 个 display 覆盖板的值/视觉映射，照核心 `:300-319` 的公式（`15`/`14 - min(13, (total-used)*14/total)` 与 `10`/`9 - min(8, (total-used)*9/total)`）。

### 4.5 本批相关的坑（既有教训，别再踩）

1. **加行为不加白名单 = 行为不存在**：`CoverItems.portCoverId`（§108 踩过，`pressure_value`/`panel_asphalt`）。
2. **`MultiblockCraftingRecipes.java` 是生成物**：改表 → 改 `tools/gt6_multiblock_recipes.json` + 生成器 → 重跑（否则下次生成覆盖你的手改）。
3. **结构校验别忘控制器自己那一格**（`LogisticsCoreStructure.cells()` 已跳过；自己写的话要对 `(0,0,0)` 特判）。
4. **门禁在跑时不许改 Java**（会让 `java newest < 门禁日志` 变 False）；**测试数量一变就换新世界**（`Move-Item build\gametest-run\world build\gametest-run\world-<原因>-<时间>`）。
5. **审计结论必须能被门禁复现**（§113）：本文件 §2/§3 的结论都给了 `文件:行`，建议 W1 顺手把「量子充能器/长距离端点已存在」写成断言（逐条遍历注册表），别只活在文档里。

---

## §5 本次验证状态（命令照抄）

```powershell
cmd /c ".\gradlew.bat compileJava --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx512m -Dnet.minecraftforge.gradle.check.certs=false --console=plain > build\compile.log 2>&1"
cmd /c ".\gradlew.bat runGameTestServer --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx512m -PgameTestHeap=4G -PgameTestJvmArgs=-XX:+UseSerialGC -Dnet.minecraftforge.gradle.check.certs=false --console=plain > build\gametest.log 2>&1"
python -c "import io;t=io.open('build/gametest.log',encoding='utf-8',errors='replace').read();[print(l[:200]) for l in t.splitlines() if 'required tests' in l or 'failed!' in l]"
cmd /c ".\gradlew.bat build --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx512m -Dnet.minecraftforge.gradle.check.certs=false --console=plain > build\build.log 2>&1"
python tools\handoff_check.py
```

| 项 | 值 |
|---|---|
| `compileJava` | **BUILD SUCCESSFUL**（2026-09-19，本次） |
| 门禁（第一次，复用 §124 那张世界） | **3 条红，全部与本次改动无关**：`DungeonTests.crateDrawsBlockBeforeMaterial`（"the crate was placed"）、`PipeCoverTests.aPipeDropsItsCoversWhenBroken`、`ItemPipeCoverTests.anItemPipeDropsItsCoversWhenBroken`（后两条 "found 2"＝前几轮遗留掉落物）；同时有 `Can't keep up! … 11273ms or 225 ticks behind`（客户端 PID 55304 在跑）。**判据**：3 条都在 `CreationTime 2026-09-19 02:13:58` 的复用世界上跑，且未被引用的新类不可能影响它们；**处置＝挪走世界**（`build\gametest-run\world` → `world-red125a-20260919-2045`）**换新世界重跑** |
| 门禁（新世界，最终） | **`All 681 required tests passed :)`**（2026-09-19 20:55:16，0 条 `failed!`，测试数 681 不变） |
| jar / 顺序不变量 | `gregtech-1.0.0.jar` **32.5 MB（34,045,205 字节）**、**72,845** 条目（72,840 → 72,845，+5 = 新类的 class 文件）、sha256 `16661AFA3C3C04847C296B49E2D9E4D334E13464A691A8407F853931B8A5C79B`；`java newest < gate log` **True**、`gate log < jar` **True**（`python tools/handoff_check.py`） |
| 注册量 | 不变（本次没有注册任何东西）：items **70,760** / blocks **12,099** / 材料 1156 / 流体 925 / 缺失 token 49 / 多方块部件 89 |

**收尾结果**（本次已完成）：compileJava **BUILD SUCCESSFUL** → 门禁 **`All 681 required tests passed :)`**（20:55:16）→ `gradlew build` **成功**（jar 重建，sha256 `16661AFA…`）→ `tools/handoff_check.py` **True/True** → `tools/audit_porting_progress.py` 已刷新 `docs/porting-progress.json`（注册量不变）。**工作树唯一的 Java 新增就是那个 157 行的结构类**，其余为生成的 JSON/文档。

---

## §6 建议调用的 skills

| skill | 何时用 |
|---|---|
| `tdd` | 本项目节奏：改代码 → 立刻加/改 GameTest → 跑门禁 |
| `diagnose` | 门禁红/行为不符时按「复现→最小化→假设→插桩→修→回归」 |
| `handoff` | 下一轮再交班时重写本文件 |
| 本机的项目专属 skill | `pwsh-run-python-script-file`（PowerShell 里多行 Python 必须写文件）、`pwsh-locate-file-then-read`、`pwsh-inventory-port-ids`、`map-gt6-recipe-keys-to-port-items`、`generate-machine-recipe-table`、`gt6-port-ropes-explosives-and-tint-textures` |

---

## §7 本次**没有**做（明确边界）

- 没有写任何门禁测试、没有接线、没有重建 jar（门禁与 jar 由本次收尾步骤补跑，见 §5）；
- 没有改任何既有 Java/资源；
- **没有执行 `git commit`。**
