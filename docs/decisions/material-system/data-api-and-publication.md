# 公共数据 API 与多代发布契约

本文定义向外部附属与内部展示模块提供材料信息的公共只读契约，以及进程静态定义与平台多代重载之间保持一致的版本发布机制。文档严格区分当前已实现并交付的接口与尚处于`提议`状态的未来扩展规划。

## 契约设计边界

- **稳定键与模型隔离**：公共查询全部基于不可变键（`MaterialKey`、`DataKey`、`FormKey`、`MaterialGroupKey`）和只读视图接口，不把内部可变模型 `GTMaterial`、游戏物品栈 `ItemStack` 或 `RecipeMap` 暴露为协议字段。
- **明确失败语义**：对于宿主服务未绑定、材料系统未就绪、旧版本宿主不支持或查询未知键等场景，公开接口须明确抛出异常或区分状态，禁止统统返回空集掩盖故障。

## 已实现的公共视图（API 1.4–1.8）

附属接入手册与 Gradle 坐标细节详见[附属 API 文档](../../ADDON_API.zh-CN.md)。已交付能力划分如下：

| 公共视图与入口 | 生效时机 | 核心职责与约束 |
| --- | --- | --- |
| **`MaterialCatalogView` / `MaterialView`**<br>（`GregTechMaterials.catalog()`） | 进程静态 `READY` 后发布，由 `revision()` 标定版本 | 提供已安装规范材料（不含替代组）及强类型 `MaterialMetadata`、`PropertyKey<T>`（熔点、沸点、密度、化学式等）、只读分类与 `MaterialProcessesView`；禁止把配方来源列表、世界生成或 UI 数据堆砌在主材料视图上 |
| **`MaterialTagsView` / `MaterialGroupView`** | 同 `MaterialCatalogView` | 提供带命名空间隔离的分类 Tag 包含查询，以及取代 `Any*` 传统符号的材料替代组 `MaterialGroupKey` 成员与嵌套查询 |
| **`MaterialFormRulesView`** | 定义期规则（`READY` 后可读） | 按 `FormKey` 与 `MaterialFormKind`（`ITEM`、`BLOCK`、`FLUID`）暴露定义期形态规则 `MaterialFormRule`、默认材料单位与显式请求来源（`MaterialFormRequest`） |
| **`MaterialFormsView`** | 平台注册及外部 Tag 裁决之后发布，随 Tag 重载同步刷新 | 通过 `MaterialFormBinding` 返回各材料在游戏中实际注册且经认证的物品、方块与流体绑定（包含经显式统一化或有效 `FORM_TAG` 吸纳的等效表示） |
| **`AddonMaterialContributor` & `MaterialInfoTool.Registrar`** | 初始化窗口与链接期 | 附属声明自有命名空间的元数据、分类/组与过程规则（如 1.8 `MaterialProcessKeys.DECOMPOSITION`）；宿主通过 `MaterialProcessRule<T>` 保留自定义规则对象的扩展能力，约定提交注册后调用方不得再修改该对象 |

## 跨代一致性印记（`MaterialPublication.Stamp`）

由于材料目录定义在进程内一次定稿，而 Tag 成分、形态绑定与机器配方随世界或数据包重载多代替换，`MaterialPublication` 维护原子发布的版本戳 `Stamp(revision, materialRevision, compositionRevision, recipeRevision)`：

1. **进程定义提交**：`GTMaterialRegistry.init()` 到达 `READY` 时调用 `materialDefinitionsPublished(materialRevision)` 唯一一次写定基础材料版本，不允许二次改写。
2. **平台代次提交**：每次启动装配或热重载在全部 Tag 候选裁决、有效成分构建、`MaterialFormContent` 绑定刷新以及回收/分解配方重建均成功后，才由平台层推进 `platformGenerationPublished(compositionRevision, recipeRevision)`。
3. **失败回滚不动旧印记**：重载任一环节报错触发回滚时，`MaterialPublication.current()` 维持上一成功 `Stamp` 不变，保证下游观测不到“成分切至新代但配方仍留旧代”的跨代撕裂。

## `提议`：来源用途索引与有界处理图（尚未实施）

> [!NOTE]
> 本节规划尚处于**提议**状态，尚未进入公共 `api` 产物，不构成当前可调用 API 承诺。

- **会话化版本快照（`MaterialDataSnapshot` / `SnapshotStamp`）**：
  - 配方来源、用途与动态世界生成信息随具体服务端世界和数据包配置变化，拟引入显式接收服务端或会话上下文的快照入口，每次重载生成绑定唯一 `SnapshotStamp` 的自洽索引，避免跨服、断线或热重载后旧引用混用；
  - 来源与用途索引从最终 `RecipeMap`、原版炉镜像、已发布的回收/分解结果及真实生效矿脉配置直接投影（而非从化学成分表猜测或对 `Unit` 族做笛卡尔积展开）。
- **有界材料处理图（`ProcessGraph`）**：
  - 拟在核心层基于来源/用途快照支持有限深度、节点与分支预算的前/反向图检索，精确保留合金/分解/回收闭环中的共同输入、概率分支与状态条件，并在到达预算时诚实标注截断；
  - 客户端颜色、图标与坐标映射留在接入层，由 JEI 与 EMI 共用同一图语义模型与世界生成事实投影，解除彼此间的内部类耦合，同时确保专用服务端在无客户端可选模组时独立运行。
