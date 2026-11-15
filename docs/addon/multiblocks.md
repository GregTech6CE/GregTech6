# 多方块声明与端口

本页用于绑定 L1 主控、编写结构约束和端口规则，并核对运行时输出、结构回调与预览行为。

返回[附属编写指南](README.md)。

## 显式绑定

在 `AddonMachineContributor.addonMachines()` 中提交 `controller.withStructure(layout)`。配方机、行为机和燃料发电机使用同一个 `MultiblockMachine` 声明；配方主控仍需指定 `largeRecipe()`，燃料主控仍需指定 `multi()`。结构的名称可以独立于机器类型，多个主控可复用同一份 `StructureLayout`。字符层图、固定单元与重复层继续使用既有布局 API。

这里的独立名称标识可复用模板，不是另一套运行时注册键。宿主为每个绑定生成以主控 `machineType` 命名的布局副本，后续注册和布局查找使用该键；原模板对象及其名称不会改变，也不会额外注册模板名称别名。例如，同一个 `shared_tower` 模板绑定两种主控后，会按各自的 `machineType` 注册。

```java
return List.of(recipeController.withStructure(sharedLayout),
               behaviorController.withStructure(sharedLayout));
```

此片段中的主控和布局需由调用方先构建。旧 `structureLayouts()` 从 API 1.3.0 起标记 `@Deprecated(forRemoval = false)`，仍是受支持的兼容入口，宿主会读取旧列表并将其与显式声明归一化；旧列表的布局名称必须等于主控 `machineType`。同一主控类型绑定不同结构、缺少必需结构、重复布局均会报错。

KubeJS 的 `addLayout()` 和对应 `structureLayouts()` 桥接同样保留并标记弃用，继续支持现有脚本。新脚本使用 `event.addMachine(controller.withStructure(layout))`，不再单独提交布局列表。

## 世界匹配约束

世界中的额外约束写入同一份布局，用 `require(StructureConstraint.exactly(match, count))` 限制实际选中的部件数量，用 `require(StructureConstraint.sameBlock(match))` 要求一组单元使用相同方块。`StructurePattern.count('A', min, max)` 只校验字符层图展开后的字符数量，两者不能互相代替。约束可以按匹配条件、角色或两者共同选择单元；标签选择的是由该标签声明匹配成功的单元，不会额外扫描其他单元的标签。

`StructureLayout` 的三参数构造器和 `LayoutCell` 的六参数构造器从 API 1.3.0 起标记 `@Deprecated(forRemoval = false)`，保留现有调用的源码和二进制兼容。新声明使用布局 builder、`LayoutCell.at` / `relative` 配合 `withDesign` / `withPorts`，或显式携带约束、端口的完整构造器。旧构造器分别产生无约束布局和无附加端口限制的单元；`equals` 包含约束，显式绑定归一化时保留约束。

数据包的约束与端口字段见[布局 JSON 与 KubeJS](layout-data.md)。约束随布局序列化并同步客户端；与候选数量明显不相容的约束会提前报错，但不对任意多条约束执行完整可满足性求解。

## 结构快照

行为机通过 `context.structure().cells()` 读取最近成功匹配的完整单元快照：世界坐标、角色和方块状态，包含普通壁板、普通方块、空气与重复层；未成型时为空。原来的 `parts()` 仍只提供附属功能部件的操作接口。快照不会因读取而重新扫描世界，也不授予外部占用其他机器部件的权限。

## 角色与端口

结构端口用 `StructureRole` 指定基本权限，再用 `StructurePorts` 限制该位置。配方机、行为机和燃料主控复用同一份角色权限定义；`FLUID_INPUT`、`FLUID_IO`、`ITEM_IO` 也进入实际传输路径。角色限制不能把主控原本不可输出的输入槽变成输出槽。

```java
var ports = StructurePorts.builder()
        .itemInput(PortRule.faces(MachineFace.FRONT).slots(1).filter("minecraft:iron_ingot"))
        .fluidOutput(PortRule.faces(MachineFace.BACK).slots(2).filter("#minecraft:water"))
        .build();
pattern.where('P', CellMatch.part("steel_wall"), StructureRole.ITEM_FLUID_IO, ports);
```

这是声明片段：`pattern` 来自已有字符层图，主控必须真正拥有物品槽 1 和流体槽 2。仓库内双平台测试消费者见 [StructureApiTestAddon](../../minecraft/src/test/java/gregtech/gametest/StructureApiTestAddon.java)，其声明和行为不引用内部执行类。

- 面相对主控，沿用 `MachineFace` 的 GT6 面编号；不是壁板自身朝向。限制了面的通道不接受无方向请求。未指定通道沿用角色权限，`PortRule.NONE` 关闭某个通道。
- 槽位从 0 开始，沿用该角色向外暴露的主控 handler 编号，不重新编号。通用配方机先列输入槽，再列输出槽；行为机按声明顺序；燃料主控流体槽 0 为燃料，其后是排放槽。未列槽位表示所有原本可用槽。
- 过滤条件支持资源 ID 和 `#tag`，多条按“满足任意一条”处理；省略表示任意资源。标签使用当前资源重载的结果，不复制成员表。暂不提供 NBT/组件表达式过滤。
- 能源目前只在单元级进一步限制接入面，类型、包大小、容量和过载仍由主控能源声明控制。行为机的物品、流体、能源回调会收到部件实际接入面；主控自身的面配置不被误用于壁板位置。
- 行为机使用 `context.energy().emitFromPorts(size, count)` 从成型结构中的普通能源输出部件送能，按邻居实际接受的整包扣减主控缓存。它遵守部件角色和面限制；自带能源缓存的附属部件继续用 `transferToPart`。燃料多方块声明了能源输出部件时从部件输出，未声明时保持原来的主控/偏移输出路径。原版聚变堆保留四个输出位置与尝试次序，并通过相同检查送出能源。
- 固定单元、重复层及替代部件使用同一组限制。普通装饰方块不能声明代理端口；自带库存/能源的附属功能部件仍使用 `LayoutPart` 自身规则，不能混用主控槽位。未提供独立流体槽访问的特殊主控会拒绝指定槽位或输出过滤配置，而不会静默放宽。
- 自动化能力在每次访问时检查成型状态及当前规则；拆除、失效、重载后缓存的旧能力不能继续按旧权限传输。主控内部处理、主动世界操作和原版漏斗/龙头手工路径仍使用各自接口，不能用它们冒充部件自动化权限。

旧布局和旧 `LayoutCell` 构造器默认无附加端口限制，不改注册 ID 或存档格式。

## 结构状态回调

`MachineBehavior.onStructureFormed` 和 `onStructureBroken` 是服务器状态边沿回调；成型时绑定已可读，失效时视图已清空。区块卸载也会失效，重新加载并成型后再通知，不能把失效回调当作拆机掉落事件。单方块行为机不接收这两个结构回调。现有行为实现无需补方法；SDK 的默认方法保持源码兼容。

大型烘箱、聚变堆、大型物质生成机以及物流核心已使用公开约束。特殊主控的业务仍有内部实现，尚不能宣称所有 GT6 多方块都只依赖公开 API。继续迁移时应沿用现有声明、匹配缓存、部件归属及生命周期，不为附属另建一套结构识别。

API 1.3 增加默认方法和新声明类型，保留旧贡献入口；机器注册 ID、方块实体 ID 与存档数据格式未变。

## 主动输出

主动输出和被管道抽取共用 `StructurePorts` 的面、槽位和物品/流体过滤，宿主实现见[engine](../architecture/packages/engine.md#多方块运行时)。二者不能混为一谈：原版 GT6 蒸馏塔允许从多处壁板抽液，但主动排液固定在背面对应高度；部分主控拆坏结构后仍能排空缓存。这些原版行为由显式的 buffered 出口保留，不会把被动 CASING 改成能源输出口。原版热交换器无接收器时照常耗散热量。

行为机的 `context.energy().emitFromPorts(...)` 使用成型部件及端口规则；多方块调用 `emitTo(...)` 同样检查目标接收面对应的自身主控或有效输出部件，不能用任意远程坐标绕过壁板权限。单方块 `emitTo` 保持原有契约。

结构诊断入口、线程要求和返回值见 [L5 查询与诊断](l5-query-and-diagnostics.md#结构诊断)。

## 预览与部件交互

JEI/EMI 从已编译的布局生成示例和材料清单；零份可选重复组展示一份示例，因此避雷针预览保留中央杆。成型条件仍允许零份，建筑之杖按最小有效结构搭建；两者使用同一份声明，没有另建几何表。每个主方块物品只显示一个结构页面。

已成型且归属唯一的有效部件可打开已有主方块界面；普通方块、固定部件和实际匹配的重复层共用服务端验证。距离范围两平台均为点击位置中心的 8 格；拆除、结构失效、归属变化、权限丢失或超距关闭菜单。客户端不使用主方块坐标代替服务端部件锚点判定距离。潜行、工具、覆盖板和流体容器保留各自交互，不被开窗截获。该便捷交互是移植版的有意扩展，见[差异记录](../decisions/parity/README.md)。
