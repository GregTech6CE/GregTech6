# 物品类依赖

## 子原因

`GTMachineBlockItem` 原先把 `MachineSpec` / `MachineTextures` 传给物品，只为工具提示服务；1.21.1 又用 `FluidTransportBlockItem` / `ItemPipeBlockItem` 两个节点物品类。物品类因此无法直接共享。

## 例子

### 修复前

1.20.1 的储罐/管道物品构造：

```java
bare -> new GTMachineBlockItem(bare, new Item.Properties().stacksTo(16),
        new MachineSpec(id, material.getLocalName(), material.getColor(), 0, 0,
                MachineTextures.BURNING_SOLID, hardness, blastResistance));
```

1.21.1 的同类物品：

```java
bare -> new FluidTransportBlockItem(bare, new Item.Properties().stacksTo(16));
```

### 修复后

共享 `GTMachineBlockItem` 只接收方块和物品属性：

```java
bare -> new GTMachineBlockItem(bare, new Item.Properties().stacksTo(16));
```

工具提示从方块自身读取：

```java
if (getBlock() instanceof TankBlock tank) TankTooltips.appendTank(tank.spec(), stack, tooltip);
if (getBlock() instanceof FluidPipeBlock pipe) TankTooltips.appendPipe(pipe.spec(), tooltip);
if (getBlock() instanceof ItemPipeBlock pipe) TankTooltips.appendItemPipe(pipe.spec(), tooltip);
```

`appendHoverText` 的 `TooltipContext` ↔ `Level` 签名，以及 `getMaxStackSize` 的 `BLOCK_ENTITY_DATA` ↔ `BlockEntityTag` 读取，是物品类保留的 `//?`；`FluidTransportBlockItem` / `ItemPipeBlockItem` 不再需要。

### 为什么这么修复

物品类只需要方块类型和堆叠上限；`MachineSpec` 的 tint/hardness 在方块上已有。去掉 `MachineSpec` 后，`MachineTextures` 这个节点依赖也消失，物品类可以整体移到共享源码集。

### 其他可能的修复方式

- 保留 `MachineSpec`，在共享层补一个 tint 提供者。多一层间接，且仍要传节点纹理常量。
- 保留两个节点物品类，只在调用点切换。物品类本身重复。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
