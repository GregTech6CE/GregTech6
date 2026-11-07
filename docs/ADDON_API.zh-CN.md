# 附属与脚本扩展入口

本页面向附属与脚本作者；宿主实现的分层见[API 包职责](architecture/packages/api.md)。开发版附属需固定匹配的 GT6CE 运行时、平台和 SDK 构建。

按需阅读：[契约入口](#按需求找契约) · [库与 SDK](#独立库与-sdk-构建) · [多方块声明](#多方块声明api-13) · [输出与诊断](#主动输出与诊断api-13) · [布局 JSON](#布局-json-与-kubejs) · [预览与交互](#预览与部件交互)。

## 按需求找契约

| 需求 | 入口 | 时机/限制 |
| --- | --- | --- |
| 注册附属及配方就绪回调 | [GregTechAddon](../api/common/src/main/java/gregtech/api/addon/GregTechAddon.java)、[GregTechAddons](../api/common/src/main/java/gregtech/api/addon/GregTechAddons.java) | 在附属模组构造阶段注册；配方就绪后不接受迟注册 |
| 材料、形态、前缀、成分与矿物生成 | [AddonMaterialContributor](../api/common/src/main/java/gregtech/api/addon/material/AddonMaterialContributor.java) | ServiceLoader 早期收集纯声明，`link()` 在后续链接阶段执行 |
| 配方表、机器、布局、部件与管道 | [AddonMachineContributor](../api/common/src/main/java/gregtech/api/addon/machine/AddonMachineContributor.java) | 材料初始化后、机器注册前收集，不在回调中手工注册方块 |
| 机器与结构显式绑定 | `AddonMachine.withStructure(StructureLayout)`、`MultiblockMachine` | 在机器贡献阶段提交；缺少结构、冲突绑定和无效布局直接报错 |
| 字符层图与重复层 | `StructureLayout.pattern`、`StructurePattern`、`LayoutRepeat` | 统一编译为布局单元；`count` 检查图案数量，`require` 检查世界匹配结果 |
| 部件接入规则 | `StructurePorts`、`PortRule` | 在角色权限内收紧面、槽位、物品/流体过滤；主动输出沿用同一限制 |
| 结构状态与失败诊断 | `StructureAccess`、`AddonStructures`、`StructureProblem` | 在逻辑服务端读取宿主快照或按需诊断，不加载区块、不改变部件归属 |
| 能源/物品/流体/加载器等游戏能力 | [api.addon.platform](../api/minecraft/src/main/java/gregtech/api/addon/platform) | Minecraft 类型桥接，使用匹配版本和加载器的 SDK |
| KubeJS 脚本扩展 | 两节点的 `gregtech.compat.kubejs`、core 的 `engine.kubejs` | 原生插件接入与纯声明规则分层；使用对应平台版本 |

材料与机器 SPI 分别使用资源文件 `META-INF/services/gregtech.api.addon.material.AddonMaterialContributor`、`META-INF/services/gregtech.api.addon.machine.AddonMachineContributor` 列出实现类。按接口声明提供附属自身 ID 和纯数据；命名、范围与冲突规则以接口和 `AddonNames` 等校验为准。不要新用已标记弃用的 `GregTechMaterialContributor`。

`GregTechAddons` 的生命周期是单次的，重复 ID、迟注册或回调失败会明确报错；附属不调用平台负责的 `dispatchRecipesReady`。需要全新生命周期的测试应使用独立 JVM，不添加生产 reset 入口绕过约束；测试边界见[验证与验收](development/validation/README.md)。API 数字版本不能替代 SDK 与运行时构建匹配。

## 独立库与 SDK 构建

所有 API 源码内联自独立仓库 [GregTech6API](https://github.com/GregTech6CE/GregTech6API)（锁定 a9c879176，基线 API 1.2.0）；主库的 `api/` 是直接跟踪的源码，不是 Git 子模块，普通 `git clone` 即可。修改公共签名时直接在本仓库 `api/` 内更新，并在提交信息中记录 API 版本变化；主库构建不自动跟随远程分支。

命令在仓库根执行：

```bash
./gradlew :api:check :api:assemble
./gradlew :minecraft:1.20.1-forge:addonSdk
./gradlew :minecraft:1.21.1-neoforge:addonSdk
```

构建 SDK 时按目标平台选择一条命令；修改共享 API 源码后的验收按[验证与验收](development/validation/README.md)执行。Windows 使用 `.\gradlew.bat`。`:api` 使用 Java 17，仅依赖 JDK，不配置 Minecraft、Stonecutter 或模组资源；可通过 `JAVA17_HOME` 提供工具链。

| 产物坐标 | 内容与输出 |
| --- | --- |
| `gregtech:gt6ce-api:1.4.0` | Java 17 公共协议、模型、声明和宿主服务接口；`api/common/build/libs/` |
| `gregtech:gt6ce-addon-sdk-1.20.1-forge:1.4.0` | Java 17、Mojang 名称的 Forge 游戏侧 API；对应节点 `build/addon-sdk/` |
| `gregtech:gt6ce-addon-sdk-1.21.1-neoforge:1.4.0` | Java 21 的 NeoForge 游戏侧 API；对应节点 `build/addon-sdk/` |

这些是构建配置中的坐标，不表示已发布到公共 Maven 仓库。版本规则见下节[API 版本](#api-版本)。

`addonSdk` 同时构建公共库与目标平台 SDK 的 binary、sources 和 javadoc JAR。平台源码位于 `api/minecraft/src/main/java/`，由独立 `:api-platform:<node>` 项目经 Stonecutter 编译，不复制公共库类，也不包含 core/main 实现。原始产物在 `api/minecraft/versions/<node>/build/libs/`；主库的兼容命令将 SDK 复制到对应模组节点 `build/addon-sdk/`。`addonSdkJar` 仅导出平台编译 JAR。两平台 SDK 不能互换，消费者须配置对应 Minecraft/加载器开发环境。

公共库配置了 `api` Maven publication；独立平台项目配置了 `addonApi` publication，其 POM 仅声明公共 API 库的编译依赖。可用 `:api:generatePomFileForApiPublication` 或 `:api-platform:1.20.1-forge:generatePomFileForAddonApiPublication` 检查元数据，不必实际发布。主库保留同名 POM 生成委托入口。发布仓库需另行配置，发布操作不属于普通构建。

附属通过 Maven 使用平台 SDK 时应选择 `compileOnly`；直接使用本地文件时须同时提供公共 API JAR 和目标平台 SDK JAR。运行时由 GT6CE 模组携带唯一的一份公共及游戏 API，附属不可重新打包这些类。SDK 不能作为独立模组放进 `mods/`。

`AddonRuntime`、`AddonPlatform`、`AddonLoader` 的实现仍由宿主在原有初始化阶段绑定。独立库允许声明和纯模型操作；材料目录、物品与配方查询仍要求宿主就绪。SPI provider 资源留在宿主或附属，不能跟随接口复制进公共库。客户端接口只能由客户端入口加载。

## API 版本

当前分支使用 API `1.4.0`，与模组版本独立；以 [api/common/build.gradle](../api/common/build.gradle) 为准。`GregTechAddons.apiVersion()` 返回 `major * 1000 + minor`，1.4 为 `1004`，不能据此推断某个模组构建兼容。修改内联 API 时同步构建配置与 `GregTechAddons` 的版本。

宿主专用的 `StructureGrid` 与 `RecipeMapSpec` 不属于附属契约：它们分别位于 `gregtech.content.multiblock` 与 `gregtech.content.recipe`，编码内置多方块几何和配方表，附属不应依赖。

公共 API 的契约检查、SDK 与平台检查见[验证与验收](development/validation/README.md)。移动到 API 包的可变模型或内部辅助类不自动获得长期兼容保证。

## 材料目录（API 1.4）

材料和 metadata 仍通过 `AddonMaterialContributor` 提交。材料初始化 READY 后，`GregTechMaterials.catalog()` 返回当前会话的不可变 `MaterialCatalogView`：`resolve(name)` 解析规范名和别名，`find(MaterialKey)` 只返回真实材料，材料组不会伪装成可加工材料。旧宿主没有该服务时明确抛出 `UnsupportedOperationException`，未就绪时明确报告阶段，不以空集合掩盖失败。

`metadata()` 可给真实材料添加本附属 namespace 的分类 tag、flag 和类型化值。`materialTags()` 声明 tag 包含关系，`materialGroups()` 声明替代组及其材料／子组。附属只能定义自身 addon id namespace 的 tag、组和 property key；给其他材料添加描述 tag 允许，但修改其他材料的 property、flag、形态或加工能力仍需所有权。未知引用、同 key 异类型、跨所有者冲突、tag 包含环和组环均阻止材料初始化进入 READY。

```java
MaterialMetadata metadata = MaterialMetadata.builder()
        .property(new PropertyKey<>(new DataKey(addonId, "hardness"), Integer.class), 3)
        .tag(new DataKey(addonId, "conductive"))
        .build();
MaterialTagDeclaration metals = new MaterialTagDeclaration(
        new DataKey(addonId, "metals"),
        Set.of(new DataKey(addonId, "conductive")));
MaterialGroupDeclaration family = new MaterialGroupDeclaration(
        new MaterialGroupKey(new DataKey(addonId, "family")),
        "Family",
        Set.of(new MaterialKey("Iron")),
        Set.of());
```

分类 tag 只表达描述关系，不会自动生成形态、启用燃料、允许回收或授权分解；替代组成员关系也只供明确声明了组匹配的消费者使用。内容层面的实际物品、资源和配方 tag 属于 08-C 及后续阶段，不在当前定义目录中广播展开。

## 多方块声明（API 1.3）

在 `AddonMachineContributor.addonMachines()` 中提交 `controller.withStructure(layout)`。配方机、行为机和燃料发电机使用同一个 `MultiblockMachine` 声明；配方主控仍需指定 `largeRecipe()`，燃料主控仍需指定 `multi()`。结构的名称可以独立于机器类型，多个主控可复用同一份 `StructureLayout`。字符层图、固定单元与重复层继续使用既有布局 API。

这里的独立名称标识可复用模板，不是另一套运行时注册键。宿主为每个绑定生成以主控 `machineType` 命名的布局副本，后续注册和布局查找使用该键；原模板对象及其名称不会改变，也不会额外注册模板名称别名。例如，同一个 `shared_tower` 模板绑定两种主控后，会按各自的 `machineType` 注册。

```java
return List.of(recipeController.withStructure(sharedLayout),
               behaviorController.withStructure(sharedLayout));
```

此片段中的主控和布局需由调用方先构建。旧 `structureLayouts()` 从 API 1.3.0 起标记 `@Deprecated(forRemoval = false)`，仍是受支持的兼容入口，宿主会读取旧列表并将其与显式声明归一化；旧列表的布局名称必须等于主控 `machineType`。同一主控类型绑定不同结构、缺少必需结构、重复布局均会报错。

KubeJS 的 `addLayout()` 和对应 `structureLayouts()` 桥接同样保留并标记弃用，继续支持现有脚本。新脚本使用 `event.addMachine(controller.withStructure(layout))`，不再单独提交布局列表。

世界中的额外约束写入同一份布局，用 `require(StructureConstraint.exactly(match, count))` 限制实际选中的部件数量，用 `require(StructureConstraint.sameBlock(match))` 要求一组单元使用相同方块。`StructurePattern.count('A', min, max)` 只校验字符层图展开后的字符数量，两者不能互相代替。约束可以按匹配条件、角色或两者共同选择单元；标签选择的是由该标签声明匹配成功的单元，不会额外扫描其他单元的标签。

`StructureLayout` 的三参数构造器和 `LayoutCell` 的六参数构造器从 API 1.3.0 起标记 `@Deprecated(forRemoval = false)`，保留现有调用的源码和二进制兼容。新声明使用布局 builder、`LayoutCell.at` / `relative` 配合 `withDesign` / `withPorts`，或显式携带约束、端口的完整构造器。旧构造器分别产生无约束布局和无附加端口限制的单元；`equals` 包含约束，显式绑定归一化时保留约束。

数据包的约束与端口字段见[布局 JSON 与 KubeJS](#布局-json-与-kubejs)。约束随布局序列化并同步客户端；与候选数量明显不相容的约束会提前报错，但不对任意多条约束执行完整可满足性求解。

行为机通过 `context.structure().cells()` 读取最近成功匹配的完整单元快照：世界坐标、角色和方块状态，包含普通壁板、普通方块、空气与重复层；未成型时为空。原来的 `parts()` 仍只提供附属功能部件的操作接口。快照不会因读取而重新扫描世界，也不授予外部占用其他机器部件的权限。

结构端口用 `StructureRole` 指定基本权限，再用 `StructurePorts` 限制该位置。配方机、行为机和燃料主控复用同一份角色权限定义；`FLUID_INPUT`、`FLUID_IO`、`ITEM_IO` 也进入实际传输路径。角色限制不能把主控原本不可输出的输入槽变成输出槽。

```java
var ports = StructurePorts.builder()
        .itemInput(PortRule.faces(MachineFace.FRONT).slots(1).filter("minecraft:iron_ingot"))
        .fluidOutput(PortRule.faces(MachineFace.BACK).slots(2).filter("#minecraft:water"))
        .build();
pattern.where('P', CellMatch.part("steel_wall"), StructureRole.ITEM_FLUID_IO, ports);
```

这是声明片段：`pattern` 来自已有字符层图，主控必须真正拥有物品槽 1 和流体槽 2。完整可编译的双平台消费者见 `minecraft/src/test/java/gregtech/gametest/StructureApiTestAddon.java`，其声明和行为不引用内部执行类。

- 面相对主控，沿用 `MachineFace` 的 GT6 面编号；不是壁板自身朝向。限制了面的通道不接受无方向请求。未指定通道沿用角色权限，`PortRule.NONE` 关闭某个通道。
- 槽位从 0 开始，沿用该角色向外暴露的主控 handler 编号，不重新编号。通用配方机先列输入槽，再列输出槽；行为机按声明顺序；燃料主控流体槽 0 为燃料，其后是排放槽。未列槽位表示所有原本可用槽。
- 过滤条件支持资源 ID 和 `#tag`，多条按“满足任意一条”处理；省略表示任意资源。标签使用当前资源重载的结果，不复制成员表。暂不提供 NBT/组件表达式过滤。
- 能源目前只在单元级进一步限制接入面，类型、包大小、容量和过载仍由主控能源声明控制。行为机的物品、流体、能源回调会收到部件实际接入面；主控自身的面配置不被误用于壁板位置。
- 行为机使用 `context.energy().emitFromPorts(size, count)` 从成型结构中的普通能源输出部件送能，按邻居实际接受的整包扣减主控缓存。它遵守部件角色和面限制；自带能源缓存的附属部件继续用 `transferToPart`。燃料多方块声明了能源输出部件时从部件输出，未声明时保持原来的主控/偏移输出路径。原版聚变堆保留四个输出位置与尝试次序，并通过相同检查送出能源。
- 固定单元、重复层及替代部件使用同一组限制。普通装饰方块不能声明代理端口；自带库存/能源的附属功能部件仍使用 `LayoutPart` 自身规则，不能混用主控槽位。未提供独立流体槽访问的特殊主控会拒绝指定槽位或输出过滤配置，而不会静默放宽。
- 自动化能力在每次访问时检查成型状态及当前规则；拆除、失效、重载后缓存的旧能力不能继续按旧权限传输。主控内部处理、主动世界操作和原版漏斗/龙头手工路径仍使用各自接口，不能用它们冒充部件自动化权限。

旧布局和旧 `LayoutCell` 构造器默认无附加端口限制，不改注册 ID 或存档格式。

`MachineBehavior.onStructureFormed` 和 `onStructureBroken` 是服务器状态边沿回调；成型时绑定已可读，失效时视图已清空。区块卸载也会失效，重新加载并成型后再通知，不能把失效回调当作拆机掉落事件。单方块行为机不接收这两个结构回调。现有行为实现无需补方法；SDK 的默认方法保持源码兼容。

大型烘箱、聚变堆、大型物质生成机以及物流核心已使用公开约束。特殊主控的业务仍有内部实现，尚不能宣称所有 GT6 多方块都只依赖公开 API。继续迁移时应沿用现有声明、匹配缓存、部件归属及生命周期，不为附属另建一套结构识别。

API 1.3 增加默认方法和新声明类型，保留旧贡献入口；机器注册 ID、方块实体 ID 与存档数据格式未变。

## 主动输出与诊断（API 1.3）

主动输出和被管道抽取共用 `StructurePorts` 的面、槽位和物品/流体过滤，宿主实现见[engine](architecture/packages/engine.md#多方块运行时)。二者不能混为一谈：原版 GT6 蒸馏塔允许从多处壁板抽液，但主动排液固定在背面对应高度；部分主控拆坏结构后仍能排空缓存。这些原版行为由显式的 buffered 出口保留，不会把被动 CASING 改成能源输出口。原版热交换器无接收器时照常耗散热量。

行为机的 `context.energy().emitFromPorts(...)` 使用成型部件及端口规则；多方块调用 `emitTo(...)` 同样检查目标接收面对应的自身主控或有效输出部件，不能用任意远程坐标绕过壁板权限。单方块 `emitTo` 保持原有契约。

附属不需要引用内部追踪器。行为回调中按需调用 `context.structure().diagnose()`；独立工具可使用公共入口：

```java
List<StructureProblem> problems = AddonStructures.diagnose(serverLevel, controllerOrPartPos);
for (StructureProblem problem : problems) {
    player.sendSystemMessage(AddonStructures.describe(problem));
    if (problem instanceof StructureProblem.Count count) {
        int missing = Math.max(0, count.minimum() - count.actual());
        // 自定义 UI 可直接使用实际数量和上下限，不解析提示字符串。
    }
}
```

结果为不可变数据，区分错误方块（含全部可选方块或标签）、未加载区块、重复层不足、数量不足/超限、同组方块不一致、被其他主控占用、附加条件拒绝及无法诊断。包含世界坐标；不一致/占用还包含参照方块或占用主控坐标。`describe` 复用放大镜的本地化文本。

必须在逻辑服务端线程调用，适合玩家请求或工具操作，不应每 tick 扫描。查询不强制加载区块、不抢占部件、不触发成型回调；空结果只代表当前扫描未发现问题，不承诺之后的世界状态。普通方块返回 `Unavailable`，已关联部件转查所属主控；旧宿主不支持时明确抛出异常。数量是已成功匹配的单元数，未知区块不会被误报为数量不足。条件部件的工作许可仍由 `conditionsMet()` 提供，不当作结构搭建错误。

## 布局 JSON 与 KubeJS

数据包路径为 `data/<namespace>/gt6ce_layouts/<machineType>.json`。它替换已有机器的完整布局，不注册新的主方块；省略的旧单元、约束和端口规则不会自动继承；省略 `constraints` 表示无约束，省略 `ports` 表示无附加端口限制。重载后，运行时匹配缓存、部件归属和客户端预览一起失效并重建。

| 字段 | 含义 |
| --- | --- |
| `cells` | 固定单元数组。每项有整数 `right/up/back`、`match` 和 `role`；主方块为原点，不能再把原点写成部件。 |
| `match` | 已注册的部件名、方块 ID `namespace:block`、方块标签 `#namespace:tag`，或这些选择的数组；也支持 `air` 等现有匹配条件。 |
| `role` | `casing`、`item_input`、`fluid_output`、`item_fluid_io` 等 `StructureRole` 名称，忽略大小写；普通方块只能使用 `casing`。 |
| `design` | 可选部件外观编号，0–255；还会检查目标部件是否支持该编号。省略表示不改外观。 |
| `repeats` | 重复组数组：唯一的 `key`、锚点 `up`、份数 `min/max`、可选 `step`（1 或 -1）和 `cells`。组内 `up` 相对于锚点，`max=-1` 表示使用宿主的安全上限。 |
| `constraints` | 按实际匹配结果执行的数量与同类方块约束，见下。 |
| 单元的 `ports` | 该单元的接入限制；重复组单元也可声明，见下。 |

`constraints` 支持两种对象：`{"type":"count","match":"steel_wall","min":2,"max":4}` 和 `{"type":"same_block","role":"casing"}`。`match`、`role` 都是可选选择器；同时省略表示所有声明单元，同时提供时取交集。数量包含已匹配的重复层，不统计主方块。`max=-1` 表示无上限。`same_block` 要求选中单元使用相同方块注册身份，并不要求它们的朝向状态相同。未定义的字段、非法范围和无法选中任何声明单元的约束都会报错。

`ports` 的键为 `item_input`、`item_output`、`fluid_input`、`fluid_output`、`energy_input`、`energy_output`。每个键下可写：

- `faces`：相对主方块的 `front/back/left/right/top/bottom` 数组，省略为六面，空数组禁止该通道；受限时无方向能力请求不能绕过限制。
- `slots`：物品或流体聚合接口的零起始索引，空数组或省略表示不额外限制；流体输入槽在前、输出槽在后。槽位越界或主方块不能提供精确槽位访问时会报错。
- `filters`：物品/流体注册 ID 或 `#tag` 字符串数组，数组内取并集，和槽位、面、角色限制取交集；省略或空数组表示不过滤。

未知字段、未知资源 ID 和角色不支持的通道会报错。能源通道不能声明物品/流体的槽位和过滤。`ports` 只收紧角色权限，不能让纯壁板凭空拥有能源接口；同样不能让主方块的输入槽变成输出槽。

下面是可嵌入已有布局单元的字段片段，主方块必须确实拥有对应槽位：

```json
"ports": {
  "item_input": {"faces": ["front"], "slots": [0], "filters": ["minecraft:iron_ingot"]},
  "fluid_output": {"faces": ["back"], "slots": [2], "filters": ["#minecraft:water"]}
}
```

`GT6CE.layout(event, machineType, spec)` 接收完整布局对象或兼容的单元列表，使用和数据包相同的解析器，保留 `constraints` 与单元 `ports`，未知字段会报错。KubeJS 也可以通过目标版本的数据生成事件直接提交上述 JSON；Forge 插件接收 `DataPackEventJS`，NeoForge 插件接收 `KubeDataGenerator`。机器注册阶段的 Java builder 与数据生成阶段的 JSON 是不同入口，不能在配方事件中重新注册主方块。脚本运行仍须使用目标平台匹配的 KubeJS 版本；纯 JSON 解析测试不代表已验证全部脚本加载环境。

## 预览与部件交互

JEI/EMI 从已编译的布局生成示例和材料清单；零份可选重复组展示一份示例，因此避雷针预览保留中央杆。成型条件仍允许零份，建筑之杖按最小有效结构搭建；两者使用同一份声明，没有另建几何表。每个主方块物品只显示一个结构页面。

已成型且归属唯一的有效部件可打开已有主方块界面；普通方块、固定部件和实际匹配的重复层共用服务端验证。距离范围两平台均为点击位置中心的 8 格；拆除、结构失效、归属变化、权限丢失或超距关闭菜单。客户端不使用主方块坐标代替服务端部件锚点判定距离。潜行、工具、覆盖板和流体容器保留各自交互，不被开窗截获。该便捷交互是移植版的有意扩展，见[差异记录](decisions/parity/README.md)。

## KubeJS 与示例状态

开发运行用 `-PkubejsRuntime=true` 加载对应原生运行依赖；插件入口由各节点 `kubejs.plugins.txt` 声明。先看目标节点的插件、事件与声明处理代码，再决定脚本形态，不能照搬另一 MC 版本的语法。

当前工作树没有 `examples/addon-sample/` 或 `examples/kubejs-sample/`。本页不提供未经编译/运行验证的替代样例；将来新增可维护消费者示例放根 `examples/`，说明依赖、构建/运行命令与已验证平台，临时消费探针放 `work/`。
