# Provider 本身

## 子原因

Codec 需要 `RegistryOps` 才能把 `Holder<Item>`、`Holder<Enchantment>` 等解析成注册表条目；`RegistryOps` 由 `HolderLookup.Provider` 构造。provider 随世界与连接变化，不能当作全局常量。

## 例子

### 修复前

```java
private static HolderLookup.Provider LOOKUP;

public static void onServerStarted(ServerStartedEvent event) {
    LOOKUP = event.getServer().registryAccess();
}
```

### 修复后

```java
public static Tag saveItem(ItemStack stack, HolderLookup.Provider lookup) {
//? if >=1.21.1 {
    return stack.save(provider(lookup));
//?} else {
    return stack.save(new CompoundTag());
//?}
}
```

provider 由调用方传入，门面只在 1.21.1 分支断言非空。

### 为什么这么修复

- 服务端 provider 随世界加载构建，客户端 provider 随连接同步；静态字段会在重载或切换世界后指向过期快照。
- 1.21.1 的 `createSerializationContext(null)` 直接 NPE；`ItemStack.CODEC` 的 `id` 字段使用 `RegistryFixedCodec`，对 `RegistryOps` 是无条件要求。
- `RegistryAccess.EMPTY` 不能兜底：`lookup(Registries.ITEM)` 返回空，物品栈仍然无法编码；静态注册表兜底应使用 `RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)`。

### 其他可能的修复方式

- 在方法内读 `level.registryAccess()`。`loadAdditional` 可能先于 `setLevel`，`level` 为 `null`。
- 用 `ThreadLocal` 传递。入口仍然要有人设置，且客户端/服务端、数据生成、异步序列化会破。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
