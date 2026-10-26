# Agent 工作指南

本文件适用于整个仓库；进入 `core/`、`minecraft/` 或 `docs/` 后，还须阅读当地的 `AGENTS.md`。用户的明确要求优先。文档以中文维护，代码标识符沿用项目现有英文命名。

## 渐进阅读

1. [文档入口](docs/README.md)：按任务选路线，无需一次读完。
2. [开发上手](docs/development/getting-started.md)：环境、构建与验证命令。
3. [目录与产物归属](docs/development/repository-layout.md)：决定文件放哪里。
4. [架构入口](docs/architecture/README.md) → [包职责索引](docs/architecture/packages/README.md)：决定代码属于哪一层。

## 项目事实

- GT6CE 是现代 Minecraft 的 GregTech 6 移植工程。模组 ID 与命名以现有 `GregTechIdentity` 和加载器元数据为准，不要顺手改名。
- `api/`：GregTech6API 独立仓库的 Git 子模块；修改前阅读其中 `AGENTS.md`。`common/` 提供 Java 17 公共契约，`minecraft/` 提供按版本编译的游戏 API。主库只记录子模块提交，不再维护 API 源码副本。
- `core/`：Java 17 的平台无关内容定义、计算和共享资源，依赖公共 API；不得引入 Minecraft、Forge、NeoForge、客户端或可选模组类型。
- `minecraft/src/`：两版本共享的 Minecraft 代码与 Stonecutter 输入。
- `minecraft/versions/1.20.1-forge/src/`：Forge 版本源码及资源，Java 17。
- `minecraft/versions/1.21.1-neoforge/src/`：NeoForge 版本源码及资源，Java 21。
- `gradle/`、`minecraft/build.gradle`、`settings.gradle` 决定真实源码集合、生成和打包流程，不能仅凭文件夹名称推断。
- 共享代码中的 Stonecutter `//?` 条件标记及注释包裹的另一版本代码有实际用途，不能当死代码清除。

## 工作方式

- 开工先检查 `git status --short`，保留已有改动、未跟踪文件和用户资产。无初始提交或文件未跟踪时，不能认定它们是本次新建。
- 先搜索同类实现、调用方、注册入口和两平台差异，再局部修改。优先 `rg`；未安装时使用 `grep`、`find`。
- 按所在文件的缩进、命名和组织方式编写，避免顺手批量格式化、搬包或重写无关实现。
- 区分当前实现和建议边界。历史包名或 `Original*`、`GT*Gen` 名称不是行为完整、版本一致或可重新生成的证明。
- 不编辑 `build/` 内的生成副本；查找真实输入。带生成声明的已提交源码先查生成器及原始输入，缺失时说明缺口，不臆造生成流程。
- 不顺手变更注册 ID、序列化键、数据组件、NBT 格式、网络协议、配方单位或初始化顺序。必要修改须说明兼容影响与迁移方式。
- 保持客户端与服务端隔离；可选集成不能让未安装该模组的基础游戏启动失败。网络输入须验证，日志不得泄露凭据或私密配置。
- 不吞异常或关闭检查来获得“通过”，不把编译成功写成游戏行为已验证。
- 用户未要求时，不提交、推送、发布或操作真实服务器；本地构建和检查可直接完成。不要替用户接受 EULA。

## 文件必须放对地方

| 内容 | 位置 |
| --- | --- |
| 生产 Java 与实际打包资源 | 对应模块的 `src/main/java`、`src/main/resources` |
| 可重复运行的自动测试 | 对应模块实际接入构建的 `src/test/`；先确认测试框架与任务 |
| 一次性脚本、下载、反编译、原始日志、截图、探针结果、个人计划 | `work/<任务名>/`，已忽略；系统临时数据也可放 `/tmp/` |
| 可复用且值得维护的开发脚本 | `tools/`，注明输入、依赖、输出与运行方式 |
| 长期开发指南 | `docs/development/` |
| 架构与包职责 | `docs/architecture/` |
| 跨模组/跨平台对照、来源、验证结论 | `docs/integration/` |
| 发布/部署流程、可提交的脱敏执行记录 | `docs/operations/`、`docs/operations/records/` |
| 长期架构决策及取舍 | `docs/decisions/` |

**不要往任何 `src/` 放任务笔记、agent 交接、部署记录、日志、文档草稿、临时探针或一次性迁移脚本。** JavaDoc、必要的 `package-info.java` 和随模组分发的许可证是真正源码/资源的例外；不要新建 `src/docs/` 存放工作文档。

`docs/` 不存所有工作过程。长期结论提炼为有上下文和复现方法的文档，原始材料留在 `work/`。忽略规则不是保密措施，凭据不得进入版本库或公共记录。

## 验证与交付

- 文档/忽略规则：检查相对链接、命令与配置一致性，用 `git check-ignore` 验证规则；无需启动游戏或全量构建。
- `core` 逻辑：按改动运行 `./gradlew :core:check`，必要时补充有意义的回归测试。
- 共享 Minecraft 逻辑：先 `./gradlew compileBothPlatforms`；涉及资源或打包时使用 `./gradlew assembleBothPlatforms`。
- 完整检查使用 `./gradlew check`；CI 对两个节点分别执行 `build`，详见[验证指南](docs/development/validation.md)。
- 世界、机器 tick、交互、渲染、Mixin、网络或可选集成还需对应游戏内验证，记录未覆盖的平台与场景。
- 新增命令/参数/注册步骤同步对应文档，新增包职责同步包索引。
- 结束时报告改动、验证命令与结果、未验证事项。受环境阻塞时报告具体原因，不宣称成功。
