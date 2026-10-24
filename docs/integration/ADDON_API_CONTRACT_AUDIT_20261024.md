# 附属 API 契约核对（2026-10-24）

对 SDK 公开面做的一次只读核对。范围是 `core/src/api` 与 `multiversion/src/api`，以及这些类型在 `core/src/main`、`multiversion/src/main` 里的直接实现。对照的是源码和 `core/src/test/java/com/gregtech/gregtech/core/AddonDeclarationTypeContracts.java`、`AddonLifecycleContracts.java`、`docs/ADDON_API.md`。没有改代码，没有重跑测试，也没有反编译 Minecraft 的 `ItemStack`。

使用说明仍以 [中文 API 指南](../ADDON_API.zh-CN.md) 和 [英文 API 指南](../ADDON_API.md) 为准。本文记录契约和源码不一致的地方，以及已经写进测试、但调用方仍然会踩坑的行为。

## 结论

会在运行时静默算错、或让相等性契约失效的有四条：`AddonRecipes.Line` 的值相等、`MachineFace.sameSet` / `hashSet`、`GregTechAddons.dispatchRecipesReady` 对受检异常留下 `DISPATCHING`、`GTVoltageTiers.tierMax` 对表外电压和 `Long.MIN_VALUE` 的返回值。

契约写错、数据却还在的有两条：`AddonLoader.Services.fluid` 的 null 注释，以及 `MaterialDeclaration.id()` 报告一个注册时会被忽略的编号。`BehaviorMachineClient.screen` 缺少 `machineType`，是缺参数，不是算错。

其余条目是可演进性、校验不一致和工程卫生。其中若干条已经被类注释或测试锁成当前行为，改的时候要单独决定是修实现还是改文档。

## 优先修改

1. `Line.equals` / `hashCode` 按物品栈身份比较。含非空物品的两行，内容相同也不能去重。
2. `MachineFace.sameSet` 与 `hashSet` 在重复元素上违反 `equals` / `hashCode` 应有的配合。两个方法是 public 的。
3. `dispatchRecipesReady` 只捕获 `RuntimeException | Error`。受检异常逃出后阶段停在 `DISPATCHING`。
4. `tierMax` 在电压高于表时返回 `16`，作为 `VOLTAGES` 的下标会越界；`Long.MIN_VALUE` 落到 tier 0。
5. `Services.fluid` 的注释与 `AddonFluids`、`AddonLoaderServices` 相反。
6. 废弃的 `MaterialDeclaration.builder(int, String)` 把不会生效的 id 存进 record。
7. `BehaviorMachineClient.screen` 和 `BehaviorMachineMenuView` 都没有机器类型。

## 1. 会算错或破坏契约

### 1.1 `AddonRecipes.Line` 的相等性

`multiversion/src/api/java/com/gregtech/gregtech/api/addon/platform/AddonRecipes.java` 第 176–195 行在构造时复制每个栈。第 294–322 行的 `equals` / `hashCode` 使用 `Arrays.equals` / `Arrays.hashCode`，也就是元素自己的 `equals` / `hashCode`。

本仓库没有给 `ItemStack` 提供值相等。非空物品栈比较的是对象身份，而构造器又对每个栈 `copy()`，所以两个内容相同、分别构造的 `Line` 不相等，哈希也不同。放进 `Set` 或用来去重会同时保留两行。

只有流体、物品槽全是 `ItemStack.EMPTY` 的行仍然可以相等：`FluidStack` 按流体、数量和附加数据比较，`ItemStack.EMPTY` 是单例。类注释写的是不可变快照；`equals` 和 `hashCode` 是按值对象写的，比较规则却不是。

### 1.2 `MachineFace.sameSet` / `hashSet`

`core/src/api/java/com/gregtech/gregtech/api/addon/MachineFace.java` 第 55–67 行：

- `sameSet` 先要求长度相等，再比较 `HashSet`。`[A,A,B]` 与 `[A,B,B]` 返回 true。`[A,A]` 与 `[A]` 因长度不同返回 false。它既不是集合相等，也不是多重集相等。
- `hashSet` 按出现次数把 `hashCode` 相加。上面第一对的哈希分别是 `2h(A)+h(B)` 和 `h(A)+2h(B)`。
- 注释写着这是 “Order-independent hash for sameSet”。有重复元素时这句话不成立。

`EnergyInput.withFaces`（第 90–93 行）只做 `List.copyOf`，不去重。公开的 `EnergyInput.build()` 会先走 `MachineFace.distinct`，`MachineFace.all()` 和 `exceptFront()` 也没有重复，所以现有模板路径不会把重复面存进去。公开方法和 `withFaces` 本身仍然接受这种列表。

### 1.3 相等的能量声明可以读出不同的面顺序

`EnergyInput.equals`（第 102–117 行）和 `EnergyOutput.equals`（第 56–67 行）用 `sameSet` 比较面。`faces()` 返回的是有序 `List`。

`AddonDeclarationTypeContracts` 第 149–154 行把这一点写成预期：`stored.equals(flipped)` 为真，两边的 `faces()` 分别是左-右和右-左。面列表没有重复时，`hashCode` 与 `equals` 一致。能观察到的差异是顺序。根子是用 `List` 保存一个参与相等性的无序集合。

`Builder.capacity` 的注释说 0 是有效值（`EnergyInput` 第 160 行）。`collapseDefaultCapacity()`（第 95–99 行）又把容量 0 折回“未指定”。同一测试第 143–145 行要求“写了 0 等于没写”。行为机建完之后，这个区别观察不到。

### 1.4 生命周期可以停在 `DISPATCHING`

`GregTechAddons.dispatchRecipesReady`（`GregTechAddons.java` 第 47–62 行）把阶段设为 `DISPATCHING` 后，只捕获 `RuntimeException | Error`。

`onRecipesReady` 的 Java 签名抛不出受检异常。Kotlin 和 `@SneakyThrows` 可以。那种异常逃出后：

- 阶段保持 `DISPATCHING`；
- 之后的 `register` 因阶段不是 `COLLECTING` 报 “GT addon registration closed”；
- 再次 `dispatchRecipesReady` 报 “dispatched twice”；
- `recipesReady()` 只在 `READY` 时为真，因此一直是 false。

`Error` 会先写成 `FAILED`，再由 `AddonFailures.of` 原样抛出，这条路径不会卡住。`AddonLifecycleContracts` 覆盖了运行时异常和 `Error`，没有覆盖受检异常。

### 1.5 `GTVoltageTiers`

`core/src/api/java/com/gregtech/gregtech/api/energy/GTVoltageTiers.java`：

- 第 19、25 行的 `VOLTAGES` 和 `NAMES` 是公共可变数组，内容是私有表的副本。查找用私有表，所以“改了不影响查找”这句注释是对的，测试第 158–161 行也锁了这一点。所有 addon 读的仍是同一对数组，一处写入，别处能看见。
- `tierMax`（第 30–38 行）在电压高于表时返回 `VOLTAGE_TABLE.length`，即 16。`VOLTAGES` 的合法下标是 0..15，`VOLTAGES[tierMax(v)]` 会越界。`nameOf` 和 `maxVoltageOf` 有上界保护。
- `Math.abs(Long.MIN_VALUE)` 仍是负数，循环里第一档就满足 `value <= VOLTAGE_TABLE[i]`，返回 tier 0。
- `NAME_TABLE` 第 14、15 档都是 `"XV"`，注释称这是 GT6 `CS.VN`。`nameOf` 区分不了这两档；高于表的电压也返回最后一个名字 `"XV"`。

### 1.6 自定义屏幕拿不到机器类型

`BehaviorMachineClient.screen(BehaviorMachineMenuView, Inventory, Component)`（第 27 行）没有 `machineType`。`BehaviorMachineMenuView` 只有能量、进度、开关、储罐和 `containerId`。同接口的 `renderer(String machineType)`（第 30 行）带了这个参数。一个 addon 声明两种行为机器时，屏幕回调无法按类型分支。

### 1.7 同名 `item` / `setItem` 语义相反

| | 机器 `MachineBehaviorContext` | 零件 `PartContext` |
| --- | --- | --- |
| `item(int)` | 第 43 行注释写活引用。实现是 `items.getStackInSlot(slot)`（`BehaviorMachineBlockEntity` 第 1146 行） | 第 45 行注释写分离副本。实现是 `getStackInSlot(slot).copy()`（`FunctionalPartRuntime` 第 117–119 行） |
| `setItem` | 第 1147 行：null 变空栈，其余 `copy()`，不按格子上限截断 | 第 457–463 行：越界抛异常，数量截到 `getSlotLimit` |

两套注释和实现各自一致。addon 在两种上下文之间切换时，同名方法的读写规则不同。

### 1.8 流体的 null 契约互相矛盾

`AddonLoader.Services.fluid`（`AddonLoader.java` 第 30–31 行）写着未注册时返回 null。`AddonFluids`（第 13–23 行）写着未知字段、null 参数或非正数量都是 `FluidStack.EMPTY`，并且原样返回服务结果。`AddonLoaderServices`（第 43–47 行）实际返回 `EMPTY`。

错的是接口注释。第三方若按那句注释返回 null，门面会把 null 交出去。

### 1.9 `MaterialDeclaration` 的 id 被保存，注册时忽略

`MaterialDeclaration.java`：

- 紧凑构造器允许 `0..9999`（第 30 行）。
- `builder(String)` 传入 0（第 59 行）。
- 废弃的 `builder(int, String)` 注释说编号会被忽略（第 61–63 行），方法体仍校验 `1..9999` 并把该值放进 builder（第 67–68 行）。`build()` 把它写入 record 的 `id()`。

`AddonMaterials.warnIgnoredDeclarationIds`（第 142–147 行）对非 0 的 id 打警告。`definitions()`（第 150–167 行）调用 `MaterialDefinition.builder(1, declaration.name())`，声明的编号不参与生成。`id()` 对外报告的是一个不会生效的值。0 在紧凑构造器里合法，在废弃 builder 里非法，两种含义叠在同一个字段上。

## 2. 公开面上的内部规则

下列类型位于 SDK，Javadoc 或方法体携带运行时策略。以后改一行就是二进制或行为上的破坏性变更。

| 类型 | 公开内容 |
| --- | --- |
| `RecipeKeptInputs` | `settle`、`fluidDemand`，以及 kept 索引的归一化 |
| `PartEnergyRules` | `admit`：6 个 `long` 再加 1 个 `boolean` |
| `BehaviorEnergyRules` | `admitInput`：6 个 `long` 加 3 个 `boolean`；`readCapacity` 解释 NBT 键 `gt.capacity` |
| `BehaviorRangeRules` | 上限注释写着 kTFRU 最宽矿机是 32 |
| `BehaviorRecipeCatalysts` | JEI/EMI 索引用的泛型 `add` |
| `AddonOverclock` | `enabled(boolean, boolean, boolean, boolean)` |
| `BasicMachineEnergy` | 注释含 ALIGN-047 |
| `MachineWorkCost` | `calculate(long, long, int, boolean, int, long, long, boolean, boolean)`；注释含 `BASIC_MACHINE_EXECUTION_AUDIT.md`、ALIGN-040、“GT6 BasicMachine:766-773” |
| `InstalledService` | 一次性绑定的实现 |
| `AddonFailures` | 回调失败的包装规则 |
| `GTVoltageTiers` | 注释点名 `BasicMachineOriginalParams` |

`MaterialDeclaration` 有 18 个组件、9 个 boolean。第 49–56 行的兼容构造器把后加的 `centrifuge` 和 `electrolyzer` 固定为 false。`AddonRecipes.Line` 有 5 个公开构造器和 3 个 `read` 重载；最长的构造器末尾连续 4 个 `int[]`：`outputChances`、`keptInputs`、`keptFluidInputs`、`presenceFluidInputs`。

`MachineBehaviorContext` 上，同一词根的返回单位不同：

- `capacity()` 是能量上限，`capacity(int)` 是储罐的 mB。`tankCapacity(int)` 是后者的别名。
- `energy()` 是主缓冲，`energy(String)` 是辅助缓冲。`auxiliaryEnergy(String)` 是别名。`consume` 同样拆成两个。
- `emit(long, long)` 返回接受的包数。`emit(String, long, long)` 返回能量单位。`emitToPart` 是后者的别名。
- `MachineEnergy` 把这些操作再包了一层，返回 `EnergyTransfer`。

布尔方向相反的参数：

- `FluidAccess`、`InventoryAccess`、`HeatHandler`、`MachineBehavior.acceptFluid`、上下文的 `fill` / `drain` 使用 `simulate`，true 表示不执行。
- `AddonPlatform.Services.inject` / `extract` 使用 `execute`，true 表示执行。
- `EnergyBlock.doEnergyInjection` 和 `doInject` 的最后一个参数都叫 `doInject`；`doEnergyExtraction` 和 `doExtract` 的参数都叫 `doExtract`（`EnergyBlock.java` 第 27、33、69、71 行）。参数名和方法名相同。

`AddonRecipes.Map` 遮蔽 `java.util.Map`。`GregTechMaterials.Component` 和 `net.minecraft.network.chat.Component` 同名，addon 里两者经常同时出现。

方向有两套：

- `canInsert`、`canFill`、`acceptsEnergy`、`onTool`、声明侧的 `gasValve` 用 `MachineFace`。
- `gtSignal`、`fillMoldAtSide`、`pushCompressedGas` 用 `Direction`。
- `CrucibleMold`、`GtSignalSource`、`ProgressSensorSource`、`CompressionSensorSource`、`TemperatureSource` 五处写明 core 类型不能命名 `Direction`，side 使用 `Direction.ordinal()`。

三个机器模板的规则不同，`MachineCasing` 第 9 行自己写了 “the rules are not the same”：

| | 机器类型 | tier | `energy(null)` |
| --- | --- | --- | --- |
| `RecipeMachine` | 直接用 id，含 `/` 非法（第 165–166 行） | `≥ 1`，无上限（第 167–169 行） | 第 121 行抛 “Missing energy” |
| `BehaviorMachine` | 把 id 里的 `/` 换成 `_`（第 297 行） | `1..255`（第 300–302 行） | 第 230 行接受；第 303 行允许只声明输出 |
| `FuelGenerator` | 直接用 id，含 `/` 非法（第 100–101 行） | 禁止 `tierSpecified`（第 96–97 行） | `energy(非 null)` 抛异常（第 74–76 行） |

只为了在运行时抛异常而存在的 builder 方法：`RecipeMachine.Builder.energyOutput` / `output`（第 136–143 行），`FuelGenerator.Builder.energy` / `energyOutput` / `io`（第 74–84 行），`BehaviorMachine.Builder.output`（第 240–243 行）。传入非 null 就抛，传入 null 就返回 this。`BehaviorMachine.energyOutput` 是真正的 setter。

集合 setter 的方向不统一。`BehaviorMachine.Builder` 的 `recipeMaps`、`clientState`、`item`、`fluid`、`toggle` 是追加。`MachineIo.Builder` 的面 setter、`RecipeMachine.Builder.structureKeys`、`AddonRecipes.Line.Builder` 的各字段 setter 是替换。

要到 `build()` 才发现缺失的必填项：`LayoutPart` 的 hardness（第 220 行）、`PartTanks` 的 capacity（第 74–75 行）、`BehaviorMachine` 的 gui（第 312–313 行）、`RecipeMachine` 的 energy 和 recipeMap（第 170、211 行）、`FuelGenerator` 的 fuelMap 和 output（第 102–104 行）。

用标志位和哨兵代替“未设置”：`rangeSpecified`、`capacitySpecified`、`facesSpecified`、`tierSpecified`、`cheapOverclockingSpecified`、`hardnessSpecified`、`stackSpecified`；`LayoutCell.design = -1`，`LayoutRepeat.max = -1`，`FluidPipeDeclaration.maxTemperature = 0` 表示用熔点乘 1.25，`MaterialDeclaration.id = 0`，`EnergyType.ANY = "*"`。

给 addon 实现、并且由 `ServiceLoader.load` 装载的接口是 8 个：`AddonMaterialContributor`、`AddonMachineContributor`、`MachineTuningContributor`、`AddonSensorContributor`、`LayoutPartContributor`、`BehaviorMachineContributor`、`LayoutPartClient`、`BehaviorMachineClient`。每个都声明 `addonId()`。`GregTechAddon` 用的是 `id()`，并走 `GregTechAddons.register`。`core/src/main` 里还有废弃的 `GregTechMaterialContributor` 和 `GregTechMachineContributor`，运行时的加载点是 10 个。

`machineTypes()` 加 `create(String)` 这个同名组合只出现在 `BehaviorMachineContributor`。同一种“名字集合 + 按字符串分派”还有三份，方法名不同：`BehaviorMachineClient.renderer(String)`、`LayoutPartContributor.partNames()` + `create(String)`、`LayoutPartClient.renderer(String)`。

能量类型是 `EnergyType` 里的字符串常量，`same()` 做忽略大小写比较，未知但符合 `NAME` 的字符串会原样保留。`GregTechAddon.Context.platform` 是字符串。`AddonSensor.unit()` 的合法 glyph 只列在第 10–14 行的 Javadoc 里。

## 3. 校验和边界

同一类数值在多处手写：

- `1_000_000_000_000L` 出现 4 次：`BehaviorMachine` 第 389 和 412 行，`RecipeMachine` 第 189 行，`FuelOutput` 第 80 行。配方机的主输入只拒绝负数，测试写明没有上限。
- `1_024_000_000` 是 `FuelGenerator.MAX_TANK`、`PartTanks.MAX_CAPACITY`、`CompressedGasTankDeclaration.MAX_CAPACITY`，以及 `BehaviorMachine` 第 341 行的裸字面量。
- 效率上限 `100_000` 在 `RecipeMachine` 第 215 行、`FuelGenerator` 第 105 行、`MachineTuning` 第 35 行。默认效率是 `10_000`。这三处都没有写单位。

`AddonNames` 的类注释说共享模式是为了避免每次重新编译正则。下面这些调用仍使用 `String.matches`，每次编译一份新模式：`SmeltingTargetRemoval` 第 15 行（与 `AddonNames.MATERIAL` 相同）、`MachineCasing` 第 73 和 77 行、`StructureLayout` 第 72 行、`MachineTuning` 第 18 行、`PrefixDeclaration` 第 60、68、81、116 行、`CellMatch` 的位置校验、`LayoutPart` 的 gui 与贴图、`BehaviorMachine` 的 gui 与客户端键。贴图路径正则在 `PrefixDeclaration` 第 81 行和 `LayoutPart` 第 217 行各写一遍，模式相同。

null 的拒绝方式不统一。`LayoutCell` 第 46–47 行：`match == null` 抛 `IllegalArgumentException`，`role == null` 经 `Objects.requireNonNull` 抛 `NullPointerException`。`InstalledService.bind` 对 null 实现抛 `IllegalArgumentException`。`GregTechAddons.register` 对 null addon 抛 `NullPointerException`。

varargs 里的 null 元素走不同的异常。`MachineIo`、`EnergyInput`、`EnergyOutput` 先 `List.of`，null 元素是 `NullPointerException`，到不了 `MachineFace.distinct` 第 40 行的 “Missing …”。`PartSlots.insert` / `extract` 先 `MachineFace.listed` 再 `distinct`，null 元素能走到那条 `IllegalArgumentException`。`MachineIo.itemOutputTarget(null)` 被接受，`itemAutoInput(null)` 抛 “Missing automatic face”。

构造时会改写调用方的数：

- `CrucibleContent` 把负的 amount 和 temperature 收成 0（第 16–17 行）。
- `Line` 把 `duration <= 0` 收成 1（第 183 行）。
- presence 流体在已复制的栈上 `setAmount(1)`（第 194 行）。指南说这个正数只是身份占位；读回 `fluidInputs()` 时数量已经不是传入值。
- 物品输出中的 null 变成 `ItemStack.EMPTY`（第 365 行）。
- `GregTechMaterials.StackComposition.of` 丢掉 `material == null` 或数量不大于 0 的组分。canonical 构造器只把 null 前缀收成空串并复制列表。`new StackComposition("", List.of(new Component(null, 1L)))` 的 `present()` 为 true。

关系没有检查：

- `MaterialDeclaration` 不要求熔点小于等于沸点。矿副产只检查名字形态，可以重复，也可以指向自身。
- `PrefixDeclaration` 的 `materials` 与 `excludedMaterials` 可以重叠。材料名只拒绝空白和重复，不按 `AddonNames.MATERIAL` 校验。
- `MaterialFormRequest` 只对两个名字 `trim()`。
- `RecipeTable.build` 不要求 `minimalInputItems <= inputItems`，流体侧同样。
- `MaterialComposition` 和 `CrucibleReactionDeclaration` 的 parts 可以重复。
- `MachineTuning` 在 min、max 都存在时检查 `max >= min`，不要求 `energyIn` 落在这个区间里。
- `MachineIo.build` 不要求自动面属于对应的面列表。

`MachineIo` 的类注释（第 9–11 行）和测试（第 205–208 行）已经写明默认值：不调用 `io()` 时物品和流体六面开放；一旦建出 `MachineIo`，没写到的通道是空列表。

`LayoutRepeat.absoluteUp`（第 60–62 行）是公开方法，公式 `up + step * (cell.up() + copy * height)` 没有溢出检查。`StructureLayout.absoluteLayer`（第 106–114 行）是同一公式，使用 `Math.addExact` 和 `Math.multiplyExact`。

`BehaviorState` 的 int、long、boolean、string 各用一张表（`BehaviorMachineBlockEntity.StoredState` 第 1611–1615 行，零件的 `DeclaredState` 同样）。`setInt("x", 1)` 之后 `getLong("x")` 返回 0。接口没有写类型隔离。

“字符串不能为 null、长度不超过 256”写在两处实现里：`BehaviorMachineBlockEntity` 第 1639–1640 行，`FunctionalPartRuntime` 第 538–539 行。`BehaviorState` 接口没有这句。零件的 `check` 对未声明的 key 抛 “Undeclared part field”（第 562–565 行）。机器的 `check` 只要求 key 匹配 `[a-z0-9_.]{1,64}`（第 1713–1716 行）。`BehaviorState` 的类注释按机器语义写缺省读数；`PartContext.state` 的注释写未命名 key 会抛。

`AddonRuntime.bind`、`AddonPlatform.bind`、`AddonLoader.bind` 都是 public，注释写明 addon 不得调用。`InstalledService` 不检查调用方，谁先绑定谁占住实现。`docs/ADDON_API.md` 里构造器重叠说的是材料提交：Forge 1.20.1 上 addon 构造器与 GregTech 构造器可能重叠。这个窗口里，public 的 `bind` 也可以被先调用。

`GregTechAddons` 的 `ADDONS` 和 `phase` 没有重置入口。`core/build.gradle` 第 355–372 行把 `AddonLifecycleContracts` 的 `runtime` 和 `fatal` 各自放进一个新 JVM。测试类用 `main` 加 `switch` 选择场景。同文件第 335–353 行对热力源合同做了同样的事。

## 4. 写法

下列类型手写了 `equals` / `hashCode`，没有 `toString`：`RecipeTable`、`MachineCasing`、`MachineIo`、`EnergyInput`、`EnergyOutput`、`RecipeMachine`、`BehaviorMachine`、`FuelGenerator`、`FuelOutput`、`LayoutPart`、`LayoutCell`、`LayoutRepeat`、`StructureLayout`、`PartSlots`、`PartTanks`、`IoTarget`。API 里手写 `toString` 的是 `CellMatch` 和 `AddonRecipes.Line`。record 自带 `toString`。

`FuelOutput` 用 `front`、`back`、`behind` 三个字段保存位置，`placement()` 再推回 `Placement`。`RecipeMachine.basic()` / `largeRecipe()`、`FuelGenerator.single()` / `multi()`、`RecipeTable.machine()` / `fuel()` 都是成对的 boolean getter。

`BehaviorMachine` 第 135–170 行的 `energyMask`、`auxiliaryMask`、`energyOutMask` 和 `faceMask` 是四段几乎相同的按位或。`EVERY_FACE = (1 << 6) - 1`（第 21 行）。`MachineFace` 没有对应的 `mask` 方法。

`PartClientView`、`BehaviorMachineClientView`、`BehaviorState` 各自声明 `getInt`、`getLong`、`getBoolean`、`getString`。

`BehaviorMachine` 第 22–44 行有 22 个 `public static final int`：8 个尺寸和间距，14 个 UV。它们是编译期常量，javac 会把字面量内联进 addon 字节码。`SelectorTags.MIN` / `MAX` 和 `FuelGenerator.MAX_BEHIND`、`MAX_AMPERES`、`MAX_TANK` 也是。`GregTechAddons` 第 14–16 行专门解释了为什么 `API_VERSION` 不能写成字面量常量。

`MachineBehavior` 上的 `getCrucibleMaterial`、`getCrucibleTemperature`、`getCrucibleContentAmount` 是这个接口里仅有的 `get` 前缀。旁边的 `CrucibleContent` 已经是一份快照。熔点在 `MaterialDeclaration` 里是 `int`；`CrucibleContent.temperature`、`HeatHandler.temperatureK`、`CrucibleMold.fillMold` 的温度参数是 `long`。

`HeatHandler` 的类注释写明正数加热、负数冷却。`extractHeat` 的方法注释只说移除 `signedHU`。`extractHeat(-5, …)` 要回到类注释才能确定含义。

`EnergyBlock.getEnergyStored()` 是 JavaBean 名字，同仓库的声明类型用 `stored()`。`MachineBehaviorContext` 里既有 `progress(float)` 这种同名写入，也有 `setToggle`。API 源码里使用 `javax.annotation.Nullable` 的还有 `SensorContext`、`PartContext`、`BehaviorMachineClient`、`LayoutPartClient`、`BoundPart`，不只 `MachineControl`。`CompressionSensorSource.gibblValue` 没有单位说明。`CellMatch`、`EnergyInput`、`GregTechAddons`、`StackComposition` 在方法体内写了 `java.util.List`、`Locale`、`Map`、`ArrayList` 的全名。

零散的实现和报错：

- `RecipeKeptInputs` 第 32 行，`count == 0` 的两个分支都是 `new boolean[0]`。`count == 0` 且索引数组非空时，消息是 “outside 0..-1”。
- `AddonFailures.of` 遇到 `Error` 直接抛出，其余情况返回 `IllegalStateException`。Javadoc 写了这一点。调用形式是 `throw AddonFailures.of(...)`，`Error` 在 `of` 内部离开，返回值那次 `throw` 不会执行。
- `MachineIo` 的四个面 getter 对 `distinct` 已经返回的不可变列表再调用 `List.copyOf`。
- `AddonLoaderServices` 每次 `map(String)` 都 `new MapView`。`AddonRecipes.map` 把这个对象返回给调用方。
- 每个 dynamic provider 被询问前，`itemSnapshot` / `fluidSnapshot` 都会复制全部栈。`AddonRecipes.Dynamic` 的合同要求这次隔离。
- `StructureLayout` 第 79 行的 “Duplicate structure cell” 没有坐标。`BehaviorMachine` 第 311 行的 “Invalid gui size” 没有实际宽高。第 327–328 行把物品超过 54、流体超过 8、开关超过 16 都报成 “has too many slots”。
- `PrefixDeclaration` 第 75–77 行把换行符和超长一起报成 “display text is too long”。第 113–114 行在材料列表为空时只提到 tool 或 smithable；`PrefixCondition.positive()` 还包括 `METAL`、`MOLTEN`、`ALLOY`。
- 物品输入为 null 时按数量 0 处理。`RecipeKeptInputs.requirePositiveCount` 的消息建议改用 `keptInputs`。

## 5. 工程卫生

API 源码里有 72 个文件把 `/** SPDX-License-Identifier: LGPL-3.0-or-later */` 放在真正的类注释前面，形成悬空文档注释。按“紧挨类型声明、正文以 SPDX 开头”统计，类文档第一句就是许可证标识的有 33 个文件。`GregTechAddon` 和 `AddonTools` 的类文档只有这一句。

`sensor` 包的文件头不统一。`ProgressSensorSource` 和 `CompressionSensorSource` 使用文件开头的 `/* GregTech-6 Team ... */`。`TemperatureSource` 仍是包声明之后的悬空 `/** SPDX */`。

`com.gregtech.gregtech.api` 在 `core/src/main` 下有 148 个 Java 文件，在 `multiversion/src/main` 下另有 86 个。它们与 SDK 两棵 api 树的相对路径交集是 0。`addon`、`material`、`energy`、`machine`、`recipe`、`sensor` 这六个包名同时出现在 SDK 和这两棵实现树里。addon 作者只看包名分不出哪些类型在 SDK 中。

`AddonPlatform` 的类注释写明：签名里出现加载器流体类型的方法放在 `AddonLoader`。两个门面的边界是签名约束。

`AddonTool` 的注释写着这是当前 addon 会询问的那一组工具，不是内部工具表的全部。`AddonTools.isMagnifyingGlass` 只转发 `AddonTool.MAGNIFYING_GLASS`。`AddonTools` 没有类文档。

## 本次没有做的事

没有提出补丁，没有把上述行为改成新的公开合同，也没有重跑 `AddonDeclarationTypeContracts` 或生命周期 JVM。`ItemStack` 的身份相等来自平台类型；本仓库的构造器复制和 `Arrays.equals` 是在这个前提下读出来的。
