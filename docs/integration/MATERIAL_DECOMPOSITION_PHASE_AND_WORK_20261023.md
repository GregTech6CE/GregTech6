# 通用分解相态、输入与工时合同

对应[分解合同核查](MATERIAL_DECOMPOSITION_AUDIT.md) ALIGN-190–193。准入与地图选择由 `MaterialProperty.DECOMPOSABLE` / `CENTRIFUGE` / `ELECTROLYSER` 和 `MaterialDecompositionData` 负责，本次不改变这部分。

## 补齐的合同

1. **输入形态**：材料有可用流体绑定时优先生成流体输入配方，并抑制该地图的固体入口；否则生成 `divider` 个粉尘，`divider % 9 == 0` 时再生成 `divider/9` 个块粉。`blockDust` 在端口是方块形态，通过 `GTBlocks` 取方块物品。
2. **输出相态**：组件熔点不高于 `CS.DEF_ENV_TEMP`（293K）时走该材料的已注册流体，覆盖气、液、具名和熔融绑定；其余走 `mTargetPulver` 的粉尘。流体重解析使用 `GeneratedRecipeResolver.materialFluid`，包括 vanilla water 和 144 mB/U 熔融比例。
3. **工作参数**：`EU/t = max(16, total * 14 / U)`，`duration = ceil(total * 292 / U)`；`total` 是未除分母的组分总量。配方公因子归一会继续作用于输入、输出和时长。
4. **组分失败策略**：单个组件无法解析时丢弃该组件，其余输出照常提交。

## 保留的现代差异

- 每种材料只有一个可用流体绑定解析入口，因此原版“液体输入 + 气体输入”两个变体合并为一个可用形态。
- 电解槽继续带 selector 0，并通过 `withCatalystInputs(0)` 保留；`MachineRecipeMapDefinitions.Electrolyzer` 的最小输入保持放宽。
- `blockDust` 使用端口方块形态，不重建 GT6 的前缀物品。

## 样例

| 材料 | 现在的结果 |
| --- | --- |
| Alumina / Rutile / Hematite | 仅 `DECOMPOSABLE`，无通用机器配方 |
| Bronze | 离心机，576 mB 熔融输入，56 EU/t、1168 ticks；不再生成粉尘入口 |
| Lazurite | 电解槽，392 EU/t、归一后 4088 ticks |
| Saltwater | 电解槽，4000 mB 液体输入，输出 3000 mB 水与盐粉 |
| Nitrogen Monoxide | 电解槽，2000 mB 气体输入，输出氮气和氧气 |
| Cobalt hexahydrate | 离心机，水输出经 vanilla water 解析，不再整条跳过 |

## 验证状态

- 构建与合约结果见[验证回执](verification/material-decomposition-phase-and-work-20261023.json)。
- `MaterialDecompositionPolicyTests` 增加流体输入、气体输入、vanilla water、Bronze 熔融入口和工时参数断言。
- 回执只覆盖编译、共享行为合约和两端构建；GameTest 若未实际运行会在回执中单列。
