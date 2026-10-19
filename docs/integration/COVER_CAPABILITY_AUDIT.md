# 覆盖板侧面能力接入核查

日期：2026-10-19；基线 `1a6d5c3b1`。原版来源 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，GregTech-6 Team / Gregorius Techneticies，LGPL 源码只读。此次沿安装、能力提供、包装器、Mixin 选择条件到原版父类核查，不以 helper 存在代替实际调用。

## 原版通用拦截入口

`TileEntityBase06Covers.canInsertItem` 与 `canExtractItem` 是 final 方法，先调用该面的覆盖板 intercept，再调用宿主自己的 canInsertItem2／canExtractItem2。电池盒沿 `TileEntityBase10EnergyBatBox → TileEntityBase09FacingSingle → TileEntityBase08Directional → TileEntityBase07Paintable → TileEntityBase06Covers` 继承此入口。其电池种类准入并不取消过滤器筛选。

普通物品过滤器在停止时拦截，空白名单也拦截；原版检索器则始终拒绝自身面的普通物品插入和抽取，主动搬运另走管网路径。泵／传送带／机械臂的被动方向拦截按 visual 判断，不按停止状态直接封死两向。当前 ComponentCoverRules 的方向谓词与这些表达式对应，不能因为被动方向没有 stopped 就判为缺失。

## 当前接入矩阵

以下是源码路径结论，不代表全部实体已经游戏验证。

| 路径 | 普通物品过滤 | 检索器普通进出拦截 | 备注 |
| --- | --- | --- | --- |
| BasicMachineBlockEntity.SidedItemHandler | 有，插入／抽取／isItemValid 都调用 | 有，coverBlocksItemTraffic | 在现有方向和机器输入准入之后共同生效 |
| ItemPipeBlockEntity.FaceItemHandler 及内部推送 | 有，coverPermitsItemTraffic | 有，组合谓词先拒绝检索器 | 外部连接位／入口锁问题已记 ALIGN-082，不重复计数 |
| MetalChestBlockEntity／UsbSwitchBlockEntity 的 ComponentCoverAccess | 有，读取当前覆盖板和停止状态 | 无 | 通用物品适配器只识别组件方向和普通物品过滤 |
| EnergyNodeBlockEntity.sidedBatteryInventory | 无 | 无 | 原生包装器只有挡板、组件方向和电池自身准入；见 ALIGN-105 |
| ComponentCoverCapabilitiesMixin | 取决于 uses(owner)，不是覆盖所有上述原生路径 | 通用包装器自身没有 | Forge 列表与 Neo BlockCapability 注入均有相同后备宿主门控 |

基础机器 SidedFluidHandler、流体管道侧面能力及 ComponentCoverAccess.fluids 都有过滤填充／抽取的调用。涡轮蒸汽口已显式使用 fluids 包装；不能从电池物品侧遗漏推成全部能源设备都漏过滤。

## ALIGN-105：电池盒可以装过滤器，但侧面库存未执行过滤

安装链：EnergyNodeBlockEntity.attachCover → PanelCoverRuntime.canAttach。物品过滤器只要求宿主 componentItems 非空；PanelCoverHost 默认获取无方向物品能力，电池盒提供 batterySlots，因而存在可安装路径。

执行链：Forge getCapability(ITEM_HANDLER, side) 提供 sidedBatteryInventory；Neo GTEnergyNodes 的注册明确指向 EnergyNodeBlockEntity.itemCapability，非 null side 也进入同一包装器。insertItem／extractItem 检查 shutter 和 ComponentCoverRuntime.allowsItem，后者仅处理传送带／机械臂，不处理 FILTER_ITEM。isItemValid 同样没有物品过滤调用。

这使默认空白名单、已设置的物品名单以及过滤器的停止门控无法在电池盒该能力路径生效。电池自身种类限制仍然存在，不能描述成任意物品都可以塞入。

还核对了潜在补偿入口：ComponentCoverCapabilitiesMixin 虽在 Forge 目标列表中列出 EnergyNodeBlockEntity，但会调用 ComponentCoverFallback.uses。后者要求 getCover 的声明类是注入后的 BlockEntity；EnergyNodeBlockEntity 自己重写 getCover，因此不会被此后备包装补上筛选。Neo 分支也有相同 uses 门控。Mixin JSON 已包含该类，实际注入运行未测试。

当前只展开包装器代码、补充职责说明，没有把此次审计变成玩法修复。

## ALIGN-102 补充：过宽安装还产生无功能的库存检索器

原版检索器仅允许物品管道。当前 PanelCoverRuntime 没有对应特判，落到 CoverItems.isCover；以金属箱为例，ComponentCoverStorage.attach 接受这一结果。普通金属箱显式使用 ComponentCoverAccess.items：其中 ComponentCoverRuntime.allowsItem 对检索器返回放行，itemFilterPermits 也因不是 FILTER_ITEM 而放行，所以不具备原版检索器的普通进出拦截。

ComponentCoverStorage.tick 只调用组件 tick 和面板生命周期；PanelCoverRuntime.tickAttachments 只处理排水口／通风口。当前 tickRetriever 的实际调用者仅为基础机器和物品管道。因此金属箱上会出现可装、可设置模板，但不执行检索搬运的覆盖板；这属于 ALIGN-102 安装范围与执行路径不对应的补充，不另设重复编号。基础机器／管道已有检索器拦截，不能把此结论扩展到它们。

## 不应误报的边界与未完成项

- 流体无指定种类的抽取：原版父类先选可抽罐，再用实际流体执行过滤；通用包装器先模拟抽取一个候选再过滤，方向与此对应。不能仅因没有自动跳到下一个罐就判定缺少筛选。本轮未证明每个多罐宿主的选罐顺序、原生模拟合同或执行一致性。
- 普通物品过滤器对 EMPTY／null 候选的返回，以及空黑名单流体过滤器先返回 true 的顺序，与原版某些空候选入口不同。当前已读入口仍有底层空栈／抽取限制；尚未建立有效物品／流体绕过案例，先保留为边界待验，不宣称有实际资源泄漏。
- 后备 Mixin 在 Forge 使用 require=0，源码目标列表和 JSON 不等于运行时每个方法都注入成功；未启动游戏，不声称全宿主覆盖。
- 普通过滤器已有的 GameTest 检查伤害通配，不覆盖本轮电池盒实际侧面能力缺口。
- 待继续：流体模板的材料容器回退、压力阀、覆盖板 UI／tooltips、所有宿主自动输出与直接物流接口、失败回滚及完整保存恢复。

## 代码整理和验证

展开 ComponentCoverAccess 的两版物品／流体访问方法，将门控、候选模拟和委托调用分行；展开 ComponentCoverFallback 的原生／后备宿主识别；展开电池盒侧面库存方法。保留表达式、执行顺序、版本分支和原有版权头。

三份共享生产文件相对基线分别投影 Forge／Neo，共六份代码与字面量比较一致。仅做此项必要轻量验证及差异空白检查；没有新增模拟测试、原生编译、完整测试、游戏、存档重启、构建或推送。详见 [来源与验证回执](verification/cover-capability-audit-20261019.json)。
