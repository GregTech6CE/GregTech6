# 同步 tag

## 例子

### 修复前

```java
//? if >=1.21.1 {
@Override
public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
    return saveWithoutMetadata(lookup);
}

@Override
public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
    loadAdditional(tag, lookup);
}
//?} else {
@Override
public CompoundTag getUpdateTag() {
    return saveWithoutMetadata();
}

@Override
public void handleUpdateTag(CompoundTag tag) {
    load(tag);
}
//?}
```

### 修复后

签名分支保留，方法体只调用共享的 `saveWithoutMetadata` / `loadAdditional`。`NbtCompat.saveWithoutMetadata` 已把 provider 差异收在门面内部。

### 为什么这么修复

`getUpdateTag` 是网络同步入口，provider 来自区块包上下文或 `level.registryAccess()`。与存档路径一样，不能在方法内缓存 provider。

### 其他可能的修复方式

- 在 `handleUpdateTag` 里重新解析。会多一次编解码，且客户端 provider 可能与服务端快照不同。
- 不同步自定义数据，客户端按需请求。改变协议，超出分支折叠范围。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
