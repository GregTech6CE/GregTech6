# 起火器、打火机和灭火工具核查

第四十二批，2026-10-20，恢复基线 `ba6e74c0f`。已先快进同步远端十个提交；本批归档暂停前的五组证据，并按当前源码重新确认。原版来源为 Gregorius Techneticies／GregTech-6 Team 的 LGPL-3.0-or-later 源码，不改动原目录或许可声明。

## ALIGN-127：易燃起火器少了一次普通概率判定

原版 `Behavior_FlintAndTinder:49–54` 在易燃／燃烧材料的增强判定失败后，仍会进入普通概率分支。默认基础值 30、增强阈值 65，按独立等权随机抽样计算，实际成功机会为 `65% + 35% × 30% = 75.5%`；原版 tooltip 只显示增强阈值 65，不能用 tooltip 数字反证执行概率。

当前 `BehaviorFlintAndTinder.useOn` 将 `ignitionChance` 的 65 作为唯一一次抽样阈值。普通材料默认 30、耐久不超过一的材料 100 两种情况已有对应。材料 `BURNING` 的缺口继续归既有 ALIGN-006，不重复编号。概率是源码计算，不是游戏或实际伪随机序列测量。

## ALIGN-128：一次性点火物品没有创造模式消耗豁免

原版 `Behavior_Lighter:86–99` 无论失败判定还是成功处理目标，只有非无限物品玩家才调用 `useUp`。当前 `BehaviorLighter.useOn` 的 `singleUse` 分支直接调用 `Consumable.usedUp`，后者在没有空容器时复制并减少一个物品；两版物品入口直接将结果写回手中，没有补回这一件。

影响火柴、干草／树皮起火物等一次性形态。多次使用的打火机已经在 `consumeFuel` 中处理创造模式，不将此项写成所有点火物品都扣错燃料。

## ALIGN-129：苦力怕点火没有走原版左键拦截

原版两种点火行为实现 `onLeftClickEntity`，服务端点火并返回 true。当前多物品通过 `interactLivingEntity` 右键路径进入 `ItemBehaviors.useOnEntity`；GT 工具起火器则从 `GTToolItem.hurtEnemy` 进入，发生在普通攻击命中流程中，不能等同于攻击前拦截。

当前辅助点火函数本身没有完整替代原版的服务端回调门槛。已存在的直接辅助函数 GameTest 只能证明函数会点燃苦力怕，不能证明攻击取消、伤害、客户端同步均对齐。本批未启动游戏，不将可能额外伤害说成已实测。

## ALIGN-130：燃烧室没有完整接入点火／灭火工具分派

原版 Solid、Liquid、FluidBed 三个生成器均在 `onToolClick2` 识别 igniter／extinguisher，并限制正面或无玩家自动工具；液体燃烧室还更新余火计时。

当前两种共享燃烧室实体在 Forge 投影中未实现 `ItemBehaviors.Ignitable`；NeoForge 投影有 `ManualToolBehaviorAccess.Ignitable` 适配。两版燃烧室均未实现灭火工具使用的 `Extinguishable`。方块已有原版打火石交互、`PoweredToolTarget` 或其他停止入口，不能代替随机点火物品／CO₂ 喷罐所用的接口。

因此结论是具体工具入口不完整，不是燃烧室整体不能点燃／停止。雷管已有点火及灭火接口，是本调用链的正例。

## ALIGN-131：原生工具返回零后错误进入世界点火回退

原版 `IBlockToolable.Util:77–79` 根据目标是否实现工具接口，在原生与 ToolCompat 两条路径中二选一；原生返回零不会追加兼容点火。

当前两版 `BehaviorFlintAndTinder.ignite` 只在成本大于零时结束，否则继续尝试 TNT 和相邻空气点火。已经燃烧的 GT 雷管 `DynamiteBlockEntity.onIgnite` 返回零，之后相邻空气仍可能被点燃；原版对应的 `MultiTileEntityDynamite` 不会转入 ToolCompat。对一次性打火物，这还可能把原版应拒绝且不消耗的操作变为消耗。此为已确认调用链差异，未在世界中复现该案例。

## 可读性、验证和边界

展开 NeoForge 的 `MultiBehaviorItem` 导入／类缩进／参数间距，以及灭火接口的压行声明。仅修改空白，Java 代码、注释和字面量保持；没有修改上述五项玩法。

原版来源哈希、当前路径哈希和两份源码的词法比较见[回执](verification/ignition-extinguisher-audit-20261020.json)。本批只做源码核对及词法等价检查，不启动游戏、完整测试或构建。继续声音、配置、喷罐生命周期与其余工具；全面盘点仍未完成。
