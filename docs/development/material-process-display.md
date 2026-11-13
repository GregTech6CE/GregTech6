# 矿物处理 JEI 展示接入

本文说明[计划 08-E](material-system-refactor/08-material-data-api.md#e有界矿物处理图与消费者迁移)的前端接入契约。生产路径现在从同一 `MaterialProcessSnapshot` 取得有界 core 图结果，并在 JEI 与 EMI 中共享 `Page` 和 `MaterialProcessPanel`。空状态 `unbound()` 只在没有成功发布快照时使用；具体机器行、世界生成、远程客户端和人工点击仍需按验证范围分别验收。

这是 Minecraft 实现层的客户端调用接口，随模组编译，不是独立 API JAR 的公共材料服务，也不改变 API 1.8.0。分类和面板只在已安装 JEI／EMI 的客户端路径加载；无 viewer 的专用服务端入口不得引用它们。

## 入口与职责

| 类型 | 职责 |
| --- | --- |
| `gregtech.compat.common.MaterialProcessDisplay` | 已完成有界查询的展示投影、校验和分页；不读取材料目录、配方表或服务端状态 |
| `MaterialProcessDisplay.Port` | 一个原生状态敏感的资源选择器的展示，含角色、数量单位、概率与有限图标 |
| `MaterialProcessDisplay.Step` | 同一步骤的全部共同输入、全部输出、机器／环境／时间／能量／出处说明 |
| `MaterialProcessDisplay.Connection` | 调用方提供的步骤关联，包含资源引用及确认状态；支持分支、回边和自环 |
| `MaterialProcessDisplay.Page` | 每页至多两个步骤，保留全图关联、覆盖说明和绑定到同一查询结果的跳转回调 |
| `gregtech.client.MaterialProcessPanel` | 固定 280×216 的坐标、绘制、悬停与点击；不依赖 JEI／EMI |
| `gregtech.compat.jei.MaterialProcessCategory` | 原生 JEI 槽位、索引、事件和 tooltip；分类类型为 `TYPE`，标识为 `gt6ce:material_process` |
| `gregtech.compat.emi.MaterialProcessEmiRecipe` | EMI 分类、原生索引和共享面板适配；不读取材料目录或配方表 |
| `gregtech.content.recipe.MaterialProcessSnapshot` | 绑定一次完整发布、会话和世界生成 revision；索引最终机器行与符号选择器 |
| `gregtech.content.worldgen.MaterialWorldgenFacts` | 只含活动世界生成事实，不构造原生图标 |
| `gregtech.engine.material.MaterialProcessGraph` | core 有界前向／反向查询、循环终止和显式预算截断 |
| `gregtech.compat.common.MaterialProcessProjection` | 把 core 查询转成快照内 `DataKey`、有限图标和展示 `Page` |

复用现有 `DataKey` 表达展示引用，复用 `RecipeOutputChances.CERTAIN` 的万分比契约、viewer 原生槽位、分页、配方索引与运行期注册。没有另建 SPI、全局来源缓存、锁、服务端查询协议或材料事实表。JEI 与 EMI 共用展示模型和面板，任何共用类型都不能引用具体 viewer 类。

## 构造契约

- `DataKey` 在这里是调用方签发的快照内引用，不是新的材料身份或存档 ID。`Port.resource` 必须区分材料、形态、相态和原生状态；不同 tag／unit 选择器也不能因图标相同而合并。已有 `MaterialKey`、`FormKey` 与原生匹配引用由后续适配器解析到该引用，不重新建立材料目录。
- `Port(resource, name, icons, role, main, amount, chance, details)` 的 `amount` 必须包含原生批量及单位，如件、U 或 mB。前端只显示文本，不换算、不从栈数量推导材料量。`icons` 为有限代表，空列表表示符号资源；不展开 tag 或 unit 候选全集。使用现有 `FluidDisplayBinding.display(...)` 取得带相态／状态的流体代理物品，不创建另一套流体身份转换。
- `Role.CONSUMED`、`CATALYST`、`PRESENCE` 只用于输入；`PRIMARY`、`BYPRODUCT`、`UNKNOWN` 只用于输出。输入不因为存在数量就自动消耗。无可靠主／副角色时用 `UNKNOWN`，不得按槽位猜测。
- `main` 仅决定中心路线位置，每一步的输入和输出分别至多一个。它不改变角色、确定性或权威性；未指定主路线时所有端口均侧排。
- `chance` 使用 `OptionalInt`，已知概率为 1—10000，未知为 `OptionalInt.empty()`，确定产出为 `OptionalInt.of(RecipeOutputChances.CERTAIN)`。输入概率不参与绘制。互斥／共享随机事件、未知相关性和原生状态限制必须放在 `details`；不能用边际概率声称各输出独立。世界生成权重放说明，不能当成掉落概率。
- `Step(reference, name, inputs, outputs, details, resolved)` 保存同一步的联合输入、多输出及条件。机器、时间、能量、温度、维度、选择器、来源等通过 `details` 传入。`resolved=false` 表示原生关系／约束尚未解析，连接为虚线，工艺按钮不能打开原配方。`resolved=true` 也不承诺玩家库存可执行。
- `Connection(source, target, resource, confirmed)` 必须指向当前结果内的两个步骤，且资源同时出现在源步骤输出和目标步骤输入中。前端不按相同物品图标补边；`confirmed=false` 保留为未确认关系。
- 文本与图标在构造时复制，列表只读，图标访问返回副本。调用方须将传入后取得的 `Component` 及说明视为只读。禁止传 `null`、空物品栈、错误侧角色、重复步骤引用、悬空关联或非法概率；构造／分页时抛出 `IllegalArgumentException`，不把无效数据改成空结果。

`pages(title, steps, connections, notices, navigation)` 按调用方的既定顺序分页，不执行图遍历或自动规划。当前预算为最多 64 步、128 条关联、每步每侧 64 个端口、每端口 16 个代表图标；超限直接拒绝。调用方应在查询层按预算取得结果，并提供准确截断原因，不能先全量枚举再交给 UI 截断。

`notices` 必须非空，在每页页脚可见，悬停显示全部内容。调用方必须说明完整／部分覆盖、未支持族和深度／节点／端口／边预算截断；空结果也必须说明范围。默认 `unbound()` 只表示尚未接线，不能当作“该材料没有来源”。

## 布局与交互

主加工位于中线，共同输入及催化剂位于左侧，其他产物位于右侧，副产物分支使用金色。批量、单位、角色、概率和完整条件在端口及工艺 tooltip 中读取。黄色角标和虚线表示随机、未知概率或未解析关系。

只有显式关联连接的相邻主输出／主输入才绘制跨步骤中心箭头。其余分支、跨页关联、回边与自环以每步的关联数量和完整 tooltip 引用表示，不将非相邻步骤强行连线；循环不触发递归展开。原生状态相异但图标相同的端口仍分别显示。

每侧最多六个侧排图标，更多端口显示“+N 个端口”，悬停保留全部端口信息。被折叠的物品代表仍加入 JEI 原生不可见索引，保留 R/U 检索。这是布局折叠，不是数据查询截断。符号端口以问号占位并保留说明和自定义跳转，不能将其图标代表当成完整候选集合。

- 左击工艺调用 `Navigation.openRecipe(step.reference)`，只在 `resolved=true` 时尝试。
- 左击资源调用 `Navigation.openForm(resource, false)` 查询来源，右击传 `true` 查询用途；回调返回 `false` 时不消费该点击，允许 JEI 原生行为继续处理。R/U 等快捷键仍由 JEI 管理。
- `Navigation.NONE` 是不支持跳转的明确默认实现，不需要 `null` 回调。
- 翻页、分类展示与配方检索交给 JEI；没有自建分页状态或全局 UI 实例缓存。

## 后续调用方式

在既有 JEI 注册回调中，把从同一已发布内容版本得到的展示投影传入：

```java
var pages = MaterialProcessDisplay.pages(title, steps, connections, notices, navigation);
registration.addRecipes(MaterialProcessCategory.TYPE, pages);
```

这里的 `title`、`steps`、`connections`、`notices`、`navigation` 是调用方的局部变量。生产路径由 `MaterialProcessProjection.initialPages(...)` 从当前 `MaterialProcessSnapshot` 生成这些值；`MaterialProcessDisplay.unbound()` 只用于没有成功发布快照的显式空状态，不能与正式结果同时注册。

`Navigation` 由内容适配器创建并捕获该次查询的会话／generation。每次跳转先检查当前版本，过期或不能解析时返回 `false`，不得跳转到新版本中“相似”的配方。不要捕获旧 `Level`／服务器实例，也不要从客户端直接扫描集成服务器表。生产者应将未来 `ProcessRef` 和实际材料形态引用映射到展示引用，适配器使用原生服务重新解析；UI 不发明第二个快照协议。

解析得到具体配方及其已注册分类后，可复用 JEI 的精确跳转：

```java
runtime.getRecipesGui().showRecipes(category, List.of(recipe), List.of());
```

`category` 和 `recipe` 必须来自当前有效原生引用；`RecipeViewerNavigation.open(map)` 只打开整个机器分类，不能替代这一步的精确定位。形态跳转应重新查询对应有界来源／用途，而不是按图标猜材料。

动态打开查询结果可复用 JEI `IRecipeManager.getRecipeCategory(TYPE)` 和 `IRecipesGui.showRecipes(category, pages, List.of())`；优先让结果仅随当前显示布局存活。若使用运行期 `addRecipes`，调用方必须负责重载、断线、换服以及旧记录和回调的释放；`hideRecipes` 只是隐藏，不能当作回收旧快照的手段。`MachineRecipeClient` 在断线时清除当前快照，`MaterialProcessSnapshot.Store` 只保留当前成功代和一代前代；展示页本身不缓存材料目录或配方表。

## 验证范围

检查路由为 `runtime`／语言资源 `content`；双平台编译、格式、资源打包与相关 JUnit 命令见[验证入口](validation/README.md)。[游戏检查](validation/checks/manual-ingame.md#矿物处理展示页)分别覆盖两平台及后续远程接线。

当前生产页覆盖最终机器配方与活动世界生成；tag／unit 选择器保持符号形式，没有可靠角色信息的输出保持未知。真实洗矿／合金、两次重载、远程 generation、JEI／EMI 精确跳转及两平台人工点击仍必须分别记录。测试不得为演示把假配方或额外材料来源写入生产表。
