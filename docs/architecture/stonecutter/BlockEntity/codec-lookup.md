# codecLookup

## 子原因

持久化逻辑较长时，每个调用点都写 `//?` 会让分支数量随字段增长。用一个局部别名把 provider 绑定在签名分支里，共享方法体只引用别名。

## 例子

### 修复前

```java
//? if >=1.21.1 {
tag.put(NBT_INVENTORY, HandlerNbt.save(inventory.items(), lookup));
tag.put("gt.potion_input", HandlerNbt.save(inventory.potionInput(), lookup));
//?} else {
tag.put(NBT_INVENTORY, HandlerNbt.save(inventory.items(), null));
tag.put("gt.potion_input", HandlerNbt.save(inventory.potionInput(), null));
//?}
```

### 修复后

```java
@Override

//? if >=1.21.1 {
public void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    HolderLookup.Provider codecLookup = lookup;
//?} else {
public void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    HolderLookup.Provider codecLookup = null;
//?}

    tag.put(NBT_INVENTORY, HandlerNbt.save(inventory.items(), codecLookup));
    tag.put("gt.potion_input", HandlerNbt.save(inventory.potionInput(), codecLookup));
}
```

### 为什么这么修复

分支数量等于 override 数量，不随持久化字段增长。`codecLookup` 只在签名分支里赋值，共享方法体不需要知道版本。

### 其他可能的修复方式

- 每个调用点各自分支。字段少时可行，字段多时分支数量线性增长。
- 把 provider 存成字段。provider 随世界/连接变化，字段会在重载后过期。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
