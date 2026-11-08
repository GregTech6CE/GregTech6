# 无 level 路径

## 子原因

`getUpdateTag`、客户端 tooltip、`GTToolEnchantments` 等路径没有可用的 `level`，不能从 `level.registryAccess()` 取 provider，也不能传 `null`。

## 例子

### 修复前

```java
public static boolean apply(ItemStack stack) {
    var level = Minecraft.getInstance().level;
    return apply(stack, level.registryAccess());
}
```

### 修复后

```java
public static HolderLookup.Provider currentLookup() {
    if (EffectiveSide.get().isClient()) return clientLookup.get();
    var server = ServerLifecycleHooks.getCurrentServer();
    return server == null ? null : server.registryAccess();
}
```

调用方在 provider 为 `null` 时跳过需要注册表的操作。目标是把这类逻辑收进统一的 `RegistryContext`（待建），而不是每个类各维护一个静态 `Supplier`。

### 为什么这么修复

客户端与服务端的注册表快照不同；无 level 时只能按侧取。`RegistryContext` 应统一处理“客户端连接 → 服务端 → 内置注册表”的兜底，并永不返回 `null`。

### 其他可能的修复方式

- 在无 level 路径传 `null`。1.21.1 会 NPE。
- 缓存客户端 provider。连接切换或重载后过期。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
