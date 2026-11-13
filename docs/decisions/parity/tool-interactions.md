# 工具交互行为

本页记录 CE 与原版 GT6 在工具交互上的行为差异。它们落在真实 `//?` 分支附近，但分支本身通常是平台 API 或类落点差异；折叠分支时不能改掉这里的行为。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| `isInteractionTool` | 保留 `isTool(stack)` 守卫：没有 `GT.ToolStats` 的堆叠不算交互工具。原版按物品基础 `toolType()` 计算合成损耗，不按口袋工具当前模式。 | 防止空电动工具或未组装工具被误判为可交互；见 `GTToolHelper.isInteractionTool`。 |
| plunger | 机器路径先抽输出罐；输出罐为空时才抽输入罐；每次最多 1000 mB。抽出的流体交给 `GarbageData` 丢弃。 | 原版 `Behavior_Plunger_Fluid` 的顺序；CE 把机器路径补进 `BasicMachineBlockEntity.useMachineTool`，见 `purgeTank`。 |
| igniter | 物品级点火已存在；`BasicMachineBlock.useRemainder` 对会消费点火器的宿主返回 `PASS`，避免 GUI 吞掉点火。 | 原版 `Behavior_FlintAndTinder` 在方块拒绝后执行；CE 的机器 GUI 不能抢先消费。 |
| 封面工具守卫 | 封面过滤器的工具判断用 `PanelCoverHost.consumesTool`，由宿主按实际工具动作决定；静态列表会漏 igniter/plunger、多 soldering_iron。 | 原版静态列表跨宿主近似；CE 改为宿主守卫，见 `UtilityCoverInteraction.use`。 |

## 与 Stonecutter 分支的关系

- `PoweredToolItem` 接口已移入共享源码集，`ElectricToolItem` 两节点都实现它；`GTToolHelper` 的类型分支随之消失。机器采集判定（`canHarvest`/`harvestSpeed`）统一走 `ElectricWrenchHarvest` 的扳手目标表与品质/速度语义：电锯等非扳手工具不参与机器拆解，电动扳手速度按目录倍率（LV 2 / MV 3 / HV 4）乘头部材料速度。
- `IgnitableBlockHandler` 的 import 和 `hurtAndBreak` 分支是事件总线 API 差异。
- `UtilityCoverInteraction` 的 `level.registryAccess()` 分支是 provider 参数差异。

这些分支的折叠见 [Placement/node-class-split.md](../../architecture/stonecutter/Placement/node-class-split.md)、[Facade/event-bus.md](../../architecture/stonecutter/Facade/event-bus.md)、[ItemStack/registry-access.md](../../architecture/stonecutter/ItemStack/registry-access.md)；行为本身以本页为准。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
