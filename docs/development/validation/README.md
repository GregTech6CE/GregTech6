# 验证与验收

本目录把改动路径映射到 package 与检查，是 GT6CE 的验收入口。检查命令均在仓库根目录执行。

## 内容

| 路径 | 用途 |
| --- | --- |
| [routes.yaml](routes.yaml) | 改动路径到 package 与检查的路由 |
| [checks/](checks/) | 各检查的验证方式、注意事项与运行命令 |
| `overrides/` | 少数路径的特例，出现时创建 |

`checks/` 目前包含 `junit`、`api-contracts`、`gametest`、`compile`、`assemble`、`format` 与 `manual-ingame`。package 的验收章节在 `docs/architecture/packages/<package>.md`（入口见[包索引](../../architecture/packages/README.md)），本目录不重复。

## 工作流程

修改代码、资源或构建脚本后按以下流程选择检查。package 的 `## 验收` 与检查文档的 `## 注意事项` 必须逐条满足；`## 运行命令` 是硬门禁，任一命令失败即停止后续检查。

1. 列出改动文件：`git status --short` 或 `git diff --name-only`。
2. 运行 `python tools/validation/route.py route --changed <路径>...`，得到命中的 package 与检查；安装、输出字段与 `check` 用法见[工具文档](../../../tools/validation/README.md)。
3. 读取并遵守命中的 `docs/architecture/packages/<package>.md` 验收章节，逐条核对其中要求。
4. 按命中的 `checks/<id>.md` 逐条核对并满足 `## 注意事项`，再执行 `## 运行命令`；硬门禁失败时立即停止。
5. 按下述格式报告结果。

纯文档改动检查相对链接、标题锚点和命令一致性，无需编译或启动游戏。可先运行 `python tools/validation/route.py check` 检查路由与检查文档；它不检查全仓链接或标题锚点，详见[工具说明](../../../tools/validation/README.md#校验内容)。

## 结果与证据

交付时分别列出“执行且通过”“执行但失败或阻塞”“尚未执行”，写明实际命令、失败原因与未覆盖的平台或场景。编译、打包、自动测试和游戏行为分别判定，不能相互代替。

长期报告按场景使用“未验证 / 部分验证 / 在下述范围验证通过 / 已知失败”，记录源码状态、环境版本、操作、预期与实际结果。使用[集成报告模板](../../integration/report-template.md)或[执行记录模板](../../operations/records/TEMPLATE.md)保存可复现结论；原始材料的归属见[文件指南](../repository-layout.md#临时内容如何沉淀)。

## CI

`verify` 运行 `:api:check :core:check :buildtools:check :minecraft:spotlessCheck`；`build` 对两个节点运行 `assemble`、`compileTestJava` 与 `spotlessCheck`。GameTest 不在 CI 或 `check` 内，需要本地用两个节点的 `runGameTestServer` 单独运行。
