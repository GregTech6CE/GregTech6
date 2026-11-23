# 工具交互行为

本页记录 CE 与原版 GT6 在工具交互上的行为差异。它们落在真实 `//?` 分支附近，但分支本身通常是平台 API 或类落点差异；折叠分支时不能改掉这里的行为。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| `isInteractionTool` | 保留 `isTool(stack)` 守卫：没有 `GT.ToolStats` 的堆叠不算交互工具。原版按物品基础 `toolType()` 计算合成损耗，不按口袋工具当前模式。 | 防止空电动工具或未组装工具被误判为可交互；见 `GTToolHelper.isInteractionTool`。 |
| plunger | 机器路径先抽输出罐；输出罐为空时才抽输入罐；每次最多 1000 mB。抽出的流体交给 `GarbageData` 丢弃。 | 原版 `Behavior_Plunger_Fluid` 的顺序；CE 把机器路径补进 `BasicMachineBlockEntity.useMachineTool`，见 `purgeTank`。 |
| igniter | 物品级点火已存在；`BasicMachineBlock.useRemainder` 对会消费点火器的宿主返回 `PASS`，避免 GUI 吞掉点火。 | 原版 `Behavior_FlintAndTinder` 在方块拒绝后执行；CE 的机器 GUI 不能抢先消费。 |
| 封面工具守卫 | 封面过滤器的工具判断用 `PanelCoverHost.consumesTool`，由宿主按实际工具动作决定；静态列表会漏 igniter/plunger、多 soldering_iron。 | 原版静态列表跨宿主近似；CE 改为宿主守卫，见 `UtilityCoverInteraction.use`。 |
| 胶带合卷 | 合并四卷旧胶带的剩余用量，结果最多一整卷。 | 成分随剩余用量缩放，合卷后的可回收材料受输入约束。见[物品成分解析](../material-system/item-composition.md#从当前配方反推成分)。 |
| 拆解覆盖板回收 | 拆除机器、管道或线缆（含红石线缆 `SignalWire` 上附着的原版红石火把、红石中继器等附加覆盖物）时，覆盖板与附加物均不随原版 NBT 打包进宿主方块物品，而是经由 `CoverDrops` / `ComponentCoverStorage` 拆分单独掉落并剥离面板运行态；使用扳手、活动扳手等可自动收集工具拆解时，覆盖板同样优先尝试插入玩家背包，满则溢出掉落。 | 保持宿主物品干净、避免重复或非法跨宿主附板，同时保留原版扳手拆解自动入包的收集体验；见 `CoverDrops.dropCover`、`CoverCollectDropMixin` 与 `ComponentCoverRemovalMixin`。 |
| 流体容器与展示物品 | 常规流体容器（`FluidContainerInteraction`、`CrucibleContainerInteraction`、`WoodenBucketItem`）在创造模式与生存模式按同一套存量消耗与置换规则处理；`FluidDisplayItemLike`（含通用载体 `gt6ce:fluid_display`，流体身份存于 `gt.display_fluid` 载荷）作为创造物品保留创造获取门槛（EMI 仅在作弊模式从原生流体代理提取 `gt6ce:fluid_display`，并在索引中隐藏 `gt6ce:fluid_display` 与 `fluid_item_lava` 以防重复），但一旦持有即允许在创造与生存模式灌入储罐、管道、机器、坩埚及 `fluid_funnel`/`cap_nozzle` 且不消耗自身；`tap`/`nozzle` 仍只抽不放，展示物品在任何模式下均禁止饮用。流体滤网盖板按固定 1000 mB 只读取样，物体滤网与吸取盖板拒绝展示物品。 | 常规容器两模式同语义避免创造凭空复制容器与流体；`FluidDisplayItemLike` 作为可由管理端统管回收的创造灌液工具，允许生存手持用于布设或测试产液，同时保留不可饮用与不进物体筛选的边界。 |
| 筛选覆盖板覆写与掉落 | 已贴附的 `item_filter`、`fluid_filter`、`item_retriever_cover` 与 6 种可筛选物流总线允许持不同有效物品或有效流体容器（含对流体筛选使用 `FluidDisplayItemLike`）右键直接覆写新筛选目标（覆写物品时重置 `gt.filter.wildcard`）；`item_filter` 持同物品再次右键仍保留精确 ↔ 通配符切换；原版 GT6 要求已设筛选必须先由软锤清空才可重设。软锤清空（保留 `gt.cover.inverted`）、1×1 合成格自重置（`item_filter`/`fluid_filter`/`item_retriever_cover`，清空全部筛选与黑名单状态）及 `FluidDisplayItemLike` 不进物体筛选的守卫保持不变；撬棍拆下与破坏主机方块掉落（`CoverStackData.detached`）时，`item_filter`、`fluid_filter`、`item_retriever_cover` 保留 `gt.filter.*`/`wildcard` 与 `gt.cover.inverted`（黑/白名单状态可随物品迁移，偏离原版 `AbstractCoverDefault.getCoverItem` 不打包 `mVisuals` 导致拆下恒回白名单），其余覆盖板与物流总线的面状态（如非筛选 `gt.cover.inverted`、`gt.logistics.value`）仍统一剥离。物品栏内黑名单状态的筛选覆盖板通过 `gt6ce:cover_inverted` 模型覆写渲染为黑名单样式；同时将 `retrieveritem` 的 `normal.png`（左上角亮色，默认白名单）与 `inverted.png`（左上角黑色，黑名单）对调，对齐默认亮色物品图标与直觉。 | 简化已装覆盖板在机器/管道上的调试换料与带黑名单迁移复用，使物品栏图标与贴附态黑白名单视觉保持一致，同时保留同物品通配切换、工具优先防误覆写与破坏/撬棍掉落语义一致。 |

## 与 Stonecutter 分支的关系

- `PoweredToolItem` 接口已移入共享源码集，`ElectricToolItem` 两节点都实现它；`GTToolHelper` 的类型分支随之消失。机器采集判定（`canHarvest`/`harvestSpeed`）统一走 `ElectricWrenchHarvest` 的扳手目标表与品质/速度语义：电锯等非扳手工具不参与机器拆解，电动扳手速度按目录倍率（LV 2 / MV 3 / HV 4）乘头部材料速度。
- `IgnitableBlockHandler` 的 import 和 `hurtAndBreak` 分支是事件总线 API 差异。
- `UtilityCoverInteraction` 的 `level.registryAccess()` 分支是 provider 参数差异。

这些分支的折叠见 [Placement/node-class-split.md](../../architecture/stonecutter/Placement/node-class-split.md)、[Facade/event-bus.md](../../architecture/stonecutter/Facade/event-bus.md)、[ItemStack/registry-access.md](../../architecture/stonecutter/ItemStack/registry-access.md)；行为本身以本页为准。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
