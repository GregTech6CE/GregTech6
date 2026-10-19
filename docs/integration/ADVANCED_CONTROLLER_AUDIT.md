# 聚变与内爆主控核查

日期：2026-10-19。基线：`4f7932a5b`。范围是这两个控制器的结构目录、启动／开工条件、运行状态出口与内爆启动声音；不是全部多方块验收。本批保持玩法代码，整理共享参数／布局和聚变生成器。

## 来源与当前入口

原版源码根目录：`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。

- `gregtech/loaders/b/Loader_MultiTileEntities.java`：1228 行内爆 17110，1242 行聚变 17198。
- `gregtech/tileentity/multiblocks/MultiTileEntityFusionReactor.java`：结构、处理器数量、四个 EU 出口；继承 `TileEntityBase10MultiBlockMachine`，加工仍走 `MultiTileEntityBasicMachine`。
- `gregtech/tileentity/multiblocks/MultiTileEntityImplosionCompressor.java`：25 壁板／中空、输入输出位置与 `onProcessStarted()`。
- `gregapi/tileentity/machines/MultiTileEntityBasicMachine.java`：`canOutput()`、`checkRecipe()`、`doInject()`、`doActive()` 和状态／进度接口。
- `gregapi/tileentity/multiblocks/MultiTileEntityMultiBlockPart.java`、`gregapi/tileentity/base/TileEntityBase01Root.java`：部件能量调用转给主控，不绕过主控停止检查。

当前共享原生 Java 根目录：`multiversion/src/main/java/com/gregtech/gregtech`。

- `blockentity/machine/FusionReactorControllerBlockEntity.java`：专用 `tickFusion()`、`startRecipe()`、`injectPortEnergy()`。
- `blockentity/machine/ImplosionCompressorControllerBlockEntity.java`：结构成立才调基础机器 tick。
- `blockentity/machine/BasicMachineBlockEntity.java`：私有基础机状态、`machineControl()`、覆盖板 tick／信号。
- `blockentity/machine/LargeRecipeMachineBlockEntity.java`：聚变输出罐使用 `Long.MAX_VALUE`。
- `blockentity/machine/MultiblockPortBlockEntity.java`：部件能量接受／需求／注入代理。
- `content/cover/PanelCoverHost.java`、`PanelCoverRuntime.java`：从 MachineControl 取得状态／进度，更新面板信号。

NeoForge 的 `block/machine/BasicMachineBlock.java` 分派到聚变专用 ticker；Forge 在版本目录 `block/machine/FusionReactorControllerBlock.java` 分派到同一个 ticker。下列专用执行差异涉及两版本，未逐版运行 Minecraft。

## 已有对应部分

不能用基础机器拒绝负功率配方来判断聚变没有发电。当前专用执行器接受非正 EUt，先扣料／保存待产物、等 LU 充能、每 tick 发 `-EUt`，完成时交付；同一配方连续运行免再次充能。主控本体不接能量，环部件区分 LU 输入与 EU 输出，CPU 验证允许坐标互换但要求总数 3／12／12。

Java 17 独立 core 观察：

| 项目 | 当前结果 | 原版对照 |
| --- | --- | --- |
| 聚变结构（不含主控） | 886 格，576 钨壁／144 线圈／50 通风／36 不锈钢壁／53 镀锌钢壁／处理器 3+12+12 | 数量对应原版结构和说明 |
| 聚变角色目录 | 454 CASING、68 ENERGY_INPUT、4 ENERGY_OUTPUT、360 ITEM_FLUID_IO | 四个能源出口在环的四个轴端，目录存在不等于所有交互正确 |
| 内爆结构（不含主控） | 25 ITEM_IO 壁板、一空气 | 几何数量对应；原版壁板还有流体／能源角色，本批未认定其全部语义等价 |
| 聚变参数 | TU，额定 8192、最小 1、最大 16384、并行 1；启动 LU | 对应原版注册数值 |
| 内爆参数 | TU，额定 1、最小 1、最大 16、并行 64 | 对应原版注册数值 |

几何复现检查比较整个生成文件的坐标、顺序、部件与角色，886 格全文件未变；没有进行四向游戏结构搭建。原版部分区块未加载时保留旧结构结果、当前原子绑定／失效释放的模式也见 ALIGN-036，不重复记成新的独立缺项。

## ALIGN-052：聚变开工绕过输出准入

原版 `checkRecipe()` 在扣输入前调用 `canOutput()`：检查对应物品槽、空输出要求、不同流体需要的空罐，以及非豁免流体的积压阈值 `max(16000, 1 + 单次输出量 × 并行上限)`。

当前 `startRecipe()` 从输入查表后直接 `RecipeInputs.consume()` 和 `MachineWorkOutputs.roll()`，写回扣料结果；没有调用输出可容纳／空输出／积液预判。直到运行到末尾才 `products.flush()`。例如先把所有输出罐占满不相容流体，再提供有效输入和启动 LU，原版不启动，本实现可先扣料并推进发电，最后挂起待产物。

这比 ALIGN-038 的焦炉积液阈值更广：聚变连其他输出准入也未走。当前大容量输出罐不是 8000 L，不能靠改硬容量解决。末尾 flush 失败仍保留待产物，不据此声称已丢失产物。现有 `FusionRegressionTests.reservedProductsSurviveSaveAndBlockedOutputs` 在**开工后**堵输出，不能证明开工前堵塞的准入正确；本批未运行该 GameTest。

## ALIGN-053：聚变停机仍可充入启动 LU

原版部件 `doEnergyInjection()` 通过 Root 的转发进入主控；`isEnergyAcceptingFrom(..., false)` 和 `doInject()` 均有停止门槛，`mStopped` 时返回零。

当前用 `machineControl(null).setEnabled(false)` 可以使聚变 tick 暂停，但 `MultiblockPortBlockEntity.isEnergyAcceptingFrom()` 只查能源角色和结构，`doInject()` 仅查结构再转发。聚变 `injectPortEnergy()` 检查 LU、角色、正包、大小不超过 16384、剩余启动能量和结构，完全不读取 `controlStopped`／enabled；需求接口也不读。

因此，已经开始扣料且仍待充能的聚变，在完整结构下停机后仍可经部件消耗 LU 并减少 `chargeRemaining`。不是“空闲可预充”：当前确有 `chargeRemaining > 0` 门槛。正常停机后的接收差异为源码路径结论，未游戏复现。超额能量包的原版过载与当前拒绝、充能后停机／重载的连续性另待核，不混进本结论。

## ALIGN-054：聚变专用运行状态未接上基础控制／覆盖板

原版聚变沿基础机器运行，`getStateRunningActively()`、`getProgressValue()` 等与加工共用 `mActive`／`mProgress`，不是另一份永不更新的数据。

当前聚变保存独立的 `running/progress/duration/products`，覆写 `isRunning()` 和 `getProgressPercent()` 并更新外观方块状态。但继承的 `machineControl()` 匿名对象直接读父类私有 `mRunning/mActive/mProgress/mMaxProgress`，不会调用这两个覆写方法；聚变正常新任务不更新这些字段。`PanelCoverHost.coverControl()` 优先取这个 Provider，`PanelCoverRuntime` 的进度／状态输出随后读取的就是父类字段。

此外专用 tick 没有经过基础 tick 的 `coverTicks++`、`panels.beforeTick()`、`tickCovers()`、`updateCoverSignals()`／`panels.afterTick()`。故它不是仅仅 GUI 一个百分比显示问题，基础控制状态与覆盖板周期动作／信号刷新也缺少常规调用。附着／拆卸等动作仍可能触发局部刷新，不能称所有覆盖板完全无效；`switchesAllowRunning()` 仍会检查基础控制开关。具体覆盖板逐项、外部传感器及重载状态尚未游戏验证。

## ALIGN-055：内爆加工开始缺少爆炸声

原版内爆覆写 `onProcessStarted()` 调用 `UT.Sounds.send(SFX.MC_EXPLODE, this, F)`，由基础机成功开始配方时触发。

当前内爆的成功结构只转入 `BasicMachineBlockEntity.serverTick()`；这两个类都没有对应启动音效调用或 `onProcessStarted` 分派。不是要求真实爆炸破坏方块，而是缺少原版加工开始的声音反馈。音频效果未游戏试听。结构破坏后外层 ticker 跳过父 tick 的状态清理、壁板流体角色等仍留在后续核查，不在本条扩大断言。

## 本批标准化和验证边界

- 展开 `OriginalAdvancedControllerData` 与 `SharedImplosionStructure`，保持代码和字面量；不调整参数、角色、循环顺序。
- `tools/import_fusion_structure.py` 改为真正生成 core 的 `SharedFusionStructure`，不覆盖原生 `FusionStructure` 适配器、不需要创建 native 投影视图。新增 `--source`、`--output`、只读 `--check`；拒绝来源与输出相同，尺寸／数量／重叠用显式异常检查。
- 原生成器写旧原生类名和路径，不能再用它重建当前共享结构。当前生成结果与已维护表全文一致。`--check` 对不一致输出返回失败且不写文件。
- 当时 `tools/tests/test_fusion_resources.py` 仍从原生适配器寻找坐标、经工具视图读取 Forge 主控资源，本批未运行。后续第十四批已改为维护目录并分别检查两版主控；`fusion_reactor_main.json` 在 core 中仍存在、Forge 仍使用，不能说该资源已废弃。见 [后续核查](MATTER_AND_FUSION_CONTINUITY_AUDIT.md)。

实际运行 Java 17 的参数／布局观察与两个格式化类、聚变表独立编译；生成器进行了全文核对、临时输出以及拒绝写入检查。详见 [回执](verification/advanced-controller-audit-20261019.json)。没有运行 Gradle、全套测试、GameTest、客户端、存档重启或打包；本批四项玩法差异尚未修复。

下一步继续特殊主控的重载／停机连续性、物质制造机与壁板交互，再扩大储能／变压器与跨区块网络。全面审计与源码标准化继续进行。
