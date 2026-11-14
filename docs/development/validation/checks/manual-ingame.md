# manual-ingame：游戏内人工验收

## 验证方式

游戏内验收覆盖自动检查无法证明的行为：世界状态、机器 tick、交互权限、渲染、Mixin 注入、网络同步、可选集成和存档迁移。它不替代 JUnit、GameTest 或编译，而是在这些检查之外确认玩家实际看到和操作到的行为。

修改世界、方块实体、机器 tick、交互、渲染、Mixin、网络、配置或可选集成，或 package 文档的验收章节要求游戏内确认时执行。纯文档改动不启动游戏；只运行一个平台不能推出另一个平台行为相同。

只选择与改动相关的场景，至少考虑：

- 无输入、资源不足、输出阻塞、能量或流体不足。
- 方向变化、拆装、区块卸载与重载、存档保存与读取。
- 客户端与专用服务端分别启动；专用服务端不加载客户端类型。
- 可选集成：未安装目标模组与安装匹配版本两组启动。
- 资源包重载、GUI 缩放、JEI/EMI 显示与快捷键。

具体到机器、配方、流体、能源、网络与可选集成的勾选项写在对应 package 文档的验收章节里。

## 验证原理

### 矿物处理展示页

接口与测试输入契约见[展示接入](../../material-process-display.md)。生产页已连接 `MaterialProcessSnapshot`，JEI 与 EMI 复用同一图和面板；没有成功快照时才显示未接入说明。

1. 在 JEI／EMI 中打开真实材料的“矿物处理”。页面应直接显示依赖图，没有分类分栏。连续链水平向右，替代路线和分叉就近分行；未分类步骤、无外部入口环及自环不能消失。不同 GUI 缩放下右侧、底部裁剪正确。
2. 切割机三种润滑剂相同主物品／机器图标／产物时只显示一条路线，全部原生步骤仍保留，辅料不生成图节点，通过右上角 `+` 和 tooltip 查看。物品和机器均左击普通来源、右击普通用途，不精确锁定原配方；常规 tooltip 只显示名称；`+` 区域只列合并配方数与辅料名。数量、概率、时间、能量和 native guard 仍保存在原生配方。锤子加工应使用 EMI 同分类已有图标，其他工具／多方块类别也应一致。
3. 资源局部副本带 `↗`，回流带 `↩`，悬停同步突出相关实例和箭头。相同显示代表跨 selector 共享节点，不同 NBT／组件状态分开；同图标不能补造跨步骤关联。连接均为单段直线，分支斜向相连，输入无箭头、输出有箭头；遮挡图标的长边和回流改用局部副本，没有顶部回流通道。确认关系不因循环改为虚线。
4. 对带 NBT／组件物品与真实流体，逐个副本检查左／右击、R/U、tooltip 和 EMI ingredient provider；流体代理应查询实际流体。各节点使用实际显示的完整物品／流体状态查询，无法恢复时保留符号，不用无数据假栈。
5. 来源／内部转换／去向仅在工序 tooltip 说明；“本材料＋辅料 → 本材料其他形态＋副产物”仍属内部转换。工具、存在条件及额外辅料／副产物均不扩展生产路线。主材料的多输入、多输出分支仍保留。
6. 大图初始和复位保持至少 75% 可读比例并定位代表链；小图适配视口。滚轮围绕鼠标缩放，顶部 `−／+` 缩放；空白处左右、上下拖动有效。在 100%、25% 和约 8% 比例下确认线宽、虚线间距及箭头与图标同比缩小，不出现盖住整图的固定尺寸箭头。缩放和平移后，图标、`+`、tooltip、点击及 EMI 悬停命中一致。
7. 连续重载两次后确认配方增删同步、旧引用拒绝跳转；断线／换服不沿用回调。布局副本数不改变配方数量或原生 viewer 索引；方法标签只来自 API target／reagent 声明。
8. 两平台分别检查 JEI-only、EMI-only 和双装环境，双装沿既有去重规则。另验无 viewer 的专用服务端与独立远程客户端；未实际执行的场景不能用编译或集成服务器静态表代替。

`MaterialProcessSnapshotTests` 中的 `presentationPreservesNativeItemState`、`overviewGroupsVisibleRoutesAndHidesAuxiliaries`、`overviewAlignsChainsAndPreservesFeedback`、`overviewSharesHubsWithoutInventingConnections` 和 `overviewKeepsDenseEdgesLocal` 覆盖原生状态、锤子分类图标、辅料隐藏、原始步骤保留、外观去重、公共资源集中、连续链、顺序稳定、循环、副本、密集图直线避让和关联保留。编译这些测试不代表已运行 GameTest；箭头外观、真实 EMI 查询和鼠标手感仍需人工确认。

## 注意事项

- 使用隔离实例；EULA 由操作者自行处理；完整日志、截图与实例配置放 `work/<任务名>/`，公共记录只保留脱敏结论。
- 记录提交或源码状态、JDK、Minecraft 与加载器版本、GT6CE 构建、依赖模组及版本、配置差异。
- 每条场景写清前置条件与操作、预期、实际、判定和平台；判定使用“未验证 / 部分验证 / 验证通过 / 已知失败”。
- 结束时分别列出“执行且通过”“执行但失败或阻塞”“尚未执行”，不要由一个平台启动成功推出两个平台行为相同。
- 不用人工观察替代本可自动化的规则；能在 JUnit 或 GameTest 中覆盖的行为优先补自动测试。
- 不把编译成功、客户端画面正常或单个平台启动写成完整验收。
- 不通过关闭 Mixin、放宽权限或跳过可选集成启动来获得“通过”。

## 运行命令

运行客户端与专用服务端：

```bash
./gradlew :minecraft:1.20.1-forge:runClient
./gradlew :minecraft:1.21.1-neoforge:runClient
./gradlew :minecraft:1.20.1-forge:runServer -PserverDirectory=../../../work/server-forge
./gradlew :minecraft:1.21.1-neoforge:runServer -PserverDirectory=../../../work/server-neoforge
```

Forge 默认实例在节点内 `run/`，NeoForge 客户端/服务端在 `run-client/`、`run-server/`。都属于本机数据，不提交存档或配置。

`-PserverDirectory` 由 Gradle 按节点项目目录解析；要放到仓库根 `work/` 时使用上面的 `../../../work/...`。

可选集成开关：`-PkubejsRuntime=true`、`-PcompatRuntime=ie|mek|ae|pr|hc`（每次选择一个值）、`-PemiSmoke=true`、`-PemiOnlySmoke=true`。具体依赖见 [Forge 配置](../../../../gradle/platform-forge.gradle)与 [NeoForge 配置](../../../../gradle/platform-neoforge.gradle)；加载依赖不代表集成已通过验证。

`-PdirectCoreClasspath=true`、`-PdirectCoreResources=true` 是本地运行优化；常规打包验收保持默认配置。

修改 GUI 模板、别名、图标引用或 PNG 时，先做静态预览：

```bash
python tools/gui/preview.py
python tools/gui/preview.py --output work/gui-preview
```

依赖、参数、输出与预览边界见[工具说明](../../../../tools/gui/README.md)。
