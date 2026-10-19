# 自动控制与运行检测状态核查

日期：2026-10-19；基线 `755df5754`。原版源码来自 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，Gregorius Techneticies / GregTech-6 Team，LGPL 来源保持只读。

本批核对 MachineCoverSpec 的条件、实际提供状态的两条路径、控制扩展器转发与红石输出接入。没有完成全部宿主状态生命周期、所有方向红石反馈、原生 Mixin 注入或保存重启验证。

## 条件表达式已有对应

| 控制器 | 原版允许条件 | 当前谓词 |
| --- | --- | --- |
| 红石开关 | 有信号与反向标志异或 | powered |
| 自动红石 | 正在加工且本轮未成功，或红石允许 | processing && !success 或 powered |
| 自动开关 | 具备工作条件或正在加工 | possible 或 processing |
| 定时重启 | 正在加工或周期末十 tick | processing 或 timer % interval >= interval - 10 |

MultiItemTechnological 的五个重启周期为 1200／6000／12000／24000／36000，与当前声明一致。机器返回的状态与时钟若不对，表达式一致仍不能证明行为一致。

原版 AbstractCoverAttachmentController 不以 mStopped 阻止其控制逻辑；当前机器开关分支也没有因所有覆盖板停止而统一跳过，这里不是遗漏。原版 CoverData 按侧面顺序调用控制器；基础机 switchesAllowRunning 逐次覆盖 allowed，通用 beforeTick 逐次 setEnabled，都是最后一个匹配控制器决定结果，不是把所有条件取逻辑且。本轮未逐一证明 setter 副作用和中间状态相同。

## ALIGN-110：通用控制接口丢失“刚完成加工”状态

原版 ITileEntityRunningSuccessfully 明确区分正在加工与刚完成加工，完成检测器读取 getStateRunningSuccessfully。控制扩展器实现该接口，并由 MultiTileEntityExtender 把查询转发给目标机器。自动红石开关也使用此状态避免已完成的一轮继续保持工作。

当前 MachineControl 只有 running／active、progress 等方法，没有 completed/success 对应状态。PanelCoverRuntime 的通用控制器 State 和检测器 State 都把 success 写为 false。ExtenderBlockEntity.machineControl 只转发接口已有的字段，无法补回目标完成状态。

因此，在通用路径上，正常完成检测输出固定为 0，反向配置在 control 有效时固定为 15；自动红石在 processing=true、实际刚完成=true、无外部允许信号的条件下，也会与原版条件不同。这里的真假组合是源码条件，不是已测到所有机器都会出现该组合。

安装入口另有同源缺口：PanelCoverRuntime 对所有 MachineCoverSpec 只检查 supportsSwitch，没有按原版检测器需要的具体状态接口区分，所以可以接受没有成功状态来源的成功检测器。应补齐状态能力与安装条件，不能把运行中简单当作成功。

基础机器不属于“全部成功检测坏掉”：BasicMachineBlockEntity.coverState 传入自己的 successful 标志，updateCoverSignals 另行计算检测器输出；BasicMachineBlock.getSignal／getDirectSignal 读取此专用路径，CoverSignalMixin 也排除基础机。因此只读通用 false 常量不能覆盖其实际实现。聚变专用状态脱节另记 ALIGN-054，不重复算作本项全部内容。

## ALIGN-111：控制扩展器把开工条件近似成活动／进度

原版 MultiTileEntityExtender.getStateRunningPossible 在控制扩展模式下，直接查询目标 ITileEntityRunningPossible。当前 MachineControl 同样没有 explicit possible 字段；ExtenderBlockEntity 未重写 PanelCoverHost.coverPossible，落到 `c.active() || c.progressMax()>0`，也未声明 coverSupportsPossible。

基础机自己的 workPossible 会检查候选配方、并行数和工作成本，不要求已经进入加工。源码条件例子：目标有合法材料和配方，尚未启动、无活动进度，目标可能返回 workPossible=true，但扩展器 c.active=false、progressMax=0，将其判为 false。由此影响扩展器上的可能运行检测器、自动开关，以及状态面板的“可能”能力显示。该例没有在游戏中重现，实际目标刷新时点与能量条件还需测试。

这不是所有宿主的 possible 都缺失：基础机和 EnergyNode 等有各自 coverPossible 重写；本项明确以控制扩展器及使用默认近似的宿主为范围。也不能用 progressMax 是否非零代替一个独立状态接口后宣称原版对齐。

## ALIGN-100 补充：重启周期数值相同但时钟不同

原版定时开关使用实体 tick 回调传入的 aTimer。当前基础机器使用持久化的 coverTicks（每个有效机器 tick 增加，保存为 gt.cover_ticks），通用 PanelCoverRuntime 使用 level.getGameTime。后者使同世界设备按世界时间相位触发，不是各自放置后的实体周期。

这会影响首次触发、不同时间放置的设备与加载恢复后的相位，不能把周期数值对应写成定时行为完全对应。沿用 ALIGN-100，不新增重复编号。本轮没有实际存档重启测试，也未穷举所有特殊控制器的调用时钟。

## 红石路由和剩余边界

- 普通检测器反相和强／弱输出已有实现，对应 AbstractCoverAttachmentDetector 的反向位与强度位。并未因使用现代独立布尔字段而判为错误。
- 基础机经自身输出方法，其他宿主有 CoverSignalMixin 的 BlockState 返回值包装；两版配置均包含它。输入使用邻接坐标查询，输出使用 query 的相反面，实际六面红石连线尚未运行验证。
- ExtenderBlockEntity 虽位于 block.misc 包，但 ComponentCoverFallback.ownClass 会遍历父类，EnergyRelayBlockEntity 位于 blockentity 包，因此不能只看子类包名误判它不进入通用信号包装。
- Mixin 的目标和配置存在，不证明所有环境下都已正确注入；其实体筛选、相邻强信号反馈、多个面板交互仍留在队列。

## 代码整理和验证

展开 MachineCoverSpec 的注册查询、控制条件与检测器信号计算；展开 MachineControl 默认能力方法，注明没有 possible／success 状态；在通用检测器构造 State 处说明 false 的来源。未改变接口成员、字段值、公式、调用顺序或玩法。

三份共享文件分别投影 Forge／Neo，六份代码与字面量比较一致。仅做必要源码核对与差异空白检查，未新增模拟测试、编译、GameTest、游戏、保存重启、构建或推送。见 [来源与验证回执](verification/controller-detector-audit-20261019.json)。

下一步继续面板显示／传感器的状态与量化、状态同步、特殊控制器及全面核查队列。
