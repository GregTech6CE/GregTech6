# 过滤器与物品检索器覆盖板核查

日期：2026-10-19；基线 `a190181c9`。原版根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。主要来源 CoverFilterItem、CoverFilterFluid、CoverRetrieverItem、ST.move 与 ItemStackSet／Map／Container；原作者 GregTech-6 Team / Gregorius Techneticies，原始 LGPL 文件只读。

本轮核对匹配、设置、检索器调度和实际流向。不是物流总线过滤系统、所有宿主权限或所有物品数据迁移的完整验收。

## 普通过滤器已经具备的路径

- 原版普通物品／流体过滤器，停止时阻断；没有模板时白名单阻断、黑名单放行；有模板时按黑白模式翻转匹配。当前 CoverUtilityBehaviors.filterPermits 和对应物品／流体包装有这些路径。空候选查询边界仍需分别验证，不把普通有效候选的结论扩展到 null／EMPTY。
- 普通物品过滤器使用物品身份和伤害值匹配，忽略其余标签／components；再次用匹配物品点击切换伤害通配。当前 itemFilterPermits 与 setItemFilter 已实现，并未把所有物品数据当作严格条件。
- 流体过滤器匹配真实 Fluid 身份、忽略标签，原版 FL.equal(..., true) 也是如此；不是上一批容量查询中混用 FluidType 的问题。
- 螺丝刀切换黑白名单、软锤清模板而保留模式，当前有独立设置／保存代码。流体模板设置是一次性的；普通物品重复点击另有通配逻辑。
- 原版不允许把物品过滤器装在两个物品管道之间；当前 PanelCoverRuntime.canAttach 和 CoverConnections 有对应检查。已核对存在性，没有运行所有连接／拆除时序。

## ALIGN-102：检索器变成从邻接库存向自身抽取

原版 CoverRetrieverItem 仅允许安装在可 tick 的物品管道上。实际 onTickPre 从管网扫描、按成本排序，遍历管道允许接收的邻接库存，排除检索器面对的目标库存；第一次 ST.move 成功就返回，并给已使用管道累计传输次数。目标 tTarget 是覆盖板面对的库存。

当前 ItemPipeBlockEntity 调用 CoverUtilityBehaviors.tickRetriever 时把自身 this 作为 target；BasicMachineBlockEntity 则把自身 itemHandler 作为 target。该函数只读取 front 的物品能力作为 source，完全没有管网扫描，按源槽循环向 target 插入。

因此原版的“管网远端库存 → 覆盖板面对的库存”变成“覆盖板面对的库存 → 宿主自身”。原版首次搬动一组即结束，当前可在 64 个物品预算内继续多个源槽、多个物品种类；物品管道单槽容量会进一步限制实际结果，不能声称每种宿主都会一次混入多种物品。

当前也没有原版的管网吞吐准入／计数合同；这与已记录的 ALIGN-080 物品管道限流缺口有关，不另宣称已有完整网络只差排序。PanelCoverRuntime 没有把检索器安装限制为物品管道，基础机器存在实际执行分支，所以这不是单纯遗留的未使用 helper。

调用链已确定，未在游戏中搭建远端箱子网络。源码注释中“用面前库存替代管网而其余不变”的描述已删除。

## ALIGN-103：恢复后的待执行标志按成功清除，失败时持续重试

原版 onStoppedUpdate 在解除停止时把 mValues 置 1；onTickPre 同时满足服务端、未停止、周期或 pending、管道类型和 pipeCapacityCheck 后，在扫描前将 mValues 清零。即使这次找不到来源或目标无空间，也不会保留 pending 到下一 tick；若没有通过容量门控则仍保留。

当前管道和基础机器调用 tickRetriever 的返回值是“是否实际搬动物品”。两处调用者都仅在 acted=true 时清 pending；恢复当 tick 没搬到物品则置 true。此后 pending 绕过二十 tick 的周期门控，失败后继续保持，可逐 tick 重试直到成功。

这改变恢复后的尝试频率，但不是“停止开关失效”：stopped 的提前返回仍在。当前本地 coverTicks 与原版 SERVER_TIME 的相位问题沿用 ALIGN-100 的调度审计；本项专门记录 pending 生命周期。

两个 pending 存储都未持久化。已删去“标志最多只有一 tick，重载一定重新武装”的未证实注释；完整保存恢复行为未验。

## ALIGN-104：检索器误用普通过滤器和物流模板的规则

原版检索器没有模板时将 tFilter 设为 null，ST.move 仅在 aFilter 非 null 时执行筛选，因此两种模式均允许任意候选。当前直接调用普通过滤器的 filterPermits；空白名单返回 false，使默认无模板的检索器不取任何东西。普通物品过滤器空白名单应阻断，不能通过全局反转 filterPermits 来修检索器。

有模板时，原版 ST.move 使用 ItemStackSet.contains(candidate, true)，沿 ItemStackMap 到 ItemStackContainer，仅比较物品与元数据，支持元数据通配，不比较额外 NBT。当前检索器使用物流 FilterRules.itemMatches：模板无标签／components patch 时忽略其余数据，否则调用完整 ItemStack 标签／components 比较。

这意味着同一种、同伤害值的工具，如果候选多了名称等数据，而模板带有伤害数据，当前可能拒绝，原版集合匹配不因此拒绝。这里描述原生源码规则，没有假定所有物品模板都为空标签，也没有把 1.7.10 元数据与现代 components 视为完全等价。

检索器设置模板也复用了普通物品过滤器的 setItemFilter，允许重复点击切换 gt.filter.wildcard；但 tickRetriever 不读取这个字段。原版检索器右键仅在没有模板时设置，后续不切换通配。普通物品过滤器已经读取其通配字段，不属于这个遗漏。

## 代码标准化与验证

展开 core FilterPolicy 的压行定义和原生 FilterRules 的两版匹配适配、前缀识别；说明它是物流模板策略，而不是所有过滤器／检索器统一的原版规则。整理模板设置代码，修正 CoverUtilityBehaviors 及两类宿主对检索器流向、pending 和存档的误导注释；流体过滤说明改为实际使用的 Fluid 身份比较，删除相互矛盾的 stack equality 描述。五份生产文件代码／字面量保持不变，四份原生文件分别展开 Forge／Neo 分支核对。

本轮只做源码与字面量等价检查，不新增模拟网络测试，也未运行原生编译、游戏、保存重启、构建或推送。现有普通物品过滤器 GameTest 有伤害通配场景，不等于证明检索器使用了同一字段。来源哈希和验证边界见 [回执](verification/filter-retriever-audit-20261019.json)。

待继续：各宿主的插入／抽取覆盖板拦截、流体模板的材料容器回退、空候选查询、UI／tooltips 与实际筛选条件、失败回滚和物流总线自己的模板合同。
