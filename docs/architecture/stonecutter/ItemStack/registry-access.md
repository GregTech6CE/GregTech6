# registryAccess 参数

## 子原因

1.21.1 的 `setItemFilter` / `setFluidFilter` 需要 `HolderLookup.Provider`；1.20.1 不需要。调用点因此多一个 `level.registryAccess()` 参数分支。

## 例子

### 修复前

```java
//? if >=1.21.1 {
/*if(!host.consumesTool(held)){
    if(!level.isClientSide){if(item)CoverUtilityBehaviors.setItemFilter(installed,held,level.registryAccess());else CoverUtilityBehaviors.setFluidFilter(installed,sample(held),level.registryAccess());host.panels().changed();}
*///?} else {
if(!host.consumesTool(held)){
    if(!level.isClientSide){if(item)CoverUtilityBehaviors.setItemFilter(installed,held);else CoverUtilityBehaviors.setFluidFilter(installed,sample(held));host.panels().changed();}
//?}
```

### 修复后

保留最小的 provider 分支；`!host.consumesTool(held)` 守卫与过滤器行为共享：

```java
//? if >=1.21.1 {
/*if (!host.consumesTool(held)) {
    if (!level.isClientSide) {
        if (item) CoverUtilityBehaviors.setItemFilter(installed, held, level.registryAccess());
        else CoverUtilityBehaviors.setFluidFilter(installed, sample(held), level.registryAccess());
        host.panels().changed();
    }
*///?} else {
if (!host.consumesTool(held)) {
    if (!level.isClientSide) {
        if (item) CoverUtilityBehaviors.setItemFilter(installed, held);
        else CoverUtilityBehaviors.setFluidFilter(installed, sample(held));
        host.panels().changed();
    }
//?}
```

### 为什么这么修复

provider 参数是 1.21.1 数据组件 API 的要求，不是行为差异；分支只影响 `registryAccess()` 的传递。封面工具守卫本身见[工具交互行为](../../../decisions/parity/tool-interactions.md)。

### 其他可能的修复方式

- 在共享层包一个 `RegistryContext` 门面。需要调用点迁移。
- 把整个封面交互移到节点。会复制业务逻辑。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
