# ItemStack wire-format

## 子原因

`ItemStack#save` / `parseOptional` 的 `//?` 分支不仅切换 provider 参数，也切换写出的 NBT 形状。读取旧存档或跨版本复制数据时，必须按形状判断。

## 例子

### `//?` 分支

```java
//? if >=1.21.1 {
tag.put("item", stack.save(lookup));            // {id, count, components}
//?} else {
tag.put("item", stack.save(new CompoundTag())); // {id, Count, tag}
//?}
```

### 修复前

```json5
{
  id: "minecraft:diamond_pickaxe",
  Count: 1b,
  tag: { Damage: 12, "GT.ToolStats": { ... } }
}
```

### 修复后

```json5
{
  id: "minecraft:diamond_pickaxe",
  count: 1,
  components: {
    "minecraft:damage": 12,
    "minecraft:custom_data": { "GT.ToolStats": { ... } }
  }
}
```

### 为什么这么修复

1.21.1 的 `ItemStack.CODEC` 只写 `count` 与 `components`；旧 `Count` 与 `tag` 不再被识别。读取侧由 `NbtCompat.readItem` 把旧 `tag` 搬进 `CUSTOM_DATA`。

### 其他可能的修复方式

- 同时保留两种形状，读取时按存在性判断。会增加写出分支，且容易写出混合形状。
- 在存档加载时统一重写。需要遍历所有嵌套结构，成本高。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
