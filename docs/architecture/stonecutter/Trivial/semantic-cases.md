# 语义案例

## 子原因

以下案例看起来像同义分支，实际承载行为，不能按代码相似度折叠。

## 例子

### `craftKind`

按物品基础 `toolType()` 计算合成损耗，不按口袋工具当前模式；`isTool` 守卫排除无 `GT.ToolStats` 的堆叠。迁移时保留这个语义。

### `plunger`

原版从输出罐、输入罐依次抽 1000 mB；CE 补进 `useMachineTool`，用 `GarbageData` 丢弃。

### `igniter`

物品级行为已存在，但 `useRemainder` 的 GUI 会吞掉；对宿主会消费的点火器返回 `PASS`。

### 封面工具守卫

静态列表跨宿主近似，漏 `igniter`/`plunger`、多 `soldering_iron`；改为 `PanelCoverHost.consumesTool`，由宿主按实际工具动作覆盖。

### 为什么这么修复

这些分支承载的是行为差异，不是版本差异。折叠前先确认原版消费路径与 CE 的补充逻辑。

### 其他可能的修复方式

- 按代码相似度直接折叠。会丢失 `craftKind` 的守卫、`plunger` 的抽取顺序或 `igniter` 的 `PASS` 行为。
- 保留两版各写一份。维护面大，且容易漂移。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
