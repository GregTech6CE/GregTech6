# 矿物处理图展示接入

本文说明[计划 08-E](material-system-refactor/08-material-data-api.md#e有界矿物处理图与消费者迁移)的前端接入契约。生产路径从同一 `MaterialProcessSnapshot` 取得一种材料全部形态的直接关系，并在 JEI 与 EMI 中共享单页可缩放、自由平移图和 `MaterialProcessPanel`。空状态 `unbound()` 只在没有成功发布快照时使用；具体机器行、世界生成、远程客户端和人工交互仍需按验证范围分别验收。

这是 Minecraft 实现层的客户端调用接口，随模组编译；方法声明来自 API 1.9.0，但本页面本身不是独立 API JAR 的公共材料服务。分类和面板只在已安装 JEI／EMI 的客户端路径加载；无 viewer 的专用服务端入口不得引用它们。

拓扑分层与资源局部副本的设计依据见[配方总览图布局设计](material-process-layout.md)。当前实现按原生依赖排布；辅料、工具、存在条件及额外副产物不作为额外图节点；`+` 只列可靠辅料名称，完整约束通过普通配方查询查看。

## 入口与职责

| 类型 | 职责 |
| --- | --- |
| `gregtech.compat.common.MaterialProcessDisplay` | 已完成有界查询的展示投影和校验；不读取材料目录、配方表或服务端状态 |
| `MaterialProcessDisplay.Port` | 一个原生状态敏感的资源选择器的展示，含角色、数量单位、概率与有限图标 |
| `MaterialProcessDisplay.Step` | 同一步骤的全部共同输入、全部输出、机器／环境／时间／能量／出处说明，以及可选的 special-process method 身份 |
| `MaterialProcessDisplay.Connection` | 调用方提供的步骤关联，包含资源引用及确认状态；支持分支、回边和自环 |
| `MaterialProcessDisplay.Page` | 一种材料的全部已查询步骤、全图关联、覆盖说明和绑定到同一查询结果的跳转回调；页面不按步骤拆页 |
| `gregtech.compat.common.MaterialProcessOverview` | 按显示身份合并重复路线与资源，保留原始步骤／端口，计算依赖层级、连续链及排线路径 |
| `gregtech.client.MaterialProcessPanel` | 固定 280×216 视口内的拓扑图、副本／回流标记、可读缩放、双向平移、同步悬停高亮、名称提示与普通来源／用途查询；不依赖 JEI／EMI |
| `gregtech.compat.jei.MaterialProcessCategory` | JEI 不可见检索索引、拖拽／滚轮事件和 tooltip；分类类型为 `TYPE`，标识为 `gt6ce:material_process` |
| `gregtech.compat.emi.MaterialProcessEmiRecipe` | EMI 索引、拖拽／滚轮事件和共享面板适配；不读取材料目录或配方表 |
| `gregtech.content.recipe.MaterialProcessSnapshot` | 绑定一次完整发布、会话和世界生成 revision；索引最终机器行与符号选择器 |
| `gregtech.content.worldgen.MaterialWorldgenFacts` | 只含活动世界生成事实，不构造原生图标 |
| `gregtech.engine.material.MaterialProcessGraph` | core 有界递归查询、材料页直连查询、循环终止和显式预算截断 |
| `gregtech.compat.common.MaterialProcessProjection` | 把 core 查询转成快照内 `DataKey`、有限图标和展示 `Page` |

复用现有 `DataKey` 表达展示引用，复用 `RecipeOutputChances.CERTAIN` 的万分比契约、viewer 检索索引、配方索引与运行期注册。没有另建 SPI、全局来源缓存、锁、服务端查询协议或材料事实表。JEI 与 EMI 共用展示模型和面板，任何共用类型都不能引用具体 viewer 类。

## 构造契约

- `DataKey` 在这里是调用方签发的快照内引用，不是新的材料身份或存档 ID。`Port.resource` 必须区分材料、形态、相态和原生状态；不同 tag／unit 选择器的原生引用保持独立。`Port.identity` 另作展示共享键：完整原生状态或规范选择器描述不包含查询代号及配方专属 selector 后缀；有原生图标时按实际显示代表的完整物品／流体状态识别相关展示实例；多候选 selector 可以与该代表同步高亮，并在总览共享一个资源节点，但原始候选集、消耗量和 guard 仍属于各原生步骤，不声称 selector 与具体物品可互换。不同 NBT／数据组件状态不得仅因图标相同而合并。已有 `MaterialKey`、`FormKey` 与原生匹配引用由后续适配器解析到该引用，不重新建立材料目录。
- `Port` 的 `amount` 必须包含原生批量及单位，如件、U 或 mB。`quantity` 是生产者同时提供的紧凑图标角标（件数、省略为 1 的件数、U、mB 或未知标记），前端不解析 `amount` 文本、不换算、不从图标栈数量推导材料量。旧构造方法默认不显示数量角标。紧凑总览不在名称提示中显示数量与概率，完整值保留在原始步骤并通过 viewer 普通配方查询查看，避免共享资源节点承担多个不同配方的批量。`icons` 为有限代表，空列表表示符号资源；不展开 tag 或 unit 候选全集。使用现有 `FluidDisplayBinding.display(...)` 取得带相态／状态的流体代理物品，不创建另一套流体身份转换。
- `Role.CONSUMED`、`CATALYST`、`PRESENCE` 只用于输入；`PRIMARY`、`BYPRODUCT`、`UNKNOWN` 只用于输出。输入不因为存在数量就自动消耗。无可靠主／副角色时用 `UNKNOWN`，不得按槽位猜测。
- `main` 是生产者提供的路线提示，每一步的输入和输出分别至多一个。总览显示本材料在该侧的全部消耗输入或产物；该侧没有本材料时，用 `main` 或稳定端口顺序选一个外部主输入／产物。工具、存在条件及其余辅料／额外副产物留在 `+` 中。这不改变原始角色，也不声称一个可见输入就是完整配方。
- `chance` 使用 `OptionalInt`，已知概率为 1—10000，未知为 `OptionalInt.empty()`，确定产出为 `OptionalInt.of(RecipeOutputChances.CERTAIN)`。输入概率不参与绘制。互斥／共享随机事件、未知相关性和原生状态限制必须放在 `details`；不能用边际概率声称各输出独立。世界生成权重放说明，不能当成掉落概率。
- `Step` 保存同一步的联合输入、多输出及条件。`icon` 是生产者提供的代表机器图标，访问时返回副本；没有可靠机器代表时使用空图标，面板显示问号。`worldGenerated` 由快照的真实世界生成引用设置，不根据空输入、图标或引用字符串猜测；兼容构造方法默认为 `false`。活动世界生成步骤使用镐作为采集符号。普通机器工序复用 EMI 分类的 `RecipeMachines` 索引与 `RecipeViewerIcons.category(...)`，包括锤子等工具类图标；不再只遍历基础机器。机器、时间、能量、温度、维度、选择器、来源等通过 `details` 传入。`resolved=false` 表示原生关系／约束尚未解析，连接为虚线；显示图标仍可执行普通来源／用途查询。`resolved=true` 也不承诺玩家库存可执行。
- `Connection(source, target, resource, confirmed)` 必须指向当前结果内的两个步骤，且资源同时出现在源步骤输出和目标步骤输入中。前端不按相同物品图标补边；`confirmed=false` 保留为未确认关系。
- 文本与图标在构造时复制，列表只读，图标访问返回副本。调用方须将传入后取得的 `Component` 及说明视为只读。禁止传 `null`、空物品栈、错误侧角色、重复步骤引用、悬空关联或非法概率；构造／校验时抛出 `IllegalArgumentException`，不把无效数据改成空结果。

`pages(title, material, steps, connections, notices, navigation)` 生成一张材料页，不执行图遍历、自动规划或步骤分页。当前展示上限为 512 步、4096 条关联、每步每侧 128 个端口、每端口 16 个代表图标；超限直接拒绝。调用方应在查询层按预算取得结果，并提供准确截断原因，不能先全量枚举再交给 UI 截断。

`notices` 必须非空，通过页脚的 `i` 提示入口悬停显示全部内容。调用方必须说明完整／部分覆盖、未支持族和节点／端口／边预算截断；空结果也必须说明范围。默认 `unbound()` 只表示尚未接线，不能当作“该材料没有来源”。

## 布局与交互

生产查询使用 `MaterialProcessGraph.queryMaterial(...)`：先展开根材料的 ore、raw、crushed、dust、fluid 等全部已发布形态，再收集与任一形态直接相邻的步骤，不沿另一材料的产物继续递归。因此同一材料页同时包含：

- “来源”：实际消耗非本材料、产出本材料且不消耗本材料的配方，以及明确来自世界生成的本材料。
- “内部转换”：实际消耗并产出本材料形态的步骤；洗矿等含辅料和副产物的转换仍归这里。
- “去向”：实际消耗本材料、产出其他材料且不再产出本材料的配方。

分类只使用 `CONSUMED` 输入。仅作为催化剂或存在条件出现的本材料不能建立加工流向；无输入但没有世界生成标记的步骤不能据此声称是来源。没有满足上述条件的步骤不纳入这三个材料转换分类，不能把“不是来源”直接当成去向。

分类保留为材料流向的语义说明，不决定节点准入或位置，也不绘制分割线。未分类、纯符号、无输入或无输出的合法步骤仍保留原始引用。图按**机器图标的完整物品状态＋可见输入身份集合＋可见输出身份集合**合并外观相同的路线；数量、条件、辅料、特殊工艺 method 或不同原生引用不再强制复制一条外观相同的路线。无图标工序没有可靠外观身份，仍按引用区分。

合并只作用于总览：`Node.steps` 保留全部原生步骤；每条可见边的 `endpoints()` 保留全部 `Endpoint(step, output, index, port)`。原生配方、选择器约束、数量、概率与 viewer 检索索引不合并或删除。此图不用于推断配方执行等价性。

同一 `Port.identity` 在整图优先共享一个资源节点，即使来自不同 selector 或快照资源引用；不同完整 NBT／组件状态仍分开。点击只查询当前显示物品／流体的普通来源或用途，不再把一个节点绑定到任意一条原生配方。`Connection` 仍原样保留，不因展示共享新增逻辑关联。

依赖图按稳定引用遍历，解除反馈边的前向层级约束后计算 DAG 层级；纯环与自环保留方向和标记。相邻层、低连接度且连续较长的路线优先对齐。等长分支按资源家族集中排序，再按邻近节点重心分配行；不因连接度大于阈值就拆散公共物品。非主链边跨越超过三层、纵向距离超过 192 单位、穿过中间节点或属于反馈边时，将该端口改为机器附近的局部资源副本，再重新排布。每个可见端口最多拆出一个副本，连续主链和邻近分支仍共享资源。

常规悬停只显示实际物品／机器名称；无原生图标时使用符号名称。机器右上角 `+` 的独立悬停区域显示合并配方数和去重后的辅料名称，最多列出十二项；完整配方信息通过 viewer 普通查询查看。局部副本与回流仍有 `↗`／`↩` 标记，相关资源与边同步高亮。

所有可见连接统一使用单段直线，分支直接斜向相连，不再混用正交折线。输入侧只连线，输出侧保留箭头，连续链为 `材料—机器→产出`。回流使用带 `↩` 标记的局部副本，不在画布顶部预留回流通道；副本之间不额外连线。线宽、虚线间距及箭头与节点使用同一画布变换，随缩放同比变化。布局在首次打开页面创建面板时完成；EMI 注册索引时不提前排布全部材料，缩放、平移和悬停不重新排图。

`MaterialProcessProjection` 从同一快照的原生配方复制物品栈和流体状态。含指纹的状态字符串不是序列化数据，不能去掉指纹后新建无数据物品代替；无法恢复时保留无图标符号。EMI 的流体索引、查询和悬停提供器通过 `MaterialProcessEmiStacks` 将代理显示物品还原为真实流体键和数据，而非索引代理物品本身。物品则保留完整 NBT／组件。悬停及点击使用当前实际显示的代表，而原生索引保留全部有限代表；有可用真实图标时优先使用，不让另一条无图标引用遮蔽它。同显示身份可以有多个带标记的局部实例；实例复制不改变原始 selector 或原生索引。

拖拽空白区域向任意方向平移，滚轮围绕鼠标缩放，顶部 `−／+` 围绕视口中心缩放，“复位”采用 `max(0.75, min(1, 整图适配比例))`；超出视口时定位本材料的代表链，小图适配视口。滚轮仍可主动缩小查看全图。裁剪边界先通过 viewer 当前渲染矩阵转换为绝对 GUI 坐标；悬停、点击和 EMI 原生 ingredient provider 共同使用实际渲染矩阵及画布逆变换。页脚 `i` 提供覆盖及截断说明。

- 机器和资源均使用普通来源／用途查询：左击查来源，右击查用途。机器调用 `Navigation.openStack(icon, uses)`，资源通过 `openPort` 的默认方法将当前代表传给 `openStack`，不再精确跳转某条配方。
- 左／右击资源通过 `Navigation.openPort(port, uses)` 打开 viewer 原生配方／用途查询；EMI 的悬停 ingredient provider 同样提供真实物品或流体句柄，以支持原生快捷键。`openForm` 保留为显式打开另一材料总览的接口，不再作为生产页资源点击时重新打开总览的路径。
- `Navigation.NONE` 明确表示不支持跳转。具体回调先核对同一快照是否仍有效；无法解析时返回 `false`。
- 一个材料仍只注册一个 `Page`，局部副本只影响展示，没有删除原配方或另建执行模型。

## 后续调用方式

在既有 JEI 注册回调中，把从同一已发布内容版本得到的展示投影传入：

```java
var pages = MaterialProcessDisplay.pages(title, material, steps, connections, notices, navigation);
registration.addRecipes(MaterialProcessCategory.TYPE, pages);
```

这里的 `title`、`material`、`steps`、`connections`、`notices`、`navigation` 是调用方的局部变量。生产路径由 `MaterialProcessProjection.initialPages(...)` 从当前 `MaterialProcessSnapshot` 生成这些值；`MaterialProcessDisplay.unbound()` 只用于没有成功发布快照的显式空状态，不能与正式结果同时注册。

`Navigation` 由内容适配器创建并捕获该次查询的会话／generation。每次跳转先检查当前版本，过期或不能解析时返回 `false`，不得跳转到新版本中“相似”的配方。不要捕获旧 `Level`／服务器实例，也不要从客户端直接扫描集成服务器表。生产者应将未来 `ProcessRef` 和实际材料形态引用映射到展示引用，适配器使用原生服务重新解析；UI 不发明第二个快照协议。

`openStack` 必须通过 viewer 原生的物品／流体 focus 查询来源或用途。EMI 使用 `EmiApi.displayRecipes`／`displayUses`；JEI 使用 OUTPUT／INPUT focus。机器图标也加入 EMI ingredient provider，R/U 与鼠标采用同一个普通查询目标。普通查询保留完整原生状态，不解析某个 `Step.reference` 来选择精确配方。

动态打开查询结果可复用 JEI `IRecipeManager.getRecipeCategory(TYPE)` 和 `IRecipesGui.showRecipes(category, pages, List.of())`；优先让结果仅随当前显示布局存活。若使用运行期 `addRecipes`，调用方必须负责重载、断线、换服以及旧记录和回调的释放；`hideRecipes` 只是隐藏，不能当作回收旧快照的手段。`MachineRecipeClient` 在断线时清除当前快照，`MaterialProcessSnapshot.Store` 只保留当前成功代和一代前代；展示页本身不缓存材料目录或配方表。

## 验证范围

检查路由为 `runtime`／语言资源 `content`；双平台编译、格式、资源打包与相关 JUnit 命令见[验证入口](validation/README.md)。[游戏检查](validation/checks/manual-ingame.md#矿物处理展示页)分别覆盖两平台及后续远程接线。

当前生产页覆盖最终机器配方与活动世界生成；tag／unit 选择器保持符号形式，没有可靠角色信息的输出保持未知。真实洗矿／合金、两次重载、远程 generation、JEI／EMI 普通来源／用途查询及两平台人工点击仍必须分别记录。测试不得为演示把假配方或额外材料来源写入生产表。

## 特殊工艺所有权

API 用 `SpecialProcessMethod` 绑定现有 typed process key、配方表和展示键。可处理材料注册 `SpecialProcessValue.Target`，辅料材料注册同一 key 下的 `SpecialProcessValue.Reagent`；宿主按 target 的表单计数、产物、概率、辅料量和反应参数生成配方，并把 method ID 写入最终 `Recipe`。已有手工 Bath 行也必须通过同一 target／reagent 规则匹配并获得 method ID，不能仅凭步骤名折叠。

每个 Target／Reagent 规则还会派生对应材料标签。`MaterialTagPack` 根据规则表单生成物品／流体标签，供过滤器和自动化脚本使用；分类标签和配方 method ID 共用声明，不维护第二份手写名单。
