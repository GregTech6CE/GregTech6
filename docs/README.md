# GT6CE 文档

本目录存放项目的开发指南、架构说明、跨模组兼容结论、长期决策与扩展文档。按当前任务选择对应入口即可，无需顺序通读。

## 目录索引

| 主题 | 入口 | 说明 |
| --- | --- | --- |
| 开发流程与规范 | [开发指南](development/README.md) | 环境搭建、文件归属、Java 注释、GUI 制作与验证验收 |
| 源码架构与跨版本 | [架构总览](architecture/README.md) | 源码分层、包职责边界、资源与 Stonecutter 跨版本维护 |
| 跨模组兼容与联动 | [兼容与联动](integration/README.md) | 联动内容、配方协调与冲突审计结论 |
| 长期设计决策 | [长期决策](decisions/README.md) | 材料系统设计、跨平台边界与原版 GT6 特性差异 |
| 附属与脚本开发 | [附属 API](ADDON_API.zh-CN.md) | 公开扩展契约、SDK 构建与 KubeJS 集成 |
| 项目路线与协作 | [v1.0 路线图](roadmap/v1.0.md) | 开发方针、版本里程碑与问题/PR/RFC 协作方式 |

## 文档维护

- **归属与链接**：新增文档放在对应专题目录下并从该目录的 `README.md` 链接；文件开头简明说明用途，必要时给出阅读顺序。
- **单一权威**：每条规则只在其所属专题维护，其他文档直接链接引用。包文档聚焦职责、入口、边界与验收，代码细节以源码为准。
- **格式与模板**：Stonecutter 分类 README 与子样例按 [Stonecutter 文档模板](architecture/stonecutter/templates.md) 编写，变更后同步更新 [分类总览](architecture/stonecutter/categories.md)。
- **事实核对**：使用相对路径链接现存文件，命令注明执行目录与当前 Gradle 配置一致；明确区分已实现行为、约定、规划与未验证范围。
- **交付验证**：文档变更后使用 `python tools/validation/route.py check` 检查路由与链接一致性；修改忽略规则时同步核对临时产物与正式输入文件的状态。
