# Minecraft 层

先遵守[根指南](../AGENTS.md)，按任务读[运行时包](../docs/architecture/packages/runtime.md)、[平台适配](../docs/architecture/packages/platforms.md)和[跨版本指南](../docs/architecture/versions.md)。

- 共享输入在本目录 `src/`；版本输入在 `versions/<version-loader>/src/`。同名包不代表文件可放任意源码目录。
- 公开游戏 API 位于独立子模块 `api/minecraft/src/main/`，由 `:api-platform:<node>` 编译；本目录只依赖其输出，不再维护 `src/api` 副本。
- 当前 Stonecutter 活动节点为 `1.20.1-forge`。保留条件标记，尽量用小范围条件分支处理差异；整类差异才考虑版本节点实现。
- `versions/*/build/generated/stonecutter/` 是生成结果，只读检查，不手工修复。新增同名类前检查共享输入和两节点，避免重复类。
- 渲染、模型、屏幕及客户端事件留在 `gregtech.client` 和明确的客户端初始化路径，公用静态初始化不引用客户端类型。
- 游戏对象、能力、物品/流体栈、网络和持久化留在本层；复用 `core` 中的纯规则，不复制一套平台无关逻辑。
- 注册和生命周期检查 `GregTech`、`GregTechNeoForge` 及 bootstrap，保持基础数据先于依赖注册的顺序。
- Mixin 检查两节点的配置和目标方法签名；可选模组相关类保持加载隔离。
- 网络数据检查方向、边界、线程与服务端权限；客户端显示值不能作为服务端规则的权威来源。
- 资源遵循版本格式，注意 1.20.1 与 1.21.1 数据包路径的单复数差异。

开发运行目录是节点内的 `run/`、`run-client/`、`run-server/`、`run-data/`，均为本地数据。不提交存档、实例配置或日志；自定义 `serverDirectory` 指向隔离实例。

Forge `runData` 目前直接输出至该节点的 `src/main/resources/`，运行后须审查生成差异，不能当无副作用检查。NeoForge 是否有某个开发任务，以实际 Gradle 任务为准。

共享修改默认双平台编译。打包验收使用 Forge `distributionJar` 和 NeoForge `jar`，Forge 中间 JAR 不用于部署。文档、探针、截图和部署记录遵循根目录归属表。
