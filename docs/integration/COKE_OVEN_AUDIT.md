# 焦炉与共用加工入口核查

日期：2026-10-19；基线：`90196e6de`。原版根目录：`C:/Dev/gregtech6-master/gregtech6-master/src/main/java/`。本批检查原版 `MultiTileEntityCokeOven`、`TileEntityBase10MultiBlockMachine`、`MultiTileEntityBasicMachine`，以及当前 `CokeOvenControllerBlockEntity` → `LargeRecipeMachineBlockEntity` → `BasicMachineBlockEntity`。本批没有修改玩法。

## 已有实现及验证边界

原版 `gregtech/loaders/b/Loader_MultiTileEntities:1193` 的 17000 焦炉使用 Ceramic、TU、额定输入 1、范围 1–16、并行上限 16、硬度／抗爆 5，要求点火。当前 core 参数与原生 `CokeOvenControllerBlock.makeSpec` 对应：顶面物品输入、底面物品输出、非顶面流体输出、向下自动排液，没有自动物品输入／输出。

core 实际枚举为 25 个 18000 耐火砖位置、一个空气位置，另有主控自身；九个流体接收位置位于底层之下。四个水平朝向上，接收位置集合与原版围绕主体中心的九格集合相同，**遍历顺序不全相同**，见 ALIGN-037。

现有煤配方表 `content/recipe/generated/GTOtherGen:175` 保留 0 EU/t、3600 tick、500 L Creosote 的一煤→一焦煤行，原版 `Loader_Recipes_Other:775` 有对应行。实际 core `MachineWorkCost` 对此参数的 1 和 16 并行均为 minimumPower=1、totalWork=3600，符合原版 TU 并行不乘时长的分支。这是计算类观察，不是完整 Minecraft 配方注册／生存链验收。

现有焦炉会持久化点火倒计时，保存加工进度和已抽取输入的待输出产物，结构恢复后能沿既有进度继续；字段和源码分支存在不代表已通过世界重启测试。点火只限制启动，不会要求整个 3600 tick 加工都保持点火窗口。

## ALIGN-036：不完整结构的部件归属不同

原版 `MultiTileEntityCokeOven.checkStructure2:54–71` 检查全部格子，遇错仅设 `tSuccess=false`，仍对其他正确部件执行 `checkAndSetTarget`。该工具会逐块保存主控、角色和设计；缺一块不等于剩余部件全无主控。边界区块不在场时则返回原 `mStructureOkay`。

当前 `LargeRecipeMachineBlockEntity.isStructureOk` 首次遇到缺区块、空气被堵、错误部件就 `bindings.clear(this::release)` 并返回 false；只有收集完整候选后才调用 PartBindings.update。它是整体验证后绑定的策略，不是原版逐格认领。共享 `MultiblockPortBlockEntity` 的工具／传感器转发依赖保存的主控位置，因此损坏结构上的已绑定部件诊断行为也可能受影响；这里没有声称原版失效部件仍能正常加工，原版传输还有结构门槛。

## ALIGN-037：自动排液时机、速率和目标优先级不同

原版 `MultiTileEntityBasicMachine.onTick2:451–463` 在 doWork 之前调用 doOutputFluids，停机和结构失效不阻止这一步。doOutputFluids → `FL.move(IFluidTank, Delegator)` 使用默认最大搬运量（Long.MAX_VALUE，原生流体 API 处再受 int／接收空间限制），没有 1000 L 的机器节流。

当前焦炉从父类普通 autoIO 路径排液；BasicMachine tick 在结构失效或开关禁用时提前返回。其 `autoOutputFluids:1016` 每次只模拟 `drain(1000)`。LargeRecipeMachine 对另一些原版大型机器有工作前排液补救，但 `OriginalLargeRecipeMachineData.handles("cokeoven")` 为 false，且焦炉覆写 beforeMachineTick 只递减点火时间，不能拿其他机器分支证明焦炉已对齐。

例：已存 8000 L 杂酚油、接收罐能收完且没有覆盖板阻拦，原版一次可搬完，当前一次最多 1000 L；开关关闭时当前此自动路径不执行。外部管道主动抽取属于另一条路径，不包含在“自动不排液”的判断中。

原版焦炉按世界 X/Z 从主体中心 (-1,-1) 开始找第一个可接收端；当前九格按主控朝向旋转。朝东时，相对主控的首个候选原版为 (-2,-2,-1)，当前为 (0,-2,-1)。两者覆盖同样九个位置，但多个接收端同时存在时优先级不同。二者都有目标缓存，缓存目标依旧存在时的继续使用不应被误判为遗漏。

## ALIGN-038：焦炉没有原版流体积压停产阈值

原版 `MultiTileEntityBasicMachine.canOutput:651–666` 对同种已有流体检查 `max(16000, 1 + 单次流体输出量 * mParallel)`；达到该量就拒绝开启下一批，VOID_OVERFLOW 流体除外。原版 `FL.Oil_Creosote` 无该豁免。

当前焦炉把输出罐设为 Long.MAX_VALUE；`outputLimitedParallel`／`fluidsFit` 仅检查可容纳本批输出的剩余空间，未应用上述积压阈值。以每煤 500 L、16 并行为例，原版阈值为 16000 L；在接收端堵塞、已有 16000 L 杂酚油但焦煤输出位仍有空间时，原版不再开下一批，当前只要其他条件满足仍可继续生产。该差异来自实际原版公式、当前配方行和分支核对，没有启动游戏。

原版的阈值是“已有量到达后停开下一批”，不是把罐硬容量设成 16000 L，也不是所有大输出配方都统一用 16000。修复不能直接缩小储罐，更不能删除真实已存流体。

## ALIGN-039：点火剩余时间的消费顺序差一刻

原版点火工具把 mIgnited 设为 40，在 doWork 结束才递减。checkRecipe 启动许可为 `!mRequiresIgnition || mIgnited > 0 || mActive`。当前焦炉 beforeMachineTick 先调用 tickIgnition，再在 recipeStartAllowed 中只检查 ignitionTicks>0。

具体状态：无运行中配方、剩余点火时间 1、当前输入和输出条件允许启动。原版本 tick 可启动后再减为 0，当前先减为 0 而拒绝启动；实际 core 观察 tickIgnition(1)=0，原生调用顺序另由源码核实。已经在加工的任务并不会因此在第 40 tick 被取消。

原版每成功输出一个待输出项目／流体都会刷新点火窗口，当前只在全部 pendingOutputs.flush 成功后调用 onProcessFinished 刷新；部分输出堵塞的精确状态还需实际原生测试，不把它与上述一刻边界混作同一个已运行验证。

## 结构标准化与后续

展开 OriginalCokeOvenRules、OriginalMultiblockMachineParameters、LargeMachineProcessingRules 三份压行源码，保持方法名、所有代码 token、字面量、注册顺序与署名不变。Java 17 独立编译及归一化比较通过；焦炉参数、几何、TU 计算和点火边界由小型 core 观察输出，见 [回执](verification/coke-oven-audit-20261019.json)。

没有运行 Gradle、Minecraft、GameTest、世界重启或新构建。共用加工器的剩余能量继承、模式／空输出条件、全部输出映射、配方动态回调、保存恢复和所有多方块并未完成核查；尤其本批查看父类的结论只在焦炉调用链上确认，不直接宣称每一台机器都具有同样缺陷。
