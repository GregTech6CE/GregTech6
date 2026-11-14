# tools

本目录存放可复用、随代码演进的开发、构建与检查工具。各子目录包含对应脚本、随附数据及说明该工具依赖、输入、输出与用法的 `README.md`。

| 目录 | 用途 |
| --- | --- |
| [buildtools/](buildtools/README.md) | 将 `core` 中的 1.20.1 配方与物品标签树转换为 1.21.1 原生资源。 |
| [comments/](comments/README.md) | 检查 Java 注释中的行号引用、历史叙述与上游符号，并验证纯注释改动。 |
| [gui/](gui/README.md) | 检查 GUI 模板、别名、图标引用并生成静态预览。 |
| [recipes/](recipes/README.md) | 审计数据包配方的形状、标签、电路家族、歧义与 NeoForge 覆盖。 |
| [resources/](resources/README.md) | 核对模板化发行包的模型、方块状态，审计 PNG 块结构、CRC 与透明像素覆盖。 |
| [validation/](validation/README.md) | 读取验证路由表，计算改动对应的检查并校验文档闭合。 |
