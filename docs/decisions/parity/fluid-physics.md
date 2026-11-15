# 流体物理参数

本页记录 GT 流体运动与坠落参数的移植决策。原版 GT6（1.7.10）没有对应 API，流体推动与坠落行为由方块材料决定。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 坠落伤害 | GT 液体（非气体/等离子）`fallDistanceModifier` 为 0，落入即清零坠落距离；原版仅水材料流体如此，高温流体（>500K）用 `Material.lava`，仍计坠落伤害。气体/等离子两侧统一覆盖为 1.0。 | 原版 `BlockBaseFluid` 按相态/温度选材料：气体 `MaterialGas`、>500K `Material.lava`、其余 `Material.water`（`Material.water` 清零坠落距离）。CE 对液体统一清零，高温液体是已知简化。 |
| 运动缩放 | GT 流体 `motionScale` 为 0.007，是原版水默认 0.014 的一半；气体/等离子覆盖为 0。 | 原版无对应值，采用 1.20 移植值并统一到两节点；粘度/密度已由 `FluidPhysicalProperties` 按原版移植。 |

## 决策背景

- 两个加载器的 `FluidType.Properties` 默认值均为 `motionScale 0.014` / `fallDistanceModifier 0.5`。统一前 1.20 显式设置 0.007/0.0，1.21 使用默认值，后者偏离原版的水材料语义。
- 统一取 `fallDistanceModifier(0.0F)` / `motionScale(0.007D)`：坠落行为朝原版对齐；`motionScale` 无原版约束，保留移植值。
- `nonLiquid` 覆盖（气体/等离子：`motionScale 0`、`fallDistanceModifier 1.0`、不推实体、不可灭火、不可游泳/溺水）两节点共享，不受本决策影响。
- 高温液体的坠落伤害简化如后续需要按原版区分，应在 `createFluidType` 按 `FluidPhysicalProperties` 的温度判定分支，而不是恢复加载器默认值。

相关链接：[跨版本统一方针](../../architecture/stonecutter/unification-policy.md)