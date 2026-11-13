# 物品成分解析与运行时准入

本文说明静态物品成分如何在多源候选之间按优先级裁决并保持全链路一致，以及运行时如何区分堆栈存储内容物、模具纯形状与容器状态。设计确保同一物品在查询、展示、形态等价判断与批量回收生成中读到同一份带完整溯源的有效成分，且成分记录按整条来源裁决、绝不跨优先级拼接授权或组件。

## 结构化来源与优先级仲裁

`ItemCompositionResolver` 将同一物品收到的全部 `ItemCompositionCandidate` 依据 `ItemCompositionSource.Kind` 分为四级，选出存在存活候选的最高层级整体胜出：

| 优先级 | 来源类别 `Kind` | 适用产生方 | 同级与跨级仲裁规则 |
| --- | --- | --- | --- |
| **1（显式层）** | `EXPLICIT`、`VANILLA_JSON`、`CORRECTION`、`BLOCK_WEIGHT`、`LEGACY` | 显式物品构造、原版成分 JSON、人工修正表、方块重量定义、废弃旧入口适配 | `LEGACY` 保留同物品最后写入兼容语义；其余结构化来源同级冲突且无覆盖声明时**直接报错**；内容与权威一致时合并溯源信息 |
| **2（统一层）** | `UNIFICATION` | 原版与外部模组物品统一化映射（矿石、锭、储藏块等） | 仅当显式层缺失时生效 |
| **3（标签层）** | `FORM_TAG` | 基于 `MaterialEquivalence` 从原生资源 Tag 派生的常见形态候选 | 前两层缺失时生效；单物品若命中多个不兼容常见形态 Tag，记录冲突详情并排除该歧义物品进入有效集 |
| **4（反推层）** | `RECIPE_REVERSE` | 由特定合成或逆向规则推导的兜底成分 | 最低优先级兜底，绝不覆盖前三层声明 |

- **显式覆盖链检验**：有意覆盖同级前序声明必须显式填写 `replacesSourceId`。系统允许覆盖者先于目标注册，在全部显式生产端完成后由 `validateExplicitSnapshot()` 统一校验目标存在、禁止多候选争抢同一被覆盖目标，并拒绝覆盖环；`ItemCompositionSource` 同时保存完整覆盖链以供诊断溯源。
- **组分排序无关性**：规范化构造时，组件统一按“显式主材料优先（若存在）、其余组分按规范键稳定排序”整理，确保复合部件（如复合工具头）中的次要金属不丢失，也不受注册顺序影响。

## 原版炉授权、纯形态识别与耐久缩放

- **炉规则准入与成分描述分离**：拥有成分不代表有权改写原版熔炼或拆除原版高炉行。`CORRECTION`、`BLOCK_WEIGHT` 与常规描述性 `VANILLA_JSON` 的默认 `MaterialProcessingAuthority` 为 `DENY`（仅当 JSON 显式以 `inherit` 继承统一化规则时放行）；`hasMaterialProcessingRule(ItemStack)` 只考察**胜出来源**，被淘汰的底层候选不能激活炉规则。
- **纯形态判定边界**：`MaterialEquivalence.form()` 只承认单一完整材料形态为纯材料形态；类似复合钻头等含多种成分或复合物品的条目即使具有主材料身份，也不会被误判为纯锭/纯工具头。
- **耐久缩放边界**：`ItemCompositionScaling.scale()` 按现存耐久与最大耐久的比例使用防溢出整数算法缩算剩余组分。若损坏导致原主材料余量降为 0 而副金属仍有剩余，缩放结果保持“无有效主材料”标识，绝不把残余副金属提拔伪装为主材料。

## 显式封闭与 Tag 代次原子重载

`ItemMaterialRegistry` 将显式静态注册与随世界重载的资源 Tag 清晰隔开：

- **显式层一次性封闭**：显式成分与统一化绑定在进程生命周期内不可重载；全部显式生产端注册并验证通过后，设置 `explicitClosed` 锁死，之后拒绝任何显式层写入。
- **Tag 代次事务化重载**：每次资源/数据包重载触发 `beginTagGeneration()`，先生成覆盖上一代有效成分、Tag 候选、裁决诊断、`tagInputsReady` 与 `compositionRevision` 的检查点，随后切换为 `TAG_INPUT_NOT_READY` 并清空旧 Tag 候选再解析新代 Tag；仅当整批解析与校验通过后替换 `EFFECTIVE`、推进实质变更后的 `compositionRevision` 并刷新 `entries()` 缓存与 `MaterialFormContent` 绑定。任意环节出错时按检查点完整回滚，不暴露半完成快照。
- **视图拆分**：`base()` 与 `entries()` 面向日常消费与回收生成；`declared()` 排除外部 Tag 派生，用于不接受外部 Tag 回导的内部生产端；`compositionDiagnosis()` 暴露胜出、覆盖与降级落选原因供审计与诊断。

## 容器、库存内容物与纯形状区分

历史上将任何带 NBT/DataComponent 的方块物品视作含内容物会误伤纯形状状态。现由内容层通过 `ItemMaterialRuntimeFacts` 向引擎注入 `ItemMaterialContentFacts` 策略：

- **纯形状不视为内容物**：已雕刻的 `MoldBlock` 若仅包含 `id="gt6ce:mold"` 与形态键，或已按规则完成生成的空金属箱，判定为无库存内容物（`hasStoredContents = false`），允许按正常规则参与熔炼与回收。
- **细粒度拒绝诊断（`RecoveryRejection`）**：废除物品级旧批量总许可字段（旧 `recoverable` 仅保留兼容语义），运行时按 `NO_COMPOSITION`、`NO_COMPONENTS`、`STORED_CONTENTS`、`UNSUPPORTED_CONTAINER`（如装液桶禁止入池，空原版桶放行）与 `ELIGIBLE` 给出明确拒绝分类；最终是否接受该前缀与热工艺转换由回收域规则把关。
