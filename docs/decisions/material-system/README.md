# 材料系统设计

本专题说明 GregTech6CE 重构后材料系统的整体设计与子域边界，覆盖目录声明、身份安装、属性与形态绑定、物品成分裁决、回收与分解计算，以及公共只读 API 与多代发布契约。设计目标是消除历史上跨静态表、生成类与运行时注册表的双写和隐式覆盖，保证每类材料事实只有唯一权威定义，并按各自生命周期发布给下游消费。

## 设计原则

- **单一事实权威**：材料身份与静态定义归规范目录；物品成分由带来源优先级的候选裁决器统一收敛；分解路线以类型化声明为唯一权威；回收发布通过所有权与冲突仲裁边界收敛。兼容旧入口只做单向透传或受限回退，不维持第二套状态。
- **固有定义与原生绑定分离**：进程内静态的材料身份、物性与关系在引擎初始化阶段一次性冻结；物品、方块、流体、Tag 候选与机器配方绑定留在平台与运行时层，随资源重载按完整代次提交。
- **确定性与阶段封闭**：安装与链接阶段出现未知引用、环路、身份冲突或非法规则时立刻终止启动并保持索引完整回退；运行时 Tag 与配方重载在整代失败时维持上一成功代次不变，不暴露半完成状态。
- **按消费者边界分层暴露**：引擎核心规则不依赖 `ItemStack` 或加载器类型；公开契约仅提供稳定键、不可变视图与显式声明接口，不将内部 `GTMaterial` 可变模型或配方表结构直接暴露为扩展协议。

## 分层架构与职责边界

| 源码层 | 核心包与入口 | 承担职责 | 禁止越界行为 |
| --- | --- | --- | --- |
| `api` | `gregtech.api.addon.material` | 公开稳定键（`MaterialKey`、`DataKey`、`FormKey`、`MaterialGroupKey`）、`AddonMaterialContributor`、类型化规则对象与只读目录/形态视图契约 | 不引用 `core` 实现类或游戏 `Item` / `Fluid` |
| `core` | `gregtech.content.material.catalog`<br>`gregtech.engine.material`<br>`gregtech.engine.recipe` | 七类材料规范目录与分解声明；身份安装状态机（`GTMaterialRegistry`、`MaterialRegistryState`）、引用链接（`MaterialLinker`）、成分候选裁决（`ItemCompositionResolver`）、回收与分解纯计划（`ItemRecoveryPlanner`、`MaterialDecompositionPlanner`）及发布版本号管理（`MaterialPublication`） | 纯规则与计划类不读取 `RecipeMap`、前缀渲染细节或游戏物品栈实例 |
| `minecraft` | `gregtech.engine.material`<br>`gregtech.content.material`<br>`gregtech.content.recipe` | 运行时形态与实体绑定（`MaterialFormContent`、`MaterialEquivalence`）、物品成分安装与重载（`ItemMaterialRegistry`、`MaterialItemCompositions`）、堆栈内容策略（`ItemMaterialContentFacts`）、回收与分解配方适配器及统一发布边界（`RecoveryRecipePublisher`、`DecompositionRecipeLoader`） | 不绕过裁决器直接覆写 `EFFECTIVE` 成分或绕过发布仲裁直接写入回收配方 |

## 生命周期与数据流向

```
[1. 进程静态初始化（一次性）]
  content.material.catalog + Addon SPI
        │
        ▼  GTMaterialRegistry.init()
  MaterialRegistryState（身份/别名原子校验）
        → MaterialLinker（引用与组环检查）
        → 角色与规则闭包 / 冻结 MaterialInfoRegistry
        → MaterialViews（发布 MaterialCatalogView）
        → MaterialPublication.materialDefinitionsPublished(rev)

[2. 显式物品成分装配（启动期一次封闭）]
  显式物品 / 方块重量 / 现代修正 / 原版统一化 / 模组统一化 / CR.REV
        │  提交带 ItemCompositionSource 的候选
        ▼
  ItemMaterialRegistry（validateExplicitSnapshot 校验替换链后锁定 explicitClosed）

[3. 平台内容绑定与可重载代次（启动与资源重载）]
  原生资源 Tag 导入 / 数据包变更
        │  beginTagGeneration() → 构建新代 Tag 候选 → ItemCompositionResolver
        ▼  刷新 MaterialFormContent 绑定 → 回收/分解 RecipeMap overlay 重建
  成功：提交 MaterialPublication.platformGenerationPublished(compRev, recipeRev)
  失败：按检查点回滚成分快照与机器配方，保持上一完整 Stamp 不变
```

## 子域目录

1. [目录声明与生命周期安装](catalog-and-registration.md)：规范目录组织、五阶段安装状态机、附属合并与冲突策略、引用校验与冻结时机。
2. [属性、形态与标签体系](properties-forms-and-tags.md)：主定义与辅助信息拆解、类型化规则、布尔闭包、定义期形态规则与原生绑定分离、四类 Tag 及替代组边界。
3. [物品成分解析与运行时准入](item-composition.md)：结构化来源优先级、同级冲突与显式替换链、显式层封闭与 Tag 代次原子重载、耐久缩放及容器/模具形状区分。
4. [统一回收计划与配方发布仲裁](recovery.md)：基于有效成分的纯粉碎/热回收计划、大份量拆栈与精度表达、生产端输入证明、所有权与冲突发布仲裁。
5. [统一材料分解规则与计划](decomposition.md)：`MaterialDecompositionRule` 唯一权威、规范组成与精确单位计算、液/气替代相态与固体兜底、原生适配器与重载隔离。
6. [公共数据 API 与多代发布契约](data-api-and-publication.md)：已实现的只读视图（目录、元数据、分类/组、形态规则与实际绑定）、`MaterialPublication.Stamp` 一致性保证，以及明确标为`提议`的多快照来源图规划。
