# FluidStack wire-format

## 子原因

`FluidStack#save` / `writeToNBT` 的 `//?` 分支不仅切换 provider 参数，也切换写出的 NBT 形状。读取旧存档或跨版本复制数据时，必须按形状判断。

## 例子

### `//?` 分支

```java
//? if >=1.21.1 {
tag.put("Fluid", fluid.save(lookup));                   // {id, amount, components}
//?} else {
tag.put("Fluid", fluid.writeToNBT(new CompoundTag())); // {FluidName, Amount, Tag}
//?}
```

### 修复前

```json5
{
  FluidName: "minecraft:water",
  Amount: 1000,
  Tag: { ... }
}
```

### 修复后

```json5
{
  id: "minecraft:water",
  amount: 1000,
  components: { ... }
}
```

### 为什么这么修复

1.21.1 的 `FluidStack` codec 只写 `id`、`amount` 与 `components`；旧 `FluidName`、`Amount` 与 `Tag` 不再被识别。读取侧由 `NbtCompat.readFluid` 处理。

### 其他可能的修复方式

- 同时保留两种形状。会增加写出分支，且容易写出混合形状。
- 在存档加载时统一重写。需要遍历所有嵌套结构，成本高。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
