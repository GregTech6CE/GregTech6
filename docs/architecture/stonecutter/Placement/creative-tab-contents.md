# 创造栏内容源

## 子原因

1.20.1 的 `CreativeTabBootstrap` 用 `populateContents` 和一组家族专用方法手工装配每个创造栏；1.21.1 的 `populateTabs` 先处理前缀 tab，其余家族页交给 `OriginCreativeContents.contents(family)`。同一批物品因此有两套分类和排序逻辑。

## 例子

### 修复前

1.20.1 的工具页手工列出：

```java
for (var entry : GTToolBlocks.manual()) { ... }
for (var entry : GTMiscBlocks.all()) { ... }
for (var entry : GTWoods.all()) { ... }
for (var entry : GTElectricItems.all()) { ... }
```

1.21.1 的工具页：

```java
for (var stack : OriginCreativeContents.contents(event.getTabKey().location().getPath()))
    event.accept(stack.copyWithCount(1));
```

### 修复后

两个节点都走同一入口：

```java
@SubscribeEvent
public static void populateTabs(BuildCreativeModeTabContentsEvent event) {
    if (!event.getTabKey().location().getNamespace().equals(REGISTRY_NAMESPACE)) return;
    for (var prefix : PrefixRegistry.all()) { ... }
    for (var prefix : BlockPrefixRegistry.all()) { ... }
    for (var stack : OriginCreativeContents.contents(event.getTabKey().location().getPath()))
        event.accept(stack.copyWithCount(1));
}
```

共享入口只保留前缀 tab 与 `OriginCreativeContents` 两条路径；`GTCreativeTabs` 只保留前缀 tab 与家族页需要的字段，旧装配专用的 tab 字段、`*TabKey()` 和 icon 方法不再需要。

### 为什么这么修复

`OriginCreativeContents` 已经按物品 ID/类型分类并缓存页面，两个节点使用同一份内容源后，创造栏内容与顺序不会各自漂移。1.20.1 的可见内容会按 1.21 基线变化，这是统一方针允许的行为变更。

### 其他可能的修复方式

- 保留两套装配，用脚本对比输出。仍然需要人工核对差异。
- 只把 1.20.1 的家族列表换成共享列表，`populateContents` 仍保留。分支和死代码仍在。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
