# 文件与产物归属

先决定内容是否需要长期维护，再决定它是不是实际生产输入。`src/` 的用途是编译、测试与打包，不是所有文件的默认落点。

| 目录 | 应当放什么 | 不应放什么 |
| --- | --- | --- |
| `api/` | 内联 API 源码，由 `:api`、`:api-platform` 直接编译；来源与版本见[附属 API](../ADDON_API.zh-CN.md#独立库与-sdk-构建) | 未说明兼容影响的公共 API 改动 |
| `api/common/src/main/java/` | Java 17 公共协议、模型、声明与宿主服务接口 | core 实现、内容目录、游戏类型、任务报告 |
| `api/common/src/test/` | 仅依赖公共 API JAR 的消费者与契约回归 | 主模组实现和资源 |
| `core/src/main/java/` | 平台无关内容定义、运行规则 | 游戏 API、加载器适配、任务报告 |
| `core/src/test/` | 平台无关的定向 JUnit 回归：配方 JSON、目录表与生成行 | 游戏类型、运行时依赖、任务报告 |
| `core/src/main/resources/` | 共用贴图、模型、声音、数据与分发许可 | 下载包、调试日志、文档草稿 |
| `minecraft/src/main/` | 共享游戏实现及资源输入 | 构建生成副本、部署笔记 |
| `minecraft/src/test/` | 共享 GameTest：测试 Java、案例 TSV、结构模板 | 生产实现、任务报告 |
| `minecraft/versions/*/src/test/` | 仅 loader API 必需的版本专属测试 | 可共享场景、复制的模板 |
| `api/minecraft/src/main/` | 内联 API 中经 Stonecutter 编译的游戏侧公开 API | core 或模组 main 实现依赖 |
| `minecraft/versions/*/src/main/` | 版本专属实现、加载器元数据及资源 | 测试实例、原始证据 |
| `gradle/`、各 `*.gradle*` | 构建定义、Wrapper 与平台任务配置 | 个人安装路径、凭据 |
| `docs/` | 经整理的长期指南、验证结论和记录 | 完整日志、随手计划、下载 |
| `docs/development/validation/` | 验证规则、检查文档、命令与路由表；验收章节在 `docs/architecture/packages/` | 一次性计划、未提炼的日志、与规则无关的执行记录 |
| `tools/<scope>/` | 按 scope 分组、随代码演进的检查/验证脚本或构建期模块、其数据文件与 `README.md` | 一次性实验、上游导入/再生成脚本；散放在 `tools/` 根目录的文件 |
| `examples/` | 将来可维护、可独立使用的附属/脚本示例 | 临时探针或未验证伪示例 |
| `work/<任务名>/` | 当前任务原始材料与私有执行参数，Git 忽略 | 唯一一份尚未提炼的长期结论 |
| 各级 `build/` | Gradle 生成（含 `generated/stonecutter/`、`generated/recipe-resources/`）、编译、打包、报告 | 手工维护的生产输入 |
| 节点 `run/`、`run-*/` | 本地游戏实例、存档、配置与运行输出 | 要提交的源码 |

现有工具见[工具目录](../../tools/README.md)；`examples/` 是未来独立示例的归属约定。工具说明与脚本、数据同放在 `tools/<scope>/`。构建期模块仍是 Gradle 子项目，由 `settings.gradle` 的 `projectDir` 指到这里（如 `tools/buildtools`），不用 `build` 作目录名以免被 `build/` 忽略规则覆盖。

从上游源码导入数据、再生成提交表、或对本地 dump 做一次性比对的脚本不进仓库：它们绑定固定提交的上游源码，只对一个版本有效。这类脚本连同说明放本机被忽略的 `work/local-tools/`，仓库只保留已提交的生成文件与来源说明。

## 文档归属

| 内容 | 维护位置 |
| --- | --- |
| 环境、开发步骤与专题操作 | [development](README.md) |
| 检查选择、注意事项和运行命令 | [validation](validation/README.md) |
| 分层、系统原理与资源机制 | [architecture](../architecture/README.md) |
| 包职责、边界与验收条件 | [architecture/packages](../architecture/packages/README.md) |
| 跨平台、跨模组与上游对照结论 | [integration](../integration/README.md) |
| 交付与实例更新流程、脱敏执行记录 | [operations](../operations/README.md) |
| 长期取舍与原版差异 | [decisions](../decisions/README.md) |
| 单个开发工具的依赖、参数与输出 | 对应 `tools/<scope>/README.md` |

顶层文档入口以导航为主；正文放在所属专题，其他页面链接引用。工作过程先留在 `work/<任务名>/`，形成可复用结论后再归档。

## 临时内容如何沉淀

例如排查管道传输问题：一次性分析脚本与完整日志放 `work/fluid-pipe-check/`；可复用的检查器整理到 `tools/<scope>/` 并在该目录 README 记录用法；跨平台行为结论放 `docs/integration/`；算法边界补到架构文档；若确实执行了实例更新，再写 `docs/operations/records/`。无需给每次小改动建立长期记录。

开发用探针优先放 `work/`，不会自动参与生产编译；若要长期保留为自动测试，接入合适测试源码集及依赖，并记录运行命令；测试目录与案例表约定见[验证与验收](validation/README.md)。不要把探针改名成业务类混入 `src/main/java/`。

## 文档、资源与生成物的区别

- JavaDoc、`package-info.java` 解释代码契约，可以留在 Java 源码树。
- 游戏中读取的书本、语言文件、数据包 JSON 以及随包分发的 NOTICE 是生产资源。
- 架构说明、agent 交接、部署日志不是游戏资源；放进 resources 会污染发行包。
- 已提交的 `content/**/generated/*.java` 是生产输入，不能统一忽略或删除；`build/generated/stonecutter/` 则是可再生产物。
- 文件叫 `GTTemporaryGen.java` 也不代表临时工作文件，判断依据是实际用途与引用关系。

## .gitignore 的边界

规则覆盖构建、运行实例、IDE、本地环境、工具缓存与 `work/`，保留 Wrapper JAR、资源 JSON、生成内容表和 `docs/operations/records/`。历史本地笔记位置继续忽略，新笔记使用 `work/`。

不要用 `*.jar`、`*.json`、`*.md` 或任意 `generated/` 的全局规则掩盖真实输入；也不要随意 `git add -f`。`.gitignore` 不影响已跟踪文件，也不保护秘密；若凭据已经提交，仅新增忽略规则不能消除泄露。

可用 `git check-ignore -v --no-index <路径>` 检查规则；交付前核对 `git status --short --untracked-files=all`，确保新增文档与工具可被发现。
