# 03：整理属性与形态规则

本计划让结构化属性、布尔能力和形态规则各自承担明确职责，并统一形态生成与重量查询。状态：已实施本页范围；前置为 [02](02-material-registration.md)。

与 [08-B／C](08-material-data-api.md) 同步实施：08-B 的分类 tag、行为 flag、替代组定义与查询已随 02 完成；本阶段继续迁移现有 `Any*` 工艺消费者，并实施 Minecraft 资源 tag 与 08-C 形态视图。形态 API 分别回答规则允许、实际绑定和有效获取途径。具体接口语义以[设计提议 0001](../../decisions/0001-material-data-api.md)为准。

## 当前边界与目标

`GTMaterial` 位于 `api/common/src/main/java/gregtech/api/material/`，混合身份、燃料、工具和加工目标；`MaterialProperty` 混合类型、状态和生成能力。core 的 `content.data.MaterialPrefix`、`MaterialPrefixes`、`content.data.generated.MaterialForms` 和 `engine.prefix.BlockMaterialPrefix` 分别承载形态事实。

本阶段复用现有材料对象身份，逐个字段域更换存储和消费者。目标是“身份／展示信息 + 有类型的稀疏属性 + 布尔 flag”，先验证收益再迁下一域；不要求一次性引入完整属性框架或重写全部 prefix。

## 实施步骤

### 1. 建立字段与消费者映射

1. 为燃料、矿石加工、工具、流体／热学、组成、生成能力分别列出读写者及默认值。
2. 区分“没有该能力”和“有能力但数值为零”。例如零灰烬可能合法，不得因为没有灰烬就删除燃料属性。
3. 记录字段变动是否影响附属查询、物品生成、坩埚行为或配方单位；保持由 02 确认的稳定身份与引用关系。

交付：每个域的唯一存储点与迁移顺序；先做燃料，再做矿石／工具，最后迁热学、组成与形态能力。

### 2. 先迁燃料并验证一个完整闭环

1. 引入燃料值和燃烧目标／灰烬数量的类型化值对象，由 02 的声明持有；取值缺失代表不是燃料。
2. `GTMaterial` 的旧燃料 getter／setter 在过渡期只读写该属性，不能保留一组独立旧字段。
3. 迁移 `FuelRecipeLoader`、`FurnaceFuelValue`、`FurnaceFuelHelper` 等消费者；真实游戏类留 Minecraft 层，燃料量计算留纯规则。
4. 扩展已有 `FuelParityTest`：保留 Lignite、Peat、Charcoal 回归，核对 WoodRubber、Bamboo、Livingwood、Dreamwood、Greatwood、Silverwood 的热值与灰烬。无来源证据的条目保持待核对。
5. 多组件燃料的燃料值与灰烬从同一完整组件集合计算；先核对输出能力与 GT6 规则，单槽不足时明确拒绝、批处理或既定副产物处理，不能任取第一组件作为通用答案。

本步验收通过后才复制模式到其他属性，避免未验证的抽象扩散。

### 3. 逐域迁移属性与 flag

1. 对矿石、工具等实际需要组合字段的域建立小型不可变属性对象；简单布尔能力放 flag，不为每个标量建立无消费者的类型。
2. 若使用 `PropertyKey<T>`，读取 API 必须保持类型匹配，不能让调用者任意强转 `Map<String, Object>`；属性对象不能反向依赖 core 或游戏实现类型。
3. 梳理 flag 的依赖：可补全的布尔依赖做确定性的闭包；需要数值属性的依赖缺失则报错，不凭空生成工具强度或流体转换率。
4. 检测依赖环、重复冲突属性和链接后非法修改；最终校验时机应覆盖合法附属声明与链接回调之后的状态。
5. 逐域迁移 getter／setter 和消费者，再删除旧字段。旧 `MaterialProperty` 在调用方迁完前只作为映射门面，其值不能与新 flag 成为两个可独立修改的集合。
6. 核对 Phosphor 的 MELTING，变更依据与配方／坩埚行为单独记录；不能把属性迁移与无依据的行为更改混成一项。

### 4. 统一形态可生成性与数量

1. 从 `engine.material.MaterialItemDefinitions.candidatePrefixes()` 及方块注册入口追踪实际候选、过滤和最终注册，不按 prefix 声明总数推断生成数量。
2. 保留物品 prefix 与方块 prefix 必需的不同元数据，共用稳定形态身份、材料量和可生成性规则；无需为了“一张表”强迫两者变成同一个类。
3. 将 `MaterialForms` 的有效规则并入规范声明，删除 ID／名称双轨查询。显式强制形态、附属 prefix、polymer／wire 请求与历史例外均作为可定位规则。
4. prefix 继续决定默认材料量、图标与显示类别；复合工具头、弹药等实际成分由 04 的权威成分记录决定，不能被默认重量替代。
5. 形态注册与配方生成共用“此形态是否实际存在”的查询；缺失形态不生成悬空配方。遍历顺序固定，但不构造全部材料×形态的常驻笛卡尔积。
6. `MaterialPrefixes` 没有 delegate 的名称先分类为外部兼容身份、纯元数据或死条目；只有 07 确认无消费与兼容需求后才能删除。

## 独立验证

检查集：D、F、J、C、G；API 类型或契约改变加 A，资源或新生成物改变加 B。

| 验证域 | 必须通过的断言 |
| --- | --- |
| 属性值 | 无属性、零值、非零值、非法负值分别处理；旧访问器与新属性读取结果一致 |
| flag | 闭包不依赖声明顺序；环／缺必需属性失败；不能产生无数据的加工能力 |
| 燃料 | 固体燃料权威案例保持；多组件输入的热值与灰烬使用同一数量口径 |
| 形态 | 普通金属、石英族、Paper、多组锭／板、显式强制形态和附属请求符合对照 |
| 数量 | 物品与方块重量单位不变；复合形态总量由实际组成而非首材料推算 |
| 游戏生成 | 两节点最终物品／方块身份、可查询形态配方一致；缺形态的负例不生成配方 |

为代表材料保留具体形态列表，避免只比较全局数量。补一个缺属性的 flag 和一个无效目标形态作为负例，确认诊断能定位材料、属性／prefix 与来源。

## 完成与拆分

建议按“燃料完整闭环 → 矿石／工具域 → 热学与组成域 → flag → 形态查询”交付。每个域完成时必须删掉原来的独立存储，再进入下一域。

阶段完成条件：已迁移属性唯一存储；形态生成和消费者使用同一规则；旧表只剩有明确迁移归属的部分；01 对照差异可解释。公开 API 迁移另列影响，不能因 `GTMaterial` 标记为 Internal 就省略产物契约检查。

## 实施结果（2026-11-08）

- `GTMaterial` 不再保存组成、工具、燃料、矿石副产物、加工目标或倍率字段；这些辅助事实只存于 `MaterialInfoRegistry` 的每材料可变条目，并在定义、附属链接和最终角色处理结束后一次冻结。旧访问器保留签名，只通过现有 `MaterialInfoTool.Host` 读写同一状态；组成中的坩埚权限也已并入组成值，不再另存一份。
- `FuelRule` 是燃料值与残留的唯一类型化声明。完整组件燃料计划从同一组件集合汇总热值和灰烬；同种灰烬合并，不同灰烬或超过单槽容量的结果拒绝燃烧，不再取首组件或截断。按固定 GT6 提交 `3703e403` 修正 WoodRubber、Bamboo、Livingwood、Dreamwood、Greatwood、Silverwood 的热值和灰烬；补 Bentonite 的 18 份 `SodiumHydroxide + 18U Clay` 组成，并按同源删除 Phosphor 的额外 `MELTING`。
- 布尔 flag 使用统一、与声明顺序无关的闭包：气体同时为液体，离心／电解权限包含 `DECOMPOSABLE`；`FLAMMABLE` 与 `UNBURNABLE` 冲突失败。附属离心／电解声明没有组成时在 READY 前失败，内置遗留组成的数据迁移仍由 06 负责。
- 流体 phase 修正已直接写回对应 catalog 声明，不再使用 catalog 外的按材料名二次覆盖表。
- API 1.7 引入的 typed `MaterialFormRulesView.rules()` 统一投影 item、block、fluid 定义期规则，`MaterialFormRule` 携带种类、默认材料量和显式请求来源；旧的 `forms()`／`allows()` 明确只表示 item 形态。配置了不存在的 process form 会在 READY 前失败。`MaterialFormsView` 在平台注册完成后发布 item、block、fluid 及 `ItemMaterialRegistry` 显式统一物品的实际绑定；binding 出现 kind 冲突时拒绝发布，规则允许与真实注册仍保持为不同事实。
- `MaterialForms` 改为从规范辅助快照读取，不再维护 ID／名称双轨查询。`MaterialGroups.members/contains` 通过 `MaterialTagsView` 读取组关系，现有 `Any*` 配方、tag、工具和终局材料 consumer 已改为读取该视图；旧符号只保留兼容入口。
- 常见 form 的 tag 映射继续集中在 `MaterialEquivalence`；`CraftingMaterialForms` 已删除第二份映射并复用该入口。更广的配方／世界生成 tag 仍归原领域，外部 tag 候选裁决留给 04。
- 补测覆盖 Paper 的全部源表多组板、NetherQuartz 的宝石／宝石板／棒、Iron 的物品／方块重量分离，以及 Tantalum 的强制宝石规则。发布期 form 读取改为使用已冻结的辅助快照，避免定义期视图漏掉源表形态。
- 检查结果：API 105 个 JUnit、core 131 个 JUnit、API 契约、本轮修改文件 Spotless、双平台生产编译与测试源码编译通过；Forge 1.20.1 和 NeoForge 1.21.1 各在全新目录实际执行 69 个 GameTest 并全部通过。生产资源和发行产物未改变，因此本阶段未执行 B；客户端手工场景不属于本页完成条件，未执行 M。
