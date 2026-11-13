# Agent 工作指南

本指南适用于整个仓库，局部规范遵循对应目录下的 `README.md`。用户的明确要求具有最高优先级。文档以中文维护，代码标识符沿用项目现有英文命名。

## 工作方式

- **现场保护**：开工前运行 `git status --short`，完整保留已有改动、未跟踪文件与用户资产。
- **先查后改**：修改前先检索同类实现、调用方、注册入口与跨版本差异，基于代码事实做局部改动。
- **遵循风格与真实输入**：沿用所在文件的缩进、命名与组织方式；直接修改源码、配置与资源等真实输入，绕开构建生成副本。面对带有生成声明的已提交源码时，先核实生成工具与原始输入是否存在。
- **保持兼容与边界**：修改公开契约、注册标识、持久化格式或网络协议时，说明兼容影响与迁移方案；保持服务端权威、前后端隔离以及可选依赖的加载隔离。
- **同步文档与诚实交付**：仅在用户明确要求时执行提交、推送或外部发布操作；本地构建与验证命令可直接执行。改动涉及规范、命令、包职责或行为变更时同步更新对应文档，并在结束时按“执行且通过”“执行但失败或阻塞”“尚未执行”三类如实汇报实际验证范围。

## 必读领域文档

- [文档总入口](docs/README.md)：文档体系导航与维护规范。
- [开发上手](docs/development/getting-started.md)：环境工具链、Gradle 构建入口与排错顺序。
- [文件与产物归属](docs/development/repository-layout.md)：源码、测试、工具、文档与临时产物的存放位置。
- [架构总览](docs/architecture/README.md)：源码分层、依赖方向与模块职责。
- [验证与验收](docs/development/validation/README.md)：按改动路径计算检查项、验收要求与交付标准。

## 可选领域文档

按本次改动涉及的领域按需查阅：

- [包职责索引](docs/architecture/packages/README.md)：修改 `api`、`content`、`engine`、`runtime`、`platform` 或 `buildtools` 时查阅。
- [跨版本要求与入口](docs/architecture/versions.md) · [Stonecutter 跨版本维护](docs/architecture/stonecutter/README.md)：处理 Forge 1.20.1 / NeoForge 1.21.1 差异与 `//?` 条件分支时查阅。
- [资源与生成内容](docs/architecture/resources.md)：修改贴图、模型、数据包、生成表或处理 `runData`、许可声明时查阅。
- [Java 注释规范](docs/development/comments.md)：编写或评审 Java 注释与纯数据表来源声明时查阅。
- [碎片贴图与机器 GUI 模板](docs/development/gui-templates.md)：新增或修改机器 GUI 布局与精灵图时查阅。
- [Issue 分类与标签](docs/development/github-issue-tags.md)：涉及 Issue 分流或标签约定时查阅。
- [附属与脚本扩展](docs/ADDON_API.zh-CN.md)：涉及 Addon API、SDK 或 KubeJS 扩展时查阅。
- [长期决策](docs/decisions/README.md)：涉及[材料系统设计](docs/decisions/material-system/README.md)、[跨平台差异](docs/decisions/platform/README.md)或[原版 GT6 特性差异](docs/decisions/parity/README.md)时查阅与登记。
- [跨模组兼容与联动](docs/integration/README.md)：查询或编写其他模组的联动及冲突审计报告时查阅。
- [开发与检查工具](tools/README.md)：使用构建期资源转换或静态检查脚本时查阅。
