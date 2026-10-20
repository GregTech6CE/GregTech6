# 原生整类重复盘点与配方业务共享

第五十五批，2026-10-21，观察基线 `e5c6b2021`。本批处理源码结构，不新增 ALIGN 行为缺口；正式行为记录仍为193条，包含历史修复项。

## 范围与实际重复

完整扫描 `multiversion/src/main/java` 的1041份维护Java，分别显式展开Forge／NeoForge，共2082份视图。每份恰有一个package，投影没有异常；741份维护输入含条件。原生接口条件本身不代表业务重复，也没有据此推导整个项目共享率或完成百分比。

检查暴露后的重复package／类型声明、完整方法和实际入口，确认四项整类业务重复候选：

| 文件（共享Java根内） | 实际共用业务与入口 | 必须保留的区别 | 本批结果 |
| --- | --- | --- | --- |
| `content/recipe/VanillaProcessingRecipes.java` | MachineRecipeBootstrap调用register，依次masonry、grinding、ice、compression及辅助方法 | FluidStack导入、ice中holder可用性查询 | 411→144行，一份共用类体 |
| `content/recipe/AlloyRecipeLoader.java` | 同一bootstrap运行run；熔炼、合金、坩埚展示、配方参数及辅助方法 | FluidStack／holder类型、isPresent／isBound | 543→190行，一份共用类体 |
| `content/recipe/ChemicalRecipeLoader.java` | bootstrap运行盐水、氯碱、硫链、氨、炼油、生物质及辅助方法 | 导入及make／gasOrLiquid的原生holder | 独立后续处理，不纳入本批两文件证明 |
| `content/recipe/generated/GeneratedRecipeResolver.java` | bootstrap的loadAll及战利品、主配方、聚变、电池等解析调用 | **Forge先combs后technology，Neo顺序相反**；注册表查找、命名空间表达式和流体holder亦不同 | 后续独立合并，不能把高相似度当行为相同 |

解析器的两个表都存在，不等于先后可互换：加载碰撞和已有记录会受顺序影响。本批仅记录这个结构边界，没有在缺少原版选择合同前将一端顺序改成另一端。

`MetalChestBlockItem`只有21行，是Forge客户端扩展回调与Neo事件扩展的边界，没有重复大段容器业务；`GTEngines`两分支分别直接注册与转发MachineRegistry，不能按重复class声明机械删除。已共享的DecompositionRecipeLoader只有原生导入／查询条件，也排除出本次候选。

另有171项方法密度筛选结果尚未逐项审查，**不是171个重复或缺陷**。PacketSyncFluids的载荷API、FluidDisplayBinding的保存组件、PortableContainerLimits的NBT／组件和生命周期差异，也不能只因调用相同core规则就无条件合并。

## 两份实际共享结果

VanillaProcessingRecipes与AlloyRecipeLoader合计954→334维护行，减少620行维护输入。删除的是原整份平台类体和其中空行，不把行数当共享业务或修复数量。两文件现在各有一个package、一份公开类及共同配方流程，只在原生导入／类型和holder查询处保留Stonecutter条件；公开入口、数据、参数、计数、跳过策略和调用顺序不变。

旧Neo块注释与Forge Javadoc不同，共享说明沿用原Forge拼写。执行证明包含导入、控制流程、数值和字面量原位置，不仅比较删空白文本或独立的字符串列表。没有以去重为由修改已记录的熔炼、分解、相态或配方缺口。

| 维护文件 | Forge执行词法项 | NeoForge执行词法项 | 对基线结果 |
| --- | ---: | ---: | --- |
| VanillaProcessingRecipes | 1728 | 1730 | 两平台完整词法／字面量位置逐项相同 |
| AlloyRecipeLoader | 2150 | 2158 | 两平台完整词法／字面量位置逐项相同 |

## 来源、验证与后续

[回执](verification/native-body-sharing-audit-20261021.json)保存1041份观察源码身份、2082份视图摘要、四个候选的实际方法／入口和差异上下文、排除理由、工具身份及两文件去重前后完整执行证明。root重新核对全部基线视图和四份最终视图，保留原观察hash，不把重构后的文件冒充扫描前快照；Git旧视图CRLF与通用换行视图的摘要口径分开。

没有新增原版／第三方代码导入；已有来源注释和署名保留。用户WoodMaterials编辑及CompoundMaterials列对齐未纳入本批改动。未运行生成器、Gradle、Java编译、测试、游戏、存档重启、构建或推送；源码执行不变不代表已完成配方玩法验收。

继续独立处理ChemicalRecipeLoader、保留加载顺序的GeneratedRecipeResolver，再审方法级职责和版本边界。全面原版核查仍继续光莲、地表堆和其它未核系统，不能把四个候选处理完当全部架构完成。
