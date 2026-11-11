# 04：统一物品成分解析

本计划让同一物品在生成、查询、回收和展示中得到同一份有来源的成分结果。状态：已实施本页范围；前置 [01](01-baseline.md) 与 [02](02-material-registration.md) 已完成。实现与检查结论见[04 验收记录](../../integration/material-refactor-04-composition.md)。

资源 tag 的导入／导出与别名桥接按 [08-C](08-material-data-api.md#c形态视图与资源-tag-桥接) 接线；导入只提交低优先级成分候选，不能把分类、资源 tag 成员关系或“含某材料”直接当作形态等价和回收授权。成分快照参与 08-D 的同版本内容发布。

## 当前问题与范围

core 的 `engine.material.ItemComposition` 将 prefix、组件、自由文本 source 和 `recoverable` 放在一起；Minecraft 的 `ItemMaterialRegistry.register()` 直接覆盖。`entries()` 只含显式项，`base()` 还查询标签派生，`get()` 再按耐久缩放。`MaterialEquivalence.form()` 对 `MaterialFormItem` 提前返回，使复合物品可能被当作纯材料形态。

本阶段统一成分的选择和解释，05 再统一机器回收生成。材料自身的化学组成仍由材料目录管理；物品成分只是“这个物品包含多少哪些材料”。

## 实施步骤

### 1. 把来源与优先级变成规则

1. 候选记录带结构化来源：来源类别、声明 ID／生产端、必要的 GT6 提交或项目规则标识；自由文本只保留说明用途。
2. 初始优先级采用下表，且对整个成分记录裁决，不把高优先级的组件与低优先级授权无意拼接。

| 顺序 | 候选来源 | 适用方式 |
| --- | --- | --- |
| 1 | 显式物品定义、人工审核的现代修正、JSON 成分 | 共同进入显式层；有意覆盖必须指明被替代声明，不依赖加载先后 |
| 2 | 原版／外部物品统一化目标 | 显式层缺失时采用 |
| 3 | 材料形态标签派生 | 前两层缺失时采用；同物品多个不一致标签为冲突 |
| 4 | CR.REV 配方反推 | 仅兜底，注明派生来源，拒绝矛盾与不可确定的循环 |

3. 同优先级语义相同的候选合并来源；不一致时失败并指出两个声明。显式覆盖必须唯一，检查目标存在及覆盖链无环。
4. 优先级较低的候选不覆盖较高候选；不同授权也属于冲突语义，不把 `true` 默认当作更可信。
5. 明确组件集合顺序与主材料是两件事：纯形态要求单一完整材料；确需主材料的显示／工具身份使用显式字段，不再把首组件当通用答案。

交付：可在 core JUnit 中测试的候选归一化与冲突裁决；不依赖 `Item`，使用稳定物品键或外层映射。

### 2. 收拢生产端并发布一致视图

1. 将 JSON、统一化、修正、方块重量、构造器自注册、标签派生和 CR.REV 的写入改为提交候选；逐个生产端撤掉直接 `put`。
2. JSON 显式给出 prefix，或用结构化声明指定继承的统一化目标；不从“之前碰巧注册的记录”读取 prefix。缺少明确继承目标时报错。
3. 解析后发布不可变的有效成分快照，按物品稳定 ID 遍历。批量生成与 `base()` 查询使用同一快照；`explicit()` 如保留，明确仅用于诊断和来源查询。
4. 标签加载和配方反推结束后再发布需要它们的快照。所有派生项有归属和 revision，重载先构建新结果、校验成功再替换，不能累积旧标签候选。
5. 若派生链需多轮求解，规定有限收敛过程和环检测；不能因遍历顺序不同给出不同结果。组件按来源语义确定，不能从回收配方反推原物品成分形成循环。
6. 保留“未加载标签”和“该物品无成分”的区别；初始化阶段消费者不得把临时缺失永久缓存。

### 3. 独立表达成分与加工准入

1. 内部将 `recoverable` 对应的语义明确命名为批量回收授权。是否存在成分、是否满足某热加工工艺、是否允许批量回收分别判定。
2. 现有 JSON 键若保持不变，解析时映射到内部语义；若确需改名，提供旧键兼容读取与双键冲突检查，并写明迁移，不静默改变格式。
3. 栈状态检查统一处理耐久、内容物和实体／流体容器；区分模具形状等纯状态与真实库存，不把所有 NBT／数据组件都视为“含内容物”。基础成分快照不缓存 `ItemStack`，状态缩放在查询时完成。
4. 损坏比例采用经裁决的整数算法，验证乘法溢出与向下取整。不能简单全局拒绝所有损坏物品，也不能让静态满额配方接受已缩量的物品。
5. `MaterialEquivalence`、库存形态识别和 tooltip 从有效记录读取；物品实例的 prefix／material 可作为身份信息，但不能覆盖实际复合成分。
6. `OvenRecipeBridge` 区分“成分可查询”和“应该按材料规则改写熔炼”；描述性成分不能自动授权删除原版配方。

### 4. 合并双平台成分安装

1. 以 `GregTechNeoForge.finishSharedSetup()` 中的 `registerBlockCompositions()` 与 Forge 的 `MaterialLinkBootstrap`、材料注册入口为对照，列出缺失的方块／植物成分。
2. 将纯粹的物品／方块成分提交步骤放共享 Minecraft 入口，平台层只负责在对象可解析后调用；不移动所有注册家族或任意改变整个初始化顺序。
3. 铜块覆盖、Glowtus／BedrockFlowers、普通材料块与石材／半砖逐项裁决。共同存在的物品必须一致，版本独有物品按明确存在条件处理。

## 独立验证

检查集：D、F、J、C、G；JSON 或成分资源改变加 B，公开契约改变加 A。

| 场景 | 通过条件 |
| --- | --- |
| 顺序独立 | 打乱同一组候选提交顺序，有效结果与诊断一致 |
| 冲突 | 同级不同组件／授权失败；显式覆盖唯一且可追踪；同义候选可合并 |
| 查询一致 | 任一有效物品都能从遍历视图与单物品查询取得同一基础成分，含标签派生 |
| 完整组成 | 复合工具头中的 Steel 不丢失；等价形态识别不把它当纯主材料 |
| JSON | 无效材料、非法量、未知 prefix、缺继承目标、互相矛盾的键均失败 |
| 状态 | 完好、部分损坏、耗尽；空桶、装液桶；空容器、有物品／实体容器分别断言 |
| 重载 | 标签成员变更后，新有效视图替换旧视图，revision 与相关缓存失效；重复同数据重载不增加记录 |
| 双平台 | 相同物品的组件、量、prefix 与授权一致；铜块等已知差异有明确处理依据 |

纯候选测试不需要游戏；物品身份、标签与组件／NBT 检查必须跑两个节点。重载测试不能只比较 map 大小，还要验证被移出标签的物品失去相应派生行为。

## 完成与拆分

建议交付：纯裁决规则 → 显式来源迁移 → 标签／CR.REV 与快照 → 查询消费者与状态判定 → 双平台安装。每步保留一个权威解析结果，尚未迁移生产端通过适配提交，不能继续绕开冲突检测。

完成条件：所有成分消费者可解释“选中了哪条声明、为什么”；批量生成与运行期视图一致；没有无来源的最后写入者覆盖。05 可以直接消费本阶段结果，不再重新推断物品成分或默认授权。

## 实施结果（2026-11-09）

- 纯规则新增 `ItemCompositionSource`、`ItemCompositionCandidate`、`ItemCompositionResolver` 和 `ItemCompositionScaling`。候选按结构化来源优先级整条裁决；同级相同内容合并来源，不同内容或回收授权失败；显式覆盖必须命中现存声明并拒绝覆盖环。
- `ItemComposition` 保存显式主材料、结构化来源列表与独立的批量回收授权。组件按“主材料优先、其余材料稳定排序”归一化，因此提交顺序不再改变结果；兼容访问器只在迁移期转发这些字段。
- `ItemMaterialRegistry` 改为显式候选与完整 tag 候选两张输入表，单物品查询和稳定 ID 遍历读取同一有效快照。tag 重载先解析完整替换集，失败时不修改有效结果；只有内容变化才推进 revision。
- JSON、原版统一化、模组统一化、人工修正、物品/方块构造器、木材和肉类生产端均提交带声明 ID 的候选。木材与肉类定义对 JSON 的既有覆盖改为唯一、可追踪的显式替代，不再依赖加载顺序。
- `MaterialEquivalence.form()` 只接受单一完整材料形态；复合工具头保留全部组件但不再当作纯主材料。耐久缩放使用无乘法溢出的整除算法。铸模空腔归类为纯形状状态，不再阻止熔化或回收。
- 成分存在、批量回收授权和炉规则准入已拆开。JSON 中的描述性成分类别不删除原版熔炼行；原生材料形态、统一化与 tag 规则仍可进入炉策略。`OvenRecipeBridge` 通过 `hasMaterialProcessingRule` 使用这一独立判定。
- 共享 `MaterialItemCompositions` 负责物品形态、普通材料块、石材/半砖重量、机器构造材料、线缆、木板、运输构件和原版绑定；Forge 与 NeoForge 入口只追加各自实际注册对象可解析的矿石、植物和机器注册表成分。
- 检查集 D、F、J、C、G、B 已执行：core 与 API JUnit、格式和路由检查通过，两节点生产及测试源码编译通过，`assembleBothPlatforms` 通过；Forge 1.20.1 实际执行 73 个 GameTest 全部通过，NeoForge 1.21.1 实际执行 78 个 GameTest 全部通过。JSON 负例覆盖未知材料、非正数量、未知 prefix、缺失继承目标及互相矛盾的键。当前测试环境未安装能提供外部 common-form tag 的模组，因此真实有效 tag-only 物品由纯候选测试覆盖；两节点实际重载覆盖了 420 个 ore-prefix tag 候选、快照 revision 与重复重载不累积。公开 API 和游戏内展示未改，未执行 A、M。

## 审查闭合（2026-11-10）

本轮审查重新区分了 04 自身欠账与后续阶段：失败重载的原子性、结构化来源、加载顺序、JSON 继承、损坏缩放和成分重载断言都属于本页完成条件；05 的发布原子性只覆盖回收／配方行，07 才负责更广泛的旧 API 和内容退出，不能替代这些 04 修复。

- 外部 tag 重载在构建和校验新 generation 前不再清空成分快照；失败发布会保留上一份有效成分，成功发布后才整体替换。回收／配方行的完整事务边界仍由 05 负责。
- `ItemCompositionCandidate` 移除了未消费的 `itemKey`，`ItemComposition` 移除了无消费者的 `recoverable()` 兼容访问器；legacy 来源构造和注册入口明确标记弃用，结构化入口继续作为迁移目标。
- `ItemCompositionSource.reference` 现在承接旧自由文本来源，并在诊断描述中输出；通过结构化注册入口提交时，原来源会保留为引用，不再只留下生产端名称。
- 显式覆盖允许在目标声明之前注册，最终由 `ItemMaterialRegistry.validateExplicitSnapshot()` 在全部生产端完成后统一严格校验目标存在；注册顺序不再决定覆盖结果。
- 木料、肉类和现代修正改用 JSON 数据表本身解析声明 ID，不再查询当时的候选快照。`vanilla_compositions.json` 中 88 个使用 `vanilla-unification` 的条目显式写入 `inherit`；无 `prefix`／`inherit` 的其余条目保持无 prefix，不再隐式借用统一化表。
- 损坏缩放抽取为 `ItemCompositionScaling.scale(ItemComposition, ...)`；主材料缩为 0 而其他组分仍存在时会移除失效主材料身份，不再触发构造异常。
- GameTest 增加 tag 冲突发布失败不改旧快照、清空 tag 后派生成分消失的断言；真实重载比较完整有效成分映射，不再只比较条目数。
- 检查结果：本轮定向 core JUnit、双平台生产与测试源码编译、`assembleBothPlatforms`、路由检查和 ratchet Spotless 通过；Forge 1.20.1 在全新目录执行 74 个 GameTest 全部通过，NeoForge 1.21.1 在全新目录执行 79 个 GameTest 全部通过。未执行客户端 tooltip／JEI／EMI 手工验收。

### 第二轮审查闭合

- `hasMaterialProcessingRule()` 只读取有效成分快照中的胜出来源；vanilla JSON 仅在其声明显式 `inherit: vanilla-unification` 时授权炉策略，被淘汰的低优先级 tag／统一化候选不再改变结果。
- Panel 材料改为提交最低优先级的 CR.REV 候选，不再读取当时已经发布的 `base()`。构造家族从同批静态规则或 `OriginalDeviceMaterialData` 推导，木板名称按物种声明稳定映射。
- `ItemCompositionSource` 保存完整覆盖链；同一同级声明被多个候选覆盖时发布前失败，最终来源可直接说明其覆盖了哪些声明。
- Forge 与 NeoForge 的 vanilla 材料绑定统一调用共享 `MaterialItemCompositions.registerVanillaBindings()`；NeoForge 重复的基础机器成分提交已删除。
- 复验通过：完整 `:core:test`、双平台生产与测试源码编译、ratchet Spotless；Forge 1.20.1 GameTest 74/74，NeoForge 1.21.1 GameTest 79/79。

### 第三轮审查闭合

- 主材料的单一组件推断已移出 `ItemComposition` 规范构造器，只保留在 legacy 构造入口；损坏缩放丢失原主材料时继续使用无效主材料，不再把剩余次要组分提升为新的主材料。
- `ItemMaterialRegistry` 新增 `CompositionLookupState`，区分有效成分、已完成 generation 的明确缺失和 tag 输入尚未发布；首次成功发布 tag generation 会推进 revision，失败发布不改变 readiness。
- 定向 core JUnit、双平台生产与测试源码编译通过；Forge 1.20.1 GameTest 75/75，NeoForge 1.21.1 GameTest 80/80。新增 GameTest 验证 tag-only 成分能生成 Shredder 行，撤销 tag 后该行不可查询。

### 第四轮审查闭合（2026-11-11）

- `ItemMaterialRegistry` 在全部显式生产端完成最终覆盖校验后关闭显式层；此后显式注册失败，tag generation 仍可按重载生命周期替换。
- 新增 `declared()` 有效成分视图，用于需要排除外部 tag 回导的消费端。生产代码不再调用仅供诊断的 `explicit()`；原调用已改为读取 `base()` 或 `declared()`。
- `RecoveryInputProof` 取代分开的 thermal 输入证明，统一校验物品身份、损坏、内容物、催化剂、容器和空流体处理。
- Forge 1.20.1 GameTest 77/77、NeoForge 1.21.1 GameTest 82/82；双平台生产与测试源码编译、core JUnit 和 ratchet Spotless 通过。

### 第五轮审查闭合（2026-11-11）

- 外部 tag 导入先收集同一物品的全部可识别形态，再交给 `ItemCompositionResolver` 统一合并或报告冲突；歧义项记录物品 ID 和冲突描述后不进入 generation，形态消费者只读取胜出的有效成分。
- `ItemMaterialRegistry.entries()` 按现有 composition revision 缓存不可变有序快照；同 revision 重复遍历不再复制和排序全量组件。
- tag generation 内容或 readiness 变化后直接刷新公开形态绑定，查询路径不需要等待下一次平台启动。
- Forge 1.20.1 GameTest 78/78、NeoForge 1.21.1 GameTest 83/83；双平台生产与测试源码编译通过。
