# 注册表快照

## 子原因

同一个注册表 id 在不同 `RegistryAccess` 快照里是不同的 `Holder.Reference` 实例。`ItemEnchantments` 按 holder 身份比较，跨快照混用会查不到、加不上。

## 例子

### 修复前

```java
var existing = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
var enchants = new ItemEnchantments.Mutable(existing);
enchants.upgrade(holderFromAnotherSnapshot, level);
```

### 修复后

```java
var registry = lookup.lookupOrThrow(Registries.ENCHANTMENT);
var current = registry.getOrThrow(currentKey);
enchants.removeIf(holder -> holder == previous);
enchants.upgrade(current, level);
```

`GTToolEnchantments` 的 rebind 循环：取旧 holder 的 key，用当前注册表重新解析，再替换。

### 为什么这么修复

物品副本可能跨世界或从集成服务端目录复制到客户端。旧 holder 属于另一个快照，`ItemEnchantments` 按身份比较时找不到对应条目。用当前快照重新解析可以把身份对齐。

### 其他可能的修复方式

- 只按 key 编码，不保留 holder。编码能过，但运行时附魔查询会失效。
- 缓存旧 holder。快照切换后过期。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
