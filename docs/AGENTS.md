# 文档维护

遵守[根指南](../AGENTS.md)。文档位置按[文件与产物归属](development/repository-layout.md#文档归属)选择；本目录只补充写作与维护要求。

- 新文档从所属目录 README 链接；开头写清用途，必要时给出阅读顺序。包文档讲职责、入口、边界和验收，避免逐类抄源码。
- 同一规则只在所属专题维护，其他页面直接链接到该节。
- Stonecutter 分类 README 与子样例按 [Stonecutter 文档模板](architecture/stonecutter/templates.md) 编写；新增或修改后同步更新 `categories.md` 分类总览。
- 使用相对链接，引用现存文件；命令说明执行目录。新增或修改命令、参数、任务和路径时对照 Gradle 配置核实。
- 区分当前实现、约定、计划和未验证范围；不编造工具、示例、兼容保证或证据。模板与实际报告分开。
- 交付前按[验证与验收](development/validation/README.md)检查文档一致性；若修改忽略规则，还需确认临时产物被忽略，文档、资源与 Wrapper 未被误忽略。
