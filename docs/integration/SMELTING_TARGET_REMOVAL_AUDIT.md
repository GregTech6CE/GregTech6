# 附属模组移除熔炼目标后的坩埚物料保留

## 范围与根因

本次检查针对上游 PR #100（`97c0648bf`）新增的公开声明 `SmeltingTargetRemoval`，不改变其 API 或移除语义。该声明明确约定：所有附属模组的熔炼目标声明处理完毕后，移除指定目标；材料达到熔点后保留自身身份，而不是转为另一种材料。

实际可达路径为：

1. `AddonMaterialContributor.smeltingTargets()` 和 `smeltingTargetRemovals()` 通过受支持的 `ServiceLoader` 入口收集。
2. `AddonMaterials` 在链接末尾应用移除声明。
3. `GTMaterial.clearSmeltingTarget()` 将目标设为自身、产出比例设为 0，并移除 `MELTING` 标记。
4. 原先 `CrucibleProcess.process()` 在跨越熔点或接收高温新物料时，仍进入转换分支：删除原栈，再计算 `原数量 × 0 / U`。零数量结果不会重新加入内容列表，物料因而无声消失。

这不是未支持的冻结后修改，也不是默认“未声明目标”的情况：默认映射为自身、比例 U；公开的显式移除产生的是自身、比例 0。相关实现位于：

- `core/src/api/java/com/gregtech/gregtech/api/addon/SmeltingTargetRemoval.java`
- `core/src/main/java/com/gregtech/gregtech/api/addon/AddonMaterials.java`
- `core/src/main/java/com/gregtech/gregtech/api/material/GTMaterial.java`
- `core/src/main/java/com/gregtech/gregtech/api/machine/crucible/CrucibleProcess.java`

## 最小修复语义

只在坩埚转换分支增加 `getTargetSmeltingAmount() > 0` 条件。显式移除仍保持目标自身、比例 0、无 `MELTING`：不把比例恢复为 U，也不重新启用熔炼配方或显示条目。

正常正比例转换、未声明目标时的自身映射及冷却分支保留；先于转换执行的反应、低密度逸散、沸腾／燃烧、爆炸和酸蚀分支不被绕过。因此“保留物料”不等于免疫正常相变或危险效果。生产代码不增加 API 签名、注册 ID 或存档格式变更。

## 回归测试

### 真实公开入口的 core 合同

`CrucibleAddonContracts` 新增独立 JVM 场景 `smeltingremoval`，任务为 `:core:crucibleAddonContracts_smeltingremoval`，并纳入常规 `:core:check`。

测试创建真实服务描述文件，调用 `AddonMaterials.collectContributors(ClassLoader)`，由公开 `AddonMaterialContributor` 声明四种独立、归属明确的测试材料。先声明源材料的熔炼目标，再通过 `SmeltingTargetRemoval` 移除。没有修改 Tin 或其它内建材料。

27 项断言覆盖：注册归属与移除结果、熔炼预览仍缺席、跨越熔点、高温新物料、冷却／再加热、非整 U 数量精确保留、正常正比例转换、默认自身映射，以及到达沸点后仍正常汽化。

### 两端真实坩埚回归

共享测试精确身份为：

`smeltingtargetremovaltests.removedsmeltingtargetretainsconsumeddustacrosshotphase`

使用已被集中 CI 选择的 `gregtech_issue_audit` holder，以及两端各自路径下的 `crucible_removal_empty` 模板。该用例连同 PR #100 新增的分解检查，将原 601 项扩展为 **605 项：Forge 307、NeoForge 298**；未把未选择的旧 NBT／挤压用例计入执行数量。

两端 sample contributor 只增加 `SampleRetainedMatter`／`SampleRemovalTarget`，通过同一公开声明顺序移除前者的目标；保留上游前缀数量 `8 * GTValues.U`。输入来自实际注册的 addon dust，经世界中的 `ItemEntity`、坩埚吸取及缓存消费进入内容，不直接安装材料栈或注入 NBT。温度通过 `HeatHandler` 和真实平台 tick 提升。

两个钢坩埚分别覆盖冷态投入后跨越熔点，以及已处高温时投入冷物料；要求实际输入已消费、缓存为空、原材料身份和精确 U 内容仍在，并检查后续重复 tick。已移除目标的 dust 仍是合法物理输入：两端 `CrucibleItemInput` 的材料物品分支直接使用 `CrucibleInputRules.materialItem`，不要求 `MELTING` 或有效熔炼预览。

独立九格立方模板包含全部封闭测试空间。只允许加载／实体 tick 就绪检查重试；封闭、清空内部、放置、投料和加热仅执行一次。操作限制在模板内部，避开测试控制器；先封闭再清空以隔离自然地形水体。独立源码复核未发现可仅凭预览缺席、未消费输入或跨坩埚吸取而误通过的路径。

## 已取得的执行证据

|阶段|结果与边界|
|---|---|
|未修改生产代码的 core 基线|**实际失败，退出码 1**；精确断言为 `smelting target removal must retain material identity and amount: crossing melting`。|
|最小 guard 后的独立场景|**实际通过，27 项断言**。|
|最小 guard 后的 `:core:check`|**实际通过，退出码 0**；158 个任务中 149 个执行、9 个已最新，耗时 1 分 37 秒。|
|新增 native 源码及两端 sample 的缓存 API 编译|Forge JDK17、NeoForge JDK21 均通过；这是编译证据，**不是原生测试执行**。|
|集成后的完整双构建与 605 项原生门禁|**实际全部通过：Forge 307 + NeoForge 298，均严格检查退出码 0。** 双端构建/core 通过；156 项 Python、491 组 REMI、每端 1039 共享打包类及 SDK 118 核心/46 原生类检查通过。|

原始回执和日志分别保留于 `work/validation/smelting-removal-baseline/`、`work/validation/smelting-removal-core-fixed/`；完整门禁目录为 `work/validation/upstream-smelting-fixed/`。基线运行期间有无关 Python 证据测试变动，因此不声称完整工作树首尾一致；该基线生产代码未改。修复后 core 输入／终态 diff 一致，只作为 core 范围的身份比较。

## 结论边界

目前已证明公开移除声明可触发物料损失，且最小修复通过实际 core 基线／修复对照；两端实际原生坩埚回归及完整 605 项门禁也均通过。没有据此保证所有附属模组 API 的二进制兼容性，也未覆盖任意第三方运行时修改。

既有重启持久化证明仍属于 `9fed8aead` 基线；本次 PR #100 集成尚未重跑该冷重启流程，普通 605 项集中门禁不能替代它。
