# 统一回收计划与配方发布仲裁

本文规范粉碎机（Shredder）、锻造砧（Anvil）、热回收（Smelter / Melter）与工业坩埚的共同回收准则，通过“有效成分与栈状态 → 纯回收计划 → 唯一拥有者仲裁发布”数据流，解决历史上多生产端并行写入、字符串硬编码排除与 64U 静默截断等问题。

## 纯回收计算与数量守恒

引擎层 `ItemRecoveryPlanner`（以及面向材料的封装 `MaterialRecoveryRules`）通过抽象接口 `ItemRecoveryPlanner.MaterialFacts` 接收粉化目标与可用粉尘形态权重表，在脱离 `ItemStack` 与 `RecipeMap` 的前提下生成纯计划：

- **成分不越权授权**：存在成分只说明物料组成，物品能否进入粉碎机、热熔或坩埚仍由各自工艺的前缀准入、多组件限制与栈实时状态决定。
- **大份量拆栈与单槽余量报告**：
  - `MaterialRecoveryRules.dustPlan()` 与 `OM.dustAll()` 按 64 堆叠上限将大份量材料拆分为完整的多组粉尘计划（例如 72U 材料拆为 `64 + 8` 两组合法栈），消除无声截断；
  - `dustSlotPlan()` 与 `OM.dust()` 为兼顾单输出槽旧调用保留单栈选择规则，但显式汇报无法纳入该栈的 `remainderUnits` 供调用方扣量或拒收；多输出场景（分解、零件余料、外部矿石链路等）统一消费完整列表。
- **明确拒绝原因与损坏动态重算**：`ShredderPlan` 通过 `ShredderRejection` 严格区分空组分、无效组分、缺失粉化目标（`MISSING_PULVERIZATION_TARGET`）、低于最小粒度的不可表达量（`UNREPRESENTABLE_AMOUNT`）、输出槽上限溢出（`OUTPUT_LIMIT`）及整数溢出（`OVERFLOW`）；面对部分损坏输入，动态路径读取经耐久缩放的剩余成分重建出力与耗时，并由 `RecoveryInputProof` 保护静态配方行，拒绝用静态满额产出消费非满额输入或含内容物容器。

## 形态准入与原矿分类收敛

- **共享准入目录**：`RecoveryFormCatalog` 集中维护粉碎机生成形态子集（`RecyclablePrefixes`）与热回收完整 `RECYCLABLE` 集合，同时负责脏矿排除与容器形态约束，替代各配方加载器私自复制名单的做法。
- **结构化原矿分类**：通用原版回收通过 `VanillaUnificationDefinitions.OreInputKind` 区分矿石块（ore）、粗金属物品（raw metal item）与粗金属块（raw metal block），取代旧基于路径前缀 `raw_` 的脆性字符串过滤。
- **统一所有权入口**：`ThermalRecoveryOwnership` 统一裁决审核粉尘分数组与通用热回收的归属，移除对具体材料 ID 或矿材名的特殊硬编码；附属材料只要申报 `SMELTING` 目标和唯一流体绑定即可沿通用热回收路径以标准单位换算产出流体。

## `RecoveryRecipePublisher` 与冲突/重载隔离

`RecoveryRecipePublisher` 是粉碎机、锻造砧与全部热回收家族的唯一入表与仲裁边界。每条候选配方行携带显式 `Recipe.RecoveryOwnership`（标明配方族与作用域：`EXPLICIT` 显式专有行 vs `GENERIC` 通用回退行）：

| 冲突场景 | 仲裁动作（`Action`） | 设计目的 |
| --- | --- | --- |
| 索引桶内无等价条件碰撞 | `PUBLISH` | 正常入表 |
| 同家族同条件同输出 | `DUPLICATE` | 幂等去重，跳过后续重复行 |
| 已存在 `EXPLICIT`，新来 `GENERIC` | `EXPLICIT_OWNER` | 通用回退自动让位 |
| 已存在 `GENERIC`，新来 `EXPLICIT` | `REPLACE_GENERIC` | 移除旧通用回退并替换为专用行 |
| 同优先级同输入条件但**输出不同** | 抛出 `RecoveryConflictException` | 暴露多写者未决冲突 |

- **按输入索引桶查重**：带 `RecoveryInputProof` 状态守卫的行不能依赖普通 `RecipeMap#findCollision`，发布器直接查询 `RecipeMap.mRecipeItemMap` 物品输入桶并比对真实执行条件与催化剂，避免全表扫描。
- **启动停机 vs 单行/整代隔离**：内置静态基础行的非重复冲突终止启动；外部 Tag、数据包或附属派生的候选调用 `publishIsolated()` 记录告警并仅跳过冲突行；若发布流程出现结构性异常则由 `MachineRecipeOverlay` 整代回滚。
- **动态 Tag 回收行生命期**：通用粉碎的显式启动行留在静态基线；来源为 `FORM_TAG` 的动态行交由 `rebuildTagRows()` / `clearTagRows()` 接入 `MachineRecipeOverlay` 生命周期，随外部 Tag 重载同步更新或撤回动态索引。
