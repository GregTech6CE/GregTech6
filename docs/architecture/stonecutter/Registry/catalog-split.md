# 复合目录拆分

## 子原因

1.20.1 的 `GTToolBlocks.registerAll` 同时注册手动工作台、铁砧、绳索、脚手架、工具方块、炸药、便携容器和流体附件；1.21.1 已把这些内容拆到 `GTManualStations` 与 `GTToolBlocks` 两个目录。一个类承担多个内容家族时，两个版本的列表容易漂移。

## 例子

### 修复前

1.20.1 的 `GTToolBlocks.registerAll` 里同时出现：

```java
add(registrar, "mortar_block", ManualToolBlockEntity.Kind.MORTAR, ...);
CRANK = registrar.block(REGISTRY_NAMESPACE, "crank", ...);
DYNAMITE = registrar.block(REGISTRY_NAMESPACE, "dynamite", ...);
```

### 修复后

职责按目录拆开：

| 目录 | 负责 |
| --- | --- |
| `GTManualStations` | 手动工作台、铁砧、绳索、脚手架、坩埚/盆、硬币模具、除尘漏斗、植物盆、树液袋、高级按钮、曲柄 |
| `GTToolBlocks` | 炸药、便携容器、流体附件 |
| `GTBumbleBlocks` | 蜂巢与养蜂机（`initialize` = `initializeHives` + `initializeApiaries`） |

1.20.1 的入口改为：

```java
GTBumbleBlocks.initialize(registrar);
GTManualStations.initialize(registrar);
GTToolBlocks.registerAll(registrar);
```

### 为什么这么修复

每个目录只负责一个内容家族，注册 ID、物品类和 BE 类型都跟着各自家族走。拆分后 `GTManualStations` 可以整体移到共享源码集，`GTToolBlocks` 只剩炸药/容器/附件，两个节点的列表可以逐项对齐。

### 其他可能的修复方式

- 保留一个复合目录，用注释分节。两个版本的列表仍可能漂移。
- 按方块类型拆成更多目录。目录过细会增加调用点，按内容家族拆即可。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
