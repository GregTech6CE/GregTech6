# registryAccess 参数

## 子原因

1.21.1 的 `setItemFilter` / `setFluidFilter` 需要 `HolderLookup.Provider`，1.20.1 不需要，调用点因此多出 `level.registryAccess()` 参数分支。但 `CoverUtilityBehaviors` 在 1.20.1 侧已有带 provider 的**委托适配器**（转调无 provider 真实现、忽略 lookup，见 [delegate-overload.md](delegate-overload.md) 的共享调用者适配器），且 `level.registryAccess()` 两版都存在——3 参调用在两节点都能编译，分支可以**消除**而非保留。

## 例子

### 修复前

`UtilityCoverInteraction` 把整块守卫包进分支，实际两侧只差一个 provider 参数：

```java
//? if >=1.21.1 {
/*if(!host.consumesTool(held)){
    if(!level.isClientSide){...CoverUtilityBehaviors.setFluidFilter(installed,sample(held),level.registryAccess());host.panels().changed();}
*///?} else {
if(!host.consumesTool(held)){
    if(!level.isClientSide){...CoverUtilityBehaviors.setFluidFilter(installed,sample(held));host.panels().changed();}
//?}
```

### 修复后

共享守卫结构，统一调 3 参，分支消失：

```java
if (!host.consumesTool(held)) {
    if (!level.isClientSide) {
        CoverUtilityBehaviors.setFluidFilter(installed, sample(held), level.registryAccess());
        host.panels().changed();
    }
}
```

### 为什么这么修复

1.20.1 的 3 参适配器忽略 lookup，`level.registryAccess()` 两版都有，故 3 参调用两节点通用，调用点无需分支。适配器本身按平台签名例外保留（[delegate-overload.md](delegate-overload.md)）；它的作用正是让共享调用点消掉 provider 分支。封面工具守卫行为见[工具交互行为](../../../decisions/parity/tool-interactions.md)。

### 其他可能的修复方式

- 保留调用点 provider 分支：适配器已使 3 参调用两节点通用，保留分支冗余（本文件旧版即此误）。
- 无适配器时先补 1.20 适配器再消分支；若适配器有共享调用者以外的代价，则保留最小 provider 分支并按平台例外写明。
- 在共享层包 `RegistryContext` 门面：需调用点迁移，成本更高。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)