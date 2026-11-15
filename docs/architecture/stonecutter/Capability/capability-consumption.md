# 能力消费

## 子原因

共享代码要查询相邻方块实体的物品/流体/能量能力，但两个加载器的查询 API 不同：NeoForge 用 `Level.getCapability(BlockCapability, BlockPos, Direction)` 直接返回能力对象（可能为 `null`），Forge 用 `BlockEntity.getCapability(Capability, Direction)` 返回 `LazyOptional`，需 `resolve()`/`orElse(null)` 解包。每个消费点各自分支会重复同一套解包逻辑；需要缓存邻居视图时，缓存类型也不同（NeoForge `BlockCapabilityCache` ↔ Forge `LazyOptional`）。

## 例子

### 修复前

消费点内联分支查询邻居能力（一次性查询至今仍保留此形态，见 `LogisticsItemRouter`）：

```java
//? if >=1.21.1 {
/*IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK,
        hostPosition, (Direction)null);
*///?} else {
IItemHandler handler = hostBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
        .resolve().orElse(null);
//?}
```

### 修复后

需要反复查询同一邻居的消费方（如 `MachineAutoIO` 的自动输入/输出）持有共享门面 `AdjacentCapabilityCache<T>`，调用点不再分支：

```java
private final AdjacentCapabilityCache<IItemHandler> adjacentItems;
// 构造器只分支 capability 常量：Capabilities.ItemHandler.BLOCK ↔ ForgeCapabilities.ITEM_HANDLER

IItemHandler source = adjacentItems.get(be, absolute.getOpposite());
```

平台差异收进门面内部，`get` 的缓存与解包分支保留：

```java
//? if >=1.21.1 {
/*return entry.cached == null ? null : entry.cached.getCapability();
*///?} else {
// An empty/invalid LazyOptional must be queried again: capabilities may appear later.
if (!entry.cached.isPresent()) entry.cached = neighbour.getCapability(capability, receivingSide);
return entry.cached.orElse(null);
//?}
```

### 为什么这么修复

- 查询 API 与缓存类型是平台 API 运行时差异，不能共享；门面把差异吸收在一处，多个调用点（`MachineAutoIO` 的 5 处 `get`）共用同一条无分支路径。
- 邻居视图的失效规则（owner/邻居移除、方块状态变化、level 变化）是共享逻辑，留在门面统一维护；Forge 侧额外处理 `LazyOptional` 失效后重新查询。
- 门面内保留的分支满足平台 API 例外四条件：API 运行时差异（`BlockCapabilityCache` ↔ `LazyOptional`）、无法用更上层门面吸收（缓存类型本身不同）、已排除自身注册/包装重复（调用点不再分支）、分支处写明原因。验证时两节点分别检查邻居能力出现/消失与方块状态变化后的取值。
- 一次性查询（无缓存需求）可保留内联分支，或走 `EnergyCompat`/`NativeItemInventories` 这类共享静态查询助手；不强制所有消费点都进门面。

### 其他可能的修复方式

- 每个消费点各自内联分支。解包逻辑随消费点重复，缓存失效规则无法统一。
- 把查询也做成共享静态方法但不缓存。反复查询同一邻居时重复创建 `LazyOptional`/`BlockCapabilityCache`，失去失效订阅。

相关链接：[capability-exposure.md](capability-exposure.md)（暴露侧）、[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)