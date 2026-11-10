# 节点目录迁移

## 子原因

同一内容家族只在 1.21.1 节点有目录，1.20.1 由另一个共享类重复注册。`GTManualStations` 原先只在 `minecraft/versions/1.21.1-neoforge/...`，1.20.1 的手动工作台由 `GTToolBlocks.registerAll` 注册。

## 例子

### 修复前

1.21.1 的 `GTManualStations` 在节点源码集：

```text
minecraft/versions/1.21.1-neoforge/src/main/java/gregtech/content/registry/GTManualStations.java
```

1.20.1 的 `GTToolBlocks.registerAll` 同时注册手动工作台、铁砧、绳索、脚手架和工具方块。

### 修复后

`GTManualStations` 移到共享源码集：

```text
minecraft/src/main/java/gregtech/content/registry/GTManualStations.java
```

两个节点的内容入口都调用同一个目录：

```java
// ForgeContent.blocks
GTManualStations.initialize(registrar);

// NeoContent.blocks
GTManualStations.initialize(registrar);
```

`GTToolBlocks` 只保留炸药、便携容器和流体附件；手动工作台、铁砧、绳索、脚手架等由 `GTManualStations` 负责，不再需要 `AnvilDefinitions` 或 `DungeonBindings.scaffold` 这类转发。

### 为什么这么修复

目录本身只依赖共享类（`ContentRegistrar`、`RegisteredBlock`、`BlockBehaviour`），没有节点 API。放在节点源码集只是历史位置；移到共享后，1.20.1 不再维护第二套手动工作台注册。

### 其他可能的修复方式

- 保留节点目录，在 1.20.1 复制一份。两份列表会逐渐漂移。
- 只把调用点改成共享目录，目录仍留在节点。1.21.1 能编译，1.20.1 无法引用。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
