# Agent 工作指南

本文件适用于整个仓库；进入 `core/`、`minecraft/` 或 `docs/` 后，还须阅读当地的 `AGENTS.md`。用户的明确要求优先。文档以中文维护，代码标识符沿用项目现有英文命名。

## 渐进阅读

1. [文档入口](docs/README.md)：按任务选路线，无需一次读完。
2. [开发上手](docs/development/getting-started.md)：环境与构建。
3. [验证与验收](docs/development/validation/README.md)：按改动读取对应的 package 与检查。
4. [目录与产物归属](docs/development/repository-layout.md)：决定文件放哪里。
5. [架构入口](docs/architecture/README.md) → [包职责索引](docs/architecture/packages/README.md)：决定代码属于哪一层。

## 项目事实

- GT6CE 是现代 Minecraft 的 GregTech 6 移植工程。模组 ID 与命名以现有 `GregTechIdentity` 和加载器元数据为准，不要顺手改名。
- `api/`：内联自 GregTech6API 的源码（锁定 a9c879176，基线 API 1.2.0；当前 API 1.8.0）；`common/` 提供 Java 17 公共契约，`minecraft/` 提供按版本编译的游戏 API。修改公共签名时直接在本仓库 `api/` 内更新，并在提交信息中记录 API 版本变化。
- `core/`：Java 17 的平台无关内容定义、计算和共享资源，依赖公共 API；不得引入 Minecraft、Forge、NeoForge、客户端或可选模组类型。
- `minecraft/src/`：两版本共享的 Minecraft 代码与 Stonecutter 输入。
- `minecraft/versions/1.20.1-forge/src/`：Forge 版本源码及资源，Java 17。
- `minecraft/versions/1.21.1-neoforge/src/`：NeoForge 版本源码及资源，Java 21。
- `gradle/`、`minecraft/build.gradle`、`settings.gradle` 决定真实源码集合、生成和打包流程，不能仅凭文件夹名称推断。
- 共享代码中的 Stonecutter `//?` 条件标记及注释包裹的另一版本代码有实际用途，不能当死代码清除。
- 跨版本差异的裁决顺序、统一目标和平台 API 例外按[跨版本统一方针](docs/architecture/stonecutter/unification-policy.md)执行。

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

## 注释

注释只在读者可能做错决定时写；不写复述代码、来源台账或历史叙述。

**写什么**

- 移植差异：GT6 的行为 → 本端行为 → 为什么不同（API 缺失、平台、数据模型、性能）。
- 不变量：注册顺序、单位与比例、优先级、线程、存档/网络格式、初始化时机。
- 非显然的绕行：loader 差异、Stonecutter 分支、兼容性补丁。
- 公开 API 与引擎契约的 JavaDoc。
- 纯数据表的逐行来源；这是唯一允许在注释里出现行号的地方。手写数据表在文件头写 `// source-rows` 声明。

**不写什么**

- 复述代码；只有来源、没有本端后果的引用；“本批次”“之前是”、任务或计划卡编号等历史（交给 `git log`）。
- 注释正文里的行号：`Foo.java:123`、`Loader_X:123`、`line 123`、`Class, 123` 都不写。

**怎么引用上游**

- 本端独有代码、行为已经说清、或指向 `run()` 这类超大方法时，不引用上游。
- 需要对照具体行为、常量或公式时用符号：`{@code Class#member}` 或 `{@code Class.member(...)}`；一次一个主符号，并写清本端后果。
- 数据表逐行转录可保留 `Class:line`，行号相对固定提交 `3703e40308c8c030763fd6297dea8b210d2a77b1`（2026-06-15）；换基线时统一重生成。
- 本仓库代码引用用 `{@link Foo}`、`{@link #member}`（JavaDoc）或类名、方法名（`//`），不写行号。

`tools/comments/check_comment_refs.py` 检查以上行号与历史规则；CI 的 verify job 用 `--diff` 只检查改动行。它不判断注释是否有价值，那部分仍需人工评审。

## 文件必须放对地方

| 内容 | 位置 |
| --- | --- |
| 生产 Java 与实际打包资源 | 对应模块的 `src/main/java`、`src/main/resources` |
| 可重复运行的自动测试 | 先确认测试框架与任务，再根据[文件与产物归属](docs/development/repository-layout.md)确定对应模块实际接入构建的 `src/test/` |
| 一次性脚本、上游导入/再生成脚本、下载、反编译、原始日志、截图、探针结果、个人计划 | `work/<任务名>/`，已忽略；系统临时数据也可放 `/tmp/` |
| 可复用且随代码演进的开发/检查脚本 | `tools/<scope>/`，脚本、自带数据与该目录 `README.md` 同放，README 注明输入、依赖、输出与运行方式 |
| 长期开发指南 | `docs/development/` |
| 架构与包职责 | `docs/architecture/` |
| 跨模组/跨平台对照、来源、验证结论 | `docs/integration/` |
| 发布/部署流程、可提交的脱敏执行记录 | `docs/operations/`、`docs/operations/records/` |
| 长期架构决策及取舍 | `docs/decisions/` |

**不要往任何 `src/` 放任务笔记、agent 交接、部署记录、日志、文档草稿、临时探针或一次性迁移脚本。** JavaDoc、必要的 `package-info.java` 和随模组分发的许可证是真正源码/资源的例外；不要新建 `src/docs/` 存放工作文档。

`docs/` 不存所有工作过程。长期结论提炼为有上下文和复现方法的文档，原始材料留在 `work/`。忽略规则不是保密措施，凭据不得进入版本库或公共记录。

## 验证与交付

针对修改的内容和范围，读取对应的编码规范和验收所需的步骤和命令。入口见[验证与验收](docs/development/validation/README.md)，其中给出改动路径到 package 与检查集的映射，检查文档给出注意事项与运行命令。格式检查由 `format` 强制覆盖，不把格式问题留到 CI。

- 结束时分别报告“执行且通过”“执行但失败或阻塞”“尚未执行”，写明未覆盖的平台与场景。
- 新增命令、参数、注册步骤或包职责时，同步验证文档、检查文档与包索引。
- 如果改动修改了原版 GT6 的实现，或者删除了原版 GT6 的物品、流体、材料，则在 `docs/decisions/parity` 中按格式记录。
- 受环境阻塞时报告具体原因，不宣称成功。
