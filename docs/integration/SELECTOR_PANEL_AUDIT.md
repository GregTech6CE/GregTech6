# 模式选择器的停止、加载与按钮时序核查

日期：2026-10-19；基线 `70d637645`。原版参考根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，Gregorius Techneticies / GregTech-6 Team，LGPL 文件只读。

本批集中固定标签、手动选择器、红石选择器、按钮面板的执行与恢复。尚未完成所有控制器优先级、显示器、传感器输出、面板几何、所有宿主及真实保存重启验收。

## ALIGN-107：固定编号选择标签错误地服从覆盖板停止状态

原版 CoverSelectorTag 的 onCoverPlaced、onCoverLoaded 立即向 ITileEntitySwitchableMode 设置 mMode；onTickPre 每个服务端 tick 再设置一次，没有检查 mStopped。CoverData.tickPre 也没有在停止时跳过所有覆盖板，而是调用各自行为，让行为自行决定是否受停止影响。

当前 PanelCoverRuntime.beforeTick 的标签分支包含 `c.supportsMode() && !stopped`，所以停止时不会强制指定模式。attached 只初始化 PanelCover.selector 的显示值；固定标签不在该枚举三种 selector 内，也没有立即 setMode 分支。标签效果延到后续 beforeTick，而且此时还受 stopped 限制。

这是标签特有的合同差异，不能通过把所有选择器的 stopped 门控删除来修：原版手动／按钮点击、红石更新和按钮倒计时本来就受 mStopped 约束。若宿主当前模式恰好已等于标签，短期显示可能看不出区别；核查的是实际模式设置入口和停止时强制语义。

## ALIGN-108：停止状态加载会消耗手动／按钮选择器的恢复机会

原版 CoverData 构造恢复已保存的 mStopped，再调用各覆盖板 onCoverLoaded。CoverSelectorManual 和 CoverSelectorButtonPanel 均不检查 mStopped，而直接以保存的 visuals 低四位恢复宿主模式。红石选择器加载时也会立即设置当前红石输入对应的模式。

当前保存仍存在：覆盖板物品携带 VALUE／STYLE／RESET／COUNTDOWN，ComponentCoverStorage 与原生宿主保存、读回物品数据，再调用 panels.loaded()；CoverDrops.loadRuntime 还恢复停止标志。问题不是“完全没有保存”。

PanelCoverRuntime.beforeTick 的选择器循环在 stopped 时 continue，跳过 `else if (restore) setMode(value & 15)`；循环结束却无条件 `restore=false`。暂停解除后，手动／按钮不会再执行恢复，而是用当前 c.mode() 更新面板 VALUE。

源码条件例子：保存的面板值为 7、重新创建的宿主模式为 0、首次 beforeTick 仍停止。当前跳过模式恢复并清除 restore；解除停止后把显示同步成 0。原版 onCoverLoaded 会先设置为 7。若宿主自身另行保存并恢复了同一个 7，则这条差异被掩盖；例如电池／转换器已有自己的状态保存，不能从此推断所有机器重启都会丢模式。

红石选择器恢复运行后会重新读信号，不依赖 restore，因此本项“恢复机会丢失”主要针对手动／按钮；其停止加载期间没有立即设置红石模式是同一恢复时机差异，不能写成红石模式永远丢失。本轮没有实际关闭服务器／重开世界。

## ALIGN-109：按钮自动复位从机器工作后提前到工作前

原版 CoverSelectorButtonPanel 在 onTickPost 处理计数：大于 1 时减一，变为 1 就将模式置 0，再同步显示。TileEntityBase06Covers.onTick 明确按 `cover tickPre → onTick2（机器工作）→ cover tickPost` 排序。

当前 PanelCoverRuntime.beforeTick 执行同样的减一／归零操作。EnergyNodeBlockEntity.tickOriginalConverter 明确先 panels.beforeTick，再 rotaryConverter.tick，最后 panels.afterTick；可选择模式的电动机等走这一调用链，所以并非只是方法名不同。

当进入一轮 tick 时倒计时为 2、模式为 7：

| 阶段 | 原版 | 当前 |
| --- | --- | --- |
| 本轮机器工作 | 使用模式 7 | beforeTick 已清零，使用模式 0 |
| 该轮结束 | 倒计时 1，模式 0 | 倒计时 1，模式 0 |

两者最终数字可能一样，但最后一轮加工／输出采用的模式不同。按下后的 10 → 1 共九次合格递减是两边共有的规则，本项不声称倒计时长度写错。实际点击发生在 tick 的哪个时点、宿主调度和游戏输出仍需运行验证。

## 已存在的对应行为

- canAttach 按 MachineControl.supportsMode 拒绝不支持模式的宿主；当前基础配方机不提供假模式支持。
- 手动／按钮安装时显示复制宿主当前模式；移除模式选择器时归零，原版 AbstractCoverAttachmentSelector 也如此。
- 手动增减回绕、四个位按钮区域和按钮网格选择均已有代码；此处只确认逻辑入口，没有做全方向点击／渲染验收。
- 按钮八种外观、螺丝刀复位模式、点击设置 COUNTDOWN=10、停止时冻结倒计时已有实现。
- 当前 VALUE／STYLE／RESET／COUNTDOWN 的拆分是现代存储表达，不因为原版用了 mValues／mVisuals 就判为错误。
- 既有 PanelCoverTests 有暂停按钮、方向坐标和普通面板测试；这些源码不是本批停止加载或实际机器末轮模式的实测证据。

## 标准化与验证

展开 PanelCover 的角色分类方法和注册路径匹配；展开 PanelCoverRuntime.beforeTick 的控制器、标签、选择器恢复和倒计时阶段，补充与原版不一致的注释。没有改动字段、条件、表达式、执行顺序或玩法。

两份共享源码分别展开 Forge／Neo，四份代码与字面量比较一致。未新增模拟测试、原生编译、GameTest 执行、完整测试、游戏、保存重启、构建或推送。来源哈希及验证边界见 [回执](verification/selector-panel-audit-20261019.json)。

后续继续覆盖板控制器多面优先级、红石显示／信号方向、状态同步与保存，以及既有全面审计队列。
