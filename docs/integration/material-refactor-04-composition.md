# 材料重构 04：统一物品成分解析验收

日期：2026-11-09。适用分支：`master`，Stonecutter 节点 `1.20.1-forge` 与 `1.21.1-neoforge`。

## 结论

04 计划范围内已实施。物品成分现在只有一份有效快照，遍历、单物品查询、状态缩放、回收授权和炉规则准入均从该结果或独立策略判定读取。最后写入者覆盖已由结构化候选和冲突失败替代。

## 权威入口

| 职责 | 入口 |
| --- | --- |
| 来源与优先级 | `ItemCompositionSource` |
| 单物品候选 | `ItemCompositionCandidate` |
| 纯候选裁决 | `ItemCompositionResolver` |
| 有效值 | `ItemComposition` |
| 损伤缩放 | `ItemCompositionScaling` |
| 运行期快照与生产端提交 | `ItemMaterialRegistry` |
| 双平台共享安装 | `MaterialItemCompositions` |
| 外部 tag 候选 | `ExternalOreProcessing` |

优先级为显式物品/JSON/修正、统一化目标、材料形态 tag、CR.REV 反推。裁决按整条记录进行，不跨层拼接组件或授权。显式覆盖必须命名现存声明；覆盖环、同级冲突、重复声明和缺失目标在发布前失败。

## 行为裁决

- 木材与肉的 GT6 显式定义继续覆盖旧 JSON 表，但现在通过声明 ID 显式替代，不依赖加载调用顺序。
- 组件归一化以显式主材料为首，其余材料稳定排序。JSON 多组件行的主材料由行内首项声明；GT6 定义必须显式提供主材料。
- 复合形态不再满足 `MaterialEquivalence.form()` 的纯形态条件，但完整组件仍用于 tooltip、熔化、回收和分解。
- 铸模空腔是与真实库存不同的纯状态；无内容物的带形状铸模继续允许熔化和批量回收。
- `recoverable` 与 `bulkRecovery` 旧 JSON 键继续做兼容读取和相互冲突校验，但不再形成物品级回收门槛；规范 `ItemComposition` 将该兼容值归一为 `true`。成分存在、熔化准入、具体回收工艺和炉策略互不替代。
- CR.REV、显式 GT 材料定义、统一化和 tag 规则仍可授权炉策略；vanilla JSON 描述性行与人工描述修正不会单独删除原版熔炼行。

## 检查结果

执行且通过：

- `:core:test`：新增候选顺序独立性、同级合并/冲突、唯一覆盖与环、溢出安全损伤缩放测试。
- `:api:test`。
- Forge 与 NeoForge 生产源码和测试源码编译。
- `assembleBothPlatforms`、ratchet Spotless 和验证路由检查。
- Forge 1.20.1 在全新目录执行 73 个 GameTest，日志为 `All 73 required tests passed`。
- NeoForge 1.21.1 在全新目录执行 78 个 GameTest，日志为 `All 78 required tests passed`。
- JSON 负例验证未知材料、非正数量、未知 prefix、缺失继承目标、prefix/inherit 冲突及旧新回收键冲突均给出具体诊断。
- 两节点实际资源重载两次：每轮产生 420 个 ore-prefix tag 候选，有效快照 revision 单调推进，重复重载不累积条目，固定成分保持一致。

2026-11-10 审查闭合后补充：

- tag 发布失败保持上一份有效成分不变；清空 tag generation 时，依赖该 generation 的物品失去派生成分。回收／配方行的整体事务边界仍按 05 实施。
- 显式覆盖目标可以晚于覆盖声明注册，最终在全部成分生产端完成后严格校验；木料、肉类和现代修正不再依赖当时已发布的 JSON 候选。
- 来源引用保留在 `ItemCompositionSource.reference`，候选不再携带未使用的第二份物品键；legacy 构造和通用 `register(Item, ItemComposition)` 入口已标记弃用。
- 损坏复合物品的主材料缩为 0 时，快照保留仍存在的其余组分和稳定排序，不因主材料字段抛出异常。
- JSON 中 88 个统一化条目显式声明 `inherit: vanilla-unification`；未声明 `prefix`／`inherit` 的条目不再隐式继承。
- 复核通过：定向 core JUnit、双平台生产与测试源码编译、`assembleBothPlatforms`、路由检查和 ratchet Spotless；Forge 实际执行 74 个 GameTest、NeoForge 实际执行 79 个 GameTest，均报告全部通过。客户端展示仍未执行手工验收。

执行但未通过后已修复：

- 原版统一化空组件分支一度仍标为显式层，和 JSON 的 4U/9U 紫水晶块冲突；改为统一化来源后按优先级裁决。
- 木材与肉类 GT6 定义和 JSON 的组件/主材料不同；两处原最后写入行为改为显式声明替代。
- 初版快照在每个候选提交时全量重建，导致启动近似二次复杂度；改为单物品增量更新，tag generation 仍整批预解析后原子替换。

尚未执行：

- 当前测试实例未安装能提供外部 common-form tag 的模组，因此没有实机 tag-only 物品样例；纯 tag 候选裁决由 core JUnit 覆盖。
- 本阶段不改公开 API；tooltip、JEI/EMI 和客户端实际显示未执行 A、M。

## 第二轮审查闭合（2026-11-10）

- 炉策略改为只检查有效成分的胜出来源；JSON 的炉授权只由声明显式继承的 vanilla-unification 提供，不再读取被淘汰候选。
- Panel 材料全部以 CR.REV 候选提交，包含 concrete、玻璃与木板家族；不再以当前 `base()` 决定是否写入。
- 覆盖关系写入结构化来源并保留完整覆盖链；同一同级声明被多个候选覆盖会直接失败。
- Forge 与 NeoForge 共用 vanilla 绑定入口；NeoForge 重复的基础机器成分生产者已删除。
- 完整 `:core:test`、双平台生产与测试源码编译、ratchet Spotless 通过；Forge 74/74、NeoForge 79/79 GameTest 通过。客户端显示仍未做手工验收。

## 第三轮审查闭合（2026-11-10）

- 规范 `ItemComposition` 不再从单一剩余组件推断主材料；legacy 构造器继续为旧声明执行迁移期推断，损伤缩放则保留“原主材料已丢失”的无效状态。
- `CompositionLookupState` 现在区分 `PRESENT`、`ABSENT` 与 `TAG_INPUT_NOT_READY`。首次成功发布 tag generation 会推进 revision，发布失败仍保留旧 generation。
- 定向 core JUnit、双平台生产与测试源码编译通过；Forge 1.20.1 GameTest 75/75，NeoForge 1.21.1 GameTest 80/80。新增 tag-only Shredder 加入/撤销运行时断言。

## 第四轮审查闭合（2026-11-11）

- 显式成分层在最终覆盖校验后关闭；后续显式提交明确失败。
- 新增 `declared()`，过滤外部 tag 赢家；生产消费者不再把诊断用 `explicit()` 当权威输入。
- 输入证明统一为 `RecoveryInputProof`，移除重复的 thermal 状态检查。
- 完整 `:core:test`、双平台生产与测试源码编译、GameTest 源码编译和 ratchet Spotless 通过；Forge 1.20.1 GameTest 77/77，NeoForge 1.21.1 GameTest 82/82。

## 第五轮审查闭合（2026-11-12）

- 来源裁决确定为受控类别与显式覆盖链；同一优先级内容不一致失败，自由文本不再反向决定来源类别。方块重量、CR.REV 和其余内置生产端改用结构化来源。
- `ItemCompositionResolver.resolveDetailed()` 与 `ItemMaterialRegistry.compositionDiagnosis()` 保留胜出、被覆盖和低优先级候选及原因，tag generation 变化时诊断与有效快照同步更新。
- 弃用双参数注册仅对旧附属保留最后写入兼容；结构化和 tag 候选继续严格校验。legacy 多组件主材料／数量与 `recoverable()` 兼容行为已恢复。
- API 1.8.0 与 `gregtech:gt6ce-api:1.8.0` 发布坐标作为本轮累计材料 API 变更的最终版本决定。
- 执行且通过：`:api:check`、`:core:test`、Forge／NeoForge 生产与测试源码编译、ratchet Spotless、验证路由及注释检查；Forge 1.20.1 GameTest 90/90，NeoForge 1.21.1 GameTest 95/95。
- 首轮 Forge GameTest 发现 CR.REV 单组件的主材料被清除，Stone 铸模因此未保留 45U 构造数量；恢复兼容主材料推断后最终全量通过。
