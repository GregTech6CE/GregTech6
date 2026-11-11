# Logger

## 子原因

1.20.1 用 `GregTech.LOGGER`；1.21.1 用 `LogUtils.getLogger()`。

## 例子

### 修复前

```java
//? if >=1.21.1 {
private static final Logger LOGGER = LogUtils.getLogger();
//?} else {
private static final Logger LOGGER = GregTech.LOGGER;
//?}
```

### 修复后

```java
private static final Logger LOGGER = Mc.logger();
```

### 为什么这么修复

日志门面只影响日志来源名称，不影响业务行为。统一后调用点不再关心加载器。

### 其他可能的修复方式

- 调用点各自分支。日志调用点多，分支会扩散。
- 在节点侧各写一套 logger。会制造两套入口。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
