# 属性、形态与标签体系

本文界定材料内部物性与能力、定义期形态规则与真实游戏绑定之间的分层边界，并将长期混用的四类“标签”概念（分类 Tag、能力 Flag、替代组与 MC 资源 Tag）拆分为各自正交的模型与消费者。

## 稀疏规则、类型化属性与布尔闭包

- **固有物性与过程规则分离**：熔点、沸点、密度与化学式归属规范主定义，通过 `MaterialProperties` 中的强类型 `PropertyKey<T>` 对外暴露。燃料（`FuelRule`）、矿石加工（`OreProcessingRule`）、单跳加工目标（`MaterialTarget` 为粉碎、熔炼、压粉目标）与分解规则（`MaterialDecompositionRule`）则作为稀疏规则通过 `MaterialProcessKey<T>` 挂载在辅助定义上；缺失规则代表“不具备该能力”，借此区分“无能力”与“零灰烬”等零值能力。
- **多组件燃料一致性**：计算多组件物品燃料时，热值与灰烬从同一完整组件集合结算；同种灰烬合并，不同灰烬混杂或超出单槽上限时拒绝燃烧，不再任取首个组件代表整体。
- **扩展性与不可变契约**：`MaterialProcessRule<T>` 保持对附属自定义类型的值扩展性而不作高开销深拷贝，但协议规定调用方在提交注册后不得变更所传入的值对象；属性键注册出现同键异类型时立即失败。
- **顺序无关的布尔闭包**：`MaterialPropertyRules` 与 `MaterialRoleFlags` 对布尔标志位执行确定性收敛——例如气体隐含液体性质，离心或电解规则单向投影为 `DECOMPOSABLE` 兼容标记；相互冲突的标记（如同时存在 `FLAMMABLE` 与 `UNBURNABLE`）或缺失必要数值依赖的能力在 `READY` 前报错拒绝。

## 定义期形态规则与运行期绑定

系统将“设计规则允许生成什么形态”与“游戏中实际注册了什么对象”明确拆为两层：

- **定义期形态规则（`MaterialFormRule` / `MaterialFormRulesView`）**：
  - 在材料初始化 `READY` 后即可查询，统一反映 item、block、fluid 形态的允许规则、默认材料量及请求来源（标准表单、特定请求、`polymer`/`wire` 请求或强制规则）。
  - 引用未知加工目标形态在 `READY` 前失败；旧 `forms()` / `allows()` 访问器仅保留作物品前缀兼容入口。
- **运行期实际绑定（`MaterialFormBinding` / `MaterialFormsView`）**：
  - `MaterialFormContent` 在平台注册完原生对象并完成外部 Tag 裁决后，发布各材料在游戏中实际可用的物品、方块与流体实例绑定（包含 `ItemMaterialRegistry` 通过显式统一化或有效 `FORM_TAG` 吸纳的等效物）。
  - 同一绑定出现 `MaterialFormKind` 冲突时拒绝发布；配方生成等消费者以实际绑定为准，不对着仅存在于定义规则中但未实际注册的前缀生成悬空配方。

## 四类标签与替代组的正交边界

| 类别 | 权威模型与来源 | 允许用途 | 显式禁止的行为 |
| --- | --- | --- | --- |
| **描述性分类 Tag** | `DataKey` 与 `MaterialTagsView`（内置分类源自目录；附属可声明带自身命名空间的分类与包含关系） | UI 展示、筛选、附属按分类检索；包含环与未知引用在 `READY` 前失败 | 赋予物品生成、回收、燃料或分解等机械能力（“金属分类”不自动生成锭或熔融能力） |
| **行为能力 Flag** | `MaterialProperty` 布尔标志集合（经确定性闭包收敛） | 决定候选前缀生成范围、世界生成候选与燃烧/导电等物理表现 | 绕过 `MaterialDecompositionRule` 或无组成数据时自行开放分解配方 |
| **材料替代组** | `MaterialGroupKey` / `MaterialGroupView`（来自 `GroupMaterials` 与 `AddonMaterialContributor` 组声明） | 承接 `Any*` 等等价组匹配（如工具头与特定工艺槽位中的材料替代） | 通过 `setRegistration` 将组成员合并为同一真实材料；组本身不可拥有独立物性与 ID；组嵌套禁止环路 |
| **原生资源 Tag** | `MaterialEquivalence` 集中维护常见前缀的拼写别名（如 `aluminium`/`aluminum`）与资源 Tag 映射 | 从已确认纯形态导出官方资源 Tag；在 Tag 加载后作为 `FORM_TAG` 低优先级候选提交给成分裁决 | 反写材料目录生成规则、凭多 Tag 猜测材料份量，或在重载时创建/删除原生物品 |
