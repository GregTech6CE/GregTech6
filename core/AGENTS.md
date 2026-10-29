# core 模块

先遵守[根指南](../AGENTS.md)，再读[架构边界](../docs/architecture/README.md)。这里使用 Java 17，独立于游戏和加载器。

- `gregtech.api` 由仓库内 `api/common/` 提供（内联自 GregTech6API a9c879176），core 通过 `:api` 使用；不要在 core 中重新建立同包副本，参见 [API 指南](../docs/architecture/packages/api.md)。
- `gregtech.content`：材料、配方、布局、参数和内容目录，表达“有什么”，参见 [content 指南](../docs/architecture/packages/content.md)。
- `gregtech.engine`：计算、状态、解析、查询和执行机制，表达“怎么工作”，参见 [engine 指南](../docs/architecture/packages/engine.md)。
- `gregtech.bootstrap`：共享初始化步骤契约；加载器装配属于 Minecraft 层。
- `gregtech.util`：跨领域的纯工具；有领域归属的逻辑放回该领域包。

不得引入 `net.minecraft.*`、`net.minecraftforge.*`、`net.neoforged.*` 或第三方集成 API。需要游戏能力时，通过明确契约与 Minecraft 层适配；不要让 `core` 反向依赖 `minecraft`。

API 不引用实现层，不让新公开声明泄露内部运行时类型。既有依赖交叉不代表鼓励继续扩大，局部变更不要求一次性重写历史架构。

共享资源位于 `src/main/resources/`，只放实际随模组分发的资源，保留 `META-INF/` 来源与许可声明。分析报告、生成器和下载缓存不能放入 resources，详见[资源指南](../docs/architecture/resources.md)。

`content/**/generated/` 中有从原 GT6 提取的源码，不能仅凭包名假定当前 Gradle 任务会再生；先检查文件头和生成工具是否存在。验证使用 `:core:check`，跨层接口修改还需双平台编译。
