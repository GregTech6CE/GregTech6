# 验证与验收

本目录把改动路径映射到 package 与检查，是 GT6CE 的验收入口。

## 内容

| 路径 | 用途 |
| --- | --- |
| [routes.yaml](routes.yaml) | 改动路径到 package 与检查的路由 |
| [checks/](checks/) | 各检查的验证方式、注意事项与运行命令 |
| `overrides/` | 少数路径的特例，出现时创建 |

`checks/` 目前包含 `junit`、`api-contracts`、`gametest`、`compile`、`assemble`、`format` 与 `manual-ingame`。package 的验收章节在 `docs/architecture/packages/<package>.md`（入口见[包索引](../../architecture/packages/README.md)），本目录不重复。

## 工作流程

修改代码、资源或构建脚本后，先按改动文件查路由；纯文档改动只检查相对链接与命令一致性。package 验收章节与检查文档的 `## 注意事项` 是软门禁，Agent 必须逐条核对、遵守并满足其中要求；`## 运行命令` 是硬门禁，任一命令不通过即判定失败并停止后续检查。

1. 列出改动文件：`git status --short` 或 `git diff --name-only`。
2. 运行 `python tools/validation/route.py route --changed <路径>...`，得到命中的 package 与检查；安装、输出字段与 `check` 用法见[工具文档](../../../tools/validation/README.md)。
3. 读取并遵守命中的 `docs/architecture/packages/<package>.md` 验收章节，逐条核对其中要求。
4. 按命中的 `checks/<id>.md` 逐条核对并满足 `## 注意事项`，再执行 `## 运行命令`；硬门禁失败时立即停止。
5. 结束时分别报告“执行且通过”“执行但失败或阻塞”“尚未执行”。

## CI

`verify` 运行 `:api:check :core:check :minecraft:spotlessCheck`；`build` 对两个节点运行 `assemble`、`compileTestJava` 与 `spotlessCheck`。GameTest 不在 CI 或 `check` 内，需要本地用两个节点的 `runGameTestServer` 单独运行。
