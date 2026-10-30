# 文件与产物归属

先决定内容是否需要长期维护，再决定它是不是实际生产输入。`src/` 的用途是编译、测试与打包，不是所有文件的默认落点。

| 目录 | 应当放什么 | 不应放什么 |
| --- | --- | --- |
| `api/` | 内联的 GregTech6API 源码（锁定 a9c879176，API 0.3.0），由 `:api`、`:api-platform` 直接编译 | 未更新 API 版本记录、未说明来源基线的 API 改动 |
| `api/common/src/main/java/` | Java 17 公共协议、模型、声明与宿主服务接口 | core 实现、内容目录、游戏类型、任务报告 |
| `api/common/src/test/` | 仅依赖公共 API JAR 的消费者与契约回归 | 主模组实现和资源 |
| `core/src/main/java/` | 平台无关内容定义、运行规则 | 游戏 API、加载器适配、任务报告 |
| `core/src/main/resources/` | 共用贴图、模型、声音、数据与分发许可 | 下载包、调试日志、文档草稿 |
| `minecraft/src/main/` | 共享游戏实现及资源输入 | 构建生成副本、部署笔记 |
| `api/minecraft/src/main/` | 内联 API 中经 Stonecutter 编译的游戏侧公开 API | core 或模组 main 实现依赖 |
| `minecraft/versions/*/src/main/` | 版本专属实现、加载器元数据及资源 | 测试实例、原始证据 |
| `gradle/`、各 `*.gradle*` | 构建定义、Wrapper 与平台任务配置 | 个人安装路径、凭据 |
| `docs/` | 经整理的长期指南、验证结论和记录 | 完整日志、随手计划、下载 |
| `tools/` | 可复用的开发/生成/检查脚本及使用说明 | 一次性实验；不要为占位创建空工具 |
| `examples/` | 将来可维护、可独立使用的附属/脚本示例 | 临时探针或未验证伪示例 |
| `work/<任务名>/` | 当前任务原始材料与私有执行参数，Git 忽略 | 唯一一份尚未提炼的长期结论 |
| 各级 `build/` | Gradle 生成、编译、打包、报告 | 手工维护的生产输入 |
| 节点 `run/`、`run-*/` | 本地游戏实例、存档、配置与运行输出 | 要提交的源码 |

`tools/`、`examples/` 是新增内容的归属约定，本文不表示仓库已经包含可用脚本或示例。

## 临时内容如何沉淀

例如排查管道传输问题：一次性分析脚本与完整日志放 `work/fluid-pipe-check/`；可复用的检查器整理到 `tools/`；跨平台行为结论放 `docs/integration/`；算法边界补到架构文档；若确实执行了实例更新，再写 `docs/operations/records/`。无需给每次小改动建立长期报告。

开发用探针优先放 `work/`，不会自动参与生产编译；若要长期保留为自动测试，接入合适测试源码集及依赖，并记录运行命令。不要把探针改名成业务类混入 `src/main/java/`。

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
