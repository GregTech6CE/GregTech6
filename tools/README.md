# tools

仓库内只放可复用、随代码一起演进的检查/验证脚本。按 scope 分目录，每个目录里是脚本、脚本自带的数据文件和 `README.md`；工具说明写在该 README，不写进 `docs/development/`。

| 目录 | 用途 |
| --- | --- |
| [comments/](comments/README.md) | 检查代码注释里的失效引用，并证明一次改动只碰了注释。 |
| [gui/](gui/README.md) | 检查 GUI 模板、别名、图标引用与静态预览。 |
| [recipes/](recipes/README.md) | 审计数据包配方的形状、标签、电路家族、歧义与 NeoForge 覆盖。 |
| [resources/](resources/README.md) | 核对模板化发行包的模型、方块状态与 PNG 完整性。 |
| [validation/](validation/README.md) | 读取验证路由表，计算改动对应的检查并校验文档闭合。 |

一次性工具——从上游源码导入数据、再生成某张提交表、或对本地 dump 做一次性比对——不进本仓库：它们依赖固定提交的上游源码、只对一个版本有效，放进 `tools/` 会持续腐烂。这类脚本与说明留在本机被忽略的 `work/local-tools/`，仓库只保留已提交的生成文件和代码里的来源说明。
