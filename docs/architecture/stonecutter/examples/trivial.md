# Trivial 分支与常见折叠

返回[示例索引](README.md)。本文汇总历史清理中反复出现的低风险 `//?` 形态，以及它们各自应折叠、应收进门面还是应保留。

## 问题来源

共享源码里大量分支并不是真实语义差异，而是同一段代码在两侧各写一遍：两侧完全相同、一侧为空、一侧只有 import、同条件拆成两个单侧块、只差一个常量或命名空间。这些分支不改变行为，只增加维护面。

历史扫描的规模：

| 形态 | 清理前数量 | 说明 |
| --- | --- | --- |
| 两侧完全相同的分支 | 29 | 清理前扫描 |
| 空块 | 588 | 清理前扫描 |
| 单侧 import 分支 | 80+ | 清理前扫描 |
| 同义候选（宽口径） | 889 | 清理前扫描，含误报 |

数量是清理前的快照，用于说明形态分布，不是当前待办数。

## 建议方案

按代价从低到高处理：

1. **同义分支**：两侧代码相同，删除分支，保留一份。
2. **空块**：删除空侧与整个分支。
3. **单侧 import**：把 import 移到分支外；只有一侧需要的 import 才保留。
4. **拆分重复**：同条件两个单侧块、非空体相同时，合并成一个块。
5. **注册表/id/logger/模型位置**：收进 `Mc`、`StackCompat`、共享常量或模型门面。
6. **包装类**：删除只转发的节点类，调用点改用共享门面。
7. **静态谓词/列表**：先核对原版消费路径，再决定是否合并。

判断标准只有一条：**分支是否真的消失**。只把分支内的一行调用换成门面、分支本身原样保留，不算有效改动。

## 例子与分析

### 同义分支

清理前，扳手判断在两个分支里完全相同：

```java
//? if >=1.21.1 {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?} else {
if (GTToolHelper.isMachineWrench(tool)) return true;
//?}
```

清理后只保留一份。相关提交：`2e5627aa`、`1b350bb8`、`d90383b2`。

分析：两侧调用的是同一个共享门面方法，分支没有承载任何版本差异。这类分支可以直接删除。

### 空块

```java
//? if >=1.21.1 {
//?} else {
charge = spec().capacity();
//?}
```

清理后只保留 else 侧的内容；如果另一侧确实不需要任何代码，删除整个分支。空块数量最多，但风险最低。

分析：空块通常来自“先加分支、后删内容”的中间状态。删除前确认另一侧不是有意的占位。

### 单侧 import

```java
//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
```

如果两个版本都需要该类型，import 应放在分支外；如果只有一个版本需要，保留分支。

相关提交：`e15ae180`、`038dbba7`。

分析：import 不是行为差异。大多数单侧 import 只是历史上把整段 import 放进了分支。

### 拆分重复

同一个条件被拆成两个单侧块，非空体相同：

```java
//? if >=1.21.1 {
methodA();
//?}

//? if >=1.21.1 {
methodA();
//?}
```

合并成一个块。相关提交：`e15ae180`、`60bec479`。

分析：拆分通常来自两次独立编辑。合并后分支数量减半。

### 注册表查找

```java
//? if >=1.21.1 {
var item = BuiltInRegistries.ITEM.get(id);
//?} else {
var item = ForgeRegistries.ITEMS.getValue(id);
//?}
```

收进 `Mc.item(id)` / `Mc.items()` 等辅助方法。相关提交：`73853464`、`14a22508`。

分析：两个版本都能查到同一个注册表，差异只在 API 名称。门面方法同名同义，调用点可以共享。

### id 与命名空间

```java
//? if >=1.21.1 {
var id = ResourceLocation.fromNamespaceAndPath("gt6ce", path);
//?} else {
var id = GregTech.id(path);
//?}
```

收进共享的 `ResourceLocation` 辅助方法或 `GregTechIdentity` 常量。相关提交：`1e942f72`、`8923ebba`、`d3647e54`。

分析：`GregTech.id` 与 `ResourceLocation.fromNamespaceAndPath` 的语义相同；`GregTech.MODID` / `NAMESPACE` 与 `GregTechIdentity.MOD_ID` / `REGISTRY_NAMESPACE` 也是同一组常量。注意大小写：共享 helper 可能统一 `toLowerCase()`，迁移前核对调用点是否依赖原始大小写。

### logger

```java
//? if >=1.21.1 {
private static final Logger LOGGER = LogUtils.getLogger();
//?} else {
private static final Logger LOGGER = GregTech.LOGGER;
//?}
```

收进 `Mc.logger()`。相关提交：`dce56139`。

分析：日志门面只影响日志来源名称，不影响业务行为。统一后调用点不再关心加载器。

### 模型资源位置

`ModelResourceLocation` 的构造在两个版本上不同，且分支形态多（独立构造、inventory 变体、事件 API）。相关提交：`062957aa`。

分析：这类差异不适合在调用点逐个折叠，应集中到模型门面（如 `BlockItemClientModels`）或共享构造 helper。迁移时先核对模型路径与 inventory 变体，避免只改构造而改变查找键。

### 身份比较

```java
//? if >=1.21.1 {
return ItemStack.isSameItemSameComponents(first, second);
//?} else {
return ItemStack.isSameItemSameTags(first, second);
//?}
```

收进 `StackCompat.sameItem` / `sameFluid`。相关提交：`23790c26`、`9174324d`、`aced0f71`。

分析：这是真实语义差异的收敛，不是简单折叠。`isSameItemSameTags` 与 `isSameItemSameComponents` 对“相同数据”的定义不同，门面必须保持两侧同义。

### 包装类

历史清理中已删除的节点侧包装类包括 `NeoToolBindings`、`FluidPipeToolInteractions`、`WireToolInteractions`、`AxleToolInteractions`；它们只转发共享代码，删除后调用点改用 `GTToolHelper` 等共享门面。

分析：包装类会制造“两套入口”。删除前先确认调用点已全部迁移，否则节点编译会断。

### 静态谓词与列表

`GTToolType.byId("crowbar")` 与 `GTToolType.CROWBAR`、`PanelCoverHost.consumesTool` 与静态列表，属于“两版各有一套判断”。先查原版实际消费路径，再决定合并到共享谓词还是保留节点实现。

分析：这类差异可能承载真实行为，不能只按“代码看起来一样”折叠。

### 重复注册与重复字段

同一注册项在两个版本各写一遍，或消费者各自分支取注册表时，先确认注册归属：能共享的注册表走 `ContentRegistrar`，只能节点注册的保留在节点实现。不要在两版各维护一份同一内容的注册列表。

分析：重复注册通常不会立刻报错，但会让两版内容逐渐漂移。收敛前先确认注册 ID 与顺序不变。

### 标签目录单复数

1.20.1 使用 `tags/items`，1.21.1 使用 `tags/item`；方块与流体同理（`blocks`/`block`、`fluids`/`fluid`）。相关提交：`c751063`。

分析：这是资源路径差异，不是 Java 分支。折叠时以节点实际读取的目录为准，不能把一边的 JSON 路径直接拷给另一边。

### 语义案例

以下案例看起来像同义分支，实际承载行为，不能按代码相似度折叠：

- `craftKind`：按物品基础 `toolType()` 计算合成损耗，不按口袋工具当前模式；`isTool` 守卫排除无 `GT.ToolStats` 的堆叠。
- `plunger`：原版从输出罐、输入罐依次抽 1000 mB；CE 补进 `useMachineTool`，用 `GarbageData` 丢弃。
- `igniter`：物品级行为已存在，但 `useRemainder` 的 GUI 会吞掉；对宿主会消费的点火器返回 `PASS`。
- 封面工具守卫：静态列表跨宿主近似，漏 `igniter`/`plunger`、多 `soldering_iron`；改为 `PanelCoverHost.consumesTool`，由宿主按实际工具动作覆盖。

## 常见陷阱

- **只换调用不删分支**：把分支内的一行换成门面，分支仍在，`//?` 数量不变。要么删除分支，要么记录为遗留项。
- **两侧看似相同、实际语义不同**：`isTool` 与 `isInteractionTool`、`byId` 与枚举常量、`isBound` 与 `isPresent` 都可能不是同义。先核对消费路径。
- **静态列表漏项**：合并两版列表时逐项核对，不要取交集或并集了事。
- **大小写与命名空间**：共享 helper 可能统一小写；迁移前确认调用点是否依赖原始大小写。
- **活动节点**：当前活动节点是 `1.20.1-forge`，非活动分支被注释包裹；折叠后检查失败节点的生成源码。
