# 材料重构 06：统一分解验收

本页记录[计划 06](../development/material-system-refactor/06-decomposition.md)的落地边界、行为差异和验收证据。正确性依据优先使用仓库内 parity 决策，其次为 GT6 固定提交 `3703e40308c8c030763fd6297dea8b210d2a77b1`；旧运行快照只用于发现变化。

## 当前实现

- `MaterialDecompositionRule` 是唯一公开的分解值对象，`MaterialProcessKeys.DECOMPOSITION` 将它挂入现有 `MaterialInfoDefinition.Processes`。规则保存离心／电解路线、液体／气体原生身份、`sourceUnits → mB` 换算和固体兜底；API 版本为 1.8，版本码 `1008`。
- `DecompositionMaterials` 保留固定提交中的材料相态声明。`MaterialDefinitions` 在建立规范声明时把该值关联到材料；最终权限只读取 typed rule，`CENTRIFUGE`、`ELECTROLYSER`、`DECOMPOSABLE` 仅作为其兼容镜像。旧 feature 只在没有 typed rule 时生成默认兼容规则，不能再拓宽显式规则。
- `MaterialDecompositionPlanner` 是唯一计划入口。它处理 divider、输入批量、整数约简与范围、液体／气体输入、环境相态输出、按声明的一跳粉化目标、EUt、时长、电解选择器、输出顺序及槽位截尾。
- `DecompositionRecipeLoader` 只负责把计划解析为标准物品／流体，并发布 `Centrifuge`、`Electrolyzer` 配方。其行为 overlay-owned rows，已接入 baseline 排除、逐代重建和失败恢复。`SourceDecompositionData` 和 `SourceDecompositionNativeFacts` 已退出生产路径。

## 数据裁决

- 规范目录当前发布 263 个分解上下文：250 个材料至少有一条离心／电解路线，13 个只为组成组件提供相态；69 个材料带显式流体相态绑定。
- 补齐 22 个已有分解路线但缺少组成的材料，声明取自固定 MT 数据。`Ironwood`、`SkyStone`、`VibrantAlloy` 的原组成引用已停注材料，因此保留为明确无分解能力，不创建伪造替代物。
- 当前发布结果为 243 个材料、258 条离心／电解配方、21 个被拒绝或跳过的路线候选。相比此前快照的 229 个材料、243 条配方，增加来自补齐组成和启用已声明相态输入；减少来自无法表示完整产物或输入绑定的候选。
- 启动日志逐项报告未解析的输入或输出绑定。当前 9 个候选包含 `EnderPearl`、`FierySteel`、`Fireleaf`、`FlamascusSteel`、`Glowstone`（气体输入）、`Knightmetal`、`LiveRoot`、`Steeleaf`、`Thaumium`；失败候选不会发布截断或伪有效配方。可替代的其他液体／气体输入和固体 fallback 仍可独立发布。

## 独立验证

### 执行且通过

- API 与 core JUnit：API 108 个、core 159 个；新增测试覆盖 typed route 优先于旧 flag、共享流体单位、Powellite、Datolite、Glowstone、槽位截尾、整数溢出和禁止材料。
- Forge 1.20.1：全新目录 `work/material-system-refactor/fix06-forge`，`All 78 required tests passed`。测试通过共享 `RecipeProcessor` 实际启动、消耗、完成并刷新附属离心分解输出；真实资源重载期间分解 owned rows 保持 258 条，电解配方在两次重建后仍可查询。
- NeoForge 1.21.1：全新目录 `work/material-system-refactor/fix06-neo`，`All 83 required tests passed`，包含同样的分解重载断言。
- API 契约通过；两节点生产与测试源码编译通过，`compileBothPlatforms` 成功。

### 尚未执行

- 未在客户端界面手工检查 JEI／EMI 中的离心、电解配方布局和电解选择器提示。

