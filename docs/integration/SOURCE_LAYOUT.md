# 当前源码导航与命名约定

此文描述 `main` 的 Stonecutter 结构。原版行为差异见 [原版对齐审计](ORIGINAL_ALIGNMENT_AUDIT.md)；目录整理不代表全部功能已经对齐。

## 先按层找代码

| 目录 | 职责 |
| --- | --- |
| `core/src/main/java/com/gregtech/gregtech` | 不依赖 Minecraft 的材料、数量、配方合同、能源计算、结构目录等领域代码 |
| `core/src/api/java` | 对附属模组公开的纯 Java API |
| `multiversion/src/main/java/com/gregtech/gregtech` | 两版本共用的 Minecraft 交互、注册适配、渲染、机器和界面；必要差异用 Stonecutter 条件表示 |
| `multiversion/src/api/java` | 需要 Minecraft 类型的公开 API |
| `multiversion/versions/1.20.1-forge/src` | Forge 生命周期与仍需分离的版本适配 |
| `multiversion/versions/1.21.1-neoforge/src` | NeoForge 生命周期与仍需分离的版本适配 |
| `*/build/generated/stonecutter`、`build/native-tools` | 自动生成／投影视图，不是人工维护入口 |
| `tools/integration` | 来源导入、结构／语言检查和构建衔接；旧原生写入工具通过 `tools/native_tool.py` 执行 |

`core` 中的数据不能为了方便调用而依赖原生加载器。主源码、API、GameTest 和客户端探针分别维护，不能把测试逻辑塞回生产启动流程。

## 初始化和配方从哪里看

- Forge 入口：版本目录的 `com/gregtech/gregtech/GregTech.java`。按 `content`、`machine registration`、`material linking` 职责调用初始化步骤。
- NeoForge 入口：版本目录的 `platform/neoforge/GregTechNeoForge.java`。保留原生注册事件及材料／发酵准备顺序。
- 共用机器配方入口：共享目录的 `content/recipe/MachineRecipeBootstrap.java`。两平台共用注册清单；显式配方在前，通用形态处理随后，导入的大表最后补充。
- 发酵配方在 NeoForge 材料后处理前绑定，在 Forge 清单中注册；燃料表在 Forge 基础数据阶段创建，在 NeoForge 配方阶段创建。这两个条件点被明确保留，不能为“统一”随意调换顺序。
- `bootstrap/InitializationStep` 是一个有明确职责的初始化动作，也兼容 `Runnable`；`SetupOnce` 防止重复注册并保留首次异常。
- Forge 的工作台配方随 RecipeManager 重载注入，见 `recipe/RuntimeRecipeLifecycle.java`。`*RecipeInjector` 表示这些注入器，不与初始化机器配方混用。
- 导入配方表位于 core 的 `content/recipe/generated`；原生 `GeneratedRecipeResolver` 负责物品／流体解析，`GeneratedRecipeSink` 负责向当前解析上下文输出纯数据。`GTTechnologyRecipes` 是手写的原版嵌入配方目录，不是机器控制器。

## 命名与修改约定

- 包名表达职责，不再新增 `a/b/c` 阶段目录或并列的 `loader/loaders`。
- 类名使用正常 Java 命名：如 `MaterialBootstrap`、`DeferredRegistrySubmission`、`FusionRecipeLoader`、`WoodCraftingRecipeInjector`、`OvenRecipeBridge`。
- 公共类型名与文件名一致。生成目录名与数据来源名保留含义，不把所有 `GT` 前缀机械删除。
- `Loader_Recipes_*` 等原版类名仍可以出现在来源注释、原版路径和历史回执里；这些是溯源信息，不是未改名的活动类。
- 修改生成数据时同步生成器，输出必须落在实际维护目录；不得重新生成另一份平台数据副本。
- 共用初始化清单的顺序影响配方冲突优先级。重构需比较两平台改动前后的实际调用顺序，并检查平台准备动作与附属 `recipesReady` 回调的位置。
- 一般生产代码保持按方法／职责分行。剩余压行实现、过大职责类和原版属性映射仍在审计，不将本轮改名算作全部清理完成。

本轮 30 个类名的完整映射、两版 54 步初始化序列和验证范围见 [标准化回执](verification/loader-standardization-20261019.json)。

材料生成表需要区分“表已存在”与“属性已正确继承／被玩法消费”。目前的工作属性表和处理目标表仍有确认缺口，见 [材料处理审计](MATERIAL_PROCESSING_AUDIT.md)。`tools/integration/inventory_processing_target_consumers.py` 是只读导航工具，输出引用位置，不会替代语义核查或写回原版源码。
