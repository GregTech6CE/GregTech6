# logger 与模型

## 子原因

日志来源与模型资源位置的构造在两个版本上不同；调用点各自分支会让同一段逻辑出现多份。

## 例子

### 修复前（logger）

```java
//? if >=1.21.1 {
private static final Logger LOGGER = LogUtils.getLogger();
//?} else {
private static final Logger LOGGER = GregTech.LOGGER;
//?}
```

### 修复后（logger）

```java
private static final Logger LOGGER = Mc.logger();
```

相关提交：`dce56139`。

### 修复前（模型资源位置）

`ModelResourceLocation` 的构造在两个版本上不同，且分支形态多（独立构造、inventory 变体、事件 API）。

### 修复后（模型资源位置）

集中到模型门面（如 `BlockItemClientModels`）或共享构造 helper。相关提交：`062957aa`。

### 为什么这么修复

日志门面只影响日志来源名称，不影响业务行为；模型资源位置集中后，调用点不再关心版本。迁移模型时先核对模型路径与 inventory 变体，避免只改构造而改变查找键。

### 其他可能的修复方式

- 调用点各自分支。模型分支形态多，容易漏 inventory 变体。
- 在节点侧各写一套模型 helper。会制造两套入口。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
