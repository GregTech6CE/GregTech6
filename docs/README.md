# GT6CE 文档

本目录面向开发者与 agent。玩法及支持版本先读[项目介绍](../README.zh-CN.md)，工作规则读[根 AGENTS.md](../AGENTS.md)。以下内容基于本仓库源码与 Gradle 配置整理；设计约定不等于所有历史代码已完全满足约定。

## 按层阅读

| 层级 | 读什么 | 读完能做什么 |
| --- | --- | --- |
| 0：开始工作 | [开发上手](development/getting-started.md)、[文件归属](development/repository-layout.md) | 选择 JDK、构建目标和文件位置 |
| 1：建立整体认识 | [架构](architecture/README.md)、[包索引](architecture/packages/README.md) | 区分模块、领域、运行时与适配 |
| 2：进入具体包 | [API](architecture/packages/api.md)、[engine](architecture/packages/engine.md)、[content](architecture/packages/content.md)、[运行时](architecture/packages/runtime.md)、[平台](architecture/packages/platforms.md) | 定位职责、代表入口和修改边界 |
| 3：处理横向问题 | [跨版本](architecture/versions.md)、[资源与生成物](architecture/resources.md)、[验证](development/validation.md)、[测试](development/testing.md)、[附属 API](ADDON_API.zh-CN.md) | 安全修改跨平台功能、资源和扩展接口 |
| 4：留下可维护成果 | [集成证据](integration/README.md)、[操作与部署](operations/README.md)、[决策](decisions/README.md) | 保存可复现结论与执行记录 |

## 按任务找入口

- 新材料或配方：先看 `content` 中的目录/表，再看 `engine.material`、`engine.recipe`，最后看 Minecraft 注册与加载入口。
- 新机器或多方块：从 `api.machine` / `api.multiblock` 的契约和 `content.machine` / `content.multiblock` 的定义开始，再接入运行时、注册、同步和客户端。
- 渲染或 tooltip：从运行时指南的 `client` 查起；权威规则仍在服务端及共享逻辑。
- 能源、流体或物流：在 engine 的对应领域找规则，在 content 找具体设备，在平台包找能力适配。
- 可选模组或脚本：读运行时的 `compat` 与附属 API 指南，记录基础启动和集成启动两种结果。
- 发布或更新测试实例：读操作指南，执行后用记录模板保存脱敏结论。

## 文档边界与维护

`development/` 写操作步骤，`architecture/` 写职责与原理，`integration/` 写来源与验证，`operations/` 写交付与运行，`decisions/` 写长期取舍。原始日志、下载、个人计划和一次性脚本统一放根目录 `work/<任务名>/`。

这些目录是长期归档约定，不代表已配置发布服务或外部示例项目；自动测试的现状与入口见[测试](development/testing.md)。新增文档先挂到所属入口；接口、命令、包或流程变更时一并更新相关页面。
